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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.mail.EmailList;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.shared.user.Affiliation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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

  private static final String APP_TITLE = "appTitle";
  private static final String APP_URL = "app.url";

  private static final String FALSE = "false";
  private static final String TRUE = "true";
/*
  private static final List<String> AMAS_SITES =
      Arrays.asList("Dari", "Farsi", "Korean", "Mandarin", "MSA", "Pashto", "Russian", "Spanish", "Urdu");
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

  private static final String LESSON_PLAN_FILE = "lessonPlanFile";
  private static final String USE_H_2 = "useH2";

  private static final String SLEEP_BETWEEN_DECODES_MILLIS = "sleepBetweenDecodesMillis";
  public static final String MODELS_DIR = "MODELS_DIR";
  private static final String DB_CONFIG = "dbConfig";
  private static final String POSTGRES_HYDRA = "postgresHydra";
  private static final String POSTGRES = "postgres";
  //public static final String CONFIG = "config";
  //public static final String CONFIG_JSON = "config.json";

  /**
   * @see #useProperties
   */
  private static final String UI_PROPERTIES = "ui.properties";
  private static final String CONFIG_FILE1 = "config.file";
  private static final String ANALYSIS_INITIAL_SCORES = "analysisInitialScores";
  private static final String ANALYSIS_NUM_FINAL_AVERAGE_SCORES = "analysisNumFinalScores";
  private static final String APPLICATION_CONF = "/opt/netprof/config/application.conf";
  private static final String RELEASE_DATE = "releaseDate";

  @Deprecated
  private String miraClassifierURL = MIRA_DEVEL;// MIRA_LEN; //MIRA_DEVEL;
  @Deprecated
  private static final String NP_SERVER = "np.ll.mit.edu";
  private static final String HYDRA_HOST_URL_DEFAULT = "https://netprof1-dev.llan.ll.mit.edu/netprof/";

  private static final String USE_SCORE_CACHE = "useScoreCache";

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  private static final String H2_DATABASE = "h2Database";
  private static final String LANGUAGE = "language";

  private static final String MEDIA_DIR = "mediaDir";
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

  private static final String USE_PHONE_TO_DISPLAY = "usePhoneToDisplay";

  private static final int MIN_DYNAMIC_RANGE_DEFAULT = 24; // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final int SLEEP_BETWEEN_DECODES_DEFAULT = 100; // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final String MIN_DYNAMIC_RANGE = "minDynamicRange";
  private static final String RUN_REF_DECODE_WITH_HYDEC = "runRefDecodeWithHydec";
  private static final String CHECK_AUDIO_ON_STARTUP = "checkAudioOnStartup";
  private static final String CHECK_AUDIO_FILE_EXISTS = "checkAudioFileExists";
  private static final String DO_AUDIO_CHECKS_IN_PRODUCTION = "doAudioChecksInProduction";
  private static final String CHECK_AUDIO_TRANSCRIPT = "checkAudioTranscript";
  private static final String MIN_ANALYSIS_SCORE = "minAnalysisScore";
  private static final String HYDRA_HOST = "hydraHost";

  private static final int MIN_SCORE_TO_SHOW = 20;// 0.20f;
  private static final int USER_INITIAL_SCORES = 20;
  private static final int USER_FINAL_SCORES = 30;

  /**
   * Note netprof is all lower case.
   */
  private static final String DEFAULT_NETPROF_AUDIO_DIR = "/opt/netprof/";
  private static final String DEFAULT_DCODR_DIR = "/opt/dcodr/";
  public static final String BEST_AUDIO = "bestAudio";
  private static final String ANSWERS = "answers";

  private Properties props = new Properties();
  private Properties uiprops = new Properties();

  private double minPronScore;

  // just for automated testing
  private boolean quietAudioOK;

  /**
   * @deprecated - need preferred voices per project...
   */
  private final Set<Integer> preferredVoices = new HashSet<>();
  private EmailList emailList;
  private final Map<String, String> phoneToDisplay = new HashMap<>();
  private List<Affiliation> affliations = new ArrayList<>();
  public static final String WEBSERVICE_HOST_PORT = "webserviceHostPort";

  /**
   * @see mitll.langtest.server.database.copy.CreateProject#createProject
   */
  public static List<String> CORE_PROPERTIES = Arrays.asList(
      ServerProperties.MODELS_DIR,
      WEBSERVICE_HOST_PORT
  );
  private String configFileFullPath;

  private final Set<String> lincoln = new HashSet<>(Arrays.asList(
      "gvidaver",
      "rbudd",
      "jmelot",
      "esalesky",
      "gatewood",
      "testing",
      "grading",
      "fullperm",
      //"0001abcd",
      "egodoy",
      "rb2rb2",
      "dajone3",
      //"WagnerSandy",
      "rbtrbt"));

  public ServerProperties() {
  }

  private Map<String, String> manifest = new HashMap<>();

  /**
   * @param props
   * @param configDir
   * @see mitll.langtest.server.property.ServerInitializationManagerNetProf#getServerProperties
   */
  public ServerProperties(Properties props,
                          Map<String, String> manifest,
                          File configDir) {
    this.props = props;
    this.manifest = manifest;

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
    readProps(configDir, configFile, "");//getDateFromManifest(servletContext));
    readPhonemeMap(configDir);
  }

  /**
   * @param configDir
   * @param configFile
   * @param dateFromManifest
   */
  private void readProps(String configDir, String configFile, String dateFromManifest) {
    String configFileFullPath = configDir + File.separator + configFile;
    if (!new File(configFileFullPath).exists()) {
      logger.error("couldn't find config file " + new File(configFileFullPath));
    } else {
      try {
        props = readPropertiesFromFile(configFileFullPath);
        useProperties(new File(configDir), dateFromManifest);
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
    return props.getProperty(LESSON_PLAN_FILE, "");//, props.getProperty(LANGUAGE) + ".json");
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

  public boolean isLaptop() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getHostName().contains("MITLL");
    } catch (UnknownHostException e) {
      return false;
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

/*
  private int getIntProperty(String audioOffset) {
    try {
      return Integer.parseInt(props.getProperty(audioOffset));
    } catch (NumberFormatException e) {
      return 0;
    }
  }
*/

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

    props.put("scoringModel", props.getProperty(MODELS_DIR, ""));

    String lessonPlan = getLessonPlan();
    if (lessonPlan != null && lessonPlan.startsWith("http")) props.setProperty("talksToDomino", TRUE);

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

  private String getProperty(String prop) {
    return props.getProperty(prop);
  }

  /**
   * if true, use old school (hydec)
   * OR if there is no webservice port specified
   *
   * @return true if only use old school hydec decoder
   */
  public boolean getOldSchoolService() {
    return Boolean.parseBoolean(props.getProperty("oldSchoolService", FALSE));// || props.getProperty("webserviceHostPort") == null;
  }

/*  private String getDateFromManifest(ServletContext servletContext) {
    try {
      InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Built-Date");
    } catch (Exception ex) {
    }
    return "";
  }*/

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

  /**
   * Should be a per-project option.
   *
   * @return
   */
  @Deprecated
  public boolean shouldDropRefResult() {
    return getDefaultFalse("dropRefResultTable");
  }


  public Map<String, String> getPhoneToDisplay() {
    return phoneToDisplay;
  }

  /**
   * @param phone
   * @return
   * @see mitll.langtest.server.audio.ScoreToJSON#getJsonForScore
   */
  public String getDisplayPhoneme(String phone) {
    String s = phoneToDisplay.get(phone);
    if (s == null) return phone;
    else return s;
  }

  /**
   * @return
   * @see DatabaseImpl#getContextPractice()
   */
  public String getDialogFile() {
    return props.getProperty("dialog");
  }

  private void readPhonemeMap(String configDir) {
    String phonemeMapping = props.getProperty("phonemeMapping");

    if (phonemeMapping == null) {
      //   logger.debug("no phoneme mapping file property");
      return;
    }

    File file1 = new File(configDir + File.separator + phonemeMapping);

    if (!file1.exists()) {
      logger.error("couldn't find phoneme mapping file " + file1);
      return;
    }

    FileReader file;
    String line;
    try {
      file = new FileReader(file1);
      BufferedReader reader = new BufferedReader(file);

      /*line =*/
      reader.readLine(); // skip header

      while ((line = reader.readLine()) != null) {
        String[] split = line.split("\\s++");
        if (split.length == 2) {
          String key = split[0].trim();
          String value = split[1].trim();
          phoneToDisplay.put(key, value);
        }
      }
      reader.close();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public boolean usePhoneToDisplay() {
    return getDefaultFalse(USE_PHONE_TO_DISPLAY);
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

  public int getUserInitialScores() {
    return getIntPropertyDef(ANALYSIS_INITIAL_SCORES, USER_INITIAL_SCORES);
  }

  public int getUserFinalScores() {
    return getIntPropertyDef(ANALYSIS_NUM_FINAL_AVERAGE_SCORES, USER_FINAL_SCORES);
  }

  public int getMinDynamicRange() {
    return getIntPropertyDef(MIN_DYNAMIC_RANGE, MIN_DYNAMIC_RANGE_DEFAULT);
  }

  /**
   * @return 0-1 float
   */
  public float getMinAnalysisScore() {
    int intPropertyDef = getIntPropertyDef(MIN_ANALYSIS_SCORE, MIN_SCORE_TO_SHOW);
    return (float) intPropertyDef / 100f;
  }

  /**
   * true if you want to compare hydec scores with hydra scores for reference audio
   *
   * @return
   */
  public boolean shouldDoDecodeWithHydec() {
    return getDefaultFalse(RUN_REF_DECODE_WITH_HYDEC);
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
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#setAudioDAO(IAudioDAO, int)
   */
  public boolean doAudioFileExistsCheck() {
    return getDefaultFalse(CHECK_AUDIO_FILE_EXISTS);
  }

  public boolean doAudioChecksInProduction() {
    return getDefaultTrue(DO_AUDIO_CHECKS_IN_PRODUCTION);
  }

  public int getSleepBetweenDecodes() {
    return getIntPropertyDef(SLEEP_BETWEEN_DECODES_MILLIS, SLEEP_BETWEEN_DECODES_DEFAULT);
  }

  private static final int TRIM_SILENCE_BEFORE = 300;
  private static final int TRIM_SILENCE_AFTER = 300;

  public long getTrimBefore() {
    return getIntPropertyDef("trimBeforeMillis", TRIM_SILENCE_BEFORE);
  }

  public long getTrimAfter() {
    return getIntPropertyDef("trimAfterMillis", TRIM_SILENCE_AFTER);
  }

  public String getDBConfig() {
    return props.getProperty(DB_CONFIG, POSTGRES_HYDRA);
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
    return props.getProperty(APP_TITLE, "netprof");
  }

  /**
   * Not what you want probably...
   *
   * @return
   * @see mitll.langtest.server.rest.RestUserManagement#addUser
   * @deprecated
   */
  public String getAppURL() {
    return props.getProperty(APP_URL, "https://" + getNPServer() + "/" + "netProf");
  }

  /**
   * @return
   * @see
   * @see mitll.langtest.server.mail.EmailHelper#EmailHelper(ServerProperties, IUserDAO, MailSupport, PathHelper)
   * @deprecated
   */
  public String getNPServer() {
    return props.getProperty("SERVER_NAME", NP_SERVER);
  }

  /**
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#checkForWebservice
   */
  public String getHydraHost() {
    return props.getProperty(HYDRA_HOST, HYDRA_HOST_URL_DEFAULT);
  }

  /**
   * @return
   * @deprecated
   */
  public boolean hasModel() {
    return getCurrentModel() != null;
  }

  public String getCurrentModel() {
    String models_dir = getProperty("MODELS_DIR");
    return models_dir != null ? models_dir.replaceAll("models.", "") : "";
  }

  public List<Affiliation> getAffiliations() {
    return affliations;
  }

  public Set<String> getLincolnPeople() {
    return lincoln;
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getStartupInfo
   */
  public Map<String, String> getUIProperties() {
    return getPropertyMap(uiprops);
  }
}
