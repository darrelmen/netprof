package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
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
import java.io.*;
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
  private static final String EMAIL_H = "emailH";
  private static final String USERID = "userid";
  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  private static final String EVENT = "event";
  private static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000l;
  private static final String HAS_RESET = "hasReset";
  private static final String TOKEN = "token";
  private static final String PASSWORD_CORRECT = "passwordCorrect";
  private static final String PASSWORD_EMAIL_SENT = "PASSWORD_EMAIL_SENT";
  private static final String NOT_VALID = "NOT_VALID";
  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";
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
            toReturn.put("valid", valid);
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
            toReturn.put("valid", changePFor(token, passwordH));
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

    //logger.debug("getPhoneReport " + user + " selection " + selection);
    try {
      long l = Long.parseLong(user);
      toReturn = db.getJsonPhoneReport(l, selection);
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
  }

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
        toReturn = db.getJsonScoreHistory(l, selection, getExerciseSorter()/*, audioFileHelper.getCollator()*/);
      } catch (NumberFormatException e) {
        toReturn.put(ERROR, "User id should be a number");
      }
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

  private void gotHasUser(JSONObject toReturn, String[] split1) {
    String first = split1[0];
    String user = first.split("=")[1];

    String second = split1[1];
    String passwordH = second.split("=")[1];

    User userFound = db.getUserDAO().getUser(user, passwordH);

    logger.debug("hasUser " + user + " pass " + passwordH + " -> " + userFound);

    toReturn.put(USERID, userFound == null ? -1 : userFound.getId());
    toReturn.put(HAS_RESET, userFound == null ? -1 : userFound.hasResetKey());
    toReturn.put(TOKEN, userFound == null ? "" : userFound.getResetKey());
    toReturn.put(PASSWORD_CORRECT,
        userFound == null ? "false" :
            userFound.getPasswordHash() == null ? "false" :
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

  private void writeJsonToOutput(HttpServletResponse response, JSONObject jsonObject) throws IOException {
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
      } else {
        jsonObject.put(ERROR, "unknown req " + requestType);
      }
    } else {
      jsonObject = getJsonForAudio(request, null, deviceType, device);
    }

    writeJsonToOutput(response, jsonObject);
  }

  /**
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
    User userFound = db.getUserDAO().getUser(user, passwordH);

    if (userFound == null) {
      String age = request.getHeader("age");
      String gender = request.getHeader("gender");
      String dialect = request.getHeader("dialect");
      String emailH = request.getHeader(EMAIL_H);

      logger.debug("doPost : Request " + requestType + " for " + deviceType + " user " + user + " adding " + gender + " age " + age + " dialect " + dialect);
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

      if (user1 == null) {
        jsonObject.put(EXISTING_USER_NAME, "");
      } else {
        jsonObject.put(USERID, user1.getId());
      }
    } else {
      logger.debug("addUser - existing user for " + user + " pass " + passwordH + " -> " + userFound);

      if (userFound.hasResetKey()) {
        jsonObject.put(ERROR, "password was reset");
      } else {
        jsonObject.put(USERID, userFound.getId());
      }
    }
  }

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);

    long userid = getUserFromParam2(user);
    if (db.getUserDAO().getUserWhere(userid) == null) {
      jsonObject.put(ERROR, "unknown user " + userid);
    } else {
      String context = request.getHeader("context");
      String exid = request.getHeader("exid");
      String widgetid = request.getHeader("widget");
      String widgetType = request.getHeader("widgetType");

      //   logger.debug("doPost : Request " + requestType + " for " + deviceType + " user " + user + " " + exid);

      if (widgetid == null) {
        db.logEvent(exid == null ? "N/A" : exid, context, userid, device);
      } else {
        db.logEvent(widgetid, widgetType, exid == null ? "N/A" : exid, context, userid, device);
      }
    }
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
    jsonObject.put("version", "1.0");
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
    jsonForNode.put("type", node.getType());
    jsonForNode.put("name", node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      JSONArray exercises = getJsonForSelection(typeToValues);
      jsonForNode.put("items", exercises);
    } else {
      for (SectionNode child : node.getChildren()) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode(child, typeToValues));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put("children", jsonArray);
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
    //  getExerciseSorter().sortByForeign(copy, getAudioFileHelper());
    getExerciseSorter().sortedByPronLengthThenPhone(copy, audioFileHelper.getPhoneToCount());

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
    // Gets file name for HTTP header
    //   if (requestType != null) {
    String user = request.getHeader(USER);
    String exerciseID = request.getHeader("exercise");
    int reqid = getReqID(request);
    logger.debug("getJsonForAudio got request " + requestType + " for user " + user + " exercise " + exerciseID +
        " req " + reqid);
    int i = getUserFromParam(user);
    String wavPath = pathHelper.getLocalPathToAnswer("plan", exerciseID, 0, i);
    File saveFile = pathHelper.getAbsoluteFile(wavPath);
    new File(saveFile.getParent()).mkdirs();

    writeToOutputStream(request.getInputStream(), saveFile);
    return getJsonForAudioForUser(reqid, exerciseID, i, Request.valueOf(requestType.toUpperCase()), wavPath, saveFile,
        deviceType, device);
    //  } else {   // for backwards compatibility
    //    return handleRequestWithNoType(request);
    //  }
  }

  private int getReqID(HttpServletRequest request) {
    String reqid = request.getHeader("reqid");
    logger.debug("got req id " + reqid);
    if (reqid == null) reqid = "1";
    try {
      int req = Integer.parseInt(reqid);
      logger.debug("returning req id " + req);

      return req;
    } catch (NumberFormatException e) {
      logger.warn("Got parse error on reqid " + reqid);
    }
    return 1;
  }

  /**
   * Down this pathway, we can do simple decoding - word/phrase + audio file.
   * Generally, we want lots of other information - who asked for it, in reference to which exercise, etc.
   *
   * This exists mainly to support some theoretical future webservice application.
   * @see #getJsonForAudio
   * @param request filename retrieved from header, word to decode from header
   * @return json result
   * @throws IOException
   */
