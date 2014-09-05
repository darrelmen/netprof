package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.SectionNode;
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
public class LoadTestServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(LoadTestServlet.class);
  public static final String DATABASE_REFERENCE = "databaseReference";
  public static final String LOAD_TESTING = "loadTesting";
  public static final String ADD_ANON_USER = "addAnonUser";
  public static final String GET_EXERCISE_I_DS_FOR = "getExerciseIDsFor";
  public static final String GET_EXERCISE = "getExercise";
  public static final String GET_FIRST_EXERCISE = "getFirstExercise";
  public static final String GET_RANDOM_EXERCISE = "getRandomExercise";
  public static final String LOG_EVENT = "logEvent";
  public static final String LISTS_FOR_USER = "listsForUser";
  public static final String IMAGE_FOR_AUDIO = "imageForAudio";
  //private JSONObject chapters, nestedChapters;

  /**
   * Remembers chapters from previous requests...
   *
   * @param request
   * @param response
   * @throws javax.servlet.ServletException
   * @throws java.io.IOException
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
/*    logger.debug("ScoreServlet.doGet : Request " + request.getQueryString() + " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());
        */

    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    //getAudioFileHelper();
    String queryString = request.getQueryString();

    LoadTesting loadTesting = getLoadTesting();

    JSONObject toReturn = new JSONObject();
    if (queryString != null) {
     /*if (queryString.startsWith(ADD_ANON_USER)) {
        long l = loadTesting.addAnonUser();
        toReturn.put("userid", l);
      } else if (queryString.startsWith(GET_EXERCISE_I_DS_FOR)) {
        String s = queryString.split("=")[1];
        ExerciseListWrapper exerciseIDs = loadTesting.getExerciseIDs(Integer.parseInt(s));
        JSONArray jsonArray = new JSONArray();
        for (CommonShell id : exerciseIDs.getExercises()) {
          jsonArray.add(id.getID());
        }
        toReturn.put("ids", jsonArray);
       */
      if (queryString.startsWith(GET_RANDOM_EXERCISE)) {
        CommonExercise exercise = loadTesting.getRandomExercise();
        toReturn = getJsonForExercise(exercise);
      } else if (queryString.startsWith(GET_FIRST_EXERCISE)) {
        CommonExercise exercise = loadTesting.getFirstExercise();
        toReturn = getJsonForExercise(exercise);
      } else if (queryString.startsWith(GET_EXERCISE)) {
        String[] split1 = queryString.split("&");
        String first = split1[0];
        //  logger.debug("first " + first);
        String exid = first.split("=")[1];

        String second = split1[1];
        //  logger.debug("second " + second);
        String userid = second.split("=")[1];

        //logger.debug("user " + userid + " ex " + exid);

        CommonExercise exercise = loadTesting.getExercise(exid, Integer.parseInt(userid));
        toReturn = getJsonForExercise(exercise);
      } else if (queryString.startsWith(LOG_EVENT)) {
        // logEvent=exid&userid=12
        String[] split1 = queryString.split("&");
        String first = split1[0];
        //  logger.debug("first " + first);
        String exid = first.split("=")[1];

        String second = split1[1];
        //  logger.debug("second " + second);
        String userid = second.split("=")[1];

        //logger.debug("user " + userid + " ex " + exid);

        try {
          //loadTesting.logEvent("next","button",exid,"next",Long.parseLong(userid),"");
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      } else if (queryString.startsWith(LISTS_FOR_USER)) {
        String s = queryString.split("=")[1];
        try {
          //Collection<UserList> listsForUser = loadTesting.getListsForUser(Long.parseLong(s), true, false);
          //if (listsForUser.isEmpty()) logger.info("huh? no lists for " + s);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      } else if (queryString.startsWith(IMAGE_FOR_AUDIO)) {
        //  ?imageForAudio=1&audioFile=....


        String[] split1 = queryString.split("&");
        String first = split1[0];
        //  logger.debug("first " + first);
        String exid = first.split("=")[1];

        String second = split1[1];
        //  logger.debug("second " + second);
        String audioFile = second.split("=")[1];
        try {
          //  logger.debug("audio file " + audioFile);
          //  ImageResponse response1 = loadTesting.getImageForAudioFile(audioFile, exid);
          //  if (!response1.successful) logger.info("huh? no image for " + exid + " and " +audioFile);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    PrintWriter writer = response.getWriter();
    writer.println(toReturn.toString());
    writer.close();
  }

  private LoadTesting getLoadTesting() {
    LoadTesting db = null;

    Object databaseReference = getServletContext().getAttribute(LOAD_TESTING);
    if (databaseReference != null) {
      db = (LoadTesting) databaseReference;
      //   logger.debug("found existing reference " + db + " under " + getServletContext());
    } else {
      logger.error("huh? no existing db reference?");
    }
    return db;
  }


  /**
   * TODO : Is handling a multi-part request slow?
   *
   * @param request
   * @param response
   * @throws javax.servlet.ServletException
   * @throws java.io.IOException
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    boolean isMultipart = ServletFileUpload.isMultipartContent(request);

    JSONObject jsonObject;
    if (isMultipart) {
      logger.debug("got " + request.getParts().size() + " parts isMultipart " + isMultipart);
      jsonObject = getJsonForParts(request);
    } else {
      jsonObject = getJsonForAudio(request);
    }

    PrintWriter writer = response.getWriter();
    writer.println(jsonObject.toString());

    writer.close();
  }

  /**
   * Use apache commons file upload to grab the parts - is this really necessary?
   *
   * @param request
   * @return
   */
  private JSONObject getJsonForParts(HttpServletRequest request) {
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
      String name = next.getName();

      // logger.debug("got name " + name);

      logger.debug("got " + next.getContentType() + " " + next.getFieldName() + " " + next.getString() + " " + next.isInMemory() + " " + next.getSize());
      InputStream inputStream = next.getInputStream();
      BufferedReader bufferedReader = getBufferedReader(inputStream);
      String word = bufferedReader.readLine();
      bufferedReader.close();
      logger.debug("word is " + word);


      next = iterator.next();
      name = next.getName();

      logger.debug("got " + name + " " + next.getContentType() + " " + next.getFieldName() + " " + next.isInMemory() + " " + next.getSize());


      File tempDir = Files.createTempDir();
      File saveFile = new File(tempDir + File.separator + "MyAudioFile.wav");
      // opens input stream of the request for reading data
      writeToFile(next.getInputStream(), saveFile);
      long now = System.currentTimeMillis();
      logger.debug("took " + (now - then) + " millis to parse request and write the file");
      //    logger.debug("wrote to file " + saveFile.getAbsolutePath());
      return getJsonForWordAndAudio(word, saveFile);

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

//    logger.debug("got " + sectionNodes);
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
      ensureMP3s(exercise);
      JSONObject ex = getJsonForExercise(exercise);
      exercises.add(ex);
    }
    return exercises;
  }

  /* protected JSONObject getJsonForExercise(CommonExercise exercise) {
     JSONObject ex = super.getJsonForExercise(exercise);
     return ex;
   }
 */
  private JSONObject getJsonForAudio(HttpServletRequest request) throws IOException {
    // Gets file name for HTTP header
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

  private JSONObject getJsonForScoreFlashcard(PretestScore book) {
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
  }

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
    }
    return audioFileHelper;
  }

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(DATABASE_REFERENCE);
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
      asrScoreForAudio = audioFileHelper.getFlashcardAnswer(testAudioFile, sentence);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    return asrScoreForAudio;
  }


  /**
   * @param db
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#init()
   */
  private String setInstallPath(DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (!new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile, serverProps.getLanguage(), true,
        relativeConfigDir + File.separator + serverProps.getMediaDir());

    return lessonPlanFile;
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}