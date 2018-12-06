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
 * © 2015-2018 Massachusetts Institute of Technology.
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

package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.dialog.CreateListComplete;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by go22670 on 7/3/17.
 */
public class ListView implements ContentView, CreateListComplete {
  private final Logger logger = Logger.getLogger("ListView");

  private static final String DO_QUIZ = "Do quiz";
  private static final String DELETE_LIST = "Delete list.";

  private static final String MAKE_A_NEW_LIST = "Make a new list.";
  private static final String CAN_T_IMPORT_INTO_FAVORITES = "Can't import into favorites...";

  private static final String PRACTICE_THE_LIST = "Practice the list.";
  private static final int MAX_HEIGHT = 710;
  /**
   * @see #getImport
   */
  private static final String IMPORT = "Import";
  /**
   * With quiz button
   */
  private static final int MIN_WIDTH = 659;//599;

  private static final String EDIT_THE_ITEMS_ON_LIST = "Edit the items on list.";
  private static final String MY_LISTS = "myLists";

  /**
   * @see #getEdit
   */
  private static final String EDIT_THE_LIST = "Edit the list, make it public.";
  /**
   *
   */
  private static final String EDIT_THE_LIST_OR_QUIZ = "Edit the list, make it public, or make it a quiz.";
  private static final String SHARE = "Share";
  private static final String SHARE_THE_LIST = "Share the list with someone.";
  private static final String VISITED = "Visited";
  private static final String SAVE = "Save";


  /**
   * @see #getAddItems
   */
  private static final String ITEMS = "Items";
  private static final String ADD = "Add";
  private static final String CANCEL = "Cancel";
  private static final String LEARN_THE_LIST = "Learn the list.";
  private static final String EDIT_TITLE = "";

  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to view a list or quiz";

  /**
   *
   */
  private static final String YOUR_LISTS1 = "Your Lists";

  /**
   * @see ContentView#showContent
   */
  private static final String YOUR_LISTS = "Your Lists and Quizzes";

  private static final String LEARN = INavigation.VIEWS.LEARN.toString();
  private static final String DRILL = INavigation.VIEWS.PRACTICE.toString();
  private static final String STORAGE_ID = "others";
  /**
   * @see #addPublicTable
   */
  private static final String OTHERS_PUBLIC_LISTS = "Public Lists";

  private static final int HEADING_SIZE = 3;
  private static final int VISITED_PAGE_SIZE = 5;
  private static final int VISITED_SHORT_SIZE = 5;

  private static final int BROWSE_PAGE_SIZE = 6;// 7;
  private static final int BROWSE_SHORT_PAGE_SIZE = 6;

  private static final String CREATE_NEW_LIST = "Create New List";
  private static final String EDIT1 = "Edit";
  private static final String EDIT = EDIT1;

  /**
   *
   */
  private static final String ADD_EDIT_ITEMS = "Add/Edit Items";

  private static final int MY_LIST_HEIGHT = 560;//530;//560;
  private static final int browseBigger = 30;
  private static final int MARGIN_FOR_BUTTON = 40;
  private static final int VISITED_HEIGHT = (MY_LIST_HEIGHT / 2) - MARGIN_FOR_BUTTON - browseBigger;
  private static final int BROWSE_HEIGHT = (MY_LIST_HEIGHT / 2) - MARGIN_FOR_BUTTON + browseBigger;

  private final ExerciseController controller;
  private ListContainer myLists;
  private final Set<String> names = new HashSet<>();
  private Button quizButton, editButton, removeButton;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.NewContentChooser#NewContentChooser
   */
  public ListView(ExerciseController controller) {
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
    DivWidget right = new DivWidget();
    right.getElement().setId("right");

    leftRight.add(right);

    right.setWidth("100%");
    DivWidget top = new DivWidget();
    top.getElement().setId("top");
    right.add(top);

    styleTopAndBottom(top);

    top.addStyleName("bottomFiveMargin");

    DivWidget bottom = new DivWidget();
    right.add(bottom);
    bottom.getElement().setId("bottom");

    styleTopAndBottom(bottom);

    addYourLists(left);
    addVisited(top);
    addPublic(bottom);
  }