/*
  private JSONObject handleRequestWithNoType(HttpServletRequest request) throws IOException {
    String fileName = request.getHeader("fileName");
    String word = request.getHeader("word");
    boolean isFlashcard = request.getHeader("flashcard") != null;

    File tempDir = Files.createTempDir();
    File saveFile = new File(tempDir + File.separator + fileName);

    // opens input stream of the request for reading data
    writeToOutputStream(request.getInputStream(), saveFile);

    return isFlashcard ? getJsonForWordAndAudioFlashcard(word, saveFile) : getJsonForWordAndAudio(word, saveFile);
  }
*/

  /**
   * @param reqid      label response with req id so the client can tell if it got a stale response
   * @param exerciseID for this exercise
   * @param user       by this user
   * @param request
   * @param wavPath    relative path to posted audio file
   * @param saveFile   File handle to file
   * @param deviceType iPad,iPhone, or browser
   * @param device     id for device - helpful for iPads, etc.
   * @return score json
   * @seex #getJsonForParts(javax.servlet.http.HttpServletRequest, String)
   * @see #getJsonForAudio(javax.servlet.http.HttpServletRequest, String, String, String)
   */
  private JSONObject getJsonForAudioForUser(int reqid, String exerciseID, int user, Request request, String wavPath, File saveFile,
                                            String deviceType, String device) {
    long then = System.currentTimeMillis();
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    JSONObject jsonForScore = new JSONObject();
    if (exercise1 == null) {
      jsonForScore.put("valid", "bad_exercise_id");
    } else {
      boolean doFlashcard = request == Request.DECODE;
      AudioAnswer answer = getAudioAnswer(reqid, exerciseID, user, doFlashcard, wavPath, saveFile, deviceType, device, exercise1);
      long now = System.currentTimeMillis();
      PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
      float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();
      logger.debug("score flashcard " + doFlashcard +
          " exercise id " + exerciseID + " took " + (now - then) +
          " millis for " + saveFile.getName() + " = " + hydecScore);

      if (answer != null && answer.isValid()) {
        jsonForScore = getJsonForScore(pretestScore);
        if (doFlashcard) {
          jsonForScore.put(IS_CORRECT, answer.isCorrect());
          jsonForScore.put(SAID_WORD, answer.isSaidAnswer());
          // attempt to get more feedback when we're too sensitive and match the unknown model
          if (!answer.isCorrect() && !answer.isSaidAnswer() //&& pretestScore != null && pretestScore.getsTypeToEndTimes().isEmpty()
              ) {
            answer = getAudioAnswerAlign(reqid, exerciseID, user, false, wavPath, saveFile, deviceType, device, exercise1);
            logger.debug("Alignment on an unknown model gets " + answer.getPretestScore());
            //   logger.debug("score info " + answer.getPretestScore().getsTypeToEndTimes());
            jsonForScore = getJsonForScore(answer.getPretestScore());
            jsonForScore.put(IS_CORRECT, false);
            jsonForScore.put(SAID_WORD, false);   // don't say they said the word - decode says they didn't
          }
        }
      }
      addValidity(exerciseID, jsonForScore, answer);

      if (request == Request.RECORD) { // this is OK, since we didn't actually do alignment on the audio...
        loadTesting.addToAudioTable(user, exercise1, answer);
      }
    }
    return jsonForScore;
  }

  private void addValidity(String exerciseID, JSONObject jsonForScore, AudioAnswer answer) {
    jsonForScore.put("exid", exerciseID);
    jsonForScore.put("valid", answer == null ? "invalid" : answer.getValidity().toString());
    jsonForScore.put("reqid", answer == null ? 1 : "" + answer.getReqid());
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
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAudioAnswer(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath, File saveFile,
                                     String deviceType, String device, CommonExercise exercise1) {
    AudioAnswer answer;

    if (doFlashcard) {
      answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, -1, deviceType, device, reqid);
    } else {
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid, wavPath, exercise1.getRefSentence(), exerciseID);
      answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device, reqid);
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
   * @return
   * @see #getJsonForAudio
   */
  private AudioAnswer getAudioAnswerAlign(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath, File saveFile,
                                          String deviceType, String device, CommonExercise exercise1) {
    PretestScore asrScoreForAudio = getASRScoreForAudioNoCache(reqid, saveFile.getAbsolutePath(), exercise1.getRefSentence(), exerciseID);
    AudioAnswer answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device, reqid);
    answer.setPretestScore(asrScoreForAudio);
    return answer;
  }

  /**
   * @param word
   * @param saveFile
   * @return
   * @see #getJsonForAudio(javax.servlet.http.HttpServletRequest, String, String, String)
   * @see #getJsonForParts(javax.servlet.http.HttpServletRequest, String)
   * @deprecated we should move toward the API that records the audio in the results table
   */
