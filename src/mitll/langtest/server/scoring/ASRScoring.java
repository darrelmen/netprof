package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import corpus.ArabicLTS;
import corpus.EnglishLTS;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.AudioCheck;
import mitll.langtest.server.AudioConversion;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Does ASR scoring using hydec.  Results in either alignment or decoding, depending on the mode.
 * Decoding is used with autoCRT of audio.
 *
 * Takes the label files and generates transcript images for display in the client.
 *
 * User: go22670
 * Date: 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoring extends Scoring {
  private static final int FOREGROUND_VOCAB_LIMIT = 30;
  //private static final int VOCAB_SIZE_LIMIT = 50;
  private static Logger logger = Logger.getLogger(ASRScoring.class);

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

  private static final String HLDA_DIR_DEFAULT = "rsi-sctm-hlda";
  public static final String SMALL_LM_SLF = "smallLM.slf";

  private static final String CFG_TEMPLATE_PROP = "configTemplate";
  private static final String CFG_TEMPLATE_DEFAULT = "generic-nn-model.cfg.template";

  private static final String DECODE_CFG_TEMPLATE_PROP = "decodeConfigTemplate";
  private static final String DECODE_CFG_TEMPLATE_DEFAULT = "arabic-nn-model-decode.cfg.template";

  private static final String DEFAULT_MODELS_DIR = "models.dli-levantine";

  private final SmallVocabDecoder svDecoderHelper = new SmallVocabDecoder();

  /**
   * By keeping these here, we ensure that we only ever read the dictionary once
   */
  private HTKDictionary htkDictionary;
  private final LTS letterToSoundClass;
  private final Cache<String, Scores> audioToScore;
  private final Map<String, String> properties;
  private final String platform = Utils.package$.MODULE$.platform();
  private String language = "";
  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param deployPath
   * @param properties
   */
  public ASRScoring(String deployPath, Map<String, String> properties) {
    super(deployPath);

    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    this.properties = properties;
    this.language = properties.get("language") != null ? properties.get("language") : "";
    this.letterToSoundClass = language != null && language.equals("English") ? new EnglishLTS() : new ArabicLTS();
    readDictionary();
  }

/*  private Set<String> wordsInDict = new HashSet<String>();
  private void readDict() {
    String modelsDir = getModelsDir();

    String hldaDir = getProp(HLDA_DIR, HLDA_DIR_DEFAULT);
    String dictOverride = getProp(DICTIONARY, "");
    String dictFile = dictOverride.length() > 0 ?  modelsDir + File.separator + dictOverride :
      modelsDir + File.separator + hldaDir +File.separator+ DICT_WO_SP;
    boolean dictExists   = new File(dictFile).exists();

    if (dictExists) {
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), FileExerciseDAO.ENCODING));
        String line2;
        while ((line2 = reader.readLine()) != null) {
          String[] split = line2.split("\\s");
          String word = split[0];
          wordsInDict.add(word);
        }
        reader.close();
        logger.info("read dict " + dictFile + " and found " + wordsInDict.size() + " words");
      } catch (IOException e) {
        logger.error(e);
      }
    }
  }*/

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio(int, String, String, int, int, boolean, boolean, String, boolean)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @param useCache
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                  boolean decode, String tmpDir,
                                  boolean useCache) {
    return scoreRepeatExercise(testAudioDir,testAudioFileNoSuffix,
        sentence,
        scoringDir,imageOutDir,imageWidth,imageHeight, useScoreForBkgColor,
      decode, tmpDir,
      useCache);
  }

  /**
   * Use hydec to do scoring<br></br>
   *
   * Some magic happens in {@link #writeTranscripts(String, int, int, String, boolean)} where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   *
   * For alignment, of course, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is utterly unrelated to the reference.)).
   *
   * @see #scoreRepeat
   * @param testAudioDir where the audio is
   * @param testAudioFileNoSuffix file name without a suffix
   * @param sentence to align
   * @param scoringDir where the hydec subset is (models, bin.linux64, etc.)
   * @param imageOutDir where to write the images (audioImage)
   * @param imageWidth image width
   * @param imageHeight image height
   * @param useScoreForBkgColor true if we want to color the segments by score else all are gray
   * @param useCache
   * @return score info coming back from alignment/reco
   */
  private PretestScore scoreRepeatExercise(String testAudioDir, String testAudioFileNoSuffix,
                                           String sentence,
                                           String scoringDir,

                                           String imageOutDir,
                                           int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                           boolean decode, String tmpDir,
                                           boolean useCache) {
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

    Scores scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir,
      decode, tmpDir, useCache);
    if (scores == null) {
      logger.warn("getScoreForAudio failed to generate scores.");
      Random rand = new Random();
      return new PretestScore(rand.nextBoolean() ? 0.99f : 0.01f);
    }
    ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix, useScoreForBkgColor);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<Float>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo, duration);
    String recoSentence = getRecoSentence(eventAndFileInfo);

    PretestScore pretestScore =
        new PretestScore(scores.hydecScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes, recoSentence);
    return pretestScore;
  }

