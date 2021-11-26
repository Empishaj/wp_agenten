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

public class event_agent_auftrag extends Agent implements java.io.Serializable {

	private static final long serialVersionUID = 7848115657717666099L;

	String guid = "";
	String auftrags_nummer = "?";
	String datum_auftragserstellung = "";
	String datum_auftrager_ledigt = "";
	String kundennummer = "";
	String rechnungsNummer = "";
	boolean erledigt = false;
	boolean storno = false;

	String lieferscheinnummer = "";

	boolean auftrag_vorhanden = false;

	int neu_menge_reserviert_xml = 0;
	int neu_menge_lager_xml = 0;
	int neu_menge_fakturiert_xml = 0;
	int neu_menge_lieferschein_xml = 0;
	String interne_artikel_nummer = "";

	String tempDatumWareneingangAusXML = "";
	int position = 0;

	public event_agent_auftrag(int _managerid, String _manager_run_creation_code, boolean _isLinux, String _opsystem,
			String _system_path_delimiter, String _duplikat_path, DatenBankVerbindung _db)
			throws SQLException, ClassNotFoundException {

		super();

		this.DB_CONNECTOR = _db;
		this.AGENT_DBID = 991;
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
		this.ANZAHL_XML_OBJEKTE = 0;

	}

	public void start(File MANAGER_curr_workfile, int MANAGER_filepositionsindex, String _fileid, int mandantid,
			String mandant_kuerzel)
			throws SQLException, ClassNotFoundException, IOException, ParserConfigurationException, SAXException {

		this.FILE_CREATION_CODE = _fileid;
		this.XML_CONTENT_IS_INSERT_TYPE = false;
		this.XML_FILE_WAS_EVER_PROCESSED = false;
		this.AUSWAHL_MANDANT_DBID = mandantid;
		this.AUSWAHL_MANDANT_NAME = mandant_kuerzel;

		if (this.AGENT_ACTIVITY == true) {

			this.DEBUGG_LOGGER.addMassage("-----------------------------------------------------------------------");

			this.DEBUGG_LOGGER.addMassage("[" + MANAGER_filepositionsindex + "] > DATEI: [" + FILE_CREATION_CODE
					+ "] - [START AGENT: " + this.AGENT_NAME + " Agent-RUN= " + this.AGENT_RUN_CODE + ", Manager-RUN="
					+ this.MANAGER_RUN_CODE + "]");
			this.DEBUGG_LOGGER.addMassage("-----------------------------------------------------------------------");

			this.AGENT_DELIVER_XML_FILE = MANAGER_curr_workfile;

			// -----------------
			this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_VERARBEITE_DATEIEN;

			this.update_agenten_prozess_status(this.AGENT_STATUS_CURRENT_CODE, this.AGENT_RUN_CODE, this.DB_CONNECTOR);

			// -----------------

			this.XML_FILE_WAS_EVER_PROCESSED = this.wurde_diese_datei_schon_mal_verarbeitet(AGENT_DELIVER_XML_FILE,
					this.DB_CONNECTOR, this.AGENT_DBID);

			if (this.XML_FILE_WAS_EVER_PROCESSED == false) {

				// -----------------------------------------------------------------------

				Path absolut_path = Paths.get(this.AGENT_DELIVER_XML_FILE.getAbsolutePath());

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

				this.WORK_FILE_DB_ID = find_work_file_dbid(FILE_CREATION_CODE, this.DB_CONNECTOR);

				// -----------------------------------

				this.AGENT_WORK_XML_FILE_NAME = getSystem_work_file_name(MANAGER_filepositionsindex);

				// ------------------------------------------

				this.AGENT_WORK_FILE_XML = new File(
						this.AGENT_FILE_WORK_PATH + this.SYSTEM_PATH_DELIMITER + this.AGENT_WORK_XML_FILE_NAME);

				this.AGENT_DELIVER_XML_FILE.renameTo(this.AGENT_WORK_FILE_XML);
				// ------------------------------------------

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(this.AGENT_WORK_FILE_XML);

				doc = dBuilder.parse(this.AGENT_WORK_FILE_XML);
				doc.getDocumentElement().normalize();

				// ------------------------------------------

				Element node_kopf_wurzel_element = (Element) doc.getElementsByTagName("AhList").item(0);
				Element auftrag_kopf_element = (Element) (doc.getElementsByTagName("Ah").item(0));

				//// Check Datei Typ
				// ------------------------------------------

				try {

					this.FILE_CONTENT_OPERATION_TYPE = new String(node_kopf_wurzel_element.getAttribute("Operation"));

				} catch (NullPointerException ecxc) {

					this.FILE_CONTENT_OPERATION_TYPE = "?";

				}

				if ("Insert".equals(FILE_CONTENT_OPERATION_TYPE) || "Insert" == FILE_CONTENT_OPERATION_TYPE)

				{

					this.XML_CONTENT_IS_INSERT_TYPE = true;
					this.FILE_CONTENT_ID = 4444;

				} else {

					this.XML_CONTENT_IS_INSERT_TYPE = false;
					this.FILE_CONTENT_ID = 4466;

				}

				// {initialisiere Kopf des Lieferscheins}
				// -----------------------------------------------------------------------

				try {

					this.auftrags_nummer = auftrag_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					this.auftrags_nummer = "?";

				}

				try {

					this.guid = auftrag_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
							.replaceAll("[^A-Za-z0-9+-]", "").trim();

				} catch (NullPointerException ecxc) {
					this.guid = "?";
				}

				try {

					Date datum_wareneingang_temp = null;

					this.tempDatumWareneingangAusXML = auftrag_kopf_element.getElementsByTagName("Date").item(0)
							.getTextContent().trim();

					if (this.tempDatumWareneingangAusXML.trim() == "") {
						this.tempDatumWareneingangAusXML = "9999-09-09 00:00:00";

					}

					datum_wareneingang_temp = this.XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

					this.datum_auftragserstellung = formatdb.format(datum_wareneingang_temp);

				} catch (NullPointerException ecxc) {
					this.datum_auftragserstellung = "9999-09-09 00:00:00";
				} catch (ParseException e1) {

					this.datum_auftragserstellung = "9999-09-09 00:00:00";
					e1.printStackTrace();
				}

				try {

					String tempDatumWareneingangAusXML = "";
					Date datum_wareneingang_temp = null;

					tempDatumWareneingangAusXML = auftrag_kopf_element.getElementsByTagName("ErledigtAm").item(0)
							.getTextContent().trim();

					if (tempDatumWareneingangAusXML.trim() == "") {
						tempDatumWareneingangAusXML = "9999-09-09 00:00:00";

					}

					datum_wareneingang_temp = XML_FORMAT_DATUM.parse(tempDatumWareneingangAusXML);

					datum_auftrager_ledigt = formatdb.format(datum_wareneingang_temp);

				} catch (NullPointerException ecxc) {
					this.datum_auftrager_ledigt = "9999-09-09 00:00:00";
				} catch (ParseException e1) {

					this.datum_auftrager_ledigt = "9999-09-09 00:00:00";
					e1.printStackTrace();
				}

				try {
					this.erledigt = new Boolean(
							auftrag_kopf_element.getElementsByTagName("Erledigt").item(0).getTextContent().trim());

				} catch (NullPointerException ecxc) {
					this.erledigt = false;
				}

				try {
					this.storno = new Boolean(
							auftrag_kopf_element.getElementsByTagName("Storno").item(0).getTextContent().trim());

				} catch (NullPointerException ecxc) {
					this.storno = false;
				}

				try {

					this.rechnungsNummer = auftrag_kopf_element.getElementsByTagName("RefRechnung").item(0)
							.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

					if (this.rechnungsNummer == "") {
						this.rechnungsNummer = "?";
					}

				} catch (NullPointerException ecxc) {
					rechnungsNummer = "?";
				}

				try {

					this.lieferscheinnummer = auftrag_kopf_element.getElementsByTagName("LfRefNr").item(0)
							.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim();

					if (this.lieferscheinnummer == "") {
						this.lieferscheinnummer = "?";
					}

				} catch (NullPointerException ecxc) {
					this.lieferscheinnummer = "?";
				}

				try {

					this.kundennummer = auftrag_kopf_element.getElementsByTagName("KdNr").item(0).getTextContent()
							.trim().replaceAll("[^A-Za-z0-9+-]", "").trim();

					if (this.kundennummer == "") {
						this.kundennummer = "?";
					}

				} catch (NullPointerException ecxc) {
					this.kundennummer = "?";
				}

				// Pruefe ob dieser Eintrag bereits exisitert
				this.CHECK_HEADER_ENTRY_EXISTENCE = existiert_dieser_auftrag(auftrags_nummer);
				this.DEBUGG_LOGGER.addMassage("- Auftrag [" + auftrags_nummer + "]");
				this.DEBUGG_LOGGER.addMassage("---- Kunde: [" + kundennummer + "]");
				this.DEBUGG_LOGGER.addMassage("---- Rechnungsnummer: [a=?,n=" + rechnungsNummer + "]");
				this.DEBUGG_LOGGER.addMassage("---- Lieferschein: [ " + lieferscheinnummer + "]");
				this.DEBUGG_LOGGER.addMassage("---- Erledigt am: [a=?,n= " + datum_auftrager_ledigt + "]");
				this.DEBUGG_LOGGER.addMassage("---- Auftrag wurde storniert: [" + storno + "]");
				this.DEBUGG_LOGGER.addMassage("---- Auftrag wurde erledigt: [" + erledigt + "]");

				// ==INSERT---------------------------------------------------------------------------
				if (this.XML_CONTENT_IS_INSERT_TYPE == true) {

					this.DEBUGG_LOGGER
							.addMassage("------------------------------------------------------------------------");

					this.DEBUGG_LOGGER.addMassage("#{I_AUF_001} AUFTRAG - NEU - INSERT");
					this.DEBUGG_LOGGER.addMassage("------------------------------------------------------------");

					// Nur wenn dieser EIntrag existiert
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == false) {

						String log = "#{I_AUF_001} AUFTRAG - NEU - INSERT,\n---- Auftragsnummer: [" + auftrags_nummer
								+ "],\n Rechnungsnummer[" + rechnungsNummer + "]\n Kundennummer: [" + kundennummer
								+ "]  \n---- am: " + datum_auftragserstellung + " von A-RUN: [" + this.AGENT_RUN_CODE
								+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
								+ this.FILE_CREATION_CODE + "]\n\n";

						String sqlInsertAuftragsKopf = "INSERT INTO static_auftraege (" //
								+ " guid, "// 1
								+ " interne_auftragsnummer,"// 2
								+ " kundennummer,"// 3
								+ " rechnungsnummer,"/// 4
								+ " datum_des_auftrags,"// 5
								+ " last_agentruncode, "// 6
								+ " last_managerruncode,"// 7
								+ " insert_work_file_id, "// 8
								+ " last_agent_id,  "// 9
								+ " last_oparation_datum,"// 10
								+ " last_agent_operation, " // 11
								+ " auftrag_erledigt_am," // 12
								+ " erledigt," // 13
								+ " storno," // 14
								+ " mandant_id ) "// 15
								+ " VALUES ('" + guid + "','" // 1
								+ auftrags_nummer + "'," // 2
								+ "'" + kundennummer + "'," // 3
								+ "'" + rechnungsNummer + "','" // 4
								+ datum_auftragserstellung + "','"// 5
								+ this.AGENT_RUN_CODE + "','" // 6
								+ this.MANAGER_RUN_CODE + "', " // 7
								+ this.WORK_FILE_DB_ID + "," // 8
								+ this.AGENT_DBID + ",'" // 9
								+ formatdb.format(new Date()) + "'," // 10

								+ "'" + log + "', '" + datum_auftrager_ledigt + "'," + erledigt + ", " + storno + ", "
								+ this.AUSWAHL_MANDANT_DBID + " );";

						this.DB_CONNECTOR.insertToDatabase(sqlInsertAuftragsKopf, "sqlInsertAuftragKopf");

					} else {

						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>ihe_DUPLIKATE<< VERSCHOBEN]");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						Path path = Paths.get(this.AGENT_WORK_FILE_XML.getAbsolutePath());

						BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

						String file_creation_time = format1
								.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

						String newSystemNameBeforeDeliver = "ihe_duplikat_" + file_creation_time + "_" + this.AGENT_NAME
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
								+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " ");

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

						this.DEBUGG_LOGGER
								.addMassage("--------------------------------------------------------------\n");
						// ----------------------------------------------------------------------------------------------------------

					}

				}

