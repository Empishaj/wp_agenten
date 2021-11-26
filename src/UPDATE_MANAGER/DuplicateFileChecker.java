package UPDATE_MANAGER;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class DuplicateFileChecker implements java.io.Serializable {

    private static final long serialVersionUID = -8113116947822618511L;

    SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

    private Vector<Agent_List_Objekt> agent_list = new Vector<Agent_List_Objekt>();

    String sqlInitAgent = "SELECT dbid, agentname_kurz, file_name_pattern, activity  FROM agent where manager_id=1 and activity=1;";

    boolean isLinux = false;

    String system_path_delimiter = "";

    String oparating_system = "";

    String deliver_path = "";

    String duplikate_path = "";

    DatenBankVerbindung db;

    DecimalFormat df = new DecimalFormat("00000");

    public DuplicateFileChecker(boolean _isLinux, String _opsystem, String _system_path_delimiter, String _deliverpath,
	    String _duplikate_path, DatenBankVerbindung _db) {

	isLinux = _isLinux;

	oparating_system = _opsystem;

	system_path_delimiter = _system_path_delimiter;

	deliver_path = _deliverpath;

	duplikate_path = _duplikate_path;

	db = _db;

    }

    public void start() throws IOException, SQLException {

	System.out.println("------------------------ CHECK DUPPLIKATE ------------------------------");

	agent_list.clear();

	db.readFromDatabase(sqlInitAgent, "DateiNormalisierung > start()");

	try {
	    while (db.rs.next()) {

		agent_list.add(new Agent_List_Objekt(db.rs.getInt(1), db.rs.getString(2), db.rs.getString(3),
			db.rs.getBoolean(4), isLinux));
	    }
	} catch (SQLException e) {

	    e.printStackTrace();
	}

	for (int o = 0; o < agent_list.size(); o++) {

	    String patternfile = agent_list.get(o).getAgent_datei_filter();

	    File deliver_dir = new File(deliver_path);

	    if (deliver_dir.exists() == true) {

		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String fileName) {
			return fileName.contains(patternfile);
		    }
		};

		File[] deliverd_files = deliver_dir.listFiles(filter);

		File deliverd_xml_file = null;

		if (deliverd_files.length > 0) {

		    for (int i = 0; i < deliverd_files.length; i++) {

			int index = i + 1;

			boolean file_existenz = false;

			deliverd_xml_file = deliverd_files[i].getAbsoluteFile();

			FileInputStream fis = new FileInputStream(deliverd_xml_file);
			String md5_filehash = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			fis.close();

			String sqlCheckExistenzOfInsert = "SELECT EXISTS (SELECT * FROM work_agent_files WHERE filehash='"
				+ md5_filehash + "')";

			db.readFromDatabase(sqlCheckExistenzOfInsert, "sqlCheckExistenzOfInsert ");

			while (db.rs.next())

			{
			    file_existenz = db.rs.getBoolean(1);
			}

			if (file_existenz == true) {

			    deliverd_xml_file = deliverd_files[i].getAbsoluteFile();
			    
			    Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

			    BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

			    String file_creation_time = format1
				    .format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

			    String newSystemNameBeforeDeliver = "dfc_duplikat_" + file_creation_time + "_"
				    + agent_list.get(o).getAgent_id() + "_" + agent_list.get(o).getAgent_name() + "_"
				    + df.format(index) + "_" + agent_list.get(o).getAgent_datei_filter() + ".xml";

			    File xml_file_duplikate = new File(
				    duplikate_path + system_path_delimiter + newSystemNameBeforeDeliver);

			    deliverd_xml_file.renameTo(xml_file_duplikate);
			}

		    }

		}

	    }

	}

    }

    public Vector<Agent_List_Objekt> getAgent_list() {
	return agent_list;
    }

    public void setAgent_list(Vector<Agent_List_Objekt> agent_list) {
	this.agent_list = agent_list;
    }

}
