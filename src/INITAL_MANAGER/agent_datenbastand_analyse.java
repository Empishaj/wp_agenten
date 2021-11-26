package INITAL_MANAGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

class agent_datenbastand_analyse implements java.io.Serializable {

    private static final long serialVersionUID = 4303251825088949856L;

    String db_system_mail_sender_host_name = "wplag.de";
    String db_system_mail_sender_host_port = "25";

    RandomCode code = new RandomCode();

    String file_code = "";

    Message html_email;
    Session session;
    Authenticator authentifizierung;

    String pfadtrenner = "";
    String webspace_url = "";
    String html_list_file_name = "";
    String html_logs_file_name = "";
    HtmlDokument htmlInhalt = new HtmlDokument();
    DatenBankVerbindung db = new DatenBankVerbindung();
    String web_link_to_html_artikel_liste = "";
    String web_link_to_html_artikel_logs = "";

    static SimpleDateFormat formatlogfile = new SimpleDateFormat("yyyyMMddHHmmss");
    static SimpleDateFormat formatlogfile2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    SimpleDateFormat formatdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Debugger DEBUGER = new Debugger();

    boolean isLinux = false;

    public agent_datenbastand_analyse(String _pfadtrnner, String _webspace) {
	pfadtrenner = _pfadtrnner;
	webspace_url = _webspace;
    }

