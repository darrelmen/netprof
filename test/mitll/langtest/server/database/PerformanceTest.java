package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.analysis.*;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by GO22670 on 1/30/14.
 */
public class PerformanceTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PerformanceTest.class);
  public static final int CHINESE_DUDE = 71;
  private static DatabaseImpl database;
  private static String dbName;

/*  @BeforeClass
  public static void setup() {
    logger.debug("setup called");

    //  String config = "mandarinClassroom";//"spanish";//"mandarin";
    String config = "spanish";//"mandarin";
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    //  dbName = "mandarinCopy";// "npfSpanish";//"mandarin";// "mandarin";
    dbName = "npfSpanish";// "npfSpanish";//"mandarin";// "mandarin";
    database = new DatabaseImpl(parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), false, null);
    logger.debug("made " + database);
    String media = parent + File.separator + "media";
    logger.debug("media " + media);
    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    List<CommonExercise> exercises = database.getExercises();
  }*/

  @Test
  public void testBest() {
    try {
      BufferedWriter writer = getWriter("bestScore");
      int chineseDude = CHINESE_DUDE;
      //int id = 71;
      int id = 107;

      //int id = 1;//116;
/*
      List<BestScore> resultForUser = database.getAnalysis().getResultForUser(id);
      Collections.sort(resultForUser, new Comparator<BestScore>() {
        @Override
        public int compare(BestScore o1, BestScore o2) {
          return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
        }
      });
      for (BestScore bestScore : resultForUser) {
        logger.info("got " + bestScore);
        writer.write(bestScore.toCSV() + "\n");
      }
      writer.close();
*/

/*      UserPerformance performance = database.getAnalysis().getResultForUserByBin(71, ResultDAO.FIVE_MINUTES);
      writer = getWriter("UserPerformance_5_Min");
      writer.write(performance.toCSV());
      writer.close();


      performance = database.getAnalysis().getResultForUserByBin(71, ResultDAO.HOUR);
      writer = getWriter("UserPerformance_Hour");
      writer.write(performance.toCSV());
      writer.close();

      performance = database.getAnalysis().getResultForUserByBin(71, ResultDAO.DAY);
      writer = getWriter("UserPerformance_Day");
      writer.write(performance.toCSV());
      writer.close();*/


      UserPerformance performance = database.getAnalysis().getPerformanceForUser(id, 1);
      writer = getWriter("RawUserPerformance");
      writer.write(performance.toRawCSV());
      writer.close();

      List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(id, 1);
      for (WordScore ws : wordScoresForUser) logger.info("got " + ws);

    } catch (IOException e) {
    }
  }

  @Test
  public void testSpecific() {
    String path = "npfSpanish";//.replaceAll(".h2.db", "");

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection,"spanish",path);
    database.getAnalysis().getPerformanceForUser(107, 1);
  }


  @Test
  public void testJennifer() {
    String path = "npfUrdu";
    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection,"urdu",path);
    Analysis analysis = database.getAnalysis();
    UserPerformance performanceForUser = analysis.getPerformanceForUser(117, 1);
    logger.info("perf "+ performanceForUser);

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(117, 1);

    for(WordScore ws : wordScoresForUser) logger.debug("Got " + ws);
  }

  @Test
  public void testMissingInfo() {
    String path = "npfSpanish";//.replaceAll(".h2.db", "");

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection,"spanish",path);
    List<Result> resultsToDecode = database.getResultDAO().getResultsToDecode();
   // for (Result res : resultsToDecode) logger.info("Got " + res);
  }

  @Test
  public void testWords() {
    // int id = 71;
    int id = 26;
    List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(id, 1);
    for (WordScore ws : wordScoresForUser) logger.info("testWords got " + ws);
  }

  @Test
  public void testPhones() {
    long then = System.currentTimeMillis();
    //   int id = 71; // psanish
    //int id = 41; // big mandarin classroom user
    int id = 71;
    PhoneReport phoneReport = database.getAnalysis().getPhonesForUser(id, 1);

    Map<String, List<WordAndScore>> phonesForUser = phoneReport.getPhoneToWordAndScoreSorted();
    long now = System.currentTimeMillis();

    logger.info("took " + (now - then) + " to get " + phonesForUser);

    if (!phonesForUser.isEmpty()) {
      for (Map.Entry<String, List<WordAndScore>> pair : phonesForUser.entrySet()) {
        List<WordAndScore> value = pair.getValue();
        logger.info(pair.getKey() + " = " + value.size() + " first " + value.get(0));
        int i1 = Math.min(10, value.size());
        for (int i = 0; i < i1; i++) logger.info("\t" + value.get(i));
      }
    }
//    for(WordScore ws : wordScoresForUser) logger.info("got " + ws);
  }

  @Test
  public void testUsers() {
    long then = System.currentTimeMillis();
    //   int id = 71; // psanish
    //int id = 41; // big mandarin classroom user
//    int id = 71;
    List<UserInfo> userInfo = database.getAnalysis().getUserInfo(database.getUserDAO(), 5);
    // PhoneReport phoneReport = (PhoneReport) userInfo;

    // Map<String, List<WordAndScore>> phonesForUser = phoneReport.getPhoneToWordAndScoreSorted();
    long now = System.currentTimeMillis();

    logger.info("took " + (now - then) + " to get " + userInfo.size());

    if (!userInfo.isEmpty()) {
      for (UserInfo userInfo1 : userInfo) {
        logger.info("got " + userInfo1);
        //   List<WordAndScore> value = pair.getValue();
        // logger.info(pair.getKey() + " = " + value.size() + " first " + value.get(0));
        // int i1 = Math.min(10, value.size());
        // for (int i = 0; i < i1; i++) logger.info("\t" + value.get(i));
      }
    }
//    for(WordScore ws : wordScoresForUser) logger.info("got " + ws);
  }

  /*
  private BufferedWriter getWriter(String prefix) throws IOException {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy_HH_mm_ss");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(new PathHelper("war"), today, prefix);
    logger.info("writing to " + file.getAbsolutePath());
    return new BufferedWriter(new FileWriter(file));
  }*/

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
