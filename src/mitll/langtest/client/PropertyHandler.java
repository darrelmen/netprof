/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client;

import com.google.gwt.user.client.Window;
import mitll.hlt.domino.shared.Constants;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.server.rest.RestUserManagement;

import java.util.*;
import java.util.logging.Logger;


/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/13
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyHandler {
  private final Logger logger = Logger.getLogger("PropertyHandler");

  public static final String CPW_TOKEN = "CPWtoken";
  public static final String CPW_TOKEN2 = "CPW-token";

  private static final String TALKS_TO_DOMINO = "talksToDomino";
  private static final String PRACTICE_CONTEXT = "practiceContext";
  private static final String FONT_FAMILY = "fontFamily";

  private static final String RTL = "rtl";
  private static final String IS_AMAS = "isAMAS";

  /**
   * Possibly we need to add a delay after button is released to actually tell flash to stop recording.
   *
   * @see RecordButton#startOrStopRecording()
   */
  private static final int DEFAULT_AFTER_STOP_DELAY_MILLIS = 90;

  // property file property names
  //private static final String ENABLE_ALL_USERS = "enableAllUsers";
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
  // private static final String ADMIN_PARAM = "admin";
  private static final String ANALYSIS = "analysis";
  private static final String TURK_PARAM = "turk";
  private static final String NUM_GRADES_TO_COLLECT_PARAM = NUM_GRADES_TO_COLLECT;

  //  private static final String DLI_LANGUAGE_TESTING = "NetProF";
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
  private static final String RP = RestUserManagement.RESET_PASSWORD_FROM_EMAIL;//"rp";
  private static final String CD = "cd";
  private static final String ER = "er";

  private static final String CLICK_AND_HOLD = "clickAndHold";
  private static final String SHOW_CONTEXT = "showContext";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";
  //  private static final String SHOW_WELCOME = "showWelcome";
//  private static final String NO_MODEL = "noModel";
  private static final String PREFERRED_VOICES = "preferredVoices";

  private boolean analysis = false;
  private boolean canPracticeContext = false;
//  private boolean enableAllUsers;
  private boolean isAMAS;
  private boolean usePhoneToDisplay;

//  private static final String AMAS_WELCOME = "Welcome to the Automatic Multi-Skilled Assessment System (AMAS)";
  private static final String AMAS_PRONUNCIATION_FEEDBACK = "AMAS — Automatic Multi-Skilled Assessment System";

  private static final String INITIAL_PROMPT = "Practice pronunciation and learn vocabulary.";//"Learn how to pronounce words and practice vocabulary.";
  private static final String AMAS_INITIAL_PROMPT = "Test your Listening and Reading Skills.";

  private static final List<String> SITE_LIST = Arrays.asList(
      "Dari",
      "Egyptian",
      "English",
      "Farsi",
      "German",
      "Korean",
      "Iraqi",
      "Japanese",
      "Levantine",
      "Mandarin",
      "MSA",
      "Pashto1",
      "Pashto2",
      "Pashto3",
      "Russian",
      "Spanish",
      "Sudanese",
      "Tagalog",
      "Urdu");

  //  private static final List<String> AMAS_SITES = Arrays.asList("Dari", "Farsi", "Korean", "Mandarin", "MSA", "Pashto", "Russian", "Spanish", "Urdu");
  // private boolean beta;
  private String fontFamily = "";
  private String modelDir;
  private int afterStopDelayMillis;

  /**
   * @return
   * @see mitll.langtest.client.recorder.RecordButton#showTooLoud
   */
  public String getTooLoudMessage() {
    return "If your recording is too loud, please follow the following steps to adjust your microphone level settings in" +
        " Windows on your MacBook: <br/>" +
        "1.\tClick on ‘Control Panel’<br/>" +
        "2.\tSelect ‘Sound’<br/>" +
        "3.\tClick on ‘Recording’<br/>" +
        "4.\tDouble-Click on the microphone symbol to open a settings window<br/>" +
        "5.\tSelect the ‘Levels’ tab<br/>" +
        "6.\tMove the ‘Microphone Boost’ slider to the lowest level<br/>" +
        "7.\tMove the ‘Microphone’ level to 90% <br/>" +
        "8.\tClick OK<br/>";
  }

  public Set<Long> getPreferredVoices() {
    return preferredVoices;
  }

/*
  public boolean enableAllUsers() {
    return enableAllUsers;
  }
*/

  public boolean shouldUsePhoneToDisplay() {
    return usePhoneToDisplay;
  }

  public boolean isAMAS() {
    return isAMAS;
  }

  public boolean isBeta() {
    return true;
  }

  boolean talksToDomino() {
    return talksToDomino;
  }

  /**
   * Typically 50 or 100 milliseconds.
   *
   * @return
   */
  public int getAfterStopDelayMillis() {
    return afterStopDelayMillis;
  }

/*
  public void setAfterStopDelayMillis(int afterStopDelayMillis) {
    this.afterStopDelayMillis = afterStopDelayMillis;
  }
*/

  public String getFontFamily() {
    return fontFamily;
  }

  public void setFontFamily(String fontFamily) {
    this.fontFamily = fontFamily;
  }

  public String getModelDir() {
    return modelDir;
  }

//  @Deprecated  public enum LOGIN_TYPE {ANONYMOUS, STUDENT}

  private boolean spectrogram = false;
  private boolean clickAndHold = true;
  private boolean quietAudioOK;
  private boolean showContext = true;
  private final Set<Long> preferredVoices = new HashSet<>();

  /**
   * @see #checkParams
   */
  private String resetPassToken = "";
  private String cdEnableToken = "", emailRToken = "";

  private final Map<String, String> props;

  private boolean grading = false;
  private boolean bkgColorForRef = false;
  private String exercise_title;
  private boolean demoMode;

  private boolean showWelcome = false;// default value
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

  private String splashTitle = null;
  private String appTitle = null;

  private boolean rightAlignContent;
//  private LOGIN_TYPE loginType = LOGIN_TYPE.STUDENT;

  private boolean showFlashcardAnswer = true;
  private boolean allowPlusInURL;
  // private boolean noModel = false;

  private static final String RESPONSE_TYPE = "responseType";
  private static final String SPEECH = "Speech";
  public static final String TEXT = "Text";
  private static final String AUDIO = "Audio";
  private String responseType = AUDIO;
  private boolean talksToDomino = false;

  /**
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad()
   */
  public PropertyHandler(Map<String, String> props) {
    this.props = props;
    useProps();
    checkParams();
  }

  /**
   * Copy values from server props.
   */
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
        //    else if (key.equals(SHOW_WELCOME)) showWelcome = getBoolean(value);
      else if (key.equals(NAME_FOR_ITEM)) nameForItem = value;
      else if (key.equals(NAME_FOR_ANSWER)) nameForAnswer = value;
      else if (key.equals(NAME_FOR_RECORDER)) nameForRecorder = value;
      else if (key.equals(NUM_GRADES_TO_COLLECT))
        numGradesToCollect = getInt(value, NUM_GRADES_TO_COLLECT_DEFAULT, NUM_GRADES_TO_COLLECT);
      else if (key.equals(LOG_CLIENT_MESSAGES)) logClientMessages = getBoolean(value);
      else if (key.equals(LANGUAGE)) language = value;
      else if (key.equals(SPLASH_TITLE)) splashTitle = value;
      else if (key.equals(RIGHT_ALIGN_CONTENT) || key.equals(RTL)) {
        rightAlignContent = getBoolean(value);
      } else if (key.equals(SHOW_FLASHCARD_ANSWER)) showFlashcardAnswer = getBoolean(value);
      else if (key.equals(ALLOW_PLUS_IN_URL)) allowPlusInURL = getBoolean(value);
      else if (key.equals(SHOW_SPECTROGRAM)) spectrogram = getBoolean(value);

        //    else if (key.equals(NO_MODEL)) noModel = getBoolean(value);
      else if (key.equals(DIALOG)) dialog = value;
      else if (key.equals(QUIET_AUDIO_OK)) quietAudioOK = getBoolean(value);
      else if (key.equals(SHOW_CONTEXT)) showContext = getBoolean(value);
   //   else if (key.equals(ENABLE_ALL_USERS)) enableAllUsers = getBoolean(value);
      else if (key.equals(IS_AMAS)) isAMAS = getBoolean(value);
      else if (key.equals(TALKS_TO_DOMINO)) talksToDomino = getBoolean(value);
      else if (key.equals(PRACTICE_CONTEXT)) canPracticeContext = getBoolean(value);
      else if (key.equals(FONT_FAMILY)) fontFamily = value;
      else if (key.equals("scoringModel")) modelDir = value;
      else if (key.equals("afterStopDelayMillis")) {
        afterStopDelayMillis = getInt(value, DEFAULT_AFTER_STOP_DELAY_MILLIS, "afterStopDelayMillis");
      }
      //else if (key.equals(IS_AMAS)) isAMAS = getBoolean(value);
      else if (key.equals(USE_PHONE_TO_DISPLAY)) {
        // logger.info("found " + USE_PHONE_TO_DISPLAY + " = " + value);
        usePhoneToDisplay = getBoolean(value);
      } else if (key.equals(PREFERRED_VOICES)) {
        getPreferredVoices(value);
      } /*else if (key.equals(LOGIN_TYPE_PARAM)) {
        try {
          loginType = LOGIN_TYPE.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
          logger.warning("unknown value for " + key + " : " + value);
        }
      }*/
    }

    if (appTitle == null) {
      appTitle = language + getAppTitleSuffix();
    }

    if (isAMAS()) {
      appTitle = getAppTitleSuffix();
    }
    if (splashTitle == null) {
      splashTitle = "";//language + getSpashTitleSuffix();
    }
  }

