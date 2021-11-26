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

public class agent_bestellung_stammdaten extends Agent implements java.io.Serializable {

	int agentrun_db_id = 0;
	int work_file_db_id = 0;

	private static final long serialVersionUID = 914098162380113117L;

	public agent_bestellung_stammdaten(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter) {

		this.AGENT_NAME = "BINIMA";
		this.AGENT_DBID = 664;
		this.FILE_CONTENT_DBID = 18;

		this.AGENT_RUN_CREATION_CODE = ZUFALLS_GENERATOR.newRandomCode();

		this.RUN_MANAGER_DBID = _managerid;

		this.MANAGER_RUN_CREATION_CODE = _manager_run_creation_code;

		this.IS_LINUX = _isLinux;
		this.OPERATION_SYSTEM = _opsystem;

		this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;

		this.AGENT_DEBUG_INFO = new Debugger(this.DEBUGGER_STATUS);

	}

	public void start() throws SQLException, ClassNotFoundException, IOException, SAXException,
			ParserConfigurationException, ParseException {

		this.AGENT_START_PROCESS_TIME = new Date();

		String sql001 = "SELECT agentname_kurz, "
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

		String sqlFindId = "Select id from agent_run_logs where run_creation_code='" + AGENT_RUN_CREATION_CODE + "'";

		DB_CONNECTOR.readFromDatabase(sqlFindId, "sqlFindId");

		while (DB_CONNECTOR.rs.next()) {

			agentrun_db_id = DB_CONNECTOR.rs.getInt(1);
		}

		this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_INITIALISIERUNG;

		DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE
				+ " where agenten_id=" + this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

		if (this.AGENT_ACTIVITY == true) {

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

								AUSWAHL_MANDANT_DBID = 0;

								if (deliverd_xml_file.getName().contains("_wp_")) {
									this.AUSWAHL_MANDANT_DBID = this.WP_MANDANT_DBID;

									this.AUSWAHL_MANDANT_NAME = "_wp_";

								}

								if (deliverd_xml_file.getName().contains("_ka_")) {
									this.AUSWAHL_MANDANT_DBID = this.KA_MANDANT_DBID;

									this.AUSWAHL_MANDANT_NAME = "_ka_";
								}

								Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

								// Die Attribute der Datei werden ermittelt
								BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

								file_dateigroesse = attr.size();

								GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault());

								calendar.setTimeInMillis(attr.creationTime().toMillis());

								file_creation_time = db_date_formate.format(calendar.getTime());

								String sql_insert_file = "Insert into " //
										+ " work_agent_files (" //
										+ " agent_id," //
										+ " filecontent_id, " //
										+ " deliver_filename, " //
										+ " prozess_begin_time," //
										+ " run_creation_code, " //
										+ " file_status_id, "//
										+ " dateigroesse,"//
										+ " erstelldatum," //
										+ " file_creation_code, " //
										+ " mandant_id, "//
										+ " agent_run_id) VALUES (" //
										+ AGENT_DBID + "," //
										+ FILE_CONTENT_DBID + ",'" //
										+ deliverd_xml_file.getName() + "','" //
										+ db_date_formate.format(FILE_PROCESS_START_TIME) + "', '" //
										+ this.AGENT_RUN_CREATION_CODE + "'," //
										+ 1 + " , " //
										+ file_dateigroesse + ",'" //
										+ //
										file_creation_time + "','" //
										+ file_creation_code + "'," + this.AUSWAHL_MANDANT_DBID + ", " + agentrun_db_id
										+ ");";

								DB_CONNECTOR.insertToDatabase(sql_insert_file,
										"Bestellungen Initial Import > Single File Insert DB");

								String sql_find_work_file_db_id = "SELECT id FROM work_agent_files where file_creation_code='"
										+ file_creation_code + "';";

								DB_CONNECTOR.readFromDatabase(sql_find_work_file_db_id, "sql_find_work_file_db_id");

								while (DB_CONNECTOR.rs.next()) {

									work_file_db_id = DB_CONNECTOR.rs.getInt(1);
								}

								this.AGENT_DEBUG_INFO.addMassage("- [DATEI-NR.: " + (i + 1) + "] wird verarbeitet.\n");

								String system_work_file_name = this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
										+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
										+ this.AGENT_FILE_NAME_PATTERN + "_"
										+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_" + (i + 1) + "_"
										+ file_creation_code + "_work.xml";

								File work_xml_file = new File(
										this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + system_work_file_name);

								deliverd_xml_file.renameTo(work_xml_file);

								this.AGENT_DEBUG_INFO.addMassage(
										"- [DATEI WIRD VOM DELIVER ORDNER ABGEHOLT] > Datei wurde in dem >>WORK<< ORDNER verschoben");

								// Der Creation Code wird benoetigt um nach dem
								// Insert
								// den Datensatz zu aktualisieren
								// Damit Spartman eine Abfrage um die ID zu
								// ermitteln

								DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
								DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
								Document doc = dBuilder.parse(work_xml_file);

								doc.getDocumentElement().normalize();

								NodeList alle_bestellungen_liste = doc.getElementsByTagName("Bh");

								this.AGENT_DEBUG_INFO.addMassage("- [XML-DATEI] > Die XML-Objekte werden eingelesen");

								for (int position_liste = 0; position_liste < alle_bestellungen_liste
										.getLength(); position_liste++) {

									// Bestellkopf

									String bestellnummer = "";
									String guid = "";
									String bestelldatum = "";
									boolean erledigt = false;
									String erledigtam = "";
									boolean storno = false;

									Node single_bestellung = alle_bestellungen_liste.item(position_liste);

									if (single_bestellung.getNodeType() == Node.ELEMENT_NODE) {

										Element eElement_bestellung = (Element) single_bestellung;

										try {

											erledigt = new Boolean(eElement_bestellung
													.getElementsByTagName("BhErledigt").item(0).getTextContent().trim()
													.replace(" ", "-").replaceAll("[^A-Za-z0-9+-]", ""));

										} catch (java.lang.NullPointerException e) {
											erledigt = false;
										}

										try {

											storno = new Boolean(eElement_bestellung.getElementsByTagName("BhStorno")
													.item(0).getTextContent().trim().replace(" ", "-")
													.replaceAll("[^A-Za-z0-9+-]", ""));

										} catch (java.lang.NullPointerException e) {
											storno = false;
										}

										try {

											bestellnummer = eElement_bestellung.getElementsByTagName("Nr").item(0)
													.getTextContent().trim().replace(" ", "-")
													.replaceAll("[^A-Za-z0-9+-]", "");

										} catch (java.lang.NullPointerException e) {
											bestellnummer = "?";
										}

										try {

											guid = eElement_bestellung.getElementsByTagName("GUID").item(0)
													.getTextContent().trim().replace(" ", "-")
													.replaceAll("[^A-Za-z0-9+-]", "");

										} catch (java.lang.NullPointerException e) {
											guid = "?";
										}

										try {

											String tempDatumAusXML = "";
											Date datum_temp = null;

											tempDatumAusXML = eElement_bestellung.getElementsByTagName("Date").item(0)
													.getTextContent().trim();

											datum_temp = short_rewert_format.parse(tempDatumAusXML);

											bestelldatum = db_date_formate.format(datum_temp);

										} catch (java.lang.NullPointerException e) {
											bestelldatum = "9999-09-09 00:00:00";
										}

										erledigtam = "9999-09-09 00:00:00";

										if (erledigt == false && storno == false) {

											String SQLfuegeBestellKopfein = "INSERT INTO static_bestellung ( "
													+ " bestell_guid," //
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
													+ " bestell_datum,"//
													+ " mandant_id ) VALUES ('" //
													+ guid + "','" //
													+ bestellnummer//
													+ "'," + erledigt //
													+ "," + storno //
													+ ", '" + erledigtam //
													+ "' , '- INSERT BESTELLUNG NEU, Bestellnummer: [" + bestellnummer//
													+ "], am: " + long_datum_zeit_formate.format(new Date())//
													+ " von A-RUN: [" //
													+ this.AGENT_RUN_CREATION_CODE + "] - Datei: [" + file_creation_code
													+ "]\n', '" //

													+ db_date_formate.format(new Date()) //
													+ "', " + agentrun_db_id + ", "//
													+ work_file_db_id + ", '" //
													+ this.AGENT_RUN_CREATION_CODE //
													+ "', '" + this.MANAGER_RUN_CREATION_CODE + "', '" + bestelldatum
													+ "'," + this.AUSWAHL_MANDANT_DBID + " );";

											try {

												DB_CONNECTOR.insertToDatabase(SQLfuegeBestellKopfein,
														"SQLfuegeBestellKopfein");

												// Bestellpositionen werden
												// Abgearbeitet

												NodeList bestell_positionen = eElement_bestellung
														.getElementsByTagName("Bp");

												if (bestell_positionen.getLength() > 0) {

													for (int p = 0; p < bestell_positionen.getLength(); p++) {

														Node bestellPosition = bestell_positionen.item(p);

														if (bestellPosition.getNodeType() == Node.ELEMENT_NODE) {

															Element PositionsElement = (Element) bestellPosition;

															String position_guid = "";

															String artikelnummer = "";

															int rec = 0;

															int menge_in_xml = 0;

															int mengeGeliefert_in_xml = 0;

															int dif_noch_nicht_gekommen = 0;

															int dif2_geliefert_altGeliefert = 0;

															try {

																BigDecimal men = new BigDecimal(
																		PositionsElement.getElementsByTagName("Rec")
																				.item(0).getTextContent()
																				.replaceAll("[^A-Za-z0-9+-]", ""));

																rec = men.intValue();

															} catch (java.lang.NullPointerException e) {
																rec = 0;
															}

															try {

																position_guid = PositionsElement
																		.getElementsByTagName("GUID").item(0)
																		.getTextContent().trim().replace(" ", "-")
																		.replaceAll("[^A-Za-z0-9+-]", "");

															} catch (java.lang.NullPointerException e) {
																position_guid = "?";
															}

															try {

																artikelnummer = PositionsElement
																		.getElementsByTagName("ArtNr").item(0)
																		.getTextContent().trim().replace(" ", "-")
																		.replaceAll(
																				"[^A-Za-z0-9/\\u2024\\u002E\\u002D]",
																				"");

															} catch (java.lang.NullPointerException e) {
																artikelnummer = "?";
															}

															try {

																BigDecimal men = new BigDecimal(
																		PositionsElement.getElementsByTagName("Menge")
																				.item(0).getTextContent());

																menge_in_xml = men.intValue();

															} catch (java.lang.NullPointerException e) {

																menge_in_xml = 0;
															}

															try {

																BigDecimal men = new BigDecimal(PositionsElement
																		.getElementsByTagName("MengeGeliefert").item(0)
																		.getTextContent());

																mengeGeliefert_in_xml = men.intValue();

															} catch (java.lang.NullPointerException e) {

																mengeGeliefert_in_xml = 0;
															}

															try {

																if (artikelnummer.contains("WP-") == false
																		&& artikelnummer.contains("WPK-") == false
																		&& artikelnummer != "" && artikelnummer != "?"
																		&& artikelnummer.length() >= 5) {

																	int alt_bestellmenge_im_artikel = 0;
																	String alter_log_artikel = "";

																	String sqlbestellmenge_im_artikel = "SELECT bestellt_anzahl, agent_operation FROM static_artikel where interne_artikelnummer='"
																			+ artikelnummer + "';";

																	DB_CONNECTOR.readFromDatabase(
																			sqlbestellmenge_im_artikel,
																			"sqlbestellmenge_im_artikel");

																	while (DB_CONNECTOR.rs.next()) {

																		alt_bestellmenge_im_artikel = DB_CONNECTOR.rs
																				.getInt(1);
																		alter_log_artikel = DB_CONNECTOR.rs
																				.getString(2);

																	}

																	dif_noch_nicht_gekommen = (menge_in_xml
																			- mengeGeliefert_in_xml);

																	dif2_geliefert_altGeliefert = (mengeGeliefert_in_xml
																			- 0);

																	// Menge,
																	// ist die
																	// Summe der
																	// gesammten
																	// Bestellmenge
																	// mengeGeliefert
																	// ist die
																	// Summe die
																	// bereits
																	// geliefert
																	// wurd und
																	// bereits
																	// im
																	// Artikel
																	// eingetragen
																	// wurde

																	// DIF ist
																	// die Summe
																	// die noch
																	// nicht
																	// geliefert
																	// wurde und
																	// bei dem
																	// Artikel
																	// unter der
																	// BestellAnzahl
																	// mit auf
																	// summiert
																	// wird.

																	// Beim
																	// Insert
																	// ist die
																	// Bestellmenge
																	// des
																	// Artikel
																	// gleich
																	// der
																	// alten
																	// Bestellmenge
																	// plus die
																	// bestellte
																	// menge_in_xml
																	// abzueglich
																	// der Ware
																	// die
																	// bereits
																	// geliefer
																	// wurde

																	int neu_bestellmenge_im_artikel = 0;

																	if (erledigt == false && storno == false) {

																		neu_bestellmenge_im_artikel = (alt_bestellmenge_im_artikel
																				+ dif_noch_nicht_gekommen);

																	} else {

																		neu_bestellmenge_im_artikel = alt_bestellmenge_im_artikel;
																	}

																	String sqlPositionEinfuegen = "INSERT INTO static_bestellung_positionen (" //
																			+ " position, bestellungnummer,artikel_nummer,"
																			+ " menge_bestellt, menge_geliefert,menge_noch_zu_liefern, agent_operation, last_operation_datum, guid_position ) VALUES ("
																			+ rec + ",'" + bestellnummer + "','"
																			+ artikelnummer + "'," + menge_in_xml + ","
																			+ mengeGeliefert_in_xml + ","
																			+ dif_noch_nicht_gekommen
																			+ ",'- INITIAL BESTELLUNG - " + menge_in_xml
																			+ " Stueck(e) bestellt.\n---- Position:["
																			+ rec + "]\n---- Artikel: [" + artikelnummer
																			+ "]\n---- Bestellnummer: [" + bestellnummer
																			+ "].\n---- Bestellt: [" + menge_in_xml
																			+ "]\n---- Geliefert:["
																			+ mengeGeliefert_in_xml
																			+ "]\n---- Noch zu liefern:["
																			+ dif_noch_nicht_gekommen + "]\n---- "
																			+ "Bestellmenge im Artikel ["
																			+ artikelnummer + "]: [a="
																			+ alt_bestellmenge_im_artikel + ", n="
																			+ neu_bestellmenge_im_artikel
																			+ "]\n---- Erledigt: [" + erledigt
																			+ "]\n---- Storno: [" + storno
																			+ "]\n---- am: "
																			+ long_datum_zeit_formate.format(new Date())//
																			+ " von A-RUN: [" //
																			+ this.AGENT_RUN_CREATION_CODE
																			+ "]\n---- Datei: [" + file_creation_code
																			+ "].\n','"
																			+ db_date_formate.format(new Date()) + "','"
																			+ position_guid + "');";

																	String neuer_log_artikel = alter_log_artikel
																			+ "-{I_BEST_INIT1} INITIAL BESTELLUNG - "
																			+ menge_in_xml
																			+ " Stueck(e) bestellt.\n---- Position:["
																			+ rec + "]\n---- Artikel: [" + artikelnummer
																			+ "]\n---- Bestellnummer: [" + bestellnummer
																			+ "].\n---- Bestellt: [" + menge_in_xml
																			+ "]\n---- Geliefert:["
																			+ mengeGeliefert_in_xml
																			+ "]\n---- Noch zu liefern:["
																			+ dif_noch_nicht_gekommen + "]\n---- "
																			+ "Bestellmenge im Artikel ["
																			+ artikelnummer + "]: [a="
																			+ alt_bestellmenge_im_artikel + ", n="
																			+ neu_bestellmenge_im_artikel
																			+ "]\n---- Erledigt: [" + erledigt
																			+ "]\n---- Storno: [" + storno
																			+ "]\n---- am: "
																			+ long_datum_zeit_formate.format(new Date())//
																			+ " von A-RUN: [" //
																			+ this.AGENT_RUN_CREATION_CODE
																			+ "]\n---- Datei: [" + file_creation_code
																			+ "].\n\n";

																	String SQLUPdateArtikel = "UPDATE static_artikel set bestellt_anzahl="
																			+ neu_bestellmenge_im_artikel
																			+ ", agent_operation='" + neuer_log_artikel
																			+ "', last_update='"
																			+ db_date_formate.format(new Date())
																			+ "' where interne_artikelnummer='"
																			+ artikelnummer + "';";

																	DB_CONNECTOR.insertToDatabase(sqlPositionEinfuegen,
																			"sqlPositionEinfuegen");

																	DB_CONNECTOR.updateOnDatabase(SQLUPdateArtikel);

																}

															} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

																this.AGENT_DEBUG_INFO.addMassage("Artikel: [B:"
																		+ bestellnummer + " A:" + artikelnummer
																		+ "], P=" + rec + " existiert bereits!");

															}
														}

													}

												}
											} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

											}

										}

										if (erledigt == true) {

											String SQLfuegeBestellKopfein1 = "INSERT INTO static_bestellung ( "
													+ " bestell_guid," //
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
													+ " bestell_datum,"//
													+ " mandant_id ) VALUES ('" //
													+ guid + "','" //
													+ bestellnummer//
													+ "'," + erledigt //
													+ "," + storno //
													+ ", '" + erledigtam //
													+ "' , '- INSERT BESTELLUNG ALT-ERLEDIGT, Bestellnummer: ["
													+ bestellnummer//
													+ "], am: " + long_datum_zeit_formate.format(new Date())//
													+ " von A-RUN: [" //
													+ this.AGENT_RUN_CREATION_CODE + "] - Datei: [" + file_creation_code
													+ "]\n', '" //

													+ db_date_formate.format(new Date()) //
													+ "', " + agentrun_db_id + ", "//
													+ work_file_db_id + ", '" //
													+ this.AGENT_RUN_CREATION_CODE //
													+ "', '" + this.MANAGER_RUN_CREATION_CODE + "', '" + bestelldatum
													+ "'," + this.AUSWAHL_MANDANT_DBID + " );";

											try {

												DB_CONNECTOR.insertToDatabase(SQLfuegeBestellKopfein1,
														"SQLfuegeBestellKopfein");

												NodeList bestell_positionen = eElement_bestellung
														.getElementsByTagName("Bp");

												if (bestell_positionen.getLength() > 0) {

													for (int p = 0; p < bestell_positionen.getLength(); p++) {

														Node bestellPosition = bestell_positionen.item(p);

														if (bestellPosition.getNodeType() == Node.ELEMENT_NODE) {

															Element PositionsElement = (Element) bestellPosition;

															String position_guid = "";

															String artikelnummer = "";

															int rec = 0;

															int menge_in_xml = 0;

															int mengeGeliefert_in_xml = 0;

															try {

																BigDecimal men = new BigDecimal(
																		PositionsElement.getElementsByTagName("Rec")
																				.item(0).getTextContent()
																				.replaceAll("[^A-Za-z0-9+-]", ""));

																rec = men.intValue();

															} catch (java.lang.NullPointerException e) {
																rec = 0;
															}

															try {

																position_guid = PositionsElement
																		.getElementsByTagName("GUID").item(0)
																		.getTextContent().trim().replace(" ", "-")
																		.replaceAll("[^A-Za-z0-9+-]", "");

															} catch (java.lang.NullPointerException e) {
																position_guid = "?";
															}

															try {

																artikelnummer = PositionsElement
																		.getElementsByTagName("ArtNr").item(0)
																		.getTextContent().trim().replace(" ", "-")
																		.replaceAll(
																				"[^A-Za-z0-9/\\u2024\\u002E\\u002D]",
																				"");

															} catch (java.lang.NullPointerException e) {
																artikelnummer = "?";
															}

															try {

																BigDecimal men = new BigDecimal(
																		PositionsElement.getElementsByTagName("Menge")
																				.item(0).getTextContent());

																menge_in_xml = men.intValue();

															} catch (java.lang.NullPointerException e) {

																menge_in_xml = 0;
															}

															try {

																BigDecimal men = new BigDecimal(PositionsElement
																		.getElementsByTagName("MengeGeliefert").item(0)
																		.getTextContent());

																mengeGeliefert_in_xml = men.intValue();

															} catch (java.lang.NullPointerException e) {

																mengeGeliefert_in_xml = 0;
															}

															try {

																if (artikelnummer.contains("WP-") == false
																		&& artikelnummer.contains("WPK-") == false
																		&& artikelnummer != "" && artikelnummer != "?"
																		&& artikelnummer.length() >= 5) {

																	String sqlPositionEinfuegen = "INSERT INTO static_bestellung_positionen (" //
																			+ " position, bestellungnummer,artikel_nummer,"
																			+ " menge_bestellt, menge_geliefert,menge_noch_zu_liefern, agent_operation, last_operation_datum, guid_position ) VALUES ("
																			+ rec + ",'" + bestellnummer + "','"
																			+ artikelnummer + "'," + menge_in_xml + ","
																			+ mengeGeliefert_in_xml + ","
																			+ (menge_in_xml - mengeGeliefert_in_xml)
																			+ ",'- INITIAL BESTELLUNG ALT-ERLEDIGT - "
																			+ menge_in_xml
																			+ " Stueck(e) bestellt.\n---- Position:["
																			+ rec + "]\n---- Artikel: [" + artikelnummer
																			+ "]\n---- Bestellnummer: [" + bestellnummer
																			+ "].\n---- Bestellt: [" + menge_in_xml
																			+ "]\n---- Geliefert:["
																			+ mengeGeliefert_in_xml
																			+ "]\n---- Noch zu liefern:["
																			+ (menge_in_xml - mengeGeliefert_in_xml)
																			+ "]\n---- "

																			+ "]\n---- Erledigt: [" + erledigt
																			+ "]\n---- Storno: [" + storno
																			+ "]\n---- am: "
																			+ long_datum_zeit_formate.format(new Date())//
																			+ " von A-RUN: [" //
																			+ this.AGENT_RUN_CREATION_CODE
																			+ "]\n---- Datei: [" + file_creation_code
																			+ "].\n','"
																			+ db_date_formate.format(new Date()) + "','"
																			+ position_guid + "');";

																	DB_CONNECTOR.insertToDatabase(sqlPositionEinfuegen,
																			"sqlPositionEinfuegen");

																}

															} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {

																this.AGENT_DEBUG_INFO.addMassage("Artikel: [B:"
																		+ bestellnummer + " A:" + artikelnummer
																		+ "], P=" + rec + " existiert bereits!");

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
									.addMassage("[AGENT] Im verzeichnis sind keine Daten fuer den Agenten \n");

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
							+ long_datum_zeit_formate.format(AGENT_END_PROCESS_TIME) + "");

					this.AGENT_DEBUG_INFO.addMassage("[PROZESS-TIME] > " + agent_full_process_diff_time_in_sec
							+ " Sekunden =  " + (agent_full_process_diff_time_in_sec / 60) + " Minuten");

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
