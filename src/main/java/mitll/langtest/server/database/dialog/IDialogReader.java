package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface IDialogReader {
  String DIALOG = "dialog";
  List<String> SPEAKER_LABELS = Arrays.asList("A", "B", "C", "D", "E", "F");
  String UNIT = IDialog.METADATA.UNIT.getLC();
  String CHAPTER = IDialog.METADATA.CHAPTER.getLC();
  String PAGE = IDialog.METADATA.PAGE.getLC();
  String PRESENTATION = IDialog.METADATA.PRESENTATION.getLC();
  String SPEAKER = IDialog.METADATA.SPEAKER.getCap();
  String OPT_NETPROF_DIALOG = "/opt/netprof/dialog/";
  String IMAGES = "images/";
  String JPG = ".jpg";

  Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID, Map<ClientExercise, String> exToAudio, Project project);
}
