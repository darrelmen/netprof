package mitll.langtest.server.test;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/8/13
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecoTest {
  private static final Logger logger = Logger.getLogger(RecoTest.class);
  private final PathHelper pathHelper;
  private final LangTestDatabaseImpl langTest;
  private final AudioFileHelper audioFileHelper;

  public RecoTest(LangTestDatabaseImpl langTest,ServerProperties serverProps, PathHelper pathHelper, AudioFileHelper audioFileHelper) {
    this.pathHelper = pathHelper;
    this.langTest = langTest;
    this.audioFileHelper = audioFileHelper;

    if (serverProps.doRecoTest()) {
      doRecoTest();
    }
    if (serverProps.doRecoTest2()) {
      doRecoTest2();
    }
  }
  /**
   * Run through all the exercises and test them against their ref audio.
   * Ideally these should all or almost all correct.
   */
  private void doRecoTest() {
    List<CommonExercise> exercises = langTest.getExercises();
    //langTest.makeAutoCRT();

    int incorrect = 0;
    try {
      for (CommonExercise exercise : exercises) {
        File audioFile = new File(pathHelper.getInstallPath(), exercise.getRefAudio());
        if (audioFile.exists()) {
          boolean isCorrect = isCorrect(exercise);
          if (!isCorrect) incorrect++;
        } else {
          logger.warn("for " + exercise + " can't find ref audio " + audioFile.getAbsolutePath());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("out of " + exercises.size() + " incorrect = " + incorrect);
  }


  private void doRecoTest2() {
    List<CommonExercise> exercises = langTest.getExercises();
   // langTest.makeAutoCRT();

    int incorrect = 0;
    int total = 0;
    try {
      for (CommonExercise exercise : exercises) {
        List<CommonExercise> others = new ArrayList<CommonExercise>(exercises);
        others.remove(exercise);

        for (CommonExercise other : others) {
          File audioFile = new File(pathHelper.getInstallPath(), other.getRefAudio());
          if (audioFile.exists()) {
            boolean isMatch = isMatch(exercise, audioFile);
            total++;
            if (isMatch) {
              logger.debug("for " + exercise.getID() + " falsely confused audio from " +other.getID() + " as correct match.");
              incorrect++;
            }
          } else {
            logger.warn("for " + exercise + " can't find ref audio " + audioFile.getAbsolutePath());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("out of " + total + " incorrect = " + incorrect + (100f * ((float) incorrect / (float) total)) + "%");
  }

  private boolean isCorrect(CommonExercise exercise) throws Exception {
    File audioFile = new File(pathHelper.getInstallPath(), exercise.getRefAudio());
    return isMatch(exercise, audioFile);
  }

  /**
   * Does audioFile match the text in the ref sentence(s) in the exercise, given the other exercises
   *
   * @param exercise
   * @param audioFile
   * @return true if ref sentence from exercise is in the audio file
   * @throws Exception
   */
  private boolean isMatch(CommonExercise exercise, File audioFile) throws Exception {
    AudioAnswer audioAnswer = new AudioAnswer();
    audioFileHelper.getFlashcardAnswer(exercise, audioFile, audioAnswer);
    if (audioAnswer.getScore() == -1) {
      logger.error("hydec bad config file, stopping...");
      throw new Exception("hydec bad config file, stopping...");
    }
    logger.debug("---> exercise #" + exercise.getID() + " reco " + audioAnswer.getDecodeOutput() +
      " correct " + audioAnswer.isCorrect() + (audioAnswer.isCorrect() ? "" : " audio = " + audioFile));
    return audioAnswer.isCorrect();
  }
}
