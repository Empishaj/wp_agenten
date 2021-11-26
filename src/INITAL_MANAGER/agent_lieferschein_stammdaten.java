package INITAL_MANAGER;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class agent_lieferschein_stammdaten extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 1395996086610670710L;

	public agent_lieferschein_stammdaten(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter) throws SQLException, ClassNotFoundException, IOException,
			ParserConfigurationException, SAXException, ParseException {

		this.AGENT_NAME = "LIINMA";

		this.AGENT_DBID = 666;

		this.FILE_CONTENT_DBID = 20;

		this.AGENT_RUN_CREATION_CODE = ZUFALLS_GENERATOR.newRandomCode();

		this.RUN_MANAGER_DBID = _managerid;

		this.MANAGER_RUN_CREATION_CODE = _manager_run_creation_code;

		this.IS_LINUX = _isLinux;

		this.OPERATION_SYSTEM = _opsystem;

		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;

		this.ANZAHL_XML_OBJEKTE_GESAMT = 0;

		this.AGENT_DEBUG_INFO = new Debugger(this.DEBUGGER_STATUS);

	}

	public void start()

			throws SQLException, ClassNotFoundException, IOException, SAXException, ParserConfigurationException,
			ParseException {

		this.AGENT_START_PROCESS_TIME = new Date();

		String sql001 = "SELECT agentname_kurz, "
				+ " COALESCE(last_prozess_time, 'noch nie'), activity, file_name_pattern, truncate_table_before_insert, truncate_table_before_insert_single_file, debug_status,manager_start_position, sql_max_collection_size  FROM agent where dbid="
				+ AGENT_DBID + ";";

		this.DB_CONNECTOR.readFromDatabase(sql001, "initial_auftra_agent");

		while (this.DB_CONNECTOR.rs.next()) {

			this.AGENT_NAME = DB_CONNECTOR.rs.getString(1);
			this.AGENT_LAST_PROZESS_TIME = DB_CONNECTOR.rs.getString(2);
			this.AGENT_ACTIVITY = this.DB_CONNECTOR.rs.getBoolean(3);
			this.AGENT_FILE_NAME_PATTERN = DB_CONNECTOR.rs.getString(4);
			this.TRUNCATE_TABLE_BEFORE_INSERT = DB_CONNECTOR.rs.getBoolean(5);
			this.TRUNCATE_TABLE_BEFORE_INSERT_SINGLE_FILE = DB_CONNECTOR.rs.getBoolean(6);
			this.DEBUGGER_STATUS = DB_CONNECTOR.rs.getBoolean(7);
			this.MANAGER_START_POSITION = DB_CONNECTOR.rs.getInt(8);
			this.SQL_MAX_COLLECTION_SIZE = DB_CONNECTOR.rs.getInt(9);

		}

		if (IS_LINUX == false) {

			String sql002 = "SELECT a.java_parameter_value as system_paths_deliver_win, b.java_parameter_value  as system_paths_work_win,c.java_parameter_value  as system_paths_proccesed_win,"
					+ " d.java_parameter_value  as system_paths_logs_win from system_parameter as a, system_parameter as b,system_parameter as c,system_parameter as d where a.java_parameter_name='system_paths_deliver_win' "
					+ " and b.java_parameter_name='system_paths_work_win' and c.java_parameter_name='system_paths_processed_win' and d.java_parameter_name='system_paths_logs_win' ;";

			this.DB_CONNECTOR.readFromDatabase(sql002, "iiasa_initial_import_artikel_stammdaten_agent");

			while (this.DB_CONNECTOR.rs.next()) {

				this.AGENT_FILE_DELIVER_PATH = DB_CONNECTOR.rs.getString(1);
				this.AGENT_FILE_WORK_PATH = DB_CONNECTOR.rs.getString(2);
				this.AGENT_FILE_PROCESSED_PATH = DB_CONNECTOR.rs.getString(3);
				this.AGENT_FILE_LOGFILE_PATH = DB_CONNECTOR.rs.getString(4);

			}

		}

		if (IS_LINUX == true) {
			String sql002 = "SELECT a.java_parameter_value as system_paths_deliver_win, b.java_parameter_value  as system_paths_work_win,c.java_parameter_value  as system_paths_proccesed_win,"
					+ " d.java_parameter_value  as system_paths_logs_win from system_parameter as a, system_parameter as b,system_parameter as c,system_parameter as d where a.java_parameter_name='system_paths_deliver_linux' "
					+ " and b.java_parameter_name='system_paths_work_linux' and c.java_parameter_name='system_paths_processed_linux' and d.java_parameter_name='system_paths_logs_linux' ;";

			this.DB_CONNECTOR.readFromDatabase(sql002, "iiasa_initial_import_artikel_stammdaten_agent");

			while (this.DB_CONNECTOR.rs.next()) {

				this.AGENT_FILE_DELIVER_PATH = DB_CONNECTOR.rs.getString(1);
				this.AGENT_FILE_WORK_PATH = DB_CONNECTOR.rs.getString(2);
				this.AGENT_FILE_PROCESSED_PATH = DB_CONNECTOR.rs.getString(3);
				this.AGENT_FILE_LOGFILE_PATH = DB_CONNECTOR.rs.getString(4);

			}

		}

		DB_CONNECTOR.updateOnDatabase(
				"INSERT INTO agent_run_logs (agenten_id, manager_id, run_creation_code, start_time, status_id) VALUES ("
						+ AGENT_DBID + "," + RUN_MANAGER_DBID + ",'" + this.AGENT_RUN_CREATION_CODE + "','"

						+ db_date_formate.format(AGENT_START_PROCESS_TIME) + "', " + this.AGENT_STATUS_CURRENT_CODE
						+ ");");

		this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_INITIALISIERUNG;

		DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE
				+ " where agenten_id=" + this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

		if (this.AGENT_ACTIVITY == true)

		{

			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			DB_CONNECTOR.updateOnDatabase(
					"UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id="
							+ this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

			this.AGENT_DEBUG_INFO.addMassage("[ START " + this.AGENT_NAME + " ]\n");

			// Dieser Pfad kann in der Datenbank geaendert werden
			File deliver_dir = new File(this.AGENT_FILE_DELIVER_PATH);

			if (deliver_dir.exists() == true) {

				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File directory, String fileName) {
						return fileName.contains(AGENT_FILE_NAME_PATTERN);
					}
				};

				File[] deliverd_files = deliver_dir.listFiles(filter);

				File deliverd_xml_file = null;

				long file_dateigroesse = 0;

				String file_creation_time = "";

				ANZAHL_VERARBEITETE_XML_DATEIEN = deliverd_files.length;

				if (ANZAHL_VERARBEITETE_XML_DATEIEN > 0) {

					String file_creation_code = ZUFALLS_GENERATOR.newRandomCode();

					this.AGENT_DEBUG_INFO.addMassage("[DATEI - " + file_creation_code
							+ " - VERARBEITUNG STARTET] Es wurde/n " + deliverd_files.length + " Datei/en gefunden \n");

					for (int i = 0; i < deliverd_files.length; i++) {

						this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

						DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set log_content='"
								+ this.AGENT_DEBUG_INFO.debugString.toString() + "', status_id="
								+ this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
								+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

						int agentrunid = 0;

						String sqlFindId = "Select id from agent_run_logs where run_creation_code='"
								+ AGENT_RUN_CREATION_CODE + "'";

						DB_CONNECTOR.readFromDatabase(sqlFindId, "sqlFindId");

						while (DB_CONNECTOR.rs.next()) {

							agentrunid = DB_CONNECTOR.rs.getInt(1);
						}

						this.AGENT_DEBUG_INFO.addMassage(
								"-------------------------------------------------------------------------------- \n");

						if (deliverd_files[i].isFile()) {

							// Die Laufendenummer wird fuer jede neue Datei
							// zurueck gesetzt

							anzahl_XML_objekte_pro_datei = 0;
							anzahl_erzeugter_SQL_objekte_der_datei = 0;

							FILE_PROCESS_START_TIME = new Date();

							deliverd_xml_file = deliverd_files[i].getAbsoluteFile();

							Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

							// Die Attribute der Datei werden ermittelt
							BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

							file_dateigroesse = attr.size();

							GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault());

							calendar.setTimeInMillis(attr.creationTime().toMillis());

							file_creation_time = db_date_formate.format(calendar.getTime());

							AUSWAHL_MANDANT_DBID = 0;

							if (deliverd_xml_file.getName().contains("_wp_")) {
								this.AUSWAHL_MANDANT_DBID = this.WP_MANDANT_DBID;
								this.AUSWAHL_MANDANT_NAME = "_wp_";

							}

							if (deliverd_xml_file.getName().contains("_ka_")) {
								this.AUSWAHL_MANDANT_DBID = this.KA_MANDANT_DBID;
								this.AUSWAHL_MANDANT_NAME = "_ka_";
							}

							String sql_insert_file = "Insert into " //
									+ " work_agent_files (" //
									+ " agent_id," //
									+ " filecontent_id, " //
									+ " deliver_filename, " //
									+ " prozess_begin_time," //
									+ " run_creation_code, " //
									+ " file_status_id, "//

									+ " dateigroesse,"//

									+ " erstelldatum,file_creation_code,mandant_id ) VALUES (" //
									+ AGENT_DBID + "," //
									+ FILE_CONTENT_DBID + ",'" //
									+ deliverd_xml_file.getName() + "','" //
									+ db_date_formate.format(FILE_PROCESS_START_TIME) + "', '" //
									+ this.AGENT_RUN_CREATION_CODE + "'," //
									+ 1 + " , " //
									+ file_dateigroesse + ",'" //
									+ //
									file_creation_time + "','" //
									+ file_creation_code + "'," + this.AUSWAHL_MANDANT_DBID + ");";

							DB_CONNECTOR.insertToDatabase(sql_insert_file,
									"Lieferschein Initial Import > Single File Insert DB");

							int work_file_db_id = 0;

							String sql_find_work_file_db_id = "SELECT id FROM work_agent_files where file_creation_code='"
									+ file_creation_code + "';";

							DB_CONNECTOR.readFromDatabase(sql_find_work_file_db_id, "sql_find_work_file_db_id");

							while (DB_CONNECTOR.rs.next()) {

								work_file_db_id = DB_CONNECTOR.rs.getInt(1);
							}

							this.AGENT_DEBUG_INFO.addMassage("- [DATEI-NR.: " + (i + 1) + "] wird verarbeitet. \n");

							String system_work_file_name = this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
									+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
									+ this.AGENT_FILE_NAME_PATTERN + "_"
									+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_" + (i + 1) + "_"
									+ file_creation_code + "_work.xml";

							File work_xml_file = new File(
									this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + system_work_file_name);

							deliverd_xml_file.renameTo(work_xml_file);

							this.AGENT_DEBUG_INFO.addMassage(
									"- [DATEI WIRD VOM DELIVER ORDNER ABGEHOLT] > Datei wurde in dem >>WORK<< ORDNER verschoben \n");

							// Der Creation Code wird benoetigt um nach dem
							// Insert
							// den Datensatz zu aktualisieren
							// Damit Spartman eine Abfrage um die ID zu
							// ermitteln

							DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
							DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
							Document doc = dBuilder.parse(work_xml_file);

							doc.getDocumentElement().normalize();

							NodeList alle_lieferscheine_liste = doc.getElementsByTagName("Lh");

							this.AGENT_DEBUG_INFO.addMassage("- [XML-DATEI] > Die XML-Objekte werden eingelesen \n");

							for (int position_liste = 0; position_liste < alle_lieferscheine_liste
									.getLength(); position_liste++) {

								String lieferschein_nummer = "";
								String guid = "";
								String lieferschein_datum = "";
								String rechnungsnummer = "";
								String auftragsnummer = "";
								boolean faktura = false;
								boolean gebucht = false;

								Node single_lieferschein = alle_lieferscheine_liste.item(position_liste);

								if (single_lieferschein.getNodeType() == Node.ELEMENT_NODE) {

									Element eElement_lieferschein = (Element) single_lieferschein;

									try {

										rechnungsnummer = eElement_lieferschein.getElementsByTagName("RefRechnung")
												.item(0).getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9+-]", "");

										if (rechnungsnummer == "") {
											rechnungsnummer = "?";
										}

									} catch (java.lang.NullPointerException e) {
										rechnungsnummer = "?";
									}

									try {

										lieferschein_nummer = eElement_lieferschein.getElementsByTagName("Nr").item(0)
												.getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

									} catch (java.lang.NullPointerException e) {
										lieferschein_nummer = "?";
									}

									try {

										guid = eElement_lieferschein.getElementsByTagName("GUID").item(0)
												.getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9+-]", "");

									} catch (java.lang.NullPointerException e) {
										guid = "?";
									}

									try {

										String tempDatumAusXML = "";
										Date datum_temp = null;

										tempDatumAusXML = eElement_lieferschein.getElementsByTagName("Date").item(0)
												.getTextContent().trim();

										datum_temp = short_rewert_format.parse(tempDatumAusXML);

										lieferschein_datum = db_date_formate.format(datum_temp);

									} catch (java.lang.NullPointerException e) {
										lieferschein_datum = "9999-09-09 00:00:00";
									}

									try {

										auftragsnummer = eElement_lieferschein.getElementsByTagName("RefAuftrag")
												.item(0).getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9+-]", "");

									} catch (java.lang.NullPointerException e) {
										auftragsnummer = "?";
									}

									try {

										faktura = new Boolean(eElement_lieferschein.getElementsByTagName("LhDoFak")
												.item(0).getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9+-]", ""));

									} catch (java.lang.NullPointerException e) {
										faktura = false;
									}

									try {

										gebucht = new Boolean(eElement_lieferschein.getElementsByTagName("LhGebucht")
												.item(0).getTextContent().trim().replace(" ", "-")
												.replaceAll("[^A-Za-z0-9+-]", ""));

									} catch (java.lang.NullPointerException e) {
										gebucht = false;
									}

									if (rechnungsnummer == "?") {

										String SQLfuegeLieferscheinKopf = "INSERT INTO static_lieferschein(" //
												+ " lieferschein_guid," //
												+ " lieferschein_nummer," //
												+ " auftrags_nummer,"//
												+ " datum_der_lieferung," //
												+ " last_agent_operation," //
												+ " last_oparation_datum," //
												+ " last_agent_id," //
												+ " insert_work_file_id,"//
												+ " faktura," //
												+ " gebucht,"//
												+ " last_agentruncode,"//
												+ " last_managerruncode, " //
												+ " rechnungsnummer,mandant_id)  VALUES   ('" + guid + "','" //
												+ lieferschein_nummer + "','" //
												+ auftragsnummer + "','" + lieferschein_datum
												+ "','- INSERT LIEFERSCHEIN NEU, Lieferscheinnummer: ["
												+ lieferschein_nummer + "], am: "
												+ long_datum_zeit_formate.format(new Date()) + " von A-RUN: [" //
												+ this.AGENT_RUN_CREATION_CODE + "] - Datei: [" + file_creation_code
												+ "]\n\n','" + db_date_formate.format(new Date()) + "'," + agentrunid
												+ "," + work_file_db_id + "," + faktura + ", " + gebucht + ", '"
												+ this.AGENT_RUN_CREATION_CODE + "','?','" + rechnungsnummer + "',"
												+ this.AUSWAHL_MANDANT_DBID + ");";

										try {
											DB_CONNECTOR.insertToDatabase(SQLfuegeLieferscheinKopf,
													" SQLfuegeLieferscheinKopf");

											NodeList lieferschein_positionen = eElement_lieferschein
													.getElementsByTagName("Lp");

											if (lieferschein_positionen.getLength() > 0) {

												for (int p = 0; p < lieferschein_positionen.getLength(); p++) {

													Node auftragPosition = lieferschein_positionen.item(p);

													if (auftragPosition.getNodeType() == Node.ELEMENT_NODE) {

														Element PositionsElement = (Element) auftragPosition;

														String artikelnummer = "";
														int menge = 0;
														int menge_lager = 0;
														int menge_fak = 0;
														int menge_lieferschein = 0;
														int rec = 0;

														try {

															artikelnummer = PositionsElement
																	.getElementsByTagName("ArtNr").item(0)
																	.getTextContent().trim().replace(" ", "-")
																	.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]",
																			"");

														} catch (java.lang.NullPointerException e) {
															artikelnummer = "?";
														}

														try {

															BigDecimal men = new BigDecimal(PositionsElement
																	.getElementsByTagName("Position").item(0)
																	.getTextContent().replaceAll("[^A-Za-z0-9+-]", ""));

															rec = men.intValue();

														} catch (java.lang.NullPointerException e) {
															rec = 0;
														}

														try {

															BigDecimal men = new BigDecimal(
																	PositionsElement.getElementsByTagName("Menge")
																			.item(0).getTextContent());

															menge = men.intValue();

														} catch (java.lang.NullPointerException e) {

															menge = 0;
														}

														try {

															BigDecimal men = new BigDecimal(
																	PositionsElement.getElementsByTagName("MengeLager")
																			.item(0).getTextContent());

															menge_lager = men.intValue();

														} catch (java.lang.NullPointerException e) {

															menge_lager = 0;
														}

														try {

															BigDecimal men = new BigDecimal(
																	PositionsElement.getElementsByTagName("MengeFak")
																			.item(0).getTextContent());

															menge_fak = men.intValue();

														} catch (java.lang.NullPointerException e) {

															menge_fak = 0;
														}

														try {

															BigDecimal men = new BigDecimal(
																	PositionsElement.getElementsByTagName("MengeLp")
																			.item(0).getTextContent());

															menge_lieferschein = men.intValue();

														} catch (java.lang.NullPointerException e) {

															menge_lieferschein = 0;
														}

														String sqlPositionEinfuegen = "INSERT INTO static_lieferschein_positionen (" //
																+ " position, " //
																+ " lieferschein_nummer, " //
																+ " artikel_nummer,"//
																+ " menge,"//
																+ " mengelager, "//
																+ " mege_faktura, "//
																+ " menge_lieferschein, "//

																+ " last_operation, "//
																+ " last_operation_datum) VALUES (" + rec + ",'"//
																+ lieferschein_nummer + "','" //
																+ artikelnummer + "'," //
																+ menge + "," + menge_lager + "," + menge_fak + ","
																+ menge_lieferschein + ", "
																+ "'- INSERT LIEFERSCHEIN POSITION NEU, Lieferschein: ["
																+ lieferschein_nummer//

																+ "], am: " + long_datum_zeit_formate.format(new Date())//
																+ " von A-RUN: [" //
																+ this.AGENT_RUN_CREATION_CODE + "] - Datei: ["
																+ file_creation_code + "]\n','"
																+ db_date_formate.format(new Date()) + "');";

														try {
															if (artikelnummer.contains("WP-") == false
																	&& artikelnummer.contains("WPK-") == false
																	&& artikelnummer != "" && artikelnummer != "?"
																	&& artikelnummer.length() >= 5) {

																DB_CONNECTOR.insertToDatabase(sqlPositionEinfuegen,
																		"sqlPositionEinfuegen");

															}
														} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

														}
													}
												}

											}

										} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

										}

									}

								}

							}

							FILE_PROCESS_END_TIME = new Date();

							this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
									.toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

							this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] >  "
									+ this.single_file_full_process_diff_time_in_sec + " Sekunden =  "
									+ (this.single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");

							AGENT_PROCESSED_XML_FILE_NAME = this.getSystem_processed_file_name(i);

							File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER
									+ AGENT_PROCESSED_XML_FILE_NAME);

							work_xml_file.renameTo(processed_xml_file);

							this.AGENT_DEBUG_INFO.addMassage("[DATEI WIRD NACH >> PROCESSED << VERSCHOBEN] \n");

							DB_CONNECTOR.updateOnDatabase("UPDATE work_agent_files set prozess_end_time='"
									+ db_date_formate.format(FILE_PROCESS_END_TIME) + "', file_status_id=3,"
									+ " datensaetze_anzahl=" + anzahl_XML_objekte_pro_datei + ", work_filename='"
									+ system_work_file_name + "', processed_filename='" + AGENT_PROCESSED_XML_FILE_NAME
									+ "' " //
									+ " where file_creation_code='" + file_creation_code + "';");

						} else {

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_AGENT_HAT_KEINE_DATEN;

							this.AGENT_END_PROCESS_TIME = new Date();

							this.AGENT_DEBUG_INFO
									.addMassage("[AGENT] Im verzeichnis sind keine Daten fuer den Agenten \n");

							DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set log_content='"
									+ this.AGENT_DEBUG_INFO.debugString.toString() + "', end_time='"
									+ db_date_formate.format(AGENT_END_PROCESS_TIME) + "', datensatz_gesamt_anzahl=" + 0
									+ " ,datei_bearbeitet_anzahl=" + 0 + ", status_id=" + this.AGENT_STATUS_CURRENT_CODE
									+ " where agenten_id=" + this.AGENT_DBID + " and run_creation_code='"
									+ AGENT_RUN_CREATION_CODE + "';");
						}

					}

				} else {

					this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_AGENT_HAT_KEINE_DATEN;

					this.AGENT_END_PROCESS_TIME = new Date();

					this.AGENT_DEBUG_INFO.addMassage("[AGENT] Das Verzeichnis ist leer  \n");

					DB_CONNECTOR.updateOnDatabase(
							"UPDATE agent_run_logs set log_content='" + this.AGENT_DEBUG_INFO.debugString.toString()
									+ "', end_time='" + db_date_formate.format(AGENT_END_PROCESS_TIME)
									+ "', datensatz_gesamt_anzahl=" + 0 + " ,datei_bearbeitet_anzahl=" + 0
									+ ", status_id=" + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id="
									+ this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

				}

			} else {

				this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_DELIVER_ORDNER_EXISITERT_NICHT;

				this.AGENT_DEBUG_INFO.addMassage("[UEBERGABE ORDNER] Pfad wurde nicht gefunden \n");

				this.AGENT_END_PROCESS_TIME = new Date();

				DB_CONNECTOR.updateOnDatabase(
						"UPDATE agent_run_logs set log_content='" + this.AGENT_DEBUG_INFO.debugString.toString()
								+ "', end_time='" + db_date_formate.format(AGENT_END_PROCESS_TIME)
								+ "', datensatz_gesamt_anzahl=" + 0 + " ,datei_bearbeitet_anzahl=" + 0 + ", status_id="
								+ this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
								+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

			}

		} else {

			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_DEAKTIV;

			this.AGENT_END_PROCESS_TIME = new Date();

			this.AGENT_DEBUG_INFO.addMassage("[" + AGENT_NAME + "] - STATUS=deaktiviert \n");
			this.AGENT_DEBUG_INFO
					.addMassage("[" + AGENT_NAME + "] - STATUS aendern > [DB] > Tabelle=agent, Col=activity, Set=1 \n");

			DB_CONNECTOR.updateOnDatabase(
					"UPDATE agent_run_logs set log_content='" + this.AGENT_DEBUG_INFO.debugString.toString()
							+ "', end_time='" + db_date_formate.format(AGENT_END_PROCESS_TIME)
							+ "', datensatz_gesamt_anzahl=" + 0 + " ,datei_bearbeitet_anzahl=" + 0 + ", status_id="
							+ this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
							+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

		}

	}

}
