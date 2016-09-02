package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/5/15.
 */
public class BaseTest {
  private static final Logger logger = Logger.getLogger(BaseTest.class);

  protected static DatabaseImpl getDatabase(String config) {
    return getDatabase(config, false);
  }

  protected static DatabaseImpl getDatabase(String config, boolean useH2) {
    String installPath = "war";
    return getDatabase(installPath, config, useH2);
  }

  private static DatabaseImpl getDatabase(String installPath, String config, boolean useH2) {
    File file = new File(installPath + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, name);
    serverProps.setH2(useH2);
    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(),
        serverProps, new PathHelper(installPath), false, null, false);

    database.setInstallPath(installPath, parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    // database.setDependencies(mediaDir, installPath);
    return database;
  }

  protected static DatabaseImpl getDatabaseLight(String config, boolean useH2) {
    String installPath = "war";
    String s = "quizlet.properties";
    File file = new File(installPath + File.separator + "config" + File.separator + config + File.separator + s);
    ServerProperties serverProps = getServerProperties(config, s);
    DatabaseImpl database = getDatabaseVeryLight(config, s, useH2);
    database.setInstallPath(installPath,
        file.getParentFile().getAbsolutePath() + File.separator + database.getServerProps().getLessonPlan(), serverProps.getMediaDir());
    return database;
  }

  /**
   * @param config
   * @param useH2
   * @paramx host
   * @paramx user
   * @paramx pass
   * @param optPropsFile
   * @return
   * @see mitll.langtest.server.database.postgres.PostgresTest#testCopy
   */
  protected static DatabaseImpl getDatabaseLight(String config,
                                                 boolean useH2,
                                                 boolean useLocal,
                                                 String optPropsFile) {

    logger.info("db " + config + " props " + optPropsFile);

    String installPath = "war";
    String propsFile = optPropsFile != null ? optPropsFile : "quizlet.properties";

    logger.info("db " + config + " props " + propsFile);

    File file = new File(installPath + File.separator + "config" + File.separator + config + File.separator + propsFile);

    logger.info("path " + file.getAbsolutePath());

    ServerProperties serverProps = getServerProperties(config, propsFile);

    if (useLocal) {
      serverProps.setLocalPostgres();
    }
    else {
      serverProps.setHydraPostgres();
    }
//    serverProps.getProps().setProperty("databaseHost", host);
//    serverProps.getProps().setProperty("databaseUser", user);
//    serverProps.getProps().setProperty("databasePassword", pass);

    serverProps.setH2(useH2);

    String parent = file.getParentFile().getAbsolutePath();
    String name = file.getName();

    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper("war"), false, null, false);

    database.setInstallPath(installPath,
        file.getParentFile().getAbsolutePath() + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());

    return database;
  }

  protected static ServerProperties getServerProperties(String config, String propsFile) {
    // String s = "quizlet.properties";
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + propsFile);
    return new ServerProperties(file.getParentFile().getAbsolutePath(), file.getName());
  }

  protected static DatabaseImpl getDatabaseVeryLight(String config, String propsFile, boolean useH2) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + propsFile);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, name);
    serverProps.setH2(useH2);
    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper("war"), false, null, false);
    return database;
  }

  protected void finish(Statement statement, ResultSet rs) throws SQLException {
    rs.close();
    statement.close();
  }

  protected BufferedWriter getWriter(String prefix) throws IOException {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy_HH_mm_ss");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(new PathHelper("war"), today, prefix);
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
   * @param connection
   * @param config
   * @param dbName
   * @return
   * @see ReportAllTest#testReports()
   * @see ReportAllTest#testYTD()
   */
  protected DatabaseImpl getDatabase(DatabaseConnection connection, String config, String dbName) {
    File file = getPropertiesFile(config);
    String parent = file.getParent();
    DatabaseImpl database =
        new DatabaseImpl(connection, parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), null);
    // logger.debug("made " + database);

    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    database.getExercises(-1);
    return database;
  }

  protected File getPropertiesFile(String config) {
    String quizlet = "quizlet";
    if (config.equals("msa")) quizlet = "classroom";
    else if (config.equals("pashto1")) quizlet = "pashtoQuizlet1";
    else if (config.equals("pashto2")) quizlet = "pashtoQuizlet2";
    else if (config.equals("pashto3")) quizlet = "pashtoQuizlet3";
    String config1 = config.startsWith("pashto") ? "pashto" : config;
    return new File("war" + File.separator + "config" + File.separator + config1 + File.separator + quizlet +
        ".properties");
  }

  protected H2Connection getH2Connection(String path) {
    return new H2Connection(".", path, true, null, false);
  }
}
