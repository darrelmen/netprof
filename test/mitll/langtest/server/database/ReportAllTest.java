package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class ReportAllTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(ReportAllTest.class);
  public static final boolean DO_ONE = true;

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
  public void testMaleFemaleRefCoverageSudanese() {
    int i = 0;
    List<String> configs = Collections.singletonList("sudanese");
    for (String db : Arrays.asList("newSudanese2")) {
      String config = configs.get(i++);

      H2Connection connection = getH2Connection("war/config/sudanese/" + db);
      DatabaseImpl database = getDatabase(connection, config, db);
      Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress();

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
    logProgress("egyptian");
  }

  @Test
  public void testMaleFemaleRefCoverageFarsi() {
    logProgress("farsi");
  }

  @Test
  public void testMaleFemaleRefCoverageKorean() {
    logProgress("korean");
  }

  @Test
  public void testMaleFemaleRefCoverageMandarin() {
    logProgress("mandarin");
  }

  @Test
  public void testMaleFemaleRefCoverageLevantine() {
    logProgress("levantine");
  }

  private void logProgress(String mandarin) {
    Map<String, Float> maleFemaleProgress = getDatabase(mandarin).getMaleFemaleProgress();
    logger.info(maleFemaleProgress.toString());
  }

  @Test
  public void testMaleFemaleRefCoverage() {
    int i = 0;
    List<String> configs = Collections.singletonList("russian");
    for (String db : Arrays.asList("npfRussian")) {
      String config = configs.get(i++);

      H2Connection connection = getH2Connection("war/config/russian/" + db);
      DatabaseImpl database = getDatabase(connection, config, db);
      Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress();

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
    PathHelper war = new PathHelper("war");

    configs = Collections.singletonList("english");
    for (String db : Arrays.asList("npfEnglish")) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      H2Connection connection = getH2Connection("war/config/english/" + db);

      DatabaseImpl database = getDatabase(connection, config, db);
      database.doReport(war, config, 2015);

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
    PathHelper war = new PathHelper("war");

    if (DO_ONE) {
      configs = Collections.singletonList("spanish");
    }
    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String config = configs.get(i++);

      logger.info("doing " + config + " ------- ");

      //H2Connection connection = getH2(db);
      H2Connection connection = new H2Connection("war/config/spanish", "npfSpanish", true,null);

      DatabaseImpl database = getDatabase(connection, config, db);
      database.doReport(war, config, 2016);

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

      DatabaseImpl database = getDatabase(connection, config, db);
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
