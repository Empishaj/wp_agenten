package UPDATE_MANAGER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
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

public class event_agent_artikel extends Agent implements java.io.Serializable {

    private static final long serialVersionUID = -5998327442337384471L;

    public event_agent_artikel(int _managerid, String _manager_run_creation_code, boolean _isLinux, String _opsystem,
	    String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
	    throws SQLException, ClassNotFoundException {

	super();

	this.DB_CONNECTOR = _db;
	this.AGENT_DBID = 993;
	this.FILE_CONTENT_ID = 0;
	this.MANAGER_RUN_CODE = _manager_run_creation_code;
	this.RUN_MANAGER_ID = _managerid;
	this.IS_LINUX = _isLinux;
	this.OPERATING_SYSTEM = _opsystem;
	this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
	this.AGENT_DUPLIKAT_PATH = _duplikat_path;
	this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
	this.initialisiere_agenten_000(this.DB_CONNECTOR,this.AGENT_DBID);
	this.initialisiere_agenten_pfade(DB_CONNECTOR);

    }

    public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
	    String mandant_kuerzel)
	    throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

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
	int _reserviert_anzahl = 0;
	boolean ArtBestand = false;
	boolean ArtRV = false;

	this.AUSWAHL_MANDANT_DBID = mandantid;
	this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;
	this.FILE_CREATION_CODE = _fileid;
	this.ANZAHL_XML_OBJEKTE = 0;

	/*
	 * Hier ist der Aktivitaetsschalter des Agenten der von der Datenbank
	 * gesteuert werden kann.
	 * 
	 */
	if (this.AGENT_ACTIVITY == true) {

	    this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
		    + "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
		    + this.MANAGER_RUN_CODE + "] \n");

	    
	    /*
	     * 
	     * Die aktuelle XML-Datei die verarbeitet wird
	     */
	    AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

	    // -----------------
	    
	    this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

