package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

import static mitll.langtest.shared.dialog.IDialog.METADATA.FLTITLE;

/**
 * Dialog data from Paul - 6/20/18
 */
public class KPDialogs extends DialogReader implements IDialogReader {
  private static final Logger logger = LogManager.getLogger(KPDialogs.class);

  private final String docIDS =
      "333815\n" +
          "333816\n" +
          "333817\n" +
          "333818\n" +
          "333819\n" +
          "333821\n" +
          "333822\n" +
          "333823\n" +
          "333824\n" +
          "333825";

  private final String title =
      "Meeting someone for the first time\n" +
          "What time is it?\n" +
          "You dialed the wrong number.\n" +
          "What will you do during the coming school break?\n" +
          "Where should I go to exchange currency?\n" +
          "What do Koreans do in their spare time?\n" +
          "Please give me two tickets for the 10:30 showing?\n" +
          "Please exchange this for a blue tie.\n" +
          "Common Ailments and Symptoms\n" +
          "Medical Emergencies";

  private final String fltitle =
      "처음 만났을 때\n" +
          "지금 몇 시예요?\n" +
          "전화 잘못 거셨어요.\n" +

          "이번 방학에 뭐 할 거야?\n" +
          "환전하려면 어디로 가야 해요?\n" +
          "한국 사람들은 시간이 날 때 뭐 해요?\n" +

          "10시 반 표 두 장 주세요.\n" +
          "파란색 넥타이로 바꿔 주세요.\n" +
          "독감에 걸려서 고생했어.\n" +

          "구급차 좀 빨리 보내주세요.";

  private final String dir =
      "010_C01\n" +
          "001_C05\n" +
          "003_C09\n" +

          "023_C09\n" +
          "036_C13\n" +
          "001_C17\n" +
          "019_C17\n" +
          "001_C18\n" +
          "005_C29\n" +
          "010_C30";

  private final String unit =
      "1\n" +
          "2\n" +
          "3\n" +
          "3\n" +
          "4\n" +
          "5\n" +
          "5\n" +
          "5\n" +
          "8\n" +
          "8";
  private final String chapter =
      "1\n" +
          "5\n" +
          "9\n" +
          "9\n" +
          "13\n" +
          "17\n" +
          "17\n" +
          "18\n" +
          "29\n" +
          "30";
  private final String page =
      "12\n" +
          "5\n" +
          "5\n" +
          "15\n" +
          "25\n" +
          "5\n" +
          "26\n" +
          "4\n" +
          "7\n" +
          "12";
  private final String pres =
      "Topic Presentation B\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation C\n" +
          "Topic Presentation A\n" +
          "Topic Presentation A\n" +
          "Topic Presentation B";

  private DialogProps dialogProps = new DialogProps(docIDS, title, fltitle, dir, unit, chapter, page, pres);

  /**
   * @param defaultUser
   * @param projID
   * @param exToAudio
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase
   */
  @Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser, int projID,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project) {
    return getDialogsByProp(defaultUser, projID, exToAudio, project, dialogProps);
  }

}
