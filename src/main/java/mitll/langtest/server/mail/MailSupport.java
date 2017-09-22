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
import mitll.langtest.server.database.Report;
import mitll.langtest.server.rest.RestUserManagement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class MailSupport {
  private static final Logger logger = LogManager.getLogger(MailSupport.class);

  private static final String RECIPIENT_NAME = "Gordon Vidaver";
  private static final String DATA_COLLECT_WEBMASTER = "Data Collect Webmaster";
  private static final String EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final String LOCALHOST = "localhost";
  private static final int MAIL_PORT = 25;
  private static final int TEST_MAIL_PORT = 2525;
  private static final String MAIL_SMTP_HOST = "mail.smtp.host";
  private static final String MAIL_DEBUG = "mail.debug";
  private static final String MAIL_SMTP_PORT = "mail.smtp.port";
  private static final String TEXT_HTML = "text/html";
  private final boolean debugEmail;
  private final boolean testEmail;

  /**
   * @param debugEmail
   * @param testEmail
   * @see mitll.langtest.server.LangTestDatabaseImpl#getMailSupport()
   * @see RestUserManagement#getMailSupport()
   */
  public MailSupport(boolean debugEmail, boolean testEmail) {
    this.debugEmail = debugEmail;
    this.testEmail = false;
    if (testEmail) logger.warn("\n\n\n--->using test email");
  }

  /**
   * @param serverName
   * @param to
   * @param replyTo
   * @param subject
   * @param message
   * @see Report#sendEmails
   */
  public boolean sendEmail(String serverName, String to, String replyTo, String subject, String message) {
    return sendEmail(serverName, null, to, replyTo, subject, message, null, Collections.emptyList());
  }

  /**
   * @param serverName
   * @param baseURL
   * @param to
   * @param replyTo
   * @param subject
   * @param message
   * @param linkText
   * @param ccEmails
   * @see EmailHelper#sendEmail
   */
  boolean sendEmail(String serverName, String baseURL, String to, String replyTo, String subject, String message,
                    String linkText, Collection<String> ccEmails) {
    List<String> toAddresses = (to.contains(",")) ? Arrays.asList(to.split(",")) : new ArrayList<>();
    if (toAddresses.isEmpty()) {
      toAddresses.add(to);
    }

    String body = getHTMLEmail(linkText, message, baseURL);

    String fromEmail = "admin@" + serverName;
    return normalFullEmail(fromEmail, fromEmail, replyTo, ccEmails, toAddresses, subject, body);
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#logMessage
   * @see mitll.langtest.server.LangTestDatabaseImpl#sendEmail(String, String)
   */
  public void email(String receiver, String subject, String message) {
    normalEmail(RECIPIENT_NAME, receiver, new ArrayList<>(), subject, message, LOCALHOST, testEmail);
  }

  /**
   * @param receiver
   * @param subject
   * @param messageBody
   * @param toAttach
   * @param receiverName
   * @return
   * @see Report#sendExcelViaEmail(MailSupport, List, List, PathHelper, List)
   */
  public boolean emailAttachment(String receiver, String subject, String messageBody, File toAttach, String receiverName) {
    Message message = new MimeMessage(getMailSession(LOCALHOST, testEmail));

    try {
      message.setFrom(new InternetAddress(EMAIL, DATA_COLLECT_WEBMASTER));
      InternetAddress address = new InternetAddress(receiver, receiverName);
      logger.debug("makeMessage sending to " + address + " at port " + MAIL_PORT);
      message.addRecipient(Message.RecipientType.TO, address);
      // addCC(ccEmails, msg);
      message.setSubject(subject);
      //  message.setText(messageBody);
      message.setSentDate(new Date());

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

      // sets the multipart as message's content
      message.setContent(multipart);

      Transport.send(message);
      return true;
    } catch (Exception e) {
      logger.error("Got " + e, e);

      return false;
    }
  }

  /**
   * @param recipientName
   * @param recipientEmail
   * @param ccEmails
   * @param subject
   * @param message
   * @param email_server
   * @param useTestPort
   * @see #email(String, String, String)
   */
  private void normalEmail(String recipientName, String recipientEmail, List<String> ccEmails,
                           String subject, String message, String email_server, boolean useTestPort) {
    try {
      Transport.send(
          makeMessage(
              getMailSession(email_server, useTestPort),
              recipientName,
              recipientEmail,
              ccEmails,
              subject,
              message));
    } catch (MailConnectException e) {
      if (!useTestPort) {
        normalEmail(recipientName, recipientEmail, ccEmails, subject, message, email_server, true);
      } else {
        logger.error("Couldn't send email to " + recipientEmail + ". Got " + e, e);
      }
      // OK try with test email
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.info("couldn't send email - no mail daemon? ");
      } else {
        logger.error("Couldn't send email to " + recipientEmail + ". Got " + e, e);
      }
    }
  }

  private Session getMailSession(String email_server, boolean testEmail) {
    return Session.getInstance(getMailProps(email_server, testEmail), null);
  }

  @NotNull
  private Properties getMailProps(String email_server, boolean useTestEmail) {
    Properties props = new Properties();
    props.put(MAIL_SMTP_HOST, email_server);
    props.put(MAIL_DEBUG, "" + debugEmail);

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
   * @see #sendEmail(String, String, String, String, String, String, String, Collection)
   */
  private boolean normalFullEmail(String senderName,
                                  String senderEmail,
                                  String replyToEmail,

                                  Collection<String> ccEmails,
                                  Collection<String> recipientEmails,

                                  String subject,
                                  String message) {
    try {
      Transport.send(makeHTMLMessage(getMailSession(LOCALHOST, testEmail),
          senderName, senderEmail, replyToEmail, recipientEmails,
          ccEmails, subject, message));
      return true;
    } catch (Exception e) {
      if (e.getMessage().contains("Could not connect to SMTP")) {
        logger.warn("couldn't send email - no mail daemon? subj " + subject + " : " + e.getMessage());
      } else {
        logger.error("Couldn't send email to " + recipientEmails + ". Got " + e, e);
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
   * @return
   * @throws Exception
   * @see #normalEmail(String, String, List, String, String, String, boolean)
   */
  private Message makeMessage(Session session,
                              String recipientName,
                              String recipientEmail,
                              Collection<String> ccEmails,
                              String subject,
                              String message) throws Exception {
    Message msg = new MimeMessage(session);
    configure(recipientName, recipientEmail, ccEmails, subject, message, msg);

    return msg;
  }

  private void configure(String recipientName, String recipientEmail, Collection<String> ccEmails, String subject, String message, Message msg) throws MessagingException, UnsupportedEncodingException {
    msg.setFrom(new InternetAddress(EMAIL, DATA_COLLECT_WEBMASTER));
    InternetAddress address = new InternetAddress(recipientEmail, recipientName);
    logger.debug("makeMessage sending to " + address + " at port " + MAIL_PORT);
    msg.addRecipient(Message.RecipientType.TO, address);
    addCC(ccEmails, msg);
    msg.setSubject(subject);
    msg.setText(message);
    msg.setSentDate(new Date());
  }

  private void addCC(Collection<String> ccEmails, Message msg) throws MessagingException {
    for (String ccEmail : ccEmails) {
      msg.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
    }
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
                                  String subject, String message) throws Exception {
    logger.info("Sending from " + senderEmail + " " + senderName + " to " + recipientEmails + " sub " + subject);

    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(senderEmail, senderName));
    for (String receiver : recipientEmails) {
      //logger.info("\tSending  to " + receiver);
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
    }
    addCC(ccEmails, msg);
    msg.setSubject(subject);
    msg.setText(message);
    msg.addHeader("Content-Type", TEXT_HTML);
    //logger.info("Session is " + session + " message " + msg);
    addReplyTo(replyToEmail, msg);
    return msg;
  }

  private void addReplyTo(String replyToEmail, Message msg) throws MessagingException {
    if (replyToEmail != null && replyToEmail.length() > 0) {
      Address[] replies = new Address[1];
      replies[0] = new InternetAddress(replyToEmail);
      msg.setReplyTo(replies);
    }
  }
}