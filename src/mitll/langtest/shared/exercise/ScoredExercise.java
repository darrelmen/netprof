package mitll.langtest.shared.exercise;

import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.List;

/**
 * Created by go22670 on 1/26/16.
 */
public interface ScoredExercise {
  List<CorrectAndScore> getScores();

  float getAvgScore();
}
