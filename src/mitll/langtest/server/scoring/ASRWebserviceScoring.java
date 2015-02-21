package mitll.langtest.server.scoring;

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
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import org.apache.commons.io.FileUtils;
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
	private LTSFactory ltsFactory;

	/**
	 * @param deployPath
	 * @param properties
	 * @param langTestDatabase
	 * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
	 * @see mitll.langtest.server.audio.AudioFileHelper#makeASRScoring()
	 */
	public ASRWebserviceScoring(String deployPath, Map<String, String> properties, LangTestDatabaseImpl langTestDatabase) {
		this(deployPath, properties);
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
	 * @param properties
	 * @paramx dict
	 * @see #ASRScoring(String, java.util.Map, mitll.langtest.server.LangTestDatabaseImpl)
	 */
	private ASRWebserviceScoring(String deployPath, Map<String, String> properties) {
		super(deployPath);
		logger.debug("Creating ASRWebserviceScoring object");
		lowScoreThresholdKeepTempDir = KEEP_THRESHOLD;
		audioToScore = CacheBuilder.newBuilder().maximumSize(1000).build();

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

	int multiple = 0;
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

	/*public static class PhoneInfo {
		private List<String> firstPron;
		private Set<String> phoneSet;

		public PhoneInfo(List<String> firstPron, Set<String> phoneSet) {
			this.firstPron = firstPron;
			this.phoneSet = phoneSet;
		}

		public String toString() {
			return "Phones " + getPhoneSet() + " " + getFirstPron();
		}

		public List<String> getFirstPron() {
			return firstPron;
		}

		public Set<String> getPhoneSet() {
			return phoneSet;
		}
	}*/


	/**
	 * For chinese, maybe later other languages.
	 * @param longPhrase
	 * @return
	 * @seex AutoCRT#getRefs
	 * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getScoreForAudio
	 */
	public static String getSegmented(String longPhrase) {
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
	// JESS alignment and decoding
	private PretestScore scoreRepeatExercise(String testAudioDir,
			String testAudioFileNoSuffix,
			String sentence, // TODO make two params, transcript and lm (null if no slf)
			String scoringDir,

			String imageOutDir,
			int imageWidth, int imageHeight,
			boolean useScoreForBkgColor,
			boolean decode, String tmpDir,
			boolean useCache, String prefix) {
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
		Scores scores = useCache ? audioToScore.getIfPresent(key) : null;
		String phoneLab = "";
		String wordLab = "";
		// actually run the scoring
		String rawAudioPath = testAudioDir + File.separator + testAudioFileNoSuffix + ".raw";
		AudioConversion.wav2raw(testAudioDir + File.separator + testAudioFileNoSuffix + ".wav", rawAudioPath);
		logger.debug("Converting: " + (testAudioDir + File.separator + testAudioFileNoSuffix + ".wav to: " + rawAudioPath));
		// TODO remove the 16k hardcoding?
		double duration = (new AudioCheck()).getDurationInSeconds(wavFile);
		//int end = (int)((duration * 16000.0) / 100.0);
		int end = (int)(duration * 100.0);
		if(scores == null) {                  
			Object[] result = runHydra(rawAudioPath, sentence, tmpDir, decode, end);
			scores = (Scores)result[0];
			wordLab = (String)result[1];
			phoneLab = (String)result[2];
			audioToScore.put(key, scores);
		}
		if (scores == null) {
			logger.error("getScoreForAudio failed to generate scores.");
			return new PretestScore(0.01f);
		}
		return getPretestScore(imageOutDir, imageWidth, imageHeight, useScoreForBkgColor, decode, prefix, noSuffix, wavFile, scores, phoneLab, wordLab, duration);
	}

	private PretestScore getPretestScore(String imageOutDir, int imageWidth, int imageHeight, boolean useScoreForBkgColor,
			boolean decode, String prefix, String noSuffix, File wavFile, Scores scores, String phoneLab, String wordLab, double duration) {
		ImageWriter.EventAndFileInfo eventAndFileInfo = writeTranscripts(imageOutDir, imageWidth, imageHeight, noSuffix,
				useScoreForBkgColor,
				prefix + (useScoreForBkgColor ? "bkgColorForRef" : ""), "", decode, phoneLab, wordLab, true);
		Map<NetPronImageType, String> sTypeToImage = getTypeToRelativeURLMap(eventAndFileInfo.typeToFile);
		Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = getTypeToEndTimes(eventAndFileInfo);
		String recoSentence = getRecoSentence(eventAndFileInfo);

		return new PretestScore(scores.hydraScore, getPhoneToScore(scores), sTypeToImage, typeToEndTimes, recoSentence, (float) duration);
	}

	////////////////////////////////
	////////////////////////////////

	private String createHydraDictWithoutSP(String transcript) {
		String dict = "[";
		transcript = "<s> " + transcript + " </s>";
		int ctr = 0;
		for(String word : transcript.split(" ")) {
			if(!word.equals(" ") && !word.equals("")) {
				if(htkDictionary.contains(word)) {
					scala.collection.immutable.List<String[]> prons = htkDictionary.apply(word);
					for(int i = 0; i < prons.size(); i++) {
						if(ctr != 0) dict += ";";
						ctr += 1;
						dict += word + ",";
						String[] pron = prons.apply(i);
						int ctr2 = 0;
						for(String p : pron) {
							if(ctr2 != 0) dict += " ";
							ctr2 += 1;
							dict += p;
						}
					}
				}
				else {
					for(String[] pron : letterToSoundClass.process(word.toLowerCase())) {
						if(ctr != 0) dict += ";";
						dict += word + ",";
						int ctr2 = 0;
						for(String p : pron) {
							if(ctr2 != 0) dict += " ";
							ctr2 += 1;
							dict += p;
						}
					}
				}
			}
		}
		dict += ";UNKNOWNMODEL,+UNK+]";
		return dict;
	}

	private Object[] runHydra(String audioPath, String transcript, String tmpDir, boolean decode, int end) {
		// reference trans	
		String cleaned = transcript.replaceAll("\\u2022", " ").replaceAll("\\p{Z}+", " ").replaceAll(";", " ").replaceAll("~", " ").replaceAll("\\u2191", " ").replaceAll("\\u2193", " ");
		if (isMandarin) {
			cleaned = (decode ? SLFFile.UNKNOWN_MODEL + " " : "") + getSegmented(transcript.trim()); // segmentation method will filter out the UNK model
		}

		// generate dictionary
		String dictWithoutSP = createHydraDictWithoutSP(cleaned);
		String smallLM = "[]";
		// generate SLF file (if decoding)
		if(decode) {
			ArrayList<String> lmSentences = new ArrayList<String>();
			lmSentences.add(cleaned);
			smallLM = "[" + (new SLFFile()).createSimpleSLFFile(lmSentences) + "]";
		}

		String hydraInput = tmpDir + "/:" + audioPath + ":" + dictWithoutSP + ":" + smallLM + ":xxx,0," + end + ",[]";
		long then = System.currentTimeMillis();
		String ip = langTestDatabase.getWebserviceIP();
		int port = langTestDatabase.getWebservicePort();
		HTTPClient httpClient = new HTTPClient(ip, port);
		String resultsStr = httpClient.sendAndReceive(hydraInput);
		try {
			httpClient.closeConn();
		}
		catch(IOException e) {
			logger.error("Error closing http connection");
		}
		String[] results = resultsStr.split("\n"); // 0th entry-overall score and phone scores, 1st entry-word alignments, 2nd entry-phone alignments
		long timeToRunHydra = System.currentTimeMillis() - then;	
		logger.debug("Took " + timeToRunHydra + " millis to run hydra");
		if(results[0] == "") {
			logger.error("Failure during running of hydra.");
			return null;
		}
		// TODO makes this a tuple3 type 
		String[] split = results[0].split(";");
		Scores scores = new Scores(split); 
		// clean up tmp directory if above score threshold 
		/*logger.debug("overall score: " + split[0]);
		if (Float.parseFloat(split[0]) > lowScoreThresholdKeepTempDir) {   // keep really bad scores for now
			try {
				logger.debug("deleting " + tmpDir + " since score is " + split[0]);
				FileUtils.deleteDirectory(new File(tmpDir));
			} catch (IOException e) {
				logger.error("Deleting dir " + tmpDir + " got " +e,e);
			}
		}*/
		return new Object[]{scores, results[1], results[2]};
	}


	////////////////////////////////
	////////////////////////////////

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
	 * @see #ASRWebserviceScoring(String, java.util.Map, mitll.langtest.server.LangTestDatabaseImpl)
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
					valid = false;
				}
			}
			if (valid) filtered.add(sentence);
			else {
				skipped.add(sentence);
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
