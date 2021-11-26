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

public class event_agent_lieferschein extends Agent implements java.io.Serializable {

    private static final long serialVersionUID = 7848115657717666099L;

    String liefernummer = "";
    String datum_liefernummer = "";
    String guid = "";
    String kundennummer = "";
    boolean gebucht = false;
    boolean fakturiert = false;
    String auftragsnummer = "";
    boolean auftrag_vorhanden = false;
    int position = 0;

    String filetype_operation_content = "?";

    int menge = 0;
    int mengeLager = 0;
    int mengeFak = 0;
    int mengelieferschein = 0;
    String interne_artikel_nummer = "";
    int position_im_auftrag = 0;

    String rechnungsNummer = "";

    public event_agent_lieferschein(int _managerid, String _manager_run_creation_code, boolean _isLinux,
	    String _opsystem, String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
	    throws SQLException, ClassNotFoundException {

	super();
	this.DB_CONNECTOR = _db;
	this.AGENT_DBID = 997;
	this.FILE_CONTENT_ID = 0;
	this.MANAGER_RUN_CODE = _manager_run_creation_code;
	this.RUN_MANAGER_ID = _managerid;
	this.IS_LINUX = _isLinux;
	this.OPERATING_SYSTEM = _opsystem;
	this.SYSTEM_PATH_DELIMITER = _system_path_delimiter;
	this.AGENT_DUPLIKAT_PATH = _duplikat_path;
	this.AGENT_NAME = find_agent_name_by_id(this.AGENT_DBID, DB_CONNECTOR);
	this.initialisiere_agenten_000(this.DB_CONNECTOR,this.AGENT_DBID);
	initialisiere_agenten_pfade(this.DB_CONNECTOR);
 

    }

    public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
	    String mandant_kuerzel)
	    throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

	this.auftrag_vorhanden = false;

	this.XML_FILE_WAS_EVER_PROCESSED = true;
	this.auftrag_vorhanden = false;
	this.liefernummer = "?";
	this.FILE_CREATION_CODE = _fileid;
	this.AUSWAHL_MANDANT_DBID = mandantid;
	this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;
	this.ANZAHL_XML_OBJEKTE = 0;

	if (this.AGENT_ACTIVITY == true) {

	    this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
		    + "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
		    + this.MANAGER_RUN_CODE + "]");

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

		// Lese XML Datei ein

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(AGENT_WORK_FILE_XML);

		doc = dBuilder.parse(AGENT_WORK_FILE_XML);

		doc.getDocumentElement().normalize();

		NodeList lieferschein_kopf_node = doc.getElementsByTagName("LhList");

		Node node_kopf_wurzel = lieferschein_kopf_node.item(0);

		Element node_kopf_wurzel_element = (Element) node_kopf_wurzel;

		Element lieferschein_kopf_element = (Element) (doc.getElementsByTagName("Lh").item(0));

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

		// Hole Auftragsnummer falls vorhanden
		// ------------------------------------------

		NodeList refAuftragNummer = lieferschein_kopf_element.getElementsByTagName("RefAuftrag");

		if (refAuftragNummer.getLength() > 0) {

		    auftrag_vorhanden = true;

		    try {

			this.auftragsnummer = lieferschein_kopf_element.getElementsByTagName("RefAuftrag").item(0)
				.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

			if (this.auftragsnummer.trim() == "") {

			    auftrag_vorhanden = false;
			    auftragsnummer = "?";
			}

		    } catch (NullPointerException ecxc) {
			auftragsnummer = "?";
			auftrag_vorhanden = false;
		    }

		} else {

		    auftrag_vorhanden = false;
		    auftragsnummer = "?";
		}

		// {initialisiere Kopf des Lieferscheins}
		// -----------------------------------------------------------------------

		try {

		    liefernummer = lieferschein_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
			    .replaceAll("[^A-Za-z0-9+-]", "").trim();

		} catch (NullPointerException ecxc) {
		    liefernummer = "?";
		}

		try {

		    guid = lieferschein_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
			    .replaceAll("[^A-Za-z0-9+-]", "").trim();

		} catch (NullPointerException ecxc) {
		    guid = "?";
		}

		try {

		    String tempDatumWareneingangAusXML = "";
		    Date datum_wareneingang_temp = null;

		    tempDatumWareneingangAusXML = lieferschein_kopf_element.getElementsByTagName("Date").item(0)
			    .getTextContent().trim();

		    datum_wareneingang_temp = XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

		    datum_liefernummer = formatdb.format(datum_wareneingang_temp);

		} catch (NullPointerException ecxc) {
		    datum_liefernummer = "9999-09-09 00:00:00";
		} catch (ParseException e1) {

		    datum_liefernummer = "9999-09-09 00:00:00";
		    e1.printStackTrace();
		}

		try {

		    kundennummer = lieferschein_kopf_element.getElementsByTagName("KdNr").item(0).getTextContent()
			    .replaceAll("[^A-Za-z0-9+-]", "").trim();

		} catch (NullPointerException ecxc) {
		    kundennummer = "?";
		}

		try {

		    gebucht = new Boolean(lieferschein_kopf_element.getElementsByTagName("LhGebucht").item(0)
			    .getTextContent().trim());

		} catch (NullPointerException ecxc) {
		    gebucht = false;
		}

