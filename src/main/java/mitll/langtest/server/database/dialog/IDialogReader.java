package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;

import java.util.Map;

public interface IDialogReader {
  /**
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase
   * @param defaultUser
   * @param exToAudio
   * @param project
   * @param englishProject
   * @return
   */
  Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                      Map<ClientExercise, String> exToAudio, Project project, Project englishProject);
}
