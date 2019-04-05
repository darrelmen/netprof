/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

@Deprecated
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
   * @param englishProject
   * @return
   * @see mitll.langtest.server.database.project.DialogPopulate#populateDatabase
   */
  //@Override
  public Map<Dialog, SlickDialog> getDialogs(int defaultUser,
                                             Map<ClientExercise, String> exToAudio,
                                             Project project, Project englishProject) {
    return getDialogsByProp(defaultUser, exToAudio, project, dialogProps, new ArrayList<>());
  }
}
