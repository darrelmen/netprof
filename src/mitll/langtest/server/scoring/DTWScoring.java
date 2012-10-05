package mitll.langtest.server.scoring;

import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import pronz.speech.Audio;
import pronz.speech.Audio$;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAudioFile(int, String, java.util.Collection, int, int)
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

    PretestScore pretestScore = new PretestScore(0, scores, sTypeToImage);
    //System.out.println(new Date() + " : DTWScoring : got score " + pretestScore);
    return pretestScore;
  }

  private String checkExists(String checkDir) {
    if (!new File(checkDir).exists()) {
      checkDir = deployPath + File.separator + checkDir;
    }
    return checkDir;
  }

  /**
   * @see #score
   * @param testAudio
   * @param refDir
   * @param refs
   * @return
   */
  private Float[] computeMultiRefRepeatExerciseScores(Audio testAudio, String refDir, Collection<String> refs)  {
    Audio[] referenceAudios = new Audio[refs.size()];

    int i = 0;
    for (String r : refs) {
      //System.out.println("ref dir " + refDir + " file " + r + " dirs " + dirs);

      referenceAudios[i++] = Audio$.MODULE$.apply(refDir, r, false /* notForScoring */, dirs);
    }

    return testAudio.multisvVectorScore(referenceAudios, os);
  }

  private double dotproduct(List<Double> a, List<Double> b) {
    double c = 0;
    if (a.size() != b.size()) {
      System.err.println("huh? not same length " + a.size() + " vs " + b.size());
    }
      for (int i = 0; i < Math.min(a.size(),b.size()); i++) {
        c += (a.get(i) * b.get(i));
      }

    return c;
  }

  /**
   *         // scoreVector passed in is [A,B,C,D,E,F,G,H,I,J,K,M]
   // Calculate L and insert it to make [A,B,C,D,E,F,G,H,I,J,K,L,M]
   // The min_score K is the penultimate value in scoreVector.
   * @param scoreVector
   * @param transform_score_c1
   * @param transform_score_c2
   */
  private double transformScore(Float[] scoreVector, float transform_score_c1, float transform_score_c2, List<Double> calibration_coefficients,
                                double calibration_offset) {
    float min_score = scoreVector[scoreVector.length - 2];
    float L = (min_score - transform_score_c1) / transform_score_c2;
    List<Float> copy = new ArrayList<Float>(Arrays.asList(scoreVector));

    copy.remove(copy.size() - 1);
    copy.add(L);
    copy.add(scoreVector[scoreVector.length - 1]);
    System.out.println("transformScore copy " + copy);

    List<Double> doubles = new ArrayList<Double>();
    for (Float f : copy) doubles.add((double) f);
    double dotproduct = dotproduct(calibration_coefficients, doubles);
    double transformedScore = dotproduct + calibration_offset;
    System.out.println("trans " + transformedScore + " dot " +dotproduct +" cal " + calibration_offset);
    // Force it into [0.0,1.0] range.
    return Math.max(Math.min(1.0, transformedScore), 0.0);
  }

  /*
  f(x) = ca A  +  cb B  +  cc C  +  cd D  +  ce E  +  cf F  +
  cg G  +  ch H  +  ci I  +  cj J  +  ck K  +  cl L  +  cm M  +
  offset

  These values (A-M) are generated by:

  1. rescore.scala (which outputs a vector of 12 scores corresponding to A-K + M)
  2. apply-norm-rescore.scala (which generates L),

  L = g_ex(K) = (K - m_ex) / s_ex
  */
  private double transform(Float[] scoreVector, float transform_score_c1, float transform_score_c2) {
    List<Double> calibration_coefficients = Arrays.asList(-0.0004309,  // *A:  score1
        -0.02978,   // *B: frame ratio1
        -0.0008011, // *C: score2
        0.09567,    // *D: ratio2
        -0.002392,  // *E: score3
        0.04993,    // *F: ratio3
        0.001706,  // *G: score4
        0.1842,    // *H: ratio4
        0.000989,   // *I: score5
        -0.08482,    // *J: ratio5
        -0.00837,  // *K: min(scorei, 1<=i<=5)
        -0.01101,  // *L: L = g_ex(K) = (K - m_ex) / s_ex
        // m_ex = transform_score_c1
        // s_ex = transform_score_c2
        0.3047    // *M: corresponding ratio of chosen min score
    );
    double calibration_offset = 0.66197;
    return transformScore(scoreVector,transform_score_c1,transform_score_c2,calibration_coefficients,calibration_offset);
  }

  //transform_score_c1=68.51101&transform_score_c2=2.67174
  public static void main(String [] arg) {
    Float[] objects = new Float[4];
    objects[0] = 102f;
    objects[1] = 0.6f;
    objects[2] = 102f;
    objects[3] = 0.6f;
    double transform = new DTWScoring("").transform(objects, 68.51101f, 2.67174f);
    System.out.println("got " + transform);
  }
}
