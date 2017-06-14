package mitll.langtest.client.scoring;

import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created by go22670 on 1/27/17.
 */
public interface MiniScoreListener {
  void showChart(String host);

  void gotScore(PretestScore score, String path);

  /**
   * @see MiniScoreListener#gotScore(PretestScore, String)
   * @see SimpleRecordAudioPanel#addScores
   * @param hydecScore
   */
  void addScore(CorrectAndScore hydecScore);
}
