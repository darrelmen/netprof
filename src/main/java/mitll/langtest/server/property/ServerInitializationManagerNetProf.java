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
 */

package mitll.langtest.server.property;


import mitll.hlt.domino.shared.util.DateFormatter;
import mitll.hlt.domino.shared.util.GWTDateFormatter;
import mitll.langtest.server.ServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * Created by go22670 on 1/9/17.
 */
public class ServerInitializationManagerNetProf {
  private static final Logger log = LogManager.getLogger();

  private static final String appName = "netprof";

  private static final boolean DEBUG = false;
  private static final String DEFAULT_PROPERTY_HOME =
      File.separator + "opt" +
          File.separator + appName +
          File.separator + "config";

  /**
   * The name of the config file attribute optionally passed in as -D.
   */
  private static final String CONFIG_FILE_ATTR_NM = appName + ".cfg.file";

  /**
   * The name of the config home directory attribute from the web.xml or -D.
   */
  private static final String CONFIG_HOME_ATTR_NM = appName + ".cfg.home";

  /**
   * The default properties filename
   */
  private static final String DEFAULT_PROPS_FN = appName + ".properties";
  private static final String UNKNOWN1 = "Unknown";
  public static final String UNKNOWN = UNKNOWN1;

 /**
   * @param newContext
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties
   */
  public ServerProperties getServerProps(ServletContext newContext) {
    DateFormatter.init(new GWTDateFormatter());

    ServerProperties props;
    try {
      InputStream in = findServerProperties(newContext);
      props = getServerProperties(in, newContext);
    } catch (Exception e) {
      log.error("getServerProps : trying to read props - got " + e, e);
      props = new ServerProperties();
    }
    return props;
  }

  /**
   * Determine the location to load Domino properties from.
   * The earliest value found is used. In general values can be either a resource on the classpath, or
   * an absolute file.<br/>
   * <ol>
   * <li>A java -D argument -Ddomino.config='domino.properties'. This is assumed to be the exact file/resource name (and path)</li>
   * <li>A java -D argument -Dconfig.home='/opt/domino/config/'. This is assumed to be the folder
   * containing the file. The filename will be determined based on the name
   * of the web application (domino.properties, domino-ilr.properties), or
   * default to domino.properties</li>
   * <li>The servlet context attribute value of config.home. This is assumed to be the folder
   * containing the file. The filename will be determined based on the name
   * of the web application (domino.properties, domino-ilr.properties)</li>
   * <li>The default domino.properties. This is usually located under src/resources</li>
   * </ol>
   *
   * @return An InputStream that contains the properties, or null if the file
   * can't be found.
   */
  private InputStream findServerProperties(ServletContext ctx) {
    if (DEBUG) log.info("Determining properties file.");

    // first try for -Dconfig.file
    String fullCFN = System.getProperty(CONFIG_FILE_ATTR_NM);
    InputStream in = null;
    if (fullCFN != null) {
      log.info("Properties file provided with -D" + CONFIG_FILE_ATTR_NM +
          ": '" + fullCFN + "'");
      in = openPropertiesFileStream(fullCFN);
      if (in != null) {
        return in;
      } else {
        log.warn("Could not open stream for file " + fullCFN);
      }
    }

    // next try for -Dconfig.home and generate name
    in = buildConfigNameAndOpenFile(ctx, System.getProperty(CONFIG_HOME_ATTR_NM));
    if (in != null) {
      log.info("Try to open properties file provided with -D" + CONFIG_HOME_ATTR_NM +
          ": '" + System.getProperty(CONFIG_HOME_ATTR_NM) + "'");
      return in;
    }


    // next try for web.xml config.home and generate name
    if (ctx != null) {
      in = buildConfigNameAndOpenFile(ctx, ctx.getInitParameter(CONFIG_HOME_ATTR_NM));
      if (in != null) {
        log.info("Try to open properties file provided with web.xml: '" +
            System.getProperty(CONFIG_HOME_ATTR_NM) + "'");
        return in;
      }
    }

    // finally use default.
    in = buildConfigNameAndOpenFile(ctx, DEFAULT_PROPERTY_HOME);
    if (DEBUG) log.info("Using default properties file!");
    return in;
  }
/*
  public String getConfigLocation(ServletContext ctx) {
    String fullCFN = System.getProperty(CONFIG_FILE_ATTR_NM);

    if (fullCFN != null) {
      log.info("Properties file provided with -D" + CONFIG_FILE_ATTR_NM +
          ": '" + fullCFN + "'");
   return  fullCFN;
    }

    // next try for -Dconfig.home and generate name
    in = buildConfigNameAndOpenFile(ctx, System.getProperty(CONFIG_HOME_ATTR_NM));
    if (in != null) {
      log.info("Try to open properties file provided with -D" + CONFIG_HOME_ATTR_NM +
          ": '" + System.getProperty(CONFIG_HOME_ATTR_NM) + "'");
      return in;
    }


    // next try for web.xml config.home and generate name
    if (ctx != null) {
      in = buildConfigNameAndOpenFile(ctx, ctx.getInitParameter(CONFIG_HOME_ATTR_NM));
      if (in != null) {
        log.info("Try to open properties file provided with web.xml: '" +
            System.getProperty(CONFIG_HOME_ATTR_NM) + "'");
        return in;
      }
    }

    // finally use default.
    in = buildConfigNameAndOpenFile(ctx, DEFAULT_PROPERTY_HOME);
  }*/

