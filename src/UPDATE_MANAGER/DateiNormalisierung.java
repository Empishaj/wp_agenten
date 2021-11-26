package UPDATE_MANAGER;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.comparator.NameFileComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DateiNormalisierung implements java.io.Serializable {

	private static final long serialVersionUID = -8113116947822618511L;

	SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	private Vector<Agent_List_Objekt> agent_list = new Vector<Agent_List_Objekt>();

	String sqlInitAgent = "SELECT dbid, agentname_kurz, file_name_pattern, activity  FROM agent where manager_id=1 order by manager_start_position;";

	boolean isLinux = false;

	String system_path_delimiter = "";

	String oparating_system = "";

	String deliver_path = "";

	String stack_path = "";

	String duplikate_path = "";

	DatenBankVerbindung db;

	DecimalFormat df = new DecimalFormat("00000");

	String mandant_name = "_xx_";

	String INSERT_ORDER = "000";
	String UPDATE_ORDER = "111";

	Debuger DEBBUG = new Debuger();

	public DateiNormalisierung(boolean _isLinux, String _opsystem, String _system_path_delimiter, String _deliverpath,
			String _stackpath, String _duplikate, DatenBankVerbindung _db) {

		isLinux = _isLinux;
		oparating_system = _opsystem;
		system_path_delimiter = _system_path_delimiter;
		deliver_path = _deliverpath;
		stack_path = _stackpath;
		duplikate_path = _duplikate;
		db = _db;

	}

	public void start() throws IOException, SQLException, ParseException, SAXException, ParserConfigurationException {

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

		/////// ---------------------------------------------

		File duplikate_dir = new File(duplikate_path);

		if (duplikate_dir.exists() == true) {

			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("duplikat");
				}
			};

			File[] dup_files = duplikate_dir.listFiles(filter);

			DEBBUG.addMassage("Duplikate: [" + dup_files.length + "]");
			DEBBUG.addMassage("-----------------------------------------------");

			File xml_file_dup = null;

			if (dup_files.length > 0) {

				// for (int i = 0; i < dup_files.length; i++) {

				// xml_file_dup = dup_files[i].getAbsoluteFile();

				// xml_file_dup.delete();

				// }

			}

		}

		/////// ---------------------------------------------
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

				DEBBUG.addMassage(agent_list.get(o).getAgent_name() + ": [ " + deliverd_files.length + " ] - "
						+ agent_list.get(o).getAgent_datei_filter());

				File deliverd_xml_file = null;

				if (deliverd_files.length > 0) {

					for (int i = 0; i < deliverd_files.length; i++) {

						int index = i + 1;

						deliverd_xml_file = deliverd_files[i].getAbsoluteFile();

						if (deliverd_xml_file.getName().contains("_wp")) {
							this.mandant_name = "_wp_";

						}

						if (deliverd_xml_file.getName().contains("_ka")) {

							this.mandant_name = "_ka_";
						}

						Path path = Paths.get(deliverd_xml_file.getAbsolutePath());

						BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

						long time = attr.lastModifiedTime().to(TimeUnit.MILLISECONDS);

						String file_creation_time_alt = format1.format(new Date(time));
						String file_creation_time_neu = "20000101010101.010";

						String timeStamp = "";

						String newSystemNameBeforeDeliver = file_creation_time_alt + "_"
								+ agent_list.get(o).getAgent_datei_filter() + this.mandant_name + df.format(index)
								+ ".xml";

						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						Document doc = dBuilder.parse(deliverd_xml_file);

						doc.getDocumentElement().normalize();

						// ----------------------------------------------------------

						if (deliverd_xml_file.getName().contains("rechnung_update_import")) {

							if (deliverd_xml_file.getName().contains("20000101010101.010")) {

								timeStamp = file_creation_time_neu;
							} else {

								timeStamp = file_creation_time_alt;
							}

							Element node_kopf_wurzel_element = (Element) doc.getElementsByTagName("KhList").item(0);

							String filetype_operation_content = "";

							boolean erledigt = false;

							String order = "xxx";

							try {
								filetype_operation_content = new String(
										node_kopf_wurzel_element.getAttribute("Operation"));

							} catch (NullPointerException ecxc) {
								filetype_operation_content = "?";
							}

							if (INSERT_ORDER.equals(filetype_operation_content) == true
									|| INSERT_ORDER == filetype_operation_content)

							{
								order = INSERT_ORDER;

							} else {

								order = UPDATE_ORDER;

								try {
									erledigt = new Boolean(node_kopf_wurzel_element.getElementsByTagName("Erledigt")
											.item(0).getTextContent().trim());

									if (erledigt == true) {
										order = INSERT_ORDER;

									} else {

										order = INSERT_ORDER;

									}

								} catch (NullPointerException ecxc) {

									order = INSERT_ORDER;
									erledigt = false;

								}

							}

							newSystemNameBeforeDeliver = timeStamp + "_" + order + this.mandant_name
									+ agent_list.get(o).getAgent_datei_filter() + "_" + df.format(index) + ".xml";

						}

						if (deliverd_xml_file.getName().contains("auftrag_update_import")) {

							if (deliverd_xml_file.getName().contains("20000101010101.010")) {

								timeStamp = file_creation_time_neu;
							} else {

								timeStamp = file_creation_time_alt;
							}

							Element node_kopf_wurzel_element = (Element) doc.getElementsByTagName("AhList").item(0);

							String filetype_operation_content = "";

							boolean erledigt = false;

							String order = "xxx";

							try {
								filetype_operation_content = new String(
										node_kopf_wurzel_element.getAttribute("Operation"));

							} catch (NullPointerException ecxc) {
								filetype_operation_content = "?";
							}

							if (INSERT_ORDER.equals(filetype_operation_content)
									|| INSERT_ORDER == filetype_operation_content)

							{
								order = INSERT_ORDER;

							} else {

								order = UPDATE_ORDER;

								order = INSERT_ORDER;

								try {
									erledigt = new Boolean(node_kopf_wurzel_element.getElementsByTagName("Erledigt")
											.item(0).getTextContent().trim());

									if (erledigt == true) {
										order = INSERT_ORDER;

									} else {

										order = UPDATE_ORDER;

									}

								} catch (NullPointerException ecxc) {

									order = INSERT_ORDER;
									erledigt = false;

								}

							}

							newSystemNameBeforeDeliver = timeStamp + "_" + order + "_"
									+ agent_list.get(o).getAgent_datei_filter() + this.mandant_name + df.format(index)
									+ ".xml";

						}

						if (deliverd_xml_file.getName().contains("bestellung_update_import")) {

							if (deliverd_xml_file.getName().contains("20000101010101.010")) {

								timeStamp = file_creation_time_neu;
							} else {

								timeStamp = file_creation_time_alt;
							}

							Element node_kopf_wurzel_element = (Element) doc.getElementsByTagName("BhList").item(0);

							String filetype_operation_content = "";

							boolean erledigt = false;

							String order = "xxx";

							try {
								filetype_operation_content = new String(
										node_kopf_wurzel_element.getAttribute("Operation"));

							} catch (NullPointerException ecxc) {
								filetype_operation_content = "?";
							}

							if (INSERT_ORDER.equals(filetype_operation_content)
									|| INSERT_ORDER == filetype_operation_content)

							{
								order = INSERT_ORDER;

							} else {

								order = UPDATE_ORDER;

								try {
									erledigt = new Boolean(node_kopf_wurzel_element.getElementsByTagName("BhErledigt")
											.item(0).getTextContent().trim());

									if (erledigt == true) {

										order = UPDATE_ORDER;
									} else {

										order = INSERT_ORDER;

									}

								} catch (NullPointerException ecxc) {

									order = INSERT_ORDER;
									erledigt = false;

								}

							}

							newSystemNameBeforeDeliver = timeStamp + "_" + order + "_"
									+ agent_list.get(o).getAgent_datei_filter() + this.mandant_name + df.format(index)
									+ ".xml";

						}

						if (deliverd_xml_file.getName().contains("lieferschein_update_import")) {

							if (deliverd_xml_file.getName().contains("20000101010101.010")) {

								timeStamp = file_creation_time_neu;
							} else {

								timeStamp = file_creation_time_alt;
							}

							Element node_kopf_wurzel_element = (Element) doc.getElementsByTagName("LhList").item(0);

							String filetype_operation_content = "";

							String order = "xxx";

							try {
								filetype_operation_content = new String(
										node_kopf_wurzel_element.getAttribute("Operation"));

							} catch (NullPointerException ecxc) {
								filetype_operation_content = "?";
							}

							if (INSERT_ORDER.equals(filetype_operation_content)
									|| INSERT_ORDER == filetype_operation_content)

							{
								order = INSERT_ORDER;

							} else {

								order = UPDATE_ORDER;

							}
							newSystemNameBeforeDeliver = timeStamp + "_" + order + "_"
									+ agent_list.get(o).getAgent_datei_filter() + this.mandant_name + df.format(index)
									+ ".xml";

						}

						if (deliverd_xml_file.getName().contains("wareneingang_update_import")) {

							if (deliverd_xml_file.getName().contains("20000101010101.010")) {

								timeStamp = file_creation_time_neu;
							} else {

								timeStamp = file_creation_time_alt;
							}

							newSystemNameBeforeDeliver = timeStamp + "_" + agent_list.get(o).getAgent_datei_filter()
									+ this.mandant_name + df.format(index) + ".xml";

						}

						File xml_file_normalisiert = new File(
								deliver_path + system_path_delimiter + newSystemNameBeforeDeliver);

						deliverd_xml_file.renameTo(xml_file_normalisiert);

					}

				}

			}

		}

		/////// ---------------------------------------------
		File stack_dir = new File(stack_path);

		if (stack_dir.exists() == true) {

			File[] stack_files = stack_dir.listFiles();

			Arrays.sort(stack_files, NameFileComparator.NAME_INSENSITIVE_REVERSE);

			File stack_xml_file = null;

			if (stack_files.length > 0) {

				for (int i = 0; i < stack_files.length; i++) {

					int index = i + 1;

					stack_xml_file = stack_files[i].getAbsoluteFile();

					if (stack_xml_file.getName().contains("_wp")) {
						this.mandant_name = "_wp_";

					}

					if (stack_xml_file.getName().contains("_ka")) {

						this.mandant_name = "_ka_";
					}

					Path path = Paths.get(stack_xml_file.getAbsolutePath());

					BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

					Date vergangenHeit = new Date(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS));

					DEBBUG.addMassage("-----------------------");
					DEBBUG.addMassage("alt: " + vergangenHeit.toString());

					SimpleDateFormat format3 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

					String DatumVergangenheit2 = "20000101010101.010";

					Date nu = format3.parse(DatumVergangenheit2);

					DEBBUG.addMassage("neu: " + nu.toString());
					DEBBUG.addMassage("-----------------------");

					String file_creation_time_stack_insert = format3.format(nu);

					String back_from_stack_file = file_creation_time_stack_insert + "_" + INSERT_ORDER + "_"
							+ df.format(index) + this.mandant_name + stack_xml_file.getName();

					File xml_file_return = new File(deliver_path + system_path_delimiter + back_from_stack_file);

					stack_xml_file.renameTo(xml_file_return);

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
