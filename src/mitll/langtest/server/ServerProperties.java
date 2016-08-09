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

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.mail.EmailList;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.User;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.*;
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
  private static final Logger logger = Logger.getLogger(ServerProperties.class);

  private static final String FALSE = "false";
  private static final String TRUE = "true";

  /**
   * TODO : read this from a config file, or maybe from the sites.json file on np
   */
  private static final List<String> SITE_LIST = Arrays.asList(
      "Dari",
      "Egyptian",
      "English",
      "Farsi",
      "German",
      "Korean",
      "Iraqi",
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

  private static final List<String> AMAS_SITES =
      Arrays.asList("Dari", "Farsi", "Korean", "Mandarin", "MSA", "Pashto", "Russian", "Spanish", "Urdu");


  public static final String MIRA_DEVEL_HOST = "mira-devel.llan.ll.mit.edu/scorer/item"; //"mira-devel.llan.ll.mit.edu/msa/item";
  private static final String MIRA_DEVEL = "https://" + MIRA_DEVEL_HOST;
  private static final String MIRA_LEN = "https://mira.ll.mit.edu/scorer/item";
  private static final String MIRA_DEFAULT = MIRA_LEN;
  private static final String MIRA_CLASSIFIER_URL = "miraClassifierURL";

//  private static final String WEBSERVICE_HOST_IP1 = "webserviceHostIP";
//  private static final String WEBSERVICE_HOST_PORT = "webserviceHostPort";
  private static final String LESSON_PLAN_FILE = "lessonPlanFile";
  private static final String USE_MYSQL = "useMYSQL";
  private static final String USE_H_2 = "useH2";
  private static final String USE_POSTGRE_SQL = "usePostgreSQL";
  private static final String USE_ORM = "useORM";
  private static final String TYPE_ORDER = "typeOrder";
  private static final String FONT_FAMILY = "fontFamily";
  private static final String SLEEP_BETWEEN_DECODES_MILLIS = "sleepBetweenDecodesMillis";
  public static final String MODELS_DIR = "MODELS_DIR";
  private String miraClassifierURL = MIRA_DEVEL;// MIRA_LEN; //MIRA_DEVEL;

  /**
   * I.e. the hydra web service for ASR
   */
//  private static final String WEBSERVICE_HOST_IP = "127.0.0.1";
  private static final String USE_SCORE_CACHE = "useScoreCache";

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  private static final String H2_DATABASE = "h2Database";
  private static final String LANGUAGE = "language";

  private static final String MEDIA_DIR = "mediaDir";
  //  private static final String RECO_TEST = "recoTest";
//  private static final String RECO_TEST2 = "recoTest2";
  private static final String MIN_PRON_SCORE = "minPronScore";
  private static final String MIN_PRON_SCORE_DEFAULT = "" + 0.31f;
  private static final String USE_PREDEFINED_TYPE_ORDER = "usePredefinedTypeOrder";
  private static final String SKIP_SEMICOLONS = "skipSemicolons";
  private static final String AUDIO_OFFSET = "audioOffset";
  private static final String MAX_NUM_EXERCISES = "maxNumExercises";
  private static final String NO_MODEL = "noModel";
  private static final String TIER_INDEX = "tierIndex";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";
  private static final String CONFIG_FILE = "configFile";

  private static final String PREFERRED_VOICES = "preferredVoices";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";
  private static final String ENABLE_ALL_USERS = "enableAllUsers";
  private static final String DO_DECODE = "dodecode";
  private static final String DO_TRIM = "dotrim";

  private static final String USE_PHONE_TO_DISPLAY = "usePhoneToDisplay";
  private static final String ADD_MISSING_INFO = "addMissingInfo";
  private static final int MIN_DYNAMIC_RANGE_DEFAULT = 20; // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final int SLEEP_BETWEEN_DECODES_DEFAULT = 100; // Paul Gatewood 11/24/15 : The bottom line is we should set the minimum Dynamic Range threshold to 20dB for NetProf users
  private static final String MIN_DYNAMIC_RANGE = "minDynamicRange";
  private static final String RUN_REF_DECODE_WITH_HYDEC = "runRefDecodeWithHydec";
  private static final String BEST_AUDIO = "bestAudio";

  private Properties props = new Properties();

  private double minPronScore;

  // just for automated testing
  private boolean quietAudioOK;
  private final Set<Long> preferredVoices = new HashSet<Long>();
  private EmailList emailList;
  private final int userInitialScores = 20;
  //  private boolean RTL;
  private String fontFamily;
  private String fontFaceURL;
  //private boolean RTL;
/*
  private int sleepBetweenDecodes;
  private long trimBeforeAndAfter;
  private long trimBefore;
  private long trimAfter;
*/

  public static List<String> CORE_PROPERTIES = Arrays.asList(
      ServerProperties.MODELS_DIR,
      "N_OUTPUT",
      "N_HIDDEN",
      "webserviceHostPort"
  );

  /**
   * @param servletContext
   * @param configDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties
   */
  public ServerProperties(ServletContext servletContext, String configDir) {
    this(servletContext, configDir, servletContext.getInitParameter(CONFIG_FILE));
  }

  /**
   * @param configDir
   * @param configFile
   * @seex mitll.langtest.server.database.ImportCourseExamples#makeDatabaseImpl
   */
  public ServerProperties(String configDir, String configFile) {
    this(null, configDir, configFile);
  }

  private ServerProperties(ServletContext servletContext, String configDir, String configFile) {
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    readProps(configDir, configFile, getDateFromManifest(servletContext));
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
        props = new Properties();
        props.load(new FileInputStream(configFileFullPath));
        emailList = new EmailList(props);
        readProperties(dateFromManifest);
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#readProperties(javax.servlet.ServletContext)
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

  @Deprecated  public String getLanguage() {
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
   * @return
   * @Deprecated each project will have it's types and type order
   */
  public Collection<String> getTypes() {
    String property = props.getProperty(TYPE_ORDER, "Unit,Chapter");
    return Arrays.asList(property.split(","));
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#setInstallPath
   */
  public String getMediaDir() {
    return props.getProperty(MEDIA_DIR, BEST_AUDIO);
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

  @Deprecated
  public boolean isNoModel() {
    return getDefaultFalse(NO_MODEL);
  }

  boolean removeExercisesWithMissingAudio() {
    return getDefaultTrue(REMOVE_EXERCISES_WITH_MISSING_AUDIO);
  }

  public boolean enableAllUsers() {
    return getDefaultFalse(ENABLE_ALL_USERS);
  }

  public boolean shouldDoDecode() {
    return getDefaultFalse(DO_DECODE);
  }

  public boolean isAMAS() {
    return getDefaultFalse("isAMAS");
  }

  /**
   * @return
   * @see mitll.langtest.server.decoder.RefResultDecoder#doRefDecode(Collection, String)
   */
  public boolean shouldTrimAudio() {
    return getDefaultTrue(DO_TRIM);
  }

  public int getAudioOffset() {
    return getIntProperty(AUDIO_OFFSET);
  }

  private int getIntProperty(String audioOffset) {
    try {
      return Integer.parseInt(props.getProperty(audioOffset));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private int getIntPropertyDef(String audioOffset, String defaultValue) {
    try {
      return Integer.parseInt(props.getProperty(audioOffset, defaultValue));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public int getMaxNumExercises() {
    int maxNumExercises = Integer.MAX_VALUE;
    String maxNumExercises1 = MAX_NUM_EXERCISES;
    try {
      String property = props.getProperty(maxNumExercises1);
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
    Map<String, String> kv = new HashMap<String, String>();
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
   * TODO : remember to set the approvers and approver emails properties for new customers
   * TODO : also you may want to set the welcome message to OFF
   * {@link mitll.langtest.client.PropertyHandler#SHOW_WELCOME}
   * <p>
   * Here's where we can over-ride default values.
   *
   * @param dateFromManifest
   * @see
   */
  private void readProperties(String dateFromManifest) {
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      props.setProperty("releaseDate", dateFromManifest);
    }

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

    String property = props.getProperty(PREFERRED_VOICES);
    if (property != null) {
      for (String userid : property.split(",")) {
        try {
          preferredVoices.add(Long.parseLong(userid));
          logger.info("pref users " + preferredVoices);
        } catch (NumberFormatException e) {
          logger.error("couldn't parse userid " + userid);
        }
      }
    }
    miraClassifierURL = props.getProperty(MIRA_CLASSIFIER_URL, MIRA_DEFAULT);

    props.put("scoringModel", props.getProperty(MODELS_DIR, ""));

    String lessonPlan = getLessonPlan();
    if (lessonPlan != null && lessonPlan.startsWith("http")) props.setProperty("talksToDomino", TRUE);

    if (getFontFamily() != null) {
      props.setProperty(FONT_FAMILY, getFontFamily());
      logger.info(FONT_FAMILY +
          "=" + getFontFamily() + " : " + props.getProperty(FONT_FAMILY));
    }
  }

  private boolean getDefaultFalse(String param) {
    return props.getProperty(param, FALSE).equals(TRUE);
  }

  private boolean getDefaultTrue(String param) {
    return props.getProperty(param, TRUE).equals(TRUE);
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

  private String getDateFromManifest(ServletContext servletContext) {
    try {
      InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Built-Date");
    } catch (Exception ex) {
    }
    return "";
  }

  /**
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   */
  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  public Set<Long> getPreferredVoices() {
    return preferredVoices;
  }

  public boolean shouldDropRefResult() {
    return getDefaultFalse("dropRefResultTable");
  }

  private final Map<String, String> phoneToDisplay = new HashMap<>();

  public Map<String, String> getPhoneToDisplay() {
    return phoneToDisplay;
  }

  /**
   * @param phone
   * @return
   * @see ScoreServlet#getJsonForScore(PretestScore, boolean)
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

  public boolean addMissingInfo() {
    return getDefaultFalse(ADD_MISSING_INFO);
  }

  // EMAIL ------------------------

  private EmailList getEmailList() {
    return emailList;
  }

  public boolean isDebugEMail() {
    return getEmailList().isDebugEMail();
  }

  public boolean isTestEmail() {
    return getEmailList().isTestEmail();
  }

  public String getEmailAddress() {
    return emailList.getEmailAddress();
  }

  public String getApprovalEmailAddress() {
    return emailList.getApprovalEmailAddress();
  }

  /**
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#addContentDeveloper(String, String, User, MailSupport, String)
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String, String)
   */
  public List<String> getApprovers() {
    return emailList.getApprovers();
  }

  public List<String> getApproverEmails() {
    return emailList.getApproverEmails();
  }

  /**
   * @return
   * @see UserDAO#UserDAO
   */
  public Set<String> getAdmins() {
    Set<String> strings = Collections.emptySet();
    return emailList == null ? strings : emailList.getAdmins();
  }

  public List<String> getReportEmails() {
    return emailList.getReportEmails();
  }

  public int getUserInitialScores() {
    return userInitialScores;
  }

  public int getMinDynamicRange() {
    return getIntPropertyDef(MIN_DYNAMIC_RANGE, "" + MIN_DYNAMIC_RANGE_DEFAULT);
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

  public Collection<String> getSites() {
    return isAMAS() ? AMAS_SITES : SITE_LIST;
  }

  public boolean useMYSQL() {
    return getDefaultFalse(USE_MYSQL);
  }

  public boolean useH2() {
    return getDefaultFalse(USE_H_2);
  }

  public void setH2(boolean val) {
    props.setProperty(USE_H_2, Boolean.valueOf(val).toString());
  }

  public boolean usePostgres() {
    return getDefaultFalse(USE_POSTGRE_SQL);
  }

  public boolean useORM() {
    return getDefaultTrue(USE_ORM);
  }

  public void setRTL(boolean isRTL) {
    props.setProperty("rtl", isRTL ? "true" : "false");
  }

  public void setFontFamily(String fontNames) {
    this.fontFamily = fontNames;
    props.setProperty(FONT_FAMILY, fontNames);
  }

  private String getFontFamily() {
    return fontFamily;
  }

  public void setFontFaceURL(String fontFaceURL) {
    this.fontFaceURL = fontFaceURL;
  }

  public String getFontFaceURL() {
    return fontFaceURL;
  }

  /**
   * Something like : "https://domino-devel/dominoNP/attach/"
   *
   * @return
   */
  public String getAudioAttachPrefix() {
    return props.getProperty("audioAttachPrefix");//,"https://domino-devel/dominoNP/attach/");
  }

  public int getSleepBetweenDecodes() {
    return getIntPropertyDef(SLEEP_BETWEEN_DECODES_MILLIS, "" + SLEEP_BETWEEN_DECODES_DEFAULT);
  }

  private static final long TRIM_SILENCE_BEFORE = 300;
  private static final long TRIM_SILENCE_AFTER = 300;

  public long getTrimBefore() {
    return getIntPropertyDef("trimBeforeMillis", "" + TRIM_SILENCE_BEFORE);
  }

  public long getTrimAfter()  {
    return getIntPropertyDef("trimAfterMillis", "" + TRIM_SILENCE_AFTER);
  }

  public String getDatabaseType() {
    return props.getProperty("databaseType", "postgresql");
  }

  public String getDatabaseHost() {
    return props.getProperty("databaseHost", "localhost");
  }

  public int getDatabasePort()    { return getIntPropertyDef("databasePort", "5432"); }

  public String getDatabaseName() {
    return props.getProperty("databaseName", "netprof");
  }

  public String getDatabaseUser() {
    return props.getProperty("databaseUser", "postgres");
  }

  public String getDatabasePassword() {
    return props.getProperty("databasePassword", "pgadmin");
  }

  public Properties getProps() {
    return props;
  }
}
