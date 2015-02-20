package mitll.langtest.server;

import mitll.langtest.server.audio.SLFFile;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This has a lot of overlap with the PropertyHandler set of properties.
 * <p/>
 * TODO : There should be a better way of handling their relationship.
 * <p/>
 * User: GO22670
 * Date: 3/5/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerProperties {
  private static final Logger logger = Logger.getLogger(ServerProperties.class);

  private static final String DEBUG_EMAIL = "debugEmail";
  private static final String TEST_EMAIL = "testEmail";
  private static final String DOIMAGES = "doimages";
  private static final String USE_SCORE_CACHE = "useScoreCache";

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  private static final String H2_DATABASE = "h2Database";
  private static final String H2_DATABASE_DEFAULT = "vlr-parle";   //likely never what you want
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String LANGUAGE = "language";
  /**
   * @deprecated
   */
  private static final String WORDPAIRS = "wordPairs";

  private static final String MEDIA_DIR = "mediaDir";
  private static final String RECO_TEST = "recoTest";
  private static final String RECO_TEST2 = "recoTest2";
  private static final String MIN_PRON_SCORE = "minPronScore";
  private static final String MIN_PRON_SCORE_DEFAULT = "0.20";
  private static final String USE_PREDEFINED_TYPE_ORDER = "usePredefinedTypeOrder";
  private static final String SKIP_SEMICOLONS = "skipSemicolons";
  private static final String EMAIL_ADDRESS = "emailAddress";
  private static final String AUDIO_OFFSET = "audioOffset";
  private static final String MAX_NUM_EXERCISES = "maxNumExercises";
  private static final String MAPPING_FILE = "mappingFile";
  private static final String NO_MODEL = "noModel";
  private static final String TIER_INDEX = "tierIndex";
  private static final String VLR_PARLE_PILOT_ITEMS_TXT = "vlr-parle-pilot-items.txt";
  private static final String QUIET_AUDIO_OK = "quietAudioOK";
  private static final String UNKNOWN_MODEL_BIAS = "unknownModelBias";

  private static final String CONFIG_FILE = "configFile";

  private static final String APPROVAL_EMAIL = "approvalEmail";
  private static final String GORDON_VIDAVER = "gordon.vidaver@ll.mit.edu";
  private static final String DOUG_JONES = "daj@ll.mit.edu";
  private static final String DEFAULT_EMAIL = GORDON_VIDAVER;
  private static final String APPROVERS = "approvers";
  private static final String APPROVER_EMAILS = "approverEmails";
  private static final String ADMINS = "admins";
  private static final String PREFERRED_VOICES = "preferredVoices";
  private static final String REMOVE_EXERCISES_WITH_MISSING_AUDIO = "removeExercisesWithMissingAudio";
  private static final String ENABLE_ALL_USERS = "enableAllUsers";

  private static final List<String> DLI_APPROVERS = Arrays.asList(
      "Tamas",
      "Tamas",
      "David",
      "Sandy",
      "Gordon");

  public static final String TAMAS_1 = "tamas.g.marius.civ@mail.mil";
  public static final String TAMAS_2 = "tamas.marius@dliflc.edu";
  private static final List<String> DLI_EMAILS = Arrays.asList(
      TAMAS_1,
      TAMAS_2,
      "david.randolph@dliflc.edu",
      "sandra.wagner@dliflc.edu",
      GORDON_VIDAVER);

  private static final Set<String> ADMINLIST = new HashSet<String>(Arrays.asList("swade", "gvidaver", "tmarius",
      "drandolph", "swagner", "gmarkovic", "djones", "jmelot", "rbudd", "jray", "jwilliams", "pgatewood"));

  /**
   * Set this property for non-DLI deployments.
   */
  private static final String REPORT_EMAILS = "reportEmails";

  private List<String> REPORT_DEFAULT = Arrays.asList(ServerProperties.TAMAS_1, ServerProperties.TAMAS_2, ServerProperties.GORDON_VIDAVER, ServerProperties.DOUG_JONES);
  private List<String> reportEmails = REPORT_DEFAULT;

  private List<String> approvers = DLI_APPROVERS;
  private List<String> approverEmails = DLI_EMAILS;
  private Set<String> admins = ADMINLIST;

  private Properties props = new Properties();

  private double minPronScore;

  // just for automated testing
  private boolean quietAudioOK;
  private float unknownModelBias;
  private Set<Long> preferredVoices = new HashSet<Long>();

  public ServerProperties(ServletContext servletContext, String configDir) {
    this(servletContext, configDir, servletContext.getInitParameter(CONFIG_FILE));
  }

  public ServerProperties(String configDir, String configFile) {
    this(null, configDir, configFile);
  }

  private ServerProperties(ServletContext servletContext, String configDir, String configFile) {
    String dateFromManifest = getDateFromManifest(servletContext);
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    readProps(configDir, configFile, dateFromManifest);
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
    return props.getProperty(H2_DATABASE, H2_DATABASE_DEFAULT);
  }
  public String getLessonPlan() {
    return props.getProperty("lessonPlanFile", "lesson.plan");
  }

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
  public boolean isTestEmail() {
    return getDefaultFalse(TEST_EMAIL);
  }

  public boolean isWordPairs() {
    return getDefaultFalse(WORDPAIRS);
  }

  public boolean doImages() {
    return getDefaultFalse(DOIMAGES);
  }

  public String getLanguage() {
    return props.getProperty(LANGUAGE);
  }

  //specify this in  in config file like: tierIndex=5,4,-1. That
  //tells us to treat (zero-indexed) column 5 like the "unit" column,
  //column 4 like the "chapter" column, and that there isn't a "week" column
  //--note that depending on how many unique elements are in each column, the 
  //rows that appear on the classroom site may not be in the order 
  //"unit,chapter,week"
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
   * @see LangTestDatabaseImpl#setInstallPath
   */
  public String getMediaDir() {
    return props.getProperty(MEDIA_DIR, "media");
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

  public boolean isNoModel() {
    return getDefaultFalse(NO_MODEL);
  }
  public boolean removeExercisesWithMissingAudio() {
    return getDefaultTrue(REMOVE_EXERCISES_WITH_MISSING_AUDIO);
  }
  public boolean enableAllUsers() {
    return getDefaultFalse(ENABLE_ALL_USERS);
  }
  public String getEmailAddress() {
    return props.getProperty(EMAIL_ADDRESS, DEFAULT_EMAIL);
  }

  public String getApprovalEmailAddress() {
    return props.getProperty(APPROVAL_EMAIL, DEFAULT_EMAIL);
  }

  public int getAudioOffset() {
    try {
      return Integer.parseInt(props.getProperty(AUDIO_OFFSET));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public String getMappingFile() {
    return props.getProperty(MAPPING_FILE, VLR_PARLE_PILOT_ITEMS_TXT);
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
   * <p/>
   * Note that this will only ever be called once.
   * TODO : remember to set the approvers and approver emails properties for new customers
   * TODO : also you may want to set the welcome message to OFF
   * {@link mitll.langtest.client.PropertyHandler#SHOW_WELCOME}
   *
   * Here's where we can over-ride default values.
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

    try {
      unknownModelBias = Float.parseFloat(props.getProperty(UNKNOWN_MODEL_BIAS, ""+SLFFile.UNKNOWN_MODEL_BIAS_CONSTANT));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + UNKNOWN_MODEL_BIAS, e);
      try {
        unknownModelBias = SLFFile.UNKNOWN_MODEL_BIAS_CONSTANT;
      } catch (NumberFormatException e1) {
       logger.error(e1);
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


    property = props.getProperty(APPROVERS);
    if (property != null) approvers = Arrays.asList(property.split(","));

    property = props.getProperty(APPROVER_EMAILS);
    if (property != null) approverEmails = Arrays.asList(property.split(","));

    property = props.getProperty(ADMINS);
    if (property != null) admins = new HashSet<String>(Arrays.asList(property.split(",")));

    property = props.getProperty(REPORT_EMAILS);
    if (property != null) {
      if (property.trim().isEmpty()) reportEmails = Collections.emptyList();
      else reportEmails = Arrays.asList(property.split(","));
    }
  }

  private boolean getDefaultFalse(String param) {
    return props.getProperty(param, "false").equals("true");
  }

  private boolean getDefaultTrue(String param) {
    return props.getProperty(param, "true").equals("true");
  }

  private String getDateFromManifest(ServletContext servletContext) {
    try {
      InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Built-Date");
    } catch (Exception ex) {
//      logger.warn("Error while reading version: " + ex.getMessage());
    }
    return "";
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile(String, String, mitll.langtest.shared.CommonExercise, int, int, int, String, boolean, boolean, boolean, String, String)
   * @return
   */
  public boolean isQuietAudioOK() {
    return quietAudioOK;
  }

  public List<String> getApprovers() {
    return approvers;
  }

  public List<String> getApproverEmails() {
    return approverEmails;
  }

  /**
   * @see mitll.langtest.server.database.UserDAO#UserDAO
   * @return
   */
  public Set<String> getAdmins() {
    return admins;
  }

  public List<String> getReportEmails() { return reportEmails;  }

  public float getUnknownModelBias() {  return unknownModelBias;  }

  public Set<Long> getPreferredVoices() {
    return preferredVoices;
  }
}
