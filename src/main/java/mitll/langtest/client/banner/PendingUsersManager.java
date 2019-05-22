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

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.result.ActiveUsersManager;
import mitll.langtest.shared.user.ActiveUser;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

class PendingUsersManager extends ActiveUsersManager {
  private final Logger logger = Logger.getLogger("PendingUsersManager");

  private static final String APPROVED = "Approved?";
  private Set<Integer> appprove = new HashSet<>();
  private Set<Integer> disapprove = new HashSet<>();

  private Button okButton, disapproveButton, approveButton;
  private IBanner banner;

  PendingUsersManager(ExerciseController controller, IBanner banner) {
    super(controller);
    this.banner = banner;
  }

  /**
   * @param hours
   * @param dialogBox
   * @param dialogVPanel
   * @see #show(String, int)
   */
  protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
    addPrompt(dialogVPanel);

//    logger.info("getUsers req " +controller.getProjectID());
    controller.getUserService().getPendingUsers(controller.getProjectID(), new AsyncCallback<List<ActiveUser>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError("getting pending teacher requests", caught);
      }

      @Override
      public void onSuccess(List<ActiveUser> result) {
        //  logger.info("gotUsers req " +controller.getProjectID() + " " +result.size());
        gotUsers(result, dialogVPanel, dialogBox);

        boolean enabled = !result.isEmpty();
//        logger.info("has items :  " +enabled);
        disapproveButton.setEnabled(enabled);
        approveButton.setEnabled(enabled);
      }
    });
  }

  @Override
  protected ActiveUserBasicUserContainer getUserContainer() {
    return new ActiveUserBasicUserContainer() {

      @Override
      protected void addVisitedCols(List<ActiveUser> list) {
        addIsApproved();
      }

      @Override
      protected String getDateColHeader() {
        return "Request time";
      }

      private void addIsApproved() {
        Column<ActiveUser, SafeHtml> diff = getPublic();
        diff.setSortable(true);
        addColumn(diff, new TextHeader(APPROVED));
        table.addColumnSortHandler(getPublicSorted(diff, getList()));
        table.setColumnWidth(diff, 150 + "px");
      }

      private Column<ActiveUser, SafeHtml> getPublic() {
        return new Column<ActiveUser, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
          @Override
          public void onBrowserEvent(Cell.Context context, Element elem, ActiveUser object, NativeEvent event) {
            super.onBrowserEvent(context, elem, object, event);
            checkGotClick(object, event);
          }

          @Override
          public SafeHtml getValue(ActiveUser shell) {
            return getSafeHtml(shell.getState().toString());
          }
        };
      }

      private ColumnSortEvent.ListHandler<ActiveUser> getPublicSorted(Column<ActiveUser, SafeHtml> englishCol,
                                                                      List<ActiveUser> dataList) {
        ColumnSortEvent.ListHandler<ActiveUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
        columnSortHandler.setComparator(englishCol, Comparator.comparing(ActiveUser::getState));
        return columnSortHandler;
      }
    };

  }

  @Override
  protected void addPrompt(Panel dialogVPanel) {
    HTML child = new HTML("Please confirm these users are, in fact, teachers at DLI.");
    child.addStyleName("bottomFiveMargin");
    dialogVPanel.add(child);
  }

  /**
   * @param dialogBox
   * @param horiz
   * @return
   */
  @Override
  protected Button addButtons(DialogBox dialogBox, DivWidget horiz) {
    {
      Button cancel = getButton("Cancel");
      cancel.setType(ButtonType.INFO);
      cancel.addClickHandler(event -> dialogBox.hide());
      cancel.addStyleName("leftFiveMargin");
      cancel.addStyleName("rightFiveMargin");
      horiz.add(cancel);
    }

    okButton = super.addButtons(dialogBox, horiz);
    okButton.setEnabled(false);
    okButton.addStyleName("leftTenMargin");


    {
      disapproveButton = getButton("Disapprove");
      disapproveButton.addClickHandler(event -> gotDisapprove());
      disapproveButton.setType(ButtonType.WARNING);
      disapproveButton.addStyleName("leftFiveMargin");
      disapproveButton.addStyleName("rightFiveMargin");
      horiz.add(disapproveButton);
    }

    {
      approveButton = getButton("Approve");
      approveButton.addClickHandler(event -> gotApprove());
      horiz.add(approveButton);
    }

    return okButton;
  }

  @Override
  protected void gotOKClick(DialogBox dialogBox) {
//    logger.info("\n\n\ngotOKClick got ok click! " + appprove + " vs " + disapprove);
    controller.getUserService().approveAndDisapprove(appprove, disapprove, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {

        logger.info("OK, done");

        // refresh the menu after OK
        banner.setCogTitle();
      }
    });
    super.gotOKClick(dialogBox);


  }

  private void gotDisapprove() {
    okButton.setEnabled(true);
    ActiveUser currentSelection = getCurrentSelection();
    if (currentSelection != null) {
      int id = currentSelection.getID();
      appprove.remove(id);
      disapprove.add(id);
      afterAction(currentSelection, ActiveUser.PENDING.DENIED);
    }
  }

  private void gotApprove() {
    okButton.setEnabled(true);

    ActiveUser currentSelection = getCurrentSelection();
    if (currentSelection != null) {
      int id = currentSelection.getID();
      disapprove.remove(id);
      appprove.add(id);
      afterAction(currentSelection, ActiveUser.PENDING.APPROVED);
    }
  }

  private void afterAction(ActiveUser currentSelection, ActiveUser.PENDING approved) {
    currentSelection.setState(approved);
    reload();
  }
}
