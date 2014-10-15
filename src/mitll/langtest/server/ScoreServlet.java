package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.User;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * All in support of Liz tethered iOS app.
 * <p/>
 * User: GO22670
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);
  public static final String REQUEST = "request";
  public static final String NESTED_CHAPTERS = "nestedChapters";
  public static final String ALIGN = "align";
  public static final String DECODE = "decode";
  private JSONObject chapters, nestedChapters;
  public static final String LOAD_TESTING = "loadTesting";
  private static final String ADD_USER = "addUser";
  private static final String HAS_USER = "hasUser";
  private boolean debug = true;

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
        if (queryString.startsWith(NESTED_CHAPTERS)) {
          if (nestedChapters == null) {
            nestedChapters = getJsonNestedChapters();
          }
          toReturn = nestedChapters;
        } else if (queryString.startsWith(HAS_USER)) {

          String[] split1 = queryString.split("&");
          if (split1.length != 2) {
            toReturn.put("ERROR", "expecting two query parameters");
          } else {
            String first = split1[0];
            String user = first.split("=")[1];

            String second = split1[1];
            String passwordH = second.split("=")[1];

            logger.debug("hasUser " + user + " pass " + passwordH);
            User userFound = db.getUserDAO().getUser(user, passwordH);
            toReturn.put("userid", userFound == null ? -1 : userFound.getId());
            toReturn.put("passwordCorrect",
                userFound == null ? "false" : userFound.getPasswordHash().equalsIgnoreCase(passwordH));
          }
        } else {
          toReturn.put("ERROR", "unknown req " + queryString);

        }
      } else {
        if (chapters == null) {
          chapters = getJsonChapters();
        }
        toReturn = chapters;
        //toReturn.put("ERROR", "null req");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      PrintWriter writer = response.getWriter();
      String x = toReturn.toString();
      if (x.length() > 1000) {
        logger.debug("Reply " + x.substring(0,1000));
      }
      else {
        logger.debug("Reply " +x);
      }
      writer.println(x);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
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
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    getAudioFileHelper();

    configureResponse(response);

    JSONObject jsonObject = new JSONObject();
    String requestType = request.getHeader(REQUEST);

    String deviceType = request.getHeader("deviceType");
    String device = request.getHeader("device");

    if (requestType != null) {
      if (requestType.startsWith(ADD_USER)) {
        String user = request.getHeader("user");
        String passwordH = request.getHeader("passwordH");
        String emailH = request.getHeader("emailH");

        logger.debug("req " + deviceType + " " + device);
        User user1 = db.addUser(user, passwordH, emailH, deviceType, device);

        if (user1 == null) {
          jsonObject.put("ExistingUserName", "");
        } else {
          jsonObject.put("userid", user1.getId());
        }
      } else if (requestType.startsWith(ALIGN) || requestType.startsWith(DECODE)) {
        //jsonObject = getJsonForParts(request, requestType);
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        if (isMultipart) {
          logger.debug("got " + request.getParts().size() + " parts isMultipart " + isMultipart);
          jsonObject = getJsonForParts(request, requestType);
        } else {
          jsonObject = getJsonForAudio(request, requestType, deviceType, device);
        }
      } else if (requestType.startsWith("event")) {
        // log event
        String user = request.getHeader("user");
        String context = request.getHeader("context");
        String exid = request.getHeader("exid");
        String widgetid = request.getHeader("widget");
        String widgetType = request.getHeader("widgetType");
        long userid = 0;
        try {
          userid = Long.parseLong(user);
        } catch (NumberFormatException e) {
          logger.warn("couldn't parse event userid " +user);
          userid = -1;
        }
        if (db.getUserDAO().getUserWhere(userid) == null) {
          jsonObject.put("ERROR","unknown user " + userid);
        }
        else {
          if (widgetid == null) {
            db.logEvent(exid == null ? "N/A" : exid, context, userid);
          } else {
            db.logEvent(widgetid, widgetType, exid == null ? "N/A" : exid, context, userid);
          }
        }
      } else {
        jsonObject.put("ERROR", "unknown req " + requestType);
      }
    } else {
      boolean isMultipart = ServletFileUpload.isMultipartContent(request);

      if (isMultipart) {
        logger.debug("got " + request.getParts().size() + " parts isMultipart " + isMultipart);
        jsonObject = getJsonForParts(request, null);
      } else {
        jsonObject = getJsonForAudio(request, null, deviceType, device);
      }
    }

    PrintWriter writer = response.getWriter();
    writer.println(jsonObject.toString());

    writer.close();
  }

  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
  }

  /**
   * Use apache commons file upload to grab the parts - is this really necessary?
   *
   * @param request
   * @return
   * @see #doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private JSONObject getJsonForParts(HttpServletRequest request, String requestType) {
    long then = System.currentTimeMillis();

    // boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    // Create a factory for disk-based file items
    DiskFileItemFactory factory = new DiskFileItemFactory();

// Configure a repository (to ensure a secure temp location is used)
    ServletContext servletContext = this.getServletConfig().getServletContext();
    File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
    factory.setRepository(repository);

// Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload(factory);

// Parse the request
    try {
      List<FileItem> items = upload.parseRequest(request);
      Iterator<FileItem> iterator = items.iterator();
      FileItem next = iterator.next();

      logger.debug("got " + next.getContentType() + " " + next.getFieldName() + " " + next.getString() + " " + next.isInMemory() + " " + next.getSize());
      InputStream inputStream = next.getInputStream();
      BufferedReader bufferedReader = getBufferedReader(inputStream);
      String word = bufferedReader.readLine();
      bufferedReader.close();
      logger.debug("word is " + word);

      next = iterator.next();
      String name = next.getName();

      logger.debug("Scoring : got " + name + " " + next.getContentType() + " " + next.getFieldName() + " " + next.isInMemory() + " " + next.getSize());

      if (requestType != null) {
        String user = request.getHeader("user");
        String exerciseID = request.getHeader("exercise");
        String deviceType = request.getHeader("deviceType");
        String device = request.getHeader("device");

        logger.debug("got request " + requestType + " for user " + user + " exercise " + exerciseID);
        int i = -1;
        try {
          i = Integer.parseInt(user);
        } catch (NumberFormatException e) {
          logger.error("expecting a number for user id " + user);
        }
        String wavPath = pathHelper.getLocalPathToAnswer("plan", exerciseID, 0, i);
        File saveFile = new File(wavPath);
        writeToFile(next.getInputStream(), saveFile);

        return getJsonForAudioForUser(exerciseID, i, isDecode(requestType), wavPath, saveFile, deviceType, device);
      } else {
        File tempDir = Files.createTempDir();
        File saveFile = new File(tempDir + File.separator + "MyAudioFile.wav");
        // opens input stream of the request for reading data
        writeToFile(next.getInputStream(), saveFile);
        long now = System.currentTimeMillis();
        logger.debug("took " + (now - then) + " millis to parse request and write the file");

        return getJsonForWordAndAudio(word, saveFile);
      }

    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return new JSONObject();
  }

/*  private JSONObject getJsonForAudioParts(HttpServletRequest request) {
    try {
      String word = "";
      File saveFile = null;
      File tempDir = Files.createTempDir();
      for (Part part : request.getParts()) {
        if (part.getName().equalsIgnoreCase("word")) {
          InputStream inputStream = part.getInputStream();
          BufferedReader bufferedReader = getBufferedReader(inputStream);
          word = bufferedReader.readLine();
          bufferedReader.close();
          logger.debug("word is " + word);
        } else if (part.getName().equalsIgnoreCase("audio")) {
          saveFile = new File(tempDir + File.separator+ "MyAudioFile.wav");
          // opens input stream of the request for reading data
          writeToFile(part.getInputStream(), saveFile);
          logger.debug("wrote to file " + saveFile.getAbsolutePath());
        }
      }

      return getJsonForWordAndAudio(word, saveFile);

    } catch (IOException e) {
      logger.error("got " +e,e);
    } catch (ServletException e) {
      logger.error("got " + e, e);
    }
    return new JSONObject();
  }*/

  private static final String ENCODING = "UTF8";

  private BufferedReader getBufferedReader(InputStream resourceAsStream) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(resourceAsStream, ENCODING));
  }

  /**
   * @return
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @deprecated - we do nested chapters now
   */
  private JSONObject getJsonChapters() {
    JSONObject jsonObject = new JSONObject();
    setInstallPath(db);
    db.getExercises();
    List<SectionNode> sectionNodes = db.getSectionHelper().getSectionNodes();
    for (SectionNode node : sectionNodes) {
      Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();
      List<String> value = new ArrayList<String>();
      value.add(node.getName());
      typeToValues.put(node.getType(), value);
      JSONArray exercises = getJsonForSelection(typeToValues);
      jsonObject.put(node.getName(), exercises);
    }
    return jsonObject;
  }

  private JSONObject getJsonNestedChapters() {
    JSONObject jsonObject = new JSONObject();
    setInstallPath(db);
    db.getExercises();
    JSONArray jsonArray = new JSONArray();

    List<SectionNode> sectionNodes = db.getSectionHelper().getSectionNodes();

    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();

   logger.debug("getJsonNestedChapters got " + sectionNodes);
    for (SectionNode node : sectionNodes) {
      typeToValues.put(node.getType(), Collections.singletonList(node.getName()));
      JSONObject jsonForNode = getJsonForNode(node, typeToValues);
      typeToValues.remove(node.getType());

      jsonArray.add(jsonForNode);
    }
    jsonObject.put("content", jsonArray);
    jsonObject.put("version", "1.0");
    jsonObject.put("hasModel", !db.getServerProps().isNoModel());

    return jsonObject;
  }

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

  private JSONArray getJsonForSelection(Map<String, Collection<String>> typeToValues) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToValues);

    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);
    new ExerciseSorter(db.getSectionHelper().getTypeOrder()).sortByTooltip(copy);

    return getJsonArray(copy);
  }

  /**
   * This is the json that describes an individual entry.
   *
   * @param copy
   * @return
   */
  private JSONArray getJsonArray(List<CommonExercise> copy) {
    JSONArray exercises = new JSONArray();

    for (CommonExercise exercise : copy) {
      if (!debug) ensureMP3s(exercise);
      exercises.add(getJsonForExercise(exercise));
    }
    return exercises;
  }

  /**
   * @param request
   * @param deviceType
   *@param device @return
   * @throws IOException
   * @see #doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  private JSONObject getJsonForAudio(HttpServletRequest request, String requestType,
                                     String deviceType, String device) throws IOException {
    // Gets file name for HTTP header

    if (requestType != null) {
      String user = request.getHeader("user");
      String exerciseID = request.getHeader("exercise");

      logger.debug("getJsonForAudio got request " + requestType + " for user " + user + " exercise " + exerciseID);
      int i = -1;
      try {
        i = Integer.parseInt(user);
      } catch (NumberFormatException e) {
        logger.error("expecting a number for user id " + user);
      }
      String wavPath = pathHelper.getLocalPathToAnswer("plan", exerciseID, 0, i);
      File saveFile = new File(wavPath);
      writeToOutputStream(request, saveFile);
      return getJsonForAudioForUser(exerciseID, i, isDecode(requestType), wavPath, saveFile, deviceType, device);
    } else {
      String fileName = request.getHeader("fileName");
      String word = request.getHeader("word");
      boolean isFlashcard = request.getHeader("flashcard") != null;

      File tempDir = Files.createTempDir();
      File saveFile = new File(tempDir + File.separator + fileName);

      // prints out all header values
/*    logger.debug("===== Begin headers =====");
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String headerName = names.nextElement();
      System.out.println(headerName + " = " + request.getHeader(headerName));
    }
    logger.debug("===== End headers =====\n");*/

      // opens input stream of the request for reading data
      writeToOutputStream(request, saveFile);

      if (isFlashcard) {
        return getJsonForWordAndAudioFlashcard(word, saveFile);
      } else {
        return getJsonForWordAndAudio(word, saveFile);
      }
    }
  }

  private boolean isDecode(String requestType) {
    return requestType.equalsIgnoreCase("decode");
  }

  private JSONObject getJsonForAudioForUser(String exerciseID, int user, boolean doFlashcard, String wavPath, File saveFile,
                                            String deviceType, String device) {
    long then = System.currentTimeMillis();
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

    JSONObject jsonForScore = new JSONObject();
    if (exercise1 == null) {
      jsonForScore.put("valid", "bad_exercise_id");

    } else {
      AudioAnswer answer;

      if (!doFlashcard) {
        PretestScore asrScoreForAudio = getASRScoreForAudio(wavPath, exercise1.getRefSentence(), exerciseID);
        answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, asrScoreForAudio.getHydecScore(), deviceType, device);
        answer.setPretestScore(asrScoreForAudio);
      } else {
        answer = getAnswer(exerciseID, user, doFlashcard, wavPath, saveFile, -1, deviceType, device);
      }
      long now = System.currentTimeMillis();
      PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
      float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();
      logger.debug("score flashcard " + doFlashcard +
          " exercise id " + exerciseID + " took " + (now - then) +
          " millis for " + saveFile.getName() + " = " + hydecScore);

      if (answer != null && answer.isValid()) {
        jsonForScore = getJsonForScore(pretestScore);
        if (doFlashcard) {
          jsonForScore.put("isCorrect", answer.isCorrect());
          jsonForScore.put("saidWord", answer.isSaidAnswer());
        }
      }
      jsonForScore.put("valid", answer.getValidity().toString());
    }
    return jsonForScore;
  }

  private JSONObject getJsonForWordAndAudio(String word, File saveFile) {
    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    long then = System.currentTimeMillis();
    PretestScore book = getASRScoreForAudio(audioFileHelper, saveFile.getAbsolutePath(), word);
    long now = System.currentTimeMillis();
    logger.debug("score for '" + word + "' took " + (now - then) +
        " millis for " + saveFile.getName() + " = " + book);

    return getJsonForScore(book);
  }

  /**
   * @param word
   * @param saveFile
   * @return
   * @see #getJsonForAudio
   */
  private JSONObject getJsonForWordAndAudioFlashcard(String word, File saveFile) {
    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    long then = System.currentTimeMillis();
    AudioFileHelper.ScoreAndAnswer scoreAndAnswer = getFlashcardScore(audioFileHelper, saveFile, word);
    long now = System.currentTimeMillis();
    float hydecScore = scoreAndAnswer.score == null ? -1 : scoreAndAnswer.score.getHydecScore();
    logger.debug("score for '" + word + "' took " + (now - then) +
        " millis for " + saveFile.getName() + " = " + hydecScore);

    JSONObject jsonForScore = getJsonForScore(scoreAndAnswer.score);
    jsonForScore.put("isCorrect", scoreAndAnswer.answer.isCorrect());
    jsonForScore.put("saidWord", scoreAndAnswer.answer.isSaidAnswer());

    return jsonForScore;
  }

  /**
   * TODO : add in devicetype and device pass through
   *
   * @param exerciseID
   * @param user
   * @param doFlashcard
   * @param wavPath
   * @param file
   * @param deviceType
   *@param device @return
   */
  private AudioAnswer getAnswer(String exerciseID, int user, boolean doFlashcard, String wavPath, File file, float score,
                                String deviceType, String device) {
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items
    return audioFileHelper.getAnswer(exerciseID, exercise1, user, doFlashcard, wavPath, file, deviceType, device, score);
  }

  private void writeToOutputStream(HttpServletRequest request, File saveFile) throws IOException {
    InputStream inputStream = request.getInputStream();
    writeToFile(inputStream, saveFile);
  }

  private JSONObject getJsonForScore(PretestScore book) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score", book.getHydecScore());

    for (Map.Entry<NetPronImageType, List<TranscriptSegment>> pair : book.getsTypeToEndTimes().entrySet()) {
      List<TranscriptSegment> value = pair.getValue();
      JSONArray value1 = new JSONArray();

      for (TranscriptSegment segment : value) {
        JSONObject object = new JSONObject();
        object.put("event", segment.getEvent());
        object.put("start", segment.getStart());
        object.put("end", segment.getEnd());
        object.put("score", segment.getScore());

        value1.add(object);
      }

      jsonObject.put(pair.getKey().toString(), value1);
    }
    return jsonObject;
  }

 /* private JSONObject getJsonForScoreFlashcard(PretestScore book) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score", book.getHydecScore());

    for (Map.Entry<NetPronImageType, List<TranscriptSegment>> pair : book.getsTypeToEndTimes().entrySet()) {
      List<TranscriptSegment> value = pair.getValue();
      JSONArray value1 = new JSONArray();
      //logger.debug("got " + pair.getKey() + " with " + value.size() + " now " + jsonObject);

      for (TranscriptSegment segment : value) {
        JSONObject object = new JSONObject();
        // logger.debug("\tgot " + pair.getKey() + " with " + value1.size() + " now " + jsonObject);
        object.put("event", segment.getEvent());
        object.put("start", segment.getStart());
        object.put("end", segment.getEnd());
        object.put("score", segment.getScore());

        value1.add(object);
        //logger.debug("\tobject " + object);
      }

      jsonObject.put(pair.getKey().toString(), value1);
    }
    return jsonObject;
  }*/

  private DatabaseImpl db;
  private AudioFileHelper audioFileHelper;

  /**
   * Get a reference to the current database object, made in the main LangTestDatabaseImpl servlet
   *
   * @return
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @see #getJsonForWordAndAudio(String, java.io.File)
   */
  private AudioFileHelper getAudioFileHelper() {
    if (audioFileHelper == null) {
      setPaths();

      db = getDatabase();
      serverProps = db.getServerProps();
      // logger.debug("configDir " + configDir);
      audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, null);

      makeAutoCRT(audioFileHelper);
    }
    return audioFileHelper;
  }

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      logger.debug("found existing database reference " + db + " under " + getServletContext());
    } else {
      logger.error("huh? no existing db reference?");
    }
    return db;
  }

  /**
   * Do alignment of audio file against sentence.
   *
   * @param audioFileHelper
   * @param testAudioFile
   * @param sentence
   * @return
   * @see #getJsonForWordAndAudio(String, java.io.File)
   */
  private PretestScore getASRScoreForAudio(AudioFileHelper audioFileHelper, String testAudioFile, String sentence) {
    // logger.debug("getASRScoreForAudio " +testAudioFile);
    PretestScore asrScoreForAudio = null;
    try {
      asrScoreForAudio = audioFileHelper.getASRScoreForAudio(-1, testAudioFile, sentence, 128, 128, false,
          false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), "");
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return asrScoreForAudio;
  }

  /**
   * TODO : this is wacky -- have to do this for alignment but not for decoding
   *
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @return
   */
  private PretestScore getASRScoreForAudio(String testAudioFile, String sentence,
                                           String exerciseID) {
    return audioFileHelper.getASRScoreForAudio(1, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), exerciseID);
  }

  /**
   * @param audioFileHelper
   * @param testAudioFile
   * @param sentence
   * @return
   * @see #getJsonForWordAndAudioFlashcard(String, java.io.File)
   */
  private AudioFileHelper.ScoreAndAnswer getFlashcardScore(final AudioFileHelper audioFileHelper, File testAudioFile, String sentence) {
    // logger.debug("getASRScoreForAudio " +testAudioFile);

    AudioFileHelper.ScoreAndAnswer asrScoreForAudio = new AudioFileHelper.ScoreAndAnswer(new PretestScore(), new AudioAnswer());
    if (!audioFileHelper.checkLTS(sentence)) {
      logger.error("couldn't decode the word '' since it's not in the dictionary or passes letter-to-sound.  E.g. english word with an arabic model.");
      return asrScoreForAudio;
    }

    try {
      makeAutoCRT(audioFileHelper);
      asrScoreForAudio = audioFileHelper.getFlashcardAnswer(testAudioFile, sentence);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return asrScoreForAudio;
  }

  private void makeAutoCRT(final AudioFileHelper audioFileHelper) {
    AutoCRTScoring crtScoring = new AutoCRTScoring() {
      @Override
      public PretestScore getASRScoreForAudio(File testAudioFile, Collection<String> lmSentences) {
        return audioFileHelper.getASRScoreForAudio(testAudioFile, lmSentences);
      }

      @Override
      public Collection<String> getValidPhrases(Collection<String> phrases) {
        return audioFileHelper.getValidPhrases(phrases);
      }
    };
    audioFileHelper.makeAutoCRT(relativeConfigDir, crtScoring);
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