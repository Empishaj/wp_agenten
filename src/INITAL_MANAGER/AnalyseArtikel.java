package INITAL_MANAGER;

public class AnalyseArtikel implements java.io.Serializable {

    private static final long serialVersionUID = -5060338858801894863L;

    public AnalyseArtikel() {

    }

    private String artikel_nummer = "";

    private int lager_alt = 0;
    private int lager_neu = 0;

    private int bestellt_alt = 0;
    private int bestellt_neu = 0;

    private int reserviert_alt = 0;
    private int reserviert_neu = 0;

    private int reserviert_rech_alt = 0;
    private int reserviert_rech_neu = 0;

    private int lager_diff = 0;
    private int reserviert_diff = 0;
    private int bestellt_diff = 0;
    private int rech_diff = 0;

    private String log = "";

    public AnalyseArtikel(

	    String _artikel_nummer,

	    int _lager_neu, int _lager_alt, int _lager_diff,

	    int _reserviert_neu, int _reserviert_alt, int _reserviert_diff,

	    int _bestellt_neu, int _bestellt_alt, int _bestellt_diff,

	    int _reserviert_rech_neu, int _reserviert_rech_alt, int _rech_diff,

	    String _log

    ) {

	this.artikel_nummer = _artikel_nummer;

	this.lager_alt = _lager_alt;
	this.lager_neu = _lager_neu;
	this.lager_diff = _lager_diff;
	this.bestellt_alt = _bestellt_alt;
	this.bestellt_neu = _bestellt_neu;
	this.bestellt_diff = _bestellt_diff;
	this.reserviert_alt = _reserviert_alt;
	this.reserviert_neu = _reserviert_neu;
	this.reserviert_diff = _reserviert_diff;

	reserviert_rech_alt = _reserviert_rech_alt;
	reserviert_rech_neu = _reserviert_rech_neu;
	rech_diff = _rech_diff;
	this.log = _log;

    }

    public String getLog() {
	return log;
    }

    public String getArtikel_nummer() {
	return artikel_nummer;
    }

    public void setArtikel_nummer(String artikel_nummer) {
	this.artikel_nummer = artikel_nummer;
    }

    public int getLager_alt() {
	return lager_alt;
    }

    public void setLager_alt(int lager_alt) {
	this.lager_alt = lager_alt;
    }

    public int getLager_neu() {
	return lager_neu;
    }

    public void setLager_neu(int lager_neu) {
	this.lager_neu = lager_neu;
    }

    public int getBestellt_alt() {
	return bestellt_alt;
    }

    public void setBestellt_alt(int bestellt_alt) {
	this.bestellt_alt = bestellt_alt;
    }

    public int getBestellt_neu() {
	return bestellt_neu;
    }

    public void setBestellt_neu(int bestellt_neu) {
	this.bestellt_neu = bestellt_neu;
    }

    public int getReserviert_alt() {
	return reserviert_alt;
    }

    public void setReserviert_alt(int reserviert_alt) {
	this.reserviert_alt = reserviert_alt;
    }

    public int getReserviert_neu() {
	return reserviert_neu;
    }

    public void setReserviert_neu(int reserviert_neu) {
	this.reserviert_neu = reserviert_neu;
    }

    public int getLager_diff() {

	this.lager_diff = lager_neu - lager_alt;

	return lager_diff;
    }

    public int getReserviert_diff() {

	this.reserviert_diff = reserviert_neu - reserviert_alt;

	return reserviert_diff;
    }

    public int getBestellt_diff() {

	this.bestellt_diff = bestellt_neu - bestellt_alt;

	return bestellt_diff;
    }

    public int getReserviert_rech_alt() {
	return reserviert_rech_alt;
    }

    public void setReserviert_rech_alt(int reserviert_rech_alt) {
	this.reserviert_rech_alt = reserviert_rech_alt;
    }

    public int getReserviert_rech_neu() {
	return reserviert_rech_neu;
    }

    public void setReserviert_rech_neu(int reserviert_rech_neu) {
	this.reserviert_rech_neu = reserviert_rech_neu;
    }

    public int getRech_diff() {
	return rech_diff;
    }

    public void setRech_diff(int rech_diff) {
	this.rech_diff = rech_diff;
    }

}
