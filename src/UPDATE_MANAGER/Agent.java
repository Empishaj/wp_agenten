package UPDATE_MANAGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;

public abstract class Agent implements java.io.Serializable {

	private static final long serialVersionUID = -6891895020707991231L;

	DatenBankVerbindung DB_CONNECTOR;
	int AUSWAHL_MANDANT_DBID = 0;
	String AUSWAHL_MANDANT_NAME = "";
	String MD5_FILE_HASHCODE = "";
	int AGENT_INSTANZ_RUN_DBID = 0;
	int WORK_FILE_DB_ID = 0;

	String OPERATING_SYSTEM = "";
	boolean IS_LINUX = true;
	boolean CHECK_HEADER_ENTRY_EXISTENCE = false;
	boolean XML_FILE_WAS_EVER_PROCESSED = true;
	boolean AGENT_ACTIVITY = false;
	boolean XML_CONTENT_IS_INSERT_TYPE = false;
	int AGENT_STATUS_CURRENT_CODE = 0;
	int MANAGER_START_POSITION = 0;
	int FILE_CONTENT_ID = 0;

	/*
	*
	*/
	boolean TRUNCATE_TABLE_BEFORE_INSERT = false;
	/*
	*
	*/
	boolean TRUNCATE_TABLE_BEFORE_INSERT_SINGLE_FILE = false;
	/*
	 *
	 */
	boolean DEBUG_STATUS = true;
	/*
	 * FILE_STATUS_ID Zeigt an in welchen Stand der Verarbeitung sich die Datei
	 * befindet
	 */
	int FILE_STATUS_ID = 0;

	/*
	 * RUN_MANAGER_ID Das ist die DBID der laufenden Inszanz des Managers
	 */
	int RUN_MANAGER_ID = 0;

	// --------------------------------------

	String AGENT_DUPLIKAT_PATH = "";
	String AGENT_FILE_STACK_PATH = "";
	String FAIL_PATH = "";
	File AGENT_DELIVER_XML_FILE = null;
	File AGENT_WORK_FILE_XML = null;

	String AGENT_WORK_XML_FILE_NAME = "";

	String AGENT_PROCESSED_XML_FILE_NAME = "";
	
	String AGENT_DUPLIKAT_XML_FILE_NAME = "";

	String FILE_CREATION_TIME = "";

	String FILE_CREATION_TIME_FOR_DB = "";

	int AGENT_DBID = 0;

	String AGENT_NAME = "";

	Date SINGEL_FILE_PROCESS_START_TIME = new Date();

	Date SINGEL_FILE_PROCESS_END_TIME = new Date();

	String AGENT_FILE_DELIVER_PATH = "";

	String AGENT_FILE_WORK_PATH = "";

	String AGENT_FILE_PROCESSED_PATH = "";

	String AGENT_FILE_LOGFILE_PATH = "";

	String AGENT_LAST_PROZESS_TIME = "";

	String AGENT_FILE_NAME_PATTERN = "";

	Date AGENT_START_PROCESS_TIME = new Date();

	Date AGENT_END_PROCESS_TIME = new Date();

	RandomCode ZUFALLS_GENERATOR = new RandomCode();

	Debuger DEBUGG_LOGGER = null;

	long FILE_SIZE = 0;

	/*
	 * - isInsertType Hier wird gespeichert ob die gerade verarbeitete Datei ein
	 * Update oder ein Insert Typ ist.
	 */

	String FILE_CONTENT_OPERATION_TYPE = "?";

	Document XML_DOKUMENT = null;

	LogChecker LOG_CHECKER = new LogChecker();

	String FILE_CREATION_CODE = "";

	/*
	 * Jenach dem welches Betriessystem verwendet wird kann sich das aendern
	 */
	String SYSTEM_PATH_DELIMITER = System.getProperty("file.separator") + System.getProperty("file.separator");

	/*
	 * 
	 * Jede Instanz eines Agenten hat einen eigenen RUN-CODE
	 */
	String AGENT_RUN_CODE = "";

	/*
	 * Jede laufende Instanz eines Managers hat einen eigenen RUN_CODE
	 */
	String MANAGER_RUN_CODE = "";

	SimpleDateFormat format0 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss");

	SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yyyy um HH:mm:ss");

	SimpleDateFormat formatdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	SimpleDateFormat XML_FORMAT_DATUM = new SimpleDateFormat("yyyy-MM-dd");

