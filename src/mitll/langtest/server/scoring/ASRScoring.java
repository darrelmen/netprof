package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
  private static final double KEEP_THRESHOLD = 0.3;
  private static final Logger logger = Logger.getLogger(ASRScoring.class);
  private static final boolean DEBUG = false;

  private static final int FOREGROUND_VOCAB_LIMIT = 100;
  private static final int VOCAB_SIZE_LIMIT = 200;

  public static final String SMALL_LM_SLF = "smallLM.slf";

  private SmallVocabDecoder svDecoderHelper = null;
  private LangTestDatabaseImpl langTestDatabase;

  /**
   * By keeping these here, we ensure that we only ever read the dictionary once
   */
  private HTKDictionary htkDictionary;
  private final LTS letterToSoundClass;
  private final Cache<String, Scores> audioToScore;
  private final ConfigFileCreator configFileCreator;
  private final boolean isMandarin;

  /**
   * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
   * If the score was below a threshold, or the magic -1, we keep it around for future study.
   */
  private double lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring()
   * @param deployPath
   * @param properties
   * @param langTestDatabase
   */
  public ASRScoring(String deployPath, Map<String, String> properties, LangTestDatabaseImpl langTestDatabase) {
    this(deployPath, properties, (HTKDictionary)null);
    this.langTestDatabase = langTestDatabase;
    readDictionary();
    makeDecoder();
  }

  private String languageProperty;

  /**
   * @see #ASRScoring(String, java.util.Map, mitll.langtest.server.LangTestDatabaseImpl)
   * @param deployPath
   * @param properties
   * @param dict
   */
  private ASRScoring(String deployPath, Map<String, String> properties, HTKDictionary dict) {
    super(deployPath);
    lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    languageProperty = properties.get("language");
    String language = languageProperty != null ? languageProperty : "";

    isMandarin = language.equalsIgnoreCase("mandarin");
    this.letterToSoundClass = new LTSFactory().getLTSClass(language);
    this.htkDictionary = dict;
    makeDecoder();
    if (dict != null) logger.debug("htkDictionary size is " + dict.size());
    this.configFileCreator = new ConfigFileCreator(properties, letterToSoundClass, scoringDir);
  }

  private void makeDecoder() {
    if (svDecoderHelper == null && htkDictionary != null) {
      svDecoderHelper = new SmallVocabDecoder(htkDictionary);
    }
  }

  public SmallVocabDecoder getSmallVocabDecoder() { return svDecoderHelper; }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#checkLTS(String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#isValidForeignPhrase(String)
   * @param foreignLanguagePhrase
   * @return
   */
  public boolean checkLTS(String foreignLanguagePhrase) { return checkLTS(letterToSoundClass, foreignLanguagePhrase); }

  /**
   * So chinese is special -- it doesn't do lts -- it just uses a dictionary
   * @see mitll.langtest.server.LangTestDatabaseImpl#isValidForeignPhrase(String)
   * @param lts
   * @param foreignLanguagePhrase
   * @return
   */
  private boolean checkLTS(LTS lts, String foreignLanguagePhrase) {
    SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
    Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

    String language = isMandarin ? " MANDARIN " : "";
    //logger.debug("checkLTS '" + language + "' tokens : '" +tokens +"'");

    try {
      int i = 0;
      for (String token : tokens) {
        if (isMandarin) {
          String segmentation = smallVocabDecoder.segmentation(token.trim());
          if (segmentation.isEmpty()) {
            logger.debug("checkLTS: mandarin token : " + token + " invalid!");
            return false;
          }
        } else {
          String[][] process = lts.process(token);
          if (process == null || process.length == 0 || process[0].length == 0 ||
            process[0][0].length() == 0 || (process.length == 1 && process[0].length == 1 && process[0][0].equals("aa"))) {
            boolean htkEntry = htkDictionary.contains(token);
            if (!htkEntry && !htkDictionary.isEmpty()) {
              logger.warn("checkLTS with " + lts + "/" + languageProperty + " token #" +i+
                " : '" + token + "' hash " + token.hashCode()+
                " is invalid in " + foreignLanguagePhrase+
                " and not in dictionary (" + htkDictionary.size()+
                ")");
              return false;
            }
          }
        }
        i++;
      }
    } catch (Exception e) {
      logger.error("lts " + language + "/" + lts + " failed on '" + foreignLanguagePhrase +"'", e);
      return false;
    }
//    logger.debug("phrase '" +foreignLanguagePhrase+ "' is valid.");
    return true;
  }

  /**
   * For chinese, maybe later other languages.
   * @param longPhrase
   * @return
   */
  private String getSegmented(String longPhrase) {
    Collection<String> tokens = svDecoderHelper.getTokens(longPhrase);
    StringBuilder builder = new StringBuilder();
    for (String token : tokens) {
      builder.append(svDecoderHelper.segmentation(token.trim()));
      builder.append(" ");
    }
    String s = builder.toString();
   // logger.debug("getSegmented phrase '" + longPhrase + "' -> '" + s + "'");

    return s;
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @param useCache
   * @param prefix
   * @return PretestScore object
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, String imageOutDir,
                                  int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                  boolean decode, String tmpDir,
                                  boolean useCache, String prefix) {
    return scoreRepeatExercise(testAudioDir, testAudioFileNoSuffix,
      sentence,
      scoringDir, imageOutDir, imageWidth, imageHeight, useScoreForBkgColor,
      decode, tmpDir,
      useCache, prefix);
  }

  /**
   * Use hydec to do scoring<br></br>
   * <p/>
   * Some magic happens in {@link Scoring#writeTranscripts(String, int, int, String, boolean, String, String, boolean)} where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   * <p/>
   * For alignment, of course, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is utterly unrelated to the reference.)).
   * <p/>
   * Audio file must be a wav file, but can be any sample rate - if not 16K will be sampled down to 16K.
   *
   * @param testAudioDir          where the audio is
   * @param testAudioFileNoSuffix file name without a suffix - wav file, any sample rate
   * @param sentence              to align
   * @param scoringDir            where the hydec subset is (models, bin.linux64, etc.)
   * @param imageOutDir           where to write the images (audioImage)
   * @param imageWidth            image width
   * @param imageHeight           image height
   * @param useScoreForBkgColor   true if we want to color the segments by score else all are gray
   * @param decode                if true, skips writing image files
   * @param tmpDir                where to run hydec
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
   * @param prefix                on the names of the image files, if they are written
   * @return score info coming back from alignment/reco
   * @see #scoreRepeat
   */
  private PretestScore scoreRepeatExercise(String testAudioDir,
                                           String testAudioFileNoSuffix,
                                           String sentence,
                                           String scoringDir,

                                           String imageOutDir,
                                           int imageWidth, int imageHeight,
                                           boolean useScoreForBkgColor,
                                           boolean decode, String tmpDir,
                                           boolean useCache, String prefix) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";

    boolean b = checkLTS(sentence);
    //logger.debug("scoreRepeatExercise for " + testAudioFileNoSuffix + " under " + testAudioDir + " check lts = " + b);
    File wavFile = new File(pathname);
    boolean mustPrepend = false;
    if (!wavFile.exists() && deployPath != null) {
      //logger.debug("trying new path for " + pathname + " under " + deployPath);
      wavFile = new File(deployPath + File.separator + pathname);
      mustPrepend = true;
    }
    if (!wavFile.exists()) {
      logger.error("scoreRepeatExercise : Can't find audio wav file at : " + wavFile.getAbsolutePath());
      return new PretestScore();
    }
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
      logger.error("Got " +e,e);
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX;
    }

    Scores scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir, useCache);
    if (scores == null) {
      logger.error("getScoreForAudio failed to generate scores.");
      return new PretestScore(0.01f);
    }
    return getPretestScore(imageOutDir, imageWidth, imageHeight, useScoreForBkgColor, decode, prefix, noSuffix, wavFile, scores);
  }

  private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                       boolean decode, String prefix, String noSuffix, File wavFile, Scores scores) {
    ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
      useScoreForBkgColor,
      prefix + (useScoreForBkgColor ? "bkgColorForRef" : ""), "", decode);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo);
    String recoSentence = getRecoSentence(eventAndFileInfo);

    double duration = new AudioCheck().getDurationInSeconds(wavFile);
    return new PretestScore(scores.hydecScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes, recoSentence, (float) duration);
  }

