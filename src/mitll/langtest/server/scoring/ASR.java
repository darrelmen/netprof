package mitll.langtest.server.scoring;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import corpus.LTS;
//import mitll.langtest.server.scoring.ASRWebserviceScoring.PhoneInfo;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.PretestScore;

// TODO make this actually have method signatures...
public interface ASR {
	<T extends CommonExercise> void sort(List<T> toSort);
	Collator getCollator();
	boolean checkLTS(String foreignLanguagePhrase);
	PhoneInfo getBagOfPhones(String foreignLanguagePhrase);
	SmallVocabDecoder getSmallVocabDecoder();
	String getUsedTokens(Collection<String> lmSentences, List<String> background);

	PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
													 String sentence, Collection<String> lmSentences, String imageOutDir,
													 int imageWidth, int imageHeight, boolean useScoreForBkgColor,
													 boolean decode, String tmpDir,
													 boolean useCache, String prefix, Result precalcResult);
	
	
	class PhoneInfo {
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
	}
	
}
