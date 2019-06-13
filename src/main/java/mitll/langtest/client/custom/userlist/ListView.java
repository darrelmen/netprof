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

package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.dialog.ContentEditorView;
import mitll.langtest.client.custom.dialog.CreateDialog;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by go22670 on 7/3/17.
 */
public class ListView<T extends UserList<CommonShell>> extends ContentEditorView<T> {
  //private final Logger logger = Logger.getLogger("ListView");

  private static final String FORGET_VISITED_LIST = "Forget visited list.";

  private static final String QUIZ = "Quiz";
  private static final String DO_QUIZ = "Do quiz";

  private static final String CAN_T_IMPORT_INTO_FAVORITES = "Can't import into favorites...";

  private static final String PRACTICE_THE_LIST = "Practice the list.";
  private static final int MAX_HEIGHT = 710;

  private static final String MY_LISTS = "myLists";

  private static final String VISITED = "Visited";

  private static final String LEARN_THE_LIST = "Learn the list.";


  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to view a list or quiz";

  /**
   *
   */
  private static final String YOUR_LISTS1 = "Your Lists";

  /**
   * @see ContentView#showContent
   * @see #showYourLists(Collection, DivWidget)
   */
  private static final String YOUR_LISTS = "Your Lists and Quizzes";

  private static final String LEARN = INavigation.VIEWS.LEARN.toString();
  private static final String DRILL = INavigation.VIEWS.PRACTICE.toString();
  private static final String STORAGE_ID = "others";
  /**
   * @see #addPublicTable
   */
  private static final String OTHERS_PUBLIC_LISTS = "Public Lists";

  private static final int VISITED_PAGE_SIZE = 5;
  private static final int VISITED_SHORT_SIZE = 5;

  private static final int BROWSE_PAGE_SIZE = 6;// 7;
  private static final int BROWSE_SHORT_PAGE_SIZE = 6;

  /**
   * @see ContentEditorView#editList(mitll.langtest.shared.custom.INameable)
   */
  private static final String ADD_EDIT_ITEMS = "Add/Edit Items";

  private static final int MY_LIST_HEIGHT = 560;//530;//560;
  private static final int browseBigger = 30;
  private static final int MARGIN_FOR_BUTTON = 40;
  private static final int VISITED_HEIGHT = (MY_LIST_HEIGHT / 2) - MARGIN_FOR_BUTTON - browseBigger;
  private static final int BROWSE_HEIGHT = (MY_LIST_HEIGHT / 2) - MARGIN_FOR_BUTTON + browseBigger;

  private Button quizButton;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.NewContentChooser#NewContentChooser
   */
  public ListView(ExerciseController controller) {
    super(controller);
  }

