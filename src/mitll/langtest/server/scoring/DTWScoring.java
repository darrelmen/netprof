package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import pronz.speech.Audio;
import pronz.speech.Audio$;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/11/12
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DTWScoring extends Scoring {
  String deployPath;

  public DTWScoring(String deployPath) {
    super(deployPath);
    this.deployPath = deployPath;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAudioFile(String, java.util.Collection, int, int)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param refAudioDir
   * @param refAudioFiles
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @return
   */
  public PretestScore score(String testAudioDir, String testAudioFileNoSuffix,
                            String refAudioDir, Collection<String> refAudioFiles,
                            String imageOutDir,
                            int imageWidth, int imageHeight) {
    String testNoSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String testWav = testNoSuffix + ".wav";

    System.out.println(new Date() + " : scoring " +testAudioFileNoSuffix + "/" + testWav +
        " against " + refAudioFiles + " in " + refAudioDir);

    File wavFile = new File(testWav);
    if (!wavFile.exists()) {
      wavFile = new File(deployPath,wavFile.getAbsolutePath());
      testAudioDir = deployPath + File.separator + testAudioDir;
    }

    if (!wavFile.exists()) {
      System.err.println("score Can't find test wav " + wavFile.getAbsolutePath());
      return new PretestScore();
    }

    System.out.println("test audio dir " + testAudioDir + " file " + testAudioFileNoSuffix + " dirs " + dirs);
    // the path to the test audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<testsRelativePath>/<testName>
    Audio testAudio = Audio$.MODULE$.apply(
        testAudioDir, testAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    refAudioDir = checkExists(refAudioDir);
    if (!new File(refAudioDir).exists()) {
      System.err.println("score Can't find ref dir " + new File(refAudioDir));
    }
    Float[] scores = computeMultiRefRepeatExerciseScores(testAudio, refAudioDir, refAudioFiles);

    if (!imageOutDir.startsWith(deployPath)) {
      imageOutDir =  deployPath + File.separator + imageOutDir;
    }

    Map<NetPronImageType, String> sTypeToImage = writeTranscripts(imageOutDir, imageWidth, imageHeight, testNoSuffix);

    PretestScore pretestScore = new PretestScore(scores, sTypeToImage);
    //System.out.println(new Date() + " : DTWScoring : got score " + pretestScore);
    return pretestScore;
  }

  private String checkExists(String checkDir) {
    if (!new File(checkDir).exists()) {
      checkDir = deployPath + File.separator + checkDir;
    }
    return checkDir;
  }

  private Float[] computeMultiRefRepeatExerciseScores(Audio testAudio, String refDir, Collection<String> refs)  {
    Audio[] referenceAudios = new Audio[refs.size()];

    int i = 0;
    for (String r : refs) {
      //System.out.println("ref dir " + refDir + " file " + r + " dirs " + dirs);

      referenceAudios[i++] = Audio$.MODULE$.apply(refDir, r, false /* notForScoring */, dirs);
    }

    return testAudio.multisvVectorScore(referenceAudios, os);
  }
}
