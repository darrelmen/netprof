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

package mitll.langtest.server.rest;

import com.google.gson.JsonObject;
import mitll.hlt.domino.server.util.ServletUtil;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.user.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

import static mitll.langtest.server.services.MyRemoteServiceServlet.USER_AGENT;
import static mitll.langtest.shared.user.LoginResult.ResultType.Failed;
import static mitll.langtest.shared.user.LoginResult.ResultType.Success;

public class RestUserManagement {
  private static final Logger logger = LogManager.getLogger(RestUserManagement.class);

  private static final String FORGOT_USERNAME = "forgotUsername";

  /**
   * called from ios : EAFForgotPasswordViewController.resetPassword
   *
   * @see #doGet
   */
  private static final String RESET_PASS = "resetPassword";
  private static final String SET_PASSWORD = "request=setPassword";

  /**
   * Where is this used ???
   */
  private static final String EXPECTING_TWO_QUERY_PARAMETERS = "expecting two query parameters";
  private static final String ERROR = "ERROR";
  private static final String EXPECTING_ONE_QUERY_PARAMETER = "expecting one query parameter";
  private static final String EXISTING_USER_NAME = "ExistingUserName";
  private static final String USER = "user";

  /**
   * @see #addUser
   */
  private static final String EMAIL_H = "emailH";
  private static final String EMAIL = "email";
  /**
   *
   */
  private static final String USERID = "userid";
  /**
   *
   */
  private static final String PASSWORD_CORRECT = "passwordCorrect";

  private static final String HAS_RESET = "hasReset";
  private static final String TOKEN = "token";
  private static final String PASSWORD_EMAIL_SENT = "PASSWORD_EMAIL_SENT";
  private static final String NOT_VALID = "NOT_VALID";
  private static final String FALSE = "false";

  private static final String VALID = "valid";
  private static final String AGE = "age";
  private static final String GENDER = "gender";
  private static final String DIALECT = "dialect";
  private static final String KIND = "kind";

  private static final String DLIFLC = "DLIFLC";
  private static final String MALE = "male";
  private static final String FIRST = "First";
  private static final String LAST = "Last";
  private static final String LOGIN_RESULT = "loginResult";
  private static final String TRUE = "TRUE";
  private static final String SUCCESS = "success";
  private static final String AFFILIATION = "affiliation";
  private static final String FIRST1 = "first";
  private static final String LAST1 = "last";
  /**
   * on iOS:
   *
   * @see EAFSignUpViewController.useJsonChapterData
   * @see #addUser(HttpServletRequest, String, String, String, JsonObject)
   */
  private static final String RESET_PASS_KEY = "resetPassKey";

  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  /**
   * @param db
   * @param serverProps
   * @see ScoreServlet#makeAudioFileHelper
   */
  public RestUserManagement(DatabaseImpl db, ServerProperties serverProps) {
    this.db = db;
    this.serverProps = serverProps;
  }

  /**
   * Accepts queries - hasUser, forgotUsername, resetPassword, rp, setPassword
   * <p>
   * TODO : accept forgot user name without email arg
   *
   * @param request
   * @param queryString
   * @param toReturn
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  public boolean doGet(HttpServletRequest request, String queryString, JsonObject toReturn) {
    if (queryString.startsWith(FORGOT_USERNAME)) {
      String[] split1 = getParams(queryString);
      if (split1.length != 1) {
        toReturn.addProperty(ERROR, EXPECTING_ONE_QUERY_PARAMETER);
      } else {
        String first = split1[0];
        toReturn.addProperty(VALID, forgotUsername(getArg(first)));
      }
      return true;
    } else if (queryString.startsWith(RESET_PASS)) {
      logger.info("doGet - calling reset " + queryString);
      String[] split1 = getParams(queryString);
      if (split1.length != 2) {
        toReturn.addProperty(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        resetPassword(getBaseURL(request), toReturn, split1);
      }
      return true;
    } else if (queryString.startsWith(SET_PASSWORD)) {
      String[] split1 = getParams(queryString);
      if (split1.length < 3) {
        toReturn.addProperty(ERROR, "expecting 3");
      } else {
        // request=setPassword&token=oaNLq6fnjnrfoVhstj6XtWdWY4iwsU&pass=domino22&userid=14480
        doSetPassword(getBaseURL(request), queryString, toReturn, split1);
      }
      return true;
    }
    return false;
  }

  private void resetPassword(String baseURL, JsonObject toReturn, String[] split1) {
    String user = getFirst(split1[0]);
    String optionalEmail = getArg(split1[1]);
    String token = resetPassword(user, baseURL, optionalEmail);//emailFromDevice, request.getRequestURL().toString());
    toReturn.addProperty(TOKEN, token);
  }

  /**
   * e.g. request=setPassword&token=oaNLq6fnjnrfoVhstj6XtWdWY4iwsU&pass=domino22&userid=14480
   *
   * @param baseURL
   * @param queryString
   * @param toReturn
   * @param split1
   */
  private void doSetPassword(String baseURL, String queryString, JsonObject toReturn, String[] split1) {
    logger.info("setPassword : req " + queryString);// + " pass " +passwordH);

    int i = 1;
    String token = getFirst(split1[i++]);
    String passwordH = getArg(split1[i++]);
    String userID = getArg(split1[i++]);
    if (token.isEmpty()) {
      toReturn.addProperty(ERROR, "no token in request - expecting it first");
    } else if (passwordH.isEmpty()) {
      toReturn.addProperty(ERROR, "no password in request - expecting it second");
    }
    if (userID.isEmpty()) {
      toReturn.addProperty(ERROR, "no user id in request - expecting it first");
    }
    int user = getUserID(userID);
    logger.info("setPassword : userid " + userID + " token " + token);// + " pass " +passwordH);
    toReturn.addProperty(VALID, changePFor(user, passwordH, baseURL, token));
  }

