package UPDATE_MANAGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class event_agent_barverkauf extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 7848115657717666099L;

	String barverkaufsnummer = "";
	String guid = "";
	String datum = "";
	boolean gebucht = false;
	boolean abschlussGebucht = false;

	public event_agent_barverkauf(int _managerid, String _manager_run_creation_code, boolean _isLinux, String _opsystem,
			String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
			throws SQLException, ClassNotFoundException {

		super();

		// 0
		this.DB_CONNECTOR = _db;
		// 1
		this.AGENT_DBID = 998;
		// 2
		this.FILE_CONTENT_ID = 0;
		// 3
		this.MANAGER_RUN_CODE = _manager_run_creation_code;
		// 4
		this.RUN_MANAGER_ID = _managerid;
		// 5
		this.IS_LINUX = _isLinux;
		// 6
		this.OPERATING_SYSTEM = _opsystem;
		// 7
		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
		// 8
		this.AGENT_DUPLIKAT_PATH = _duplikat_path;
		// 9
		this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
		// 10

		this.initialisiere_agenten_000(this.DB_CONNECTOR, this.AGENT_DBID);

		this.initialisiere_agenten_pfade(this.DB_CONNECTOR);

	}

	public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
			String mandant_kuerzel)
			throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

		this.FILE_CREATION_CODE = _fileid;
		this.XML_CONTENT_IS_INSERT_TYPE = false;
		this.XML_FILE_WAS_EVER_PROCESSED = true;
		this.ANZAHL_XML_OBJEKTE = 0;
		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;
		this.AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage("**************************************************************************");
			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "]");

			// -----------------
			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			this.update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);
			// -----------------

			this.XML_FILE_WAS_EVER_PROCESSED = wurde_diese_datei_schon_mal_verarbeitet(AGENT_DELIVER_XML_FILE,
					this.DB_CONNECTOR, this.AGENT_DBID);

			// Wird nur verarbeitet wenn die Datei nicht einen exisitierenden Hash-Code hat

			if (this.XML_FILE_WAS_EVER_PROCESSED == false) {

				this.DEBUGG_LOGGER.addMassage("Datei wurde noch nie verarbeitet!");

				this.SINGEL_FILE_PROCESS_START_TIME = new Date();

				// -----------------------------------------------------------------------

				Path absolut_path = Paths.get(this.AGENT_DELIVER_XML_FILE.getAbsolutePath());

				// Die Attribute der Datei werden ermittelt
				BasicFileAttributes datei_attribute = Files.readAttributes(absolut_path, BasicFileAttributes.class);

				this.FILE_SIZE = datei_attribute.size();

				this.FILE_CREATION_TIME_FOR_DB = formatdb
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.FILE_CREATION_TIME = format0
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.put_deliver_file_in_db(AGENT_DBID, this.AGENT_DELIVER_XML_FILE.getName(),
						formatdb.format(SINGEL_FILE_PROCESS_START_TIME), this.AGENT_RUN_CODE, this.FILE_SIZE,
						FILE_CREATION_TIME_FOR_DB, FILE_CREATION_CODE, AGENT_DBID, MD5_FILE_HASHCODE, this.DB_CONNECTOR,
						this.AUSWAHL_MANDANT_DBID);

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

				// -----------
				NodeList nListHeadAttributes = doc.getElementsByTagName("BvhList");
				Node node1 = nListHeadAttributes.item(0);
				Element eb = (Element) node1;

				setFileOperationType(new String(eb.getAttribute("Operation")));

				// -------------

				Element barverkauf_kopf_element = (Element) (doc.getElementsByTagName("Bvh").item(0));

				try {

					barverkaufsnummer = barverkauf_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					barverkaufsnummer = "?";

				}

				try {

					guid = barverkauf_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					guid = "?";
				}

				try {

					String tempDatumWareneingangAusXML = "";
					Date datum_wareneingang_temp = null;

					tempDatumWareneingangAusXML = barverkauf_kopf_element.getElementsByTagName("Date").item(0)
							.getTextContent().trim();

					if (tempDatumWareneingangAusXML.trim() == "") {
						tempDatumWareneingangAusXML = "9999-09-09 00:00:00";

					}

					datum_wareneingang_temp = XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

					datum = formatdb.format(datum_wareneingang_temp);

				} catch (NullPointerException ecxc) {
					datum = "9999-09-09 00:00:00";
				} catch (ParseException e1) {

					datum = "9999-09-09 00:00:00";
					e1.printStackTrace();
				}

				try {
					gebucht = new Boolean(
							barverkauf_kopf_element.getElementsByTagName("Erledigt").item(0).getTextContent().trim());

				} catch (NullPointerException ecxc) {
					gebucht = false;
				}

				try {
					abschlussGebucht = new Boolean(barverkauf_kopf_element.getElementsByTagName("AbschlussGebucht")
							.item(0).getTextContent().trim());

				} catch (NullPointerException ecxc) {
					abschlussGebucht = false;
				}

				this.CHECK_HEADER_ENTRY_EXISTENCE = this.existiert_dieser_barverkauf(barverkaufsnummer);

				// Barverkauf Kopf existiert nicht
				if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

					this.DEBUGG_LOGGER.addMassage("Barverkauf [" + barverkaufsnummer + "] hat statt gefunden am:[ "
							+ datum + " ] - Datei: [" + this.FILE_CREATION_CODE + "] - TYPE-INSERT: ["
							+ this.XML_CONTENT_IS_INSERT_TYPE + "]");
					this.DEBUGG_LOGGER.addMassage("-----------------------------------------------------------");

					String sqlInsertBarverkaufsKopf = "INSERT INTO static_barverkauf (guid, barverkaufsnummer, datum, gebucht,"
							+ " abschlussgebucht,last_agentruncode,last_managerruncode, insert_work_file_id,last_agent_id, last_oparation_datum, last_agent_operation, mandant_id) "// 11
							+ " VALUES ( '" + guid + "', '" + barverkaufsnummer + "','" + datum + "', " + gebucht + ", "
							+ abschlussGebucht + ", '" + this.AGENT_RUN_CODE + "', '" + this.MANAGER_RUN_CODE + "', "
							+ this.WORK_FILE_DB_ID + "," + this.AGENT_DBID + ", '" + this.formatdb.format(new Date())
							+ "', '- INSERT BARVERKAUF [" + barverkaufsnummer + "]\n----  am: "
							+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE + "] und M-RUN: ["
							+ this.MANAGER_RUN_CODE + "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n\n',"
							+ this.AUSWAHL_MANDANT_DBID + " );";

					this.DB_CONNECTOR.insertToDatabase(sqlInsertBarverkaufsKopf, "sqlInsertBarverkaufsKopf");

					// =Verarbeite
					// Positionen---------------------------------------------------------------------

					NodeList barverkaufspositionen = barverkauf_kopf_element.getElementsByTagName("Bvp");

					this.ANZAHL_XML_OBJEKTE = barverkaufspositionen.getLength();

					this.DEBUGG_LOGGER.addMassage("Barverkauf Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE);

					if (this.ANZAHL_XML_OBJEKTE > 0) {

						for (int position_im_barverkauf = 0; position_im_barverkauf < this.ANZAHL_XML_OBJEKTE; position_im_barverkauf++) {

							Node einzelne_positions_wurzel = barverkaufspositionen.item(position_im_barverkauf);

							if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

								Element barverkaufPositionElement = (Element) einzelne_positions_wurzel;

								int position = 0;
								String artikelnummer = "";
								int menge_lager = 0;

								try {

									BigDecimal menG = new BigDecimal(
											barverkaufPositionElement.getElementsByTagName("Position").item(0)
													.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

									position = menG.intValue();

								} catch (NullPointerException ecxc) {
									position = 0;
								}

								try {

									artikelnummer = barverkaufPositionElement.getElementsByTagName("ArtNr").item(0)
											.getTextContent().trim().replace(" ", "$")
											.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "").trim();

								} catch (NullPointerException ecxc) {
									artikelnummer = "?";
								}

								try {

									BigDecimal menG = new BigDecimal(barverkaufPositionElement
											.getElementsByTagName("MengeLager").item(0).getTextContent());

									menge_lager = menG.intValue();

								} catch (NullPointerException ecxc) {
									menge_lager = 0;
								}

								String newLog1 = "- INSERT BARVERKAUF POSITION: Barverkauf: [" + barverkaufsnummer
										+ "]\n---- Artikel: [" + artikelnummer + "],\n---- Menge=" + menge_lager
										+ ",\n---- am: " + format2.format(new Date()) + " von A-RUN: ["
										+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
										+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n";

								String sqlInsertBarverkaufPosition = "Insert INTO static_barverkauf_positionen (position,"
										+ " barverkaufsnummer," //
										+ " artikelnummer," //
										+ " menge_lieferschein, " //
										+ " last_operation, " //
										+ " last_operation_datum) VALUES (" + position + " ,'" + barverkaufsnummer
										+ "','" + artikelnummer + "', " + menge_lager + ", '" + newLog1 + "' , '"
										+ formatdb.format(new Date()) + "' );";

								boolean positonExisitiert = checkAuftragsPosition(position, barverkaufsnummer,
										artikelnummer);

								if (artikelnummer.contains("WP-") == false && artikelnummer.contains("WPK-") == false
										&& positonExisitiert == false && artikelnummer != "" && artikelnummer != "?"
										&& artikelnummer.length() >= 5) {

									this.DB_CONNECTOR.insertToDatabase(sqlInsertBarverkaufPosition,
											"sqlInsertBarverkaufPosition");

									int _alt_artikel_lager_menge = 0;
									String _alt_artikel_log = "";

									String SQLbereitsImRESERVIERTAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
											+ artikelnummer + "';";

									this.DB_CONNECTOR.readFromDatabase(SQLbereitsImRESERVIERTAusStaticArtikel,
											"SQLbereitsImRESERVIERTAusStaticArtikel");

									while (this.DB_CONNECTOR.rs.next()) {
										_alt_artikel_lager_menge = this.DB_CONNECTOR.rs.getInt(1);
										_alt_artikel_log = this.DB_CONNECTOR.rs.getString(2);
									}

									int _neu_artikel_lager_menge = _alt_artikel_lager_menge - menge_lager;

									String _neu_artikel_log = _alt_artikel_log + "- BARVERKAUF UPDATE - Barverkauf ["
											+ barverkaufsnummer + "] hat [" + menge_lager
											+ "] Steuck(e) entnommen. Lagerbestand: [a=" + _alt_artikel_lager_menge
											+ ", n=" + _neu_artikel_lager_menge + "]\n----  am: "
											+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
											+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
											+ this.FILE_CREATION_CODE + "]\n\n";

									String SQLneuerArtikelReservierungAnzahl = "UPDATE static_artikel set lager_anzahl="
											+ _neu_artikel_lager_menge + ", last_update='" + formatdb.format(new Date())
											+ "', last_update_agent=" + AGENT_DBID + ", agent_operation='"
											+ _neu_artikel_log + "' where interne_artikelnummer='" + artikelnummer
											+ "';";

									this.DB_CONNECTOR.updateOnDatabase(SQLneuerArtikelReservierungAnzahl);

									this.DEBUGG_LOGGER.addMassage(
											"---- Der Barverkauf aktualisiert den Artikel [" + artikelnummer + "]:");

									this.DEBUGG_LOGGER.addMassage("---- Lagerbestand: [a=" + _alt_artikel_lager_menge
											+ ", n=" + _neu_artikel_lager_menge + " ]");

									this.DEBUGG_LOGGER.addMassage("---- Durch Barverkauf: [" + barverkaufsnummer
											+ "] wurden: [" + menge_lager + "] Stueck(e) verkauft.");

									String guid_verkauf = "BARVERKAUF_" + barverkaufsnummer + "_" + position;

									String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
											+ guid_verkauf + "','" + artikelnummer + "'," + menge_lager + ", '" + datum
											+ "',0);";

									this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");

								} // --

							} // ---

						} // - for

						this.DEBUGG_LOGGER.addMassage("Barverkauf [" + barverkaufsnummer + "] hat keine Positionen.");
						this.DEBUGG_LOGGER.addMassage("*********************************************************");
						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

						File processed_xml_file = new File(
								this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

						AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

						this.FILE_STATUS_ID = 3;

						beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
								this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
								AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

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
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

						// ----------------------------------------------------------------------------------------------------------

					} else {

						this.DEBUGG_LOGGER.addMassage("Barverkauf [" + barverkaufsnummer + "] hat keine Positionen.");
						this.DEBUGG_LOGGER.addMassage("*********************************************************");
						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

						File processed_xml_file = new File(
								this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

						AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

						this.FILE_STATUS_ID = 3;

						beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
								this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
								AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

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
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

						// ----------------------------------------------------------------------------------------------------------

					}

				}
				// -------------------------------------------------------
				// Es gab bereits eine Insert Datei
				// -------------------------------------------------------
				if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

					this.DEBUGG_LOGGER.addMassage("Barverkauf existiert bereits");

					// update
					if (this.XML_CONTENT_IS_INSERT_TYPE == false) {

						this.DEBUGG_LOGGER
								.addMassage("#{BAV_UPD001} Barverkauf [" + barverkaufsnummer + "] wird aktualisiert!");

						boolean alt_gebucht = false;
						boolean alt_abschlussGebucht = false;

						String sql_check_db_status = "Select gebucht, abschlussGebucht from static_barverkauf where barverkaufsnummer='"
								+ barverkaufsnummer + "';";

						this.DB_CONNECTOR.readFromDatabase(sql_check_db_status, "sql_check_db_status");

						while (this.DB_CONNECTOR.rs.next()) {

							alt_gebucht = this.DB_CONNECTOR.rs.getBoolean(1);
							alt_abschlussGebucht = this.DB_CONNECTOR.rs.getBoolean(2);

						}

						if (this.gebucht == true && alt_gebucht == false) {

							String update_gebucht = "update static_barverkauf set gebucht=1 where barverkaufsnummer='"
									+ barverkaufsnummer + "';";
							this.DB_CONNECTOR.updateOnDatabase(update_gebucht);

						}

						if (this.abschlussGebucht == true && alt_abschlussGebucht == false) {
							String update_abschlussGebucht = "update static_barverkauf set abschlussgebucht=1 where barverkaufsnummer='"
									+ barverkaufsnummer + "';";
							this.DB_CONNECTOR.updateOnDatabase(update_abschlussGebucht);
						}

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList barverkaufspositionen = barverkauf_kopf_element.getElementsByTagName("Bvp");

						this.ANZAHL_XML_OBJEKTE = barverkaufspositionen.getLength();

						this.DEBUGG_LOGGER.addMassage("Barverkauf Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE);

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_im_barverkauf = 0; position_im_barverkauf < this.ANZAHL_XML_OBJEKTE; position_im_barverkauf++) {

								Node einzelne_positions_wurzel = barverkaufspositionen.item(position_im_barverkauf);

								if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

									Element barverkaufPositionElement = (Element) einzelne_positions_wurzel;

									int position = 0;
									String artikelnummer = "";
									int menge_lager = 0;

									try {

										BigDecimal menG = new BigDecimal(
												barverkaufPositionElement.getElementsByTagName("Position").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										position = menG.intValue();

									} catch (NullPointerException ecxc) {
										position = 0;
									}

									try {

										artikelnummer = barverkaufPositionElement.getElementsByTagName("ArtNr").item(0)
												.getTextContent().trim().replace(" ", "$")
												.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "").trim();

									} catch (NullPointerException ecxc) {
										artikelnummer = "?";
									}

									try {

										BigDecimal menG = new BigDecimal(barverkaufPositionElement
												.getElementsByTagName("MengeLager").item(0).getTextContent());

										menge_lager = menG.intValue();

									} catch (NullPointerException ecxc) {
										menge_lager = 0;
									}

									boolean verkauf_existance = true;

									String check_position_existance = "SELECT EXISTS (SELECT * FROM static_barverkauf_positionen where position="
											+ position + " and barverkaufsnummer='" + barverkaufsnummer
											+ "' and artikelnummer='" + artikelnummer + "');";

									this.DB_CONNECTOR.readFromDatabase(check_position_existance,
											"check_position_existance ");

									while (this.DB_CONNECTOR.rs.next()) {

										verkauf_existance = this.DB_CONNECTOR.rs.getBoolean(1);

									}

									if (verkauf_existance == false) {

										this.DEBUGG_LOGGER.addMassage("Barverkauf Positionen [" + position_im_barverkauf
												+ "] wird hinzugefuegt:");

										String newLog1 = "- INSERT BARVERKAUF POSITION: Barverkauf: ["
												+ barverkaufsnummer + "]\n---- Artikel: [" + artikelnummer
												+ "],\n---- Menge=" + menge_lager + ",\n---- am: "
												+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
												+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
												+ this.FILE_CREATION_CODE + "]\n";

										String sqlInsertBarverkaufPosition = "Insert INTO static_barverkauf_positionen (position,"
												+ " barverkaufsnummer," //
												+ " artikelnummer," //
												+ " menge_lieferschein, " //
												+ " last_operation, " //
												+ " last_operation_datum) VALUES (" + position + " ,'"
												+ barverkaufsnummer + "','" + artikelnummer + "', " + menge_lager
												+ ", '" + newLog1 + "' , '" + formatdb.format(new Date()) + "' );";

										boolean positonExisitiert = checkAuftragsPosition(position, barverkaufsnummer,
												artikelnummer);

										if (artikelnummer.contains("WP-") == false
												&& artikelnummer.contains("WPK-") == false && positonExisitiert == false
												&& artikelnummer != "" && artikelnummer != "?"
												&& artikelnummer.length() >= 5) {

											this.DB_CONNECTOR.insertToDatabase(sqlInsertBarverkaufPosition,
													"sqlInsertBarverkaufPosition");

											int _alt_artikel_lager_menge = 0;
											String _alt_artikel_log = "";

											String SQLbereitsImRESERVIERTAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
													+ artikelnummer + "';";

											this.DB_CONNECTOR.readFromDatabase(SQLbereitsImRESERVIERTAusStaticArtikel,
													"SQLbereitsImRESERVIERTAusStaticArtikel");

											while (this.DB_CONNECTOR.rs.next()) {
												_alt_artikel_lager_menge = this.DB_CONNECTOR.rs.getInt(1);
												_alt_artikel_log = this.DB_CONNECTOR.rs.getString(2);
											}

											this.DEBUGG_LOGGER.addMassage(
													"Artike [" + artikelnummer + "] , Menge [" + menge_lager + "]");

											int _neu_artikel_lager_menge = _alt_artikel_lager_menge - menge_lager;

											String _neu_artikel_log = _alt_artikel_log
													+ "- BARVERKAUF UPDATE - Barverkauf [" + barverkaufsnummer
													+ "] hat [" + menge_lager
													+ "] Steuck(e) entnommen. Lagerbestand: [a="
													+ _alt_artikel_lager_menge + ", n=" + _neu_artikel_lager_menge
													+ "]\n----  am: " + format2.format(new Date()) + " von A-RUN: ["
													+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
													+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n\n";

											String SQLneuerArtikelReservierungAnzahl = "UPDATE static_artikel set lager_anzahl="
													+ _neu_artikel_lager_menge + ", last_update='"
													+ formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
													+ ", agent_operation='" + _neu_artikel_log
													+ "' where interne_artikelnummer='" + artikelnummer + "';";

											this.DB_CONNECTOR.updateOnDatabase(SQLneuerArtikelReservierungAnzahl);

											this.DEBUGG_LOGGER
													.addMassage("---- Der Barverkauf aktualisiert den Artikel ["
															+ artikelnummer + "]:");

											this.DEBUGG_LOGGER
													.addMassage("---- Lagerbestand: [a=" + _alt_artikel_lager_menge
															+ ", n=" + _neu_artikel_lager_menge + " ]");

											this.DEBUGG_LOGGER.addMassage("---- Durch Barverkauf: [" + barverkaufsnummer
													+ "] wurden: [" + menge_lager + "] Stueck(e) verkauft.");

											String guid_verkauf = "BARVERKAUF_" + barverkaufsnummer + "_" + position;

											String SQL_Verkauf = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
													+ guid_verkauf + "','" + artikelnummer + "'," + menge_lager + ", '"
													+ datum + "',0);";

											this.DB_CONNECTOR.insertToDatabase(SQL_Verkauf, "SQL_Verkauf");
										}

									}
									this.DEBUGG_LOGGER.addMassage("................................................");
								}

								this.DEBUGG_LOGGER.addMassage("................................................");
							}

							this.DEBUGG_LOGGER
									.addMassage("Barverkauf [" + barverkaufsnummer + "] hat keine Positionen.");
							this.DEBUGG_LOGGER.addMassage("*********************************************************");
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
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

							// ----------------------------------------------------------------------------------------------------------

						} else {

							this.DEBUGG_LOGGER
									.addMassage("Barverkauf [" + barverkaufsnummer + "] hat keine Positionen.");
							this.DEBUGG_LOGGER.addMassage("*********************************************************");
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
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

							// ----------------------------------------------------------------------------------------------------------

						}

					}

					if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

						this.DEBUGG_LOGGER.addMassage("*********************************************************");
						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

						File processed_xml_file = new File(
								this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

						AGENT_WORK_FILE_XML.renameTo(processed_xml_file);

						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

						this.FILE_STATUS_ID = 3;

						beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
								this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
								AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

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
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

						// ----------------------------------------------------------------------------------------------------------

					}

				}
			}

			// Diese Datei wurde schon mal mit dem gleichen Inhalt geliefert -geprueft
			if (this.XML_FILE_WAS_EVER_PROCESSED == true) {

				this.DEBUGG_LOGGER.addMassage("*********************************************************");
				this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

				// Verschiebe Datei
				// ----------------------------------------------------------------------------------------------------------

				AGENT_DUPLIKAT_XML_FILE_NAME = getSystem_duplikat_file_name(MANAGER_filepositionsindex);

				File processed_xml_file = new File(
						this.AGENT_DUPLIKAT_PATH + SYSTEM_PATH_DELIMITER + AGENT_DUPLIKAT_XML_FILE_NAME);

				this.AGENT_DELIVER_XML_FILE.renameTo(processed_xml_file);

				// ----------------------------------------------------------------------------------------------------------

				SINGEL_FILE_PROCESS_END_TIME = new Date();

				this.FILE_STATUS_ID = 3;

				beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
						this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
						AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

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

				this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE
						+ ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

				this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
						+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

				// ----------------------------------------------------------------------------------------------------------

			}

		} else

		{
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

	public boolean existiert_dieser_barverkauf(String _barverkaufsnummer) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_barverkauf WHERE barverkaufsnummer='"
				+ _barverkaufsnummer + "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;
	}

	public boolean checkAuftragsPosition(int _position, String _barverkaufsnummer, String _artikelnummer)
			throws SQLException {

		String sqlCheckExsistenz = "SELECT EXISTS (SELECT * FROM static_barverkauf_positionen WHERE position="
				+ _position + " and barverkaufsnummer='" + _barverkaufsnummer + "' and artikelnummer='" + _artikelnummer
				+ "');";

		boolean temp = true;

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;

	}

}
