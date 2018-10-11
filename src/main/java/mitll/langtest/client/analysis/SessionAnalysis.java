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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.dialog.IDialogSession;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/27/15.
 */
public class SessionAnalysis extends TwoColumnAnalysis<IDialogSession> {
  public static final String SESSION = "Session";
  // private final Logger logger = Logger.getLogger("StudentAnalysis");
  int user;

  /**
   * @param controller
   * @param user
   * @see NewContentChooser#showProgress
   */
  public SessionAnalysis(final ExerciseController controller, int user) {
    Timer pleaseWaitTimer = getPleaseWaitTimer(controller);

    this.user = user;
    controller.getDialogService().getDialogSessions(
        user,
        new SelectionState().getDialog(),
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
  protected void addBottom(DivWidget bottom) {
//    rightSide.add(bottom);
    //super.addBottom(bottom);
  }

  protected DivWidget addTop(Collection<IDialogSession> users,
                             ExerciseController controller, DivWidget bottom) {
    DivWidget rightSide = getRightSide();
    DivWidget table = getTable(users, controller, bottom, rightSide);
    add(getTop(table, bottom));
    return rightSide;
  }

  @Override
  protected String getStorageKey() {
    return "selected_session";
  }

  protected DivWidget getTable(Collection<IDialogSession> users,
                               ExerciseController controller,
                               DivWidget bottom,
                               DivWidget rightSide) {
    if (users.isEmpty()) {
      DivWidget divWidget = new DivWidget();
      divWidget.add(new HTML("No Sessions yet..."));
      return divWidget;
    } else {
      SessionContainer<IDialogSession> userContainer =
          new SessionContainer<>(controller, rightSide, bottom, user);
      DivWidget sessions = getContainerDiv(userContainer.getTable(users));
      sessions.addStyleName("cardBorderShadow");
      return sessions;
    }
  }

  private DivWidget getContainerDiv(DivWidget table) {
    return getContainerDiv(table, getHeading(SESSION));
  }

  private DivWidget getContainerDiv(Panel tableWithPager, Heading heading) {
    DivWidget wordsContainer = new DivWidget();
    wordsContainer.add(heading);
    wordsContainer.add(tableWithPager);
    return wordsContainer;
  }

  @NotNull
  private Heading getHeading(String words) {
    Heading wordsTitle = new Heading(3, words);
    wordsTitle.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    wordsTitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return wordsTitle;
  }
}
