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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 7/3/17.
 */
public class DialogEditorView<T extends IDialog> extends ContentEditorView<T> {
  private final Logger logger = Logger.getLogger("DialogEditorView");

  private static final String LIST = "dialog";
  private static final String DELETE_LIST = "Delete " + LIST + ".";

  //private static final int MAX_HEIGHT = 710;

  /**
   * With quiz button
   */
  private static final int MIN_WIDTH = 659;//599;

  private static final String EDIT_THE_ITEMS_ON_LIST = "Edit the items in the " + LIST + ".";
  // private static final String MY_LISTS = "myLists";

  /**
   *
   */
  private static final String SHARE = "Share";
  private static final String SHARE_THE_LIST = "Share the " + LIST + " with someone.";
  private static final String SAVE = "Save";


  /**
   * @see #getAddItems
   */
  private static final String ITEMS = "Items";
  private static final String ADD = "Add";
  private static final String CANCEL = "Cancel";

  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to view a " + LIST;

  /**
   *
   */
  private static final String YOUR_LISTS1 = "Your Dialogs";


  public static final String DIALOG = "Dialog";
  private static final String EDIT1 = "Edit";
//  private static final String EDIT = EDIT1;

  /**
   * @see #editList()
   */
  private static final String ADD_EDIT_ITEMS = "Add/Edit Items";

  private static final int MY_LIST_HEIGHT = 500;//530;//560;

  private Button editButton, removeButton;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.NewContentChooser#NewContentChooser
   */
  public DialogEditorView(ExerciseController controller) {
    super(controller);
  }

  @Override
  protected String getItemName() {
    return "Dialog";
  }

  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    super.showContent(listContent, instanceName);

//    DivWidget right = new DivWidget();
//    right.getElement().setId("right");

//    leftRight.add(right);
//
//    right.setWidth("100%");
//    DivWidget top = new DivWidget();
//    top.getElement().setId("top");
//    right.add(top);
//
//    styleTopAndBottom(top);
//
//    top.addStyleName("bottomFiveMargin");
//
//    DivWidget bottom = new DivWidget();
//    right.add(bottom);
//    bottom.getElement().setId("bottom");
//
//    styleTopAndBottom(bottom);

    addYours(left);
  }

//  private void styleTopAndBottom(DivWidget bottom) {
//    bottom.addStyleName("leftTenMargin");
//    bottom.addStyleName("rightFiveMargin");
//    bottom.addStyleName("cardBorderShadow");
//  }

  private void addYours(DivWidget left) {
    showYours(Collections.emptyList(), left);

    ExerciseListRequest request = new ExerciseListRequest(0, controller.getUser());
    controller.getDialogService().getDialogs(request,
        new AsyncCallback<ExerciseListWrapper<T>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<T> result) {
            myLists.populateTable(result.getExercises());
            populateUniqueListNames(result.getExercises());

            Scheduler.get().scheduleDeferred(() -> setShareHREF(getCurrentSelectionFromMyLists()));
          }
        });
  }

  /**
   * @param result
   * @param left
   * @see #addYours
   */
  private void showYours(Collection<T> result, DivWidget left) {
    DialogContainer<T> myLists = new MyDialogContainer();
    Panel tableWithPager = (this.myLists = myLists).getTableWithPager(result);

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.RIGHT);
    addPagerAndHeader(tableWithPager, YOUR_LISTS1, left);
    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    tableWithPager.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    left.add(getButtons(DialogEditorView.this.myLists));
  }

  @Override
  protected void populateUniqueListNames(Collection<T> result) {
    result.forEach(list -> {
      // if (list.getUserID() == controller.getUser()) {
      names.add(list.getName());
      logger.info("names now " + names);
      // }
    });
  }

  @NotNull
  @Override
  protected CreateDialog<T> getCreateDialog() {
    return new CreateDialogDialog<T>(names, controller);
  }

  @Override
  protected void editList() {

  }

  @Override
  protected void doDelete(UIObject delete, T currentSelection) {

  }

//  @Override
//  protected void doEdit() {
//
//  }


  /*  @NotNull
  private String getListName() {
    IDialog originalList = getCurrentSelectionFromMyLists();
    boolean hasDescrip = !originalList.getOrientation().isEmpty();
    return originalList.getForeignLanguage() +
        (hasDescrip ? " (" + originalList.getOrientation() + ")" : "");
  }*/


