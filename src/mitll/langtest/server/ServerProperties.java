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

  private Properties props = null;

  public boolean dataCollectMode;
  public boolean collectAudio;
  public boolean biasTowardsUnanswered, useOutsideResultCounts;
  public String outsideFile;
  public boolean isUrdu;
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
      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  public boolean getUseFile() {
    return !props.getProperty(READ_FROM_FILE, "false").equals("false");
  }

  public String getH2Database() { return props.getProperty(H2_DATABASE, H2_DATABASE_DEFAULT); }

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
  public void readProperties(ServletContext servletContext) {
    try {
      firstNInOrder = Integer.parseInt(props.getProperty(FIRST_N_IN_ORDER, "" + Integer.MAX_VALUE));
    } catch (NumberFormatException e) {
      logger.error("Couldn't parse property " + FIRST_N_IN_ORDER,e);
      firstNInOrder = Integer.MAX_VALUE;
    }
    dataCollectMode = !props.getProperty(DATA_COLLECT_MODE, "false").equals("false");
    collectAudio = !props.getProperty(COLLECT_AUDIO, COLLECT_AUDIO_DEFAULT).equals("false");
    isUrdu = !props.getProperty(URDU, "false").equals("false");
    biasTowardsUnanswered = !props.getProperty(BIAS_TOWARDS_UNANSWERED, "true").equals("false");
    useOutsideResultCounts = !props.getProperty(USE_OUTSIDE_RESULT_COUNTS, "true").equals("false");
    isDataCollectAdminView = !props.getProperty("dataCollectAdminView", "false").equals("false");
    outsideFile = props.getProperty(OUTSIDE_FILE, OUTSIDE_FILE_DEFAULT);
    String dateFromManifest = getDateFromManifest(servletContext);
    if (dateFromManifest != null && dateFromManifest.length() > 0) {
      logger.debug("Date from manifest " + dateFromManifest);
      props.setProperty("releaseDate",dateFromManifest);
    }
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
