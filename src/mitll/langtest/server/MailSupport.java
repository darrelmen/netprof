package mitll.langtest.server;

import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailSupport {
  private static final String SWADE = "swade@ll.mit.edu";
  private static Logger logger = Logger.getLogger(MailSupport.class);
  private static final String EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final boolean DEBUG_MAIL = false;
  private Properties props;
  //private final LangTestDatabaseImpl langTestDatabaseImpl;
//  email = props.getProperty("email", EMAIL);

  public MailSupport(Properties props) {
    this.props = props;
  }

/*  public void sendEmail(String recipientName, String recipientEmail, List<String> ccEmails, String subject, String message, boolean includeSiteURL) {
    logger.debug("Began method: sendEmail(" + recipientName + ", " + recipientEmail + ", " + subject + ", " + message + ", " + includeSiteURL + ")");

    if (includeSiteURL) {
      message += "\n\nThe URL for the site is " + langTestDatabaseImpl.getServletContext().getInitParameter("siteurl");
    }

    try {
      String email_transport = langTestDatabaseImpl.getServletContext().getInitParameter("email_transport");
      String email_server    = langTestDatabaseImpl.getServletContext().getInitParameter("email_server");

      boolean secureP = email_transport.equals("smtps");
      if (secureP) {
        String email_username  = langTestDatabaseImpl.getServletContext().getInitParameter("email_username");
        String email_password  = langTestDatabaseImpl.getServletContext().getInitParameter("email_password");
        secureEmail(recipientName, recipientEmail, ccEmails, subject, message, email_transport, email_server, email_username, email_password);
      } else {
        normalEmail(recipientName, recipientEmail, ccEmails, subject, message, email_server);
      }
    } catch (Exception ex) {
      logger.error("Method: sendEmail(" + recipientName + ", " + recipientEmail + ", " + subject + ", " + message + ", " + includeSiteURL + ")", ex);
    }
  }*/


  public void email(String subject, String message) {
    normalEmail("Gordon Vidaver", EMAIL, new ArrayList<String>(),subject, message,"localhost");
  }

  public void email(String recipientName, String recipientEmail, String subject, String message) {
    normalEmail(recipientName, recipientEmail, new ArrayList<String>(),subject, message,"localhost");
  }

  public void ccemail(String recipientName, String recipientEmail, String subject, String message) {
    List<String> ccs = new ArrayList<String>();
    ccs.add(SWADE);
    normalEmail(recipientName, recipientEmail, ccs, subject, message,"localhost");
  }

  public void email(String recipientName, String recipientEmail, List<String> ccEmails, String subject, String message) {
    normalEmail(recipientName, recipientEmail, ccEmails, subject, message,"localhost");
  }

  private void normalEmail(String recipientName, String recipientEmail, List<String> ccEmails, String subject, String message, String email_server)/* throws Exception*/ {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", email_server);
      props.put("mail.debug", ""+DEBUG_MAIL);
      Session session = Session.getDefaultInstance(props, null);
      Message msg = makeMessage(session, recipientName, recipientEmail, ccEmails, subject, message);
      Transport.send(msg);
    } catch (Exception e) {
      logger.error("Couldn't send email to " +recipientEmail+". Got " +e,e);
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

  private Message makeMessage(Session session, String recipientName, String recipientEmail, List<String> ccEmails,
                              String subject, String message) throws Exception {
    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(EMAIL, "Data Collect Webmaster"));
    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail, recipientName));
    for (String ccEmail : ccEmails) {
      msg.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
    }
    msg.setSubject(subject);
    msg.setText(message);

    return msg;
  }
}