	    update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);

	    // -----------------

	    /*
	     * 
	     * Hier wird geprueft ob die Datei schon mal verarbeitet wurde
	     */
	    
	    this.XML_FILE_WAS_EVER_PROCESSED = wurde_diese_datei_schon_mal_verarbeitet(AGENT_DELIVER_XML_FILE,
		    DB_CONNECTOR, this.AGENT_DBID);

	    if (XML_FILE_WAS_EVER_PROCESSED == false) {
		// -----------------------------------------------------------------------

		Path absolut_path = Paths.get(this.AGENT_DELIVER_XML_FILE.getAbsolutePath());

		// Die Attribute der Datei werden ermittelt
		BasicFileAttributes datei_attribute = Files.readAttributes(absolut_path, BasicFileAttributes.class);

		this.FILE_CREATION_TIME_FOR_DB = formatdb
			.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

		this.FILE_CREATION_TIME = format0
			.format(new Date(datei_attribute.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

		this.FILE_SIZE = datei_attribute.size();

		this.SINGEL_FILE_PROCESS_START_TIME = new Date();

		this.put_deliver_file_in_db(AGENT_DBID, this.AGENT_DELIVER_XML_FILE.getName(),
			formatdb.format(SINGEL_FILE_PROCESS_START_TIME), this.AGENT_RUN_CODE, this.FILE_SIZE,
			FILE_CREATION_TIME_FOR_DB, FILE_CREATION_CODE, this.AGENT_INSTANZ_RUN_DBID, MD5_FILE_HASHCODE,
			DB_CONNECTOR, this.AUSWAHL_MANDANT_DBID);

		this.WORK_FILE_DB_ID = this.find_work_file_dbid(FILE_CREATION_CODE, DB_CONNECTOR);

		this.DEBUGG_LOGGER.addMassage("Datei: " + this.AGENT_DELIVER_XML_FILE.getName()
			+ " wurde in die Tabelle: work_agent_files hinzugefuegt.");

		// -----------------------------------

		this.AGENT_WORK_XML_FILE_NAME = this.MANAGER_RUN_CODE + "_" + (MANAGER_filepositionsindex + 1) + "_"
			+ this.AGENT_NAME + "_" + this.AGENT_FILE_NAME_PATTERN + "_" + FILE_CREATION_TIME + "_work"
			+ this.AUSWAHL_MANDANT_NAME + FILE_CREATION_CODE + ".xml";

		// ------------------------------------------

		this.AGENT_WORK_FILE_XML = new File(
			this.AGENT_FILE_WORK_PATH + SYSTEM_PATH_DELIMITER + AGENT_WORK_XML_FILE_NAME);

		this.AGENT_DELIVER_XML_FILE.renameTo(AGENT_WORK_FILE_XML);
		// ------------------------------------------

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(AGENT_WORK_FILE_XML);
		doc.getDocumentElement().normalize();

		// ------------------------------------------------------

		NodeList nListHeadAttributes = doc.getElementsByTagName("ArtList");
		Node node = nListHeadAttributes.item(0);
		Element e = (Element) node;

		setFileOperationType(new String(e.getAttribute("Operation")));

		// ------------------------------------------------------

		NodeList artikel_root_node = doc.getElementsByTagName("Art");

		this.ANZAHL_XML_OBJEKTE = artikel_root_node.getLength();

		// ------------------------------------------------------

		for (int fin = 0; fin < this.ANZAHL_XML_OBJEKTE; fin++) {

		    _guid = "";
		    _interne_artikelnummer = "";
		    _match1 = "";
		    _match2 = "";
		    _waregruppe = "";
		    _hersteller_artikelnummer = "";
		    _lieferantenid = 0;
		    _ean = "";
		    _bezeichnung = "";
		    _lager_anzahl = 0;
		    _reserviert_anzahl = 0;

		    Node nNode = artikel_root_node.item(fin);

		    if (nNode.getNodeType() == Node.ELEMENT_NODE) {

			Element artikel_single_element = (Element) nNode;

			// Aktueller Bestand
			NodeList bestandliste = artikel_single_element.getElementsByTagName("ArtBestand");

			// Aktuelle Reservierungen
			NodeList auftrag_liste = artikel_single_element.getElementsByTagName("ArtRV");

			if (auftrag_liste.getLength() > 0) {
			    ArtRV = true;
			    this.DEBUGG_LOGGER.addMassage("Protokolliert einen Auftrag (Reservierung). ArtRV = true\n");

			} else {

			    ArtRV = false;
			}

			if (bestandliste.getLength() > 0) {

			    ArtBestand = true;
			    this.DEBUGG_LOGGER.addMassage(
				    "Datei: Protokolliert einen Wareneingang (Bestandsaenderung). ArtBestand=true \n");

			} else {
			    ArtBestand = false;
			}
			// EVENT-DATEI
			// ----------------------------------------------------------------------------------
			// Ein neuer Artikel wurde erzeugt

			try {

			    _guid = artikel_single_element.getElementsByTagName("GUID").item(0).getTextContent()
				    .replaceAll(
					    "[^A-Za-z0-9\\u0027\\u2019\\u02BC\\u0313\\u0315\\u2022\\u2212\\u002D\\u2013]",
					    "")
				    .trim();

			} catch (NullPointerException ge) {
			    _guid = "";
			}

			try {

			    _interne_artikelnummer = artikel_single_element.getElementsByTagName("Nr").item(0)
				    .getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "").trim();

			} catch (java.lang.NullPointerException ge) {
			    _interne_artikelnummer = "";
			}

			try {

			    _bezeichnung = artikel_single_element.getElementsByTagName("Langtext").item(0)
				    .getTextContent().replaceAll(
					    "[^A-Za-z0-9\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2212\\u002D\\u2013]",
					    "");

			} catch (java.lang.NullPointerException ge) {
			    _bezeichnung = "";
			}

			try {

			    _match1 = artikel_single_element.getElementsByTagName("Match1").item(0).getTextContent()
				    .replaceAll(
					    "[^A-Za-z0-9\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2212\\u002D\\u2013]",
					    "");

			} catch (java.lang.NullPointerException ge) {
			    _match1 = "";
			}

			try {

			    _match2 = artikel_single_element.getElementsByTagName("Match2").item(0).getTextContent()

				    .replaceAll(
					    "[^A-Za-z0-9+-\\u00c4\\u00e4\\u00d6\\u00f6\\u00dc\\u00fc\\u00df\\u0020\\u2212\\u002D\\u2013]",
					    "");

			} catch (java.lang.NullPointerException ge) {
			    _match2 = "";
			}

			try {

			    _waregruppe = artikel_single_element.getElementsByTagName("WHG").item(0).getTextContent()
				    .replaceAll("[^A-Za-z0-9\\u2024\\u002E\\u002D]", "");

			} catch (java.lang.NullPointerException ge) {
			    _waregruppe = "";
			}

			try {

			    _hersteller_artikelnummer = artikel_single_element.getElementsByTagName("HstNr").item(0)
				    .getTextContent().replaceAll("[^A-Za-z0-9]", "");

			} catch (java.lang.NullPointerException ge) {
			    _hersteller_artikelnummer = "";
			}

			try {

			    _lieferantenid = Integer.parseInt(artikel_single_element.getElementsByTagName("BhGk")
				    .item(0).getTextContent().replaceAll("[^A-Za-z0-9+-]", ""));

			} catch (java.lang.NullPointerException ge) {

			    _lieferantenid = 0;
			}

			try {

			    _ean = artikel_single_element.getElementsByTagName("EAN").item(0).getTextContent()
				    .replaceAll("[^A-Za-z0-9\\u2024\\u002E\\u002D]", "").trim();

			} catch (NullPointerException gh) {
			    _ean = "";
			}

			// -------------------------------------------------------------

			if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

			    try {

				this.FILE_CONTENT_ID = FileTypes.ARTIKEL_NEU_ERSTELLT_FILE;

				this.DEBUGG_LOGGER.addMassage("#{ART_INSERT_001} - Neuer Artikel\n");

				this.DEBUGG_LOGGER.addMassage("Mandant == " + this.AUSWAHL_MANDANT_DBID + " - "
					+ this.AUSWAHL_MANDANT_NAME + " \n");

				String SQL_insert_new_product = "INSERT INTO static_artikel (guid_taifun, interne_artikelnummer, match1, match2, warengruppe, hersteller_artikelnummer, lieferentenid, ean, bezeichnung, lager_anzahl, reserviert_anzahl, last_update_agent, last_update,agent_operation, favorit, mandant_id  ) VALUES "
					+ " ('" + _guid + "','" + _interne_artikelnummer + "','" + _match1 + "','"
					+ _match2 + "','" + _waregruppe + "','" + _hersteller_artikelnummer + "',"
					+ _lieferantenid + ",'" + _ean + "','" + _bezeichnung + "'," + _lager_anzahl
					+ "," + _reserviert_anzahl + "," + AGENT_DBID + ",'"
					+ formatdb.format(new Date()) + "','- INSERT ARTIKERL NEU am: "
					+ format2.format(new Date()) + "\n',1," + this.AUSWAHL_MANDANT_DBID + ");";

				this.DEBUGG_LOGGER.addMassage("Der neue Artikel [" + _interne_artikelnummer
					+ "] wurde in static_artikel hinzugefuegt. \n");

				DB_CONNECTOR.insertToDatabase(SQL_insert_new_product, "SQL_insert_new_product");

				DB_CONNECTOR.updateOnDatabase("UPDATE static_artikel art INNER JOIN "
					+ " static_warengruppe wart ON art.warengruppe=wart.kurz "
					+ " SET art.warungruppe_id=wart.id WHERE art.interne_artikelnummer='"
					+ _interne_artikelnummer + "';");

				this.DEBUGG_LOGGER.addMassage("Der neue Artikel [" + _interne_artikelnummer
					+ "] wurde um die Warengruppe ergaenzt \n");

			    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ctsu) {

				this.DEBUGG_LOGGER.addMassage("Der neue Artikel [" + _interne_artikelnummer
					+ "] war bereits in static_artikel vorhanden. \n");
			    }

			} // Ende neuer Artikel

			// // Artikel UPDATE

			if (this.XML_CONTENT_IS_INSERT_TYPE == false) {

			    this.DEBUGG_LOGGER.addMassage("#{ART_UPDATE_001}");

			    this.FILE_CONTENT_ID = FileTypes.ARTIKEL_INHALTLICH_VERAENDERT_FILE;

			    String oldLogArtikel = "";

			    String altBezeichnung = "";

			    String selectOldArtikellog = "Select agent_operation, bezeichnung from static_artikel where interne_artikelnummer='"
				    + _interne_artikelnummer + "' and mandant_id=" + this.AUSWAHL_MANDANT_DBID + ";";

			    DB_CONNECTOR.readFromDatabase(selectOldArtikellog, "selectOldArtikellog");

			    while (DB_CONNECTOR.rs.next()) {

				oldLogArtikel = DB_CONNECTOR.rs.getString(1);
				altBezeichnung = DB_CONNECTOR.rs.getString(2);
			    }

			    String newLogArtikel = "- UPDATE #{U_002},  Artikel wurde inhaltlich veraendert:\n---- Inhalt: [a="
				    + altBezeichnung + ", n=" + _bezeichnung + "],\n---- Warengruppe:[" + _waregruppe
				    + "],\n---- Lieferant-ID: [" + _lieferantenid + "]\n---- am: "
				    + format2.format(new Date()) + "\n---- Datei: [" + this.FILE_CREATION_CODE + "]";

			    String dblog = this.LOG_CHECKER.generateNewLogForDB(oldLogArtikel, newLogArtikel);

			    DB_CONNECTOR.updateOnDatabase("UPDATE static_artikel set bezeichnung='" + _bezeichnung
				    + "', agent_operation='" + dblog + "', warengruppe='" + _waregruppe
				    + "',  lieferentenid=" + _lieferantenid + ", match1='" + _match1 + "' , match2='"
				    + _match2 + "' where interne_artikelnummer='" + _interne_artikelnummer
				    + "' and mandant_id=" + this.AUSWAHL_MANDANT_DBID + ";");

			    DB_CONNECTOR.updateOnDatabase(
				    "UPDATE static_artikel ab INNER JOIN static_warengruppe da on ab.warengruppe=da.kurz set ab.warungruppe_id=da.id where interne_artikelnummer='"
					    + _interne_artikelnummer + "' ;");

			    this.DEBUGG_LOGGER.addMassage("Der Artikel wurde inhaltlich aktualiesert: \n");

			    this.DEBUGG_LOGGER.addMassage("Inhalt: [" + _bezeichnung + "],\n---- Warengruppe:["
				    + _waregruppe + "],\n---- Lieferant-ID: [" + _lieferantenid + "]\n---- am: "
				    + format2.format(new Date()) + "\n---- Datei: [" + this.FILE_CREATION_CODE
				    + "]\n\n");

			}

			// ----------------------------------------------------------------------------------

			// Ein Auftrag wurde angelegt, die Menge muss reserviert
			// werden. Protokoll

			if (this.XML_CONTENT_IS_INSERT_TYPE == true && ArtBestand == false && ArtRV == true) {

			    this.FILE_CONTENT_ID = FileTypes.ARTIKEL_PROTOKOL_ZU_AUFTRAG;

			    this.DEBUGG_LOGGER.addMassage("#{U_002} Auftrag Protokoll");

			    this.DEBUGG_LOGGER.addMassage("- Ein neuer Auftrag wurde erzeugt:\n" + "Datei: ["
				    + this.FILE_CREATION_CODE + "]");

			}

			// ----------------------------------------------------------------------------------

			// Protokoll-Datei zum Wareneingang. Wird
			// vernachlaessigt
			// wenn eine Bestandsdatei mit der GUID bereits
			// verarbeitet wurde. Protokoll Datei

			if (this.XML_CONTENT_IS_INSERT_TYPE == true && ArtBestand == true && ArtRV == false) {

			    this.FILE_CONTENT_ID = FileTypes.ARTIKEL_PROTOKOL_ZU_WARENEINGANG;

			    this.DEBUGG_LOGGER.addMassage("#{U_003} Protokoll zum  Wareneingang");

			    this.DEBUGG_LOGGER.addMassage("- Ein neuer Wareneingang wurde erzeugt:" + "Datei: ["
				    + this.FILE_CREATION_CODE + "]\n");

			}
		    }
		}

		this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN]");

		// ----------------------------------------------------------------------------------------------------------
		// Verschiebe Datei
		// ----------------------------------------------------------------------------------------------------------

		AGENT_PROCESSED_XML_FILE_NAME = getSystem_processed_file_name(MANAGER_filepositionsindex);

		File AGENT_PROCESSED_XML_FILE = new File(
			this.AGENT_FILE_PROCESSED_PATH + SYSTEM_PATH_DELIMITER + AGENT_PROCESSED_XML_FILE_NAME);

		AGENT_WORK_FILE_XML.renameTo(AGENT_PROCESSED_XML_FILE);

		// ----------------------------------------------------------------------------------------------------------

		SINGEL_FILE_PROCESS_END_TIME = new Date();

		this.FILE_STATUS_ID = 3;

		this.beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
			this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
			AGENT_PROCESSED_XML_FILE_NAME, DB_CONNECTOR);

		this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
			+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

		// ----------------------------------------------------------------------------------------------------------
		// Beende Agenten OBJEKT
		// ----------------------------------------------------------------------------------------------------------

		this.AGENT_END_PROCESS_TIME = new Date();

		this.beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, DB_CONNECTOR);

		// ----------------------------------------------------------------------------------------------------------
		// Beende Agenten RUN
		// ----------------------------------------------------------------------------------------------------------

		this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_ERFOLGREICH_BEENDET;

		this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
			this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
			this.DEBUGG_LOGGER.debug_string.toString(), DB_CONNECTOR);

		// ----------------------------------------------------------------------------------------------------------

		this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE
			+ ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

		this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
			+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

		// ----------------------------------------------------------------------------------------------------------

	    }

	    if (XML_FILE_WAS_EVER_PROCESSED == true) {

		String file_duplikat_label = "";

		String findeDuplikaSQL = "SELECT concat(file_creation_code,'_',id)  FROM  work_agent_files where filehash='"
			+ this.MD5_FILE_HASHCODE + "';";

		this.DB_CONNECTOR.readFromDatabase(findeDuplikaSQL, "findeDuplikaSQL");

		while (this.DB_CONNECTOR.rs.next()) {

		    file_duplikat_label = this.DB_CONNECTOR.rs.getString(1);

		}

		this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >> xfwep_DUPLIKATE << VERSCHOBEN]");

		// ----------------------------------------------------------------------------------------------------------
		// Verschiebe Datei
		// ----------------------------------------------------------------------------------------------------------

		Path path = Paths.get(AGENT_DELIVER_XML_FILE.getAbsolutePath());

		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

		String file_creation_time = format1.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

		String xml_file_duplikate_file_name = "xfwep_duplikat_" + file_creation_time + "_" + this.AGENT_NAME
			+ "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + MANAGER_filepositionsindex
			+ "_OFILE_" + file_duplikat_label + ".xml";

		File xml_file_duplikate = new File(
			this.AGENT_DUPLIKAT_PATH + SYSTEM_PATH_DELIMITER + xml_file_duplikate_file_name);

		AGENT_DELIVER_XML_FILE.renameTo(xml_file_duplikate);

		// ----------------------------------------------------------------------------------------------------------
		// Beende Datei OBJEKT
		// ----------------------------------------------------------------------------------------------------------

		SINGEL_FILE_PROCESS_END_TIME = new Date();

		this.FILE_STATUS_ID = 4;

		this.beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
			this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
			AGENT_PROCESSED_XML_FILE_NAME, DB_CONNECTOR);

		this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
			+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME));

		// ----------------------------------------------------------------------------------------------------------
		// Beende Agenten OBJEKT
		// ----------------------------------------------------------------------------------------------------------

		this.AGENT_END_PROCESS_TIME = new Date();

		this.beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, DB_CONNECTOR);

		// ----------------------------------------------------------------------------------------------------------
		// Beende Agenten RUN
		// ----------------------------------------------------------------------------------------------------------

		this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITUNG_ABGEBROCHEN_DUPLIKAT;

		this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
			this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
			this.DEBUGG_LOGGER.debug_string.toString(), DB_CONNECTOR);

		// ----------------------------------------------------------------------------------------------------------

		this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE
			+ ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

		this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
			+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME));

		// ----------------------------------------------------------------------------------------------------------

	    }
	} else {

	    // ----------------------------------------------------------------------------------------------------------
	    // Beende Datei OBJEKT
	    // ----------------------------------------------------------------------------------------------------------

	    this.AGENT_END_PROCESS_TIME = new Date();

	    beende_agenten_objekt(this.AGENT_END_PROCESS_TIME, AGENT_DBID, DB_CONNECTOR);

	    // ----------------------------------------------------------------------------------------------------------
	    // Beende Agenten RUN
	    // ----------------------------------------------------------------------------------------------------------

	    this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_DEAKTIV;

	    this.beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
		    this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
		    this.DEBUGG_LOGGER.debug_string.toString(), DB_CONNECTOR);

	    // -----------------

	    this.DEBUGG_LOGGER.addMassage("[ AGENT IST DEAKTIVIERT ]");
	}
    }
}
