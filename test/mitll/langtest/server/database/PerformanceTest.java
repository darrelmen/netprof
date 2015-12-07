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
import java.util.List;
import java.util.Map;

/**
 * Created by GO22670 on 1/30/14.
 */
public class PerformanceTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PerformanceTest.class);
  public static final int CHINESE_DUDE = 71;

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


      String path = "../dbs/"+"npfSpanish";//.replaceAll(".h2.db", "");

      H2Connection connection = getH2Connection(path);
      DatabaseImpl database = getDatabase(connection, "spanish", path);

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
  public void testDavidSilver() {
    String path = "../dbs/"+"npfSpanish";//.replaceAll(".h2.db", "");

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, "spanish", path);
    int id = 71;
    //int id = 535;   // tiffany
    UserPerformance performanceForUser = database.getAnalysis().getPerformanceForUser(id, 1);

    logger.info("perf " +performanceForUser);
    Map<Long, List<PhoneSession>> granularityToSessions = performanceForUser.getGranularityToSessions();
    if (granularityToSessions == null) logger.error("no sesions?");
    else {
      logger.info("perf " + granularityToSessions.keySet());
      logger.info("perf " + granularityToSessions.values());
    }
   // List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(id, 1);

    PhoneReport report = database.getAnalysis().getPhonesForUser(id, 1);

    Map<String, PhoneStats> phoneToAvgSorted = report.getPhoneToAvgSorted();
    if (phoneToAvgSorted == null) {
      logger.info("huh? phoneToAvgSorted is null ");
    } else {
      float itotal = 0;
      float iwtotal = 0;
      float total = 0;
      float wtotal = 0;
      int c = 0;
      for (Map.Entry<String, PhoneStats> ps : phoneToAvgSorted.entrySet()) {
        PhoneStats value = ps.getValue();
        logger.warn("got " + value);
     //   logger.info(ps.getKey() + ",\t"+  value.getInitial() +",\t"+ value.getCurrent() +",\t"+ value.getDiff()+",\t" +value.getCount());
        itotal += value.getInitial();
        iwtotal += value.getCount()*value.getInitial();

        total += value.getCurrent();
        wtotal += value.getCount()*value.getCurrent();
        c += value.getCount();

        int totalExamples = 0;
        for (PhoneSession session : value.getSessions()) {
          totalExamples += session.getExamples().size();
        }
        logger.info("for " +ps.getKey() + " got " +totalExamples);
      }
      logger.info("i total avg " + (itotal/phoneToAvgSorted.size()));
      logger.info("c total avg " + (total/phoneToAvgSorted.size()));


      logger.info("i w total avg " + (iwtotal/c));
      logger.info("c w total avg " + (wtotal/c));


    }

//    for (Map.Entry<String, List<WordAndScore>> pair : report.getPhoneToWordAndScoreSorted().entrySet()) {
//      StringBuilder builder = new StringBuilder();
//
//      for (WordAndScore wordAndScore : pair.getValue()) builder.append(wordAndScore.getScore() +", ");
//      logger.info(pair.getKey() + " " + builder);
//    }
  }

  @Test
  public void testJennifer() {
    String path = "npfUrdu";
    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, "urdu", path);
    Analysis analysis = database.getAnalysis();
    // int id = 117;
    int id = 104;
    UserPerformance performanceForUser = analysis.getPerformanceForUser(id, 1);
    logger.info("perf " + performanceForUser);

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(id, 1);


//    for (WordScore ws : wordScoresForUser) {
//      logger.debug("Got " + ws);
//    }
  }

  @Test
  public void testAlexander() {
    String path = "../dbs/"+"msaClassroom";
    String urdu = "msa";

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, urdu, path);
    Analysis analysis = database.getAnalysis();
    // int id = 117;
    int id = 285;
    UserPerformance performanceForUser = analysis.getPerformanceForUser(id, 1);
    logger.info("perf " + performanceForUser);
    logger.info("perf " + performanceForUser.getGranularityToSessions());

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(id, 1);

    for (WordScore ws : wordScoresForUser) logger.debug("Got " + ws);
  }

  @Test
  public void testKarateFighter() {
    String path = "../dbs/"+"msaClassroom";
    String urdu = "msa";

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, urdu, path);
    Analysis analysis = database.getAnalysis();
    // int id = 117;
    int id = 171;
    UserPerformance performanceForUser = analysis.getPerformanceForUser(id, 1);
    logger.info("perf " + performanceForUser);

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(id, 1);

    logger.info("got " +wordScoresForUser.size());

   // for (WordScore ws : wordScoresForUser) logger.debug("Got " + ws);
  }

  @Test
  public void testMissingInfo() {
    String path = "npfSpanish";//.replaceAll(".h2.db", "");
    String spanish = "spanish";

    path = "../dbs/"+ "msaClassroom";
    spanish = "msa";

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, spanish, path);
    List<Result> resultsToDecode = database.getResultDAO().getResultsToDecode();

    logger.info("results " + resultsToDecode.size());
    int count = 0;
    for (Result res : resultsToDecode) {
      //if (res.getAnswer().contains("best")) {
        logger.info("Got " + res);
        if (count++ > 1000) break;
     // }
    }
  }

  @Test
  public void testMissingUrdu() {
    String path = "npfUrdu";//.replaceAll(".h2.db", "");
    String urdu = "urdu";

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, urdu, path);
    List<Result> resultsToDecode = database.getResultDAO().getResultsToDecode();

    logger.info("results " + resultsToDecode.size());
    int count = 0;
    for (Result res : resultsToDecode) {
      //if (res.getAnswer().contains("best")) {
        logger.info("Got " + res);
       // if (count++ > 100) break;
     // }
    }
  }

  @Test
  public void testLizzy() {
    String path = "npfUrdu";//.replaceAll(".h2.db", "");
    String urdu = "urdu";

    H2Connection connection = getH2Connection(path);
    DatabaseImpl database = getDatabase(connection, urdu, path);
    Analysis analysis = database.getAnalysis();
    // int id = 117;
    int id = 162;
    UserPerformance performanceForUser = analysis.getPerformanceForUser(id, 1);
    logger.info("perf " + performanceForUser);

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(id, 1);

    for (WordScore ws : wordScoresForUser) logger.debug("Got WordScore " + ws);
  }

  @Test
  public void testEgyptian() {
    String path = "../dbs/"+"npfClassroomEgyptian";//.replaceAll(".h2.db", "");
    String urdu = "egyptian";

    DatabaseImpl database = getDatabase(getH2Connection(path), urdu, path);
    Analysis analysis = database.getAnalysis();

    List<UserInfo> userInfo = analysis.getUserInfo(database.getUserDAO(), 5);
    for (UserInfo userInfo1 : userInfo) logger.warn("Got " + userInfo1);
  }

  @Test
  public void testWords() {
    // int id = 71;
    int id = 26;
    DatabaseImpl database = getSpanishDatabase();
    List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(id, 1);
    for (WordScore ws : wordScoresForUser) logger.info("testWords got " + ws);
  }

  public DatabaseImpl getSpanishDatabase() {
    String path = "../dbs/"+"npfSpanish";//.replaceAll(".h2.db", "");
    return getDatabase(getH2Connection(path), "spanish", path);
  }

  @Test
  public void testPhones() {
    long then = System.currentTimeMillis();
    //   int id = 71; // psanish
    //int id = 41; // big mandarin classroom user
    int id = 71;
    DatabaseImpl database = getSpanishDatabase();
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
    DatabaseImpl database = getSpanishDatabase();

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
