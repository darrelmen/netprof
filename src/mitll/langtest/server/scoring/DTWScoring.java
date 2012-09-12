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
  public DTWScoring(String deployPath) { super(deployPath); }

  public PretestScore score(String testAudioDir, String testAudioFileNoSuffix,
                            String refAudioDir, Collection<String> refAudioFiles,
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
    Audio[] referenceAudios = new Audio[refs.size()];

    int i = 0;
    for (String r : refs) {
      referenceAudios[i++] = Audio$.MODULE$.apply(refDir, r, false /* notForScoring */, dirs);
    }

    return testAudio.multisvVectorScore(referenceAudios, os);
  }
}