  private int getUserID(String userID) {
    int user = -1;

    try {
      user = Integer.parseInt(userID);
    } catch (NumberFormatException e) {
      logger.warn("couldn't parse " + userID);
    }
    return user;
  }

  private String getFirst(String first1) {
    return getArg(first1);
  }

  @NotNull
  private String[] getParams(String queryString) {
    return queryString.split("&");
  }

  private String getArg(String first) {
    String[] split = first.split("=");
    if (split.length == 2) {
      return split[1];
    } else {
      logger.warn("getArg couldn't get arg value from " + first + " expecting A=B");
      return "";
    }
  }

  /**
   * @param toReturn
   * @param request
   * @param securityManager
   * @param projid           if > 0 then we remember the project for this user
   * @param user
   * @param freeTextPassword
   * @param strictValidity
   * @see ScoreServlet#checkUserAndLogin
   */
  public void tryToLogin(JsonObject toReturn,
                         HttpServletRequest request,
                         IUserSecurityManager securityManager,
                         int projid,
                         String user,
                         String freeTextPassword,
                         boolean strictValidity) {
    IUserDAO userDAO = db.getUserDAO();
    User userFound = userDAO.getUserByID(user);
    logger.debug("tryToLogin user " + user);// + "' pass '" + passwordH.length() + "' -> " + userFound);

    if (userFound == null) {
      toReturn.addProperty(USERID, -1);
      toReturn.addProperty(EMAIL_H, -1);
      toReturn.addProperty(EMAIL, -1);
      toReturn.addProperty(KIND, -1);
      toReturn.addProperty(HAS_RESET, -1);
      toReturn.addProperty(TOKEN, "");
      toReturn.addProperty(PASSWORD_CORRECT, FALSE);
    } else {
      int userid = userFound.getID();
      toReturn.addProperty(USERID, userid);
      // TODO : do we need to do something else here?
      toReturn.addProperty(EMAIL_H, "");
      toReturn.addProperty(EMAIL, userFound.getEmail());
      toReturn.addProperty(KIND, userFound.getUserKind().toString());
      toReturn.addProperty(HAS_RESET, userFound.hasResetKey());
      toReturn.addProperty(TOKEN, userFound.getResetKey());

      // so we can tell if we need to collect more info, etc.
      LoginResult loginResult = loginUser(user, freeTextPassword, request, securityManager, strictValidity);
      LoginResult.ResultType resultType = loginResult.getResultType();

      if (resultType == Success && projid > 0) {
        db.rememberUsersCurrentProject(userid, projid);
      }

      // darrel wanted this.
      toReturn.addProperty("session", loginResult.getSessionID());
      toReturn.addProperty(LOGIN_RESULT, resultType.name());
      toReturn.addProperty(PASSWORD_CORRECT, (resultType == Success) ? TRUE : FALSE);
    }
  }

  private LoginResult.ResultType getLoginResultType(HttpServletRequest request, IUserSecurityManager securityManager,
                                                    String user, String freeTextPassword, boolean strictValidity) {
    LoginResult loginResult = loginUser(user, freeTextPassword, request, securityManager, strictValidity);
    return loginResult.getResultType();
  }

