package mitll.langtest.client.scoring;

import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;

/**
 * Created by go22670 on 1/27/17.
 */
public interface MiniScoreListener {
  /**
   * @see SimpleRecordAudioPanel#getScoreHistory
   * @param host
   */
  void showChart(String host);

  /**
   * @see SimpleRecordAudioPanel#useResult(PretestScore, boolean, String)
   * @param score
   * @param path
   */
  void gotScore(PretestScore score, String path);

  /**
   * @see MiniScoreListener#gotScore(PretestScore, String)
   * @see SimpleRecordAudioPanel#showRecordingHistory
   * @param hydecScore
   */
  void addScore(CorrectAndScore hydecScore);
}
