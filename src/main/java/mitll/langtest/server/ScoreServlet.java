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

package mitll.langtest.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.report.ReportingServices;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.json.ProjectExport;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.scoring.JsonScoring;
import mitll.langtest.server.sorter.SimpleSorter;
import mitll.langtest.shared.analysis.PhoneReportRequest;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.QuizSpec;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ModelType;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static mitll.langtest.server.ScoreServlet.PostRequest.ALIGN;
import static mitll.langtest.shared.project.ProjectStatus.DELETED;

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

  private static final String REGEX = " ";  // no break space!
  private static final String TIC_REGEX = "&#39;";

  private static final String EXID = "exid";
  private static final String REQID = "reqid";
  private static final String VERSION = "version";
  private static final String CONTEXT1 = "context";
  private static final String CONTEXT = CONTEXT1;
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

  private static final String SORT_BY_LATEST_SCORE = "sortByLatestScore";
  private static final String TRUE = "true";
  private static final String FALSE = "false";

  private static final Set<String> notInteresting = new HashSet<>(Arrays.asList(
      "Accept-Encoding",
      "Accept-Language",
      "accept",
      "connection",
      "password",
      "pass"));
  private static final String SENTENCES_PARAM = HeaderValue.SENTENCES.name().toLowerCase();
  private static final String PROJID = "projid";
  private static final String AMPERSAND = "&";
  private static final String DATE = "Date";
  private static final int SAVE_FILE_WARN_THRESHOLD = 20;

  private boolean removeExercisesWithMissingAudioDefault = true;

  private RestUserManagement userManagement;

  public enum GetRequest {
    HASUSER,

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
    LIST,
    SESSION,
    SENTENCES, // only
    EXERCISE_TEXT("exerciseText"),
    ENGLISH,
    KALDI,
    LANGUAGE,
    FULL,
    WIDGET_TYPE("widgetType"),
    RESULT_ID("resultID"),
    /**
     * From EAFEventPoster.postRT -
     */
    ROUND_TRIP1("roundTrip"),
    DIALOGSESSION,
    RECORDINGSESSION,

    AUDIOTYPE,
    ISREFERENCE,
    STREAMSESSION,
    STREAMSTATE,
    STREAMSPACKET,
    STREAMPACKETDUR,
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


  private final Map<Integer, JsonObject> projectToNestedChaptersEverything = new ConcurrentHashMap<>();
  private final Map<Integer, Long> projectToWhenCachedEverything = new ConcurrentHashMap<>();

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

      try {
        if (realRequest == GetRequest.NESTED_CHAPTERS) {
          String[] split1 = queryString.split(AMPERSAND);

          if (hasContextArg(queryString)) {
            toReturn = getJsonNestedChapters(projid, true, true);
          } else {
            if (split1.length == 2) {
              String removeExercisesWithMissingAudio = getRemoveExercisesParam(queryString);
              boolean shouldRemoveExercisesWithNoAudio = removeExercisesWithMissingAudio.equals(TRUE);
              boolean dontRemove = removeExercisesWithMissingAudio.equals(FALSE);
              if (shouldRemoveExercisesWithNoAudio || dontRemove) {
                if (dontRemove) {
                  toReturn = getCachedNestedChapters(projid, shouldRemoveExercisesWithNoAudio);
                } else {
                  toReturn = getJsonNestedChapters(projid, true, false);
                }
              } else {
                toReturn.addProperty(ERROR, "expecting param " + REMOVE_EXERCISES_WITH_MISSING_AUDIO);
              }
            } else {
              toReturn = getCachedNested(projid);
            }
          }
        } else if (realRequest == GetRequest.CHAPTER_HISTORY) {
          queryString = removePrefix(queryString, CHAPTER_HISTORY);
          toReturn = getChapterHistory(queryString, toReturn, projid, userID);
        } else if (realRequest == GetRequest.PHONE_REPORT) {
          queryString = removePrefix(queryString, PHONE_REPORT);
          String[] split1 = queryString.split(AMPERSAND);
          if (split1.length < 2) {
            toReturn.addProperty(ERROR, "expecting at least two query parameters");
          } else {
            long sessionID = getLong(queryString, HeaderValue.SESSION.name().toLowerCase());
            toReturn = getPhoneReport(toReturn, split1, projid, userID, sessionID);
          }
        } else if (realRequest == GetRequest.LIST) {
          toReturn = db.getUserListManager().getListsJson(userID, projid, false);
        } else if (realRequest == GetRequest.QUIZ) {
          toReturn = db.getUserListManager().getListsJson(userID, projid, true);
        } else if (realRequest == GetRequest.CONTENT) {
          long listid = getListParam(queryString);
          //logger.info("list id " + listid);
      /*    try {
            new Thread().sleep(10000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }*/
          toReturn = getJsonForListContent(projid, Long.valueOf(listid).intValue());
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

  private boolean hasContextArg(String queryString) {
    return hasArg(queryString, CONTEXT1);
  }

  private boolean hasSortByLatestScore(String queryString) {
    return hasArg(queryString, SORT_BY_LATEST_SCORE);
  }

  private boolean hasArg(String queryString, String context) {
    return queryString.toLowerCase().contains(AMPERSAND + context + "=true") || queryString.toLowerCase().contains(AMPERSAND + context);
  }

  private JsonObject getCachedNested(int projid) {
    logger.info("getCachedNested returning cached chapters for " + projid);
    return getNested(projid, System.currentTimeMillis(), REFRESH_CONTENT_INTERVAL, projectToNestedChapters, projectToWhenCached, true);
  }

  private JsonObject getCachedNestedChapters(int projid, boolean shouldRemoveExercisesWithNoAudio) {
    logger.info("getCachedNestedChapters returning cached chapters for " + projid + " remove audio " + shouldRemoveExercisesWithNoAudio);

    return getNested(projid, System.currentTimeMillis(), REFRESH_CONTENT_INTERVAL_THREE, projectToNestedChaptersEverything, projectToWhenCachedEverything, shouldRemoveExercisesWithNoAudio);
  }

  private JsonObject getNested(int projid,
                               long now, long refreshContentInterval,
                               Map<Integer, JsonObject> projectToNestedChapters,
                               Map<Integer, Long> projectToWhenCached,
                               boolean shouldRemoveExercisesWithNoAudio) {
    JsonObject nestedChapters = projectToNestedChapters.get(projid);
    Long whenCached = projectToWhenCached.get(projid);

    if (nestedChapters == null || (now - whenCached > refreshContentInterval)) {
      nestedChapters = getJsonNestedChapters(projid, shouldRemoveExercisesWithNoAudio, false);
      JsonArray asJsonArray = nestedChapters.getAsJsonArray(CONTENT);
      if (asJsonArray == null || asJsonArray.size() == 0) {
        logger.error("getNested no content for " + projid + " : " + asJsonArray);
      } else {
        projectToNestedChapters.put(projid, nestedChapters);
        projectToWhenCached.put(projid, now);
      }
    }
    return nestedChapters;
  }

  private long getListParam(String queryString) {
    return getLong(queryString, HeaderValue.LIST.name().toLowerCase());
  }

  private long getLong(String queryString, String paramToLookFor) {
    String[] split1 = queryString.split(AMPERSAND);
    long listid = -1;
    for (String arg : split1) {
      String[] split = arg.split("=");
      if (split.length == 2) {
        String key = split[0];
        if (key.equals(paramToLookFor)) {
          String value = split[1];
          try {
            listid = Long.parseLong(value);
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
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private int getTripleProjID(HttpServletRequest request) {
    int projid = getProjID(request);

    String queryString = request.getQueryString();

    if (projid == -1 && queryString != null) {
      String[] split1 = queryString.split(AMPERSAND);
      Map<String, Collection<String>> selection = new ParamParser(split1).invoke(false).getSelection();
      if (selection.get(PROJID) != null) {
        String projid1 = selection.get(PROJID).iterator().next();
        try {
          projid = Integer.parseInt(projid1);
        } catch (NumberFormatException e) {
          logger.warn("getTripleProjID : couldn't parse '" + projid1 + "'");
        }
      } else {
        logger.info("getTripleProjID no " + PROJID + " in " + selection + " from " + queryString);
      }
    }

    // language overrides user id mapping...
    if (projid == -1) {
      String language = getLanguage(request);
      if (language != null) {
        projid = getProjectID(language, getIsKaldi(request));
      }
    }

    if (projid == -1) {
      // not using header
      projid = getProjectIDFromSession(request);
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
      logger.info("reply : (" + language + ") took " + l + " millis");// to do " + request.getQueryString());
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


  private void reportOnHeaders(HttpServletRequest request) {
    toSet(request)
        .stream()
        .filter(name -> !notInteresting.contains(name))
        .collect(Collectors.toList())
        .forEach(header -> logger.info("\trequest header " + header + " = " + request.getHeader(header)));
  }

  @NotNull
  private Set<String> toSet(HttpServletRequest request) {
    Enumeration<String> headerNames = request.getHeaderNames();
    Set<String> headers = new TreeSet<>();
    while (headerNames.hasMoreElements()) headers.add(headerNames.nextElement());
    return headers;
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
   * @param languageName
   * @param isKaldi
   * @return
   */
  private int getProjectID(String languageName, boolean isKaldi) {
    Language language;

    // fix for john steinberg runs
    if (languageName.equalsIgnoreCase("mandarin_simplified")) {
      logger.info("getProjectID clamp " + languageName + " to " + Language.MANDARIN.toString());
      languageName = Language.MANDARIN.toString();
    }

    try {
      language = Language.valueOf(languageName.toUpperCase());
      IProjectManagement projectManagement = db.getProjectManagement();
      List<Project> projectByLanguage = projectManagement.getProjectByLanguage(language);

      if (isKaldi) {
        projectByLanguage = findKaldiProjects(projectByLanguage);
      } else {
        projectByLanguage = findHydraProjects(projectByLanguage);

        List<Project> candidates = projectByLanguage;
        projectByLanguage = candidates.stream().filter(project -> project.getStatus() == ProjectStatus.PRODUCTION).collect(Collectors.toList());
        if (projectByLanguage.isEmpty()) {
          logger.warn("getProjectID : can't find any production projects for " + language);
          projectByLanguage = candidates;
        }
        if (projectByLanguage.isEmpty()) {
          projectByLanguage = findHydraProjects(projectManagement.getProjectByLanguage(language));
          if (projectByLanguage.isEmpty()) {
            logger.warn("getProjectID : can't find any projects for " + language);
          }
        }
      }

      if (projectByLanguage.size() > 1) {
        logger.warn("getProjectIDFromSession more than one production languageName for " + language + " : " + projectByLanguage.size());
        Project project = projectByLanguage.get(0);
        logger.warn("getProjectIDFromSession will choose " + project + " : model = " + project.getModelType());
      }

      return projectByLanguage.isEmpty() ? -1 : projectByLanguage.get(0).getID();
    } catch (IllegalArgumentException e) {
      logger.error("can't parse languageName " + languageName);
      return -1;
    }
  }

  @NotNull
  private List<Project> findHydraProjects(List<Project> productionByLanguage) {
    Set<ProjectStatus> notThese = new HashSet<>(Arrays.asList(ProjectStatus.RETIRED, DELETED));
    return productionByLanguage
        .stream()
        .filter(project -> project.getModelType() != ModelType.KALDI && !notThese.contains(project.getStatus()))
        .collect(Collectors.toList());
  }

  @NotNull
  private List<Project> findKaldiProjects(List<Project> productionByLanguage) {
    Set<ProjectStatus> notThese = new HashSet<>(Arrays.asList(ProjectStatus.RETIRED, DELETED));
    return productionByLanguage
        .stream()
        .filter(project -> project.getModelType() == ModelType.KALDI && !notThese.contains(project.getStatus()))
        .collect(Collectors.toList());
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
    String[] split1 = queryString.split(AMPERSAND);
    if (split1.length == 2) {
      return getParamValue(split1[1], REMOVE_EXERCISES_WITH_MISSING_AUDIO);
    } else {
      return removeExercisesWithMissingAudioDefault ? TRUE : FALSE;
    }
  }

  private String getParamValue(String s, String param) {
    boolean hasParam = s.startsWith(param);
    if (!hasParam) {
      return "expecting param " + param;
    }
    return s.equals(param + "=true") ? TRUE : FALSE;
  }

  /**
   * @param toReturn
   * @param split1
   * @param projid
   * @param sessionID
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */
  private JsonObject getPhoneReport(JsonObject toReturn, String[] split1, int projid, int userid, long sessionID) {
    Map<String, Collection<String>> selection = new ParamParser(split1).invoke(true).getSelection();

    try {
      long then = System.currentTimeMillis();

      int projectID = getProjectID(projid, userid);
      Collection<String> strings = selection.get(SENTENCES_PARAM);
      boolean sentencesOnly = (strings != null && strings.iterator().next().equalsIgnoreCase("TRUE"));
      logger.info("getPhoneSummary : user " + userid + " selection " + selection + " proj " + projid + " sentencesOnly " + sentencesOnly);
      PhoneReportRequest phoneReportRequest = new PhoneReportRequest(userid, projid, selection, sessionID, sentencesOnly);
      toReturn = db.getJsonPhoneReport(phoneReportRequest);
      long now = System.currentTimeMillis();
      if (now - then > 5) {
        logger.info("getPhoneSummary :" +
            "\n\tuser      " + userid +
            "\n\tselection " + selection +
            "\n\tsentences " + sentencesOnly +
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
   * @see #doGet
   */
  private JsonObject getChapterHistory(String queryString, JsonObject toReturn, int projectid, int userID) {
    Project project = getProject(projectid);

    logger.info("getChapterHistory for project " + projectid + " : " + (project == null ? "" : project.getName()));

    if (projectid == -1) {
      toReturn.addProperty(ERROR, "no project specified for user " + userID);
    } else {
      String[] split1 = queryString.split(AMPERSAND);
      if (split1.length < 2) {
        toReturn.addProperty(ERROR, "expecting at least two query parameters");
      } else {
        Map<String, Collection<String>> selection = new ParamParser(split1).invoke(true).getSelection();

        //logger.debug("chapterHistory " + user + " selection " + selection);
        try {
          boolean sortByLatestScore = hasSortByLatestScore(queryString);
          logger.info("getChapterHistory Sort by latest " + sortByLatestScore);
          toReturn = db.getJsonScoreHistory(projectid, userID, selection, hasContextArg(queryString), sortByLatestScore, new SimpleSorter());
        } catch (NumberFormatException e) {
          toReturn.addProperty(ERROR, "User id should be a number");
        }
      }
    }
    return toReturn;
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
        JsonObject = getJsonForAudio(request, ALIGN, deviceType, device);
      } catch (Exception e) {
        logger.error("doPost got " + e, e);
        JsonObject.addProperty(ERROR, "got except " + e.getMessage());
      }
    }

    writeJsonToOutput(response, JsonObject);

    long now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("doPost request " + requestType + " took " + (now - then) + " millis");
    }
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

    if (resultID == null) {
      String message = "addRT missing header " + HeaderValue.RESULT_ID;
      //   logger.error(message);
      JsonObject.addProperty(ERROR, "addRT " + message);
    } else if (roundTripMillis == null) {
      String message = "addRT missing header " + HeaderValue.ROUND_TRIP1;
      logger.error(message);
      JsonObject.addProperty(ERROR, "addRT " + message);
    } else {
      try {
        addRT(JsonObject, resultID, roundTripMillis);
      } catch (NumberFormatException e) {
        logger.warn("addRT: Got bad format " + e, e);
        addRTError(JsonObject, e);
      }
    }
  }

  private void addRT(JsonObject JsonObject, String resultID, String roundTripMillis) {
    int resultID1 = Integer.parseInt(resultID);
    long roundTripMillis1 = Long.parseLong(roundTripMillis);
    //logger.info("addRT : " + resultID1 + " = " + roundTripMillis1);

    if (roundTripMillis1 == 0) {
      logger.warn("addRT : huh? got 0 for " + roundTripMillis);
    }

    addRT(resultID1, roundTripMillis1, JsonObject);
  }

  private void addRTError(JsonObject JsonObject, NumberFormatException e) {
    String message = e.getMessage();
    JsonObject.addProperty(ERROR, "addRT bad param format " + message);
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

      if (context == null) {
        context = getContentFromPost(request, context);
      } else {
//        logger.info("gotLogEvent not reading from stream since got " + context);
      }

      int projid = getProjid(request);

/*      logger.info("gotLogEvent : " +
          "\n\tprojid " + projid +
          "\n\tcontext " + context +
          "\n\texid " + exid +
          "\n\twidgetid " + widgetid +
          "\n\twidgetType " + widgetType
      );*/

      if (widgetid == null) {
        db.logEvent(exid == null ? "N/A" : exid, context, userid, device, projid);
      } else {
        db.logEvent(widgetid, widgetType, exid == null ? "N/A" : exid, context, userid, device, projid);
      }
    }
  }

  private String getContentFromPost(HttpServletRequest request, String context) {
    StringWriter stringWriter = new StringWriter();
    try {
      org.apache.commons.io.IOUtils.copy(request.getInputStream(), stringWriter, Charset.defaultCharset());
      context = stringWriter.toString();
      //  logger.info("gotLogEvent context now " + context);
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return context;
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
  private void addRT(int resultID, long roundTripMillis, JsonObject JsonObject) {
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
   * join against audio dao ex->audio map again to get user exercise audio! {@link JsonExport#getJsonArray}
   *
   * @param projectid
   * @param removeExercisesWithMissingAudio
   * @param justContext
   * @return json for content
   * @see #doGet
   */
  private JsonObject getJsonNestedChapters(int projectid, boolean removeExercisesWithMissingAudio, boolean justContext) {
    return getJsonForExport(projectid, justContext, removeExercisesWithMissingAudio, getJsonExport(projectid));
  }

  @NotNull
  private JsonObject getJsonForExport(int projectid, boolean justContext, boolean removeExercisesWithMissingAudio,
                                      JsonExport jsonExport) {
    long then2 = System.currentTimeMillis();

    JsonObject JsonObject = new JsonObject();
    {
      JsonArray contentAsJson = jsonExport.getContentAsJson(removeExercisesWithMissingAudio, justContext);
      JsonObject.add(CONTENT, contentAsJson);
      addVersion(projectid, then2, JsonObject);
    }
    return JsonObject;
  }

  /**
   * @param projectid
   * @param listid
   * @return
   * @see #doGet(HttpServletRequest, HttpServletResponse)
   */

  private JsonObject getJsonForListContent(int projectid, int listid) {
    JsonExport jsonExport = getJsonExport(projectid);
    long then = System.currentTimeMillis();

    JsonObject JsonObject = new JsonObject();
    {
      List<CommonExercise> exercisesForList = db.getUserListManager().getCommonExercisesOnList(projectid, listid);
      QuizSpec quizInfo = db.getUserListManager().getQuizInfo(listid);
      JsonArray contentAsJsonFor = jsonExport.getContentAsJsonFor(quizInfo.isDefault(), exercisesForList);
      JsonObject.add(CONTENT, contentAsJsonFor);
      addVersion(projectid, then, JsonObject);
    }
    return JsonObject;
  }

  private void addVersion(int projectid, long then, JsonObject jsonObject) {
    long now = System.currentTimeMillis();
    if (now - then > 1000) {
      logger.warn("addVersion " + getLanguage(projectid) + " addVersion took " + (now - then) + " millis");
    }
    addVersion(jsonObject, projectid);
  }

  private JsonExport getJsonExport(int projectid) {
    if (projectid == -1) {
      logger.error("getJsonNestedChapters project id is not defined : " + projectid);
    } else {
      logger.debug("getJsonNestedChapters get content for project id " + projectid);
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
    int sessionUser;

    try {
      sessionUser = checkSession(request);
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
    // language overrides user id mapping...???
    {
      ExAndText exerciseIDFromText = getExerciseIDFromText(request, realExID, projid);
      realExID = exerciseIDFromText.exid;
      postedWordOrPhrase = exerciseIDFromText.text;
    }

    String user = getUser(request);
    int userid = getUserid(sessionUser, user);

    boolean fullJSON = isFullJSON(request);

    long now = System.currentTimeMillis();

    {
      String dev = !deviceType.equals("unk") && !device.equals("unk") ? "\n\tdevice   " + deviceType + "/" + device : "";
      String req = requestType == ALIGN ? "" : "\n\trequest  " + requestType;
      String prepInfo = (now - then) > 10 ? "\n\tprep time " + (now - then) : "";
      logger.info("getJsonForAudio got" +
          req +
          "\n\tfor user " + user + "/" + userid +
          "\n\tprojid   " + projid +
          "\n\texid     " + realExID +
          "\n\treq      " + reqid +
          "\n\tfull     " + fullJSON +
          prepInfo +
          dev);
    }

    then = System.currentTimeMillis();
    FileSaver.FileSaveResponse response = new FileSaver().writeAudioFile(
        pathHelper, request.getInputStream(), realExID, userid, getProject(projid).getLanguageEnum(), true);

    File fileToScore = response.getFile();
    now = System.currentTimeMillis();
    if (now - then > SAVE_FILE_WARN_THRESHOLD) {
      logger.info("getJsonForAudio save file to " + fileToScore.getAbsolutePath() + " took " + (now - then) + " millis");
    }
    //  then = System.currentTimeMillis();

    if (response.getStatus() == FileSaver.FileSaveResponse.STATUS.OK) {
//      AudioCheck.ValidityAndDur validityAndDur = new AudioConversion(false, db.getServerProps().getMinDynamicRange())
//          .getValidityAndDur(fileToScore, false, db.getServerProps().isQuietAudioOK(), then);

//      now = System.currentTimeMillis();
//      if (now - then > 10) {
//        logger.info("getJsonForAudio audio conversion for " + fileToScore.getAbsolutePath() + " took " + (now - then) + " millis");
//      }

      // TODO : put back trim silence? or is it done somewhere else
//    new AudioConversion(null).trimSilence(fileToScore);

      if (requestType == PostRequest.DECODE && CONVERT_DECODE_TO_ALIGN) {
        // logger.info("\tfor now we force decode requests into alignment...");
        requestType = ALIGN;
      }

      if (fileToScore.length() < AudioCheck.WAV_HEADER_LENGTH) {
        JsonObject jsonObject = new JsonObject();
        new JsonScoring(getDatabase()).addValidity(realExID, jsonObject, Validity.TOO_SHORT, "" + reqid);
        return jsonObject;
      } else {
        JsonObject jsonForAudioForUser = jsonScoring.getJsonForAudioForUser(
            reqid,
            projid,
            realExID,

            postedWordOrPhrase,

            userid,
            requestType,
            fileToScore,
            deviceType,
            device,
            new DecoderOptions()
                .setAllowAlternates(getAllowAlternates(request))
                .setUsePhoneToDisplay(getUsePhoneToDisplay(request)),
            fullJSON);
        jsonForAudioForUser.addProperty("scoring", getIsKaldi(request) ? "Kaldi" : "Hydra");
        return jsonForAudioForUser;
      }
    } else {
      JsonObject jsonObject = new JsonObject();
      new JsonScoring(getDatabase()).addValidity(realExID, jsonObject, Validity.TOO_LONG, "" + reqid);
      return jsonObject;
    }
  }

  private int getUserid(int sessionUser, String user) {
    int userid = userManagement.getUserFromParam(user);
    if (userid != sessionUser && userid != -1) {
      logger.info("getJsonForAudio posted user was " + userid + " but session user was " + sessionUser);
    }
    userid = sessionUser;
    return userid;
  }

  private boolean isFullJSON(HttpServletRequest request) {
    return getHeader(request, HeaderValue.FULL) != null;
  }

  /**
   * First see if it's on the request session,
   * then see if it's on the request header,
   * then if there's a language on the request header, use the project for the language
   * <p>
   * the last helps if we're running from a dev laptop to a server...
   *
   * @param request
   * @return
   * @see #getJsonForAudio(HttpServletRequest, PostRequest, String, String)
   */
  private int getProjid(HttpServletRequest request) {
    int projid = getProjID(request);

    if (projid == -1) {
      projid = getProjidFromLanguage(request, projid);
      if (projid != -1) {
        logger.info("getProjid got projid from language " + projid);
      }
    }

    if (projid == -1) {
      projid = getProjectIDFromSession(request);
      logger.info("getProjid got projid from session " + projid);
    }

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
    if (request.getHeader(HeaderValue.PROJID.toString()) == null) {
      logger.warn("missing expected project id header");
      return -1;
    } else {
      return request.getIntHeader(HeaderValue.PROJID.toString());
    }
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

  /**
   * @param request
   * @return
   * @see #getTripleProjID(HttpServletRequest)
   * @see #getProjidFromLanguage(HttpServletRequest, int)
   */
  private String getLanguage(HttpServletRequest request) {
    return getHeader(request, HeaderValue.LANGUAGE);
  }

  private boolean getIsKaldi(HttpServletRequest request) {
    String header = getHeader(request, HeaderValue.KALDI);
    return header != null && header.equalsIgnoreCase("TRUE");
  }

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
   * @see #getProjid(HttpServletRequest)
   * @see #getJsonForAudio
   **/
  private int getProjidFromLanguage(HttpServletRequest request, int projid) {
    String language = getLanguage(request);

    //logger.debug("getJsonForAudio got langauge from request " + language);

    if (language != null) {
      projid = getProjectID(language, getIsKaldi(request));
      logger.info("getJsonForAudio got projid from language " + projid);

      if (projid == -1) {
        projid = getProjectIDFromSession(request);
        logger.info("getJsonForAudio got projid from request again " + projid);
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
      if (flText == null || flText.isEmpty()) {
        logger.info("getExerciseIDFromText no optional header " + HeaderValue.EXERCISE_TEXT);
        return new ExAndText(realExID, decoded);
      } else {
        decoded = new String(Base64.getDecoder().decode(flText.getBytes()));

        String removeTics = getTrim(decoded);
        logger.info("getExerciseIDFromText request to decode '" + exerciseText + "' to " +
            "\n\tdecode  '" + decoded + "'" +
            "\n\tcleaned '" + removeTics + "'");
        decoded = removeTics;

        CommonExercise exercise = project1.getExerciseBySearchBoth(exerciseText.trim(), decoded.trim());

        if (exercise != null) {
          logger.info("getExerciseIDFromText for '" + exerciseText + "' '" + decoded + "' found exercise id " + exercise.getID());
          realExID = exercise.getID();
        } else {
          logger.warn("getExerciseIDFromText can't find exercise for '" + exerciseText + "'='" + decoded + "' - using unknown exercise");
        }
      }
    }
    return new ExAndText(realExID, decoded);
  }

  private String getTrim(String part) {
    return part.replaceAll(REGEX, " ").replaceAll(TIC_REGEX, "'").trim();
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
    return getBooleanParam(request, USE_PHONE_TO_DISPLAY);
  }

  private boolean getAllowAlternates(HttpServletRequest request) {
    return getBooleanParam(request, ALLOW_ALTERNATES);
  }

  private boolean getBooleanParam(HttpServletRequest request, String param) {
    String use_phone_to_display = request.getHeader(param);
    return use_phone_to_display != null && !use_phone_to_display.equals(FALSE);
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
        setPaths(getServletContext());
        ServerProperties serverProps = db.getServerProps();
        this.userManagement = new RestUserManagement(db, serverProps);
        removeExercisesWithMissingAudioDefault = serverProps.removeExercisesWithMissingAudio();
        jsonScoring = new JsonScoring(db);
      }
    }
  }

  private void addVersion(JsonObject JsonObject, int projid) {
    JsonObject.addProperty(VERSION, VERSION_NOW);
    JsonObject.addProperty(HAS_MODEL, getProject(projid).hasModel());
    JsonObject.addProperty(DATE, new Date().toString());
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }

}