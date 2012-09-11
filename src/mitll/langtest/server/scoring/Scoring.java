package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.imagewriter.AudioConverter;
import audio.imagewriter.ImageWriter;
import audio.tools.FileCopier;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import pronz.dirs.Dirs;
import pronz.speech.ASRParameters;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Function1;
import scala.Tuple2;
import scala.runtime.AbstractFunction1;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class Scoring {
  private static final String CONFIGURATIONS = "windowsConfig";
  public static final String LINUX_SOX_BIN_DIR = "/usr/local/bin";
  public static final String WINDOWS_SOX_BIN_DIR = "C:\\Users\\go22670\\sox-14-3-2";
  private Map<String, ASRParameters> languageLookUp = new HashMap<String, ASRParameters>();
  private Dirs dirs;
  private static final float MIN_AUDIO_SECONDS = 0.3f;
  private static final float MAX_AUDIO_SECONDS = 15.0f;
  String scoringDir;
  private String os;

  public Scoring(String deployPath) {
    String property = System.getProperty("os.name").toLowerCase();
    this.os = property.contains("win") ? "win32" : property
        .contains("mac") ? "macos"
        : property.contains("linux") ? System
        .getProperty("os.arch").contains("64") ? "linux64"
        : "linux" : "linux";

    scoringDir = deployPath + File.separator + "scoring";

    String configFullPath = scoringDir + File.separator + CONFIGURATIONS;   // TODO point at os specific config file
    ASRParameters arabic = ASRParameters.jload("Arabic", configFullPath);
    if (arabic == null) {
      System.err.println("can't find Arabic parameters at " + configFullPath);
    } else {
      languageLookUp.put("Arabic", arabic);
    }

    ASRParameters english = ASRParameters.jload("English", configFullPath);
    if (english == null) {
      System.err.println("can't find English parameters at " + configFullPath);
    } else {
      languageLookUp.put("English", english);
    }

    dirs = pronz.dirs.Dirs$.MODULE$.apply(scoringDir + File.separator
        + "tmp", "", scoringDir, new Log(null, true));
  }

  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String refAudioDir, String refAudioFileNoSuffix,
                                 // String sentence, String asrLanguage,
                                 // String scoringDir,
                                  String imageOutDir,
                                  int imageWidth, int imageHeight) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,refAudioDir,refAudioFileNoSuffix,"This is a test.","Arabic",scoringDir,imageOutDir,imageWidth,imageHeight);
  }

  /**
   * Use hydec to do scoring
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param refAudioDir
   * @param refAudioFileNoSuffix
   * @param sentence
   * @param asrLanguage
   * @param scoringDir
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @return
   */
  public PretestScore scoreRepeatExercise(String testAudioDir, String testAudioFileNoSuffix,
                                          String refAudioDir, String refAudioFileNoSuffix,
                                          String sentence, String asrLanguage,
                                          String scoringDir,
                                          String imageOutDir,
                                          int imageWidth, int imageHeight
  ) {
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + ".wav";
    File wavFile = new File(pathname);
    if (!wavFile.exists()) {
      System.err.println("Can't find " + wavFile.getAbsolutePath());
      return new PretestScore();
    }

    testAudioFileNoSuffix = convertTo16Khz(testAudioDir, testAudioFileNoSuffix, pathname, wavFile);
    // the path to the test audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<testsRelativePath>/<testName>
    Audio testAudio = Audio$.MODULE$.apply(
        testAudioDir, testAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    // the path to the ref audio is <tomcatWriteDirectory>/<pretestFilesRelativePath>/<planName>/<referenceName>
    // referenceName is called exercise_name in the db.
    Audio refAudio = Audio$.MODULE$.apply(
        refAudioDir, refAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    Scores scores = computeRepeatExerciseScores(scoringDir, testAudio, refAudio, sentence, asrLanguage);

    // get the phones for display in the phone accuracy pane, maps start
    // time to Transcript events
    Map<String, Float> phones = scores.eventScores.get("phones");
    Map<String, Float> emptyMap = Collections.emptyMap();
    Map<String, Float> phoneScores = phones != null ? new HashMap<String, Float>(phones) : emptyMap;


    // These may not all exist. The speech file is created only by multisv
    // right now. writeTranscriptImages() ignores missing files.
    String phoneLabFile = testAudioDir + File.separator
        + testAudioFileNoSuffix + ".phones.lab";
    //log("Phone File: " + phoneLabFile);
    String speechLabFile = testAudioDir + File.separator
        + testAudioFileNoSuffix + ".speech.lab";
    // log("Speech File: " + speechLabFile);
    String wordLabFile = testAudioDir + File.separator
        + testAudioFileNoSuffix + ".words.lab";
    Map<ImageType, String> typeToFile = new HashMap<ImageType, String>();

    if (new File(phoneLabFile).exists()) typeToFile.put(ImageType.PHONE_TRANSCRIPT, phoneLabFile);
    if (new File(wordLabFile).exists()) typeToFile.put(ImageType.WORD_TRANSCRIPT, wordLabFile);
    if (new File(speechLabFile).exists()) typeToFile.put(ImageType.SPEECH_TRANSCRIPT, speechLabFile);

    Map<ImageType, String> typeToImageFile = new ImageWriter().writeTranscripts(pathname, imageOutDir, imageWidth, imageHeight, typeToFile);
    Map<NetPronImageType, String> sTypeToImage = new HashMap<NetPronImageType, String>();
    for (Map.Entry<ImageType, String> kv : typeToImageFile.entrySet()) {
      String name = kv.getKey().toString();
      NetPronImageType key = NetPronImageType.valueOf(name);
      //System.out.println("key is " + name + "/" + key + " -> " +kv.getValue());
      sTypeToImage.put(key, kv.getValue());
    }

    // XXX
    // TODO: Must compute transformed scores! Not implemented yet.
    System.out.println("Hydec Score: " + scores.hydecScore);
    StringBuilder b = new StringBuilder();
    for (Float f : scores.svScoreVector) b.append(f).append(",");
    System.out.println("SV Score: " + b);
    System.out.println("Map: " + sTypeToImage);

    return new PretestScore(scores.hydecScore, scores.svScoreVector, phoneScores, sTypeToImage);
  }

  /**
   * Checks if the sample rate is not 16K (as required by things like sv).
   * If not, then uses sox to make an audio file with the right sample rate.
   *
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param pathname
   * @param wavFile
   * @return
   */
  private String convertTo16Khz(String testAudioDir, String testAudioFileNoSuffix, String pathname, File wavFile) {
    try {
      AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(wavFile);
      float sampleRate = audioFileFormat.getFormat().getSampleRate();
      if (sampleRate != 16000f) {
        String binPath = WINDOWS_SOX_BIN_DIR;
        if (! new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR;
        String s = new AudioConverter().convertTo16KHZ(binPath, pathname);     // TODO : work in linux
        String sampled = testAudioDir + File.separator + testAudioFileNoSuffix + "_16K.wav";
        if (new FileCopier().copy(s, sampled)) {
          String name = new File(sampled).getName();
          testAudioFileNoSuffix = name.substring(0, name.length() - 4);
        }
      }
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return testAudioFileNoSuffix;
  }

  /**
     *  convert the imageWriterTranscriptEvents to
      pretestTranscriptEvents (make them serializable for GWTRPC)
     */
    private void convertEvents() {}

    /**
     * @seex #scoreAudio
     * @seex #scoreRepeatExercise(com.goodcomponents.exercises.RepeatExercise, String, String)
     * @param testAudio
     * @param refAudio
     * @param sentence
     * @param language
     * @return
     * @throws Exception
     */
    // Just compute the score: don't deal with any GUI or history
  private Scores computeRepeatExerciseScores(String tomcatWriteDirectory, Audio testAudio, Audio refAudio, String sentence, String language) {
    float testAudioSeconds = testAudio.seconds();
    if (testAudioSeconds < MIN_AUDIO_SECONDS || testAudioSeconds > MAX_AUDIO_SECONDS) {
     // throw new Exception("Recording is too short (< " + MIN_AUDIO_SECONDS + "s) or too long (> " + MAX_AUDIO_SECONDS + "s)");
    }

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
    boolean dictExists = new File(dictFile).exists();
    if (!configExists || !dictExists) {
      if (!configExists) System.err.println("computeRepeatExerciseScores : Can't find config file at " + configFile);
      if (!dictExists) System.err.println("computeRepeatExerciseScores : Can't find dict file at " + dictFile);
      return getEmptyScores();
    }

    ASRParameters asrparametersFullPaths = new ASRParameters(
        configFile,
        dictFile,
        asrparameters.letterToSoundClassString(), platform);

    Function1<Float, Float> identityFn = new AbstractFunction1<Float, Float>() {
      public Float apply(Float score) {
        return score;
      }
    };

    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut = testAudio.jscore(sentence, asrparametersFullPaths, new String[] {});
    Float hydec_score = jscoreOut._1;
    Float sv_score = testAudio.sv(refAudio, os, identityFn);
    Float[] svScoreVector = { sv_score, 1.0f }; // Fake ratio.
    return new Scores(hydec_score, jscoreOut._2, svScoreVector);
  }

  private Scores getEmptyScores() {
    Float[] floats = {0f, 1f};
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores, floats);
  }

  public static void main(String [] arg) {
    Scoring scoring = new Scoring("C:\\Users\\go22670\\DLITest\\LangTest\\war");
    String testAudioDir = "C:\\Users\\go22670\\DLITest\\LangTest\\war\\media\\ac-L0P-001";
    PretestScore pretestScore = scoring.scoreRepeat(testAudioDir, "ad0035_ems", testAudioDir, "ad0035_ems", "out", 1024, 100);
    System.out.println("score " + pretestScore);
  }
}
