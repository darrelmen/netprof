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
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.userlist.ImportBulk;
import mitll.langtest.client.custom.userlist.TableAndPager;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.INameable;
import mitll.langtest.shared.custom.IPublicPrivate;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public abstract class ContentEditorView<T extends INameable & IPublicPrivate>
    extends TableAndPager
    implements ContentView, CreateComplete<T> {
  private final Logger logger = Logger.getLogger("ContentEditorView");

  private static final String MAKE_A_NEW_LIST = "Make a new ";// + LIST + ".";
  private static final String CREATE_NEW_LIST = "Create New ";// + DIALOG;
  /**
   * @see #getRemoveButton
   */
  private static final String DELETE_LIST = "Delete";// list.";
  /**
   * @see #getEdit
   */
  private static final String EDIT_THE_LIST = "Edit the ";// + LIST + ", make it public.";

  static final String SHARE = "Share";
  static final String SHARE_THE_LIST = "Share the ";// + LIST + " with someone.";
  private static final String SAVE = "Save";

  private static final String EDIT_THE_ITEMS_ON_LIST = "Edit the items on ";
  /**
   * @see #getAddItems
   */
  private static final String ITEMS = "Items";
  private static final String ADD = "Add";
  private static final String CANCEL = "Cancel";

  /**
   * @see #getImport
   */
  private static final String IMPORT = "Import";
  private static final String EDIT1 = "Edit";
  private static final String EDIT = EDIT1;

  private static final String EDIT_TITLE = "";

  protected final Set<String> names = new HashSet<>();
  protected final ExerciseController<?> controller;
  protected DivWidget leftRight, left;
  protected Button share;
  private static final int MIN_WIDTH = 668;//659;
  protected DialogHelper dialogHelper;
  private ButtonMemoryItemContainer<T> myLists;

  protected Button editButton, removeButton;

  public ContentEditorView(ExerciseController controller) {
    this.controller = controller;
  }

  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    names.clear();

    listContent.clear();

    DivWidget leftRight = new DivWidget();
    leftRight.addStyleName("inlineFlex");
    listContent.add(leftRight);

    DivWidget left = new DivWidget();
    leftRight.add(left);
    left.addStyleName("rightFiveMargin");
    left.addStyleName("cardBorderShadow");

    this.left = left;
    this.leftRight = leftRight;
  }

  protected void setShareHREFLater() {
    Scheduler.get().scheduleDeferred(() -> setShareHREF(getCurrentSelectionFromMyLists()));
  }

  protected abstract void populateUniqueListNames(Collection<T> result);

  /**
   * @param container
   * @return
   */
  @NotNull
  protected DivWidget getButtons(ButtonMemoryItemContainer<T> container) {
    DivWidget buttons = getCommonButtonContainer();

    addImportButton(buttons);
    buttons.add(share = getShare());

    return buttons;
  }

  @NotNull
  DivWidget getCommonButtonContainer() {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.getElement().getStyle().setProperty("minWidth", MIN_WIDTH + "px");
    buttons.add(getAddButton());

    if (canMakeQuiz()) {
      buttons.add(getAddQuizButton());
    }

    buttons.add(removeButton = getRemoveButton());
    removeButton.addStyleName("rightTenMargin");
    buttons.add(editButton = getEdit());
    buttons.add(getAddItems());
    return buttons;
  }

  protected T getCurrentSelection() {
    return myLists.getCurrentSelection();
  }

  protected void addImportButton(DivWidget buttons) {
    buttons.add(getImport());
  }

  protected boolean canMakeQuiz() {
    return false;
  }

  protected int getItemID(ButtonMemoryItemContainer<T> container) {
    if (container == null) return -1;
    else {
      T currentSelection = getCurrentSelection(container);
      return currentSelection == null ? -1 : currentSelection.getID();
    }
  }

  protected Button getAddQuizButton() {
    return null;
  }

  private IsWidget getImport() {
    Button successButton = getSuccessButton(IMPORT);
    successButton.setIcon(IconType.UPLOAD);
    successButton.addClickHandler(event -> doImport());
    return successButton;
  }

  private void doImport() {
    doImport(getCurrentSelection(myLists));
  }

  private void doImport(T currentSelection) {
    ImportBulk importBulk = new ImportBulk();
    DivWidget contents = importBulk.showImportItem(controller.getLanguageInfo().toDisplay());

//    int numItems = currentSelection.getNumItems();
//    logger.info("editList : on " + currentSelection.getName() + " now " + numItems);

    DialogHelper dialogHelper = new DialogHelper(false);
    Button closeButton = dialogHelper.show(
        "Import Bulk from Text",
        Collections.emptyList(),
        contents,
        "Import",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            return gotYesOnImport(currentSelection, importBulk);
          }

          @Override
          public void gotNo() {
          }

          @Override
          public void gotHidden() {

          }
        }, 550);

    closeButton.setType(ButtonType.SUCCESS);
  }

  protected boolean gotYesOnImport(T currentSelection, ImportBulk importBulk) {
    return false;
  }

  /**
   * @return
   * @see #getButtons
   */
  @NotNull
  private Button getAddButton() {
    final Button add = new Button("", IconType.PLUS);
    add.addClickHandler(event -> dialogHelper = doAdd());
    add.setType(ButtonType.SUCCESS);
    addTooltip(add, MAKE_A_NEW_LIST + getItemName());
    return add;
  }

  protected abstract String getItemName();

  protected void addTooltip(Widget add, String tip) {
    new TooltipHelper().addTopTooltip(add, tip);
  }

  protected T getCurrentSelection(ButtonMemoryItemContainer<T> container) {
    return container.getCurrentSelection();
  }

  protected void removeFromLists(ButtonMemoryItemContainer<T> listContainer, T currentSelection) {
    if (listContainer == myLists) {
      String name = currentSelection.getName();
      names.remove(name);
    }

    int index = listContainer.getIndex(currentSelection);
    int origIndex = index;
    //logger.info("deleteList ---> did do deleteList " + uniqueID + " index " + index);
    listContainer.forgetItem(currentSelection);
    int numItems = listContainer.getNumItems();
    if (numItems == 0) {
      //delete.setEnabled(false);
      listContainer.disableAll();
    } else {
      if (index == numItems) {
        index = numItems - 1;
      }
      HasID at = listContainer.getAt(index);

      int id = at.getID();

      logger.info("removeFromLists " +
          "\n\torig index    " + origIndex +
          "\n\tcurrent index " + index +
          "\n\tnext is       " + at.getID());

//      listContainer.markCurrentExercise(id);
      //  Scheduler.get().scheduleDeferred(() -> {
      listContainer.markCurrentExercise(id);
      //  listContainer.redraw();
      //});
    }
  }

  /**
   * TODO : fill in creation dialog
   *
   * @return
   * @see #getAddButton
   */
  private DialogHelper doAdd() {
    DivWidget contents = new DivWidget();
    CreateDialog<T> createDialog = getCreateDialog();
    createDialog.doCreate(contents);
    return getNewListButton(contents, createDialog, CREATE_NEW_LIST + getItemName());
  }

  protected void gotDeleteResponse(Boolean result, T currentSelection, int uniqueID) {
    if (result) {
      removeFromLists(myLists, currentSelection);
    } else {
      logger.warning("doDelete ---> did not do delete of " + uniqueID);
    }
  }

  @NotNull
  protected abstract CreateDialog<T> getCreateDialog();

  @NotNull
  protected abstract CreateDialog<T> getEditDialog();

  @NotNull
  protected DialogHelper getNewListButton(DivWidget contents, CreateDialog createListDialog, String title) {
    DialogHelper dialogHelper = new DialogHelper(true);
    createListDialog.setDialogHelper(dialogHelper);
    Button closeButton = dialogHelper.show(
        title,
        Collections.emptyList(),
        contents,
        ADD,
        CANCEL,
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            return createListDialog.isOKToCreate();
          }

          @Override
          public void gotNo() {
          }

          @Override
          public void gotHidden() {

          }
        }, 580);

    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setIcon(IconType.PLUS);
    return dialogHelper;
  }

  /**
   * @see #getCommonButtonContainer
   * @return
   */
  private Button getEdit() {
    Button successButton = getSuccessButton(EDIT_TITLE);
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> doEdit());
    addTooltip(successButton, EDIT_THE_LIST + getSuffix());
    return successButton;
  }

  protected abstract String getName();

  /**
   * @return
   * @see #getButtons
   */
  private IsWidget getAddItems() {
    Button successButton = getSuccessButton(getEditItemButtonTitle());
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> editList(getCurrentSelection()));
    addTooltip(successButton, getEditItemTooltipPrefix() + getSuffix());
    return successButton;
  }

  @NotNull
  protected String getEditItemTooltipPrefix() {
    return EDIT_THE_ITEMS_ON_LIST;
  }

  @NotNull
  protected String getEditItemButtonTitle() {
    return ITEMS;
  }

  @NotNull
  protected String getSuffix() {
    return " " + getName();
  }

  /**
   * @return
   * @see #getButtons
   */
  protected Button getShare() {
    Button successButton = getSuccessButton(SHARE);
    successButton.setIcon(IconType.SHARE);
    addTooltip(successButton, SHARE_THE_LIST + getSuffix());
    return successButton;
  }

  /**
   * @param selectedItem
   * @see DialogEditorView#editList(IDialog)
   */
  protected abstract void editList(T selectedItem);

  @NotNull
  protected Button getSuccessButton(String learn1) {
    Button learn = new Button(learn1);
    learn.setType(ButtonType.SUCCESS);
    learn.addStyleName("leftFiveMargin");
    return learn;
  }

  @NotNull
  private Button getRemoveButton() {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDelete(add, getCurrentSelectionFromMyLists()));
    add.setType(ButtonType.DANGER);
    addTooltip(add, DELETE_LIST);
    myLists.addButton(add);
    return add;
  }

  protected T getCurrentSelectionFromMyLists() {
    return getCurrentSelection(myLists);
  }

  private void gotDelete(Button delete, T currentSelection) {
    if (currentSelection != null) {
      warnFirst(delete, currentSelection);
    }
  }

  private void warnFirst(Button delete, T currentSelection) {
    new DialogHelper(true).show(
        "Delete " + currentSelection.getName() + " forever?",
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
  }

  /**
   * @param delete
   * @param currentSelection
   * @see #warnFirst(Button, INameable)
   */
  protected abstract void doDelete(UIObject delete, T currentSelection);

  /**
   * @return
   * @see #getEdit
   */
  CreateDialog<T> doEdit() {
    T currentSelection = myLists.getCurrentSelection();
    if (currentSelection == null) {
      return null;
    } else {
      DivWidget contents = new DivWidget();
      CreateDialog<T> editDialog = getEditDialog();
      editDialog.doCreate(contents);

      logger.info("doEdit current selection " + currentSelection);
      DialogHelper dialogHelper = new DialogHelper(true);
      Button closeButton = dialogHelper.show(
          EDIT,
          Collections.emptyList(),
          contents,
          SAVE,
          CANCEL,
          new DialogHelper.CloseListener() {
            @Override
            public boolean gotYes() {
              boolean okToCreate = editDialog.isValidName();
              if (okToCreate) {
                editDialog.doEdit(currentSelection, myLists);

                myLists.flush();
                myLists.redraw();

                afterGotYesOnEdit();
              }
              return okToCreate;
            }

            @Override
            public void gotNo() {
            }

            @Override
            public void gotHidden() {
            }
          }, 550);

      closeButton.setType(ButtonType.SUCCESS);

      return editDialog;
    }
  }

  protected void afterGotYesOnEdit() {
  }

  protected void setShareHREF(T user) {
    if (user != null) {
      setShareButtonHREF();
    }
  }

  private void setShareButtonHREF() {
    share.setHref(getMailTo());
  }

  protected abstract String getMailTo();

  /**
   * @param userList
   * @see CreateListDialog#addUserList
   */
  @Override
  public void madeIt(T userList) {
    logger.info("madeIt made it " + userList.getName());
    try {
      dialogHelper.hide();
      myLists.addItemAfter(null, userList);
      myLists.enableAll();
      names.add(userList.getName());
      editButton.setEnabled(true);
    } catch (Exception e) {
      logger.warning("got " + e);
    }

/*    Scheduler.get().scheduleDeferred(() -> {
      logger.info("madeIt markCurrentExercise " + userList.getName());

      myLists.markCurrentExercise(userList.getID());
    });*/
  }

  public ButtonMemoryItemContainer<T> getMyLists() {
    return myLists;
  }

  /**
   * @param myLists
   * @see DialogEditorView#showYours(Collection, DivWidget)
   */
  protected void setMyLists(ButtonMemoryItemContainer<T> myLists) {
    this.myLists = myLists;
  }
}