  private void addVisited(DivWidget top) {
    ListContainer listContainer = addVisitedTable(top);

    controller.getListService().getListsForUser(false, true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists visited by user", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        listContainer.populateTable(result);
      }
    });
  }

  private ListContainer addVisitedTable(DivWidget top) {
    ListContainer listContainer =
        new ListContainer(controller, VISITED_PAGE_SIZE, false, "visited", VISITED_SHORT_SIZE, false, false) {
          @Override
          protected boolean hasDoubleClick() {
            return true;
          }

          @Override
          protected void gotDoubleClickOn(UserList<CommonShell> selected) {
            showLearnList(this);
          }
        };
    Panel tableWithPager = listContainer.getTableWithPager(Collections.emptyList());
    addPagerAndHeader(tableWithPager, VISITED, top);
    tableWithPager.addStyleName("rightFiveMargin");

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.LEFT);
    tableWithPager.setHeight(VISITED_HEIGHT + "px");

    DivWidget ldButtons = new DivWidget();
    {
      ldButtons.addStyleName("topFiveMargin");
      ldButtons.add(getRemoveVisitorButton(listContainer));
      addDrillAndLearn(ldButtons, listContainer);
    }
    top.add(ldButtons);
    return listContainer;
  }

  private void addPublic(DivWidget bottom) {
    ListContainer listContainer = addPublicTable(bottom);
    controller.getListService().getLists(new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting all lists", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        listContainer.populateTable(result);
      }
    });
  }

  private ListContainer addPublicTable(DivWidget bottom) {
    ListContainer listContainer =
        new ListContainer(controller, BROWSE_PAGE_SIZE, false, STORAGE_ID, BROWSE_SHORT_PAGE_SIZE, false, true) {

          @Override
          protected boolean hasDoubleClick() {
            return true;
          }

          @Override
          protected void gotDoubleClickOn(UserList<CommonShell> selected) {
            if (selected.getListType() == UserList.LIST_TYPE.QUIZ) {
              showQuiz(this);
            } else {
              showLearnList(this);
            }
          }
        };
    Panel tableWithPager = listContainer.getTableWithPager(Collections.emptyList());
    addPagerAndHeader(tableWithPager, OTHERS_PUBLIC_LISTS, bottom);
    tableWithPager.setHeight(BROWSE_HEIGHT + "px");
    tableWithPager.getElement().getStyle().setProperty("minWidth", "700px");

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.LEFT);


    bottom.add(getLDButtons(listContainer));

    return listContainer;
  }

  private void styleTopAndBottom(DivWidget bottom) {
    bottom.addStyleName("leftTenMargin");
    bottom.addStyleName("rightFiveMargin");
    bottom.addStyleName("cardBorderShadow");
  }

  private void addYourLists(DivWidget left) {
    showYourLists(Collections.emptyList(), left);

    controller.getListService().getListsForUser(true, false, canMakeQuiz(), new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists created by user", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        myLists.populateTable(result);
        populateUniqueListNames(result);
        Scheduler.get().scheduleDeferred(() -> setShareHREF(getCurrentSelectionFromMyLists()));
      }
    });
  }

  /**
   * @param result
   * @param left
   * @see #addYourLists
   */
  private void showYourLists(Collection<UserList<CommonShell>> result, DivWidget left) {
    ListContainer myLists = new MyListContainer();
    Panel tableWithPager = (ListView.this.myLists = myLists).getTableWithPager(result);
    //   tableWithPager.setHeight("520px");
    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.RIGHT);
    addPagerAndHeader(tableWithPager, canMakeQuiz() ? YOUR_LISTS : YOUR_LISTS1, left);
    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    left.add(getButtons(ListView.this.myLists));
  }

  private void populateUniqueListNames(Collection<UserList<CommonShell>> result) {
    result.forEach(list -> {
      if (list.getUserID() == controller.getUser()) {
        names.add(list.getName());
      }
    });
  }

  private boolean canMakeQuiz() {
    Collection<User.Permission> permissions = controller.getPermissions();
    return permissions.contains(User.Permission.TEACHER_PERM) || permissions.contains(User.Permission.PROJECT_ADMIN);
  }

  private void addPagerAndHeader(Panel tableWithPager, String visited, DivWidget top) {
    Heading w = new Heading(HEADING_SIZE, visited);
    w.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    w.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    top.add(w);
    top.add(tableWithPager);
    tableWithPager.getElement().getStyle().setClear(Style.Clear.BOTH);
    tableWithPager.setWidth("100%");
  }

  private Button share;

  /**
   * @param container
   * @return
   */
  @NotNull
  private DivWidget getButtons(ListContainer container) {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.getElement().getStyle().setProperty("minWidth", MIN_WIDTH + "px");
    buttons.add(getAddButton());

    if (canMakeQuiz()) {
      buttons.add(getAddQuizButton());
    }

    buttons.add(removeButton = getRemoveButton());

    buttons.add(editButton = getEdit());
    buttons.add(getAddItems());
    buttons.add(getImport());
    buttons.add(share = getShare());
    addDrillAndLearn(buttons, container);
    if (canMakeQuiz()) {
      buttons.add(quizButton = getQuizButton(myLists));
    }
    return buttons;
  }

  private Button getEdit() {
    Button successButton = getSuccessButton(EDIT_TITLE);
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> doEdit());
    addTooltip(successButton, canMakeQuiz() ? EDIT_THE_LIST_OR_QUIZ : EDIT_THE_LIST);
    // successButton.setEnabled(!myLists.isEmpty());
    return successButton;
  }

  private Button getShare() {
    Button successButton = getSuccessButton(SHARE);
    successButton.setIcon(IconType.SHARE);
    addTooltip(successButton, SHARE_THE_LIST);
    //  successButton.setEnabled(!myLists.isEmpty());
    return successButton;
  }

