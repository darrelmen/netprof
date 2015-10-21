package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.EventAndFileInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Does ASR scoring using hydec.  Results in either alignment or decoding, depending on the mode.
 * Decoding is used with autoCRT of audio.
 * <p>
 * Takes the label files and generates transcript images for display in the client.
 * <p>
 * User: go22670
 * Date: 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoring extends Scoring implements CollationSort, ASR {
  private static final Logger logger = Logger.getLogger(ASRScoring.class);
  private final Cache<String, Scores> audioToScore;

  private static final boolean DEBUG = false;

  /**
   * @param deployPath
   * @param serverProperties
   * @param langTestDatabase
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   */
  public ASRScoring(String deployPath, ServerProperties serverProperties, LogAndNotify langTestDatabase) {
    super(deployPath, serverProperties, langTestDatabase);
    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();
  }


  /**
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence              that should be what the test audio contains
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @param useCache
   * @param prefix
   * @param precalcResult
   * @param usePhoneToDisplay
   * @return PretestScore object
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence, Collection<String> lmSentences, String imageOutDir,
                                  int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                  boolean decode, String tmpDir,
                                  boolean useCache, String prefix, Result precalcResult, boolean usePhoneToDisplay) {
    return scoreRepeatExercise(testAudioDir, testAudioFileNoSuffix,
        sentence,
        scoringDir,
        imageOutDir, imageWidth, imageHeight, useScoreForBkgColor,
        decode, tmpDir,
        useCache, prefix, precalcResult, usePhoneToDisplay);
  }

  /**
   * Use hydec to do scoring<br></br>
   * <p>
   * Some magic happens in {@link Scoring#writeTranscripts} where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   * <p>
   * For alignment, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is unrelated to the expected.)).
   * <p>
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
   * @param precalcResult
   * @return score info coming back from alignment/reco
   * @see ASR#scoreRepeat
   */
  private PretestScore scoreRepeatExercise(String testAudioDir,
                                           String testAudioFileNoSuffix,
                                           String sentence,
                                           String scoringDir,

                                           String imageOutDir,
                                           int imageWidth, int imageHeight,
                                           boolean useScoreForBkgColor,
                                           boolean decode, String tmpDir,
                                           boolean useCache, String prefix,
                                           Result precalcResult,
                                           boolean usePhoneToDisplay) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";

    boolean b = checkLTS(sentence).isEmpty();

    if (!b) {
      logger.info("scoreRepeatExercise for " + testAudioFileNoSuffix + " under " + testAudioDir + " '" + sentence + "' is not in lts");
    }
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

    // resample if needed
    try {
      String audioDir = testAudioDir;
      if (mustPrepend) {
        audioDir = deployPath + File.separator + audioDir;
        if (!new File(audioDir).exists()) logger.error("Couldn't find " + audioDir);
        else testAudioDir = audioDir;
      }
      testAudioFileNoSuffix = new AudioConversion().convertTo16Khz(audioDir, testAudioFileNoSuffix);
    } catch (UnsupportedAudioFileException e) {
      logger.error("Got " + e, e);
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX;
    }

    Scores scores;
    JSONObject jsonObject = null;

    PrecalcScores precalcScores = new PrecalcScores(props, precalcResult, usePhoneToDisplay);

    if (precalcScores.isValid()) {
      scores = precalcScores.getScores();
      jsonObject = precalcScores.getJsonObject();
    } else {
      if (precalcResult != null) {
        logger.debug("unusable precalc result, so recalculating : " + precalcResult);
      }
      scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir, useCache);
    }
    if (scores == null) {
      logger.error("getScoreForAudio failed to generate scores.");
      return new PretestScore(0.01f);
    }
    return getPretestScore(imageOutDir, imageWidth, imageHeight, useScoreForBkgColor, decode, prefix, noSuffix, wavFile,
        scores, jsonObject, usePhoneToDisplay);
  }

  /**
   * Make image files for words, and phones, find out the reco sentence from the events.
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @param decode
   * @param prefix
   * @param noSuffix
   * @param wavFile
   * @param scores
   * @param jsonObject          if not-null, uses it to make the word and phone transcripts instead of .lab files
   * @return
   * @see #scoreRepeatExercise
   */
  private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                       boolean decode, String prefix, String noSuffix, File wavFile, Scores scores,
                                       JSONObject jsonObject,
                                       boolean usePhoneToDisplay) {
    //  logger.debug("getPretestScore jsonObject " + jsonObject);
    //  logger.debug("getPretestScore scores     " + scores);

    boolean reallyUsePhone = usePhoneToDisplay || props.usePhoneToDisplay();

    // we cache the images, so we don't want to return an image for a different option...
    String prefix1 = prefix + (useScoreForBkgColor ? "bkgColorForRef" : "") + (reallyUsePhone ? "_phoneToDisp" : "");

    //logger.debug("getPretestScore prefix " + prefix1);
    if (jsonObject != null) logger.debug("generating images from " + jsonObject);

    EventAndFileInfo eventAndFileInfo = jsonObject == null ?
        writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
            useScoreForBkgColor,
            prefix1, "", decode, false, reallyUsePhone) :
        writeTranscriptsCached(imageOutDir, imageWidth, imageHeight, noSuffix,
            useScoreForBkgColor,
            prefix1, "", decode, false, jsonObject, reallyUsePhone);

    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo);
    String recoSentence = getRecoSentence(eventAndFileInfo);

    double duration = new AudioCheck().getDurationInSeconds(wavFile);

    return new PretestScore(scores.hydraScore,
        getPhoneToScore(scores),
        getWordToScore(scores),
        sTypeToImage, typeToEndTimes, recoSentence, (float) duration, scores.getProcessDur());
  }

  /**
   * @param testAudioDir          audio file directory
   * @param testAudioFileNoSuffix file name without suffix
   * @param sentence              for alignment, the sentence to align, for decoding, the vocab list to use to filter against the dictionary
   * @param scoringDir            war/scoring path
   * @param decode                true if doing decoding, false for alignment
   * @param tmpDir                to use to run hydec in
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
   * @return Scores -- hydec score and event (word/phoneme) scores
   * @see #scoreRepeatExercise
   */
  private Scores getScoreForAudio(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence,
                                  String scoringDir,
                                  boolean decode, String tmpDir, boolean useCache) {
    String key = testAudioDir + File.separator + testAudioFileNoSuffix;
    Scores scores = useCache ? audioToScore.getIfPresent(key) : null;

    if (isMandarin) {
      sentence = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") + getSegmented(sentence.trim()); // segmentation method will filter out the UNK model
    }
    if (scores == null) {
      if (DEBUG)
        logger.debug("no cached score for file '" + key + "', so doing " + (decode ? "decoding" : "alignment") + " on " + sentence);
      scores = calcScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir);
      audioToScore.put(key, scores);
    } else {
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
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence              only for align
   * @param scoringDir
   * @return Scores which is the overall score and the event scores
   * @see #getScoreForAudio(String, String, String, String, boolean, String, boolean)
   * @see #scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String, Result)
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
   * @return
   * @see AutoCRTScoring#getASRScoreForAudio(File, Collection, boolean, boolean)
   */
  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    return getUniqueTokensInLM(lmSentences, getSmallVocabDecoder().getVocab(background, VOCAB_SIZE_LIMIT));
  }

  /**
   * Get the unique set of tokens to use to filter against our full dictionary.
   * We check all these words for existence in the dictionary.
   * <p>
   * Any OOV words have letter-to-sound called to create word->phoneme mappings.
   * This happens in {@see pronz.speech.Audio#hscore}
   *
   * @param lmSentences
   * @param backgroundVocab
   * @return
   * @see #getUsedTokens
   */
  private String getUniqueTokensInLM(Collection<String> lmSentences, List<String> backgroundVocab) {
    String sentence;
    Set<String> backSet = new HashSet<String>(backgroundVocab);
    List<String> mergedVocab = new ArrayList<String>(backgroundVocab);
    List<String> foregroundVocab = getSmallVocabDecoder().getSimpleVocab(lmSentences, FOREGROUND_VOCAB_LIMIT);
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
   * @param eventAndFileInfo
   * @return
   * @see #scoreRepeatExercise
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToEndTimes(EventAndFileInfo eventAndFileInfo) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = eventAndFileInfo.typeToEvent;

    return new ParseResultJson(props).getNetPronImageTypeToEndTimes(typeToEvent);
  }

