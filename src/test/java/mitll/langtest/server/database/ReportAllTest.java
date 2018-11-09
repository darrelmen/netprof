package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.exercise.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class ReportAllTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ReportAllTest.class);
  public static final boolean DO_ONE = false;

/*
  protected static DatabaseImpl getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    logger.debug("config dir " + parent + " config     " + name);
    ServerProperties serverProps = new ServerProperties(parent, name);
    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war"), false, null);
    // logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    return database;
  }
*/

  @Test
  public void testTrunc() {
    String test = "/answers/spanish/answers/plan/1945/0/subject-83/answer_1474213851511.wav";
    System.out.println(removeAnswers(test));
  }

  private static final String ANSWERS = "answers";

  @NotNull
  private String removeAnswers(String fileToFind) {
    int answers = fileToFind.indexOf(ANSWERS);
    if (answers != -1) {
      fileToFind = fileToFind.substring(answers+ANSWERS.length());

      answers = fileToFind.indexOf(ANSWERS);
      if (answers != -1) {
        fileToFind = fileToFind.substring(answers);
      }
      logger.info("getUserForFile test " + fileToFind);
    }
    return fileToFind;
  }

  @Test
  public void testMaleFemaleRefCoverageSudanese() {
    int i = 0;
    List<String> configs = Collections.singletonList("sudanese");
    for (String db : Arrays.asList("newSudanese2")) {
      String config = configs.get(i++);

      H2Connection connection = getH2Connection("war/config/sudanese/" + db);
      DatabaseImpl database = getDatabase(config);
      Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(-1);

      logger.info(maleFemaleProgress.toString());

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //  break;
    }
  }

  @Test
  public void testMaleFemaleRefCoverageEgyptian() {
    DatabaseImpl database = getDatabase();
    Project egyptian = database.getProjectByName("egyptian");
    Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(egyptian.getID());
    logger.info(maleFemaleProgress.toString());
  }

  @Test
  public void testMaleFemaleRefCoverage() {
    int i = 0;
    List<String> configs = Collections.singletonList("russian");
    for (String db : Arrays.asList("npfRussian")) {
      String config = configs.get(i++);

      H2Connection connection = getH2Connection("war/config/russian/" + db);
      DatabaseImpl database = getDatabase(config);
      Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(-1);

      logger.info(maleFemaleProgress.toString());

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //  break;
    }
  }


  @Test
  public void testReport() {
    List<String> strings = getDBs();
    logger.debug("Got " + strings);

    List<String> configs = Arrays.asList();
    int i = 0;
    PathHelper war = new PathHelper("war", null);

    configs = Collections.singletonList("english");
    for (String db : Arrays.asList("npfEnglish")) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2Connection("war/config/english/" + db);

      DatabaseImpl database = getDatabase(config);
      database.doReportForYear(2015);

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //  break;
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
    PathHelper war = new PathHelper("war", null);

    if (DO_ONE) {
      configs = Collections.singletonList("spanish");
    }
    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2(db);

      DatabaseImpl database = getDatabase(config);
      database.doReportForYear(2016);

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

    //   configs = Collections.singletonList("spanish");
    Map<String, Integer> configToUsers = new TreeMap<>();
    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2(db);

      DatabaseImpl database = getDatabase(config);
      int activeUsersYTD = 0;//database.getReport(config).getActiveUsersYTD();
      logger.info(config + "," + activeUsersYTD);
      configToUsers.put(config, activeUsersYTD);
    }
    logger.info(configToUsers);
  }

  private H2Connection getH2(String db) {
    String path = "../dbs/" + db.replaceAll(".h2.db", "");
    logger.debug("got " + path);
    return getH2Connection(path);
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
            "npfRussian.h2.db\n" +
            "npfSpanish.h2.db\n" +
            "sudaneseToday.h2.db\n" +
            "npfTagalog.h2.db\n" +
            "npfUrdu.h2.db\n";
    String[] split = s.split("\n");
    List<String> strings = Arrays.asList(split);
    if (DO_ONE) {
      strings = Collections.singletonList("npfSpanish.h2.db");
    }
    return strings;
  }
}
