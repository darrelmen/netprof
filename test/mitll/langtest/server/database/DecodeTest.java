package mitll.langtest.server.database;

import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class DecodeTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(DecodeTest.class);
  //public static final boolean DO_ONE = false;

  @Test
  public void testFrench() {
    DatabaseImpl<CommonExercise> russian = getDatabase("french");
    CommonExercise exercise = russian.getExercise("2127");
    //String context = exercise.getContext();

    russian.getSectionHelper().report();
    logger.info("got\n" + exercise);
  }

  @Test
  public void testSpanishAudio() {
    DatabaseImpl<CommonExercise> russian = getDatabase("spanish");
    CommonExercise exercise = russian.getExercise("3277");
    russian.attachAudio(exercise);
    //String context = exercise.getContext();

    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      logger.info("Found " + audioAttribute);
    }
//    russian.getSectionHelper().report();
    logger.info("got\n" + exercise);
  }

  @Test
  public void testFixSudanese() {
    DatabaseImpl<CommonExercise> russian = getDatabase("sudanese");

    PathHelper war = new PathHelper("war");
    Collection<AudioAttribute> audioAttributes = russian.getAudioDAO().getAudioAttributes();
    int fixed = 0;
    for (AudioAttribute attribute : audioAttributes) {
      String audioRef = attribute.getAudioRef();
      File audioFile = getAbsoluteFile(war, audioRef);
      String exid = attribute.getExid();
      if (audioFile.exists()) {
        long length = audioFile.length();
        if (length == 16428) {
          List<CorrectAndScore> resultsForExIDInForUser = russian.getResultDAO().getResultsForExIDInForUser(attribute.getUserid(), false, exid);
          logger.info("found suspect file " + audioRef + " and " + resultsForExIDInForUser.size());

          for (CorrectAndScore correctAndScore : resultsForExIDInForUser) {
            long diff = attribute.getTimestamp() - correctAndScore.getTimestamp();
            if (diff > 0 && diff < 200) {
              String orig = correctAndScore.getPath();
              logger.info("\tin db, found original (" + diff + ") " + orig);
              File origFile = getAbsoluteFile(war, orig);
              if (origFile.exists()) {
                double durationInSeconds = new AudioCheck(null).getDurationInSeconds(origFile) * 1000;
                long durationInMillis = attribute.getDurationInMillis();

                if (durationInMillis == (long) durationInSeconds) {
                  logger.info("\t\tDur " + durationInSeconds + " vs " + durationInMillis + " got match - fixing...");
                  new PathWriter().copyAndNormalize(origFile, russian.getServerProps(), audioFile);
                  logger.info("\t\tgot match - after length = " + audioFile.length());
                  fixed++;
                } else {
                  logger.warn("\t\tNO MATCH Dur " + durationInSeconds + " vs " + durationInMillis);
                }
              } else {
                logger.warn("\t\tcan't find " + origFile.getAbsolutePath());
              }
            }
          }
        }
      } else {
        if (exid.startsWith("38")) {
          logger.info("no file at " + audioFile.getAbsolutePath());
        }
      }
    }
    logger.info("Fixed " + fixed + " files");

    CommonExercise exercise = russian.getExercise("2127");
    //String context = exercise.getContext();

    // russian.getSectionHelper().report();
    // logger.info("got\n" + exercise);
  }


  private File getAbsoluteFile(PathHelper pathHelper, String path) {
    return pathHelper.getAbsoluteFile(path);
  }

  @Test
  public void testKorean() {
    DatabaseImpl<CommonExercise> russian = getDatabase("korean");
    CommonExercise exercise = russian.getExercise("2127");
    //String context = exercise.getContext();

    logger.info("got\n" + exercise);
  }

  @Test
  public void testSpanishContext() {
    DatabaseImpl<CommonExercise> russian = getDatabase("spanish");
    CommonExercise exercise = russian.getExercise("50264");
    //String context = exercise.getContext();

    logger.info("got\n" + exercise);
  }


  //public static final boolean DO_ONE = false;

  @Test
  public void testEgyptianReadOneExercise() {
    DatabaseImpl<CommonExercise> db = getDatabase("egyptian");
    CommonExercise exercise = db.getExercises().iterator().next();
    //String context = exercise.getContext();
    logger.info("got\n" + exercise);
  }

  @Test
  public void testJapaneseReadOneExercise() {
    DatabaseImpl<CommonExercise> db = getDatabase("japanese");
    CommonExercise exercise = db.getExercises().iterator().next();

    //String context = exercise.getContext();

    logger.info("got\n" + exercise);
    logger.info("got\n" + db.getCustomOrPredefExercise("1826"));


  }

  @Test
  public void testDefect() {
    DatabaseImpl<CommonExercise> db = getDatabase("msa");
    CommonExercise exercise = db.getExercises().iterator().next();

    //String context = exercise.getContext();

    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise("3069");
    db.attachAudio(customOrPredefExercise);
    logger.info("got\n" + customOrPredefExercise);
    for (AudioAttribute audioAttribute : customOrPredefExercise.getAudioAttributes()) {
      logger.info("\t" + audioAttribute);
    }
  }

  @Test
  public void testRussianContext() {
    DatabaseImpl<CommonExercise> russian = getDatabase("russian");
    CommonExercise exercise = russian.getExercise("2600");
    String context = exercise.getContext();


    logger.info("got\n" + context);
  }

  @Test
  public void testSpanishReport() {
    JSONObject jsonObject = getDatabase("spanish").doReport(new PathHelper("war"), "", 2016);
    logger.info("got\n" + jsonObject);
  }

  @Test
  public void testMSA() {
    DatabaseImpl msa = getDatabase("msa");
    //JSONObject war = msa.doReport(new PathHelper("war"));
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), msa.getServerProps(), msa, null);
    audioFileHelper.checkLTSOnForeignPhrase("test", "");
    // logger.info("json:\n"+war);
  }

  @Test
  public void testUser() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");
    User userByID = spanish.getUserDAO().getUserByID("Jesse.McDonald");
    logger.info("got " + userByID);

    String emailH = Md5Hash.getHash("jesse.mcdonald@dliflc.edu");

    logger.info("hash " + emailH);
  }

  @Test
  public void testSpanish() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");


    Map<String, Float> maleFemaleProgress = spanish.getMaleFemaleProgress();

    logger.info("got " + maleFemaleProgress);
    Set<String> failed = new TreeSet<>();
    AudioDAO audioDAO = spanish.getAudioDAO();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    Collection<CommonExercise> exercises = spanish.getExercises();

    for (CommonExercise exercise : exercises) {

      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
//					logger.warn("hmm - audio recorded for " + )
        boolean didAll = audioDAO.attachAudio(exercise, "war", "config/spanish", audioAttributes);
        // attrc += audioAttributes.size();
        if (!didAll) {
          failed.add(exercise.getID());
        }
      }
    }
    if (!failed.isEmpty()) {
      logger.warn("failed to attach audio to " + failed.size() + " exercises : " + failed);
    }
