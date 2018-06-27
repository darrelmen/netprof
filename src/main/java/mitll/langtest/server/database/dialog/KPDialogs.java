package mitll.langtest.server.database.dialog;

import mitll.npdata.dao.SlickDialog;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog data from Paul - 6/20/18
 */
public class KPDialogs {
  String docIDS = "333815\n" +
      "333816\n" +
      "333817\n" +
      "333818\n" +
      "333819\n" +
      "333821\n" +
      "333822\n" +
      "333823\n" +
      "333824\n" +
      "333825";
  String title = "Meeting someone for the first time\n" +
      "What time is it?\n" +
      "You dialed the wrong number.\n" +
      "What will you do during the coming school break?\n" +
      "Where should I go to exchange currency?\n" +
      "What do Koreans do in their spare time?\n" +
      "Please give me two tickets for the 10:30 showing?\n" +
      "Please exchange this for a blue tie.\n" +
      "Common Ailments and Symptoms\n" +
      "Medical Emergencies";
  String dir = "010_C01\n" +
      "001_C05\n" +
      "003_C09\n" +
      "023_C09\n" +
      "036_C13\n" +
      "001_C17\n" +
      "019_C17\n" +
      "001_C18\n" +
      "005_C29\n" +
      "010_C30";
  String chapter = "1\n" +
      "5\n" +
      "9\n" +
      "9\n" +
      "13\n" +
      "17\n" +
      "17\n" +
      "18\n" +
      "29\n" +
      "30";
  String page = "12\n" +
      "5\n" +
      "5\n" +
      "15\n" +
      "25\n" +
      "5\n" +
      "26\n" +
      "4\n" +
      "7\n" +
      "12";
  String pres = "Topic Presentation B\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation C\n" +
      "Topic Presentation A\n" +
      "Topic Presentation C\n" +
      "Topic Presentation A\n" +
      "Topic Presentation A\n" +
      "Topic Presentation B";

  String unit = "1\n" +
      "2\n" +
      "3\n" +
      "3\n" +
      "4\n" +
      "5\n" +
      "5\n" +
      "5\n" +
      "8\n" +
      "8";

  public List<SlickDialog> getDialogs(int defaultUser, int projID) {
    String[] docs = docIDS.split("\n");
    String[] titles = title.split("\n");
    String[] units = unit.split("\n");
    String[] chapters = chapter.split("\n");
    String[] pages = page.split("\n");
    String[] topics = pres.split("\n");
    String[] dirs = dir.split("\n");

    List<SlickDialog> dialogs = new ArrayList<>();
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    for (int i = 0; i < docs.length; i++) {
      dialogs.add(new SlickDialog(-1,
          defaultUser,
          projID,
          -1,
          -1,
          modified,
          modified,
          DialogType.DIALOG.toString(),
          "",
          "",
          titles[i],
          "",
          topics[i],
          2,
          0.5F,
//          "",
          "",
          "",
          ""
      ));
    }
    return dialogs;
  }
}
