package UPDATE_MANAGER;

public class LogChecker {

	/*
	 * Dieses Modul erzeugt Protokoll-Daten fuer die Datenbank. Die alten Daten aus
	 * der Datenbank und die neuen aus dem jeweiligen Agenten werden nach einander
	 * geordnet. Dabei wird beachtet das der Speicher der Datenbank nicht
	 * überlaeuft. In diesem Fall wird das Protokoll von oben abgeschnitten.
	 */

	public StringBuilder mysql_log = null;

	public int maxLengthOfLog = 1073741000;

	public String generateNewLogForDB(String _oldDB, String _newLog) {

		mysql_log = new StringBuilder();
		mysql_log.setLength(0);

		int length_old = _oldDB.length();
		int length_new = _newLog.length();

		int neuLaenge = length_old + length_new;

		if (neuLaenge <= maxLengthOfLog) {

			mysql_log.append(_oldDB);
			mysql_log.append("\n\n");
			mysql_log.append(_newLog);
			mysql_log.append("\n\n");

		}
		if (neuLaenge > maxLengthOfLog) {

			mysql_log.append(_newLog);
			mysql_log.append("\n\n");
			mysql_log.append(">>> Logfile Overload <<<");

		}

		return mysql_log.toString();
	}

}
