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
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/30/14.
 */
public class EmailHelper {
  private static final Logger logger = LogManager.getLogger(EmailHelper.class);

  @Deprecated  private static final String MY_EMAIL = "gordon.vidaver@ll.mit.edu";
  /**
   *
   */
  private static final String CLOSING = "Regards, Administrator";
  @Deprecated  private static final String GORDON = "Gordon";

  private static final String RP = "rp";
  private static final String CD = "cd";
  private static final String ER = "er";

  private static final String PASSWORD_RESET = "Password Reset";
  private static final String RESET_PASSWORD = "Reset Password";
  // private static final String REPLY_TO = "admin@" + NP_SERVER;
  private static final String YOUR_USER_NAME = "Your user name";
  private static final String NETPROF_HELP_DLIFLC_EDU = "netprof-help@dliflc.edu";

  private static final String HELP_EMAIL = "<a href='mailto:" + NETPROF_HELP_DLIFLC_EDU + "'>NetProF Help</a>";
  private static final String USER_CONF_FIRST_LINE = "You are now a user of NetProF.<br/>";
  private static final String USER_CONF_SECOND_LINE = "If you have any questions, see the user manual or email " +
      HELP_EMAIL + ".";

  private final IUserDAO userDAO;
  private final MailSupport mailSupport;
  private ServerProperties serverProperties;
  private PathHelper pathHelper;
  private String REPLY_TO, NP_SERVER;

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

  public String getHash(String toHash) {
    return StringUtils.toHexString(Md5Utils.getMd5Digest(toHash.getBytes()));
  }

  /**
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
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.server.rest.RestUserManagement#resetPassword
   * @see mitll.langtest.server.services.UserServiceImpl#resetPassword
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug(" resetPassword for " + user + " url " + url);

    user = user.trim();
    email = email.trim();

    String hash1 = getHash(email);
    Integer validUserAndEmail = userDAO.getIDForUserAndEmail(user, hash1);

    if (validUserAndEmail != null) {
      logger.debug("resetPassword for " + user + " sending reset password email.");
      String toHash = user + "_" + System.currentTimeMillis();
      String hash = getHash(toHash);
      if (!userDAO.updateKey(validUserAndEmail, true, hash)) {
        logger.error("huh? couldn't add the reset password key to user id = " + validUserAndEmail);
      }

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
      logger.debug("url is " + url);
      sendEmail(url + "?" + RP + "=" + hash,
          email,
          PASSWORD_RESET,
          message,
          RESET_PASSWORD // link text
      );

      return true;
    } else {
      logger.error(" couldn't find user " + user + " and email " + email + " " + hash1);
      String message = "User " + user + " with email " + email + " tried to reset password - but they're not valid.";
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      logger.debug(prefixedMessage);
      mailSupport.email(serverProperties.getEmailAddress(),
          "Invalid password reset", prefixedMessage);
      return false;
    }
  }

  /**
   * @param link
   * @param to
   * @param subject
   * @param message
   * @param linkText
   * @see #sendUserApproval(String, String, String, String)
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

  /**
   * We're going to do user management in domino.
   *
   * @param token
   * @param language
   * @return
   * @seex mitll.langtest.client.LangTest#handleCDToken
   * @see mitll.langtest.server.services.UserServiceImpl#enableCDUser(String, String, String)
   */
  @Deprecated  public String enableCDUser(String token, String emailR, String url, String language) {
    User userWhereEnabledReq = userDAO.getUserWithEnabledKey(token);
    Integer userID;
    if (userWhereEnabledReq == null) {
      logger.debug("enableCDUser user id null for token " + token + " email " + emailR + " url " + url);
      userID = null;
    } else {
      userID = userWhereEnabledReq.getID();
      logger.debug("enableCDUser user id '" + userID + "' for " + token + " vs " + userWhereEnabledReq.getID());
    }
    String email = rot13(emailR);

    if (userID == null) {
      return null;
    } else {
      boolean b = userDAO.enableUser(userID);
      String userID1 = null;
      if (b) {
        userDAO.clearKey(userID, false);

        User userWhere = userDAO.getUserWhere(userID);
        url = trimURL(url);

        logger.debug("Sending enable CD User email for " + userID + " and " + userWhere);
        userID1 = userWhere.getUserID();
        sendUserApproval(url, email, userID1, language);

        // send ack to everyone, so they don't ahve to
        String subject = "Content Developer approved for " + userID1;

        List<String> approvers = serverProperties.getApprovers();
        List<String> emails = serverProperties.getApproverEmails();

        for (int i = 0; i < approvers.size(); i++) {
          String tamas = approvers.get(i);
          String approvalEmailAddress = emails.get(i);

          String message = getApprovalAck(userID1, tamas, language);
          mailSupport.sendEmail(NP_SERVER,
              approvalEmailAddress,
              MY_EMAIL,
              subject,
              message
          );
        }
        mailSupport.sendEmail(NP_SERVER,
            MY_EMAIL,
            MY_EMAIL,
            subject,
            getApprovalAck(userID1, GORDON, language)
        );
      } else {
        logger.debug("NOT sending enable CD User email for " + userID);
      }
      return (b ? userID1 : null);
    }
  }

