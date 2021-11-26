package UPDATE_MANAGER;

import java.io.File;
import java.io.FileInputStream;
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

public class event_agent_wareneingang extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 7848115657717666099L;

	boolean istBestellungVorhandenInDerDatenbank = false;

	public event_agent_wareneingang(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter, String _duplikat_path, String _stackpath, String _failPath,
			DatenBankVerbindung _db) throws SQLException, ClassNotFoundException {

		super();

		this.DB_CONNECTOR = _db;
		this.AGENT_DBID = 995;
		this.FILE_CONTENT_ID = 0;
		this.MANAGER_RUN_CODE = _manager_run_creation_code;
		this.RUN_MANAGER_ID = _managerid;
		this.IS_LINUX = _isLinux;
		this.OPERATING_SYSTEM = _opsystem;
		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
		this.AGENT_DUPLIKAT_PATH = _duplikat_path;
		this.AGENT_FILE_STACK_PATH = _stackpath;
		this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
		this.initialisiere_agenten_000(this.DB_CONNECTOR, this.AGENT_DBID);
		this.initialisiere_agenten_pfade(this.DB_CONNECTOR);

		this.ANZAHL_XML_OBJEKTE = 0;
		this.FAIL_PATH = _failPath;

	}

	public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
			String mandant_kuerzel)
			throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

		String filetype_operation_content = "?";
		String wareneingangsnummer = "";
		String datum_wareneingang = "";
		String guid = "";
		String bestellreferenz = "";
		String eingangsRechnung = "";

		this.XML_FILE_WAS_EVER_PROCESSED = true;
		this.FILE_CREATION_CODE = _fileid;
		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "]");

			this.DEBUGG_LOGGER.addMassage("------------------------------------------------------------------------");

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

				SINGEL_FILE_PROCESS_START_TIME = new Date();

				put_deliver_file_in_db(AGENT_DBID, this.AGENT_DELIVER_XML_FILE.getName(),
						formatdb.format(SINGEL_FILE_PROCESS_START_TIME), this.AGENT_RUN_CODE, file_size,
						FILE_CREATION_TIME_FOR_DB, FILE_CREATION_CODE, AGENT_DBID, MD5_FILE_HASHCODE, this.DB_CONNECTOR,
						this.AUSWAHL_MANDANT_DBID);

				this.DEBUGG_LOGGER.addMassage("Datei: " + this.AGENT_DELIVER_XML_FILE.getName()
						+ " wurde in die Tabelle: work_agent_files hinzugefuegt \n");

				// Finde id des Workfiles

				this.WORK_FILE_DB_ID = find_work_file_dbid(FILE_CREATION_CODE, this.DB_CONNECTOR);

				// -----------------------------------

				this.AGENT_WORK_XML_FILE_NAME = this.MANAGER_RUN_CODE + "_" + (MANAGER_filepositionsindex + 1) + "_"
						+ this.AGENT_NAME + "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME
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

				NodeList wareneingang_kopf_node = doc.getElementsByTagName("WehList");

				Node node_kopf_wurzel = wareneingang_kopf_node.item(0);

				Element node_kopf_wurzel_element = (Element) node_kopf_wurzel;

				Element wareneingangs_kopf_element = (Element) (doc.getElementsByTagName("Weh").item(0));

				//// Check Datei Typ
				// ------------------------------------------

				try {
					filetype_operation_content = new String(node_kopf_wurzel_element.getAttribute("Operation"));

				} catch (NullPointerException ecxc) {
					filetype_operation_content = "?";
				}

				if ("Insert".equals(filetype_operation_content) || "Insert" == filetype_operation_content)

				{

					this.XML_CONTENT_IS_INSERT_TYPE = true;

				} else {

					this.XML_CONTENT_IS_INSERT_TYPE = false;

				}

				// {initialisiere Kopf des Wareneingang}
				// -----------------------------------------------------------------------

				wareneingangsnummer = "";
				datum_wareneingang = "";
				guid = "";
				bestellreferenz = "";
				eingangsRechnung = "";

				try {

					eingangsRechnung = wareneingangs_kopf_element.getElementsByTagName("RefEh").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

				} catch (NullPointerException ecxc) {
					eingangsRechnung = "?";
				}

				try {

					wareneingangsnummer = wareneingangs_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

				} catch (NullPointerException ecxc) {
					wareneingangsnummer = "?";
				}

				try {

					guid = wareneingangs_kopf_element.getElementsByTagName("GUID").item(0).getTextContent().trim()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					guid = "?";
				}

				try {

					String tempDatumWareneingangAusXML = "";
					Date datum_wareneingang_temp = null;

					tempDatumWareneingangAusXML = wareneingangs_kopf_element.getElementsByTagName("Date").item(0)
							.getTextContent().trim();

					datum_wareneingang_temp = XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

					datum_wareneingang = formatdb.format(datum_wareneingang_temp);

				} catch (NullPointerException ecxc) {
					datum_wareneingang = "9999-09-09 00:00:00";
				} catch (ParseException e1) {

					datum_wareneingang = "9999-09-09 00:00:00";
					e1.printStackTrace();
				}

				boolean bestellung_vorhanden = false;

				boolean eingangsrechnung_vorhanden = false;

				NodeList refEingangsRechnung = wareneingangs_kopf_element.getElementsByTagName("RefEh");

				NodeList refBestellNummer = wareneingangs_kopf_element.getElementsByTagName("RefBh");

				if (refBestellNummer.getLength() > 0) {

					bestellung_vorhanden = true;

					try {

						bestellreferenz = wareneingangs_kopf_element.getElementsByTagName("RefBh").item(0)
								.getTextContent().trim().replaceAll("[^A-Za-z0-9+-]", "").trim();

						if (bestellreferenz.trim() == "") {

							bestellung_vorhanden = false;
							bestellreferenz = "?";
						}

					} catch (NullPointerException ecxc) {
						bestellreferenz = "?";
						bestellung_vorhanden = false;
					}

				} else {

					bestellung_vorhanden = false;
					bestellreferenz = "?";
				}

				if (refEingangsRechnung.getLength() > 0) {

					if (eingangsRechnung.length() == 0) {
						eingangsrechnung_vorhanden = false;
					}

					if (eingangsRechnung.length() > 0) {
						eingangsrechnung_vorhanden = true;
					}

				} else {
					eingangsrechnung_vorhanden = false;
				}

				if (eingangsrechnung_vorhanden == true) {

					String backToDeliver = getSystem_processed_file_name(MANAGER_filepositionsindex);

					File backToDeliver_xml_file = new File(
							this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER + backToDeliver);

					AGENT_WORK_FILE_XML.renameTo(backToDeliver_xml_file);

					String removeFileFromDB = "DELETE from work_agent_files where file_creation_code='"
							+ this.FILE_CREATION_CODE + "';";

					this.DB_CONNECTOR.updateOnDatabase(removeFileFromDB);

					this.DEBUGG_LOGGER.addMassage("WARENEINGANGS RECHNUNG WIRD NICHT BEACHTET");

					this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
							+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

					this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
							+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

					this.DEBUGG_LOGGER.addMassage(
							"---------------------------------------------------------------------------------------");

				} else {

					if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

						if (bestellung_vorhanden == false) {

							this.FILE_CONTENT_ID = FileTypes.WARENEINGANG_INSERT_EVENT_OHNE_BESTELLUNG_FILE;

							String newLog = "#{INSERT_WEING_OB_0909} INSERT WARENEINGANG NEU - OHNE BESTELLUNG\n---- am: "
									+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
									+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]";

							String insertWarenEingangOhneBestellung = "INSERT INTO static_wareneingang ( " //
									+ " wareneingang_guid, " //
									+ " wareneingans_nummer, " //
									+ " datum_des_wareneingangs, " //
									+ " last_agent_operation, " //
									+ " last_oparation_datum, " //
									+ " last_agent_id, " //
									+ " insert_work_file_id, " //
									+ " last_agentruncode, " //
									+ " last_managerruncode," //
									+ " ref_bestellung, mandant_id) VALUES ('" + guid + "', '" + wareneingangsnummer
									+ "', '" + datum_wareneingang + "','" + newLog + "', '"
									+ formatdb.format(new Date()) + "', " + this.AGENT_DBID + "," + this.WORK_FILE_DB_ID
									+ ",'" + this.AGENT_RUN_CODE + "','" + this.MANAGER_RUN_CODE + "','"
									+ bestellreferenz + "', " + this.AUSWAHL_MANDANT_DBID + ");";

							try {

								this.DB_CONNECTOR.insertToDatabase(insertWarenEingangOhneBestellung,
										"insertWarenEingangOhneBestellung");

							} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException er) {

							}

							NodeList wareneingangs_positionen = wareneingangs_kopf_element.getElementsByTagName("Wep");
							this.ANZAHL_XML_OBJEKTE = wareneingangs_positionen.getLength();

							this.DEBUGG_LOGGER
									.addMassage("Wareneingangs Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "\n");

							String artikel_nummer = "";
							int menge = 0;
							int position = 0;

							if (this.ANZAHL_XML_OBJEKTE > 0) {

								for (int position_im_wareneing = 0; position_im_wareneing < wareneingangs_positionen
										.getLength(); position_im_wareneing++) {

									Node einzelne_positions_wurzel = wareneingangs_positionen
											.item(position_im_wareneing);

									if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

										try {

											Element wareneingangpositionElement = (Element) einzelne_positions_wurzel;

											artikel_nummer = "";
											menge = 0;
											position = 0;

											try {

												artikel_nummer = wareneingangpositionElement
														.getElementsByTagName("ArtNr").item(0).getTextContent().trim()
														.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

											} catch (NullPointerException ecxc) {
												artikel_nummer = "?";
											}

											try {

												BigDecimal menG = new BigDecimal(wareneingangpositionElement
														.getElementsByTagName("Menge").item(0).getTextContent());

												menge = menG.intValue();

											} catch (NullPointerException ecxc) {
												menge = 0;
											}

											try {

												BigDecimal menG = new BigDecimal(wareneingangpositionElement
														.getElementsByTagName("Rec").item(0).getTextContent()
														.replaceAll("[^A-Za-z0-9+-]", "").trim());

												position = menG.intValue();

											} catch (NullPointerException ecxc) {
												position = 0;
											}

											String logneu = "#{INSERT_WEING_OB_0909P} INSERT WARENEINGANGSPOSITION OHNE BESTELLUNG\n---- Artikel: ["
													+ artikel_nummer + "], Menge: [" + menge + "]\n----  am: "
													+ format2.format(new Date()) + "\n---- von A-RUN: ["
													+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
													+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]";

											String insertWarenEingangsPosition = "INSERT INTO static_wareineingang_positionen ("
													+ " position, wareneingangsnummer," + " artikelnummer,  menge,"
													+ " last_agent_operantion,"
													+ " last_operation_datum, eingangsdatum ) VALUES (" + position
													+ ", '" + wareneingangsnummer + "', '" + artikel_nummer + "', "
													+ menge + ", '" + logneu + "', '" + formatdb.format(new Date())
													+ "','" + datum_wareneingang + "');";

											this.DB_CONNECTOR.insertToDatabase(insertWarenEingangsPosition,
													"insertWarenEingangsPosition");

											this.DEBUGG_LOGGER.addMassage("Wareneingang Position wurde am [ "
													+ datum_wareneingang + " ] erzeugt: [ Intere Wareneingangsnummer = "
													+ wareneingangsnummer + " ] mit dem Artikel: [ " + artikel_nummer
													+ " ] und Menge = " + menge + "");

											// Artikelbestand wird aktualisiert

											String logArtikeldbold = "";
											String logdbneuArt = "";
											int bereitsImLagerStaticArtikel = 0;

											String SQLbereitsImLagerAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
													+ artikel_nummer + "';";

											this.DB_CONNECTOR.readFromDatabase(SQLbereitsImLagerAusStaticArtikel,
													"SQLbereitsImLagerAusStaticArtikel");

											while (this.DB_CONNECTOR.rs.next()) {

												bereitsImLagerStaticArtikel = this.DB_CONNECTOR.rs.getInt(1);
												logArtikeldbold = this.DB_CONNECTOR.rs.getString(2);
											}

											int neuerLagerBestandAnzahl = bereitsImLagerStaticArtikel + menge;

											logdbneuArt = logArtikeldbold
													+ "#{INSERT_WEING_OB_0909P} - WARENEINGANG NEU, NUMMER: [ "
													+ wareneingangsnummer + " ], Lagerbestand: [ a="
													+ bereitsImLagerStaticArtikel + ", n= " + neuerLagerBestandAnzahl
													+ " ], Artikel: [" + artikel_nummer + "],[ Eingang=" + menge
													+ " ] am: " + format2.format(new Date()) + " von A-RUN: ["
													+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
													+ "], Datei: [" + this.FILE_CREATION_CODE + "]";

											String SQLneuerLagerBestand = "UPDATE static_artikel set lager_anzahl="
													+ neuerLagerBestandAnzahl + ", last_update='"
													+ formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
													+ ", agent_operation='" + logdbneuArt
													+ "' where interne_artikelnummer='" + artikel_nummer + "';";

											this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

											this.DEBUGG_LOGGER.addMassage(
													"Der Lagerbestand wurde in static_artikel aktualisiert:");

											this.DEBUGG_LOGGER.addMassage(
													"- Lagerbestand Menge (alt): " + bereitsImLagerStaticArtikel);
											this.DEBUGG_LOGGER.addMassage(
													"- Lagerbestand Menge (neu): " + neuerLagerBestandAnzahl);
											this.DEBUGG_LOGGER.addMassage("- Wareneingangsmenge: " + menge + "");
											this.DEBUGG_LOGGER.addMassage(
													"-----------------------------------------------------------------------------------");
											// ----------------------------------------------------------------------------------------------------------

										} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException er) {

											this.DEBUGG_LOGGER.addMassage(
													"-----------------------------------------------------------------------------------");

											this.DEBUGG_LOGGER.addMassage("Wareneingang[" + wareneingangsnummer
													+ "] - Position [" + position + "] - Artikel [" + artikel_nummer
													+ "] - Menge[" + menge + "] - existiert bereits");

											this.DEBUGG_LOGGER.addMassage(
													"-----------------------------------------------------------------------------------");

										}

									}

								} // Schleifen Ende

								// Verschiebe Datei
								// ----------------------------------------------------------------------------------------------------------

								AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
										MANAGER_filepositionsindex);

								File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
										+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

								AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

								// ----------------------------------------------------------------------------------------------------------

								SINGEL_FILE_PROCESS_END_TIME = new Date();

								this.FILE_STATUS_ID = 3;

								beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
										this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
										this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
										this.DB_CONNECTOR);

								this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

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
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
										+ "");
								this.DEBUGG_LOGGER.addMassage(
										"-----------------------------------------------------------------------------------");

							} else {

								this.DEBUGG_LOGGER
										.addMassage("Wareneingang [" + wareneingangsnummer + "] hat keine Positionen.");

								this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

								// Verschiebe Datei
								// ----------------------------------------------------------------------------------------------------------

								AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
										MANAGER_filepositionsindex);

								File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
										+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

								AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

								// ----------------------------------------------------------------------------------------------------------

								SINGEL_FILE_PROCESS_END_TIME = new Date();

								this.FILE_STATUS_ID = 3;

								beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
										this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
										this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
										this.DB_CONNECTOR);

								this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

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
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
										+ "\n");

								// ----------------------------------------------------------------------------------------------------------

							}

						}

						// -------------------------------------------
						if (bestellung_vorhanden == true) {

							istBestellungVorhandenInDerDatenbank = existiert_diese_bestellung_in_der_db(
									bestellreferenz);

							if (istBestellungVorhandenInDerDatenbank == true) {

								this.FILE_CONTENT_ID = FileTypes.WARENEINGANG_INSERT_EVENT_MIT_BESTELLUNG_FILE;

								this.DEBUGG_LOGGER
										.addMassage("Protokolliert einen Wareneingang anhand eine Bestellung \n");
								this.DEBUGG_LOGGER.addMassage(
										"Datei wird nicht verarbeitet, da dies von dem Bestell-Event-Update erledigt wird. \n");

								this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN] \n");

								// Verschiebe Datei
								// ----------------------------------------------------------------------------------------------------------

								AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
										MANAGER_filepositionsindex);

								File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
										+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

								AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

								// ----------------------------------------------------------------------------------------------------------

								SINGEL_FILE_PROCESS_END_TIME = new Date();

								this.FILE_STATUS_ID = 3;

								this.AGENT_FILE_STACK_PATH = this.AGENT_NAME + "_" + this.AGENT_FILE_NAME_PATTERN
										+ this.AUSWAHL_MANDANT_NAME + FILE_CREATION_TIME + "_stack_"
										+ FILE_CREATION_CODE + ".xml";

								WareneingangToBestellungXMLGenerator generiereBestellung = new WareneingangToBestellungXMLGenerator(
										this.IS_LINUX, this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER,
										this.AGENT_FILE_DELIVER_PATH, this.AGENT_FILE_STACK_PATH,
										this.AGENT_FILE_STACK_PATH, this.WORK_FILE_DB_ID, "Update", this.DB_CONNECTOR);

								generiereBestellung.start(processed_xml_file, bestellreferenz, FILE_CREATION_CODE);

								event_agent_bestellungen bearbeite_bestellung = new event_agent_bestellungen(
										this.RUN_MANAGER_ID, this.MANAGER_RUN_CODE, this.IS_LINUX,
										this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER, this.AGENT_DUPLIKAT_PATH,
										this.DB_CONNECTOR);

								bearbeite_bestellung.start(generiereBestellung.getGeneratedBestellungsDatei(),
										MANAGER_filepositionsindex, this.FILE_CREATION_CODE + "_GEN",
										this.AUSWAHL_MANDANT_DBID, this.AUSWAHL_MANDANT_NAME);

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

								beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
										this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
										this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
										this.DB_CONNECTOR);

								// ----------------------------------------------------------------------------------------------------------

								this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
										+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

								this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

								this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

								this.DEBUGG_LOGGER.addMassage(
										"---------------------------------------------------------------------------------------");
								// ----------------------------------------------------------------------------------------------------------

							} else {

								String backToDeliver = getSystem_BackToDeliverFILENAME(MANAGER_filepositionsindex);

								File backToDeliver_xml_file = new File(
										this.AGENT_FILE_DELIVER_PATH + SYSTEM_PATH_DELIMITER + backToDeliver);

								AGENT_WORK_FILE_XML.renameTo(backToDeliver_xml_file);

								String removeFileFromDB = "DELETE from work_agent_files where file_creation_code='"
										+ this.FILE_CREATION_CODE + "';";

								this.DB_CONNECTOR.updateOnDatabase(removeFileFromDB);

								this.DEBUGG_LOGGER.addMassage(
										"WARENEINGANG MIT EINER BESTELLUNG - ABER BESTELLUNG NICHT VORHANDEN");

								this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
										+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

								this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

								this.DEBUGG_LOGGER.addMassage(
										"---------------------------------------------------------------------------------------");
								// ----------------------------------------------------------------------------------------------------------
							}

						}
					}

					// Ende Insert

					if (this.XML_CONTENT_IS_INSERT_TYPE == false) {

						if (bestellung_vorhanden == false) {

							this.FILE_CONTENT_ID = FileTypes.WARENEINGANG_INSERT_EVENT_OHNE_BESTELLUNG_FILE;

							boolean exisitiert_der_header_des_we = exisitiert_der_header_des_we(wareneingangsnummer);

							if (exisitiert_der_header_des_we == true) {

								String newLog = "#{UPDATE_WEING_OB_8517} UPDATE WARENEINGANG [" + wareneingangsnummer
										+ "]- OHNE BESTELLUNG am: " + format2.format(new Date()) + " von A-RUN: ["
										+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]";

								this.DEBUGG_LOGGER.addMassage(newLog);

								String old_log_db = "";

								String getOldLogDBSQL = "SELECT last_agent_operation FROM static_wareneingang where wareneingans_nummer='"
										+ wareneingangsnummer + "';";

								this.DB_CONNECTOR.readFromDatabase(getOldLogDBSQL, "getOldLogDBSQL");

								while (this.DB_CONNECTOR.rs.next()) {

									old_log_db = this.DB_CONNECTOR.rs.getString(1);

								}

								String updateWareneingangsKopf = "UPDATE static_wareneingang set last_agent_operation='"
										+ LOG_CHECKER.generateNewLogForDB(old_log_db, newLog)
										+ "', last_oparation_datum='" + formatdb.format(new Date())
										+ "' where wareneingans_nummer='" + wareneingangsnummer + "';";

								this.DB_CONNECTOR.updateOnDatabase(updateWareneingangsKopf);

								// Verarbeite
								// Positionen---------------------------------------------------------------------

								NodeList wareneingangs_positionen = wareneingangs_kopf_element
										.getElementsByTagName("Wep");
								this.ANZAHL_XML_OBJEKTE = wareneingangs_positionen.getLength();

								this.DEBUGG_LOGGER.addMassage(
										"Wareneingangs Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "\n");

								String artikel_nummer = "";
								int menge = 0;
								int position = 0;

								if (this.ANZAHL_XML_OBJEKTE > 0) {

									for (int position_im_wareneing = 0; position_im_wareneing < this.ANZAHL_XML_OBJEKTE; position_im_wareneing++) {

										Node einzelne_positions_wurzel = wareneingangs_positionen
												.item(position_im_wareneing);

										if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

											Element wareneingangpositionElement = (Element) einzelne_positions_wurzel;

											artikel_nummer = "";
											menge = 0;
											position = 0;

											try {

												artikel_nummer = wareneingangpositionElement
														.getElementsByTagName("ArtNr").item(0).getTextContent().trim()
														.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

											} catch (NullPointerException ecxc) {
												artikel_nummer = "?";
											}

											try {

												BigDecimal menG = new BigDecimal(wareneingangpositionElement
														.getElementsByTagName("Menge").item(0).getTextContent());

												menge = menG.intValue();

											} catch (NullPointerException ecxc) {
												menge = 0;
											}

											try {

												BigDecimal menG = new BigDecimal(wareneingangpositionElement
														.getElementsByTagName("Rec").item(0).getTextContent()
														.replaceAll("[^A-Za-z0-9+-]", "").trim());

												position = menG.intValue();

											} catch (NullPointerException ecxc) {
												position = 0;
											}

											boolean exisitiert_dieser_artikel_im_wareneingang = exisitiert_der_artikel_im_wareneingang(
													position, artikel_nummer, wareneingangsnummer);

											String logneu = "";

											if (exisitiert_dieser_artikel_im_wareneingang == true) {

												logneu = "P [" + position + "] - Artikel[" + artikel_nummer
														+ "] - Menge [" + menge + "] - vorhanden";

												this.DEBUGG_LOGGER.addMassage(logneu);

											}

											if (exisitiert_dieser_artikel_im_wareneingang == false) {

												logneu = "#{IFU_WEING_OB_8517} WARENEINGANGSPOSITION OHNE BESTELLUNG\n---- Artikel: ["
														+ artikel_nummer + "]\n---- Menge: [" + menge + "]\n----  am: "
														+ format2.format(new Date()) + "\n---- von A-RUN: ["
														+ this.AGENT_RUN_CODE + "] \n---- M-RUN: ["
														+ this.MANAGER_RUN_CODE + "]\n---- Datei: ["
														+ this.FILE_CREATION_CODE + "]";

												String insertWarenEingangsPosition = "INSERT INTO static_wareineingang_positionen ("
														+ " position, wareneingangsnummer," + " artikelnummer,  menge,"
														+ " last_agent_operantion,"
														+ " last_operation_datum, eingangsdatum ) VALUES (" + position
														+ ", '" + wareneingangsnummer + "', '" + artikel_nummer + "', "
														+ menge + ", '" + logneu + "', '" + formatdb.format(new Date())
														+ "','" + datum_wareneingang + "');";

												this.DB_CONNECTOR.insertToDatabase(insertWarenEingangsPosition,
														"insertWarenEingangsPosition");

												this.DEBUGG_LOGGER.addMassage(
														"#{IFU_WEING_OB_8517P} Wareneingang Position wurde am [ "
																+ datum_wareneingang + " ]\n---- Wareneingangsnummer ["
																+ wareneingangsnummer + " ] \n---- Artikel: [ "
																+ artikel_nummer + " ]\n---- Menge[" + menge
																+ "] Stueck");

												// Artikelbestand wird
												// aktualisiert

												String logArtikeldbold = "";
												String logdbneuArt = "";
												int bereitsImLagerStaticArtikel = 0;

												String SQLbereitsImLagerAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
														+ artikel_nummer + "';";

												this.DB_CONNECTOR.readFromDatabase(SQLbereitsImLagerAusStaticArtikel,
														"SQLbereitsImLagerAusStaticArtikel");

												while (this.DB_CONNECTOR.rs.next()) {

													bereitsImLagerStaticArtikel = this.DB_CONNECTOR.rs.getInt(1);
													logArtikeldbold = this.DB_CONNECTOR.rs.getString(2);
												}

												int neuerLagerBestandAnzahl = bereitsImLagerStaticArtikel + menge;

												logdbneuArt = logArtikeldbold
														+ "#{IFU_WEING_OB_8517P} WARENEINGANG NEU\n---- NUMMER: [ "
														+ wareneingangsnummer + " ]\n---- Lagerbestand: [ a="
														+ bereitsImLagerStaticArtikel + ", n= "
														+ neuerLagerBestandAnzahl + " ]\n----  Artikel: ["
														+ artikel_nummer + "]\n---- [ Eingang=" + menge
														+ " ]\n---- am: " + format2.format(new Date())
														+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n----  M-RUN: ["
														+ this.MANAGER_RUN_CODE + "]\n---- Datei: ["
														+ this.FILE_CREATION_CODE + "]";

												String SQLneuerLagerBestand = "UPDATE static_artikel set lager_anzahl="
														+ neuerLagerBestandAnzahl + ", last_update='"
														+ formatdb.format(new Date()) + "', last_update_agent="
														+ AGENT_DBID + ", agent_operation='" + logdbneuArt
														+ "' where interne_artikelnummer='" + artikel_nummer + "';";

												this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

												this.DEBUGG_LOGGER.addMassage(
														"Der Lagerbestand wurde in static_artikel aktualisiert:");

												this.DEBUGG_LOGGER.addMassage(
														"- Lagerbestand Menge (alt): " + bereitsImLagerStaticArtikel);
												this.DEBUGG_LOGGER.addMassage(
														"- Lagerbestand Menge (neu): " + neuerLagerBestandAnzahl);
												this.DEBUGG_LOGGER.addMassage("- Wareneingangsmenge: " + menge + "");

											}

										}

									}

									this.DEBUGG_LOGGER.addMassage(
											"---------------------------------------------------------------------------------------");
									this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

									// Verschiebe Datei
									// ----------------------------------------------------------------------------------------------------------

									AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
											MANAGER_filepositionsindex);

									File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
											+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

									AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

									// ----------------------------------------------------------------------------------------------------------

									SINGEL_FILE_PROCESS_END_TIME = new Date();

									this.FILE_STATUS_ID = 3;

									beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
											this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
											this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
											this.DB_CONNECTOR);

									this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > " + laufzeit_dauer(
											SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

									// ----------------------------------------------------------------------------------------------------------
									// Beende Agenten OBJEKT
									// ----------------------------------------------------------------------------------------------------------

									this.AGENT_END_PROCESS_TIME = new Date();

									beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

									// Beende Agenten RUN
									// ----------------------------------------------------------------------------------------------------------

									this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

									beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID,
											this.AGENT_RUN_CODE, this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME,
											this.ANZAHL_XML_OBJEKTE, this.DEBUGG_LOGGER.debug_string.toString(),
											this.DB_CONNECTOR);

									// ----------------------------------------------------------------------------------------------------------

									this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
											+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

									this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
											+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
											+ "\n");
									this.DEBUGG_LOGGER.addMassage(
											"---------------------------------------------------------------------------------------");

									// ----------------------------------------------------------------------------------------------------------

								} else {

									this.DEBUGG_LOGGER.addMassage(
											"---------------------------------------------------------------------------------------");
									this.DEBUGG_LOGGER.addMassage(
											"Wareneingang [" + wareneingangsnummer + "] hat keine Positionen.");

									// Verschiebe Datei
									// ----------------------------------------------------------------------------------------------------------

									AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
											MANAGER_filepositionsindex);

									File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
											+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

									AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

									// ----------------------------------------------------------------------------------------------------------

									SINGEL_FILE_PROCESS_END_TIME = new Date();

									this.FILE_STATUS_ID = 3;

									beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
											this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
											this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
											this.DB_CONNECTOR);

									this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

									this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > " + laufzeit_dauer(
											SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

									// ----------------------------------------------------------------------------------------------------------
									// Beende Agenten OBJEKT
									// ----------------------------------------------------------------------------------------------------------

									this.AGENT_END_PROCESS_TIME = new Date();

									beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

									// Beende Agenten RUN
									// ----------------------------------------------------------------------------------------------------------

									this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

									beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID,
											this.AGENT_RUN_CODE, this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME,
											this.ANZAHL_XML_OBJEKTE, this.DEBUGG_LOGGER.debug_string.toString(),
											this.DB_CONNECTOR);

									// ----------------------------------------------------------------------------------------------------------

									this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
											+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

									this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
											+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
											+ "\n");

									this.DEBUGG_LOGGER.addMassage(
											"---------------------------------------------------------------------------------------");
									// ----------------------------------------------------------------------------------------------------------

								}

							}

							if (exisitiert_der_header_des_we == false) {

								this.DEBUGG_LOGGER.addMassage("Wareneingang hat keinen Insert");

								this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>STACK<< VERSCHOBEN]");

								this.DEBUGG_LOGGER.addMassage("Datenbank Eintrag der Datei wird geloescht.");

								this.DB_CONNECTOR
										.updateOnDatabase("DELETE from work_agent_files where file_creation_code='"
												+ FILE_CREATION_CODE + "';");

								// Verschiebe Datei
								// ----------------------------------------------------------------------------------------------------------

								String system_stack_file_name = (MANAGER_filepositionsindex + 1) + "_"
										+ this.AGENT_FILE_NAME_PATTERN + "_stack" + this.AUSWAHL_MANDANT_NAME
										+ FILE_CREATION_CODE + ".xml";

								File stack_xml_file = new File(
										this.AGENT_FILE_STACK_PATH + SYSTEM_PATH_DELIMITER + system_stack_file_name);

								AGENT_WORK_FILE_XML.renameTo(stack_xml_file);

								// Beende Datei OBJEKT
								// ----------------------------------------------------------------------------------------------------------

								SINGEL_FILE_PROCESS_END_TIME = new Date();

								this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

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
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

								// ----------------------------------------------------------------------------------------------------------

							}

						}

						// -------------------------------------------
						if (bestellung_vorhanden == true) {

							istBestellungVorhandenInDerDatenbank = existiert_diese_bestellung_in_der_db(
									bestellreferenz);

							if (istBestellungVorhandenInDerDatenbank == true) {

								this.DEBUGG_LOGGER.addMassage(
										"---------------------------------------------------------------------------------------");

								this.FILE_CONTENT_ID = FileTypes.WARENEINGANG_INSERT_EVENT_MIT_BESTELLUNG_FILE;

								this.DEBUGG_LOGGER
										.addMassage("Protokolliert einen Wareneingang anhand eine Bestellung \n");
								this.DEBUGG_LOGGER.addMassage(
										"Datei wird nicht verarbeitet, da dies von dem Bestell-Event-Update erledigt wird. \n");

								// Verschiebe Datei
								// ----------------------------------------------------------------------------------------------------------

								WareneingangToBestellungXMLGenerator generiereBestellung = new WareneingangToBestellungXMLGenerator(
										this.IS_LINUX, this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER,
										this.AGENT_FILE_DELIVER_PATH, this.AGENT_FILE_STACK_PATH,
										this.AGENT_FILE_STACK_PATH, this.WORK_FILE_DB_ID, "Update", this.DB_CONNECTOR);

								generiereBestellung.start(AGENT_WORK_FILE_XML, bestellreferenz, FILE_CREATION_CODE);

								event_agent_bestellungen bearbeite_bestellung = new event_agent_bestellungen(
										this.RUN_MANAGER_ID, this.MANAGER_RUN_CODE, this.IS_LINUX,
										this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER, this.AGENT_DUPLIKAT_PATH,
										this.DB_CONNECTOR);

								bearbeite_bestellung.start(generiereBestellung.getGeneratedBestellungsDatei(),
										MANAGER_filepositionsindex, this.FILE_CREATION_CODE + "_GEN",
										this.AUSWAHL_MANDANT_DBID, this.AUSWAHL_MANDANT_NAME);

								AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(
										MANAGER_filepositionsindex);

								File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
										+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

								AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

								// ----------------------------------------------------------------------------------------------------------
								// Beende Agenten OBJEKT
								// ----------------------------------------------------------------------------------------------------------

								this.FILE_STATUS_ID = 3;

								this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

								this.AGENT_END_PROCESS_TIME = new Date();

								beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

								beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
										this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
										this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

								beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
										this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
										this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME,
										this.DB_CONNECTOR);

								// ----------------------------------------------------------------------------------------------------------

								this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
										+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

								this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

								this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

								this.DEBUGG_LOGGER.addMassage(
										"---------------------------------------------------------------------------------------");
								// ----------------------------------------------------------------------------------------------------------

							}

							if (istBestellungVorhandenInDerDatenbank == false) {

								this.DEBUGG_LOGGER.addMassage(
										"WARENEINGANG MIT EINER BESTELLUNG - ABER BESTELLUNG NICHT VORHANDEN");
								this.DEBUGG_LOGGER.addMassage("GERERIERE BESTELLUNG - BITTE WARTEN....");

								String removeFileFromDB = "DELETE from work_agent_files where file_creation_code='"
										+ this.FILE_CREATION_CODE + "';";

								this.DB_CONNECTOR.updateOnDatabase(removeFileFromDB);

								// -----------------------------------

								WareneingangToBestellungXMLGenerator generiereBestellung = new WareneingangToBestellungXMLGenerator(
										this.IS_LINUX, this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER,
										this.AGENT_FILE_DELIVER_PATH, this.AGENT_FILE_STACK_PATH,
										this.AGENT_FILE_STACK_PATH, this.WORK_FILE_DB_ID, "Insert", this.DB_CONNECTOR);

								generiereBestellung.start(AGENT_WORK_FILE_XML, bestellreferenz, FILE_CREATION_CODE);

								// -----------------------------------

								event_agent_bestellungen bearbeite_bestellung = new event_agent_bestellungen(
										this.RUN_MANAGER_ID, this.MANAGER_RUN_CODE, this.IS_LINUX,
										this.OPERATING_SYSTEM, this.SYSTEM_PATH_DELIMITER, this.AGENT_DUPLIKAT_PATH,
										this.DB_CONNECTOR);

								bearbeite_bestellung.start(generiereBestellung.getGeneratedBestellungsDatei(),
										MANAGER_filepositionsindex, this.FILE_CREATION_CODE + "_GEN",
										this.AUSWAHL_MANDANT_DBID, this.AUSWAHL_MANDANT_NAME);

								AGENT_PROCESSED_XML_FILE_NAME = getSystem_BackToDeliverFILENAME(
										MANAGER_filepositionsindex);

								File processed_xml_file = new File(this.AGENT_FILE_DELIVER_PATH + SYSTEM_PATH_DELIMITER
										+ AGENT_PROCESSED_XML_FILE_NAME);

								AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

								// -----------------------------------

								this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
										+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

								this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

								this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN] \n");

								this.DEBUGG_LOGGER.addMassage(
										"---------------------------------------------------------------------------------------");
								// ----------------------------------------------------------------------------------------------------------
							}

						}

					}

				}

			}

			if (this.XML_FILE_WAS_EVER_PROCESSED == true) {
				// Verschiebe Datei
				// ----------------------------------------------------------------------------------------------------------

				String file_duplikat_label = "";

				String findeDuplikaSQL = "SELECT concat(file_creation_code,'_',id)  FROM  work_agent_files where filehash='"
						+ this.MD5_FILE_HASHCODE + "';";

				this.DB_CONNECTOR.readFromDatabase(findeDuplikaSQL, "findeDuplikaSQL");

				while (this.DB_CONNECTOR.rs.next()) {

					file_duplikat_label = this.DB_CONNECTOR.rs.getString(1);

				}

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

				this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>xfwep_DUPLIKATE<< VERSCHOBEN]\n");

				this.FILE_STATUS_ID = 4;

				beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
						this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
						AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

				this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
						+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

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
						+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

				this.DEBUGG_LOGGER.addMassage(
						"---------------------------------------------------------------------------------------");

				// ----------------------------------------------------------------------------------------------------------
			}

		} else {

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
			this.DEBUGG_LOGGER.addMassage(
					"---------------------------------------------------------------------------------------");
		}

	}

	public boolean exisitiert_der_header_des_we(String _wareneingang) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_wareneingang WHERE wareneingans_nummer='"
				+ _wareneingang + "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;
	}

	public boolean existiert_diese_bestellung_in_der_db(String _bestellnummer) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_bestellung WHERE interne_nummer_bestellung='"
				+ _bestellnummer + "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;
	}

	public boolean exisitiert_der_artikel_im_wareneingang(int position, String artikelnummer, String wareneingansnummer)
			throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_wareineingang_positionen where position="
				+ position + " and wareneingangsnummer='" + wareneingansnummer + "' and artikelnummer='" + artikelnummer
				+ "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;

	}

}
