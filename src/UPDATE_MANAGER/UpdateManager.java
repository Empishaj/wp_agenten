package UPDATE_MANAGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.comparator.NameFileComparator;
import org.xml.sax.SAXException;

public class UpdateManager implements java.io.Serializable {

	private static final long serialVersionUID = -6765704880568827281L;

	static String sqlDeliverPathWin = "SELECT a.java_parameter_value as aa, b.java_parameter_value as bb, c.java_parameter_value as cc, d.java_parameter_value as dd, e.java_parameter_value as ee, f.java_parameter_value as ff , g.java_parameter_value as gg FROM system_parameter as a, system_parameter as b, system_parameter as c, system_parameter as d, system_parameter as e,   system_parameter as f, system_parameter as g WHERE a.java_parameter_name='system_paths_deliver_win' and b.java_parameter_name='system_paths_duplikate_win' and c.java_parameter_name='system_paths_stack_win' and d.java_parameter_name='system_paths_logs_win' and e.java_parameter_name='system_max_load_files_from_manger' and f.java_parameter_name='system_paths_failtoprocess_win' and g.java_parameter_name='system_paths_work_win';";

	static String sqlDeliverPathLinux = "SELECT a.java_parameter_value as aa,b.java_parameter_value as bb ,c.java_parameter_value as cc, d.java_parameter_value as dd, e.java_parameter_value as ee, f.java_parameter_value as ff, g.java_parameter_value as gg  FROM system_parameter as a, system_parameter as b, system_parameter as c, system_parameter as d,  system_parameter as e, system_parameter as f, system_parameter as g WHERE a.java_parameter_name='system_paths_deliver_linux' and b.java_parameter_name='system_paths_duplikate_linux' and c.java_parameter_name='system_paths_stack_linux' and d.java_parameter_name='system_paths_logs_linux' and e.java_parameter_name='system_max_load_files_from_manger' and f.java_parameter_name='system_paths_failtoprocess_linux' and g.java_parameter_name='system_paths_work_linux';";

	static DatenBankVerbindung db = new DatenBankVerbindung();
	
	static SimpleDateFormat formatdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static SimpleDateFormat formatlogfile = new SimpleDateFormat("yyyyMMddHHmmss");
	
	static RandomCode random = new RandomCode();
	static Debuger dubegInfo = new Debuger();
	static String run_creation_code = random.newRandomCode();

