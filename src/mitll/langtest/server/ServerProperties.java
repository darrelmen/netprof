package mitll.langtest.server;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This has a lot of overlap with the PropertyHandler set of properties.
 *
 * TODO : There should be a better way of handling their relationship.
 *
 * User: GO22670
 * Date: 3/5/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerProperties {
  private static Logger logger = Logger.getLogger(ServerProperties.class);

  private static final String DEBUG_EMAIL = "debugEmail";
  private static final String DOIMAGES = "doimages";
  private static final String USE_SCORE_CACHE = "useScoreCache";
  private static final String USE_WEIGHTS = "useWeights";

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  private static final String FIRST_N_IN_ORDER = "firstNInOrder";
  private static final String DATA_COLLECT_MODE = "dataCollect";
  private static final String COLLECT_AUDIO = "collectAudio";
  private static final String COLLECT_AUDIO_DEFAULT = "true";
  private static final String BIAS_TOWARDS_UNANSWERED = "biasTowardsUnanswered";
  private static final String USE_OUTSIDE_RESULT_COUNTS = "useOutsideResultCounts";
  private static final String OUTSIDE_FILE = "outsideFile";
  private static final String OUTSIDE_FILE_DEFAULT = "distributions.txt";
  private static final String H2_DATABASE = "h2Database";
  private static final String H2_DATABASE_DEFAULT = "vlr-parle";
  private static final String H2_STUDENT_ANSWERS_DATABASE = "h2StudentAnswers";
  private static final String H2_STUDENT_ANSWERS_DATABASE_DEFAULT = "h2StudentAnswers";
  private static final String URDU = "urdu";
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String FLASHCARD = "flashcard";
  private static final String CRTDATACOLLECT = "crtDataCollect";
  private static final String LANGUAGE = "language";
  private static final String WORDPAIRS = "wordPairs";
  private static final String AUTOCRT = "autocrt";
  private static final String MEDIA_DIR = "mediaDir";
  private static final String RECO_TEST = "recoTest";
  private static final String RECO_TEST2 = "recoTest2";
  private static final String ARABIC_TEXT_DATA_COLLECT = "arabicTextDataCollect";
  private static final String COLLECT_ONLY_AUDIO = "collectAudioOnly";
  private static final String TIMED_GAME = "timedGame";
  private static final String MIN_PRON_SCORE = "minPronScore";
  private static final String MIN_PRON_SCORE_DEFAULT = "0.20";
  private static final String GOODWAVE_MODE = "goodwaveMode";
  private static final String FLASHCARD_TEACHER_VIEW = "flashcardTeacherView";
  private static final String USE_PREDEFINED_TYPE_ORDER = "usePredefinedTypeOrder";
  private static final String SORT_BY_ID = "sortByID";
  private static final String SKIP_SEMICOLONS = "skipSemicolons";
  private static final String SORT_EXERCISES = "sortExercises";
  private static final String EMAIL_ADDRESS = "emailAddress";
  private static final String DEFAULT_EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final String AUDIO_OFFSET = "audioOffset";
  private static final String COLLECT_SYNONYMS = "collectSynonyms";
  //private static final String EXERCISES_IN_ORDER = "exercisesInOrder";
  private static final String FOREIGN_LANGUAGE_QUESTIONS_ONLY = "foreignLanguageQuestionsOnly";
  private static final String MAX_NUM_EXERCISES = "maxNumExercises";

  private Properties props = new Properties();

  public boolean dataCollectMode;
  private boolean collectAudio;
  public boolean biasTowardsUnanswered, useOutsideResultCounts;
  public String outsideFile;
  public boolean isUrdu;
  public int firstNInOrder;
  public boolean isDataCollectAdminView;
  private double minPronScore;
  int maxNumExercises = Integer.MAX_VALUE;

  public void readPropertiesFile(ServletContext servletContext, String configDir) {
   String configFile = servletContext.getInitParameter("configFile");
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    String configFileFullPath = configDir + File.separator + configFile;
    if (!new File(configFileFullPath).exists()) {
      logger.error("couldn't find config file " + new File(configFileFullPath));
    } else {
      try {
        props = new Properties();
        props.load(new FileInputStream(configFileFullPath));
        readProperties(servletContext);
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  /**
   * @see LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   * @return
   */
  public String getH2Database() { return props.getProperty(H2_DATABASE, H2_DATABASE_DEFAULT); }
  public String getH2StudentAnswersDatabase() { return props.getProperty(H2_STUDENT_ANSWERS_DATABASE, H2_STUDENT_ANSWERS_DATABASE_DEFAULT); }
  public String getLessonPlan() { return props.getProperty("lessonPlanFile", "lesson.plan"); }

  public boolean getUseFile() {
    return getDefaultFalse(READ_FROM_FILE);
  }

  public boolean doRecoTest() {
    return getDefaultFalse(RECO_TEST);
  }

  public boolean doRecoTest2() {
    return getDefaultFalse(RECO_TEST2);
  }

  public boolean useScoreCache() {
    return getDefaultTrue(USE_SCORE_CACHE);
  }

  public boolean isDebugEMail() {
    return getDefaultFalse(DEBUG_EMAIL);
  }
  public boolean isFlashcard() {
    return getDefaultFalse(FLASHCARD);
  }

  public boolean isCRTDataCollect() {
    return getDefaultFalse(CRTDATACOLLECT);
  }

  public boolean isWordPairs() {
    return getDefaultFalse(WORDPAIRS);
  }

  public boolean doImages() {
    return getDefaultFalse(DOIMAGES);
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   * @return
   */
  public boolean isAutoCRT() {
    return getDefaultFalse(AUTOCRT);
  }

  public String getLanguage() {
    return props.getProperty(LANGUAGE, "English");
  }

  /**
   * @see LangTestDatabaseImpl#setInstallPath
   * @return
   */
  public String getMediaDir() {
    return props.getProperty(MEDIA_DIR, "media");
  }

  public double getMinPronScore() {
    return minPronScore;
  }
  public boolean isArabicTextDataCollect() {
    return !props.getProperty(ARABIC_TEXT_DATA_COLLECT, "false").equals("false");
  }

  public boolean isCollectOnlyAudio() {
    return !props.getProperty(COLLECT_ONLY_AUDIO, "false").equals("false");
  }

  public boolean isTimedGame() {
    return !props.getProperty(TIMED_GAME, "false").equals("false");
  }

  public boolean isGoodwaveMode() {
    return !props.getProperty(GOODWAVE_MODE, "false").equals("false");
  }

  public boolean isFlashcardTeacherView() {
    return !props.getProperty(FLASHCARD_TEACHER_VIEW, "false").equals("false");
  }

  public boolean usePredefinedTypeOrder() {
    return !props.getProperty(USE_PREDEFINED_TYPE_ORDER, "false").equals("false");
  }

  public boolean shouldUseWeights() {
    return !props.getProperty(USE_WEIGHTS, "false").equals("false");
  }

  public boolean sortExercisesByID() {
    return getDefaultFalse(SORT_BY_ID);
  }

  public boolean isCollectAudio() {
    return collectAudio;
  }

  public boolean shouldSkipSemicolonEntries() {
    return getDefaultTrue(SKIP_SEMICOLONS);
  }

  public boolean sortExercises() {
    return getDefaultFalse(SORT_EXERCISES);
  }

  public boolean showForeignLanguageQuestionsOnly() {
    return getDefaultFalse(FOREIGN_LANGUAGE_QUESTIONS_ONLY);
  }
  
  public boolean getCollectSynonyms() {
    return props.getProperty(COLLECT_SYNONYMS, "true").equals("true");
  }

  public String getEmailAddress() {
    return props.getProperty(EMAIL_ADDRESS, DEFAULT_EMAIL);
  }

  public int getAudioOffset() {
    try {
      return Integer.parseInt(props.getProperty(AUDIO_OFFSET));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public int getMaxNumExercises() {
    try {
      String property = props.getProperty(MAX_NUM_EXERCISES);
      if (property == null) return maxNumExercises;
      return Integer.parseInt(property);
    } catch (NumberFormatException e) {
      return maxNumExercises;
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
    //logger.debug("for config " + relativeConfigDir + " prop file has " + kv.size() + " properties : " + props.keySet());
    return kv;
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   *
   * Note that this will only ever be called once.
   * @see
   * @param servletContext
   */
  private void readProperties(ServletContext servletContext) {
    try {
      firstNInOrder = Integer.parseInt(props.getProperty(FIRST_N_IN_ORDER, "" + Integer.MAX_VALUE));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + FIRST_N_IN_ORDER,e);
      firstNInOrder = Integer.MAX_VALUE;
    }
    dataCollectMode = getDefaultFalse(DATA_COLLECT_MODE);
    collectAudio = !props.getProperty(COLLECT_AUDIO, COLLECT_AUDIO_DEFAULT).equals("false");
    isUrdu = !props.getProperty(URDU, "false").equals("false");
    biasTowardsUnanswered = !props.getProperty(BIAS_TOWARDS_UNANSWERED, "true").equals("false");
    useOutsideResultCounts = !props.getProperty(USE_OUTSIDE_RESULT_COUNTS, "false").equals("false");
    isDataCollectAdminView = !props.getProperty("dataCollectAdminView", "false").equals("false");
    outsideFile = props.getProperty(OUTSIDE_FILE, OUTSIDE_FILE_DEFAULT);

    String dateFromManifest = getDateFromManifest(servletContext);
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      //logger.debug("Date from manifest " + dateFromManifest);
      props.setProperty("releaseDate",dateFromManifest);
    }

    try {
      minPronScore = Double.parseDouble(props.getProperty(MIN_PRON_SCORE, MIN_PRON_SCORE_DEFAULT));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + MIN_PRON_SCORE, e);
      try {
        minPronScore = Double.parseDouble(MIN_PRON_SCORE_DEFAULT);
      } catch (NumberFormatException e1) {
        e1.printStackTrace();
      }
    }

  }

  private boolean getDefaultFalse(String param) {
    return !props.getProperty(param, "false").equals("false");
  }

  private boolean getDefaultTrue(String param) {
    return !props.getProperty(param, "true").equals("false");
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
}
