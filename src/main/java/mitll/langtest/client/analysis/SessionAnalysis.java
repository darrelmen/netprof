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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.dialog.IDialogSession;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class SessionAnalysis extends TwoColumnAnalysis<IDialogSession> {
  // private final Logger logger = Logger.getLogger("StudentAnalysis");

  private static final String SESSION = "Session";
  private static final String NO_SESSIONS_YET = "No Sessions yet...";
  private final int user;
  private final DivWidget userContainerBottom;
  private int dialogID;

  /**
   * @param controller
   * @param user
   * @param userContainerBottom
   * @param dialogID
   * @see NewContentChooser#showProgress
   */
  public SessionAnalysis(final ExerciseController controller, int user, DivWidget userContainerBottom, int dialogID) {
    Timer pleaseWaitTimer = getPleaseWaitTimer(controller);
    this.dialogID = dialogID;
    this.user = user;
    this.userContainerBottom = userContainerBottom;

    controller.getDialogService().getDialogSessions(
        user,
        dialogID,
        new AsyncCallback<List<IDialogSession>>() {
          @Override
          public void onFailure(Throwable caught) {
            finishPleaseWait(pleaseWaitTimer, controller.getMessageHelper());
            controller.handleNonFatalError("Error retrieving user dialog sessions", caught);
          }

          @Override
          public void onSuccess(List<IDialogSession> result) {
            showItems(result, pleaseWaitTimer, controller);
          }
        });
  }

  @Override
  protected DivWidget getContainerDiv(DivWidget table) {
    DivWidget containerDiv = super.getContainerDiv(table);
    containerDiv.getElement().getStyle().setProperty("maxHeight", 425 + "px");
    return containerDiv;
  }

  @Override
  protected String getStorageKey() {
    return "selected_session";
  }

  @NotNull
  @Override
  protected String getNoDataYetMessage() {
    return NO_SESSIONS_YET;
  }

  protected String getHeaderLabel() {
    return SESSION;
  }

  @NotNull
  protected MemoryItemContainer<IDialogSession> getItemContainer(ExerciseController controller,
                                                                 DivWidget bottom,
                                                                 DivWidget rightSide) {
    return new SessionContainer<IDialogSession>(controller, userContainerBottom == null ? bottom : userContainerBottom, rightSide, user, dialogID) {
      @Override
      public Panel getTableWithPager(Collection<IDialogSession> users) {
        Panel tableWithPager = super.getTableWithPager(users);
        setMinHeight(tableWithPager, 290);
        return tableWithPager;
      }
    };
  }
}
