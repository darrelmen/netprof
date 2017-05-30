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
import mitll.langtest.server.audio.DecoderOptions;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.json.ProjectExport;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.scoring.JsonScoring;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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
 * All in support of tethered iOS app.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = LogManager.getLogger(ScoreServlet.class);

  private static final String REQUEST = "request";
  private static final String NESTED_CHAPTERS = "nestedChapters";
  private static final String CHAPTER_HISTORY = "chapterHistory";
  // private static final String REF_INFO = "refInfo";
  private static final String ROUND_TRIP1 = "roundTrip";
  private static final String PHONE_REPORT = "phoneReport";
  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private static final String PROJECTS = "projects";

  private static final String ERROR = "ERROR";
  private static final String USER = "user";

  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  public static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000L;
  private static final long REFRESH_CONTENT_INTERVAL_THREE = 3 * 60 * 60 * 1000L;

  private static final String EXID = "exid";
  private static final String REQID = "reqid";
  private static final String VERSION = "version";
  private static final String CONTEXT = "context";
  private static final String WIDGET = "widget";
  private static final String REQUEST1 = "request=";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";

  private static final String YEAR = "year";
  private static final String JSON_REPORT = "jsonReport";
  private static final String REPORT = "report";
  private static final String VERSION_NOW = "1.0";
  private static final String EXPORT = "export";
  private static final String REMOVE_REF_RESULT = "removeRefResult";
  private static final String RESULT_ID = "resultID";
  private static final String USE_PHONE_TO_DISPLAY = "USE_PHONE_TO_DISPLAY";
  private static final String ALLOW_ALTERNATES = "ALLOW_ALTERNATES";
  private static final String EXERCISE = "exercise";
  public static final String EXERCISE_TEXT = "exerciseText";

  private boolean removeExercisesWithMissingAudioDefault = true;

  private RestUserManagement userManagement;

  public enum Request {EVENT, HASUSER, ADDUSER, ROUNDTRIP, DECODE, ALIGN, RECORD, WRITEFILE, UNKNOWN}

  private JSONObject nestedChapters;
  private JSONObject nestedChaptersEverything;
  private long whenCached = -1;
  private long whenCachedEverything = -1;

  //  private static final double ALIGNMENT_SCORE_CORRECT = 0.5;
  private JsonScoring jsonScoring;

  /**
   * Remembers chapters from previous requests...
   * <p>
   * OK, so we can make a number of requests - mainly for the iOS app.
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

    String passwordFromBody = "";
    int projid = getProject(request);

    // language overrides user id mapping...
    {
      String language = request.getHeader("language");
      if (language != null) {
        projid = getProjectID(language);
        if (projid == -1) projid = getProject(request);
      }
    }

    // ok can't figure it out from language, check header.
    if (projid == -1) {
      projid = request.getIntHeader("projid");
      logger.warn("doGet got project id from url header " + projid);
    }

    getDatabase();
    String language = projid == -1 ? "unknownLanguage" : getLanguage(projid);
    if (!queryString.contains(NESTED_CHAPTERS) || true) { // quiet output for polling from status webapp
      String pathInfo = request.getPathInfo();
      logger.debug("ScoreServlet.doGet (" + language + "):" +
          "\n\tRequest '" + queryString + "'" +
          "\n\tpath " + pathInfo +
          "\n\turi " + request.getRequestURI() +
          "\n\turl" + request.getRequestURL() + "  " + request.getServletPath());
    }

    long then = System.currentTimeMillis();
    configureResponse(response);

    makeAudioFileHelper();
    JSONObject toReturn = new JSONObject();
    String jsonString = "";
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
              if (nestedChaptersEverything == null ||
                  (System.currentTimeMillis() - whenCachedEverything > REFRESH_CONTENT_INTERVAL_THREE)) {
                nestedChaptersEverything = getJsonNestedChapters(shouldRemoveExercisesWithNoAudio, projid);
                whenCachedEverything = System.currentTimeMillis();
              }
              toReturn = nestedChaptersEverything;
            } else {
              toReturn = getJsonNestedChapters(true, projid);
            }
          } else {
            toReturn.put(ERROR, "expecting param " + REMOVE_EXERCISES_WITH_MISSING_AUDIO);
          }
        } else {
          if (nestedChapters == null || (System.currentTimeMillis() - whenCached > REFRESH_CONTENT_INTERVAL)) {
            nestedChapters = getJsonNestedChapters(true, projid);
            whenCached = System.currentTimeMillis();
          }
          toReturn = nestedChapters;
        }
      } else if (matchesRequest(queryString, "hasUser")) {

        logger.info("got doGet " + queryString);

        checkUserAndLogin(request, toReturn);
      } else if (userManagement.doGet(
          request,
          response,
          queryString,
          toReturn,
          securityManager,
          projid,
          passwordFromBody)) {
        logger.info("doGet " + language + " handled user command");// for " + queryString);
      } else if (matchesRequest(queryString, CHAPTER_HISTORY)) {
        queryString = removePrefix(queryString, CHAPTER_HISTORY);
        toReturn = getChapterHistory(queryString, toReturn, projid);
      } else if (matchesRequest(queryString, PROJECTS)) {
        queryString = removePrefix(queryString, PROJECTS);
        jsonString = getProjects();
//      } else if (matchesRequest(queryString, REF_INFO)) {
//        logger.warn("\n\n\n someone made this request " + REF_INFO);
//        queryString = removePrefix(queryString, REF_INFO);
//        toReturn = getRefInfo(queryString, toReturn);
      } else if (matchesRequest(queryString, JSON_REPORT)) {
        queryString = removePrefix(queryString, JSON_REPORT);
        int year = getYear(queryString);
        getReport(toReturn, year);
      } else if (matchesRequest(queryString, EXPORT)) {
        toReturn = getJSONForExercises(projid);
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
          toReturn = getPhoneReport(toReturn, split1, projid);
        }
      } else {
        toReturn.put(ERROR, "unknown req " + queryString);
      }
    } catch (Exception e) {
      logger.error(getLanguage(projid) + " : doing query " + queryString + " got " + e, e);
      db.logAndNotify(e);
    }

    long now = System.currentTimeMillis();
    long l = now - then;
    if (l > 10) {
      logger.info("doGet : (" + language + ") took " + l + " millis");// to do " + request.getQueryString());
    }
    then = now;
    String x = jsonString.isEmpty() ? toReturn.toString() : jsonString;
    now = System.currentTimeMillis();
    l = now - then;
    if (l > 50) {
      logger.info("doGet : (" + language + ") took " + l + " millis" +
          //" to do " + request.getQueryString() +
          " and to do toString on json");
    }

    reply(response, x);
  }

  private int getProjectID(String language) {
    return db.getProjectDAO().getByLanguage(language);
  }

/*  private int getIntValue(String projid) {
    int projectid = -1;
    String s = projid.split("&")[0];
    try {
      projectid = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      logger.error("getIntValue can't parse " + s);
    }
    return projectid;
  }*/

  /**
   * TODO : put this back - need to add project as an argument
   * Worries about sql injection attack.
   * Remove ref result entry for an exercise, helpful if you want to clear the ref result for just one exercise
   *
   * @param queryString
   * @return
   */
  private JSONObject removeRefResult(String queryString) {
    String[] split = queryString.split("=");
    if (split.length != 2) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("success", Boolean.valueOf(false).toString());
      jsonObject.put("error", "Expecting exid");
      return jsonObject;
    } else {
      String exid = split[1];
      int i = Integer.parseInt(exid);
      CommonExercise exercise = db.getExercise(-100, i);
      if (exercise == null) {
        logger.info("removeRefResult can't find '" + exid + "'");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", Boolean.valueOf(false).toString());
        jsonObject.put("error", "no exercise with that id");
        return jsonObject;
      } else {
        boolean b = db.getRefResultDAO().removeForExercise(i);
        logger.info("removeRefResult Remove ref for " + exid + " got " + b);
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
   * @param projid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getPhoneReport(JSONObject toReturn, String[] split1, int projid) {
    UserAndSelection userAndSelection = new UserAndSelection(split1).invoke();
    String user = userAndSelection.getUser();
    Map<String, Collection<String>> selection = userAndSelection.getSelection();

    logger.debug("getPhoneReport : user " + user + " selection " + selection);
    try {
      long then = System.currentTimeMillis();
      int userid = Integer.parseInt(user);

      int projectID = projid != -1 ? projid : getMostRecentProjectByUser(userid);
      toReturn = db.getJsonPhoneReport(userid, projectID, selection);
      long now = System.currentTimeMillis();
      if (now - then > 250) {
        logger.debug("getPhoneReport : user " + user + " selection " + selection +
            " took " + (now - then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
  }

  private String getProjects() {
    Collection<Project> productionProjects = db.getProjectManagement().getProductionProjects();
/*
    logger.info("getProjects got " + productionProjects.size() + " projects");
    for (Project project : productionProjects) logger.info(" project " + project);
*/
    return new ProjectExport().toJSON(productionProjects);
  }

  /**
   * @param queryString
   * @param toReturn
   * @param projectid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getChapterHistory(String queryString, JSONObject toReturn, int projectid) {
    logger.info("getChapterHistory for project " + projectid);
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      UserAndSelection userAndSelection = new UserAndSelection(split1).invoke();
      Map<String, Collection<String>> selection = userAndSelection.getSelection();

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        int l = Integer.parseInt(userAndSelection.getUser());
        toReturn = db.getJsonScoreHistory(l, selection, getExerciseSorter(projectid), projectid);
      } catch (NumberFormatException e) {
        toReturn.put(ERROR, "User id should be a number");
      }
    }
    return toReturn;
  }

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @param queryString
   * @param toReturn
   * @return
   * @deprecatedx not clear who calls this!
   */
/*  private JSONObject getRefInfo(String queryString, JSONObject toReturn) {
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
      toReturn = db.getJsonRefResult(selection, 1);
    }
    return toReturn;
  }*/

  /**
   * Don't die if audio file helper is not available.
   *
   * @param projectid
   * @return
   * @see #doGet
   * @see #getChapterHistory(String, JSONObject, int)
   */
  private ExerciseSorter getExerciseSorter(int projectid) {
    Map<String, Integer> stringIntegerHashMap = new HashMap<>();
    AudioFileHelper audioFileHelper = getAudioFileHelper(projectid);
    Map<String, Integer> phoneToCount = audioFileHelper == null ? stringIntegerHashMap : audioFileHelper.getPhoneToCount();
    return new ExerciseSorter(db.getSectionHelper(projectid).getTypeOrder(), phoneToCount);
  }

  private AudioFileHelper getAudioFileHelper(int projectid) {
    return getProject(projectid).getAudioFileHelper();
  }

  /**
   * @param response
   * @param jsonObject
   * @see #(HttpServletRequest, HttpServletResponse)
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
      db.logAndNotify(e);
    }
  }

  /**
   * Here's where we get posts from the iOS app.
   * Requests are:
   * 1) Add user
   * 2) Align/decode/record audio - record supports appen corpora recording
   * 3) Log UI event (e.g. a button click)
   * 4) Add round trip bookkeeping - so we can how long the user had to wait for a response
   * 5)
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
    makeAudioFileHelper();

    configureResponse(response);

    String requestType = request.getHeader(REQUEST);
    String deviceType = getOrUnk(request, DEVICE_TYPE);
    String device = getOrUnk(request, DEVICE);

    JSONObject jsonObject = new JSONObject();
    if (requestType != null) {
      Request realRequest = getRequest(requestType);
      if (realRequest != Request.EVENT) {
        logger.debug("doPost got request " + requestType + " device " + deviceType + "/" + device);
      }

      switch (realRequest) {
        case HASUSER:
          checkUserAndLogin(request, jsonObject);
          break;
        case ADDUSER:
          userManagement.addUser(request, requestType, deviceType, device, jsonObject);
          break;
        case ALIGN:
        case DECODE:
        case RECORD:
          jsonObject = getJsonForAudio(request, realRequest, deviceType, device);
          break;
        case EVENT:
          gotLogEvent(request, device, jsonObject);
          break;
        case ROUNDTRIP:
          addRT(request, jsonObject);
          break;
        default:
          gotUnknown(requestType, deviceType, device, jsonObject);
          break;
      }
    } else {
      logger.info("doPost request type is null - assume align.");
      jsonObject = getJsonForAudio(request, Request.ALIGN, deviceType, device);
    }

    writeJsonToOutput(response, jsonObject);
  }

  @NotNull
  private String getOrUnk(HttpServletRequest request, String deviceType1) {
    String deviceType = request.getHeader(deviceType1);
    if (deviceType == null) deviceType = "unk";
    return deviceType;
  }

  private void checkUserAndLogin(HttpServletRequest request, JSONObject jsonObject) {
    userManagement.tryToLogin(
        jsonObject,
        request.getHeader("pass"),
        request,
        securityManager,
        request.getIntHeader("projid"),
        request.getHeader("userid")
    );
  }

  private void gotUnknown(String requestType, String deviceType, String device, JSONObject jsonObject) {
    jsonObject.put(ERROR, "unknown req " + requestType);
    logger.warn("doPost unknown request " + requestType + " device " + deviceType + "/" + device);
  }

  private void addRT(HttpServletRequest request, JSONObject jsonObject) {
    String resultID = request.getHeader(RESULT_ID);
    String roundTripMillis = request.getHeader(ROUND_TRIP1);

    try {
      addRT(Integer.parseInt(resultID), Integer.parseInt(roundTripMillis), jsonObject);
    } catch (NumberFormatException e) {
      jsonObject.put(ERROR, "bad param format " + e.getMessage());
    }
  }

  private Request getRequest(String requestType) {
    for (Request request : Request.values()) {
      if (requestType.startsWith(request.toString().toLowerCase())) return request;
    }
    return Request.UNKNOWN;
  }

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);

    int userid = user == null ? -1 : userManagement.getUserFromParamWarnIfBad(user);
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

  private User getUser(int userid) {
    return db.getUserDAO().getUserWhere(userid);
  }

  /**
   * @param resultID
   * @param roundTripMillis
   * @param jsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void addRT(int resultID, int roundTripMillis, JSONObject jsonObject) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTripMillis);
    jsonObject.put("OK", "OK");
  }

  /**
   * @param response
   */
  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
  }

  /**
   * @param response
   * @param year
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private void configureResponseHTML(HttpServletResponse response, int year) {
    response.setContentType("text/html; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    String language = "";
    String fileName = year == -1 ? "reportFor" + language : ("reportFor" + language + "_forYear" + year);

    response.setHeader("Content-disposition", "attachment; filename=" +
        fileName + ".html");
  }

  /**
   * join against audio dao ex->audio map again to get user exercise audio! {@link JsonExport#getJsonArray}
   *
   * @param removeExercisesWithMissingAudio
   * @param projectid
   * @return json for content
   * @see #doGet
   */
  private JSONObject getJsonNestedChapters(boolean removeExercisesWithMissingAudio, int projectid) {
    JSONObject jsonObject = new JSONObject();

    long then = System.currentTimeMillis();

    if (projectid == -1) {
      logger.error("getJsonNestedChapters project id is not defined : " + projectid);
    } else {
      logger.debug("getJsonNestedChapters get content for project id " + projectid);
    }

    JsonExport jsonExport = getJSONExport(projectid);
    String language = getLanguage(projectid);
    long now = System.currentTimeMillis();
    if (now - then > 1000) {
      logger.warn("getJsonNestedChapters " + language + " getJSONExport took " + (now - then) + " millis");
    }
    then = now;

    jsonObject.put(CONTENT, jsonExport.getContentAsJson(removeExercisesWithMissingAudio));
    now = System.currentTimeMillis();
    if (now - then > 1000) {
      logger.warn("getJsonNestedChapters " + language + " getContentAsJson took " + (now - then) + " millis");
    }
    addVersion(jsonObject, projectid);

    return jsonObject;
  }

  private String getLanguage(int projectid) {
    return getProject(projectid).getLanguage();
  }

  /**
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getJSONForExercises(int projectid) {
    return getJSONExerciseExport(getJSONExport(projectid), projectid);
  }

  private JSONObject getJSONExerciseExport(JsonExport jsonExport, int projectid) {
    JSONObject jsonObject = new JSONObject();
    addVersion(jsonObject, projectid);
    jsonExport.addJSONExerciseExport(jsonObject, db.getExercises(projectid));
    return jsonObject;
  }

  /**
   * Install path, etc. should have been done by now
   *
   * @param projectid
   * @return
   */
  private JsonExport getJSONExport(int projectid) {
    db.getExercises(projectid);

    Map<String, Integer> stringIntegerMap = Collections.emptyMap();
    AudioFileHelper audioFileHelper = getAudioFileHelper(projectid);

    JsonExport jsonExport = new JsonExport(
        audioFileHelper == null ? stringIntegerMap : audioFileHelper.getPhoneToCount(),
        db.getSectionHelper(projectid),
        serverProps.getPreferredVoices(),
        getLanguage(projectid).equalsIgnoreCase("english")
    );

    db.attachAllAudio(projectid);
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
   * @return JSON representing response
   * @throws IOException
   * @see #doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private JSONObject getJsonForAudio(HttpServletRequest request,
                                     Request requestType,
                                     String deviceType,
                                     String device) throws IOException {
    int realExID = 0;
    try {
      realExID = Integer.parseInt(request.getHeader(EXERCISE));
      if (realExID == -1) {
        realExID = db.getUserExerciseDAO().getUnknownExercise().id();
        logger.info("getJsonForAudio : using unknown exercise id " + realExID);
      } else {
        logger.info("getJsonForAudio got exercise id " + realExID);
      }
    } catch (NumberFormatException e) {
      logger.info("couldn't parse " + request.getHeader(EXERCISE));
    }
    int reqid = getReqID(request);
    int projid = getProject(request);

    logger.debug("getJsonForAudio got projid from session " + projid);
    if (projid == -1) {
      projid = request.getIntHeader("projid");
      logger.debug("getJsonForAudio got projid from request " + projid);
    }

    // language overrides user id mapping...
    {
      projid = getProjidFromLanguage(request, projid);
      realExID = getExerciseIDFromText(request, realExID, projid);
    }

    String user = request.getHeader(USER);
    int userid = userManagement.getUserFromParam(user);
    String fullJSONFormat = request.getHeader("full");

    logger.debug("getJsonForAudio got" +
        "\n\trequest  " + requestType +
        "\n\tfor user " + user +
        "\n\tprojid  " + projid +
        "\n\texercise id   " + realExID +
        //"\n\texercise text " + realExID +
        "\n\treq      " + reqid +
        "\n\tfull     " + fullJSONFormat +
        "\n\tdevice   " + deviceType + "/" + device);

    File saveFile = writeAudioFile(request.getInputStream(), projid, realExID, userid);

    // TODO : put back trim silence? or is it done somewhere else
//    new AudioConversion(null).trimSilence(saveFile);

    return jsonScoring.getJsonForAudioForUser(
        reqid,
        projid,
        realExID,
        userid,
        requestType,
        saveFile.getAbsolutePath(),
        saveFile,
        deviceType,
        device,
        new DecoderOptions()
            .setAllowAlternates(getAllowAlternates(request))
            .setUsePhoneToDisplay(getUsePhoneToDisplay(request)),
        fullJSONFormat != null);
  }

  /**
   * Find the project by using the language header.
   * Or failing that, find the session user and use their current project.
   *
   * @param request
   * @param projid
   * @return
   * @see #getJsonForAudio
   */
  private int getProjidFromLanguage(HttpServletRequest request, int projid) {
    String language = request.getHeader("language");
    //logger.debug("getJsonForAudio got langauge from request " + language);

    if (language != null) {
      projid = getProjectID(language);
      logger.debug("getJsonForAudio got projid from language " + projid);

      if (projid == -1) {
        projid = getProject(request);
        logger.debug("getJsonForAudio got projid from request again " + projid);
      }
    }
    return projid;
  }

  /**
   * @param request
   * @param realExID
   * @param projid
   * @return
   * @see #getJsonForAudio
   */
  private int getExerciseIDFromText(HttpServletRequest request, int realExID, int projid) {
    String exerciseText = request.getHeader("english");
    if (exerciseText == null) exerciseText = "";
    if (projid > 0) {
      Project project1 = db.getProject(projid);
      String flText = request.getHeader(EXERCISE_TEXT);
      if (flText == null) {
        logger.info("getExerciseIDFromText no optional header " + EXERCISE_TEXT);
        return realExID;
      } else {
        String decoded = new String(Base64.getDecoder().decode(flText.getBytes()));

        logger.info("getExerciseIDFromText request to decode '" + exerciseText + "' = '" + decoded + "'");

        CommonExercise exercise = project1.getExerciseBySearchBoth(exerciseText.trim(), decoded.trim());

        if (exercise != null) {
          logger.info("getExerciseIDFromText for '" + exerciseText + "' '" + decoded + "' found exercise id " + exercise.getID());
          realExID = exercise.getID();
        } else {
          logger.warn("getExerciseIDFromText can't find exercise for '" + exerciseText + "'='" + flText + "' - using unknown exercise");
        }
      }
    }
    return realExID;
  }

  @NotNull
  private File writeAudioFile(ServletInputStream inputStream, int project, int realExID, int userid) throws IOException {
    String wavPath = pathHelper.getAbsoluteToAnswer(
        db.getProject(project).getLanguage(),
        realExID,
        userid);
    File saveFile = new File(wavPath);
    makeFileSaveDir(saveFile);

    writeToFile(inputStream, saveFile);
    return saveFile;
  }

  private void makeFileSaveDir(File saveFile) {
    File parent = new File(saveFile.getParent());
    boolean mkdirs = parent.mkdirs();
    if (!mkdirs && !parent.exists()) {
      logger.error("Couldn't make " + parent.getAbsolutePath() + " : permissions set? chown done ?");
    }
  }

  private boolean getUsePhoneToDisplay(HttpServletRequest request) {
    return getParam(request, USE_PHONE_TO_DISPLAY);
  }

  private boolean getAllowAlternates(HttpServletRequest request) {
    return getParam(request, ALLOW_ALTERNATES);
  }

  private boolean getParam(HttpServletRequest request, String param) {
    String use_phone_to_display = request.getHeader(param);
    return use_phone_to_display != null && !use_phone_to_display.equals("false");
  }

  /**
   * @param request
   * @return
   * @see #getJsonForAudio
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
   * TODO: Get audio file helper on project choice
   * Get a reference to the current database object, made in the main LangTestDatabaseImpl servlet
   * TODO : Latchy.
   *
   * @return
   * @see #doGet
   */
  private void makeAudioFileHelper() {
    if (userManagement == null) {
      db = getDatabase();
      if (db != null) {
        setPaths();
        ServerProperties serverProps = db.getServerProps();
        this.userManagement = new RestUserManagement(db, serverProps, pathHelper);
        removeExercisesWithMissingAudioDefault = serverProps.removeExercisesWithMissingAudio();

        jsonScoring = new JsonScoring(db);
      }
    }
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


  private void addVersion(JSONObject jsonObject, int projid) {
    jsonObject.put(VERSION, VERSION_NOW);
    jsonObject.put(HAS_MODEL, getProject(projid).hasModel());
    jsonObject.put("Date", new Date().toString());
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }

  private static class UserAndSelection {
    private final String[] split1;
    private String user;
    private Map<String, Collection<String>> selection;

    UserAndSelection(String... split1) {
      this.split1 = split1;
    }

    public String getUser() {
      return user;
    }

    public Map<String, Collection<String>> getSelection() {
      return selection;
    }

    UserAndSelection invoke() {
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