		try {

		    fakturiert = new Boolean(
			    lieferschein_kopf_element.getElementsByTagName("LhDoFak").item(0).getTextContent().trim());

		} catch (NullPointerException ecxc) {
		    fakturiert = false;
		}

		try {

		    rechnungsNummer = lieferschein_kopf_element.getElementsByTagName("RefRechnung").item(0)
			    .getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

		} catch (NullPointerException ecxc) {
		    rechnungsNummer = "?";
		}

		// Pruefe ob dieser Eintrag bereits exisitert
		this.CHECK_HEADER_ENTRY_EXISTENCE = existiert_diese_lieferung(liefernummer);

		// ==INSERT---------------------------------------------------------------------------
		if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

		    // Nur wenn dieser EIntrag existiert
		    if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

			// Referenziert ein Lieferschein nicht auf einen
			// Auftrag, so wirkt sich der Lieferschein
			// Direkt auf das Lager aus.

			String logLieferschein = "";

			if (this.auftrag_vorhanden == false) {
			    logLieferschein = "- LIEFERSCHEIN INSERT - OHNE AUFTRAG\n";

			    this.DEBUGG_LOGGER.addMassage(logLieferschein);

			} else {

			    logLieferschein = "- LIEFERSCHEIN INSERT - MIT AUFTRAG\n";

			    this.DEBUGG_LOGGER.addMassage(logLieferschein);

			}

			this.DEBUGG_LOGGER.addMassage("---- Lieferschein [" + liefernummer + " ] wurde am [ "
				+ datum_liefernummer + " ] erzeugt ");

			String sqlInsertLieferscheinKopf = "INSERT INTO static_lieferschein ( lieferschein_guid, "
				+ " lieferschein_nummer, kundennummer, datum_der_lieferung,  faktura,"
				+ " gebucht,  last_managerruncode, " + " last_agentruncode, " + " last_agent_operation,"
				+ " last_oparation_datum, " + " last_agent_id,"
				+ " insert_work_file_id, rechnungsnummer, mandant_id) VALUES ('" + guid + "','"
				+ liefernummer + "','" + kundennummer + "','" + datum_liefernummer + "'," + fakturiert
				+ "," + gebucht + ",'" + this.MANAGER_RUN_CODE + "','" + this.AGENT_RUN_CODE + "','"
				+ logLieferschein + "','" + formatdb.format(new Date()) + "'," + this.AGENT_DBID + ","
				+ WORK_FILE_DB_ID + ",'" + rechnungsNummer + "', " + this.AUSWAHL_MANDANT_DBID + ");";

			this.DB_CONNECTOR.insertToDatabase(sqlInsertLieferscheinKopf, "sqlInsertLieferscheinKopf");

			// =Verarbeite
			// Positionen---------------------------------------------------------------------

			NodeList lieferscheinpositionen = lieferschein_kopf_element.getElementsByTagName("Lp");
			this.ANZAHL_XML_OBJEKTE = lieferscheinpositionen.getLength();

