package mitll.langtest.server.mail;

import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by go22670 on 9/30/14.
 */
public class EmailHelper {
  private static final Logger logger = Logger.getLogger(EmailHelper.class);

  private static final String NP_SERVER = "np.ll.mit.edu";
  private static final String MY_EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final String CLOSING = "Regards, Administrator";
  private static final String GORDON = "Gordon";

  private static final String RP = "rp";
  private static final String CD = "cd";
  private static final String ER = "er";

  private static final String PASSWORD_RESET = "Password Reset";
  private static final String RESET_PASSWORD = "Reset Password";
  private static final String REPLY_TO = "admin@" + NP_SERVER;

  private final String language;
  private final UserDAO userDAO;
  private final MailSupport mailSupport;
  private ServerProperties serverProperties;
  private PathHelper pathHelper;

  public EmailHelper(ServerProperties serverProperties, UserDAO userDAO, MailSupport mailSupport, PathHelper pathHelper) {
    this.language = serverProperties.getLanguage();
    this.serverProperties = serverProperties;
    this.userDAO = userDAO;
    this.mailSupport = mailSupport;
    this.pathHelper = pathHelper;
  }

  private String getHash(String toHash) {
    return StringUtils.toHexString(Md5Utils.getMd5Digest(toHash.getBytes()));
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#forgotUsername(String, String, String)
   * @param email
   * @param url
   * @param valid
   */
  public void getUserNameEmail(String email, String url, User valid) {
    url = trimURL(url);

    if (valid != null) {
      logger.debug("Sending user email...");
      String message = "Hi " + valid.getUserID() + ",<br/>" +
          "Your user name is " + valid.getUserID() + "." +
          "<br/><br/>" +
          CLOSING;
      sendEmail(url // baseURL
          ,
          email, // destination email
          "Your user name", // subject
          message,
          "Click here to return to the site." // link text
      );
    }
  }

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.UserPassLogin#getForgotPassword()
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user);

    User validUserAndEmail = userDAO.isValidUserAndEmail(user, getHash(email));

    if (validUserAndEmail != null) {
      logger.debug("resetPassword for " + user + " sending reset password email.");
      String toHash = user + "_" + System.currentTimeMillis();
      String hash = getHash(toHash);
      userDAO.addKey(validUserAndEmail.getId(), true, hash);

      String message = "Hi " + user + ",<br/><br/>" +
          "Click the link below to change your password." +
          "<br/><br/>" +
          CLOSING;

      url = trimURL(url);
      sendEmail(url + "?" + RP + "=" + hash,
          email,
          PASSWORD_RESET,
          message,
          RESET_PASSWORD // link text
      );

      //logger.debug("key map is " +keyToUser);
      return true;
    } else {
      logger.debug("couldn't find user " + user + " and email " + email);
      String message = "User " + user + " with email " + email + " tried to reset password - but they're not valid.";
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      logger.debug(prefixedMessage);
      mailSupport.email(serverProperties.getEmailAddress(), "Invalid password reset for " + serverProperties.getLanguage(), prefixedMessage);
      return false;
    }
  }

  private void sendEmail(String link, String to, String subject, String message, String linkText) {
    mailSupport.sendEmail(NP_SERVER,
        link,
        to,
        REPLY_TO,
        subject,
        message,
        linkText// link text
    );
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#handleCDToken
   */
  public String enableCDUser(String token, String emailR, String url) {
    User userWhereEnabledReq = userDAO.getUserWhereEnabledReq(token);
    Long userID;
    if (userWhereEnabledReq == null) {
      logger.debug("user id null for " + token);
      userID = null;
    } else {
      userID = userWhereEnabledReq.getId();
      logger.debug("user id '" + userID + "' for " + token + " vs " + userWhereEnabledReq.getId());
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

        logger.debug("Sending enable CD User email...");
        userID1 = userWhere.getUserID();
        sendUserApproval(url, email, userID1);

        // send ack to everyone, so they don't ahve to
        String subject = "Content Developer approved for " + userID1 + " for " + language;

        List<String> approvers = serverProperties.getApprovers();
        List<String> emails = serverProperties.getApproverEmails();

        for (int i = 0; i < approvers.size(); i++) {
          String tamas = approvers.get(i);
          String approvalEmailAddress = emails.get(i);

          String message = getApprovalAck(userID1, tamas);
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
            getApprovalAck(userID1, GORDON)
        );
      }
      return (b ? userID1 : null);
    }
  }

  private String getApprovalAck(String userID1, String tamas) {
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
   * @seex #enableCDEmail(String, String, mitll.langtest.shared.User)
   * @see #enableCDUser(String, String, String)
   */
  private void sendUserApproval(String url, String email, String userID1) {
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser(String, String, String, mitll.langtest.shared.User.Kind, String, String, boolean, int, String, boolean)
   */
  public void addContentDeveloper(String url, String email, User user, MailSupport mailSupport) {
    url = trimURL(url);
    String userID1 = user.getUserID();
    String toHash = userID1 + "_" + System.currentTimeMillis();
    String hash = getHash(toHash);
    userDAO.addKey(user.getId(), false, hash);
    List<String> approvers = serverProperties.getApprovers();
    List<String> emails = serverProperties.getApproverEmails();
    for (int i = 0; i < approvers.size(); i++) {
      String tamas = approvers.get(i);
      String approvalEmailAddress = emails.get(i);
      String message = getEmailApproval(userID1, tamas, email);
      sendApprovalEmail(url, email, userID1, hash, message, approvalEmailAddress, mailSupport);
    }
  }

  private void sendApprovalEmail(String url, String email, String userID1, String hash, String message, String approvalEmailAddress, MailSupport mailSupport) {
    mailSupport.sendEmail(NP_SERVER,
        url + "?" +
            CD +
            "=" + hash + "&" +  // content developer token
            ER +
            "=" + rot13(email), // email encoding
        approvalEmailAddress,
        serverProperties.getApprovalEmailAddress(),
        "Content Developer approval for " + userID1 + " for " + language,
        message,
        "Click to approve" // link text
    );
  }

  private String getEmailApproval(String userID1, String tamas, String email) {
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

}
