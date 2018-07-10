package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;

import java.util.Map;

public interface IDialogReader {
  Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID, Map<ClientExercise, String> exToAudio, Project project);
}
