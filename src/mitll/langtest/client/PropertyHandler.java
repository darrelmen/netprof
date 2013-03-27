package mitll.langtest.client;

import com.google.gwt.user.client.Window;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyHandler {
  // property file property names
  private static final String GRADING_PROP = "grading";
  private static final String ENGLISH_ONLY_MODE = "englishOnlyMode";
  private static final String GOODWAVE_MODE = "goodwaveMode";
  private static final String ARABIC_TEXT_DATA_COLLECT = "arabicTextDataCollect";
  private static final String SHOW_TURK_TOKEN = "showTurkToken";
  private static final String APP_TITLE = "appTitle";
  private static final String SPLASH_TITLE = "splashTitle";
  private static final String SEGMENT_REPEATS = "segmentRepeats";
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String RELEASE_DATE = "releaseDate";
  private static final String BKG_COLOR_FOR_REF1 = "bkgColorForRef";
  private static final String AUTO_CRT = "autocrt";
  private static final String DEMO_MODE = "demo";
  private static final String DATA_COLLECT_MODE = "dataCollect";
  private static final String CRT_DATA_COLLECT_MODE = "crtDataCollect";
  private static final String COLLECT_AUDIO= "collectAudio";
  private static final String RECORD_TIMEOUT = "recordTimeout";
  private static final String TEACHER_VIEW = "teacherView";
  private static final String ADMIN_VIEW = "adminView";
  private static final String DATA_COLLECT_ADMIN_VIEW = "dataCollectAdminView";
  private static final String MINIMAL_UI = "minimalUI";
  private static final String NAME_FOR_ITEM = "nameForItem";
  private static final String NAME_FOR_ANSWER = "nameForAnswer";
  private static final String NAME_FOR_RECORDER = "nameForRecorder";
  private static final String NUM_GRADES_TO_COLLECT = "numGradesToCollect";
  private static final String LOG_CLIENT_MESSAGES = "logClient";
  private static final String SHOW_SECTIONS = "showSections";
  //private static final String DEBUG_EMAIL = "debugEmail";
  private static final String FLASHCARD = "flashcard";
  private static final String TIMED_GAME = "timedGame";
  private static final String LANGUAGE = "language";

  // URL parameters that can override above parameters
  private static final String GRADING = GRADING_PROP;
  private static final String ENGLISH = "english";
  private static final String GOODWAVE = "goodwave";
  private static final String ARABIC_COLLECT = "arabicCollect";
  private static final String TURK = "turk";
  private static final String REPEATS = "repeats";
  private static final String BKG_COLOR_FOR_REF = "bkgColorForRef";
  private static final String EXERCISE_TITLE = "exercise_title";
  private static final String TEACHER_CLASS = "class";
  private static final String LESSON = "lesson";
  private static final String TEACHER_PARAM = TEACHER_VIEW;
  private static final String ADMIN_PARAM = "admin";
  private static final String NUM_GRADES_TO_COLLECT_PARAM = NUM_GRADES_TO_COLLECT;

  private static final String DLI_LANGUAGE_TESTING = "NetProF";
  private static final boolean DEFAULT_GOODWAVE_MODE = false;
  private static final boolean DEFAULT_ARABIC_TEXT_COLLECT = false;
  private static final boolean DEFAULT_SHOW_TURK_TOKEN = false;
  private static final int DEFAULT_SEGMENT_REPEATS = 1;
  private static final int DEFAULT_TIMEOUT = 45000;
  private static final String DEFAULT_EXERCISE = null;
  private static final int NUM_GRADES_TO_COLLECT_DEFAULT = 1;

  private Map<String, String> props;

  private boolean grading = false;
  private boolean englishOnlyMode = false;
  private boolean goodwaveMode = DEFAULT_GOODWAVE_MODE;
  private boolean arabicTextDataCollect = DEFAULT_ARABIC_TEXT_COLLECT;
  private boolean showTurkToken = DEFAULT_SHOW_TURK_TOKEN;
  private int segmentRepeats = DEFAULT_SEGMENT_REPEATS;
  private boolean bkgColorForRef = false;
  private String exercise_title;
  private String appTitle = DLI_LANGUAGE_TESTING;
  private boolean readFromFile;
  private boolean autocrt;
  private boolean demoMode;
  private boolean dataCollectMode;
  private boolean collectAudio = true;
  private boolean teacherView = false;
  private boolean dataCollectAdminView = false;
  private boolean adminView = false;
  private boolean logClientMessages = false;
  private boolean minimalUI = false;
  private int numGradesToCollect = NUM_GRADES_TO_COLLECT_DEFAULT;
  private String nameForItem = "Item";
  private String nameForAnswer = "Recording";
  private String nameForRecorder = "Speaker";
  private String teacherClass = "class";
  private String lesson = "lesson";
  private String language = "";
  private boolean showSections = false;
  //private boolean debugEmail = true;
  private boolean flashCard = false;
  private boolean timedGame = false;
  private String releaseDate;
  private int recordTimeout = DEFAULT_TIMEOUT;
  private float screenPortion;
  private boolean CRTDataCollectMode;
  private String splashTitle;

  public PropertyHandler(Map<String,String> props) {
    this.props = props;
    useProps();
    checkParams();
  }

  private void useProps() {
    for (Map.Entry<String, String> kv : props.entrySet()) {
      String key = kv.getKey();
      String value = kv.getValue();
      if (key.equals(GRADING_PROP)) grading = getBoolean(value);
      else if (key.equals(ENGLISH_ONLY_MODE)) englishOnlyMode = getBoolean(value);
      else if (key.equals(GOODWAVE_MODE)) goodwaveMode = getBoolean(value);
      else if (key.equals(ARABIC_TEXT_DATA_COLLECT)) arabicTextDataCollect = getBoolean(value);
      else if (key.equals(SHOW_TURK_TOKEN)) showTurkToken = getBoolean(value);
      else if (key.equals(APP_TITLE)) appTitle = value;
      else if (key.equals(SEGMENT_REPEATS)) segmentRepeats = getInt(value,DEFAULT_SEGMENT_REPEATS,SEGMENT_REPEATS)-1;
      else if (key.equals(READ_FROM_FILE)) readFromFile = getBoolean(value);
      else if (key.equals(RELEASE_DATE)) releaseDate = value;
      else if (key.equals(BKG_COLOR_FOR_REF1)) bkgColorForRef = getBoolean(value);
      else if (key.equals(AUTO_CRT)) autocrt = getBoolean(value);
      else if (key.equals(DEMO_MODE)) demoMode = getBoolean(value);
      else if (key.equals(DATA_COLLECT_MODE)) dataCollectMode = getBoolean(value);
      else if (key.equals(CRT_DATA_COLLECT_MODE)) CRTDataCollectMode = getBoolean(value);
      else if (key.equals(RECORD_TIMEOUT)) recordTimeout = getInt(value,DEFAULT_TIMEOUT,RECORD_TIMEOUT);
      else if (key.equals(COLLECT_AUDIO)) collectAudio = getBoolean(value);
      else if (key.equals(ADMIN_VIEW)) adminView = getBoolean(value);
      else if (key.equals(MINIMAL_UI)) minimalUI = getBoolean(value);
      else if (key.equals(NAME_FOR_ITEM)) nameForItem = value;
      else if (key.equals(NAME_FOR_ANSWER)) nameForAnswer = value;
      else if (key.equals(NAME_FOR_RECORDER)) nameForRecorder = value;
      else if (key.equals(TEACHER_VIEW)) teacherView = getBoolean(value);
      else if (key.equals(DATA_COLLECT_ADMIN_VIEW)) dataCollectAdminView = getBoolean(value);
      else if (key.equals(NUM_GRADES_TO_COLLECT)) numGradesToCollect = getInt(value,NUM_GRADES_TO_COLLECT_DEFAULT,NUM_GRADES_TO_COLLECT);
      else if (key.equals(LOG_CLIENT_MESSAGES)) logClientMessages = getBoolean(value);
      else if (key.equals(SHOW_SECTIONS)) showSections = getBoolean(value);
    //  else if (key.equals(DEBUG_EMAIL)) debugEmail = getBoolean(value);
      else if (key.equals(FLASHCARD)) flashCard = getBoolean(value);
      else if (key.equals(LANGUAGE)) language = value;
      else if (key.equals(SPLASH_TITLE)) splashTitle = value;
      else if (key.equals(TIMED_GAME)) timedGame = getBoolean(value);
    }
  }

  private int getInt(String value, int defValue, String propName) {
    try {
      if (value == null) return defValue;
      int i = Integer.parseInt(value);
      System.out.println("value for " + propName +"=" +i + " vs default = " +defValue);
      return i;
    } catch (NumberFormatException e) {
      System.err.println("couldn't parse " + value + "using " + defValue +" for " + propName);
    }
    return defValue;
  }

  private boolean getBoolean(String value) {
    return Boolean.parseBoolean(value);
  }

  /**
   * Override config.properties settings with URL parameters, if provided.
   * <br></br>
   * exercise_title=nl0002_lms&transform_score_c1=68.51101&transform_score_c2=2.67174
   * @return
   */
  private boolean checkParams() {
    String isGrading = Window.Location.getParameter(GRADING);
    String isEnglish = Window.Location.getParameter(ENGLISH);
    String numGrades = Window.Location.getParameter(NUM_GRADES_TO_COLLECT_PARAM);
    String goodwave = Window.Location.getParameter(GOODWAVE);
    String repeats = Window.Location.getParameter(REPEATS);
    String arabicCollect = Window.Location.getParameter(ARABIC_COLLECT);
    String turk = Window.Location.getParameter(TURK);
    String bkgColorForRefParam = Window.Location.getParameter(BKG_COLOR_FOR_REF);
    String demoParam = Window.Location.getParameter(DEMO_MODE);

    String exercise_title = Window.Location.getParameter(EXERCISE_TITLE);
    if (exercise_title != null) {
      this.exercise_title = exercise_title;
    }
    else {
      this.exercise_title = DEFAULT_EXERCISE;
    }

    String tClass = Window.Location.getParameter(TEACHER_CLASS);
    if (tClass != null) {
      this.teacherClass = tClass;
    }
    else {
     // this.exercise_title = DEFAULT_EXERCISE;
    }

    String lessonParam = Window.Location.getParameter(LESSON);
    if (lessonParam != null) {
      this.lesson = lessonParam;
    }
    else {
      // this.exercise_title = DEFAULT_EXERCISE;
    }

    String screenPortionParam =  Window.Location.getParameter("screenPortion");
    screenPortion = 1.0f;
    if (screenPortionParam != null) {
      try {
        screenPortion = Float.parseFloat(screenPortionParam);
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    // System.out.println("param grading " + isGrading);
    englishOnlyMode = isEnglishOnlyMode() || (isEnglish != null && !isEnglish.equals("false"));
    goodwaveMode    = isGoodwaveMode() || (goodwave != null && !goodwave.equals("false"));
    if (goodwave != null && goodwave.equals("false")) goodwaveMode = false;
    //GWT.log("goodwave mode = " + goodwaveMode + "/" +goodwave);
    boolean grading = this.isGrading() || (isGrading != null && !isGrading.equals("false")) || isEnglishOnlyMode();
    setGrading(grading);
    // get audio repeats
    segmentRepeats = getInt(repeats, DEFAULT_SEGMENT_REPEATS,REPEATS) -1;
    numGradesToCollect = getInt(numGrades, NUM_GRADES_TO_COLLECT_DEFAULT, NUM_GRADES_TO_COLLECT);
    if (numGrades != null)
    if (arabicCollect != null) {
      arabicTextDataCollect = !arabicCollect.equals("false");
    }
    if (turk != null) {
      showTurkToken = !turk.equals("false");
    }
    if (bkgColorForRefParam != null) {
      bkgColorForRef = !bkgColorForRefParam.equals("false");
    }
    if (demoParam != null) {
      demoMode = !demoParam.equals("false");
    }
    if (Window.Location.getParameter(DATA_COLLECT_MODE) != null) {
      dataCollectMode = !Window.Location.getParameter(DATA_COLLECT_MODE).equals("false");
    }
    String adminParam = Window.Location.getParameter(ADMIN_PARAM);
    if (adminParam != null) {
      adminView = !adminParam.equals("false");
    }
    return grading;
  }

  public boolean isArabicTextDataCollect() {
    return arabicTextDataCollect;
  }

  /**
   * @see mitll.langtest.client.LangTest#modeSelect()
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @see mitll.langtest.client.LangTest#setFactory()
   *
   * @return
   */
  public boolean isGrading() {
    return grading;
  }

  public void setGrading(boolean v) { this.grading = v; }

  public boolean isEnglishOnlyMode() {
    return englishOnlyMode;
  }

  public boolean isGoodwaveMode() {
    return goodwaveMode;
  }

  public boolean isShowTurkToken() {
    return showTurkToken;
  }

  public int getSegmentRepeats() {
    return segmentRepeats;
  }

  public boolean isBkgColorForRef() {
    return bkgColorForRef;
  }

  public String getExercise_title() {
    return exercise_title;
  }

  public String getAppTitle() {
    return appTitle;
  }

  public boolean isReadFromFile() {
    return readFromFile;
  }

  public boolean isAutocrt() {
    return autocrt;
  }

  public boolean isDemoMode() {
    return demoMode;
  }

  public boolean isDataCollectMode() {
    return dataCollectMode;
  }

  public boolean isCollectAudio() {
    return collectAudio;
  }

  public boolean isTeacherView() {
    return teacherView;
  }

  public boolean isAdminView() {
    return adminView;
  }

  public boolean isMinimalUI() {
    return minimalUI;
  }

  public String getReleaseDate() {
    return releaseDate;
  }

  public int getRecordTimeout() {
    return recordTimeout;
  }

  public float getScreenPortion() {
    return screenPortion;
  }

  public String getNameForItem() { return nameForItem; }
  public String getNameForAnswer() { return nameForAnswer; }
  public String getNameForRecorder() { return nameForRecorder; }

  public String getTeacherClass() { return teacherClass; }
  public String getLesson() { return lesson; }

  public boolean isDataCollectAdminView() {
    return dataCollectAdminView;
  }

  public int getNumGradesToCollect() {
    return numGradesToCollect;
  }

  public boolean isLogClientMessages() {
    return logClientMessages;
  }

  public boolean isShowSections() {
    return showSections;
  }

  public boolean isFlashCard() {
    return flashCard;
  }

  public String getLanguage() {
    return language;
  }

  public String getSplash() {
    return splashTitle;
  }

  public boolean isCRTDataCollectMode() {
    return CRTDataCollectMode;
  }

  public boolean isTimedGame() {
    return timedGame;
  }
}
