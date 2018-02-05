package mitll.langtest.server.scoring;

import java.util.Collection;
import java.util.List;

public interface IPronunciationLookup {
  String createHydraDict(String transcript, String transliteration);

  int getNumPhonesFromDictionaryOrLTS(String transcript, String transliteration);

  String getPronunciationsFromDictOrLTS(String transcript, String transliteration, boolean justPhones);

  String getCleanedTranscript(String cleaned);

  String getPronStringForWord(String word, String[] apply, boolean justPhones);

  String getUsedTokens(Collection<String> lmSentences, List<String> background);

  SmallVocabDecoder getSmallVocabDecoder();
}
