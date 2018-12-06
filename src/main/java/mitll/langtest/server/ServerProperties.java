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

package mitll.langtest.server;

import com.typesafe.config.ConfigFactory;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.mail.EmailList;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.Affiliation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectProperty.MODELS_DIR;
import static mitll.langtest.shared.project.ProjectProperty.WEBSERVICE_HOST_PORT;

/**
 * This has a lot of overlap with the PropertyHandler set of properties.
 * <p>
 * TODO : There should be a better way of handling their relationship.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/5/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerProperties {
  private static final Logger logger = LogManager.getLogger(ServerProperties.class);

  /**
   * TODO : good idea????
   * ON THIS BRANCH!
   */
  private static final String APP_NAME_DEFAULT = "netprof";//dialog";

  /**
   * As of 8/2/17 we have these languages on the hydra2 server:
   * korean, levantine, msa, and russian
   * By default, we set the hydra host to point to h2 for these.
   * This is consistent with the routing in the httpd.conf file on the netprof server, e.g.:
   * <p>
   * # h2 services
   * JkMount /netprof/downloadAudio/h2 h2
   * JkMount /netprof/scoreServlet/h2 h2
   * JkMount /netprof/langtest/audio-manager/h2 h2
   * JkMount /netprof/langtest/scoring-manager/h2 h2
   * <p>
   * # for every language on h2
   * JkMount /netprof/bestAudio/korean/* h2
   * JkMount /netprof/answers/korean/* h2
   * JkMount /netprof/audioimages/korean/* h2
   */

  /**
   * Languages on the hydra2 server.
   */
  private static final String HYDRA_2_LANGUAGES = "hydra2Languages";
  private static final String HYDRA_2_LANGUAGES_DEFAULT = "korean,levantine,msa,russian";

  /**
   * Going forward we might have more hydra hosts to handle more load, or a different partition of languages.
   */
  public static final String H2_HOST = "h2";

  private static final String APP_NAME = "appName";
  private static final String APP_TITLE = "appTitle";

  private static final String FALSE = "false";
  private static final String TRUE = "true";

  /**
   * Mira stuff... mostly kinda dead
   */
  @Deprecated
  public static final String MIRA_DEVEL_HOST = "mira-devel.llan.ll.mit.edu/scorer/item"; //"mira-devel.llan.ll.mit.edu/msa/item";
  @Deprecated
  private static final String MIRA_DEVEL = "https://" + MIRA_DEVEL_HOST;
  @Deprecated
  private static final String MIRA_LEN = "https://mira.ll.mit.edu/scorer/item";
  @Deprecated
  private static final String MIRA_DEFAULT = MIRA_LEN;
  @Deprecated
  private static final String MIRA_CLASSIFIER_URL = "miraClassifierURL";


  private static final String MAIL_SERVER = "mail.server";
  private static final String SERVER_NAME = "SERVER_NAME";
  private static final String DEBUG_ONE_PROJECT = "debugOneProject";
  private static final String IOS_VERSION = "1.0.1";
  private static final String I_OS_VERSION = "iOSVersion";
  private static final String IMPL_VERSION = Attributes.Name.IMPLEMENTATION_VERSION.toString();
  public static final String DEFAULT_MAIL_FROM = "netprof-admin@ll.mit.edu";
  private static final String MAIL_REPLYTO = "mail.replyto";
  private static final String HEARTBEAT_REC = "gordon.vidaver@ll.mit.edu,zebin.xia@dliflc.edu";
  private static final int DEFAULT_PERIOD = 5 * 60 * 1000;
  private static final String DOMINO_LL_MIT_EDU = "domino.ll.mit.edu";
  private static final String LOG_MAILHOST = "log.mailhost";
  private static final String LOG_MAILFROM = "log.mailfrom";
  private static final String MAIL_FROM = "mail.from";

  //  private static final String IMAGE = "image";
  private static final String NETPROF = "netprof";

  private static final String POSTGRES_HYDRA = "postgresHydra";
  private static final String POSTGRES_DATA2_DIALOG = "postgresData2Dialog";


  public static final String ADD_USER_VIA_EMAIL = "addUserViaEmail";
  public static final String SEND_HEARTBEAT = "sendHeartbeat";
  public static final String HEARTBEAT_PERIOD = "heartbeatPeriod";
  public static final String HEARTBEAT_REC1 = "heartbeatRec";

  // private String dbConfig = POSTGRES_DATA2_DIALOG;

  private static final String SCORING_MODEL = "scoringModel";
  private static final String TALKS_TO_DOMINO = "talksToDomino";

  //private List<String> hearbeatRecDef = Arrays.asList(HEARTBEAT_REC.split(","));

  @Deprecated
  private String miraClassifierURL = MIRA_DEVEL;// MIRA_LEN; //MIRA_DEVEL;

  private static final String LESSON_PLAN_FILE = "lessonPlanFile";
  private static final String USE_H_2 = "useH2";

  private static final String SLEEP_BETWEEN_DECODES_MILLIS = "sleepBetweenDecodesMillis";
  private static final String DB_CONFIG = "dbConfig";
  private static final String POSTGRES = "postgres";

  /**
   * @see #useProperties
   */
  private static final String UI_PROPERTIES = "ui.properties";
  private static final String CONFIG_FILE1 = "config.file";
  private static final String RELEASE_DATE = "releaseDate";
  // use postfix relay to llmx2
  private static final String LLMAIL_LL_MIT_EDU = "localhost";

  private static final String NP_SERVER = "netprof.ll.mit.edu";

  /**
   * For development, from a laptop.
   */
  private static final String HYDRA_HOST_URL_DEFAULT = "https://netprof1-dev.llan.ll.mit.edu/netprof/";
