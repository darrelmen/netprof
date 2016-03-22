package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

/**
 * Created by GO22670 on 1/30/14.
 */
public class DecodeTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(DecodeTest.class);
  public static final boolean DO_ONE = false;

  private static DatabaseImpl getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    ServerProperties serverProps = new ServerProperties(parent, name);
    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war"), false, null);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    return database;
  }

  @Test
  public void testRussian() {
    DatabaseImpl russian = getDatabase("russian");
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), russian.getServerProps(), russian, null);
    CommonExercise exercise = russian.getExercise("8");
    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false);
  }

  @Test
  public void testEnglish() {
    DatabaseImpl russian = getDatabase("english");
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), russian.getServerProps(), russian, null);
    CommonExercise exercise = russian.getExercise("2253");
    logger.info("got " +exercise);

    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false);
  }
}
