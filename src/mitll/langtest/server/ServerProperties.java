package mitll.langtest.server;

import mitll.langtest.server.database.DatabaseImpl;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerProperties {
  public static final String SHOW_SECTIONS = "showSections";
  public static final String DEBUG_EMAIL = "debugEmail";
  private static final String DOIMAGES = "doimages";
  private static Logger logger = Logger.getLogger(ServerProperties.class);

  private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
  public static final String FIRST_N_IN_ORDER = "firstNInOrder";
  private static final String DATA_COLLECT_MODE = "dataCollect";
  private static final String COLLECT_AUDIO = "collectAudio";
  private static final String COLLECT_AUDIO_DEFAULT = "true";
  private static final String BIAS_TOWARDS_UNANSWERED = "biasTowardsUnanswered";
  private static final String USE_OUTSIDE_RESULT_COUNTS = "useOutsideResultCounts";
  private static final String OUTSIDE_FILE = "outsideFile";
  private static final String OUTSIDE_FILE_DEFAULT = "distributions.txt";
  private static final String H2_DATABASE = "h2Database";
  private static final String H2_DATABASE_DEFAULT = "vlr-parle";
  private static final String URDU = "urdu";
  private static final String READ_FROM_FILE = "readFromFile";
  private static final String FLASHCARD = "flashcard";
  private static final String CRTDATACOLLECT = "crtDataCollect";
  private static final String LANGUAGE = "language";
  private static final String WORDPAIRS = "wordPairs";
  private static final String AUTOCRT = "autocrt";

  private Properties props = null;

  public boolean dataCollectMode;
  public boolean collectAudio;
  public boolean biasTowardsUnanswered, useOutsideResultCounts;
  public String outsideFile;
  public boolean isUrdu;
  public boolean isWordPairs;
  public int firstNInOrder;
  public boolean isDataCollectAdminView;

  public void readPropertiesFile(ServletContext servletContext, String configDir) {
   String configFile = servletContext.getInitParameter("configFile");
    if (configFile == null) configFile = DEFAULT_PROPERTIES_FILE;
    String configFileFullPath = configDir + File.separator + configFile;
    if (!new File(configFileFullPath).exists()) {
      logger.error("couldn't find config file " + new File(configFileFullPath));
    } else {
      try {
        props = new Properties();
        props.load(new FileInputStream(configFileFullPath));
        readProperties(servletContext);
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  public boolean getUseFile() {
    return getDefaultFalse(READ_FROM_FILE);
  }

  public String getH2Database() { return props.getProperty(H2_DATABASE, H2_DATABASE_DEFAULT); }
  public String getLessonPlan() { return props.getProperty("lessonPlanFile", "lesson.plan"); }

  public boolean isShowSections() {
    return getDefaultFalse(SHOW_SECTIONS);
  }

  public boolean isDebugEMail() {
    return getDefaultFalse(DEBUG_EMAIL);
  }
  public boolean isFlashcard() {
    return getDefaultFalse(FLASHCARD);
  }

  public boolean isCRTDataCollect() {
    return getDefaultFalse(CRTDATACOLLECT);
  }

  public boolean isWordPairs() {
    return getDefaultFalse(WORDPAIRS);
  }

  public boolean doImages() {
    return getDefaultFalse(DOIMAGES);
  }

  public boolean isAutoCRT() {
    return getDefaultFalse(AUTOCRT);
  }

  public String getLanguage() {
    return props.getProperty(LANGUAGE, "English");
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * @return
   */
  public Map<String, String> getProperties() {
    Map<String,String> kv = new HashMap<String, String>();
    for (Object prop : props.keySet()) {
      String sp = (String)prop;
      kv.put(sp,props.getProperty(sp).trim());
    }
    //logger.debug("for config " + relativeConfigDir + " prop file has " + kv.size() + " properties : " + props.keySet());
    return kv;
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   *
   * Note that this will only ever be called once.
   * @see
   * @param servletContext
   */
  private void readProperties(ServletContext servletContext) {
    try {
      firstNInOrder = Integer.parseInt(props.getProperty(FIRST_N_IN_ORDER, "" + Integer.MAX_VALUE));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + FIRST_N_IN_ORDER,e);
      firstNInOrder = Integer.MAX_VALUE;
    }
    dataCollectMode = getDefaultFalse(DATA_COLLECT_MODE);
    collectAudio = !props.getProperty(COLLECT_AUDIO, COLLECT_AUDIO_DEFAULT).equals("false");
    isUrdu = getDefaultFalse(URDU);
    biasTowardsUnanswered = getDefaultTrue(BIAS_TOWARDS_UNANSWERED);
    useOutsideResultCounts = getDefaultTrue(USE_OUTSIDE_RESULT_COUNTS);
    isDataCollectAdminView = getDefaultFalse("dataCollectAdminView");
    outsideFile = props.getProperty(OUTSIDE_FILE, OUTSIDE_FILE_DEFAULT);
    String dateFromManifest = getDateFromManifest(servletContext);
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      logger.debug("Date from manifest " + dateFromManifest);
      props.setProperty("releaseDate",dateFromManifest);
    }
  }

  private boolean getDefaultFalse(String param) {
    return !props.getProperty(param, "false").equals("false");
  }

  private boolean getDefaultTrue(String param) {
    return !props.getProperty(param, "true").equals("false");
  }

  private String getDateFromManifest(ServletContext servletContext) {
    InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");

    try {
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      return attributes.getValue("Built-Date");
    }
    catch(Exception ex) {
//      logger.warn("Error while reading version: " + ex.getMessage());
    }
    return "";
  }
}
