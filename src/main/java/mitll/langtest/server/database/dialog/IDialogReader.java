package mitll.langtest.server.database.dialog;

import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.List;
import java.util.Map;

public interface IDialogReader {
  List<Dialog> getDialogs(int defaultUser, int projID, Map<CommonExercise, String> exToAudio);
}
