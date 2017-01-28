package mitll.langtest.client.scoring;

import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created by go22670 on 1/27/17.
 */
public interface MiniScoreListener {
  void gotScore(PretestScore score, boolean showOnlyOneExercise, String path);

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel#gotScore(PretestScore, boolean, String)
   * @param hydecScore
   */
  void addScore(CorrectAndScore hydecScore);
}
