package STATISTIK_MANAGER;


import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class full_statistik_lagerbestand_letztes_jahr_rating_agent implements java.io.Serializable {

    private static final long serialVersionUID = -8999313669930913027L;

    Debuger DEBUGG_LOGGER = new Debuger();

    SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    SimpleDateFormat db_date_formate_jahr = new SimpleDateFormat("yyyy");

    public void start(

	    boolean _AktuellesJahrPosition_warenausgang, boolean _AktuellesJahrPosition_wareneingang,

	    DatenBankVerbindung _db) throws SQLException {

	boolean AktuellesJahrPosition_warenausgang = _AktuellesJahrPosition_warenausgang;
	boolean AktuellesJahrPosition_wareneingang = _AktuellesJahrPosition_wareneingang;

	// ----------------------------

	int aktuellesJahr = Integer.parseInt(db_date_formate_jahr.format(new Date()));

	int vorJahr = (aktuellesJahr - 1);

	// -----------------------------

	Date FILE_PROCESS_START_TIME = new Date();

	DatenBankVerbindung db = _db;

	//// -------------------------------------

	if (AktuellesJahrPosition_warenausgang == true || AktuellesJahrPosition_wareneingang == true) {

	    String sqlKorrekturLetztesJahr = "UPDATE statistik_" + aktuellesJahr
		    + " SET abschluss_vorjahr_im_lager=((lager_aktuell+verkauf_total)-wareneingang_total), last_update='"
		    + db_date_formate.format(new Date()) + "';";

	    db.updateOnDatabase(sqlKorrekturLetztesJahr);

	    String sqlUpdateVonLetztesJahr = "UPDATE statistik_" + aktuellesJahr + " st18 join statistik_" + vorJahr
		    + " st17 on st18.artikelnummer=st17.artikelnummer "
		    + " SET st18.abschluss_vorjahr_im_lager_real=IFNULL(st17.soll_inventur_ende_jahr ,0), st18.last_update='"
		    + db_date_formate.format(new Date()) + "';";

	    db.updateOnDatabase(sqlUpdateVonLetztesJahr);
	}

	Date FILE_PROCESS_END_TIME = new Date();

	long single_file_full_process_diff_time_in_sec = TimeUnit.MILLISECONDS
		.toSeconds(FILE_PROCESS_END_TIME.getTime() - FILE_PROCESS_START_TIME.getTime());

	DEBUGG_LOGGER.addMassage("[DATEI VERARBEITUNGSDAUER] >  " + single_file_full_process_diff_time_in_sec
		+ " Sekunden =  " + (single_file_full_process_diff_time_in_sec / 60) + " Minuten \n");

    }

}
