package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.List;

public interface IPronunciationLookup {
  /**
   * @param transcript
   * @param transliteration
   * @param possibleProns
   * @return
   * @see ASR#getHydraDict(String, String, List)
   */
  String createHydraDict(String transcript, String transliteration, List<WordAndProns> possibleProns);

  String getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones, boolean makeCandidates, List<WordAndProns> possible);

  int getNumPhonesFromDictionaryOrLTS(String transcript, String transliteration);

  String getCleanedTranscript(String cleaned);

  String getPronStringForWord(String word, Collection<String> apply, boolean justPhones);

  String getUsedTokens(Collection<String> lmSentences, List<String> background);

  SmallVocabDecoder getSmallVocabDecoder();
}