  private String getApprovalAck(String userID1, String tamas, String language) {
    return "Hi " +
        tamas + ",<br/><br/>" +
        "User '" + userID1 +
        "' is now an active content developer for " + language +
        "." + "<br/><br/>" +
        CLOSING;
  }

  /**
   * @param url
   * @param email
   * @param userID1
   * @param language
   * @seex #enableCDEmail(String, String, mitll.langtest.shared.user.User)
   * @see #enableCDUser(String, String, String, String)
   */
  private void sendUserApproval(String url, String email, String userID1, String language) {
    String message = "Hi " + userID1 + ",<br/>" +
        "You have been approved to be a content developer for " + language + "." +
        "<br/>Click on the link below to log in." +
        "<br/><br/>" +
        CLOSING;

    sendEmail(url, // baseURL
        email, // destination email
        "Account approved", // subject
        message,
        "Click here to return to the site." // link text
    );
  }

  /**
   * User needs to be approved before account is activated.
   *
   * @param url
   * @param email
   * @param user
   * @param mailSupport
   * @param language
   * @see mitll.langtest.server.services.UserServiceImpl#addUser
   * @seex mitll.langtest.client.user.UserPassLogin#gotSignUp
   */
  @Deprecated public void addContentDeveloper(String url, String email, User user, MailSupport mailSupport, String language) {
    url = trimURL(url);
    String userID1 = user.getUserID();
    String toHash = userID1 + "_" + System.currentTimeMillis();
    String hash = getHash(toHash);
    if (!userDAO.updateKey(user.getID(), false, hash)) {
      logger.error("huh? couldn't add the CD update key to " + user);
    }
    List<String> approvers = serverProperties.getApprovers();
    List<String> emails = serverProperties.getApproverEmails();
    for (int i = 0; i < approvers.size(); i++) {
      String message = getEmailApproval(userID1, approvers.get(i), email, language);
      sendApprovalEmail(url, email, userID1, hash, message, emails.get(i), mailSupport, language);
    }
  }

  private void sendApprovalEmail(String url, String email, String userID1, String hash, String message,
                                 String approvalEmailAddress, MailSupport mailSupport, String language) {
    String baseURL = url + "?" +
        CD +
        "=" + hash + "&" +  // content developer token
        ER +
        "=" + rot13(email);

    mailSupport.sendEmail(NP_SERVER,
        baseURL, // email encoding
        approvalEmailAddress,
        serverProperties.getApprovalEmailAddress(),
        "Content Developer approval for " + userID1 + " for " + language,
        message,
        "Click to approve", // link text
        Collections.singleton(EmailList.RAY_BUDD));
  }

  private String getEmailApproval(String userID1, String tamas, String email, String language) {
    return "Hi " +
        tamas + ",<br/><br/>" +
        "User <b>" + userID1 +
        "</b> with email <b>" + email + "</b><br/>" +
        " would like to be a content developer for " + language +
        "." + "<br/>" +

        "Click the link to allow them." +
        "<br/><br/>" +
        CLOSING;
  }

