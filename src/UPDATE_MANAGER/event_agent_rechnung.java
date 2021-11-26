package UPDATE_MANAGER;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class event_agent_rechnung extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 7848115657717666099L;

	boolean refAuftrag = false;
	boolean refLieferschein = false;

	String rechnungsnummer = "";
	String guid = "";
	String rechnungsdatum = "";
	String kundennummer = "";
	String lieferscheinnummer = "";
	String auftragsnummer = "";
	boolean rechnung_gebucht = false;
	int rechnungstyp = 0;
	// ------------------------------------
	int artikel_position = 0;
	String artikel_nummer = "";
	int menge = 0;
	int menge_lager = 0;
	int menge_fekturiert = 0;
	int menge_liefeschein = 0;

	String neu_artikel_log = "";
	String alt_artikel_log = "";

	public event_agent_rechnung(int _managerid, String _manager_run_creation_code, boolean _isLinux, String _opsystem,
			String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
			throws SQLException, ClassNotFoundException {

		super();
		this.DB_CONNECTOR = _db;

		this.AGENT_DBID = 999;
		this.FILE_CONTENT_ID = 0;
		this.MANAGER_RUN_CODE = _manager_run_creation_code;
		this.RUN_MANAGER_ID = _managerid;
		this.IS_LINUX = _isLinux;
		this.OPERATING_SYSTEM = _opsystem;
		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
		this.AGENT_DUPLIKAT_PATH = _duplikat_path;
		this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
		this.initialisiere_agenten_000(this.DB_CONNECTOR, this.AGENT_DBID);
		initialisiere_agenten_pfade(this.DB_CONNECTOR);

		this.ANZAHL_XML_OBJEKTE = 0;

	}

	public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
			String mandant_kuerzel)
			throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

		this.FILE_CREATION_CODE = _fileid;
		this.XML_CONTENT_IS_INSERT_TYPE = false;
		this.refAuftrag = false;
		this.refLieferschein = false;
		this.XML_FILE_WAS_EVER_PROCESSED = true;
		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;
		this.ANZAHL_XML_OBJEKTE = 0;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage(
					"---------------------------------------------------------------------------------------------------");
			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "]");
			this.DEBUGG_LOGGER.addMassage(
					"---------------------------------------------------------------------------------------------------");

			AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

			// -----------------
			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;
			update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);

			// -----------------

			this.XML_FILE_WAS_EVER_PROCESSED = wurde_diese_datei_schon_mal_verarbeitet(AGENT_DELIVER_XML_FILE,
					this.DB_CONNECTOR, this.AGENT_DBID);

			if (this.XML_FILE_WAS_EVER_PROCESSED == false) {

				// -----------------------------------------------------------------------

				Path absolut_path = Paths.get(this.AGENT_DELIVER_XML_FILE.getAbsolutePath());

				// Die Attribute der Datei werden ermittelt
				BasicFileAttributes datei_attribute = Files.readAttributes(absolut_path, BasicFileAttributes.class);

				long file_size = datei_attribute.size();

				this.FILE_CREATION_TIME_FOR_DB = formatdb
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.FILE_CREATION_TIME = format0
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.SINGEL_FILE_PROCESS_START_TIME = new Date();

				this.put_deliver_file_in_db(AGENT_DBID, this.AGENT_DELIVER_XML_FILE.getName(),
						formatdb.format(SINGEL_FILE_PROCESS_START_TIME), this.AGENT_RUN_CODE, file_size,
						FILE_CREATION_TIME_FOR_DB, FILE_CREATION_CODE, AGENT_DBID, MD5_FILE_HASHCODE, this.DB_CONNECTOR,
						this.AUSWAHL_MANDANT_DBID);

				// Finde id des Workfiles

				this.WORK_FILE_DB_ID = find_work_file_dbid(FILE_CREATION_CODE, this.DB_CONNECTOR);

				// -----------------------------------

				this.AGENT_WORK_XML_FILE_NAME = "XR_" + this.MANAGER_RUN_CODE + "_" + (MANAGER_filepositionsindex + 1)
						+ "_" + this.AGENT_NAME + "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME
						+ FILE_CREATION_TIME + "_work_" + FILE_CREATION_CODE + ".xml";

				// ------------------------------------------

				this.AGENT_WORK_FILE_XML = new File(
						this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + AGENT_WORK_XML_FILE_NAME);

				this.AGENT_DELIVER_XML_FILE.renameTo(AGENT_WORK_FILE_XML);
				// ------------------------------------------

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(AGENT_WORK_FILE_XML);

				doc = dBuilder.parse(AGENT_WORK_FILE_XML);

				doc.getDocumentElement().normalize();

				// ------------------------------------------

				NodeList rechnung_kopf_node = doc.getElementsByTagName("KhList");

				Node node_kopf_wurzel = rechnung_kopf_node.item(0);

				Element node_kopf_wurzel_element = (Element) node_kopf_wurzel;

				Element rechnung_kopf_element = (Element) (doc.getElementsByTagName("Kh").item(0));
				// ------------------------------------------

				//// Check Datei Typ
				// ------------------------------------------

				try {
					this.FILE_CONTENT_OPERATION_TYPE = new String(node_kopf_wurzel_element.getAttribute("Operation"));

				} catch (NullPointerException ecxc) {
					this.FILE_CONTENT_OPERATION_TYPE = "?";
				}

				if ("Insert".equals(FILE_CONTENT_OPERATION_TYPE) || "Insert" == FILE_CONTENT_OPERATION_TYPE)

				{

					XML_CONTENT_IS_INSERT_TYPE = true;

				} else {

					XML_CONTENT_IS_INSERT_TYPE = false;

				}

				// ------------------------------------------
				//// Lese Rechnungskopf
				// ------------------------------------------

				try {

					this.rechnungsnummer = rechnung_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
							.trim().replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					this.rechnungsnummer = "?";

				}

				try {

					this.guid = rechnung_kopf_element.getElementsByTagName("GUID").item(0).getTextContent().trim()
							.replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					this.guid = "?";

				}

				try {

					String tempDatumWareneingangAusXML = "";
					Date datum_wareneingang_temp = null;

					tempDatumWareneingangAusXML = rechnung_kopf_element.getElementsByTagName("Date").item(0)
							.getTextContent().trim();

					datum_wareneingang_temp = XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

					this.rechnungsdatum = formatdb.format(datum_wareneingang_temp);

				} catch (NullPointerException ecxc) {
					this.rechnungsdatum = "9999-09-09 00:00:00";
				} catch (ParseException e1) {

					this.rechnungsdatum = "9999-09-09 00:00:00";
					e1.printStackTrace();
				}

				try {

					this.kundennummer = rechnung_kopf_element.getElementsByTagName("KdNr").item(0).getTextContent()
							.trim().replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					this.kundennummer = "?";

				}

				try {

					this.rechnung_gebucht = new Boolean(rechnung_kopf_element.getElementsByTagName("RechnungGebucht")
							.item(0).getTextContent().trim().replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim());

				} catch (NullPointerException ecxc) {
					this.rechnung_gebucht = false;

				}

				try {

					BigDecimal menG = new BigDecimal(
							rechnung_kopf_element.getElementsByTagName("RechnungTyp").item(0).getTextContent());

					this.rechnungstyp = menG.intValue();

				} catch (NullPointerException ecxc) {
					this.rechnungstyp = 0;
				}

				NodeList refNodeListAuftrag = rechnung_kopf_element.getElementsByTagName("RefAuftrag");

				if (refNodeListAuftrag.getLength() > 0) {

					this.refAuftrag = true;

					try {

						this.auftragsnummer = rechnung_kopf_element.getElementsByTagName("RefAuftrag").item(0)
								.getTextContent().trim().replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim();

						if (this.auftragsnummer.trim() == "") {

							this.refAuftrag = false;
							this.auftragsnummer = "?";
						}

					} catch (NullPointerException ecxc) {
						this.auftragsnummer = "?";
						refAuftrag = false;
					}

				} else {

					this.refAuftrag = false;
					this.auftragsnummer = "?";
				}

				NodeList refNodeListLieferschein = rechnung_kopf_element.getElementsByTagName("LfRefNr");

				if (refNodeListLieferschein.getLength() > 0) {

					this.refLieferschein = true;

					try {

						this.lieferscheinnummer = rechnung_kopf_element.getElementsByTagName("LfRefNr").item(0)
								.getTextContent().trim().replace(" ", "$").replaceAll("[^A-Za-z0-9+-]", "").trim();

						if (this.lieferscheinnummer.trim() == "") {

							this.refLieferschein = false;
							this.lieferscheinnummer = "?";
						}

					} catch (NullPointerException ecxc) {
						this.lieferscheinnummer = "?";
						this.refLieferschein = false;
					}

				} else {

					this.refLieferschein = false;
					this.lieferscheinnummer = "?";
				}

				// Pruefe ob dieser Eintrag bereits exisitert
				this.CHECK_HEADER_ENTRY_EXISTENCE = existiert_diese_rechnung(this.rechnungsnummer);

				// bis hier fuer alle
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------

				// ==INSERT---------------------------------------------------------------------------

				if (XML_CONTENT_IS_INSERT_TYPE == true) {

					if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

						// Es handelt sich um eine Rechnung ohne Referenz

						String l001 = "";

						if (refAuftrag == false && refLieferschein == false) {

							this.DEBUGG_LOGGER
									.addMassage("#{I_RE_001} Rechnung ohne Referenzen wurde erzeugt (INSERT).");
							l001 = "#{I_RE_001} Rechnung ohne Referenzen wurde erzeugt (INSERT).n";

						}

						if (refAuftrag == true && refLieferschein == false) {

							this.DEBUGG_LOGGER
									.addMassage("#{I_RE_002} Rechnung mit Auftrag ohne Lieferschein (INSERT).");
							l001 = "#{I_RE_002} Rechnung mit Auftrag ohne Lieferschein (INSERT).\n";

						}

						if (refAuftrag == false && refLieferschein == true) {

							this.DEBUGG_LOGGER
									.addMassage("#{I_RE_003} Rechnung ohne Auftrag mit Lieferschein (INSERT).");
							l001 = "#{I_RE_003} Rechnung ohne Auftrag mit Lieferschein (INSERT).\n";

						}

						if (refAuftrag == true && refLieferschein == true) {

							this.DEBUGG_LOGGER
									.addMassage("#{I_RE_004} Rechnung mit Auftrag und mit Lieferschein (INSERT).");
							l001 = "#{I_RE_004} Rechnung mit Auftrag und mit Lieferschein (INSERT).\n";

						}

						this.DEBUGG_LOGGER.addMassage("---- Gebucht: [" + this.rechnung_gebucht + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsnummer: [" + this.rechnungsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Auftragsnummer: [" + this.auftragsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsdatum: [" + this.rechnungsdatum + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungtyp: [" + this.rechnungstyp + "]");
						this.DEBUGG_LOGGER.addMassage("---- Datei: [" + this.FILE_CREATION_CODE + "]");

						String l002 = "---- Gebucht: [" + this.rechnung_gebucht + "]\n";
						String l003 = "---- Rechnungsnummer: [" + this.rechnungsnummer + "]\n";
						String l004 = "---- Auftragsnummer: [" + this.auftragsnummer + "]\n";
						String l005 = "---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]\n";
						String l006 = "---- Rechnungsdatum: [" + this.rechnungsdatum + "]\n";
						String l007 = "---- Rechnungtyp: [" + this.rechnungstyp + "] \n";
						String l008 = "---- Datei: [" + this.FILE_CREATION_CODE + "]\n";
						String l009 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: [" + this.MANAGER_RUN_CODE
								+ "]";

						this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007 + l008 + l009;

						String sqlInsertRechnungsKopf = "INSERT INTO static_rechnung (" //
								+ " guid," //
								+ " rechnungsnummer,"//
								+ " rechnungsdatum,"//
								+ " last_agent_id,"//
								+ " insert_work_file_id,"//
								+ " lieferschein_nummer,"//
								+ " auftrags_nummer,"//
								+ " kunden_nummer,"//
								+ " gebucht,"//
								+ " last_manager_run_code,"//
								+ " last_agentrun_code,"//
								+ " last_agent_operation,"//
								+ " last_oparation_datum,"//
								+ " mandant_id, " ///
								+ " rechnungstyp ) VALUES ('" //
								+ this.guid + "','" //
								+ this.rechnungsnummer + "','" //
								+ this.rechnungsdatum + "'," //
								+ this.AGENT_DBID + "," //
								+ this.WORK_FILE_DB_ID + ",'"//
								+ this.lieferscheinnummer + "','" //
								+ this.auftragsnummer + "','" //
								+ this.kundennummer + "'," //
								+ this.rechnung_gebucht + ",'"//
								+ this.MANAGER_RUN_CODE + "','" //
								+ this.AGENT_RUN_CODE + "','" //
								+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log) + "','" //
								+ formatdb.format(new Date()) + "'," //
								+ this.AUSWAHL_MANDANT_DBID + "," //
								+ this.rechnungstyp + " );";

						this.DB_CONNECTOR.insertToDatabase(sqlInsertRechnungsKopf, "sqlInsertRechnungsKopf");

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList Rechnungspositionen = rechnung_kopf_element.getElementsByTagName("Kp");
						this.ANZAHL_XML_OBJEKTE = Rechnungspositionen.getLength();

						this.DEBUGG_LOGGER.addMassage(
								"-----------------------------------------POSITIONEN-----------------------------------------------");

						this.DEBUGG_LOGGER.addMassage("Rechnungs-Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");

						this.DEBUGG_LOGGER.addMassage(
								"---------------------------------------------------------------------------------------------------");

						// Alle Positionen werden eingetragen, da diese
						// Rechnung keine Referenzen hat.

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_im_rechnung = 0; position_im_rechnung < Rechnungspositionen
									.getLength(); position_im_rechnung++) {

								Node einzelne_positions_wurzel = Rechnungspositionen.item(position_im_rechnung);

								if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

									Element rechnungspositionElement = (Element) einzelne_positions_wurzel;

									int _alt_artikel_lagerbestand = 0;
									int _alt_artikel_durch_rechnung_reserviert = 0;
									int _neu_artikel_lagerbestand = 0;
									int _neu_artikel_durch_rechnung_reserviert = 0;

									try {

										BigDecimal menG = new BigDecimal(
												rechnungspositionElement.getElementsByTagName("Position").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										artikel_position = menG.intValue();

									} catch (NullPointerException ecxc) {
										artikel_position = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("Menge").item(0).getTextContent());

										menge = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLager").item(0).getTextContent());

										menge_lager = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_lager = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeFak").item(0).getTextContent());

										menge_fekturiert = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_fekturiert = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLp").item(0).getTextContent());

										menge_liefeschein = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_liefeschein = 0;
									}

									try {

										artikel_nummer = rechnungspositionElement.getElementsByTagName("ArtNr").item(0)
												.getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

									} catch (NullPointerException ecxc) {
										artikel_nummer = "?";
									}

									// ---------------------------------------------------------------------
									// Fuege Position ein
									// ---------------------------------------------------------------------

									if (this.artikel_nummer.contains("WP-") == false
											&& this.artikel_nummer.contains("WPK-") == false
											&& this.artikel_nummer != "" && this.artikel_nummer != "?"
											&& this.artikel_nummer.length() >= 5) {

										if (refAuftrag == false && refLieferschein == false) {

											// Normale Rechnung

											this.DEBUGG_LOGGER.addMassage(
													"#{I_RE_001_1} - Rechnungsposition wird vom Lager abgezogen:");
											l001 = "#{I_RE_001_1} - Position:\n";

											String sqlGetArtikelAlteWerte = "Select lager_anzahl, reserviert_durch_rechnung, agent_operation from static_artikel  where interne_artikelnummer='"
													+ artikel_nummer + "';";

											this.DB_CONNECTOR.readFromDatabase(sqlGetArtikelAlteWerte,
													"sqlGetArtikelAlteWerte");

											while (this.DB_CONNECTOR.rs.next()) {

												_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
												_alt_artikel_durch_rechnung_reserviert = this.DB_CONNECTOR.rs.getInt(2);
												this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

											}

											String gutschrieftStornoLog = "";

											if (this.rechnungstyp == 1 || this.rechnungstyp == 23) {

												menge = (menge * -1);

												gutschrieftStornoLog = "**************************************\n ------- GUTSCHRIFT -------\n**************************************\n ";
											}

											_neu_artikel_lagerbestand = (_alt_artikel_lagerbestand - (menge));
											_neu_artikel_durch_rechnung_reserviert = _alt_artikel_durch_rechnung_reserviert;

											l002 = "---- Rechnung: [" + this.rechnungsnummer + "]\n";
											l003 = "---- Gebucht:  [" + this.rechnung_gebucht + "]\n";
											l004 = "---- Lagerbestand: [a=" + _alt_artikel_lagerbestand + ",n="
													+ _neu_artikel_lagerbestand + "]\n";

											l005 = "---- Gesamt reserviert durch Rechnungen: [a="
													+ _alt_artikel_durch_rechnung_reserviert + ", n="
													+ _neu_artikel_durch_rechnung_reserviert + "]\n";

											l006 = "---- durch diese Rechnung abgezogen: [" + menge + "] Stueck(e)\n";
											l007 = "---- am: " + format2.format(new Date()) + " \n";

											l008 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
													+ this.MANAGER_RUN_CODE + "] \n";

											l009 = "---- Datei: [" + this.FILE_CREATION_CODE + "]";

											this.neu_artikel_log = gutschrieftStornoLog + l001 + l002 + l003 + l004
													+ l005 + l006 + l007 + l008 + l009;

											this.DEBUGG_LOGGER.addMassage("P [" + position_im_rechnung + "]");
											this.DEBUGG_LOGGER.addMassage(
													"---------------------------------------------------------------------------------------------------");

											String guid_verkauf = "";

											if (this.rechnungstyp == 0) {
												this.DEBUGG_LOGGER.addMassage(
														"- #{I_RE_001_1_1} - Rechnungsposition wird vom Lager abgezogen:");

											}

											if (this.rechnungstyp == 1 || this.rechnungstyp == 23) {

												this.DEBUGG_LOGGER.addMassage(
														"- #{I_RE_001_1_1} - Rechnungsposition wird dem Lager wieder hinzugefuegt:");

											}

											if (this.rechnungstyp == 1 || this.rechnungstyp == 23) {

												String typ = "";

												if (this.rechnungstyp == 1) {
													typ = " Gutschrift ";
													guid_verkauf = "GUTSCHRIFT_" + this.rechnungsnummer + "_"
															+ artikel_position + "_" + artikel_nummer;
												}

												if (this.rechnungstyp == 23) {
													typ = " Rechnungs-STORNO ";
													guid_verkauf = "RECHNUNGS_STORNO_" + this.rechnungsnummer + "_"
															+ artikel_position + "_" + artikel_nummer;
												}

												this.DEBUGG_LOGGER.addMassage("---- durch diese [" + typ
														+ "] wieder in das Lager zurueckgebracht: [" + menge
														+ "] Stueck(e)");
											}

											if (this.rechnungstyp == 0) {
												this.DEBUGG_LOGGER.addMassage("---- durch diese Rechnung abgezogen: ["
														+ menge + "] Stueck(e)");

												guid_verkauf = "RECHNUNG_OHNE_REFERENZ_" + this.rechnungsnummer + "_"
														+ artikel_position + "_" + artikel_nummer;

											}

											this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + artikel_nummer + "]");
											this.DEBUGG_LOGGER.addMassage("---- Menge:   [" + menge + "]");
											this.DEBUGG_LOGGER.addMassage("---- MengeLager: [" + menge_lager + "]");
											this.DEBUGG_LOGGER.addMassage(
													"---- Artikel Lagerbestand: [a=" + _alt_artikel_lagerbestand
															+ ", n=" + _neu_artikel_lagerbestand + "]");

											// - Rechnungspositionen

											String insertPosition = "INSERT INTO static_rechnung_positionen (" //
													+ "position," //
													+ "rechnungsnummer,"//
													+ "artikelnummer,"//
													+ "auftragsnummer," //
													+ "lieferscheinnummer,  "//
													+ "menge," //
													+ "mengelager,"//
													+ "last_operation,"//
													+ "last_operation_datum"//
													+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer
													+ "', '" + artikel_nummer + "','?','?', " + menge + ", "
													+ menge_lager + ",'"
													+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log) + "','"
													+ formatdb.format(new Date()) + "' );";

											this.DB_CONNECTOR.insertToDatabase(insertPosition, "insertPosition");

											// Aktualisiere Bestaende

											String upDateArtikel = "Update static_artikel set lager_anzahl="
													+ _neu_artikel_lagerbestand + ", reserviert_durch_rechnung="
													+ _neu_artikel_durch_rechnung_reserviert + ", agent_operation='"
													+ LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
															this.neu_artikel_log)
													+ "' where interne_artikelnummer='" + artikel_nummer + "' ;";

											this.DB_CONNECTOR.updateOnDatabase(upDateArtikel);

											// - Warenausgangsprotokoll

											try {

												String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
														+ guid_verkauf + "','" + artikel_nummer + "'," + menge + ", '"
														+ this.rechnungsdatum + "',0);";

												this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");

											}

											catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

											}

											this.DEBUGG_LOGGER
													.addMassage("---- ARTIKEL WURDE IM WARENAUSGANG EINGETRAGEN");

											this.DEBUGG_LOGGER.addMassage(
													"---------------------------------------------------------------------------------------------------");

										}

										if ((refAuftrag == true && refLieferschein == false)
												|| (refAuftrag == false && refLieferschein == true)
												|| (refAuftrag == true && refLieferschein == true)) {

											boolean istInReferenz = istPositionInEinerReferenz(this.refAuftrag,
													this.refLieferschein, this.auftragsnummer, this.lieferscheinnummer,
													artikel_nummer, artikel_position);

											boolean istInRechnung = checkRechnungPositionInRechnung(artikel_position,
													this.rechnungsnummer, artikel_nummer);

											if (istInReferenz == false & istInRechnung == false) {

												String gutschrieftStornoLog = "";

												if (this.rechnungstyp == 1 || this.rechnungstyp == 23) {

													menge = (menge * -1);

													gutschrieftStornoLog = "**************************************\n ------- GUTSCHRIFT -------\n**************************************\n ";
												}

												l001 = "#{I_RE_002_1} - Position:\n";

												l002 = "---- Gebucht: [" + this.rechnung_gebucht + "]\n";
												l003 = "---- Rechnungsnummer: [" + this.rechnungsnummer + "]\n";
												l004 = "---- Auftragsnummer: [" + this.auftragsnummer + "]\n";
												l005 = "---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]\n";
												l006 = "---- Rechnungsdatum: [" + this.rechnungsdatum + "]\n "
														+ "---- Artikel: [" + artikel_nummer + "]\n---- Menge=" + menge
														+ "\n---- Mengelager=" + menge_lager + "\n";
												l007 = "---- Rechnungtyp: [" + this.rechnungstyp + "] \n";
												l008 = "---- Datei: [" + this.FILE_CREATION_CODE + "]\n";
												l009 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
														+ this.MANAGER_RUN_CODE + "]";

												this.neu_artikel_log = gutschrieftStornoLog + l001 + l002 + l003 + l004
														+ l005 + l006 + l007 + l008 + l009;

												String insertPosition = "INSERT INTO static_rechnung_positionen (" //
														+ "position," //
														+ "rechnungsnummer,"//
														+ "artikelnummer,"//
														+ "auftragsnummer," //
														+ "lieferscheinnummer,  "//
														+ "menge," //
														+ "mengelager,"//
														+ "last_operation,"//
														+ "last_operation_datum"//
														+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer
														+ "', '" + artikel_nummer + "','?','?', " + menge + ", "
														+ menge_lager + ",'"
														+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log)
														+ "','" + formatdb.format(new Date()) + "' );";

												this.DB_CONNECTOR.insertToDatabase(insertPosition, "insertPosition");

												String sqlGetArtikelAlteWerte = "Select lager_anzahl, reserviert_durch_rechnung, agent_operation from static_artikel  where interne_artikelnummer='"
														+ artikel_nummer + "';";

												this.DB_CONNECTOR.readFromDatabase(sqlGetArtikelAlteWerte,
														"sqlGetArtikelAlteWerte");

												while (this.DB_CONNECTOR.rs.next()) {

													_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
													_alt_artikel_durch_rechnung_reserviert = this.DB_CONNECTOR.rs
															.getInt(2);
													this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

												}

												_neu_artikel_lagerbestand = (_alt_artikel_lagerbestand - (menge));
												_neu_artikel_durch_rechnung_reserviert = _alt_artikel_durch_rechnung_reserviert;

												l002 = "---- Rechnung: [" + this.rechnungsnummer + "]\n";
												l003 = "---- Gebucht:  [" + this.rechnung_gebucht + "]\n";
												l004 = "---- Lagerbestand: [a=" + _alt_artikel_lagerbestand + ",n="
														+ _neu_artikel_lagerbestand + "]\n";

												l005 = "---- Gesamt reserviert durch Rechnungen: [a="
														+ _alt_artikel_durch_rechnung_reserviert + ", n="
														+ _neu_artikel_durch_rechnung_reserviert + "]\n";

												l006 = "---- durch diese Rechnung abgezogen: [" + menge
														+ "] Stueck(e)\n";
												l007 = "---- am: " + format2.format(new Date()) + " \n";

												l008 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
														+ this.MANAGER_RUN_CODE + "] \n";

												l009 = "---- Datei: [" + this.FILE_CREATION_CODE + "]";

												this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007
														+ l008 + l009;

												this.DEBUGG_LOGGER.addMassage("P [" + position_im_rechnung + "]");
												this.DEBUGG_LOGGER.addMassage(
														"- #{I_RE_002_1} - Rechnungsposition wird vom Lager abgezogen:");
												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------------------------------------------");

												this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + artikel_nummer + "]");
												this.DEBUGG_LOGGER.addMassage("---- Menge:   [" + menge + "]");
												this.DEBUGG_LOGGER.addMassage("---- MengeLager: [" + menge_lager + "]");
												this.DEBUGG_LOGGER.addMassage(
														"---- Artikel Lagerbestand: [a=" + _alt_artikel_lagerbestand
																+ ", n=" + _neu_artikel_lagerbestand + "]");
												this.DEBUGG_LOGGER.addMassage("---- durch diese Rechnung abgezogen: ["
														+ menge + "] Stueck(e)");

												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------------------------------------------");

												// - Rechnungspositionen

												String insertPositionS = "INSERT INTO static_rechnung_positionen (" //
														+ "position," //
														+ "rechnungsnummer,"//
														+ "artikelnummer,"//
														+ "auftragsnummer," //
														+ "lieferscheinnummer,  "//
														+ "menge," //
														+ "mengelager,"//
														+ "last_operation,"//
														+ "last_operation_datum"//
														+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer
														+ "', '" + artikel_nummer + "','" + this.auftragsnummer + "','"
														+ this.lieferscheinnummer + "', " + menge + ", " + menge_lager
														+ ",'"
														+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log)
														+ "','" + formatdb.format(new Date()) + "' );";

												this.DB_CONNECTOR.insertToDatabase(insertPositionS, "insertPosition");

												// Aktualisiere Bestaende

												String upDateArtikel = "Update static_artikel set lager_anzahl="
														+ _neu_artikel_lagerbestand + ", reserviert_durch_rechnung="
														+ _neu_artikel_durch_rechnung_reserviert + ", agent_operation='"
														+ LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
																this.neu_artikel_log)
														+ "' where interne_artikelnummer='" + artikel_nummer + "' ;";

												this.DB_CONNECTOR.updateOnDatabase(upDateArtikel);

												// - Warenausgangsprotokoll

												String guid_verkauf = "RECHNUNG_MIT_AUFTRAG_OHNE_LIEFERSCHEIN_"
														+ this.rechnungsnummer + "_" + artikel_position + "_"
														+ artikel_nummer;

												try {

													String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
															+ guid_verkauf + "','" + artikel_nummer + "'," + menge
															+ ", '" + this.rechnungsdatum + "',0);";

													this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");
												}

												catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

												}

												this.DEBUGG_LOGGER
														.addMassage("---- ARTIKEL WURDE IM WARENAUSGANG EINGETRAGEN");

											} else {

												// Artikel war bereits in der
												// Rechnung vorhanden

											}
										}

									}
								}

							}
							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------
							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ " \n");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------\n\n");

							// ----------------------------------------------------------------------------------------------------------

							// Ende von: anzahl_der_rechnungs_positionen > 0
						} else {

							this.DEBUGG_LOGGER.addMassage(
									"#{RE_006} - Rechnung [" + this.rechnungsnummer + "] hat keine Positionen.");
							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------
							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ "");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------\n\n");
						}

					}

					else {

						this.DEBUGG_LOGGER.addMassage(
								"---- #{I_RE_010} - Insert in der Datei aber der Rechnungskopf existiert bereits!");
						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>DUPLIKATE<< VERSCHOBEN]");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						Path path = Paths.get(this.AGENT_WORK_FILE_XML.getAbsolutePath());

						BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

						String file_creation_time = format1
								.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

						String newSystemNameBeforeDeliver = "duplikat_" + file_creation_time + "_" + this.AGENT_NAME
								+ "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME
								+ MANAGER_filepositionsindex + "_" + FILE_CREATION_CODE + ".xml";

						File xml_file_duplikate = new File(
								this.AGENT_DUPLIKAT_PATH + SYSTEM_PATH_DELIMITER + newSystemNameBeforeDeliver);

						this.AGENT_WORK_FILE_XML.renameTo(xml_file_duplikate);

						// Beende Datei OBJEKT
						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

						this.FILE_STATUS_ID = 4;

						beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
								this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
								AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

						this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
								+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + "");

						// ----------------------------------------------------------------------------------------------------------

						// ----------------------------------------------------------------------------------------------------------
						// Beende Agenten OBJEKT
						// ----------------------------------------------------------------------------------------------------------

						this.AGENT_END_PROCESS_TIME = new Date();

						beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

						// Beende Agenten RUN
						// ----------------------------------------------------------------------------------------------------------

						this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITUNG_ABGEBROCHEN_DUPLIKAT;

						beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
								this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
								this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

						// ----------------------------------------------------------------------------------------------------------

						this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
								+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

						this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");
						this.DEBUGG_LOGGER.addMassage(
								"---------------------------------------------------------------------------------------------------\n\n");

						// ----------------------------------------------------------------------------------------------------------

					}

				} // Ende INSERT

				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// --------------------------------------------------------------------------------------------------------------------------------------
				// ==UPDATE------------------------------------------------------------------------------------------------------------------------------

				if (XML_CONTENT_IS_INSERT_TYPE == false) {

					// Diese Rechnung exisitiert nicht in der DB
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

						// Es handelt sich um eine Rechnung ohne Referenz

						String l001 = "";

						if (refAuftrag == false && refLieferschein == false) {

							this.DEBUGG_LOGGER.addMassage(
									"#{IFU_RE_001} Rechnung ohne Referenzen wurde erzeugt (INSERT FROM UPDATE).");
							l001 = "#{IFU_RE_001} Rechnung ohne Referenzen wurde erzeugt (INSERT FROM UPDATE).\n";

						}

						if (refAuftrag == true && refLieferschein == false) {

							this.DEBUGG_LOGGER.addMassage(
									"#{IFU_RE_002} Rechnung mit Auftrag ohne Lieferschein (INSERT FROM UPDATE).");
							l001 = "#{IFU_RE_002} Rechnung mit Auftrag ohne Lieferschein (INSERT FROM UPDATE).\n";

						}

						if (refAuftrag == false && refLieferschein == true) {

							this.DEBUGG_LOGGER.addMassage(
									"#{IFU_RE_003} Rechnung ohne Auftrag mit Lieferschein (INSERT FROM UPDATE).");
							l001 = "#{IFU_RE_003} Rechnung ohne Auftrag mit Lieferschein (INSERT FROM UPDATE).\n";

						}

						if (refAuftrag == true && refLieferschein == true) {

							this.DEBUGG_LOGGER
									.addMassage("#{IFU_RE_004} Rechnung mit Auftrag und mit Lieferschein (INSERT).");
							l001 = "#{IFU_RE_004} Rechnung mit Auftrag und mit Lieferschein (INSERT).\n";

						}

						this.DEBUGG_LOGGER.addMassage("---- Gebucht: [" + this.rechnung_gebucht + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsnummer: [" + this.rechnungsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Auftragsnummer: [" + this.auftragsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsdatum: [" + this.rechnungsdatum + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungtyp: [" + this.rechnungstyp + "]");
						this.DEBUGG_LOGGER.addMassage("---- Datei: [" + this.FILE_CREATION_CODE + "]");

						String l002 = "---- Gebucht: [" + this.rechnung_gebucht + "]\n";
						String l003 = "---- Rechnungsnummer: [" + this.rechnungsnummer + "]\n";
						String l004 = "---- Auftragsnummer: [" + this.auftragsnummer + "]\n";
						String l005 = "---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]\n";
						String l006 = "---- Rechnungsdatum: [" + this.rechnungsdatum + "]\n";
						String l007 = "---- Rechnungtyp: [" + this.rechnungstyp + "] \n";
						String l008 = "---- Datei: [" + this.FILE_CREATION_CODE + "]\n";
						String l009 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: [" + this.MANAGER_RUN_CODE
								+ "]";

						this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007 + l008 + l009;

						String sqlInsertRechnungsKopf = "INSERT INTO static_rechnung (" //
								+ " guid," //
								+ " rechnungsnummer,"//
								+ " rechnungsdatum,"//
								+ " last_agent_id,"//
								+ " insert_work_file_id,"//
								+ " lieferschein_nummer,"//
								+ " auftrags_nummer,"//
								+ " kunden_nummer,"//
								+ " gebucht,"//
								+ " last_manager_run_code,"//
								+ " last_agentrun_code,"//
								+ " last_agent_operation,"//
								+ " last_oparation_datum,"//
								+ " mandant_id, " ///
								+ " rechnungstyp ) VALUES ('" //
								+ this.guid + "','" //
								+ this.rechnungsnummer + "','" //
								+ this.rechnungsdatum + "'," //
								+ this.AGENT_DBID + "," //
								+ this.WORK_FILE_DB_ID + ",'"//
								+ this.lieferscheinnummer + "','" //
								+ this.auftragsnummer + "','" //
								+ this.kundennummer + "'," //
								+ this.rechnung_gebucht + ",'"//
								+ this.MANAGER_RUN_CODE + "','" //
								+ this.AGENT_RUN_CODE + "','" //
								+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log) + "','" //
								+ formatdb.format(new Date()) + "'," //
								+ this.AUSWAHL_MANDANT_DBID + "," //
								+ this.rechnungstyp + " );";

						this.DB_CONNECTOR.insertToDatabase(sqlInsertRechnungsKopf, "sqlInsertRechnungsKopf");

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList Rechnungspositionen = rechnung_kopf_element.getElementsByTagName("Kp");
						this.ANZAHL_XML_OBJEKTE = Rechnungspositionen.getLength();

						this.DEBUGG_LOGGER.addMassage(
								"-----------------------------------------POSITIONEN-----------------------------------------------");

						this.DEBUGG_LOGGER.addMassage("Rechnungs-Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");

						this.DEBUGG_LOGGER.addMassage(
								"---------------------------------------------------------------------------------------------------");

						// Alle Positionen werden eingetragen, da diese
						// Rechnung keine Referenzen hat.

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_im_rechnung = 0; position_im_rechnung < Rechnungspositionen
									.getLength(); position_im_rechnung++) {

								Node einzelne_positions_wurzel = Rechnungspositionen.item(position_im_rechnung);

								if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

									Element rechnungspositionElement = (Element) einzelne_positions_wurzel;

									int _alt_artikel_lagerbestand = 0;
									int _alt_artikel_durch_rechnung_reserviert = 0;

									int _neu_artikel_lagerbestand = 0;
									int _neu_artikel_durch_rechnung_reserviert = 0;

									try {

										BigDecimal menG = new BigDecimal(
												rechnungspositionElement.getElementsByTagName("Position").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										artikel_position = menG.intValue();

									} catch (NullPointerException ecxc) {
										artikel_position = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("Menge").item(0).getTextContent());

										menge = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLager").item(0).getTextContent());

										menge_lager = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_lager = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeFak").item(0).getTextContent());

										menge_fekturiert = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_fekturiert = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLp").item(0).getTextContent());

										menge_liefeschein = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_liefeschein = 0;
									}

									try {

										artikel_nummer = rechnungspositionElement.getElementsByTagName("ArtNr").item(0)
												.getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "")
												.trim();

									} catch (NullPointerException ecxc) {
										artikel_nummer = "?";
									}

									// ---------------------------------------------------------------------
									// Fuege Position ein
									// ---------------------------------------------------------------------

									if (this.artikel_nummer.contains("WP-") == false
											&& this.artikel_nummer.contains("WPK-") == false
											&& this.artikel_nummer != "" && this.artikel_nummer != "?"
											&& this.artikel_nummer.length() >= 5) {

										if (refAuftrag == false && refLieferschein == false) {

											String gutschrieftStornoLog = "";

											if (this.rechnungstyp == 1 || this.rechnungstyp == 23) {

												menge = (menge * -1);

												gutschrieftStornoLog = "**************************************\n ------- GUTSCHRIFT -------\n**************************************\n ";
											}

											l001 = "#{7888} - Position:\n";

											String sqlGetArtikelAlteWerte = "Select lager_anzahl, reserviert_durch_rechnung, agent_operation from static_artikel  where interne_artikelnummer='"
													+ artikel_nummer + "';";

											this.DB_CONNECTOR.readFromDatabase(sqlGetArtikelAlteWerte,
													"sqlGetArtikelAlteWerte");

											while (this.DB_CONNECTOR.rs.next()) {

												_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
												_alt_artikel_durch_rechnung_reserviert = this.DB_CONNECTOR.rs.getInt(2);
												this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

											}

											_neu_artikel_lagerbestand = (_alt_artikel_lagerbestand - (menge));
											_neu_artikel_durch_rechnung_reserviert = _alt_artikel_durch_rechnung_reserviert;

											l002 = "---- Rechnung: [" + this.rechnungsnummer + "]\n";
											l003 = "---- Gebucht:  [" + this.rechnung_gebucht + "]\n";
											l004 = "---- Lagerbestand: [a=" + _alt_artikel_lagerbestand + ",n="
													+ _neu_artikel_lagerbestand + "]\n";

											l005 = "---- Gesamt reserviert durch Rechnungen: [a="
													+ _alt_artikel_durch_rechnung_reserviert + ", n="
													+ _neu_artikel_durch_rechnung_reserviert + "]\n";

											l006 = "---- durch diese Rechnung abgezogen: [" + menge + "] Stueck(e)\n";
											l007 = "---- am: " + format2.format(new Date()) + " \n";

											l008 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
													+ this.MANAGER_RUN_CODE + "] \n";

											l009 = "---- Datei: [" + this.FILE_CREATION_CODE + "]";

											this.neu_artikel_log = gutschrieftStornoLog + l001 + l002 + l003 + l004
													+ l005 + l006 + l007 + l008 + l009;

											this.DEBUGG_LOGGER.addMassage("P [" + position_im_rechnung + "]");
											this.DEBUGG_LOGGER.addMassage(
													"---------------------------------------------------------------------------------------------------");
											this.DEBUGG_LOGGER.addMassage(
													"- #{IFU_RE_001_1_1} - Rechnungsposition wird vom Lager abgezogen:");
											this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + artikel_nummer + "]");
											this.DEBUGG_LOGGER.addMassage("---- Menge:   [" + menge + "]");
											this.DEBUGG_LOGGER.addMassage("---- MengeLager: [" + menge_lager + "]");
											this.DEBUGG_LOGGER.addMassage(
													"---- Artikel Lagerbestand: [a=" + _alt_artikel_lagerbestand
															+ ", n=" + _neu_artikel_lagerbestand + "]");
											this.DEBUGG_LOGGER.addMassage(
													"---- durch diese Rechnung abgezogen: [" + menge + "] Stueck(e)");

											// - Rechnungspositionen

											String insertPosition = "INSERT INTO static_rechnung_positionen (" //
													+ "position," //
													+ "rechnungsnummer,"//
													+ "artikelnummer,"//
													+ "auftragsnummer," //
													+ "lieferscheinnummer,  "//
													+ "menge," //
													+ "mengelager,"//
													+ "last_operation,"//
													+ "last_operation_datum"//
													+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer
													+ "', '" + artikel_nummer + "','" + this.auftragsnummer + "','"
													+ this.lieferscheinnummer + "', " + menge + ", " + menge_lager
													+ ",'" + LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log)
													+ "','" + formatdb.format(new Date()) + "' );";

											this.DB_CONNECTOR.insertToDatabase(insertPosition, "insertPosition");

											// Aktualisiere Bestaende

											String upDateArtikel = "Update static_artikel set lager_anzahl="
													+ _neu_artikel_lagerbestand + ", reserviert_durch_rechnung="
													+ _neu_artikel_durch_rechnung_reserviert + ", agent_operation='"
													+ LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
															this.neu_artikel_log)
													+ "' where interne_artikelnummer='" + artikel_nummer + "' ;";

											this.DB_CONNECTOR.updateOnDatabase(upDateArtikel);

											// - Warenausgangsprotokoll

											String guid_verkauf = "RECHNUNG_OHNE_REFERENZ_" + this.rechnungsnummer + "_"
													+ artikel_position + "_" + artikel_nummer + "_"
													+ position_im_rechnung;

											try {

												String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
														+ guid_verkauf + "','" + artikel_nummer + "'," + menge + ", '"
														+ this.rechnungsdatum + "',0);";

												this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");
											}

											catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

											}

											this.DEBUGG_LOGGER
													.addMassage("---- ARTIKEL WURDE IM WARENAUSGANG EINGETRAGEN");

											this.DEBUGG_LOGGER.addMassage(
													"---------------------------------------------------------------------------------------------------");

										}

										if ((refAuftrag == true && refLieferschein == false)
												|| (refAuftrag == false && refLieferschein == true)
												|| (refAuftrag == true && refLieferschein == true)) {

											boolean istInReferenz = istPositionInEinerReferenz(this.refAuftrag,
													this.refLieferschein, this.auftragsnummer, this.lieferscheinnummer,
													artikel_nummer, artikel_position);

											boolean istInRechnung = checkRechnungPositionInRechnung(artikel_position,
													this.rechnungsnummer, artikel_nummer);

											if (istInReferenz == false && istInRechnung == false) {

												String sqlGetArtikelAlteWerte = "Select lager_anzahl, reserviert_durch_rechnung, agent_operation from static_artikel  where interne_artikelnummer='"
														+ artikel_nummer + "';";

												this.DB_CONNECTOR.readFromDatabase(sqlGetArtikelAlteWerte,
														"sqlGetArtikelAlteWerte");

												while (this.DB_CONNECTOR.rs.next()) {

													_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
													_alt_artikel_durch_rechnung_reserviert = this.DB_CONNECTOR.rs
															.getInt(2);
													this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

												}

												_neu_artikel_lagerbestand = (_alt_artikel_lagerbestand - (menge));
												_neu_artikel_durch_rechnung_reserviert = _alt_artikel_durch_rechnung_reserviert;

												l001 = "#{IFU_RE_002_1} - Position:\n";

												l002 = "---- Rechnung: [" + this.rechnungsnummer + "]\n";
												l003 = "---- Gebucht:  [" + this.rechnung_gebucht + "]\n";
												l004 = "---- Lagerbestand: [a=" + _alt_artikel_lagerbestand + ",n="
														+ _neu_artikel_lagerbestand + "]\n";

												l005 = "---- Gesamt reserviert durch Rechnungen: [a="
														+ _alt_artikel_durch_rechnung_reserviert + ", n="
														+ _neu_artikel_durch_rechnung_reserviert + "]\n";

												l006 = "---- durch diese Rechnung abgezogen: [" + menge
														+ "] Stueck(e)\n";
												l007 = "---- am: " + format2.format(new Date()) + " \n";

												l008 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
														+ this.MANAGER_RUN_CODE + "] \n";

												l009 = "---- Datei: [" + this.FILE_CREATION_CODE + "]";

												this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007
														+ l008 + l009;

												this.DEBUGG_LOGGER.addMassage("P [" + position_im_rechnung + "]");
												this.DEBUGG_LOGGER.addMassage(
														"- #{IFU_RE_002_1} - Rechnungsposition wird vom Lager abgezogen:");
												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------------------------------------------");

												this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + artikel_nummer + "]");
												this.DEBUGG_LOGGER.addMassage("---- Menge:   [" + menge + "]");
												this.DEBUGG_LOGGER.addMassage("---- MengeLager: [" + menge_lager + "]");
												this.DEBUGG_LOGGER.addMassage(
														"---- Artikel Lagerbestand: [a=" + _alt_artikel_lagerbestand
																+ ", n=" + _neu_artikel_lagerbestand + "]");
												this.DEBUGG_LOGGER.addMassage("---- durch diese Rechnung abgezogen: ["
														+ menge + "] Stueck(e)");

												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------------------------------------------");

												// - Rechnungspositionen

												String insertPositionS = "INSERT INTO static_rechnung_positionen (" //
														+ "position," //
														+ "rechnungsnummer,"//
														+ "artikelnummer,"//
														+ "auftragsnummer," //
														+ "lieferscheinnummer,  "//
														+ "menge," //
														+ "mengelager,"//
														+ "last_operation,"//
														+ "last_operation_datum"//
														+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer
														+ "', '" + artikel_nummer + "','" + this.auftragsnummer + "','"
														+ this.lieferscheinnummer + "', " + menge + ", " + menge_lager
														+ ",'"
														+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log)
														+ "','" + formatdb.format(new Date()) + "' );";

												this.DB_CONNECTOR.insertToDatabase(insertPositionS, "insertPosition");

												// Aktualisiere Bestaende

												String upDateArtikel = "Update static_artikel set lager_anzahl="
														+ _neu_artikel_lagerbestand + ", reserviert_durch_rechnung="
														+ _neu_artikel_durch_rechnung_reserviert + ", agent_operation='"
														+ LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
																this.neu_artikel_log)
														+ "' where interne_artikelnummer='" + artikel_nummer + "' ;";

												this.DB_CONNECTOR.updateOnDatabase(upDateArtikel);

												// - Warenausgangsprotokoll

												String guid_verkauf = "RECHNUNG_MIT_AUFTRAG_OHNE_LIEFERSCHEIN_"
														+ this.rechnungsnummer + "_" + artikel_position + "_"
														+ artikel_nummer + "_" + position_im_rechnung;

												try {

													String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
															+ guid_verkauf + "','" + artikel_nummer + "'," + menge
															+ ", '" + this.rechnungsdatum + "',0);";

													this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");

												} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

												}

												this.DEBUGG_LOGGER
														.addMassage("---- ARTIKEL WURDE IM WARENAUSGANG EINGETRAGEN");

											}
										}

									}
								}

							}
							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------
							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ " \n");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------\n\n");

							// ----------------------------------------------------------------------------------------------------------

							// Ende von: anzahl_der_rechnungs_positionen > 0
						} else {

							this.DEBUGG_LOGGER.addMassage(
									"#{IFU_RE_006} - Rechnung [" + this.rechnungsnummer + "] hat keine Positionen.");
							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------
							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ "");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------\n\n");
						}

					}

					// ------------------------------------------------------------------------------------------------------------
					// ------------------------------------------------------------------------------------------------------------
					// ------------------------------------------------------------------------------------------------------------
					// ------------------------------------------------------------------------------------------------------------
					// ------------------------------------------------------------------------------------------------------------
					// ------------------------------------------------------------------------------------------------------------

					// Richtiges UPDATE

					if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

						String l001 = "";

						if (refAuftrag == false && refLieferschein == false) {

							this.DEBUGG_LOGGER
									.addMassage("#{UP_RE_001} Rechnung ohne Referenzen wurde erzeugt (UPDATE).");
							l001 = "#{UP_RE_001} Rechnung ohne Referenzen wurde erzeugt (UPDATE).\n";

						}

						if (refAuftrag == true && refLieferschein == false) {

							this.DEBUGG_LOGGER
									.addMassage("#{UP_RE_002} Rechnung mit Auftrag ohne Lieferschein (UPDATE).");
							l001 = "#{UP_RE_002} Rechnung mit Auftrag ohne Lieferschein (UPDATE).\n";

						}

						if (refAuftrag == false && refLieferschein == true) {

							this.DEBUGG_LOGGER
									.addMassage("#{UP_RE_003} Rechnung ohne Auftrag mit Lieferschein (UPDATE).");
							l001 = "#{UP_RE_003} Rechnung ohne Auftrag mit Lieferschein (UPDATE).\n";

						}

						if (refAuftrag == true && refLieferschein == true) {

							this.DEBUGG_LOGGER
									.addMassage("#{UP_RE_004} Rechnung mit Auftrag und mit Lieferschein (UPDATE).");
							l001 = "#{UP_RE_004} Rechnung mit Auftrag und mit Lieferschein (UPDATE).\n";

						}

						String lastLog = "";
						boolean altGebucht = false;

						String sqlGetOldLogRechnug = "Select last_agent_operation, gebucht from static_rechnung where rechnungsnummer='"
								+ this.rechnungsnummer + "';";

						this.DB_CONNECTOR.readFromDatabase(sqlGetOldLogRechnug, "sqlGetOldLogRechnug");

						while (this.DB_CONNECTOR.rs.next()) {
							lastLog = this.DB_CONNECTOR.rs.getString(1);
							altGebucht = this.DB_CONNECTOR.rs.getBoolean(2);
						}

						this.DEBUGG_LOGGER
								.addMassage("---- Gebucht: [a=" + altGebucht + ", n=" + rechnung_gebucht + "]");
						this.DEBUGG_LOGGER.addMassage("---- Gebucht: [" + this.rechnung_gebucht + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsnummer: [" + this.rechnungsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Auftragsnummer: [" + this.auftragsnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungsdatum: [" + this.rechnungsdatum + "]");
						this.DEBUGG_LOGGER.addMassage("---- Rechnungtyp: [" + this.rechnungstyp + "]");
						this.DEBUGG_LOGGER.addMassage("---- Datei: [" + this.FILE_CREATION_CODE + "]");

						String l002 = "---- Gebucht:  [a=" + altGebucht + ", n=" + rechnung_gebucht + "]\n";
						String l003 = "---- Rechnungsnummer: [" + this.rechnungsnummer + "]\n";
						String l004 = "---- Auftragsnummer: [" + this.auftragsnummer + "]\n";
						String l005 = "---- Lieferscheinnummer: [" + this.lieferscheinnummer + "]\n";
						String l006 = "---- Rechnungsdatum: [" + this.rechnungsdatum + "]\n";
						String l007 = "---- Rechnungtyp: [" + this.rechnungstyp + "] \n";
						String l008 = "---- Datei: [" + this.FILE_CREATION_CODE + "]\n";
						String l009 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: [" + this.MANAGER_RUN_CODE
								+ "]";

						this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007 + l008 + l009;

						// Rechnungskopf wird aktualisiert

						String UpdateRechnungsKopf = "UPDATE static_rechnung set gebucht=" + this.rechnung_gebucht
								+ ",last_agent_operation='"
								+ LOG_CHECKER.generateNewLogForDB(lastLog, this.neu_artikel_log)
								+ "', last_oparation_datum='" + formatdb.format(new Date()) + "',last_agent_id="
								+ this.AGENT_DBID + ", update_work_file_id=" + this.WORK_FILE_DB_ID
								+ ",last_manager_run_code='" + this.MANAGER_RUN_CODE + "', last_agentrun_code='"
								+ this.AGENT_RUN_CODE + "' where rechnungsnummer='" + this.rechnungsnummer + "';";

						this.DB_CONNECTOR.updateOnDatabase(UpdateRechnungsKopf);

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList Rechnungspositionen = rechnung_kopf_element.getElementsByTagName("Kp");
						this.ANZAHL_XML_OBJEKTE = Rechnungspositionen.getLength();

						this.DEBUGG_LOGGER.addMassage(
								"---------------------------------------------------------------------------------------------------");

						this.DEBUGG_LOGGER.addMassage("Rechnungs-Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");
						this.DEBUGG_LOGGER.addMassage(
								"---------------------------------------------------------------------------------------------------");

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_im_rechnung = 0; position_im_rechnung < Rechnungspositionen
									.getLength(); position_im_rechnung++) {

								Node einzelne_positions_wurzel = Rechnungspositionen.item(position_im_rechnung);

								if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

									Element rechnungspositionElement = (Element) einzelne_positions_wurzel;

									try {

										BigDecimal menG = new BigDecimal(
												rechnungspositionElement.getElementsByTagName("Position").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										artikel_position = menG.intValue();

									} catch (NullPointerException ecxc) {
										artikel_position = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("Menge").item(0).getTextContent());

										menge = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLager").item(0).getTextContent());

										menge_lager = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_lager = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeFak").item(0).getTextContent());

										menge_fekturiert = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_fekturiert = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(rechnungspositionElement
												.getElementsByTagName("MengeLp").item(0).getTextContent());

										menge_liefeschein = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_liefeschein = 0;
									}

									try {

										artikel_nummer = rechnungspositionElement.getElementsByTagName("ArtNr").item(0)
												.getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "")
												.trim();

									} catch (NullPointerException ecxc) {
										artikel_nummer = "?";
									}

									// =Fuege Position ein
									// in---------------------------------------------------------------------

									boolean istPositionInEineReferenz = istPositionInEinerReferenz(refAuftrag,
											this.refLieferschein, this.auftragsnummer, this.lieferscheinnummer,
											artikel_nummer, artikel_position);

									boolean istPostitionBereitsInRechnung = checkRechnungPositionInRechnung(
											artikel_position, this.rechnungsnummer, artikel_nummer);

									// Wenn die Position nur in der Rechnung
									// exisitert

									if (istPositionInEineReferenz == false && istPostitionBereitsInRechnung == true) {

										int _alt_rechnungs_position_menge = 0;
										int _alt_rechnungs_position_menge_lager = 0;

										String getRechnungsPostionInfo = "Select menge, mengelager, last_operation from static_rechnung_positionen where rechnungsnummer='"
												+ this.rechnungsnummer + "' and artikelnummer='" + this.artikel_nummer
												+ "';";

										this.DB_CONNECTOR.readFromDatabase(getRechnungsPostionInfo,
												"getRechnungsPostionInfo");

										while (this.DB_CONNECTOR.rs.next()) {

											_alt_rechnungs_position_menge = this.DB_CONNECTOR.rs.getInt(1);
											_alt_rechnungs_position_menge_lager = this.DB_CONNECTOR.rs.getInt(2);
											this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

										}

										/// ---------------------------------

										this.neu_artikel_log = "- #{UP_RE_023} Rechnungsposition UPDATE:\n---- Rechnung: ["
												+ this.rechnungsnummer + "],\n---- Menge: [a="
												+ _alt_rechnungs_position_menge + ", n=" + this.menge
												+ "],\n---- Menge-Lager: [a=" + _alt_rechnungs_position_menge_lager
												+ ", n=" + this.menge_lager + "]\n---- am: "
												+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
												+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
												+ this.FILE_CREATION_CODE + "]";

										String UpdateRechnungsPosition = "Update static_rechnung_positionen set mengelager="
												+ this.menge_lager + ", last_operation='"
												+ this.LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
														this.neu_artikel_log)
												+ "'  where rechnungsnummer='" + this.rechnungsnummer
												+ "' and artikelnummer='" + this.artikel_nummer + "';";

										this.DB_CONNECTOR.updateOnDatabase(UpdateRechnungsPosition);

									} else {

										if (istPositionInEineReferenz == true) {

											this.DEBUGG_LOGGER.addMassage("---- #{UP_RE_023} Artikel [" + artikel_nummer
													+ "] ist in einer Referenz vorhanden.");
										}

									}

									if (istPositionInEineReferenz == false && istPostitionBereitsInRechnung == false
											&& this.artikel_nummer.contains("WP-") == false
											&& this.artikel_nummer.contains("WPK-") == false
											&& this.artikel_nummer != "" && this.artikel_nummer != "?"
											&& this.artikel_nummer.length() >=5) {

										int _alt_artikel_lagerbestand = 0;
										int _alt_artikel_durch_rechnung_reserviert = 0;
										int _neu_artikel_lagerbestand = 0;
										int _neu_artikel_durch_rechnung_reserviert = 0;

										this.DEBUGG_LOGGER.addMassage(
												"#{I_RE_001_1} - Rechnungsposition wird vom Lager abgezogen:");
										l001 = "#{I_RE_001_1} - Position:\n";

										String sqlGetArtikelAlteWerte = "Select lager_anzahl, reserviert_durch_rechnung, agent_operation from static_artikel  where interne_artikelnummer='"
												+ artikel_nummer + "';";

										this.DB_CONNECTOR.readFromDatabase(sqlGetArtikelAlteWerte,
												"sqlGetArtikelAlteWerte");

										while (this.DB_CONNECTOR.rs.next()) {

											_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
											_alt_artikel_durch_rechnung_reserviert = this.DB_CONNECTOR.rs.getInt(2);
											this.alt_artikel_log = this.DB_CONNECTOR.rs.getString(3);

										}

										_neu_artikel_lagerbestand = _alt_artikel_lagerbestand - menge;
										_neu_artikel_durch_rechnung_reserviert = _alt_artikel_durch_rechnung_reserviert;

										l002 = "---- Rechnung: [" + this.rechnungsnummer + "]\n";
										l003 = "---- Gebucht:  [" + this.rechnung_gebucht + "]\n";
										l004 = "---- Lagerbestand: [a=" + _alt_artikel_lagerbestand + ",n="
												+ _neu_artikel_lagerbestand + "]\n";

										l005 = "---- Gesamt reserviert durch Rechnungen: [a="
												+ _alt_artikel_durch_rechnung_reserviert + ", n="
												+ _neu_artikel_durch_rechnung_reserviert + "]\n";

										l006 = "---- durch diese Rechnung abgezogen: [" + menge + "] Stueck(e)\n";
										l007 = "---- am: " + format2.format(new Date()) + " \n";

										l008 = "---- A-RUN: [" + this.AGENT_RUN_CODE + "] - M-RUN: ["
												+ this.MANAGER_RUN_CODE + "] \n";

										l009 = "---- Datei: [" + this.FILE_CREATION_CODE + "]";

										this.neu_artikel_log = l001 + l002 + l003 + l004 + l005 + l006 + l007 + l008
												+ l009;

										this.DEBUGG_LOGGER.addMassage("P [" + position_im_rechnung + "]");
										this.DEBUGG_LOGGER.addMassage(
												"---------------------------------------------------------------------------------------------------");
										this.DEBUGG_LOGGER.addMassage(
												"- #{I_RE_001_1_1} - Rechnungsposition wird vom Lager abgezogen:");
										this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + artikel_nummer + "]");
										this.DEBUGG_LOGGER.addMassage("---- Menge:   [" + menge + "]");
										this.DEBUGG_LOGGER.addMassage("---- MengeLager: [" + menge_lager + "]");
										this.DEBUGG_LOGGER.addMassage("---- Artikel Lagerbestand: [a="
												+ _alt_artikel_lagerbestand + ", n=" + _neu_artikel_lagerbestand + "]");
										this.DEBUGG_LOGGER.addMassage(
												"---- durch diese Rechnung abgezogen: [" + menge + "] Stueck(e)");

										// - Rechnungspositionen

										String insertPosition = "INSERT INTO static_rechnung_positionen (" //
												+ "position," //
												+ "rechnungsnummer,"//
												+ "artikelnummer,"//
												+ "auftragsnummer," //
												+ "lieferscheinnummer,  "//
												+ "menge," //
												+ "mengelager,"//
												+ "last_operation,"//
												+ "last_operation_datum"//
												+ ") VALUES (" + artikel_position + ",'" + this.rechnungsnummer + "', '"
												+ artikel_nummer + "','?','?', " + menge + ", " + menge_lager + ",'"
												+ LOG_CHECKER.generateNewLogForDB("", this.neu_artikel_log) + "','"
												+ formatdb.format(new Date()) + "' );";

										this.DB_CONNECTOR.insertToDatabase(insertPosition, "insertPosition");

										// Aktualisiere Bestaende

										String upDateArtikel = "Update static_artikel set lager_anzahl="
												+ _neu_artikel_lagerbestand + ", reserviert_durch_rechnung="
												+ _neu_artikel_durch_rechnung_reserviert + ", agent_operation='"
												+ LOG_CHECKER.generateNewLogForDB(this.alt_artikel_log,
														this.neu_artikel_log)
												+ "' where interne_artikelnummer='" + artikel_nummer + "' ;";

										this.DB_CONNECTOR.updateOnDatabase(upDateArtikel);

										// - Warenausgangsprotokoll

										String guid_verkauf = "RECHNUNG_OHNE_REFERENZ_" + this.rechnungsnummer + "_"
												+ artikel_position + "_" + artikel_nummer + "_" + position_im_rechnung;

										try {
											String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
													+ guid_verkauf + "','" + artikel_nummer + "'," + menge + ", '"
													+ this.rechnungsdatum + "',0);";

											this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");
										} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

										}

										this.DEBUGG_LOGGER.addMassage("---- ARTIKEL WURDE IM WARENAUSGANG EINGETRAGEN");

										this.DEBUGG_LOGGER.addMassage(
												"---------------------------------------------------------------------------------------------------");

									}

								}

							}

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------
							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ "");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------\n\n");

							// ----------------------------------------------------------------------------------------------------------

							// Ende von: anzahl_der_rechnungs_positionen > 0
						} else {

							this.DEBUGG_LOGGER.addMassage(
									"#{UP_RE_030} - Rechnung [" + this.rechnungsnummer + "] hat keine Positionen.");
							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------------------------------");
							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

							// Verschiebe Datei
							// ----------------------------------------------------------------------------------------------------------

							AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

							// ----------------------------------------------------------------------------------------------------------

							SINGEL_FILE_PROCESS_END_TIME = new Date();

							this.FILE_STATUS_ID = 3;

							beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
									this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
									this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

							this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
									+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME)
									+ "");

							// ----------------------------------------------------------------------------------------------------------
							// Beende Agenten OBJEKT
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_END_PROCESS_TIME = new Date();

							beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
									this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
									this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");
							this.DEBUGG_LOGGER.addMassage("---------------------------------------------------\n\n");

						}

						// Header True
					}

					// isInsertType == false (ende)
				}

				// Ende File Existenz=false
			}

			if (this.XML_FILE_WAS_EVER_PROCESSED == true) {

				this.DEBUGG_LOGGER.addMassage("#{UP_RE_031}");

				String file_duplikat_label = "";

				String findeDuplikaSQL = "SELECT concat(file_creation_code,'_',id)  FROM  work_agent_files where filehash='"
						+ this.MD5_FILE_HASHCODE + "';";

				this.DB_CONNECTOR.readFromDatabase(findeDuplikaSQL, "findeDuplikaSQL");

				while (this.DB_CONNECTOR.rs.next()) {

					file_duplikat_label = this.DB_CONNECTOR.rs.getString(1);

				}

				// Verschiebe Datei
				// ----------------------------------------------------------------------------------------------------------

				Path path = Paths.get(AGENT_DELIVER_XML_FILE.getAbsolutePath());

				BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

				String file_creation_time = format1.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				String newSystemNameBeforeDeliver = "xfwep_duplikat_" + file_creation_time + "_" + this.AGENT_NAME + "_"
						+ this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + MANAGER_filepositionsindex
						+ "_OFILE_" + file_duplikat_label + ".xml";

				File xml_file_duplikate = new File(
						this.AGENT_DUPLIKAT_PATH + SYSTEM_PATH_DELIMITER + newSystemNameBeforeDeliver);

				AGENT_DELIVER_XML_FILE.renameTo(xml_file_duplikate);

				// Beende Datei OBJEKT
				// ----------------------------------------------------------------------------------------------------------

				SINGEL_FILE_PROCESS_END_TIME = new Date();

				this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>xfwep_DUPLIKATE<< VERSCHOBEN]");

				this.FILE_STATUS_ID = 4;

				beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
						this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
						AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

				this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
						+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + "");

				// ----------------------------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------------------------
				// Beende Agenten OBJEKT
				// ----------------------------------------------------------------------------------------------------------

				this.AGENT_END_PROCESS_TIME = new Date();

				beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

				// Beende Agenten RUN
				// ----------------------------------------------------------------------------------------------------------

				this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITUNG_ABGEBROCHEN_DUPLIKAT;

				beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
						this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
						this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

				// ----------------------------------------------------------------------------------------------------------

				this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE
						+ ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

				this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
						+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

				// ----------------------------------------------------------------------------------------------------------
			}

			// Ende Aktivity=true
		}

		else

		{

			this.DEBUGG_LOGGER.addMassage("#{UP_RE_032}");

			// Beende Datei OBJEKT
			// ----------------------------------------------------------------------------------------------------------

			this.AGENT_END_PROCESS_TIME = new Date();

			beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

			// Beende Agenten RUN
			// ----------------------------------------------------------------------------------------------------------

			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_DEAKTIV;

			beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
					this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
					this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

			// -----------------

			this.DEBUGG_LOGGER.addMassage("[ AGENT IST DEAKTIVIERT ]");
		}

	}

	public boolean existiert_diese_rechnung(String _rechnung) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_rechnung WHERE rechnungsnummer='" + _rechnung
				+ "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;
	}

	public boolean checkRechnungPositionInRechnung(int _position, String _rechnungsnummer, String _artikelnummer)
			throws SQLException {

		String sqlCheckExsistenz = "SELECT EXISTS (SELECT * FROM static_rechnung_positionen where rechnungsnummer='"
				+ _rechnungsnummer + "' and artikelnummer='" + _artikelnummer + "');";

		boolean temp = true;

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;

	}

	public boolean istPositionInEinerReferenz(boolean refAuftrag, boolean refLieferschein, String auftragsnummer,
			String lieferscheinnummer, String artikel_nummer, int position) throws SQLException {

		boolean temp = true;
		boolean tempAuftrag = true;

		boolean tempLieferschein = true;

		if (refAuftrag == false & refLieferschein == false) {
			temp = false;
		}

		if (refAuftrag == true)

		{

			String sqlCheckExsistenz1 = "SELECT EXISTS(SELECT * FROM static_auftraege_positionen where auftrags_nummer='"
					+ auftragsnummer + "' and artikel_nummer='" + artikel_nummer + "');";

			this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz1, "sqlCheckExsistenz");

			while (this.DB_CONNECTOR.rs.next()) {

				tempAuftrag = this.DB_CONNECTOR.rs.getBoolean(1);
			}

		} else {

			tempAuftrag = false;

		}

		if (refLieferschein == true) {

			String sqlCheckExsistenz2 = "SELECT EXISTS(SELECT * FROM static_lieferschein_positionen where lieferschein_nummer='"
					+ lieferscheinnummer + "' and artikel_nummer='" + artikel_nummer + "');";

			this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz2, "sqlCheckExsistenz");

			while (this.DB_CONNECTOR.rs.next()) {

				tempLieferschein = this.DB_CONNECTOR.rs.getBoolean(1);
			}
		} else {

			tempLieferschein = false;
		}

		if (tempLieferschein == true | tempAuftrag == true) {
			temp = true;

		}

		if (tempLieferschein == false && tempAuftrag == false) {
			temp = false;
		}

		return temp;

	}

}
