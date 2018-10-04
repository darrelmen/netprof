/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.shared.dialog.*;
import mitll.npdata.dao.SlickDialogSession;

import java.util.List;
import java.util.Map;

public interface IDialogSessionDAO extends IDAO {
  int add(int userid,
          int projid,
          int dialogid,

          long modified,
          long end,
          INavigation.VIEWS view,
          DialogStatus status,
          int numrecordings,
          float score,
          float speakingrate
  );

  // seems like we want a summary of the latest scores
  Map<Integer, Integer> getLatestDialogSessionScores(int projid, int userid);

  /**
   * @param userid
   * @param dialog
   * @return
   * @see mitll.langtest.server.services.DialogServiceImpl#getDialogSessions(int, int)
   */
  List<IDialogSession> getDialogSessions(int userid, int dialog);

/*
  List<IDialogSession> getCurrentDialogSessions(int userid);
*/

  /**
   * For when we want to drop the current dialog data and reload
   *
   * @param id
   * @see mitll.langtest.server.database.project.DialogPopulate#cleanDialog
   */
  void removeForProject(int id);

  /**
   * @see mitll.langtest.server.services.DialogServiceImpl#addSession
   * @param dialogSession
   * @return
   */
  int add(DialogSession dialogSession);

  SlickDialogSession byID(int dialogSessionID);

  void update(SlickDialogSession slickDialogSession);
}