/*
  private void doDelete(UIObject delete, T currentSelection) {
    final int uniqueID = currentSelection.getID();
    controller.logEvent(delete, "Button", currentSelection.getEnglish(), "Delete");
    controller.getDialogService().delete(uniqueID, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        //   logger.warning("delete list call failed?");
        controller.handleNonFatalError("deleting a " + LIST, caught);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          removeFromLists(myLists, currentSelection);
        } else {
          logger.warning("deleteList ---> did not do deleteList " + uniqueID);
        }
      }
    });
  }
*/

 /* private void warnFirst(Button delete, T currentSelection) {
    new DialogHelper(true).show(
        "Delete " + currentSelection.getForeignLanguage() + " forever?",
        new Heading(2, "Are you sure?"),
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            doDelete(delete, currentSelection);
            return true;
          }

          @Override
          public void gotNo() {

          }

          @Override
          public void gotHidden() {

          }
        },
        500, -1);
  }*/

/*  private void removeFromLists(DialogContainer<T> listContainer, T currentSelection) {
    if (listContainer == myLists) {
      String name = currentSelection.getForeignLanguage();
      names.remove(name);
    }

    int index = listContainer.getIndex(currentSelection);
    //logger.info("deleteList ---> did do deleteList " + uniqueID + " index " + index);
    listContainer.forgetItem(currentSelection);
    int numItems = listContainer.getNumItems();
    if (numItems == 0) {
      //delete.setEnabled(false);
      listContainer.disableAll();
    } else {
      if (index == numItems) index = numItems - 1;
      HasID at = listContainer.getAt(index);
      //logger.info("next is " + at.getName());
      int id = at.getID();
//      listContainer.markCurrentExercise(id);
      Scheduler.get().scheduleDeferred(() -> listContainer.markCurrentExercise(id));

    }
  }*/

  @NotNull
  @Override
  protected String getMailTo() {
    IDialog currentSelection = myLists.getCurrentSelection();

    return new UserListSupport(controller)
        .getMailToDialog(currentSelection.getID(), currentSelection.getForeignLanguage());
  }


  /**
   * @param userList
   * @see CreateListDialog#addUserList
   */
 /* @Override
  public void madeIt(T userList) {
    // logger.info("madeIt made it " + userList.getName());
    try {
      dialogHelper.hide();
      myLists.addExerciseAfter(null, userList);
      myLists.enableAll();
      names.add(userList.getForeignLanguage());
      editButton.setEnabled(true);
    } catch (Exception e) {
      logger.warning("got " + e);
    }
    Scheduler.get().scheduleDeferred(() -> myLists.markCurrentExercise(userList.getID()));
  }
*/

  /**
   * @seex CreateListDialog#makeCreateButton
   */
  @Override
  public void gotEdit() {
    //  editDialog.doEdit(myLists.getCurrentSelection(), myLists);
  }

  private class MyDialogContainer extends DialogContainer<T> {
    MyDialogContainer() {
      super(DialogEditorView.this.controller);
    }

    @Override
    public void gotClickOnItem(final T user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
      // enableQuizButton(quizButton);
    }

    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    @Override
    protected void gotDoubleClickOn(T selected) {
      logger.info("gotDoubleClickOn got double click on " + selected);
      //showLearnOrQuiz(selected);
      editList();
    }
  }

/*
  private void setShareButtonHREF() {
    share.setHref(getMailTo());
  }
*/

  @NotNull
  @Override
  protected CreateDialog<T> getEditDialog() {
    return null;
  }

  private class MyShownCloseListener implements DialogHelper.ShownCloseListener {
    EditItem editItem;

    MyShownCloseListener(EditItem editItem) {
      this.editItem = editItem;
    }

    @Override
    public boolean gotYes() {
//            int numItems = currentSelectionFromMyLists.getNumItems();
      //   logger.info("editList : on " + currentSelectionFromMyLists.getName() + " now " + numItems);
      myLists.flush();
      myLists.redraw();

      return true;
    }

    /**
     * CRITICAL TO REMOVE LISTENER!
     */
    @Override
    public void gotHidden() {
      // logger.info("Got hidden ");
      editItem.removeHistoryListener();
      History.newItem("");
    }

    @Override
    public void gotNo() {
    }

    @Override
    public void gotShown() {
      // logger.info("editList : edit view shown!");

      editItem.reload();
      editItem.grabFocus();
    }
  }
}
