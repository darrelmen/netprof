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
        selectedUser.getView().toString(), false, reqCounter, INavigation.VIEWS.STUDY, new AnalysisRequest()
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

/*  @NotNull
  @Override
  protected DivWidget getBottom(boolean isTeacherView) {
    DivWidget bottom = super.getBottom(isTeacherView);
    bottom.removeStyleName("inlineFlex");
    return bottom;
  }*/
}
