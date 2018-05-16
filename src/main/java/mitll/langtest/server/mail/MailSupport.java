/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.mail;

import com.sun.mail.util.MailConnectException;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.IReport;
import mitll.langtest.server.rest.RestUserManagement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static mitll.langtest.server.ServerProperties.DEFAULT_MAIL_FROM;

/**
 * * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class MailSupport {
  private static final Logger logger = LogManager.getLogger(MailSupport.class);

  private static final String NETPROF_ADMIN = "Netprof Admin";

  /**
   * @see #email
   */
  private static final int MAIL_PORT = 25;
  private static final int TEST_MAIL_PORT = 2525;
  private static final String MAIL_SMTP_HOST = "mail.smtp.host";
  private static final String MAIL_SMTP_PORT = "mail.smtp.port";
  private static final String MAIL_DEBUG = "mail.debug";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String TEXT_HTML = "text/html";// charset=UTF-8";
  public static final int TRIES = 3;

  private final boolean debugEmail;
  private final boolean testEmail;

  private final String mailServer;
  private final String mailFrom;

  public MailSupport(ServerProperties serverProps) {
    this(serverProps.isDebugEMail(),
        serverProps.isTestEmail(),
        serverProps.getMailServer(),
        serverProps.getMailFrom()
    );
  }

  /**
   * @param debugEmail
   * @param testEmail
   * @see mitll.langtest.server.LangTestDatabaseImpl#getMailSupport()
   * @see RestUserManagement#getMailSupport()
   */
  private MailSupport(boolean debugEmail, boolean testEmail, String mailServer, String mailFrom) {
    this.debugEmail = true;
    this.testEmail = testEmail;
    this.mailServer = mailServer;
    this.mailFrom = mailFrom;
    if (testEmail) {
      logger.warn("MailSupport --->using test email");
    }
  }

  /**
   * @param baseURL
   * @param to
   * @param replyTo
   * @param subject
   * @param message
   * @param linkText
   * @param ccEmails
   * @see EmailHelper#sendEmail
   */
  boolean sendEmail(String baseURL, String to, String replyTo, String subject, String message,
                    String linkText, Collection<String> ccEmails) {
    List<String> toAddresses = (to.contains(",")) ? Arrays.asList(to.split(",")) : new ArrayList<>();
    if (toAddresses.isEmpty()) {
      toAddresses.add(to);
    }

    return normalFullEmail(replyTo, replyTo, replyTo, ccEmails, toAddresses, subject, getHTMLEmail(linkText, message, baseURL));
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
   * @param receiver could be comma separated...
   * @param subject
   * @param message
   * @see mitll.langtest.server.LangTestDatabaseImpl#logAndNotifyServerException(Exception)
   * @see mitll.langtest.server.LangTestDatabaseImpl#logMessage
   * @see mitll.langtest.server.LangTestDatabaseImpl#sendEmail(String, String)
   */
  public void email(String receiver, String subject, String message) {
    if (receiver.contains(",")) {
      Arrays.asList(receiver.split(",")).forEach(rec -> sendEmail(subject, message, rec));
    } else {
      sendEmail(subject, message, receiver);
    }
  }

  private void sendEmail(String subject, String message, String rec) {
    normalEmail(rec, rec, new ArrayList<>(), subject, message, mailServer, testEmail, mailFrom, mailServer);
  }

  /**
   * @param receiver
   * @param subject
   * @param messageBody
   * @param toAttach
   * @param receiverName
   * @return
   * @see IReport#sendExcelViaEmail(MailSupport, List, List, List, PathHelper)
   */
  public boolean emailAttachment(String receiver, String subject, String messageBody, File toAttach, String receiverName) {
    Message message = new MimeMessage(getMailSession());

    try {
      String from = DEFAULT_MAIL_FROM;
      message.setFrom(getAddress(NETPROF_ADMIN, from));
      InternetAddress address = getAddress(receiverName, receiver);

      logger.info("emailAttachment sending to " + address + " at port " + MAIL_PORT + " via " + mailServer);

      message.addRecipient(Message.RecipientType.TO, address);
      message.setSubject(subject);

      addHeaders(message);

      // sets the multipart as message's content
      message.setContent(getMultipart(messageBody, toAttach, message));

      logger.info("emailAttachment sending..." +
          "\n\tto   " + receiver +
          "\n\tfrom " + from +
          "\n\tsub  " + subject);

      sendMessage(message);

      logger.info("emailAttachment sent" +
          "\n\tto   " + receiver +
          "\n\tfrom " + from +
          "\n\tsub  " + subject);

      return true;
    } catch (Exception e) {
      logger.error("Got " + e, e);

      return false;
    }
  }

  private void sendMessage(Message message) {
    sendMessage(message, TRIES);
  }

  private void sendMessage(Message message, int tries) {
    if (tries > 0) {
      try {
        logger.info("sendMessage about to send email (" + tries + ") :" +
            "\n\tmessage " + message
        //    +            "\n\tsession " + message.getSession()
        );

        long then = System.currentTimeMillis();

        Transport.send(message);
        long now = System.currentTimeMillis();
        logger.info("sendMessage sent (" + (now - then) + ") email " + message);
      } catch (MessagingException e) {
        logger.warn("sendMessage got " + e, e);
        if (e.getMessage().contains("421")) {
          sendMessage(message, --tries);
        }
      }
    }
  }


  private Multipart getMultipart(String messageBody, File toAttach, Message message) throws MessagingException, IOException {
    Multipart multipart = new MimeMultipart();

    {// creates body part for the message
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      String htmlEmail = getHTMLEmail(null, messageBody, null);
      messageBodyPart.setContent(htmlEmail, "text/html");
      // messageBodyPart.setText(htmlEmail, "utf-8", "text/html");

      // adds parts to the multipart
      multipart.addBodyPart(messageBodyPart);
    }

    // creates body part for the attachment
    {
      MimeBodyPart attachPart = new MimeBodyPart();
      attachPart.setContent(message, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      attachPart.attachFile(toAttach);

      multipart.addBodyPart(attachPart);
    }
    return multipart;
  }


  /**
   * @param recipientName
   * @param recipientEmail
   * @param ccEmails
   * @param subject
   * @param message
   * @param email_server
   * @param useTestPort
   * @param from
   * @param smtpHost
   * @see #email(String, String, String)
   */
  private void normalEmail(String recipientName,
                           String recipientEmail,
                           List<String> ccEmails,
                           String subject,
                           String message,
                           String email_server,
                           boolean useTestPort, String from, String smtpHost) {
    try {
      Message msg = makeMessage(
          getMailSession(email_server, useTestPort),
          recipientName,
          recipientEmail,
          ccEmails,
          subject,
          message,
          from, smtpHost);
      sendMessage(msg);
    } catch (MailConnectException e) {
      if (!useTestPort) {
        normalEmail(recipientName, recipientEmail, ccEmails, subject, message, email_server, true, from, smtpHost);
      } else {
        logger.error("Couldn't send email to " + recipientEmail + ". Got " + e, e);
      }
      // OK try with test email
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.warn("couldn't send email - no mail daemon (" + mailServer + ") " + "? " + e);
      } else {
        logger.error("Couldn't send email to " + recipientEmail + ". Got " + e, e);
      }
    }
  }

  @NotNull
  private Session getMailSession() {
    return getMailSession(mailServer, testEmail);
  }

  private Session getMailSession(String email_server, boolean testEmail) {
    return Session.getInstance(getMailProps(email_server, testEmail), null);
  }

  @NotNull
  private Properties getMailProps(String email_server, boolean useTestEmail) {
    Properties props = new Properties();
    props.put(MAIL_SMTP_HOST, email_server);
    props.put(MAIL_DEBUG, "" + debugEmail);

    logger.info("getMailProps : smtp host = " + email_server);
    logger.info("getMailProps : props     = " + props);

    if (useTestEmail) {
      props.put(MAIL_SMTP_PORT, "" + TEST_MAIL_PORT);
      logger.debug("getMailProps : using port " + TEST_MAIL_PORT);
    }
    return props;
  }

  /**
   * @param senderName
   * @param senderEmail
   * @param ccEmails
   * @param recipientEmails
   * @param subject
   * @param message
   * @see #sendEmail(String, String, String, String, String, String, Collection)
   */
  private boolean normalFullEmail(String senderName,
                                  String senderEmail,
                                  String replyToEmail,

                                  Collection<String> ccEmails,
                                  Collection<String> recipientEmails,

                                  String subject,
                                  String message) {
    try {
      Message msg = makeHTMLMessage(getMailSession(),
          senderName,
          senderEmail,
          replyToEmail, recipientEmails,
          ccEmails, subject, message);
      sendMessage(msg);
      return true;
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.warn("couldn't send email - no mail daemon? subj " + subject + " : " + e.getMessage());
      } else {
        logger.warn("Couldn't send email to " + recipientEmails + ". Got " + e, e);
      }
      return false;
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

  /**
   * @param session
   * @param recipientName
   * @param recipientEmail
   * @param ccEmails
   * @param subject
   * @param message
   * @param from
   * @param smtpHost
   * @return
   * @throws Exception
   * @see #normalEmail(String, String, List, String, String, String, boolean, String, String)
   */
  private Message makeMessage(Session session,
                              String recipientName,
                              String recipientEmail,
                              Collection<String> ccEmails,
                              String subject,
                              String message,
                              String from, String smtpHost) throws Exception {
    Message msg = new MimeMessage(session);
    configure(recipientName, recipientEmail, ccEmails, subject, message, msg, from, smtpHost);
    return msg;
  }

  private void configure(String recipientName,
                         String recipientEmail,
                         Collection<String> ccEmails,
                         String subject,
                         String messageBody,
                         Message message,
                         String from,
                         String smtp) throws MessagingException, UnsupportedEncodingException {
    InternetAddress address = getAddress(recipientName, recipientEmail);

    logger.info("email: " +
        "\n\tsending to " + address +
        "\n\tfrom       " + from +
        (ccEmails.isEmpty() ? "" : "\n\tcc         " + ccEmails) +
        "\n\tvia smtp   " + smtp +
        "\n\tat port    " + MAIL_PORT +
        "\n\tsubject    " + subject +
        "\n\tmessage    " + messageBody);

    message.addRecipient(Message.RecipientType.TO, address);
    addCC(ccEmails, message);
    message.setFrom(getAddress(NETPROF_ADMIN, from));

    message.setSubject(subject);
    message.setText(messageBody);

    addHeaders(message);
  }

  /**
   * @param session
   * @param senderName
   * @param senderEmail
   * @param replyToEmail
   * @param recipientEmails
   * @param ccEmails
   * @param subject
   * @param message
   * @return
   * @throws Exception
   * @see #normalFullEmail(String, String, String, Collection, Collection, String, String)
   */
  private Message makeHTMLMessage(Session session,
                                  String senderName,
                                  String senderEmail,
                                  String replyToEmail,
                                  Collection<String> recipientEmails,
                                  Collection<String> ccEmails,
                                  String subject,
                                  String message)
      throws MessagingException, UnsupportedEncodingException {
    logger.info("makeHTMLMessage sending" +
        "\n\tfrom " + senderEmail + " " + senderName +
        "\n\tto   " + recipientEmails +
        "\n\tsub  " + subject);

    Message msg = new MimeMessage(session);
    msg.setFrom(getAddress(senderName, senderEmail));

    for (String receiver : recipientEmails) {
      //logger.info("\tSending  to " + receiver);
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
    }
    addCC(ccEmails, msg);

    msg.setSubject(subject);
    //msg.setSentDate(new Date());
    msg.setText(message);

    addHeaders(msg);
    msg.addHeader(CONTENT_TYPE, TEXT_HTML);
    //logger.info("Session is " + session + " message " + msg);
    addReplyTo(replyToEmail, msg);
    return msg;
  }

  private void addHeaders(Message message) throws MessagingException {
    // message.addHeader("format", "flowed");
    // message.addHeader("Content-Transfer-Encoding", "8bit");
    message.setSentDate(new Date());
  }

  @NotNull
  private InternetAddress getAddress(String recipientName, String recipientEmail) throws UnsupportedEncodingException {
    return new InternetAddress(recipientEmail, recipientName);
  }

  private void addCC(Collection<String> ccEmails, Message msg) throws MessagingException {
    for (String ccEmail : ccEmails) {
      msg.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
    }
  }

  private void addReplyTo(String replyToEmail, Message msg) throws MessagingException {
    if (replyToEmail != null && replyToEmail.length() > 0) {
      Address[] replies = new Address[1];
      replies[0] = new InternetAddress(replyToEmail);
      msg.setReplyTo(replies);
    }
  }
}