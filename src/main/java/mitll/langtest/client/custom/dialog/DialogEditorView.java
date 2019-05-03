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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.DialogEditor;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Created by go22670 on 7/3/17.
 */
public class DialogEditorView<T extends IDialog> extends ContentEditorView<T> {
  private final Logger logger = Logger.getLogger("DialogEditorView");

  private static final String LIST = "dialog";
  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to view a " + LIST;

  /**
   *
   */
  private static final String YOUR_LISTS1 = "Your Dialogs";
  public static final String DIALOG = "Dialog";

  private static final int MY_LIST_HEIGHT = 450;//500;//530;//560;

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
    addYours(left);
  }

  private void addYours(DivWidget left) {
    showYours(Collections.emptyList(), left);

    ExerciseListRequest request =
        new ExerciseListRequest(0, controller.getUser(), controller.getProjectID()).setSortByDate(true);

    controller.getDialogService().getDialogs(request,
        new AsyncCallback<ExerciseListWrapper<T>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<T> result) {
            getMyLists().populateTable(result.getExercises());
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
    setMyLists(myLists);
    Panel tableWithPager = myLists.getTableWithPager(result);

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.BOTTOM);
    addPagerAndHeader(tableWithPager, YOUR_LISTS1, left);
    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    tableWithPager.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    left.add(getButtons(this.getMyLists()));
  }

  @Override
  protected void populateUniqueListNames(Collection<T> result) {
    result.forEach(list -> names.add(list.getName()));
  }

  /**
   * @return
   * @see ContentEditorView#doAdd
   */
  @NotNull
  @Override
  protected CreateDialog<T> getCreateDialog() {
    return new CreateDialogDialog<T>(names, controller, this);
  }

  /**
   * TODO : fill in with fancy editor
   *
   * @param selectedItem
   */
  @Override
  protected void editList(T selectedItem) {
    DivWidget listContent = new DivWidget();

    DialogEditor editorTurnDialogEditor = new DialogEditor(controller, INavigation.VIEWS.DIALOG_EDITOR, selectedItem);
    editorTurnDialogEditor.showContent(listContent, INavigation.VIEWS.DIALOG_EDITOR);

    logger.info("list content " + listContent);

    new DialogHelper(true).show(
        "Add/Edit Turns" + " : " + getListName(),
        Collections.emptyList(),
        listContent,
        "Done",
        null,
        new MyShownCloseListener(editorTurnDialogEditor), 710, -1, true);

  }

  private class MyShownCloseListener implements DialogHelper.ShownCloseListener {
    DialogEditor editItem;

    MyShownCloseListener(DialogEditor editItem) {
      this.editItem = editItem;
    }

    @Override
    public boolean gotYes() {
//            int numItems = currentSelectionFromMyLists.getNumItems();
      //   logger.info("editList : on " + currentSelectionFromMyLists.getName() + " now " + numItems);
      getMyLists().flush();
      getMyLists().redraw();
      return true;
    }

    /**
     * CRITICAL TO REMOVE LISTENER!
     */
    @Override
    public void gotHidden() {
      // logger.info("Got hidden ");
      // editItem.removeHistoryListener();
      // History.newItem("");
    }

    @Override
    public void gotNo() {
    }

    @Override
    public void gotShown() {
      // logger.info("editList : edit view shown!");
      //editItem.reload();
      editItem.grabFocus();
    }
  }

  @NotNull
  private String getListName() {
    T originalList = getCurrentSelectionFromMyLists();
    boolean hasDescrip = !originalList.getEnglish().isEmpty();
    return originalList.getName() +
        (hasDescrip ? " (" + originalList.getEnglish() + ")" : "");
  }

  @Override
  protected void doDelete(UIObject delete, T currentSelection) {
    final int uniqueID = currentSelection.getID();

    controller.getDialogService().delete(controller.getProjectID(), uniqueID, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Boolean result) {
        gotDeleteResponse(result, currentSelection, uniqueID);

      }
    });
  }

  @NotNull
  @Override
  protected String getMailTo() {
    IDialog currentSelection = getCurrentSelection();
    return new UserListSupport(controller)
        .getMailToDialog(currentSelection.getID(), currentSelection.getForeignLanguage());
  }

  /**
   * TODO: get rid of?
   *
   * @seex CreateListDialog#makeCreateButton
   */
  @Override
  public void gotEdit() {
  }

  /**
   * @see #showYours(Collection, DivWidget)
   */
  private class MyDialogContainer extends DialogContainer<T> {
    MyDialogContainer() {
      super(DialogEditorView.this.controller);
    }

    @Override
    public void gotClickOnItem(final T user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
    }

    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    /**
     * @param selected
     * @see SimplePagingContainer#addDoubleClick
     */
    @Override
    protected void gotDoubleClickOn(T selected) {
      logger.info("gotDoubleClickOn got double click on " + selected);
      editList(getCurrentSelection());
    }
  }

  @Override
  protected void addImportButton(DivWidget buttons) {
  }


  @NotNull
  @Override
  protected CreateDialog<T> getEditDialog() {
    return new CreateDialogDialog<T>(getCurrentSelectionFromMyLists(), names, true, controller, this);
  }

  @Override
  protected String getName() {
    return "Dialog";
  }
}
