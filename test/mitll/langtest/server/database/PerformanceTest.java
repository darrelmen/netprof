package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.analysis.*;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by GO22670 on 1/30/14.
 */
public class PerformanceTest {
  private static final Logger logger = Logger.getLogger(PerformanceTest.class);
  private static DatabaseImpl database;
  private static String dbName;

  @BeforeClass
  public static void setup() {
    logger.debug("setup called");

    String config = "spanish";//"mandarin";
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    dbName = "npfSpanish";//"mandarin";// "mandarin";
    database = new DatabaseImpl(parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), false, null);
    logger.debug("made " + database);
    String media = parent + File.separator + "media";
    logger.debug("media " + media);
    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    List<CommonExercise> exercises = database.getExercises();
  }

  @Test
  public void testBest() {
    try {
      BufferedWriter writer = getWriter("bestScore");
      List<BestScore> resultForUser = database.getAnalysis().getResultForUser(71);
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

      UserPerformance performance = database.getAnalysis().getResultForUserByBin(71, ResultDAO.FIVE_MINUTES);
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
      writer.close();


      performance = database.getAnalysis().getPerformanceForUser(71);
      writer = getWriter("RawUserPerformance");
      writer.write(performance.toRawCSV());
      writer.close();

      List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(71);
      for (WordScore ws : wordScoresForUser) logger.info("got " + ws);

    } catch (IOException e) {


    }
  }

  @Test
  public void testWords() {
    List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(71);
    for (WordScore ws : wordScoresForUser) logger.info("got " + ws);
  }

  @Test
  public void testPhones() {
    long then = System.currentTimeMillis();
    PhoneReport phoneReport = database.getAnalysis().getPhonesForUser(71);

    Map<String, List<WordAndScore>> phonesForUser = phoneReport.getPhoneToWordAndScoreSorted();
    long now = System.currentTimeMillis();

    logger.info("took " + (now - then) + " to get " + phonesForUser);

    for (Map.Entry<String, List<WordAndScore>> pair : phonesForUser.entrySet()) {
      List<WordAndScore> value = pair.getValue();
      logger.info(pair.getKey() + " = " + value.size() + " first " + value.get(0));
      for (int i = 0; i < 10; i++) logger.info("\t" + value.get(i));
    }
//    for(WordScore ws : wordScoresForUser) logger.info("got " + ws);
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
