package INITAL_MANAGER;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Agent implements java.io.Serializable {

    private static final long serialVersionUID = 4161075004349583071L;

    boolean TRUNCATE_TABLE_BEFORE_INSERT = false;

    boolean TRUNCATE_TABLE_BEFORE_INSERT_SINGLE_FILE = false;

    boolean DEBUGGER_STATUS = true;

    boolean AGENT_ACTIVITY = false;

    boolean IS_LINUX = true;

    SimpleDateFormat long_date_formate = new SimpleDateFormat("yyyyMMddHHmmss");

    SimpleDateFormat short_rewert_format = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat long_datum_zeit_formate = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm:ss");

    SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    int WORK_FILE_DB_ID = 0;

    int AGENT_INSTANZ_RUN_DBID = 0;

    Date AGENT_START_PROCESS_TIME = null;

    Date AGENT_END_PROCESS_TIME = null;

    Date FILE_PROCESS_START_TIME = null;

    Date FILE_PROCESS_END_TIME = null;

    String AGENT_FILE_DELIVER_PATH = "";

    String AGENT_FILE_WORK_PATH = "";

    String AGENT_FILE_PROCESSED_PATH = "";

    String AGENT_FILE_LOGFILE_PATH = "";

    String AGENT_LAST_PROZESS_TIME = "";

    String AGENT_FILE_NAME_PATTERN = "";

    String FILE_CREATION_TIME = "";

    String OPERATION_SYSTEM = "";

    String SYSTEM_PATH_DELIMITER = System.getProperty("file.separator") + System.getProperty("file.separator");

    String AGENT_RUN_CREATION_CODE = "";

    String MANAGER_RUN_CREATION_CODE = "";

    String FILE_CREATION_CODE = "";

    long FILE_SIZE = 0;

    String AGENT_NAME = "";

    int WP_MANDANT_DBID = 111;

    int KA_MANDANT_DBID = 222;

    int sqlZise = 0;

    int anzahl_XML_objekte_pro_datei = 0;

    int anzahl_erzeugter_SQL_objekte_der_datei = 0;

    int ANZAHL_XML_OBJEKTE_GESAMT = 0;

    int ANZAHL_VERARBEITETE_XML_DATEIEN = 0;

    int RUN_MANAGER_DBID = 0;

    int AGENT_DBID = 0;

    int FILE_CONTENT_DBID = 0;

    int SQL_MAX_COLLECTION_SIZE = 0;

    int AGENT_STATUS_CURRENT_CODE = 0;

    int MANAGER_START_POSITION = 0;

    int AUSWAHL_MANDANT_DBID = 0;

    String AUSWAHL_MANDANT_NAME = "";

    long agent_full_process_diff_time_in_sec = 0;

    long single_file_full_process_diff_time_in_sec = 0;

    RandomCode ZUFALLS_GENERATOR = new RandomCode();

    Debugger AGENT_DEBUG_INFO = null;

    String AGENT_WORK_XML_FILE_NAME = "";

    DatenBankVerbindung DB_CONNECTOR = new DatenBankVerbindung();

    String AGENT_PROCESSED_XML_FILE_NAME = "";
    String SYSTEM_DELIVER_FILE_NAME = "";
    String SYSTEM_STACK_FILE_NAME = "";

    public String getSystem_processed_file_name(int _index) {

	DecimalFormat df = new DecimalFormat("00000");

	int index = (_index + 1);

	return this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + "proccesed_" + df.format(index) + "_MC_"
		+ this.MANAGER_RUN_CREATION_CODE + "_FC_" + FILE_CREATION_CODE + ".xml";

    }

    public String getSystem_work_file_name(int _index) {

	DecimalFormat df = new DecimalFormat("00000");

	int index = (_index + 1);

	return this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + "work_" + df.format(index) + "_MC_"
		+ this.MANAGER_RUN_CREATION_CODE + "_FC_" + FILE_CREATION_CODE + ".xml";

    }

    public String getSystem_duplikat_file_name(int _index) {

	DecimalFormat df = new DecimalFormat("00000");

	int index = (_index + 1);

	return this.AGENT_FILE_NAME_PATTERN + this.AUSWAHL_MANDANT_NAME + "duplikat_" + df.format(index) + "_MC_"
		+ this.MANAGER_RUN_CREATION_CODE + "_FC_" + FILE_CREATION_CODE + ".xml";

    }
}
