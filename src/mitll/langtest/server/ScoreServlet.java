package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.load.LoadTesting;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.*;

/**
 * All in support of Liz tethered iOS app.
 * <p/>
 * User: GO22670
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);
  private static final String REQUEST = "request";
  private static final String NESTED_CHAPTERS = "nestedChapters";
  private static final String LEAST_RECORDED_CHAPTERS = "leastRecordedChapters";
  private static final String ALIGN = "align";
  private static final String DECODE = "decode";
  private static final String SCORE = "score";
  private static final String CHAPTER_HISTORY = "chapterHistory";
  private static final String REF_INFO = "refInfo";
  private static final String ROUND_TRIP = "roundTrip";
  private static final String RECORD_HISTORY = "recordHistory";
  private static final String PHONE_REPORT = "phoneReport";
  /**
   * Where is this used ???
   */
  private static final String EXERCISE_HISTORY = "exerciseHistory";
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
  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  private static final String EVENT = "event";
  private static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000l;
  private static final String HAS_RESET = "hasReset";
  private static final String TOKEN = "token";

  /**
   * @see DatabaseImpl#userExists(HttpServletRequest, String, String)
   */
  public static final String PASSWORD_CORRECT = "passwordCorrect";
  private static final String PASSWORD_EMAIL_SENT = "PASSWORD_EMAIL_SENT";
  private static final String NOT_VALID = "NOT_VALID";
  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";
  private static final String FALSE = "false";

  private static final String EXID = "exid";
  private static final String VALID = "valid";
  private static final String REQID = "reqid";
  private static final String INVALID = "invalid";
  private static final String TYPE = "type";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  private static final String VERSION = "version";
  private static final String AGE = "age";
  private static final String GENDER = "gender";
  private static final String DIALECT = "dialect";
  private static final String CONTEXT = "context";
  private static final String WIDGET = "widget";
  private static final String CHILDREN = "children";
  public static final String KIND = "kind";

  private LoadTesting loadTesting;

  public enum Request {DECODE, ALIGN, RECORD}

  // Doug said to remove items with missing audio. 1/12/15
  private boolean REMOVE_EXERCISES_WITH_MISSING_AUDIO;
  private static final String START = "start";
  private static final String END = "end";

  private JSONObject nestedChapters;
  private long whenCached = -1;

  private DatabaseImpl db;
  private AudioFileHelper audioFileHelper;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#shareLoadTesting
   */
  public static final String LOAD_TESTING = "loadTesting";

  private static final String ADD_USER = "addUser";
  private static final String HAS_USER = "hasUser";
  private static final String FORGOT_USERNAME = "forgotUsername";
  private static final String RESET_PASS = "resetPassword";
  private static final String SET_PASSWORD = "setPassword";
  private static final double ALIGNMENT_SCORE_CORRECT = 0.5;
  //private boolean debug = true;

  /**
   * Remembers chapters from previous requests...
   *
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doGet : Request " + request.getQueryString() + " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    configureResponse(response);

    getAudioFileHelper();
    String queryString = request.getQueryString();
    JSONObject toReturn = new JSONObject();
    try {
      if (queryString != null) {
        queryString = URLDecoder.decode(queryString, "UTF-8");
        if (queryString.startsWith(NESTED_CHAPTERS)) {
          if (nestedChapters == null || (System.currentTimeMillis() - whenCached > REFRESH_CONTENT_INTERVAL)) {
            nestedChapters = getJsonNestedChapters();
            whenCached = System.currentTimeMillis();
          }
          toReturn = nestedChapters;
        } else if (queryString.startsWith(LEAST_RECORDED_CHAPTERS)) {
          toReturn = getJsonLeastRecordedChapters();
        } else if (queryString.startsWith(HAS_USER)) {
          String[] split1 = queryString.split("&");
          if (split1.length != 2) {
            toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
          } else {
            gotHasUser(toReturn, split1);
          }
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
                getHTML("OK, your password has been reset. Please go back to NetProF and login.",  "Password has been reset");
            reply(response, rep);
            return;
          }
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
        } else if (queryString.startsWith(EXERCISE_HISTORY)) { // TODO : who calls this???
          String[] split1 = queryString.split("&");
          if (split1.length != 2) {
            toReturn.put(ERROR, EXPECTING_TWO_QUERY_PARAMETERS);
          } else {
            String first = split1[0];
            String user = first.split("=")[1];

            String second = split1[1];
            String exercise = second.split("=")[1];

            logger.debug("exerciseHistory " + user + " pass " + exercise);
            try {
              long l = Long.parseLong(user);
              toReturn = db.getResultDAO().getHistoryAsJson(l, exercise);
            } catch (NumberFormatException e) {
              toReturn.put(ERROR, "User id should be a number");
            }
          }
        } else if (queryString.startsWith(CHAPTER_HISTORY) || queryString.startsWith("request=" + CHAPTER_HISTORY)) {
          queryString = queryString.substring(queryString.indexOf(CHAPTER_HISTORY) + CHAPTER_HISTORY.length());
          toReturn = getChapterHistory(queryString, toReturn);
        } else if (queryString.startsWith(REF_INFO) || queryString.startsWith("request=" + REF_INFO)) {
          queryString = queryString.substring(queryString.indexOf(REF_INFO) + REF_INFO.length());
          toReturn = getRefInfo(queryString, toReturn);
        } else if (queryString.startsWith(RECORD_HISTORY) || queryString.startsWith("request=" + RECORD_HISTORY)) {
          queryString = queryString.substring(queryString.indexOf(RECORD_HISTORY) + RECORD_HISTORY.length());
          toReturn = getRecordHistory(queryString, toReturn);
        } else if (queryString.startsWith(PHONE_REPORT) || queryString.startsWith("request=" + PHONE_REPORT)) {
          queryString = queryString.substring(queryString.indexOf(PHONE_REPORT) + PHONE_REPORT.length());
          String[] split1 = queryString.split("&");
          if (split1.length < 2) {
            toReturn.put(ERROR, "expecting at least two query parameters");
          } else {
            toReturn = getPhoneReport(toReturn, split1);
          }
        } else {
          toReturn.put(ERROR, "unknown req " + queryString);
        }
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    String x = toReturn.toString();
    reply(response, x);
  }

  private JSONObject getPhoneReport(JSONObject toReturn, String[] split1) {
    String user = "";
    Map<String, Collection<String>> selection = new TreeMap<String, Collection<String>>();
    for (String param : split1) {
      //logger.debug("param '" +param+               "'");
      String[] split = param.split("=");
      if (split.length == 2) {
        String key = split[0];
        String value = split[1];
        if (key.equals(USER)) {
          user = value;
        } else {
          selection.put(key, Collections.singleton(value));
        }
      }
    }

    logger.debug("getPhoneReport (" +serverProps.getLanguage() + ") : user " + user + " selection " + selection);
    try {
      long l = Long.parseLong(user);
      long then = System.currentTimeMillis();
      toReturn = db.getJsonPhoneReport(l, selection);
      long now = System.currentTimeMillis();
      if (now-then > 250) {
        logger.debug("getPhoneReport (" +serverProps.getLanguage() + ") : user " + user + " selection " + selection + " took " + (now-then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
  }

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @param queryString
   * @param toReturn
   * @return
   */
  private JSONObject getChapterHistory(String queryString, JSONObject toReturn) {
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      String user = "";
      Map<String, Collection<String>> selection = new TreeMap<String, Collection<String>>();
      for (String param : split1) {
        //logger.debug("param '" +param+               "'");
        String[] split = param.split("=");
        if (split.length == 2) {
          String key = split[0];
          String value = split[1];
          if (key.equals(USER)) {
            user = value;
          } else {
            selection.put(key, Collections.singleton(value));
          }
        }
      }

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        long l = Long.parseLong(user);
        toReturn = db.getJsonScoreHistory(l, selection, getExerciseSorter());
      } catch (NumberFormatException e) {
        toReturn.put(ERROR, "User id should be a number");
      }
    }
    return toReturn;
  }

  private JSONObject getRefInfo(String queryString, JSONObject toReturn) {
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      Map<String, Collection<String>> selection = new TreeMap<String, Collection<String>>();
      for (String param : split1) {
        String[] split = param.split("=");
        if (split.length == 2) {
          String key = split[0];
          String value = split[1];
          selection.put(key, Collections.singleton(value));
        }
      }

      //logger.debug("chapterHistory " + user + " selection " + selection);
      toReturn = db.getJsonRefResult(selection);
    }
    return toReturn;
  }

  /**
   * This is for Appen.
   *
   * @see #doGet
   * @param queryString
   * @param toReturn
   * @return
   */
  private JSONObject getRecordHistory(String queryString, JSONObject toReturn) {
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      String user = "";
      Map<String, Collection<String>> selection = new TreeMap<String, Collection<String>>();
      for (String param : split1) {
        //logger.debug("param '" +param+               "'");
        String[] split = param.split("=");
        if (split.length == 2) {
          String key = split[0];
          String value = split[1];
          if (key.equals(USER)) {
            user = value;
          } else {
            selection.put(key, Collections.singleton(value));
          }
        }
      }

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        long l = Long.parseLong(user);
        toReturn = db.getJsonScoreHistoryRecorded(l, selection, audioFileHelper.getCollator());
      } catch (NumberFormatException e) {
        toReturn.put(ERROR, "User id should be a number");
      }
    }
    return toReturn;
  }

  /**
   * Return enough information so we could create a new user from the json.
   * @see DatabaseImpl#
   * @param toReturn
   * @param split1
   */
  private void gotHasUser(JSONObject toReturn, String[] split1) {
    String first = split1[0];
    String user = first.split("=")[1];

    String second = split1[1];
    String[] split = second.split("=");
    String passwordH = split.length > 1 ? split[1] : "";

    User userFound = db.getUserDAO().getUserByID(user);

    Boolean noUserWithID = userFound == null;

    if (!noUserWithID) {
      logger.debug("hasUser '" + user + "' pass '" + passwordH + "' -> " + userFound);
    }

    toReturn.put(USERID,    noUserWithID ? -1 : userFound.getId());
    toReturn.put(EMAIL_H,   noUserWithID ? -1 : userFound.getEmailHash());
    toReturn.put(KIND,      noUserWithID ? -1 : userFound.getUserKind().toString());
    toReturn.put(HAS_RESET, noUserWithID ? -1 : userFound.hasResetKey());
    toReturn.put(TOKEN, noUserWithID ? "" : userFound.getResetKey());
    toReturn.put(PASSWORD_CORRECT,
        noUserWithID ? FALSE :
            userFound.getPasswordHash() == null ? FALSE :
                userFound.getPasswordHash().equalsIgnoreCase(passwordH));
  }

  /**
   * Don't die if audio file helper is not available.
   *
   * @return
   * @see #doGet
   * @see #getJsonForSelection
   */
  private ExerciseSorter getExerciseSorter() {
    Map<String, Integer> phoneToCount = audioFileHelper == null ? new HashMap<String, Integer>() : audioFileHelper.getPhoneToCount();
    return new ExerciseSorter(db.getSectionHelper().getTypeOrder(), phoneToCount);
  }

  /**
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   * @param response
   * @param jsonObject
   */
  private void writeJsonToOutput(HttpServletResponse response, JSONObject jsonObject) {
    reply(response, jsonObject.toString());
  }

  private void reply(HttpServletResponse response, String x) {
    try {
      PrintWriter writer = response.getWriter();
//      if (x.length() > 1000) {
//        logger.debug("Reply " + x.substring(0, 1000));
//      } else {
//        logger.debug("Reply " + x);
//      }
      writer.println(x);
      writer.close();
    } catch (IOException e) {
      logger.error("got " + e, e);
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
   * TODO : Is handling a multi-part request slow?
   *
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    //   String pathInfo = request.getPathInfo();
//    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
//        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());
    getAudioFileHelper();

    configureResponse(response);

    String requestType = request.getHeader(REQUEST);
    String deviceType = request.getHeader(DEVICE_TYPE);
    if (deviceType == null) deviceType = "unk";
    String device = request.getHeader(DEVICE);
    if (device == null) device = "unk";

    JSONObject jsonObject = new JSONObject();
    if (requestType != null) {
      if (requestType.startsWith(ADD_USER)) {
        addUser(request, requestType, deviceType, device, jsonObject);
      } else if (requestType.startsWith(ALIGN) || requestType.startsWith(DECODE) ||
          requestType.startsWith(Request.RECORD.toString().toLowerCase())) {
        jsonObject = getJsonForAudio(request, requestType, deviceType, device);
      } else if (requestType.startsWith(EVENT)) {
        // log event
        gotLogEvent(request, device, jsonObject);
      } else if (requestType.startsWith(ROUND_TRIP)) {
        String resultID = request.getHeader("resultID");
        String roundTripMillis = request.getHeader("roundTrip");

        try {
          addRT(Long.parseLong(resultID), Integer.parseInt(roundTripMillis), jsonObject);
        } catch (NumberFormatException e) {
          jsonObject.put(ERROR, "bad param format " + e.getMessage());
        }
      } else {
        jsonObject.put(ERROR, "unknown req " + requestType);
      }
    } else {
      jsonObject = getJsonForAudio(request, null, deviceType, device);
    }

    writeJsonToOutput(response, jsonObject);
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
   * @see #doPost
   */
  private void addUser(HttpServletRequest request, String requestType, String deviceType, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);
    String passwordH = request.getHeader(PASSWORD_H);

    // first check if user exists already with this password -- if so go ahead and log them in.
    User exactMatch = db.getUserDAO().getUserWithPass(user, passwordH);

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
        //}
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

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);

    long userid = getUserFromParam2(user);
    if (db.getUserDAO().getUserWhere(userid) == null) {
      jsonObject.put(ERROR, "unknown user " + userid);
    } else {
      String context = request.getHeader(CONTEXT);
      String exid = request.getHeader(EXID);
      String widgetid = request.getHeader(WIDGET);
      String widgetType = request.getHeader("widgetType");

      //   logger.debug("doPost : Request " + requestType + " for " + deviceType + " user " + user + " " + exid);

      if (widgetid == null) {
        db.logEvent(exid == null ? "N/A" : exid, context, userid, device);
      } else {
        db.logEvent(widgetid, widgetType, exid == null ? "N/A" : exid, context, userid, device);
      }
    }
  }

  /**
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   * @param resultID
   * @param roundTripMillis
   * @param jsonObject
   */
  private void addRT(long resultID, int roundTripMillis, JSONObject jsonObject) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTripMillis);
    jsonObject.put("OK","OK");
  }

  private long getUserFromParam2(String user) {
    long userid;
    try {
      userid = Long.parseLong(user);
    } catch (NumberFormatException e) {
      logger.warn("couldn't parse event userid " + user);
      userid = -1;
    }
    return userid;
  }

  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
  }

  private int getUserFromParam(String user) {
    int i = -1;
    try {
      i = Integer.parseInt(user);
    } catch (NumberFormatException e) {
      logger.error("expecting a number for user id " + user);
    }
    return i;
  }

  /**
   * join against audio dao ex->audio map again to get user exercise audio! {@link #getJsonArray(java.util.List)}
   *
   * @return
   * @see #doGet
   */
  private JSONObject getJsonNestedChapters() {
    setInstallPath(db);
    db.getExercises();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT, getContentAsJson());
    jsonObject.put(VERSION, "1.0");
    jsonObject.put(HAS_MODEL, !db.getServerProps().isNoModel());

    return jsonObject;
  }

  /**
   * @return
   * @see #getJsonNestedChapters
   */
  private JSONArray getContentAsJson() {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();

    for (SectionNode node : db.getSectionHelper().getSectionNodes()) {
      String type = node.getType();
      typeToValues.put(type, Collections.singletonList(node.getName()));
      JSONObject jsonForNode = getJsonForNode(node, typeToValues);
      typeToValues.remove(type);

      jsonArray.add(jsonForNode);
    }
    return jsonArray;
  }

  /**
   * @param node
   * @param typeToValues
   * @return
   * @see #getContentAsJson
   */
  private JSONObject getJsonForNode(SectionNode node, Map<String, Collection<String>> typeToValues) {
    JSONObject jsonForNode = new JSONObject();
    jsonForNode.put(TYPE, node.getType());
    jsonForNode.put(NAME, node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      JSONArray exercises = getJsonForSelection(typeToValues);
      jsonForNode.put(ITEMS, exercises);
    } else {
      for (SectionNode child : node.getChildren()) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode(child, typeToValues));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put(CHILDREN, jsonArray);
    return jsonForNode;
  }

  /**
   * @param typeToValues for this unit and chapter
   * @return
   * @see #getJsonForNode
   */
  private JSONArray getJsonForSelection(Map<String, Collection<String>> typeToValues) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToValues);

    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);

    if (REMOVE_EXERCISES_WITH_MISSING_AUDIO) {
      Iterator<CommonExercise> iterator = copy.iterator();
      for (; iterator.hasNext(); ) {
        CommonExercise next = iterator.next();
        if (!next.hasRefAudio()) iterator.remove();
      }
    }
    if (audioFileHelper != null) {
      getExerciseSorter().sortedByPronLengthThenPhone(copy, audioFileHelper.getPhoneToCount());
    }
    else {
      logger.warn("audioFileHelper not set yet!");
    }
    return getJsonArray(copy);
  }

  /**
   * This is the json that describes an individual entry.
   * <p/>
   * Makes sure to attach audio to exercises (this is especially important for userexercises that mask out
   * exercises with new reference audio).
   *
   * @param copy
   * @return
   */
  private JSONArray getJsonArray(List<CommonExercise> copy) {
    JSONArray exercises = new JSONArray();

    Map<String, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio();
    String installPath = pathHelper.getInstallPath();

    for (CommonExercise exercise : copy) {
      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        db.getAudioDAO().attachAudio(exercise, installPath, relativeConfigDir, audioAttributes);
      }
      //if (!debug) ensureMP3s(exercise);
      exercises.add(getJsonForExercise(exercise));
    }
    return exercises;
  }

  /**
   * Write the posted audio file to a location based on the user id and the exercise id.
   * If request type is decode, decode the file and return score info in json.
   *
   * Allow alternate decode possibilities if header is set.
   *
   * @param request
   * @param requestType - decode or align or record
   * @param deviceType
   * @param device
   * @return
   * @throws IOException
   * @see #doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private JSONObject getJsonForAudio(HttpServletRequest request, String requestType,
                                     String deviceType, String device) throws IOException {
    String user = request.getHeader(USER);
    String exerciseID = request.getHeader("exercise");
    int reqid = getReqID(request);
    logger.debug("getJsonForAudio got request " + requestType + " for user " + user + " exercise " + exerciseID +
        " req " + reqid);
    int i = getUserFromParam(user);
    String wavPath = pathHelper.getLocalPathToAnswer("plan", exerciseID, 0, i);
    File saveFile = pathHelper.getAbsoluteFile(wavPath);
    new File(saveFile.getParent()).mkdirs();

    String allow_alternates = request.getHeader("ALLOW_ALTERNATES");
    boolean allowAlternates = allow_alternates != null && !allow_alternates.equals("false");

    String use_phone_to_display = request.getHeader("USE_PHONE_TO_DISPLAY");
    boolean usePhoneToDisplay = use_phone_to_display != null && !use_phone_to_display.equals("false");

    writeToOutputStream(request.getInputStream(), saveFile);
    return getJsonForAudioForUser(reqid, exerciseID, i, Request.valueOf(requestType.toUpperCase()), wavPath, saveFile,
        deviceType, device, allowAlternates, usePhoneToDisplay);
  }

  /**
   * @see #getJsonForAudio(HttpServletRequest, String, String, String)
   * @param request
   * @return
   */
  private int getReqID(HttpServletRequest request) {
    String reqid = request.getHeader(REQID);
    //logger.debug("got req id " + reqid);
    if (reqid == null) reqid = "1";
    try {
      int req = Integer.parseInt(reqid);
      //logger.debug("returning req id " + req);
      return req;
    } catch (NumberFormatException e) {
      logger.warn("Got parse error on reqid " + reqid);
    }
    return 1;
  }

  /**
   * @param reqid      label response with req id so the client can tell if it got a stale response
   * @param exerciseID for this exercise
   * @param user       by this user
   * @param request
   * @param wavPath    relative path to posted audio file
   * @param saveFile   File handle to file
   * @param deviceType iPad,iPhone, or browser
   * @param device     id for device - helpful for iPads, etc.
   * @param allowAlternates
   * @param usePhoneToDisplay
   * @return score json
   * @see #getJsonForAudio(javax.servlet.http.HttpServletRequest, String, String, String)
   */
  private JSONObject getJsonForAudioForUser(int reqid, String exerciseID, int user, Request request, String wavPath,
                                            File saveFile,
                                            String deviceType, String device, boolean allowAlternates,
                                            boolean usePhoneToDisplay) {
    long then = System.currentTimeMillis();
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    JSONObject jsonForScore = new JSONObject();
    if (exercise1 == null) {
      jsonForScore.put(VALID, "bad_exercise_id");
    } else {
      boolean doFlashcard = request == Request.DECODE;
      AudioAnswer answer = getAudioAnswer(reqid, exerciseID, user, doFlashcard, wavPath, saveFile, deviceType, device,
          exercise1, allowAlternates, usePhoneToDisplay);
      long now = System.currentTimeMillis();
      PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
      float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();
      logger.debug("score flashcard " + doFlashcard +
          " exercise id " + exerciseID + " took " + (now - then) +
          " millis for " + saveFile.getName() + " = " + hydecScore);

      if (answer != null && answer.isValid()) {
        jsonForScore = getJsonForScore(pretestScore,usePhoneToDisplay);
        if (doFlashcard) {
          jsonForScore.put(IS_CORRECT, answer.isCorrect());
          jsonForScore.put(SAID_WORD,  answer.isSaidAnswer());
          jsonForScore.put("resultID", answer.getResultID());

          // attempt to get more feedback when we're too sensitive and match the unknown model
          if (!answer.isCorrect() && !answer.isSaidAnswer()) {
            answer = getAudioAnswerAlign(reqid, exerciseID, user, false, wavPath, saveFile, deviceType, device,
                exercise1, usePhoneToDisplay);
            PretestScore pretestScore1 = answer.getPretestScore();
            logger.debug("Alignment on an unknown model gets " + pretestScore1);
            //   logger.debug("score info " + answer.getPretestScore().getsTypeToEndTimes());
            jsonForScore = getJsonForScore(pretestScore1, usePhoneToDisplay);

            // so we mark it correct if the score is above 50% on alignment
            jsonForScore.put(IS_CORRECT, pretestScore1.getHydecScore() > ALIGNMENT_SCORE_CORRECT);
            jsonForScore.put(SCORE, pretestScore1.getHydecScore());
            jsonForScore.put(SAID_WORD, false);   // don't say they said the word - decode says they didn't
          }
        }
      }
      addValidity(exerciseID, jsonForScore, answer);

      if (request == Request.RECORD) { // for Appen - this is OK, since we didn't actually do alignment on the audio...
        loadTesting.addToAudioTable(user, exercise1, answer);
      }
    }
    return jsonForScore;
  }

  /**
   * @see #getJsonForAudioForUser(int, String, int, Request, String, File, String, String, boolean, boolean)
   * @param exerciseID
   * @param jsonForScore
   * @param answer
   */
  private void addValidity(String exerciseID, JSONObject jsonForScore, AudioAnswer answer) {
    jsonForScore.put(EXID, exerciseID);
    jsonForScore.put(VALID, answer == null ? INVALID : answer.getValidity().toString());
    jsonForScore.put(REQID, answer == null ? 1 : "" + answer.getReqid());
  }

  /**
   * @param reqid       label response with req id so the client can tell if it got a stale response
   * @param exerciseID  for this exercise
   * @param user        by this user
   * @param doFlashcard
   * @param wavPath     path to posted audio file
   * @param saveFile
   * @param deviceType
   * @param device
   * @param exercise1
   * @param allowAlternates
   * @param usePhoneToDisplay
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAudioAnswer(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath, File saveFile,
                                     String deviceType, String device, CommonExercise exercise1, boolean allowAlternates, boolean usePhoneToDisplay) {
    AudioAnswer answer;

    if (doFlashcard) {
      answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, -1, deviceType, device, reqid, allowAlternates);
    } else {
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid, wavPath, exercise1.getRefSentence(), exerciseID, usePhoneToDisplay);
      answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device, reqid, allowAlternates);
      answer.setPretestScore(asrScoreForAudio);
    }
    return answer;
  }

  /**
   * @param reqid
   * @param exerciseID
   * @param user
   * @param doFlashcard
   * @param wavPath
   * @param saveFile
   * @param deviceType
   * @param device
   * @param exercise1
   * @param usePhoneToDisplay
   * @return
   * @see #getJsonForAudio
   */
  private AudioAnswer getAudioAnswerAlign(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath, File saveFile,
                                          String deviceType, String device, CommonExercise exercise1, boolean usePhoneToDisplay) {
    PretestScore asrScoreForAudio = getASRScoreForAudioNoCache(reqid, saveFile.getAbsolutePath(), exercise1.getRefSentence(), exerciseID,usePhoneToDisplay);
    AudioAnswer answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device, reqid, false);
    answer.setPretestScore(asrScoreForAudio);
    return answer;
  }

  /**
   * Don't wait for mp3 to write to return - can take 70 millis for a short file.
   * @param exerciseID
   * @param user
   * @param doFlashcard
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @param reqid
   * @param allowAlternates
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAnswer(String exerciseID, int user, boolean doFlashcard, String wavPath, File file, float score,
                                String deviceType, String device, int reqid, boolean allowAlternates) {
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    AudioAnswer answer = audioFileHelper.getAnswer(exerciseID, exercise1, user, doFlashcard, wavPath, file, deviceType,
        device, score, reqid, allowAlternates);

    final String path = answer.getPath();
    final String foreignLanguage = exercise1.getForeignLanguage();

    new Thread(new Runnable() {
      @Override
      public void run() {
        //long then = System.currentTimeMillis();
        ensureMP3(path, foreignLanguage);
       // long now = System.currentTimeMillis();
 //       logger.debug("Took " + (now-then) + " millis to write mp3 version");
      }
    }).start();

    return answer;
  }

  /**
   * @param inputStream
   * @param saveFile
   * @throws IOException
   * @see #getJsonForAudio(javax.servlet.http.HttpServletRequest, String, String, String)
   */
  private void writeToOutputStream(ServletInputStream inputStream, File saveFile) throws IOException {
    writeToFile(inputStream, saveFile);
  }

  /**
   * For both words and phones, return event text, start, end times, and score for event.
   *
   * @param score
   * @return
   * @see #getJsonForAudioForUser
   */
  private JSONObject getJsonForScore(PretestScore score, boolean usePhoneDisplay) {
    JSONObject jsonObject = new JSONObject();

    jsonObject.put(SCORE, score.getHydecScore());

    for (Map.Entry<NetPronImageType, List<TranscriptSegment>> pair : score.getsTypeToEndTimes().entrySet()) {
      List<TranscriptSegment> value = pair.getValue();
      JSONArray value1 = new JSONArray();
      NetPronImageType imageType = pair.getKey();

      boolean usePhone = imageType == NetPronImageType.PHONE_TRANSCRIPT &&
          (serverProps.usePhoneToDisplay() || usePhoneDisplay);

      for (TranscriptSegment segment : value) {
        JSONObject object = new JSONObject();
        String event = segment.getEvent();
        if (usePhone) event = serverProps.getDisplayPhoneme(event);

        object.put(EVENT, event);
        object.put(START, segment.getStart());
        object.put(END,   segment.getEnd());
        object.put(SCORE, segment.getScore());

        value1.add(object);
      }

      jsonObject.put(imageType.toString(), value1);
    }
    return jsonObject;
  }

  /**
   * Get a reference to the current database object, made in the main LangTestDatabaseImpl servlet
   *
   * @return
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private void getAudioFileHelper() {
    if (audioFileHelper == null) {
      setPaths();

      db = getDatabase();
      serverProps = db.getServerProps();
      audioFileHelper = getAudioFileHelperRef();
      loadTesting = getLoadTesting();
      REMOVE_EXERCISES_WITH_MISSING_AUDIO = serverProps.removeExercisesWithMissingAudio();
    }
   // return audioFileHelper;
  }

  /**
   * @see #getAudioFileHelper()
   * @return
   */
  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;
    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      // logger.debug("found existing database reference " + db + " under " + getServletContext());
    } else {
      logger.error("huh? no existing db reference?");
    }
    return db;
  }

  /**
   * @see #getAudioFileHelper()
   * @return
   */
  private AudioFileHelper getAudioFileHelperRef() {
    AudioFileHelper fileHelper = null;
    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.AUDIO_FILE_HELPER_REFERENCE);
    if (databaseReference != null) {
      fileHelper = (AudioFileHelper) databaseReference;
      // logger.debug("found existing audio file reference " + fileHelper + " under " + getServletContext());
    } else {
      logger.error("huh? for " + db.getServerProps().getLanguage() + " no existing audio file reference?");
    }
    return fileHelper;
  }

  private LoadTesting getLoadTesting() {
    LoadTesting ref = null;
    Object databaseReference = getServletContext().getAttribute(LOAD_TESTING);
    if (databaseReference != null) {
      ref = (LoadTesting) databaseReference;
      // logger.debug("found existing audio file reference " + fileHelper + " under " + getServletContext());
    } else {
      logger.error("huh? for " + db.getServerProps().getLanguage() + " no existing load test reference?");
    }
    return ref;
  }

  /**
   * TODO : this is wacky -- have to do this for alignment but not for decoding
   *
   * @see #getAudioAnswer(int, String, int, boolean, String, File, String, String, CommonExercise, boolean, boolean)
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   */
  private PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                           String exerciseID, boolean usePhoneToDisplay) {
    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), exerciseID, null,usePhoneToDisplay, false);
  }

  /**
   * @see #getAudioAnswerAlign(int, String, int, boolean, String, File, String, String, CommonExercise, boolean)
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   */
  private PretestScore getASRScoreForAudioNoCache(int reqid, String testAudioFile, String sentence,
                                                  String exerciseID, boolean usePhoneToDisplay) {
    //  logger.debug("getASRScoreForAudioNoCache for " + testAudioFile + " under " + sentence);
    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), false, exerciseID, null,usePhoneToDisplay, false);
  }

  /**
   * Just for appen -
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getJsonLeastRecordedChapters() {
    setInstallPath(db);
    db.getExercises();

    Map<String, Integer> exToCount = db.getAudioDAO().getExToCount();

    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();

    List<SectionNode> sectionNodes = db.getSectionHelper().getSectionNodes();
    for (SectionNode node : sectionNodes) {
      String type = node.getType();
      typeToValues.put(type, Collections.singletonList(node.getName()));
      recurse(node, typeToValues,exToCount);
      typeToValues.remove(type);
    }

    Collections.sort(sectionNodes);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT, getContentAsJson2(sectionNodes));
    jsonObject.put(VERSION, "1.0");
    jsonObject.put(HAS_MODEL, !db.getServerProps().isNoModel());

    return jsonObject;
  }

  /**
   * @see #getJsonLeastRecordedChapters()
   * @param sectionNodes
   * @return
   */
  private JSONArray getContentAsJson2(Collection<SectionNode> sectionNodes) {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();

    if (audioFileHelper != null) {

      for (SectionNode node : sectionNodes) {
        String type = node.getType();
        typeToValues.put(type, Collections.singletonList(node.getName()));
        JSONObject jsonForNode = getJsonForNode2(node, typeToValues);
        typeToValues.remove(type);

        jsonArray.add(jsonForNode);
      }
    }
    return jsonArray;
  }

  /**
   * @param node
   * @param typeToValues
   * @return
   * @see #getContentAsJson2
   */
  private JSONObject getJsonForNode2(SectionNode node, Map<String, Collection<String>> typeToValues) {
    JSONObject jsonForNode = new JSONObject();
    jsonForNode.put(TYPE, node.getType());
    jsonForNode.put(NAME, node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      JSONArray exercises = getJsonForSelection(typeToValues);
      jsonForNode.put(ITEMS, exercises);
    } else {
      List<SectionNode> children = node.getChildren();
      Collections.sort(children); // by avg recorded number

      for (SectionNode child : children) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode2(child, typeToValues));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put(CHILDREN, jsonArray);
    return jsonForNode;
  }

  /**
   * @see #getJsonLeastRecordedChapters()
   * @param node
   * @param typeToValues
   * @param exToCount
   */
  private void recurse(SectionNode node, Map<String, Collection<String>> typeToValues, Map<String, Integer> exToCount) {
    if (node.isLeaf()) {
      Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToValues);
      float total = 0f;
      for (CommonExercise ex : exercisesForState) {
        Integer integer = exToCount.get(ex.getID());
        if (integer == null) {
  //        logger.error("huh? unknown ex id " +ex.getID());
        }
        else {
          total += integer.floatValue();
        }
      }
      total /= new Integer(exercisesForState.size()).floatValue();
      node.setWeight(total); // avg number of recordings
    //  logger.debug("set weight on " +node);
    } else {
      for (SectionNode child : node.getChildren()) {
        String type = child.getType();
        typeToValues.put(type, Collections.singletonList(child.getName()));
        recurse(child, typeToValues, exToCount);
        typeToValues.remove(type);
      }
    }
  }

  /**
   * @param db
   * @return
   * @see #getJsonNestedChapters()
   */
  private void setInstallPath(DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (!new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile,
        relativeConfigDir + File.separator + serverProps.getMediaDir());
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}