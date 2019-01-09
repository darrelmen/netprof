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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.report.ReportingServices;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.json.ProjectExport;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.scoring.JsonScoring;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.IUserListLight;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
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

  private static final String CHAPTER_HISTORY = "chapterHistory";
  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private static final String PHONE_REPORT = "phoneReport";
  private static final String ERROR = "ERROR";

  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  private static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000L;
  private static final long REFRESH_CONTENT_INTERVAL_THREE = 3 * 60 * 60 * 1000L;

  private static final String EXID = "exid";
  private static final String REQID = "reqid";
  private static final String VERSION = "version";
  private static final String CONTEXT = "context";
  private static final String WIDGET = "widget";

  private static final String REQUEST1 = HeaderValue.REQUEST.toString() + "=";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";

  /**
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private static final String VERSION_NOW = "1.0";
  private static final String USE_PHONE_TO_DISPLAY = "USE_PHONE_TO_DISPLAY";
  private static final String ALLOW_ALTERNATES = "ALLOW_ALTERNATES";
  private static final String NO_SESSION = "no session";

  private static final boolean REPORT_ON_HEADERS = false;
  /**
   * for now we force decode requests into forced alignment... better to do false positive than false negative
   */
  private static final boolean CONVERT_DECODE_TO_ALIGN = true;
  private static final String MESSAGE = "message";
  private static final String UTF_8 = "UTF-8";
  public static final String LIST = "list";

  private boolean removeExercisesWithMissingAudioDefault = true;

  private RestUserManagement userManagement;

  public enum GetRequest {
    HASUSER,
    //ADDUSER,
    PROJECTS,
    NESTED_CHAPTERS,
    CHAPTER_HISTORY,
    PHONE_REPORT,
    LIST,
    QUIZ,
    CONTENT,
    UNKNOWN
  }

  public enum PostRequest {
    EVENT,
    HASUSER,  // ??? why here too ???
    ADDUSER,
    SETPROJECT,
    ROUNDTRIP,
    /**
     * @see #doPost
     */
    DECODE,
    /**
     * @see #doPost
     */
    ALIGN,
    RECORD,
    WRITEFILE,
    STREAM,
    UNKNOWN
  }

  public enum HeaderValue {
    PASS,
    PROJID,
    USER,
    USERID,
    REQUEST,
    EXERCISE,
    EXERCISE_TEXT("exerciseText"),
    ENGLISH,
    LANGUAGE,
    FULL,
    WIDGET_TYPE("widgetType"),
    RESULT_ID("resultID"),
    ROUND_TRIP1("roundTrip"),
    DIALOGSESSION,
    RECORDINGSESSION,

    AUDIOTYPE,
    ISREFERENCE,
    STREAMSESSION,
    STREAMSTATE,
    STREAMSPACKET,
    STREAMSTOP,
    STREAMTIMESTAMP;  // when it's sent

    String value = null;

    HeaderValue() {
    }

    HeaderValue(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value == null ? name().toLowerCase() : value;
    }
  }

  private final Map<Integer, JsonObject> projectToNestedChaptersEverything = new HashMap<>();
  private final Map<Integer, Long> projectToWhenCachedEverything = new HashMap<>();

  private final Map<Integer, JsonObject> projectToNestedChapters = new HashMap<>();
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
      long then = System.currentTimeMillis();
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
        JsonObject toReturn = new JsonObject();
        checkUserAndLogin(request, toReturn);
        reply(response, toReturn);
        return;
      }

      JsonObject toReturn = new JsonObject();

      // fix for https://gh.ll.mit.edu/DLI-LTEA/iOSNetProF/issues/28
      if (userManagement.doGet(
          request,
          queryString,
          toReturn
      )) {
        configureResponse(response);

        logger.info("doGet handled user command");

        reply(response, "", then, toReturn);
        return;
      }

      int userID = checkSession(request);

      // so, first check header - no race!
      // if not on header, check language
      // if no header projid and no language requested, lookup
      int projid = getTripleProjID(request);

      String language = projid == -1 ? "unknownLanguage" : getLanguage(projid);

      logger.info("doGet (" + language + "):" +
          "\n\trequest '" + queryString + "'" +
          "\n\tprojid  " + projid +
          "\n\tpath    " + request.getPathInfo() +
          "\n\turi     " + request.getRequestURI() +
          "\n\turl     " + request.getRequestURL() + "  " + request.getServletPath());

      if (REPORT_ON_HEADERS) reportOnHeaders(request);

      configureResponse(response);

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
                JsonObject nestedChaptersEverything = projectToNestedChaptersEverything.get(projid);
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
              toReturn.addProperty(ERROR, "expecting param " + REMOVE_EXERCISES_WITH_MISSING_AUDIO);
            }
          } else {
            JsonObject nestedChapters = projectToNestedChapters.get(projid);
            Long whenCached = projectToWhenCached.get(projid);

            if (nestedChapters == null || (System.currentTimeMillis() - whenCached > REFRESH_CONTENT_INTERVAL)) {
              nestedChapters = getJsonNestedChapters(true, projid);
              projectToNestedChapters.put(projid, nestedChapters);
              projectToWhenCached.put(projid, System.currentTimeMillis());
            }
            toReturn = nestedChapters;
          }
        } else if (realRequest == GetRequest.CHAPTER_HISTORY) {
          queryString = removePrefix(queryString, CHAPTER_HISTORY);
          toReturn = getChapterHistory(queryString, toReturn, projid, userID);
        } else if (realRequest == GetRequest.PHONE_REPORT) {
          queryString = removePrefix(queryString, PHONE_REPORT);
          String[] split1 = queryString.split("&");
          if (split1.length < 2) {
            toReturn.addProperty(ERROR, "expecting at least two query parameters");
          } else {
            toReturn = getPhoneReport(toReturn, split1, projid, userID);
          }
        } else if (realRequest == GetRequest.LIST) {
          toReturn = db.getUserListManager().getListsJson(userID, projid, false);
        } else if (realRequest == GetRequest.QUIZ) {
          toReturn = db.getUserListManager().getListsJson(userID, projid, true);
        } else if (realRequest == GetRequest.CONTENT) {
          int listid = getListParam(queryString);
          logger.info("list id " +listid);
          toReturn =  getJsonForListContent(true, projid, listid);
        } else {
          toReturn.addProperty(ERROR, "unknown req " + queryString);
        }
      } catch (Exception e) {
        logger.error(getLanguage(projid) + " : doing query " + queryString + " got " + e, e);
        reportingServices.getLogAndNotify().logAndNotifyServerException(e);
      }

      reply(response, language, then, toReturn);
    } catch (DominoSessionException e) {
      logger.warn("doGet Got " + e);
      reply(response, NO_SESSION);
    } catch (Exception e) {
      logger.error("doGet Got " + e, e);
      db.logAndNotify(e);
      throw new IOException("doGet couldn't process request.", e);
    }
  }

  private int getListParam(String queryString) {
    String[] split1 = queryString.split("&");
    int listid=-1;
    for (String arg:split1) {
      String[] split = arg.split("=");
      if (split.length == 2) {
        String key = split[0];
        if (key.equals(LIST)) {
          String value = split[1];
          try {
            listid =Integer.parseInt(value);
          } catch (NumberFormatException e) {
            logger.warn("can't parse " + value);
          }
        }
      }
    }
    return listid;
  }

  /**
   * first check header - no race!
   * if not on header, check language
   * if no header projid and no language requested, lookup
   *
   * @param request
   * @return
   */
  private int getTripleProjID(HttpServletRequest request) {
    int projid = getProjID(request);

    // language overrides user id mapping...
    {
      String language = getLanguage(request);
      if (language != null) {
        projid = getProjectID(language);
      }
    }

    if (projid == -1) {
      // not using header
      projid = getProjectID(request);
    }
    return projid;
  }

  /**
   * @param response
   * @param language just for debugging
   * @param then
   * @param toReturn
   */
  private void reply(HttpServletResponse response, String language, long then, JsonObject toReturn) {
    long now = System.currentTimeMillis();
    long l = now - then;
    if (l > 10) {
      logger.info("doGet : (" + language + ") took " + l + " millis");// to do " + request.getQueryString());
    }
    then = now;

    String respString = toReturn.toString();
    now = System.currentTimeMillis();
    l = now - then;
    if (l > 50) {
      logger.info("reply : (" + language + ") took " + l + " millis" +
          //" to do " + request.getQueryString() +
          " and to do toString on json");
    }

    reply(response, respString);
  }

  private final Set<String> notInteresting = new HashSet<>(Arrays.asList(
      "Accept-Encoding",
      "Accept-Language",
      "accept",
      "connection",
      "password",
      "pass"));

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
      queryString = URLDecoder.decode(request.getQueryString(), UTF_8);
    }
    return queryString;
  }

  /**
   * @param request
   * @return
   * @throws DominoSessionException
   * @see #doGet
   */
  private int checkSession(HttpServletRequest request) throws DominoSessionException {
    int userIDFromSession = securityManager.getUserIDFromSessionLight(request);
//    logger.info("checkSession user id from session is " + userIDFromSession);
    return userIDFromSession;
  }

  /**
   * PRODUCTION instance - assume only one?
   *
   * @param language
   * @return
   */
  private int getProjectID(String language) {
    return getDAOContainer().getProjectDAO().getByLanguageProductionOnly(language);
  }

  private DAOContainer getDAOContainer() {
    return db;
  }

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

  /**
   * @param toReturn
   * @param split1
   * @param projid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JsonObject getPhoneReport(JsonObject toReturn, String[] split1, int projid, int userid) {
    Map<String, Collection<String>> selection = new UserAndSelection(split1).invoke().getSelection();

    logger.info("getPhoneSummary : user " + userid + " selection " + selection + " proj " + projid);
    try {
      long then = System.currentTimeMillis();

      int projectID = getProjectID(projid, userid);
      toReturn = db.getJsonPhoneReport(userid, projectID, selection);
      long now = System.currentTimeMillis();
      if (now - then > 5) {
        logger.info("getPhoneSummary :" +
            "\n\tuser      " + userid +
            "\n\tselection " + selection +
            "\n\tprojectID " + projectID +
            "\n\ttook      " + (now - then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.addProperty(ERROR, "User id should be a number");
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
  private JsonObject getChapterHistory(String queryString, JsonObject toReturn, int projectid, int userID) {
    logger.info("getChapterHistory for project " + projectid);
    String[] split1 = queryString.split("&");
    if (split1.length < 2) {
      toReturn.addProperty(ERROR, "expecting at least two query parameters");
    } else {
      Map<String, Collection<String>> selection = new UserAndSelection(split1).invoke().getSelection();

      //logger.debug("chapterHistory " + user + " selection " + selection);
      try {
        toReturn = db.getJsonScoreHistory(userID, projectid, selection, getExerciseSorter(projectid));
      } catch (NumberFormatException e) {
        toReturn.addProperty(ERROR, "User id should be a number");
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
   * @param JsonObject
   * @see #(HttpServletRequest, HttpServletResponse)
   */
  private void writeJsonToOutput(HttpServletResponse response, JsonObject JsonObject) {
    reply(response, JsonObject.toString());
  }

  private void reply(HttpServletResponse response, JsonObject JsonObject) {
    reply(response, JsonObject.toString());
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    long then = System.currentTimeMillis();
//    logger.info("ScoreServlet.doPost : Request " + request.getQueryString() +// " path " + pathInfo +
//        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    makeAudioFileHelper();

    configureResponse(response);

    String requestType = getRequestType(request);
    String deviceType = getOrUnk(request, DEVICE_TYPE);
    String device = getOrUnk(request, DEVICE);

    JsonObject JsonObject = new JsonObject();
    if (requestType != null) {
      PostRequest realRequest = getPostRequest(requestType);
      if (realRequest != PostRequest.EVENT) {
        logger.info("doPost got request " + requestType + "/" + realRequest + " device " + deviceType + "/" + device);
      }

      if (REPORT_ON_HEADERS) reportOnHeaders(request);

      switch (realRequest) {
        case HASUSER:  // when ?
          checkUserAndLogin(request, JsonObject);
          break;
        case ADDUSER:
          userManagement.addUser(request, requestType, deviceType, device, JsonObject);
          break;
        case SETPROJECT:  // client needs to set the current user's project
          setProjectForUser(request, JsonObject);
          break;
        case ALIGN:
        case DECODE:
        case RECORD:
          try {
            JsonObject = getJsonForAudio(request, realRequest, deviceType, device);
          } catch (IOException e) {
            logger.error("doPost got " + e, e);
            JsonObject.addProperty(ERROR, "got except " + e.getMessage());
          }
          break;
        case STREAM:

        case EVENT:
          gotLogEvent(request, device, JsonObject);
          break;
        case ROUNDTRIP:
          addRT(request, JsonObject);
          break;
        default:
          gotUnknown(requestType, deviceType, device, JsonObject);
          break;
      }
    } else {
      logger.info("doPost request type is null - assume align.");
      try {
        JsonObject = getJsonForAudio(request, PostRequest.ALIGN, deviceType, device);
      } catch (Exception e) {
        logger.error("doPost got " + e, e);
        JsonObject.addProperty(ERROR, "got except " + e.getMessage());
      }
    }

    writeJsonToOutput(response, JsonObject);

    long now = System.currentTimeMillis();
    logger.info("doPost request " + requestType + " took " + (now - then) + " millis");
  }

  @NotNull
  private String getOrUnk(HttpServletRequest request, String deviceType1) {
    String deviceType = request.getHeader(deviceType1);
    if (deviceType == null) deviceType = "unk";
    return deviceType;
  }

  /**
   * @param request
   * @param JsonObject
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void checkUserAndLogin(HttpServletRequest request, JsonObject JsonObject) {
    reportOnHeaders(request);

    userManagement.tryToLogin(
        JsonObject,
        request,
        securityManager,

        getProjID(request),
        getUserID(request),
        getPass(request),
        false);
  }

  private void setProjectForUser(HttpServletRequest request, JsonObject JsonObject) {
    try {
      int userID = checkSession(request);

      if (userID == -1) {  // how can this happen?
        JsonObject.addProperty(MESSAGE, "no user id");
      } else {
        reportOnHeaders(request);

        int projID = getProjID(request);

        if (projID > 0) {
          userManagement.setProjectForUser(JsonObject, userID, projID);
        } else {
          JsonObject.addProperty(MESSAGE, "no project id");
        }
      }
    } catch (DominoSessionException dse) {
      logger.info("got " + dse);
      JsonObject.addProperty(MESSAGE, NO_SESSION);
    }
  }

  /**
   * @param requestType
   * @param deviceType
   * @param device
   * @param JsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void gotUnknown(String requestType, String deviceType, String device, JsonObject JsonObject) {
    JsonObject.addProperty(ERROR, "unknown req " + requestType);
    logger.warn("doPost unknown request " + requestType + " device " + deviceType + "/" + device);
  }

  private void addRT(HttpServletRequest request, JsonObject JsonObject) {
    String resultID = getHeader(request, HeaderValue.RESULT_ID);
    String roundTripMillis = getHeader(request, HeaderValue.ROUND_TRIP1);

    try {
      addRT(Integer.parseInt(resultID), Integer.parseInt(roundTripMillis), JsonObject);
    } catch (NumberFormatException e) {
      JsonObject.addProperty(ERROR, "bad param format " + e.getMessage());
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

  private void gotLogEvent(HttpServletRequest request, String device, JsonObject JsonObject) {
    String user = getUser(request);

    int userid = user == null ? -1 : userManagement.getUserFromParamWarnIfBad(user);
    if (getUser(userid) == null) {
      JsonObject.addProperty(ERROR, "unknown user " + userid);
    } else {
      String context = request.getHeader(CONTEXT);
      String exid = request.getHeader(EXID);
      String widgetid = request.getHeader(WIDGET);
      String widgetType = getHeader(request, HeaderValue.WIDGET_TYPE);

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
   * @param JsonObject
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void addRT(int resultID, int roundTripMillis, JsonObject JsonObject) {
    getDAOContainer().getAnswerDAO().addRoundTrip(resultID, roundTripMillis);
    JsonObject.addProperty("OK", "OK");
  }

  /**
   * @param response
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   * @see #doPost(HttpServletRequest, HttpServletResponse)
   */
  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding(UTF_8);
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
  private JsonObject getJsonNestedChapters(boolean removeExercisesWithMissingAudio, int projectid) {
    JsonExport jsonExport = getJsonExport(removeExercisesWithMissingAudio, projectid);

    long then2 = System.currentTimeMillis();

    JsonObject JsonObject = new JsonObject();
    {
      JsonObject.add(CONTENT, jsonExport.getContentAsJson(removeExercisesWithMissingAudio));
      long now2 = System.currentTimeMillis();
      if (now2 - then2 > 1000) {
        String language = getLanguage(projectid);
        logger.warn("getJsonNestedChapters " + language + " getContentAsJson took " + (now2 - then2) + " millis");
      }
      addVersion(JsonObject, projectid);
    }
    return JsonObject;
  }

  private JsonObject getJsonForListContent(boolean removeExercisesWithMissingAudio, int projectid, int listid) {
    JsonExport jsonExport = getJsonExport(removeExercisesWithMissingAudio, projectid);
    long then = System.currentTimeMillis();

    JsonObject JsonObject = new JsonObject();
    {
      List<CommonExercise> exercisesForList = db.getUserListManager().getCommonExercisesOnList(projectid, listid);
      JsonObject.add(CONTENT, jsonExport.getContentAsJsonFor(removeExercisesWithMissingAudio, exercisesForList));
      long now = System.currentTimeMillis();
      if (now - then > 1000) {
        String language = getLanguage(projectid);
        logger.warn("getJsonNestedChapters " + language + " getContentAsJson took " + (now - then) + " millis");
      }
      addVersion(JsonObject, projectid);
    }
    return JsonObject;
  }

  private JsonExport getJsonExport(boolean removeExercisesWithMissingAudio, int projectid) {
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
    return jsonExport;
  }

  private String getLanguage(int projectid) {
    Project project = getProject(projectid);
    return project == null ? ("" + projectid) : project.getLanguage();
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
   * @see #doPost
   */
  private JsonObject getJsonForAudio(HttpServletRequest request,
                                     PostRequest requestType,
                                     String deviceType,
                                     String device) throws IOException {
    // check session
    long then = System.currentTimeMillis();
    try {
      checkSession(request);
    } catch (DominoSessionException dse) {
      logger.info("getJsonForAudio got " + dse);
      JsonObject JsonObject = new JsonObject();
      JsonObject.addProperty(MESSAGE, NO_SESSION);
      return JsonObject;
    }

    int realExID = getRealExID(request);
    int reqid = getReqID(request);
    int projid = getProjid(request);

    String postedWordOrPhrase;
    // language overrides user id mapping...
    {
      ExAndText exerciseIDFromText = getExerciseIDFromText(request, realExID, projid);
      realExID = exerciseIDFromText.exid;
      postedWordOrPhrase = exerciseIDFromText.text;
    }

    String user = getUser(request);
    int userid = userManagement.getUserFromParam(user);
    boolean fullJSON = isFullJSON(request);

    long now = System.currentTimeMillis();
    logger.info("getJsonForAudio got" +
        "\n\trequest  " + requestType +
        "\n\tfor user " + user +
        "\n\tprojid   " + projid +
        "\n\texid     " + realExID +
        //"\n\texercise text " + realExID +
        "\n\treq      " + reqid +
        "\n\tfull     " + fullJSON +
        "\n\tprep time " + (now - then) +
        "\n\tdevice   " + deviceType + "/" + device);


    then = System.currentTimeMillis();
    File saveFile = new FileSaver().writeAudioFile(
        pathHelper, request.getInputStream(), realExID, userid, getProject(projid).getLanguage(), true);

    now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("getJsonForAudio save file to " + saveFile.getAbsolutePath() + " took " + (now - then) + " millis");
    }
    then = System.currentTimeMillis();

    new AudioConversion(false, db.getServerProps().getMinDynamicRange())
        .getValidityAndDur(saveFile, false, db.getServerProps().isQuietAudioOK(), then);

    now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("getJsonForAudio audio conversion for " + saveFile.getAbsolutePath() + " took " + (now - then) + " millis");
    }

    // TODO : put back trim silence? or is it done somewhere else
