package mitll.langtest.server.rest;

import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.User;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * REST user management functions - add user, reset password, etc.
 * Created by go22670 on 10/21/15.
 */
public class RestUserManagement {
  private static final Logger logger = Logger.getLogger(RestUserManagement.class);

  private static final String HAS_USER = "hasUser";
  private static final String FORGOT_USERNAME = "forgotUsername";
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
  private static final String PASSWORD_H = "passwordH";

  /**
   * @see DatabaseImpl#userExists(HttpServletRequest, String, String)
   */
  public static final String EMAIL_H = "emailH";
  /**
   * @see DatabaseImpl#userExists(HttpServletRequest, String, String)
   */
  public static final String USERID = "userid";
  private static final String HAS_RESET = "hasReset";
  private static final String TOKEN = "token";

  /**
   * @see DatabaseImpl#userExists(HttpServletRequest, String, String)
   */
  public static final String PASSWORD_CORRECT = "passwordCorrect";
  private static final String PASSWORD_EMAIL_SENT = "PASSWORD_EMAIL_SENT";
  private static final String NOT_VALID = "NOT_VALID";
  private static final String FALSE = "false";

  private static final String VALID = "valid";
  private static final String AGE = "age";
  private static final String GENDER = "gender";
  private static final String DIALECT = "dialect";
  public static final String KIND = "kind";

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
   * @param request
   * @param response
   * @param queryString
   * @param toReturn
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  public boolean doGet(HttpServletRequest request, HttpServletResponse response, String queryString, JSONObject toReturn) {
    if (queryString.startsWith(HAS_USER)) {
      String[] split1 = queryString.split("&");
      if (split1.length != 2) {
        toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        gotHasUser(toReturn, split1);
      }
      return true;
    } else if (queryString.startsWith(FORGOT_USERNAME)) {
      String[] split1 = queryString.split("&");
      if (split1.length != 1) {
        toReturn.put(ERROR, EXPECTING_ONE_QUERY_PARAMETER);
      } else {
        String first = split1[0];
        String emailFromDevice = first.split("=")[1];
        boolean valid = forgotUsername(emailFromDevice);
        toReturn.put(VALID, valid);
      }
      return true;
    } else if (queryString.startsWith(RESET_PASS)) {
      String[] split1 = queryString.split("&");
      if (split1.length != 2) {
        toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        String first = split1[0];
        String user = first.split("=")[1];

        String second = split1[1];
        String emailFromDevice = second.split("=")[1];
        String token = resetPassword(user, emailFromDevice, request.getRequestURL().toString());
        toReturn.put(TOKEN, token);
      }
      return true;
    } else if (queryString.startsWith("rp")) {
      String[] split1 = queryString.split("&");
      if (split1.length != 1) {
        toReturn.put(ERROR, EXPECTING_ONE_QUERY_PARAMETER);
      } else {
        String first = split1[0];
        String token = first.split("=")[1];

        // OK the real person clicked on their email link
        response.setContentType("text/html");

        String rep = (getUserIDForToken(token) == -1) ?
            getHTML("Note : your password has already been reset. Please go back to NetProF.", "Password has already been reset") :
            getHTML("OK, your password has been reset. Please go back to NetProF and login.", "Password has been reset");
        reply(response, rep);
      }
      return true;
    } else if (queryString.startsWith(SET_PASSWORD)) {
      String[] split1 = queryString.split("&");
      if (split1.length != 2) {
        toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
      } else {
        String first = split1[0];
        String token = first.split("=")[1];

        String second = split1[1];
        String passwordH = second.split("=")[1];
        toReturn.put(VALID, changePFor(token, passwordH));
      }
      return true;
    }
    return false;
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
   * @param toReturn
   * @param split1
   * @see #doGet(HttpServletRequest, HttpServletResponse, String, JSONObject)
   */
  private void gotHasUser(JSONObject toReturn, String[] split1) {
    String first = split1[0];
    String user = first.split("=")[1];

    String second = split1[1];
    String[] split = second.split("=");
    String passwordH = split.length > 1 ? split[1] : "";

    logger.debug("gotHasUser user '" + user + "' pass '" + passwordH + "'");

    User userFound = db.getUserDAO().getUserByID(user);

    logger.debug("gotHasUser user '" + user + "' pass '" + passwordH + "' -> " + userFound);

    if (userFound == null) {
      toReturn.put(USERID, -1);
      toReturn.put(EMAIL_H, -1);
      toReturn.put(KIND, -1);
      toReturn.put(HAS_RESET, -1);
      toReturn.put(TOKEN, "");
      toReturn.put(PASSWORD_CORRECT, FALSE);
    } else {
      toReturn.put(USERID,    userFound.getId());
      toReturn.put(EMAIL_H,   userFound.getEmailHash());
      toReturn.put(KIND,      userFound.getUserKind().toString());
      toReturn.put(HAS_RESET, userFound.hasResetKey());
      toReturn.put(TOKEN,     userFound.getResetKey());
      toReturn.put(PASSWORD_CORRECT,
          userFound.getPasswordHash() == null ? FALSE :
              userFound.getPasswordHash().equalsIgnoreCase(passwordH));
    }
  }

  private long getUserIDForToken(String token) {
    User user = db.getUserDAO().getUserWhereResetKey(token);
    return (user == null) ? -1 : user.getId();
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

  private boolean forgotUsername(String email) {
    String emailH = Md5Hash.getHash(email);
    User valid = db.getUserDAO().isValidEmail(emailH);
    if (valid != null) {
      getEmailHelper().getUserNameEmailDevice(email, valid);
      return true;
    } else {
      return false;
    }
  }

  private String resetPassword(String user, String email, String requestURL) {
    logger.debug(serverProps.getLanguage() + " resetPassword for " + user);
    String emailH = Md5Hash.getHash(email);
    User validUserAndEmail = db.getUserDAO().isValidUserAndEmail(user, emailH);

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

  private boolean changePFor(String token, String passwordH) {
    User userWhereResetKey = db.getUserDAO().getUserWhereResetKey(token);
    if (userWhereResetKey != null) {
      logger.debug("clearing key for " + userWhereResetKey);
      db.getUserDAO().clearKey(userWhereResetKey.getId(), true);

      if (!db.getUserDAO().changePassword(userWhereResetKey.getId(), passwordH)) {
        logger.error("couldn't update user password for user " + userWhereResetKey);
      }
      return true;
    } else {
      logger.debug("NOT clearing key for " + token);

      return false;
    }
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), pathHelper);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  /**
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
  public void addUser(HttpServletRequest request, String requestType, String deviceType, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);
    String passwordH = request.getHeader(PASSWORD_H);

    // first check if user exists already with this password -- if so go ahead and log them in.
    User exactMatch = db.getUserDAO().getUserWithPass(user, passwordH);

    logger.info("addUser user " + user + " pass "+ passwordH + " match " + exactMatch);

    if (exactMatch == null) {
      User checkExisting = db.getUserDAO().getUserByID(user);

      if (checkExisting == null) { // OK, nobody with matching user and password
        String age = request.getHeader(AGE);
        String gender = request.getHeader(GENDER);
        String dialect = request.getHeader(DIALECT);
        String emailH = request.getHeader(EMAIL_H);

        logger.debug("addUser : Request " + requestType + " for " + deviceType + " user " + user +
            " adding " + gender +
            " age " + age + " dialect " + dialect);

        User user1 = null;
        if (age != null && gender != null && dialect != null) {
          try {
            int age1 = Integer.parseInt(age);
            user1 = db.addUser(user, passwordH, emailH, deviceType, device, User.Kind.CONTENT_DEVELOPER, gender.equalsIgnoreCase("male"), age1, dialect);
          } catch (NumberFormatException e) {
            logger.warn("couldn't parse age " + age);
            jsonObject.put(ERROR, "bad age");
          }
        } else {
          user1 = db.addUser(user, passwordH, emailH, deviceType, device);
        }

        if (user1 == null) { // how could this happen?
          jsonObject.put(EXISTING_USER_NAME, "");
        } else {
          jsonObject.put(USERID, user1.getId());
        }

      } else {
        jsonObject.put(EXISTING_USER_NAME, "");
      }
    } else {
      logger.debug("addUser - found existing user for " + user + " pass " + passwordH + " -> " + exactMatch);

      if (exactMatch.hasResetKey()) {
        jsonObject.put(ERROR, "password was reset");
      } else {
        jsonObject.put(USERID, exactMatch.getId());
      }
    }
  }

  public long getUserFromParam2(String user) {
    long userid;
    try {
      userid = Long.parseLong(user);
    } catch (NumberFormatException e) {
      logger.warn("couldn't parse event userid " + user);
      userid = -1;
    }
    return userid;
  }
/*
  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
  }*/

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
