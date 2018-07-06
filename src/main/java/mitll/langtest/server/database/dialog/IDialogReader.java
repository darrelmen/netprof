package mitll.langtest.server.database.dialog;

import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.npdata.dao.SlickDialog;

import java.util.Map;

public interface IDialogReader {
  Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID, Map<CommonExercise, String> exToAudio);
}
