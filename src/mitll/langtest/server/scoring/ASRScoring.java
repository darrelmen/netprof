package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.image.TranscriptReader;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import pronz.speech.ASRParameters;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Does ASR scoring using hydec.
 *
 * Takes the label files and generates transcript images for display in the client.
 *
 * User: go22670
 * Date: 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoring extends Scoring {
  private static Logger logger = Logger.getLogger(ASRScoring.class);

  private final Map<String, ASRParameters> languageLookUp = new HashMap<String, ASRParameters>();
/*  private static final float MIN_AUDIO_SECONDS = 0.3f;
  private static final float MAX_AUDIO_SECONDS = 15.0f;*/
  private final Cache<String, Scores> audioToScore;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAudioFile(int, String, java.util.Collection, int, int)
   * @param deployPath
   */
  public ASRScoring(String deployPath) {
    super(deployPath);

    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    ASRParameters arabic = ASRParameters.jload("Arabic", configFullPath);
    if (arabic == null) {
      logger.error("can't find Arabic parameters at " + configFullPath);
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio(int, String, String, int, int)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,
        sentence,
        "Arabic",
        scoringDir,imageOutDir,imageWidth,imageHeight);
  }

  /**
   * Use hydec to do scoring
   *
   * Skips sv scoring for the moment -- why would we do it?
   *
   * @see #scoreRepeat(String, String, String, String, int, int)
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
  private PretestScore scoreRepeatExercise(String testAudioDir, String testAudioFileNoSuffix,
                                          String sentence, String asrLanguage,
                                          String scoringDir,

                                          String imageOutDir,
                                          int imageWidth, int imageHeight) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";
    File wavFile = new File(pathname);
    boolean mustPrepend = false;
    if (!wavFile.exists()) {
      wavFile = new File(deployPath + File.separator + pathname);
      mustPrepend = true;
    }
    if (!wavFile.exists()) {
      logger.error("scoreRepeatExercise : Can't find audio wav file at : " + wavFile.getAbsolutePath());
      return new PretestScore();
    }

    try {
      String audioDir = testAudioDir;
      if (mustPrepend) {
         audioDir = deployPath + File.separator + audioDir;
        if (!new File(audioDir).exists()) logger.error("Couldn't find " + audioDir);
        else testAudioDir = audioDir;
      }
      testAudioFileNoSuffix = new AudioConversion().convertTo16Khz(audioDir, testAudioFileNoSuffix);
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

    Scores scores = audioToScore.getIfPresent(testAudioDir + File.separator + testAudioFileNoSuffix);

    if (scores == null) {
      scores = computeRepeatExerciseScores(scoringDir, testAudio, sentence, asrLanguage);
      audioToScore.put(testAudioDir + File.separator + testAudioFileNoSuffix, scores);
    }
    else {
      logger.info("found cached score for file '" + testAudioDir + File.separator + testAudioFileNoSuffix + "'");
    }

    ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<Float>> typeToEndTimes = new HashMap<NetPronImageType, List<Float>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<Float> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) { typeToEndTimes.put(key, endTimes = new ArrayList<Float>()); }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        endTimes.add(event.getValue().end);
      }
    }

    PretestScore pretestScore =
        new PretestScore(scores.hydecScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes);
    return pretestScore;
  }

  /**
   * Make sure that when we scale the phone scores by {@link #SCORE_SCALAR} we do it for both the scores and the image.
   * <br></br>
   * get the phones for display in the phone accuracy pane
   * @param scores from hydec
   * @return map of phone name to score
   */
  private Map<String, Float> getPhoneToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get("phones");
    if (phones == null) {
      return Collections.emptyMap();
    }
    else {
      Map<String, Float> phoneToScore = new HashMap<String, Float>();
      for (Map.Entry<String, Float> phoneScorePair : phones.entrySet()) {
        String key = phoneScorePair.getKey();
        if (!key.equals("sil")) {
          phoneToScore.put(key, Math.min(1.0f, phoneScorePair.getValue() * SCORE_SCALAR));
        }
        else {
          System.out.println("Skipping sils.");
        }
      }
      return phoneToScore;
    }
  }

    /**
     * Assumes that testAudio was recorded through the UI, which should prevent audio that is too short or too long.
     *
     * @see #scoreRepeatExercise
     * @param testAudio
     * @param sentence
     * @param language
     * @return
     * @throws Exception
     */
  private Scores computeRepeatExerciseScores(String tomcatWriteDirectory, Audio testAudio, String sentence, String language) {
/*    float testAudioSeconds = testAudio.seconds();
    if (testAudioSeconds < MIN_AUDIO_SECONDS || testAudioSeconds > MAX_AUDIO_SECONDS) {
     // throw new Exception("Recording is too short (< " + MIN_AUDIO_SECONDS + "s) or too long (> " + MAX_AUDIO_SECONDS + "s)");
    }*/

    // RepeatExercises use ASR for scoring, so get the language parameters.
    ASRParameters asrparameters = languageLookUp.get(language);
    if (asrparameters == null) {
      logger.error("computeRepeatExerciseScores : no ASR parameters for " + language);
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
      if (!configExists) logger.error("computeRepeatExerciseScores : Can't find config file at " + configFile);
      if (!dictExists)   logger.error("computeRepeatExerciseScores : Can't find dict file at " + dictFile);
      return getEmptyScores();
    }

    ASRParameters asrparametersFullPaths = new ASRParameters(
        configFile,
        dictFile,
        asrparameters.letterToSoundClassString(), platform);

      //System.out.println(" : Calling jscore with " + sentence + " and " + asrparametersFullPaths);
    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut;
    synchronized (this) {   // hydec can't be called concurrently -- not thread safe
      jscoreOut = testAudio.jscore(sentence, asrparametersFullPaths, new String[] {});
    }
    Float hydec_score = jscoreOut._1;
    logger.info(" : got score " + hydec_score);

    deleteTmpDir();

    Float[] svScoreVector = { 0f, 1.0f }; // Fake ratio.
    return new Scores(hydec_score, jscoreOut._2, svScoreVector);
  }

  /**
   * Note that the log file sticks around, so the delete doesn't completely succeed.
   */
  private void deleteTmpDir() {
    File tmpDirFile = new File(tmpDir);
    if (tmpDirFile.exists()) {
      try {
        logger.info("deleting " + tmpDirFile.getAbsolutePath());
        FileUtils.deleteDirectory(tmpDirFile);
      } catch (IOException e) {
        //e.printStackTrace();
      }
    }
/*    if (tmpDirFile.exists()) {
      logger.warn("huh? " + tmpDirFile.getAbsolutePath() + " exists???");
    }*/
  }

  private Scores getEmptyScores() {
    Float[] floats = {0f, 1f};
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores, floats);
  }

/*  public static void main(String [] arg) {
    ASRScoring scoring = new ASRScoring("C:\\Users\\go22670\\DLITest\\LangTest\\war");
    String testAudioDir = "C:\\Users\\go22670\\DLITest\\LangTest\\war\\media\\ac-L0P-001";
    PretestScore pretestScore = scoring.scoreRepeat(testAudioDir, "ad0035_ems", *//*testAudioDir, "ad0035_ems",*//* "This is a test.", "out", 1024, 100);
    System.out.println("score " + pretestScore);
  }*/
}