    public void start(boolean _isLinux) throws SQLException {

	DEBUGER.debugStatus = true;

	this.isLinux = _isLinux;

	int i = 0;

	file_code = code.newRandomCode();

	Vector<AnalyseArtikel> temp = new Vector<AnalyseArtikel>();

	String sqlCheckBestand = "Select artikel,nlag,alag, diflag, nres,ares, difres,nbest, abest,difbes, nrechres,arechres,difrech, log from "
		+ "(Select neu.interne_artikelnummer as artikel, neu.lager_anzahl as nlag, alt.lager_anzahl as alag, neu.lager_anzahl- alt.lager_anzahl as diflag, "
		+ "neu.reserviert_anzahl as nres, alt.reserviert_anzahl as ares, neu.reserviert_anzahl- alt.reserviert_anzahl as difres, neu.bestellt_anzahl as nbest, "
		+ "alt.bestellt_anzahl as abest, neu.bestellt_anzahl- alt.bestellt_anzahl as difbes, neu.reserviert_durch_rechnung as nrechres, "
		+ "alt.reserviert_durch_rechnung as arechres, neu.reserviert_durch_rechnung- alt.reserviert_durch_rechnung as difrech, "
		+ "alt.agent_operation as log from static_artikel as neu, static_artikel_old as alt where  neu.interne_artikelnummer=alt.interne_artikelnummer ) "
		+ "as analysetab where diflag<>0 or difres<>0 or  difbes<>0 or difrech<>0";

	db.readFromDatabase(sqlCheckBestand, "sqlCheckBestand");

	while (db.rs.next()) {
	    i++;

	    temp.addElement(new AnalyseArtikel(

		    db.rs.getString(1), db.rs.getInt(2), db.rs.getInt(3), db.rs.getInt(4), db.rs.getInt(5),
		    db.rs.getInt(6), db.rs.getInt(7), db.rs.getInt(8), db.rs.getInt(9), db.rs.getInt(10),
		    db.rs.getInt(11), db.rs.getInt(12), db.rs.getInt(13), db.rs.getString(14)

	    ));
	    DEBUGER.addMassage("[" + i + "] - [" + db.rs.getString(1) + "]");
	    DEBUGER.addMassage("********************************************************************************");
	    DEBUGER.addMassage(db.rs.getString(14));
	    DEBUGER.addMassage("********************************************************************************");

	}

	StringBuffer html_content = htmlInhalt.generiereHTMLInhalt(temp);

	String date = formatlogfile.format(new Date());
	String date2 = formatlogfile2.format(new Date());

	html_list_file_name = file_code + "_datenanalyse_" + date + ".html";
	html_logs_file_name = file_code + "_logs_" + date + ".txt";

	web_link_to_html_artikel_liste = "https://wplag.de/" + html_list_file_name;
	web_link_to_html_artikel_logs = "https://wplag.de/" + html_logs_file_name;

	File html_datei_list_in_webspace = new File(webspace_url + pfadtrenner + html_list_file_name);
	File html_datei_logs_in_webspace = new File(webspace_url + pfadtrenner + html_logs_file_name);

	MimeMultipart content = new MimeMultipart("mixed");

	MimeBodyPart htmlCo = new MimeBodyPart();

	String mainContent = "Hallo zusammen,<br/><br/> hier ist die aktuelle Analyse der Applikation: <a href=\""
		+ web_link_to_html_artikel_liste + "\" style=\"color:red; font-weight:bold;\" > oeffnen </a>";

	String insertAnalyseDatei = "INSERT INTO static_analyse_files(file_path,file_url,creationtime ) VALUES ('"
		+ html_datei_list_in_webspace.getAbsolutePath() + "','" + web_link_to_html_artikel_liste + "','"
		+ formatdb.format(new Date()) + "');";

	try {

	    db.insertToDatabase(insertAnalyseDatei, "insertAnalyseDatei");

	} catch (ClassNotFoundException e1) {

	    e1.printStackTrace();
	}

	try {

	    if (this.isLinux == true) {

		htmlCo.setContent(mainContent, "text/html; charset=\"utf-8\"");

		content.addBodyPart(htmlCo);

		Properties props = new Properties();

		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", db_system_mail_sender_host_name);
		props.put("mail.smtp.auth", true);
		props.put("mail.smtp.port", db_system_mail_sender_host_port);

		authentifizierung = new SMTPAuthenticator();

		session = Session.getDefaultInstance(props, authentifizierung);

		session.setDebug(false);

		html_email = new MimeMessage(session);

		html_email.setSubject("Datenanalyse vom: " + date2 + " ");

		html_email.setDescription("Datenanalyse vom: " + date2 + " ");

		html_email.setFrom(new InternetAddress("info@wplag.de", " WPLAG - Wilken Poelker"));

		html_email.setRecipient(Message.RecipientType.TO, new InternetAddress("info@graute-edv.de"));

		html_email.setRecipient(Message.RecipientType.BCC, new InternetAddress("pishaj.emiliano@gmail.com"));

		html_email.setHeader("SSL", "text/plain");

		html_email.setContent(content, "text/html");

		html_email.setHeader("MIME-Version", "1.0");
		html_email.setHeader("Content-Type", content.getContentType());
		html_email.setHeader("X-Mailer", "https://wplag.de");
		html_email.setSentDate(new Date());
		html_email.saveChanges();

		Transport.send(html_email);

		session.getTransport().close();

	    } else {

		DEBUGER.addMassage("Kein Mailsversand unter Windows moeglich!");
	    }

	} catch (MessagingException e) {

	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {

	    e.printStackTrace();
	}

	try (FileOutputStream oS = new FileOutputStream(html_datei_list_in_webspace))

	{
	    oS.write(html_content.toString().getBytes());

	    String link = "<a href=\"" + web_link_to_html_artikel_logs
		    + "\"  target=\"_blank\" style=\" position: relative;text-align: center;"
		    + " font: normal 19px/59% Courier New, Courier, monospace; "
		    + " border: 1px solid;  padding: 10px;  color: #000000;  "
		    + " margin: 0px auto;  float: right;\">logs</a>";

	    oS.write(link.getBytes());

	} catch (IOException e) {
	    e.printStackTrace();
	}

	try (FileOutputStream oS = new FileOutputStream(html_datei_logs_in_webspace))

	{
	    oS.write(DEBUGER.debugString.toString().getBytes());

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {

	public PasswordAuthentication getPasswordAuthentication() {

	    return new PasswordAuthentication("info@wplag.de", "6Dukg6*7");
	}
    }

}
