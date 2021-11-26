package STATISTIK_MANAGER;

public class VerkaufUpdateObjekt {

    VerkaufUpdateObjekt() {

    }

    private String artikelnummer = "";
    private String monat = "";
    private String jahr = "";
    private int verkauft = 0;

    public VerkaufUpdateObjekt(

	    String _artikelnummer, String _monat, String _jahr, int _verkauft

    ) {

	this.artikelnummer = _artikelnummer;
	this.monat = _monat;
	this.jahr = _jahr;
	this.verkauft = _verkauft;
    }

    public String getArtikelnummer() {
	return artikelnummer;
    }

    public void setArtikelnummer(String artikelnummer) {
	this.artikelnummer = artikelnummer;
    }

    public String getMonat() {
	return monat;
    }

    public void setMonat(String monat) {
	this.monat = monat;
    }

    public String getJahr() {
	return jahr;
    }

    public void setJahr(String jahr) {
	this.jahr = jahr;
    }

    public int getVerkauft() {
	return verkauft;
    }

    public void setVerkauft(int verkauft) {
	this.verkauft = verkauft;
    }

}