/*  private JSONObject getJsonForWordAndAudio(String word, File saveFile) {
    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    long then = System.currentTimeMillis();
    PretestScore book = getASRScoreForAudio(audioFileHelper, saveFile.getAbsolutePath(), word);
    long now = System.currentTimeMillis();
    logger.debug("score for '" + word + "' took " + (now - then) + " millis for " + saveFile.getName() + " = " + book);

    return getJsonForScore(book);
  }*/

  /**
   * @param word
   * @param saveFile
   * @return
   * @see #getJsonForAudio
   * @deprecated we should move toward the API that records the audio in the results table
   */
/*  private JSONObject getJsonForWordAndAudioFlashcard(String word, File saveFile) {
    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    long then = System.currentTimeMillis();
    AudioFileHelper.ScoreAndAnswer scoreAndAnswer = getFlashcardScore(audioFileHelper, saveFile, word);
    long now = System.currentTimeMillis();
    float hydecScore = scoreAndAnswer.score == null ? -1 : scoreAndAnswer.score.getHydecScore();
    logger.debug("score for '" + word + "' took " + (now - then) +
        " millis for " + saveFile.getName() + " = " + hydecScore);

    JSONObject jsonForScore = getJsonForScore(scoreAndAnswer.score);
    jsonForScore.put(IS_CORRECT, scoreAndAnswer.answer.isCorrect());
    jsonForScore.put(SAID_WORD,  scoreAndAnswer.answer.isSaidAnswer());
    jsonForScore.put("exid", "unknown");

    return jsonForScore;
  }*/

  /**
   * @param exerciseID
   * @param user
   * @param doFlashcard
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @param reqid
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAnswer(String exerciseID, int user, boolean doFlashcard, String wavPath, File file, float score,
                                String deviceType, String device, int reqid) {
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    AudioAnswer answer = audioFileHelper.getAnswer(exerciseID, exercise1, user, doFlashcard, wavPath, file, deviceType,
        device, score, reqid);
    ensureMP3(answer.getPath(), exercise1.getForeignLanguage());

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
   * @param score
   * @return
   * @seex #getJsonForWordAndAudio
   * @seex #getJsonForWordAndAudioFlashcard
   * @see #getJsonForAudioForUser
   */
  private JSONObject getJsonForScore(PretestScore score) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(SCORE, score.getHydecScore());

    for (Map.Entry<NetPronImageType, List<TranscriptSegment>> pair : score.getsTypeToEndTimes().entrySet()) {
      List<TranscriptSegment> value = pair.getValue();
      JSONArray value1 = new JSONArray();

      for (TranscriptSegment segment : value) {
        JSONObject object = new JSONObject();
        object.put(EVENT, segment.getEvent());
        object.put(START, segment.getStart());
        object.put(END, segment.getEnd());
        object.put(SCORE, segment.getScore());

        value1.add(object);
      }

      jsonObject.put(pair.getKey().toString(), value1);
    }
    return jsonObject;
  }

  /**
   * Get a reference to the current database object, made in the main LangTestDatabaseImpl servlet
   *
   * @return
   * @seex #getJsonForWordAndAudio(String, java.io.File)
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private AudioFileHelper getAudioFileHelper() {
    if (audioFileHelper == null) {
      setPaths();

      db = getDatabase();
      serverProps = db.getServerProps();
      audioFileHelper = getAudioFileHelperRef();
      loadTesting = getLoadTesting();
      REMOVE_EXERCISES_WITH_MISSING_AUDIO = serverProps.removeExercisesWithMissingAudio();
    }
    return audioFileHelper;
  }

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
   * Do alignment of audio file against sentence.
   *
   * @param audioFileHelper
   * @param testAudioFile
   * @param sentence
   * @return
   * @seex #getJsonForWordAndAudio(String, java.io.File)
   */
