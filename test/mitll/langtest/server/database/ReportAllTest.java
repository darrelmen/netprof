package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.connection.H2Connection;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.*;

/**
 * Created by GO22670 on 1/30/14.
 */
public class ReportAllTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(ReportAllTest.class);
  public static final boolean DO_ONE = false;


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

      H2Connection connection = getH2Connection("war/config/english/"+db);

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

      H2Connection connection = getH2(db);

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
