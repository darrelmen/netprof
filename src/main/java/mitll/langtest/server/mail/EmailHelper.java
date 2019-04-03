/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.mail;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.services.OpenUserServiceImpl;

import java.util.Collections;
import java.util.List;

public class EmailHelper {
  //private static final Logger logger = LogManager.getLogger(EmailHelper.class);
  /**
   *
   */
  private static final String CLOSING = "Regards, Administrator";
  private static final String YOUR_USER_NAME = "Your user name";
  private static final String LOCALHOST = "127.0.0.1";

  private final MailSupport mailSupport;
  private final String replyTo;

  /**
   * @param serverProperties
   * @param mailSupport
   * @see RestUserManagement#getEmailHelper
   * @see OpenUserServiceImpl#getEmailHelper
   */
  public EmailHelper(ServerProperties serverProperties, MailSupport mailSupport) {
    this.mailSupport = mailSupport;
    replyTo = serverProperties.getMailReplyTo();
  }

/*
  private String getHash(String toHash) {
    return StringUtils.toHexString(Md5Utils.getMd5Digest(toHash.getBytes()));
  }
*/

  /**
   * @param email
   * @param url
   * @param userIDs
   * @see mitll.langtest.server.services.OpenUserServiceImpl#forgotUsername
   */
  public void getUserNameEmail(String email, String url, List<String> userIDs) {
    sendEmail(trimURL(url), // baseURL
        email, // destination email
        YOUR_USER_NAME, // subject
        getUserNameMessage(userIDs),
        "Click here to return to the site." // link text
    );
  }

  /**
   * @param email
   * @param userID
   * @see RestUserManagement#forgotUsername
   */
  public void getUserNameEmailDevice(String email, List<String> userID) {
    //logger.debug("Sending user email...");
    sendEmail(null // baseURL
        ,
        email, // destination email
        YOUR_USER_NAME, // subject
        getUserNameMessage(userID),
        null // link text
    );
  }

  private String getUserNameMessage(List<String> userIDs) {
    int size = userIDs.size();
    String message = size == 0 ? " could not be found" : size == 1 ? " is " + userIDs.get(0) : " choices are " + String.join(", ", userIDs);
    String salutation = size == 0 ? "Unknown User" : size == 1 ? userIDs.get(0) : (userIDs.get(0) + " etc.");
    return "Hi " + salutation + ",<br/>" +
        YOUR_USER_NAME +
        message + "." +
        "<br/><br/>" +
        CLOSING;
  }

  /**
   * @param link
   * @param to
   * @param subject
   * @param message
   * @param linkText
   * @seex #resetPassword
   * @see #getUserNameEmail
   */
  private void sendEmail(String link, String to, String subject, String message, String linkText) {
    mailSupport.sendEmail(
        link,
        to,
        replyTo,
        subject,
        message,
        linkText,
        Collections.emptyList());
  }

  private String trimURL(String url) {
    if (url.contains(LOCALHOST)) { // just for testing
      return "http://127.0.0.1:8888/LangTest.html?gwt.codesvr=127.0.0.1:9997";
    } else {
      return url.split("\\?")[0].split("\\#")[0];
    }
  }
}
