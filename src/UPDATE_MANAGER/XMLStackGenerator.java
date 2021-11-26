package UPDATE_MANAGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XMLStackGenerator implements java.io.Serializable {

	/*
	 * Dieser Agent hat die Aufgabe nicht vorhandene Insert-Dateien zu generierien.
	 * Dies geschieht explizit wenn eine Update-Datei kommt es jedoch dazu keine
	 * Insert Datei gibt.
	 * 
	 */

	private static final long serialVersionUID = 2536478361587281539L;

	SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	boolean isLinux = false;

	String system_path_delimiter = "";

	String oparating_system = "";

	String deliver_path = "";

	String stack_path = "";

	String duplikate_path = "";

	DatenBankVerbindung db;

	String file_creation_time = "";

	String mandant_name = "_xx_";

	public XMLStackGenerator(boolean _isLinux, String _opsystem, String _system_path_delimiter, String _deliverpath,
			String _stackpath, String _duplikate, DatenBankVerbindung _db) {

		isLinux = _isLinux;
		oparating_system = _opsystem;
		system_path_delimiter = _system_path_delimiter;
		deliver_path = _deliverpath;
		stack_path = _stackpath;
		duplikate_path = _duplikate;
		db = _db;

	}

	public void start() throws IOException, SQLException, SAXException, ParserConfigurationException {

		File stack_dir = new File(stack_path);

		if (stack_dir.exists() == true) {

			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return fileName.contains("stack");
				}
			};

			File[] stack_files = stack_dir.listFiles(filter);

			File stack_xml_file = null;

			System.out.println("--|> STACK-FILES: [ " + stack_files.length + " ]");

			if (stack_files.length > 0) {

				for (int i = 0; i < stack_files.length; i++) {

					stack_xml_file = stack_files[i].getAbsoluteFile();

					Path path = Paths.get(stack_xml_file.getAbsolutePath());

					if (stack_xml_file.getName().contains("_wp_")) {
						this.mandant_name = "_wp_";

					}

					if (stack_xml_file.getName().contains("_ka_")) {

						this.mandant_name = "_ka_";
					}

					// Kuenstlicher Zeitstempel zurueck in die Zukunft
					file_creation_time = "20000101010101.010";

					String insert_xml_file_name = "";
					File insert_xml_file = null;
					File real_update_xml_file = null;

					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(stack_xml_file);
					doc = dBuilder.parse(stack_xml_file);
					doc.getDocumentElement().normalize();

					StringBuilder datei_inhalt = new StringBuilder();

					if (stack_xml_file.getName().contains("bestellung_update_import"))

					{

						insert_xml_file_name = "_bestellung_update_import_" + file_creation_time + this.mandant_name
								+ (i + 1) + ".xml";

						System.out.println("-----> " + insert_xml_file_name);

						insert_xml_file = new File(deliver_path + system_path_delimiter + insert_xml_file_name);

						Element bestell_kopf_element = (Element) (doc.getElementsByTagName("Bh").item(0));

						String nummer_der_bestellung = "";

						String datum_der_bestellung = "";

						String guid_der_bestellung = "";

						try {

							nummer_der_bestellung = bestell_kopf_element.getElementsByTagName("Nr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							nummer_der_bestellung = "";

						}

						try {

							datum_der_bestellung = bestell_kopf_element.getElementsByTagName("Date").item(0)
									.getTextContent().trim();

						} catch (NullPointerException ecxc) {

							datum_der_bestellung = "09.09.9999";

						}

						try {

							guid_der_bestellung = bestell_kopf_element.getElementsByTagName("GUID").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid_der_bestellung = "";
						}

						datei_inhalt.setLength(0);

						datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
						datei_inhalt.append(
								"<BhList Operation=\"Insert\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

						datei_inhalt.append("<Bh>");
						datei_inhalt.append("<Nr>" + nummer_der_bestellung + "</Nr>");
						datei_inhalt.append("<Date>" + datum_der_bestellung + "</Date>");
						datei_inhalt.append("<GUID>" + guid_der_bestellung + "</GUID>");
						datei_inhalt.append("<BhErledigt>false</BhErledigt>");
						datei_inhalt.append("<BhStorno>false</BhStorno>");
						datei_inhalt.append("<generated>wplag</generated>");
						datei_inhalt.append("</Bh>");
						datei_inhalt.append("</BhList>");

						try (FileOutputStream oS = new FileOutputStream(insert_xml_file)) {
							oS.write(datei_inhalt.toString().getBytes());

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					if (stack_xml_file.getName().contains("lieferschein_update_import"))

					{
						insert_xml_file_name = "_lieferschein_update_import_" + file_creation_time + this.mandant_name
								+ (i + 1) + ".xml";

						System.out.println("-----> " + insert_xml_file_name);

						insert_xml_file = new File(deliver_path + system_path_delimiter + insert_xml_file_name);

						Element lieferschein_kopf_element = (Element) (doc.getElementsByTagName("Lh").item(0));

						String auftragsnummer = "";
						String lieferscheinnummer = "";
						String guid = "";
						String datum = "";
						String kundennummer = "";
						String rechungsnummer = "";

						try {

							auftragsnummer = lieferschein_kopf_element.getElementsByTagName("RefAuftrag").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							auftragsnummer = "";

						}

						try {

							lieferscheinnummer = lieferschein_kopf_element.getElementsByTagName("Nr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							lieferscheinnummer = "";

						}

						try {

							guid = lieferschein_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid = "";
						}

						try {

							datum = lieferschein_kopf_element.getElementsByTagName("Date").item(0).getTextContent()
									.trim();

						} catch (NullPointerException ecxc) {

							datum = "09.09.9999";
						}

						try {

							kundennummer = lieferschein_kopf_element.getElementsByTagName("KdNr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							kundennummer = "";
						}

						try {

							rechungsnummer = lieferschein_kopf_element.getElementsByTagName("RefRechnung").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							rechungsnummer = "";
						}

						datei_inhalt.setLength(0);

						datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
						datei_inhalt.append(
								"<LhList Operation=\"Insert\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

						datei_inhalt.append("<Lh>");
						datei_inhalt.append("<Nr>" + lieferscheinnummer + "</Nr>");
						datei_inhalt.append("<Date>" + datum + "</Date>");
						datei_inhalt.append("<GUID>" + guid + "</GUID>");
						datei_inhalt.append("<RefAuftrag>" + auftragsnummer + "</RefAuftrag>");
						datei_inhalt.append("<RefRechnung>" + rechungsnummer + "</RefRechnung>");
						datei_inhalt.append("<KdNr>" + kundennummer + "</KdNr>");
						datei_inhalt.append("<LhGebucht>false</LhGebucht>");
						datei_inhalt.append("<LhDoFak>false</LhDoFak>");
						datei_inhalt.append("<generated>wplag</generated>");
						datei_inhalt.append("</Lh>");
						datei_inhalt.append("</LhList>");

						try (FileOutputStream oS = new FileOutputStream(insert_xml_file)) {
							oS.write(datei_inhalt.toString().getBytes());

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					if (stack_xml_file.getName().contains("wareneingang_update_import"))

					{
						insert_xml_file_name = "_wareneingang_update_import_" + file_creation_time + this.mandant_name
								+ (i + 1) + ".xml";

						System.out.println("-----> " + insert_xml_file_name);

						insert_xml_file = new File(deliver_path + system_path_delimiter + insert_xml_file_name);

						Element wareneingang_kopf_element = (Element) (doc.getElementsByTagName("Weh").item(0));

						String wareneingangsnummer = "";
						String guid = "";
						String datum = "";
						String bestellnummer = "";

						try {

							wareneingangsnummer = wareneingang_kopf_element.getElementsByTagName("Nr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							wareneingangsnummer = "";
						}

						try {

							guid = wareneingang_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid = "";
						}

						try {

							datum = wareneingang_kopf_element.getElementsByTagName("Date").item(0).getTextContent()
									.trim();

						} catch (NullPointerException ecxc) {
							datum = "09.09.9999";
						}

						try {

							bestellnummer = wareneingang_kopf_element.getElementsByTagName("WepBhNr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							bestellnummer = "";

						}

						datei_inhalt.setLength(0);

						datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
						datei_inhalt.append(
								"<WehList Operation=\"Insert\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

						datei_inhalt.append("<Weh>");
						datei_inhalt.append("<Nr>" + wareneingangsnummer + "</Nr>");
						datei_inhalt.append("<Date>" + datum + "</Date>");
						datei_inhalt.append("<GUID>" + guid + "</GUID>");
						datei_inhalt.append("<WepBhNr>" + bestellnummer + "</WepBhNr>");
						datei_inhalt.append("<generated>wplag</generated>");
						datei_inhalt.append("</Weh>");
						datei_inhalt.append("</WehList>");

						try (FileOutputStream oS = new FileOutputStream(insert_xml_file)) {
							oS.write(datei_inhalt.toString().getBytes());

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					if (stack_xml_file.getName().contains("auftrag_update_import"))

					{
						insert_xml_file_name = "_auftrag_update_import_" + file_creation_time + this.mandant_name
								+ (i + 1) + ".xml";

						insert_xml_file = new File(deliver_path + system_path_delimiter + insert_xml_file_name);

						Element auftrag_kopf_element = (Element) (doc.getElementsByTagName("Ah").item(0));

						String auftragsnummer = "";
						String guid = "";
						String datum = "";

						String rechnungsnummer = "";
						String lieferscheinnummer = "";
						String kundennummer = "";

						try {

							auftragsnummer = auftrag_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							auftragsnummer = "";

						}

						try {

							guid = auftrag_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid = "";
						}

						try {

							datum = auftrag_kopf_element.getElementsByTagName("Date").item(0).getTextContent().trim();

						} catch (NullPointerException ecxc) {
							datum = "09.09.9999";
						}

						try {

							rechnungsnummer = auftrag_kopf_element.getElementsByTagName("RefRechnung").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							rechnungsnummer = "";
						}

						try {

							lieferscheinnummer = auftrag_kopf_element.getElementsByTagName("LfRefNr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							lieferscheinnummer = "";
						}

						try {

							kundennummer = auftrag_kopf_element.getElementsByTagName("KdNr").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							kundennummer = "";
						}

						datei_inhalt.setLength(0);

						datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
						datei_inhalt.append(
								"<AhList Operation=\"Insert\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

						datei_inhalt.append("<Ah>");
						datei_inhalt.append("<Nr>" + auftragsnummer + "</Nr>");
						datei_inhalt.append("<Date>" + datum + "</Date>");
						datei_inhalt.append("<GUID>" + guid + "</GUID>");
						datei_inhalt.append("<RefRechnung>" + rechnungsnummer + "</RefRechnung>");
						datei_inhalt.append("<LfRefNr>" + lieferscheinnummer + "</LfRefNr>");
						datei_inhalt.append("<KdNr>" + kundennummer + "</KdNr>");
						datei_inhalt.append("<Erledigt>false</Erledigt>");
						datei_inhalt.append("<Storno>false</Storno>");
						datei_inhalt.append("<generated>wplag</generated>");

						datei_inhalt.append("</Ah>");
						datei_inhalt.append("</AhList>");

						try (FileOutputStream oS = new FileOutputStream(insert_xml_file)) {
							oS.write(datei_inhalt.toString().getBytes());

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					if (stack_xml_file.getName().contains("rechnung_update_import"))

					{
						insert_xml_file_name = "_rechnung_update_import_" + file_creation_time + this.mandant_name
								+ (i + 1) + ".xml";

						insert_xml_file = new File(deliver_path + system_path_delimiter + insert_xml_file_name);

						Element rechnung_kopf_element = (Element) (doc.getElementsByTagName("Kh").item(0));

						String rechnungsnummer = "";
						String datum = "";
						String guid = "";
						String kundennumer = "";
						String auftrag = "";
						String lieferschein = "";
						int rechnungstyp = 0;

						try {

							rechnungsnummer = rechnung_kopf_element.getElementsByTagName("Nr").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							rechnungsnummer = "";

						}

						try {

							guid = rechnung_kopf_element.getElementsByTagName("GUID").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							guid = "";

						}

						try {

							datum = rechnung_kopf_element.getElementsByTagName("Date").item(0).getTextContent().trim();

						} catch (NullPointerException ecxc) {
							datum = "9999-09-09";
						}

						try {

							kundennumer = rechnung_kopf_element.getElementsByTagName("KdNr").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							kundennumer = "";

						}

						try {

							auftrag = rechnung_kopf_element.getElementsByTagName("RefAuftrag").item(0).getTextContent()
									.replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							auftrag = "";

						}
						try {

							lieferschein = rechnung_kopf_element.getElementsByTagName("LfRefNr").item(0)
									.getTextContent().replaceAll("[^A-Za-z0-9+-]", "");

						} catch (NullPointerException ecxc) {
							lieferschein = "";

						}

						try {

							BigDecimal menG = new BigDecimal(
									rechnung_kopf_element.getElementsByTagName("RechnungTyp").item(0).getTextContent());

							rechnungstyp = menG.intValue();

						} catch (NullPointerException ecxc) {
							rechnungstyp = 0;
						}

						datei_inhalt.setLength(0);

						datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
						datei_inhalt.append(
								"<KhList Operation=\"Insert\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

						datei_inhalt.append("<Kh>");
						datei_inhalt.append("<Nr>" + rechnungsnummer + "</Nr>");
						datei_inhalt.append("<Date>" + datum + "</Date>");
						datei_inhalt.append("<GUID>" + guid + "</GUID>");
						datei_inhalt.append("<KdNr>" + kundennumer + "</KdNr>");
						datei_inhalt.append("<RefAuftrag>" + auftrag + "</RefAuftrag>");
						datei_inhalt.append("<LfRefNr>" + lieferschein + "</LfRefNr>");
						datei_inhalt.append("<RechnungTyp>" + rechnungstyp + "</RechnungTyp>");
						datei_inhalt.append("<RechnungGebucht>false</RechnungGebucht>");
						datei_inhalt.append("<generated>wplag</generated>");
						datei_inhalt.append("</Kh>");
						datei_inhalt.append("</KhList>");

						try (FileOutputStream oS = new FileOutputStream(insert_xml_file)) {
							oS.write(datei_inhalt.toString().getBytes());

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					real_update_xml_file = new File(deliver_path + system_path_delimiter + stack_xml_file.getName());

					stack_xml_file.renameTo(real_update_xml_file);
				}
			}

		}

	}

}
