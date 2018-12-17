package mitll.langtest.client.scoring;

import mitll.langtest.client.dialog.RehearseViewHelper;
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

  void switchAudioToStudent();
  void switchAudioToReference();

  void revealScore();
}
