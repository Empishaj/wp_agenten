package INITAL_MANAGER;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLSplitter implements java.io.Serializable {

    private static final long serialVersionUID = 2819540377530566619L;

    boolean isLinux = false;

    String system_path_delimiter = "";

    String oparating_system = "";

    String deliver_path = "";

    SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

    public XMLSplitter(boolean _isLinux, String _opsystem, String _system_path_delimiter, String _deliverpath) {

	isLinux = _isLinux;

	oparating_system = _opsystem;

	system_path_delimiter = _system_path_delimiter;

	deliver_path = _deliverpath;

    }

    String auswahl_mandant = "";

    public void start() throws Exception {

	FilenameFilter filter2 = new FilenameFilter() {

	    public boolean accept(File directory, String fileName) {
		return fileName.contains("big_artikel");
	    }
	};

	File deliver_dir2 = new File(deliver_path);

	File[] deliverd_files2 = deliver_dir2.listFiles(filter2);

	File deliverd_xml_file = null;

	if (deliverd_files2.length > 0) {

	    for (int i = 0; i < deliverd_files2.length; i++) {

		deliverd_xml_file = deliverd_files2[i].getAbsoluteFile();

		Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

		if (deliverd_xml_file.getName().contains("_wp")) {
		    auswahl_mandant = "_wp_";
		}
		if (deliverd_xml_file.getName().contains("_ka")) {
		    auswahl_mandant = "_ka_";
		}

		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

		String file_creation_time = format1.format(new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS)));

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(deliverd_xml_file);
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodesArt = (NodeList) xpath.evaluate("//ArtList/Art", doc, XPathConstants.NODESET);

		int itemsPerFile = 5000;

		int fileNumber = 0;

		org.w3c.dom.Document currentDoc = dbf.newDocumentBuilder().newDocument();

		currentDoc.normalize();

		Node rootNode = currentDoc.createElement("ArtList");

		String file_name_new = file_creation_time + auswahl_mandant + "splitt_artikel_initial_import_"
			+ (fileNumber + 1) + ".xml";

		File currentFile = new File(deliver_path + system_path_delimiter + file_name_new);

		for (int k = 1; k <= nodesArt.getLength(); k++) {

		    Node artikel_position = currentDoc.importNode(nodesArt.item(k - 1), true);

		    NodeList artikelBestandsListe = artikel_position.getChildNodes();

		    for (int p = 1; p <= artikelBestandsListe.getLength(); p++) {

			Node bestand = artikelBestandsListe.item(p - 1);

			if (bestand != null) {
			    artikel_position.appendChild(bestand);
			}
		    }

		    rootNode.appendChild(artikel_position);

		    if (k % itemsPerFile == 0) {

			writeToFile(rootNode, currentFile);

			file_name_new = file_creation_time +  auswahl_mandant+"splitt_artikel_initial_import_" + (++fileNumber + 1)
				+ ".xml";
			rootNode = currentDoc.createElement("ArtList");
			currentFile = new File(deliver_path + system_path_delimiter + file_name_new);
		    }
		}
		writeToFile(rootNode, currentFile);
		deliverd_xml_file.delete();
	    }

	}
    }

    private static void writeToFile(Node node, File file) throws Exception {

	Transformer transformer = TransformerFactory.newInstance().newTransformer();

	transformer.setOutputProperty(OutputKeys.INDENT, "no");
	transformer.setOutputProperty(OutputKeys.ENCODING, "windows-1252");

	FileWriter mdk = new FileWriter(file);
	mdk.flush();

	transformer.transform(new DOMSource(node), new StreamResult(mdk));
    }

}
