package STATISTIK_MANAGER;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

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

public class CheckDoppelteArtikel implements java.io.Serializable {

    private static final long serialVersionUID = -3301032071103729866L;

    String db_system_mail_sender_host_name = "wplag.de";
    String db_system_mail_sender_host_port = "25";
    Message html_email;
    Session session;
    Authenticator authentifizierung;

    DatenBankVerbindung db = new DatenBankVerbindung();

    String textList = "<b>Artikel die in Taifun doppelt sind:</b></br></br> <ul style=\" width: 650px;  \" > ";

    String alleDoppeltenDatenSaetzenSQL = "SELECT A.`interne_artikelnummer`, A.`guid_taifun`, A.`warengruppe` "
	    + " FROM `static_artikel` as A, `static_artikel` as B where A.interne_artikelnummer=B.interne_artikelnummer and A.guid_taifun<> B.guid_taifun;";

    public void start() throws SQLException {

	db.readFromDatabase(alleDoppeltenDatenSaetzenSQL, "alleDoppeltenDatenSaetzenSQL");

	while (db.rs.next()) {

	    textList = textList + "<li>- [<b>" + db.rs.getString(1) + "</b> ," + db.rs.getString(2) + " ,"
		    + db.rs.getString(3) + " ] </li>";

	}

	textList = textList
		+ "</ul> </br> </br><span style=\" width: 650px; color:red; \" >Diese E-Mail wurde vom System automatisch gesendet! </br> <b>Bitte schnell bearbeiten, da sonst die Statistik nicht funktioniert.</b></span>";

	try {

	    authentifizierung = new SMTPAuthenticator();

	    SimpleDateFormat formatlogfile2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	    String date2 = formatlogfile2.format(new Date());

	    MimeMultipart content = new MimeMultipart("mixed");

	    MimeBodyPart htmlCo = new MimeBodyPart();

	    htmlCo.setContent(textList, "text/html; charset=\"utf-8\"");

	    content.addBodyPart(htmlCo);

	    Properties props = new Properties();

	    props.put("mail.transport.protocol", "smtp");
	    props.put("mail.smtp.host", db_system_mail_sender_host_name);
	    props.put("mail.smtp.auth", true);
	    props.put("mail.smtp.port", db_system_mail_sender_host_port);

	    session = Session.getDefaultInstance(props, authentifizierung);

	    session.setDebug(false);

	    html_email = new MimeMessage(session);

	    html_email.setSubject("Doppelte Artikel in Taifun vom: " + date2 + " ");

	    html_email.setDescription("Doppelte Artikel vom: " + date2 + " ");

	    html_email.setFrom(new InternetAddress("xxxx", " xxx"));

	    html_email.setRecipient(Message.RecipientType.TO, new InternetAddress("xxx"));

	    html_email.setRecipient(Message.RecipientType.BCC, new InternetAddress("xxx"));

	    html_email.setHeader("SSL", "text/plain");

	    html_email.setContent(content, "text/html");

	    html_email.setHeader("MIME-Version", "1.0");
	    html_email.setHeader("Content-Type", content.getContentType());
	    html_email.setHeader("X-Mailer", "https://wplag.de");
	    html_email.setSentDate(new Date());
	    html_email.saveChanges();

	    Transport.send(html_email);

	    session.getTransport().close();

	} catch (MessagingException e) {

	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {

	    e.printStackTrace();
	}

    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {

	public PasswordAuthentication getPasswordAuthentication() {

	    return new PasswordAuthentication("xxxx", "xxxx");
	}
    }

}
