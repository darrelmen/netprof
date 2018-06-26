package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.logging.log4j.*;
import org.junit.BeforeClass;

import java.io.File;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/17/16.
 */
public class AnalysisAgainTest {
  private static final Logger logger = LogManager.getLogger(AnalysisAgainTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("sudanese");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    String dbName = serverProps.getH2Database();
    database = new DatabaseImpl(serverProps, new PathHelper("war", serverProps), null, null);
    logger.debug("made " + database);
    database.setInstallPath(parent + File.separator + database.getServerProps().getLessonPlan());
    database.getExercises(-1, false);
  }

/*
  @Test
  public void testMe() {
    database.getSectionHelper(-1);

    UserPerformance performance = database.getAnalysis(-1).getPerformanceForUser(1, 1);
    logger.info("got " + performance);

    List<WordScore> wordScoresForUser = database.getAnalysis(-1).getWordScoresForUser(1, 1);
    for (WordScore ws : wordScoresForUser) {
      //if (ws.getExID().equals("50246"))
      logger.info("ws " +ws.getId() + " " + ws.getNativeAudio() + " " + ws.getFileRef());
    }

  }
*/

}
