package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IPronunciationLookup {
  /**
   * @param transcript
   * @param transliteration
   * @param possibleProns
   * @return
   * @see ASR#getHydraDict(String, String, List)
   */
  TransNormDict createHydraDict(String transcript, String transliteration, List<WordAndProns> possibleProns);

  InDictStat getTokenStats(String transcript);

  TransNormDict getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones, boolean makeCandidates, List<WordAndProns> possible);

  int getNumPhonesFromDictionaryOrLTS(String transcript, String transliteration);

  String getCleanedTranscript(String cleaned);

  String getPronStringForWord(String word, Collection<String> apply, boolean justPhones);

  String getUsedTokens(Collection<String> lmSentences, List<String> background);

  SmallVocabDecoder getSmallVocabDecoder();
  Set<String> getOOV();

  class InDictStat {
    private int numTokens;
    private int numInDict;

    InDictStat(int numInDict, int numTokens) {
      this.numInDict = numInDict;
      this.numTokens = numTokens;
    }

    public int getNumTokens() {
      return numTokens;
    }

    public int getNumInDict() {
      return numInDict;
    }
  }
}
