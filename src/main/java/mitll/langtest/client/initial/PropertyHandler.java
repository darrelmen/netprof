/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.initial;

import com.google.gwt.user.client.Window;
import mitll.langtest.client.recorder.RecordButton;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static mitll.langtest.client.user.ResetPassword.SHOW_ADVERTISED_IOS;


public class PropertyHandler {
  private final Logger logger = Logger.getLogger("PropertyHandler");

  /**
   *
   */
  public static final boolean IS_BETA = false;


  private static final String AFTER_STOP_DELAY_MILLIS = "afterStopDelayMillis";
  private static final String SHOW_SPECTROGRAM1 = "showSpectrogram";


  private static final String PRONUNCIATION_FEEDBACK = "netprof";//"Pronunciation Feedback";
  private static final String CPW_TOKEN = "CPWtoken";
  public static final String CPW_TOKEN_2 = "CPW-token";

  private static final String IS_BETA_PROP = "isBeta";

  /**
   * Possibly we need to add a delay after button is released to actually tell flash to stop recording.
   *
   * @see RecordButton#startOrStopRecording()
   */
  private static final int DEFAULT_AFTER_STOP_DELAY_MILLIS = 160;//120;//185;//85;

  private static final String APP_TITLE = "appTitle";
  private static final String SPLASH_TITLE = "splashTitle";
  @Deprecated
  private static final String DEMO_MODE = "demo";
  private static final String RECORD_TIMEOUT = "recordTimeout";

  //  private static final String NAME_FOR_ITEM = "nameForItem";
  private static final String NAME_FOR_ANSWER = "nameForAnswer";
  //  private static final String NAME_FOR_RECORDER = "nameForRecorder";
  private static final String LOG_CLIENT_MESSAGES = "logClient";

  // URL parameters that can override above parameters

  private static final String BKG_COLOR_FOR_REF = "bkgColorForRef";

  private static final int DEFAULT_TIMEOUT = 45000;
  private static final String ALLOW_PLUS_IN_URL = "allowPlusInURL";
  private static final String SHOW_SPECTROGRAM = "spectrogram";
  private static final String SHOW_SPECTROGRAM2 = "showSpectrogram";
  private static final String USE_PHONE_TO_DISPLAY = "usePhoneToDisplay";

  private static final String CLICK_AND_HOLD = "clickAndHold";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";
  private static final String PREFERRED_VOICES = "preferredVoices";
  private static final String DOMINO_SERVER = "domino.url";

//  private static final String VOCAB_MODE_CHOICE_TITLE = "preferredVoices";
//  private static final String DIALOG_MODE_CHOICE_TITLE = "preferredVoices";

  private static final String HELP = "help";
  private static final String HELP_EMAIL = "helpEmail";
  private static final String NETPROF_HELP_DLIFLC_EDU = "netprof-help@dliflc.edu";

  // private static final String INITIAL_PROMPT = "Practice pronunciation and learn vocabulary.";//"Learn how to pronounce words and practice vocabulary.";
  private static final String INITIAL_PROMPT = "Practice speaking words, sentences, and conversations.";//"Learn how to pronounce words and practice vocabulary.";

  //private static final String INITIAL_PROMPT = "Practice dialogs to improve pronunciation and learn vocabulary.";//"Learn how to pronounce words and practice vocabulary.";

  //  private static final String PRACTICE_VOCABULARY_WITH_AUDIO_FLASHCARDS = "Practice vocabulary with audio flashcards.";
 // private static final String PRACTICE_VOCABULARY_WITH_AUDIO_FLASHCARDS = "Practice two person conversations and as an interpreter.";
  private static final String PRACTICE_VOCABULARY_WITH_AUDIO_FLASHCARDS = "Play the role of an interpreter in a dialog.";
  //  public static final String RECEIVE_FEEDBACK_ON_STRENGTHS_AND_WEAKNESSES = "Receive feedback on strengths and weaknesses.";
  private static final String THIRD_BULLET = "Create and share vocabulary lists for study and review.";//"Make your own lists of words to study later or to share.";

  private boolean usePhoneToDisplay;

  private int afterStopDelayMillis = DEFAULT_AFTER_STOP_DELAY_MILLIS;