				// ==UPDATE---------------------------------------------------------------------------

				if (XML_CONTENT_IS_INSERT_TYPE == false) {

					this.DEBUGG_LOGGER.addMassage("------------------------------------------------------------");
					this.DEBUGG_LOGGER.addMassage("#{APU_0022}-#{ALG: AUFTRAG - UPDATE}");
					this.DEBUGG_LOGGER.addMassage("------------------------------------------------------------");

					this.CHECK_HEADER_ENTRY_EXISTENCE = existiert_dieser_auftrag(auftrags_nummer);

					// Update nur moeglich wenn true
					if (this.CHECK_HEADER_ENTRY_EXISTENCE == true) {

						String _alt_kundennummer = "";
						String _alt_rechnungsnummer = "";
						String _alt_erledigt_am = "";
						boolean _alt_storno = false;
						boolean _alt_erledigt = false;
						String _alt_lastlog = "";

						String sqlCheckoldData_lieferschein = "SELECT kundennummer, rechnungsnummer, auftrag_erledigt_am, IFNULL(last_agent_operation,''), storno, erledigt  FROM  static_auftraege where interne_auftragsnummer='"
								+ auftrags_nummer + "';";

						this.DB_CONNECTOR.readFromDatabase(sqlCheckoldData_lieferschein,
								"sqlCheckoldData_lieferschein");

						// Auftragshistorie wird geladen

						while (this.DB_CONNECTOR.rs.next()) {

							_alt_kundennummer = this.DB_CONNECTOR.rs.getString(1);
							_alt_rechnungsnummer = this.DB_CONNECTOR.rs.getString(2);
							_alt_erledigt_am = this.DB_CONNECTOR.rs.getString(3);
							_alt_lastlog = this.DB_CONNECTOR.rs.getString(4);
							_alt_storno = this.DB_CONNECTOR.rs.getBoolean(5);
							_alt_erledigt = this.DB_CONNECTOR.rs.getBoolean(6);

						}

						String newLog = "#{APU_0022}-#{ALG: AUFTRAG - UPDATE} [ " + auftrags_nummer
								+ " ] wurde aktualisiert.\n---- Kundennummer: [a=" + _alt_kundennummer + ",n="
								+ kundennummer + "], " + "\n---- Rechnungsnummer: [a=" + _alt_rechnungsnummer + ",n="
								+ rechnungsNummer + "], Erledigt am: [a=" + _alt_erledigt_am + ",n= "
								+ datum_auftrager_ledigt + "],\n---- Auftrag wurde erledigt: [" + erledigt
								+ "], \n---- Auftrag wurde storniert: [" + storno + "],\n----  am: "
								+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE + "] und M-RUN: ["
								+ this.MANAGER_RUN_CODE + "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n";

						String dblog = this.LOG_CHECKER.generateNewLogForDB(_alt_lastlog, newLog);

						String sqlUpdateLieferschein = "UPDATE static_auftraege set kundennummer='" + kundennummer
								+ "', rechnungsnummer='" + rechnungsNummer + "', last_agent_operation='" + dblog
								+ "', last_oparation_datum='" + formatdb.format(new Date()) + "', last_agent_id="
								+ this.AGENT_DBID + ", update_work_file_id=" + this.WORK_FILE_DB_ID
								+ ", auftrag_erledigt_am='" + datum_auftrager_ledigt + "', erledigt=" + erledigt
								+ ", storno=" + storno + "  where interne_auftragsnummer ='" + auftrags_nummer + "';";

						this.DB_CONNECTOR.updateOnDatabase(sqlUpdateLieferschein);

						// =Verarbeite
						// Positionen---------------------------------------------------------------------

						NodeList auftrag_positionen_xml = auftrag_kopf_element.getElementsByTagName("Ap");

						this.ANZAHL_XML_OBJEKTE = auftrag_positionen_xml.getLength();

						this.DEBUGG_LOGGER
								.addMassage("--------------------------POSITIONEN---------------------------------");

						this.DEBUGG_LOGGER.addMassage("AUFTRAGSPOSITIONEN ANZAHL: [" + this.ANZAHL_XML_OBJEKTE + "]");

						this.DEBUGG_LOGGER
								.addMassage("---------------------------------------------------------------------");

						if (this.ANZAHL_XML_OBJEKTE > 0) {

							for (int position_im_auftrag_xml = 0; position_im_auftrag_xml < this.ANZAHL_XML_OBJEKTE; position_im_auftrag_xml++)

							{

								Node einzelne_position_wurzel_im_auftrag = auftrag_positionen_xml
										.item(position_im_auftrag_xml);

								if (einzelne_position_wurzel_im_auftrag.getNodeType() == Node.ELEMENT_NODE) {

									Element auftrags_position_element = (Element) einzelne_position_wurzel_im_auftrag;

									try {

										BigDecimal menG = new BigDecimal(
												auftrags_position_element.getElementsByTagName("Rec").item(0)
														.getTextContent().replaceAll("[^A-Za-z0-9+-]", "").trim());

										position = menG.intValue();

									} catch (NullPointerException ecxc) {
										position = 0;
									}

									try {

										interne_artikel_nummer = auftrags_position_element.getElementsByTagName("ArtNr")
												.item(0).getTextContent()
												.replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

									} catch (NullPointerException ecxc) {
										interne_artikel_nummer = "?";
									}

									try {

										BigDecimal menG = new BigDecimal(auftrags_position_element
												.getElementsByTagName("Menge").item(0).getTextContent());

										neu_menge_reserviert_xml = menG.intValue();

									} catch (NullPointerException ecxc) {
										neu_menge_reserviert_xml = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(auftrags_position_element
												.getElementsByTagName("MengeLager").item(0).getTextContent());

										neu_menge_lager_xml = menG.intValue();

									} catch (NullPointerException ecxc) {
										neu_menge_lager_xml = 0;

									}

									try {

										BigDecimal menG = new BigDecimal(auftrags_position_element
												.getElementsByTagName("MengeFak").item(0).getTextContent());

										neu_menge_fakturiert_xml = menG.intValue();

									} catch (NullPointerException ecxc) {
										neu_menge_fakturiert_xml = 0;
									}

									try {

										BigDecimal menG = new BigDecimal(auftrags_position_element
												.getElementsByTagName("MengeLp").item(0).getTextContent());

										neu_menge_lieferschein_xml = menG.intValue();

									} catch (NullPointerException ecxc) {
										neu_menge_lieferschein_xml = 0;

									}

								}

								boolean auftrags_position_existenz = checkAuftragsPosition(position, auftrags_nummer,
										interne_artikel_nummer);

								this.DEBUGG_LOGGER.addMassage("----------------------------------------------");

								this.DEBUGG_LOGGER.addMassage(
										"Position [" + position + "] existiert: " + auftrags_position_existenz);
								this.DEBUGG_LOGGER.addMassage("----------------------------------------------");

								// neue Position wird eingefuegt

								if (auftrags_position_existenz == false) {

									if (interne_artikel_nummer.contains("WP-") == false
											&& interne_artikel_nummer.contains("WPK-") == false
											&& interne_artikel_nummer != "" && interne_artikel_nummer != "?"
											&& interne_artikel_nummer.length() >= 5) {

										this.DEBUGG_LOGGER.addMassage("P: [" + position
												+ "] #{IFU_ALG_0345} INSERT beim UPDATE - Existenz=false");
										this.DEBUGG_LOGGER.addMassage("------------------------------------");

										this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + interne_artikel_nummer + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- Menge reserviert [" + neu_menge_reserviert_xml + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- vom Lager entnommen [" + neu_menge_lager_xml + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- Menge fakturiert [" + neu_menge_fakturiert_xml + "]");
										this.DEBUGG_LOGGER.addMassage(
												"---- Menge geliefert [" + neu_menge_lieferschein_xml + "]");

										this.DEBUGG_LOGGER.addMassage(
												"-------------------------------------------------------------------");

										String newLog1AuftragsPosition = "P: [" + position
												+ "] #{IFU_ALG_0345} INSERT beim UPDATE - Existenz=false "
												+ " \n---- Artikel: [" + interne_artikel_nummer + "],\n---- Menge="
												+ neu_menge_reserviert_xml + ",\n---- Mengelager=" + neu_menge_lager_xml
												+ ",\n---- Fakturierte Menge=" + neu_menge_fakturiert_xml
												+ ",\n---- Menge Lieferschein=" + neu_menge_lieferschein_xml
												+ "\n---- am: " + format2.format(new Date()) + " von A-RUN: ["
												+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
												+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n";

										String sqlInsertAuftragPosition = "Insert INTO static_auftraege_positionen (position,"
												+ " auftrags_nummer," //
												+ " artikel_nummer," //
												+ " menge_reserviert, " //
												+ " menge_lager, " //
												+ " menge_fakturiert, " //
												+ " menge_lieferschein," //
												+ " last_operation," //
												+ " last_operation_datum) VALUES (" + position + " ,'" + auftrags_nummer
												+ "','" + interne_artikel_nummer + "', " + neu_menge_reserviert_xml
												+ "," + neu_menge_lager_xml + "," + neu_menge_fakturiert_xml + ","
												+ neu_menge_lieferschein_xml + ", '" + newLog1AuftragsPosition + "' , '"
												+ formatdb.format(new Date()) + "' );";

										try {

											// jede Position wird eingetragen
											this.DB_CONNECTOR.insertToDatabase(sqlInsertAuftragPosition,
													"sqlInsertAuftragPosition");

										} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException o) {

											this.DEBUGG_LOGGER.addMassage("------- Doppelter-Datenbank-Eintrag ------");

											this.DEBUGG_LOGGER
													.addMassage("---- Artikel: [" + interne_artikel_nummer + "]");

											this.DEBUGG_LOGGER.addMassage(
													"-------------------------------------------------------------------");

										}

										String _alt_artikel_log = "";
										String neu_artikel_log = "";
										int _alt_artikel_lagerbestand = 0;
										int _alt_artikel_bereits_reserviert = 0;

										String SQLbereitsImRESERVIERTAusStaticArtikel = "Select reserviert_anzahl, IFNULL(agent_operation,''), lager_anzahl from static_artikel where interne_artikelnummer='"
												+ interne_artikel_nummer + "'";

										this.DB_CONNECTOR.readFromDatabase(SQLbereitsImRESERVIERTAusStaticArtikel,
												"SQLbereitsImRESERVIERTAusStaticArtikel");

										while (this.DB_CONNECTOR.rs.next()) {

											_alt_artikel_bereits_reserviert = this.DB_CONNECTOR.rs.getInt(1);
											_alt_artikel_log = this.DB_CONNECTOR.rs.getString(2);
											_alt_artikel_lagerbestand = this.DB_CONNECTOR.rs.getInt(3);
										}

										int nicht_im_lieferschein_uebernommen = (neu_menge_reserviert_xml
												- neu_menge_lieferschein_xml);

										int _neu_reserviert_im_artikel = (_alt_artikel_bereits_reserviert
												+ nicht_im_lieferschein_uebernommen);

										// Korrektur falls im Minusbereich
										if (nicht_im_lieferschein_uebernommen < 0) {
											nicht_im_lieferschein_uebernommen = 0;
										}

										neu_artikel_log = "-#{IFU_ALG_0345} AUFTRAG POSITION INSERT beim UPDATE ,\n---- Auftrag: [ "
												+ auftrags_nummer + " ],\n---- Artikel: [" + interne_artikel_nummer
												+ "] Artikelbestand im Lager = " + _alt_artikel_lagerbestand
												+ " Stueck,\n---- reservierte Lagerbestaende gesamt: [ a="
												+ _alt_artikel_bereits_reserviert + ", n=" + _neu_reserviert_im_artikel
												+ " ]\n---- [ durch diesen Auftrag reserviert="
												+ _neu_reserviert_im_artikel + " ]\n----Menge Reserviert: ["
												+ neu_menge_reserviert_xml + "]\n----Menge entnommen vom Lagert: ["
												+ neu_menge_lager_xml + "]\n----Menge Fakturiert: ["
												+ neu_menge_fakturiert_xml + "]\n----Menge geliefert: ["
												+ neu_menge_lieferschein_xml + "]\n---- am: "
												+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
												+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
												+ this.FILE_CREATION_CODE + "]";

										String SQLneuerArtikelReservierungAnzahl = "UPDATE static_artikel set reserviert_anzahl="
												+ _neu_reserviert_im_artikel + ", last_update='"
												+ formatdb.format(new Date()) + "', last_update_agent=" + AGENT_DBID
												+ ", agent_operation='"
												+ this.LOG_CHECKER.generateNewLogForDB(_alt_artikel_log,
														neu_artikel_log)
												+ "' where interne_artikelnummer='" + interne_artikel_nummer + "';";

										this.DB_CONNECTOR.updateOnDatabase(SQLneuerArtikelReservierungAnzahl);

										this.DEBUGG_LOGGER.addMassage("---- Der Auftrag hat den Artikel ["
												+ interne_artikel_nummer + "] aktualisiert:");

										this.DEBUGG_LOGGER.addMassage(
												"---- Reservierte Menge: [a=" + _alt_artikel_bereits_reserviert + ",n="
														+ _neu_reserviert_im_artikel + "]");

										this.DEBUGG_LOGGER
												.addMassage("---- Lagerbestand: [" + _alt_artikel_lagerbestand + "]");

										this.DEBUGG_LOGGER.addMassage("---- Bereits geliefert: ["
												+ neu_menge_lieferschein_xml + "] Stueck(e).");

										this.DEBUGG_LOGGER.addMassage(
												"---- Vom Lager entnommen: [" + neu_menge_lager_xml + "] Stueck(e).");

										this.DEBUGG_LOGGER.addMassage("---- noch zu liefern: ["
												+ nicht_im_lieferschein_uebernommen + "] Stueck(e).");

									}

								}

								// Vorhandene Position wird aktualisiert

								if (auftrags_position_existenz == true) {

									if (interne_artikel_nummer.contains("WP-") == false
											&& interne_artikel_nummer.contains("WPK-") == false
											&& interne_artikel_nummer != "" && interne_artikel_nummer != "?"
											&& interne_artikel_nummer.length() >= 5) {

										this.DEBUGG_LOGGER.addMassage(
												"---- #{ALG_UP_09812} AUFTRAG POSITION - UPDATE - Existenz=true");

										this.DEBUGG_LOGGER
												.addMassage("---- Position: [" + position + "] wird aktualisiert");
										this.DEBUGG_LOGGER.addMassage("---- Artikel: [" + interne_artikel_nummer + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- Menge reserviert [" + neu_menge_reserviert_xml + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- vom Lager entnommen [" + neu_menge_lager_xml + "]");
										this.DEBUGG_LOGGER
												.addMassage("---- Menge fakturiert [" + neu_menge_fakturiert_xml + "]");
										this.DEBUGG_LOGGER.addMassage(
												"---- Menge geliefert [" + neu_menge_lieferschein_xml + "]");

										int _alt_menge_reserviert = 0;
										int _alt_mengelager = 0;
										int _alt_mege_faktura = 0;
										int _alt_menge_geliefert = 0;
										String _alt_last_operation = "";

										String sqlReadPositionAuftrags1 = "Select menge_reserviert,menge_lager,menge_fakturiert,menge_lieferschein, last_operation from static_auftraege_positionen"
												+ " where artikel_nummer='" + interne_artikel_nummer
												+ "' and auftrags_nummer='" + auftrags_nummer + "' and position="
												+ position + " ;";

										this.DB_CONNECTOR.readFromDatabase(sqlReadPositionAuftrags1,
												"sqlReadPositionAuftrags1");

										while (this.DB_CONNECTOR.rs.next()) {
											_alt_menge_reserviert = this.DB_CONNECTOR.rs.getInt(1);
											_alt_mengelager = this.DB_CONNECTOR.rs.getInt(2);
											_alt_mege_faktura = this.DB_CONNECTOR.rs.getInt(3);
											_alt_menge_geliefert = this.DB_CONNECTOR.rs.getInt(4);
											_alt_last_operation = this.DB_CONNECTOR.rs.getString(5);

										}

										int uebrige_reservierte_menge = 0;
										int _alt_artikel_lager_anzahl = 0;
										int _alt_artikel_reserviert_anzahl = 0;
										String _alt_log_artikel = "";

										int _neu_artikel_lager_anzahl = 0;
										int _neu_artikel_reserviert_anzahl = 0;

										String SQLInitArtikel = "Select lager_anzahl, reserviert_anzahl, IFNULL(agent_operation,'') from static_artikel where interne_artikelnummer='"
												+ interne_artikel_nummer + "';";

										this.DB_CONNECTOR.readFromDatabase(SQLInitArtikel, "SQLInitArtikel");

										while (this.DB_CONNECTOR.rs.next()) {

											_alt_artikel_lager_anzahl = this.DB_CONNECTOR.rs.getInt(1);
											_alt_artikel_reserviert_anzahl = this.DB_CONNECTOR.rs.getInt(2);
											_alt_log_artikel = this.DB_CONNECTOR.rs.getString(3);
										}

										String _neu_last_operation = "- #{ALG_UP_09812: AUFTRAG - UPDATE - HEADER-EXISTANCE=true - POSITION-EXISTANCE=true}:\n---- Menge: [a="
												+ _alt_menge_reserviert + ", n=" + neu_menge_reserviert_xml
												+ "],\n---- MengeLager: [a=" + _alt_mengelager + ", n="
												+ neu_menge_lager_xml + "],\n---- Menge Fakturiert: [a="
												+ _alt_mege_faktura + ", n=" + neu_menge_fakturiert_xml
												+ "],\n---- Menge Lieferschein: [a=" + _alt_menge_geliefert + ", n="
												+ neu_menge_lieferschein_xml + "] ,\n---- am: "
												+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
												+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
												+ this.FILE_CREATION_CODE + "]\n\n";

										String logArtikelDB = this.LOG_CHECKER.generateNewLogForDB(_alt_last_operation,
												_neu_last_operation);

										String sqlUpdatePosition = "UPDATE static_auftraege_positionen set menge_reserviert="
												+ neu_menge_reserviert_xml + ", menge_lager=" + neu_menge_lager_xml
												+ ", menge_fakturiert=" + neu_menge_fakturiert_xml
												+ ", menge_lieferschein=" + neu_menge_lieferschein_xml
												+ ", last_operation='" + logArtikelDB
												+ "', noch_reserviert_im_artikel=0  where auftrags_nummer='"
												+ auftrags_nummer + "' and artikel_nummer='" + interne_artikel_nummer
												+ "' and position=" + position + ";";

										this.DB_CONNECTOR.updateOnDatabase(sqlUpdatePosition);

										// -------------------------------------------------------------------------

										// ---------------------------------
										uebrige_reservierte_menge = (neu_menge_reserviert_xml
												- neu_menge_lieferschein_xml);

										this.DEBUGG_LOGGER.addMassage("uebrige_reservierte_menge ["
												+ uebrige_reservierte_menge + "]= (neu_menge_reserviert ["
												+ neu_menge_reserviert_xml + "] - neu_menge_geliefert ["
												+ neu_menge_lieferschein_xml + "])");

										// ---------------------------------

										_neu_artikel_lager_anzahl = _alt_artikel_lager_anzahl;

										// ---------------------------------
										_neu_artikel_reserviert_anzahl = ((_alt_artikel_reserviert_anzahl
												- (_alt_menge_reserviert - _alt_menge_geliefert))
												+ (neu_menge_reserviert_xml - neu_menge_lieferschein_xml));

										this.DEBUGG_LOGGER.addMassage("_neu_artikel_reserviert_anzahl ["
												+ _neu_artikel_reserviert_anzahl
												+ "] = ((_alt_artikel_reserviert_anzahl  ["
												+ _alt_artikel_reserviert_anzahl + "]  - (_alt_menge_reserviert ["
												+ _alt_menge_reserviert + "] - _alt_menge_geliefert ["
												+ _alt_menge_geliefert + "] )) + (neu_menge_reserviert ["
												+ neu_menge_reserviert_xml + "]  - neu_menge_geliefert  ["
												+ neu_menge_lieferschein_xml + "] ))");
										// ---------------------------------

										this.DEBUGG_LOGGER.addMassage(

												"--------------------------------ARTIKEL UPDATE--------------------------------");

										this.DEBUGG_LOGGER.addMassage("---- Der Auftrag hat den Artikel ["
												+ interne_artikel_nummer + "] aktualisiert: ");

										this.DEBUGG_LOGGER.addMassage(
												"---- Reservierte Menge: [a=" + _alt_artikel_reserviert_anzahl + ",n="
														+ _neu_artikel_reserviert_anzahl + "]");

										this.DEBUGG_LOGGER.addMassage("---- Bereits geliefert: ["
												+ neu_menge_lieferschein_xml + "] Stueck(e).");

										this.DEBUGG_LOGGER.addMassage(
												"---- Vom Lager entnommen: [" + neu_menge_lager_xml + "] Stueck(e).");

										this.DEBUGG_LOGGER.addMassage("---- noch zu liefern: ["
												+ (neu_menge_reserviert_xml - neu_menge_lieferschein_xml)
												+ "] Stueck(e).");

										this.DEBUGG_LOGGER.addMassage(
												"********************************************************************************");

										String _neu_log_artikel1 = "- #{ALG_UP_09812} - AUFTRAGSPOSITION AKTUALISIERT ARTIKEL (erledigt):"
												+ " \n---- Auftrag: [" + auftrags_nummer + "]," + "\n---- Artikel: ["
												+ interne_artikel_nummer + "]," + "\n---- Artikelbetand: [a="
												+ _alt_artikel_lager_anzahl + ", n=" + _neu_artikel_lager_anzahl + "],"
												+ "\n---- Reserviert im Artikel: [a=" + _alt_artikel_reserviert_anzahl
												+ ", n=" + _neu_artikel_reserviert_anzahl + "],\n---- am: "
												+ format2.format(new Date()) + " von A-RUN: [" + this.AGENT_RUN_CODE
												+ "] und M-RUN: [" + this.MANAGER_RUN_CODE + "]\n---- Datei: ["
												+ this.FILE_CREATION_CODE + "]\n";

										String logArtikelDB1 = this.LOG_CHECKER.generateNewLogForDB(_alt_log_artikel,
												_neu_log_artikel1);

										String sqlUpdateArtikel = "UPDATE static_artikel set reserviert_anzahl="
												+ _neu_artikel_reserviert_anzahl + ", agent_operation='" + logArtikelDB1
												+ "', last_update='" + formatdb.format(new Date())
												+ "', last_update_agent=" + this.AGENT_DBID
												+ " where interne_artikelnummer='" + interne_artikel_nummer + "';";

										this.DB_CONNECTOR.updateOnDatabase(sqlUpdateArtikel);

										// -------------------------------------------------------------------------

										if (erledigt == true && _alt_erledigt == false
												|| (erledigt == false && storno == true && _alt_storno == false)) {

											// Korrektur:
											// Wenn noch was reserviert ist
											// dann wird der Ueberschuss
											// reduziert.

											_neu_artikel_reserviert_anzahl = (_neu_artikel_reserviert_anzahl
													- uebrige_reservierte_menge);

											this.DEBUGG_LOGGER.addMassage("_neu_artikel_reserviert_anzahl ["
													+ _neu_artikel_reserviert_anzahl
													+ "] = (_neu_artikel_reserviert_anzahl ["
													+ _neu_artikel_reserviert_anzahl + "] - uebrige_reservierte_menge ["
													+ uebrige_reservierte_menge + "])");

											if (erledigt == true && _alt_erledigt == false) {

												this.DEBUGG_LOGGER.addMassage(
														"-------------------------AUFTRAG ERLEDIGT-->--ARTIKEL UPDATE-------------------------");

												this.DEBUGG_LOGGER.addMassage(
														"---- #{0022} - #{ALG: AUFTRAG - INSERT - HEADER-EXISITENCE=FALSE - POSITION-KEIN-WP-ARTIKEL - ERLEDIGT=true}");

												this.DEBUGG_LOGGER.addMassage(
														"---- Auftrag: [" + auftrags_nummer + "] wurde erledigt:");

												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------");

												String _neu_log_artikel2 = "- #{00888}- AUFTRAGSPOSITION AKTUALISIERT ARTIKEL (erledigt):"
														+ " \n---- Auftrag: [" + auftrags_nummer + "],"
														+ "\n---- Artikel: [" + interne_artikel_nummer + "],"
														+ "\n---- Artikelbetand: [a=" + _alt_artikel_lager_anzahl
														+ ", n=" + _neu_artikel_lager_anzahl + "],"
														+ "\n---- Reserviert im Artikel: [a="
														+ _alt_artikel_reserviert_anzahl + ", n="
														+ _neu_artikel_reserviert_anzahl + "],\n---- am: "
														+ format2.format(new Date()) + " von A-RUN: ["
														+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
														+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n";

												String logArtikelDB2 = this.LOG_CHECKER
														.generateNewLogForDB(_alt_log_artikel, _neu_log_artikel2);

												sqlUpdateArtikel = "UPDATE static_artikel set reserviert_anzahl="
														+ _neu_artikel_reserviert_anzahl + ", agent_operation='"
														+ logArtikelDB2 + "', last_update='"
														+ formatdb.format(new Date()) + "', last_update_agent="
														+ this.AGENT_DBID + " where interne_artikelnummer='"
														+ interne_artikel_nummer + "';";

												this.DB_CONNECTOR.updateOnDatabase(sqlUpdateArtikel);

												String guid = "AUFTRAG_" + auftrags_nummer + "_" + position + "_"
														+ neu_menge_lieferschein_xml + "_" + interne_artikel_nummer;

												String SQL_AUFTRAG = "INSERT INTO static_warenausgang_positionen (guid, artikelnummer, menge, verkauft_am, typid) VALUE ('"
														+ guid + "','" + interne_artikel_nummer + "',"
														+ neu_menge_lieferschein_xml + ", '"
														+ tempDatumWareneingangAusXML + "',0);";

												try {

													this.DB_CONNECTOR.insertToDatabase(SQL_AUFTRAG, "SQL_AUFTRAG");

												} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException o) {

													this.DEBUGG_LOGGER
															.addMassage("------- Doppelter-Datenbank-Eintrag ------");
													this.DEBUGG_LOGGER.addMassage(o.getMessage());

													this.DEBUGG_LOGGER
															.addMassage("-------------------------------------------");
												}

											}

											if (storno == true) {

												_neu_artikel_reserviert_anzahl = (_alt_artikel_reserviert_anzahl
														- neu_menge_reserviert_xml);

												this.DEBUGG_LOGGER.addMassage(
														"-------------------------AUFTRAG STORNIERT-->--ARTIKEL UPDATE-------------------------");

												this.DEBUGG_LOGGER.addMassage(
														"---- #{0023} - #{ALG: AUFTRAG - INSERT - HEADER-EXISITENCE=FALSE - POSITION-KEIN-WP-ARTIKEL - STORNO=true}");

												this.DEBUGG_LOGGER.addMassage(
														"---- Auftrag: [" + auftrags_nummer + "] wurde storniert:");

												this.DEBUGG_LOGGER.addMassage(
														"---------------------------------------------------------------\n");

												String _neu_log_artikel3 = "- #{UP_0023_STORNO}- AUFTRAGSPOSITION AKTUALISIERT ARTIKEL (Storno):"
														+ " \n---- Auftrag: [" + auftrags_nummer + "],"
														+ "\n---- Artikel: [" + interne_artikel_nummer + "],"
														+ "\n---- Artikelbetand: [a=" + _alt_artikel_lager_anzahl
														+ ", n=" + _neu_artikel_lager_anzahl + "],"
														+ "\n---- Reserviert im Artikel: [a="
														+ _alt_artikel_reserviert_anzahl + ", n="
														+ _neu_artikel_reserviert_anzahl + "],\n---- am: "
														+ format2.format(new Date()) + " von A-RUN: ["
														+ this.AGENT_RUN_CODE + "] und M-RUN: [" + this.MANAGER_RUN_CODE
														+ "]\n---- Datei: [" + this.FILE_CREATION_CODE + "]\n\n";

												String logArtikelDB3 = this.LOG_CHECKER
														.generateNewLogForDB(_alt_log_artikel, _neu_log_artikel3);

												sqlUpdateArtikel = "UPDATE static_artikel reserviert_anzahl="
														+ _neu_artikel_reserviert_anzahl + ", agent_operation='"
														+ logArtikelDB3 + "', last_update='"
														+ formatdb.format(new Date()) + "', last_update_agent="
														+ this.AGENT_DBID + " where interne_artikelnummer='"
														+ interne_artikel_nummer + "';";

												this.DB_CONNECTOR.updateOnDatabase(sqlUpdateArtikel);
											}

											// -------------------------------------------------------------------------

											this.DEBUGG_LOGGER.addMassage("- Artikel: [" + interne_artikel_nummer
													+ "] wurde vom Auftrag: [" + auftrags_nummer + "] aktualisiert.");

											this.DEBUGG_LOGGER
													.addMassage("---- Artikelbetand: [a=" + _alt_artikel_lager_anzahl
															+ ", n=" + _neu_artikel_lager_anzahl + "]");

											this.DEBUGG_LOGGER
													.addMassage("---- Reserviert: [a=" + _alt_artikel_reserviert_anzahl
															+ ", n=" + _neu_artikel_reserviert_anzahl + "]," + "");

											this.DEBUGG_LOGGER.addMassage(
													"*************************************************************************************************");

										}
										
								
										

									}

								}

							}

							this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>PROCESSED<< VERSCHOBEN] \n");

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
							this.DEBUGG_LOGGER
									.addMassage("--------------------------------------------------------------");

							// ----------------------------------------------------------------------------------------------------------

						} else {

							this.DEBUGG_LOGGER.addMassage("AUFTRAG [" + auftrags_nummer
									+ "] hat keine Positionen. - Datei: [" + this.FILE_CREATION_CODE + "]");
							this.DEBUGG_LOGGER
									.addMassage("----------------------------------------------------------------");
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
							this.DEBUGG_LOGGER
									.addMassage("--------------------------------------------------------------");

							// ----------------------------------------------------------------------------------------------------------

						}

					} else {

						this.DEBUGG_LOGGER.addMassage("Es wurde kein Insert-Eintrag gefunden zu dem Auftrag: ["
								+ auftrags_nummer + "] - Datei: [" + this.FILE_CREATION_CODE + "]");

						this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >>STACK<< VERSCHOBEN]");

