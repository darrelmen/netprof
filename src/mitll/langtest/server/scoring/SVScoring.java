package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.PretestScore;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Function1;
import scala.runtime.AbstractFunction1;

import java.io.File;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/11/12
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SVScoring {
  public Scores computeMultiRefRepeatExerciseScores(Audio testAudio, String referenceName, String planName) throws Exception {

/*    float testAudioSeconds = testAudio.seconds();
    if (testAudioSeconds < MIN_AUDIO_SECONDS || testAudioSeconds > MAX_AUDIO_SECONDS) {
      //throw new Exception("Recording is too short (< " + MIN_AUDIO_SECONDS + "s) or too long (> " + MAX_AUDIO_SECONDS + "s)");
    }*/

    // Remove the suffix that marks a specific reference, e.g. "4-multirefrepeat-0" => "4-multirefrepeat".
/*    String baseReferenceName = referenceName.replaceFirst("-[\\d]+$", "");

    Connection connection = this.dbLogin();
    PreparedStatement numRecordingsStatement = connection.prepareStatement(
        "SELECT num_recordings FROM exercises WHERE plan_name = ? AND exercise_name = ?");

    numRecordingsStatement.setString(1, planName);
    numRecordingsStatement.setString(2, baseReferenceName);
    ResultSet numRecordings = numRecordingsStatement.executeQuery();

    if (!numRecordings.first()) {
      throw new Exception("Could not fetch exercises.num_recordings where plan_name = " + planName + " and exercise_name = " + baseReferenceName);
    }*/
    int numRecs = 1;//numRecordings.getInt(1);

    // Skip the first audio file, which is just for demonstration and is not
    // used for scoring (the "reference" file).
    Audio[] referenceAudios = new Audio[numRecs - 1];
    for (int i = 2; i < numRecs + 1; i++) {
      // Add a "-<i>" suffix for each reference audio, 2 through n.
      String audioRefName = baseReferenceName + '-' + Integer.toString(i);
      referenceAudios[i - 2] = Audio$.MODULE$.apply(tomcatWriteDirectory
          + File.separator + pretestFilesRelativePath
          + File.separator + planName,
          audioRefName,
          false /* notForScoring */, dirs);
    }

    Function1<Float, Float> identityFn = new AbstractFunction1<Float, Float>() {
      public Float apply(Float score) {
        return score;
      }
    };

    Float[] sv_score_vector = testAudio.multisvVectorScore(referenceAudios, os);
    return new Scores(null, null, sv_score_vector);
  }

  @Override
  public PretestScore scoreMultiRefRepeatExercise(
      MultiRefRepeatExercise multiRefRepeatExercise, String planName,
      String testName) throws Exception {
    try {
      // the path to the test audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<testsRelativePath>/<testName>
      Audio testAudio = Audio$.MODULE$.apply(tomcatWriteDirectory +
          File.separator + pretestFilesRelativePath +
          File.separator + planName +
          File.separator + testsRelativePath +
          File.separator + testName,
          multiRefRepeatExercise.getBasename(),
          false /* notForScoring */, dirs);

      Scores scores = computeMultiRefRepeatExerciseScores(testAudio, multiRefRepeatExercise.getReferenceName(), planName);
      return new PretestScore(0, scores.svScoreVector, new ArrayList<Float>(), null, null);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }
}
