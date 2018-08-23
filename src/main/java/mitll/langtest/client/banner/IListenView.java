package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Image;
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.shared.answer.Validity;

public interface IListenView {
  int getVolume();

  void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn);

  void setSmiley(Image smiley, double total);

  void addPacketValidity(Validity validity);
  void stopRecording();

  int getNumValidities();

}
