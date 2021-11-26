package INITAL_MANAGER;


public class DM_Artikel  implements java.io.Serializable {

	private int id = 0;

	// xml=GUID
	private String taifun_guid = "";

	// xml=???
	private String bezeichnung = "";

	// xml=Nr
	private String interne_artikelnummer = "";

	// xml=Match1
	private String match1 = "";

	// xml=Match1
	private String match2 = "";

	// xml=WHG
	private String waregruppe = "";

	// xml=HstNr
	private String hersteller_artikelnummer = "";

	// xml=BhGk
	private int lieferantenid = 0;

	// xml=EAN
	private String ean = "";

	private int hersteller_id = 0;
	private int warengruppe_id = 0;

	private int match1_id = 0;
	private int match2_id = 0;

	private int im_lager_anzahl = 0;
	private int auf_lieferschein_anzahl = 0;
	private int verfuegbar_anzahl = 0;

	public DM_Artikel() {

	}

	public DM_Artikel(

			String _guid, //
			String _interne_artikelnummer, //
			String _match1, //
			String _match2, //
			String _waregruppe, //
			String _hersteller_artikelnummer, //
			int _lieferantenid, //
			String _ean, //
			String _bezeichnung, //
			int _im_lager_anzahl, //
			int _auf_lieferschein_anzahl

	) {

		this.taifun_guid = _guid;
		this.interne_artikelnummer = _interne_artikelnummer;
		this.match1 = _match1;
		this.match2 = _match2;
		this.waregruppe = _waregruppe;

		this.hersteller_artikelnummer = _hersteller_artikelnummer;
		this.lieferantenid = _lieferantenid;
		this.ean = _ean;
		this.bezeichnung = _bezeichnung;

		this.im_lager_anzahl = _im_lager_anzahl;
		this.auf_lieferschein_anzahl = _auf_lieferschein_anzahl;

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTaifun_guid() {
		return taifun_guid;
	}

	public void setTaifun_guid(String taifun_guid) {
		this.taifun_guid = taifun_guid;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}

	public String getInterne_artikelnummer() {
		return interne_artikelnummer;
	}

	public void setInterne_artikelnummer(String interne_artikelnummer) {
		this.interne_artikelnummer = interne_artikelnummer;
	}

	public String getMatch1() {
		return match1;
	}

	public void setMatch1(String match1) {
		this.match1 = match1;
	}

	public String getMatch2() {
		return match2;
	}

	public void setMatch2(String match2) {
		this.match2 = match2;
	}

	public String getWaregruppe() {
		return waregruppe;
	}

	public void setWaregruppe(String waregruppe) {
		this.waregruppe = waregruppe;
	}

	public int getLieferantenid() {
		return lieferantenid;
	}

	public void setLieferantenid(int lieferantenid) {
		this.lieferantenid = lieferantenid;
	}

	public String getEan() {
		return ean;
	}

	public void setEan(String ean) {
		this.ean = ean;
	}

	public int getWarengruppe_id() {
		return warengruppe_id;
	}

	public void setWarengruppe_id(int warengruppe_id) {
		this.warengruppe_id = warengruppe_id;
	}

	public int getMatch1_id() {
		return match1_id;
	}

	public void setMatch1_id(int match1_id) {
		this.match1_id = match1_id;
	}

	public int getMatch2_id() {
		return match2_id;
	}

	public void setMatch2_id(int match2_id) {
		this.match2_id = match2_id;
	}

	public String getHersteller_artikelnummer() {
		return hersteller_artikelnummer;
	}

	public void setHersteller_artikelnummer(String hersteller_artikelnummer) {
		this.hersteller_artikelnummer = hersteller_artikelnummer;
	}

	public int getIm_lager_anzahl() {
		return im_lager_anzahl;
	}

	public void setIm_lager_anzahl(int im_lager_anzahl) {
		this.im_lager_anzahl = im_lager_anzahl;
	}

	public int getAuf_lieferschein_anzahl() {
		return auf_lieferschein_anzahl;
	}

	public void setAuf_lieferschein_anzahl(int auf_lieferschein_anzahl) {
		this.auf_lieferschein_anzahl = auf_lieferschein_anzahl;
	}

	public int getVerfuegbar_anzahl() {

		verfuegbar_anzahl = im_lager_anzahl - auf_lieferschein_anzahl;

		return verfuegbar_anzahl;
	}

	public void setVerfuegbar_anzahl(int verfuegbar_anzahl) {
		this.verfuegbar_anzahl = verfuegbar_anzahl;
	}

}
