package mitll.langtest.client.scoring;

import mitll.langtest.client.banner.RehearseViewHelper;
import mitll.langtest.shared.answer.AudioAnswer;

public interface IRecordDialogTurn {
  /**
   * @see RehearseViewHelper#showScores()
   */
  void showScoreInfo();

  /**
   * @see RehearseViewHelper#clearScores()
   */
  void clearScoreInfo();


  void useResult(AudioAnswer result);
  void useInvalidResult();
}
