package mitll.langtest.client;

import com.google.gwt.user.client.Window;
import mitll.langtest.client.list.ResponseChoice;

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
  private static final String APP_TITLE = "appTitle";
  private static final String SPLASH_TITLE = "splashTitle";
  private static final String RELEASE_DATE = "releaseDate";
  private static final String BKG_COLOR_FOR_REF1 = "bkgColorForRef";
  private static final String DEMO_MODE = "demo";
  private static final String RECORD_TIMEOUT = "recordTimeout";
  private static final String ADMIN_VIEW = "adminView";
  private static final String NAME_FOR_ITEM = "nameForItem";
  private static final String NAME_FOR_ANSWER = "nameForAnswer";
  private static final String NAME_FOR_RECORDER = "nameForRecorder";
  private static final String NUM_GRADES_TO_COLLECT = "numGradesToCollect";
  private static final String LOG_CLIENT_MESSAGES = "logClient";
  private static final String LANGUAGE = "language";
  private static final String RIGHT_ALIGN_CONTENT = "rightAlignContent";
  private static final String SCREEN_PORTION = "screenPortion";

  // URL parameters that can override above parameters
  private static final String GRADING = GRADING_PROP;
 // private static final String TURK = "turk";
  private static final String BKG_COLOR_FOR_REF = "bkgColorForRef";
  private static final String EXERCISE_TITLE = "exercise_title";
  private static final String ADMIN_PARAM = "admin";
  private static final String TURK_PARAM = "turk";
  private static final String NUM_GRADES_TO_COLLECT_PARAM = NUM_GRADES_TO_COLLECT;

  private static final String DLI_LANGUAGE_TESTING = "NetProF";
  private static final int DEFAULT_TIMEOUT = 45000;
  private static final String DEFAULT_EXERCISE = null;
  private static final int NUM_GRADES_TO_COLLECT_DEFAULT = 1;
  private static final String LOGIN_TYPE_PARAM = "loginType";
  private static final String SHOW_FLASHCARD_ANSWER = "showFlashcardAnswer";
  private static final String FLASHCARD_TEXT_RESPONSE = "flashcardTextResponse";
  private static final String ALLOW_PLUS_IN_URL = "allowPlusInURL";
  private static final String SHOW_SPECTROGRAM = "spectrogram";
  private static final String RP = "rp";
  private static final String CD = "cd";
  private static final String ER = "er";
  private static final String CLICK_AND_HOLD = "clickAndHold";
  private static final String SHOW_CONTEXT = "showContext";
  public static final String QUIET_AUDIO_OK = "quietAudioOK";
  private boolean spectrogram = false;
  private static final String NO_MODEL = "noModel";
  // private static final String INSTRUMENT = "instrument";
  private boolean clickAndHold = true;
  private boolean quietAudioOK;
  private String resetPassToken = "";
  private String cdEnableToken = "", emailRToken = "";

  private boolean showContext = false;

  public boolean doClickAndHold() { return clickAndHold; }

  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }
