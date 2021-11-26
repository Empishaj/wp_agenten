package STATISTIK_MANAGER;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StatistikRatingManager {

    static CheckDoppelteArtikel checkArtikel = new CheckDoppelteArtikel();

    public static void main(String[] args) throws SQLException, ClassNotFoundException {

	Debuger DEBUGG_LOGGER = new Debuger();

	Date FILE_PROCESS_START_TIME = new Date();

	DatenBankVerbindung db = new DatenBankVerbindung();

	SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy");

	int aktuellesJahr = Integer.parseInt(db_date_formate.format(new Date()));

	int vorjahr = (aktuellesJahr - 1);

	db.updateOnDatabase("SET SQL_SAFE_UPDATES = 0;");

	boolean doppelte_datensaetze = false;

	String sql_depollte_datensaetze = "SELECT EXISTS (SELECT * FROM `static_artikel` as A, `static_artikel` as B where A.interne_artikelnummer=B.interne_artikelnummer and A.guid_taifun<> B.guid_taifun);";

	db.readFromDatabase(sql_depollte_datensaetze, "StatistikRatingManager");

	while (db.rs.next()) {
	    doppelte_datensaetze = db.rs.getBoolean(1);
	}

	if (doppelte_datensaetze == false) {

	    full_statistik_warenausgang_rating_agent warenausgang_rating = new full_statistik_warenausgang_rating_agent();

	    full_statistik_wareneingang_rating_agent wareneingang_rating = new full_statistik_wareneingang_rating_agent();

	    log_letztes_jahr_statistik_und_backup lockLastYear = new log_letztes_jahr_statistik_und_backup();

	    full_statistik_lagerbestand_letztes_jahr_rating_agent letztes_jahr_bestand = new full_statistik_lagerbestand_letztes_jahr_rating_agent();

	    DEBUGG_LOGGER.addMassage(
		    "*************************************************************************************************************** \n");
	    DEBUGG_LOGGER.addMassage("[MANAGER STARTET VERKAUF RATING]\n\n");

	    warenausgang_rating.start(aktuellesJahr, vorjahr, db);

	    DEBUGG_LOGGER.addMassage(warenausgang_rating.DEBUGG_LOGGER.debug_string.toString() + "\n");

	    DEBUGG_LOGGER.addMassage(
		    "*************************************************************************************************************** \n");
	    DEBUGG_LOGGER.addMassage("[MANAGER STARTET WAREBEINGANG RATING]\n\n");

	    wareneingang_rating.start(aktuellesJahr, vorjahr, db);

	    DEBUGG_LOGGER.addMassage(wareneingang_rating.DEBUGG_LOGGER.debug_string.toString() + "\n");

	    DEBUGG_LOGGER.addMassage(
		    "*************************************************************************************************************** \n");
	    DEBUGG_LOGGER.addMassage("[MANAGER STARTET DIE FESTSCHREIBUNG DES LETZTEN JAHRES ]\n\n");

	    lockLastYear.start(aktuellesJahr, vorjahr, db);

	    DEBUGG_LOGGER.addMassage(lockLastYear.DEBUGG_LOGGER.debug_string.toString() + "\n");

	    DEBUGG_LOGGER.addMassage(
		    "*************************************************************************************************************** \n");
	    DEBUGG_LOGGER.addMassage("[MANAGER STARTET VORJAHR LAGERENDBESTAND RATING]\n\n");

	    letztes_jahr_bestand.start(warenausgang_rating.isAktuellesJahrPosition_warenausgang(),
		    wareneingang_rating.isAktuellesJahrPosition_wareneingang(), db);

	    DEBUGG_LOGGER.addMassage(letztes_jahr_bestand.DEBUGG_LOGGER.debug_string.toString() + "\n");

	    DEBUGG_LOGGER.addMassage(
		    "*************************************************************************************************************** \n");

	    Date FILE_PROCESS_END_TIME = new Date();

	    long single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
		    .toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

	    DEBUGG_LOGGER.addMassage("[STATISIK VERARBEITUNGSDAUER] >  " + single_file_full_process_diff_time_in_sec
		    + " Sekunden =  " + (single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");

	} else {

	    checkArtikel.start();

	}

    }

}