	static int MANAGERID = 1;
	static int WP_MANDANT_DBID = 111;
	static int KA_MANDANT_DBID = 222;

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, SAXException,
			ParserConfigurationException, ParseException {

		System.out.println("START: UPDATE MANAGER");

		RandomCode file_creation_code_generator = new RandomCode();

		int max_load_file_from_manager = 0;

		String fileid = "";

		String duplikat_path = "";

		String deliver_path = "";

		String stack_path = "";

		String log_path = "";

		String fail_to_process = "";

		String work_path = "";

		boolean isLinux = false;

		String opSystem = System.getProperty("os.name").toLowerCase();

		String system_pfadTrenner = "";

		if (opSystem.indexOf("win") >= 0) {

			isLinux = false;
			system_pfadTrenner = System.getProperty("file.separator") + System.getProperty("file.separator");

		}

		if (opSystem.indexOf("linux") >= 0) {

			isLinux = true;
			system_pfadTrenner = System.getProperty("file.separator") + System.getProperty("file.separator");

		}

		if (isLinux == false)

		{

			db.readFromDatabase(sqlDeliverPathWin, "sqlDeliverPath");

			while (db.rs.next())

			{
				deliver_path = db.rs.getString(1);
				duplikat_path = db.rs.getString(2);
				stack_path = db.rs.getString(3);
				log_path = db.rs.getString(4);
				max_load_file_from_manager = db.rs.getInt(5);
				fail_to_process = db.rs.getString(6);
				work_path = db.rs.getString(7);

			}
		} else {

			db.readFromDatabase(sqlDeliverPathLinux, "sqlDeliverPath");

			while (db.rs.next())

			{
				deliver_path = db.rs.getString(1);
				duplikat_path = db.rs.getString(2);
				stack_path = db.rs.getString(3);
				log_path = db.rs.getString(4);
				max_load_file_from_manager = db.rs.getInt(5);
				fail_to_process = db.rs.getString(6);
				work_path = db.rs.getString(7);

			}
		}

		db.updateOnDatabase("SET SQL_SAFE_UPDATES = 0;");

		// -----------------------------------

		/*
		 * Pre-Prozessing
		 */
		// DuplicateFileChecker checkdeuplikate = new
		// DuplicateFileChecker(isLinux, opSystem, system_pfadTrenner,
		// deliver_path, duplikat_path, db);

		FailToProcessBackToDeliver xmlWorkToDeliver = new FailToProcessBackToDeliver(isLinux, opSystem,
				system_pfadTrenner, fail_to_process, work_path, deliver_path, db);

		XMLStackGenerator xmlGeneratorStack = new XMLStackGenerator(isLinux, opSystem, system_pfadTrenner, deliver_path,
				stack_path, duplikat_path, db);

		DateiNormalisierung normalisierung = new DateiNormalisierung(isLinux, opSystem, system_pfadTrenner,
				deliver_path, stack_path, duplikat_path, db);

		/*
		 * Main-Prozessing
		 */

		event_agent_warengruppe warengruppe_event = new event_agent_warengruppe(MANAGERID, run_creation_code, isLinux,
				opSystem, system_pfadTrenner, duplikat_path, db);

		event_agent_lieferant lieferant_event = new event_agent_lieferant(MANAGERID, run_creation_code, isLinux,
				opSystem, system_pfadTrenner, duplikat_path, db);

		event_agent_artikel artikel_events = new event_agent_artikel(MANAGERID, run_creation_code, isLinux, opSystem,
				system_pfadTrenner, duplikat_path, db);

		event_agent_bestellungen bestell_events = new event_agent_bestellungen(MANAGERID, run_creation_code, isLinux,
				opSystem, system_pfadTrenner, duplikat_path, db);

		event_agent_wareneingang wareneing_events = new event_agent_wareneingang(MANAGERID, run_creation_code, isLinux,
				opSystem, system_pfadTrenner, duplikat_path, stack_path, fail_to_process, db);

		event_agent_auftrag auftrag_events = new event_agent_auftrag(MANAGERID, run_creation_code, isLinux, opSystem,
				system_pfadTrenner, duplikat_path, db);

		event_agent_lieferschein lieferschein_events = new event_agent_lieferschein(MANAGERID, run_creation_code,
				isLinux, opSystem, system_pfadTrenner, duplikat_path, db);

		event_agent_barverkauf barverkauf_event = new event_agent_barverkauf(MANAGERID, run_creation_code, isLinux,
				opSystem, system_pfadTrenner, duplikat_path, db);

		event_agent_rechnung rechnung_event = new event_agent_rechnung(MANAGERID, run_creation_code, isLinux, opSystem,
				system_pfadTrenner, duplikat_path, db);

		// -----------------------------------

		/*
		 * Pre-Prozessing
		 */

		dubegInfo.debug_string.append("START: PREE-PROCESSING am: " + formatdb.format(new Date()) + " \n");

		xmlWorkToDeliver.start();

		xmlGeneratorStack.start();

		normalisierung.start();

		// checkdeuplikate.start();

		// -----------------------------------

		/*
		 * Sortierungs-Filter
		 */

		dubegInfo.debug_string.append("START: SORTIERE DATEIEN am: " + formatdb.format(new Date()) + " \n");

		File deliver_dir = new File(deliver_path);

		File[] files_all = deliver_dir.listFiles();

		if (deliver_dir.exists() == true) {

			FilenameFilter filter_lieferant = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("lieferant_update_import");
				}
			};

