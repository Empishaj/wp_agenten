package INITAL_MANAGER;

public class DM_Lieferant  implements java.io.Serializable {
 
    private static final long serialVersionUID = 1220345274439982060L;
	private int id = 0;
	private String bezeichnung = "";

	public DM_Lieferant() {

	}

	public DM_Lieferant(

			int _id, String _bezeichnung) {

		this.id = _id;
		this.bezeichnung = _bezeichnung;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
