package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.H2Connection;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import mitll.langtest.server.database.copy.CopyToPostgres;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/5/15.
 */
public class BaseTest {
  private static final Logger logger = LogManager.getLogger(BaseTest.class);
  private static final String QUIZLET_PROPERTIES = "quizlet.properties";
  public static final String DOMINO_PROPERTIES = "domino.properties";

  protected static DatabaseImpl getH2Database(String config) {
    return getDatabase(config, true);
  }
  protected static DatabaseImpl getDatabase(String config, boolean useH2) {
    return getDatabase("war", config, useH2);
  }

  private static DatabaseImpl getDatabase(String installPath, String config, boolean useH2) {
    File file = new File(installPath + File.separator + "config" + File.separator + config + File.separator + QUIZLET_PROPERTIES);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, name);
    serverProps.setH2(useH2);
    DatabaseImpl database = new DatabaseImpl(serverProps, new PathHelper(installPath, serverProps), null, null);
    database.setInstallPath(parent + File.separator + database.getServerProps().getLessonPlan());

    return database;
  }

  protected static DatabaseImpl getDatabaseLight(String config, boolean useH2) {
    String installPath = "war";
    String s = QUIZLET_PROPERTIES;
    File file = new File(installPath + File.separator + "config" + File.separator + config + File.separator + s);
    DatabaseImpl database = getDatabaseVeryLight(config, s, useH2);
    database.setInstallPath(
        file.getParentFile().getAbsolutePath() + File.separator + database.getServerProps().getLessonPlan());
    return database;
  }

  /**
   * @param config
   * @param useH2
   * @param optPropsFile
   * @return
   * @paramx host
   * @paramx user
   * @paramx pass
   * @see mitll.langtest.server.database.postgres.PostgresTest#testCopy
   */
  protected static DatabaseImpl getDatabaseLight(String config,
                                                 boolean useH2,
                                                 boolean useLocal,
                                                 String optPropsFile) {
    return CopyToPostgres.getDatabaseLight(config, useH2, useLocal, optPropsFile, "war", "config");
  }

  protected static DatabaseImpl getDatabaseVeryLight(String config, String propsFile, boolean useH2) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + propsFile);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, name);
    serverProps.setH2(useH2);
    return getDatabase(serverProps);
  }

  @NotNull
  private static DatabaseImpl getDatabase(ServerProperties serverProps) {
    DatabaseImpl database = new DatabaseImpl(serverProps,
        new PathHelper("war", serverProps), null, null);
    return database;
  }

  protected static DatabaseImpl getDatabase() {  return getDatabase(getProps());  }

  protected static ServerProperties getProps() {
    File file = new File("/opt/netprof/config/netprof.properties");
    String parent = file.getParentFile().getAbsolutePath();
    return new ServerProperties(parent, file.getName());
  }

  void finish(Statement statement, ResultSet rs) throws SQLException {
    rs.close();
    statement.close();
  }

  BufferedWriter getWriter(String prefix) throws IOException {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy_HH_mm_ss");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(new PathHelper("war", null), today, prefix);
    logger.info("writing to " + file.getAbsolutePath());
    return new BufferedWriter(new FileWriter(file));
  }

  private File getReportFile(PathHelper pathHelper, String today, String prefix) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = prefix + "_report_" + today + ".csv";
    return new File(reports, fileName);
  }

  /**
   * @param config
   * @return
   * @see ReportAllTest#testReports()
   * @see ReportAllTest#testYTD()
   */
  protected static DatabaseImpl getDatabase(String config) {
    File file = getPropertiesFile(config);
    String parent = file.getParent();
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    DatabaseImpl database = getDatabase(serverProps);
    database.setInstallPath(parent + File.separator + database.getServerProps().getLessonPlan());
    database.getExercises(-1, false);
    return database;
  }

  private static File getPropertiesFile(String config) {
    String quizlet = "quizlet";
    if (config.equals("msa")) quizlet = "classroom";
    else if (config.equals("pashto1")) quizlet = "pashtoQuizlet1";
    else if (config.equals("pashto2")) quizlet = "pashtoQuizlet2";
    else if (config.equals("pashto3")) quizlet = "pashtoQuizlet3";
    String config1 = config.startsWith("pashto") ? "pashto" : config;
    return new File("war" + File.separator + "config" + File.separator + config1 + File.separator + quizlet +
        ".properties");
  }

  H2Connection getH2Connection(String path) {
    return new H2Connection(".", path, true, null, false);
  }

  protected DatabaseImpl getAndPopulate() {
    return getDatabase().setInstallPath("").populateProjects(-1);
  }
}
