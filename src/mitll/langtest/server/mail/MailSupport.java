package mitll.langtest.server.mail;

import org.apache.log4j.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailSupport {
  private static final String SWADE = "swade@ll.mit.edu";
  private static final String RECIPIENT_NAME = "Gordon Vidaver";
  private static final String DATA_COLLECT_WEBMASTER = "Data Collect Webmaster";
  private static Logger logger = Logger.getLogger(MailSupport.class);
  private static final String EMAIL = "gordon.vidaver@ll.mit.edu";
  private boolean debugEmail;

  public MailSupport(Properties props) {
    debugEmail = props.get("debugEmail") != null && !props.get("debugEmail").toString().equals("false");
  }

  public void email(String subject, String message) {
    normalEmail(RECIPIENT_NAME, EMAIL, new ArrayList<String>(),subject, message,"localhost");
  }

/*  public void email(String recipientName, String recipientEmail, String subject, String message) {
    normalEmail(recipientName, recipientEmail, new ArrayList<String>(),subject, message,"localhost");
  }

  public void ccemail(String recipientName, String recipientEmail, String subject, String message) {
    List<String> ccs = new ArrayList<String>();
    ccs.add(SWADE);
    normalEmail(recipientName, recipientEmail, ccs, subject, message,"localhost");
  }

  public void email(String recipientName, String recipientEmail, List<String> ccEmails, String subject, String message) {
    normalEmail(recipientName, recipientEmail, ccEmails, subject, message,"localhost");
  }*/

  private void normalEmail(String recipientName, String recipientEmail, List<String> ccEmails,
                           String subject, String message, String email_server) {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", email_server);
      props.put("mail.debug", ""+debugEmail);
      Session session = Session.getDefaultInstance(props, null);
      Message msg = makeMessage(session, recipientName, recipientEmail, ccEmails, subject, message);
      Transport.send(msg);
    } catch (Exception e) {
      logger.error("Couldn't send email to " +recipientEmail+". Got " +e,e);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#sendEmail
   * @param senderName
   * @param senderEmail
   * @param recipientEmails
   * @param subject
   * @param message
   */
  public void normalFullEmail(String senderName,
                              String senderEmail,
                              String replyToEmail,
                              List<String> recipientEmails,

                              String subject, String message) {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", "localhost");
      props.put("mail.debug", ""+debugEmail);
      Session session = Session.getDefaultInstance(props, null);
      Message msg = makeHTMLMessage(session,
        senderName, senderEmail, replyToEmail, recipientEmails,
        subject, message);
      Transport.send(msg);
    } catch (Exception e) {
      logger.error("Couldn't send email to " +recipientEmails+". Got " +e,e);
    }
  }

  public void secureEmail(String recipientName, String recipientEmail, List<String> ccEmails,
                           String subject, String message, String email_transport, String email_server,
                           String email_username, String email_password) throws Exception {
    Properties props = new Properties();
    props.put("mail.smtps.auth", "true");
    Session session = Session.getDefaultInstance(props, null);

    Message msg = makeMessage(session, recipientName, recipientEmail, ccEmails, subject, message);
    Transport transport = session.getTransport(email_transport);

    transport.connect(email_server, email_username, email_password);
    transport.sendMessage(msg, msg.getAllRecipients());
    transport.close();
  }

  private Message makeMessage(Session session,
                              String recipientName, String recipientEmail,
                              List<String> ccEmails,
                              String subject, String message) throws Exception {
    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(EMAIL, DATA_COLLECT_WEBMASTER));
    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail, recipientName));
    for (String ccEmail : ccEmails) {
      msg.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
    }
    msg.setSubject(subject);
    msg.setText(message);

    return msg;
  }

  private Message makeHTMLMessage(Session session,
                                  String senderName,
                                  String senderEmail,
                                  String replyToEmail,
                                  List<String> recipientEmails,
                                  String subject, String message) throws Exception {
    Message msg = new MimeMessage(session);

    logger.info("Sending from " + senderEmail + " " + senderName + " to " +recipientEmails + " sub " +subject + " " +message);
    msg.setFrom(new InternetAddress(senderEmail, senderName));
    for (String receiver : recipientEmails) {
      logger.info("\tSending  to " +receiver);
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
    }
    msg.setSubject(subject);
    msg.setText(message);
    msg.addHeader("Content-Type", "text/html");
    logger.info("Session is " + session + " message " + msg);
    addReplyTo(replyToEmail, msg);
    return msg;
  }

  private void addReplyTo(String replyToEmail, Message msg) throws MessagingException {
    if (replyToEmail != null && replyToEmail.length() > 0) {
      InternetAddress internetAddress = new InternetAddress(replyToEmail);
      Address[] replies = new Address[1];
      replies[0] = internetAddress;
      msg.setReplyTo(replies);
    }
  }
}