package INITAL_MANAGER;

public class AgentenStati implements java.io.Serializable {

    private static final long serialVersionUID = -5723801801804727652L;
    public static int STATUS_INITIALISIERUNG = 111;
    public static int STATUS_VERARBEITE_DATEIEN = 222;
    public static int STATUS_VERARBEITUNG_ABGEBROCHEN_DUPLIKAT = 233;
    public static int STATUS_DEAKTIV = 444;
    public static int STATUS_ERROR = 666;
    public static int STATUS_ERFOLGREICH_BEENDET = 777;
    public static int STATUS_AGENT_HAT_KEINE_DATEN = 888;
    public static int STATUS_DELIVER_ORDNER_EXISITERT_NICHT = 999;

}
