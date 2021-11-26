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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class agent_warengruppen_stammdaten extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = -3600785818535498538L;

	public agent_warengruppen_stammdaten(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter) {

		this.AGENT_NAME = "WINIMA";
		this.AGENT_DBID = 661;
		this.FILE_CONTENT_DBID = 3;

		this.AGENT_RUN_CREATION_CODE = ZUFALLS_GENERATOR.newRandomCode();

		this.RUN_MANAGER_DBID = _managerid;

		this.MANAGER_RUN_CREATION_CODE = _manager_run_creation_code;

		this.IS_LINUX = _isLinux;
		this.OPERATION_SYSTEM = _opsystem;

		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;

		this.AGENT_DEBUG_INFO = new Debugger(this.DEBUGGER_STATUS);

	}

	public void start()
			throws SQLException, ClassNotFoundException, IOException, SAXException, ParserConfigurationException {

		this.AGENT_START_PROCESS_TIME = new Date();

		String sql001 = " SELECT agentname_kurz, "
				+ " COALESCE(last_prozess_time, 'noch nie'), activity, file_name_pattern, truncate_table_before_insert, truncate_table_before_insert_single_file, debug_status,manager_start_position FROM agent where dbid="
				+ AGENT_DBID + ";";

		this.DB_CONNECTOR.readFromDatabase(sql001, "event_agent_artikel ");

		while (this.DB_CONNECTOR.rs.next()) {

			this.AGENT_NAME = DB_CONNECTOR.rs.getString(1);
			this.AGENT_LAST_PROZESS_TIME = DB_CONNECTOR.rs.getString(2);
			this.AGENT_ACTIVITY = this.DB_CONNECTOR.rs.getBoolean(3);
			this.AGENT_FILE_NAME_PATTERN = DB_CONNECTOR.rs.getString(4);
			this.TRUNCATE_TABLE_BEFORE_INSERT = DB_CONNECTOR.rs.getBoolean(5);
			this.TRUNCATE_TABLE_BEFORE_INSERT_SINGLE_FILE = DB_CONNECTOR.rs.getBoolean(6);
			this.DEBUGGER_STATUS = DB_CONNECTOR.rs.getBoolean(7);
			this.MANAGER_START_POSITION = DB_CONNECTOR.rs.getInt(8);

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

		if (this.AGENT_ACTIVITY == true) {

			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			DB_CONNECTOR.updateOnDatabase(
					"UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id="
							+ this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

			this.AGENT_DEBUG_INFO.addMassage("[ START " + this.AGENT_NAME + "]");

			/*
			 * -----------------------------------------------------------------
			 * 
			 */

			// Dieser Pfad kann in der Datenbank geaendert werden
			File deliver_dir = new File(this.AGENT_FILE_DELIVER_PATH);

			if (deliver_dir.exists() == true) {

				this.AGENT_DEBUG_INFO.addMassage("[UEBERGABE-ORDNER] Pfad wurde gefunden");

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

					this.AGENT_DEBUG_INFO.addMassage("Es wurde/n " + deliverd_files.length + " Datei/en gefunden");

					for (int i = 0; i < deliverd_files.length; i++) {

						this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

						DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set log_content='"
								+ this.AGENT_DEBUG_INFO.debugString.toString() + "', status_id="
								+ this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
								+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

						this.AGENT_DEBUG_INFO.addMassage(
								"----------------------------------------------------------------------------------------------------------------");
						if (deliverd_files[i].isFile()
								&& deliverd_files[i].getName().contains(AGENT_FILE_NAME_PATTERN)) {

							try {

								this.AGENT_DEBUG_INFO.addMassage(
										"[DATEI-VERARBEITUNG STARTET - DATEI: " + file_creation_code + " ]");

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

										+ " erstelldatum,file_creation_code , mandant_id) VALUES (" //
										+ AGENT_DBID + "," //
										+ FILE_CONTENT_DBID + ",'" //
										+ deliverd_xml_file.getName() + "','" //
										+ db_date_formate.format(FILE_PROCESS_START_TIME) + "', '" //
										+ this.AGENT_RUN_CREATION_CODE + "'," //
										+ 1 + " , " //
										+ file_dateigroesse + ",'" //
										+ //
										file_creation_time + "','" //
										+ file_creation_code + "'," + this.AUSWAHL_MANDANT_DBID + " );";

								DB_CONNECTOR.insertToDatabase(sql_insert_file,
										"Artikel Initial Import > Single File Insert DB");

								this.AGENT_DEBUG_INFO.addMassage("-[DATEI-NR.: " + (i + 1) + "] - "
										+ deliverd_xml_file.getName() + " wird verarbeitet.");

								String system_work_file_name = this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
										+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
										+ this.AGENT_FILE_NAME_PATTERN + "_"
										+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_" + (i + 1) + "_"
										+ file_creation_code + "_work.xml";

								File work_xml_file = new File(
										this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + system_work_file_name);

								deliverd_xml_file.renameTo(work_xml_file);

								this.AGENT_DEBUG_INFO.addMassage("--[DATEI WIRD VOM DELIVER ORDNER ABGEHOLT] > Datei: "
										+ deliverd_xml_file.getName() + " wurde in dem >> WORK << ORDNER verschoben");

								this.AGENT_DEBUG_INFO.addMassage("---[DATEI WIRD UMBENANNT] > Datei: "
										+ deliverd_xml_file.getName() + " wird zu " + system_work_file_name + "");

								// Der Creation Code wird benoetigt um nach dem
								// Insert
								// den Datensatz zu aktualisieren
								// Damit Spartman eine Abfrage um die ID zu
								// ermitteln

								Vector<DM_Warengruppe> warengruppen = new Vector<DM_Warengruppe>();

								DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
								DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
								Document doc = dBuilder.parse(work_xml_file);

								doc.getDocumentElement().normalize();

								NodeList nList = doc.getElementsByTagName("WG");

								this.AGENT_DEBUG_INFO
										.addMassage("------[XML-DATEI] > Die XML-Objekte werden eingelesen");

								for (int temp = 0; temp < nList.getLength(); temp++) {

									int _id = 0;
									String _whg = "";
									String _txt1 = "";

									Node nNode = nList.item(temp);

									if (nNode.getNodeType() == Node.ELEMENT_NODE) {

										Element eElement = (Element) nNode;

										try {

											BigDecimal men = new BigDecimal(
													eElement.getElementsByTagName("Id").item(0).getTextContent());

											_id = men.intValue();

										} catch (java.lang.NullPointerException e) {
											_id = 0;
										}

										try {

											_whg = eElement.getElementsByTagName("WHG").item(0).getTextContent()
													.replaceAll("[^A-Za-z0-9+-]", "").trim();

										} catch (java.lang.NullPointerException e) {
											_whg = "?";
										}

										try {

											_txt1 = eElement.getElementsByTagName("Txt1").item(0).getTextContent()
													.replaceAll("[^A-Za-z0-9+-]", "").trim();

										} catch (java.lang.NullPointerException e) {
											_txt1 = "?";
										}

									}

									warengruppen.addElement(new DM_Warengruppe(_id, _whg, _txt1));

									anzahl_XML_objekte_pro_datei++;
									ANZAHL_XML_OBJEKTE_GESAMT++;
								}

								int _id = 0;
								String _whg = "";
								String _txt1 = "";

								int sqlZise = 0;
								StringBuffer buf = new StringBuffer("");

								this.AGENT_DEBUG_INFO.addMassage(
										"------[SQL-INSERT] > Die XML-Objekte werden in die Datenbank geschrieben");

								for (int wa = 0; wa < warengruppen.size(); wa++) {

									if (sqlZise < 1000) {

										if (sqlZise == 0) {

											buf.append(
													" INSERT INTO static_warengruppe (id,kurz,lang,mandant_id ) VALUES ");

										}

										_id = warengruppen.get(wa).getId();
										_whg = warengruppen.get(wa).getKurz();
										_txt1 = warengruppen.get(wa).getLang();

										buf.append("(" + _id + ",'" + _whg + "','" + _txt1 + "',"
												+ this.AUSWAHL_MANDANT_DBID + "),\n");

										sqlZise++;

										if (sqlZise == 999) {

											sqlZise = 0;

											String korr = new String(
													(String) buf.toString().subSequence(0, buf.length() - 2));

											buf.delete(0, buf.length() - 1);

											buf.append(korr);

											buf.append("; \n");

											DB_CONNECTOR.insertToDatabase(buf.toString(), "");

											buf.delete(0, buf.length());
											buf.setLength(0);

										}

										if (warengruppen.size() == anzahl_erzeugter_SQL_objekte_der_datei + 1) {

											String korr = new String(
													(String) buf.toString().subSequence(0, buf.length() - 2));

											buf.delete(0, buf.length() - 1);

											buf.append(korr);

											buf.append("; \n");

											DB_CONNECTOR.insertToDatabase(buf.toString(), "");

											buf.delete(0, buf.length());
											buf.setLength(0);

										}

										anzahl_erzeugter_SQL_objekte_der_datei++;

									}

								}

								FILE_PROCESS_END_TIME = new Date();

								this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
										.toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

								this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] > "
										+ this.single_file_full_process_diff_time_in_sec + " Sekunden =  "
										+ (this.single_file_full_process_diff_time_in_sec / 60) + " Minuten");

								AGENT_PROCESSED_XML_FILE_NAME = this.getSystem_processed_file_name(i);

								File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
										+ SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

								work_xml_file.renameTo(processed_xml_file);

								this.AGENT_DEBUG_INFO.addMassage("[DATEI WIRD NACH >> PROCESSED << VERSCHOBEN]");

								DB_CONNECTOR.updateOnDatabase("UPDATE work_agent_files set prozess_end_time='"
										+ db_date_formate.format(FILE_PROCESS_END_TIME) + "', file_status_id=3,"
										+ " datensaetze_anzahl=" + anzahl_XML_objekte_pro_datei + ", work_filename='"
										+ system_work_file_name + "', processed_filename='"
										+ AGENT_PROCESSED_XML_FILE_NAME + "' " //
										+ " where file_creation_code='" + file_creation_code + "';");

							} catch (org.xml.sax.SAXParseException e) {

								this.AGENT_DEBUG_INFO
										.addMassage("--------[DATEI-FEHLER] > [ " + this.getClass().getName()
												+ " ] > [DATEI] > [ " + deliverd_files[i].getAbsolutePath() + " ]");

								this.AGENT_DEBUG_INFO.addMassage("-----------[DETAILS] > [ " + e.getMessage() + " "
										+ e.getLineNumber() + "  " + e.getColumnNumber() + " ]");

							}

						} else {

							this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_AGENT_HAT_KEINE_DATEN;

							this.AGENT_END_PROCESS_TIME = new Date();

							this.AGENT_DEBUG_INFO
									.addMassage("[AGENT] Im verzeichnis sind keine Daten fuer den Agenten");

							DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set log_content='"
									+ this.AGENT_DEBUG_INFO.debugString.toString() + "', end_time='"
									+ db_date_formate.format(AGENT_END_PROCESS_TIME) + "', datensatz_gesamt_anzahl=" + 0
									+ " ,datei_bearbeitet_anzahl=" + 0 + ", status_id=" + this.AGENT_STATUS_CURRENT_CODE
									+ " where agenten_id=" + this.AGENT_DBID + " and run_creation_code='"
									+ AGENT_RUN_CREATION_CODE + "';");

						}

					}

					this.AGENT_DEBUG_INFO.addMassage(
							"----------------------------------------------------------------------------------------------------------------");

					this.AGENT_END_PROCESS_TIME = new Date();

					this.agent_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
							.toSeconds(AGENT_END_PROCESS_TIME.getTime() - AGENT_START_PROCESS_TIME.getTime());

					this.AGENT_DEBUG_INFO.addMassage("[ENDE " + this.AGENT_NAME + "] > am: "
							+ long_datum_zeit_formate.format(AGENT_END_PROCESS_TIME) + " \n");

					this.AGENT_DEBUG_INFO.addMassage("[PROZESS-TIME] > " + agent_full_process_diff_time_in_sec
							+ " Sekunden =  " + (agent_full_process_diff_time_in_sec / 60) + " Minuten \n");

					this.AGENT_DEBUG_INFO.addMassage(
							"***************************************************************************************************************\n");

					DB_CONNECTOR.updateOnDatabase("UPDATE agent set last_prozess_time='"
							+ db_date_formate.format(AGENT_START_PROCESS_TIME) + "' where dbid=" + AGENT_DBID);

					this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

					DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set log_content='"
							+ this.AGENT_DEBUG_INFO.debugString.toString() + "', end_time='"
							+ db_date_formate.format(AGENT_END_PROCESS_TIME) + "', datensatz_gesamt_anzahl="
							+ ANZAHL_XML_OBJEKTE_GESAMT + " ,datei_bearbeitet_anzahl=" + ANZAHL_VERARBEITETE_XML_DATEIEN
							+ ", status_id=" + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
							+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

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

			this.AGENT_DEBUG_INFO.addMassage("[" + AGENT_NAME + "] - STATUS=deaktiviert");
			this.AGENT_DEBUG_INFO
					.addMassage("[" + AGENT_NAME + "] - STATUS aendern > [DB] > Tabelle=agent, Col=activity, Set=1");

			DB_CONNECTOR.updateOnDatabase(
					"UPDATE agent_run_logs set log_content='" + this.AGENT_DEBUG_INFO.debugString.toString()
							+ "', end_time='" + db_date_formate.format(AGENT_END_PROCESS_TIME)
							+ "', datensatz_gesamt_anzahl=" + 0 + " ,datei_bearbeitet_anzahl=" + 0 + ", status_id="
							+ this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
							+ " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

		}

	}
}
