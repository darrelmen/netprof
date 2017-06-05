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

package mitll.langtest.server.rest;

import mitll.hlt.domino.server.util.ServletUtil;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

import static mitll.langtest.shared.user.LoginResult.ResultType.Failed;
import static mitll.langtest.shared.user.LoginResult.ResultType.Success;

/**
 * REST user management functions - add user, reset password, etc.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class RestUserManagement {
  private static final Logger logger = LogManager.getLogger(RestUserManagement.class);

  private static final String HAS_USER = "hasUser";
  private static final String FORGOT_USERNAME = "forgotUsername";

  /**
   * @see #doGet
   */
  private static final String RESET_PASS = "resetPassword";
  private static final String SET_PASSWORD = "setPassword";

  /**
   * Where is this used ???
   */
  private static final String EXPECTING_TWO_QUERY_PARAMETERS = "expecting two query parameters";
  private static final String ERROR = "ERROR";
  private static final String EXPECTING_ONE_QUERY_PARAMETER = "expecting one query parameter";
  private static final String EXISTING_USER_NAME = "ExistingUserName";
  private static final String USER = "user";

  //  private static final String PASSWORD_H = "passwordH";
//  private static final String FREE_TEXT_PASSWORD = "freeTextPassword";

  /**
   * @seex mitll.langtest.server.database.user.UserManagement#userExists
   * @see #addUser
   */
  private static final String EMAIL_H = "emailH";
  private static final String EMAIL = "email";
  /**
   * @seex mitll.langtest.server.database.user.UserManagement#userExists
   */
  private static final String USERID = "userid";
  /**
   * @seex mitll.langtest.server.database.user.UserManagement#userExists
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

  /**
   * @see #doGet
   * @see EmailHelper#resetPassword
   * @deprecated
   */
  public static final String RESET_PASSWORD_FROM_EMAIL = "rp";
  public static final String USERS = "users";

  private DatabaseImpl db;
  private ServerProperties serverProps;
  protected String configDir;
  private PathHelper pathHelper;

  public RestUserManagement(DatabaseImpl db,
                            ServerProperties serverProps,
                            PathHelper pathHelper) {
    this.db = db;
    this.serverProps = serverProps;
    this.pathHelper = pathHelper;
  }

  /**
   * Accepts queries - hasUser, forgotUsername, resetPassword, rp, setPassword
   * <p>
   * TODO : accept forgot user name without email arg
   *
   * @param request
   * @param response
   * @param queryString
   * @param toReturn
   * @param projid
   * @param passwordFromBody
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  public boolean doGet(HttpServletRequest request,
                       HttpServletResponse response,
                       String queryString,
                       JSONObject toReturn,
                       int projid,
                       String passwordFromBody) {
/*    if (queryString.startsWith(HAS_USER)) {
      String[] split1 = getParams(queryString);
      if (split1.length < 2) {
        toReturn.put(ERROR, "expecting two or three parameters");
      } else {
//        String freeTextPassword = request.getParameter("pass");
        if (passwordFromBody != null) logger.info("doGet got free text password " + passwordFromBody.length());
        gotHasUser(toReturn, split1, passwordFromBody, request, securityManager, projid);
      }
      return true;
    } else
      */
    if (queryString.startsWith(FORGOT_USERNAME)) {
      String[] split1 = getParams(queryString);
      if (split1.length != 1) {
        toReturn.put(ERROR, EXPECTING_ONE_QUERY_PARAMETER);
      } else {
        String first = split1[0];
        toReturn.put(VALID, forgotUsername(getArg(first)));
      }
      return true;
    } else if (queryString.startsWith(RESET_PASS)) {
      logger.warn("\n\n\n calling reset RESET_PASS? ");
      String[] split1 = getParams(queryString);
      if (split1.length != 2) {
        toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        String first = split1[0];
        String user = getArg(first);

        String second = split1[1];
        String emailFromDevice = getArg(second);//second.split("=")[1];
        String token = resetPassword(user, emailFromDevice, request.getRequestURL().toString());
        toReturn.put(TOKEN, token);
      }
      return true;
    } else if (queryString.startsWith(RESET_PASSWORD_FROM_EMAIL)) {

      logger.warn("\n\n\n calling reset password? ");
      String[] split1 = getParams(queryString);
      if (split1.length != 1) {
        toReturn.put(ERROR, EXPECTING_ONE_QUERY_PARAMETER);
      } else {
        String first = split1[0];
        String token = getArg(first);

        // OK the real person clicked on their email link
        response.setContentType("text/html");

        String rep = (getUserIDForToken(token) == -1) ?
            getHTML("Note : your password has already been reset. Please go back to NetProF.", "Password has already been reset") :
            getHTML("OK, your password has been reset. Please go back to NetProF and login.", "Password has been reset");
        reply(response, rep);
      }
      return true;
    } else if (queryString.startsWith(SET_PASSWORD)) {
      String[] split1 = getParams(queryString);
      if (split1.length != 2) {
        toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        String first = split1[0];
        String token = getArg(first);

        String second = split1[1];
        String passwordH = getArg(second);
        toReturn.put(VALID, changePFor(token, passwordH, getBaseURL(request)));
      }
      return true;
    } else if (queryString.equals(USERS)) {  // TODO - require a session
      toReturn.put(USERS, db.usersToJSON());
      return true;
    }
    return false;
  }

  @NotNull
  private String[] getParams(String queryString) {
    return queryString.split("&");
  }

  private String getArg(String first) {
    return first.split("=")[1];
  }

  private void reply(HttpServletResponse response, String x) {
    try {
      PrintWriter writer = response.getWriter();
      writer.println(x);
      writer.close();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Return enough information so we could create a new user from the json.
   *
   * @paramx toReturn
   * @paramx split1
   * @paramx projid
   * @see #doGet
   */
/*
  private void gotHasUser(JSONObject toReturn,
                          String[] split1,
                          String freeTextPassword,
                          HttpServletRequest request,
                          IUserSecurityManager securityManager,
                          int projid) {
    String first = split1[0];
    String user = first.split("=")[1];

    String second = split1[1];
    String[] split = second.split("=");
    String passwordH = split.length > 1 ? split[1] : "";
    // String freeTextPassword = split.length > 2 ? split[2] : "";

    if (freeTextPassword == null) {
      freeTextPassword = split.length > 2 ? split[2] : "";
    }
    if (!freeTextPassword.isEmpty()) {
      passwordH = freeTextPassword;
    }
    // logger.debug("gotHasUser user '" + user + "' pass '" + passwordH + "'");

    tryToLogin(toReturn, freeTextPassword, request, securityManager, projid, user);//, passwordH);
  }
*/
  public void tryToLogin(JSONObject toReturn,
                         String freeTextPassword,
                         HttpServletRequest request,
                         IUserSecurityManager securityManager,
                         int projid,
                         String user//,
//                          String passwordH
  ) {
    IUserDAO userDAO = db.getUserDAO();
    User userFound = userDAO.getUserByID(user);
    logger.debug("gotHasUser user " + user);// + "' pass '" + passwordH.length() + "' -> " + userFound);

    if (userFound == null) {
      toReturn.put(USERID, -1);
      toReturn.put(EMAIL_H, -1);
      toReturn.put(EMAIL, -1);
      toReturn.put(KIND, -1);
      toReturn.put(HAS_RESET, -1);
      toReturn.put(TOKEN, "");
      toReturn.put(PASSWORD_CORRECT, FALSE);
    } else {
      //User strictUserWithPass = userDAO.getStrictUserWithPass(user, passwordH);

      int userid = userFound.getID();
      toReturn.put(USERID, userid);
      // TODO : do we need to do something else here?
      toReturn.put(EMAIL_H, "");//userFound.getEmailHash());
      toReturn.put(EMAIL, userFound.getEmail());//userFound.getEmailHash());
      toReturn.put(KIND, userFound.getUserKind().toString());
      toReturn.put(HAS_RESET, userFound.hasResetKey());
      toReturn.put(TOKEN, userFound.getResetKey());

      // so we can tell if we need to collect more info, etc.
      LoginResult loginResult = loginUser(user, freeTextPassword, request, securityManager);
      toReturn.put("loginResult", loginResult.getResultType().name());

      if (loginResult.getResultType() == Success) {
        db.rememberProject(userid, projid);
        toReturn.put(PASSWORD_CORRECT, "TRUE");
      } else {
        toReturn.put(PASSWORD_CORRECT, FALSE);
      }
    }
  }

  public LoginResult loginUser(String userId,
                               String attemptedFreeTextPassword,
                               HttpServletRequest request,
                               IUserSecurityManager securityManager) {
    try {
      String remoteAddr = securityManager.getRemoteAddr(request);
      String userAgent = request.getHeader("User-Agent");
      // ensure a session is created.
      HttpSession session = createSession(request);
      logger.info("Login session " + session.getId() + " isNew=" + session.isNew());
      logger.info("loginUser : userid " + userId);// + " password '" + attemptedHashedPassword + "'");
      return securityManager.getLoginResult(userId, attemptedFreeTextPassword, remoteAddr, userAgent, session);
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
   * @param token
   * @return
   * @see #doGet
   */
  private long getUserIDForToken(String token) {
    User user = db.getUserDAO().getUserWithResetKey(token);
    return (user == null) ? -1 : user.getID();
  }

  /**
   * @param message
   * @return
   * @see #doGet
   */
  private String getHTML(String message, String title) {
    return "<html>" +
        "<head>" +
        "<title>" +
        title +
        "</title>" +
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
        "<p></p>" +
        "</span>" +
        "</h1>\n" +
        "      </td>\n" +
        "     </tr>" +

        "   <tr>\n" +
        "    <td style='padding:0in 0in 0in 0in'>\n" +
        "    <p>" +
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
   * If there's a valid user with that email - send them an email.
   *
   * @param email
   * @return
   */
  private boolean forgotUsername(String email) {
    // String hash = Md5Hash.getHash(email);
    String valid = db.getUserDAO().isValidEmail(email);
    if (valid != null) {
      getEmailHelper().getUserNameEmailDevice(email, valid);
      return true;
    } else {
      return false;
    }
  }

  private String resetPassword(String user, String email, String requestURL) {
    logger.warn("resetPassword for " + user + " and " + email);
    //String emailH = Md5Hash.getHash(email);
    Integer validUserAndEmail = db.getUserDAO().getIDForUserAndEmail(user, email);

    if (validUserAndEmail != null) {
      if (getEmailHelper().resetPassword(user, email, requestURL)) {
        return PASSWORD_EMAIL_SENT;
      } else {
        return ERROR;
      }
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
  private boolean changePFor(String userid, String freeTextPassword, String baseURL) {
    User userByID = db.getUserDAO().getUserByID(userid);

    //  freeTextPassword = rot13(freeTextPassword);
    //   String hash = Md5Hash.getHash(freeTextPassword);
    boolean b = db.getUserDAO().changePassword(userByID.getID(), freeTextPassword, baseURL);

    if (!b) {
      logger.error("changePFor : couldn't update user password for user " + userByID);
    }

    return b;
/*
    User userByID = db.getUserDAO().getUserWithResetKey(userid);
    if (userByID != null) {
      logger.debug("clearing key for " + userByID);
      db.getUserDAO().clearKey(userByID.getID(), true);

      if (!db.getUserDAO().changePassword(userByID.getID(), freeTextPassword)) {
        logger.error("couldn't update user password for user " + userByID);
      }
      return true;
    } else {
      logger.debug("NOT clearing key for " + userid);

      return false;
    }
*/
  }

  private String getBaseURL(HttpServletRequest r) {
    return ServletUtil.get().getBaseURL(r);
  }


  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), pathHelper);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
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
                      JSONObject jsonObject) {
    String user = request.getHeader(USER);

    //  String passwordH = request.getHeader(PASSWORD_H);
/*
    String freeTextPassword = request.getHeader(FREE_TEXT_PASSWORD);
    if (freeTextPassword == null) {
      freeTextPassword = passwordH;
    }
    */

    User existingUser = db.getUserDAO().getUserByID(user);

    logger.info("addUser user " + user + " match " + existingUser);
    int projid = -1;
    try {
      String project = request.getHeader("projid");
      if (project != null) {
        projid = Integer.parseInt(project); // TODO : figure out which project a user is in right now
      }
    } catch (NumberFormatException e) {
      logger.error("Got " + e, e);
    }

    if (existingUser == null) {
      User checkExisting = existingUser;// kinda stupid but here we are

      if (checkExisting == null) { // OK, nobody with matching user and password
        String age = request.getHeader(AGE);
        String gender = request.getHeader(GENDER);
        String dialect = request.getHeader(DIALECT);
        String emailH = request.getHeader(EMAIL_H);
        String email = request.getHeader(EMAIL);
        if (email == null) email = "";

        logger.debug("addUser : Request " + requestType + " for " + deviceType + " user " + user +
            " adding " + gender +
            " age " + age + " dialect " + dialect);

        User user1 = null;
        //String appURL = serverProps.getAppURL();
        if (age != null && gender != null && dialect != null) {
          try {
            int age1 = Integer.parseInt(age);
            boolean male = gender.equalsIgnoreCase("male");

            SignUpUser user2 = new SignUpUser(user,
                //                passwordH,
                emailH,
                email,
                User.Kind.CONTENT_DEVELOPER,
                male,
                male ? MiniUser.Gender.Male : MiniUser.Gender.Female,
                age1,
                dialect,
                deviceType,
                device,
                "",
                "",
                serverProps.getAppURL(),
                "OTHER");
//            user1 = getUserManagement().addUser(user, passwordH, emailH, email, deviceType, device,
//                User.Kind.CONTENT_DEVELOPER, male, age1, dialect);
            user1 = getUserManagement().addUser(user2);
          } catch (NumberFormatException e) {
            logger.warn("couldn't parse age " + age);
            jsonObject.put(ERROR, "bad age");
          }
        } else {
          user1 = addUserFromIPAD(request, deviceType, device, user, gender, emailH, email);
        }

        if (user1 == null) { // how could this happen?
          jsonObject.put(EXISTING_USER_NAME, "");
        } else {
          jsonObject.put(USERID, user1.getID());
        }

      } else {
        jsonObject.put(EXISTING_USER_NAME, "");
      }
    } else {
      logger.debug("addUser - found existing user for " + user +
          //" pass " + passwordH +
          " -> " + existingUser);

      if (existingUser.hasResetKey()) {
        jsonObject.put(ERROR, "password was reset");
      } else {
        jsonObject.put(USERID, existingUser.getID());
      }
    }
  }

  private User addUserFromIPAD(HttpServletRequest request,
                               String deviceType, String device,
                               String user, String gender, String emailH, String email) {
    User user1;
    String appURL = request.getRequestURL().toString().replaceAll(request.getServletPath(), "");

    logger.info("AppURL " + appURL);
    // String gender = request.getHeader(GENDER);
    String first = request.getHeader("first");
    String last = request.getHeader("last");
    String affiliation = request.getHeader("affiliation");

    boolean isMale = gender == null ? false : gender.equalsIgnoreCase("male");
    SignUpUser user2 = new SignUpUser(user,
        emailH, email,
        User.Kind.STUDENT,
        isMale,
        isMale ? MiniUser.Gender.Male : MiniUser.Gender.Female, 89, "", deviceType, device, first, last, appURL, affiliation);
    user1 = getUserManagement().addUser(user2);
    return user1;
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
    try {
      i = Integer.parseInt(user);
    } catch (NumberFormatException e) {
      logger.error("expecting a number for user id " + user);
    }
    return i;
  }
}
