package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 2/17/16.
 */
public class AMASReaderTest {
  private static final Logger logger = Logger.getLogger(AMASReaderTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("amasMSA");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "amas.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    String dbName = serverProps.getH2Database();
    database = new DatabaseImpl(parent, file.getName(), dbName, serverProps, new PathHelper("war"), false, null);
    logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
//    database.getExercises();
  }

  @Test
  public void testMe() {
    database.getSectionHelper().report();


    Collection exercises = database.getAMASExercises();
    logger.info("\n\ngot " + exercises.size());
  }

}