			FilenameFilter filter_warengruppe = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("warengruppe_update_import");
				}
			};

			FilenameFilter filter_artikel = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("artikel_update_import");
				}
			};

			FilenameFilter filter_auftrag = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("auftrag_update_import");
				}
			};

			FilenameFilter filter_lieferschein = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("lieferschein_update_import");
				}
			};

			FilenameFilter filter_bestellung = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("bestellung_update_import");
				}
			};

			FilenameFilter filter_wareneingang = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("wareneingang_update_import");
				}
			};

			FilenameFilter filter_barverkauf = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("barverkauf_update_import");
				}
			};

			FilenameFilter filter_rechnung = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("rechnung_update_import");
				}
			};

			/*
			 * Sortierungs-Racks
			 */

			File[] deliverd_lieferant_files = deliver_dir.listFiles(filter_lieferant);
			File[] deliverd_warengruppe_files = deliver_dir.listFiles(filter_warengruppe);
			File[] deliverd_artikel_files = deliver_dir.listFiles(filter_artikel);
			File[] deliverd_auftrag_files = deliver_dir.listFiles(filter_auftrag);
			File[] deliverd_lieferschein_files = deliver_dir.listFiles(filter_lieferschein);
			File[] deliverd_bestellung_files = deliver_dir.listFiles(filter_bestellung);
			File[] deliverd_wareneingang_files = deliver_dir.listFiles(filter_wareneingang);
			File[] deliverd_barverkauf_files = deliver_dir.listFiles(filter_barverkauf);
			File[] deliverd_rechnung_files = deliver_dir.listFiles(filter_rechnung);

			/*
			 * Sortierung nach Dateinamen
			 */

			Arrays.sort(deliverd_artikel_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			Arrays.sort(deliverd_auftrag_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			Arrays.sort(deliverd_lieferschein_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			Arrays.sort(deliverd_bestellung_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			Arrays.sort(deliverd_wareneingang_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			Arrays.sort(deliverd_rechnung_files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

			// ----------------------------------------------------------------------------------------

			/*
			 * Main-Processing
			 */

			dubegInfo.debug_string.append("START: DATEI NORMALISIERUNG am: " + formatdb.format(new Date()) + " \n");

			dubegInfo.debug_string.append(normalisierung.DEBBUG.debug_string.toString());

			File work_file = null;

			dubegInfo.debug_string.append("START: VERARBEITUNG LIEFERANT am: " + formatdb.format(new Date()) + " \n");

			int maxLimitOnRun = 500;

			int runLength = maxLimitOnRun;

			if (files_all.length > maxLimitOnRun) {
				runLength = maxLimitOnRun;
			}
			if (files_all.length <= maxLimitOnRun) {
				runLength = files_all.length;
			}

			for (int j = 0; j <= runLength; j++) {

				for (int i = 0; i < deliverd_lieferant_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_lieferant_files[i].isFile()) {

						work_file = deliverd_lieferant_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						lieferant_event.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(lieferant_event.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------2

				dubegInfo.debug_string
						.append("START: VERARBEITUNG WARENGRUPPE am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_warengruppe_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_warengruppe_files[i].isFile()) {

						work_file = deliverd_warengruppe_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						warengruppe_event.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(warengruppe_event.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");

					}

				}

				// ----------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------3

				dubegInfo.debug_string.append("START: VERARBEITUNG ARTIKEL am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_artikel_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_artikel_files[i].isFile()) {

						work_file = deliverd_artikel_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						artikel_events.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(artikel_events.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------4

				dubegInfo.debug_string
						.append("START: VERARBEITUNG BESTELLUNG am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_bestellung_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_bestellung_files[i].isFile()) {

						work_file = deliverd_bestellung_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						bestell_events.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(bestell_events.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------
				// ----------------------------------------------------------------------------------------5

				dubegInfo.debug_string
						.append("START: VERARBEITUNG WARENEINGANG am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_wareneingang_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_wareneingang_files[i].isFile()) {

						work_file = deliverd_wareneingang_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						wareneing_events.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(wareneing_events.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");

					}

				}

				// ----------------------------------------------------------------------------------------6

				dubegInfo.debug_string.append("START: VERARBEITUNG AUFTRAG am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_auftrag_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_auftrag_files[i].isFile()) {

						work_file = deliverd_auftrag_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						auftrag_events.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);

						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(auftrag_events.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------
				// ----------------------------------------------------------------------------------------7

				dubegInfo.debug_string
						.append("START: VERARBEITUNG LIEFERSCHEIN am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_lieferschein_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_lieferschein_files[i].isFile()) {

						work_file = deliverd_lieferschein_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						lieferschein_events.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);
						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(lieferschein_events.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------8

				dubegInfo.debug_string
						.append("START: VERARBEITUNG BARVERKAUF am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_barverkauf_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_barverkauf_files[i].isFile()) {

						work_file = deliverd_barverkauf_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						barverkauf_event.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);
						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(barverkauf_event.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

				// ----------------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------------9

				dubegInfo.debug_string
						.append("START: VERARBEITUNG RECHNUNG am: " + formatdb.format(new Date()) + " \n");

				for (int i = 0; i < deliverd_rechnung_files.length; i++) {

					fileid = file_creation_code_generator.newRandomCode();

					if (deliverd_rechnung_files[i].isFile()) {

						work_file = deliverd_rechnung_files[i].getAbsoluteFile();

						int auswahl_mandant_id = 0;
						String auswahl_mandant_name = "";

						if (work_file.getName().contains("_wp")) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}
						if (work_file.getName().contains("_ka")) {
							auswahl_mandant_id = KA_MANDANT_DBID;
							auswahl_mandant_name = "_ka_";

						}

						if (work_file.getName().contains("_wp") == false
								&& work_file.getName().contains("_ka") == false) {
							auswahl_mandant_id = WP_MANDANT_DBID;
							auswahl_mandant_name = "_wp_";

						}

						rechnung_event.start(work_file, (i + 1), fileid, auswahl_mandant_id, auswahl_mandant_name);
						dubegInfo.debug_string.append(
								"#######################################LOG-START##################################################\n");
						dubegInfo.debug_string.append(rechnung_event.DEBUGG_LOGGER.debug_string.toString());
						dubegInfo.debug_string.append(
								"#######################################LOG-ENDE###################################################\n\n");
					}

				}

			}

			/*
			 * Post-Processing
			 */

			// Laengen-Schutz fur Log-Informationen

			dubegInfo.debug_string.append("START: POST-PROCESSING am: " + formatdb.format(new Date()) + " \n");

			int maxLength = 1073741000; // Maximale Laenge an Zeichen fuer
			// LONGTEXT MYSQL, max 4 GB fuer
			// Log-Infos je Run

			String logDB = "";

			if (dubegInfo.debug_string.length() > maxLength) {

				logDB = dubegInfo.debug_string.substring(0, (maxLength - 100)) + "\n\n Log-Infos wurden geschnitten.";
			}
			if (dubegInfo.debug_string.length() <= maxLength) {

				logDB = dubegInfo.debug_string.toString();
			}

			// ----------------------------------------------------------------------------------------
			Date MANAGER_END_PROCESS_TIME = new Date();
			// ----------------------------------------------------------------------------------------

			db.updateOnDatabase("UPDATE agent_manager_runs set general_log='" + logDB + "', end_time='"
					+ formatdb.format(MANAGER_END_PROCESS_TIME) + "' where run_creation_code='" + run_creation_code
					+ "';");

			String logfileName = log_path + system_pfadTrenner + formatlogfile.format(new Date()) + "_"
					+ run_creation_code + "_log.txt";

			File log = new File(logfileName);

			// Log-File Generierung

			try (FileOutputStream oS = new FileOutputStream(log)) {
				oS.write(dubegInfo.debug_string.toString().getBytes());
			} catch (IOException e) {

				e.printStackTrace();
			}

		} else {
			dubegInfo.debug_string.append("FATAL-ERROR: Deliver-Ordner ist nicht vorhanden!");
		}

	}

}
