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

public class event_agent_bestellungen extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = -5998327442337384471L;

	public event_agent_bestellungen(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
			throws SQLException, ClassNotFoundException {

		super();

		this.DB_CONNECTOR = _db;
		this.AGENT_DBID = 994;
		this.FILE_CONTENT_ID = 0;
		this.MANAGER_RUN_CODE = _manager_run_creation_code;
		this.RUN_MANAGER_ID = _managerid;
		this.IS_LINUX = _isLinux;
		this.OPERATING_SYSTEM = _opsystem;
		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
		this.AGENT_DUPLIKAT_PATH = _duplikat_path;
		this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
		this.initialisiere_agenten_000(this.DB_CONNECTOR, this.AGENT_DBID);
		this.initialisiere_agenten_pfade(this.DB_CONNECTOR);

	}

	public event_agent_bestellungen() {

	}

	// #todo: guid_id verwenden um die Datensatze von einander zu unterscheiden

	public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
			String mandant_kuerzel)
			throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

		this.FILE_CREATION_CODE = _fileid;
		this.ANZAHL_XML_OBJEKTE = 0;

		String _position_guid = "";
		String _interne_artikel_nummer = "";
		int _rec = 0;
		int _bestellung_xml_neue_menge = 0;
		int _bestellung_xml_menge_geliefert = 0;
		int _bestellung_xml_neue_soll_menge = 0;

		String nummer_der_bestellung = "";
		String datum_der_bestellung = "";

		String guid_der_bestellung = "";
		boolean bestellung_ist_erledigt = false;
		String bestellung_erledigt_am_datum = "";
		boolean bestellung_ist_storniert = false;

		String filetype_operation_content = "?";

		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "]");

			AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

			// -----------------

			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			this.update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);

			// -----------------

			this.XML_FILE_WAS_EVER_PROCESSED = this.wurde_diese_datei_schon_mal_verarbeitet(AGENT_DELIVER_XML_FILE,
					this.DB_CONNECTOR, this.AGENT_DBID);

			if (this.XML_FILE_WAS_EVER_PROCESSED == false) {

				// -----------------------------------------------------------------------

				Path absolut_path = Paths.get(this.AGENT_DELIVER_XML_FILE.getAbsolutePath());

				// Die Attribute der Datei werden ermittelt
				BasicFileAttributes datei_attribute = Files.readAttributes(absolut_path, BasicFileAttributes.class);

				this.FILE_SIZE = datei_attribute.size();

				this.FILE_CREATION_TIME_FOR_DB = formatdb
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.FILE_CREATION_TIME = format0
						.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

				this.SINGEL_FILE_PROCESS_START_TIME = new Date();

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

				NodeList bestell_kopf_node = doc.getElementsByTagName("BhList");

				Node node_kopf_wurzel = bestell_kopf_node.item(0);

				Element node_kopf_wurzel_element = (Element) node_kopf_wurzel;

				Element bestell_kopf_element = (Element) (doc.getElementsByTagName("Bh").item(0));

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

				try {

					nummer_der_bestellung = bestell_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					nummer_der_bestellung = "?";

				}

				this.CHECK_HEADER_ENTRY_EXISTENCE = existiert_diese_bestellung(nummer_der_bestellung);

				// ==INSERT---------------------------------------------------------------------------
				if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

					this.FILE_CONTENT_ID = FileTypes.BESTELLUNG_INSERT_EVENT_FILE;

					// Nur wenn dieser EIntrag nicht existiert
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

						datum_der_bestellung = "";
						guid_der_bestellung = "";
						bestellung_ist_erledigt = false;
						bestellung_erledigt_am_datum = "";
						bestellung_ist_storniert = false;

						try {

							nummer_der_bestellung = bestell_kopf_element.getElementsByTagName("Nr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "").trim();

						} catch (NullPointerException ecxc) {
							nummer_der_bestellung = "?";
						}

						try {

							String tempDatumBestellunmgAusXML = "";
							Date datum_der_bestellung_temp = null;

							tempDatumBestellunmgAusXML = bestell_kopf_element.getElementsByTagName("Date").item(0)
									.getTextContent().trim();

							datum_der_bestellung_temp = XML_FORMAT_DATUM.parse(tempDatumBestellunmgAusXML);

							datum_der_bestellung = formatdb.format(datum_der_bestellung_temp);

						} catch (NullPointerException ecxc) {
							datum_der_bestellung = "9999-09-09 00:00:00";
						} catch (ParseException e1) {

							datum_der_bestellung = "9999-09-09 00:00:00";
							e1.printStackTrace();
						}

						try {

							String tempDatumErledigtAusXML = "";

							Date datum_erledigt_bestellung_temp = null;

							tempDatumErledigtAusXML = bestell_kopf_element.getElementsByTagName("Date").item(0)
									.getTextContent().trim();

							datum_erledigt_bestellung_temp = XML_FORMAT_DATUM.parse(tempDatumErledigtAusXML);

							bestellung_erledigt_am_datum = formatdb.format(datum_erledigt_bestellung_temp);

						} catch (NullPointerException ecxc) {
							bestellung_erledigt_am_datum = "9999-09-09 00:00:00";
						} catch (ParseException e1) {

							bestellung_erledigt_am_datum = "9999-09-09 00:00:00";
							e1.printStackTrace();
						}

						try {

							guid_der_bestellung = bestell_kopf_element.getElementsByTagName("GUID").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid_der_bestellung = "?";
						}

						try {

							bestellung_ist_erledigt = new Boolean(bestell_kopf_element
									.getElementsByTagName("BhErledigt").item(0).getTextContent().trim());

						} catch (NullPointerException ecxc) {
							bestellung_ist_erledigt = false;
						}

						try {

							bestellung_ist_storniert = new Boolean(bestell_kopf_element.getElementsByTagName("BhStorno")
									.item(0).getTextContent().trim());

						} catch (NullPointerException ecxc) {
							bestellung_ist_storniert = false;
						}

						String SQLfuegeBestellKompfEin = "INSERT INTO static_bestellung ( bestell_guid," //
								+ " interne_nummer_bestellung," //
								+ " erledigt,"//
								+ " storno," //
								+ " bestellung_erledigt_am," //
								+ " last_agent_operation," //
								+ " last_oparation_datum," //
								+ " last_agent_id," //
								+ " insert_work_file_id,"//
								+ " last_agentruncode, " //
								+ " last_managerruncode," //
								+ " bestell_datum, mandant_id ) VALUES ('" //
								+ guid_der_bestellung + "','" //
								+ nummer_der_bestellung//
								+ "'," + bestellung_ist_erledigt//
								+ "," + bestellung_ist_storniert //
								+ ", '" + bestellung_erledigt_am_datum//
								+ "' , '#{I_BEST_0001} INSERT BESTELLUNG NEU\n---- Bestellnummer: ["
								+ nummer_der_bestellung//
								+ "]\n---- bearbeitet am: " + format2.format(new Date())//
								+ "\n---- A-RUN: [" //
								+ this.AGENT_RUN_CODE + "]\n---- Datei: [" + FILE_CREATION_CODE + "]', '" //

								+ formatdb.format(new Date()) //
								+ "', " + AGENT_DBID + ", "//
								+ this.WORK_FILE_DB_ID + ", '" //
								+ this.AGENT_RUN_CODE //
								+ "', '" + this.MANAGER_RUN_CODE + "', '" + datum_der_bestellung + "', "
								+ this.AUSWAHL_MANDANT_DBID + " );";

						this.DB_CONNECTOR.insertToDatabase(SQLfuegeBestellKompfEin, "SQLfuegeBestellKompfEin");

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList bestellung_positionen = bestell_kopf_element.getElementsByTagName("Bp");

						this.ANZAHL_XML_OBJEKTE = bestellung_positionen.getLength();

						this.DEBUGG_LOGGER.addMassage("Bestellungs-Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");

						this.DEBUGG_LOGGER
								.addMassage("------------------------------------------------------------------");

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_in_bestellung = 0; position_in_bestellung < bestellung_positionen
									.getLength(); position_in_bestellung++) {

								Node einzelne_positions_wurzel = bestellung_positionen.item(position_in_bestellung);

								if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

									Element bestellpositionElement = (Element) einzelne_positions_wurzel;

									_rec = 0;
									_interne_artikel_nummer = "";
									_bestellung_xml_neue_menge = 0;
									_bestellung_xml_menge_geliefert = 0;
									_bestellung_xml_neue_soll_menge = 0;
									_position_guid = "";

									try {

										BigDecimal menG = new BigDecimal(
												bestellpositionElement.getElementsByTagName("Rec").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										_rec = menG.intValue();

									} catch (NullPointerException ecxc) {
										_rec = 0;
									}

									try {

										_position_guid = bestellpositionElement.getElementsByTagName("GUID").item(0)
												.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

									} catch (NullPointerException ecxc) {
										_position_guid = "?";
									}

									try {

										_interne_artikel_nummer = bestellpositionElement.getElementsByTagName("ArtNr")
												.item(0).getTextContent()
												.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

									} catch (NullPointerException ecxc) {
										_interne_artikel_nummer = "?";
									}

									try {

										BigDecimal menG = new BigDecimal(bestellpositionElement
												.getElementsByTagName("Menge").item(0).getTextContent());

										_bestellung_xml_neue_menge = menG.intValue();

									} catch (NullPointerException ecxc) {
										_bestellung_xml_neue_menge = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(bestellpositionElement
												.getElementsByTagName("MengeGeliefert").item(0).getTextContent());

										_bestellung_xml_menge_geliefert = menG.intValue();

									} catch (NullPointerException ecxc) {
										_bestellung_xml_menge_geliefert = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(bestellpositionElement
												.getElementsByTagName("MengeSoll").item(0).getTextContent());

										_bestellung_xml_neue_soll_menge = menG.intValue();

									} catch (NullPointerException ecxc) {
										_bestellung_xml_neue_soll_menge = 0;
									}

									int dif_noch_nicht_gekommen = (_bestellung_xml_neue_menge
											- _bestellung_xml_menge_geliefert);

									String newLog = "#{I_BEST_1528} INSERT BESTELLUNG NEU am: "
											+ format2.format(new Date()) + " A-RUN: [" + this.AGENT_RUN_CODE
											+ "]M-RUN: [" + this.MANAGER_RUN_CODE + "]";

									// Der Eintrag wird gespeichert

									try {

										String sqlPositionEinfuegen = "INSERT INTO static_bestellung_positionen (" //
												+ " position, bestellungnummer,artikel_nummer,"
												+ " menge_bestellt, menge_geliefert,menge_noch_zu_liefern,agent_operation, last_operation_datum, guid_position ) VALUES ("
												+ _rec + ",'" + nummer_der_bestellung + "','" + _interne_artikel_nummer
												+ "'," + _bestellung_xml_neue_menge + ","
												+ _bestellung_xml_menge_geliefert + ","
												+ _bestellung_xml_neue_soll_menge + ",'"
												+ this.LOG_CHECKER.generateNewLogForDB("", newLog) + "','"
												+ formatdb.format(new Date()) + "', '" + _position_guid + "');";

										if (_interne_artikel_nummer.contains("WP-") == false
												&& _interne_artikel_nummer.contains("WPK-") == false
												&& _interne_artikel_nummer != "" && _interne_artikel_nummer != "?"
												&& _interne_artikel_nummer.length() >= 5) {

											this.DB_CONNECTOR.insertToDatabase(sqlPositionEinfuegen,
													"sqlPositionEinfuegen");

											this.DEBUGG_LOGGER
													.addMassage("Bestellung [" + nummer_der_bestellung + " ]");

											this.DEBUGG_LOGGER.addMassage("P[" + _rec + "] - Artikel: [ "
													+ _interne_artikel_nummer + " ]\n---- Position GUID: ["
													+ _position_guid + "]\n---- Menge = " + _bestellung_xml_neue_menge
													+ "\n---- Menge-Geliefert= " + _bestellung_xml_menge_geliefert
													+ "\n---- Soll-menge= " + _bestellung_xml_neue_soll_menge + " ");

											int bestellMengeInXML = _bestellung_xml_neue_menge;

											String logdbold = "";
											String logdbneuArt = "";
											String SQLbereitsBestelltAusStaticArtikel = "Select bestellt_anzahl, ifnull(agent_operation,'') from static_artikel where interne_artikelnummer='"
													+ _interne_artikel_nummer + "';";

											int _alt_bestell_menge_aus_artikel = 0;

											this.DB_CONNECTOR.readFromDatabase(SQLbereitsBestelltAusStaticArtikel,
													"SQLbereitsBestelltAusStaticArtikel");

											while (this.DB_CONNECTOR.rs.next()) {

												_alt_bestell_menge_aus_artikel = this.DB_CONNECTOR.rs.getInt(1);
												logdbold = this.DB_CONNECTOR.rs.getString(2);
											}

											int neuerBestellAnzahl = _alt_bestell_menge_aus_artikel
													+ dif_noch_nicht_gekommen;

											logdbneuArt = "#{I_BEST_627} BESTELLUNG NEU, Bestellung: [ "
													+ nummer_der_bestellung + " ]\n---- Erledigt ["
													+ bestellung_ist_erledigt + "]\n---- Storno ["
													+ bestellung_ist_storniert + "]" + "\n---- Position GUID: ["
													+ _position_guid + "],\n---- Artikel bestellt: [ a="
													+ _alt_bestell_menge_aus_artikel + ", n= " + neuerBestellAnzahl
													+ " ]\n---- [ neu bestellt=" + bestellMengeInXML
													+ " ]\n---- bearbeitet am: " + format2.format(new Date())
													+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
													+ this.MANAGER_RUN_CODE + "]\n---- Datei:[" + FILE_CREATION_CODE
													+ "]";

											String SQLneuerLagerBestand = "UPDATE static_artikel set bestellt_anzahl="
													+ neuerBestellAnzahl + ", last_update='"
													+ formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
													+ ", agent_operation='"
													+ this.LOG_CHECKER.generateNewLogForDB(logdbold, logdbneuArt)
													+ "' where interne_artikelnummer='" + _interne_artikel_nummer
													+ "';";

											this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

											this.DEBUGG_LOGGER.addMassage("---- Die Bestellmenge wurde im Artikel: [ "
													+ _interne_artikel_nummer + " ] aktualisiert:");

											this.DEBUGG_LOGGER.addMassage(
													"---- Bestellte Menge [a=" + _alt_bestell_menge_aus_artikel + ", n="
															+ neuerBestellAnzahl + "]");

											this.DEBUGG_LOGGER.addMassage("---- Bestell-Menge in dieser Bestellung: ["
													+ bestellMengeInXML + "]");

											this.DEBUGG_LOGGER.addMassage(
													"------------------------------------------------------------------");

										}
									} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

									}

								}

							}

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

							this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID,
									this.AGENT_RUN_CODE, this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME,
									this.ANZAHL_XML_OBJEKTE, this.DEBUGG_LOGGER.debug_string.toString(),
									this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]\n");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
									+ "\n");

							// ----------------------------------------------------------------------------------------------------------

						} else {

							this.DEBUGG_LOGGER
									.addMassage("Bestellung [" + nummer_der_bestellung + "] hat keine Positionen.");

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

							this.beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

							// Beende Agenten RUN
							// ----------------------------------------------------------------------------------------------------------

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

							this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID,
									this.AGENT_RUN_CODE, this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME,
									this.ANZAHL_XML_OBJEKTE, this.DEBUGG_LOGGER.debug_string.toString(),
									this.DB_CONNECTOR);

							// ----------------------------------------------------------------------------------------------------------

							this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
									+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

							this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
									+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

							// ----------------------------------------------------------------------------------------------------------

						}

					}

					if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						Path path = Paths.get(this.AGENT_WORK_FILE_XML.getAbsolutePath());

						BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

						String file_creation_time = format1
								.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

						String duplikat_file_name = "i_chee_duplikat_" + file_creation_time + "_" + this.AGENT_NAME
								+ "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME
								+ MANAGER_filepositionsindex + ".xml";

						File xml_file_duplikate = new File(
								this.AGENT_DUPLIKAT_PATH + SYSTEM_PATH_DELIMITER + duplikat_file_name);

						this.AGENT_WORK_FILE_XML.renameTo(xml_file_duplikate);

						// Beende Datei OBJEKT
						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>i_chee_DUPLIKATE<< VERSCHOBEN]");

						this.FILE_STATUS_ID = 4;

						this.beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID,
								this.FILE_STATUS_ID, this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE,
								this.AGENT_WORK_XML_FILE_NAME, AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

						this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
								+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " ");

						// ----------------------------------------------------------------------------------------------------------

						// ----------------------------------------------------------------------------------------------------------
						// Beende Agenten OBJEKT
						// ----------------------------------------------------------------------------------------------------------

						this.AGENT_END_PROCESS_TIME = new Date();

						this.beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, this.DB_CONNECTOR);

						// Beende Agenten RUN
						// ----------------------------------------------------------------------------------------------------------

						this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITUNG_ABGEBROCHEN_DUPLIKAT;

						this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
								this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
								this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

						// ----------------------------------------------------------------------------------------------------------

						this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
								+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]\n");

						this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "\n");

						// ----------------------------------------------------------------------------------------------------------

					}

				}

				// ==UPDATE---------------------------------------------------------------------------
				if (XML_CONTENT_IS_INSERT_TYPE == false) {

					this.FILE_CONTENT_ID = FileTypes.BESTELLUNG_UPDATE_EVENT_FILE;

					// Nur wenn dieser EIntrag existiert
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

						// Datei 2 (Update)
						// Bestellmenge:
						// 10
						// Geliefert:3
						// Artikel
						// bestellt:
						// (10-3)=7
						// Artikel
						// lager: 3
						// DIF= 10-3
						// =7
						// DIF2=
						// neuGeliefert-altegeliefert
						// = 3-0= 3
						// ---------------------

						// Datei 3 (Update)
						// Bestellmenge:
						// 10
						// Geliefert:5
						// Artikel
						// bestellt:
						// (10-([alt]3+[neu]2))=
						// 5
						// Artikel
						// lager:
						// [alt]-3+(alt]3+[neu]2)
						// = 5
						// DIF= 10-5
						// = 5
						// DIF2=
						// neuGeliefert-altegeliefert
						// = 5-3=2
						// ---------------------
						// Datei 4 (Update)
						// Bestellmenge:
						// 10
						// Geliefert:9
						// Artikel
						// bestellt:
						// (10-([alt]5+[neu]4))=
						// 1
						// Artikel
						// lager:
						// 5-[alt]5+(alt]5+[neu]4)
						// = 9
						// DIF= 10-9
						// = 1
						// DIF2=
						// neuGeliefert-altegeliefert
						// = 9-5 //
						// ---------------------

						boolean _alt_bestellung_ist_erledigt = false;
						String _alt_bestellung_erledigt_am_datum = "";
						boolean _alt_bestellung_ist_storniert = false;
						String _alt_lastlog = "";

						try {

							String tempDatumErledigtAusXML = "";
							Date datum_erledigt_bestellung_temp = null;

							tempDatumErledigtAusXML = bestell_kopf_element.getElementsByTagName("Date").item(0)
									.getTextContent().trim();

							datum_erledigt_bestellung_temp = XML_FORMAT_DATUM.parse(tempDatumErledigtAusXML);

							bestellung_erledigt_am_datum = formatdb.format(datum_erledigt_bestellung_temp);

						} catch (NullPointerException ecxc) {
							bestellung_erledigt_am_datum = "9999-09-09 00:00:00";
						} catch (ParseException e1) {

							bestellung_erledigt_am_datum = "9999-09-09 00:00:00";
							e1.printStackTrace();
						}

						try {

							bestellung_ist_erledigt = new Boolean(bestell_kopf_element
									.getElementsByTagName("BhErledigt").item(0).getTextContent().trim());

						} catch (NullPointerException ecxc) {
							bestellung_ist_erledigt = false;
						}

						try {

							bestellung_ist_storniert = new Boolean(bestell_kopf_element.getElementsByTagName("BhStorno")
									.item(0).getTextContent().trim());

						} catch (NullPointerException ecxc) {
							bestellung_ist_storniert = false;
						}

						// wenn die existente bestellung noch nicht als erledigt
						// Markiert wurde
						// Oder nicht storniert wurde

						String checkOldBestellung = "SELECT erledigt, storno, DATE_FORMAT(bestellung_erledigt_am, '%d.%M.%Y'), "
								+ " ifnull(last_agent_operation,'') FROM static_bestellung where interne_nummer_bestellung='"
								+ nummer_der_bestellung + "';";

						this.DB_CONNECTOR.readFromDatabase(checkOldBestellung, "checkOldBestellung");

						while (this.DB_CONNECTOR.rs.next()) {

							_alt_bestellung_ist_erledigt = this.DB_CONNECTOR.rs.getBoolean(1);
							_alt_bestellung_ist_storniert = this.DB_CONNECTOR.rs.getBoolean(2);
							_alt_bestellung_erledigt_am_datum = this.DB_CONNECTOR.rs.getString(3);
							_alt_lastlog = this.DB_CONNECTOR.rs.getString(4);
						}

						if (_alt_bestellung_ist_erledigt == false & _alt_bestellung_ist_storniert == false) {

							String newLog = "-#{UP_BEST_09879} Bestellung [ " + nummer_der_bestellung
									+ " ] wurde aktualisiert -  am: " + format2.format(new Date())
									+ "\n---- Storniert: [a=" + _alt_bestellung_ist_storniert + ",n="
									+ bestellung_ist_storniert + "]\n---- Erledigt: [a=" + _alt_bestellung_ist_erledigt
									+ ",n=" + bestellung_ist_erledigt + "]\n---- Erledigt Datum: [a="
									+ _alt_bestellung_erledigt_am_datum + ",n=" + bestellung_erledigt_am_datum
									+ "]\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
									+ this.MANAGER_RUN_CODE + "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]";

							String updateBestellungsKopf = "UPDATE static_bestellung set erledigt="
									+ bestellung_ist_erledigt + ", storno=" + bestellung_ist_storniert
									+ ", bestellung_erledigt_am='" + bestellung_erledigt_am_datum
									+ "', last_agent_operation='"
									+ this.LOG_CHECKER.generateNewLogForDB(_alt_lastlog, newLog)
									+ "', last_oparation_datum='" + formatdb.format(new Date())
									+ "' where interne_nummer_bestellung='" + nummer_der_bestellung + "';";

							this.DB_CONNECTOR.updateOnDatabase(updateBestellungsKopf);

							this.DEBUGG_LOGGER.addMassage("#{UP_BEST_09879} Bestellung  [ " + nummer_der_bestellung
									+ " ] wurde aktualisiert.\n---- Storniert: [a=" + _alt_bestellung_ist_storniert
									+ ",n=" + bestellung_ist_storniert + "]\n---- Erledigt: [a="
									+ _alt_bestellung_ist_erledigt + ",n=" + bestellung_ist_erledigt
									+ "]\n---- Erledigt Datum: [a=" + _alt_bestellung_erledigt_am_datum + ",n="
									+ bestellung_erledigt_am_datum + "]  -  Bearbeitet am: "
									+ format2.format(new Date()) + "\n---- A-RUN: [" + this.AGENT_RUN_CODE
									+ "]\n---- M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
									+ this.FILE_CREATION_CODE + "]");

							// =Verarbeite
							// Positionen---------------------------------------------------------------------

							NodeList bestellung_positionen = bestell_kopf_element.getElementsByTagName("Bp");

							this.ANZAHL_XML_OBJEKTE = bestellung_positionen.getLength();

							this.DEBUGG_LOGGER.addMassage(
									"--------------------------POSITIONEN---------------------------------");

							this.DEBUGG_LOGGER
									.addMassage("Bestellungs-Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------");

							if (this.ANZAHL_XML_OBJEKTE > 0) {

								for (int position_in_bestellung = 0; position_in_bestellung < bestellung_positionen
										.getLength(); position_in_bestellung++) {

									Node einzelne_positions_wurzel = bestellung_positionen.item(position_in_bestellung);

									if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

										Element bestellpositionElement = (Element) einzelne_positions_wurzel;

										_rec = 0;
										_interne_artikel_nummer = "";
										_bestellung_xml_neue_menge = 0;
										_bestellung_xml_menge_geliefert = 0;
										_bestellung_xml_neue_soll_menge = 0;

										_position_guid = "";

										try {

											BigDecimal menG = new BigDecimal(
													bestellpositionElement.getElementsByTagName("Rec").item(0)
															.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

											_rec = menG.intValue();

										} catch (NullPointerException ecxc) {
											_rec = 0;
										}

										try {

											_position_guid = bestellpositionElement.getElementsByTagName("GUID").item(0)
													.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

										} catch (NullPointerException ecxc) {
											_position_guid = "?";
										}

										try {

											_interne_artikel_nummer = bestellpositionElement
													.getElementsByTagName("ArtNr").item(0).getTextContent()
													.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "").trim();

										} catch (NullPointerException ecxc) {
											_interne_artikel_nummer = "?";
										}

										try {

											BigDecimal menG = new BigDecimal(bestellpositionElement
													.getElementsByTagName("Menge").item(0).getTextContent());

											_bestellung_xml_neue_menge = menG.intValue();

										} catch (NullPointerException ecxc) {
											_bestellung_xml_neue_menge = 0;
										}

										try {

											BigDecimal menG = new BigDecimal(bestellpositionElement
													.getElementsByTagName("MengeGeliefert").item(0).getTextContent());

											_bestellung_xml_menge_geliefert = menG.intValue();

										} catch (NullPointerException ecxc) {
											_bestellung_xml_menge_geliefert = 0;
										}

										try {

											BigDecimal menG = new BigDecimal(bestellpositionElement
													.getElementsByTagName("MengeSoll").item(0).getTextContent());

											_bestellung_xml_neue_soll_menge = menG.intValue();

										} catch (NullPointerException ecxc) {
											_bestellung_xml_neue_soll_menge = 0;
										}

										boolean position_existenz = checkBestellPosition(nummer_der_bestellung,
												_interne_artikel_nummer, _position_guid, _rec);

										// Datei 3 (Update)
										// Bestellmenge:
										// 10
										// Geliefert:5
										// Artikel
										// bestellt:
										// (10-([alt]3+[neu]2))=
										// 5
										// Artikel
										// lager:
										// [alt]-3+(alt]3+[neu]2)
										// = 5
										// DIF= 10-5
										// = 5
										// DIF2=
										// neuGeliefert-altegeliefert
										// = 5-3=2

										// UPDATE
										if (position_existenz == true) {

											int _alt_bestellung_menge_db = 0;
											int _alt_bestellung_geliefert_db = 0;
											int _alt_bestellung_soll_menge_db = 0;

											String _alt_last_operation_bestell_position = "";

											/// -----------------------------------------------------------------

											String sqlReadPositionBestellung1 = "Select menge_bestellt, menge_geliefert, menge_noch_zu_liefern, ifnull(agent_operation,'') from static_bestellung_positionen "
													+ " where bestellungnummer='" + nummer_der_bestellung
													+ "' and artikel_nummer='" + _interne_artikel_nummer
													+ "' and position=" + _rec + ";";

											this.DB_CONNECTOR.readFromDatabase(sqlReadPositionBestellung1,
													"sqlReadPositionBestellung1");

											while (this.DB_CONNECTOR.rs.next()) {
												_alt_bestellung_menge_db = this.DB_CONNECTOR.rs.getInt(1);
												_alt_bestellung_geliefert_db = this.DB_CONNECTOR.rs.getInt(2);
												_alt_bestellung_soll_menge_db = this.DB_CONNECTOR.rs.getInt(3);
												_alt_last_operation_bestell_position = this.DB_CONNECTOR.rs
														.getString(4);

											}

											// Additive arthimetische Operation

											// -------------------

											String _neu_last_operation_bestellung = "-#{UP_BEST_8998} BESTELL-POSITION UPDATE\n---- Bestellung: [ "
													+ nummer_der_bestellung + " ]\n---- Position GUID: ["
													+ _position_guid + "]\n---- Menge: [a=" + _alt_bestellung_menge_db
													+ ",n=" + _bestellung_xml_neue_menge + ", dif="
													+ (_bestellung_xml_neue_menge - _alt_bestellung_menge_db)
													+ "]\n---- Geliefert:[a=" + _alt_bestellung_geliefert_db + ",n="
													+ _bestellung_xml_menge_geliefert + ", dif="
													+ (_bestellung_xml_menge_geliefert - _alt_bestellung_geliefert_db)
													+ "]\n---- Sollmenge: [a=" + _alt_bestellung_soll_menge_db + ",n="
													+ _bestellung_xml_neue_soll_menge + ", dif="
													+ (_bestellung_xml_neue_soll_menge - _alt_bestellung_soll_menge_db)
													+ "]\n---- bearbeitet am: " + format2.format(new Date())
													+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
													+ this.MANAGER_RUN_CODE + "]\n---- DATEI: [" + FILE_CREATION_CODE
													+ "]";

											String UPDATEPosition = "UPDATE static_bestellung_positionen set menge_bestellt="
													+ _bestellung_xml_neue_menge + ", menge_geliefert="
													+ _bestellung_xml_menge_geliefert + ",menge_noch_zu_liefern="
													+ _bestellung_xml_neue_soll_menge + ", agent_operation='"
													+ this.LOG_CHECKER.generateNewLogForDB(
															_alt_last_operation_bestell_position,
															_neu_last_operation_bestellung)
													+ "', last_operation_datum='" + formatdb.format(new Date())
													+ "' where bestellungnummer='" + nummer_der_bestellung
													+ "' and artikel_nummer='" + _interne_artikel_nummer
													+ "' and position=" + _rec + "; ";

											if (_interne_artikel_nummer.contains("WP-") == false
													&& _interne_artikel_nummer.contains("WPK-") == false
													&& _interne_artikel_nummer != "" && _interne_artikel_nummer != "?"
													&& _interne_artikel_nummer.length() >= 5) {

												this.DB_CONNECTOR.updateOnDatabase(UPDATEPosition);

												String sqlbestellmenge_im_artikel = "SELECT bestellt_anzahl, lager_anzahl, agent_operation FROM static_artikel where interne_artikelnummer='"
														+ _interne_artikel_nummer + "';";

												this.DB_CONNECTOR.readFromDatabase(sqlbestellmenge_im_artikel,
														"sqlbestellmenge_im_artikel");

												int alt_bestellmenge_artikel = 0;
												int alt_lagerbestand_artikel = 0;

												String alt_logartikel_db = "";

												while (this.DB_CONNECTOR.rs.next()) {

													alt_bestellmenge_artikel = this.DB_CONNECTOR.rs.getInt(1);
													alt_lagerbestand_artikel = this.DB_CONNECTOR.rs.getInt(2);
													alt_logartikel_db = this.DB_CONNECTOR.rs.getString(3);

												}

												int dif2_geliefert_altGeliefert = (_bestellung_xml_menge_geliefert
														- _alt_bestellung_geliefert_db);

												int neu_menge_soll_in_bestellung = _bestellung_xml_neue_menge
														- _bestellung_xml_menge_geliefert;

												int neu_artikel_bestellt_menge = ((alt_bestellmenge_artikel
														+ _alt_bestellung_geliefert_db)
														- (_alt_bestellung_geliefert_db + dif2_geliefert_altGeliefert));

												int neu_gelieferte_menge = _bestellung_xml_menge_geliefert
														- _alt_bestellung_geliefert_db;

												int neu_artikel_lagerbestand = (alt_lagerbestand_artikel
														- _alt_bestellung_geliefert_db)
														+ (_alt_bestellung_geliefert_db + neu_gelieferte_menge);

												this.DEBUGG_LOGGER.addMassage("P[" + _rec + "] - Artikel ["
														+ _interne_artikel_nummer + "] wurde durch Bestellung: ["
														+ nummer_der_bestellung + "] aktualisiert!");

												this.DEBUGG_LOGGER
														.addMassage("---- Position GUID: [" + _position_guid + "]");

												this.DEBUGG_LOGGER.addMassage("---- Menge: [a="
														+ _alt_bestellung_menge_db + ",n=" + _bestellung_xml_neue_menge
														+ ", diff="
														+ (_bestellung_xml_neue_menge - _alt_bestellung_menge_db)
														+ "] ");

												this.DEBUGG_LOGGER
														.addMassage("---- Geliefert:[a=" + _alt_bestellung_geliefert_db
																+ ",n=" + _bestellung_xml_menge_geliefert + ", dif="
																+ (_bestellung_xml_menge_geliefert
																		- _alt_bestellung_geliefert_db)
																+ "]");

												this.DEBUGG_LOGGER.addMassage(
														"---- SollMenge: [a=" + _alt_bestellung_soll_menge_db + ",n="
																+ _bestellung_xml_neue_soll_menge + ", dif="
																+ (_bestellung_xml_neue_soll_menge
																		- _alt_bestellung_soll_menge_db)
																+ "]");

												this.DEBUGG_LOGGER.addMassage("---- Artikel Bestellmenge: [ a="
														+ alt_bestellmenge_artikel + ", n=" + neu_artikel_bestellt_menge
														+ ", dif="
														+ (neu_artikel_bestellt_menge - alt_bestellmenge_artikel)
														+ " ]");

												this.DEBUGG_LOGGER.addMassage("---- Artikel Bestellmenge Berechnung: ("
														+ alt_bestellmenge_artikel + "+" + _alt_bestellung_geliefert_db
														+ ")-(" + _alt_bestellung_geliefert_db + " + "
														+ dif2_geliefert_altGeliefert + ") = "
														+ neu_artikel_bestellt_menge + "");

												this.DEBUGG_LOGGER.addMassage("---- Artikel Lagerbestand Berechnung: ("
														+ alt_lagerbestand_artikel + "-" + _alt_bestellung_geliefert_db
														+ ")+(" + _alt_bestellung_geliefert_db + " + "
														+ neu_gelieferte_menge + ") = " + neu_artikel_lagerbestand
														+ "");

												this.DEBUGG_LOGGER.addMassage("---- Geliefert: [a="
														+ _alt_bestellung_geliefert_db + " ,n" + neu_gelieferte_menge
														+ ", dif="
														+ (neu_gelieferte_menge - _alt_bestellung_geliefert_db) + "]");

												this.DEBUGG_LOGGER.addMassage("---- Lagerbestand des Artikels: [ a="
														+ alt_lagerbestand_artikel + ", n=" + neu_artikel_lagerbestand
														+ ", dif="
														+ (neu_artikel_lagerbestand - alt_lagerbestand_artikel) + " ]");

												this.DEBUGG_LOGGER.addMassage("---- Bestellung Menge geliefert: ["
														+ _bestellung_xml_menge_geliefert + "]");

												this.DEBUGG_LOGGER.addMassage("---- Bestellung nicht geliefert: ["
														+ neu_menge_soll_in_bestellung + "]");

												this.DEBUGG_LOGGER.addMassage(
														"------------------------------------------------------------------");

												String neueArtikekLog = "-#{UP_BEST_7899} BESTELLUNG UPDATE - Bestellung: [ "
														+ nummer_der_bestellung + " ],\n---- Position GUID: ["
														+ _position_guid + "]\n---- Artikel Lagerbestand: [ a="
														+ alt_lagerbestand_artikel + ", n=" + neu_artikel_lagerbestand
														+ " ],\n---- Artikel Bestellmenge: [ a="
														+ alt_bestellmenge_artikel + ", n=" + neu_artikel_bestellt_menge
														+ " ],\n---- Geliefert aus Bestellung: [" + neu_gelieferte_menge
														+ "],\n---- Storniert: [a=" + _alt_bestellung_ist_storniert
														+ ",n=" + bestellung_ist_storniert + "], \n---- Erledigt: [a="
														+ _alt_bestellung_ist_erledigt + ",n=" + bestellung_ist_erledigt
														+ "]\n---- bearbeitet am: " + format2.format(new Date())
														+ "\n---- RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
														+ this.MANAGER_RUN_CODE + "]\n---- DATEI: ["
														+ FILE_CREATION_CODE + "]";

												String SQL_update_artikel = "Update static_artikel set " ///

														+ " lager_anzahl=" + neu_artikel_lagerbestand + ", " //
														+ " bestellt_anzahl=" + neu_artikel_bestellt_menge + ", " //
														+ " last_update='" + formatdb.format(new Date()) + "', " //
														+ " last_update_agent=" + this.AGENT_DBID
														+ ", agent_operation='"
														+ this.LOG_CHECKER.generateNewLogForDB(alt_logartikel_db,
																neueArtikekLog)
														+ "' where interne_artikelnummer='" + _interne_artikel_nummer
														+ "';";

												this.DB_CONNECTOR.updateOnDatabase(SQL_update_artikel);

											}

										}

										if (position_existenz == false) {

											try {

												if (_interne_artikel_nummer.contains("WP-") == false
														&& _interne_artikel_nummer.contains("WPK-") == false
														&& _interne_artikel_nummer != ""
														&& _interne_artikel_nummer != "?"
														&& _interne_artikel_nummer.length() >= 5) {

													this.DEBUGG_LOGGER.addMassage("P: [" + _rec + "] - Artikel: [ "
															+ _interne_artikel_nummer + " ] und Menge = "
															+ _bestellung_xml_neue_menge + "");

													String logdbold = "";
													String logdbneuArt = "";

													String SQLbereitsBestelltAusStaticArtikel = "Select bestellt_anzahl, IFNULL(agent_operation,''), lager_anzahl from static_artikel where interne_artikelnummer='"
															+ _interne_artikel_nummer + "';";

													int bereitsBestelltAusStaticArtikel = 0;

													int alt_lager_anzahl = 0;

													int neu_lager_anzahl = 0;

													this.DB_CONNECTOR.readFromDatabase(
															SQLbereitsBestelltAusStaticArtikel,
															"SQLbereitsBestelltAusStaticArtikel");

													while (this.DB_CONNECTOR.rs.next()) {

														bereitsBestelltAusStaticArtikel = this.DB_CONNECTOR.rs
																.getInt(1);

														logdbold = this.DB_CONNECTOR.rs.getString(2);

														alt_lager_anzahl = this.DB_CONNECTOR.rs.getInt(3);
													}

													// -------------------------------------

													// -------------------------------------

													int neuerBestellAnzahl = (bereitsBestelltAusStaticArtikel
															+ (_bestellung_xml_neue_menge
																	- _bestellung_xml_menge_geliefert));

													if (_alt_bestellung_ist_erledigt == false
															&& bestellung_ist_erledigt == true) {

														neu_lager_anzahl = (alt_lager_anzahl
																+ _bestellung_xml_menge_geliefert);

													} else {

														neu_lager_anzahl = alt_lager_anzahl;

														this.DEBUGG_LOGGER.addMassage("neu_lager_anzahl ["
																+ neu_lager_anzahl + "] = alt_lager_anzahl ["
																+ alt_lager_anzahl + "]");

													}

													logdbneuArt = "-#{IFU_BEST_0909} BESTELLUNG NEU, Bestellung: [ "
															+ nummer_der_bestellung + " ]\n---- Erledigt ["
															+ bestellung_ist_erledigt + "]\n---- Storno ["
															+ bestellung_ist_storniert + "]\n---- Position GUID: ["
															+ _position_guid + "]\n---- Artikel bestellt: [a="
															+ bereitsBestelltAusStaticArtikel + ", n= "
															+ neuerBestellAnzahl + "]\n---- Artikelbestand: a=["
															+ alt_lager_anzahl + "], n=[" + neu_lager_anzahl
															+ "]\n---- [ neu bestellt=" + _bestellung_xml_neue_menge
															+ " ]\n---- Geliefert: [" + _bestellung_xml_menge_geliefert
															+ "] \n---- bearbeitet am: " + format2.format(new Date())
															+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE
															+ "]\n---- M-RUN: [" + this.MANAGER_RUN_CODE
															+ "]\n---- DATEI: [" + FILE_CREATION_CODE + "].";

													String SQLneuerLagerBestand = "UPDATE static_artikel set bestellt_anzahl="
															+ neuerBestellAnzahl + ", lager_anzahl=" + neu_lager_anzahl
															+ ", last_update='" + formatdb.format(new Date())
															+ "', last_update_agent=" + AGENT_DBID
															+ ", agent_operation='"
															+ this.LOG_CHECKER.generateNewLogForDB(logdbold,
																	logdbneuArt)
															+ "' where interne_artikelnummer='"
															+ _interne_artikel_nummer + "';";

													this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

													this.DEBUGG_LOGGER
															.addMassage("---- Die Bestellmenge wurde im Artikel: [ "
																	+ _interne_artikel_nummer + " ] aktualisiert:");

													this.DEBUGG_LOGGER.addMassage(
															"---- Erledigt [" + bestellung_ist_erledigt + "]");

													this.DEBUGG_LOGGER.addMassage(
															"---- Storno [" + bestellung_ist_storniert + "]");

													this.DEBUGG_LOGGER
															.addMassage("---- Position GUID: [" + _position_guid + "]");

													this.DEBUGG_LOGGER.addMassage("---- Artikelbestand [a="
															+ alt_lager_anzahl + ", n=" + neu_lager_anzahl + "]");

													this.DEBUGG_LOGGER.addMassage("---- Bestellte Menge im Artikel [a="
															+ bereitsBestelltAusStaticArtikel + ", n="
															+ neuerBestellAnzahl + "]");

													this.DEBUGG_LOGGER
															.addMassage("---- Bestell-Menge in dieser Bestellung: "
																	+ _bestellung_xml_neue_menge + "");

													this.DEBUGG_LOGGER.addMassage("---- Geliefert-Menge in dieser XML: "
															+ _bestellung_xml_menge_geliefert + "");

													this.DEBUGG_LOGGER.addMassage(
															"------------------------------------------------------------------");

													String newLog2 = "-#{IFU_BEST_0909} INSERT BESTELLUNG POSITION NEU"
															+ " \n---- bearbeitet am: " + format2.format(new Date())
															+ "\n---- Bestellung: [ " + nummer_der_bestellung
															+ " ]\n---- Erledigt [" + bestellung_ist_erledigt
															+ "]\n---- Storno [" + bestellung_ist_storniert
															+ "]\n---- Position GUID: [" + _position_guid
															+ "]\n---- Bestellmenge im Artikel Lager: [ a="
															+ bereitsBestelltAusStaticArtikel + ", n= "
															+ neuerBestellAnzahl + " ]\n---- Artikelbestand [a="
															+ alt_lager_anzahl + ", n=" + neu_lager_anzahl
															+ "]\n---- neu bestellt [" + _bestellung_xml_neue_menge
															+ " ]\n---- Geliefert [" + _bestellung_xml_menge_geliefert
															+ "]\n---- Soll-Menge [" + _bestellung_xml_neue_soll_menge
															+ "]\n---- A-RUN: [" + this.AGENT_RUN_CODE
															+ "]\n---- M-RUN: [" + this.MANAGER_RUN_CODE
															+ "].\n---- DATEI: [" + FILE_CREATION_CODE + "].";

													// Der Eintrag wird
													// gespeichert

													String sqlPositionEinfuegen = "INSERT INTO static_bestellung_positionen (" //
															+ " position, bestellungnummer,artikel_nummer,"
															+ " menge_bestellt, menge_geliefert, menge_noch_zu_liefern, agent_operation, last_operation_datum, guid_position ) VALUES ("
															+ _rec + ",'" + nummer_der_bestellung + "','"
															+ _interne_artikel_nummer + "',"
															+ _bestellung_xml_neue_menge + ","
															+ _bestellung_xml_menge_geliefert + ","
															+ _bestellung_xml_neue_soll_menge + ",'"
															+ this.LOG_CHECKER.generateNewLogForDB("", newLog2) + "','"
															+ formatdb.format(new Date()) + "','" + _position_guid
															+ "');";

													this.DB_CONNECTOR.insertToDatabase(sqlPositionEinfuegen,
															"sqlPositionEinfuegen");

												}

											} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ui) {

											}

										}

									}

								}

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
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
										+ "");

								// ----------------------------------------------------------------------------------------------------------

							} else {

								this.DEBUGG_LOGGER
										.addMassage("Bestellung [" + nummer_der_bestellung + "] hat keine Positionen.");

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
										+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
										+ "");

								// ----------------------------------------------------------------------------------------------------------
							}

						} else {

							this.DEBUGG_LOGGER.addMassage(
									"---------------------------------------------------------------------------");
							this.DEBUGG_LOGGER
									.addMassage("Bestellung [" + nummer_der_bestellung + "] ist bereits erledigt.");

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
									"---------------------------------------------------------------------------");

							// ----------------------------------------------------------------------------------------------------------

						}

					}

					// Wenn nicht dann verschiebe es in die Warteschlange
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

						this.DEBUGG_LOGGER.addMassage("Es wurde kein Insert-Eintrag gefunden zu der Bestellung: ["
								+ nummer_der_bestellung + "] - Datei: [" + this.FILE_CREATION_CODE + "]");

						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>u_chee_STACK<< VERSCHOBEN]");

						this.DEBUGG_LOGGER.addMassage("Datenbank Eintrag der Datei wird geloescht");

						this.DB_CONNECTOR.updateOnDatabase(
								"DELETE from work_agent_files where file_creation_code='" + FILE_CREATION_CODE + "';");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						String system_stack_file_name = "u_chee_" + (MANAGER_filepositionsindex + 1) + "_"
								+ this.AGENT_FILE_NAME_PATTERN + "_stack" + this.AUSWAHL_MANDANT_NAME
								+ FILE_CREATION_CODE + ".xml";

						File stack_xml_file = new File(
								this.AGENT_FILE_STACK_PATH + SYSTEM_PATH_DELIMITER + system_stack_file_name);

						AGENT_WORK_FILE_XML.renameTo(stack_xml_file);

						// Beende Datei OBJEKT
						// ----------------------------------------------------------------------------------------------------------

						SINGEL_FILE_PROCESS_END_TIME = new Date();

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

						// ----------------------------------------------------------------------------------------------------------

					}

				}

			}

			else {

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

	public int boolTOInteger(boolean val) {

		int temp = 0;

		if (val == true) {
			temp = 1;
		} else {
			temp = 0;
		}

		return temp;
	}

	public boolean existiert_diese_bestellung(String _bestellnummer) throws SQLException {

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

	public boolean checkBestellPosition(String _bestellnummer, String _artikelnummer, String position_guid, int rec)
			throws SQLException {

		String sqlCheckExsistenz = "SELECT EXISTS (SELECT * FROM static_bestellung_positionen WHERE bestellungnummer='"
				+ _bestellnummer + "' and guid_position='" + position_guid + "');";

		boolean temp = true;

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;

	}
}
