package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import pronz.speech.Audio;
import pronz.speech.Audio$;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/11/12
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DTWScoring extends Scoring {
  public DTWScoring(String deployPath) {
    super(deployPath);
  }

  public PretestScore score(String testAudioDir, String testAudioFileNoSuffix,
                            String refAudioDir, Collection<String> refAudioFiles,
                            // String scoringDir,
                            String imageOutDir,
                            int imageWidth, int imageHeight) {

    String testNoSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String testWav = testNoSuffix + ".wav";

    System.out.println("scoring " +testAudioFileNoSuffix + "/" + testWav +
        " against " + refAudioFiles + " in " + refAudioDir);

    File wavFile = new File(testWav);
    if (!wavFile.exists()) {
      System.err.println("Can't find " + wavFile.getAbsolutePath());
      return new PretestScore();
    }
    // the path to the test audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<testsRelativePath>/<testName>
    Audio testAudio = Audio$.MODULE$.apply(
        testAudioDir, testAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    Float[] scores = computeMultiRefRepeatExerciseScores(testAudio, refAudioDir, refAudioFiles);
    Map<NetPronImageType, String> sTypeToImage = writeTranscripts(imageOutDir, imageWidth, imageHeight, testNoSuffix);

    return new PretestScore(scores, sTypeToImage);
  }

  private Float[] computeMultiRefRepeatExerciseScores(Audio testAudio, String refDir, Collection<String> refs)  {

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
/*
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
          false */
/* notForScoring *//*
, dirs);
    }
*/

    Audio[] referenceAudios = new Audio[refs.size()];

    int i = 0;
    for (String r : refs) {
      System.out.println("ref is '" +r+ "'");
      referenceAudios[i++] = Audio$.MODULE$.apply(refDir,
          r,
          false /* notForScoring */, dirs);
    }

    return testAudio.multisvVectorScore(referenceAudios, os);
  }

}
