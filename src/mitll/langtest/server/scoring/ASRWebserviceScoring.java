package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import audio.imagewriter.EventAndFileInfo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import corpus.HTKDictionary;
import corpus.LTS;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import org.apache.log4j.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
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
public class ASRWebserviceScoring extends Scoring implements CollationSort, ASR {
	private static final double KEEP_THRESHOLD = 0.3;
	private static final Logger logger = Logger.getLogger(ASRWebserviceScoring.class);
	private static final boolean DEBUG = false;

	private static final int FOREGROUND_VOCAB_LIMIT = 100;
	private static final int VOCAB_SIZE_LIMIT = 200;

	//	public static final String SMALL_LM_SLF = "smallLM.slf";

	private static SmallVocabDecoder svDecoderHelper = null;
	//	private LangTestDatabaseImpl langTestDatabase;

	/**
	 * By keeping these here, we ensure that we only ever read the dictionary once
	 */
	private HTKDictionary htkDictionary;
	private final LTS letterToSoundClass;
	//private final Cache<String, Scores> audioToScore;
	// TODO make Scores + phoneLab + wordLab an object so have something more descriptive than Object[]
	private final Cache<String, Object[]> audioToScore; // key => (Scores, wordLab, phoneLab)
	private final ConfigFileCreator configFileCreator;
	private final boolean isMandarin;

	/**
	 * Normally we delete the tmp dir created by hydec, but if something went wrong, we want to keep it around.
	 * If the score was below a threshold, or the magic -1, we keep it around for future study.
	 */
	private double lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
	private final LTSFactory ltsFactory;
	private final String ip;
	private final int port;
	private final String languageProperty;

	/**
	 * @param deployPath
	 * @param properties
	 * @paramx langTestDatabase
	 * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
	 * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring()
	 */
	public ASRWebserviceScoring(String deployPath, ServerProperties properties){
    super(deployPath, properties);

		logger.debug("Creating ASRWebserviceScoring object");
		//lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
		audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

		languageProperty = properties.getLanguage();
		String language = languageProperty != null ? languageProperty : "";
		ip = properties.getWebserviceIP();
		port = properties.getWebservicePort();

		isMandarin = language.equalsIgnoreCase("mandarin");

		ltsFactory = new LTSFactory(languageProperty);
		this.letterToSoundClass = ltsFactory.getLTSClass(language);
//		logger.debug(this + " LTS is " + letterToSoundClass);
		makeDecoder();
		this.configFileCreator = new ConfigFileCreator(properties.getProperties(), letterToSoundClass, scoringDir);
		readDictionary();
		makeDecoder();
	}

	/**
	 * @see AudioFileHelper#sort
	 * @param toSort
	 * @param <T>
	 */
	public <T extends CommonExercise> void sort(List<T> toSort) {		ltsFactory.sort(toSort);	}