/*
  private String getSpashTitleSuffix() {
    return isAMAS() ? "" : " feedback and practice";
  }
*/

  private String getAppTitleSuffix() {
    return isAMAS() ? AMAS_PRONUNCIATION_FEEDBACK : " Pronunciation Feedback";// Alpha";
  }

  public String getInitialPrompt() {
    return isAMAS() ? AMAS_INITIAL_PROMPT : INITIAL_PROMPT;
  }

  public String getFirstBullet() {
    return isAMAS() ? "Receive feedback on strengths and weaknesses." : "Practice vocabulary with audio flashcards.";
  }

/*
  public String getWelcomeMessage() {
    return isAMAS() ? AMAS_WELCOME : "Welcome to " + "NetProF" + "!";
  }
*/

  private void getPreferredVoices(String value) {
    for (String userid : value.split(",")) {
      try {
        preferredVoices.add(Long.parseLong(userid));
        //  logger.info("pref users " + preferredVoices);
      } catch (NumberFormatException e) {
        logger.warning("couldn't parse userid " + userid);
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
      logger.warning("couldn't parse " + value + "using " + defValue + " for " + propName);
    }
    return defValue;
  }

/*
  private float getFloat(String value, float defValue, String propName) {
    try {
      if (value == null) return defValue;
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      logger.warning("couldn't parse " + value + "using " + defValue +" for " + propName);
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
   * @return
   * @see #PropertyHandler(java.util.Map)
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
    } else {
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

    // 9/7/16 Ray says no admin mode
//    String adminParam = Window.Location.getParameter(ADMIN_PARAM);
//    if (adminParam != null) {
//      adminView = !adminParam.equals("false");
//    }
    String p = Window.Location.getParameter(ANALYSIS);
    if (p != null) {
      analysis = !p.equals("false");
    }

    p = Window.Location.getParameter("context");
    if (p != null) {
      canPracticeContext = !p.equals("false");
    }

    String turkParam = Window.Location.getParameter(TURK_PARAM);
    if (turkParam != null) {
      turkID = turkParam;
    }

    String resetPasswordID = Window.Location.getParameter(RP);
    if (resetPasswordID != null) {
      resetPassToken = resetPasswordID;
    }

    String dominoResetPasswordID = Window.Location.getParameter(CPW_TOKEN);
    if (dominoResetPasswordID != null) {
      resetPassToken = dominoResetPasswordID;
      logger.info("1 found cpw token '" +resetPassToken+          "'");
    }

    dominoResetPasswordID = Window.Location.getParameter(CPW_TOKEN2);
    if (dominoResetPasswordID != null) {
      resetPassToken = dominoResetPasswordID;
      logger.info("2 found cpw token '" +resetPassToken+          "'");
    }

    String cdEnable = Window.Location.getParameter(CD);
    if (cdEnable != null) {
      cdEnableToken = cdEnable;
    }

    String emailR = Window.Location.getParameter(ER);
    if (emailR != null) {
      emailRToken = emailR;
    }

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

/*
    String loginType = Window.Location.getParameter(LOGIN_TYPE_PARAM);
    if (loginType != null) {
      try {
        this.loginType = LOGIN_TYPE.valueOf(loginType);
      } catch (IllegalArgumentException e) {
        logger.warning("couldn't parse " + loginType);
      }
    }
*/

    if (Window.Location.getParameter(RESPONSE_TYPE) != null) {
      responseType = Window.Location.getParameter(RESPONSE_TYPE);
    }

    setResponseType();
  }

  /**
   * @return
   * @see mitll.langtest.client.LangTest#recordingModeSelect()
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  private boolean isGrading() {
    return grading;
  }

  private void setGrading(boolean v) {
    this.grading = v;
  }

  public boolean isBkgColorForRef() {
    return bkgColorForRef;
  }

  /**
   * For headstart integration.
   *
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

  public boolean isAdminView() {
    return false;//adminView;
  }

  public boolean useAnalysis() {
    return analysis;
  }

  public boolean canPracticeContext() {
    return canPracticeContext;
  }

  public String getTurkID() {
    return turkID;
  }

  public String getReleaseDate() {
    return releaseDate;
  }

  public int getRecordTimeout() {
    return recordTimeout;
  }

  public String getNameForItem() {
    return nameForItem;
  }

  public String getNameForAnswer() {
    return nameForAnswer;
  }

  public String getNameForRecorder() {
    return nameForRecorder;
  }

  public boolean isLogClientMessages() {
    return logClientMessages;
  }

  public boolean hasDialog() {
    return !dialog.isEmpty();
  }

  public String getLanguage() {
    return language;
  }

  public String getSplash() {
    return splashTitle;
  }

  /**
   * @return
   * @see LangTest#isRightAlignContent()
   */
  public boolean isRightAlignContent() {
    return rightAlignContent;
  }

  public boolean showFlashcardAnswer() {
    return showFlashcardAnswer;
  }

  public boolean shouldAllowPlusInURL() {
    return allowPlusInURL;
  }

  public boolean showSpectrogram() {
    return spectrogram;
  }

  /**
   * @return
   * @see LangTest#showLogin()
   */
  String getResetPassToken() { return resetPassToken;  }

  String getCdEnableToken() {
    return cdEnableToken;
  }

  String getEmailRToken() {
    return emailRToken;
  }

  public boolean doClickAndHold() {
    return clickAndHold;
  }

  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  public boolean showContextButton() {
    return showContext;
  }

