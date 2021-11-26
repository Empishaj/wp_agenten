package UPDATE_MANAGER;

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

public class event_agent_warengruppe extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 3636431195062328239L;

	public event_agent_warengruppe(int _managerid, String _manager_run_creation_code, boolean _isLinux,
			String _opsystem, String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
			throws SQLException, ClassNotFoundException {

		super();

		this.DB_CONNECTOR = _db;
		this.AGENT_DBID = 992;
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

		this.XML_FILE_WAS_EVER_PROCESSED = true;

		this.XML_CONTENT_IS_INSERT_TYPE = false;

		String filetype_operation_content = "?";

		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "] \n");

			this.AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

			// -----------------
			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			this.update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);

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

				NodeList warengruppe_kopf_node = doc.getElementsByTagName("WGList");

				Node node_kopf_wurzel = warengruppe_kopf_node.item(0);

				Element node_kopf_wurzel_element = (Element) node_kopf_wurzel;

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

				// {initialisiere Kopf des Lieferant}
				// -----------------------------------------------------------------------

				NodeList nList = doc.getElementsByTagName("WG");

				if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

					this.FILE_CONTENT_ID = FileTypes.WARENGRUPPE_INSERT_EVENT_FILE;

					for (int temp = 0; temp < nList.getLength(); temp++) {

						int id = 0;
						String warengruppe_lang = "";
						String warengruppe_kurz = "";

						Node nNode = nList.item(temp);

						if (nNode.getNodeType() == Node.ELEMENT_NODE) {

							Element eElement = (Element) nNode;

							try {

								BigDecimal men = new BigDecimal(
										eElement.getElementsByTagName("Id").item(0).getTextContent());

								id = men.intValue();

							} catch (java.lang.NullPointerException e) {
								id = 0;
							}

							try {
								warengruppe_kurz = eElement.getElementsByTagName("WHG").item(0).getTextContent()
										.replaceAll("[^A-Za-z0-9+-]", "").trim();

							} catch (java.lang.NullPointerException e) {
								warengruppe_kurz = "?";
							}

							try {
								warengruppe_lang = eElement.getElementsByTagName("Txt1").item(0).getTextContent()
										.replaceAll("[^A-Za-z0-9+-]", "").trim();

							} catch (java.lang.NullPointerException e) {
								warengruppe_lang = "?";
							}

							String sqlInsertwaren = "INSERT INTO static_warengruppe (id, lang, kurz, mandant_id)  VALUES ("
									+ id + ",'" + warengruppe_lang + "', '" + warengruppe_kurz + "',"
									+ this.AUSWAHL_MANDANT_DBID + ");";

							try {

								this.DB_CONNECTOR.insertToDatabase(sqlInsertwaren, "sqlInsertwaren");

								this.DEBUGG_LOGGER.addMassage("WARENGRUPPE wurde eingefuegt: [ id=" + id + ", kurz="
										+ warengruppe_kurz + ", lang=" + warengruppe_lang + "] \n");

							} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException jk) {

								this.DEBUGG_LOGGER.addMassage("WARENGRUPPE: [ id=" + id + ", kurz=" + warengruppe_kurz
										+ ", lang=" + warengruppe_lang + "] exisitiert bereits. \n");

							}

						}

					}

					this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN] \n");

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
							+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " \n");

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
							+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]\n");

					this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
							+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "\n");

				}

				if (this.XML_CONTENT_IS_INSERT_TYPE == false) {

					this.FILE_CONTENT_ID = FileTypes.WARENGRUPPE_UPDATE_EVENT_FILE;

					for (int temp = 0; temp < nList.getLength(); temp++) {

						int id = 0;
						String warengruppe_lang = "";
						String warengruppe_kurz = "";

						Node nNode = nList.item(temp);

						if (nNode.getNodeType() == Node.ELEMENT_NODE) {

							Element eElement = (Element) nNode;

							try {

								BigDecimal men = new BigDecimal(
										eElement.getElementsByTagName("Id").item(0).getTextContent());

								id = men.intValue();

							} catch (java.lang.NullPointerException e) {
								id = 0;
							}

							try {
								warengruppe_kurz = eElement.getElementsByTagName("WHG").item(0).getTextContent()
										.replace(" ", "-").replaceAll("[^A-Za-z0-9+-]", "").trim().replace("-", " ");

							} catch (java.lang.NullPointerException e) {
								warengruppe_kurz = "?";
							}

							try {
								warengruppe_lang = eElement.getElementsByTagName("Txt1").item(0).getTextContent()
										.replace(" ", "-").replaceAll("[^A-Za-z0-9+-]", "").trim().replace("-", " ");

							} catch (java.lang.NullPointerException e) {
								warengruppe_lang = "?";
							}

							String sqlInsertwaren = "UPDATE static_warengruppe set lang='" + warengruppe_lang
									+ "', kurz='" + warengruppe_kurz + "' where id=" + id + " ;";

							this.DB_CONNECTOR.insertToDatabase(sqlInsertwaren, "sqlInsertLieferant");

							this.DEBUGG_LOGGER.addMassage("WARENGRUPPE wurde akzalisiert: [ id=" + id + ", kurz="
									+ warengruppe_kurz + ", lang=" + warengruppe_lang + "] \n");

						}

					}

					this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN] \n");

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
							+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " \n");

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
							+ this.AGENT_RUN_CODE + ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]\n");

					this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
							+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "\n");
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
						+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " \n");

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
						+ ", Manager-RUN=" + this.MANAGER_RUN_CODE + "]\n");

				this.DEBUGG_LOGGER.addMassage("[ AGENT-PROZESS-TIME] > "
						+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "\n");

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

			this.DEBUGG_LOGGER.addMassage("[ AGENT IST DEAKTIVIERT ]\n");

		}

	}

}