	int ANZAHL_XML_OBJEKTE = 0;

	int anzahlSQL = 0;

	int anzahXMLGesamt = 0;

	int anzahlXMLDateien = 0;

	public boolean wurde_diese_datei_schon_mal_verarbeitet(File _deliverd_file, DatenBankVerbindung db, int agent_id)
			throws IOException, SQLException {

		boolean temp = true;

		FileInputStream fis = new FileInputStream(_deliverd_file);

		this.MD5_FILE_HASHCODE = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);

		fis.close();

		String sqlCheckExistenzOfInsert = "SELECT EXISTS (SELECT * FROM work_agent_files WHERE filehash='"
				+ this.MD5_FILE_HASHCODE + "')";

		db.readFromDatabase(sqlCheckExistenzOfInsert, "sqlCheckExistenzOfInsert ");

		while (db.rs.next())

		{
			temp = db.rs.getBoolean(1);
		}

		System.out.println("--|> HASH: " + this.MD5_FILE_HASHCODE + " - Dupplikat: [" + temp + "]");

		return temp;
	}

	public void initialisiere_agenten_000(DatenBankVerbindung db, int id) throws SQLException, ClassNotFoundException {

		this.AGENT_STATUS_CURRENT_CODE = AgentenStati.STATUS_INITIALISIERUNG;

		this.AGENT_RUN_CODE = ZUFALLS_GENERATOR.newRandomCode();

		this.AGENT_START_PROCESS_TIME = new Date();

		String sql001 = "SELECT agentname_kurz,  COALESCE(last_prozess_time, 'noch nie'),  activity, "
				+ " file_name_pattern, truncate_table_before_insert, truncate_table_before_insert_single_file,debug_status,  manager_start_position "
				+ " FROM agent where dbid=" + id + ";";

		db.readFromDatabase(sql001, "initialisiere_agenten_000");

		while (db.rs.next()) {

			this.AGENT_NAME = db.rs.getString(1);
			this.AGENT_LAST_PROZESS_TIME = db.rs.getString(2);
			this.AGENT_ACTIVITY = db.rs.getBoolean(3);
			this.AGENT_FILE_NAME_PATTERN = db.rs.getString(4);
			this.TRUNCATE_TABLE_BEFORE_INSERT = db.rs.getBoolean(5);
			this.TRUNCATE_TABLE_BEFORE_INSERT_SINGLE_FILE = db.rs.getBoolean(6);
			this.DEBUG_STATUS = db.rs.getBoolean(7);
			this.MANAGER_START_POSITION = db.rs.getInt(8);

		}

		this.DEBUGG_LOGGER = new Debuger(this.DEBUG_STATUS);

		db.updateOnDatabase(
				"INSERT INTO agent_run_logs (agenten_id, manager_id, run_creation_code, start_time, status_id, manager_run_creation_code ) VALUES ("
						+ AGENT_DBID + "," + RUN_MANAGER_ID + ",'" + this.AGENT_RUN_CODE + "','"

						+ formatdb.format(AGENT_START_PROCESS_TIME) + "', " + this.AGENT_STATUS_CURRENT_CODE + ",'"
						+ this.MANAGER_RUN_CODE + "');");

		String sqlFindId = "Select id from agent_run_logs where run_creation_code='" + this.AGENT_RUN_CODE + "'";

		db.readFromDatabase(sqlFindId, "sqlFindId");

		while (db.rs.next()) {

			this.AGENT_INSTANZ_RUN_DBID = db.rs.getInt(1);
		}

	}

	public void initialisiere_agenten_pfade(DatenBankVerbindung db) {
		try {

			if (this.IS_LINUX == false) {

				String sql002 = "SELECT a.java_parameter_value as system_paths_deliver_win, b.java_parameter_value  as system_paths_work_win, c.java_parameter_value  as system_paths_proccesed_win, d.java_parameter_value  as system_paths_logs_win, e.java_parameter_value  as system_paths_logs_win from system_parameter as a, system_parameter as b, system_parameter as c,system_parameter as d, system_parameter as e  where a.java_parameter_name='system_paths_deliver_win' and  b.java_parameter_name='system_paths_work_win' and c.java_parameter_name='system_paths_processed_win' and d.java_parameter_name='system_paths_logs_win' and e.java_parameter_name='system_paths_stack_win';";

				db.readFromDatabase(sql002, "initialisiere_agenten_pfade");

				while (db.rs.next()) {

					this.AGENT_FILE_DELIVER_PATH = db.rs.getString(1);
					this.AGENT_FILE_WORK_PATH = db.rs.getString(2);
					this.AGENT_FILE_PROCESSED_PATH = db.rs.getString(3);
					this.AGENT_FILE_LOGFILE_PATH = db.rs.getString(4);
					this.AGENT_FILE_STACK_PATH = db.rs.getString(5);

				}

			}

			if (this.IS_LINUX == true) {

				String sql002 = "SELECT a.java_parameter_value as system_paths_deliver_linux, b.java_parameter_value  as system_paths_work_linux, c.java_parameter_value  as system_paths_proccesed_linux, d.java_parameter_value  as system_paths_logs_linux, e.java_parameter_value  as system_paths_logs_linux from system_parameter as a, system_parameter as b, system_parameter as c,system_parameter as d, system_parameter as e  where a.java_parameter_name='system_paths_deliver_linux' and  b.java_parameter_name='system_paths_work_linux' and c.java_parameter_name='system_paths_processed_linux' and d.java_parameter_name='system_paths_logs_linux' and e.java_parameter_name='system_paths_stack_linux';";

				db.readFromDatabase(sql002, "iiasa_initial_import_artikel_stammdaten_agent");

				while (db.rs.next()) {

					this.AGENT_FILE_DELIVER_PATH = db.rs.getString(1);
					this.AGENT_FILE_WORK_PATH = db.rs.getString(2);
					this.AGENT_FILE_PROCESSED_PATH = db.rs.getString(3);
					this.AGENT_FILE_LOGFILE_PATH = db.rs.getString(4);
					this.AGENT_FILE_STACK_PATH = db.rs.getString(5);

				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void beende_agenten_run(int _status, int _agent_id, String _run_creation_code, String _managerruncode,
			Date ende, int _anzahl, String log, DatenBankVerbindung _db) {

		String sql = "UPDATE agent_run_logs set " + " log_content='" + log + "', " + " end_time='"
				+ formatdb.format(ende) + "'," + " datensatz_gesamt_anzahl=" + _anzahl + ","
				+ " datei_bearbeitet_anzahl=1, " + " status_id=" + _status + " where run_creation_code='"
				+ _run_creation_code + "' " + " and " + " manager_run_creation_code='" + _managerruncode + "';";

		_db.updateOnDatabase(sql);

	}

	public void put_deliver_file_in_db(int _agentid, String _filename, String _starttime, String _agent_run_code,
			long _filesize, String _file_creation_time_db, String _filecode, int _agent_runid, String _hash,
			DatenBankVerbindung _db, int mandant_id) throws ClassNotFoundException, SQLException {

		String sql_insert_file = "Insert into " //
				+ " work_agent_files (" //
				+ " agent_id," //
				+ " deliver_filename, " //
				+ " prozess_begin_time," //
				+ " run_creation_code, " //
				+ " file_status_id, "//
				+ " dateigroesse,"//
				+ " erstelldatum," //
				+ " file_creation_code, "//
				+ " agent_run_id, "//
				+ " filehash," //
				+ " mandant_id ) VALUES (" //
				+ _agentid + ",'" //
				+ _filename + "','" //
				+ _starttime + "', '" //
				+ _agent_run_code + "'," //
				+ 1 + " , " //
				+ _filesize + ",'" //
				+ //
				_file_creation_time_db + "','" //
				+ _filecode + "'," + _agent_runid + ",'" + _hash + "', " + mandant_id + " );";

		_db.insertToDatabase(sql_insert_file, "put_deliver_file_in_db");

	}

	public int find_work_file_dbid(String _creation_code, DatenBankVerbindung _db) throws SQLException {

		int temp = 0;

		String sql_find_work_file_db_id = "SELECT id FROM work_agent_files where file_creation_code='" + _creation_code
				+ "';";

		_db.readFromDatabase(sql_find_work_file_db_id, "find_work_file_dbid");

		while (_db.rs.next()) {

			temp = _db.rs.getInt(1);

		}
 

		return temp;
	}

	public void update_agenten_prozess_status(int status, String creationcode, DatenBankVerbindung _db) {

		_db.updateOnDatabase(
				"UPDATE agent_run_logs set status_id=" + status + " where run_creation_code='" + creationcode + "';");

	}

	public void beende_agenten_objekt(Date _AGENT_END_PROCESS_TIME, int agentid, DatenBankVerbindung _db) {

		_db.updateOnDatabase("UPDATE agent set last_prozess_time='" + formatdb.format(_AGENT_END_PROCESS_TIME)
				+ "' where dbid=" + agentid);

	}

	public void beende_datei(String filecode, Date ende, int agentid, int filestytusid, int contentid, int datensaetze,
			String worgfilename, String prozesedFilename, DatenBankVerbindung _db) {

		String sql = "UPDATE work_agent_files set prozess_end_time='" + formatdb.format(ende) + "', file_status_id="
				+ filestytusid + ",  datensaetze_anzahl=" + datensaetze + ", work_filename='" + worgfilename
				+ "', processed_filename='" + prozesedFilename + "', filecontent_id=" + contentid //
				+ " where file_creation_code='" + filecode + "';";

		_db.updateOnDatabase(sql);

	}

	public String laufzeit_dauer(Date start, Date ende) {

		String temp = "";

		long agent_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS.toSeconds(ende.getTime() - start.getTime());

		temp = agent_full_process_diff_time_in_sec + " Sekunden =  " + (agent_full_process_diff_time_in_sec / 60)
				+ " Minuten";

		return temp;

	}

	public String getSystem_processed_file_name(int mangerid) {

		DecimalFormat df = new DecimalFormat("00000");

		int index = mangerid + 1;

		AGENT_PROCESSED_XML_FILE_NAME = FILE_CREATION_TIME + "_" + this.AGENT_FILE_NAME_PATTERN
				+ this.AUSWAHL_MANDANT_NAME + "proccesed_" + df.format(index) + "_MC_" + this.MANAGER_RUN_CODE + "_FC_"
				+ FILE_CREATION_CODE + ".xml";

		return AGENT_PROCESSED_XML_FILE_NAME;
	}
	
	
	public String getSystem_work_file_name(int mangerid) {

		DecimalFormat df = new DecimalFormat("00000");

		int index = mangerid + 1;

		AGENT_PROCESSED_XML_FILE_NAME = FILE_CREATION_TIME + "_" + this.AGENT_FILE_NAME_PATTERN
				+ this.AUSWAHL_MANDANT_NAME + "work_" + df.format(index) + "_MC_" + this.MANAGER_RUN_CODE + "_FC_"
				+ FILE_CREATION_CODE + ".xml";

		return AGENT_PROCESSED_XML_FILE_NAME;
	}
	
	
	
	public String getSystem_duplikat_file_name(int mangerid) {

		DecimalFormat df = new DecimalFormat("00000");

		int index = mangerid + 1;

		AGENT_PROCESSED_XML_FILE_NAME = FILE_CREATION_TIME + "_" + this.AGENT_FILE_NAME_PATTERN
				+ this.AUSWAHL_MANDANT_NAME + "duplikat_" + df.format(index) + "_MC_" + this.MANAGER_RUN_CODE + "_FC_"
				+ FILE_CREATION_CODE + ".xml";

		return AGENT_PROCESSED_XML_FILE_NAME;
	}

	public String getSystem_BackToDeliverFILENAME(int mangerid) {

		DecimalFormat df = new DecimalFormat("00000");

		int index = mangerid + 1;

		return FILE_CREATION_TIME + "_" + this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + "backToDeliver_"
				+ df.format(index) + ".xml";
	}

	public String find_agent_name_by_id(int id, DatenBankVerbindung _db) throws SQLException {

		String temp = "";

		String sql = "SELECT agentname_kurz FROM agent where dbid=" + id + ";";

		_db.readFromDatabase(sql, "find_agent_name_by_id");

		while (_db.rs.next()) {

			temp = _db.rs.getString(1);

		}

		return temp;
	}

	public void setFileOperationType(String _fileHeader) {

		try {
			this.FILE_CONTENT_OPERATION_TYPE = _fileHeader;

		} catch (NullPointerException ecxc) {

			FILE_CONTENT_OPERATION_TYPE = "?";
		}

		if ("Insert".equals(this.FILE_CONTENT_OPERATION_TYPE) || "Insert" == this.FILE_CONTENT_OPERATION_TYPE)

		{

			this.XML_CONTENT_IS_INSERT_TYPE = true;

		} else {

			this.XML_CONTENT_IS_INSERT_TYPE = false;

		}

	}

}
