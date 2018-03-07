package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.List;

public interface IPronunciationLookup {
  /**
   * @see ASR#getHydraDict(String, String, List)
   * @param transcript
   * @param transliteration
   * @param possibleProns
   * @return
   */
  String createHydraDict(String transcript, String transliteration, List<String> possibleProns);

  int getNumPhonesFromDictionaryOrLTS(String transcript, String transliteration);

  String getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones, boolean makeCandidates, List<String> possible);

  String getCleanedTranscript(String cleaned);

  String getPronStringForWord(String word, String[] apply, boolean justPhones);

  String getUsedTokens(Collection<String> lmSentences, List<String> background);

  SmallVocabDecoder getSmallVocabDecoder();
}
