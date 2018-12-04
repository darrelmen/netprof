package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Read the excel spreadsheet that we're using (for now) to define the interpreter turns.
 */
public class InterpreterReader extends BaseDialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(InterpreterReader.class);

  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {

//    int projID = project.getID();
//    String[] docs = dialogProps.docIDS.split("\n");
//    String[] titles = dialogProps.title.split("\n");
//    String[] ktitles = dialogProps.fltitle.split("\n");
//
//    String[] units = dialogProps.unit.split("\n");
//    String[] chapters = dialogProps.chapter.split("\n");
//    String[] pages = dialogProps.page.split("\n");
//    String[] topics = dialogProps.pres.split("\n");
//    String[] dirs = dialogProps.dir.split("\n");

    Timestamp modified = new Timestamp(System.currentTimeMillis());

    Map<Dialog, SlickDialog> dialogToSlick = new HashMap<>();
    String dialogDataDir = getDialogDataDir(project);
    String projectLanguage = project.getLanguage().toLowerCase();

    String dirPath = dialogDataDir;
    File loc = new File(dirPath);
    boolean directory = loc.isDirectory();
    if (!directory) logger.warn("huh? not a dir");

    File excelFile = new File(loc,"interpreter.xlsx");



    return dialogToSlick;
  }
}