//    new AudioConversion(null).trimSilence(saveFile);

    if (requestType == PostRequest.DECODE && CONVERT_DECODE_TO_ALIGN) {
      logger.info("\tfor now we force decode requests into alignment...");
      requestType = PostRequest.ALIGN;
    }

    return jsonScoring.getJsonForAudioForUser(
        reqid,
        projid,
        realExID,

        postedWordOrPhrase,

        userid,
        requestType,
        saveFile.getAbsolutePath(),
        saveFile,
        deviceType,
        device,
        new DecoderOptions()
            .setAllowAlternates(getAllowAlternates(request))
            .setUsePhoneToDisplay(getUsePhoneToDisplay(request)),
        fullJSON);
  }

  private boolean isFullJSON(HttpServletRequest request) {
    return getHeader(request, HeaderValue.FULL) != null;
  }

  /**
   * First see if it's on the request session,
   * then see if it's on the request header,
   * then if there's a language on the request header, use the project for the language
   *
   * the last helps if we're running from a dev laptop to a server...
   *
   * @param request
   * @return
   */
  private int getProjid(HttpServletRequest request) {
    int projid = getProjectID(request);

    logger.debug("getProjid got projid from session " + projid);
    if (projid == -1) {
      projid = getProjID(request);
      logger.debug("getProjid got projid from request " + projid);
    }
    projid = getProjidFromLanguage(request, projid);

    return projid;
  }

  private int getRealExID(HttpServletRequest request) {
    int realExID = 0;
    try {
      realExID = Integer.parseInt(getExerciseHeader(request));
      if (realExID == -1) {
        realExID = getDAOContainer().getUserExerciseDAO().getUnknownExercise().id();
        logger.info("getRealExID : using unknown exercise id " + realExID);
      } else {
        //logger.info("getRealExID got exercise id " + realExID);
      }
    } catch (NumberFormatException e) {
      logger.info("getRealExID couldn't parse exercise request header = '" + getExerciseHeader(request) + "'");
    }
    return realExID;
  }

  private String getRequestType(HttpServletRequest request) {
    return getHeader(request, HeaderValue.REQUEST);
  }

  private int getProjID(HttpServletRequest request) {
    return request.getIntHeader(HeaderValue.PROJID.toString());
  }

  private String getExerciseHeader(HttpServletRequest request) {
    return getHeader(request, HeaderValue.EXERCISE);
  }

  private String getUser(HttpServletRequest request) {
    return getHeader(request, HeaderValue.USER);
  }

  private String getPass(HttpServletRequest request) {
    return getHeader(request, HeaderValue.PASS);
  }

  private String getUserID(HttpServletRequest request) {
    return getHeader(request, HeaderValue.USERID);
  }

  private String getLanguage(HttpServletRequest request) {
    return getHeader(request, HeaderValue.LANGUAGE);
  }

