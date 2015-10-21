package mitll.langtest.server.scoring;

import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.PretestScore;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Set;

// TODO make this actually have method signatures...
public interface ASR {
	<T extends CommonExercise> void sort(List<T> toSort);
	Collator getCollator();
	boolean validLTS(String foreignLanguagePhrase);
	PhoneInfo getBagOfPhones(String foreignLanguagePhrase);
	SmallVocabDecoder getSmallVocabDecoder();
	String getUsedTokens(Collection<String> lmSentences, List<String> background);

	/**
	 * @see mitll.langtest.server.audio.AudioFileHelper#getASRScoreForAudio
	 * @param testAudioDir
	 * @param testAudioFileNoSuffix
	 * @param sentence
	 * @param lmSentences
	 * @param imageOutDir
	 * @param imageWidth
	 * @param imageHeight
	 * @param useScoreForBkgColor
	 * @param decode
	 * @param tmpDir
	 * @param useCache
	 * @param prefix
	 * @param precalcResult
	 * @param usePhoneToDisplay
	 * @return
	 */
	PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
													 String sentence, Collection<String> lmSentences, String imageOutDir,
													 int imageWidth, int imageHeight, boolean useScoreForBkgColor,
													 boolean decode, String tmpDir,
													 boolean useCache, String prefix, Result precalcResult, boolean usePhoneToDisplay);
	
	class PhoneInfo {
		private final List<String> firstPron;
		private final Set<String> phoneSet;

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
	}
	
}
