/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.exercise.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

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
  //    database.doReportForYear(2015);

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
     // database.doReportForYear(2016);

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