/*

  public void setQuietAudioOK(boolean quietAudioOK) {
    this.quietAudioOK = quietAudioOK;
  }
*/

  public boolean showContextButton() {

    return showContext;
  }

  public enum LOGIN_TYPE { ANONYMOUS, STUDENT }

  private final Map<String, String> props;

  private boolean grading = false;
  private boolean bkgColorForRef = false;
  private String exercise_title;
  private String appTitle = DLI_LANGUAGE_TESTING;
  private boolean demoMode;
  private boolean adminView = false;
  private boolean logClientMessages = false;
  private int numGradesToCollect = NUM_GRADES_TO_COLLECT_DEFAULT;
  private String nameForItem = "Item";
  private String nameForAnswer = "Recording";
  private String nameForRecorder = "Speaker";
  private String language = "";
  private String releaseDate;
  private String turkID = "";

  private int recordTimeout = DEFAULT_TIMEOUT;

  private String splashTitle;
  private float screenPortion = 1.0f;
  private boolean rightAlignContent;

  // do we bind the record key to space -- problematic if we have text entry anywhere else on the page, say in a search
  // box
  private LOGIN_TYPE loginType = LOGIN_TYPE.ANONYMOUS;

  private boolean showFlashcardAnswer = true;
  private boolean allowPlusInURL;
  private boolean noModel = false;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad()
   * @param props
   */
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

      else if (key.equals(APP_TITLE)) appTitle = value;
      else if (key.equals(RELEASE_DATE)) releaseDate = value;
      else if (key.equals(BKG_COLOR_FOR_REF1)) bkgColorForRef = getBoolean(value);
      else if (key.equals(DEMO_MODE)) demoMode = getBoolean(value);
      else if (key.equals(RECORD_TIMEOUT)) recordTimeout = getInt(value, DEFAULT_TIMEOUT, RECORD_TIMEOUT);
      else if (key.equals(ADMIN_VIEW)) adminView = getBoolean(value);
      else if (key.equals(NAME_FOR_ITEM)) nameForItem = value;
      else if (key.equals(NAME_FOR_ANSWER)) nameForAnswer = value;
      else if (key.equals(NAME_FOR_RECORDER)) nameForRecorder = value;
      else if (key.equals(NUM_GRADES_TO_COLLECT)) numGradesToCollect = getInt(value, NUM_GRADES_TO_COLLECT_DEFAULT, NUM_GRADES_TO_COLLECT);
      else if (key.equals(LOG_CLIENT_MESSAGES)) logClientMessages = getBoolean(value);
      else if (key.equals(LANGUAGE)) language = value;
      else if (key.equals(SPLASH_TITLE)) splashTitle = value;
      else if (key.equals(RIGHT_ALIGN_CONTENT)) rightAlignContent = getBoolean(value);
      else if (key.equals(SHOW_FLASHCARD_ANSWER)) showFlashcardAnswer = getBoolean(value);
      else if (key.equals(ALLOW_PLUS_IN_URL)) allowPlusInURL = getBoolean(value);
      else if (key.equals(SHOW_SPECTROGRAM)) spectrogram = getBoolean(value);
   //   else if (key.equals(INSTRUMENT)) instrument = getBoolean(value);
      else if (key.equals(NO_MODEL)) noModel = getBoolean(value);
      else if (key.equals(QUIET_AUDIO_OK)) quietAudioOK = getBoolean(value);
      else if (key.equals(SHOW_CONTEXT)) showContext = getBoolean(value);
      else if (key.equals(LOGIN_TYPE_PARAM)) {
        try {
          loginType = LOGIN_TYPE.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
          System.err.println("unknown value for " + key + " : " + value);
        }
      }
    }
  }

  private int getInt(String value, int defValue, String propName) {
    try {
      if (value == null) return defValue;
      int i = Integer.parseInt(value);
      if (i != defValue) {
        System.out.println("getInt : value for " + propName +"=" +i + " vs default = " +defValue);
      }
      return i;
    } catch (NumberFormatException e) {
      System.err.println("couldn't parse " + value + "using " + defValue +" for " + propName);
    }
    return defValue;
  }

