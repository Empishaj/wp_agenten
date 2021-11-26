package INITAL_MANAGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InitalImportManager implements java.io.Serializable {

    private static final long serialVersionUID = 8914377806188336810L;

    static String sqlDeliverPathWin = "SELECT a.java_parameter_value as aa, e.java_parameter_value as ee, f.java_parameter_value as ff FROM system_parameter as a,  system_parameter as e, system_parameter as f WHERE a.java_parameter_name='system_paths_deliver_win' and e.java_parameter_name='system_paths_logs_win' and f.java_parameter_name='system_path_webspace_win';";

    static String sqlDeliverPathLinux = "SELECT a.java_parameter_value as aa, e.java_parameter_value as ee ,  f.java_parameter_value as ff FROM system_parameter as a,  system_parameter as e,   system_parameter as f WHERE a.java_parameter_name='system_paths_deliver_linux' and e.java_parameter_name='system_paths_logs_linux' and f.java_parameter_name='system_path_webspace_linux';";

    static DatenBankVerbindung db = new DatenBankVerbindung();

    static SimpleDateFormat formatdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static SimpleDateFormat formatlogfile = new SimpleDateFormat("yyyyMMddHHmmss");

    static SimpleDateFormat format_log_file_human = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    static RandomCode random = new RandomCode(15);

    static Debugger MANAGER_DEBUG_COLLECTOR = new Debugger();

    static String run_creation_code = random.newRandomCode();

    static int MANAGERID = 2;

    public static void main(String[] args) throws Exception {

	// Pre-Processing

	Date MANAGER_START_PROCESS_TIME = new Date();

	MANAGER_DEBUG_COLLECTOR.addMassage("MANAGER-RUN-IDENTITY: [ " + run_creation_code + " ]");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[AGENTEN-MANAGER WIRD INITIALISIERT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	String sql001 = "INSERT INTO agent_manager_runs (manager_id,run_creation_code,start_time) VALUES (" + MANAGERID
		+ ",'" + run_creation_code + "','" + formatdb.format(MANAGER_START_PROCESS_TIME) + "');";

	db.insertToDatabase(sql001, "InitalImportManager");

	String deliver_path = "";

	boolean isLinux = false;

	String log_path = "";

	String opSystem = System.getProperty("os.name").toLowerCase();

	String system_pfadTrenner = "";

	String webspace = "";

	if (opSystem.indexOf("win") >= 0) {
	    isLinux = false;
	    system_pfadTrenner = System.getProperty("file.separator") + System.getProperty("file.separator");
	}

	if (opSystem.indexOf("linux") >= 0) {
	    isLinux = true;
	    system_pfadTrenner = System.getProperty("file.separator") + System.getProperty("file.separator");
	}

	if (isLinux == false) {

	    db.readFromDatabase(sqlDeliverPathWin, "sqlDeliverPath");

	    while (db.rs.next())

	    {
		deliver_path = db.rs.getString(1);
		log_path = db.rs.getString(2);
		webspace = db.rs.getString(3);

	    }
	} else {

	    db.readFromDatabase(sqlDeliverPathLinux, "sqlDeliverPath");

	    while (db.rs.next())

	    {
		deliver_path = db.rs.getString(1);
		log_path = db.rs.getString(2);
		webspace = db.rs.getString(3);

	    }
	}

	// Main-Processing

	XMLSplitter splitter = new XMLSplitter(isLinux, opSystem, system_pfadTrenner, deliver_path);

	DateiNormalisierung normalisieren = new DateiNormalisierung(isLinux, opSystem, system_pfadTrenner,
		deliver_path);

	agent_lieferant_stammdaten agent_lieferanten_import = new agent_lieferant_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_artikel_stammdaten agent_artikel_import = new agent_artikel_stammdaten(MANAGERID, run_creation_code,
		isLinux, opSystem, system_pfadTrenner);

	agent_warengruppen_stammdaten agent_warengruppen_import = new agent_warengruppen_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_bestellung_stammdaten agent_bestellungen_import = new agent_bestellung_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_auftrag_stammdaten agent_auftrag_import = new agent_auftrag_stammdaten(MANAGERID, run_creation_code,
		isLinux, opSystem, system_pfadTrenner);

	agent_lieferschein_stammdaten agent_lieferschein_import = new agent_lieferschein_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_warenausgang_stammdaten agent_verkauf_import = new agent_warenausgang_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_wareneingang_stammdaten agent_eingang_import = new agent_wareneingang_stammdaten(MANAGERID,
		run_creation_code, isLinux, opSystem, system_pfadTrenner);

	agent_datenbastand_analyse datenanalyse = new agent_datenbastand_analyse(system_pfadTrenner, webspace);

	MANAGER_DEBUG_COLLECTOR.debugStatus = true;

	MANAGER_START_PROCESS_TIME = new Date();

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[AGENTEN-MANAGER WIRD GESTARTET] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	MANAGER_START_PROCESS_TIME = new Date();

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET XML-SPLITTER] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	splitter.start();

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET NORMALISIERUNG] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	normalisieren.start();

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET WARENGRUPPEN IMPORT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_warengruppen_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_warengruppen_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET LIEFERANTEN IMPORT]  -  " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_lieferanten_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_lieferanten_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET ARTIKEL IMPORT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_artikel_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_artikel_import.AGENT_DEBUG_INFO.debugString.toString());

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET BESTELLUNG IMPORT]  - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_bestellungen_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_bestellungen_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET WARENEINGANG IMPORT]  - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_eingang_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_eingang_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET AUFTRAG IMPORT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_auftrag_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_auftrag_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET LIEFERSCHEIN IMPORT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_lieferschein_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_lieferschein_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET VERKAUF IMPORT] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	agent_verkauf_import.start();

	MANAGER_DEBUG_COLLECTOR.addMassage(agent_verkauf_import.AGENT_DEBUG_INFO.debugString.toString() + "");

	MANAGER_START_PROCESS_TIME = new Date();

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER SCHREIBT DIE LOG DATEI] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	// Post-Processing

	int maxLength = 1073741000; // Maximale Laenge an Zeichen fuer
	// LONGTEXT MAYSQL

	String logDB = "";

	// Hier findet eine Laengenkorrektur statt. Falls die Loginformation zu lang
	// ist, wird die Info abgeschnitten,
	// damit es nicht zu einem Datenbankfehler kommt.

	if (MANAGER_DEBUG_COLLECTOR.debugString.length() > maxLength) {

	    logDB = MANAGER_DEBUG_COLLECTOR.debugString.substring(0, maxLength - 1)
		    + "\n\n Log-Infos wurden geschnitten.";
	}
	if (MANAGER_DEBUG_COLLECTOR.debugString.length() <= maxLength) {

	    logDB = MANAGER_DEBUG_COLLECTOR.debugString.toString();
	}

	// Die Log-informationen werden auf dem Datentraeger gespeichert

	String logfileName = log_path + system_pfadTrenner + "Initialisierung_" + formatlogfile.format(new Date()) + "_"
		+ run_creation_code + "_log.txt";

	File log = new File(logfileName);

	try (FileOutputStream oS = new FileOutputStream(log)) {
	    oS.write(MANAGER_DEBUG_COLLECTOR.debugString.toString().getBytes());
	} catch (IOException e) {
	    e.printStackTrace();
	}

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[MANAGER STARTET DATEN-ANALYSE] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	datenanalyse.start(isLinux);

	MANAGER_START_PROCESS_TIME = new Date();
	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"[AGENTEN-MANAGER WIRD BEENDET] - " + format_log_file_human.format(MANAGER_START_PROCESS_TIME));

	MANAGER_DEBUG_COLLECTOR.addMassage(
		"***************************************************************************************************************");

	Date MANAGER_END_PROCESS_TIME = new Date();

	db.updateOnDatabase("UPDATE agent_manager_runs set general_log='" + logDB + "', end_time='"
		+ formatdb.format(MANAGER_END_PROCESS_TIME) + "' where run_creation_code='" + run_creation_code + "';");

    }

}
