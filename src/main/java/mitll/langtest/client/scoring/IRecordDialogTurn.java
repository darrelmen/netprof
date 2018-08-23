package mitll.langtest.client.scoring;

import mitll.langtest.client.banner.RehearseViewHelper;

public interface IRecordDialogTurn {
  /**
   * @see RehearseViewHelper#showScores()
   */
  void showScoreInfo();

  /**
   * @see RehearseViewHelper#clearScores()
   */
  void clearScoreInfo();
}
