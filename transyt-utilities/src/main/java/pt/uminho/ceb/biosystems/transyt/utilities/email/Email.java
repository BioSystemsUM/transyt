package pt.uminho.ceb.biosystems.transyt.utilities.email;

import com.sun.mail.smtp.SMTPTransport;

import pt.uminho.ceb.biosystems.transyt.utilities.files.FilesUtils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class Email {
	
	private static final Logger logger = LoggerFactory.getLogger(Email.class);

	private static final String EMAIL_TEXT = "A new version of TranSyT data was compiled. \n Check the "
			+ "files at: https://merlin-sysbio.org/data/transyt/";

	public static void sendNotificationEmail() {
		
		logger.info("Sending notification email...");
		
		Map<String, String> credentials = FilesUtils.readMapFromFile("/conf/email_settings.conf");
		
		Properties prop = System.getProperties();
		prop.put("mail.smtp.host", credentials.get("SMTP_SERVER")); //optional, defined in SMTPTransport
		prop.put("mail.smtp.auth", Boolean.valueOf(credentials.get("SMTP_AUTHENTICATION")));
		prop.put("mail.smtp.port", "587"); // default port 25

		Session session = Session.getInstance(prop, null);
		Message msg = new MimeMessage(session);

		try {

			// from
			msg.setFrom(new InternetAddress(credentials.get("EMAIL_FROM")));

			// to 
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(credentials.get("EMAIL_TO"), false));

			// cc
			msg.setRecipients(Message.RecipientType.CC,
					InternetAddress.parse(credentials.get("EMAIL_TO_CC"), false));

			// subject
			msg.setSubject(credentials.get("EMAIL_SUBJECT"));

			// content 
			msg.setText(EMAIL_TEXT);

			msg.setSentDate(new Date());

			// Get SMTPTransport
			SMTPTransport t = (SMTPTransport) session.getTransport("smtp");

			// connect
			t.connect(credentials.get("SMTP_SERVER"), credentials.get("USERNAME"), credentials.get("PASSWORD"));

			// send
			t.sendMessage(msg, msg.getAllRecipients());

			logger.info("Response: " + t.getLastServerResponse());

			t.close();

		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}