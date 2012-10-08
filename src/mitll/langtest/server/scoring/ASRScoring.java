package mitll.langtest.server.scoring;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import pronz.speech.ASRParameters;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Function1;
import scala.Tuple2;
import scala.runtime.AbstractFunction1;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoring extends Scoring {
  private Map<String, ASRParameters> languageLookUp = new HashMap<String, ASRParameters>();
/*  private static final float MIN_AUDIO_SECONDS = 0.3f;
  private static final float MAX_AUDIO_SECONDS = 15.0f;*/
  Cache<String, Scores> audioToScore;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAudioFile(int, String, java.util.Collection, int, int)
   * @param deployPath
   */
  public ASRScoring(String deployPath) {
    super(deployPath);

    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    ASRParameters arabic = ASRParameters.jload("Arabic", configFullPath);
    if (arabic == null) {
      System.err.println("can't find Arabic parameters at " + configFullPath);
    } else {
      languageLookUp.put("Arabic", arabic);
    }

/*    ASRParameters english = ASRParameters.jload("English", configFullPath);
    if (english == null) {
      System.err.println("can't find English parameters at " + configFullPath);
    } else {
      languageLookUp.put("English", english);
    }*/
  }


  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAudioFile
   * TODO : pass in ref sentence and language
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @paramx refAudioDir
   * @paramx refAudioFileNoSuffix
   * @param sentence that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                //  String refAudioDir, String refAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,//refAudioDir,refAudioFileNoSuffix,
        sentence,
        "Arabic",
        scoringDir,imageOutDir,imageWidth,imageHeight);
  }

  /**
   * Use hydec to do scoring
   *
   * Skips sv scoring for the moment -- why would we do it?
   *
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @paramx refAudioDir
   * @paramx refAudioFileNoSuffix
   * @param sentence
   * @param asrLanguage
   * @param scoringDir
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @return
   */
  public PretestScore scoreRepeatExercise(String testAudioDir, String testAudioFileNoSuffix,
                                         // String refAudioDir, String refAudioFileNoSuffix,
                                          String sentence, String asrLanguage,
                                          String scoringDir,

                                          String imageOutDir,
                                          int imageWidth, int imageHeight) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";
    File wavFile = new File(pathname);
    if (!wavFile.exists()) {
      System.err.println("Can't find " + wavFile.getAbsolutePath());
      return new PretestScore();
    }

    try {
      testAudioFileNoSuffix = new AudioConversion().convertTo16Khz(testAudioDir, testAudioFileNoSuffix);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX;
    }

    // the path to the test audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<testsRelativePath>/<testName>
    Audio testAudio = Audio$.MODULE$.apply(
        testAudioDir, testAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    // the path to the ref audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<referenceName>
    // referenceName is called exercise_name in the db.
/*    Audio refAudio = Audio$.MODULE$.apply(
        refAudioDir, refAudioFileNoSuffix,
        false *//* notForScoring *//*, dirs);*/

    Scores scores = audioToScore.getIfPresent(testAudioFileNoSuffix);

    if (scores == null) {
      scores = computeRepeatExerciseScores(scoringDir, testAudio, /*refAudio, */sentence, asrLanguage);
      audioToScore.put(testAudioFileNoSuffix, scores);
    }
    else {
      System.out.println("found cached score for " + testAudioFileNoSuffix);
    }

    Map<NetPronImageType, String> sTypeToImage = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix);

    PretestScore pretestScore = new PretestScore(0, scores.hydecScore, scores.svScoreVector, getPhoneToScore(scores), sTypeToImage);
    return pretestScore;
  }

  /**
   * get the phones for display in the phone accuracy pane
   * @param scores
   * @return
   */
  private Map<String, Float> getPhoneToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get("phones");
    Map<String, Float> emptyMap = Collections.emptyMap();
    return phones != null ? new HashMap<String, Float>(phones) : emptyMap;
  }

  /**
     *  convert the imageWriterTranscriptEvents to
      pretestTranscriptEvents (make them serializable for GWTRPC)
     */
    private void convertEvents() {}

    /**
     * Assumes that testAudio was recorded through the UI, which should prevent audio that is too short or too long.
     *
     * @seex #scoreAudio
     * @see #scoreRepeatExercise
     * @param testAudio
     * @param refAudio
     * @param sentence
     * @param language
     * @return
     * @throws Exception
     */
  private Scores computeRepeatExerciseScores(String tomcatWriteDirectory, Audio testAudio, /*Audio refAudio,*/ String sentence, String language) {
/*    float testAudioSeconds = testAudio.seconds();
    if (testAudioSeconds < MIN_AUDIO_SECONDS || testAudioSeconds > MAX_AUDIO_SECONDS) {
     // throw new Exception("Recording is too short (< " + MIN_AUDIO_SECONDS + "s) or too long (> " + MAX_AUDIO_SECONDS + "s)");
    }*/

    // RepeatExercises use ASR for scoring, so get the language parameters.
    ASRParameters asrparameters = languageLookUp.get(language);
    if (asrparameters == null) {
      System.err.println("computeRepeatExerciseScores : no ASR parameters for " + language);
      return getEmptyScores();
    }
    String platform = Utils.package$.MODULE$.platform();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.
    String config = asrparameters.configFile();
    String dict = asrparameters.dictFile();
    String configFile = new File(config).isAbsolute() ? config
        : tomcatWriteDirectory + File.separator + config;
    String dictFile = new File(dict).isAbsolute() ? dict
        : tomcatWriteDirectory + File.separator + dict;

    boolean configExists = new File(configFile).exists();
    boolean dictExists   = new File(dictFile).exists();
    if (!configExists || !dictExists) {
      if (!configExists) System.err.println("computeRepeatExerciseScores : Can't find config file at " + configFile);
      if (!dictExists) System.err.println("computeRepeatExerciseScores : Can't find dict file at " + dictFile);
      return getEmptyScores();
    }

    ASRParameters asrparametersFullPaths = new ASRParameters(
        configFile,
        dictFile,
        asrparameters.letterToSoundClassString(), platform);

    //System.out.println(new Date() + " : Calling jscore with " + sentence + " and " + asrparametersFullPaths);
    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut;
    synchronized (this) {   // hydec can't be called concurrently -- not thread safe
      jscoreOut = testAudio.jscore(sentence, asrparametersFullPaths, new String[] {});
    }
    Float hydec_score = jscoreOut._1;
    System.out.println(new Date() + " : got score " + hydec_score);

/*
    Function1<Float, Float> identityFn = new AbstractFunction1<Float, Float>() {
      public Float apply(Float score) {
        return score;
      }
    };
*/    //  Float sv_score = testAudio.sv(refAudio, os, identityFn);
    Float[] svScoreVector = { 0f, 1.0f }; // Fake ratio.
    return new Scores(hydec_score, jscoreOut._2, svScoreVector);
  }

  private Scores getEmptyScores() {
    Float[] floats = {0f, 1f};
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores, floats);
  }

  public static void main(String [] arg) {
    ASRScoring scoring = new ASRScoring("C:\\Users\\go22670\\DLITest\\LangTest\\war");
    String testAudioDir = "C:\\Users\\go22670\\DLITest\\LangTest\\war\\media\\ac-L0P-001";
    PretestScore pretestScore = scoring.scoreRepeat(testAudioDir, "ad0035_ems", /*testAudioDir, "ad0035_ems",*/ "This is a test.", "out", 1024, 100);
    System.out.println("score " + pretestScore);
  }
}
