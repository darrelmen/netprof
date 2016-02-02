package mitll.langtest.shared.exercise;

import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by go22670 on 2/1/16.
 */
public interface MutableExercise {
  void setBagOfPhones(Set<String> bagOfPhones);

  void setFirstPron(List<String> phones);

  void setEnglish(String english);

  void setForeignLanguage(String foreignLanguage);

  void setTransliteration(String transliteration);

  /**
   * @see mitll.langtest.server.database.ResultDAO#attachScoreHistory(long, CommonExercise, boolean)
   * @param scoreTotal
   */
  void setScores(List<CorrectAndScore> scoreTotal);

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotationsAndAudio
   * @param v
   */
  void setAvgScore(float v);

  void setRefSentences(Collection<String> orDefault);
}
