package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.EventAndFileInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

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
public class ASRWebserviceScoring extends Scoring implements CollationSort, ASR {
  private static final Logger logger = Logger.getLogger(ASRWebserviceScoring.class);
  private static final int FOREGROUND_VOCAB_LIMIT = 100;
  private static final int VOCAB_SIZE_LIMIT = 200;

  private static final SmallVocabDecoder svDecoderHelper = null;
  private final SLFFile slfFile = new SLFFile();

  // TODO make Scores + phoneLab + wordLab an object so have something more descriptive than Object[]
  private final Cache<String, Object[]> audioToScore; // key => (Scores, wordLab, phoneLab)

  /**
   * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
   * If the score was below a threshold, or the magic -1, we keep it around for future study.
   */
//	private double lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
  private final String ip;
  private final int port;

  /**
   * @param deployPath
   * @param properties
   * @paramx langTestDatabase
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring()
   */
  public ASRWebserviceScoring(String deployPath, ServerProperties properties, LogAndNotify langTestDatabase) {
    super(deployPath, properties, langTestDatabase);
//    logger.debug("Creating ASRWebserviceScoring object");
    audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();
    ip = properties.getWebserviceIP();
    port = properties.getWebservicePort();
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
        sentence, lmSentences,
        imageOutDir, imageWidth, imageHeight, useScoreForBkgColor,
        decode, tmpDir,
        useCache, prefix, precalcResult, usePhoneToDisplay);
  }

  /**
   * Use hydec to do scoring<br></br>
   * <p>
   * Some magic happens in {@link Scoring#writeTranscripts } where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   * <p>
   * For alignment, of course, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is utterly unrelated to the reference.)).
   * <p>
   * Audio file must be a wav file, but can be any sample rate - if not 16K will be sampled down to 16K.
   *
   * @param testAudioDir          where the audio is
   * @param testAudioFileNoSuffix file name without a suffix - wav file, any sample rate
   * @param sentence              to align
   * @param imageOutDir           where to write the images (audioImage)
   * @param imageWidth            image width
   * @param imageHeight           image height
   * @param useScoreForBkgColor   true if we want to color the segments by score else all are gray
   * @param decode                if true, skips writing image files
   * @param tmpDir                where to run hydec
   * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
   * @param prefix                on the names of the image files, if they are written
   * @param precalcResult
   * @param usePhoneToDisplay     @return score info coming back from alignment/reco
   * @paramx scoringDir            where the hydec subset is (models, bin.linux64, etc.)
   * @see ASR#scoreRepeat
   */
  // JESS alignment and decoding
  private PretestScore scoreRepeatExercise(String testAudioDir,
                                           String testAudioFileNoSuffix,
                                           String sentence, Collection<String> lmSentences, // TODO make two params, transcript and lm (null if no slf)

                                           String imageOutDir,
                                           int imageWidth, int imageHeight,
                                           boolean useScoreForBkgColor,
                                           boolean decode, String tmpDir,
                                           boolean useCache, String prefix,
                                           Result precalcResult,
                                           boolean usePhoneToDisplay) {
    String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
    String pathname = noSuffix + ".wav";

    boolean b = validLTS(sentence);
    // audio conversion stuff
    File wavFile = new File(pathname);
    boolean mustPrepend = false;
    if (!wavFile.exists() && deployPath != null) {
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
      logger.error("Got " + e, e);
    }

    if (testAudioFileNoSuffix.contains(AudioConversion.SIXTEEN_K_SUFFIX)) {
      noSuffix += AudioConversion.SIXTEEN_K_SUFFIX;
    }

    String key = testAudioDir + File.separator + testAudioFileNoSuffix;
//		Scores scores = useCache ? audioToScore.getIfPresent(key) : null;
    Object[] cached = useCache ? audioToScore.getIfPresent(key) : null;
    Scores scores = null;
    String phoneLab = "";
    String wordLab = "";
    if (cached != null) {
      scores = (Scores) cached[0];
      wordLab = (String) cached[1];
      phoneLab = (String) cached[2];
    }

    // actually run the scoring
    String rawAudioPath = testAudioDir + File.separator + testAudioFileNoSuffix + ".raw";
    AudioConversion.wav2raw(testAudioDir + File.separator + testAudioFileNoSuffix + ".wav", rawAudioPath);
    //	logger.debug("Converting: " + (testAudioDir + File.separator + testAudioFileNoSuffix + ".wav to: " + rawAudioPath));
    // TODO remove the 16k hardcoding?
    double duration = (new AudioCheck()).getDurationInSeconds(wavFile);
    //int end = (int)((duration * 16000.0) / 100.0);
    int end = (int) (duration * 100.0);
    int processDur = 0;
    if (scores == null) {
      long then = System.currentTimeMillis();
      Object[] result = runHydra(rawAudioPath, sentence, lmSentences, tmpDir, decode, end);
      if (result == null) {
        return new PretestScore(0);
      } else {
        processDur = (int) (System.currentTimeMillis() - then);
        scores = (Scores) result[0];
        wordLab = (String) result[1];
        phoneLab = (String) result[2];
        audioToScore.put(key, new Object[]{scores, wordLab, phoneLab});
      }
    }
    if (scores == null) {
      logger.error("getScoreForAudio failed to generate scores.");
      return new PretestScore(0.01f);
    }
    return getPretestScore(imageOutDir, imageWidth, imageHeight, useScoreForBkgColor, decode, prefix, noSuffix,
        scores, phoneLab, wordLab, duration, processDur, usePhoneToDisplay);
  }

  /**
   * TODO : don't copy this method in both ASRScoring and ASRWebserviceScoring
   *
   * @param imageOutDir
   * @param imageWidth
   * @param imageHeight
   * @param useScoreForBkgColor
   * @param decode
   * @param prefix
   * @param noSuffix
   * @param scores
   * @param phoneLab
   * @param wordLab
   * @param duration
   * @param processDur
   * @param usePhoneToDisplay
   * @return
   */
  private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                       boolean decode, String prefix, String noSuffix, Scores scores, String phoneLab,
                                       String wordLab, double duration, int processDur, boolean usePhoneToDisplay) {
    String prefix1 = prefix + (useScoreForBkgColor ? "bkgColorForRef" : "") + (usePhoneToDisplay ? "_phoneToDisplay" : "");


    EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
        useScoreForBkgColor,
        prefix1, "", decode, phoneLab, wordLab, true, usePhoneToDisplay);
    Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo);
    String recoSentence = getRecoSentence(eventAndFileInfo);

    return new PretestScore(scores.hydraScore, getPhoneToScore(scores), getWordToScore(scores),
        sTypeToImage, typeToEndTimes, recoSentence, (float) duration, processDur);
  }

  ////////////////////////////////
  ////////////////////////////////

  /**
   * @param transcript
   * @return
   * @see #runHydra(String, String, Collection, String, boolean, int)
   */
  private String createHydraDict(String transcript) {
    if (letterToSoundClass == null) {
      logger.warn(this + " :  LTS is null???");
    }

    String dict = "[";
    //transcript = "<s> " + transcript + " </s>";
    int ctr = 0;
    for (String word : transcript.split(" ")) {
      if (!word.equals(" ") && !word.equals("")) {
        if (htkDictionary.contains(word)) {
          scala.collection.immutable.List<String[]> prons = htkDictionary.apply(word);
          for (int i = 0; i < prons.size(); i++) {
            if (ctr != 0) dict += ";";
            ctr++;
            dict += word + ",";
            String[] pron = prons.apply(i);
            int ctr2 = 0;
            for (String p : pron) {
              if (ctr2 != 0) dict += " ";
              ctr2 += 1;
              dict += p;
            }
            dict += " sp";
          }
        } else {
          if (letterToSoundClass == null) {
            logger.warn(this + " " + languageProperty + " : LTS is null???");
          } else {
            for (String[] pron : letterToSoundClass.process(word.toLowerCase())) {
              if (ctr != 0) dict += ";";
              ctr++;
              dict += word + ",";
              int ctr2 = 0;
              for (String p : pron) {
                if (ctr2 != 0) dict += " ";
                ctr2 += 1;
                dict += p;
              }
              dict += " sp";
            }
          }
        }
      }
    }
    dict += ";UNKNOWNMODEL,+UNK+;<s>,sil;</s>,sil]";
    return dict;
  }

  /**
   * @param audioPath
   * @param transcript
   * @param lmSentences
   * @param tmpDir
   * @param decode
   * @param end         frame number of end of file (I think)
   * @return
   * @see #scoreRepeatExercise(String, String, String, Collection, String, int, int, boolean, boolean, String, boolean, String, Result, boolean)
   */
  private Object[] runHydra(String audioPath, String transcript, Collection<String> lmSentences, String tmpDir, boolean decode, int end) {
    // reference trans
    String cleaned = slfFile.cleanToken(transcript);
    if (isMandarin)
      cleaned = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") + getSegmented(transcript.trim()); // segmentation method will filter out the UNK model

    // generate dictionary
    String hydraDict = createHydraDict(cleaned);
    String smallLM = "[]";

    // generate SLF file (if decoding)
    if (decode) {
      String[] slfOut = slfFile.createSimpleSLFFile(lmSentences);
      smallLM = "[" + slfOut[0] + "]";
      cleaned = slfFile.cleanToken(slfOut[1]);
    }

    String hydraInput = tmpDir + "/:" + audioPath + ":" + hydraDict + ":" + smallLM + ":xxx,0," + end + ",[<s>;" + cleaned.replaceAll("\\p{Z}", ";") + ";</s>]";
    long then = System.currentTimeMillis();
    HTTPClient httpClient = new HTTPClient(ip, port, "dcodr");

    String resultsStr = runHydra(hydraInput, httpClient);
    String[] results = resultsStr.split("\n"); // 0th entry-overall score and phone scores, 1st entry-word alignments, 2nd entry-phone alignments
    long timeToRunHydra = System.currentTimeMillis() - then;

    if (results[0].isEmpty()) {
      String input = decode ? lmSentences == null ? "huh? no sentences to decode???" : lmSentences.toString() : transcript;
      String which = decode ? " DECODING " : " ALIGNMENT ";
      String message = "Failure during running of hydra on " + audioPath + which + " with " + input;
      logger.error(message);
      langTestDatabase.logAndNotifyServerException(null, message);
      return null;
    }
    // TODO makes this a tuple3 type
    String[] split = results[0].split(";");
    Scores scores = new Scores(split);
    // clean up tmp directory if above score threshold
    logger.debug("Took " + timeToRunHydra + " millis to run hydra - overall score: " + split[0]);
    /*if (Float.parseFloat(split[0]) > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
				logger.debug("deleting " + tmpDir + " since score is " + split[0]);
				FileUtils.deleteDirectory(new File(tmpDir));
			} catch (IOException e) {
				logger.error("Deleting dir " + tmpDir + " got " +e,e);
			}
		}*/
    return new Object[]{scores, results[1].replaceAll("#", ""), results[2].replaceAll("#", "")}; // where are the # coming from?
  }

  /**
   * @param hydraInput
   * @param httpClient
   * @return
   * @see #runHydra(String, String, Collection, String, boolean, int)
   */
  private String runHydra(String hydraInput, HTTPClient httpClient) {
    try {
      String resultsStr;
      try {
        resultsStr = httpClient.sendAndReceiveAndClose(hydraInput);
      } catch (IOException e) {
        logger.error("Error closing http connection " + e, e);
        langTestDatabase.logAndNotifyServerException(e, "running hydra with " + hydraInput);
        resultsStr = "";
      }
      return resultsStr;
    } catch (Exception e) {
      logger.error("running on " + port + " with " + hydraInput + " got " + e, e);
      return "";
    }
  }

  ////////////////////////////////
  ////////////////////////////////

  /**
   * @param lmSentences
   * @param background
   * @return
   * @see AutoCRTScoring#getASRScoreForAudio(File, Collection, boolean, boolean)
   */
  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    List<String> backgroundVocab = svDecoderHelper.getVocab(background, VOCAB_SIZE_LIMIT);
    return getUniqueTokensInLM(lmSentences, backgroundVocab);
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
   * @param eventAndFileInfo
   * @return
   * @see #scoreRepeatExercise
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToEndTimes(EventAndFileInfo eventAndFileInfo) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : eventAndFileInfo.typeToEvent.entrySet()) {
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
  }

  /**
   * Take the events (originally from a .lab file generated in pronz) for WORDS and string them together into a
   * sentence.
   * We might consider defensively sorting the events by time.
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
        //        List<TranscriptEvent> sorted = new ArrayList<TranscriptEvent>(timeToEvent.values());
        //        Collections.sort(sorted);
        //        for (TranscriptEvent transcriptEvent : sorted) {
        //					String event = transcriptEvent.event;
        for (Float timeStamp : timeToEvent.keySet()) {
          String event = timeToEvent.get(timeStamp).event;
          if (!event.equals("<s>") && !event.equals("</s>") && !event.equals("sil")) {
            String trim = event.trim();
            if (trim.length() > 0) {
              b.append(trim);
              b.append(" ");
            }
            //   else {
            //logger.warn("huh? event " + transcriptEvent + " had an event word that was zero length?");
            //     }
          }
        }
      }
    }

    return b.toString().trim();
  }

  private Map<String, Float> getPhoneToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get("phones");
    return getTokenToScore(scores, phones, true);
  }

  private Map<String, Float> getWordToScore(Scores scores) {
    Map<String, Float> phones = scores.eventScores.get(Scores.WORDS);
    return getTokenToScore(scores, phones, false);
  }

  private Map<String, Float> getTokenToScore(Scores scores, Map<String, Float> phones, boolean expecting) {
    if (phones == null) {
      if (expecting) {
        logger.warn("no phone scores in " + scores.eventScores);
      }
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
}