  /**
   * Largely a no-op if no change to user -> project association
   *
   * @param toReturn
   * @param userID
   * @param projid
   */
  public void setProjectForUser(JsonObject toReturn, int userID, int projid) {
    logger.debug("setProjectForUser user " + userID);
    db.getUserProjectDAO().setCurrentProjectForUser(userID, projid);
    toReturn.addProperty(SUCCESS, TRUE);
  }

  private LoginResult loginUser(String userId,
                                String attemptedFreeTextPassword,
                                HttpServletRequest request,
                                IUserSecurityManager securityManager,
                                boolean strictValidity) {
    try {
      String remoteAddr = securityManager.getRemoteAddr(request);
      String userAgent = request.getHeader(USER_AGENT);
      // ensure a session is created.
      HttpSession session = createSession(request);
      logger.info("loginUser :" +
          "\n\tLogin session " + session.getId() +
          "\n\tisNew=        " + session.isNew() +
          "\n\tuserid        " + userId +
          "\n\tremoteAddr    " + remoteAddr +
          "\n\tuserAgent     " + userAgent
      );
//      logger.info("loginUser : userid " + userId);// + " password '" + attemptedHashedPassword + "'");
      return securityManager.getLoginResult(userId, attemptedFreeTextPassword, remoteAddr, userAgent, session, strictValidity);
    } catch (Exception e) {
      logger.error("got " + e, e);
      //   logAndNotifyServerException(e);
      return new LoginResult(Failed);
    }
  }

  private HttpSession createSession(HttpServletRequest request) {
    return request.getSession(true);
  }

  /**
   * If there's a valid user with that email - send them an email.
   *
   * @param email
   * @return
   */
  private boolean forgotUsername(String email) {
    List<String> valid = db.getUserDAO().isValidEmail(email);
    if (valid != null) {
      getEmailHelper().getUserNameEmailDevice(email, valid);
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param user
   * @param request
   * @param optionalEmail
   * @return
   * @see #doGet(HttpServletRequest, String, JsonObject)
   */
  private String resetPassword(String user, String baseURL, String optionalEmail) {
    if (user.length() == 4) user = user + "_";
    else if (user.length() == 3) user = user + "__";

    logger.info("resetPassword for '" + user + "' opt email " + optionalEmail);

    if (db.getUserDAO().isKnownUser(user)) {
      db.getUserDAO().forgotPassword(user, baseURL, optionalEmail);
      return PASSWORD_EMAIL_SENT;
    } else {
      return NOT_VALID;
    }
  }

  /**
   * TODO : remove duplicate code - here and in UserService
   * <p>
   * Password reset used to have two steps, so userid here was a token...
   *
   * @param userid           NOTE - this used to be a token -
   * @param freeTextPassword
   * @return
   * @seex UserServiceImpl#changePFor
   * @see #doGet
   */
  private boolean changePFor(int userid, String freeTextPassword, String baseURL, String userKey) {
    User userByID = db.getUserDAO().getByID(userid);
    //  boolean b = db.getUserDAO().changePassword(userByID.getID(), freeTextPassword, baseURL);

    String email = userByID != null ? userByID.getEmail() : "";

    String userID = userByID != null ? userByID.getUserID() : "unknown";
    boolean result = db.getUserDAO().changePasswordForToken(userID, userKey, freeTextPassword, baseURL, email);

    if (!result) {
      logger.error("changePFor : couldn't update user password for user " + userByID);
    }

    return result;
  }

  private String getBaseURL(HttpServletRequest r) {
    return ServletUtil.get().getBaseURL(r);
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, getMailSupport());
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps);
  }

