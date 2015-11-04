package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.scoring.CollationSort;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by GO22670 on 1/30/14.
 */
public class DumpRefResultTest {
  private static final Logger logger = Logger.getLogger(DumpRefResultTest.class);

  @BeforeClass
  public static void setup() {
    logger.debug("setup called");
  }

  @Test
  public void dump() {
    List<String> strings = getDBs();
    logger.debug("Got " + strings);

    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      H2Connection connection = getH2(db);

      try {
        BufferedWriter writer = getWriter(db + ".csv");
        writer.write("id,exid," +
            "hydraDecode," +
            "hydraDecodeDur," +
            "hydraAlign," +
            "hydraAlignDur," +
            "hydecDecode," +
            "hydecDecodeDur," +
            "hydecAlign," +
            "hydecAlignDur," +
            "speed,path\n");
        getResults(connection, writer);
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  @Test
  public void testReports() {
    List<String> strings = getDBs();
    logger.debug("Got " + strings);

    List<String> configs = Arrays.asList(
        "dari",
        "egyptian",
        "english",
        "farsi",
        "iraqi",
        "korean",
        "levantine",
        "mandarin",
        "msa",
        "pashto1", "pashto2", "pashto3",

        "russian"

        , "spanish", "sudanese", "tagalog", "urdu"
    );
    int i = 0;
    PathHelper war = new PathHelper("war");

    //  configs = Collections.singletonList("spanish");

    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2(db);

      DatabaseImpl database = getDatabase(connection, config, db);
      database.doReport(war);

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //  break;
    }
  }

  @Test
  public void testYTD() {
    List<String> strings = getDBs();
    logger.debug("Got " + strings);

    List<String> configs = Arrays.asList(
        "dari",
        "egyptian",
        "english",
        "farsi",
        "iraqi",
        "korean",
        "levantine",
        "mandarin",
        "msa",
        "pashto1", "pashto2", "pashto3",

        "russian"

        , "spanish", "sudanese", "tagalog", "urdu"
    );
    int i = 0;
   // PathHelper war = new PathHelper("war");

    //  configs = Collections.singletonList("spanish");
    Map<String, Integer> configToUsers = new TreeMap<>();
    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2(db);

      DatabaseImpl database = getDatabase(connection, config, db);
      int activeUsersYTD = database.getReport().getActiveUsersYTD();
      logger.info(config + "," + activeUsersYTD);
      configToUsers.put(config, activeUsersYTD);

//      try {
//      //  Thread.sleep(1000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
      //  break;
    }
    logger.info(configToUsers);
  }

  private H2Connection getH2(String db) {
    String path = "../Development/asr/performance-reports/dbs/" + db.replaceAll(".h2.db", "");
    logger.debug("got " + path);
    return new H2Connection(".", path, true);
  }

  private List<String> getDBs() {
    String s =
        "npfDari.h2.db\n" +
            "npfClassroomEgyptian.h2.db\n" +
            "npfEnglish.h2.db\n" +
            "npfFarsi.h2.db\n" +
            "iraqi.h2.db\n" +
            "npfLevantine.h2.db\n" +
            "npfKorean.h2.db\n" +
            "mandarin.h2.db\n" +
            "msaClassroom.h2.db\n" +
            "pashtoCE.h2.db\n" +
            "pashto2.h2.db\n" +
            "pashto3.h2.db\n" +
            "npfRussian.h2.db\n"
            +
            "npfSpanish.h2.db\n" +

            "sudaneseToday.h2.db\n" +
            "npfTagalog.h2.db\n" +
            "npfUrdu.h2.db\n";
    String[] split = s.split("\n");
    List<String> strings = Arrays.asList(split);
    //  strings = Collections.singletonList("npfSpanish.h2.db");
    return strings;
  }

  private DatabaseImpl getDatabase(DatabaseConnection connection, String config, String dbName) {
    String quizlet = "quizlet";
    if (config.equals("msa")) quizlet = "classroom";
    else if (config.equals("pashto1")) quizlet = "pashtoQuizlet1";
    else if (config.equals("pashto2")) quizlet = "pashtoQuizlet2";
    else if (config.equals("pashto3")) quizlet = "pashtoQuizlet3";
    String config1 = config.startsWith("pashto") ? "pashto" : config;
    File file = new File("war" + File.separator + "config" + File.separator + config1 + File.separator + quizlet +
        ".properties");
    String parent = file.getParent();
 //   logger.debug("config dir " + parent);
 //   logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    DatabaseImpl database =
        new DatabaseImpl(connection, parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), null);
   // logger.debug("made " + database);
    String media = parent + File.separator + "media";
   // logger.debug("media " + media);
    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    List<CommonExercise> exercises = database.getExercises();
    return database;
  }

//  @Test
//  public void testReport() {
//
//    new Report(userDAO, resultDAO, eventDAO, audioDAO).writeReport(new PathHelper("war"));
//
//  }

  public List<Result> getResults(H2Connection h2, BufferedWriter writer) {
    try {
      String sql = "SELECT * FROM " + "REFRESULT order by " +
          ResultDAO.PRON_SCORE + "," +
          RefResultDAO.ALIGNSCORE + "," +
          RefResultDAO.SPEED;

      List<Result> resultsForQuery = getResultsSQL(sql, h2, writer);
      return resultsForQuery;
    } catch (Exception ee) {
      logger.error("ee " + ee);
    }
    return new ArrayList<Result>();
  }

  private List<Result> getResultsSQL(String sql, H2Connection h2, BufferedWriter writer) throws SQLException, IOException {
    Connection connection = h2.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    logger.debug("running " + sql);
    return getResultsForQuery(connection, statement, writer);
  }

  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement, BufferedWriter writer)
      throws SQLException, IOException {
    ResultSet rs = statement.executeQuery();
    List<Result> results = new ArrayList<Result>();
    int i = 0;
    while (rs.next()) {
      int uniqueID = rs.getInt(ResultDAO.ID);
      String exID = rs.getString(Database.EXID);

      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      long hydraDecodeDur = rs.getLong(RefResultDAO.DECODE_PROCESS_DUR);
      float alignScore = rs.getFloat(RefResultDAO.ALIGNSCORE);
      long hydraAlignDur = rs.getLong(RefResultDAO.ALIGN_PROCESS_DUR);

      float hpronScore = rs.getFloat(RefResultDAO.HYDEC_DECODE_PRON_SCORE);
      long hydecDecodeDur = rs.getLong(RefResultDAO.HYDEC_DECODE_PROCESS_DUR);

      float halignScore = rs.getFloat(RefResultDAO.HYDEC_ALIGN_PRON_SCORE);
      long hydecAlignDur = rs.getLong(RefResultDAO.HYDEC_ALIGN_PROCESS_DUR);

      String path = rs.getString(ResultDAO.ANSWER);
      String speed = rs.getString(RefResultDAO.SPEED);

      writer.write(uniqueID + "," + exID + "," +
          pronScore + "," +
          hydraDecodeDur + "," +
          alignScore + "," +
          hydraAlignDur + "," +

          hpronScore + "," +
          hydecDecodeDur + "," +

          halignScore + "," +
          hydecAlignDur + "," +
          speed +
          "," + path +
          "\n");
      i++;
    }
    finish(connection, statement, rs);
    logger.debug("wrote " + i);
    return results;
  }

  protected void finish(Connection connection, Statement statement, ResultSet rs) throws SQLException {
    rs.close();
    statement.close();
  }


  private BufferedWriter getWriter(String prefix) throws IOException {
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
}