  /**
   *
   */
  private static final String DOMINO_SERVER_DEFAULT = "http://ltea-data2-dev:8080/domino-ltea/";
  private String dominoURL = DOMINO_SERVER_DEFAULT;

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

  @Deprecated
  public Set<Integer> getPreferredVoices() {
    return preferredVoices;
  }

  public boolean shouldUsePhoneToDisplay() {
    return usePhoneToDisplay;
  }

  public boolean isAMAS() {
    return false;
  }

  /**
   * Typically 50 or 100 milliseconds.
   *
   * @return
   */
  public int getAfterStopDelayMillis() {
    return afterStopDelayMillis;
  }

  private boolean spectrogram = false;
  private boolean clickAndHold = true;
  private boolean quietAudioOK;
  public boolean isBeta;
  private final Set<Integer> preferredVoices = new HashSet<>();

  /**
   * @see #checkParams
   */
  private String resetPassToken = "";
  private String sendResetPassToken = "";
  private final Map<String, String> props;
  private boolean bkgColorForRef = false;
  private boolean showAdvertiseIOS = false;

  private boolean demoMode;

  private boolean logClientMessages = false;
  private String nameForAnswer = "Recording";

  private int recordTimeout = DEFAULT_TIMEOUT;

  private String splashTitle = null;
  private String appTitle = null;
  private boolean allowPlusInURL;

  private String helpEmail = NETPROF_HELP_DLIFLC_EDU;
  private String helpMessage = "";

  /**
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad
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

      switch (key) {
        case APP_TITLE:
          appTitle = value;
          break;
        case BKG_COLOR_FOR_REF:
          bkgColorForRef = getBoolean(value);
          break;
        case DEMO_MODE:
          demoMode = getBoolean(value);
          break;
        case RECORD_TIMEOUT:
          recordTimeout = getInt(value, DEFAULT_TIMEOUT, RECORD_TIMEOUT);
          break;
        case NAME_FOR_ANSWER:
          nameForAnswer = value;
          break;
        case LOG_CLIENT_MESSAGES:
          logClientMessages = getBoolean(value);
          break;
        case SPLASH_TITLE:
          splashTitle = value;
          break;
        case ALLOW_PLUS_IN_URL:
          allowPlusInURL = getBoolean(value);
          break;
        case SHOW_SPECTROGRAM:
          spectrogram = getBoolean(value);
          break;
        case SHOW_SPECTROGRAM1:
          spectrogram = getBoolean(value);
          break;
        case QUIET_AUDIO_OK:
          quietAudioOK = getBoolean(value);
          break;
        case IS_BETA_PROP:
          isBeta = getBoolean(value);
          break;
        case DOMINO_SERVER:
          dominoURL = value;
          break;
        case HELP:
          helpMessage = value;
          break;
        case HELP_EMAIL:
          helpEmail = value;
          break;
        case AFTER_STOP_DELAY_MILLIS:
          afterStopDelayMillis = getInt(value, DEFAULT_AFTER_STOP_DELAY_MILLIS, AFTER_STOP_DELAY_MILLIS);
          break;
        case USE_PHONE_TO_DISPLAY:
          // logger.info("found " + USE_PHONE_TO_DISPLAY + " = " + value);
          usePhoneToDisplay = getBoolean(value);
          break;
        case PREFERRED_VOICES:
          getPreferredVoices(value);
          break;
      }
    }

    if (appTitle == null) {
      appTitle = getAppTitleSuffix();
    }

    if (splashTitle == null) {
      splashTitle = "";//language + getSpashTitleSuffix();
    }
  }

  private String getAppTitleSuffix() {
    return PRONUNCIATION_FEEDBACK;
  }

  public String getInitialPrompt() {
    return INITIAL_PROMPT;
  }

  public String getFirstBullet() {
    return PRACTICE_VOCABULARY_WITH_AUDIO_FLASHCARDS;
  }

  public String getThirdBullet() {
    return PRACTICE_VOCABULARY_WITH_AUDIO_FLASHCARDS;
  }

  @Deprecated
  private void getPreferredVoices(String value) {
    for (String userid : value.split(",")) {
      try {
        preferredVoices.add(Integer.parseInt(userid));
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
    if (Window.Location.getHref().endsWith("reset_password")) {
      this.sendResetPassToken = "reset_password";
    }

    String bkgColorForRefParam = Window.Location.getParameter(BKG_COLOR_FOR_REF);
    if (bkgColorForRefParam != null) {
      bkgColorForRef = !bkgColorForRefParam.equals("false");
    }
    String dominoResetPasswordID = Window.Location.getParameter(CPW_TOKEN);
    if (dominoResetPasswordID != null) {
      resetPassToken = dominoResetPasswordID;
      logger.info("1 found cpw token '" + resetPassToken + "'");
    }

    dominoResetPasswordID = Window.Location.getParameter(CPW_TOKEN_2);
    if (dominoResetPasswordID != null) {
      resetPassToken = dominoResetPasswordID;
      logger.info("2 found cpw token '" + resetPassToken + "'");
    }
    if (Window.Location.getParameter(CLICK_AND_HOLD) != null) {
      clickAndHold = !Window.Location.getParameter(CLICK_AND_HOLD).equals("false");
    }
    if (Window.Location.getParameter(SHOW_SPECTROGRAM) != null) {
      spectrogram = !Window.Location.getParameter(SHOW_SPECTROGRAM).equals("false");
      if (spectrogram) logger.info("show spectrogram");
    }
    if (Window.Location.getParameter(SHOW_SPECTROGRAM2) != null) {
      spectrogram = !Window.Location.getParameter(SHOW_SPECTROGRAM2).equals("false");
      if (spectrogram) logger.info("show spectrogram");
    }
    if (Window.Location.getParameter(USE_PHONE_TO_DISPLAY) != null) {
      usePhoneToDisplay = !Window.Location.getParameter(USE_PHONE_TO_DISPLAY).equals("false");
      if (usePhoneToDisplay) logger.info("usePhoneToDisplay is " + usePhoneToDisplay);
    }
    if (Window.Location.getParameter(SHOW_ADVERTISED_IOS) != null) {
      showAdvertiseIOS = Window.Location.getParameter(SHOW_ADVERTISED_IOS) != null;
    }
  }

  /**
   * @return
   * @see LifecycleSupport#recordingModeSelect(boolean)
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   */
  public boolean isBkgColorForRef() {
    return bkgColorForRef;
  }

