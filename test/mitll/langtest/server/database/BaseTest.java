package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.shared.exercise.CommonExercise;
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

  protected static DatabaseImpl<CommonExercise> getDatabase(String config) {
    String s = "quizlet.properties";

    return getDatabaseWithConfig(config, s);
  }

  protected static DatabaseImpl<CommonExercise> getDatabaseWithConfig(String config, String properties) {
    String rootDir = "war";
    File file = new File(rootDir + File.separator + "config" + File.separator + config + File.separator + properties);
    String parent = file.getParent();
    String name = file.getName();

    ServerProperties serverProps = new ServerProperties(parent, name);
    DatabaseImpl<CommonExercise> database = new DatabaseImpl<CommonExercise>(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper(rootDir), false, null);
    database.setInstallPath(rootDir, parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
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
   * @see ReportAllTest#testReports()
   * @see ReportAllTest#testYTD()
   * @param connection
   * @param config
   * @param dbName
   * @return
   */
  protected DatabaseImpl getDatabase(DatabaseConnection connection, String config, String dbName) {
    File file = getPropertiesFile(config);
    String parent = file.getParent();
    DatabaseImpl database =
        new DatabaseImpl(connection, parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), null);
    // logger.debug("made " + database);

    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    database.getExercises();
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
    return new H2Connection(".", path, true,null);
  }
}
