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

import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.rest.RestUserManagement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

import static mitll.langtest.server.rest.RestUserManagement.RESET_PASSWORD_FROM_EMAIL;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/30/14.
 */
public class EmailHelper {
  private static final Logger logger = LogManager.getLogger(EmailHelper.class);

  /**
   *
   */
  private static final String CLOSING = "Regards, Administrator";

  private static final String RP = RESET_PASSWORD_FROM_EMAIL;//"rp";
  private static final String PASSWORD_RESET = "Password Reset";
  private static final String RESET_PASSWORD = "Reset Password";
  private static final String YOUR_USER_NAME = "Your user name";
 // private static final String NETPROF_HELP_DLIFLC_EDU = "netprof-help@dliflc.edu";
  //private static final String HELP_EMAIL = "<a href='mailto:" + NETPROF_HELP_DLIFLC_EDU + "'>NetProF Help</a>";

  private static final String INVALID_PASSWORD_RESET = "Invalid password reset";

  private final IUserDAO userDAO;
  private final MailSupport mailSupport;
  private final ServerProperties serverProperties;
  private final PathHelper pathHelper;
  private final String REPLY_TO;
  private final String NP_SERVER;

  /**
   * @see RestUserManagement#getEmailHelper
   * @see UserServiceImpl#getEmailHelper
   * @param serverProperties
   * @param userDAO
   * @param mailSupport
   * @param pathHelper
   */
  public EmailHelper(ServerProperties serverProperties,
                     IUserDAO userDAO,
                     MailSupport mailSupport,
                     PathHelper pathHelper) {
    this.serverProperties = serverProperties;

    this.userDAO = userDAO;
    this.mailSupport = mailSupport;
    this.pathHelper = pathHelper;
    NP_SERVER = serverProperties.getNPServer();
    REPLY_TO = "admin@" + NP_SERVER;
  }

  private String getHash(String toHash) {
    return StringUtils.toHexString(Md5Utils.getMd5Digest(toHash.getBytes()));
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#forgotUsername
   * @param email
   * @param url
   * @param userID
   * @see mitll.langtest.server.services.UserServiceImpl#forgotUsername
   */
  public void getUserNameEmail(String email, String url, String userID) {
    url = trimURL(url);

    if (userID != null) {
      //logger.debug("Sending user email...");
      sendEmail(url // baseURL
          ,
          email, // destination email
          YOUR_USER_NAME, // subject
          getUserNameMessage(userID),
          "Click here to return to the site." // link text
      );
    }
    else {
      logger.warn("no user with email " + email);
    }
  }

  private String getUserNameMessage(String userID) {
    return "Hi " + userID + ",<br/>" +
        YOUR_USER_NAME +
        " is " + userID + "." +
        "<br/><br/>" +
        CLOSING;
  }

  public void getUserNameEmailDevice(String email, String userID) {
    //logger.debug("Sending user email...");
    sendEmail(null // baseURL
        ,
        email, // destination email
        YOUR_USER_NAME, // subject
        getUserNameMessage(userID),
        null // link text
    );
  }

  /**
   * TODO : - update this for domino
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.server.rest.RestUserManagement#resetPassword
   * @seex mitll.langtest.server.services.UserServiceImpl#resetPassword
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user + " url " + url);
    user = user.trim();
    email = email.trim();

    String hash1 = getHash(email);
    Integer validUserAndEmail = userDAO.getIDForUserAndEmail(user, hash1);

    if (validUserAndEmail != null) {
      logger.debug("resetPassword for " + user + " sending reset password email.");
      String toHash = user + "_" + System.currentTimeMillis();
      String hash = getHash(toHash);

      String message = "Hi " + user + ",<br/><br/>" +
          "Click the link below to reset your password." +
          "<br/>" +
          "In NetProF, click the login button to enter a new password." +
          "<br/>" +
          CLOSING;

      url = trimURL(url);
      if (!url.startsWith("https")) {
        url = url.replaceAll("http", "https");
      }
      logger.debug("resetPassword url is " + url);

      sendEmail(url + "?" + RP + "=" + hash,
          email,
          PASSWORD_RESET,
          message,
          RESET_PASSWORD // link text
      );

      return true;
    } else {
      logger.error("resetPassword couldn't find user " + user + " and email " + email + " " + hash1);

      String message = "User " + user + " with email " + email + " tried to reset password - but they're not valid.";
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;

      logger.debug(prefixedMessage);

      mailSupport.email(
          serverProperties.getEmailAddress(),
          INVALID_PASSWORD_RESET,
          prefixedMessage);
      return false;
    }
  }

  /**
   * @param link
   * @param to
   * @param subject
   * @param message
   * @param linkText
   * @seex #sendUserApproval(String, String, String, String)
   * @see #resetPassword(String, String, String)
   * @see #getUserNameEmail
   */
  private void sendEmail(String link, String to, String subject, String message, String linkText) {
    List<String> ccEmails = Collections.emptyList();
    mailSupport.sendEmail(NP_SERVER,
        link,
        to,
        REPLY_TO,
        subject,
        message,
        linkText,
        ccEmails);
  }

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) { // just for testing
      return "http://127.0.0.1:8888/LangTest.html?gwt.codesvr=127.0.0.1:9997";
    } else {
      return url.split("\\?")[0].split("\\#")[0];
    }
  }
}
