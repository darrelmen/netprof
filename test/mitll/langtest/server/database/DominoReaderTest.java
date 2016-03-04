package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by go22670 on 2/17/16.
 */
public class DominoReaderTest {
  private static final Logger logger = Logger.getLogger(DominoReaderTest.class);
 // public static final String AM_LB_002 = "AM-LB-002";
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("dominoSpanish");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    logger.debug("config dir " + parent +" config     " + name);
    ServerProperties serverProps = new ServerProperties(parent, name);
    database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war"), false, null);
   // logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
  }

  @Test
  public void testMe() {
    //database.getAMASSectionHelper().report();

    Collection<?> exercises = database.getExercises();

/*    Stream<AmasExerciseImpl> amasExerciseStream = exercises.stream().filter(ex -> ex.getID().equals("AM-LA-004"));
    Optional<AmasExerciseImpl> first = amasExerciseStream.findFirst();
    AmasExerciseImpl amasExercise = first.get();
    logger.info("first " + first+  " audio '" + amasExercise.getAudioURL() + "'");
    QAPair next1 = amasExercise.getForeignLanguageQuestions().iterator().next();
    logger.info("q " + next1);*/
    Iterator<?> iterator = exercises.iterator();
    Object next2 = iterator.next();

   // AmasExerciseImpl next = (AmasExerciseImpl) next2;
    logger.info("\n\ngot " + exercises.size());
    logger.info("e.g. " + next2);
    logger.info("e.g. " + iterator.next());
    logger.info("e.g. " + iterator.next());
    logger.info("e.g. " + iterator.next());
    logger.info("e.g. " + iterator.next());



//    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), database.getServerProps(), database, null);

//    audioFileHelper.makeAutoCRT(".");

  //  database.getAMASSectionHelper().report();
  }
}
