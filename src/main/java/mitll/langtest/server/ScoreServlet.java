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
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.report.ReportingServices;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.json.ProjectExport;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.scoring.JsonScoring;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.common.DominoSessionException;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

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

  public static final String REQUEST = "request";

  private static final String CHAPTER_HISTORY = "chapterHistory";
  private static final String ROUND_TRIP1 = "roundTrip";
  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private static final String PHONE_REPORT = "phoneReport";
  private static final String ERROR = "ERROR";
  public static final String USER = "user";

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
  private static final String REQUEST1 = REQUEST + "=";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private static final String VERSION_NOW = "1.0";
  private static final String RESULT_ID = "resultID";
  private static final String USE_PHONE_TO_DISPLAY = "USE_PHONE_TO_DISPLAY";
  private static final String ALLOW_ALTERNATES = "ALLOW_ALTERNATES";
  private static final String EXERCISE = "exercise";
  public static final String EXERCISE_TEXT = "exerciseText";
  private static final String NO_SESSION = "no session";

  public static final String PASS = "pass";
  public static final String PROJID = "projid";
  public static final String USERID = "userid";
  public static final String ENGLISH = "english";
  public static final String LANGUAGE = "language";
  public static final String FULL = "full";
  private static final String WIDGET_TYPE = "widgetType";
  private static final boolean REPORT_ON_HEADERS = false;

  private boolean removeExercisesWithMissingAudioDefault = true;

  private RestUserManagement userManagement;

  public enum GetRequest {
    HASUSER,
    ADDUSER,
    PROJECTS,
    NESTED_CHAPTERS,
    CHAPTER_HISTORY,
    PHONE_REPORT,
    UNKNOWN
  }

  public enum PostRequest {EVENT, HASUSER, ADDUSER, ROUNDTRIP, DECODE, ALIGN, RECORD, WRITEFILE, UNKNOWN}

  private final Map<Integer, JSONObject> projectToNestedChaptersEverything = new HashMap<>();
  private final Map<Integer, Long> projectToWhenCachedEverything = new HashMap<>();

  private final Map<Integer, JSONObject> projectToNestedChapters = new HashMap<>();
  private final Map<Integer, Long> projectToWhenCached = new HashMap<>();

  private JsonScoring jsonScoring;

  /**
   * Must have a session... unless asking for projects, which we need up front.
   * <p>
   * How does iOS do a login?
   * <p>
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      makeAudioFileHelper();

      ReportingServices reportingServices = getDatabase();

      String queryString = getQuery(request);

      GetRequest realRequest = getGetRequest(queryString);
      logger.info("doGet query '" + queryString + "' = request " + realRequest);
      if (realRequest == GetRequest.PROJECTS) {
        reply(response, getProjects());
        return;
      } else if (realRequest == GetRequest.HASUSER) {
        logger.info("doGet got hasUser " + queryString);
        JSONObject toReturn = new JSONObject();
        checkUserAndLogin(request, toReturn);
        reply(response, toReturn);
        return;
      }/* else if (realRequest == GetRequest.ADDUSER) {
        logger.info("doGet got add user " + queryString);
        JSONObject toReturn = new JSONObject();
        checkUserAndLogin(request, toReturn);
        reply(response, toReturn);
        return;
      }
*/
      int userID = checkSession(request);
      int projid = getProjectID(request);

      // language overrides user id mapping...
      {
        String language = getLanguage(request);
        if (language != null) {
          projid = getProjectID(language);
          if (projid == -1) projid = getProjectID(request);
        }
      }

      // ok can't figure it out from language, check header.
      if (projid == -1) {
        projid = getProjID(request);
        logger.warn("doGet got project id from url header " + projid);
      }

      String language = projid == -1 ? "unknownLanguage" : getLanguage(projid);

      logger.info("doGet (" + language + "):" +
          "\n\trequest '" + queryString + "'" +
          "\n\tprojid  " + projid +
          "\n\tpath    " + request.getPathInfo() +
          "\n\turi     " + request.getRequestURI() +
          "\n\turl     " + request.getRequestURL() + "  " + request.getServletPath());

      if (REPORT_ON_HEADERS) reportOnHeaders(request);

      long then = System.currentTimeMillis();
      configureResponse(response);

      JSONObject toReturn = new JSONObject();
      String jsonString = "";
      //toReturn.put(ERROR, "expecting request");

      try {
        if (realRequest == GetRequest.NESTED_CHAPTERS) {
          String[] split1 = queryString.split("&");
          if (split1.length == 2) {
            String removeExercisesWithMissingAudio = getRemoveExercisesParam(queryString);
            boolean shouldRemoveExercisesWithNoAudio = removeExercisesWithMissingAudio.equals("true");
            boolean dontRemove = removeExercisesWithMissingAudio.equals("false");
            if (shouldRemoveExercisesWithNoAudio || dontRemove) {
              if (dontRemove) {
                JSONObject nestedChaptersEverything = projectToNestedChaptersEverything.get(projid);
                Long whenCachedEverything = projectToWhenCachedEverything.get(projid);
                if (nestedChaptersEverything == null ||
                    (System.currentTimeMillis() - whenCachedEverything > REFRESH_CONTENT_INTERVAL_THREE)) {
                  nestedChaptersEverything = getJsonNestedChapters(shouldRemoveExercisesWithNoAudio, projid);
                  projectToNestedChaptersEverything.put(projid, nestedChaptersEverything);
                  projectToWhenCachedEverything.put(projid, System.currentTimeMillis());
                }
                toReturn = nestedChaptersEverything;
              } else {
                toReturn = getJsonNestedChapters(true, projid);
              }
            } else {
              toReturn.put(ERROR, "expecting param " + REMOVE_EXERCISES_WITH_MISSING_AUDIO);
            }
          } else {
            JSONObject nestedChapters = projectToNestedChapters.get(projid);
            Long whenCached = projectToWhenCached.get(projid);

            if (nestedChapters == null || (System.currentTimeMillis() - whenCached > REFRESH_CONTENT_INTERVAL)) {
              nestedChapters = getJsonNestedChapters(true, projid);
              projectToNestedChapters.put(projid, nestedChapters);
              projectToWhenCached.put(projid, System.currentTimeMillis());
            }
            toReturn = nestedChapters;
          }
//        } else if (matchesRequest(queryString, HAS_USER)) {
//          logger.info("got doGet hasUser " + queryString);
//          checkUserAndLogin(request, toReturn);
        } else if (userManagement.doGet(
            request,
            response,
            queryString,
            toReturn
        )) {
          logger.info("doGet " + language + " handled user command");
        } else if (realRequest == GetRequest.CHAPTER_HISTORY) {
          queryString = removePrefix(queryString, CHAPTER_HISTORY);
          toReturn = getChapterHistory(queryString, toReturn, projid, userID);
//        } else if (realRequest ==  JSON_REPORT)) {
//          queryString = removePrefix(queryString, JSON_REPORT);
//          reportingServices.getReport(getYear(queryString), toReturn);
//        } else if (matchesRequest(queryString, EXPORT)) {
//          toReturn = getJSONForExercises(projid);
////        } else if (matchesRequest(queryString, REMOVE_REF_RESULT)) {
////          toReturn = removeRefResult(queryString);
//        } else if (matchesRequest(queryString, REPORT)) {
//          queryString = removePrefix(queryString, REPORT);
//          int year = getYear(queryString);
//          configureResponseHTML(response, year);
//          reply(response, reportingServices.getReport(year, toReturn));
//          return;
        } else if (realRequest == GetRequest.PHONE_REPORT) {
          queryString = removePrefix(queryString, PHONE_REPORT);
          String[] split1 = queryString.split("&");
          if (split1.length < 2) {
            toReturn.put(ERROR, "expecting at least two query parameters");
          } else {
            toReturn = getPhoneReport(toReturn, split1, projid, userID);
          }
        } else {
          toReturn.put(ERROR, "unknown req " + queryString);
        }
      } catch (Exception e) {
        logger.error(getLanguage(projid) + " : doing query " + queryString + " got " + e, e);
        reportingServices.getLogAndNotify().logAndNotifyServerException(e);
      }

      long now = System.currentTimeMillis();
      long l = now - then;
      if (l > 10) {
        logger.info("doGet : (" + language + ") took " + l + " millis");// to do " + request.getQueryString());
      }
      then = now;

      String respString = jsonString.isEmpty() ? toReturn.toString() : jsonString;
      now = System.currentTimeMillis();
      l = now - then;
      if (l > 50) {
        logger.info("doGet : (" + language + ") took " + l + " millis" +
            //" to do " + request.getQueryString() +
            " and to do toString on json");
      }

      reply(response, respString);
    } catch (DominoSessionException e) {
      logger.warn("doGet Got " + e);
      reply(response, NO_SESSION);
    } catch (Exception e) {
      logger.error("doGet Got " + e, e);
      db.logAndNotify(e);
      throw new IOException("doGet couldn't process request.", e);
    }
  }

  private final Set<String> notInteresting = new HashSet<>(Arrays.asList("Accept-Encoding",
      "Accept-Language",
      "accept",
      "connection",
      "password"));

  private void reportOnHeaders(HttpServletRequest request) {
    Enumeration<String> headerNames = request.getHeaderNames();
    Set<String> headers = new TreeSet<>();
    while (headerNames.hasMoreElements()) headers.add(headerNames.nextElement());
    List<String> collect = headers.stream().filter(name -> !notInteresting.contains(name)).collect(Collectors.toList());
    collect.forEach(header -> logger.info("\trequest header " + header + " = " + request.getHeader(header)));
  }


  private String getQuery(HttpServletRequest request) throws UnsupportedEncodingException {
    String queryString = request.getQueryString();
    if (queryString == null) {
      queryString = ""; // how could this happen???
    } else {
      queryString = URLDecoder.decode(request.getQueryString(), "UTF-8");
    }
    return queryString;
  }

  private int checkSession(HttpServletRequest request) throws DominoSessionException {
    int userIDFromSession = securityManager.getUserIDFromSessionLight(request);
    logger.info("doGet user id from session is " + userIDFromSession);
    return userIDFromSession;
  }

  /**
   * PRODUCTION instance - assume only one?
   *
   * @param language
   * @return
   */
  private int getProjectID(String language) {
    return getDAOContainer().getProjectDAO().getByLanguage(language);
  }

  private DAOContainer getDAOContainer() {
    return db;
  }

  /**
   * TODO : put this back - need to add project as an argument
   * Worries about sql injection attack.
   * Remove ref result entry for an exercise, helpful if you want to clear the ref result for just one exercise
   *
   * @return
   * @paramx queryString
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
 /* private JSONObject removeRefResult(String queryString) {
    String[] split = queryString.split("&");
    if (split.length != 3) {
      return getJsonResponse("Expecting exid and projid");
    } else {
      // http://netprof/sccoreServlet?removeRefResult&projid=2&exid=3
      // http://netprof/sccoreServlet?removeRefResult
      // projid=2
      // exid=3

      int exid = -1;
      int projid = -1;

      for (int arg = 1; arg < split.length; arg++) {
        {
          String[] pair = split[arg].split("=");
          String key = pair[0];
          String value = pair[1];
          try {
            int exid1 = Integer.parseInt(value);
            if (key.equals("exid")) exid = exid1;
            else if (key.equals("projid")) projid = exid1;
          } catch (NumberFormatException e) {
            return getJsonResponse("expecting integer arg not " + value);
          }
        }
      }

      CommonExercise exercise = db.getExercise(projid, exid);
      if (exercise == null) {
        logger.info("removeRefResult can't find '" + exid + "'");
        return getJsonResponse("no exercise with that id");
      } else {
        boolean b = getDAOContainer().getRefResultDAO().removeForExercise(exid);
        logger.info("removeRefResult Remove ref for " + exid + " got " + b);
        JSONObject jsonObject = new JSONObject();
        addSuccess(jsonObject, b);
        return jsonObject;
      }
    }
  }*/

