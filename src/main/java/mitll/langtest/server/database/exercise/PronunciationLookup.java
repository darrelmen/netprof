package mitll.langtest.server.database.exercise;

/**
 * Created by go22670 on 1/19/17.
 */
public interface PronunciationLookup {
  String getPronunciations(String transcript, String transliteration);
  int getNumPhones(String transcript, String transliteration);
}
