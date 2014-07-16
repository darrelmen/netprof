package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import scala.actors.threadpool.Arrays;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 *
 * All in support of Liz tethered iOS app.
 *
 * User: GO22670
 */
@SuppressWarnings("serial")
public class ScoreServlet extends DatabaseServlet {
  private static final Logger logger = Logger.getLogger(ScoreServlet.class);
  static final int BUFFER_SIZE = 4096;
  private String relativeConfigDir;
  private String configDir;


  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
        " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    getAudioFileHelper();

    if (chapters == null) {
      chapters = getJsonChapters();
    }

    PrintWriter writer = response.getWriter();
    writer.println(chapters.toString());

    writer.close();
  }

  JSONObject chapters;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    logger.debug("ScoreServlet.doPost : Request " + request.getQueryString() + " path " + pathInfo +
      " uri " + request.getRequestURI() + "  " + request.getRequestURL() + "  " + request.getServletPath());

    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    String requestURI = request.getRequestURI();

    JSONObject jsonObject;
    if (requestURI.contains("chapters")) {
      jsonObject = getJsonChapters();
    }
    else {
      jsonObject = getJsonForAudio(request);
    }

    PrintWriter writer = response.getWriter();
    writer.println(jsonObject.toString());

    writer.close();
  }

  private JSONObject getJsonChapters() {
    JSONObject jsonObject;
    jsonObject =  new JSONObject();
    setInstallPath(true,db);
    db.getExercises();
    List<SectionNode> sectionNodes = db.getSectionHelper().getSectionNodes();
    for (SectionNode node : sectionNodes) {
      node.getName();

      Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();
      List<String> value = new ArrayList<String>();
      value.add(node.getName());
      typeToValues.put(node.getType(), value);
      Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToValues);

      List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);

      new ExerciseSorter(db.getSectionHelper().getTypeOrder()).getSortedByUnitThenAlpha(copy, false);

      JSONArray exercises = new JSONArray();

      for (CommonExercise exercise : copy) {
        ensureMP3s(exercise);
        JSONObject ex = new JSONObject();
        ex.put("fl",exercise.getForeignLanguage());
        ex.put("tl",exercise.getTransliteration());
        ex.put("en",exercise.getEnglish());
        ex.put("ref",exercise.hasRefAudio() ? exercise.getRefAudio() :"NO");
        exercises.add(ex);
      }
      jsonObject.put(node.getName(), exercises);
    }
    return jsonObject;
  }

  private void ensureMP3s(CommonExercise byID) {
     ensureMP3(byID.getRefAudio(), false);

    //if (!audioAttributes.isEmpty()) { logger.warn("ensureMP3s : ref audio for " + byID); }
  }

  private void ensureMP3(String wavFile, boolean overwrite) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();
      //logger.debug("ensureMP3 : wav " + wavFile + " under " + parent);

      AudioConversion audioConversion = new AudioConversion();
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      audioConversion.ensureWriteMP3(wavFile, parent, overwrite);
    }
  }

  private JSONObject getJsonForAudio(HttpServletRequest request) throws IOException {
    // Gets file name for HTTP header
    String fileName = request.getHeader("fileName");
    String word = request.getHeader("word");

    File tempDir = Files.createTempDir();
    File saveFile = new File(tempDir + File.separator+ fileName);

    // prints out all header values
/*    logger.debug("===== Begin headers =====");
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String headerName = names.nextElement();
      System.out.println(headerName + " = " + request.getHeader(headerName));
    }
    logger.debug("===== End headers =====\n");*/

    // opens input stream of the request for reading data
    InputStream inputStream = request.getInputStream();

    // opens an output stream for writing file
    FileOutputStream outputStream = new FileOutputStream(saveFile);

    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = -1;
    logger.debug("Receiving data...");

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    // System.out.println("Data received.");
    outputStream.close();
    inputStream.close();

    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    PretestScore book = getASRScoreForAudio(audioFileHelper, saveFile.getAbsolutePath(), word);
    logger.debug("score for " + word + " and " + saveFile.getName() + " = " + book);

    JSONObject jsonObject = getJsonForScore(book);
    return jsonObject;
  }

  private JSONObject getJsonForScore(PretestScore book) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score", book.getHydecScore());

    for (Map.Entry<NetPronImageType,List<TranscriptSegment>> pair : book.getsTypeToEndTimes().entrySet()) {
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

  DatabaseImpl db;
  PathHelper pathHelper;
  private AudioFileHelper getAudioFileHelper() {
    ServletContext servletContext = getServletContext();
    pathHelper = new PathHelper(servletContext);
    db = getDatabase(servletContext, pathHelper);

    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    return new AudioFileHelper(pathHelper, serverProps, db, null);
  }

  /**
   * Do alignment of audio file against sentence.
   *
   * @param audioFileHelper
   * @param testAudioFile
   * @param sentence
   * @return
   */
  public PretestScore getASRScoreForAudio(AudioFileHelper audioFileHelper, String testAudioFile, String sentence) {
   // logger.debug("getASRScoreForAudio " +testAudioFile);

    PretestScore asrScoreForAudio = null;
    try {
      asrScoreForAudio = audioFileHelper.getASRScoreForAudio(-1, testAudioFile, sentence, 128, 128, false,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), "");
    } catch (Exception e) {
      logger.error("got "+e,e);
    }

    return asrScoreForAudio;
  }


  /**
   * @see LangTestDatabaseImpl#init()
   * @param useFile
   * @param db
   * @return
   */
  public String setInstallPath(boolean useFile, DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile, serverProps.getLanguage(), useFile,
        relativeConfigDir+File.separator+serverProps.getMediaDir());

    return lessonPlanFile;
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }

/*  public static void main(String [] arg) {
    new ScoreServlet()
  }*/
}