package STATISTIK_MANAGER;

import java.text.SimpleDateFormat;
import java.util.Date;

public class log_letztes_jahr_statistik_und_backup {

    Debuger DEBUGG_LOGGER = new Debuger();

    SimpleDateFormat db_date_formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    SimpleDateFormat db_date_formate_jahr = new SimpleDateFormat("yyyy");
    SimpleDateFormat db_date_formate_monat = new SimpleDateFormat("MM");
    SimpleDateFormat db_date_formate_tag = new SimpleDateFormat("dd");

    public log_letztes_jahr_statistik_und_backup() {

    }

    public void start(int _aJahr, int _vJahr, DatenBankVerbindung _db) {

	DatenBankVerbindung db = _db;

	String monat = db_date_formate_monat.format(new Date());

	String tag = db_date_formate_tag.format(new Date());

	if ("05".contentEquals(tag) && "01".contentEquals(monat))
	{

	    DEBUGG_LOGGER.addMassage("Das Jahr " + _vJahr + " wird festgeschrieben:");

	    String sql1 = "UPDATE statistik_" + _vJahr + " st Join "
		    + " static_artikel stArti on st.artikelnummer=stArti.interne_artikelnummer "
		    + " SET st.soll_inventur_ende_jahr = stArti.lager_anzahl;";

	    db.updateOnDatabase(sql1);

	    DEBUGG_LOGGER.addMassage("Das soll_inventur_ende_jahr wurden festgeschriben!");

	    String sql2 = " UPDATE statistik_" + _vJahr
		    + " SET abschluss_vorjahr_im_lager=((soll_inventur_ende_jahr+verkauf_total)-wareneingang_total), lager_aktuell=0, bestellt_aktuell=0;";

	    db.updateOnDatabase(sql2);

	    DEBUGG_LOGGER.addMassage("Das abschluss_vorjahr_im_lager (Reel) wurden festgeschriben!");

	    String sql3 = "UPDATE statistik_" + _vJahr + " stx Join statistik_" + (_vJahr - 1)
		    + " stxm1 on stx.artikelnummer=stxm1.artikelnummer "
		    + " SET stx.abschluss_vorjahr_im_lager_real =stxm1.soll_inventur_ende_jahr;";

	    db.updateOnDatabase(sql3);

	    DEBUGG_LOGGER.addMassage("Das abschluss_vorjahr_im_lager_real (Echtwert uebernommen von " + (_vJahr - 1)
		    + ") wurden festgeschriben!");

	} else {

	    DEBUGG_LOGGER.addMassage("Das Jahr " + _vJahr + "wurde bereits festgeschrieben!!!");
	}

    }

}
