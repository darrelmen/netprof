package mitll.langtest.server;

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
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
  private static Logger logger = Logger.getLogger(RecoTest.class);
  private PathHelper pathHelper;
  private LangTestDatabaseImpl langTest;
  private AudioFileHelper audioFileHelper;

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
    List<Exercise> exercises = langTest.getExercises();
    langTest.makeAutoCRT();

    int incorrect = 0;
    try {
      for (Exercise exercise : exercises) {
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
    List<Exercise> exercises = langTest.getExercises();
    langTest.makeAutoCRT();

    int incorrect = 0;
    int total = 0;
    try {
      for (Exercise exercise : exercises) {
        List<Exercise> others = new ArrayList<Exercise>(exercises);
        others.remove(exercise);

        for (Exercise other : others) {
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

  private boolean isCorrect(Exercise exercise) throws Exception {
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
  private boolean isMatch(Exercise exercise, File audioFile) throws Exception {
    AudioAnswer audioAnswer = new AudioAnswer();
    audioFileHelper.getFlashcardAnswer(exercise, audioFile, audioAnswer);
    if (audioAnswer.getScore() == -1) {
      logger.error("hydec bad config file, stopping...");
      throw new Exception("hydec bad config file, stopping...");
    }
    logger.debug("---> exercise #" + exercise.getID() + " reco " + audioAnswer.decodeOutput +
      " correct " + audioAnswer.isCorrect() + (audioAnswer.isCorrect() ? "" : " audio = " + audioFile));
    return audioAnswer.isCorrect();
  }
}
