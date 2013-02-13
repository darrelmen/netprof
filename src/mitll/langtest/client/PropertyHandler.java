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
  private static final String SEGMENT_REPEATS = "segmentRepeats";
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String RELEASE_DATE = "releaseDate";
  private static final String BKG_COLOR_FOR_REF1 = "bkgColorForRef";
  private static final String AUTO_CRT = "autocrt";
  private static final String DEMO_MODE = "demo";
  private static final String DATA_COLLECT_MODE = "dataCollect";
  private static final String COLLECT_AUDIO= "collectAudio";
  private static final String RECORD_TIMEOUT = "recordTimeout";
  private static final String TEACHER_VIEW = "teacherView";
  private static final String ADMIN_VIEW = "adminView";
  private static final String DATA_COLLECT_ADMIN_VIEW = "dataCollectAdminView";
  private static final String MINIMAL_UI = "minimalUI";
  private static final String NAME_FOR_ITEM = "nameForItem";
  private static final String NAME_FOR_ANSWER = "nameForAnswer";
  private static final String NAME_FOR_RECORDER = "nameForRecorder";

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

  private static final String DLI_LANGUAGE_TESTING = "NetPron 2";
  private static final boolean DEFAULT_GOODWAVE_MODE = false;
  private static final boolean DEFAULT_ARABIC_TEXT_COLLECT = false;
  private static final boolean DEFAULT_SHOW_TURK_TOKEN = false;
  private static final int DEFAULT_SEGMENT_REPEATS = 2;
  private static final int DEFAULT_TIMEOUT = 45000;
  private static final String DEFAULT_EXERCISE = null;

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
  private boolean adminView = true;
  private boolean minimalUI = false;
  private String nameForItem = "Item";
  private String nameForAnswer = "Recording";
  private String nameForRecorder = "Speaker";
  private String teacherClass = "class";
  private String lesson = "lesson";
  private String releaseDate;
  private int recordTimeout = DEFAULT_TIMEOUT;
  private float screenPortion;

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
      else if (key.equals(SEGMENT_REPEATS)) segmentRepeats = Integer.parseInt(value);
      else if (key.equals(READ_FROM_FILE)) readFromFile = getBoolean(value);
      else if (key.equals(RELEASE_DATE)) releaseDate = value;
      else if (key.equals(BKG_COLOR_FOR_REF1)) bkgColorForRef = getBoolean(value);
      else if (key.equals(AUTO_CRT)) autocrt = getBoolean(value);
      else if (key.equals(DEMO_MODE)) demoMode = getBoolean(value);
      else if (key.equals(DATA_COLLECT_MODE)) dataCollectMode = getBoolean(value);
      else if (key.equals(RECORD_TIMEOUT)) recordTimeout = Integer.parseInt(value);
      else if (key.equals(COLLECT_AUDIO)) collectAudio = getBoolean(value);
      else if (key.equals(ADMIN_VIEW)) adminView = getBoolean(value);
      else if (key.equals(MINIMAL_UI)) minimalUI = getBoolean(value);
      else if (key.equals(NAME_FOR_ITEM)) nameForItem = value;
      else if (key.equals(NAME_FOR_ANSWER)) nameForAnswer = value;
      else if (key.equals(NAME_FOR_RECORDER)) nameForRecorder = value;
      else if (key.equals(TEACHER_VIEW)) teacherView = getBoolean(value);
      else if (key.equals(DATA_COLLECT_ADMIN_VIEW)) dataCollectAdminView = getBoolean(value);
    }
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
    if (repeats != null) {
      try {
        segmentRepeats = Integer.parseInt(repeats)-1;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
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
}
