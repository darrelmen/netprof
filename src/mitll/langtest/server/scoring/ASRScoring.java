package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import mitll.langtest.server.AudioCheck;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.ASRParameters;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
  private static final String CFG_TEMPLATE = "levantine-nn-model.cfg.template";
  private static final String MODELS_DIR = "models.dli-levantine";

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
    double duration = new AudioCheck().getDuration(wavFile);
    //logger.info("duration of " + wavFile.getAbsolutePath() + " is " + duration + " secs or " + duration*1000 + " millis");
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

    String tmpDir = Files.createTempDir().getAbsolutePath(); // TODO this doesn't work reliably!
   // String tmpDir = scoringDir + File.separator + TMP;
    Dirs dirs = pronz.dirs.Dirs$.MODULE$.apply(tmpDir, "", scoringDir, new Log(null, true));

    Audio testAudio = Audio$.MODULE$.apply(
        testAudioDir, testAudioFileNoSuffix,
        false /* notForScoring */, dirs);

    Scores scores = audioToScore.getIfPresent(testAudioDir + File.separator + testAudioFileNoSuffix);

    if (scores == null) {
      scores = computeRepeatExerciseScores(scoringDir, testAudio, sentence, asrLanguage, tmpDir);
      audioToScore.put(testAudioDir + File.separator + testAudioFileNoSuffix, scores);
    }
    else {
      logger.info("found cached score for file '" + testAudioDir + File.separator + testAudioFileNoSuffix + "'");
    }

    ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<Float>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo, duration);

    PretestScore pretestScore =
        new PretestScore(scores.hydecScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes);
    return pretestScore;
  }

  /**
   * Make a map of event type to segment end times (so we can map clicks to which segment is clicked on).<br></br>
   * Note we have to adjust the last segment time to be the audio duration, so we can correct for wav vs mp3 time
   * duration differences (mp3 files being typically about 0.1 seconds longer than wav files).
   * The consumer of this map is at {@link mitll.langtest.client.scoring.ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)}
   *
   * @see #scoreRepeatExercise(String, String, String, String, String, String, int, int)
   * @param eventAndFileInfo
   * @param fileDuration
   * @return
   */
  private Map<NetPronImageType, List<Float>> getTypeToEndTimes(ImageWriter.EventAndFileInfo eventAndFileInfo, double fileDuration) {
    Map<NetPronImageType, List<Float>> typeToEndTimes = new HashMap<NetPronImageType, List<Float>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<Float> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) { typeToEndTimes.put(key, endTimes = new ArrayList<Float>()); }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        endTimes.add(event.getValue().end);
      }
    }
    for ( List<Float> times : typeToEndTimes.values()) {
      Float lastEndTime = times.get(times.size() - 1);
      if (lastEndTime < fileDuration) {
       // logger.debug("setting last segment to end at end of file " + lastEndTime + " vs " + fileDuration);
        times.set(times.size() - 1,(float)fileDuration);
      }
    }
    return typeToEndTimes;
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
     */
  private Scores computeRepeatExerciseScores(String tomcatWriteDirectory, Audio testAudio, String sentence,
                                             String language, String tmpDir) {
    // RepeatExercises use ASR for scoring, so get the language parameters.
   // ASRParameters asrparameters = languageLookUp.get(language);
 /*   if (asrparameters == null) {
      logger.error("computeRepeatExerciseScores : no ASR parameters for " + language);
      return getEmptyScores();
    }*/
    String platform = Utils.package$.MODULE$.platform();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.
    //String config = asrparameters.configFile();
    //String dict = asrparameters.dictFile();
    String pathToConfigTemplate = scoringDir + File.separator + "configurations" + File.separator + CFG_TEMPLATE;   // TODO point at os specific config file
    logger.debug("template config is at " + pathToConfigTemplate);
    Map<String,String> kv = new HashMap<String, String>();
    String modelsDir = scoringDir + File.separator + MODELS_DIR;
    if (platform.startsWith("win")) {
      modelsDir = modelsDir.replaceAll("\\\\","\\\\\\\\");
      tmpDir = tmpDir.replaceAll("\\\\","\\\\\\\\");
    }

    kv.put("TEMP_DIR",tmpDir);
    kv.put("MODELS_DIR", modelsDir);
    if (platform.startsWith("win")) kv.put("/","\\\\");
    logger.debug("map is " + kv);

    String configFile = tmpDir+File.separator+"levantine-nn-model.cfg";

    doTemplateReplace(pathToConfigTemplate,configFile,kv);
    String dictFile = modelsDir + File.separator + "rsi-sctm-hlda"+File.separator+"dict-wo-sp";
/*    String configFile = new File(config).isAbsolute() ? config
        : tomcatWriteDirectory + File.separator + config;
    String dictFile = new File(dict).isAbsolute() ? dict
        : tomcatWriteDirectory + File.separator + dict;*/

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
      //  asrparameters.letterToSoundClassString(),
        "corpus.ArabicLTS",
        platform);

    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut;
    long then = System.currentTimeMillis();
    synchronized (this) {   // hydec can't be called concurrently -- not thread safe
      try {
        jscoreOut = testAudio.jscore(sentence, asrparametersFullPaths, new String[] {});
       // deleteTmpDir(tmpDir); // necessary?
      } catch (AssertionError e) {
        logger.error("Got assertion error " + e,e);
        return new Scores();
      }
    }
    float hydec_score = jscoreOut._1;
    logger.info("got score " + hydec_score +" and took " + (System.currentTimeMillis()-then) + " millis");

    return new Scores(hydec_score, jscoreOut._2);
  }


  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private void doTemplateReplace(String infile, String outfile, Map<String,String> replaceMap) {
    FileReader file;
    String line = "";
    try {
      file = new FileReader(infile);
      BufferedReader reader = new BufferedReader(file);

      FileWriter output = new FileWriter(outfile);
        BufferedWriter writer = new BufferedWriter(output);

      while ((line = reader.readLine()) != null) {
        String replaced = line;
        for (Map.Entry<String, String> kv : replaceMap.entrySet()) {
          replaced = replaced.replaceAll(kv.getKey(),kv.getValue());
        }
        writer.write(replaced +"\n");
      }
      reader.close();
      writer.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
  }

  /**
   * Note that on windows the log file sticks around, so the delete doesn't completely succeed.
   */
  private void deleteTmpDir(String tmpDir) {
    File tmpDirFile = new File(tmpDir);
    if (tmpDirFile.exists()) {
      try {
        logger.info("deleting " + tmpDirFile.getAbsolutePath());
        FileUtils.deleteDirectory(tmpDirFile);
      } catch (IOException e) {
        //e.printStackTrace();
      }
    }
    else {
      logger.info("" + tmpDirFile.getAbsolutePath() + " does not exist");
    }
    tmpDirFile.mkdir(); // we still need the directory though!
    if (tmpDirFile.exists()) {
      logger.info(" " + tmpDirFile.getAbsolutePath() + " still exists???");
    }
  }

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores);
  }

  public static void main(String [] arg) {
    String deployPath1 = "C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\scoring";
    ASRScoring scoring = new ASRScoring(deployPath1);
    Map<String,String> kv = new HashMap<String, String>();
    kv.put("TEMP_DIR","C:\\Users\\go22670\\AppData\\Local\\Temp\\1351790200971-0");
    kv.put("MODELS_DIR","models_dir_to_use");
    scoring.doTemplateReplace(deployPath1 + File.separator + "configurations" + File.separator +"levantine-nn-model.cfg.template",
        deployPath1 + File.separator + "configurations" + File.separator +"new-levantine-nn-model.cfg",kv);
    //String testAudioDir = "C:\\Users\\go22670\\DLITest\\LangTest\\war\\media\\ac-L0P-001";
    //PretestScore pretestScore = scoring.scoreRepeat(testAudioDir, "ad0035_ems", *//*testAudioDir, "ad0035_ems",*//* "This is a test.", "out", 1024, 100);
   // System.out.println("score " + pretestScore);
  }
}
