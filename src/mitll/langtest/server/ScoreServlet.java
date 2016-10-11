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

package mitll.langtest.server;

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.*;

/**
 * All in support of tethered iOS app.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);

  private static final String REQUEST = "request";
  private static final String NESTED_CHAPTERS = "nestedChapters";
  private static final String ALIGN = "align";
  private static final String DECODE = "decode";
  private static final String SCORE = "score";
  private static final String CHAPTER_HISTORY = "chapterHistory";
  private static final String REF_INFO = "refInfo";
  private static final String ROUND_TRIP = "roundTrip";
  private static final String PHONE_REPORT = "phoneReport";

  private static final String ERROR = "ERROR";
  private static final String USER = "user";

  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  private static final String EVENT = "event";
  public static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000L;
  private static final long REFRESH_CONTENT_INTERVAL_THREE = 3 * 60 * 60 * 1000L;

  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";

  private static final String EXID = "exid";
  private static final String VALID = "valid";
  private static final String REQID = "reqid";
  private static final String INVALID = "invalid";
  private static final String VERSION = "version";
  private static final String CONTEXT = "context";
  private static final String WIDGET = "widget";
  private static final String REQUEST1 = "request=";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";

  private static final String YEAR = "year";
  private static final String JSON_REPORT = "jsonReport";
  private static final String REPORT = "report";
  public static final String VERSION_NOW = "1.0";
  public static final String EXPORT = "export";
  public static final String REMOVE_REF_RESULT = "removeRefResult";
  private boolean removeExercisesWithMissingAudioDefault = true;

  private RestUserManagement userManagement;

  private enum Request {DECODE, ALIGN, RECORD}

  // Doug said to remove items with missing audio. 1/12/15
  private static final String START = "start";
  private static final String END = "end";

  private JSONObject nestedChapters;
  private JSONObject nestedChaptersEverything;
  private long whenCached = -1;
  private long whenCachedEverything = -1;

  private DatabaseImpl db;
  private AudioFileHelper audioFileHelper;

  private static final String ADD_USER = "addUser";
  private static final double ALIGNMENT_SCORE_CORRECT = 0.5;

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
    String queryString = request.getQueryString();
    if (queryString == null) queryString = ""; // how could this happen???
    if (!queryString.contains(NESTED_CHAPTERS)) { // quiet output for polling from status webapp
      String pathInfo = request.getPathInfo();
      logger.debug("ScoreServlet.doGet (" +getLanguage() +
          "): Request '" + queryString + "' path " + pathInfo +
          " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());
    }

    long then = System.currentTimeMillis();
    configureResponse(response);

    getAudioFileHelper();
    JSONObject toReturn = new JSONObject();
    //toReturn.put(ERROR, "expecting request");

    try {
      queryString = URLDecoder.decode(queryString, "UTF-8");
      if (matchesRequest(queryString, NESTED_CHAPTERS)) {
        String[] split1 = queryString.split("&");
        if (split1.length == 2) {
          String removeExercisesWithMissingAudio = getRemoveExercisesParam(queryString);
          boolean shouldRemoveExercisesWithNoAudio = removeExercisesWithMissingAudio.equals("true");
          boolean dontRemove = removeExercisesWithMissingAudio.equals("false");
          if (shouldRemoveExercisesWithNoAudio || dontRemove) {
            if (dontRemove) {
              if (nestedChaptersEverything == null || (System.currentTimeMillis() - whenCachedEverything > REFRESH_CONTENT_INTERVAL_THREE)) {
                nestedChaptersEverything = getJsonNestedChapters(shouldRemoveExercisesWithNoAudio);
                whenCachedEverything = System.currentTimeMillis();
              }
              toReturn = nestedChaptersEverything;
            }
            else {
              toReturn = getJsonNestedChapters(true);
            }
          } else {
            toReturn.put(ERROR, "expecting param " + REMOVE_EXERCISES_WITH_MISSING_AUDIO);
          }
        } else {
          if (nestedChapters == null || (System.currentTimeMillis() - whenCached > REFRESH_CONTENT_INTERVAL)) {
            nestedChapters = getJsonNestedChapters(true);
            whenCached = System.currentTimeMillis();
          }
          toReturn = nestedChapters;
        }
      } else if (userManagement.doGet(request, response, queryString, toReturn)) {
        logger.info("doGet " + getLanguage() + " handled user command for " + queryString);
      } else if (matchesRequest(queryString, CHAPTER_HISTORY)) {
        queryString = removePrefix(queryString, CHAPTER_HISTORY);
        toReturn = getChapterHistory(queryString, toReturn);
      } else if (matchesRequest(queryString, REF_INFO)) {
        queryString = removePrefix(queryString, REF_INFO);
        toReturn = getRefInfo(queryString, toReturn);
      } else if (matchesRequest(queryString, JSON_REPORT)) {
        queryString = removePrefix(queryString, JSON_REPORT);
        int year = getYear(queryString);
        getReport(toReturn, year);
      } else if (matchesRequest(queryString, EXPORT)) {
        toReturn = getJSONForExercises();
      } else if (matchesRequest(queryString, REMOVE_REF_RESULT)) {
        toReturn = removeRefResult(queryString);
      } else if (matchesRequest(queryString, REPORT)) {
        queryString = removePrefix(queryString, REPORT);
        int year = getYear(queryString);
        configureResponseHTML(response, year);
        reply(response, getReport(toReturn, year));
        return;
      } else if (matchesRequest(queryString, PHONE_REPORT)) {
        queryString = removePrefix(queryString, PHONE_REPORT);
        String[] split1 = queryString.split("&");
        if (split1.length < 2) {
          toReturn.put(ERROR, "expecting at least two query parameters");
        } else {
          toReturn = getPhoneReport(toReturn, split1);
        }
      } else {
        toReturn.put(ERROR, "unknown req " + queryString);
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    long now = System.currentTimeMillis();
    long l = now - then;
    if (l > 10) {
      logger.info("doGet : (" +getLanguage()+ ") took " + l + " millis to do " + request.getQueryString());
    }
    then = now;
    String x = toReturn.toString();
    now = System.currentTimeMillis();
    l = now - then;
    if (l > 50) {
      logger.info("doGet : (" +getLanguage()+ ") took " + l + " millis to do " + request.getQueryString() + " and to do toString on json");
    }

    reply(response, x);
  }

  /**
   * Worries about sql injection attack.
   * Remove ref result entry for an exercise, helpful if you want to clear the ref result for just one exercise
   * @param queryString
   * @return
   */
  private JSONObject removeRefResult(String queryString) {
    String[] split = queryString.split("=");
    if (split.length != 2) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("success", Boolean.valueOf(false).toString());
      jsonObject.put("error","Expecting exid");
      return jsonObject;
    }
    else {
      String exid = split[1];

      CommonExercise exercise = db.getExercise(exid);
      if (exercise == null) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", Boolean.valueOf(false).toString());
        jsonObject.put("error","no exercise with that id");
        return jsonObject;
      }
      else {
        boolean b = db.getRefResultDAO().removeForExercise(exid);
        //logger.info("Remove ref for " + exid + " got " + b);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", Boolean.valueOf(b).toString());
        return jsonObject;
      }
    }
  }

  private String getReport(JSONObject jsonObject, int year) {
    return db.getReport(year, jsonObject);
  }

  /**
   * Defaults to this year.
   *
   * @param queryString
   * @return
   */
  private int getYear(String queryString) {
    String[] split1 = queryString.split("&");
    int year = -1;
    if (split1.length == 2) {
      String param = split1[1];
//      logger.info("Got param " + param);
      int paramIntValue = getParamIntValue(param, YEAR);
      if (paramIntValue > 0) year = paramIntValue;
    }
   // else {
   //   year = Calendar.getInstance().get(Calendar.YEAR);
   // }
    return year;
  }

  private String removePrefix(String queryString, String prefix) {
    return queryString.substring(queryString.indexOf(prefix) + prefix.length());
  }

  private boolean matchesRequest(String queryString, String expected) {
    return queryString.startsWith(expected) || queryString.startsWith(REQUEST1 + expected);
  }

  /**
   * Check for a parameter to control what we send back
   *
   * @param queryString
   * @return
   */
  private String getRemoveExercisesParam(String queryString) {
    String[] split1 = queryString.split("&");
    if (split1.length == 2) {
      return getParamValue(split1[1], REMOVE_EXERCISES_WITH_MISSING_AUDIO);
    }
    return removeExercisesWithMissingAudioDefault ? "true" : "false";
  }

  private String getParamValue(String s, String param) {
    boolean hasParam = s.startsWith(param);
    if (!hasParam) {
      return "expecting param " + param;
    }
    return s.equals(param + "=true") ? "true" : "false";
  }

  private int getParamIntValue(String arg, String param) {
    boolean hasParam = arg.startsWith(param);
    if (!hasParam) {
      return -1;
    }
    String[] split = arg.split("=");
    try {
      if (split.length == 2) {
        return Integer.parseInt(split[1]);
      } else {
        return Integer.parseInt(arg);
      }
    } catch (NumberFormatException e) {
      logger.warn("got " + e + " on " + arg);
      return 0;
    }
  }


  /**
   * @param toReturn
   * @param split1
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getPhoneReport(JSONObject toReturn, String[] split1) {
    UserAndSelection userAndSelection = new UserAndSelection(split1).invoke();
    String user = userAndSelection.getUser();
    Map<String, Collection<String>> selection = userAndSelection.getSelection();

    logger.debug("getPhoneReport (" + getLanguage() + ") : user " + user + " selection " + selection);
    try {
      long then = System.currentTimeMillis();
      toReturn = db.getJsonPhoneReport(Long.parseLong(user), selection);
      long now = System.currentTimeMillis();
      if (now - then > 250) {
        logger.debug("getPhoneReport (" + getLanguage() + ") : user " + user + " selection " + selection +
            " took " + (now - then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
  }

  private String getLanguage() {
    getAudioFileHelper();

    return serverProps.getLanguage();
  }

  /**
   * @param queryString
   * @param toReturn
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getChapterHistory(String queryString, JSONObject toReturn) {
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      UserAndSelection userAndSelection = new UserAndSelection(split1).invoke();
      Map<String, Collection<String>> selection = userAndSelection.getSelection();

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        long l = Long.parseLong(userAndSelection.getUser());
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
      Map<String, Collection<String>> selection = new TreeMap<>();
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
   * Don't die if audio file helper is not available.
   *
   * @return
   * @see #doGet
   * @see #getChapterHistory(String, JSONObject)
   */
  private ExerciseSorter getExerciseSorter() {
    Map<String, Integer> stringIntegerHashMap = new HashMap<>();
    Map<String, Integer> phoneToCount = audioFileHelper == null ? stringIntegerHashMap : audioFileHelper.getPhoneToCount();
    return new ExerciseSorter(db.getSectionHelper().getTypeOrder(), phoneToCount);
  }

  /**
   * @param response
   * @param jsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void writeJsonToOutput(HttpServletResponse response, JSONObject jsonObject) {
    reply(response, jsonObject.toString());
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
      if (!requestType.startsWith(EVENT)) {
        logger.debug("doPost got request " + requestType + " device " + deviceType + "/" + device);
      }

      if (requestType.startsWith(ADD_USER)) {
        userManagement.addUser(request, requestType, deviceType, device, jsonObject);
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

        logger.warn("doPost unknown request " + requestType + " device " + deviceType + "/" + device);

      }
    } else {
      logger.info("request type is null?");
      jsonObject = getJsonForAudio(request, null, deviceType, device);
    }

    writeJsonToOutput(response, jsonObject);
  }

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);

    long userid = userManagement.getUserFromParam2(user);
    if (getUser(userid) == null) {
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

  private User getUser(long userid) {
    return db.getUserDAO().getUserWhere(userid);
  }

  private String getUserID(long userid) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    if (userWhere == null) logger.error("huh? can't find user by " + userid);
    return userWhere == null ? "" + userid : userWhere.getUserID();
  }

  /**
   * @param resultID
   * @param roundTripMillis
   * @param jsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void addRT(long resultID, int roundTripMillis, JSONObject jsonObject) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTripMillis);
    jsonObject.put("OK", "OK");
  }

  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
  }

  private void configureResponseHTML(HttpServletResponse response, int year) {
    response.setContentType("text/html; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    String fileName = year == -1 ? "reportFor" + getLanguage() : ("reportFor" +getLanguage()+"_forYear" + year);

    response.setHeader("Content-disposition", "attachment; filename=" +
        fileName + ".html");
  }

  /**
   * join against audio dao ex->audio map again to get user exercise audio! {@link JsonExport#getJsonArray(java.util.List)}
   *
   * @param removeExercisesWithMissingAudio
   * @return json for content
   * @see #doGet
   */
  private JSONObject getJsonNestedChapters(boolean removeExercisesWithMissingAudio) {
    JSONObject jsonObject = new JSONObject();

    long then = System.currentTimeMillis();
    JsonExport jsonExport = getJSONExport();
    long now = System.currentTimeMillis();
    if (now-then>1000) {
      logger.warn("getJsonNestedChapters " + getLanguage() + " getJSONExport took " + (now-then) + " millis");
    }
    then = now;

    jsonObject.put(CONTENT, jsonExport.getContentAsJson(removeExercisesWithMissingAudio));
    now = System.currentTimeMillis();
    if (now-then>1000) {
      logger.warn("getJsonNestedChapters " + getLanguage() + " getContentAsJson took " + (now-then) + " millis");
    }
    addVersion(jsonObject);

    return jsonObject;
  }

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @return
   */
  private JSONObject getJSONForExercises() {
    return getJSONExerciseExport(getJSONExport());
  }

  private JSONObject getJSONExerciseExport(JsonExport jsonExport) {
    JSONObject jsonObject = new JSONObject();
    addVersion(jsonObject);
    jsonExport.addJSONExerciseExport(jsonObject, db.getExercises());
    return jsonObject;
  }

  private JsonExport getJSONExport() {
    setInstallPath(db);
    db.getExercises();

    Map<String, Integer> stringIntegerMap = Collections.emptyMap();
    JsonExport jsonExport = new JsonExport(
        audioFileHelper == null ? stringIntegerMap : audioFileHelper.getPhoneToCount(),
        db.getSectionHelper(),
        serverProps.getPreferredVoices(),
        serverProps.getLanguage().equalsIgnoreCase("english")
    );

    db.attachAllAudio();
    return jsonExport;
  }

  /**
   * REALLY IMPORTANT.
   * <p>
   * Write the posted audio file to a location based on the user id and the exercise id.
   * If request type is decode, decode the file and return score info in json.
   * <p>
   * Allow alternate decode possibilities if header is set.
   * <p>
   * DON'T trim silence after it gets the file from the iPad/iPhone
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
        " req " + reqid + " device " + deviceType + "/" + device);

    int i = userManagement.getUserFromParam(user);
    String wavPath = pathHelper.getLocalPathToAnswer("plan", exerciseID, 0, i);
    File saveFile = pathHelper.getAbsoluteFile(wavPath);
    new File(saveFile.getParent()).mkdirs();

    String allow_alternates = request.getHeader("ALLOW_ALTERNATES");
    boolean allowAlternates = allow_alternates != null && !allow_alternates.equals("false");

    String use_phone_to_display = request.getHeader("USE_PHONE_TO_DISPLAY");
    boolean usePhoneToDisplay = use_phone_to_display != null && !use_phone_to_display.equals("false");

    writeToFile(request.getInputStream(), saveFile);

//    new AudioConversion(null).trimSilence(saveFile);

    return getJsonForAudioForUser(reqid, exerciseID, i, Request.valueOf(requestType.toUpperCase()), wavPath, saveFile,
        deviceType, device, allowAlternates, usePhoneToDisplay);
  }

  /**
   * @param request
   * @return
   * @see #getJsonForAudio(HttpServletRequest, String, String, String)
   */
  private int getReqID(HttpServletRequest request) {
    String reqid = request.getHeader(REQID);
    //logger.debug("got req id " + reqid);
    if (reqid == null) reqid = "1";
    try {
      //logger.debug("returning req id " + req);
      return Integer.parseInt(reqid);
    } catch (NumberFormatException e) {
      logger.warn("Got parse error on reqid " + reqid);
    }
    return 1;
  }

  /**
   * @param reqid             label response with req id so the client can tell if it got a stale response
   * @param exerciseID        for this exercise
   * @param user              by this user
   * @param request           not sure when this wouldn't be decode
   * @param wavPath           relative path to posted audio file
   * @param saveFile          File handle to file
   * @param deviceType        iPad,iPhone, or browser
   * @param device            id for device - helpful for iPads, etc.
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
        jsonForScore = getJsonForScore(pretestScore, usePhoneToDisplay);
        if (doFlashcard) {
          jsonForScore.put(IS_CORRECT, answer.isCorrect());
          jsonForScore.put(SAID_WORD, answer.isSaidAnswer());
          long decodeResultID = answer.getResultID();
          jsonForScore.put("resultID", decodeResultID);

          // attempt to get more feedback when we're too sensitive and match the unknown model
          if (!answer.isCorrect() && !answer.isSaidAnswer()) {
            answer = getAudioAnswerAlign(reqid, exerciseID, user, false, wavPath, saveFile, deviceType, device,
                exercise1, usePhoneToDisplay);
            PretestScore pretestScore1 = answer.getPretestScore();
            logger.debug("Alignment on an unknown model answer gets " + pretestScore1);
            //   logger.debug("score info " + answer.getPretestScore().getsTypeToEndTimes());
            jsonForScore = getJsonForScore(pretestScore1, usePhoneToDisplay);

            // so we mark it correct if the score is above 50% on alignment
            boolean isCorrect = pretestScore1.getHydecScore() > ALIGNMENT_SCORE_CORRECT;
            jsonForScore.put(IS_CORRECT, isCorrect);
            jsonForScore.put(SCORE, pretestScore1.getHydecScore());
            jsonForScore.put(SAID_WORD, false);   // don't say they said the word - decode says they didn't

            if (pretestScore1.getHydecScore() > 0.25) {
              logger.info("remember score for result " + decodeResultID);
              db.rememberScore(decodeResultID, pretestScore1, isCorrect);
            } else {
              logger.debug("skipping remembering alignment since score was too low " + pretestScore1.getHydecScore());
            }
          }
        }
      }
      addValidity(exerciseID, jsonForScore, answer);
    }
    return jsonForScore;
  }

  /**
   * @param exerciseID
   * @param jsonForScore
   * @param answer
   * @see #getJsonForAudioForUser(int, String, int, Request, String, File, String, String, boolean, boolean)
   */
  private void addValidity(String exerciseID, JSONObject jsonForScore, AudioAnswer answer) {
    jsonForScore.put(EXID, exerciseID);
    jsonForScore.put(VALID, answer == null ? INVALID : answer.getValidity().toString());
    jsonForScore.put(REQID, answer == null ? 1 : "" + answer.getReqid());
  }

  /**
   * @param reqid             label response with req id so the client can tell if it got a stale response
   * @param exerciseID        for this exercise
   * @param user              by this user
   * @param doFlashcard
   * @param wavPath           path to posted audio file
   * @param saveFile
   * @param deviceType
   * @param device
   * @param exercise1
   * @param allowAlternates
   * @param usePhoneToDisplay
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAudioAnswer(int reqid, String exerciseID, int user,
                                     boolean doFlashcard,
                                     String wavPath, File saveFile,
                                     String deviceType, String device, CommonExercise exercise1,
                                     boolean allowAlternates, boolean usePhoneToDisplay) {
    AudioAnswer answer;

    if (doFlashcard) {
      answer = getAnswer(reqid, exerciseID, user, doFlashcard, wavPath, saveFile, -1, deviceType, device, allowAlternates);
    } else {
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid, wavPath, exercise1.getForeignLanguage(), exerciseID,
          usePhoneToDisplay);
      answer = getAnswer(reqid, exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(),
          deviceType, device, allowAlternates);
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
  private AudioAnswer getAudioAnswerAlign(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath,
                                          File saveFile,
                                          String deviceType, String device, CommonExercise exercise1,
                                          boolean usePhoneToDisplay) {
    PretestScore asrScoreForAudio = getASRScoreForAudioNoCache(reqid, saveFile.getAbsolutePath(),
        exercise1.getForeignLanguage(), exerciseID, usePhoneToDisplay);
    AudioAnswer answer = getAnswer(reqid, exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(),
        deviceType, device, false);
    answer.setPretestScore(asrScoreForAudio);
    return answer;
  }

  /**
   * Don't wait for mp3 to write to return - can take 70 millis for a short file.
   *
   * @param reqid
   * @param exerciseID
   * @param user
   * @param doFlashcard
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @param allowAlternates
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAnswer(int reqid, String exerciseID, int user, boolean doFlashcard, String wavPath, File file, float score,
                                String deviceType, String device, boolean allowAlternates) {
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    AudioContext audioContext = new AudioContext(reqid, user, exerciseID, 0, doFlashcard ? "flashcard" : "learn");

    AudioAnswer answer = audioFileHelper.getAnswer(exercise1,
        audioContext,
        wavPath, file, deviceType, device, score, doFlashcard,
        allowAlternates);

    final String path = answer.getPath();
    final String foreignLanguage = exercise1.getForeignLanguage();

    ensureMP3Later(user, path, foreignLanguage);

    return answer;
  }

  private void ensureMP3Later(final int user, final String path, final String foreignLanguage) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        //long then = System.currentTimeMillis();
        ensureMP3(path, foreignLanguage, getUserID(user));
        // long now = System.currentTimeMillis();
        //       logger.debug("Took " + (now-then) + " millis to write mp3 version");
      }
    }).start();
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
        object.put(END, segment.getEnd());
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
      if (db != null) {
        serverProps = db.getServerProps();
        audioFileHelper = getAudioFileHelperRef();
        this.userManagement = new RestUserManagement(db, serverProps, pathHelper);
        removeExercisesWithMissingAudioDefault = serverProps.removeExercisesWithMissingAudio();
      }
    }
  }

  /**
   * @return
   * @see #getAudioFileHelper()
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
   * @return
   * @see #getAudioFileHelper()
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

/*  private LoadTesting getLoadTesting() {
    LoadTesting ref = null;
    Object databaseReference = getServletContext().getAttribute(LOAD_TESTING);
    if (databaseReference != null) {
      ref = (LoadTesting) databaseReference;
      // logger.debug("found existing audio file reference " + fileHelper + " under " + getServletContext());
    } else {
      logger.error("huh? for " + db.getServerProps().getLanguage() + " no existing load test reference?");
    }
    return ref;
  }*/

  /**
   * TODO : this is wacky -- have to do this for alignment but not for decoding
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   * @see #getAudioAnswer(int, String, int, boolean, String, File, String, String, CommonExercise, boolean, boolean)
   */
  private PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                           String exerciseID, boolean usePhoneToDisplay) {
    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, serverProps.useScoreCache(), exerciseID, null, usePhoneToDisplay, false);
  }

  /**
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   * @see #getAudioAnswerAlign(int, String, int, boolean, String, File, String, String, CommonExercise, boolean)
   */
  private PretestScore getASRScoreForAudioNoCache(int reqid, String testAudioFile, String sentence,
                                                  String exerciseID, boolean usePhoneToDisplay) {
    //  logger.debug("getASRScoreForAudioNoCache for " + testAudioFile + " under " + sentence);
    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, 128, 128, false,
        false, false, exerciseID, null, usePhoneToDisplay, false);
  }

  private void addVersion(JSONObject jsonObject) {
    jsonObject.put(VERSION, VERSION_NOW);
    jsonObject.put(HAS_MODEL, !db.getServerProps().isNoModel());
    jsonObject.put("Date", new Date().toString());
  }

  /**
   * @param db
   * @return
   * @see #getJsonNestedChapters(boolean)
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

  private class UserAndSelection {
    private final String[] split1;
    private String user;
    private Map<String, Collection<String>> selection;

    public UserAndSelection(String... split1) {
      this.split1 = split1;
    }

    public String getUser() {
      return user;
    }

    public Map<String, Collection<String>> getSelection() {
      return selection;
    }

    public UserAndSelection invoke() {
      user = "";
      selection = new TreeMap<>();
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
      return this;
    }
  }
}