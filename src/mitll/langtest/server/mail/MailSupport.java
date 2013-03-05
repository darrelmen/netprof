package mitll.langtest.server.mail;

import org.apache.log4j.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MailSupport {
  private static final String SWADE = "swade@ll.mit.edu";
  private static final String RECIPIENT_NAME = "Gordon Vidaver";
  private static final String DATA_COLLECT_WEBMASTER = "Data Collect Webmaster";
  private static Logger logger = Logger.getLogger(MailSupport.class);
  private static final String EMAIL = "gordon.vidaver@ll.mit.edu";
  private boolean debugEmail;

  public MailSupport(boolean debugEmail) {
    this.debugEmail = debugEmail;
  }

  public void sendEmail(String serverName, String baseURL, String to, String replyTo, String subject, String message, String token) {
    List<String> toAddresses = (to.contains(",")) ? Arrays.asList(to.split(",")) : new ArrayList<String>();
    if (toAddresses.isEmpty()) {
      toAddresses.add(to);
    }
//    logger.debug("server info " + getServletContext().getServerInfo());
    //  URI uri = new URI();
    //String serverName = getThreadLocalRequest().getServerName();
    logger.info("server name " + serverName);
/*
    String link = "\nHere's a link <a href='" + getBaseUrl() + "#" + URLEncoder.encode(token) +
      "'>" + linkTitle + "</a>.\n";
    //message += link;

    logger.info("link " +link);*/

    String link2 = baseURL + "#" + URLEncoder.encode(token);
    String body = "<html>" +
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
      "<span style='font-size:12.5pt;font-family:\"Georgia\",\"serif\";\n" +  "      font-weight:normal'>" +
      "<a\n" + "      href=\"" +
      link2 +
      "\">" +
      "<span\n" + "      style='color:#004276'>" +
      subject +
      "</span>" +
      "</a><p></p>" +
      "</span>" +
      "</h1>\n" +
      "      </td>\n" +
      "     </tr>" +

      "   <tr>\n" +
      "    <td style='padding:0in 0in 0in 0in'>\n" +
      "    <p>" +
      "<span style='font-size:8.5pt;font-family:\"Arial\",\"sans-serif\";\n" +
      "    color:#333333'>Or, copy and paste this URL into your browser: <a\n" +
      "    href=\"" +
      link2 +
      "\"><b>" +
      "<span\n" +
      "    style='color:#004276'>" +
      link2 +
      "</span></b></a>" +
      "<p></p></span>" +
      "</p>\n" +
      "    </td>\n" +
      //     "    <td style='padding:.75pt .75pt .75pt .75pt'></td>\n" +
      "   </tr>"+

      "</table>" +
      "</div>" +
      "</body>" +
      "</html>";

    String fromEmail = "email@" + serverName;
    normalFullEmail(fromEmail, fromEmail, replyTo, toAddresses,
      subject,
      body);
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