/*
  private Button getPublic() {
    Button successButton = getSuccessButton("Make Public");
    successButton.setIcon(IconType.UNLOCK);
    successButton.addClickHandler(event -> doPublic());
    return successButton;
  }
*/

  private IsWidget getImport() {
    Button successButton = getSuccessButton(IMPORT);
    successButton.setIcon(IconType.UPLOAD);
    // successButton.setSize(ButtonSize.LARGE);
    successButton.addClickHandler(event -> doImport());
    return successButton;
  }

  private void doImport() {
    UserList<CommonShell> currentSelection = getCurrentSelection(myLists);
    doImport(currentSelection, currentSelection.isFavorite());
  }

  private void doImport(UserList<CommonShell> currentSelection, boolean favorite) {
    ImportBulk importBulk = new ImportBulk();
    DivWidget contents = importBulk.showImportItem(controller.getLanguage());

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
            if (favorite) {
              Window.alert(CAN_T_IMPORT_INTO_FAVORITES);
              return false;
            } else {
              importBulk.doBulk(controller, currentSelection, myLists);
              return true;
            }
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

  /**
   * @return
   * @see #getButtons
   */
  private IsWidget getAddItems() {
    Button successButton = getSuccessButton(ITEMS);
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> editList());
    addTooltip(successButton, EDIT_THE_ITEMS_ON_LIST);
    //  successButton.setEnabled(!myLists.isEmpty());
    return successButton;
  }

  private void editList() {
    UserList<CommonShell> currentSelectionFromMyLists = getCurrentSelectionFromMyLists();
    EditItem editItem = new EditItem(controller);
    //Button closeButton =
        new DialogHelper(true).show(
        ADD_EDIT_ITEMS + " : " + getListName(),
        Collections.emptyList(),
        editItem.editItem(currentSelectionFromMyLists),
        "Done",
        null,
        new MyShownCloseListener(editItem), MAX_HEIGHT, -1, true);

   /// closeButton.setType(ButtonType.SUCCESS);

    // Scheduler.get().scheduleDeferred(editItem::reload);
  }

  @NotNull
  private String getListName() {
    UserList<CommonShell> originalList = getCurrentSelectionFromMyLists();
    boolean hasDescrip = !originalList.getDescription().isEmpty();
    return originalList.getName() +
        (hasDescrip ? " (" + originalList.getDescription() + ")" : "");
  }

  @NotNull
  private DivWidget getLDButtons(ListContainer container) {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("topFiveMargin");
    addDrillAndLearn(buttons, container);
    return buttons;
  }

  private void addDrillAndLearn(DivWidget buttons, ListContainer container) {
    buttons.add(getLearnButton(container));
    buttons.add(getDrillButton(container));
  }

  @NotNull
  private Button getLearnButton(ListContainer container) {
    Button learn = getSuccessButton(LEARN);
    learn.setType(ButtonType.INFO);
    learn.addClickHandler(event -> showLearnList(container));
    addTooltip(learn, LEARN_THE_LIST);
    //learn.setEnabled(!container.isEmpty());
    container.addButton(learn);
    return learn;
  }

  private void showLearnList(ListContainer container) {
    controller.getNavigation().showListIn(getListID(container), INavigation.VIEWS.LEARN);
  }

  /**
   * @param container
   * @see ListView.MyListContainer#gotDoubleClickOn
   */
  private void showQuiz(ListContainer container) {
    controller.showListIn(getListID(container), INavigation.VIEWS.QUIZ);
  }

  @NotNull
  private Button getDrillButton(ListContainer container) {
    Button drill = getSuccessButton(DRILL);
    drill.setType(ButtonType.INFO);

    drill.addClickHandler(event -> controller.showListIn(getListID(container), INavigation.VIEWS.PRACTICE));
    addTooltip(drill, PRACTICE_THE_LIST);
    container.addButton(drill);

    return drill;
  }

  @NotNull
  private Button getQuizButton(ListContainer container) {
    Button drill = getSuccessButton("Quiz");
    drill.setType(ButtonType.INFO);

    drill.addClickHandler(event -> showQuiz(getCurrentSelection(container)));
    addTooltip(drill, DO_QUIZ);
    container.addButton(drill);

    enableQuizButton(drill);
    return drill;
  }

  private int getListID(ListContainer container) {
    if (container == null) return -1;
    else {
      UserList<CommonShell> currentSelection = getCurrentSelection(container);
      return currentSelection == null ? -1 : currentSelection.getID();
    }
  }

  @NotNull
  private Button getSuccessButton(String learn1) {
    Button learn = new Button(learn1);
    learn.setType(ButtonType.SUCCESS);
    learn.addStyleName("leftFiveMargin");
    return learn;
  }

  private DialogHelper dialogHelper;

  /**
   * @return
   * @see #getButtons
   */
  @NotNull
  private Button getAddButton() {
    final Button add = new Button("", IconType.PLUS);
    add.addClickHandler(event -> dialogHelper = doAdd());
    add.setType(ButtonType.SUCCESS);
    addTooltip(add, MAKE_A_NEW_LIST);
    return add;
  }

  @NotNull
  private Button getAddQuizButton() {
    final Button add = new Button("Quiz", IconType.PLUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> dialogHelper = doAddQuiz());
    add.setType(ButtonType.SUCCESS);
    addTooltip(add, "Make a new quiz.");
    return add;
  }

  @NotNull
  private Button getRemoveButton() {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDelete(add, getCurrentSelectionFromMyLists()));
    add.setType(ButtonType.DANGER);
    addTooltip(add, DELETE_LIST);
    // add.setEnabled(!myLists.isEmpty());
    myLists.addButton(add);
    return add;
  }

  private UserList<CommonShell> getCurrentSelectionFromMyLists() {
    return getCurrentSelection(myLists);
  }

  @NotNull
  private Button getRemoveVisitorButton(ListContainer visited) {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDeleteVisitor(add, getCurrentSelection(visited), visited));
    add.setType(ButtonType.DANGER);
    // add.setEnabled(!visited.isEmpty());
    addTooltip(add, "Forget visited list.");
    visited.addButton(add);
    return add;
  }

  private void addTooltip(Widget add, String tip) {
    new TooltipHelper().addTopTooltip(add, tip);
  }

  private UserList<CommonShell> getCurrentSelection(ListContainer container) {
    return container.getCurrentSelection();
  }

  private void gotDelete(Button delete, UserList<CommonShell> currentSelection) {
    if (currentSelection != null) {
      if (currentSelection.getListType() == UserList.LIST_TYPE.QUIZ) {
        warnFirst(delete, currentSelection);
      } else {
        doDelete(delete, currentSelection);
      }
    }
  }

  private void doDelete(UIObject delete, UserList<CommonShell> currentSelection) {
    final int uniqueID = currentSelection.getID();
    controller.logEvent(delete, "Button", currentSelection.getName(), "Delete");

    if (currentSelection.isFavorite()) {
      Window.alert("Can't delete your favorites list.");
    } else {
      controller.getListService().deleteList(uniqueID, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          //   logger.warning("delete list call failed?");
          controller.handleNonFatalError("deleting a list", caught);
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
  }

  private void warnFirst(Button delete, UserList<CommonShell> currentSelection) {
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

  private void gotDeleteVisitor(Button delete, UserList<CommonShell> currentSelection, ListContainer container) {
    if (currentSelection == null) return;

    delete.setEnabled(false);

    controller.getListService().removeVisitor(currentSelection.getID(), controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        delete.setEnabled(true);
        controller.handleNonFatalError("removing a visitor to a list", caught);
      }

      @Override
      public void onSuccess(Void result) {
        delete.setEnabled(true);
        removeFromLists(container, currentSelection);
      }
    });
  }

  private void removeFromLists(ListContainer listContainer, UserList<CommonShell> currentSelection) {
    if (listContainer == myLists) {
      String name = currentSelection.getName();
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
  }

  /**
   * @return
   * @see #getAddButton
   */
  private DialogHelper doAdd() {
    DivWidget contents = new DivWidget();
    CreateListDialog createListDialog = new CreateListDialog(this, controller, names);
    createListDialog.doCreate(contents);


    return getNewListButton(contents, createListDialog, CREATE_NEW_LIST);
  }


  /**
   * @return
   * @see #getAddButton
   */
  private DialogHelper doAddQuiz() {
    DivWidget contents = new DivWidget();
    CreateListDialog createListDialog = new CreateListDialog(this, controller, names).setIsQuiz(true);
    createListDialog.doCreate(contents);


    return getNewListButton(contents, createListDialog, "Create New Quiz");
  }

  @NotNull
  private DialogHelper getNewListButton(DivWidget contents, CreateListDialog createListDialog, String title) {
    DialogHelper dialogHelper = new DialogHelper(true);
    createListDialog.setDialogHelper(dialogHelper);
    //String createNewList = CREATE_NEW_LIST + (canMakeQuiz() ? " or Quiz" : "");
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

  private CreateListDialog editDialog;

  private void doEdit() {
    DivWidget contents = new DivWidget();
    editDialog = new CreateListDialog(this, controller, myLists.getCurrentSelection(), true, names);
    editDialog.doCreate(contents);

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
              editDialog.doEdit(myLists.getCurrentSelection(), myLists);


              myLists.flush();
              myLists.redraw();
              enableQuizButton(quizButton);
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
  }

  @NotNull
  private String getMailTo() {
    UserList<CommonShell> currentSelection = myLists.getCurrentSelection();
    boolean isQuiz = currentSelection.getListType() == UserList.LIST_TYPE.QUIZ;
    return new UserListSupport(controller)
        .getMailTo(currentSelection.getID(), currentSelection.getName(), isQuiz);
  }

  /**
   * @param userList
   * @see CreateListDialog#addUserList
   */

  @Override
  public void madeIt(UserList userList) {
    // logger.info("madeIt made it " + userList.getName());
    try {
      dialogHelper.hide();
      myLists.addExerciseAfter(null, userList);
      myLists.enableAll();
      names.add(userList.getName());
    } catch (Exception e) {
      logger.warning("got " + e);
    }
    Scheduler.get().scheduleDeferred(() -> myLists.markCurrentExercise(userList.getID()));
  }

  /**
   * @seex CreateListDialog#makeCreateButton
   */
  @Override
  public void gotEdit() {
    editDialog.doEdit(myLists.getCurrentSelection(), myLists);
  }

  private void enableQuizButton(Button quizButton) {
    UserList<CommonShell> currentSelection = getCurrentSelection(myLists);
    quizButton.setEnabled(currentSelection != null && currentSelection.getListType() == UserList.LIST_TYPE.QUIZ);

    if (currentSelection != null && editButton != null) {
      boolean favorite = currentSelection.isFavorite();
      editButton.setEnabled(!favorite);
      removeButton.setEnabled(!favorite);
    }
  }

  private class MyListContainer extends ListContainer {
    MyListContainer() {
      super(ListView.this.controller, 18, true, MY_LISTS, 15, true, false);
    }

    @Override
    public void gotClickOnItem(final UserList<CommonShell> user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
      enableQuizButton(quizButton);

    }


    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    @Override
    protected void gotDoubleClickOn(UserList<CommonShell> selected) {
      //    logger.info("gotDoubleClickOn got double click on " + selected);
      //showLearnOrQuiz(selected);
      editList();
    }

  }

  private void setShareHREF(UserList<CommonShell> user) {
    if (user != null) {
      setShareButtonHREF();
      // share.setEnabled(!user.isFavorite());
    }
  }


  private void showQuiz(UserList<CommonShell> selected) {
    if (selected != null) {
      if (selected.getListType() == UserList.LIST_TYPE.QUIZ) {
        showQuiz(myLists);
      }
    }
  }

  private void setShareButtonHREF() {
    share.setHref(getMailTo());
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
      logger.info("Got hidden ");
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