/*
  @NotNull
  private JSONObject getJsonResponse(String message) {
    JSONObject jsonObject = new JSONObject();
    addSuccess(jsonObject, false);
    jsonObject.put(ERROR1, message);
    return jsonObject;
  }
  */

/*
  private void addSuccess(JSONObject jsonObject, boolean b) {
    jsonObject.put(SUCCESS, Boolean.valueOf(b).toString());
  }
*/

  /**
   * Defaults to this year.
   *
   * @param queryString
   * @return
   */
/*  private int getYear(String queryString) {
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
  }*/
  private String removePrefix(String queryString, String prefix) {
    return queryString.substring(queryString.indexOf(prefix) + prefix.length());
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

/*  private int getParamIntValue(String arg, String param) {
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
  }*/


  /**
   * @param toReturn
   * @param split1
   * @param projid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getPhoneReport(JSONObject toReturn, String[] split1, int projid, int userid) {
    Map<String, Collection<String>> selection = new UserAndSelection(split1).invoke().getSelection();

    logger.info("getPhoneReport : user " + userid + " selection " + selection + " proj " + projid);
    try {
      long then = System.currentTimeMillis();

      int projectID = getProjectID(projid, userid);
      toReturn = db.getJsonPhoneReport(userid, projectID, selection);
      long now = System.currentTimeMillis();
      if (now - then > 5) {
        logger.info("getPhoneReport :" +
            "\n\tuser      " + userid +
            "\n\tselection " + selection +
            "\n\tprojectID " + projectID +
            "\n\ttook      " + (now - then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
  }

  private int getProjectID(int projid, int userid) {
    return projid != -1 ? projid : getMostRecentProjectByUser(userid);
  }

  private String getProjects() {
    return new ProjectExport().toJSON(
        db.getProjectManagement().getProductionProjects(),
        db.getServerProps().getIOSVersion());
  }

  /**
   * @param queryString
   * @param toReturn
   * @param projectid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getChapterHistory(String queryString, JSONObject toReturn, int projectid, int userID) {
    logger.info("getChapterHistory for project " + projectid);
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.put(ERROR, "expecting at least two query parameters");
    } else {
      UserAndSelection userAndSelection = new UserAndSelection(split1).invoke();
      Map<String, Collection<String>> selection = userAndSelection.getSelection();

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        toReturn = db.getJsonScoreHistory(userID, selection, getExerciseSorter(projectid), projectid);
      } catch (NumberFormatException e) {
        toReturn.put(ERROR, "User id should be a number");
      }
    }
    return toReturn;
  }


  /**
   * Don't die if audio file helper is not available.
   *
   * @param projectid
   * @return
   * @see #doGet
   * @see #getChapterHistory
   */
  private ExerciseSorter getExerciseSorter(int projectid) {
    Map<String, Integer> stringIntegerHashMap = new HashMap<>();
    AudioFileHelper audioFileHelper = getAudioFileHelper(projectid);
    Map<String, Integer> phoneToCount = audioFileHelper == null ? stringIntegerHashMap : audioFileHelper.getPhoneToCount();
    return new ExerciseSorter(phoneToCount);
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

  private void reply(HttpServletResponse response, JSONObject jsonObject) {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    logger.info("ScoreServlet.doPost : Request " + request.getQueryString() +// " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    makeAudioFileHelper();

    configureResponse(response);

    String requestType = getRequestType(request);
    String deviceType = getOrUnk(request, DEVICE_TYPE);
    String device = getOrUnk(request, DEVICE);

    JSONObject jsonObject = new JSONObject();
    if (requestType != null) {
      PostRequest realRequest = getPostRequest(requestType);
      if (realRequest != PostRequest.EVENT) {
        logger.info("doPost got request " + requestType + "/" + realRequest + " device " + deviceType + "/" + device);
      }

      if (REPORT_ON_HEADERS) reportOnHeaders(request);

      switch (realRequest) {
        case HASUSER:  // when ?
          checkUserAndLogin(request, jsonObject);
          break;
        case ADDUSER:
          userManagement.addUser(request, requestType, deviceType, device, jsonObject);
          break;
        case ALIGN:
        case DECODE:
        case RECORD:
          try {
            jsonObject = getJsonForAudio(request, realRequest, deviceType, device);
          } catch (IOException e) {
            logger.error("doPost got " + e, e);
            jsonObject.put(ERROR, "got except " + e.getMessage());
          }
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
      try {
        jsonObject = getJsonForAudio(request, PostRequest.ALIGN, deviceType, device);
      } catch (Exception e) {
        logger.error("doPost got " + e, e);
        jsonObject.put(ERROR, "got except " + e.getMessage());
      }
    }

    writeJsonToOutput(response, jsonObject);
  }


  @NotNull
  private String getOrUnk(HttpServletRequest request, String deviceType1) {
    String deviceType = request.getHeader(deviceType1);
    if (deviceType == null) deviceType = "unk";
    return deviceType;
  }

  /**
   * @param request
   * @param jsonObject
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void checkUserAndLogin(HttpServletRequest request, JSONObject jsonObject) {
    reportOnHeaders(request);

    userManagement.tryToLogin(
        jsonObject,
        request,
        securityManager,

        getProjID(request),
        getUserID(request),
        getPass(request),
        false);
  }

  /**
   * @param requestType
   * @param deviceType
   * @param device
   * @param jsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
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

  private PostRequest getPostRequest(String requestType) {
    String s = requestType.toLowerCase();
    for (PostRequest request : PostRequest.values()) {
      if (s.startsWith(request.toString().toLowerCase())) return request;
    }

    return PostRequest.UNKNOWN;
  }

  private GetRequest getGetRequest(String requestType) {
    String lcReq = requestType.toLowerCase();
    if (lcReq.startsWith(REQUEST1)) {
      lcReq = lcReq.substring(REQUEST1.length());
    }
    GetRequest matched = GetRequest.UNKNOWN;

    for (GetRequest request : GetRequest.values()) {
      String prefix = request.toString().toLowerCase();

      if (lcReq.startsWith(prefix) || lcReq.startsWith(prefix.replaceAll("_", ""))) {
        //logger.info("getGetRequest lcReq '" + lcReq + "' vs '" + prefix + "'");
        matched = request;
        break;
      } else {
        //  logger.info("getGetRequest no match lcReq '" + lcReq + "' vs '" + prefix + "'");
      }
    }
//    logger.info("getGetRequest get req '" + lcReq + "' = " + matched);

    return matched;
  }

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = getUser(request);

    int userid = user == null ? -1 : userManagement.getUserFromParamWarnIfBad(user);
    if (getUser(userid) == null) {
      jsonObject.put(ERROR, "unknown user " + userid);
    } else {
      String context = request.getHeader(CONTEXT);
      String exid = request.getHeader(EXID);
      String widgetid = request.getHeader(WIDGET);
      String widgetType = request.getHeader(WIDGET_TYPE);

      //   logger.debug("doPost : Request " + requestType + " for " + deviceType + " user " + user + " " + exid);

      if (widgetid == null) {
        db.logEvent(exid == null ? "N/A" : exid, context, userid, device, -1);
      } else {
        db.logEvent(widgetid, widgetType, exid == null ? "N/A" : exid, context, userid, device, -1);
      }
    }
  }

  private User getUser(int userid) {
    return getDAOContainer().getUserDAO().getUserWhere(userid);
  }

  /**
   * @param resultID
   * @param roundTripMillis
   * @param jsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void addRT(int resultID, int roundTripMillis, JSONObject jsonObject) {
    getDAOContainer().getAnswerDAO().addRoundTrip(resultID, roundTripMillis);
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
/*  private void configureResponseHTML(HttpServletResponse response, int year) {
    response.setContentType("text/html; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
//    String language = "";
//    String fileName = year == -1 ? "reportFor" + language : ("reportFor" + language + "_forYear" + year);
    // response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".html");
  }*/

  /**
   * join against audio dao ex->audio map again to get user exercise audio! {@link JsonExport#getJsonArray}
   *
   * @param removeExercisesWithMissingAudio
   * @param projectid
   * @return json for content
   * @see #doGet
   */
  private JSONObject getJsonNestedChapters(boolean removeExercisesWithMissingAudio, int projectid) {

    if (projectid == -1) {
      logger.error("getJsonNestedChapters project id is not defined : " + projectid);
    } else {
      logger.debug("getJsonNestedChapters get content for project id " + projectid + " remove exercises " + removeExercisesWithMissingAudio);
    }

    long then = System.currentTimeMillis();
    JsonExport jsonExport = db.getJSONExport(projectid);
    long now = System.currentTimeMillis();
    if (now - then > 1000) {
      String language = getLanguage(projectid);
      logger.warn("getJsonNestedChapters " + language + " getJSONExport took " + (now - then) + " millis");
    }
    then = now;

    JSONObject jsonObject = new JSONObject();
    {
      jsonObject.put(CONTENT, jsonExport.getContentAsJson(removeExercisesWithMissingAudio));
      now = System.currentTimeMillis();
      if (now - then > 1000) {
        String language = getLanguage(projectid);
        logger.warn("getJsonNestedChapters " + language + " getContentAsJson took " + (now - then) + " millis");
      }
      addVersion(jsonObject, projectid);
    }
    return jsonObject;
  }

  private String getLanguage(int projectid) {
    Project project = getProject(projectid);
    return project == null ? ("" + projectid) : project.getLanguage();
  }

  /**
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
/*
  private JSONObject getJSONForExercises(int projectid) {
    return getJSONExerciseExport(db.getJSONExport(projectid), projectid);
  }
*/

/*  private JSONObject getJSONExerciseExport(JsonExport jsonExport, int projectid) {
    JSONObject jsonObject = new JSONObject();
    addVersion(jsonObject, projectid);
    jsonExport.addJSONExerciseExport(jsonObject, db.getExercises(projectid));
    return jsonObject;
  }*/

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
                                     PostRequest requestType,
                                     String deviceType,
                                     String device) throws IOException {
    // check session
    try {
      checkSession(request);
    } catch (DominoSessionException dse) {
      logger.info("got " + dse);
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("message", NO_SESSION);
      return jsonObject;
    }

    int realExID = 0;
    try {
      realExID = Integer.parseInt(getExerciseHeader(request));
      if (realExID == -1) {
        realExID = getDAOContainer().getUserExerciseDAO().getUnknownExercise().id();
        logger.info("getJsonForAudio : using unknown exercise id " + realExID);
      } else {
        logger.info("getJsonForAudio got exercise id " + realExID);
      }
    } catch (NumberFormatException e) {
      logger.info("couldn't parse exercise request header = '" + getExerciseHeader(request) + "'");
    }
    int reqid = getReqID(request);
    int projid = getProjectID(request);

    logger.debug("getJsonForAudio got projid from session " + projid);
    if (projid == -1) {
      projid = getProjID(request);
      logger.debug("getJsonForAudio got projid from request " + projid);
    }

    // language overrides user id mapping...
    {
      projid = getProjidFromLanguage(request, projid);
      realExID = getExerciseIDFromText(request, realExID, projid);
    }

    String user = getUser(request);
    int userid = userManagement.getUserFromParam(user);
    String fullJSONFormat = request.getHeader(FULL);

    logger.info("getJsonForAudio got" +
        "\n\trequest  " + requestType +
        "\n\tfor user " + user +
        "\n\tprojid  " + projid +
        "\n\texercise id   " + realExID +
        //"\n\texercise text " + realExID +
        "\n\treq      " + reqid +
        "\n\tfull     " + fullJSONFormat +
        "\n\tdevice   " + deviceType + "/" + device);

    File saveFile = writeAudioFile(request.getInputStream(), projid, realExID, userid);

    logger.info("getJsonForAudio save file to " + saveFile.getAbsolutePath());
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

  private String getRequestType(HttpServletRequest request) {
    return request.getHeader(REQUEST);
  }

  private int getProjID(HttpServletRequest request) {
    return request.getIntHeader(PROJID);
  }

  private String getExerciseHeader(HttpServletRequest request) {
    return request.getHeader(EXERCISE);
  }

  private String getUser(HttpServletRequest request) {
    return request.getHeader(USER);
  }

  private String getPass(HttpServletRequest request) {
    return request.getHeader(PASS);
  }

  private String getUserID(HttpServletRequest request) {
    return request.getHeader(USERID);
  }

  private String getLanguage(HttpServletRequest request) {
    return request.getHeader(LANGUAGE);
  }

  /**
   * Find the project by using the language header.
   * Or failing that, find the session user and use their current project.
   *
   * @param request
   * @param projid
   * @return
   * @see #getJsonForAudio
   **/
  private int getProjidFromLanguage(HttpServletRequest request, int projid) {
    String language = getLanguage(request);
    //logger.debug("getJsonForAudio got langauge from request " + language);

    if (language != null) {
      projid = getProjectID(language);
      logger.debug("getJsonForAudio got projid from language " + projid);

      if (projid == -1) {
        projid = getProjectID(request);
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
    String exerciseText = request.getHeader(ENGLISH);
    if (exerciseText == null) exerciseText = "";
    if (projid > 0) {
      Project project1 = getProject(projid);
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

  /**
   * @param inputStream
   * @param project
   * @param realExID
   * @param userid
   * @return
   * @throws IOException
   * @see #getJsonForAudio
   */
  @NotNull
  private File writeAudioFile(ServletInputStream inputStream, int project, int realExID, int userid) throws IOException {
    String wavPath = pathHelper.getAbsoluteToAnswer(
        getProject(project).getLanguage(),
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
    //private String user;
    private Map<String, Collection<String>> selection;

    UserAndSelection(String... split1) {
      this.split1 = split1;
    }

/*
    public String getUser() {
      return user;
    }
*/

    Map<String, Collection<String>> getSelection() {
      return selection;
    }

    UserAndSelection invoke() {
      // user = "";
      selection = new TreeMap<>();
      for (String param : split1) {
        //logger.debug("param '" +param+               "'");
        String[] split = param.split("=");
        if (split.length == 2) {
          String key = split[0];
          String value = split[1];
          if (key.equals(USER)) {
            //user = value;
          } else {
            selection.put(key, Collections.singleton(value));
          }
        }
      }
      return this;
    }
  }
}