/*
  public boolean shouldShowWelcome() {
    return showWelcome;
  }
*/

  private static boolean knownChoice(String choice) {
    return TEXT.equals(choice) || AUDIO.equals(choice) || SPEECH.equals(choice);
  }

  /**
   * Parse URL to extract the responseType values
   */
  private void setResponseType() {
    String href = Window.Location.getHref();
    if (href.contains("responseType=")) {
      //     logger.info("found response type " + href);
      String s = href.split("responseType=")[1];
      String candidate = s.split("\\*\\*\\*")[0];
      if (knownChoice(candidate)) {
        responseType = candidate;
      } else {
        logger.warning("responseType unknown " + candidate);
      }
    }
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  public boolean isOdaMode() {
    return false;
  }

  /**
   * TODO : Consider rewording for other customers...
   *
   * @return
   * @see UserPassLogin#getLoginInfo
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
   *
   * @return
   * @seex UserPassLogin#getRecordAudioPopover
   */
/*
  public String getRecordAudioPopoverText() {
    return "Click here if you have been assigned to record reference audio or do quality control.<br/>" +
        "After you click sign up, " +
        "LTEA personnel will approve your account.<br/>" +
        "You will receive an email once it's approved.<br/>" +
        "You will not be able to access NetProF until approval is granted.";
  }
*/

/*  public String getAMASHelpMessage() {
    return
        "Welcome to the Automatic Multi-Skilled Assessment System (AMAS)<br/>" +
            "<br/>" +
            "If you are a first-time user of this site, or an existing user of an earlier version of AMAS you will need " +
            "to use the \"Sign Up\" box to add/update a password and an email address to your account. Your email is " +
            "only used if you ever forget your password.<br/>" +
            "<br/>" +
            "Once you create/update your Username, Email, and Password, click on “sign up” and you will be taken to " +
            "the site. For future access, use the Login box to access the AMAS site.<br/>" +
            "<br/>" +
            "The site will remember your login information on this computer for up to one year. You will need to login " +
            "with your username and password again if you access AMAS from a different machine.";
  }*/
}
