package INITAL_MANAGER;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DateiNormalisierung implements java.io.Serializable {

    private static final long serialVersionUID = -8113116947822618511L;

    SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

    private Vector<Agent_List_Objekt> agent_list = new Vector<Agent_List_Objekt>();

    DatenBankVerbindung db = new DatenBankVerbindung();

    String sqlInitAgent = "SELECT dbid, agentname_kurz, file_name_pattern, activity  FROM agent where manager_id=2 and activity=1;";

    boolean isLinux = false;

    String system_path_delimiter = "";

    String oparating_system = "";

    String deliver_path = "";

    public DateiNormalisierung(boolean _isLinux, String _opsystem, String _system_path_delimiter, String _deliverpath) {

	isLinux = _isLinux;

	oparating_system = _opsystem;

	system_path_delimiter = _system_path_delimiter;

	deliver_path = _deliverpath;

    }

    public void start() throws Exception {

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

	    String agent_file_pattern = agent_list.get(o).getAgent_datei_filter();

	    File deliver_dir = new File(deliver_path);

	    if (deliver_dir.exists() == true) {

		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String fileName) {
			return fileName.contains(agent_file_pattern);
		    }
		};

		File[] deliverd_files = deliver_dir.listFiles(filter);

		File deliverd_xml_file = null;

		if (deliverd_files.length > 0) {

		    for (int i = 0; i < deliverd_files.length; i++) {

			String file_creation_time = "";

			deliverd_xml_file = deliverd_files[i].getAbsoluteFile();
			
			Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

			BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

			file_creation_time = format1
				.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

			String newSystemNameBeforeDeliver = "";

			if (deliverd_xml_file.getName().contains("_wp")) {
			    newSystemNameBeforeDeliver = file_creation_time + "_" + agent_list.get(o).getAgent_id()
				    + "_" + agent_list.get(o).getAgent_name() + "_" + (i + 1)
				    + agent_list.get(o).getAgent_datei_filter() + "_wp_" + ".xml";

			}

			if (deliverd_xml_file.getName().contains("_ka")) {
			    newSystemNameBeforeDeliver = file_creation_time + "_" + agent_list.get(o).getAgent_id()
				    + "_" + agent_list.get(o).getAgent_name() + "_" + (i + 1)
				    + agent_list.get(o).getAgent_datei_filter() + "_ka_" + ".xml";
			}

			File xml_file_normalisiert = new File(
				deliver_path + system_path_delimiter + newSystemNameBeforeDeliver);

			deliverd_xml_file.renameTo(xml_file_normalisiert);

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
