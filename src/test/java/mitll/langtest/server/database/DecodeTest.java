package mitll.langtest.server.database;

import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.user.User;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class DecodeTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DecodeTest.class);
  //public static final boolean DO_ONE = false;

  @Test
  public void testPunct() {
    String unicode = "\\u0xFF0C";

    String repl = "上有天堂，下有苏杭";

    List<String>tests = new ArrayList<>();

    tests.add("\n" + "阿布.扎比");
    tests.add("再添点茶，怎么样？");
    tests.add("professor");
    tests.add("profesor,");
    ;
    logger.info("va " + removePunct(repl));
    logger.info("va " + removePunct("戰爭就是要不斷在失敗中吸取教訓。"));

    for (String test:tests) logger.info(removePunct(test));
  }

  @Test
  public void testRussianAccent() {
    String value = StringUtils.stripAccents("оди́ннадцать");

    logger.info("Value " + value);
  }

  private String clean(String repl) {
    return repl
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("[\\p{M}\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002]", "");
  }

  protected String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("[\\p{M}\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u003F\\u00BF\\u002E\\u002C\\u0021\\u20260\\u005C\\u2013]", "");
  }

  @Test
  public void testRussianContext() {
    DatabaseImpl russian = getDatabase();
    CommonExercise exercise = russian.getProjectByName("russian").getRawExercises().iterator().next();
    String context = exercise.getContext();
    logger.info("got\n" + context);
  }

  @Test
  public void testUser() {
    DatabaseImpl spanish = getDatabase();
    User userByID = spanish.getUserDAO().getUserByID("Jesse.McDonald");
    logger.info("got " + userByID);
    String emailH = Md5Hash.getHash("jesse.mcdonald@dliflc.edu");
    logger.info("hash " + emailH);
  }

/*

  @Test
  public void testSpanish() {
    DatabaseImpl spanish = getDatabase("spanish");

    Map<String, Float> maleFemaleProgress = spanish.getMaleFemaleProgress();

    logger.info("got " +maleFemaleProgress);
    Set<String> failed = new TreeSet<>();
    IAudioDAO audioDAO = spanish.getAudioDAO();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    Collection<CommonExercise> exercises = spanish.getExercises();

    for (CommonExercise exercise : exercises) {

      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getOldID());
      if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
        boolean didAll = audioDAO.attachAudio(exercise, "war", "config/spanish", audioAttributes);
       // attrc += audioAttributes.size();
        if (!didAll) {
          failed.add(exercise.getOldID());
        }
      }
    }
    if (!failed.isEmpty()) {
      logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
    }
//    JSONObject war = russian.doReportForYear(new PathHelper("war"));
  //  logger.info("json:\n" + war);
  }
*/

/*  @Test
  public void testMandarin() {
    DatabaseImpl russian = getDatabase("mandarin");
    JSONObject war = russian.doReportForYear(new PathHelper("war", null));
    logger.info("json:\n" + war);
  }*/

/*  @Test
  public void testMSAReports() {
    DatabaseImpl russian = getDatabase("msa");
    long then = System.currentTimeMillis();
    JSONObject war = russian.doReportForYear(new PathHelper("war", null));
    long now = System.currentTimeMillis();
    logger.debug("report 1 : took " + (now - then) + " millis");
    now = then;
    JSONObject war2 = russian.doReportForYear(new PathHelper("war", null));
    now = System.currentTimeMillis();

    logger.debug("report 2 took " + (now - then) + " millis");

    logger.info("json:\n" + war);
  }

  @Test
  public void testFullMandarin() {
    JSONObject war = getDatabase("mandarin").doReportForYear(new PathHelper("war", null));
    logger.info("json:\n" + war);
  }*/

  @Test
  public void testRussian() {
    DatabaseImpl russian = getDatabase();
    AudioFileHelper audioFileHelper = getAudioFileHelper(russian);
    CommonExercise exercise = russian.getExercise(1, 8);
    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false, -1);
  }

  AudioFileHelper getAudioFileHelper(DatabaseImpl russian) {
    ServerProperties serverProps = russian.getServerProps();
    return new AudioFileHelper(new PathHelper("war", null), serverProps, russian, null, null);
  }

  @Test
  public void testEnglish() {
    DatabaseImpl russian = getDatabase();
    AudioFileHelper audioFileHelper = getAudioFileHelper(russian);
    CommonExercise exercise = russian.getExercise(1, 2253);
    logger.info("got " + exercise);

    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false, -1);
  }

  @Test
  public void testSpanish2() {
    DatabaseImpl db = getDatabase();
    CommonExercise exercise = db.getExercise(1, 50264);
    logger.info("got " + exercise);
    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      logger.info("attr " + audioAttribute);
    }
    for (CommonExercise context : exercise.getDirectlyRelated()) {
      logger.info("got " + context + " with " + context.getAudioAttributes());
    }
  }
}