  /**
   * TODO:  This mainly to support appen?
   * <p>
   * TODO : pass in the project id from the iOS app.
   * TODOx : pass in the freetext password from the iOS app.
   * <p>
   * So - what can happen - either we have a user and password match, in which case adding a user is equivalent
   * to logging in OR we have an existing user with a different password, in which case either it's a different
   * person with the same userid attempt, or the same person making a password mistake...
   *
   * @param request
   * @param requestType
   * @param deviceType
   * @param device
   * @param jsonObject
   * @see mitll.langtest.server.ScoreServlet#doPost
   */
  public void addUser(HttpServletRequest request,
                      String requestType,
                      String deviceType,
                      String device,
                      JsonObject jsonObject) {
    String user = request.getHeader(USER);

    User existingUser = db.getUserDAO().getUserByID(user);

    logger.info("addUser user " + user + " match " + existingUser);
    //getProjID(request);
    if (existingUser == null) { // OK, nobody with matching user and password
      String age = request.getHeader(AGE);
      String gender = request.getHeader(GENDER);
      String dialect = request.getHeader(DIALECT);
      String emailH = request.getHeader(EMAIL_H);
      String email = request.getHeader(EMAIL);
      String affiliation1 = request.getHeader(AFFILIATION);
      if (affiliation1 == null) affiliation1 = "OTHER";
      if (email == null) email = "";

      logger.info("addUser : Request " + requestType +
          "\n\tfor    " + deviceType +
          "\n\tdevice " + device +
          "\n\tuser " + user +
          "\n\tadding " + gender +
          " age " + age + " dialect " + dialect);

      User user1 = null;

      if (age != null && gender != null && dialect != null) {
        // not really supported anymore
        user1 = doDataCollectSignUp(deviceType, device, jsonObject, user, age, gender, emailH, email, affiliation1, user1);
      } else {
        try {
          user1 = addUserFromIPAD(request, deviceType, device, user, gender, emailH, email);
        } catch (Exception e) {
          jsonObject.addProperty(ERROR, "got " + e.getMessage());
          logger.error("Got " + e, e);
        }
      }

      if (user1 == null) { // how could this happen?
        jsonObject.addProperty(EXISTING_USER_NAME, "");
      } else {
        jsonObject.addProperty(USERID, user1.getID());
        String resetKey = user1.getResetKey();
        if (resetKey.isEmpty()) {
          logger.warn("empty reset key?");
        }
        jsonObject.addProperty(RESET_PASS_KEY, resetKey);
      }

    } else {
      logger.info("addUser - found existing user for " + user + " -> " + existingUser);
      jsonObject.addProperty(EXISTING_USER_NAME, "");
    }
  }

/*
  private void getProjID(HttpServletRequest request) {
    int projid = -1;
    try {
      String project = request.getHeader("projid");
      if (project != null) {
        projid = Integer.parseInt(project); // TODO : figure out which project a user is in right now
      }
      logger.info("Got " + projid + " projid");

    } catch (NumberFormatException e) {
      logger.error("Got " + e, e);
    }
  }
*/

  private User doDataCollectSignUp(String deviceType, String device, JsonObject jsonObject, String user, String age, String gender, String emailH, String email, String affiliation1, User user1) {
    try {
      int age1 = Integer.parseInt(age);
      boolean male = gender.equalsIgnoreCase(MALE);

      SignUpUser user2 = new SignUpUser(user,
          emailH,
          email,
          Kind.CONTENT_DEVELOPER,
          male,
          male ? MiniUser.Gender.Male : MiniUser.Gender.Female,
          age1,
          deviceType,
          device,
          "",
          "",
          affiliation1);

      user1 = getUserManagement().addUser(user2);
    } catch (NumberFormatException e) {
      logger.warn("couldn't parse age " + age);
      jsonObject.addProperty(ERROR, "bad age");
    }
    return user1;
  }

  /**
   * @param request
   * @param deviceType
   * @param device
   * @param user
   * @param gender
   * @param emailH
   * @param email
   * @return
   */
  private User addUserFromIPAD(HttpServletRequest request,
                               String deviceType,
                               String device,
                               String user,
                               String gender,
                               String emailH,
                               String email) {
    String appURL = request.getRequestURL().toString().replaceAll(request.getServletPath(), "");

    logger.info("addUserFromIPAD AppURL " + appURL + " user " + user + " email '" + email + "' emailH " + emailH);
    String first = request.getHeader(FIRST1);
    if (first == null) first = FIRST;
    String last = request.getHeader(LAST1);
    if (last == null) last = LAST;
    String affiliation = request.getHeader(AFFILIATION);
    if (affiliation == null) {
      affiliation = DLIFLC;
    }

    boolean isMale = gender == null || gender.equalsIgnoreCase(MALE);

    SignUpUser user2 = new SignUpUser(user,
        emailH,
        email,
        Kind.STUDENT,
        isMale,
        isMale ? MiniUser.Gender.Male : MiniUser.Gender.Female, 89, deviceType, device, first, last, affiliation);

    return getUserManagement().addUser(user2);
  }

  private UserManagement getUserManagement() {
    return db.getUserManagement();
  }

  public int getUserFromParamWarnIfBad(String user) {
    int userid;
    try {
      userid = Integer.parseInt(user);
    } catch (NumberFormatException e) {
      logger.warn("getUserFromParamWarnIfBad couldn't parse event userid " + user);
      userid = -1;
    }
    return userid;
  }

  public int getUserFromParam(String user) {
    int i = -1;
    if (user != null) {
      try {
        i = Integer.parseInt(user);
      } catch (NumberFormatException e) {
        logger.warn("getUserFromParam expecting a number for user id " + user);
      }
    }
    return i;
  }
}