/*  public Scores decode(String testAudioDir, String testAudioFileNoSuffix,
                       String scoringDir,

                       List<String> lmSentences, List<String> background) {
    return getScoreForAudio(testAudioDir, testAudioFileNoSuffix, "", scoringDir, lmSentences, background);
  }*/

  public Scores align(String testAudioDir, String testAudioFileNoSuffix,
                      String sentence) {
    return getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir,
       false, Files.createTempDir().getAbsolutePath(), false);
  }

  /**
   * @see #scoreRepeatExercise
   * @param testAudioDir audio file directory
   * @param testAudioFileNoSuffix file name without suffix
   * @param sentence for alignment, the sentence to align, for decoding, the vocab list to use to filter against the dictionary
   * @param scoringDir war/scoring path
   * @param decode true if doing decoding, false for alignment
   * @param tmpDir to use to run hydec in
   * @param useCache cache scores so subsequent requests for the same audio file will get the cached score
   * @return Scores -- hydec score and event (word/phoneme) scores
   */
  private Scores getScoreForAudio(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence,
                                  String scoringDir,
                                  boolean decode, String tmpDir, boolean useCache) {
    String key = testAudioDir + File.separator + testAudioFileNoSuffix;
    Scores scores = useCache ? audioToScore.getIfPresent(key) : null;
    if (scores == null) {
      scores = calcScoreForAudio(testAudioDir,testAudioFileNoSuffix,sentence,scoringDir,
        decode,tmpDir);
      audioToScore.put(key, scores);
    }
    else {
      logger.debug("found cached score for file '" + key + "'");
    }
    return scores;
  }
  /**
   * There are two modes you can use to score the audio : align mode and decode mode
   * In align mode, the decoder figures out where the words and phonemes in the sentence occur in the audio.
   * In decode mode, given a lattice file
   * (HTK slf file) <a href="http://www1.icsi.berkeley.edu/Speech/docs/HTKBook/node293_mn.html">SLF Example</a>
   * will do decoding.
   * The event scores returned are a map of event type to event name to score (e.g. "words"->"dog"->0.5)
   * The score per audio file is cached in {@link #audioToScore}
   *
   * @see #scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence  only for align
   * @param scoringDir
   * @paramx lmSentences if empty, doing align, if not, doing decode!
   * @paramx background only for decode
   * @return Scores which is the overall score and the event scores
   */
  private Scores calcScoreForAudio(String testAudioDir, String testAudioFileNoSuffix,
                                   String sentence,
                                   String scoringDir,
                                   boolean decode, String tmpDir) {
    Dirs dirs = pronz.dirs.Dirs$.MODULE$.apply(tmpDir, "", scoringDir, new Log(null, true));
    //logger.debug("dirs is " + dirs + " tmp " + dirs.tmp());

    Audio testAudio = Audio$.MODULE$.apply(
      testAudioDir, testAudioFileNoSuffix,
      false /* notForScoring */, dirs);

    //logger.debug("testAudio is " + testAudio + " dir " + testAudio.dir());

    return computeRepeatExerciseScores(testAudio, sentence, tmpDir, decode, language);
  }

  /**
   * @see AutoCRTScoring#getASRScoreForAudio(java.io.File, java.util.List
   * @param lmSentences
   * @param background
   * @return
   */
  public String getUsedTokens(List<String> lmSentences, List<String> background) {
    List<String> backgroundVocab = svDecoderHelper.getVocab(background, SmallVocabDecoder.VOCAB_SIZE_LIMIT);
    return getUniqueTokensInLM(lmSentences, backgroundVocab);
  }

  /**
   * Get the unique set of tokens to use to filter against our full dictionary.
   * We check all these words for existence in the dictionary.
   *
   * Any OOV words have letter-to-sound called to create word->phoneme mappings.
   * This happens in {@see pronz.speech.Audio#hscore}
   *
   * @see #getUsedTokens(java.util.List, java.util.List)
   * @param lmSentences
   * @param backgroundVocab
   * @return
   */
  private String getUniqueTokensInLM(List<String> lmSentences, List<String> backgroundVocab) {
    String sentence;Set<String> backSet = new HashSet<String>(backgroundVocab);
    List<String> mergedVocab = new ArrayList<String>(backgroundVocab);
    List<String> foregroundVocab = svDecoderHelper.getSimpleVocab(lmSentences, FOREGROUND_VOCAB_LIMIT);
    for (String foregroundToken : foregroundVocab) {
      if (!backSet.contains(foregroundToken)) {
        mergedVocab.add(foregroundToken);
      }
    }
    StringBuilder builder = new StringBuilder();

    for (String token : mergedVocab) builder.append(token).append(" ");

    sentence = builder.toString();
    return sentence;
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

  /**
   * Take the events (originally from a .lab file generated in pronz) for WORDS and string them together into a
   * sentence.
   * @see #scoreRepeatExercise
   * @param eventAndFileInfo
   * @return
   */
  private String getRecoSentence(ImageWriter.EventAndFileInfo eventAndFileInfo) {
    StringBuilder b = new StringBuilder();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      if (key == NetPronImageType.WORD_TRANSCRIPT) {
        Map<Float, TranscriptEvent> timeToEvent = typeToEvents.getValue();
        for (Float timeStamp : timeToEvent.keySet()) {
          String event = timeToEvent.get(timeStamp).event;
          if (!event.equals("<s>") && !event.equals("</s>") && !event.equals("sil")) {
            String trim = event.trim();
            if (trim.length() > 0) {
              //logger.debug("Got " + event + " trim '" +trim+ "'");
              b.append(trim);
              b.append(" ");
            }
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
     * @see #calcScoreForAudio(String, String, String, String, boolean, String)
     * @param testAudio
     * @param sentence
     * @param decode
     * @param language
     * @return Scores - score for audio, given the sentence and event info
     */
  private Scores computeRepeatExerciseScores(Audio testAudio, String sentence, String tmpDir, boolean decode,
                                             String language) {
    //logger.debug("tmp dir " + tmpDir);
    String modelsDir = getModelsDir();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.

    // do template replace on config file
    String configFile = getHydecConfigFile(tmpDir, modelsDir, decode);

    // do template replace on grammar file
    //createGrammarFile(modelsDir);

    // do some sanity checking
    boolean configExists = new File(configFile).exists();
    if (!configExists) {
      logger.error("computeRepeatExerciseScores : Can't find config file at " + configFile);
      return getEmptyScores();
    }

    // there are currently only two letter-to-sound classes EnglishLTS and ArabicLTS.  What about french? spanish?
    boolean isArabicScript = !(language.equalsIgnoreCase("English"));
    if (!isArabicScript) logger.debug("using english LTS sound class since language is " + language);

    return getScoresFromHydec(testAudio, sentence, configFile);
  }

  private void readDictionary() {
    String dictFile = getDictFile(getModelsDir());
    boolean dictExists = new File(dictFile).exists();
    if (!dictExists) logger.error("readDictionary : Can't find dict file at " + dictFile);

    long then = System.currentTimeMillis();
    htkDictionary = new HTKDictionary(dictFile);
    long now = System.currentTimeMillis();
    int size = htkDictionary.size(); // force read from lazy val
    logger.debug("read dict " + dictFile + " of size " +size + " in " + (now-then) + " millis");
  }

  /**
   * @see #computeRepeatExerciseScores(pronz.speech.Audio, String, String, boolean, String)
   * @param testAudio
   * @param sentence
   * @param configFile
   * @return
   */
  private Scores getScoresFromHydec(Audio testAudio, String sentence, String configFile) {
    Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut;
    long then = System.currentTimeMillis();
    try {
      jscoreOut = testAudio.jscore(sentence, htkDictionary, letterToSoundClass, configFile);
      float hydec_score = jscoreOut._1;
      long timeToRunHydec = System.currentTimeMillis() - then;
      logger.info("got score " + hydec_score +" and took " + timeToRunHydec + " millis");

      return new Scores(hydec_score, jscoreOut._2);
    } catch (AssertionError e) {
      logger.error("Got assertion error " + e,e);
      return new Scores();
    } catch (Exception ee) {
      logger.warn("Running align/decode on " + sentence +" Got " + ee, ee);
    }

    long timeToRunHydec = System.currentTimeMillis() - then;
    logger.warn("got bad score and took " + timeToRunHydec + " millis");

    Scores scores = new Scores();
    scores.hydecScore = -1;
    return scores;
  }

  private String getDictFile(String modelsDir) {
    String hldaDir = getProp(HLDA_DIR, HLDA_DIR_DEFAULT);
    String dictFile = modelsDir + File.separator + hldaDir + File.separator + DICT_WO_SP;
    boolean dictExists = new File(dictFile).exists();
    if (!dictExists) {
      dictFile = modelsDir + File.separator + DICT_WO_SP;
    }
    return dictFile;
  }

  /**
   * Creates a grammar file from the template file
   * TODO : this isn't required anymore - remove it
   * @param modelsDir
   * @deprecated shouldn't be called anymore
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
   * @see #computeRepeatExerciseScores
   * @param tmpDir where hydec will run and where the config file will be
   * @param modelsDir to point to, for config to use
   * @param decode if using the decoder cfg
   * @return path to config file
   */
  private String getHydecConfigFile(String tmpDir, String modelsDir, boolean decode) {
    boolean onWindows = platform.startsWith("win");
    Map<String,String> kv = new HashMap<String, String>();

    String levantineNOutput = getProp(N_OUTPUT, LEVANTINE_N_OUTPUT);
    String nHidden = getProp(N_HIDDEN, N_HIDDEN_DEFAULT);
    String cfgTemplate = getProp(CFG_TEMPLATE_PROP, CFG_TEMPLATE_DEFAULT);
    if (onWindows) {
      tmpDir = doWindowsSlashReplace(tmpDir);
    }

    if (decode) {
      cfgTemplate = getProp(DECODE_CFG_TEMPLATE_PROP, DECODE_CFG_TEMPLATE_DEFAULT);
      kv.put(LM_TO_USE, tmpDir +File.separator +File.separator + SMALL_LM_SLF); // hack! TODO hack replace
     // new FileCopier().copy(modelsDir+File.separator+"phones.dict",tmpDir+File.separator +"dict");   // Audio.hscore in pron sets dictionary=this value
    }
    //logger.info("using config from template " + cfgTemplate);

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
    //logger.debug("getHydecConfigFile : tmpDir is " + tmpDir);

    String pathToConfigTemplate = scoringDir + File.separator + "configurations" + File.separator + cfgTemplate;
    logger.debug("template config is at " + pathToConfigTemplate + " map is " + kv);
    doTemplateReplace(pathToConfigTemplate,configFile,kv);
    return configFile;
  }

  private String getModelsDir() {
    String modelsDir = scoringDir + File.separator + getProp(MODELS_DIR_VARIABLE, DEFAULT_MODELS_DIR);
    if (platform.startsWith("win")) {
      modelsDir = doWindowsSlashReplace(modelsDir);
    }
    return modelsDir;
  }

  private String doWindowsSlashReplace(String tmpDir) {
    return tmpDir.replaceAll("\\\\","\\\\\\\\");
  }

  private String getProp(String var, String defaultValue) {
    return properties.containsKey(var) ? properties.get(var) : defaultValue;
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

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores);
  }
}
