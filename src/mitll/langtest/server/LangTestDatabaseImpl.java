package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.scoring.DTWScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Supports all the database interactions.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, AutoCRTScoring {
  private static Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);
  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  public static final String FIRST_N_IN_ORDER = "firstNInOrder";
  private static final String DATA_COLLECT_MODE = "dataCollect";
  private static final String COLLECT_AUDIO = "collectAudio";
  private static final String COLLECT_AUDIO_DEFAULT = "true";
  private static final String BIAS_TOWARDS_UNANSWERED = "biasTowardsUnanswered";
  private static final String USE_OUTSIDE_RESULT_COUNTS = "useOutsideResultCounts";
  private static final String OUTSIDE_FILE = "outsideFile";
  private static final String OUTSIDE_FILE_DEFAULT = "distributions.txt";
  private static final String H2_DATABASE = "h2Database";
  private static final String H2_DATABASE_DEFAULT = "vlr-parle";
  private static final String URDU = "urdu";
  private static final String READ_FROM_FILE = "readFromFile";

  private static final int MB = (1024 * 1024);
  public static final String ANSWERS = "answers";
  private static final int TIMEOUT = 30;
  private static final String IMAGE_WRITER_IMAGES = "audioimages";
  private DatabaseImpl db;
  private ASRScoring asrScoring;
  private AutoCRT autoCRT;
  private boolean makeFullURLs = false;
  private Properties props = null;
  private String relativeConfigDir;
  private String configDir;
  private boolean dataCollectMode;
  private boolean collectAudio;
  private boolean biasTowardsUnanswered, useOutsideResultCounts;
  private String outsideFile;
  private boolean isUrdu;
  private int firstNInOrder;
  private boolean isDataCollectAdminView;


  private Cache<String, String> userToExerciseID = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .maximumSize(10000)
      .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES).build();

  /**
   * This allows us to upload an exercise file and create a new {@link Site}.
   * @see mitll.langtest.client.DataCollectAdmin#makeDataCollectNewSiteForm2
   * @see SiteDeployer
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    boolean isMultipart = ServletFileUpload.isMultipartContent(new ServletRequestContext(request));
    if (isMultipart) {
      SiteDeployer siteDeployer = new SiteDeployer();
      Site site = siteDeployer.getSite(request, configDir);
      if (site == null) {
        super.service(request, response);
        return;
      }

      siteDeployer.doSiteResponse(db,response, siteDeployer, site);
    } else {
      super.service(request, response);
    }
  }

  public Site getSiteByID(long id) {
    return db.getSiteByID(id);
  }

  @Override
  public List<Site> getSites() {
    return db.getDeployedSites();
  }

  /**
   * Copy template to something named by site name
   * copy exercise file from media to site/config/template
   * set fields in config.properties
   *   - apptitle
   *   - release date
   *    - lesson plan file
   *
   *    then copy to install path/../name
   *
   * @param id
   * @param name
   * @param language
   * @param notes
   * @return
   */
  @Override
  public boolean deploySite(long id, String name, String language, String notes) {
    Site siteByID = db.getSiteByID(id);

    SiteDeployer siteDeployer = new SiteDeployer();

    siteByID = db.updateSite(siteByID, name, language, notes);
    if (!siteDeployer.isValidName(siteByID, getInstallPath())) {
      return false;
    }
    boolean b = siteDeployer.deploySite(siteByID, configDir, getInstallPath());
    if (b) {
      db.deploy(siteByID);
    } else {
      logger.warn("didn't deploy " + siteByID);
    }

    if (!b) return b;
    waitUntilDeployed(siteByID);

    String firstName = "Unknown user #" + siteByID.creatorID;
    String lastName = "Unk.";
    for (User user : db.getUsers())
      if (user.id == siteByID.creatorID) {
        firstName = user.firstName;
        lastName = user.lastName;
      }

    String subject = "Site " + name + " deployed";
    String message = "Hi,\n" +
        "At site " + getBaseUrl() + "" +
        " User '" + firstName + " " + lastName + "' deployed site " + name + ".\n" +
        "Thought you might want to know.\n" +
        "Thanks,\n" +
        "Your friendly web admin.";
    new MailSupport(props).email(subject, message);

    return true;
  }

  private void waitUntilDeployed(Site siteByID) {
    int tries = 3;
    boolean valid = false;     // todo improve
    while (tries-- > 0) {
      String baseUrl = "";
      try {
        baseUrl = getBaseUrl(siteByID.name);
        URL oracle = new URL(baseUrl);
        URLConnection yc = oracle.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            yc.getInputStream()));
        in.close();

        valid = true;
      } catch (Exception e) {
        logger.info("reading " + baseUrl + " got " + e);
      }
      if (!valid) {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private String getBaseUrl() {
    HttpServletRequest request = getThreadLocalRequest();
    return getBaseUrl(request.getContextPath());
  }

  private String getBaseUrl(String name) {

    HttpServletRequest request = getThreadLocalRequest();

    if ((request.getServerPort() == 80) ||
        (request.getServerPort() == 443)) {
      return request.getScheme() + "://" +
          request.getServerName() + "/" + name;
    } else {
      return request.getScheme() + "://" +
          request.getServerName() + ":" + request.getServerPort() +
          "/" + name;
    }
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#getExercises
   * @param userID
   * @param arabicDataCollect
   * @return
   */
  public List<ExerciseShell> getExerciseIds(long userID, boolean arabicDataCollect) {
    logger.debug("getting exercise ids for User id=" + userID + " config " + relativeConfigDir);
    List<Exercise> exercises = getExercises(userID, arabicDataCollect);
    List<ExerciseShell> ids = new ArrayList<ExerciseShell>();
    for (Exercise e : exercises) {
      ids.add(new ExerciseShell(e.getID(), e.getTooltip()));
    }
    logMemory();
    return ids;
  }

  private void logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory()-free;
    long max = rt.maxMemory();
    logger.debug("heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#getExercisesInOrder()
   * @return
   */
  public List<ExerciseShell> getExerciseIds() {
    List<Exercise> exercises = getExercises();
    List<ExerciseShell> ids = new ArrayList<ExerciseShell>();
    for (Exercise e : exercises) {
      ids.add(new ExerciseShell(e.getID(), e.getTooltip()));
    }

    return ids;
  }

  public Exercise getExercise(String id) {
    List<Exercise> exercises = getExercises();
    for (Exercise e : exercises) {
      if (id.equals(e.getID())) {
        return e;
      }
    }
    return null;
  }

  public Exercise getExercise(String id, long userID, boolean arabicDataCollect) {
    List<Exercise> exercises = getExercises(userID, arabicDataCollect);
    for (Exercise e : exercises) {
      if (id.equals(e.getID())) {
        return e;
      }
    }
    return null;
  }

  /**
   * Called from the client.<br></br>
   * Complicated? Sure.
   * <ul>
   * <li>If in data collection mode, we have the option of biasing collection towards
   * items that have not yet been answered. </li>
   * <li>And for those, we can merge the counts from another collection system
   * and bias the presentation order to try to get answers for items that have least coverage in both systems. </li>
   * <li>Alternatively, we can present the first N items in order then random after that (based on the user). </li>
   * <li>Or we can attempt to present items in an order that biases towards presenting items that were all graded "correct"  </li>
   * first and all graded "incorrect" last.
   * </ul>
   *
   * @param userID
   * @param arabicDataCollect
   * @return
   * @see mitll.langtest.client.exercise.ExerciseList#getExercises(long)
   */
  public List<Exercise> getExercises(long userID, boolean arabicDataCollect) {
    String lessonPlanFile = configDir + File.separator + props.get("lessonPlanFile");

    synchronized (this) {
      if (autoCRT == null) {
        autoCRT = new AutoCRT(db.getExport(), this, getInstallPath(), relativeConfigDir);
      }
    }
    List<Exercise> exercises;
    if (dataCollectMode) {
      logger.debug("in data collect mode");
      if (biasTowardsUnanswered) {
        if (useOutsideResultCounts) {
          String outsideFileOverride = outsideFile;
          if (lessonPlanFile.contains("farsi")) outsideFileOverride = configDir + File.separator + "farsi.txt";
          else if (lessonPlanFile.contains("urdu")) outsideFileOverride = configDir + File.separator + "urdu.txt";
          else if (lessonPlanFile.contains("sudanese")) outsideFileOverride = configDir + File.separator + "sudanese.txt";
          exercises = db.getExercisesBiasTowardsUnanswered(userID, outsideFileOverride);
          db.setOutsideFile(outsideFileOverride);
        }
        else {
          exercises = db.getExercisesBiasTowardsUnanswered(userID);
        }
      } else {
        exercises = db.getExercisesFirstNInOrder(userID, firstNInOrder);
      }
      if (!collectAudio) {
        logger.debug("*not* collecting audio, just text");

        for (Exercise e : exercises) {
          e.setRecordAnswer(false);
          e.setPromptInEnglish(false);
        }
      }
    }
    else {
      exercises = arabicDataCollect ? db.getRandomBalancedList() : db.getExercises(userID);
    }

    if (makeFullURLs) convertRefAudioURLs(exercises);
    if (!exercises.isEmpty())
      logger.debug("for user #" + userID +" got " + exercises.size() + " exercises , first " + exercises.iterator().next());
          //" ref sentence = '" + exercises.iterator().next().getRefSentence() + "'");
    //logMemory();

    return exercises;
  }

  /**
   * Called from the client.
   * @see mitll.langtest.client.exercise.ExerciseList#getExercisesInOrder()
   * @return
   */
  public List<Exercise> getExercises() {
    List<Exercise> exercises = db.getExercises();
    if (makeFullURLs) convertRefAudioURLs(exercises);
    return exercises;
  }

  private void convertRefAudioURLs(List<Exercise> exercises) {
    URLUtils urlUtils = new URLUtils(getThreadLocalRequest());
    for (Exercise e : exercises) {
      if (e.getRefAudio() != null) {
        e.setRefAudio(urlUtils.makeURL(e.getRefAudio()));
      }
    }
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   *
   * @see mitll.langtest.client.exercise.GradedExerciseList#getNextUngraded
   * @param user
   * @param expectedGrades
   * @param filterForArabicTextOnly
   * @param englishOnly
   * @return next ungraded exercise
   */
  public Exercise getNextUngradedExercise(String user, int expectedGrades, boolean filterForArabicTextOnly, boolean englishOnly) {
    synchronized (this) {
      ConcurrentMap<String,String> stringStringConcurrentMap = userToExerciseID.asMap();
      Collection<String> values = stringStringConcurrentMap.values();
      String currentExerciseForUser = userToExerciseID.getIfPresent(user);
      logger.debug("getNextUngradedExercise for " + user + " current " + currentExerciseForUser + " expected " + expectedGrades);

      Collection<String> currentActiveExercises = new HashSet<String>(values);

      if (currentExerciseForUser != null) {
        currentActiveExercises.remove(currentExerciseForUser); // it's OK to include the one the user is working on now...
      }
      logger.debug("getNextUngradedExercise current set minus " + user + " is " + currentActiveExercises);

      return db.getNextUngradedExercise(currentActiveExercises, expectedGrades,
          filterForArabicTextOnly, filterForArabicTextOnly, !filterForArabicTextOnly, englishOnly);
    }
  }

  public void checkoutExerciseID(String user, String id) {
    synchronized (this) {
      userToExerciseID.put(user, id);
      logger.debug("checkoutExerciseID : after adding " + user + "->" + id +
          " active exercise map now " + userToExerciseID.asMap());
    }
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.Exercise)
   *
   * @param wavFile
   */
  public void ensureMP3(String wavFile) {
    logger.debug("ensure mp3 for " +wavFile);
    new AudioConversion().ensureWriteMP3(wavFile, getInstallPath());
  }

  /**
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   *
   * TODO : Worrying about absolute vs relative path is maddening.  Must be a better way!
   *
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height
   * @return path to an image file
   * */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height) {
    ImageWriter imageWriter = new ImageWriter();

    String wavAudioFile = getWavAudioFile(audioFile);

    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) return new ImageResponse(); // success = false!
    String imageOutDir = getImageOutDir();
    String absolutePathToImage = imageWriter.writeImageSimple(wavAudioFile, getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1);
    String installPath = getInstallPath();
    //System.out.println("Absolute path to image is " + absolutePathToImage);

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    }
    else {
      logger.error("huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = optionallyMakeURL(relativeImagePath);
    double duration = new AudioCheck().getDurationInSeconds(wavAudioFile);
    logger.debug("for " + wavAudioFile + " type " + imageType + " rel path is " + relativeImagePath +
        " url " + imageURL + " duration " + duration);

    return new ImageResponse(reqid,imageURL, duration);
  }

  private String getWavAudioFile(String audioFile) {
    if (audioFile.endsWith(".mp3")) {
      String wavFile = removeSuffix(audioFile) +".wav";
      File test = getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : getWavForMP3(audioFile);
    }
    else if (makeFullURLs) {
      audioFile = new URLUtils(getThreadLocalRequest()).convertURLToRelativeFile(audioFile);
    }
    return audioFile;
  }

  /**
   * Does DTW Scoring.
   *
   * @see mitll.langtest.client.scoring.DTWScoringPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param audioFile
   * @param refs
   * @param width
   * @param height
   * @deprecated use {@link #getASRScoreForAudio}
   * @return
   */
  public PretestScore getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height) {
    logger.info("getASRScoreForAudio " + audioFile + " against " + refs);
    if (refs.isEmpty()) {
      logger.error("getASRScoreForAudio no refs? ");
      PretestScore pretestScore = new PretestScore();
      pretestScore.setReqid(reqid);
      return pretestScore;
    }
    String installPath = getInstallPath();
    AudioConversion audioConversion = new AudioConversion();

    File testAudioFile = audioConversion.getProperAudioFile(audioFile, installPath);

    logger.debug("\tscoring after conversion " + testAudioFile.getAbsolutePath());
    String name = testAudioFile.getName();

    String imageOutDir = getImageOutDir();
    String testAudioDir = testAudioFile.getParent();

    logger.debug("\tscoring " + name + " in dir " +testAudioDir);

    Collection<String> names = new ArrayList<String>();
    String refAudioDir = null;

    for (String ref : refs) {
      File properAudioFile = audioConversion.getProperAudioFile(ref, installPath);
      if (refAudioDir == null) refAudioDir = properAudioFile.getParent();
      names.add(removeSuffix(properAudioFile.getName()));
    }
    //System.out.println("converted refs " + refs +" into " + names);

    if (names.isEmpty()) {
      logger.error("no valid ref files");
      return new PretestScore();
    } else {
      DTWScoring dtwScoring;
      dtwScoring = new DTWScoring(installPath);
      PretestScore pretestScore =
          dtwScoring.score(testAudioDir, removeSuffix(name), refAudioDir, names, imageOutDir, width, height);
      pretestScore.setReqid(reqid);
      //System.out.println("score " + pretestScore);
      return pretestScore;
    }
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * @return
   */
  public Map<String, String> getProperties() {
    Map<String,String> kv = new HashMap<String, String>();
    for (Object prop : props.keySet()) {
      String sp = (String)prop;
      kv.put(sp,props.getProperty(sp).trim());
    }
    logger.debug("for config " + relativeConfigDir + " prop file has " + kv.size() + " properties : " + props.keySet());
    return kv;
  }

  /**
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @return
   */
  public PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg) {
      return getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
          Collections.EMPTY_LIST, Collections.EMPTY_LIST);
  }

  /**
   * Get score when doing autoCRT on an audio file.
   * @see AutoCRT#getAutoCRTAnswer(String, mitll.langtest.shared.Exercise, int, java.io.File, mitll.langtest.shared.AudioAnswer.Validity, int, String)
   * @param testAudioFile
   * @param lmSentences
   * @param background
   * @paramx vocab
   * @return PretestScore for audio
   */
  public PretestScore getASRScoreForAudio(File testAudioFile, List<String> lmSentences,
                                          List<String> background) {
     return getASRScoreForAudio(0, testAudioFile.getPath(), "", 128, 128, false, lmSentences, background);
  }

  /**
   * For now, we don't use a ref audio file, since we aren't comparing against a ref audio file with the DTW/sv pathway.
   *
   * @see #writeAudioFile
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param width image dim
   * @param height  image dim
   * @param useScoreToColorBkg
   * @param background
   * @param lmSentences
   * @param background
   * @return PretestScore
   **/
  private PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg,
                                          List<String> lmSentences,
                                          List<String> background) {
    logger.info("getASRScoreForAudio scoring " + testAudioFile + " with " + sentence + " req# " + reqid);

    assert(testAudioFile != null && sentence != null);
    if (asrScoring == null) {
        asrScoring = new ASRScoring(getInstallPath(), getProperties()); // lazy eval since...?
    }
    testAudioFile = dealWithMP3Audio(testAudioFile);

    String installPath = getInstallPath();

    DirAndName testDirAndName = new DirAndName(testAudioFile, installPath).invoke();
    String testAudioName = testDirAndName.getName();
    String testAudioDir = testDirAndName.getDir();

    logger.debug("getASRScoreForAudio scoring " + testAudioName + " in dir " + testAudioDir);
    PretestScore pretestScore;
    pretestScore = asrScoring.scoreRepeat(
        testAudioDir, removeSuffix(testAudioName),
        sentence,
        getImageOutDir(), width, height, useScoreToColorBkg, lmSentences, background);
    pretestScore.setReqid(reqid);

    if (makeFullURLs) {
      URLUtils urlUtils = new URLUtils(getThreadLocalRequest());
      Map<NetPronImageType, String> typeToURL = new HashMap<NetPronImageType, String>();
      for (Map.Entry<NetPronImageType, String> kv : pretestScore.getsTypeToImage().entrySet()) {
        typeToURL.put(kv.getKey(), urlUtils.makeURL(kv.getValue()));
      }
      pretestScore.setsTypeToImage(typeToURL);
    }
    return pretestScore;
  }

  /**
   * @see #getScoreForAudioFile
   * @param testAudioFile
   * @return
   */
  private String dealWithMP3Audio(String testAudioFile) {
    if (testAudioFile.endsWith(".mp3")) {
      String noSuffix = removeSuffix(testAudioFile);
      String wavFile = noSuffix +".wav";
      File test = getAbsoluteFile(wavFile);
      if (!test.exists()) {
        logger.warn("expecting audio file with wav extension, but didn't find "  +test.getAbsolutePath());
      }
      return test.exists() ? test.getAbsolutePath() :  getWavForMP3(testAudioFile);
    }
    else {
      return testAudioFile;
    }
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - ".mp3".length());
  }

  /**
   * @see #dealWithMP3Audio(String)
   * @see #getImageForAudioFile(int, String, String, int, int)
   * @param audioFile
   * @return
   */
  private String getWavForMP3(String audioFile) {
    return getWavForMP3(audioFile, getInstallPath());
  }

  /**
   * Ultimately does lame --decode from.mp3 to.wav
   *
   * Worris about converting from either a relative path to an absolute path (given the webapp install location)
   * or if audioFile is a URL, converting it to a relative path before making an absolute path.
   *
   * Gotta be a better way...
   *
   * @see #getWavForMP3(String)
   * @param audioFile to convert
   * @return
   */
  private String getWavForMP3(String audioFile, String installPath) {
    assert(audioFile.endsWith(".mp3"));
    if (makeFullURLs) audioFile = new URLUtils(getThreadLocalRequest()).convertURLToRelativeFile(audioFile);
    String absolutePath = getAbsolute(audioFile,installPath).getAbsolutePath();

    if (!new File(absolutePath).exists())
      logger.error("getWavForMP3 : expecting file at " + absolutePath);
    else {
      AudioConversion audioConversion = new AudioConversion();
      File file = audioConversion.convertMP3ToWav(absolutePath);
      if (file.exists()) {
        String orig = audioFile;
        audioFile = file.getAbsolutePath();
        logger.info("from " + orig + " wrote to " + file + " or " + audioFile);
      }
      else {
        logger.error("getImageForAudioFile : can't find " + file.getAbsolutePath());
      }
    }
    assert(audioFile.endsWith(".wav"));
    return audioFile;
  }

  /**
   *
   * @param exid
   * @param arabicTextDataCollect
   * @return
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public ResultsAndGrades getResultsForExercise(String exid, boolean arabicTextDataCollect) {
    ResultsAndGrades resultsForExercise =
        arabicTextDataCollect ?
            db.getResultsForExercise(exid, true, true, false) :
            db.getResultsForExercise(exid);

    ensureMP3(resultsForExercise.results);
    return resultsForExercise;
  }

  /**
   * Make sure we have mp3 files in results.
   * @param results
   */
  private void ensureMP3(Collection<Result> results) {
    for (Result r : results) {
      if (r.answer.endsWith(".wav")) {
        new AudioConversion().ensureWriteMP3(r.answer, getInstallPath());
      }
    }
  }

  // Answers ---------------------

  /**
   *
   *
   * @param userID
   * @param exercise
   * @param questionID
   * @param answer
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   */
  public void addTextAnswer(int userID, Exercise exercise, int questionID, String answer) {
    db.addAnswer(userID, exercise, questionID, answer);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID) {
    return db.isAnswerValid(userID, exercise, questionID, db);
  }

  public double getScoreForAnswer(Exercise e, int questionID, String answer) {
    return autoCRT.getScoreForExercise(e, questionID, answer);
  }

  // Grades ---------------------

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#addGrade
   * @param exerciseID
   * @return
   */
  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {
    return db.addGrade(exerciseID, toAdd);
  }

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#changeGrade(mitll.langtest.shared.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) {
    db.changeGrade(toChange);
  }

  @Override
  public synchronized int userExists(String login) {
    if (db == null) getProperties();
    return db.userExists(login);
  }

  // Users ---------------------

  public long addUser(int age, String gender, int experience) {
    HttpServletRequest request = getThreadLocalRequest();
    // String header = request.getHeader("X-FORWARDED-FOR");
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    String ip = request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
    return db.addUser(age, gender, experience, ip);
  }

  /**
   *
   * @param age
   * @param gender
   * @param experience
   * @param firstName
   * @param lastName
   * @param nativeLang
   * @param dialect
   * @param userID
   * @return
   */
  @Override
  public long addUser(int age, String gender, int experience,
                      String firstName, String lastName, String nativeLang, String dialect, String userID) {
    logger.info("Adding user " + userID + " " + firstName + " " + lastName);
    HttpServletRequest request = getThreadLocalRequest();
    // String header = request.getHeader("X-FORWARDED-FOR");
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    String ip = request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
    long l = db.addUser(age, gender, experience, ip, firstName, lastName, nativeLang, dialect, userID);

    if (l != 0 && isDataCollectAdminView) {
      String subject = "User " + lastName + " registered";
      String message = "Hi,\n" +
          "At site " + getBaseUrl() + "\n" +
          " got new user " + firstName + " " + lastName + ".\n" +
          "Should this person be enabled?\n" +
          "Thanks,\n" +
          "Your friendly web admin.";
      new MailSupport(props).email(subject, message);
    }

    return l;
  }

  public List<User> getUsers() {
    return db.getUsers();
  }

  @Override
  public boolean isAdminUser(long id) { return db.isAdminUser(id); }
  @Override
  public boolean isEnabledUser(long id) { return db.isEnabledUser(id); }
  @Override
  public void setUserEnabled(long id, boolean enabled) { db.setUserEnabled(id, enabled); }

  // Results ---------------------

  /**
   * @return
   * @see mitll.langtest.client.ResultManager#showResults()
   */
  @Override
  public List<Result> getResults(int start, int end) {
    List<Result> results = db.getResults();
    Collections.sort(results, new Comparator<Result>() {
      @Override
      public int compare(Result o1, Result o2) {
        return o1.uniqueID < o2.uniqueID ? -1 : o1.uniqueID > o2.uniqueID ? +1 : 0;
      }
    });
    List<Result> resultList = results.subList(start, end);
    List<Result> copy = new ArrayList<Result>(resultList);
    return copy;
  }

  @Override
  public int getNumResults() { return db.getNumResults(); }

  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   *
   * Client references:
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param base64EncodedString generated by flash on the client
   * @param plan which set of exercises
   * @param exercise exercise within the plan
   * @param questionID question within the exercise
   * @param user answering the question
   * @param doAutoCRT if true the act of posting an audio file triggers ASR and scoring of the returned reco sentence
   * @param reqid request id from the client, so it can potentially throw away out of order responses
   * @param flq was the prompt a foreign language query
   * @param audioType regular or fast then slow audio recording
   * @return URL to audio on server and if audio is valid (not too short, etc.)
   */
  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int questionID,
                                    int user, boolean doAutoCRT, int reqid, boolean flq, String audioType) {
    String wavPath = getLocalPathToAnswer(plan, exercise, questionID, user);

    File file = getAbsoluteFile(wavPath);

    AudioCheck.ValidityAndDur validity = new AudioConversion().convertBase64ToAudioFiles(base64EncodedString, file);
    boolean isValid = validity.validity == AudioAnswer.Validity.OK;
    if (!isValid) {
      logger.warn("got invalid audio file (" + validity +
          ") user = " + user + " exercise " + exercise +
          " question " + questionID + " file " + file.getAbsolutePath());
    }
    db.addAudioAnswer(user, plan, exercise, questionID, file.getPath(),
        isValid, flq, true, audioType, validity.durationInMillis);

    String wavPathWithForwardSlashSeparators = ensureForwardSlashes(wavPath);
    String url = optionallyMakeURL(wavPathWithForwardSlashSeparators);
    // logger.info("writeAudioFile converted " + wavPathWithForwardSlashSeparators + " to url " + url);

    if (doAutoCRT && isValid) {
      return autoCRT.getAutoCRTAnswer(exercise, getExercise(exercise), reqid, file, validity.validity, questionID, url);
    }
    else {
      return new AudioAnswer(url, validity.validity, reqid);
    }
  }

  @Override
  public Map<User, Integer> getUserToResultCount() {
    return db.getUserToResultCount();
  }

  @Override
  public Map<Integer, Integer> getResultCountToCount() {
    return db.getResultCountToCount();
  }
  @Override
  public Map<String, Integer> getResultByDay() {
    return db.getResultByDay();
  }
  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return db.getResultByHourOfDay();
  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   * @return
   */
  public Map<String, List<Integer>> getResultPerExercise() {
    return db.getResultPerExercise();
  }

  @Override
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return db.getResultCountsByGender();
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return db.getDesiredCounts();
  }