/*  public Scores decode(String testAudioDir, String testAudioFileNoSuffix,
                       String scoringDir,

                       List<String> lmSentences, List<String> background) {
    return getScoreForAudio(testAudioDir, testAudioFileNoSuffix, "", scoringDir, lmSentences, background);
  }*/

  /**
   * @seex mitll.langtest.server.audio.SplitAudio#getAlignmentScores(ASRScoring, String, String, String, String)
   * @paramx testAudioDir
   * @paramx testAudioFileNoSuffix
   * @paramx sentence
   * @return
   */
/*
  public Scores align(String testAudioDir, String testAudioFileNoSuffix,
                      String sentence) {
    return getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir,
       false, Files.createTempDir().getAbsolutePath(), false);
  }
*/

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

    if (isMandarin) {
      sentence = getSegmented(sentence.trim());
    }
    if (scores == null) {
      scores = calcScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir);
      audioToScore.put(key, scores);
    }
    else {
      if (DEBUG) logger.debug("found cached score for file '" + key + "'");
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
   * @see #getScoreForAudio(String, String, String, String, boolean, String, boolean)
   * @see #scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String)
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence  only for align
   * @param scoringDir
   * @return Scores which is the overall score and the event scores
   */
  private Scores calcScoreForAudio(String testAudioDir, String testAudioFileNoSuffix,
                                   String sentence,
                                   String scoringDir,
                                   boolean decode, String tmpDir) {
    Dirs dirs = pronz.dirs.Dirs$.MODULE$.apply(tmpDir, "", scoringDir, new Log(null, true));
/*    if (false) logger.debug("dirs is " + dirs +
      " audio dir " + testAudioDir + " audio " + testAudioFileNoSuffix + " sentence " + sentence + " decode " + decode + " scoring dir " + scoringDir);
*/
    Audio testAudio = Audio$.MODULE$.apply(
      testAudioDir, testAudioFileNoSuffix,
      false /* notForScoring */, dirs);

    //logger.debug("testAudio is " + testAudio + " dir " + testAudio.dir());

    return computeRepeatExerciseScores(testAudio, sentence, tmpDir, decode);
  }

  /**
   * @param lmSentences
   * @param background
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio(java.io.File, java.util.Collection)
   * @return
   */
  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    List<String> backgroundVocab = svDecoderHelper.getVocab(background, VOCAB_SIZE_LIMIT);
    return getUniqueTokensInLM(lmSentences, backgroundVocab);
  }

  /**
   * Get the unique set of tokens to use to filter against our full dictionary.
   * We check all these words for existence in the dictionary.
   *
   * Any OOV words have letter-to-sound called to create word->phoneme mappings.
   * This happens in {@see pronz.speech.Audio#hscore}
   *
   * @see #getUsedTokens
   * @param lmSentences
   * @param backgroundVocab
   * @return
   */
  private String getUniqueTokensInLM(Collection<String> lmSentences, List<String> backgroundVocab) {
    String sentence;
    Set<String> backSet = new HashSet<String>(backgroundVocab);
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
   * @return
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToEndTimes(ImageWriter.EventAndFileInfo eventAndFileInfo) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) { typeToEndTimes.put(key, endTimes = new ArrayList<TranscriptSegment>()); }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
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
     * @return Scores - score for audio, given the sentence and event info
     */
  private Scores computeRepeatExerciseScores(Audio testAudio, String sentence, String tmpDir, boolean decode) {
    String modelsDir = configFileCreator.getModelsDir();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.

    // do template replace on config file
    String configFile = configFileCreator.getHydecConfigFile(tmpDir, modelsDir, decode);

    // do some sanity checking
    boolean configExists = new File(configFile).exists();
    if (!configExists) {
      logger.error("computeRepeatExerciseScores : Can't find config file at " + configFile);
      return getEmptyScores();
    }

    Scores scoresFromHydec = getScoresFromHydec(testAudio, sentence, configFile);
    double hydecScore = scoresFromHydec.hydecScore;
    if (/*hydecScore != -1 ||*/ hydecScore > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
        //logger.debug("deleting " + tmpDir + " since score is " +hydecScore);
        FileUtils.deleteDirectory(new File(tmpDir));
      } catch (IOException e) {
        logger.error("Deleting dir " + tmpDir + " got " +e,e);
      }
    }
    return scoresFromHydec;
  }

  private void readDictionary() { htkDictionary = makeDict(); }

  /**
   * @see #readDictionary()
   * @return
   */
  private HTKDictionary makeDict() {
    String dictFile = configFileCreator.getDictFile();
    if (new File(dictFile).exists()) {
      long then = System.currentTimeMillis();
      HTKDictionary htkDictionary = new HTKDictionary(dictFile);
      long now = System.currentTimeMillis();
      int size = htkDictionary.size(); // force read from lazy val
      if (now - then > 100) {
        logger.debug("read dict " + dictFile + " of size " + size + " in " + (now - then) + " millis");
      }
      return htkDictionary;
    }
    else {
      logger.warn("makeDict : Can't find dict file at " + dictFile);
      return new HTKDictionary();
    }
  }

  /**
   * @see #computeRepeatExerciseScores(pronz.speech.Audio, String, String, boolean)
   * @param testAudio
   * @param sentence
   * @param configFile
   * @return
   */
  private Scores getScoresFromHydec(Audio testAudio, String sentence, String configFile) {
    sentence = sentence.replaceAll("\\p{Z}+", " ");
    long then = System.currentTimeMillis();

    //logger.debug("getScoresFromHydec scoring '" + sentence +"' (" +sentence.length()+ " )");

    try {
      Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut =
        testAudio.jscore(sentence, htkDictionary, letterToSoundClass, configFile);
      float hydec_score = jscoreOut._1;
      long timeToRunHydec = System.currentTimeMillis() - then;
      logger.debug("getScoresFromHydec : scoring sentence " +sentence.length()+" characters long, got score " + hydec_score +
        " and took " + timeToRunHydec + " millis");

      return new Scores(hydec_score, jscoreOut._2);
    } catch (AssertionError e) {
      logger.error("Got assertion error " + e,e);
      return new Scores();
    } catch (Exception ee) {
      logger.warn("Running align/decode on " + sentence +" Got " + ee, ee);
      if (langTestDatabase != null) langTestDatabase.logAndNotifyServerException(ee);
    }

    long timeToRunHydec = System.currentTimeMillis() - then;
    logger.warn("got bad score and took " + timeToRunHydec + " millis");

    Scores scores = new Scores();
    scores.hydecScore = -1;
    return scores;
  }

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores);
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getValidPhrases(java.util.Collection)
   * @param phrases
   * @return
   */
  public Collection<String> getValidPhrases(Collection<String> phrases) { return getValidSentences(phrases); }

  /**
   * @see #isValid(String)
   * @param phrase
   * @return
   */
  private boolean isPhraseInDict(String phrase) {  return letterToSoundClass.process(phrase) != null;  }

  /**
   * @see #getValidPhrases(java.util.Collection)
   * @param sentences
   * @return
   */
  private Collection<String> getValidSentences(Collection<String> sentences) {
    Set<String> filtered = new TreeSet<String>();
    Set<String> skipped = new TreeSet<String>();

    for (String sentence : sentences) {
      Collection<String> tokens = svDecoderHelper.getTokens(sentence);
      boolean valid = true;
      for (String token : tokens) {
        if (!isValid(token)) {
          //logger.warn("\tgetValidSentences : token '" + token + "' is not in dictionary.");

          valid = false;
        }
      }
      if (valid) filtered.add(sentence);
      else {
        skipped.add(sentence);
       // logger.warn("getValidSentences : skipping '" + sentence + "' which is not in dictionary.");
      }
    }

    if (!skipped.isEmpty()) {
      logger.warn("getValidSentences : skipped " + skipped.size() + " sentences : " + skipped  );
    }

    return filtered;
  }

  /**
   * @see #getValidSentences(java.util.Collection)
   * @param token
   * @return
   */
  private boolean isValid(String token) { return checkToken(token) && isPhraseInDict(token);  }

  private boolean checkToken(String token) {
    boolean valid = true;
    if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL)) return true;
    for (int i = 0; i < token.length() && valid; i++) {
      char c = token.charAt(i);
      if (Character.isDigit(c)) {
        valid = false;
      }
      if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
        valid = false;
      }
    }
    return valid;
  }
}
