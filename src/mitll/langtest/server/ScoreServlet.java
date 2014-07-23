package mitll.langtest.server;

import com.google.common.io.Files;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
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
import javax.servlet.http.Part;
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
  private JSONObject chapters;

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

    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    JSONObject jsonObject;

    boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    logger.debug("got " +request.getParts().size() + " parts isMultipart " +isMultipart);

    if (isMultipart) {
      jsonObject = getJsonForParts(request);
    }
    else {
      jsonObject = getJsonForAudio(request);
    }

    PrintWriter writer = response.getWriter();
    writer.println(jsonObject.toString());

    writer.close();
  }

  /**
   * Use apache commons file upload to grab the parts - is this really necessary?
   * @param request
   * @return
   */
  private JSONObject getJsonForParts(HttpServletRequest request)
  {
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

      logger.debug("got " + name +" "  +next.getContentType() + " " + next.getFieldName() + " " + next.isInMemory() + " " + next.getSize());


      File tempDir = Files.createTempDir();
      File saveFile = new File(tempDir + File.separator+ "MyAudioFile.wav");
      // opens input stream of the request for reading data
      writeToFile(next.getInputStream(), saveFile);
      long now = System.currentTimeMillis();
      logger.debug("took " +(now-then) + " millis to parse request and write the file");
      //    logger.debug("wrote to file " + saveFile.getAbsolutePath());
      return getJsonForWordAndAudio(word, saveFile);

    } catch (Exception e) {
      logger.error("got " +e,e);
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

  public static final String ENCODING = "UTF8";

  private BufferedReader getBufferedReader(InputStream resourceAsStream) throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(resourceAsStream, ENCODING));
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

      new ExerciseSorter(db.getSectionHelper().getTypeOrder()).sortByTooltip(copy);

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
    writeToOutputStream(request, saveFile);

    JSONObject jsonObject = getJsonForWordAndAudio(word, saveFile);
    return jsonObject;
  }

  private JSONObject getJsonForWordAndAudio(String word, File saveFile) {
    logger.debug("File written to: " + saveFile.getAbsolutePath());

    AudioFileHelper audioFileHelper = getAudioFileHelper();
    long then = System.currentTimeMillis();
    PretestScore book = getASRScoreForAudio(audioFileHelper, saveFile.getAbsolutePath(), word);
    long now = System.currentTimeMillis();
    logger.debug("score for '" + word + "' took " + (now-then) +
        " millis for " + saveFile.getName() + " = " + book);

    return getJsonForScore(book);
  }

  private void writeToOutputStream(HttpServletRequest request, File saveFile) throws IOException {
    InputStream inputStream = request.getInputStream();
    writeToFile(inputStream, saveFile);


  }

  private void writeToFile(InputStream inputStream, File saveFile) throws IOException {
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

  private DatabaseImpl db;
  private PathHelper pathHelper;
  private AudioFileHelper audioFileHelper;

  /**
   * Get a reference to the current database object, made in the main LangTestDatabaseImpl servlet
   * @see #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @see #getJsonForWordAndAudio(String, java.io.File)
   * @return
   */
  private AudioFileHelper getAudioFileHelper() {
    if (audioFileHelper == null) {
      ServletContext servletContext = getServletContext();
      pathHelper = new PathHelper(servletContext);
      db = getDatabase();
      serverProps = db.getServerProps();

      String config = servletContext.getInitParameter("config");
      this.relativeConfigDir = "config" + File.separator + config;
      logger.debug("rel " + relativeConfigDir);
      this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
      logger.debug("configDir " + configDir);

      audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, null);
    }
    return audioFileHelper;
  }

  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute("databaseReference");
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      logger.debug("found existing database reference " + db + " under " +getServletContext());
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