/*  private int getStreamSession(HttpServletRequest request) {
    return request.getIntHeader(HeaderValue.STREAMSESSION.toString());
  }

  private int getStreamPacket(HttpServletRequest request) {
    return request.getIntHeader(HeaderValue.STREAMSPACKET.toString());
  }*/

  private String getHeader(HttpServletRequest request, HeaderValue resultId) {
    return request.getHeader(resultId.toString());
  }

  /**
   * For stream, we need headers: session, state (start, packet, end)
   */
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
  private ExAndText getExerciseIDFromText(HttpServletRequest request, int realExID, int projid) {
    String exerciseText = getHeader(request, HeaderValue.ENGLISH);
    String decoded = "";
    if (exerciseText == null) exerciseText = "";
    if (projid > 0) {
      Project project1 = getProject(projid);
      String flText = getHeader(request, HeaderValue.EXERCISE_TEXT);
      if (flText == null) {
        logger.info("getExerciseIDFromText no optional header " + HeaderValue.EXERCISE_TEXT);
        return new ExAndText(realExID, decoded);
      } else {
        decoded = new String(Base64.getDecoder().decode(flText.getBytes()));

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
    return new ExAndText(realExID, decoded);
  }

  private static class ExAndText {
    final int exid;
    final String text;

    ExAndText(int exid, String text) {
      this.exid = exid;
      this.text = text;
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
        this.userManagement = new RestUserManagement(db, serverProps);
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


  private void addVersion(JsonObject JsonObject, int projid) {
    JsonObject.addProperty(VERSION, VERSION_NOW);
    JsonObject.addProperty(HAS_MODEL, getProject(projid).hasModel());
    JsonObject.addProperty("Date", new Date().toString());
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }

  private static class UserAndSelection {
    private final String[] split1;
    private Map<String, Collection<String>> selection;

    UserAndSelection(String... split1) {
      this.split1 = split1;
    }

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
          if (key.equals(HeaderValue.USER.toString())) {
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