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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.dialog.IDialogSession;

class DialogSessionAnalysisTab<T extends IDialogSession> extends AnalysisTab {
  /**
   * @param controller
   * @param selectedUser
   * @param overallBottom
   * @param reqCounter
   * @param reqID
   * @param userID
   * @see SessionContainer#getAnalysisTab
   */
  DialogSessionAnalysisTab(ExerciseController controller,
                           T selectedUser,
                           DivWidget overallBottom,
                           ReqCounter reqCounter,
                           int reqID, int userID) {
    super(controller, overallBottom,
        selectedUser.getView().toString(), false, reqCounter, INavigation.VIEWS.LISTEN, new AnalysisRequest()
            .setUserid(userID)
            .setMinRecordings(0)
            .setListid(-1)
            .setReqid(reqID)
            .setDialogID(new SelectionState().getDialog())
            .setDialogSessionID(selectedUser.getID()), 850, false);
  }

  /**
   * @param tableWithPager
   * @param containerID
   * @param heading
   * @param addLeftMargin
   * @return
   */
  @Override
  protected DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading heading, boolean addLeftMargin) {
    DivWidget wordContainerDiv = super.getWordContainerDiv(tableWithPager, containerID, heading, addLeftMargin);
    wordContainerDiv.setWidth(97 + "%");
    Style style = wordContainerDiv.getElement().getStyle();
    style.setClear(Style.Clear.BOTH);
    style.setOverflow(Style.Overflow.HIDDEN);
    if (addLeftMargin) {
      wordContainerDiv.addStyleName("leftFiveMargin");
    }
    wordContainerDiv.addStyleName("bottomFiveMargin");
    return wordContainerDiv;
  }
}