  @Override
  protected String getItemName() {
    return "List";
  }

  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {

    super.showContent(listContent, instanceName);

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
    ListContainer<T> listContainer = addVisitedTable(top);

    controller.getListService().getListsForUser(false, true, false, new AsyncCallback<Collection<T>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists visited by user", caught);
      }

      @Override
      public void onSuccess(Collection<T> result) {
        listContainer.populateTable(result);
      }
    });
  }

  private ListContainer<T> addVisitedTable(DivWidget top) {
    ListContainer<T> listContainer =
        new ListContainer<T>(controller, VISITED_PAGE_SIZE, false, "visited", VISITED_SHORT_SIZE, false, false) {
          @Override
          protected boolean hasDoubleClick() {
            return true;
          }

          @Override
          protected void gotDoubleClickOn(T selected) {
            showLearnList(this);
          }
        };

    Panel tableWithPager = getTableWithPager(top, listContainer, VISITED, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.LEFT);

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
    ListContainer<T> listContainer = addPublicTable(bottom);
    controller.getListService().getLists(new AsyncCallback<Collection<T>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting all lists", caught);
      }

      @Override
      public void onSuccess(Collection<T> result) {
        listContainer.populateTable(result);
      }
    });
  }

  private ListContainer<T> addPublicTable(DivWidget bottom) {
    ListContainer<T> listContainer =
        new ListContainer<T>(controller, BROWSE_PAGE_SIZE, false, STORAGE_ID, BROWSE_SHORT_PAGE_SIZE, false, true) {

          @Override
          protected boolean hasDoubleClick() {
            return true;
          }

          @Override
          protected void gotDoubleClickOn(T selected) {
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

    controller.getListService().getListsForUser(true, false, canMakeQuiz(), new AsyncCallback<Collection<T>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists created by user", caught);
      }

      @Override
      public void onSuccess(Collection<T> result) {
        ButtonMemoryItemContainer<T> myLists = getMyLists();
        myLists.populateTable(result);
        populateUniqueListNames(result);
        setShareHREFLater();

        if (result.isEmpty()) myLists.disableAll();
      }
    });
  }

  /**
   * @param result
   * @param left
   * @see #addYourLists
   */
  private void showYourLists(Collection<T> result, DivWidget left) {
    ListContainer<T> myLists = new MyListContainer();
    setMyLists(myLists);
    Panel tableWithPager = myLists.getTableWithPager(result);

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.RIGHT);
    addPagerAndHeader(tableWithPager, canMakeQuiz() ? YOUR_LISTS : YOUR_LISTS1, left);
    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    tableWithPager.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    left.add(getButtons(getMyLists()));
  }

  protected void populateUniqueListNames(Collection<T> result) {
    result.forEach(list -> {
      if (list.getUserID() == controller.getUser()) {
        names.add(list.getName());
      }
    });
  }

  @Override
  protected boolean canMakeQuiz() {
    Collection<Permission> permissions = controller.getPermissions();
    return permissions.contains(Permission.TEACHER_PERM) || permissions.contains(Permission.PROJECT_ADMIN);
  }

  /**
   * @param container
   * @return
   * @see #showYourLists(Collection, DivWidget)
   */
  @NotNull
  @Override
  protected DivWidget getButtons(ButtonMemoryItemContainer<T> container) {
    DivWidget buttons = super.getButtons(container);
    addDrillAndLearn(buttons, container);
    if (canMakeQuiz()) {
      buttons.add(quizButton = getQuizButton(getMyLists()));
    }
    return buttons;
  }

