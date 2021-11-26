package UPDATE_MANAGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WareneingangToBestellungXMLGenerator implements java.io.Serializable {

	private static final long serialVersionUID = 611725941850015321L;

	File wareneingang_datei = null;

	File bestellung = null;

	String deliver_path = "";

	String oparating_system = "";

	String system_path_delimiter = "";

	String mandant_name = "_xx_";

	DatenBankVerbindung db;

	String file_creation_time = "";

	String gen_file_bestellung_xml_file_name = "";

	boolean isLinux = false;

	public boolean istBestellKopfInDB = false;

	SimpleDateFormat formatNormalized = new SimpleDateFormat("dd.MM.yyyy");

	SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	SimpleDateFormat formatdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	SimpleDateFormat XML_FORMAT_DATUM = new SimpleDateFormat("yyyy-MM-dd");

	StringBuilder datei_inhalt = null;

	String wareneingang_kopf_bestellnummer = "";

	Vector<WeTOBeObjekt> neuWarenEingaenge = new Vector<WeTOBeObjekt>();

	Date datum_wareneingang_temp = null;

	String stack_path = "";

	String waeingang_pattern = "";

	String wareneingang_stack_filename = "";

	String wareneingang_file_code = "";

	int file_db_id = 0;

	String type = "Update";

	WareneingangToBestellungXMLGenerator() {

	}

	WareneingangToBestellungXMLGenerator(boolean _isLinux, String _opsystem, String _system_path_delimiter,
			String _deliverpath, String _stack_path, String _wareneingang_stack_filename, int _file_db_id, String _type,
			DatenBankVerbindung _db) {

		isLinux = _isLinux;

		oparating_system = _opsystem;

		system_path_delimiter = _system_path_delimiter;

		deliver_path = _deliverpath;

		db = _db;

		stack_path = _stack_path;

		datei_inhalt = new StringBuilder();

		wareneingang_stack_filename = _wareneingang_stack_filename;

		file_db_id = _file_db_id;

		type = _type;

	}

	public void start(File workfile_wareneingang, String _bestellnummer, String _wareneingang_file_code)
			throws IOException, SAXException, ParserConfigurationException, SQLException {

		wareneingang_file_code = _wareneingang_file_code;
		wareneingang_datei = workfile_wareneingang;
		wareneingang_kopf_bestellnummer = _bestellnummer;

		istBestellKopfInDB = existiert_diese_bestellung(wareneingang_kopf_bestellnummer);

		String file_order = "";

		if (istBestellKopfInDB == true) {
			file_order = "111_";
			type = "Update";

		} else {
			file_order = "000_";
			type = "Insert";
		}

		Path path = Paths.get(wareneingang_datei.getAbsolutePath());

		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

		if (wareneingang_datei.getName().contains("_wp_")) {
			this.mandant_name = "_wp_";

		}

		if (wareneingang_datei.getName().contains("_ka_")) {

			this.mandant_name = "_ka_";
		}

		file_creation_time = format1.format(new Date((attr.lastModifiedTime().to(TimeUnit.MILLISECONDS) - 50000)));

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(wareneingang_datei);

		doc.getDocumentElement().normalize();

		gen_file_bestellung_xml_file_name = file_order + "20000101010101.010_bestellung_update_import"
				+ this.mandant_name + "GEN.xml";

		bestellung = new File(deliver_path + system_path_delimiter + gen_file_bestellung_xml_file_name);

		neuWarenEingaenge.clear();

		Element wareneingangs_kopf_element = (Element) (doc.getElementsByTagName("Weh").item(0));

		NodeList wareneingangs_positionen = wareneingangs_kopf_element.getElementsByTagName("Wep");

		int anzahl_der_wareneingangs_positionen = wareneingangs_positionen.getLength();

		if (anzahl_der_wareneingangs_positionen > 0) {

			for (int position_im_wareneing = 0; position_im_wareneing < wareneingangs_positionen
					.getLength(); position_im_wareneing++) {

				WeTOBeObjekt newWarenEingang = new WeTOBeObjekt();

				String artikel_nummer = "";
				int mengeJetztGeliefert = 0;
				int recWepBp = 0;

				newWarenEingang.setBestellungnummer(wareneingang_kopf_bestellnummer);

				Node einzelne_positions_wurzel = wareneingangs_positionen.item(position_im_wareneing);

				if (einzelne_positions_wurzel.getNodeType() == Node.ELEMENT_NODE) {

					Element wareneingangpositionElement = (Element) einzelne_positions_wurzel;

					try {

						artikel_nummer = wareneingangpositionElement.getElementsByTagName("ArtNr").item(0)
								.getTextContent().replaceAll("[^A-Za-z0-9/\\u2024\\u002E\\u002D]", "");

						newWarenEingang.setArtikel_nummer(artikel_nummer);

					} catch (NullPointerException ecxc) {
						artikel_nummer = "?";
					}

					try {

						BigDecimal menG = new BigDecimal(
								wareneingangpositionElement.getElementsByTagName("Menge").item(0).getTextContent());

						mengeJetztGeliefert = menG.intValue();

						newWarenEingang.setJetzt_neu_geliefert_wareneingang(mengeJetztGeliefert);

					} catch (NullPointerException ecxc) {
						mengeJetztGeliefert = 0;
					}

					try {

						BigDecimal menG = new BigDecimal(
								wareneingangpositionElement.getElementsByTagName("Rec").item(0).getTextContent());

						recWepBp = menG.intValue();

						newWarenEingang.setRecWepBp(recWepBp);

					} catch (NullPointerException ecxc) {
						recWepBp = 0;
					}

				}

				if (istBestellKopfInDB == true) {

					if (artikel_nummer.contains("WP-") == false && artikel_nummer.contains("WPK-") == false
							&& artikel_nummer != "" && artikel_nummer != "?" && artikel_nummer.length() >= 5) {

						int rec_aus_bestellung = 0;
						int menge_bestellt_aus_bestellung = 0;
						int menge_geliefert_aus_bestellung = 0;
						int menge_noch_zu_liefern_aus_bestellung = 0;
						String positon_guid = "";

						String posiSQL = "SELECT position, menge_bestellt, menge_geliefert, menge_noch_zu_liefern, guid_position FROM static_bestellung_positionen where artikel_nummer='"
								+ artikel_nummer + "' and bestellungnummer='" + wareneingang_kopf_bestellnummer
								+ "' and position='" + recWepBp + "';";

						db.readFromDatabase(posiSQL, "posiSQL");

						while (db.rs.next()) {

							rec_aus_bestellung = db.rs.getInt(1);
							menge_bestellt_aus_bestellung = db.rs.getInt(2);
							menge_geliefert_aus_bestellung = db.rs.getInt(3);
							menge_noch_zu_liefern_aus_bestellung = db.rs.getInt(4);
							positon_guid = db.rs.getString(5);

						}

						boolean position_wurde_ganz_geliefert = false;

						if (menge_bestellt_aus_bestellung == menge_geliefert_aus_bestellung
								&& menge_noch_zu_liefern_aus_bestellung == 0) {
							position_wurde_ganz_geliefert = true;
						}

						newWarenEingang.setBestellungGanzGeliefert(position_wurde_ganz_geliefert);

						newWarenEingang.setJetzt_neu_geliefert_wareneingang(mengeJetztGeliefert);

						newWarenEingang.setRecWepBp(rec_aus_bestellung);

						newWarenEingang.setMenge_bestellt(menge_bestellt_aus_bestellung);

						newWarenEingang.setMenge_bereits_geliefert(menge_geliefert_aus_bestellung);

						newWarenEingang.setMenge_noch_zu_liefern(menge_noch_zu_liefern_aus_bestellung);

						newWarenEingang.setPosition_guid_bestellung(positon_guid);

						this.neuWarenEingaenge.addElement(newWarenEingang);

						System.out.println("------------------------------------------------");
						System.out.println("Bestellung: " + wareneingang_kopf_bestellnummer + "\n");

						System.out.println("Bestellung-RecPosition  : " + rec_aus_bestellung + "\n");
						System.out.println("Wareneingang-RecPosition: " + recWepBp + "\n");
						System.out.println("Bestellung-GuidPosition : " + positon_guid + "\n");

						System.out.println("Jetzt geliefert aus Wareneingang: " + mengeJetztGeliefert);
						System.out.println("Bestellmenge aus der Bestellung: " + newWarenEingang.getMenge_bestellt());
						System.out.println(
								"Gelieferte Menge aus Bestellung: " + newWarenEingang.getMenge_bereits_geliefert());
						System.out
								.println("Menge Soll in der Bestellung: " + newWarenEingang.getMenge_noch_zu_liefern());
						System.out.println("------------------------------------------------");

					}

				}

				if (istBestellKopfInDB == false) {

					if (artikel_nummer.contains("WP-") == false && artikel_nummer.contains("WPK-") == false
							&& artikel_nummer != "" && artikel_nummer != "?" && artikel_nummer.length() >= 5) {

						newWarenEingang.setJetzt_neu_geliefert_wareneingang(0);

						newWarenEingang.setRecWepBp(recWepBp);

						newWarenEingang.setMenge_bestellt(mengeJetztGeliefert);

						newWarenEingang.setMenge_bereits_geliefert(0);

						newWarenEingang.setMenge_noch_zu_liefern(0);

						newWarenEingang.setPosition_guid_bestellung(
								"GUID" + recWepBp + wareneingang_kopf_bestellnummer + artikel_nummer);

						this.neuWarenEingaenge.addElement(newWarenEingang);

						System.out.println("------------------------------------------------");
						System.out.println("Bestellung: " + wareneingang_kopf_bestellnummer + "\n");
						System.out.println("Wareneingang-RecPosition: " + recWepBp + "\n");
						System.out.println("Bestellung-GuidPosition : " + "GUID" + recWepBp
								+ wareneingang_kopf_bestellnummer + artikel_nummer + "\n");

						System.out.println("Jetzt geliefert aus Wareneingang: "
								+ newWarenEingang.getJetzt_neu_geliefert_wareneingang());
						System.out.println("Bestellmenge aus der Bestellung: " + newWarenEingang.getMenge_bestellt());
						System.out.println(
								"Gelieferte Menge aus Bestellung: " + newWarenEingang.getMenge_bereits_geliefert());
						System.out
								.println("Menge Soll in der Bestellung: " + newWarenEingang.getMenge_noch_zu_liefern());
						System.out.println("------------------------------------------------");

					}

				}

			}

		}

		// -----------------------------------------------

		datei_inhalt.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
		datei_inhalt.append("<BhList Operation=\"" + type
				+ "\" xmlns=\"urn:taifun-software.de:schema:TAIFUN\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

		datei_inhalt.append("<Bh>");
		datei_inhalt.append("<Nr>" + wareneingang_kopf_bestellnummer + "</Nr>");
		datei_inhalt.append("<Date>" + XML_FORMAT_DATUM.format(new Date()) + "</Date>");
		datei_inhalt.append("<BhErledigt>false</BhErledigt>");
		datei_inhalt.append("<BhStorno>false</BhStorno>");
		datei_inhalt.append("<generated>wplag</generated>");

		datei_inhalt.append("<GUID>WPLAG" + wareneingang_kopf_bestellnummer + "-" + file_creation_time + "</GUID>");
		datei_inhalt.append("<WEFile>" + wareneingang_file_code + "</WEFile>");

		datei_inhalt.append("<BpList>");

		for (int i = 0; i < neuWarenEingaenge.size(); i++) {

			datei_inhalt.append("<Bp>");

			datei_inhalt.append("<Rec>" + neuWarenEingaenge.get(i).getRecWepBp() + "</Rec>");
			datei_inhalt.append("<GUID>" + neuWarenEingaenge.get(i).getPosition_guid_bestellung() + "</GUID>");
			datei_inhalt.append("<ArtNr>" + neuWarenEingaenge.get(i).getArtikel_nummer() + "</ArtNr>");
			datei_inhalt.append("<Menge>" + neuWarenEingaenge.get(i).getMenge_bestellt() + "</Menge>");
			datei_inhalt.append("<WepBhNr>" + wareneingang_kopf_bestellnummer + "</WepBhNr>");
			datei_inhalt
					.append("<MengeGeliefert>" + neuWarenEingaenge.get(i).insgesamt_geliefert() + "</MengeGeliefert>");
			datei_inhalt.append("<MengeSoll>" + neuWarenEingaenge.get(i).neu_soll_menge() + "</MengeSoll>");

			datei_inhalt.append("</Bp>");

		}

		datei_inhalt.append("</BpList>");
		datei_inhalt.append("</Bh>");
		datei_inhalt.append("</BhList>");

		try (FileOutputStream oS = new FileOutputStream(bestellung)) {
			oS.write(datei_inhalt.toString().getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean existiert_diese_bestellung(String _bestellnummer) throws SQLException {

		boolean temp = false;

		String sqlCheckExsistenz = "SELECT EXISTS(SELECT * FROM static_bestellung WHERE interne_nummer_bestellung='"
				+ _bestellnummer + "');";

		db.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

		while (db.rs.next())

		{
			temp = db.rs.getBoolean(1);
		}

		return temp;
	}

	public File getGeneratedBestellungsDatei() {

		return this.bestellung;

	}
}