			this.DEBUGG_LOGGER
				.addMassage("- Lieferschein Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE + "");

			this.DEBUGG_LOGGER
				.addMassage("---------------------------------------------------------------------");

			if (this.ANZAHL_XML_OBJEKTE > 0) {

			    for (int position_im_lieferschein = 0; position_im_lieferschein < lieferscheinpositionen
				    .getLength(); position_im_lieferschein++) {

				Node einzelne_positions_wurzel = lieferscheinpositionen.item(position_im_lieferschein);

				if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

				    Element auftragspositionElement = (Element) einzelne_positions_wurzel;

				    try {

					BigDecimal menG = new BigDecimal(
						auftragspositionElement.getElementsByTagName("Rec").item(0)
							.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

					position = menG.intValue();

				    } catch (NullPointerException ecxc) {
					position = 0;
				    }

				    try {

					interne_artikel_nummer = auftragspositionElement.getElementsByTagName("ArtNr")
						.item(0).getTextContent()
						.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

				    } catch (NullPointerException ecxc) {
					interne_artikel_nummer = "?";
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("Menge").item(0).getTextContent());

					menge = menG.intValue();

				    } catch (NullPointerException ecxc) {
					menge = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeLager").item(0).getTextContent());

					mengeLager = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengeLager = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeFak").item(0).getTextContent());

					mengeFak = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengeFak = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeLp").item(0).getTextContent());

					mengelieferschein = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengelieferschein = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("ApRec").item(0).getTextContent());

					position_im_auftrag = menG.intValue();

				    } catch (NullPointerException ecxc) {
					position_im_auftrag = 0;
				    }

				    String newLog1 = "- INSERT LIEFERSCHEIN POSITION:\n---- Artikel: ["
					    + interne_artikel_nummer + "],\n---- Menge=" + menge + ",\n---- Mengelager="
					    + mengeLager + ",\n---- Fakturierte Menge=" + mengeFak
					    + ",\n---- Menge Lieferschein=" + mengelieferschein + "\n---- Datei: ["
					    + this.FILE_CREATION_CODE + "]";

				    String sqlInsertLieferscheinPosition = "Insert static_lieferschein_positionen ("
					    + " position," //
					    + " lieferschein_nummer," //
					    + " artikel_nummer," //
					    + " menge, " //
					    + " mengelager, " //
					    + " mege_faktura, " //
					    + " menge_lieferschein," //
					    + " last_operation," //
					    + " last_operation_datum) VALUES (" + position + " ,'" + liefernummer
					    + "','" + interne_artikel_nummer + "', " + menge + ", " + mengeLager + ", "
					    + mengeFak + "," + mengelieferschein + ", '"
					    + this.LOG_CHECKER.generateNewLogForDB("", newLog1) + "' , '"
					    + formatdb.format(new Date()) + "' );";

				    if (interne_artikel_nummer.contains("WP-") == false
					    && interne_artikel_nummer.contains("WPK-") == false
					    && interne_artikel_nummer != "" && interne_artikel_nummer != "?"
					    && interne_artikel_nummer.length() >= 5) {

					this.DB_CONNECTOR.insertToDatabase(sqlInsertLieferscheinPosition,
						"sqlInsertNewBestellung");

					this.DEBUGG_LOGGER
						.addMassage("INSERT - Positionen werden in die Datenbank eingetragen");

					this.DEBUGG_LOGGER.addMassage("- Lieferschein: [ " + liefernummer
						+ " ],\n---- Auftragsnummer: [ " + this.auftragsnummer
						+ " ],\n---- Kunde: [" + kundennummer + "],\n---- Artikel: ["
						+ interne_artikel_nummer + "]," + " \n---- Menge=" + menge
						+ ",\n---- Mengelager=" + mengeLager + ",\n---- Fakturierte Menge="
						+ mengeFak + ",\n---- Menge Lieferschein=" + mengelieferschein
						+ "\n---- Datei: [" + this.FILE_CREATION_CODE + "]");

					if (this.auftrag_vorhanden == false) {

					    String logdbold = "";
					    String logdbneuArt = "";

					    String SQLbereitsImLagerAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
						    + interne_artikel_nummer + "';";

					    int bereitsImLagerStaticArtikel = 0;

					    this.DB_CONNECTOR.readFromDatabase(SQLbereitsImLagerAusStaticArtikel,
						    "SQLbereitsImLagerAusStaticArtikel");

					    while (this.DB_CONNECTOR.rs.next()) {

						bereitsImLagerStaticArtikel = this.DB_CONNECTOR.rs.getInt(1);
						logdbold = this.DB_CONNECTOR.rs.getString(2);
					    }

					    int neuerLagerBestandAnzahl = bereitsImLagerStaticArtikel - mengeLager;

					    logdbneuArt = "#{IL_0001} - LIEFERSCHEIN NEU - Ohne Auftrag, NUMMER: [ "
						    + liefernummer + " ],\n---- Artikel: [" + interne_artikel_nummer
						    + "],\n---- Lagerbestand: [ alt=" + bereitsImLagerStaticArtikel
						    + " ][ neu= " + neuerLagerBestandAnzahl + " ][ Ausgang="
						    + mengeLager + " ],\n---- am: " + format2.format(new Date())
						    + "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
						    + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
						    + this.FILE_CREATION_CODE + "]";

					    String SQLneuerLagerBestand = "UPDATE static_artikel set lager_anzahl="
						    + neuerLagerBestandAnzahl + ", last_update='"
						    + formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
						    + ", agent_operation='"
						    + this.LOG_CHECKER.generateNewLogForDB(logdbold, logdbneuArt)
						    + "' where interne_artikelnummer='" + interne_artikel_nummer + "';";

					    this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

					    this.DEBUGG_LOGGER.addMassage("---- Artikel:[" + interne_artikel_nummer
						    + "] wurde aktualisiert:");

					    this.DEBUGG_LOGGER
						    .addMassage("---- Lagerbestand a=[" + bereitsImLagerStaticArtikel
							    + "], n=[" + neuerLagerBestandAnzahl + "] Stueck(e).");
					    this.DEBUGG_LOGGER.addMassage(
						    "---- Vom Lager entnommen: " + mengeLager + " Stueck(e).");

					    String guid_verkauf = "LIEFERSCHEIN_" + liefernummer + "_" + position;

					    String SQL_Lieferschein = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
						    + guid_verkauf + "','" + interne_artikel_nummer + "'," + mengeLager
						    + ", '" + datum_liefernummer + "',0);";

					    this.DB_CONNECTOR.insertToDatabase(SQL_Lieferschein, "SQL_Lieferschein");

					}

					if (this.auftrag_vorhanden == true) {

					    boolean isArtikelImAuftragVorhanden = checkExistiertDiePositionBereitsInDemAuftragRef(
						    auftragsnummer, interne_artikel_nummer, position_im_auftrag);

					    if (isArtikelImAuftragVorhanden == false) {

						String logdbold = "";
						String logdbneuArt = "";

						String SQLbereitsImLagerAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
							+ interne_artikel_nummer + "';";

						int bereitsImLagerStaticArtikel = 0;

						this.DB_CONNECTOR.readFromDatabase(SQLbereitsImLagerAusStaticArtikel,
							"SQLbereitsImLagerAusStaticArtikel");

						while (this.DB_CONNECTOR.rs.next()) {

						    bereitsImLagerStaticArtikel = this.DB_CONNECTOR.rs.getInt(1);
						    logdbold = this.DB_CONNECTOR.rs.getString(2);
						}

						int neuerLagerBestandAnzahl = bereitsImLagerStaticArtikel - mengeLager;

						logdbneuArt = "#{IL_0002} -  LIEFERSCHEIN NEU - mit Auftrag  ["
							+ auftragsnummer + "], NUMMER: [ " + liefernummer
							+ " ],\n---- Artikel: [" + interne_artikel_nummer
							+ "],\n---- Lagerbestand: [ alt=" + bereitsImLagerStaticArtikel
							+ " ][ neu= " + neuerLagerBestandAnzahl + " ][ Ausgang="
							+ mengeLager + " ],\n---- am: " + format2.format(new Date())
							+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
							+ this.MANAGER_RUN_CODE + "]\n---- Datei: ["
							+ this.FILE_CREATION_CODE + "]";

						String SQLneuerLagerBestand = "UPDATE static_artikel set lager_anzahl="
							+ neuerLagerBestandAnzahl + ", last_update='"
							+ formatdb.format(new Date()) + "', last_update_agent="
							+ AGENT_DBID + ", agent_operation='"
							+ this.LOG_CHECKER.generateNewLogForDB(logdbold, logdbneuArt)
							+ "' where interne_artikelnummer='" + interne_artikel_nummer
							+ "';";

						this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

						this.DEBUGG_LOGGER.addMassage("---- Artikel:[" + interne_artikel_nummer
							+ "] wurde aktualisiert:");

						this.DEBUGG_LOGGER.addMassage(
							"---- Lagerbestand a=[" + bereitsImLagerStaticArtikel + "], n=["
								+ neuerLagerBestandAnzahl + "] Stueck(e).");

						this.DEBUGG_LOGGER.addMassage(
							"---- Vom Lager entnommen: " + mengeLager + " Stueck(e).");

						String guid_verkauf = "LIEFERSCHEIN_" + liefernummer + "_" + position;

						String SQL_Lieferschein = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
							+ guid_verkauf + "','" + interne_artikel_nummer + "',"
							+ mengeLager + ", '" + datum_liefernummer + "',0);";

						this.DB_CONNECTOR.insertToDatabase(SQL_Lieferschein,
							"SQL_Lieferschein");

					    } else {

						this.DEBUGG_LOGGER.addMassage("Dieser Artikel ["
							+ interne_artikel_nummer + "] ist bereits im Auftrag ["
							+ auftragsnummer + "] vorhanden gewesen. P [" + position
							+ "] Wird nicht verarbeitet.");

					    }

					}

					this.DEBUGG_LOGGER.addMassage(
						"---------------------------------------------------------------------");

				    } else {

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

			    beende_agenten_run(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_DBID, this.AGENT_RUN_CODE,
				    this.MANAGER_RUN_CODE, this.AGENT_END_PROCESS_TIME, this.ANZAHL_XML_OBJEKTE,
				    this.DEBUGG_LOGGER.debug_string.toString(), this.DB_CONNECTOR);

			    // ----------------------------------------------------------------------------------------------------------

			    this.DEBUGG_LOGGER.addMassage("[AGENT-ENDE: " + this.AGENT_NAME + " Agent-RUN= "
				    + this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]");

			    this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
				    + laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");

			    // ----------------------------------------------------------------------------------------------------------

			} // Positionen > 0

			else {

			    this.DEBUGG_LOGGER.addMassage("Lieferschein [" + liefernummer + "] hat keine Positionen.");

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

			    this.DEBUGG_LOGGER.addMassage("---------------------------------------------------");

			    // ----------------------------------------------------------------------------------------------------------

			}

		    } else {

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

		// ==UPDATE---------------------------------------------------------------------------
		if (this.XML_CONTENT_IS_INSERT_TYPE == false) {

		    // Update nur moeglich wenn true
		    if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

			String _alt_auftragsnummer = "";
			String _alt_rechnungsnummer = "";
			String _alt_kundennummer = "";
			boolean _alt_fakturiert = false;
			boolean _alt_gebucht = false;
			String _alt_last_log = "";

			String sqlCheckoldData_lieferschein = "Select auftrags_nummer, rechnungsnummer, kundennummer, faktura, gebucht, last_agent_operation from static_lieferschein where lieferschein_nummer='"
				+ liefernummer + "';";

			this.DB_CONNECTOR.readFromDatabase(sqlCheckoldData_lieferschein,
				"sqlCheckoldData_lieferschein");

			while (this.DB_CONNECTOR.rs.next()) {

			    _alt_auftragsnummer = this.DB_CONNECTOR.rs.getString(1);
			    _alt_rechnungsnummer = this.DB_CONNECTOR.rs.getString(2);
			    _alt_kundennummer = this.DB_CONNECTOR.rs.getString(3);
			    _alt_fakturiert = this.DB_CONNECTOR.rs.getBoolean(4);
			    _alt_gebucht = this.DB_CONNECTOR.rs.getBoolean(5);
			    _alt_last_log = this.DB_CONNECTOR.rs.getString(6);

			}

			String newLog = _alt_last_log + "- Lieferschein [ " + liefernummer
				+ " ] wurde aktualisiert.\n---- Auftragsnummer: [a=" + _alt_auftragsnummer + ",n="
				+ this.auftragsnummer + "],\n---- Rechnungsnummer: [a=" + _alt_rechnungsnummer + ",n="
				+ rechnungsNummer + "],\n---- Kundennummer: [a=" + _alt_kundennummer + ",n="
				+ kundennummer + "],\n---- Fakturiert: [a=" + _alt_fakturiert + ", n=" + fakturiert
				+ "],\n---- Gebucht: [a=" + _alt_gebucht + ", n=" + gebucht + "]\n---- Datei: ["
				+ this.FILE_CREATION_CODE + "]\n\n";

			String sqlUpdateLieferschein = "UPDATE static_lieferschein set faktura=" + fakturiert
				+ ", gebucht=" + gebucht + ", kundennummer='" + kundennummer + "', rechnungsnummer='"
				+ rechnungsNummer + "', update_work_file_id=" + this.WORK_FILE_DB_ID
				+ ", last_oparation_datum='" + formatdb.format(new Date()) + "', last_agent_operation='"
				+ newLog + "', auftrags_nummer='" + this.auftragsnummer
				+ "' where lieferschein_nummer ='" + liefernummer + "';";

			this.DB_CONNECTOR.updateOnDatabase(sqlUpdateLieferschein);

			this.DEBUGG_LOGGER.addMassage("- Lieferschein [ " + liefernummer
				+ " ] wurde aktualisiert.\n---- Auftragsnummer: [a=" + _alt_auftragsnummer + ",n="
				+ this.auftragsnummer + "],\n---- Rechnungsnummer: [a=" + _alt_rechnungsnummer + ",n="
				+ rechnungsNummer + "],\n---- Kundennummer: [a=" + _alt_kundennummer + ",n="
				+ kundennummer + "],\n---- Fakturiert: [a=" + _alt_fakturiert + ", n=" + fakturiert
				+ "]," + " \n---- Gebucht: [a=" + _alt_gebucht + ", n=" + gebucht + "]\n---- Datei: ["
				+ this.FILE_CREATION_CODE + "]");

			// =Verarbeite
			// Positionen---------------------------------------------------------------------

			NodeList lieferscheinpositionen = lieferschein_kopf_element.getElementsByTagName("Lp");

			this.ANZAHL_XML_OBJEKTE = lieferscheinpositionen.getLength();

			this.DEBUGG_LOGGER.addMassage("Lieferschein Positionen Anzahl: " + this.ANZAHL_XML_OBJEKTE);

			this.DEBUGG_LOGGER
				.addMassage("--------------------------------------------------------------------");

			if (this.ANZAHL_XML_OBJEKTE > 0) {

			    for (int position_im_lieferschein = 0; position_im_lieferschein < lieferscheinpositionen
				    .getLength(); position_im_lieferschein++) {

				Node einzelne_positions_wurzel = lieferscheinpositionen.item(position_im_lieferschein);

				if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

				    Element auftragspositionElement = (Element) einzelne_positions_wurzel;

				    try {

					BigDecimal menG = new BigDecimal(
						auftragspositionElement.getElementsByTagName("Rec").item(0)
							.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

					position = menG.intValue();

				    } catch (NullPointerException ecxc) {
					position = 0;
				    }

				    try {

					interne_artikel_nummer = auftragspositionElement.getElementsByTagName("ArtNr")
						.item(0).getTextContent()
						.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

				    } catch (NullPointerException ecxc) {
					interne_artikel_nummer = "?";
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("Menge").item(0).getTextContent());

					menge = menG.intValue();

				    } catch (NullPointerException ecxc) {
					menge = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeLager").item(0).getTextContent());

					mengeLager = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengeLager = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeFak").item(0).getTextContent());

					mengeFak = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengeFak = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("MengeLp").item(0).getTextContent());

					mengelieferschein = menG.intValue();

				    } catch (NullPointerException ecxc) {
					mengelieferschein = 0;
				    }

				    try {

					BigDecimal menG = new BigDecimal(auftragspositionElement
						.getElementsByTagName("ApRec").item(0).getTextContent());

					position_im_auftrag = menG.intValue();

				    } catch (NullPointerException ecxc) {
					position_im_auftrag = 0;
				    }

				    boolean position_existenz_lieferschein = checkLieferscheinPosition(liefernummer,
					    interne_artikel_nummer, position);

				    boolean position_existenz_auftrag = checkExistiertDiePositionBereitsInDemAuftragRef(
					    auftragsnummer, interne_artikel_nummer, position_im_auftrag);

				    // Wenn die Position nicht exisitiert
				    if (position_existenz_lieferschein == false && position_existenz_auftrag == false) {

					String newLog1 = "#{IFU_00001} INSERT LIEFERSCHEIN POSITION:\n---- Artikel: ["
						+ interne_artikel_nummer + "],\n---- Menge=" + menge
						+ ",\n---- Mengelager=" + mengeLager + ",\n---- Fakturierte Menge="
						+ mengeFak + ",\n---- Menge Lieferschein=" + mengelieferschein
						+ "\n---- Datei: [" + this.FILE_CREATION_CODE + "] \n\n";

					String sqlInsertLieferscheinPosition = "Insert static_lieferschein_positionen (position, "
						+ " lieferschein_nummer," //
						+ " artikel_nummer," //
						+ " menge, " //
						+ " mengelager, " //
						+ " mege_faktura, " //
						+ " menge_lieferschein," //
						+ " last_operation," //
						+ " last_operation_datum) VALUES (" + position + " ,'" + liefernummer
						+ "','" + interne_artikel_nummer + "', " + menge + ", " + mengeLager
						+ ", " + mengeFak + "," + mengelieferschein + ", '" + newLog1 + "' , '"
						+ formatdb.format(new Date()) + "' );";

					if (interne_artikel_nummer.contains("WP-") == false
						&& interne_artikel_nummer.contains("WPK-") == false
						&& interne_artikel_nummer != "" && interne_artikel_nummer != "?"
						&& interne_artikel_nummer.length() >= 5) {

					    this.DB_CONNECTOR.insertToDatabase(sqlInsertLieferscheinPosition,
						    "sqlInsertNewBestellung");

					    this.DEBUGG_LOGGER.addMassage(
						    "#{IFU_00001} INSERT - Positionen werden in die Datenbank eingetragen");

					    this.DEBUGG_LOGGER.addMassage("- Lieferschein: [ " + liefernummer
						    + " ],\n---- Auftragsnummer: [ " + this.auftragsnummer
						    + " ],\n---- Kunde: [" + kundennummer + "],\n---- Artikel: ["
						    + interne_artikel_nummer + "],\n----  Menge=" + menge
						    + ", Mengelager=" + mengeLager + ",\n---- Fakturierte Menge="
						    + mengeFak + ", Menge Lieferschein=" + mengelieferschein + "");

					    String logdbold = "";
					    String logdbneuArt = "";

					    String SQLbereitsImLagerAusStaticArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
						    + interne_artikel_nummer + "';";

					    int bereitsImLagerStaticArtikel = 0;

					    this.DB_CONNECTOR.readFromDatabase(SQLbereitsImLagerAusStaticArtikel,
						    "SQLbereitsImLagerAusStaticArtikel");

					    while (this.DB_CONNECTOR.rs.next()) {

						bereitsImLagerStaticArtikel = this.DB_CONNECTOR.rs.getInt(1);
						logdbold = this.DB_CONNECTOR.rs.getString(2);
					    }

					    int neuerLagerBestandAnzahl = (bereitsImLagerStaticArtikel - mengeLager);

					    logdbneuArt = logdbold
						    + "-#{IFU_8329} LIEFERSCHEIN POSITION NEU,\n---- LIEFERSCHEINNUMMER: [ "
						    + liefernummer + " ],\n---- Artikel [" + interne_artikel_nummer
						    + "],\n---- Lagerbestand: [ alt=" + bereitsImLagerStaticArtikel
						    + " ][ neu= " + neuerLagerBestandAnzahl + " ],\n---- [ Ausgang="
						    + mengeLager + " ]\n---- am: " + format2.format(new Date())
						    + " von A-RUN: [" + this.AGENT_RUN_CODE + "] und M-RUN: ["
						    + this.MANAGER_RUN_CODE + "\n---- Datei: ["
						    + this.FILE_CREATION_CODE + "\n\n";

					    String SQLneuerLagerBestand = "UPDATE static_artikel set lager_anzahl="
						    + neuerLagerBestandAnzahl + ", last_update='"
						    + formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
						    + ", agent_operation='" + logdbneuArt
						    + "' where interne_artikelnummer='" + interne_artikel_nummer + "';";

					    this.DB_CONNECTOR.updateOnDatabase(SQLneuerLagerBestand);

					    String guid_verkauf = "LIEFERSCHEIN_UP_" + liefernummer + "_" + position
						    + "_" + position_im_lieferschein;

					    String SQL_Lieferschein = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
						    + guid_verkauf + "','" + interne_artikel_nummer + "'," + mengeLager
						    + ", '" + datum_liefernummer + "',0);";

					    this.DB_CONNECTOR.insertToDatabase(SQL_Lieferschein, "SQL_Lieferschein");

					    this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + interne_artikel_nummer
						    + "] aktualisiert. \n---- Datei: [" + this.FILE_CREATION_CODE
						    + "]");

					    this.DEBUGG_LOGGER.addMassage("---- Lagerbestand Menge (alt): "
						    + bereitsImLagerStaticArtikel + " Stueck(e).");
					    this.DEBUGG_LOGGER.addMassage("---- Lagerbestand Menge (neu): "
						    + neuerLagerBestandAnzahl + " Stueck(e).");
					    this.DEBUGG_LOGGER.addMassage(
						    "---- Vom Lager entnommen: " + mengeLager + " Stueck(e).");

					    this.DEBUGG_LOGGER.addMassage(
						    "--------------------------------------------------------------------\n");

					}

				    }

				    if (position_existenz_lieferschein == true && position_existenz_auftrag == false) {

					/// -----------------------------------------------------------------

					int _alt_artikel_lagerbestand = 0;
					int _neu_artikel_lagerbestand = 0;

					int _diff_artikel_lagerbestand = 0;

					String _alter_artikel_loginfo = "";
					String _neuer_artikel_loginfo = "";
					/// -----------------------------------------------------------------
					int _alt_menge = 0;
					int _alt_mengelager = 0;
					int _alt_mege_faktura = 0;
					int _alt_menge_lieferschein = 0;
					String _alt_last_operation = "";
					/// -----------------------------------------------------------------

					String sqlReadPositionLieferschein = "Select menge,mengelager,mege_faktura, menge_lieferschein, last_operation from static_lieferschein_positionen where artikel_nummer='"
						+ interne_artikel_nummer + "' and lieferschein_nummer='" + liefernummer
						+ "' and position=" + position + ";";

					this.DB_CONNECTOR.readFromDatabase(sqlReadPositionLieferschein,
						"sqlReadPositionLieferschein");

					while (this.DB_CONNECTOR.rs.next()) {
					    _alt_menge = this.DB_CONNECTOR.rs.getInt(1);
					    _alt_mengelager = this.DB_CONNECTOR.rs.getInt(2);
					    _alt_mege_faktura = this.DB_CONNECTOR.rs.getInt(3);
					    _alt_menge_lieferschein = this.DB_CONNECTOR.rs.getInt(4);
					    _alt_last_operation = this.DB_CONNECTOR.rs.getString(5);

					}

					int dif_menge = 0;
					int _neu_menge = 0;

					int diff_lager = 0;
					int _neu_mengelager = 0;

					int diff_faktura = 0;
					int _neu_mege_faktura = 0;

					int diff_lieferschein = 0;
					int _neu_menge_lieferschein = 0;

					String _neu_last_operation = "";

					// Additive arthimetische Operation

					// ---------- Menge
					dif_menge = (_alt_menge - menge);
					_neu_menge = (_alt_menge - (dif_menge));
					// ---------- Lager
					diff_lager = (_alt_mengelager - mengeLager);
					_neu_mengelager = (_alt_mengelager - (diff_lager));
					// ----------Faktura (auf Rechnung)
					diff_faktura = (_neu_mege_faktura - mengeFak);
					_neu_mege_faktura = (_alt_mege_faktura - (diff_faktura));
					// ----------Lieferschein
					diff_lieferschein = (_alt_menge_lieferschein - mengelieferschein);
					_neu_menge_lieferschein = (_alt_menge_lieferschein - (diff_lieferschein));

					// -----------

					_neu_last_operation = "- Position wurde aktualisiert:\n---- Menge: [a="
						+ _alt_menge + ", n=" + _neu_menge + ", xml=" + menge
						+ ",\n---- wie folgt berechnet: " + _neu_menge + " = " + _alt_menge
						+ " - (" + _alt_menge + " - " + menge + " )], "
						+ "\n---- MengeLager: [a=" + _alt_mengelager + ", n=" + _neu_mengelager
						+ ", xml=" + mengeLager + ",\n---- wie folgt berechnet:"
						+ _neu_mengelager + " = " + _alt_mengelager + " - (" + _alt_mengelager
						+ "- " + mengeLager + ")],\n---- Menge Fakturiert: [a="
						+ _alt_mege_faktura + ", n=" + _neu_mege_faktura + ", xml=" + mengeFak
						+ ",\n---- wie folgt berechnet: " + _neu_mege_faktura + " = "
						+ _alt_mege_faktura + " - (" + _alt_mege_faktura + " - " + mengeFak
						+ ")],\n---- Menge Lieferschein: [a=" + _alt_menge_lieferschein + ", n="
						+ _neu_menge_lieferschein + ", xml=" + mengelieferschein
						+ ",\n----- wie folgt berechnet: " + _neu_menge_lieferschein + " = "
						+ _alt_menge_lieferschein + " - (" + _alt_menge_lieferschein + " - "
						+ mengelieferschein + ")]\n---- am: " + format2.format(new Date())
						+ "\n---- A-RUN: [" + this.AGENT_RUN_CODE + "]\n---- M-RUN: ["
						+ this.MANAGER_RUN_CODE + "]\n---- Datei: [" + this.FILE_CREATION_CODE
						+ "]";

					if (interne_artikel_nummer.contains("WP-") == false
						&& interne_artikel_nummer.contains("WPK-") == false
						&& interne_artikel_nummer != "" && interne_artikel_nummer != "?"
						&& interne_artikel_nummer.length() >= 5) {

					    String sqlUpdatePosition = "UPDATE static_lieferschein_positionen set menge="
						    + _neu_menge + ", mengelager=" + _neu_mengelager + ", mege_faktura="
						    + _neu_mege_faktura + ", menge_lieferschein="
						    + _neu_menge_lieferschein + ", last_operation='"
						    + this.LOG_CHECKER.generateNewLogForDB(_alt_last_operation,
							    _neu_last_operation)
						    + "' where lieferschein_nummer='" + liefernummer
						    + "' and artikel_nummer='" + interne_artikel_nummer
						    + "' and position=" + this.position + ";";

					    this.DB_CONNECTOR.updateOnDatabase(sqlUpdatePosition);

					    String SQLInitArtikel = "Select lager_anzahl, agent_operation from static_artikel where interne_artikelnummer='"
						    + interne_artikel_nummer + "';";

					    this.DB_CONNECTOR.readFromDatabase(SQLInitArtikel, "SQLInitArtikel");

					    while (this.DB_CONNECTOR.rs.next()) {

						_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(1);
						_alter_artikel_loginfo = this.DB_CONNECTOR.rs.getString(2);
					    }

					    _neu_artikel_lagerbestand = ((_alt_artikel_lagerbestand + _alt_mengelager)
						    - _neu_mengelager);

					    _diff_artikel_lagerbestand = _alt_artikel_lagerbestand
						    - _neu_artikel_lagerbestand;

					    _neuer_artikel_loginfo = "- #{UP_LIEFRS_012} LIEFERSCHEIN POSITION UPDATE:\n---- Lieferschein: ["
						    + liefernummer + "],\n---- Artikel: [" + interne_artikel_nummer
						    + "],\n---- Artikel im Lager: [a=" + _alt_artikel_lagerbestand
						    + ", n=" + _neu_artikel_lagerbestand + "],\n---- geaendert: [a="
						    + _alt_artikel_lagerbestand + ",n=" + _neu_artikel_lagerbestand
						    + ", dif=" + _diff_artikel_lagerbestand
						    + "],\n---- wie folgt berechnet: [artL-n] "
						    + _neu_artikel_lagerbestand + " = (( [arrL-a] "
						    + _alt_artikel_lagerbestand + " + [pos-a] " + _alt_mengelager
						    + ") - [pos-n] " + _neu_mengelager + ")\n---- am: "
						    + format2.format(new Date()) + "\n---- A-RUN: ["
						    + this.AGENT_RUN_CODE + "]\n---- M-RUN: [" + this.MANAGER_RUN_CODE
						    + "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]";

					    String sqlUpdateArtikel = "UPDATE static_artikel set lager_anzahl="
						    + _neu_artikel_lagerbestand + ", agent_operation='"
						    + this.LOG_CHECKER.generateNewLogForDB(_alter_artikel_loginfo,
							    _neuer_artikel_loginfo)
						    + "', last_update='" + formatdb.format(new Date())
						    + "', last_update_agent=" + this.AGENT_DBID
						    + " where interne_artikelnummer='" + interne_artikel_nummer + "';";

					    this.DB_CONNECTOR.updateOnDatabase(sqlUpdateArtikel);

					    this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + interne_artikel_nummer
						    + "] wurde vom Lieferschein: [" + liefernummer
						    + "] aktualisiert,\n---- Artikelbetand: [a="
						    + _alt_artikel_lagerbestand + ", n=" + _neu_artikel_lagerbestand
						    + "],\n----- wie folgt berechnet: [artL-n] "
						    + _neu_artikel_lagerbestand + " = (( [artL-a] "
						    + _alt_artikel_lagerbestand + " + [pos-a] " + _alt_mengelager
						    + ") - [pos-n] " + _neu_mengelager + "))");

					    this.DEBUGG_LOGGER.addMassage(
						    "--------------------------------------------------------------------\n");

					} else {

					    // this.debug.addMassage("Position
					    // mit
					    // Artikel: [" +
					    // interne_artikel_nummer
					    // + "] wird nicht uebernommen.");

					}

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

			} else {
			    this.DEBUGG_LOGGER.addMassage("Lieferschein [" + liefernummer
				    + "] hat keine Positionen. - Datei: [" + this.FILE_CREATION_CODE + "]");

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
				    + laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME)
				    + "\n");

			    // ----------------------------------------------------------------------------------------------------------

			}

		    } else {

			this.DEBUGG_LOGGER.addMassage("Es wurde kein Insert-Eintrag gefunden zum Lieferschein: ["
				+ liefernummer + "] - Datei: [" + this.FILE_CREATION_CODE + "]");

			this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>STACK<< VERSCHOBEN]");

			this.DEBUGG_LOGGER.addMassage("Datenbank Eintrag der Datei wird geloescht.");

			this.DB_CONNECTOR.updateOnDatabase(
				"DELETE from work_agent_files where file_creation_code='" + FILE_CREATION_CODE + "';");

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

	    }

	    if (this.XML_FILE_WAS_EVER_PROCESSED == true) {

		String file_duplikat_label = "";

		String findeDuplikaSQL = "SELECT concat(file_creation_code,'_',id)  FROM  work_agent_files where filehash='"
			+ this.MD5_FILE_HASHCODE + "';";

		this.DB_CONNECTOR.readFromDatabase(findeDuplikaSQL, "findeDuplikaSQL");

		while (this.DB_CONNECTOR.rs.next()) {

		    file_duplikat_label = this.DB_CONNECTOR.rs.getString(1);

		}

		this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>xfwep_DUPLIKATE<< VERSCHOBEN]\n");

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

    public boolean existiert_diese_lieferung(String _lieferschein) throws SQLException {

	boolean temp = false;

	String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_lieferschein WHERE lieferschein_nummer='"
		+ _lieferschein + "');";

	this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

	while (this.DB_CONNECTOR.rs.next())

	{
	    temp = this.DB_CONNECTOR.rs.getBoolean(1);
	}

	return temp;
    }

    public boolean checkLieferscheinPosition(String _lieferscheinnummer, String _artikelnummer,
	    int lieferschein_position) throws SQLException {

	String sqlCheckExsistenz = "SELECT EXISTS (SELECT * FROM static_lieferschein_positionen WHERE lieferschein_nummer='"
		+ _lieferscheinnummer + "' and artikel_nummer='" + _artikelnummer + "' and position="
		+ lieferschein_position + ");";

	boolean temp = true;

	this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

	while (this.DB_CONNECTOR.rs.next())

	{
	    temp = this.DB_CONNECTOR.rs.getBoolean(1);
	}

	return temp;

    }

    public boolean checkExistiertDiePositionBereitsInDemAuftragRef(String _auftrag, String _artikelnummer,
	    int _auftrag_position) throws SQLException {

	String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_auftraege_positionen WHERE  auftrags_nummer='"
		+ _auftrag + "' and artikel_nummer='" + _artikelnummer + "' and position=" + _auftrag_position + ");";

	boolean temp = true;

	this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

	while (this.DB_CONNECTOR.rs.next())

	{
	    temp = this.DB_CONNECTOR.rs.getBoolean(1);
	}

	return temp;

    }

}
