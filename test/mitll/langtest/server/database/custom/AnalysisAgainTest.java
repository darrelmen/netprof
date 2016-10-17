package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/17/16.
 */
public class AnalysisAgainTest {
  private static final Logger logger = Logger.getLogger(AnalysisAgainTest.class);
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
    database = new DatabaseImpl(parent, file.getName(), dbName, serverProps, new PathHelper("war"), false, null);
    logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(), serverProps.getMediaDir());
    database.getExercises();
  }

  @Test
  public void testMe() {
    database.getSectionHelper();

    UserPerformance performance = database.getAnalysis().getPerformanceForUser(1, 1);
    logger.info("got " + performance);

    List<WordScore> wordScoresForUser = database.getAnalysis().getWordScoresForUser(1, 1);
    for (WordScore ws : wordScoresForUser) {
      //if (ws.getExID().equals("50246"))
      logger.info("ws " +ws.getId() + " " + ws.getNativeAudio() + " " + ws.getFileRef());
    }

  }

}
