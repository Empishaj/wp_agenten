package UPDATE_MANAGER;


public class WeTOBeObjekt {

    private int wepBpRec = 0;
    private String bestellungnummer = "";
    private String artikel_nummer = "";
    private int menge_bestellt = 0;
    private int menge_bereits_geliefert = 0;
    private int menge_noch_zu_liefern = 0;
    private int jetzt_neu_geliefert_wareneingang = 0;
    private boolean bestellungGanzGeliefert = false;
    private String position_guid_bestellung = "";

    WeTOBeObjekt() {

    }

    WeTOBeObjekt(int _position, String _bestellungnummer, String _artikel_nummer, int _menge_bestellt,
	    int _menge_bereits_geliefert, int _menge_noch_zu_liefern, int _jetzt_neu_geliefert_wareneingang,
	    String _position_guid_bestellung)

    {

	this.wepBpRec = _position;
	this.bestellungnummer = _bestellungnummer;
	this.artikel_nummer = _artikel_nummer;
	this.menge_bestellt = _menge_bestellt;
	this.menge_bereits_geliefert = _menge_bereits_geliefert;
	this.menge_noch_zu_liefern = _menge_noch_zu_liefern;
	this.jetzt_neu_geliefert_wareneingang = _jetzt_neu_geliefert_wareneingang;
	this.position_guid_bestellung = _position_guid_bestellung;

    }

    public int getRecWepBp() {
	return wepBpRec;
    }

    public void setRecWepBp(int _wepBpRec) {
	this.wepBpRec = _wepBpRec;
    }

    public String getBestellungnummer() {
	return bestellungnummer;
    }

    public void setBestellungnummer(String bestellungnummer) {
	this.bestellungnummer = bestellungnummer;
    }

    public String getArtikel_nummer() {
	return artikel_nummer;
    }

    public void setArtikel_nummer(String artikel_nummer) {
	this.artikel_nummer = artikel_nummer;
    }

    public int getMenge_bestellt() {
	return menge_bestellt;
    }

    public void setMenge_bestellt(int menge_bestellt) {
	this.menge_bestellt = menge_bestellt;
    }

    public int getMenge_bereits_geliefert() {
	return menge_bereits_geliefert;
    }

    public void setMenge_bereits_geliefert(int menge_bereits_geliefert) {
	this.menge_bereits_geliefert = menge_bereits_geliefert;
    }

    public int getMenge_noch_zu_liefern() {
	return menge_noch_zu_liefern;
    }

    public void setMenge_noch_zu_liefern(int menge_noch_zu_liefern) {
	this.menge_noch_zu_liefern = menge_noch_zu_liefern;
    }

    public int getJetzt_neu_geliefert_wareneingang() {

	int temp = 0;

	if (bestellungGanzGeliefert == true) {

	    temp = 0;
	}

	if (bestellungGanzGeliefert == false) {

	    temp = jetzt_neu_geliefert_wareneingang;
	}

	return temp;

    }

    public void setJetzt_neu_geliefert_wareneingang(int jetzt_neu_geliefert_wareneingang) {
	this.jetzt_neu_geliefert_wareneingang = jetzt_neu_geliefert_wareneingang;

    }

    public int insgesamt_geliefert() {

	int neu_menge_soll = (getJetzt_neu_geliefert_wareneingang() + getMenge_bereits_geliefert());

	return neu_menge_soll;

    }

    public int neu_soll_menge() {

	int menge_soll_neu = (getMenge_bestellt() - insgesamt_geliefert());

	if (menge_soll_neu < 0) {
	    menge_soll_neu = 0;
	}

	return menge_soll_neu;

    }

    public String getPosition_guid_bestellung() {
	return position_guid_bestellung;
    }

    public void setPosition_guid_bestellung(String position_guid_bestellung) {
	this.position_guid_bestellung = position_guid_bestellung;
    }

    public boolean isBestellungGanzGeliefert() {
	return bestellungGanzGeliefert;
    }

    public void setBestellungGanzGeliefert(boolean bestellungGanzGeliefert) {
	this.bestellungGanzGeliefert = bestellungGanzGeliefert;
    }
}
