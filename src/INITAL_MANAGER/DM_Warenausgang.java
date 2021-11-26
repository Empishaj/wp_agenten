package INITAL_MANAGER;


public class DM_Warenausgang implements java.io.Serializable {

    private static final long serialVersionUID = 8507234875837572052L;

    public DM_Warenausgang() {

    }

    private String guid = "";

    private String artikelnummer = "";
    private String datum = "";
    private int typid = 0;
    private int menge = 0;

    public DM_Warenausgang(

	    String _guid, String _artikelnummer, String _datum, int _typid, int _menge

    ) {

	this.guid = _guid;
	this.artikelnummer = _artikelnummer;
	this.datum = _datum;
	this.typid = _typid;
	this.menge = _menge;

    }

    public String getGuid() {
	return guid;
    }

    public void setGuid(String guid) {
	this.guid = guid;
    }

    public String getArtikelnummer() {
	return artikelnummer;
    }

    public void setArtikelnummer(String artikelnummer) {
	this.artikelnummer = artikelnummer;
    }

    public String getDatum() {
	return datum;
    }

    public void setDatum(String datum) {
	this.datum = datum;
    }

    public int getTypid() {
	return typid;
    }

    public void setTypid(int typid) {
	this.typid = typid;
    }

    public int getMenge() {
	return menge;
    }

    public void setMenge(int menge) {
	this.menge = menge;
    }
}
