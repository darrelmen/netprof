package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

public class EnglishDialog extends DialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(EnglishDialog.class);

/*

  String values2 = "\n" +
      "Brown: Hello sir? Nice to meet you.\n" +
      "Park So-jung: Yes, I see you.\n" +

      "Brown: My name is Michael Brown. What is your name?\n"  +
      "Park So-jung: It is Park So Jung. Nice to meet you.\n" +

      "Brown: Yes, nice to meet you.\n" +
      "Park So-jung: Are you in the Air Force?\n" +

      "Brown: Yes, the Air Force.\n" +
      "Park So-jung:  Where are you from?\n" +
      "Brown: California.";
*/

  private final String docIDS =
      "333815\n" +
          "333816\n" +
          "333817\n";

  private final String title =
      "Meeting someone for the first time\n" +
          "What time is it?\n" +
          "You dialed the wrong number.\n";

  private final String fltitle =
      "처음 만났을 때\n" +
          "지금 몇 시예요?\n" +
          "전화 잘못 거셨어요.\n";

  private final String dir =
      "010_C01\n" +
          "001_C05\n" +
          "003_C09\n";

  private final String unit =
      "1\n" +
          "2\n" +
          "3\n";

  private final String chapter =
      "1\n" +
          "5\n" +
          "9\n";

  private final String page =
      "12\n" +
          "5\n" +
          "5\n";

  private final String pres =
      "Topic Presentation B\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n";

  private final DialogProps dialogProps = new DialogProps(docIDS, title, fltitle, dir, unit, chapter, page, pres);

  /**
   * @param defaultUser
   * @param exToAudio
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {
    return getDialogsByProp(defaultUser, exToAudio, project, dialogProps, new ArrayList<>());
  }
}
