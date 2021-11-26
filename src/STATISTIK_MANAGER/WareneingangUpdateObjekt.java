package STATISTIK_MANAGER;


public class WareneingangUpdateObjekt {

    WareneingangUpdateObjekt() {

    }

    private String artikelnummer = "";
    private String monat = "";
    private String jahr = "";
    private int eingang = 0;

    public WareneingangUpdateObjekt(

	    String _artikelnummer, String _monat, String _jahr, int _eingang

    ) {

	this.artikelnummer = _artikelnummer;
	this.monat = _monat;
	this.jahr = _jahr;
	this.eingang = _eingang;
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

    public int getEingang() {
	return eingang;
    }

    public void setEingang(int eingang) {
	this.eingang = eingang;
    }

}