/*  private Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeToEndTimes(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<TranscriptSegment>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
      }
    }

    return typeToEndTimes;
  }*/

  /**
   * Take the events (originally from a .lab file generated in pronz) for WORDS and string them together into a
   * sentence.
   *
   * @param eventAndFileInfo
   * @return
   * @see #scoreRepeatExercise
   */
  private String getRecoSentence(EventAndFileInfo eventAndFileInfo) {
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
   * Filter out sil.
   * <p>
   * Make sure that when we scale the phone scores by {@link #SCORE_SCALAR} we do it for both the scores and the image.
   * <br></br>
   * get the phones for display in the phone accuracy pane
   *
   * @param scores from hydec
   * @return map of phone name to score
   */
  private Map<String, Float> getPhoneToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get(Scores.PHONES);
    return getTokenToScore(phones);
  }

  private Map<String, Float> getWordToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get(Scores.WORDS);
    return getTokenToScore(phones);
  }

  private Map<String, Float> getTokenToScore(Map<String, Float> phones) {
    if (phones == null) {

//            logger.warn("getTokenToScore : no scores in " + scores.eventScores + " for '" + token + "'");
      return Collections.emptyMap();
    } else {
      Map<String, Float> phoneToScore = new HashMap<String, Float>();
      for (Map.Entry<String, Float> phoneScorePair : phones.entrySet()) {
        String key = phoneScorePair.getKey();
        if (!key.equals("sil")) {
          phoneToScore.put(key, Math.min(1.0f, phoneScorePair.getValue()));
        }
      }
      return phoneToScore;
    }
  }

  /**
   * Assumes that testAudio was recorded through the UI, which should prevent audio that is too short or too long.
   *
   * @param testAudio
   * @param sentence
   * @param decode
   * @return Scores - score for audio, given the sentence and event info
   * @see #calcScoreForAudio(String, String, String, String, boolean, String)
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
    double hydecScore = scoresFromHydec.hydraScore;
    if (hydecScore > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
        //logger.debug("deleting " + tmpDir + " since score is " +hydecScore);
        FileUtils.deleteDirectory(new File(tmpDir));
      } catch (IOException e) {
        logger.error("Deleting dir " + tmpDir + " got " + e, e);
      }
    }
    return scoresFromHydec;
  }

  /**
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   *
   * @param testAudio
   * @param sentence
   * @param configFile
   * @return
   * @see SmallVocabDecoder
   * @see #computeRepeatExerciseScores(pronz.speech.Audio, String, String, boolean)
   */
  private Scores getScoresFromHydec(Audio testAudio, String sentence, String configFile) {
    sentence = svd.getTrimmed(sentence);
    long then = System.currentTimeMillis();
//    logger.debug("getScoresFromHydec scoring '" + sentence +"' (" +sentence.length()+ " ) with " +
//            "LTS " + letterToSoundClass + " against " + testAudio + " with " + configFile);

    try {
      Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut =
          testAudio.jscore(sentence, htkDictionary, letterToSoundClass, configFile);
      float hydec_score = jscoreOut._1;
      long timeToRunHydec = System.currentTimeMillis() - then;

      logger.debug("getScoresFromHydec  : scoring '" + sentence + "' (" + sentence.length() + ") got score " + hydec_score +
          " and took " + timeToRunHydec + " millis");

      Map<String, Map<String, Float>> stringMapMap = jscoreOut._2;
      //logger.debug("hydec output " + stringMapMap);

      return new Scores(hydec_score, stringMapMap, (int) timeToRunHydec);
    } catch (AssertionError e) {
      logger.error("Got assertion error " + e, e);
      return new Scores((int) (System.currentTimeMillis() - then));
    } catch (Exception ee) {
      String msg = "Running align/decode on " + sentence;
      logger.warn(msg + " Got " + ee.getMessage());

      langTestDatabase.logAndNotifyServerException(ee, msg);
    }

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.warn("getScoresFromHydec : scoring '" + sentence + "' (" + sentence.length() + " ) : got bad score and took " + timeToRunHydec + " millis");

    Scores scores = new Scores((int) timeToRunHydec);
    scores.hydraScore = -1;
    return scores;
  }

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores, 0);
  }
}
