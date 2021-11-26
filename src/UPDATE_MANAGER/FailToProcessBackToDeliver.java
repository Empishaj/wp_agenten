package UPDATE_MANAGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FailToProcessBackToDeliver implements java.io.Serializable {

	/*
	 * Abgebrochene Dateien werden innerhalb der Verarbeitung abgebrochen. Diese
	 * befinden sich im WORK Verzeichniss. Um diese wieder verarbeiten zu können
	 * müssen diese wieder in das DELIVER Verzeichnis verschoben werden. Darüber
	 * hinaus muss auch in der Datenbank die Datei anhand des filehash, geloescht
	 * werden.
	 */

	private static final long serialVersionUID = 2356765619307361355L;

	SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
	boolean isLinux = false;
	String path_failToProcess = "";
	String path_work = "";
	String path_deliver = "";
	String mandant_name = "_xx_";
	DatenBankVerbindung db;
	String file_creation_time = "";
	String system_path_delimiter = "";
	String oparating_system = "";

	public FailToProcessBackToDeliver(boolean _isLinux, String _opsystem, String _system_path_delimiter,
			String _path_failToProcess, String _path_work, String _path_deliver, DatenBankVerbindung _db) {

		this.isLinux = _isLinux;
		this.oparating_system = _opsystem;
		this.system_path_delimiter = _system_path_delimiter;
		this.path_failToProcess = _path_failToProcess;
		this.path_work = _path_work;
		this.path_deliver = _path_deliver;
		this.db = _db;

		System.out.println(this.path_work);
		System.out.println(this.path_deliver);
	}

	public void start() throws IOException {

		// Lade das Arbeitsverzeichnis
		File work_dir = new File(path_work);

		if (work_dir.exists() == true) {

			FilenameFilter filter = new FilenameFilter() {

				public boolean accept(File directory, String fileName) {
					return fileName.contains("_work_");
				}

			};

			// Hier werden nur die Dateien geladen die im Dateinamen -work- haben
			File[] work_files = work_dir.listFiles(filter);

			System.out.println("--|> ABGEBROCHENE-WORK-FILES: [ " + work_files.length + " ]");

			if (work_files.length > 0) {

				for (int i = 0; i < work_files.length; i++) {

					File work_xml_file = work_files[i].getAbsoluteFile();

					Path path = Paths.get(work_xml_file.getAbsolutePath());

					BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

					file_creation_time = format1
							.format(new Date((attr.lastModifiedTime().to(TimeUnit.MILLISECONDS) - 50000)));

					// ------------------

					if (work_xml_file.getName().contains("_wp_")) {
						this.mandant_name = "wp_";

					}

					if (work_xml_file.getName().contains("_ka_")) {

						this.mandant_name = "ka_";
					}

					String filetype = "";

					if (work_xml_file.getName().contains("wareneingang_update_import")) {

						filetype = "_wareneingang_update_import_";
					}

					if (work_xml_file.getName().contains("auftrag_update_import")) {

						filetype = "_auftrag_update_import_";
					}

					if (work_xml_file.getName().contains("bestellung_update_import")) {

						filetype = "_bestellung_update_import_";
					}

					if (work_xml_file.getName().contains("rechnung_update_import")) {

						filetype = "_rechnung_update_import_";
					}
					if (work_xml_file.getName().contains("warengruppe_update_import")) {

						filetype = "_warengruppe_update_import_";
					}

					if (work_xml_file.getName().contains("artikel_update_import")) {

						filetype = "_artikel_update_import_";
					}

					if (work_xml_file.getName().contains("lieferant_update_import")) {

						filetype = "_lieferant_update_import_";
					}
					if (work_xml_file.getName().contains("lieferschein_update_import")) {

						filetype = "_lieferschein_update_import_";
					}

					if (work_xml_file.getName().contains("barverkauf_update_import")) {

						filetype = "_barverkauf_update_import_";
					}

					// ------------------

					FileInputStream fis = new FileInputStream(work_xml_file);

					String file_hash_to_delete = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);

					fis.close();

					String dqlSQL = "DELETE FROM work_agent_files where filehash='" + file_hash_to_delete + "';";

					db.updateOnDatabase(dqlSQL);

					// ------------------

					String new_deliver_name = file_creation_time + "_work_return" + filetype + this.mandant_name + "_"
							+ (i + 1) + ".xml";

					File deliver_xml_file = new File(path_deliver + system_path_delimiter + new_deliver_name);

					work_xml_file.renameTo(deliver_xml_file);

				}
			}

		}

	}

}
