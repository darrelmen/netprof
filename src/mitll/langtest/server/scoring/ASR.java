package mitll.langtest.server.scoring;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import corpus.LTS;
//import mitll.langtest.server.scoring.ASRWebserviceScoring.PhoneInfo;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;

// TODO make this actually have method signatures...
public interface ASR {

	public <T extends CommonExercise> void sort(List<T> toSort);
	public Collator getCollator();
	public boolean checkLTS(String foreignLanguagePhrase);
	public PhoneInfo getBagOfPhones(String foreignLanguagePhrase);
	public SmallVocabDecoder getSmallVocabDecoder();
	public String getUsedTokens(Collection<String> lmSentences, List<String> background);
	public Collection<String> getValidPhrases(Collection<String> phrases);
	public PretestScore scoreRepeat(String testAudioDir, String testAudioFileNoSuffix,
			String sentence, String imageOutDir,
			int imageWidth, int imageHeight, boolean useScoreForBkgColor,
			boolean decode, String tmpDir,
			boolean useCache, String prefix);
	
	
	public static class PhoneInfo {
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
