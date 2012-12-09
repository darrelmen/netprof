package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.AudioConverter;
import audio.imagewriter.ImageWriter;
import audio.tools.FileCopier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import corpus.package$;
import mitll.langtest.server.AudioCheck;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.server.ProcessRunner;
import mitll.langtest.server.database.FileExerciseDAO;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

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
  private static final String DICT_WO_SP = "dict-wo-sp";
  private static final String GRAMMAR_ALIGN_TEMPLATE = "grammar.align.template";
  private static final String GRAMMAR_ALIGN =
      GRAMMAR_ALIGN_TEMPLATE.substring(0,GRAMMAR_ALIGN_TEMPLATE.length()-".template".length());
  private static final String TEMP_DIR = "TEMP_DIR";
  private static final String MODELS_DIR_VARIABLE = "MODELS_DIR";
  private static final String N_OUTPUT = "N_OUTPUT";
  private static final String LEVANTINE_N_OUTPUT = "" + 38;

  private static final String N_HIDDEN = "N_HIDDEN";
  private static final String N_HIDDEN_DEFAULT = "" + 2500;
  private static final String OPT_SIL = "OPT_SIL";
  private static final String OPT_SIL_DEFAULT = "true";   // rsi-sctm-hlda
  private static final String HLDA_DIR = "HLDA_DIR";
  private static final String LM_TO_USE = "LM_TO_USE";
 // private static final String LM_TO_USE = "LM_TO_USE";
  private static final String HLDA_DIR_DEFAULT = "rsi-sctm-hlda";   // rsi-sctm-hlda
  private static Logger logger = Logger.getLogger(ASRScoring.class);

  private static final String CFG_TEMPLATE_PROP = "configTemplate";
  private static final String CFG_TEMPLATE_DEFAULT = "generic-nn-model.cfg.template";

  private static final String DECODE_CFG_TEMPLATE_PROP = "decodeConfigTemplate";
  private static final String DECODE_CFG_TEMPLATE_DEFAULT = "arabic-nn-model-decode.cfg.template";

  private static final String DEFAULT_MODELS_DIR = "models.dli-levantine";
 // private static final String MODEL_CFG = CFG_TEMPLATE_DEFAULT.substring(0,CFG_TEMPLATE_DEFAULT.length()-".template".length());

 // private final Map<String, ASRParameters> languageLookUp = new HashMap<String, ASRParameters>();
  private final Cache<String, Scores> audioToScore;
  private final Map<String, String> properties;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param deployPath
   * @param properties
   */
  public ASRScoring(String deployPath, Map<String, String> properties) {
    super(deployPath);

    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    this.properties = properties;
    // not for now... later if dict gets too big
/*    ASRParameters arabic = ASRParameters.jload("Arabic", configFullPath);
    if (arabic == null) {
      logger.error("can't find Arabic parameters at " + configFullPath);
    } else {
      languageLookUp.put("Arabic", arabic);
    }*/
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight, boolean useScoreForBkgColor) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,
        sentence,
        scoringDir,imageOutDir,imageWidth,imageHeight, useScoreForBkgColor, Collections.EMPTY_LIST);
  }

  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight, boolean useScoreForBkgColor, List<String> lmSentences) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,
        sentence,
        scoringDir,imageOutDir,imageWidth,imageHeight, useScoreForBkgColor,lmSentences);
  }

  /**
   * Use hydec to do scoring
   *
   * @see #scoreRepeat(String, String, String, String, int, int, boolean)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence
   * @param scoringDir
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @return score info coming back from alignment/reco
   */
  private PretestScore scoreRepeatExercise(String testAudioDir, String testAudioFileNoSuffix,
                                           String sentence,
                                           String scoringDir,

                                           String imageOutDir,
                                           int imageWidth, int imageHeight, boolean useScoreForBkgColor, List<String> lmSentences) {
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
    double duration = new AudioCheck().getDurationInSeconds(wavFile);
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

    Scores scores;
    synchronized (this) {
      String tmpDir = Files.createTempDir().getAbsolutePath();

      boolean decode = !lmSentences.isEmpty();
      if (decode) {
        createSLFFile(lmSentences, tmpDir);
      }

      // String tmpDir = scoringDir + File.separator + TMP;
      Dirs dirs = pronz.dirs.Dirs$.MODULE$.apply(tmpDir, "", scoringDir, new Log(null, true));

      Audio testAudio = Audio$.MODULE$.apply(
          testAudioDir, testAudioFileNoSuffix,
          false /* notForScoring */, dirs);

      scores = audioToScore.getIfPresent(testAudioDir + File.separator + testAudioFileNoSuffix);

      if (scores == null) {
        scores = computeRepeatExerciseScores(testAudio, sentence, tmpDir, decode);
        audioToScore.put(testAudioDir + File.separator + testAudioFileNoSuffix, scores);
      } else {
        logger.info("found cached score for file '" + testAudioDir + File.separator + testAudioFileNoSuffix + "'");
      }
      //}
    }

    ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix, useScoreForBkgColor);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<Float>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo, duration);
    String recoSentence = getRecoSentence(eventAndFileInfo);
    recoSentence = recoSentence.replaceAll("sil","").trim();

    PretestScore pretestScore =
        new PretestScore(scores.hydecScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes, recoSentence);
    return pretestScore;
  }

  /**
   * Get the graded sentences.
   * Create an srilm file using ngram-count
   * Create an slf file using HBuild
   * Do the octal conversion to utf-8 text on the result
   *
   * This only works properly on the mac and linux, sorta emulated on win32
   * @param lmSentences
   * @param tmpDir
   */
  private void createSLFFile(List<String> lmSentences, String tmpDir) {
    File lmFile = writeLMToFile(lmSentences, tmpDir);
    String platform = Utils.package$.MODULE$.platform();
    String pathToBinDir = deployPath + File.separator + "scoring" + File.separator + "bin." + platform;
    logger.info("platform  "+platform + " bins " + pathToBinDir);
    File srilmFile = runNgramCount(tmpDir, lmFile, pathToBinDir);
    String slfFile = runHBuild(tmpDir,srilmFile,pathToBinDir);
    //if (new File(slfFile).exists()) {
    String convertedFile = tmpDir + File.separator + "smallLM.slf";
    doOctalConversion(slfFile, convertedFile);

    if (platform.startsWith("win")) {
      // hack -- get slf file from model dir
      String slfDefaultFile = getModelsDir() + File.separator + "smallLM.slf";
      //   new FileCopier().copy(slfFile,tmpDir +File.separator+"smallLM.slf");
      doOctalConversion(slfDefaultFile, convertedFile);
    }
  }

  private void doOctalConversion(String slfFile, String convertedFile) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(slfFile), FileExerciseDAO.ENCODING));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(convertedFile), FileExerciseDAO.ENCODING));          //     int count = 0;
      //   logger.debug("using install path " + installPath);
      String line2;
      while ((line2 = reader.readLine()) != null) {
        writer.write(package$.MODULE$.oct2string(line2).trim());
        writer.write("\n");
      }
      reader.close();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  /**
   * ngram-count -text smallLM.txt -lm smallLmOut.srilm -write-vocab out.vocab -order 2 -cdiscount 0.0001 –unk
   * ngram-count -text smallLM.txt -lm smallLmOut.srilm -write-vocab out.vocab -order 2 -cdiscount 0.0001 –unk
   * HBuild -n smallLmOut.srilm -s '<s>' '</s>' out.vocab smallLM.slf
   *
   * @param lmSentences
   * @param tmpDir
   * @return
   */

  private File writeLMToFile(List<String> lmSentences, String tmpDir) {
    try {
      File outFile = new File(tmpDir, "smallLM.txt");
      logger.info("wrote lm to " +outFile.getAbsolutePath());
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), FileExerciseDAO.ENCODING));
      for (String s : lmSentences) writer.write(s.trim().replaceAll("\\p{P}","") + "\n");
      writer.close();
      return outFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * ngram-count -text smallLM.txt -lm smallLmOut.srilm -write-vocab out.vocab -order 2 -cdiscount 0.0001 –unk
   * @param tmpDir
   * @param lmFile
   * @param pathToBinDir
   * @return
   */
  private File runNgramCount(String tmpDir, File lmFile, String pathToBinDir) {
    String srilm = tmpDir + File.separator + "smallLMOut.srilm";
    ProcessBuilder soxFirst = new ProcessBuilder(pathToBinDir +File.separator+"ngram-count",
        "-text",
        lmFile.getAbsolutePath(),
        "-lm",
        srilm,
        "-write-vocab",
        tmpDir + File.separator + "out.vocab",
        "-order",
        "2",
        "-cdiscount",
        "0.0001",
        "-unk"
        );

    try {
      new ProcessRunner().runProcess(soxFirst);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!new File(srilm).exists()) logger.error("didn't make " + srilm);
    return new File(srilm);
  }

  /**
   * HBuild -n smallLmOut.srilm -s '<s>' '</s>' out.vocab smallLM.slf
   * @param tmpDir
   * @param srilmFile
   * @param pathToBinDir
   */
  private String runHBuild(String tmpDir, File srilmFile, String pathToBinDir) {
    //String srilm = tmpDir + File.separator + "smallLMOut.srilm";
    logger.info("running hbuild on " + srilmFile);
    String slfOut = tmpDir + File.separator + "smallLMFromHBuild.slf";
    ProcessBuilder soxFirst = new ProcessBuilder(pathToBinDir +File.separator+"HBuild",
        "-n",
        srilmFile.getAbsolutePath(),
        "-s",
        "<s>",
        "</s>",
        tmpDir + File.separator + "out.vocab",
        slfOut
    );
    //String cmd = pathToBinDir +File.separator+"HBuild"

    logger.info("proc is " + new HashSet<String>(soxFirst.command())
    );

    try {
      new ProcessRunner().runProcess(soxFirst);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!new File(slfOut).exists()) logger.error("runHBuild didn't make " + slfOut);
    return slfOut;
  }

  /**
   * Make a map of event type to segment end times (so we can map clicks to which segment is clicked on).<br></br>
   * Note we have to adjust the last segment time to be the audio duration, so we can correct for wav vs mp3 time
   * duration differences (mp3 files being typically about 0.1 seconds longer than wav files).
   * The consumer of this map is at {@link mitll.langtest.client.scoring.ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)}
   *
   * @see #scoreRepeatExercise
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
      Float lastEndTime = times.isEmpty() ? (float)fileDuration : times.get(times.size() - 1);
      if (lastEndTime < fileDuration && !times.isEmpty()) {
       // logger.debug("setting last segment to end at end of file " + lastEndTime + " vs " + fileDuration);
        times.set(times.size() - 1,(float)fileDuration);
      }
    }
    return typeToEndTimes;
  }

  private String getRecoSentence(ImageWriter.EventAndFileInfo eventAndFileInfo) {
    StringBuilder b = new StringBuilder();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      if (key == NetPronImageType.WORD_TRANSCRIPT) {
        Map<Float, TranscriptEvent> timeToEvent = typeToEvents.getValue();
        for (Float timeStamp : timeToEvent.keySet()) {
          String event = timeToEvent.get(timeStamp).event;
          if (!event.equals("<s>") && !event.equals("</s>")) {
            b.append(event);
            b.append(" ");
          }
        }
      }
    }
    return b.toString().trim();
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
     * @param decode
     * @return
     */
  private Scores computeRepeatExerciseScores(Audio testAudio, String sentence, String tmpDir, boolean decode) {
    String platform = Utils.package$.MODULE$.platform();
    String modelsDir = getModelsDir();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.

    // do template replace on config file
    String configFile = getHydecConfigFile(tmpDir, modelsDir, decode);

    // do template replace on grammar file
    createGrammarFile(modelsDir);

    // do some sanity checking
    boolean configExists = new File(configFile).exists();
    String hldaDir = getProp(HLDA_DIR, HLDA_DIR_DEFAULT);
    String dictFile = modelsDir + File.separator + hldaDir +File.separator+ DICT_WO_SP;
    boolean dictExists   = new File(dictFile).exists();
    if (!configExists || !dictExists) {
      if (!configExists) logger.error("computeRepeatExerciseScores : Can't find config file at " + configFile);
      if (!dictExists)   logger.error("computeRepeatExerciseScores : Can't find dict file at " + dictFile);
      return getEmptyScores();
    }

    ASRParameters asrparametersFullPaths = getASRParameters(platform, configFile, dictFile);
    //logger.debug("using 'dict without sp' file " + dictFile);
    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut;
    long then = System.currentTimeMillis();
  //  synchronized (this) {   // hydec can't be called concurrently -- not thread safe?
      try {
        jscoreOut = testAudio.jscore(sentence, asrparametersFullPaths, new String[] {});
          float hydec_score = jscoreOut._1;
          logger.info("got score " + hydec_score +" and took " + (System.currentTimeMillis()-then) + " millis");

          return new Scores(hydec_score, jscoreOut._2);
      } catch (AssertionError e) {
        logger.error("Got assertion error " + e,e);
        return new Scores();
      } catch (Exception ee) {
          logger.info("Got " +ee);
      }

   // }
    logger.info("got score and took " + (System.currentTimeMillis()-then) + " millis");

    return new Scores();
  }

  /**
   * Creates a grammar file from the template file
   * @param modelsDir
   */
  private void createGrammarFile(String modelsDir) {
    String grammarAlignTemplate = modelsDir + File.separator + GRAMMAR_ALIGN_TEMPLATE;
    String grammarAlign = modelsDir + File.separator +GRAMMAR_ALIGN;
    Map<String,String> kv2 = new HashMap<String, String>();
    kv2.put(MODELS_DIR_VARIABLE, modelsDir);

    // grammar align file points to a dictionary file in the models directory...
    doTemplateReplace(grammarAlignTemplate,grammarAlign,kv2);
  }

  /**
   * Creates a hydec config file from a template file by doing variable substitution.<br></br>
   * Also use the properties map to look for variables.
   *
   * @see #computeRepeatExerciseScores(pronz.speech.Audio, String, String, boolean)
   * @param tmpDir
   * @param modelsDir
   * @return path to config file
   */
  private String getHydecConfigFile(String tmpDir, String modelsDir, boolean decode) {
    String platform = Utils.package$.MODULE$.platform();
    boolean onWindows = platform.startsWith("win");
    Map<String,String> kv = new HashMap<String, String>();

    String levantineNOutput = getProp(N_OUTPUT, LEVANTINE_N_OUTPUT);
    String nHidden = getProp(N_HIDDEN, N_HIDDEN_DEFAULT);
    String cfgTemplate = getProp(CFG_TEMPLATE_PROP, CFG_TEMPLATE_DEFAULT);
    if (onWindows) {
      tmpDir = tmpDir.replaceAll("\\\\","\\\\\\\\");
    }

    if (decode) {
      cfgTemplate = getProp(DECODE_CFG_TEMPLATE_PROP, DECODE_CFG_TEMPLATE_DEFAULT);
      kv.put(LM_TO_USE, tmpDir +File.separator + File.separator + "smallLM.slf"); // hack! TODO hack replace
     // new FileCopier().copy(modelsDir+File.separator+"phones.dict",tmpDir+File.separator +"dict");   // Audio.hscore in pron sets dictionary=this value
    }
    logger.info("using config from template " + cfgTemplate);

    kv.put(TEMP_DIR,tmpDir);
    kv.put(MODELS_DIR_VARIABLE, modelsDir);
    kv.put(N_OUTPUT, levantineNOutput);
    kv.put(N_HIDDEN, nHidden);
    kv.put(OPT_SIL, getProp(OPT_SIL, OPT_SIL_DEFAULT));
    kv.put(HLDA_DIR, getProp(HLDA_DIR, HLDA_DIR_DEFAULT));
    if (onWindows) kv.put("/","\\\\");

    // we need to create a custom config file for each run, complicating the caching of the ASRParameters...
    String modelCfg = cfgTemplate.substring(0, cfgTemplate.length() - ".template".length());

    String configFile = tmpDir+ File.separator+ modelCfg;

    String pathToConfigTemplate = scoringDir + File.separator + "configurations" + File.separator + cfgTemplate;
    logger.debug("template config is at " + pathToConfigTemplate + " map is " + kv);
    doTemplateReplace(pathToConfigTemplate,configFile,kv);
    return configFile;
  }

  private String getModelsDir() {
    String platform = Utils.package$.MODULE$.platform();

    String modelsDir = scoringDir + File.separator + getProp(MODELS_DIR_VARIABLE, DEFAULT_MODELS_DIR);
    if (platform.startsWith("win")) {
      modelsDir = modelsDir.replaceAll("\\\\","\\\\\\\\");
    }
    return modelsDir;
  }

  private String getProp(String var, String defaultValue) {
    return properties.containsKey(var) ? properties.get(var) : defaultValue;
  }

  //private ASRParameters cachedParams;
  private ASRParameters getASRParameters(String platform, String configFile, String dictFile) {
/*    if (false && cachedParams != null) return cachedParams;
    else {
      cachedParams = new ASRParameters(
          configFile,
          dictFile,
          //  asrparameters.letterToSoundClassString(),
          "corpus.ArabicLTS",
          platform);
      return cachedParams;
    }*/
    return new ASRParameters(
        configFile,
        dictFile,
        "corpus.ArabicLTS",
        platform);
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
/*  private void deleteTmpDir(String tmpDir) {
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
  }*/

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores);
  }

  public static void main(String [] arg) {
 /*   String deployPath1 = "C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\scoring";
    ASRScoring scoring = new ASRScoring(deployPath1, properties);
    Map<String,String> kv = new HashMap<String, String>();
    kv.put("TEMP_DIR","C:\\Users\\go22670\\AppData\\Local\\Temp\\1351790200971-0");
    kv.put("MODELS_DIR","models_dir_to_use");
    scoring.doTemplateReplace(deployPath1 + File.separator + "configurations" + File.separator +"levantine-nn-model.cfg.template",
        deployPath1 + File.separator + "configurations" + File.separator +"new-levantine-nn-model.cfg",kv);*/
    //String testAudioDir = "C:\\Users\\go22670\\DLITest\\LangTest\\war\\media\\ac-L0P-001";
    //PretestScore pretestScore = scoring.scoreRepeat(testAudioDir, "ad0035_ems", *//*testAudioDir, "ad0035_ems",*//* "This is a test.", "out", 1024, 100);
   // System.out.println("score " + pretestScore);
  }
}