/*  private PretestScore getASRScoreForAudio(AudioFileHelper audioFileHelper, String testAudioFile, String sentence) {
    // logger.debug("getASRScoreForAudio " +testAudioFile);
    PretestScore asrScoreForAudio = null;
    try {
      asrScoreForAudio = audioFileHelper.getASRScoreForAudio(-1, testAudioFile, sentence, 128, 128, false,
          false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), "");
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return asrScoreForAudio;
  }*/

  /**
   * TODO : this is wacky -- have to do this for alignment but not for decoding
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @return
   */
  private PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                           String exerciseID) {
    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), exerciseID);
  }

  private PretestScore getASRScoreForAudioNoCache(int reqid, String testAudioFile, String sentence,
                                                  String exerciseID) {
    //  logger.debug("getASRScoreForAudioNoCache for " + testAudioFile + " under " + sentence);

    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), false, exerciseID);
  }

  /**
   * @paramx audioFileHelper
   * @paramx testAudioFile
   * @paramx sentence to decode
   * @return
   * @seex #getJsonForWordAndAudioFlashcard(String, java.io.File)
   * @deprecated - this is not in reference to an exercise
   */
/*  private AudioFileHelper.ScoreAndAnswer getFlashcardScore(final AudioFileHelper audioFileHelper, File testAudioFile,
                                                           String sentence) {
    // logger.debug("getASRScoreForAudio " +testAudioFile);

    AudioFileHelper.ScoreAndAnswer asrScoreForAudio = new AudioFileHelper.ScoreAndAnswer(new PretestScore(), new AudioAnswer());
    if (!audioFileHelper.checkLTS(sentence)) {
      logger.error("couldn't decode the word '' since it's not in the dictionary or passes letter-to-sound.  " +
          "E.g. english word with an arabic model.");
      return asrScoreForAudio;
    }

    try {
      asrScoreForAudio = audioFileHelper.getFlashcardAnswer(testAudioFile, sentence);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return asrScoreForAudio;
  }*/

  /**
   *
   * @return
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
    jsonObject.put("version", "1.0");
    jsonObject.put(HAS_MODEL, !db.getServerProps().isNoModel());

    return jsonObject;
  }

  private JSONArray getContentAsJson2(Collection<SectionNode> sectionNodes) {
    JSONArray jsonArray = new JSONArray();
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();

    for (SectionNode node : sectionNodes) {
      String type = node.getType();
      typeToValues.put(type, Collections.singletonList(node.getName()));
      JSONObject jsonForNode = getJsonForNode2(node, typeToValues);
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
  private JSONObject getJsonForNode2(SectionNode node, Map<String, Collection<String>> typeToValues) {
    JSONObject jsonForNode = new JSONObject();
    jsonForNode.put("type", node.getType());
    jsonForNode.put("name", node.getName());
    JSONArray jsonArray = new JSONArray();

    if (node.isLeaf()) {
      JSONArray exercises = getJsonForSelection(typeToValues);
      jsonForNode.put("items", exercises);
    } else {
      List<SectionNode> children = node.getChildren();
      Collections.sort(children); // by avg recorded number

      for (SectionNode child : children) {
        typeToValues.put(child.getType(), Collections.singletonList(child.getName()));
        jsonArray.add(getJsonForNode2(child, typeToValues));
        typeToValues.remove(child.getType());
      }
    }
    jsonForNode.put("children", jsonArray);
    return jsonForNode;
  }

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

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile, serverProps.getLanguage(), true,
        relativeConfigDir + File.separator + serverProps.getMediaDir());
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}