						this.DEBUGG_LOGGER.addMassage("Datenbank Eintrag der Datei wird geloescht.");

						this.DB_CONNECTOR.updateOnDatabase(
								"DELETE from work_agent_files where file_creation_code='" + FILE_CREATION_CODE + "';");

						// Verschiebe Datei
						// ----------------------------------------------------------------------------------------------------------

						String system_stack_file_name = (MANAGER_filepositionsindex + 1) + this.AUSWAHL_MANDANT_NAME
								+ this.AGENT_FILE_NAME_PATTERN + "_stack_" + FILE_CREATION_CODE + ".xml";

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
								+ laufzeit_dauer(this.AGENT_START_PROCESS_TIME, this.AGENT_END_PROCESS_TIME) + "");
						this.DEBUGG_LOGGER.addMassage("--------------------------------------------------------------");

						// ----------------------------------------------------------------------------------------------------------

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

				this.DEBUGG_LOGGER.addMassage("[DATEI WIRD NACH >> xfwep_DUPLIKATE << VERSCHOBEN]");

				this.FILE_STATUS_ID = 4;

				beende_datei(FILE_CREATION_CODE, SINGEL_FILE_PROCESS_END_TIME, AGENT_DBID, this.FILE_STATUS_ID,
						this.FILE_CONTENT_ID, this.ANZAHL_XML_OBJEKTE, this.AGENT_WORK_XML_FILE_NAME,
						AGENT_PROCESSED_XML_FILE_NAME, this.DB_CONNECTOR);

				this.DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] > "
						+ laufzeit_dauer(SINGEL_FILE_PROCESS_START_TIME, SINGEL_FILE_PROCESS_END_TIME) + " ");

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
				this.DEBUGG_LOGGER.addMassage("--------------------------------------------------------------");
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

	public boolean existiert_dieser_auftrag(String _auftagsnummer) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_auftraege WHERE interne_auftragsnummer='"
				+ _auftagsnummer + "');";

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;
	}

	public boolean checkAuftragsPosition(int _position, String _auftragsnummer, String _artikelnummer)
			throws SQLException {

		String sqlCheckExsistenz = "SELECT EXISTS ( SELECT * FROM static_auftraege_positionen WHERE auftrags_nummer='"
				+ _auftragsnummer + "' and artikel_nummer='" + _artikelnummer + "' and position=" + _position + " );";

		System.out.println(sqlCheckExsistenz);

		boolean temp = true;

		this.DB_CONNECTOR.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (this.DB_CONNECTOR.rs.next())

		{
			temp = this.DB_CONNECTOR.rs.getBoolean(1);
		}

		return temp;

	}

}
