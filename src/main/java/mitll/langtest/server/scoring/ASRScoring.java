/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.EventAndFileInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import corpus.HTKDictionary;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pronz.dirs.Dirs;
import pronz.speech.Audio;
import pronz.speech.Audio$;
import scala.Tuple2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Does ASR scoring using hydec.  Results in either alignment or decoding, depending on the mode.
 * Decoding is used with autoCRT of audio.
 * <p>
 * Takes the label files and generates transcript images for display in the client.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/10/12
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 * @deprecated don't really use this anymore
 */
public class ASRScoring extends Scoring implements ASR {
  private static final Logger logger = LogManager.getLogger(ASRScoring.class);
  private final Cache<String, Scores> audioToScore;

  private static final boolean DEBUG = false;

  /**
   * @param deployPath
   * @param serverProperties
   * @param langTestDatabase
   * @param project
   * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring
   */
  public ASRScoring(String deployPath, ServerProperties serverProperties, LogAndNotify langTestDatabase,
                    HTKDictionary htkDictionary, Project project) {
    super(deployPath, serverProperties, langTestDatabase, htkDictionary, project);
    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();
  }

  /**
   * @param testAudioDir
   * @param testAudioFileNoSuffix
   * @param sentence              that should be what the test audio contains
   * @param imageOutDir
   * @param useCache
   * @param prefix
   * @param precalcResult
   * @param usePhoneToDisplay
   * @return PretestScore object
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   */
  public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
                                  String sentence,
                                  Collection<String> lmSentences,
                                  String transliteration,

                                  String imageOutDir,
                                  ImageOptions imageOptions,

                                  boolean decode,
                                  boolean useCache, String prefix,
                                  PrecalcScores precalcResult,
                                  boolean usePhoneToDisplay) {
    return scoreRepeatExercise(testAudioDir, testAudioFileNoSuffix,
        sentence, transliteration,
        lmSentences,

        scoringDir,
        imageOutDir,
        imageOptions,
        decode,
        useCache, prefix, precalcResult, usePhoneToDisplay);
  }

  @Override
  public boolean isAvailable() {
    return true;
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
   * @param lmSentences
   * @param scoringDir            where the hydec subset is (models, bin.linux64, etc.)
   * @param imageOutDir           where to write the images (audioImage)
   * @param decode                if true, skips writing image files
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
   * @param prefix                on the names of the image files, if they are written
   * @paramx precalcResult
   * @return score info coming back from alignment/reco
   * @see ASR#scoreRepeat
   */
  private PretestScore scoreRepeatExercise(String testAudioDir,
                                           String testAudioFileNoSuffix,
                                           String sentence,
                                           String transliteration,
                                           Collection<String> lmSentences,
                                           String scoringDir,

                                           String imageOutDir,
                                           ImageOptions imageOptions,

                                           boolean decode,
                                           boolean useCache, String prefix,
                                           PrecalcScores precalcScores,
                                           boolean usePhoneToDisplay) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";

    boolean b = checkLTS(sentence, transliteration).isEmpty();

    if (!b) {
      logger.info("scoreRepeatExercise for " + testAudioFileNoSuffix + " under " + testAudioDir + " '" + sentence + "' is not in lts");
    }
    File wavFile = new File(pathname);
    boolean mustPrepend = false;
    if (!wavFile.exists()) {
      //logger.debug("trying new path for " + pathname + " under " + deployPath);
      wavFile = new File(props.getAudioBaseDir() + File.separator + pathname);
      mustPrepend = true;
    }
    if (!wavFile.exists()) {
      logger.error("scoreRepeatExercise : Can't find audio wav file at : " + wavFile.getAbsolutePath());
      return new PretestScore();
    }
//    logger.info("duration of " + wavFile.getAbsolutePath());// + " is " + duration + " secs or " + duration*1000 + " millis");

    // resample if needed
    try {
      String audioDir = testAudioDir;
      if (mustPrepend) {
        audioDir = props.getAudioBaseDir() + File.separator + audioDir;
        if (!new File(audioDir).exists()) logger.error("Couldn't find " + audioDir);
        else testAudioDir = audioDir;
      }
      testAudioFileNoSuffix = new AudioConversion(props).convertTo16Khz(audioDir, testAudioFileNoSuffix);
    } catch (UnsupportedAudioFileException e) {
      logger.error("Got " + e, e);
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX;
    }

    Scores scores;
    JsonObject jsonObject = null;

//    PrecalcScores precalcScores = new PrecalcScores(props, precalcResult, usePhoneToDisplay);

    if (precalcScores != null && precalcScores.isValid()) {
       logger.info("got valid precalc  " + precalcScores);
      scores = precalcScores.getScores();
      jsonObject = precalcScores.getJsonObject();
    } else {
//      if (precalcScores != null) {
//        logger.debug("unusable precalc result, so recalculating : " + precalcScores);
//      }

        //  logger.debug("recalculating : " + precalcResult);
      scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, transliteration, lmSentences, scoringDir, decode, useCache);
    }
    if (scores == null) {
      logger.error("getScoreForAudio failed to generate scores.");
      return new PretestScore(0.01f);
    }
    PretestScore pretestScore = getPretestScore(imageOutDir,
        imageOptions,
        decode,
        prefix, noSuffix, wavFile,
        scores, jsonObject, usePhoneToDisplay);