//    JSONObject war = russian.doReport(new PathHelper("war"));
    //  logger.info("json:\n" + war);
  }

  @Test
  public void testMandarin() {
    DatabaseImpl russian = getDatabase("mandarin");
    JSONObject war = russian.doReport(new PathHelper("war"));
    logger.info("json:\n" + war);
  }

  @Test
  public void testMSAReports() {
    DatabaseImpl russian = getDatabase("msa");
    long then = System.currentTimeMillis();
    JSONObject war = russian.doReport(new PathHelper("war"));
    long now = System.currentTimeMillis();
    logger.debug("report 1 : took " + (now - then) + " millis");
    now = then;
    JSONObject war2 = russian.doReport(new PathHelper("war"));
    now = System.currentTimeMillis();

    logger.debug("report 2 took " + (now - then) + " millis");

    logger.info("json:\n" + war);
  }

  @Test
  public void testFullMandarin() {
    JSONObject war = getDatabase("mandarin").doReport(new PathHelper("war"));
    logger.info("json:\n" + war);
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
    logger.info("got " + exercise);

    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false);
  }

  @Test
  public void testMandarinUnk() {
    DatabaseImpl database = getDatabase("mandarin");
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), database.getServerProps(), database, null);
    CommonExercise exercise = database.getExercise("5474");
    logger.info("got " + exercise);

    Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes)
      audioFileHelper.decodeOneAttribute(exercise, audioAttribute, false);
  }

  @Test
  public void testSerbian() {
    DatabaseImpl database = getDatabase("serbian");
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), database.getServerProps(), database, null);
    Collection exercises = database.getExercises();
    logger.info("Got " + exercises.size());
    if (exercises.isEmpty()) {

    } else {
      logger.info("Got " + exercises.iterator().next());
    }
  }

  @Test
  public void testSorani() {
    DatabaseImpl database = getDatabase("sorani");
  //  AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), database.getServerProps(), database, null);
    Collection exercises = database.getExercises();
    logger.info("Got " + exercises.size());
    if (exercises.isEmpty()) {

    } else {
      logger.info("Got " + exercises.iterator().next());
    }
    database.getSectionHelper().report();

    String test = "Food, Drink";
    Collection exercisesForSelectionState = database.getSectionHelper().getExercisesForSelectionState("Sub-topic", "Weaponry, Equipment");
    logger.info("Got " + exercisesForSelectionState.size());
    if (!exercisesForSelectionState.isEmpty()) logger.info("Got " + exercisesForSelectionState.iterator().next());

    exercisesForSelectionState = database.getSectionHelper().getExercisesForSelectionState("Sub-topic", test);
    logger.info("Got " + exercisesForSelectionState.size());
    if (!exercisesForSelectionState.isEmpty()) logger.info("Got " + exercisesForSelectionState.iterator().next());
  }


  @Test
  public void testTurkish() {
    doProgressReport("turkish");
  }

  @Test
  public void testSpanishProgress() {
    doProgressReport("spanish");
  }

  @Test
  public void testSudaneseProgress() {
    doProgressReport("sudanese");
  }

  @Test
  public void testMSAProgress2() {
    doProgressReport("msa");
  }

  @Test
  public void testMSAProgress() {
    DatabaseImpl database = getDatabase("msa");
    doReport(database);
    database.getExerciseIDToRefAudio();
    CommonExercise exercise = database.getExercise("1093");
    database.attachAudio(exercise);
    logger.info("ex " +exercise);
    logger.info("ex " +exercise.getAudioAttributes());
  }

  @Test
  public void testJapaneseProgress() {
    DatabaseImpl database = getDatabase("japanese");
    database.getSectionHelper().report();

    /*
    doReport(database);
    database.getExerciseIDToRefAudio();
    CommonExercise exercise = database.getExercise("1093");
    database.attachAudio(exercise);
    logger.info("ex " +exercise);
    logger.info("ex " +exercise.getAudioAttributes());
  */
  }

  private void doProgressReport(String turkish) {  doReport(getDatabase(turkish));  }

  private void doReport(DatabaseImpl database) {
//    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war"), database.getServerProps(), database, null);
    Collection exercises = database.getExercises();
    logger.info("Got " + exercises.size());
    if (exercises.isEmpty()) {

    } else {
      logger.info("Got " + exercises.iterator().next());
    }
    Map maleFemaleProgress = database.getMaleFemaleProgress();
    logger.info("got " + maleFemaleProgress);
  }
}
