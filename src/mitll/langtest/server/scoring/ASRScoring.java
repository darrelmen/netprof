package mitll.langtest.server.scoring;

import Utils.Log;
import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.EventAndFileInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.CommonExercise;
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
import java.text.Collator;
import java.util.*;

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
public class ASRScoring extends Scoring implements CollationSort, ASR {
    private static final Logger logger = Logger.getLogger(ASRScoring.class);
    private static final boolean DEBUG = false;

	private static final double KEEP_THRESHOLD = 0.3;

	private static final int FOREGROUND_VOCAB_LIMIT = 100;
	private static final int VOCAB_SIZE_LIMIT = 200;

  /**
   * @see SLFFile#createSimpleSLFFile(Collection, String, float)
   */
	public static final String SMALL_LM_SLF = "smallLM.slf";

	private static SmallVocabDecoder svDecoderHelper = null;
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
	private final LTSFactory ltsFactory;

	/**
	 * @param deployPath
	 * @param serverProperties
	 * @param langTestDatabase
	 * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
	 */
	public ASRScoring(String deployPath, ServerProperties serverProperties, LangTestDatabaseImpl langTestDatabase) {
		this(deployPath, serverProperties);
		this.langTestDatabase = langTestDatabase;
		readDictionary();
		makeDecoder();
	}

	public <T extends CommonExercise> void sort(List<T> toSort) {
		ltsFactory.sort(toSort);
	}

	@Override
	public Collator getCollator() {
		return ltsFactory.getCollator();
	}

	private final String languageProperty;

	/**
	 * @param deployPath
	 * @param serverProperties
	 * @paramx dict
	 * @see #ASRScoring(String, java.util.Map, mitll.langtest.server.LangTestDatabaseImpl)
	 */
	private ASRScoring(String deployPath, ServerProperties serverProperties) {
		super(deployPath,serverProperties);

		logger.debug("Creating ASRScoring object");
		lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
		audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

    Map<String, String> properties = serverProperties.getProperties();
		languageProperty = properties.get("language");
		String language = languageProperty != null ? languageProperty : "";

		isMandarin = language.equalsIgnoreCase("mandarin");
		ltsFactory = new LTSFactory(languageProperty);
		this.letterToSoundClass = ltsFactory.getLTSClass(language);
		makeDecoder();
		this.configFileCreator = new ConfigFileCreator(properties, letterToSoundClass, scoringDir);
	}

	private void makeDecoder() {
		if (svDecoderHelper == null && htkDictionary != null) {
			svDecoderHelper = new SmallVocabDecoder(htkDictionary);
		}
	}

	public SmallVocabDecoder getSmallVocabDecoder() {
		return svDecoderHelper;
	}

	/**
	 * @param foreignLanguagePhrase
	 * @return
	 * @see mitll.langtest.server.audio.AudioFileHelper#checkLTS(String)
	 * @see mitll.langtest.server.LangTestDatabaseImpl#isValidForeignPhrase(String)
	 */
	public boolean checkLTS(String foreignLanguagePhrase) { return checkLTS(letterToSoundClass, foreignLanguagePhrase);	}

