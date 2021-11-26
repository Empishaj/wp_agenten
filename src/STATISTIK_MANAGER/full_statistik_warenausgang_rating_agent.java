package STATISTIK_MANAGER;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class full_statistik_warenausgang_rating_agent implements java.io.Serializable {

    private static final long serialVersionUID = -8999313669930913027L;

    Debuger DEBUGG_LOGGER = new Debuger();

    public boolean aktuellesJahrPosition_warenausgang = false;

    public boolean VorJahrPosition_warenausgang = false;

    public boolean VorVorJahrPosition_warenausgang = false;

    public void start(int _aJahr, int _vJahr, DatenBankVerbindung _db) throws SQLException, ClassNotFoundException {

	Date FILE_PROCESS_START_TIME = new Date();

	SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat db_date_formate_jahr = new SimpleDateFormat("yyyy");

	int aktuellesJahr = Integer.parseInt(db_date_formate_jahr.format(new Date()));

	int vorJahr = (aktuellesJahr - 1);

	int vorVorJahr = (aktuellesJahr - 2);

	DatenBankVerbindung db = _db;

	boolean tableExistanceAktuellesJahr = false;
	boolean tableExistanceAktuellesJahrMinus1 = false;
	boolean tableExistanceAktuellesJahrMinus2 = false;

	// ----------------------------------------------------
	// Es wird ueberprueft ob die DB-Tabellen des aktuellen Jahres, des letzten
	// Jahres und des Vor vor letzen Jahres in der Datenbank existieren.

	// -----------------------------
	// aktuelles Jahr Check

	String sqlTableExistAktuellesJahr = "SELECT count(table_name) FROM information_schema.tables where table_schema = '160_wipo' and table_name like 'statistik_"
		+ _aJahr + "%' ;";

	db.readFromDatabase(sqlTableExistAktuellesJahr, " sqlTableExistAktuellesJahr");

	while (db.rs.next()) {

	    tableExistanceAktuellesJahr = db.rs.getBoolean(1);

	}
	// -----------------------------
	// vor Jahr Check

	String sqlTableExistAktuellesJahrMinus1 = "SELECT count(table_name) FROM information_schema.tables where table_schema = '160_wipo' and table_name like 'statistik_"
		+ vorJahr + "%' ;";

	db.readFromDatabase(sqlTableExistAktuellesJahrMinus1, " sqlTableExistAktuellesJahrMinus1");

	while (db.rs.next()) {

	    tableExistanceAktuellesJahrMinus1 = db.rs.getBoolean(1);

	}

	// -----------------------------
	// vor vor Jahr Check

	String sqlTableExistAktuellesJahrMinus2 = "SELECT count(table_name) FROM information_schema.tables where table_schema = '160_wipo' and table_name like 'statistik_"
		+ vorVorJahr + "%' ;";

	db.readFromDatabase(sqlTableExistAktuellesJahrMinus2, " sqlTableExistAktuellesJahrMinus1");

	while (db.rs.next()) {

	    tableExistanceAktuellesJahrMinus2 = db.rs.getBoolean(1);

	}

	// -----------------------------
	
	// Existieren diese drei Tabellen nicht, so werden diese erstellt.

	if (tableExistanceAktuellesJahr == false) {

	    String createTheTable = "CREATE TABLE `statistik_" + _aJahr + "` ("
		    + " `artikelnummer` varchar(20) NOT NULL, " + " `mandant_id` INT(11) NOT NULL DEFAULT '0', "
		    + " `lager_aktuell` int(11) NOT NULL DEFAULT '0', "
		    + " `bestellt_aktuell` int(11) NOT NULL DEFAULT '0',"
		    + " `abschluss_vorjahr_im_lager` int(11) DEFAULT '0',  `abschluss_vorjahr_im_lager_real` int(11) NOT NULL DEFAULT '0',"
		    + " `wareneingang_total` int(11) DEFAULT '0', " + " `verkauf_total` int(11) DEFAULT '0', "
		    + " `soll_inventur_ende_jahr` int(11) DEFAULT '0', " + " `warengruppeid` int(11) DEFAULT '0', "
		    + " `lieferentenid` int(11) DEFAULT '0', " + " `last_update` datetime DEFAULT NULL, "
		    + " `01v` int(11) NOT NULL DEFAULT '0',   `02v` int(11) NOT NULL DEFAULT '0', "
		    + " `03v` int(11) NOT NULL DEFAULT '0',   `04v` int(11) NOT NULL DEFAULT '0', "
		    + " `05v` int(11) NOT NULL DEFAULT '0',   `06v` int(11) NOT NULL DEFAULT '0', "
		    + " `07v` int(11) NOT NULL DEFAULT '0',  `08v` int(11) NOT NULL DEFAULT '0', "
		    + " `09v` int(11) NOT NULL DEFAULT '0',   `10v` int(11) NOT NULL DEFAULT '0', "
		    + " `11v` int(11) NOT NULL DEFAULT '0',   `12v` int(11) NOT NULL DEFAULT '0', "
		    + " `01eg` int(11) NOT NULL DEFAULT '0',   `02eg` int(11) NOT NULL DEFAULT '0', "
		    + " `03eg` int(11) NOT NULL DEFAULT '0',  `04eg` int(11) NOT NULL DEFAULT '0', "
		    + " `05eg` int(11) NOT NULL DEFAULT '0',  `06eg` int(11) NOT NULL DEFAULT '0', "
		    + " `07eg` int(11) NOT NULL DEFAULT '0',  `08eg` int(11) NOT NULL DEFAULT '0', "
		    + " `09eg` int(11) NOT NULL DEFAULT '0',  `10eg` int(11) NOT NULL DEFAULT '0', "
		    + " `11eg` int(11) NOT NULL DEFAULT '0',   `12eg` int(11) NOT NULL DEFAULT '0', "
		    + " PRIMARY KEY (`artikelnummer`), KEY `Indexts" + _aJahr
		    + "` (`warengruppeid`,`lieferentenid`,`artikelnummer`) USING BTREE ) "
		    + " ENGINE=InnoDB DEFAULT CHARSET=utf8;";

	    db.updateOnDatabase(createTheTable);

	    db.readFromDatabase(sqlTableExistAktuellesJahr, " sqlTableExist");

	    while (db.rs.next()) {

		tableExistanceAktuellesJahr = db.rs.getBoolean(1);

	    }

	}

	// Wenn diese Tabelle nun wirklich exisitert, dann lade diese Tabelle
	
	if (tableExistanceAktuellesJahr == true) {

	    // Aber entleere diese vorher
	    db.updateOnDatabase("truncate statistik_" + _aJahr + ";");

	    // Und dann lade diese
	    db.updateOnDatabase("INSERT INTO statistik_" + _aJahr
		    + " (artikelnummer,lager_aktuell,bestellt_aktuell,warengruppeid,lieferentenid,mandant_id ) "
		    + " Select DISTINCT static_artikel.interne_artikelnummer,static_artikel.lager_anzahl,"
		    + " static_artikel.bestellt_anzahl,static_artikel.warungruppe_id, static_artikel.lieferentenid,"
		    + " static_artikel.mandant_id from static_artikel where static_artikel.warungruppe_id NOT IN "
		    + " (SELECT statistik_warengruppe_ignore.warengruppe FROM statistik_warengruppe_ignore) "
		    + " and static_artikel.warungruppe_id <> 0 and static_artikel.lieferentenid <> 0;");

	    String read_verkaufte_artikel_bis_heute_sql = "SELECT artikelnummer, DATE_FORMAT(verkauft_am, '%m') as monat, DATE_FORMAT(verkauft_am, '%Y') as jahr, count(menge) as verkauft FROM static_warenausgang_positionen  where artikelnummer  !='?' group by artikelnummer, jahr, monat order by artikelnummer, monat, jahr;";

	    Vector<VerkaufUpdateObjekt> verkaufteArtikelBisHeute = new Vector<VerkaufUpdateObjekt>();

	    db.readFromDatabase(read_verkaufte_artikel_bis_heute_sql, "full_statistik_verkauf_rating_agent");

	    while (db.rs.next()) {

		verkaufteArtikelBisHeute.addElement(new VerkaufUpdateObjekt(db.rs.getString(1), db.rs.getString(2),
			db.rs.getString(3), db.rs.getInt(4)));

	    }

	    for (int i = 0; i < verkaufteArtikelBisHeute.size(); i++) {

		String monatSpalte = verkaufteArtikelBisHeute.get(i).getMonat();
		int jahrTabelle = Integer.parseInt(verkaufteArtikelBisHeute.get(i).getJahr());
		int anzahlVerkaufNeu = verkaufteArtikelBisHeute.get(i).getVerkauft();
		String artikelnummer = verkaufteArtikelBisHeute.get(i).getArtikelnummer();

		if (jahrTabelle == aktuellesJahr) {
		    aktuellesJahrPosition_warenausgang = true;

		}

		if (jahrTabelle == vorJahr) {
		    VorJahrPosition_warenausgang = true;
		}

		if (jahrTabelle == vorVorJahr) {
		    VorVorJahrPosition_warenausgang = true;
		}

		boolean existence = artikelExistenzJahrTabelleStatistik(jahrTabelle, artikelnummer, db);

		String update_statistik_table_sql = "UPDATE statistik_" + jahrTabelle + " set " + monatSpalte + "v="
			+ anzahlVerkaufNeu + ", last_update='" + db_date_formate.format(new Date())
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

	    String sqlVerkaufeGesamt = "UPDATE statistik_" + _aJahr
		    + " set verkauf_total=01v+02v+03v+04v+05v+06v+07v+08v+09v+10v+11v+12v, last_update='"
		    + db_date_formate.format(new Date()) + "';";

	    db.updateOnDatabase(sqlVerkaufeGesamt);

	    Date FILE_PROCESS_END_TIME = new Date();

	    long single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
		    .toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

	    DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] >  " + single_file_full_process_diff_time_in_sec
		    + " Sekunden =  " + (single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");
	}

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

    public boolean isAktuellesJahrPosition_warenausgang() {
	return aktuellesJahrPosition_warenausgang;
    }

    public boolean isVorJahrPosition_warenausgang() {
	return VorJahrPosition_warenausgang;
    }

    public boolean isVorVorJahrPosition_warenausgang() {
	return VorVorJahrPosition_warenausgang;
    }

}
