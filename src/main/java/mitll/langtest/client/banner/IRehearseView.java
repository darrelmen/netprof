package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Image;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;

public interface IRehearseView extends IListenView {
  //int getVolume();

  void useResult(AudioAnswer audioAnswer);

  void useInvalidResult(int exid);

  void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn);

  void setEmoticon(Image smiley, double total);

  void addPacketValidity(Validity validity);

  void stopRecording();

  int getNumValidities();
}
