package INITAL_MANAGER;


public class DM_Warengruppe  implements java.io.Serializable {

 
    private static final long serialVersionUID = -5111733641456362047L;
	private int id = 0;
	private String kurz = "";
	private String lang = "";

	public DM_Warengruppe(

			int _id, String _kurz, String _lang

	) {

		this.id = _id;
		this.kurz = _kurz;
		this.lang = _lang;
	}

	public DM_Warengruppe() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getKurz() {
		return kurz;
	}

	public void setKurz(String kurz) {
		this.kurz = kurz;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

}
