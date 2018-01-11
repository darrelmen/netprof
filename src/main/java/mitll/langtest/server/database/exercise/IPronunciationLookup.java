package mitll.langtest.server.database.exercise;

/**
 * Created by go22670 on 1/19/17.
 */
public interface IPronunciationLookup {
  String getPronunciationsFromDictOrLTS(String transcript, String transliteration);
  int getNumPhonesFromDictionary(String transcript, String transliteration);

  boolean hasDict();
  boolean hasModel();
}
