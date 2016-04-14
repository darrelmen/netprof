package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.Collection;

/**
 * Created by GO22670 on 1/30/14.
 */
public class DecodeTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(DecodeTest.class);
  public static final boolean DO_ONE = false;

  @Test
  public void testMandarin() {
    DatabaseImpl russian = getDatabase("mandarin");
    JSONObject war = russian.doReport(new PathHelper("war"), "", 2015);
    logger.info("json:\n"+war);
  }

  @Test
  public void testFullMandarin() {
    JSONObject war = getDatabase("mandarin").doReport(new PathHelper("war"));
    logger.info("json:\n"+war);
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