	@Override
	public Collator getCollator() { return ltsFactory.getCollator(); 	}

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
	public boolean checkLTS(String foreignLanguagePhrase) {
		return checkLTS(letterToSoundClass, foreignLanguagePhrase);
	}

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
			tokens = token2;
		}

		for (String token : tokens) {
			if (token.equalsIgnoreCase(SLFFile.UNKNOWN_MODEL))
				return new PhoneInfo(firstPron,uphones) ;

			boolean htkEntry = htkDictionary.contains(token);
			if (htkEntry) {
				scala.collection.immutable.List<String[]> pronunciationList = htkDictionary.apply(token);
				scala.collection.Iterator iter = pronunciationList.iterator();
				boolean first = true;
				while (iter.hasNext()) {
					Object next = iter.next();

					String[] tt = (String[]) next;
					for (String t : tt) {
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
				if (process != null) {

					boolean first = true;
					for (String[] onePronunciation : process) {
						// each pronunciation
						for (String phoneme : onePronunciation) {
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
		return new PhoneInfo(firstPron,uphones);
	}

	/**
	 * For chinese, maybe later other languages.
	 * @param longPhrase
	 * @return
	 * @seex AutoCRT#getRefs
	 * @see mitll.langtest.server.scoring.ASRWebserviceScoring#runHydra
	 */
	private static String getSegmented(String longPhrase) {
		Collection<String> tokens = svDecoderHelper.getTokens(longPhrase);
		StringBuilder builder = new StringBuilder();
		for (String token : tokens) {
			builder.append(svDecoderHelper.segmentation(token.trim()));
			builder.append(" ");
		}
		String s = builder.toString();

		return s;
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
				sentence, lmSentences, 
				imageOutDir, imageWidth, imageHeight, useScoreForBkgColor,
				decode, tmpDir,
				useCache, prefix, usePhoneToDisplay);
	}

	/**
	 * Use hydec to do scoring<br></br>
	 * <p/>
	 * Some magic happens in {@link Scoring#writeTranscripts } where .lab files are
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
	 * @param imageOutDir           where to write the images (audioImage)
	 * @param imageWidth            image width
	 * @param imageHeight           image height
	 * @param useScoreForBkgColor   true if we want to color the segments by score else all are gray
	 * @param decode                if true, skips writing image files
	 * @param tmpDir                where to run hydec
	 * @param useCache              cache scores so subsequent requests for the same audio file will get the cached score
	 * @param prefix                on the names of the image files, if they are written
	 * @param usePhoneToDisplay
   * @paramx scoringDir            where the hydec subset is (models, bin.linux64, etc.)
	 * @return score info coming back from alignment/reco
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
                                           boolean useCache, String prefix, boolean usePhoneToDisplay) {
		String noSuffix = testAudioDir + File.separator + testAudioFileNoSuffix;
		String pathname = noSuffix + ".wav";

		boolean b = checkLTS(sentence);
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
			logger.error("Got " +e,e);
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
		if(cached != null) {
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
		int end = (int)(duration * 100.0);
		int processDur = 0;
		if(scores == null) {
			long then = System.currentTimeMillis();
			Object[] result = runHydra(rawAudioPath, sentence, lmSentences, tmpDir, decode, end);
			if (result == null) {
				return new PretestScore(0);
			}
			else {
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

	private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
																			 boolean decode, String prefix, String noSuffix, Scores scores, String phoneLab,
																			 String wordLab, double duration, int processDur, boolean usePhoneToDisplay) {
    String prefix1 = prefix + (useScoreForBkgColor ? "bkgColorForRef" : "") +(usePhoneToDisplay ? "_phoneToDisplay" : "");


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
	 * @see #runHydra(String, String, Collection, String, boolean, int)
	 * @param transcript
	 * @return
	 */
	private String createHydraDict(String transcript) {
		if (letterToSoundClass == null) {
			logger.warn(this  + " :  LTS is null???");
		}

		String dict = "[";
		//transcript = "<s> " + transcript + " </s>";
		int ctr = 0;
		for(String word : transcript.split(" ")) {
			if(!word.equals(" ") && !word.equals("")) {
				if(htkDictionary.contains(word)) {
					scala.collection.immutable.List<String[]> prons = htkDictionary.apply(word);
					for(int i = 0; i < prons.size(); i++) {
						if(ctr != 0) dict += ";";
						ctr++;
						dict += word + ",";
						String[] pron = prons.apply(i);
						int ctr2 = 0;
						for(String p : pron) {
							if(ctr2 != 0) dict += " ";
							ctr2 += 1;
							dict += p;
						}
						dict += " sp";
					}
				}
				else {
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

	private SLFFile slfFile = new SLFFile();

	/**
	 *
	 * @param audioPath
	 * @param transcript
	 * @param lmSentences
	 * @param tmpDir
	 * @param decode
	 * @param end
	 * @return
	 * @see #scoreRepeatExercise(String, String, String, Collection, String, int, int, boolean, boolean, String, boolean, String, boolean)
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
		if(decode) {
			String[] slfOut = slfFile.createSimpleSLFFile(lmSentences);
			smallLM = "[" + slfOut[0] + "]";
			cleaned = slfFile.cleanToken(slfOut[1]);
		}

		String hydraInput = tmpDir + "/:" + audioPath + ":" + hydraDict + ":" + smallLM + ":xxx,0," + end + ",[<s>;" + cleaned.replaceAll("\\p{Z}",";") + ";</s>]";
		long then = System.currentTimeMillis();
		HTTPClient httpClient = new HTTPClient(ip, port);

		String resultsStr = runHydra(hydraInput, httpClient);
		String[] results = resultsStr.split("\n"); // 0th entry-overall score and phone scores, 1st entry-word alignments, 2nd entry-phone alignments
		long timeToRunHydra = System.currentTimeMillis() - then;	

		if (results[0].isEmpty()) {
			logger.error("Failure during running of hydra on " + audioPath + (decode ? " DECODING " : " ALIGNMENT ") + " with " + (decode ? transcript : lmSentences));
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
		return new Object[]{scores, results[1].replaceAll("#",""), results[2].replaceAll("#","")}; // where are the # coming from?
	}

	private String runHydra(String hydraInput, HTTPClient httpClient) {
		try {
			String resultsStr = httpClient.sendAndReceive(hydraInput);
			try {
        httpClient.closeConn();
      }
      catch(IOException e) {
        logger.error("Error closing http connection");
				resultsStr = "";
      }
			return resultsStr;
		} catch (Exception e) {
			logger.error("running on " + port + " with " + hydraInput + " got "+e,e);
			return "";
		}
	}


	////////////////////////////////
	////////////////////////////////

	/**
	 * @param lmSentences
	 * @param background
	 * @see AutoCRTScoring#getASRScoreForAudio(File, Collection, boolean)
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
	 * We might consider defensively sorting the events by time.
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

	/**
	 * Make sure that when we scale the phone scores by {@link #SCORE_SCALAR} we do it for both the scores and the image.
	 * <br></br>
	 * get the phones for display in the phone accuracy pane
	 * @param scores from hydec
	 * @return map of phone name to score
	 */
/*	private Map<String, Float> getPhoneToScore(Scores scores) {
		Map<String, Float> phones = scores.eventScores.get("phones");
		if (phones == null) {
			return Collections.emptyMap();
		}
		else {
			Map<String, Float> phoneToScore = new HashMap<String, Float>();
			for (Map.Entry<String, Float> phoneScorePair : phones.entrySet()) {
				String key = phoneScorePair.getKey();
				if (!key.equals("sil")) {
					phoneToScore.put(key, Math.min(1.0f, phoneScorePair.getValue()));
				}
			}
			return phoneToScore;
		}
	}*/
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

	/**
	 * @see #ASRWebserviceScoring
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
			logger.info("for " + languageProperty +
					" read dict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
			return htkDictionary;
		}
		else {
			logger.warn("makeDict : Can't find dict file at " + dictFile);
			return new HTKDictionary();
		}
	}
}
