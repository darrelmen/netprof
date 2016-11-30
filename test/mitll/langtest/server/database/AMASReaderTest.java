package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import org.apache.logging.log4j.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/17/16.
 */
public class AMASReaderTest {
  private static final Logger logger = LogManager.getLogger(AMASReaderTest.class);
  // public static final String AM_LB_002 = "AM-LB-002";
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
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    String dbName = serverProps.getH2Database();
    database = new DatabaseImpl(parent, file.getName(), dbName, serverProps, new PathHelper("war", serverProps), false, null, true);
    logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
  }


  @Test
  public void testReport() {
    database.doReport(new PathHelper("war", null), "", 2016);
  }

  @Test
  public void testMe() {
    //database.getAMASSectionHelper().report();

    Collection<AmasExerciseImpl> exercises = database.getAMASExercises();
    Stream<AmasExerciseImpl> amasExerciseStream = exercises.stream().filter(ex -> ex.getOldID().equals("AM-LA-004"));
    Optional<AmasExerciseImpl> first = amasExerciseStream.findFirst();
    AmasExerciseImpl amasExercise = first.get();
    logger.info("first " + first + " audio '" + amasExercise.getAudioURL() + "'");
    QAPair next1 = amasExercise.getForeignLanguageQuestions().iterator().next();
    logger.info("q " + next1);
    AmasExerciseImpl next = exercises.iterator().next();
    logger.info("e.g. " + next);
    logger.info("\n\ngot " + exercises.size());

    ServerProperties serverProps = database.getServerProps();
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war", serverProps), serverProps, database, null, null);

    audioFileHelper.makeAutoCRT(".");

    database.getAMASSectionHelper().report();
  }
}
