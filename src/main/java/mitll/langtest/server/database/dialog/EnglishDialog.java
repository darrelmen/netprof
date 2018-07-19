package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class EnglishDialog extends DialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(EnglishDialog.class);

  String values = "" +
      "브라운: 선생님, 안녕하세요? 처음 뵙겠습니다.\n" +
      "박소정: 네, 처음 뵙겠습니다.\n" +
      "브라운: 제 이름 은 마이클 브라운입니다. 선생님 성함이 어떻게 되십니까?\n" +
      "박소정: 박소정입니다. 만나서 반갑습니다.\n" +
      "브라운: 네, 만나서 반갑습니다.\n" +
      "박소정: 브라운 씨는 공군입니까?\n" +
      "브라운: 네, 공군입니다.\n" +
      "박소정: 고향이 어디십니까?\n" +
      "브라운: 캘리포니아입니다.";

  String speaker1 = "Brown";
  String speaker2 = "Park So-jung";

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


  private final String docIDS =
      "333815\n";

  private final String title =
      "Meeting someone for the first time\n";

  private final String fltitle =
      "처음 만났을 때\n";

  private final String dir =
      "010_C01\n";

  private final String chapter =
      "1\n";

  private final String page =
      "12\n";

  private final String pres =
      "Topic Presentation B\n";

  private final String unit =
      "1\n";

  private DialogProps dialogProps = new DialogProps(docIDS, title, fltitle, dir, unit, chapter, page, pres);

  /**
   * @param defaultUser
   * @param projID
   * @param exToAudio
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#addDialogInfo
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {
    return getDialogsByProp(defaultUser, projID, exToAudio, project, dialogProps);
  }


}