  /**
   * Build the config file name based on the provided home. Returns null if no home is provided
   */
  private InputStream buildConfigNameAndOpenFile(ServletContext ctx, String cfHome) {
    if (cfHome == null) {
      return null;
    }
    if (cfHome.length() > 0 && !cfHome.endsWith("/")) {
      cfHome += "/";
    }
    String cfn = DEFAULT_PROPS_FN;
    if (ctx != null && ctx.getContextPath() != null && ctx.getContextPath().length() > 1) {
      cfn = ctx.getContextPath();
      cfn = cfn.substring(1, cfn.length()) + ".properties"; // remove initial '/'
    }
    String fullCFN = cfHome + cfn;
    if (DEBUG) log.info("Trying to use properties file: " + fullCFN);
    return openPropertiesFileStream(fullCFN);
  }

  private ServerProperties getServerProperties(InputStream propsIS, ServletContext ctx) {
    if (DEBUG) log.info("getServerProperties : Initializing Properties");
    Properties props = readPropertiesStream(propsIS);

    if (props != null) {
      if (props.isEmpty()) {
        log.error("\n\n\ngetServerProperties : huh? server props is empty?\n\n\n");
      } else {
        if (DEBUG) log.debug("getServerProperties : Loaded " + props.size() + " properties");
      }

      String releaseVers = UNKNOWN;
      String buildUser = UNKNOWN;
      String buildVers = UNKNOWN;
      String buildDate = UNKNOWN;

      Attributes atts = (ctx != null) ? getManifestAttributes(ctx) : null;
      if (atts != null) {
        releaseVers = atts.getValue(Name.SPECIFICATION_VERSION);
        buildVers = atts.getValue(Name.IMPLEMENTATION_VERSION);
        buildUser = atts.getValue("Built-By");
        buildDate = atts.getValue("Built-Date");
      } else {

        if (DEBUG) {
          log.warn("getServerProperties Did not load attribute information. Are you running in " +
              "a servlet container? Context:" + ctx);
        }
      }

      return new ServerProperties(props, releaseVers, buildUser, buildVers, buildDate, configDir);
    }
    return null;
  }

  /**
   * @param in
   * @return
   */
  private Properties readPropertiesStream(InputStream in) {
    try {
      if (in != null) {
        Properties props = new Properties();
        props.load(in);
        if (DEBUG) log.debug("readPropertiesStream " + props);
        return props;
      } else {
        log.warn("Could not find " + appName + " config file! Initialization failure! " + in);
      }
    } catch (Exception e) {
      log.error("Exceptino processing properties file!", e);
    }
    return null;
  }

  private File configDir = null;

  private InputStream openPropertiesFileStream(String configFileName) {
    InputStream in = ServerInitializationManagerNetProf.class.getClassLoader().getResourceAsStream(configFileName);
    if (DEBUG) log.info("Attempted to get input stream for " + configFileName +
        " as resource. Result: " + in);
    if (in == null) {
      try {
        File file = new File(configFileName);
        configDir = file.getParentFile();
        in = FileUtils.openInputStream(file);
        if (DEBUG) log.info("Attempted to get input stream for " + configFileName +
            " as file. Result: " + in);
      } catch (Exception ex) {
        log.warn("Exception when opening file " + configFileName, ex);
        in = null;
      }
    }
    if (in == null) {
      if (DEBUG) log.warn("Could not open properties file: " + configFileName);
    }
    return in;
  }

  private Attributes getManifestAttributes(ServletContext ctx) {
    InputStream in = ctx.getResourceAsStream("/META-INF/MANIFEST.MF");
    if (in != null) {
      try {
        Manifest manifest = new Manifest(in);
        return manifest.getMainAttributes();
      } catch (Exception ex) {
        log.warn("Error while reading manifest", ex);
      }
    } else {
      if (DEBUG) log.warn("Could not find manifest!");
    }
    return null;
  }

  public File getConfigDir() {
    return configDir;
  }
}