/*
  private float getFloat(String value, float defValue, String propName) {
    try {
      if (value == null) return defValue;
      float i = Float.parseFloat(value);
      //System.out.println("value for " + propName +"=" +i + " vs default = " +defValue);
      return i;
    } catch (NumberFormatException e) {
      System.err.println("couldn't parse " + value + "using " + defValue +" for " + propName);
    }
    return defValue;
  }
*/

  private boolean getBoolean(String value) {
    return Boolean.parseBoolean(value);
  }

  /**
   * Override config.properties settings with URL parameters, if provided.
   * <br></br>
   * exercise_title=nl0002_lms&transform_score_c1=68.51101&transform_score_c2=2.67174
   *
   * @see #PropertyHandler(java.util.Map)
   * @return
   */
  private boolean checkParams() {
    String isGrading = Window.Location.getParameter(GRADING);
    String numGrades = Window.Location.getParameter(NUM_GRADES_TO_COLLECT_PARAM);
    String bkgColorForRefParam = Window.Location.getParameter(BKG_COLOR_FOR_REF);
    String demoParam = Window.Location.getParameter(DEMO_MODE);

    // supports headstart mode
    String exercise_title = Window.Location.getParameter(EXERCISE_TITLE);
    if (exercise_title != null) {
      this.exercise_title = exercise_title;
    }
    else {
      this.exercise_title = DEFAULT_EXERCISE;
    }

    String screenPortionParam =  Window.Location.getParameter("screenPortion");
    if (screenPortionParam != null) {
      try {
        screenPortion = Float.parseFloat(screenPortionParam);
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    boolean grading = this.isGrading() || (isGrading != null && !isGrading.equals("false"));
    setGrading(grading);
    // get audio repeats
    numGradesToCollect = getInt(numGrades, numGradesToCollect, NUM_GRADES_TO_COLLECT);
    if (bkgColorForRefParam != null) {
      bkgColorForRef = !bkgColorForRefParam.equals("false");
    }
    if (demoParam != null) {
      demoMode = !demoParam.equals("false");
    }

    String adminParam = Window.Location.getParameter(ADMIN_PARAM);
    if (adminParam != null) {
      adminView = !adminParam.equals("false");
    }

    String turkParam = Window.Location.getParameter(TURK_PARAM);
    if (turkParam != null) {
      turkID = turkParam;
    }

    String resetPasswordID = Window.Location.getParameter(RP);
    if (resetPasswordID != null) { resetPassToken = resetPasswordID; }

    String cdEnable = Window.Location.getParameter(CD);
    if (cdEnable != null) { cdEnableToken = cdEnable; }

    String emailR = Window.Location.getParameter(ER);
    if (emailR != null) { emailRToken = emailR; }

    if (Window.Location.getParameter(CLICK_AND_HOLD) != null) {
      clickAndHold = !Window.Location.getParameter(CLICK_AND_HOLD).equals("false");
    }

    if (Window.Location.getParameter(SHOW_SPECTROGRAM) != null) {
      spectrogram = !Window.Location.getParameter(SHOW_SPECTROGRAM).equals("false");
      if (spectrogram) System.out.println("spectrogram is " + spectrogram);
    }

    String loginType = Window.Location.getParameter(LOGIN_TYPE_PARAM);
    if (loginType != null) {
      try {
        this.loginType = LOGIN_TYPE.valueOf(loginType);
        System.out.println("log in type is " + loginType);
      } catch (IllegalArgumentException e) {
        System.err.println("couldn't parse " +loginType );
      }
    }

    return grading;
  }

  /**
   * @see mitll.langtest.client.LangTest#modeSelect()
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @seex mitll.langtest.client.LangTest#setFactory
   *
   * @return
   */
  public boolean isGrading() {
    return grading;
  }

  private void setGrading(boolean v) { this.grading = v; }

  public boolean isBkgColorForRef() {
    return bkgColorForRef;
  }

  public String getExercise_title() {
    return exercise_title;
  }

  public String getAppTitle() {
    return appTitle;
  }

  public boolean isDemoMode() {
    return demoMode;
  }

  public boolean isAdminView() {
    return adminView;
  }

  public String getTurkID() { return  turkID; }

  public String getReleaseDate() {
    return releaseDate;
  }

  public int getRecordTimeout() {  return recordTimeout;  }

  public float getScreenPortion() {
    return screenPortion;
  }

  public String getNameForItem() { return nameForItem; }
  public String getNameForAnswer() { return nameForAnswer; }
  public String getNameForRecorder() { return nameForRecorder; }

  public boolean isLogClientMessages() {
    return logClientMessages;
  }

  public String getLanguage() {
    return language;
  }

  public String getSplash() {
    return splashTitle;
  }

  public boolean isRightAlignContent() {
    return rightAlignContent;
  }

  public LOGIN_TYPE getLoginType() { return loginType; }

  public boolean showFlashcardAnswer() {
    return showFlashcardAnswer;
  }

  public boolean shouldAllowPlusInURL() { return allowPlusInURL;  }

  public boolean showSpectrogram() { return spectrogram; }

  public boolean isNoModel() {
    return noModel;
  }

  /**
   * @see LangTest#showLogin()
   * @return
   */
  public String getResetPassToken() {
    return resetPassToken;
  }

  public String getCdEnableToken() {
    return cdEnableToken;
  }
  public String getEmailRToken() {
    return emailRToken;
  }
}
