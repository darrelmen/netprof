package mitll.langtest.server.mail;

import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MailSupport {
  private static final String RECIPIENT_NAME = "Gordon Vidaver";
  private static final String DATA_COLLECT_WEBMASTER = "Data Collect Webmaster";
  private static final Logger logger = Logger.getLogger(MailSupport.class);
  private static final String EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final String LOCALHOST = "localhost";
  private static final int MAIL_PORT = 1025;
  private final boolean debugEmail;

  /**
   * @param debugEmail
   * @see mitll.langtest.server.LangTestDatabaseImpl#getMailSupport()
   */
  public MailSupport(boolean debugEmail) {

    this.debugEmail = true;


  }

  public void sendEmail(String serverName, String to, String replyTo, String subject, String message) {
    sendEmail(serverName, null, to, replyTo, subject, message, null);
  }

  /**
   * @param serverName
   * @param baseURL
   * @param to
   * @param replyTo
   * @param subject
   * @param message
   * @param linkText
   * @see mitll.langtest.server.LangTestDatabaseImpl#sendEmail(String, String, String, String, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser(String, String, String, mitll.langtest.shared.User.Kind, String, String)
   */
  public void sendEmail(String serverName, String baseURL, String to, String replyTo, String subject, String message, String linkText) {
    List<String> toAddresses = (to.contains(",")) ? Arrays.asList(to.split(",")) : new ArrayList<String>();
    if (toAddresses.isEmpty()) {
      toAddresses.add(to);
    }

    String body = getHTMLEmail(linkText, message, baseURL);

    String fromEmail = "admin@" + serverName;
    normalFullEmail(fromEmail, fromEmail, replyTo, toAddresses,
        subject,
        body);
  }

  private String getHTMLEmail(String linkText, String message, String link2) {
    String linkHTML = link2 != null && linkText != null ? "<a\n" + "      href=\"" +
        link2 +
        "\">" +
        "<span\n" + "      style='color:#004276'>" +
        linkText +
        "</span>" +
        "</a>" : "";

    String alternateCopyPaste = link2 != null && linkText != null ? "<span style='font-size:8.5pt;font-family:\"Arial\",\"sans-serif\";\n" +
        "    color:#333333'>Or, copy and paste this URL into your browser: <a\n" +
        "    href=\"" +
        link2 +
        "\"><b>" +
        "<span\n" +
        "    style='color:#004276'>" +
        link2 +
        "</span></b></a>" : "";

    return "<html>" +
        "<head>" +
        "</head>" +

        "<body lang=EN-US link=blue vlink=purple style='tab-interval:.5in'>" +
        "<div align=center>" +
        "<table>" +
        (message.length() > 0 ?
            "<tr>" +
                "    <td colspan=2 style='padding:.75pt .75pt .75pt .75pt'>\n" +
                "    <p ><span style='font-size:13.0pt;font-family:\"Georgia\",\"serif\";\n" +
                "    color:#333333'>" +
                message +
                "<p></p></span></p>\n" +
                "    </td>" +
                "</tr>" : "") +
        "     <tr >\n" +
        "      <td style='border:none;padding:10.5pt 10.5pt 10.5pt 10.5pt'>\n" +
        "      <h1 style='margin-top:0in;margin-right:0in;margin-bottom:3.0pt;\n" + "      margin-left:0in'>" +
        "<span style='font-size:12.5pt;font-family:\"Georgia\",\"serif\";\n" + "      font-weight:normal'>" +
        linkHTML +

        "<p></p>" +
        "</span>" +
        "</h1>\n" +
        "      </td>\n" +
        "     </tr>" +

        "   <tr>\n" +
        "    <td style='padding:0in 0in 0in 0in'>\n" +
        "    <p>" +

        alternateCopyPaste +

        "<p></p></span>" +
        "</p>\n" +
        "    </td>\n" +
        //     "    <td style='padding:.75pt .75pt .75pt .75pt'></td>\n" +
        "   </tr>" +

        "</table>" +
        "</div>" +
        "</body>" +
        "</html>";
  }

  /**
   * @param receiver
   * @param subject
   * @param message
   * @see mitll.langtest.server.LangTestDatabaseImpl#logAndNotifyServerException(Exception)
   * @see mitll.langtest.server.LangTestDatabaseImpl#logMessage(String)
   */
  public void email(String receiver, String subject, String message) {
    normalEmail(RECIPIENT_NAME, receiver, new ArrayList<String>(), subject, message, LOCALHOST);
  }

  private void normalEmail(String recipientName, String recipientEmail, List<String> ccEmails,
                           String subject, String message, String email_server) {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", email_server);
      props.put("mail.debug", "" + debugEmail);
      Session session = Session.getDefaultInstance(props, null);
      // logger.debug("sending email to " + recipientEmail);
      Message msg = makeMessage(session, recipientName, recipientEmail, ccEmails, subject, message);
      Transport.send(msg);
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.info("couldn't send email - no mail daemon? ");
      } else {
        logger.error("Couldn't send email to " + recipientEmail + ". Got " + e, e);
      }
    }
  }

  /**
   * @param senderName
   * @param senderEmail
   * @param recipientEmails
   * @param subject
   * @param message
   * @see #sendEmail(String, String, String, String, String, String, String)
   */
  private void normalFullEmail(String senderName,
                               String senderEmail,
                               String replyToEmail,
                               List<String> recipientEmails,

                               String subject, String message) {
    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", LOCALHOST);
      props.put("mail.debug", "" + debugEmail);

      // TODO : Remove me!
      if (debugEmail) {
        props.put("mail.smtp.port", MAIL_PORT);
        logger.debug("using port " + MAIL_PORT);
      }

      Session session = Session.getDefaultInstance(props, null);
      Message msg = makeHTMLMessage(session,
          senderName, senderEmail, replyToEmail, recipientEmails,
          subject, message);
      Transport.send(msg);
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.info("couldn't send email - no mail daemon? subj " + subject, e);
      } else {
        logger.error("Couldn't send email to " + recipientEmails + ". Got " + e, e);
      }
    }
  }

/*
  private void secureEmail(String recipientName, String recipientEmail, List<String> ccEmails,
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
*/

  private Message makeMessage(Session session,
                              String recipientName, String recipientEmail,
                              List<String> ccEmails,
                              String subject, String message) throws Exception {
    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(EMAIL, DATA_COLLECT_WEBMASTER));
    InternetAddress address = new InternetAddress(recipientEmail, recipientName);
    logger.debug("Sending to " + address);
    msg.addRecipient(Message.RecipientType.TO, address);
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

    logger.info("Sending from " + senderEmail + " " + senderName + " to " + recipientEmails + " sub " + subject + " " + message);
    msg.setFrom(new InternetAddress(senderEmail, senderName));
    for (String receiver : recipientEmails) {
      logger.info("\tSending  to " + receiver);
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