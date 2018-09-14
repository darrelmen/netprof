package mitll.langtest.shared.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.exercise.*;

public interface IDialogSession extends HasID {
  int getUserid();

  int getProjid();

  int getDialogid();

  // start
  long getModified();

  long getEnd();

  INavigation.VIEWS getView();

  DialogStatus getStatus();

  int getNumRecordings();

  float getScore();

  float getSpeakingRate();
}
