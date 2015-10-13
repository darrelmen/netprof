package mitll.langtest.client;

import com.google.gwt.user.client.Window;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyHandler {
  private Logger logger = Logger.getLogger("PropertyHandler");

  // property file property names
  private static final String ENABLE_ALL_USERS = "enableAllUsers";
  private static final String GRADING_PROP = "grading";
  private static final String APP_TITLE = "appTitle";
  private static final String SPLASH_TITLE = "splashTitle";
  private static final String RELEASE_DATE = "releaseDate";
  private static final String BKG_COLOR_FOR_REF1 = "bkgColorForRef";
  private static final String DEMO_MODE = "demo";
  private static final String RECORD_TIMEOUT = "recordTimeout";

  private static final String NAME_FOR_ITEM = "nameForItem";
  private static final String NAME_FOR_ANSWER = "nameForAnswer";
  private static final String NAME_FOR_RECORDER = "nameForRecorder";
  private static final String NUM_GRADES_TO_COLLECT = "numGradesToCollect";
  private static final String LOG_CLIENT_MESSAGES = "logClient";
  private static final String DIALOG = "dialog";

  private static final String LANGUAGE = "language";
  private static final String RIGHT_ALIGN_CONTENT = "rightAlignContent";

  // URL parameters that can override above parameters
  private static final String GRADING = GRADING_PROP;
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
  private static final String ALLOW_PLUS_IN_URL = "allowPlusInURL";
  private static final String SHOW_SPECTROGRAM = "spectrogram";
  private static final String SHOW_SPECTROGRAM2 = "showSpectrogram";
  private static final String USE_PHONE_TO_DISPLAY = "usePhoneToDisplay";

  /**
   * @see mitll.langtest.server.mail.EmailHelper#RP
   */
  private static final String RP = "rp";
  private static final String CD = "cd";
  private static final String ER = "er";

  private static final String CLICK_AND_HOLD = "clickAndHold";
  private static final String SHOW_CONTEXT = "showContext";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";
  private static final String SHOW_WELCOME = "showWelcome";
  private static final String NO_MODEL = "noModel";
  private static final String PREFERRED_VOICES = "preferredVoices";

//  public static final String SIGN_UP = "Sign Up";
  private boolean adminView;
  private boolean enableAllUsers;
  private boolean usePhoneToDisplay;

  /**
   * @see mitll.langtest.client.recorder.RecordButton#showTooLoud
   * @return
   */
  public String getTooLoudMessage() {
    return "If your recording is too loud, please follow the following steps to adjust your microphone level settings in Windows on your MacBook: <br/>" +
        "1.\tClick on ‘Control Panel’<br/>" +
        "2.\tSelect ‘Sound’<br/>" +
        "3.\tClick on ‘Recording’<br/>" +
        "4.\tDouble-Click on the microphone symbol to open a settings window<br/>" +
        "5.\tSelect the ‘Levels’ tab<br/>" +
        "6.\tMove the ‘Microphone Boost’ slider to the lowest level<br/>" +
        "7.\tMove the ‘Microphone’ level to 90% <br/>" +
        "8.\tClick OK<br/>";
  }

  public Set<Long> getPreferredVoices() {  return preferredVoices;  }

  public boolean enableAllUsers() {  return enableAllUsers;  }

  public boolean shouldUsePhoneToDisplay() {
    return usePhoneToDisplay;
  }

  public enum LOGIN_TYPE { ANONYMOUS, STUDENT }

  private boolean spectrogram = false;
  private boolean clickAndHold = true;
  private boolean quietAudioOK;
  private boolean showContext = true;
  private Set<Long> preferredVoices = new HashSet<Long>();
  private String resetPassToken = "";
  private String cdEnableToken = "", emailRToken = "";

  private final Map<String, String> props;

  private boolean grading = false;
  private boolean bkgColorForRef = false;
  private String exercise_title;
  private String appTitle = DLI_LANGUAGE_TESTING;
  private boolean demoMode;

  private boolean showWelcome = true;// default value
  private boolean logClientMessages = false;
  private int numGradesToCollect = NUM_GRADES_TO_COLLECT_DEFAULT;
  private String nameForItem = "Item";
  private String nameForAnswer = "Recording";
  private String nameForRecorder = "Speaker";
  private String language = "";
  private String dialog = "";

  private String releaseDate;
  private String turkID = "";

  private int recordTimeout = DEFAULT_TIMEOUT;

  private String splashTitle;
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
      else if (key.equals(SHOW_WELCOME)) showWelcome = getBoolean(value);
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

      else if (key.equals(NO_MODEL)) noModel = getBoolean(value);
      else if (key.equals(DIALOG)) dialog = value;
      else if (key.equals(QUIET_AUDIO_OK)) quietAudioOK = getBoolean(value);
      else if (key.equals(SHOW_CONTEXT)) showContext = getBoolean(value);
      else if (key.equals(ENABLE_ALL_USERS)) enableAllUsers = getBoolean(value);
      else if (key.equals(USE_PHONE_TO_DISPLAY)) {
        logger.info("found " + USE_PHONE_TO_DISPLAY +  " = " + value);
        usePhoneToDisplay = getBoolean(value);
      }
      else if (key.equals(PREFERRED_VOICES)) {
        for (String userid : value.split(",")) {
          try {
            preferredVoices.add(Long.parseLong(userid));
          //  logger.info("pref users " + preferredVoices);
          } catch (NumberFormatException e) {
            logger.warning("couldn't parse userid " + userid);
          }
        }
      }
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
        logger.info("getInt : value for " + propName + "=" + i + " vs default = " + defValue);
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
      return Float.parseFloat(value);
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
  private void checkParams() {
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
      if (spectrogram) logger.info("spectrogram is " + spectrogram);
    }

    if (Window.Location.getParameter(SHOW_SPECTROGRAM2) != null) {
      spectrogram = !Window.Location.getParameter(SHOW_SPECTROGRAM2).equals("false");
      if (spectrogram) logger.info("spectrogram is " + spectrogram);
    }

    if (Window.Location.getParameter(USE_PHONE_TO_DISPLAY) != null) {
      usePhoneToDisplay = !Window.Location.getParameter(USE_PHONE_TO_DISPLAY).equals("false");
      if (usePhoneToDisplay) logger.info("usePhoneToDisplay is " + usePhoneToDisplay);
    }

    String loginType = Window.Location.getParameter(LOGIN_TYPE_PARAM);
    if (loginType != null) {
      try {
        this.loginType = LOGIN_TYPE.valueOf(loginType);
      } catch (IllegalArgumentException e) {
        System.err.println("couldn't parse " +loginType );
      }
    }

   // return grading;
  }

  /**
   * @see mitll.langtest.client.LangTest#modeSelect()
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
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

  /**
   *
   * For headstart integration.
   * @return
   */
  public String getExercise_title() {
    return exercise_title;
  }

  public String getAppTitle() {
    return appTitle;
  }

  public boolean isDemoMode() {
    return demoMode;
  }

  public boolean isAdminView() {  return adminView;  }

  public String getTurkID() { return  turkID; }

  public String getReleaseDate() {
    return releaseDate;
  }

  public int getRecordTimeout() {  return recordTimeout;  }

  public String getNameForItem() { return nameForItem; }
  public String getNameForAnswer() { return nameForAnswer; }
  public String getNameForRecorder() { return nameForRecorder; }

  public boolean isLogClientMessages() {
    return logClientMessages;
  }

  public boolean hasDialog(){
	  return !dialog.isEmpty();
  }

  public String dialogFile() {return dialog;}

  public String getLanguage() {
    return language;
  }

  public String getSplash() {
    return splashTitle;
  }

  public boolean isRightAlignContent() {
    return rightAlignContent;
  }

  public LOGIN_TYPE getLoginType() { return getExercise_title() != null ? LOGIN_TYPE.ANONYMOUS : loginType; }

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

  public boolean doClickAndHold() { return clickAndHold; }

  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  public boolean showContextButton() {
    return showContext;
  }

  public boolean shouldShowWelcome() { return showWelcome; }

  /**
   * TODO : Consider rewording for other customers...
   * @return
   */
  public String getHelpMessage() {
/*    return
        "If you are a first-time user of this site, or an existing user of an earlier version of Classroom " +
            "(either as a student, teacher, or audio recorder), you will need to use the " +
            "\"" +
            SIGN_UP +
            "\" box to add a password and an email address to your account. " +
            "Your email is only used if you ever forget your password.<br/>" +
            "<br/>" +
            "If you are a teacher assigned to record <u>course material</u> - check the box next to " +
            "‘<i>assigned reference audio recorder</i>.’" +
            " Once you have submitted this form, LTEA personnel will approve your account. " +
            "You will receive an email once it's approved. " +
            "You will not be able to access Classroom until approval is granted. " +
            "Students do not need approval to access the site.<br/>" +
            "<br/>" +
            "<b>Students and Teachers</b>: After you register, use the ’Login’ box to access the site. <br/>" +
            //"Teachers who are not <i>audio recorders</i>: After you register, use the ’Login’ box to access the site. <br/>" +
            "<b>Teachers who are <i>audio recorders</i></b>: After you register and get approval, use the ‘Login’ box to access the site. <br/><br/>" +
            "The site will remember your login information on this computer for up to one year. " +
            "You will need to login with your username and password again if you access Classroom from a different machine.<br/>";*/
  return "If you are a first-time user of this site, or an existing user of an earlier version of NetProF " +
      "(either as a student, teacher, or audio recorder), you will need to use the \"Sign Up\" box to add a" +
      " password and an email address to your account. Your email is only used if you ever forget your" +
      " password.<br/>" +
      "<br/>" +
      "If you are a teacher assigned to record <u>course material</u> - " +
      "check the box next to ‘<i>assigned reference audio recorder</i>.’" +
      " Once you have submitted this form, LTEA personnel will approve your account. " +
      "You will receive an email once it's approved. " +
      "You will not be able to access the NetProF site until approval is granted. " +
      "Students do not need approval to access the site.<br/>" +
      "<br/>" +
      "<b>Students and Teachers</b>: After you register, use the ‘Login’ box to access the site. <br/>" +
      "<b>Teachers who are <i>audio recorders</i></b>: After you register and get approval, use the ‘Login’ box to access the site. <br/>" +
      "<br/>" +
      "The site will remember your login information on this computer for up to one year. " +
      "You will need to login with your username and password again if you access NetProF from a different machine.";
  }


  /**
   * TODO : Consider rewording for other customers...
   * @return
   */
  public String getRecordAudioPopoverText() {
    return "Click here if you have been assigned to record reference audio or do quality control.<br/>" +
        "After you click sign up, " +
        "LTEA personnel will approve your account.<br/>" +
        "You will receive an email once it's approved.<br/>" +
        "You will not be able to access NetProF until approval is granted.";
  }
}