  public String getAppTitle() {
    return appTitle;
  }

  /**
   * Just like in KeyStorage... TODO : move to using it for all storage
   *
   * @return
   */
  @NotNull
  public String getAppNameSmarter() {
    String appName = getAppName();
    String path = Window.Location.getPath();
    String app = path.substring(0, path.lastIndexOf("/"));
    appName = app.isEmpty() ? appName : app;

//    if (!appName.equalsIgnoreCase("Netscape")) {
//      logger.info("appName " + appName);
//    }
    return appName;
  }

  public static native String getAppName() /*-{
      return $wnd.navigator.appName;
  }-*/;

  @Deprecated
  public boolean isDemoMode() {
    return demoMode;
  }

  public int getRecordTimeout() {
    return recordTimeout;
  }

  public String getNameForAnswer() {
    return nameForAnswer;
  }

  public boolean isLogClientMessages() {
    return logClientMessages;
  }

  public boolean shouldAllowPlusInURL() {
    return allowPlusInURL;
  }

  public boolean showSpectrogram() {
    return spectrogram;
  }

  /**
   * @return
   * @see InitialUI#showLogin
   */
  String getResetPassToken() {
    return resetPassToken;
  }

  public boolean doClickAndHold() {
    return clickAndHold;
  }

  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  public String getDominoURL() {
    return dominoURL;
  }

  public String getHelpMessage() {
    return helpMessage.isEmpty() ? getHelpDefault() : helpMessage;
  }

  @NotNull
  private String getHelpDefault() {
    return "Please consult the user manual or send email to " +
        "<a href=\"mailto:" +
        helpEmail +
        "\">" +
        helpEmail +
        "</a>" +
        ".";
  }

  String getSendResetPassToken() {
    return sendResetPassToken;
  }

  public Map<String, String> getProps() {
    return props;
  }

  boolean isShowAdvertiseIOS() {
    return showAdvertiseIOS;
  }

  public String getHelpEmail() {
    return helpEmail;
  }
}
