package INITAL_MANAGER;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
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

public class agent_statistik_stammdaten extends Agent implements java.io.Serializable {

    private static final long serialVersionUID = -8572214093119219809L;

    Vector<DM_Artikel> artikeln = new Vector<DM_Artikel>();

    public agent_statistik_stammdaten(int _manager_dbid, String _manager_run_creation_code, boolean _isLinux,
	    String _opsystem, String _system_path_delimiter) {

	this.AGENT_NAME = "STAINMA";

	this.AGENT_DBID = 17;

	this.AGENT_RUN_CREATION_CODE = ZUFALLS_GENERATOR.newRandomCode();

	this.RUN_MANAGER_DBID = _manager_dbid;

	this.MANAGER_RUN_CREATION_CODE = _manager_run_creation_code;

	this.IS_LINUX = _isLinux;

	this.OPERATION_SYSTEM = _opsystem;

	this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;

	this.ANZAHL_XML_OBJEKTE_GESAMT = 0;

	this.FILE_CONTENT_DBID = 1;

	this.AGENT_DEBUG_INFO = new Debugger(this.DEBUGGER_STATUS);

    }

    public void start()

	    throws SQLException, ClassNotFoundException, IOException, SAXException, ParserConfigurationException {

	this.AGENT_START_PROCESS_TIME = new Date();

	String sql001 = "SELECT agentname_kurz, "
		+ " COALESCE(last_prozess_time, 'noch nie'), activity, file_name_pattern, truncate_table_before_insert, truncate_table_before_insert_single_file, debug_status,manager_start_position, sql_max_collection_size  FROM agent where dbid="
		+ AGENT_DBID + ";";

	this.DB_CONNECTOR.readFromDatabase(sql001, "initial_agent_artikel ");

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

	if (this.IS_LINUX == true) {
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

	this.DB_CONNECTOR.updateOnDatabase(
		"INSERT INTO agent_run_logs (agenten_id, manager_id, run_creation_code, start_time, status_id) VALUES ("
			+ AGENT_DBID + "," + RUN_MANAGER_DBID + ",'" + this.AGENT_RUN_CREATION_CODE + "','"

			+ db_date_formate.format(AGENT_START_PROCESS_TIME) + "', " + this.AGENT_STATUS_CURRENT_CODE
			+ ");");

	this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_INITIALISIERUNG;

	this.DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE
		+ " where agenten_id=" + this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

	if (this.AGENT_ACTIVITY == true) {

	    this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

	    this.DB_CONNECTOR.updateOnDatabase(
		    "UPDATE agent_run_logs set status_id=" + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id="
			    + this.AGENT_DBID + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

	    this.AGENT_DEBUG_INFO.addMassage("[START " + this.AGENT_NAME + "]\n");

	    /*
	     * -----------------------------------------------------------------
	     * 
	     */

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

		    for (int i = 0; i < ANZAHL_VERARBEITETE_XML_DATEIEN; i++) {

			this.AGENT_DEBUG_INFO.addMassage(
				"-------------------------------------------------------------------------------- \n");

			// Das AGENT_FILE_NAME_PATTERN muss in der Datenbank
			// definiert werden. Jeder Agent kann nur seine
			// eigenen
			// Dateien sehen und bearbeiten.

			if (deliverd_files[i].isFile()) {

			    artikeln.clear();
			    sqlZise = 0;
			    anzahl_XML_objekte_pro_datei = 0;
			    anzahl_erzeugter_SQL_objekte_der_datei = 0;

			    FILE_PROCESS_START_TIME = new Date();

			    String file_creation_code = ZUFALLS_GENERATOR.newRandomCode();

			    this.AGENT_DEBUG_INFO.addMassage(
				    "[ Es wurde/n " + ANZAHL_VERARBEITETE_XML_DATEIEN + " Datei/en gefunden. \n");

			    this.AGENT_DEBUG_INFO.addMassage("- [DATEI-NR.: " + (i + 1) + "] - [ " + file_creation_code
				    + " ] wird verarbeitet. \n");

			    this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			    this.DB_CONNECTOR.updateOnDatabase("UPDATE agent_run_logs set status_id="
				    + this.AGENT_STATUS_CURRENT_CODE + " where agenten_id=" + this.AGENT_DBID
				    + " and run_creation_code='" + AGENT_RUN_CREATION_CODE + "';");

			    // Die Laufendenummer wird fuer jede neue Datei
			    // zurueck gesetzt

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

			    String system_work_file_name = this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
				    + this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
				    + this.AGENT_FILE_NAME_PATTERN + "_"
				    + long_date_formate.format(FILE_PROCESS_START_TIME) + "_" + (i + 1) + "_"
				    + file_creation_code + "_work.xml";

			    File work_xml_file = new File(
				    this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + system_work_file_name);

			    this.AGENT_DEBUG_INFO.addMassage("DATEI [ " + file_creation_code
				    + " ] WIRD VOM DELIVER ORDNER ABGEHOLT und nach >>WORK<< verschoben. \n");

			    deliverd_xml_file.renameTo(work_xml_file);

			    try {

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
					+ "file_creation_code,mandant_id )" //
					+ " VALUES (" //
					+ AGENT_DBID + "," //
					+ FILE_CONTENT_DBID + ",'" //
					+ deliverd_xml_file.getName() + "','" //
					+ db_date_formate.format(FILE_PROCESS_START_TIME) + "', '" //
					+ this.AGENT_RUN_CREATION_CODE + "'," //
					+ 1 + " , " //
					+ file_dateigroesse + ",'" //
					+ //
					file_creation_time + "','" //
					+ file_creation_code + "', " + this.AUSWAHL_MANDANT_DBID + ");";

				this.DB_CONNECTOR.insertToDatabase(sql_insert_file,
					"Statistik Initial Import > Single File Insert DB");

				// Der Creation Code wird benoetigt um nach dem
				// Insert
				// den Datensatz zu aktualisieren
				// Damit Spartman eine Abfrage um die ID zu
				// ermitteln

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(work_xml_file);

				doc.getDocumentElement().normalize();

				NodeList nList = doc.getElementsByTagName("Art");

				this.AGENT_DEBUG_INFO.addMassage("[XML-DATEI] > Die XML-Objekte werden eingelesen \n");

				for (int temp = 0; temp < nList.getLength(); temp++) {

				    String _guid = "";
				    String _interne_artikelnummer = "";
				    String _match1 = "";
				    String _match2 = "";
				    String _waregruppe = "";
				    String _hersteller_artikelnummer = "";
				    int _lieferantenid = 0;
				    String _ean = "";
				    String _bezeichnung = "";
				    int _lager_anzahl = 0;
				    int _auf_auftrag_reserviert_anzahl = 0;

				    Node nNode = nList.item(temp);

				    if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					try {

					    _guid = eElement.getElementsByTagName("GUID").item(0).getTextContent()
						    .replaceAll("[^A-Za-z0-9+-]", "");

					} catch (java.lang.NullPointerException e) {
					    _guid = "?";
					}

					try {

					    _interne_artikelnummer = eElement.getElementsByTagName("Nr").item(0)
						    .getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

					} catch (java.lang.NullPointerException e) {
					    _interne_artikelnummer = "?";
					}

					try {

					    _bezeichnung = eElement.getElementsByTagName("Langtext").item(0)
						    .getTextContent().replaceAll(
							    "[^A-Za-z0-9+-\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2022]",
							    "");

					} catch (java.lang.NullPointerException e) {
					    _bezeichnung = "?";
					}

					try {

					    _match1 = eElement.getElementsByTagName("Match1").item(0).getTextContent()
						    .trim().replace(" ", "-").replaceAll(
							    "[^A-Za-z0-9+-\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2022]",
							    "");

					} catch (java.lang.NullPointerException e) {
					    _match1 = "?";
					}

					try {

					    _match2 = eElement.getElementsByTagName("Match2").item(0).getTextContent()

						    .replaceAll(
							    "[^A-Za-z0-9+-\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2022]",
							    "");

					} catch (java.lang.NullPointerException e) {
					    _match2 = "?";
					}

					try {

					    _waregruppe = eElement.getElementsByTagName("WHG").item(0).getTextContent()
						    .replaceAll("[^A-Za-z0-9+-]", "").trim();

					} catch (java.lang.NullPointerException e) {
					    _waregruppe = "?";
					}

					try {

					    _hersteller_artikelnummer = eElement.getElementsByTagName("HstNr").item(0)
						    .getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

					} catch (java.lang.NullPointerException e) {
					    _hersteller_artikelnummer = "?";
					}

					try {

					    _lieferantenid = Integer.parseInt(
						    eElement.getElementsByTagName("BhGk").item(0).getTextContent());

					} catch (java.lang.NullPointerException e) {

					    _lieferantenid = 0;
					}

					try {

					    _ean = eElement.getElementsByTagName("EAN").item(0).getTextContent()
						    .replaceAll("[^A-Za-z0-9+-]", "").trim();
					    ;

					} catch (java.lang.NullPointerException e) {
					    _ean = "?";
					}

					NodeList bestandliste = eElement.getElementsByTagName("ArtBestand");

					_lager_anzahl = 0;

					for (int item = 0; item < bestandliste.getLength(); item++) {

					    Node bestat = bestandliste.item(item);

					    if (bestat.getNodeType() == Node.ELEMENT_NODE) {

						Element bestatElement = (Element) bestat;

						BigDecimal men = new BigDecimal(bestatElement
							.getElementsByTagName("Ist").item(0).getTextContent());

						_lager_anzahl = men.intValue();

					    }

					}

					NodeList lieferliste = eElement.getElementsByTagName("ArtRV");

					_auf_auftrag_reserviert_anzahl = 0;
					int geliefert = 0;
					int mengeRest = 0;

					for (int item = 0; item < lieferliste.getLength(); item++) {

					    Node bestatLiefer = lieferliste.item(item);

					    if (bestatLiefer.getNodeType() == Node.ELEMENT_NODE) {

						Element lieferElement = (Element) bestatLiefer;

						try {

						    BigDecimal men = new BigDecimal(lieferElement
							    .getElementsByTagName("Menge").item(0).getTextContent());

						    mengeRest = men.intValue();

						} catch (java.lang.NullPointerException e) {

						    mengeRest = 0;
						}

						try {

						    BigDecimal geliefertXML = new BigDecimal(
							    lieferElement.getElementsByTagName("Geliefert").item(0)
								    .getTextContent());

						    geliefert = geliefertXML.intValue();

						} catch (java.lang.NullPointerException e) {

						    geliefert = 0;
						}

						int diff = mengeRest - geliefert;

						_auf_auftrag_reserviert_anzahl = _auf_auftrag_reserviert_anzahl + diff;

					    }

					}

				    }

				    artikeln.addElement(new DM_Artikel(_guid, _interne_artikelnummer, _match1, _match2,
					    _waregruppe, _hersteller_artikelnummer, _lieferantenid, _ean, _bezeichnung,
					    _lager_anzahl, _auf_auftrag_reserviert_anzahl));

				    anzahl_XML_objekte_pro_datei++;
				    ANZAHL_XML_OBJEKTE_GESAMT++;
				}

				String _guid = "";
				String _interne_artikelnummer = "";
				String _match1 = "";
				String _match2 = "";
				String _waregruppe = "";
				String _hersteller_artikelnummer = "";
				int _lieferantenid = 0;
				String _ean = "";
				String _bezeichnung = "";

				StringBuffer buf = new StringBuffer("");

				this.AGENT_DEBUG_INFO
					.addMassage("Die XML-Objekte werden in die Datenbank geschrieben. \n");

				this.AGENT_DEBUG_INFO.addMassage("In der Datei [ " + file_creation_code + " ] sind ["
					+ artikeln.size() + "] Artikel vorhanden.\n");

				this.AGENT_DEBUG_INFO.addMassage("Die Initialisierung speichert ["
					+ this.SQL_MAX_COLLECTION_SIZE + "] Artikel je Insert. \n");

				for (int j = 0; j < artikeln.size(); j++) {

				    if (sqlZise < this.SQL_MAX_COLLECTION_SIZE) {

					if (sqlZise == 0) {

					    buf.append(
						    "INSERT INTO static_artikel ( guid_taifun,interne_artikelnummer,match1,match2,warengruppe,hersteller_artikelnummer,lieferentenid,ean,bezeichnung,lager_anzahl, reserviert_anzahl,last_update, last_update_agent, agent_operation, mandant_id) VALUES \n");

					}

					_guid = artikeln.get(j).getTaifun_guid();
					_interne_artikelnummer = artikeln.get(j).getInterne_artikelnummer();

					_match1 = artikeln.get(j).getMatch1();
					_match2 = artikeln.get(j).getMatch2();
					_waregruppe = artikeln.get(j).getWaregruppe();
					_hersteller_artikelnummer = artikeln.get(j).getHersteller_artikelnummer();
					_lieferantenid = artikeln.get(j).getLieferantenid();
					_ean = artikeln.get(j).getEan();
					_bezeichnung = artikeln.get(j).getBezeichnung();
					int _lager_anzahl = artikeln.get(j).getIm_lager_anzahl();
					int _reserviert_anzahl = artikeln.get(j).getAuf_lieferschein_anzahl();

					buf.append(" ('" + _guid + "','" + _interne_artikelnummer + "','" + _match1
						+ "','" + _match2 + "','" + _waregruppe + "','"
						+ _hersteller_artikelnummer + "'," + _lieferantenid + ",'" + _ean
						+ "','" + _bezeichnung + "'," + _lager_anzahl + "," + _reserviert_anzahl
						+ ",'" + db_date_formate.format(new Date()) + "','" + this.AGENT_DBID
						+ "','- ARTIKEL INITIAL INSERT,  Lager initial: [" + _lager_anzahl
						+ "], reserviert initial:[" + _reserviert_anzahl + "],   DATEI: [ "
						+ file_creation_code + " ] am: "
						+ long_datum_zeit_formate.format(new Date()) + " \n\n',"
						+ AUSWAHL_MANDANT_DBID + "),\n");

					sqlZise++;
					anzahl_erzeugter_SQL_objekte_der_datei++;

					// Wenn das maximale Limit erreicht
					// wurde
					// Oder die Artikelliste geht zu ende,
					// dann
					// wird
					// ein Semikolon eingefuegt und das
					// Insert
					// in die Datenbank geschrieben

					if (this.SQL_MAX_COLLECTION_SIZE == sqlZise
						&& artikeln.size() > anzahl_erzeugter_SQL_objekte_der_datei) {

					    sqlZise = 0;

					    String korr = new String(
						    (String) buf.toString().subSequence(0, buf.length() - 2));

					    buf.delete(0, buf.length() - 1);

					    buf.append(korr);

					    buf.append(";");

					    try {

						DB_CONNECTOR.insertToDatabase(buf.toString(), "");

						this.AGENT_DEBUG_INFO.addMassage("Es wurden bereits [ "
							+ (anzahl_erzeugter_SQL_objekte_der_datei) + "] von [ "
							+ artikeln.size()
							+ "] Artikel in die Datenbank geschrieben. \n");

						buf.delete(0, buf.length());
						buf.setLength(0);

					    } catch (com.mysql.jdbc.PacketTooBigException ezu) {

						FILE_PROCESS_END_TIME = new Date();

						this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
							.toSeconds(FILE_PROCESS_END_TIME.getTime()
								- FILE_PROCESS_START_TIME.getTime());

						this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] >  "
							+ this.single_file_full_process_diff_time_in_sec
							+ " Sekunden =  "
							+ (this.single_file_full_process_diff_time_in_sec / 60)
							+ " Minuten \n");

						String system_return_file_name = "ERROR_(PacketTooBigException)_"
							+ this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
							+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
							+ this.AGENT_FILE_NAME_PATTERN + "_"
							+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_"
							+ (i + 1) + "_" + file_creation_code + "_deliver.xml";

						File return_xml_file = new File(this.AGENT_FILE_DELIVER_PATH
							+ SYSTEM_PATH_DELIMITER + system_return_file_name);

						work_xml_file.renameTo(return_xml_file);

						this.AGENT_DEBUG_INFO
							.addMassage("[DATEI WIRD NACH >> DELIVER << VERSCHOBEN] \n");

						DB_CONNECTOR.updateOnDatabase(
							"DELETE from work_agent_files where file_creation_code='"
								+ file_creation_code + "';");

						this.AGENT_DEBUG_INFO
							.addMassage("[DATEI WIRD NACH >> DELIVER << VERSCHOBEN] \n");

						DB_CONNECTOR.updateOnDatabase(
							"DELETE from work_agent_files where file_creation_code='"
								+ file_creation_code + "';");

						this.AGENT_DEBUG_INFO.addMassage(
							"[TABELE static_artikel] Artikel der Datei wurden von der DB geloescht. \n");

						this.AGENT_DEBUG_INFO
							.addMassage("ERROR: IN DATEI [" + file_creation_code + "]\n");

						this.AGENT_DEBUG_INFO.addMassage(ezu.getMessage() + "\n");

					    }
					    

					}

					if (artikeln.size() == anzahl_erzeugter_SQL_objekte_der_datei) {

					    this.AGENT_DEBUG_INFO.addMassage("Insert ende\n");

					    String korr = new String(
						    (String) buf.toString().subSequence(0, buf.length() - 2));

					    buf.delete(0, buf.length() - 1);

					    buf.append(korr);

					    buf.append(";");

					    try {

						DB_CONNECTOR.insertToDatabase(buf.toString(), "");

						this.AGENT_DEBUG_INFO.addMassage("Es wurden " + sqlZise
							+ " XML-Objekte in die Datenbank geschrieben. \n");

						this.AGENT_DEBUG_INFO.addMassage("Es wurden bereits [ "
							+ (anzahl_erzeugter_SQL_objekte_der_datei) + "] von [ "
							+ artikeln.size()
							+ "] Artikel in die Datenbank geschrieben. \n - INSERT ENDE \n");

						buf.delete(0, buf.length());
						buf.setLength(0);

						FILE_PROCESS_END_TIME = new Date();

						this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
							.toSeconds(FILE_PROCESS_END_TIME.getTime()
								- FILE_PROCESS_START_TIME.getTime());

						this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] >  "
							+ this.single_file_full_process_diff_time_in_sec
							+ " Sekunden =  "
							+ (this.single_file_full_process_diff_time_in_sec / 60)
							+ " Minuten \n");

						String system_processed_file_name = this.AGENT_RUN_CREATION_CODE
							+ this.AUSWAHL_MANDANT_NAME + this.MANAGER_START_POSITION + "_"
							+ this.AGENT_NAME + "_" + this.AGENT_FILE_NAME_PATTERN + "_"
							+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_"
							+ (i + 1) + "_" + file_creation_code + "_proccesed.xml";

						File processed_xml_file = new File(this.AGENT_FILE_PROCESSED_PATH
							+ SYSTEM_PATH_DELIMITER + system_processed_file_name);

						work_xml_file.renameTo(processed_xml_file);

						this.AGENT_DEBUG_INFO
							.addMassage("[DATEI WIRD NACH >> PROCESSED << VERSCHOBEN] \n");

						DB_CONNECTOR.updateOnDatabase(
							"UPDATE work_agent_files set prozess_end_time='"
								+ db_date_formate.format(FILE_PROCESS_END_TIME)
								+ "', file_status_id=3," + " datensaetze_anzahl="
								+ anzahl_XML_objekte_pro_datei + ", work_filename='"
								+ system_work_file_name + "', processed_filename='"
								+ system_processed_file_name + "' " //
								+ " where file_creation_code='" + file_creation_code
								+ "';");

					    } catch (com.mysql.jdbc.PacketTooBigException ezu) {

						FILE_PROCESS_END_TIME = new Date();

						this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
							.toSeconds(FILE_PROCESS_END_TIME.getTime()
								- FILE_PROCESS_START_TIME.getTime());

						this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] >  "
							+ this.single_file_full_process_diff_time_in_sec
							+ " Sekunden =  "
							+ (this.single_file_full_process_diff_time_in_sec / 60)
							+ " Minuten \n");

						String system_return_file_name = "ERROR_(PacketTooBigException)_"
							+ this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
							+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
							+ this.AGENT_FILE_NAME_PATTERN + "_"
							+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_"
							+ (i + 1) + "_" + file_creation_code + "_deliver.xml";

						File return_xml_file = new File(this.AGENT_FILE_DELIVER_PATH
							+ SYSTEM_PATH_DELIMITER + system_return_file_name);

						work_xml_file.renameTo(return_xml_file);

						this.AGENT_DEBUG_INFO
							.addMassage("[DATEI WIRD NACH >> DELIVER << VERSCHOBEN] \n");

						DB_CONNECTOR.updateOnDatabase(
							"DELETE from work_agent_files where file_creation_code='"
								+ file_creation_code + "';");

						this.AGENT_DEBUG_INFO.addMassage(
							"[TABELE static_artikel] Artikel der Datei wurden von der DB geloescht. \n");

						this.AGENT_DEBUG_INFO.addMassage("ERROR:\n");

						this.AGENT_DEBUG_INFO.addMassage(ezu.getMessage() + "\n");

					    }

					}

				    }

				}

			    } catch (com.mysql.jdbc.PacketTooBigException e) {

				FILE_PROCESS_END_TIME = new Date();

				this.single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
					.toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

				this.AGENT_DEBUG_INFO.addMassage("[DATEI VERARBEITUNGSDAUER] >  "
					+ this.single_file_full_process_diff_time_in_sec + " Sekunden =  "
					+ (this.single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");

				String system_return_file_name = "ERROR_(PacketTooBigException)_"
					+ this.AGENT_RUN_CREATION_CODE + this.AUSWAHL_MANDANT_NAME
					+ this.MANAGER_START_POSITION + "_" + this.AGENT_NAME + "_"
					+ this.AGENT_FILE_NAME_PATTERN + "_"
					+ long_date_formate.format(FILE_PROCESS_START_TIME) + "_" + (i + 1) + "_"
					+ file_creation_code + "_deliver.xml";

				File return_xml_file = new File(
					this.AGENT_FILE_DELIVER_PATH + SYSTEM_PATH_DELIMITER + system_return_file_name);

				work_xml_file.renameTo(return_xml_file);

				this.AGENT_DEBUG_INFO.addMassage("[DATEI WIRD NACH >> DELIVER << VERSCHOBEN] \n");

				DB_CONNECTOR.updateOnDatabase("DELETE from work_agent_files where file_creation_code='"
					+ file_creation_code + "';");

				this.AGENT_DEBUG_INFO.addMassage("[DATEI WIRD NACH >> DELIVER << VERSCHOBEN] \n");

				DB_CONNECTOR.updateOnDatabase("DELETE from work_agent_files where file_creation_code='"
					+ file_creation_code + "';");

				this.AGENT_DEBUG_INFO.addMassage(
					"[TABELE static_artikel] Artikel der Datei wurden von der DB geloescht. \n");

				this.AGENT_DEBUG_INFO.addMassage("ERROR: IN DATEI [" + file_creation_code + "]\n");

				this.AGENT_DEBUG_INFO.addMassage(e.getMessage() + "\n");

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
			    "-------------------------------------------------------------------------------- \n");

		    String checkSumAllArtikel = "Select count(*) from static_artikel;";

		    int artikelGesamt = 0;

		    DB_CONNECTOR.readFromDatabase(checkSumAllArtikel, "checkSumAllArtikel");

		    while (DB_CONNECTOR.rs.next()) {
			artikelGesamt = DB_CONNECTOR.rs.getInt(1);
		    }

		    this.AGENT_DEBUG_INFO
			    .addMassage("Es wurden " + artikelGesamt + " Artikel in die Datenbank geladen.\n");

		    this.AGENT_END_PROCESS_TIME = new Date();

		    this.agent_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
			    .toSeconds(AGENT_END_PROCESS_TIME.getTime() - AGENT_START_PROCESS_TIME.getTime());

		    this.AGENT_DEBUG_INFO.addMassage("[ENDE " + this.AGENT_NAME + "]\n");

		    this.AGENT_DEBUG_INFO.addMassage("[PROZESS-TIME] > " + agent_full_process_diff_time_in_sec
			    + " Sekunden =  " + (agent_full_process_diff_time_in_sec / 60) + " Minuten \n");

		    // -----------------------------
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