//  private static final String HYDRA_HOST_URL_DEFAULT = "https://netprof.ll.mit.edu/netprof/";

  private static final String USE_SCORE_CACHE = "useScoreCache";

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  private static final String H2_DATABASE = "h2Database";
  private static final String LANGUAGE = "language";

  private static final String MEDIA_DIR = "mediaDir";
  // private static final String IMAGE_DIR = "imageDir";
  private static final String ANSWER_DIR = "answerDir";
  private static final String NETPROF_AUDIO_DIR = "audioDir";
  private static final String DCODR_DIR = "dcodrDir";

  private static final String MIN_PRON_SCORE = "minPronScore";
  private static final String MIN_PRON_SCORE_DEFAULT = "" + 0.31f;
  private static final String USE_PREDEFINED_TYPE_ORDER = "usePredefinedTypeOrder";
  private static final String SKIP_SEMICOLONS = "skipSemicolons";
  private static final String MAX_NUM_EXERCISES = "maxNumExercises";
  private static final String TIER_INDEX = "tierIndex";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";

  /**
   * @deprecated - we need a per-project set
   */
  private static final String PREFERRED_VOICES = "preferredVoices";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";
  private static final String DO_DECODE = "dodecode";
  private static final String DO_TRIM = "dotrim";

  //private static final String USE_PHONE_TO_DISPLAY = "usePhoneToDisplay";

  private static final int MIN_DYNAMIC_RANGE_DEFAULT = 24;      // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final int SLEEP_BETWEEN_DECODES_DEFAULT = 100; // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final String MIN_DYNAMIC_RANGE = "minDynamicRange";
  private static final String CHECK_AUDIO_ON_STARTUP = "checkAudioOnStartup";
  private static final String CHECK_AUDIO_FILE_EXISTS = "checkAudioFileExists";
  private static final String DO_AUDIO_CHECKS_IN_PRODUCTION = "doAudioChecksInProduction";
  private static final String CHECK_AUDIO_TRANSCRIPT = "checkAudioTranscript";
  private static final String MIN_ANALYSIS_SCORE = "minAnalysisScore";
  private static final String HYDRA_HOST = "hydraHost";

  private static final int MIN_SCORE_TO_SHOW = 0;

  /**
   * Note netprof is all lower case.
   */
  public static final String DEFAULT_NETPROF_AUDIO_DIR = "/opt/netprof/";
  private static final String DEFAULT_DCODR_DIR = "/opt/dcodr/";
  private static final String APPLICATION_CONF = DEFAULT_NETPROF_AUDIO_DIR + "/config/application.conf";

  public static final String BEST_AUDIO = "bestAudio";
  private static final String ANSWERS = "answers";

  private static final String HELP_EMAIL = "helpEmail";
  private static final String HELP_EMAIL_DEF = "netprof-help@dliflc.edu";

  private Properties props = new Properties();
  private Properties uiprops = new Properties();

  private double minPronScore;

  // just for automated testing
  private boolean quietAudioOK;
  /**
   * @see #useProperties
   * @see #ServerProperties(Properties, Map, File)
   */
  private Map<String, String> manifest = new HashMap<>();

  /**
   * TODO : revisit
   *
   * @deprecated - need preferred voices per project...
   */
  private final Set<Integer> preferredVoices = new HashSet<>();
  private EmailList emailList;
  //  private final Map<String, String> phoneToDisplay = new HashMap<>();
  private final Map<Language, Map<String, String>> langToPhoneToDisplay = new HashMap<>();

  private List<Affiliation> affliations = new ArrayList<>();

  /**
   * @see mitll.langtest.server.database.copy.CreateProject#createProject
   */
  public static final List<ProjectProperty> CORE_PROPERTIES = Arrays.asList(
      MODELS_DIR,
      WEBSERVICE_HOST_PORT
  );
  private String configFileFullPath;

  public ServerProperties() {

    Map<String, String> value = new HashMap<>();

    // ā	á	ǎ	à	ē	é	ě	è	ī	í	ǐ	ì	ō	ó	ǒ	ò	ū	ú	ǔ	ù	ǖ	ǘ	ǚ	ǜ

    value.put("a1", "ā");
    value.put("a2", "á");
    value.put("a3", "ǎ");
    value.put("a4", "à");

    value.put("e1", "ē");
    value.put("e2", "é");
    value.put("e3", "ě");
    value.put("e4", "è");

    value.put("i1", "ī");
    value.put("i2", "í");
    value.put("i3", "ǐ");
    value.put("i4", "ì");

    value.put("o1", "ō");
    value.put("o2", "ó");
    value.put("o3", "ǒ");
    value.put("o4", "ò");

    value.put("u1", "ū");
    value.put("u2", "ú");
    value.put("u3", "ǔ");
    value.put("u4", "ù");

//    langToPhoneToDisplay.put("mandarin", value);
//    logger.info("now " + langToPhoneToDisplay);
  }

  /**
   * @param props
   * @param configDir
   * @see mitll.langtest.server.property.ServerInitializationManagerNetProf#getServerProperties
   */
  public ServerProperties(Properties props,
                          Map<String, String> manifest,
                          File configDir) {
    this();
    this.props = props;
    this.manifest = manifest;
    manifest.putIfAbsent(IMPL_VERSION, "blank");
    try {
      useProperties(configDir, manifest.getOrDefault(ServerInitializationManagerNetProf.BUILT_DATE, "Unknown"));
    } catch (FileNotFoundException e) {
      logger.error("ServerProperties looking in " + configDir + " :" + e, e);
    }
  }

  /**
   * Just for IMPORT.
   *
   * @param configDir
   * @param configFile
   * @seex mitll.langtest.server.database.ImportCourseExamples#makeDatabaseImpl
   */
  public ServerProperties(String configDir, String configFile) {
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    readProps(configDir, configFile);
    //readPhonemeMap(configDir);
  }

  /**
   * @param configDir
   * @param configFile
   * @paramx dateFromManifest
   */
  private void readProps(String configDir, String configFile) {
    String configFileFullPath = configDir + File.separator + configFile;
    if (!new File(configFileFullPath).exists()) {
      logger.error("couldn't find config file " + new File(configFileFullPath));
    } else {
      try {
        props = readPropertiesFromFile(configFileFullPath);
        useProperties(new File(configDir), "");
        this.configFileFullPath = configFileFullPath;
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  private void setApplicationConf() {
    String applicationConfPath = APPLICATION_CONF;
    if (props.containsKey(CONFIG_FILE1)) {
      applicationConfPath = props.getProperty(CONFIG_FILE1);
    }
    Properties props = System.getProperties();
    props.setProperty(CONFIG_FILE1, applicationConfPath);
//    logger.info("setting config.file to " + applicationConfPath);
//    logger.info("setting config.file to " + System.getProperties().get(CONFIG_FILE1));
    ConfigFactory.invalidateCaches();
  }

  @NotNull
  private Properties readPropertiesFromFile(String configFileFullPath) throws IOException {
    Properties properties = new Properties();
    if (new File(configFileFullPath).exists()) {
      FileInputStream inStream = new FileInputStream(configFileFullPath);
      properties.load(inStream);
      inStream.close();
    }
    return properties;
  }

  private void useProperties(File configDir, String dateFromManifest) throws FileNotFoundException {
    emailList = new EmailList(props);

    readProperties(dateFromManifest);
    affliations = new JsonConfigReader().getAffiliations(configDir);
    setApplicationConf();

    String configFileFullPath = configDir + File.separator + props.getProperty("uiprops", UI_PROPERTIES);

    try {
      uiprops = readPropertiesFromFile(configFileFullPath);
      uiprops.putAll(manifest);
    } catch (IOException e) {
      logger.error("got " + e + " reading from " + configFileFullPath, e);
    }

    getEmailAddress();

  }

  /**
   * @return
   * @see LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
   * @deprecated
   */
  public String getH2Database() {
    return props.getProperty(H2_DATABASE, "npf" + props.getProperty(LANGUAGE));
  }

  /**
   * @return
   * @Deprecated only for use when importing data from old site
   */
  public String getLessonPlan() {
    return props.getProperty(LESSON_PLAN_FILE, "");
  }

  public boolean useScoreCache() {
    return getDefaultTrue(USE_SCORE_CACHE);
  }

  @Deprecated
  public String getLanguage() {
    return props.getProperty(LANGUAGE, "");
  }

  //specify this in  in config file like: tierIndex=5,4,-1. That
  //tells us to treat (zero-indexed) column 5 like the "unit" column,
  //column 4 like the "chapter" column, and that there isn't a "week" column
  //--note that depending on how many unique elements are in each column, the 
  //rows that appear on the classroom site may not be in the order 
  //"unit,chapter,week"

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.ExcelImport#ExcelImport
   */
  public int[] getUnitChapterWeek() {
    int[] parsedUCW = new int[]{-1, -1, -1};
    String[] ucw = props.getProperty(TIER_INDEX, "-1,-1,-1").replaceAll("\\s+", "").split(",");
    if (ucw.length != 3)
      return parsedUCW;
    for (int i = 0; i < 3; i++)
      parsedUCW[i] = Integer.parseInt(ucw[i]);
    return parsedUCW;
  }

  /**
   * Relative to install location
   *
   * @return
   * @see LangTestDatabaseImpl#setInstallPath
   */
  public String getMediaDir() {
    return props.getProperty(MEDIA_DIR, getAudioBaseDir() + BEST_AUDIO);
  }

  /**
   * Relative to install location
   *
   * @return
   */
  public String getAnswerDir() {
    return props.getProperty(ANSWER_DIR, getAudioBaseDir() + ANSWERS);
  }

  public String getAudioBaseDir() {
    return props.getProperty(NETPROF_AUDIO_DIR, DEFAULT_NETPROF_AUDIO_DIR);
  }

  public String getDcodrBaseDir() {
    return props.getProperty(DCODR_DIR, DEFAULT_DCODR_DIR);
  }

  public boolean useProxy() {
    return getHostName().toLowerCase().contains("mitll") || props.getProperty("useProxy") != null;
  }

  /**
   * Choose a server to do work, like generate the weekly report.
   *
   * @return
   */
  public boolean isFirstHydra() {
    return getHostName().startsWith("hydra.");// || getHostName().startsWith("hydra-dev.");
  }

  public String getHostName() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getHostName();
    } catch (UnknownHostException e) {
      logger.error("got " + e, e);
      return "Unknown";
    }
  }

  public double getMinPronScore() {
    return minPronScore;
  }

  public boolean usePredefinedTypeOrder() {
    return getDefaultFalse(USE_PREDEFINED_TYPE_ORDER);
  }

  public boolean shouldSkipSemicolonEntries() {
    return getDefaultTrue(SKIP_SEMICOLONS);
  }

  boolean removeExercisesWithMissingAudio() {
    return getDefaultTrue(REMOVE_EXERCISES_WITH_MISSING_AUDIO);
  }

  /**
   * Need per project triggering of repopulate ref result table
   *
   * @return
   */
  @Deprecated
  public boolean shouldDoDecode() {
    return getDefaultFalse(DO_DECODE);
  }

  public boolean isAMAS() {
    return getDefaultFalse("isAMAS");
  }

  public boolean shouldCheckAudioTranscript() {
    return getDefaultTrue(CHECK_AUDIO_TRANSCRIPT);
  }

  /**
   * @return
   * @seex mitll.langtest.server.decoder.RefResultDecoder#doRefDecode
   */
  public boolean shouldTrimAudio() {
    return getDefaultFalse(DO_TRIM);
  }

  private int getIntPropertyDef(String audioOffset, int defaultValue) {
    try {
      String property = props.getProperty(audioOffset);
      return (property == null) ? defaultValue : Integer.parseInt(property);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public int getMaxNumExercises() {
    int maxNumExercises = Integer.MAX_VALUE;
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
   *
   * @return
   */
  public Map<String, String> getProperties() {
    return getPropertyMap(this.props);
  }

  private Map<String, String> getPropertyMap(Properties props) {
    Map<String, String> kv = new HashMap<>();
    for (Object prop : props.keySet()) {
      String sp = (String) prop;
      kv.put(sp, props.getProperty(sp).trim());
    }
    //logger.debug("for config " + relativeConfigDir + " prop file has " + kv.size() + " properties : " + props.keySet());
    return kv;
  }

  /**
   * The config web.xml file.
   * <p>
   * Note that this will only ever be called once.
   * TODOx : remember to set the approvers and approver emails properties for new customers
   * TODOx : also you may want to set the welcome message to OFF
   * <p>
   * Here's where we can over-ride default values.
   *
   * @param dateFromManifest
   * @see
   */
  private void readProperties(String dateFromManifest) {
    setReleaseDate(dateFromManifest);
    readProperties();
  }

  private void readProperties() {
    quietAudioOK = getDefaultFalse(QUIET_AUDIO_OK);

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

    // TODO TODO : read this from project!
    String property = props.getProperty(PREFERRED_VOICES);
    if (property != null) {
      for (String userid : property.split(",")) {
        try {
          preferredVoices.add(Integer.parseInt(userid));
          logger.info("preferredVoices pref users " + preferredVoices);
        } catch (NumberFormatException e) {
          logger.error("couldn't parse userid " + userid);
        }
      }
    }
    miraClassifierURL = props.getProperty(MIRA_CLASSIFIER_URL, MIRA_DEFAULT);

    props.put(SCORING_MODEL, getPropertyDef(MODELS_DIR, ""));

    String lessonPlan = getLessonPlan();
    if (lessonPlan != null && lessonPlan.startsWith("http")) props.setProperty(TALKS_TO_DOMINO, TRUE);

/*    if (getFontFamily() != null) {
      props.setProperty(FONT_FAMILY, getFontFamily());
      logger.info(FONT_FAMILY +
          "=" + getFontFamily() + " : " + props.getProperty(FONT_FAMILY));
    }*/
  }

  private void setReleaseDate(String dateFromManifest) {
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      props.setProperty(RELEASE_DATE, dateFromManifest);
    }
  }

  private boolean getDefaultFalse(String param) {
    return props.getProperty(param, FALSE).equals(TRUE);
  }

  private boolean getDefaultTrue(String param) {
    return props.getProperty(param, TRUE).equals(TRUE);
  }

  private String getProperty(ProjectProperty prop) {
    return props.getProperty(prop.getName());
  }

  private String getPropertyDef(ProjectProperty prop, String def) {
    return props.getProperty(prop.getName(), def);
  }

  /**
   * if true, use old school (hydec)
   * OR if there is no webservice port specified
   *
   * @return true if only use old school hydec decoder
   * @deprecated
   */
  public boolean getOldSchoolService() {
    return Boolean.parseBoolean(props.getProperty("oldSchoolService", FALSE));
  }

  /**
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   */
  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  @Deprecated
  public Set<Integer> getPreferredVoices() {
    return preferredVoices;
  }

  public Map<String, String> getPhoneToDisplay(Language languageEnum) {
    Map<String, String> stringStringMap = langToPhoneToDisplay.get(languageEnum);
    return stringStringMap == null ? Collections.emptyMap() : stringStringMap;
  }

  /**
   * @param language
   * @param phone
   * @return
   * @see mitll.langtest.server.audio.ScoreToJSON#getJsonForScore
   */
  public String getDisplayPhoneme(Language language, String phone) {
    Map<String, String> phoneToDisplay = getPhoneToDisplay(language);
    if (phoneToDisplay == null) {
      return phone;
    } else {
      String s = phoneToDisplay.get(phone);
      return (s == null) ? phone : s;
    }
  }

  /**
   * @param languageEnum
   * @return
   * @seex mitll.langtest.server.scoring.AlignmentHelper#getPrecalcScores(boolean, ISlimResult, String)
   * @seex mitll.langtest.server.scoring.ASRWebserviceScoring#getPretestScore(String, ImageOptions, boolean, String, String, HydraOutput, double, int, boolean, JsonObject)
   * @see AudioFileHelper#isUsePhoneToDisplay
   * @see AudioFileHelper#getEasyAlignment(ClientExercise, String)
   * @see mitll.langtest.server.audio.ScoreToJSON#getJsonForScore(PretestScore, boolean, ServerProperties, Language)
   */
  public boolean usePhoneToDisplay(Language languageEnum) {
    return languageEnum == Language.KOREAN;
//    return getDefaultFalse(USE_PHONE_TO_DISPLAY);
  }

  // EMAIL ------------------------

  private EmailList getEmailList() {
    return emailList;
  }

  public boolean isDebugEMail() {
    EmailList emailList = getEmailList();
    return emailList != null && emailList.isDebugEMail();
  }

  public boolean isTestEmail() {
    return getEmailList().isTestEmail();
  }

  public String getEmailAddress() {
    return emailList.getEmailAddress();
  }

  /**
   * @return
   * @see UserDAO#UserDAO
   * @deprecated defined in domino
   */
  public Set<String> getAdmins() {
    Set<String> strings = Collections.emptySet();
    return emailList == null ? strings : emailList.getAdmins();
  }

  public List<String> getReportEmails() {
    return emailList.getReportEmails();
  }

  public int getMinDynamicRange() {
    return getIntPropertyDef(MIN_DYNAMIC_RANGE, MIN_DYNAMIC_RANGE_DEFAULT);
  }

  /**
   * @return 0-1 float
   */
  public float getMinAnalysisScore() {
    return (float) getIntPropertyDef(MIN_ANALYSIS_SCORE, MIN_SCORE_TO_SHOW) / 100f;
  }

  public String getMiraClassifierURL() {
    return miraClassifierURL;
  }

  public boolean useMiraClassifier() {
    return getDefaultTrue("useMiraClassifier");
  }

  public String getMiraFlavor() {
    return props.getProperty("miraFlavor", getLanguage().toLowerCase() + "-amas3");
  }

  /**
   * H2 database NOT hydra2
   *
   * @return
   */
  public boolean useH2() {
    return getDefaultFalse(USE_H_2);
  }

  public void setH2(boolean val) {
    props.setProperty(USE_H_2, Boolean.valueOf(val).toString());
  }

  public boolean doAudioCheckOnStartup() {
    return getDefaultTrue(CHECK_AUDIO_ON_STARTUP);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#setAudioDAO
   */
  public boolean doAudioFileExistsCheck() {
    return getDefaultFalse(CHECK_AUDIO_FILE_EXISTS);
  }

  public boolean doAudioChecksInProduction() {
    return getDefaultTrue(DO_AUDIO_CHECKS_IN_PRODUCTION);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#ProjectManagement
   */
  public boolean debugOneProject() {
    return getDefaultFalse(DEBUG_ONE_PROJECT);
  }

  public int debugProjectID() {
    return getIntPropertyDef("debugProjectID", -1);
  }

  public int getSleepBetweenDecodes() {
    return getIntPropertyDef(SLEEP_BETWEEN_DECODES_MILLIS, SLEEP_BETWEEN_DECODES_DEFAULT);
  }

  private static final int TRIM_SILENCE_BEFORE = 300;
  private static final int TRIM_SILENCE_AFTER = 300;

  /*
  public long getTrimBefore() {
    return getIntPropertyDef("trimBeforeMillis", TRIM_SILENCE_BEFORE);
  }

  public long getTrimAfter() {
    return getIntPropertyDef("trimAfterMillis", TRIM_SILENCE_AFTER);
  }
*/

  /**
   * Dialog branch specific config!
   * Not read from config file!
   * So we can share the netprof.properties config file.
   *
   * @return
   */
  public String getDBConfig() {
    return props.getProperty(DB_CONFIG, POSTGRES_HYDRA);
  }

  /**
   * @param optDatabase
   * @see mitll.langtest.server.database.copy.CopyToPostgres#getDatabaseLight
   */
  public void setDBConfig(String optDatabase) {
    //this.dbConfig = optDatabase;
  }

  /**
   * These point to config entries in application.conf in the resources directory in npdata.
   *
   * @see mitll.langtest.server.database.copy.CopyToPostgres#getDatabaseLight
   */
  public void setLocalPostgres() {
    props.setProperty(DB_CONFIG, POSTGRES);
  }

  public Properties getProps() {
    return props;
  }

  public String getConfigFileFullPath() {
    return configFileFullPath;
  }

  public String getAppTitle() {
    return props.getProperty(APP_TITLE, NETPROF);
  }

  public String getAppName() {
    return props.getProperty(APP_NAME, APP_NAME_DEFAULT);
  }

  /**
   * @return
   * @see
   * @see mitll.langtest.server.mail.EmailHelper#EmailHelper(ServerProperties, MailSupport)
   */
  public String getNPServer() {
    return props.getProperty(SERVER_NAME, NP_SERVER);
  }

  public String getMailServer() {
    String property = System.getProperty(LOG_MAILHOST);
    return property == null ? props.getProperty(MAIL_SERVER, LLMAIL_LL_MIT_EDU) : property;
  }

  public String getMailFrom() {
    String property = System.getProperty(LOG_MAILFROM);
    return property == null ? props.getProperty(MAIL_FROM, DEFAULT_MAIL_FROM) : property;
  }

  public String getMailReplyTo() {
    String property = System.getProperty(MAIL_REPLYTO);
    return property == null ? props.getProperty(MAIL_REPLYTO, "admin@" + getNPServer()) : property;
  }

  /**
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkForWebservice
   */
  public String getHydraHost() {
    return props.getProperty(HYDRA_HOST, HYDRA_HOST_URL_DEFAULT);
  }

  private String getProp(String prop, String def) {
    return props.getProperty(prop, def);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyOneConfigCommand
   */
  public boolean hasModel() {
    String currentModel = getCurrentModel();
    return currentModel != null && !currentModel.isEmpty();
  }

  public String getCurrentModel() {
    String models_dir = getProperty(MODELS_DIR);
    return models_dir != null ? models_dir.replaceAll("models.", "") : "";
  }

  public String getHelpEmail() {
    return getProp(HELP_EMAIL, HELP_EMAIL_DEF);
  }

  public String getDominoServer() {
    return getProp("domino.server", DOMINO_LL_MIT_EDU);
  }

  public List<Affiliation> getAffiliations() {
    return affliations;
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getStartupInfo
   */
  public Map<String, String> getUIProperties() {
    return getPropertyMap(uiprops);
  }

  public Set<Language> getHydra2Languages() {
    String property = props.getProperty(HYDRA_2_LANGUAGES, HYDRA_2_LANGUAGES_DEFAULT).toUpperCase();
    Set<String> strings = new HashSet<>(Arrays.asList(property.split(",")));
    return strings.stream().map(Language::valueOf).collect(Collectors.toSet());
  }

  String getIOSVersion() {
    return props.getProperty(I_OS_VERSION, IOS_VERSION);
  }

  public String getImplementationVersion() {
    return manifest.getOrDefault(IMPL_VERSION, "unset");
  }

  public boolean addUserViaEmail() {
    return getDefaultTrue(ADD_USER_VIA_EMAIL);
  }

  /**
   * @return
   */
  public boolean sendHeartbeat() {
    return getDefaultFalse(SEND_HEARTBEAT);
  }

  public List<String> getHeartbeatRec() {
    String heartbeatRec = props.getProperty(HEARTBEAT_REC1, HEARTBEAT_REC);
    return Arrays.asList(heartbeatRec.split(","));
  }

  public int getHeartbeatPeriod() {
    return getIntPropertyDef(HEARTBEAT_PERIOD, DEFAULT_PERIOD);
  }
}