//    logger.info("now we have pretest score " +pretestScore + " json " + jsonObject);
    return pretestScore;
  }

  /**
   * Make image files for words, and phones, find out the reco sentence from the events.
   *
   * @param imageOutDir
   * @param decode
   * @param prefix
   * @param noSuffix
   * @param wavFile
   * @param scores
   * @param jsonObject          if not-null, uses it to make the word and phone transcripts instead of .lab files
   * @return
   * @see #scoreRepeatExercise
   */
  private PretestScore getPretestScore(String imageOutDir,
                                       ImageOptions imageOptions,
                                       boolean decode, String prefix, String noSuffix, File wavFile, Scores scores,
                                       JsonObject jsonObject,

                                       boolean usePhoneToDisplay) {
    //  logger.debug("getPretestScore jsonObject " + jsonObject);
//    logger.debug("getPretestScore scores     " + scores);
    int imageWidth = imageOptions.getWidth();
    int imageHeight = imageOptions.getHeight();
    boolean useScoreForBkgColor =imageOptions.isUseScoreToColorBkg();
    boolean reallyUsePhone = usePhoneToDisplay || props.usePhoneToDisplay();

    // we cache the images, so we don't want to return an image for a different option...
    String prefix1 = prefix + (imageOptions.isUseScoreToColorBkg() ? "bkgColorForRef" : "") + (reallyUsePhone ? "_phoneToDisp" : "");

    //logger.debug("getPretestScore prefix " + prefix1);
    if (DEBUG && jsonObject != null) logger.debug("generating images from " + jsonObject);

    if (!scores.isValid()) {
      // skip image generation!
      jsonObject = new JsonObject();
    }

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

    double duration = new AudioCheck(props).getDurationInSeconds(wavFile);

    return new PretestScore(scores.hydraScore,
        getPhoneToScore(scores),
        getWordToScore(scores),
        sTypeToImage, typeToEndTimes, recoSentence, (float) duration, scores.getProcessDur());
  }

  /**
   * @param testAudioDir          audio file directory
   * @param testAudioFileNoSuffix file name without suffix
   * @param sentence              for alignment, the sentence to align, for decoding, the vocab list to use to filter against the dictionary
   * @param lmSentences
   * @param scoringDir            war/scoring path
   * @param decode                true if doing decoding, false for alignment
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score    @return Scores -- hydec score and event (word/phoneme) scores
   * @see #scoreRepeatExercise
   */
  private Scores getScoreForAudio(String testAudioDir,
                                  String testAudioFileNoSuffix,
                                  String sentence,
                                  String transliteration,
                                  Collection<String> lmSentences,
                                  String scoringDir,
                                  boolean decode, boolean useCache) {
    String key = testAudioDir + File.separator + testAudioFileNoSuffix;
    Scores scores = useCache ? audioToScore.getIfPresent(key) : null;

    if (isMandarin) {
      sentence = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") + getSegmented(sentence.trim()); // segmentation method will filter out the UNK model
    }
    if (scores == null) {
      if (DEBUG)
        logger.debug("no cached score for file '" + key + "', so doing " + (decode ? "decoding" : "alignment") + " on " + sentence);
      scores = calcScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, transliteration, lmSentences, scoringDir, decode);
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
   * @param lmSentences
   * @param scoringDir
   * @return Scores which is the overall score and the event scores
   * @see #getScoreForAudio(String, String, String, Collection, String, boolean, boolean)
   */
  private Scores calcScoreForAudio(String testAudioDir,
                                   String testAudioFileNoSuffix,
                                   String sentence,
                                   String transliteration,
                                   Collection<String> lmSentences,
                                   String scoringDir,
                                   boolean decode) {

    try {
      Path tempDir = Files.createTempDirectory("calcScoreForAudio_" + languageProperty);

      Dirs dirs = pronz.dirs.Dirs$.MODULE$.apply(tempDir.toFile().getAbsolutePath(), "", scoringDir, new Log(null, true));
/*    if (false) logger.debug("dirs is " + dirs +
      " audio dir " + testAudioDir + " audio " + testAudioFileNoSuffix + " sentence " + sentence + " decode " + decode + " scoring dir " + scoringDir);
*/

      Audio testAudio = Audio$.MODULE$.apply(
          testAudioDir, testAudioFileNoSuffix,
          false /* notForScoring */, dirs);

      //logger.debug("testAudio is " + testAudio + " dir " + testAudio.dir());
      if (lmSentences != null) {
        new SLFFile().createSimpleSLFFile(lmSentences, tempDir.toFile().getAbsolutePath(), -1.2f);
      }

      Scores scores = computeRepeatExerciseScores(testAudio, sentence, transliteration, tempDir, decode);
      maybeKeepHydecDir(tempDir, scores.hydraScore);

      return scores;
    } catch (IOException e) {
      logger.error("calcScoreForAudio can't create temp dir - ");
      return new Scores();
    }
  }

  /**
   * @param lmSentences
   * @param background
   * @return
   * @see AlignDecode#getASRScoreForAudio
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
    Set<String> backSet = new HashSet<String>(backgroundVocab);
    List<String> mergedVocab = new ArrayList<String>(backgroundVocab);
    for (String foregroundToken : getSmallVocabDecoder().getSimpleVocab(lmSentences, FOREGROUND_VOCAB_LIMIT)) {
      if (!backSet.contains(foregroundToken)) {
        mergedVocab.add(foregroundToken);
      }
    }

    StringBuilder builder = new StringBuilder();
    for (String token : mergedVocab) builder.append(token).append(" ");
    return builder.toString().trim();
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
   * @see #calcScoreForAudio
   */
  private Scores computeRepeatExerciseScores(Audio testAudio, String sentence, String transliteration, Path tmpDir, boolean decode) {
    String modelsDir = configFileCreator.getModelsDir();

    // Make sure that we have an absolute path to the config and dict files.
    // Make sure that we have absolute paths.

    // do template replace on config file
    String configFile = configFileCreator.getHydecConfigFile(tmpDir.toFile().getAbsolutePath(), modelsDir, decode);

    // do some sanity checking
    boolean configExists = new File(configFile).exists();
    if (!configExists) {
      logger.error("computeRepeatExerciseScores : Can't find config file at " + configFile);
      return getEmptyScores();
    }

    Scores scoresFromHydec = getScoresFromHydec(testAudio, sentence, transliteration, configFile);
    return scoresFromHydec;
  }

  private void maybeKeepHydecDir(Path tmpDir, double hydecScore) {
    if (hydecScore > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
        //logger.debug("deleting " + tmpDir + " since score is " +hydecScore);
        FileUtils.deleteDirectory(tmpDir.toFile());
        // tmpDir.toFile().delete();
      } catch (IOException e) {
        logger.error("Deleting dir " + tmpDir + " got " + e, e);
      }
    } else {
      tmpDir.toFile().deleteOnExit();
    }
  }

  /**
   * TODO : for now don't use transliteration call - since we haven't merged.
   *
   * Tries to remove junky characters from the sentence so hydec won't choke on them.
   *
   * @param testAudio
   * @param sentence
   * @param configFile
   * @return
   * @see SmallVocabDecoder
   * @see #computeRepeatExerciseScores
   */
  private Scores getScoresFromHydec(Audio testAudio, String sentence, String transliteration, String configFile) {
    sentence = svd.getTrimmed(sentence);
    long then = System.currentTimeMillis();
    logger.debug("getScoresFromHydec scoring '" + sentence +"' (" +sentence.length()+ " ) with " +
            "LTS " + getLTS() +
        " against " + testAudio + " with " + configFile);

    try {
      Tuple2<Float, Map<String, Map<String, Float>>> jscoreOut =
          testAudio.jscore(sentence,
             // transliteration,
              htkDictionary, getLTS(), configFile);
      float hydec_score = jscoreOut._1;
      long timeToRunHydec = System.currentTimeMillis() - then;

      logger.debug("getScoresFromHydec : '" + languageProperty +
          "' scoring '" + sentence + "' (" + sentence.length() + ") got score " + hydec_score +
          " and took " + timeToRunHydec + " millis");

      Map<String, Map<String, Float>> stringMapMap = jscoreOut._2;
      //logger.debug("hydec output " + stringMapMap);
      return new Scores(hydec_score, stringMapMap, (int) timeToRunHydec);
    } catch (AssertionError e) {
      logger.error("Got assertion error " + e, e);
      return new Scores((int) (System.currentTimeMillis() - then));
    } catch (Exception ee) {
      String msg = "getScoresFromHydec : Running align/decode on " + sentence;
      logger.warn(msg + " Got " + ee.getMessage(),ee);

      if (langTestDatabase != null) {
        langTestDatabase.logAndNotifyServerException(ee, msg);
      }
    }

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.warn("getScoresFromHydec : scoring '" + sentence + "' (" + sentence.length() + " ) : got bad score and took " + timeToRunHydec + " millis");

    Scores scores = new Scores((int) timeToRunHydec);
    scores.hydraScore = -1;
    return scores;
  }

  private Scores getEmptyScores() {
    return new Scores();
  }
}