	/**
	 *
	 * @param foreignLanguagePhrase
	 * @return
	 * @see mitll.langtest.server.audio.AudioFileHelper#checkLTS
	 */
	public PhoneInfo getBagOfPhones(String foreignLanguagePhrase) {
		return checkLTS2(letterToSoundClass, foreignLanguagePhrase);
	}
	/**
	 * So chinese is special -- it doesn't do lts -- it just uses a dictionary
	 *
	 * @param lts
	 * @param foreignLanguagePhrase
	 * @return
	 * @see mitll.langtest.server.LangTestDatabaseImpl#isValidForeignPhrase(String)
	 * @see #checkLTS(String)
	 */
	private boolean checkLTS(LTS lts, String foreignLanguagePhrase) {
		SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
		Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

		String language = isMandarin ? " MANDARIN " : "";
		//logger.debug("checkLTS '" + language + "' tokens : '" +tokens +"'");

		try {
			int i = 0;
			for (String token : tokens) {
				if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL))
					return true;
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
							logger.warn("checkLTS with " + lts + "/" + languageProperty + " token #" + i +
									" : '" + token + "' hash " + token.hashCode() +
									" is invalid in " + foreignLanguagePhrase +
									" and not in dictionary (" + htkDictionary.size() +
									")");
							return false;
						}
					}
				}
				i++;
			}
		} catch (Exception e) {
			logger.error("lts " + language + "/" + lts + " failed on '" + foreignLanguagePhrase + "'", e);
			return false;
		}
		return true;
	}

	private int multiple = 0;
	/**
	 * Might be n1 x n2 x n3 different possible combinations of pronunciations of a phrase
	 * Consider running ASR on all ref audio to get actual phone sequence.
	 *
	 * @param lts
	 * @param foreignLanguagePhrase
	 */
	private PhoneInfo checkLTS2(LTS lts, String foreignLanguagePhrase) {
		SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary);
		Collection<String> tokens = smallVocabDecoder.getTokens(foreignLanguagePhrase);

		List<String> firstPron = new ArrayList<String>();
		Set<String> uphones = new TreeSet<String>();

		if (isMandarin) {
			List<String> token2 = new ArrayList<String>();
			for (String token : tokens) {
				String segmentation = smallVocabDecoder.segmentation(token.trim());
				if (segmentation.isEmpty()) {
					logger.warn("no segmentation for " + foreignLanguagePhrase +  " token " + token);
				}
				else {
					Collections.addAll(token2, segmentation.split(" "));
				}
			}
			//    logger.debug("Tokens were " + tokens + " now " + token2);
			tokens = token2;
		}

		for (String token : tokens) {
			if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL))
				return new PhoneInfo(firstPron,uphones) ;
			// either lts can handle it or the dictionary can...

			boolean htkEntry = htkDictionary.contains(token);
			if (htkEntry) {
				scala.collection.immutable.List<String[]> pronunciationList = htkDictionary.apply(token);
				//   logger.debug("token " + pronunciationList);
				scala.collection.Iterator iter = pronunciationList.iterator();
				boolean first = true;
				while (iter.hasNext()) {
					Object next = iter.next();
					//    logger.debug(next);

					String[] tt = (String[]) next;
					for (String t : tt) {
						//logger.debug(t);
						uphones.add(t);

						if (first) {
							firstPron.add(t);
						}
					}
					if (!first) multiple++;
					first = false;
				}
			} else {
				String[][] process = lts.process(token);
				//logger.debug("token " + token);
				if (process != null) {

					boolean first = true;
					for (String[] onePronunciation : process) {
						// each pronunciation
						//          ArrayList<String> pronunciation = new ArrayList<String>();
						//        pronunciations.add(pronunciation);
						for (String phoneme : onePronunciation) {
							//logger.debug("phoneme " +phoneme);
							//        pronunciation.add(phoneme);
							uphones.add(phoneme);

							if (first) {
								firstPron.add(phoneme);
							}
						}
						if (!first) multiple++;
						first = false;
					}
				}
			}
		}
		//if (multiple % 1000 == 0) logger.debug("mult " + multiple);
		return new PhoneInfo(firstPron,uphones);
	}

  /**
   * For chinese, maybe later other languages.
   * @param longPhrase
   * @return
   * @seex AutoCRT#getRefs
   * @see mitll.langtest.server.scoring.ASRScoring#getScoreForAudio
   */
  public static String getSegmented(String longPhrase) {
    Collection<String> tokens = svDecoderHelper.getTokens(longPhrase);
    StringBuilder builder = new StringBuilder();
    for (String token : tokens) {
      builder.append(svDecoderHelper.segmentation(token.trim()));
      builder.append(" ");
    }
    return builder.toString();
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
   * @param useCache
   * @param prefix
   * @param precalcResult
	 * @param usePhoneToDisplay
   * @return PretestScore object
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
        useCache, prefix, precalcResult,usePhoneToDisplay);
  }

  /**
   * Use hydec to do scoring<br></br>
   * <p/>
   * Some magic happens in {@link Scoring#writeTranscripts} where .lab files are
   * parsed to determine the start and end times for each event, which lets us both create images that
   * show the location of the words and phonemes, and for decoding, the actual reco sentence returned. <br></br>
   * <p/>
   * For alignment, the reco sentence is just the given sentence echoed back (unless alignment fails to
   * generate any alignments (e.g. for audio that's complete silence or when the
   * spoken sentence is unrelated to the expected.)).
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

    boolean b = checkLTS(sentence);

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
    if (precalcResult == null ||
        (precalcResult.isValid() &&
            (precalcResult.getPronScore() < 0 || precalcResult.getJsonScore() == null || precalcResult.getJsonScore().isEmpty()))) {
      if (precalcResult != null) {
        logger.debug("unusable precalc result, so recalculating : " + precalcResult);
      }
      scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir, useCache);
    } else {
      logger.debug("for cached result " + precalcResult);// + "\n\tgot json : " + precalcResult.getJsonScore());
      jsonObject = JSONObject.fromObject(precalcResult.getJsonScore());
      scores = getCachedScores(precalcResult, jsonObject, usePhoneToDisplay);

      boolean isDecode = precalcResult.getAudioType().equals("avp");
      if (precalcResult.isValid() &&
          (scores.eventScores.isEmpty() || (scores.eventScores.get(Scores.WORDS).isEmpty() && (!isDecode || precalcResult.isCorrect())))) {
        logger.debug("no valid precalc result, so recalculating : " + precalcResult);
        jsonObject = null;
        scores = getScoreForAudio(testAudioDir, testAudioFileNoSuffix, sentence, scoringDir, decode, tmpDir, useCache);
      } else {
        //logger.debug("precalc : events " + scores.eventScores);
      }
    }
    if (scores == null) {
      logger.error("getScoreForAudio failed to generate scores.");
      return new PretestScore(0.01f);
    }
    return getPretestScore(imageOutDir, imageWidth, imageHeight, useScoreForBkgColor, decode, prefix, noSuffix, wavFile,
        scores, jsonObject,usePhoneToDisplay);
  }

    /**
     * @see #scoreRepeatExercise
     * @param precalcResult
     * @param jsonObject
     * @return
     */
    private Scores getCachedScores(Result precalcResult, JSONObject jsonObject, boolean usePhones) {
        Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseJson(jsonObject, "words", "w", usePhones);
        Map<String, Map<String, Float>> eventScores = getEventAverages(imageTypeMapMap);

        Scores scores = new Scores(precalcResult.getPronScore(), eventScores, 0);
        //logger.debug("got cached scores " + scores + " json " + jsonObject);
        return scores;
    }

	/**
	 * @see #getCachedScores
	 * @param imageTypeMapMap
	 * @return
	 */
    private Map<String, Map<String, Float>> getEventAverages(Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap) {
        Map<String, Map<String, Float>> eventScores = new HashMap<String, Map<String, Float>>();
        // phones
        Map<Float, TranscriptEvent> floatTranscriptEventMap = imageTypeMapMap.get(ImageType.PHONE_TRANSCRIPT);
        Map<String, Float> value2 = new HashMap<>();
        eventScores.put(Scores.PHONES, value2);
        if (floatTranscriptEventMap != null) {
            getEventAverages(floatTranscriptEventMap, value2);
        }
        // words
        floatTranscriptEventMap = imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT);
        value2 = new HashMap<>();
        eventScores.put(Scores.WORDS, value2);
        if (floatTranscriptEventMap != null) {
            getEventAverages(floatTranscriptEventMap, value2);
        }
        return eventScores;
    }

  /**
   * @see #getEventAverages(Map, Map)
   * @param floatTranscriptEventMap
   * @param value2
   */
    private void getEventAverages(Map<Float, TranscriptEvent> floatTranscriptEventMap, Map<String, Float> value2) {
        Map<String, Float> value = new HashMap<>();
        Map<String, Float> cvalue = new HashMap<>();

        for (TranscriptEvent ev : floatTranscriptEventMap.values()) {
            String event = ev.event;
            if (event.equals("sil") || event.equals("<s>") || event.equals("</s>")) {
            }
            else {
                Float orDefault = cvalue.getOrDefault(event, 0.0f);
                orDefault += 1.0f;
                cvalue.put(event, orDefault);

                Float orDefault1 = value.getOrDefault(event, 0.0f);
                value.put(event, orDefault1+ev.score);
            }
        }

        for (Map.Entry<String, Float> pair : value.entrySet()) {
            String key = pair.getKey();
            value2.put(key,pair.getValue()/cvalue.get(key));
        }
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
   * @param jsonObject if not-null, uses it to make the word and phone transcripts instead of .lab files
   * @return
   * @see #scoreRepeatExercise
   */
  private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
                                       boolean decode, String prefix, String noSuffix, File wavFile, Scores scores, JSONObject jsonObject,
                                       boolean usePhoneToDisplay) {
    //  logger.debug("getPretestScore jsonObject " + jsonObject);
    //  logger.debug("getPretestScore scores     " + scores);

    boolean reallyUsePhone = usePhoneToDisplay || props.usePhoneToDisplay();

    // we cache the images, so we don't want to return an image for a different option...
    String prefix1 = prefix + (useScoreForBkgColor ? "bkgColorForRef" : "") + (reallyUsePhone ? "_phoneToDisp" : "");

    logger.debug("getPretestScore prefix " + prefix1);

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
      sentence = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") +getSegmented(sentence.trim()); // segmentation method will filter out the UNK model
    }
    if (scores == null) {
      if (DEBUG) logger.debug("no cached score for file '" + key + "', so doing " + (decode ? "decoding" : "alignment") + " on " + sentence);
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
   * @see #scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String, Result)
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
   * @see AutoCRTScoring#getASRScoreForAudio(File, Collection, boolean, boolean)
   * @return
   */
  public String getUsedTokens(Collection<String> lmSentences, List<String> background) {
    return getUniqueTokensInLM(lmSentences, svDecoderHelper.getVocab(background, VOCAB_SIZE_LIMIT));
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
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToEndTimes(EventAndFileInfo eventAndFileInfo) {
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
    double hydecScore = scoresFromHydec.hydraScore;
    if (hydecScore > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
      try {
        //logger.debug("deleting " + tmpDir + " since score is " +hydecScore);
        FileUtils.deleteDirectory(new File(tmpDir));
      } catch (IOException e) {
        logger.error("Deleting dir " + tmpDir + " got " +e,e);
      }
    }
    return scoresFromHydec;
  }

  /**
   * @see #ASRScoring(String, java.util.Map, mitll.langtest.server.LangTestDatabaseImpl)
   */
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
      //if (now - then > 300) {
        logger.info("for " + languageProperty +
            " read dict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
      //}
      return htkDictionary;
    }
    else {
      logger.warn("makeDict : Can't find dict file at " + dictFile);
      return new HTKDictionary();
    }
  }

	private SmallVocabDecoder svd = new SmallVocabDecoder();

  /**
	 * Tries to remove junky characters from the sentence so hydec won't choke on them.
	 * @see SmallVocabDecoder
   * @see #computeRepeatExerciseScores(pronz.speech.Audio, String, String, boolean)
   * @param testAudio
   * @param sentence
   * @param configFile
   * @return
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

      logger.debug("getScoresFromHydec  : scoring '" + sentence +"' (" +sentence.length()+ ") got score " + hydec_score +
        " and took " + timeToRunHydec + " millis");

        Map<String, Map<String, Float>> stringMapMap = jscoreOut._2;
        //logger.debug("hydec output " + stringMapMap);

        return new Scores(hydec_score, stringMapMap, (int)timeToRunHydec);
    } catch (AssertionError e) {
      logger.error("Got assertion error " + e,e);
      return new Scores((int)(System.currentTimeMillis() - then));
    } catch (Exception ee) {
			String msg = "Running align/decode on " + sentence;
			logger.warn(msg + " Got " + ee.getMessage());

      if (langTestDatabase != null) langTestDatabase.logAndNotifyServerException(ee, msg);
    }

    long timeToRunHydec = System.currentTimeMillis() - then;

	logger.warn("getScoresFromHydec : scoring '" + sentence + "' (" + sentence.length() + " ) : got bad score and took " + timeToRunHydec + " millis");

    Scores scores = new Scores((int)timeToRunHydec);
    scores.hydraScore = -1;
    return scores;
  }

  private Scores getEmptyScores() {
    Map<String, Map<String, Float>> eventScores = Collections.emptyMap();
    return new Scores(0f, eventScores, 0);
  }
}
