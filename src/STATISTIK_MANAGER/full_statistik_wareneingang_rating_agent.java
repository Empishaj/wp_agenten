package STATISTIK_MANAGER;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class full_statistik_wareneingang_rating_agent implements java.io.Serializable {

    private static final long serialVersionUID = -8999313669930913027L;

    Debuger DEBUGG_LOGGER = new Debuger();

    public boolean aktuellesJahrPosition_wareneingang = false;

    public boolean VorJahrPosition_wareneingang = false;

    public boolean VorVorJahrPosition_wareneingang = false;

    public void start(int _aJahr, int _vJahr, DatenBankVerbindung _db) throws SQLException, ClassNotFoundException {

	Date FILE_PROCESS_START_TIME = new Date();

	SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat db_date_formate_jahr = new SimpleDateFormat("yyyy");

	int aktuellesJahr = Integer.parseInt(db_date_formate_jahr.format(new Date()));

	int vorJahr = (aktuellesJahr - 1);

	int vorVorJahr = (aktuellesJahr - 2);

	DatenBankVerbindung db = _db;

	int jahr = _aJahr;

	String read_wareneing_artikel_heute_sql = "SELECT artikelnummer, DATE_FORMAT(eingangsdatum, '%m') as monat, "
		+ " DATE_FORMAT(eingangsdatum, '%Y') as jahr, count(menge) as eingang FROM "
		+ " static_wareineingang_positionen where artikelnummer <>'?' group by artikelnummer, "
		+ "jahr, monat order by artikelnummer, jahr, monat;";

	Vector<WareneingangUpdateObjekt> eingangArtikelHeute = new Vector<WareneingangUpdateObjekt>();

	db.readFromDatabase(read_wareneing_artikel_heute_sql, "read_wareneing_artikel_heute_sql");

	while (db.rs.next()) {

	    eingangArtikelHeute.addElement(new WareneingangUpdateObjekt(db.rs.getString(1), db.rs.getString(2),
		    db.rs.getString(3), db.rs.getInt(4)));

	}

	for (int i = 0; i < eingangArtikelHeute.size(); i++) {

	    String monatSpalte = eingangArtikelHeute.get(i).getMonat();
	    int jahrTabelle = Integer.parseInt(eingangArtikelHeute.get(i).getJahr());
	    int anzahlVerkaufNeu = eingangArtikelHeute.get(i).getEingang();
	    String artikelnummer = eingangArtikelHeute.get(i).getArtikelnummer();

	    if (jahrTabelle == aktuellesJahr) {
		aktuellesJahrPosition_wareneingang = true;
	    }

	    if (jahrTabelle == vorJahr) {
		VorJahrPosition_wareneingang = true;
	    }

	    if (jahrTabelle == vorVorJahr) {
		VorVorJahrPosition_wareneingang = true;
	    }

	    boolean existence = artikelExistenzJahrTabelleStatistik(jahrTabelle, artikelnummer, db);

	    String update_statistik_table_sql = "UPDATE statistik_" + jahrTabelle + " set " + monatSpalte + "eg="
		    + anzahlVerkaufNeu + " , last_update='" + db_date_formate.format(new Date())
		    + "' where artikelnummer='" + artikelnummer + "';";

	    if (existence == true) {

		db.updateOnDatabase(update_statistik_table_sql);

	    } else {

		int mandant = checkMandant(artikelnummer, db);

		String insert_statistik_table_sql = "Insert into statistik_" + jahrTabelle
			+ " (artikelnummer) Values ('" + artikelnummer + "');";

		String updateMandantNeuerArtikel = "Update statistik_" + jahrTabelle + " set mandant_id= " + mandant
			+ " where artikelnummer='" + artikelnummer + "';";

		if (mandant > 0) {

		    db.insertToDatabase(insert_statistik_table_sql, "");

		    db.updateOnDatabase(updateMandantNeuerArtikel);

		    db.updateOnDatabase(update_statistik_table_sql);
		}

	    }

	}

	String sqEingangGesamt = "UPDATE statistik_" + jahr
		+ " set wareneingang_total=01eg+02eg+03eg+04eg+05eg+06eg+07eg+08eg+09eg+10eg+11eg+12eg, last_update='"
		+ db_date_formate.format(new Date()) + "';";

	db.updateOnDatabase(sqEingangGesamt);
	
	

	Date FILE_PROCESS_END_TIME = new Date();

	long single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
		.toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

	DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] >  " + single_file_full_process_diff_time_in_sec
		+ " Sekunden =  " + (single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");

    }

    public boolean artikelExistenzJahrTabelleStatistik(int jahr, String artikel_nummer, DatenBankVerbindung _db)
	    throws SQLException {

	String sqlCheckExsistenz = "SELECT EXISTS (SELECT * FROM statistik_" + jahr + " where artikelnummer='"
		+ artikel_nummer + "');";

	boolean temp = true;

	DatenBankVerbindung db = _db;

	db.readFromDatabase(sqlCheckExsistenz, "sqlCheckExsistenz");

	while (db.rs.next())

	{
	    temp = db.rs.getBoolean(1);
	}

	return temp;

    }

    public int checkMandant(String artikelnummer, DatenBankVerbindung _db) throws SQLException {

	int temp = 0;

	String getMandant = "SELECT mandant_id FROM static_artikel where interne_artikelnummer='" + artikelnummer
		+ "';";

	DatenBankVerbindung db = _db;

	db.readFromDatabase(getMandant, "getMandant");

	while (db.rs.next())

	{
	    temp = db.rs.getInt(1);
	}

	return temp;

    }

    public boolean isAktuellesJahrPosition_wareneingang() {
	return aktuellesJahrPosition_wareneingang;
    }

    public boolean isVorJahrPosition_wareneingang() {
	return VorJahrPosition_wareneingang;
    }

    public boolean isVorVorJahrPosition_wareneingang() {
	return VorVorJahrPosition_wareneingang;
    }

}
