package mitll.langtest.client.dialog;

import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;

/**
 * @see mitll.langtest.client.scoring.RecordDialogExercisePanel
 */
public interface IRehearseView extends IListenView {
  void useResult(AudioAnswer audioAnswer);

  /**
   * @see
   * @param exid
   */
  void useInvalidResult(int exid);

  //int getDialogSessionID();

  void addPacketValidity(Validity validity);

  void stopRecording();

  int getNumValidities();
}
