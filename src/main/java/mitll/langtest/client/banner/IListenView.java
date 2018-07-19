package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Image;
import mitll.langtest.client.scoring.IRecordDialogTurn;

import java.util.List;

public interface IListenView {
  int getVolume();
  void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn);

 // void setTurns(List<IRecordDialogTurn> allTurns);

  void setSmiley(Image smiley, double total);
}
