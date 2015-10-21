package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.SectionNode;
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
 * <p>
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
  private static final String PHONE_REPORT = "phoneReport";

  private static final String ERROR = "ERROR";
  private static final String USER = "user";

  private static final String DEVICE_TYPE = "deviceType";
  private static final String DEVICE = "device";
  private static final String EVENT = "event";
  private static final String CONTENT = "content";
  private static final String HAS_MODEL = "hasModel";
  private static final long REFRESH_CONTENT_INTERVAL = 12 * 60 * 60 * 1000l;

  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";

  private static final String EXID = "exid";
  private static final String VALID = "valid";
  private static final String REQID = "reqid";
  private static final String INVALID = "invalid";
  private static final String TYPE = "type";
  private static final String NAME = "name";
  private static final String ITEMS = "items";
  private static final String VERSION = "version";
  private static final String CONTEXT = "context";
  private static final String WIDGET = "widget";
  private static final String CHILDREN = "children";

  //  private LoadTesting loadTesting;
  private RestUserManagement userManagement;

  private enum Request {DECODE, ALIGN, RECORD}

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
        } else if (
            userManagement.doGet(request, response, queryString, toReturn)
            ) {
          logger.info("handled user command for " + queryString);
        } else if (queryString.startsWith(CHAPTER_HISTORY) || queryString.startsWith("request=" + CHAPTER_HISTORY)) {
          queryString = queryString.substring(queryString.indexOf(CHAPTER_HISTORY) + CHAPTER_HISTORY.length());
          toReturn = getChapterHistory(queryString, toReturn);
        } else if (queryString.startsWith(REF_INFO) || queryString.startsWith("request=" + REF_INFO)) {
          queryString = queryString.substring(queryString.indexOf(REF_INFO) + REF_INFO.length());
          toReturn = getRefInfo(queryString, toReturn);
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

    logger.debug("getPhoneReport (" + serverProps.getLanguage() + ") : user " + user + " selection " + selection);
    try {
      long l = Long.parseLong(user);
      long then = System.currentTimeMillis();
      toReturn = db.getJsonPhoneReport(l, selection);
      long now = System.currentTimeMillis();
      if (now - then > 250) {
        logger.debug("getPhoneReport (" + serverProps.getLanguage() + ") : user " + user + " selection " + selection + " took " + (now - then) + " millis");
      }
    } catch (NumberFormatException e) {
      toReturn.put(ERROR, "User id should be a number");
    }
    return toReturn;
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
      }
    } else {
      jsonObject = getJsonForAudio(request, null, deviceType, device);
    }

    writeJsonToOutput(response, jsonObject);
  }

  private void gotLogEvent(HttpServletRequest request, String device, JSONObject jsonObject) {
    String user = request.getHeader(USER);

    long userid = userManagement.getUserFromParam2(user);
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
    addVersion(jsonObject);

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
    } else {
      logger.warn("audioFileHelper not set yet!");
    }
    return getJsonArray(copy);
  }

  /**
   * This is the json that describes an individual entry.
   * <p>
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
   * <p>
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
    int i = userManagement.getUserFromParam(user);
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
   * @param request
   * @return
   * @see #getJsonForAudio(HttpServletRequest, String, String, String)
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
   * @param reqid             label response with req id so the client can tell if it got a stale response
   * @param exerciseID        for this exercise
   * @param user              by this user
   * @param request
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
    PretestScore asrScoreForAudio = getASRScoreForAudioNoCache(reqid, saveFile.getAbsolutePath(), exercise1.getRefSentence(), exerciseID, usePhoneToDisplay);
    AudioAnswer answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device, reqid, false);
    answer.setPretestScore(asrScoreForAudio);
    return answer;
  }

  /**
   * Don't wait for mp3 to write to return - can take 70 millis for a short file.
   *
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
      serverProps = db.getServerProps();
      audioFileHelper = getAudioFileHelperRef();
      this.userManagement = new RestUserManagement(db, serverProps, pathHelper);
      //loadTesting = getLoadTesting();
      REMOVE_EXERCISES_WITH_MISSING_AUDIO = serverProps.removeExercisesWithMissingAudio();
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
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), exerciseID, null, usePhoneToDisplay, false);
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
        false, Files.createTempDir().getAbsolutePath(), false, exerciseID, null, usePhoneToDisplay, false);
  }

  /**
   * Just for appen -
   *
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
      recurse(node, typeToValues, exToCount);
      typeToValues.remove(type);
    }

    Collections.sort(sectionNodes);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(CONTENT, getContentAsJson2(sectionNodes));
    addVersion(jsonObject);

    return jsonObject;
  }

  private void addVersion(JSONObject jsonObject) {
    jsonObject.put(VERSION, "1.0");
    jsonObject.put(HAS_MODEL, !db.getServerProps().isNoModel());
  }

  /**
   * @param sectionNodes
   * @return
   * @see #getJsonLeastRecordedChapters()
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
   * @param node
   * @param typeToValues
   * @param exToCount
   * @see #getJsonLeastRecordedChapters()
   */
  private void recurse(SectionNode node, Map<String, Collection<String>> typeToValues, Map<String, Integer> exToCount) {
    if (node.isLeaf()) {
      Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToValues);
      float total = 0f;
      for (CommonExercise ex : exercisesForState) {
        Integer integer = exToCount.get(ex.getID());
        if (integer == null) {
          //        logger.error("huh? unknown ex id " +ex.getID());
        } else {
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