/*  @Override
  public Map<Integer, Float> getHoursToCompletion(boolean useFile) {
    return db.getHoursToCompletion();
  }*/

  public List<Session> getSessions() {
    return db.getSessions();
  }

  public Map<String,Number> getResultStats() {
    return db.getResultStats();
  }

  private String optionallyMakeURL(String wavPathWithForwardSlashSeparators) {
    return makeFullURLs ?
        new URLUtils(getThreadLocalRequest()).makeURL(wavPathWithForwardSlashSeparators) :
        wavPathWithForwardSlashSeparators;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private File getAbsoluteFile(String filePath) {
    String realContextPath = getInstallPath();
    return getAbsolute(filePath, realContextPath);
  }

  private File getAbsolute(String filePath, String realContextPath) {
    return new File(realContextPath, filePath);
  }

  /**
   * Figure out from the servlet context {@link #getServletContext()} and
   * the thread local request {@link #getThreadLocalRequest()} where this instance of the webapp was
   * installed.  This is really important since we use it to convert back and forth between
   * relative and absolute paths to audio and image files.
   * @return path to webapp install location
   */
  private String getInstallPath() {
    ServletContext context = getServletContext();
    if (context == null) {
      logger.error("no servlet context.");
      return "";
    }

    String realContextPath = getServletContext().getRealPath(getServletContext().getContextPath());

    List<String> pathElements = Arrays.asList(realContextPath.split(realContextPath.contains("\\") ? "\\\\" : "/"));

    // hack to deal with the app name being duplicated in the path
    if (pathElements.size() > 1) {
      String last = pathElements.get(pathElements.size() - 1);
      String nextToLast = pathElements.get(pathElements.size() - 2);
      if (last.equals(nextToLast)) {
        String nodups = realContextPath.substring(0, realContextPath.length() - last.length() -1); // remove trailing slash
        realContextPath = nodups;
      }
    }

    return realContextPath;
  }

  /**
   * Make a place to store the audio answer, of the form:<br></br>
   *
   * "answers"/plan/exercise/question/"subject-"user/"answer_"timestamp".wav"  <br></br>
   *
   * e.g. <br></br>
   *
   * answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav <br></br>
   *
   * or absolute  <br></br>
   *
   * C:\Users\go22670\apache-tomcat-7.0.25\webapps\netPron2\answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav
   *
   * @see #writeAudioFile
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return a path relative to the install dir
   */
  private String getLocalPathToAnswer(String plan, String exercise, int question, int user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-" + user;
    String currentTestDir = tomcatWriteDirectory + File.separator + planAndTestPath;
    String wavPath = currentTestDir + File.separator + "answer_" + System.currentTimeMillis() + ".wav";
    File audioFilePath = new File(currentTestDir);
    boolean mkdirs = audioFilePath.mkdirs();

    return wavPath;
  }

  private String getTomcatDir() {
    String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = ANSWERS;

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = ANSWERS;
    }
    return tomcatWriteDirectory;
  }

  /**
   * @see #getImageForAudioFile(int, String, String, int, int)
   * @return path to image output dir
   */
  private String getImageOutDir() {
    String imageOutdir = getServletContext().getInitParameter("imageOutdir");
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir);
    if (!test.exists()) {
      test = getAbsoluteFile(imageOutdir);
//      System.out.println("made image out dir at " + test.getAbsolutePath());
      test.mkdirs();
    }
    return imageOutdir;
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }

  @Override

  public void init() {
    readProperties(getServletContext());
    boolean useFile = !props.getProperty(READ_FROM_FILE, "false").equals("false");
    logger.info("use file " + useFile);
    setInstallPath(useFile);
  }

  private String setInstallPath(boolean useFile) {
    String lessonPlanFile = configDir + File.separator + props.get("lessonPlanFile");
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    //logger.debug("getExercises isurdu = " + isUrdu + " datacollect mode " + dataCollectMode);
    db.setInstallPath(getInstallPath(), lessonPlanFile, relativeConfigDir, isUrdu, useFile);

    return lessonPlanFile;
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   *
   * Note that this will only ever be called once.
   * @see #init()
   * @param servletContext
   */
  private void readProperties(ServletContext servletContext) {
    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = getInstallPath() + File.separator + relativeConfigDir;

    readPropertiesFile();

    String h2DatabaseFile = props.getProperty(H2_DATABASE, H2_DATABASE_DEFAULT);
    db = new DatabaseImpl(configDir, h2DatabaseFile);

    try {
      firstNInOrder = Integer.parseInt(props.getProperty(FIRST_N_IN_ORDER, "" + Integer.MAX_VALUE));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + FIRST_N_IN_ORDER,e);
      firstNInOrder = Integer.MAX_VALUE;
    }
    dataCollectMode = !props.getProperty(DATA_COLLECT_MODE, "false").equals("false");
    collectAudio = !props.getProperty(COLLECT_AUDIO, COLLECT_AUDIO_DEFAULT).equals("false");
    isUrdu = !props.getProperty(URDU, "false").equals("false");
    biasTowardsUnanswered = !props.getProperty(BIAS_TOWARDS_UNANSWERED, "true").equals("false");
    useOutsideResultCounts = !props.getProperty(USE_OUTSIDE_RESULT_COUNTS, "true").equals("false");
    isDataCollectAdminView = !props.getProperty("dataCollectAdminView", "false").equals("false");
    outsideFile = props.getProperty(OUTSIDE_FILE, OUTSIDE_FILE_DEFAULT);
    String dateFromManifest = getDateFromManifest(servletContext);
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      logger.debug("Date from manifest " + dateFromManifest);
      props.setProperty("releaseDate",dateFromManifest);
    }
  }

  private String getDateFromManifest(ServletContext servletContext) {
    InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");

    try {
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Built-Date");
    }
    catch(Exception ex) {
//      logger.warn("Error while reading version: " + ex.getMessage());
    }
    return "";
  }

  private void readPropertiesFile() {
    String configFile = getServletContext().getInitParameter("configFile");
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    String configFileFullPath = configDir + File.separator + configFile;
    if (!new File(configFileFullPath).exists()) {
      logger.error("couldn't find config file " + new File(configFileFullPath));
    } else {
      try {
        props = new Properties();
        props.load(new FileInputStream(configFileFullPath));
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  private class DirAndName {
    private String testAudioFile;
    private String installPath;
    private String testAudioName;
    private String testAudioDir;

    public DirAndName(String testAudioFile, String installPath) {
      this.testAudioFile = testAudioFile;
      this.installPath = installPath;
    }

    public String getName() {
      return testAudioName;
    }

    public String getDir() {
      return testAudioDir;
    }

    public DirAndName invoke() {
      File testAudio = new File(testAudioFile);
      testAudioName = testAudio.getName();
      testAudioDir = testAudio.getParent().substring(installPath.length());
      return this;
    }
  }
}