  /**
   * So the first part in the url is the desired role, the second is the lookup key for the invitation
   *
   * @param url
   * @param email
   * @param inviter
   * @param atRole
   * @param inviteKey
   * @param mailSupport
   */
  public void sendInviteEmail(String url,
                               String email,
                               User inviter,
                               User.Kind atRole,
                              // int inviteID,
                               String inviteKey,
                               MailSupport mailSupport) {
    url = trimURL(url);
    String hash = getHash(atRole.toString());

    String baseURL = url + "?" +
        "k" +
        "=" + hash + "&" +  // content developer token
        "i" +
        "=" + inviteKey;//getHash(email + "_" + inviteID);

    String message = getInvitation(inviter.getFullName());

    mailSupport.sendEmail(NP_SERVER,
        baseURL, // email encoding
        email,
        NETPROF_HELP_DLIFLC_EDU,
        "Invitation to NetProF from " + inviter.getFullName(),
        message,
        "Click to sign up", // link text
        Collections.singleton(EmailList.GORDON_VIDAVER));
  }

  private String getInvitation(String inviterFullName) {
    return "Hi," +
        //tamas + "," +
        "<br/><br/>" +
        inviterFullName + " has kindly invited you to use " +"NetProF"+"."+
        "<br/><br/>" +
        "Click the link to sign up." +
        "<br/><br/>" +
        CLOSING;
  }

  /**
   * @param email
   * @param userID1
   * @param firstName
   * @param mailSupport
   */
  public void sendConfirmationEmail(String email, String userID1, String firstName, MailSupport mailSupport) {
    mailSupport.sendEmail(NP_SERVER, email, NETPROF_HELP_DLIFLC_EDU, "Welcome to NetProF", getUserConfirmationEmail(userID1, firstName));
  }

  private String getUserConfirmationEmail(String userID1, String firstName) {
    return "Hi " +
        firstName + ",<br/><br/>" +
        "Your user id is " + userID1 + ".<br/>" +
        USER_CONF_FIRST_LINE +
        USER_CONF_SECOND_LINE +
        "<br/><br/>" +
        CLOSING;
  }

  private String getHelpEmail() {
    return HELP_EMAIL;
  }

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) { // just for testing
      return "http://127.0.0.1:8888/LangTest.html?gwt.codesvr=127.0.0.1:9997";
    } else {
      return url.split("\\?")[0].split("\\#")[0];
    }
  }

  private String rot13(String val) {
    StringBuilder builder = new StringBuilder();
    for (char c : val.toCharArray()) {
      if (c >= 'a' && c <= 'm') c += 13;
      else if (c >= 'A' && c <= 'M') c += 13;
      else if (c >= 'n' && c <= 'z') c -= 13;
      else if (c >= 'N' && c <= 'Z') c -= 13;
      builder.append(c);
    }
    return builder.toString();
  }

  /**
   * @see mitll.langtest.server.services.UserServiceImpl#changePassword(int, String, String)
   * @param userWhereResetKey
   */
  public void sendChangedPassword(User userWhereResetKey) {
    String email = userWhereResetKey.getEmail();
    if (email != null && !email.isEmpty()) {
      String first = userWhereResetKey.getFirst();
      String userID = userWhereResetKey.getUserID();
      String greeting = first.isEmpty() ? userID :
          first;
      String message = "Hi " + greeting + ",<br/>" +
          "Your password for your NetProF account" + (first.isEmpty() ? "" : " " + userID) +
          " has changed. If this is in error, please contact the help email at " + getHelpEmail() + "." +
          "<br/>Click on the link below to log in." +
          "<br/><br/>" +
          CLOSING;

      sendEmail(NP_SERVER, // baseURL
          email, // destination email
          "Password Changed for " + userID, // subject
          message,
          "Click here to return to the site." // link text
      );
    }
  }
}