/*
  private Button getPublic() {
    Button successButton = getSuccessButton("Make Public");
    successButton.setIcon(IconType.UNLOCK);
    successButton.addClickHandler(event -> doPublic());
    return successButton;
  }
*/

  @Override
  protected boolean gotYesOnImport(T currentSelection, ImportBulk importBulk) {
    boolean favorite = getCurrentSelection(getMyLists()).isFavorite();
    if (favorite) {
      Window.alert(CAN_T_IMPORT_INTO_FAVORITES);
      return false;
    } else {
      importBulk.doBulk(controller, currentSelection, getMyLists());
      return true;
    }
  }

  /**
   * @param selectedItem ignored here...?
   * @see
   * @see ListView.MyListContainer#gotDoubleClickOn
   * @see ContentEditorView#getAddItems
   */
  @Override
  protected void editList(T selectedItem) {
    EditItem editItem = new EditItem(controller);
    new DialogHelper(true).show(
        ADD_EDIT_ITEMS + " : " + getListName(),
        Collections.emptyList(),
        editItem.editItem(getCurrentSelectionFromMyLists()),
        "Done",
        null,
        new MyShownCloseListener(editItem), MAX_HEIGHT, -1, true);
  }

  @NotNull
  private String getListName() {
    UserList<CommonShell> originalList = getCurrentSelectionFromMyLists();
    boolean hasDescrip = !originalList.getDescription().isEmpty();
    return originalList.getName() +
        (hasDescrip ? " (" + originalList.getDescription() + ")" : "");
  }

  @NotNull
  private DivWidget getLDButtons(ListContainer<T> container) {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("topFiveMargin");
    addDrillAndLearn(buttons, container);
    return buttons;
  }

  private void addDrillAndLearn(DivWidget buttons, ButtonMemoryItemContainer<T> container) {
    buttons.add(getLearnButton(container));
    buttons.add(getDrillButton(container));
  }

  @NotNull
  private Button getLearnButton(ButtonMemoryItemContainer<T> container) {
    Button learn = buttonHelper.getSuccessButton(container, LEARN);
    learn.setType(ButtonType.INFO);
    learn.addClickHandler(event -> showLearnList(container));
    addTooltip(learn, LEARN_THE_LIST);
    return learn;
  }

  private void showLearnList(ButtonMemoryItemContainer<T> container) {
    controller.getNavigation().showListIn(getItemID(container), INavigation.VIEWS.LEARN);
  }

  /**
   * @param container
   * @see ListView.MyListContainer#gotDoubleClickOn
   */
  private void showQuiz(ButtonMemoryItemContainer<T> container) {
    controller.showListIn(getItemID(container), INavigation.VIEWS.QUIZ);
  }

  @NotNull
  private Button getDrillButton(ButtonMemoryItemContainer<T> container) {
    Button drill = buttonHelper.getSuccessButton(container, DRILL);
    drill.setType(ButtonType.INFO);

    drill.addClickHandler(event -> controller.showListIn(getItemID(container), INavigation.VIEWS.PRACTICE));
    addTooltip(drill, PRACTICE_THE_LIST);

    return drill;
  }

  @NotNull
  private Button getQuizButton(ButtonMemoryItemContainer<T> container) {
    Button drill = buttonHelper.getSuccessButton(container, QUIZ);
    drill.setType(ButtonType.INFO);

    drill.addClickHandler(event -> showQuiz(getCurrentSelection(container)));
    addTooltip(drill, DO_QUIZ);
    enableQuizButton(drill);
    return drill;
  }

  @Override
  @NotNull
  protected Button getAddQuizButton() {
    final Button add = new Button(QUIZ, IconType.PLUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> dialogHelper = doAddQuiz());
    add.setType(ButtonType.SUCCESS);
    addTooltip(add, "Make a new quiz.");
    return add;
  }

  @NotNull
  private Button getRemoveVisitorButton(ListContainer<T> visited) {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDeleteVisitor(add, getCurrentSelection(visited), visited));
    add.setType(ButtonType.DANGER);
    addTooltip(add, FORGET_VISITED_LIST);
    visited.addButton(add);
    return add;
  }

  @Override
  protected void doDelete(UIObject delete, T currentSelection) {
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
          gotDeleteResponse(result, currentSelection, uniqueID);
        }
      });
    }
  }

  private void gotDeleteVisitor(Button delete, T currentSelection, ListContainer container) {
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

  @NotNull
  protected CreateDialog<T> getCreateDialog() {
    return new CreateListDialog<T>(this, controller, names);
  }

  /**
   * @return
   * @see #getAddButton
   */
  private DialogHelper doAddQuiz() {
    DivWidget contents = new DivWidget();
    CreateListDialog<T> createListDialog = new CreateListDialog<>(this, controller, names).setIsQuiz(true);
    createListDialog.doCreate(contents);

    return getNewListButton(contents, createListDialog, "Create New Quiz");
  }

  @Override
  protected void afterGotYesOnEdit() {
    enableQuizButton(quizButton);
  }

  @NotNull
  @Override
  protected CreateDialog<T> getEditDialog() {
    return new CreateListDialog<T>(this, controller, getMyLists().getCurrentSelection(), true, names);
  }

  @Override
  protected String getName() {
    return "list";
  }

  /**
   * @seex CreateListDialog#makeCreateButton
   */
  private void enableQuizButton(Button quizButton) {
    UserList<CommonShell> currentSelection = getCurrentSelection(getMyLists());
    if (quizButton != null) {
      quizButton.setEnabled(currentSelection != null && currentSelection.getListType() == UserList.LIST_TYPE.QUIZ);
    }

    if (currentSelection != null && editButton != null) {
      boolean favorite = currentSelection.isFavorite();
      editButton.setEnabled(!favorite);
      removeButton.setEnabled(!favorite);
    }
  }

  private class MyListContainer extends ListContainer<T> {
    MyListContainer() {
      super(ListView.this.controller, 18, true, MY_LISTS, 15, true, false);
    }

    @Override
    public void gotClickOnItem(final T user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
      enableQuizButton(quizButton);
    }

    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    @Override
    protected void gotDoubleClickOn(T selected) {
      //    logger.info("gotDoubleClickOn got double click on " + selected);
      //showLearnOrQuiz(selected);
      editList(getCurrentSelection());
    }
  }

  @NotNull
  @Override
  protected String getMailTo() {
    UserList<CommonShell> currentSelection = getMyLists().getCurrentSelection();
    boolean isQuiz = currentSelection.getListType() == UserList.LIST_TYPE.QUIZ;
    return new UserListSupport(controller)
        .getMailToList(currentSelection.getID(), currentSelection.getName(), isQuiz);
  }

  private void showQuiz(T selected) {
    if (selected != null) {
      if (selected.getListType() == UserList.LIST_TYPE.QUIZ) {
        showQuiz(getMyLists());
      }
    }
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
