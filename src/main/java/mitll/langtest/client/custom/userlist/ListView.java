package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.dialog.CreateListComplete;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
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
  private static final String ITEMS = "Items";
  private final Logger logger = Logger.getLogger("ListView");

  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click on a list to learn it.";
  private static final String YOUR_LISTS = "Your Lists";
  private static final String LEARN = "Learn";
  private static final String DRILL = "Drill";
  private static final String STORAGE_ID = "others";
  /**
   * @see #showContent
   */
  private static final String OTHERS_PUBLIC_LISTS = "Public Lists";

  private static final int HEADING_SIZE = 3;
  private static final int VISITED_PAGE_SIZE = 5;
  private static final int VISITED_SHORT_SIZE = 5;

  private static final int BROWSE_PAGE_SIZE = 7;
  private static final int BROWSE_SHORT_PAGE_SIZE = 6;

  private static final String CREATE_NEW_LIST = "Create New List";
  private static final String EDIT = "Edit";
  private static final String ADD_EDIT_ITEMS = "Add/Edit Items";

  private static final int MY_LIST_HEIGHT = 560;
  private static final int browseBigger = 30;
  private static final int VISITED_HEIGHT = (MY_LIST_HEIGHT / 2) - 35 - browseBigger;
  private static final int BROWSE_HEIGHT = (MY_LIST_HEIGHT / 2) - 30 + browseBigger;

  private final ExerciseController controller;
  private ListContainer myLists;

  public ListView(ExerciseController controller) {
    this.controller = controller;
  }

  public void showContent(Panel listContent, String instanceName) {
    listContent.clear();
    DivWidget leftRight = new DivWidget();
    leftRight.addStyleName("inlineFlex");
    listContent.add(leftRight);

    DivWidget left = new DivWidget();
    leftRight.add(left);
    left.addStyleName("rightFiveMargin");
    left.addStyleName("cardBorderShadow");
    DivWidget right = new DivWidget();
    leftRight.add(right);

    right.setWidth("100%");
    DivWidget top = new DivWidget();
    right.add(top);
    top.addStyleName("leftTenMargin");
    top.addStyleName("bottomFiveMargin");
    top.addStyleName("cardBorderShadow");

    DivWidget bottom = new DivWidget();
    right.add(bottom);

    bottom.addStyleName("cardBorderShadow");
    bottom.addStyleName("leftTenMargin");

    ListServiceAsync listService = controller.getListService();
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists created by user", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        ListContainer myLists = new MyListContainer();
        Panel tableWithPager = (ListView.this.myLists = myLists).getTableWithPager(result);
        result.forEach(list -> {
          if (list.getUserID() == controller.getUser()) {
            names.add(list.getName());
          }
        });

        new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.RIGHT);

        addPagerAndHeader(tableWithPager, YOUR_LISTS, left);
        tableWithPager.setHeight(MY_LIST_HEIGHT + "px");

        left.add(getButtons(ListView.this.myLists));
      }
    });

    listService.getListsForUser(false, true, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists visited by user", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        ListContainer listContainer =
            new ListContainer(controller, VISITED_PAGE_SIZE, false, "visited", VISITED_SHORT_SIZE) {
              @Override
              protected void gotDoubleClickOn(UserList<CommonShell> selected) {
                showLearnList(this);
              }
            };
        Panel tableWithPager = listContainer.getTableWithPager(result);
        addPagerAndHeader(tableWithPager, "Visited", top);

        new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.LEFT);
        tableWithPager.setHeight(VISITED_HEIGHT + "px");

        DivWidget ldButtons = new DivWidget();
        {
          ldButtons.addStyleName("topFiveMargin");
          ldButtons.add(getRemoveVisitorButton(listContainer));
          addDrillAndLearn(ldButtons, listContainer);
        }
        top.add(ldButtons);
      }
    });


    listService.getLists(new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting all lists", caught);
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        ListContainer listContainer =
            new ListContainer(controller, BROWSE_PAGE_SIZE, false, STORAGE_ID, BROWSE_SHORT_PAGE_SIZE) {
              @Override
              protected void gotDoubleClickOn(UserList<CommonShell> selected) {
                showLearnList(this);
              }
            };
        Panel tableWithPager = listContainer.getTableWithPager(result);
        addPagerAndHeader(tableWithPager, OTHERS_PUBLIC_LISTS, bottom);
        tableWithPager.setHeight(BROWSE_HEIGHT + "px");
        tableWithPager.getElement().getStyle().setProperty("minWidth","700px");

        new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.LEFT);


        bottom.add(getLDButtons(listContainer));
      }
    });
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

  private final Set<String> names = new HashSet<>();

  @NotNull
  private DivWidget getButtons(ListContainer container) {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.add(getAddButton());
    buttons.add(getRemoveButton());
    buttons.add(getShare());

    buttons.add(getEdit());
    // buttons.add(getImport());
    buttons.add(getAddItems());
    addDrillAndLearn(buttons, container);

    return buttons;
  }

  private Button getEdit() {
    Button successButton = getSuccessButton("Title");
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> doEdit());
    addTooltip(successButton, "Edit the list title or make it public.");
    successButton.setEnabled(!myLists.isEmpty());
    return successButton;
  }

  private Button getShare() {
    Button successButton = getSuccessButton("Share");
    successButton.setIcon(IconType.SHARE);
    successButton.addClickHandler(event -> doShare());
    addTooltip(successButton, "Share the list with someone.");
    successButton.setEnabled(!myLists.isEmpty());
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

/*  private IsWidget getImport() {
    Button successButton = getSuccessButton("");
    successButton.setIcon(IconType.UPLOAD);
    successButton.addClickHandler(event -> doImport());
    return successButton;
  }*/

/*  private void doImport() {
    ImportBulk importBulk = new ImportBulk();
    UserList<CommonShell> currentSelection = getCurrentSelection(myLists);
    DivWidget contents = importBulk.showImportItem(controller);


    DialogHelper dialogHelper = new DialogHelper(false);
    Button closeButton = dialogHelper.show(
        "Import Bulk",
        Collections.emptyList(),
        contents,
        "Import",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            if (currentSelection.isFavorite()) {
              Window.alert("Can't import into favorites...");
              return false;
            } else {
              importBulk.doBulk(controller, currentSelection);
              return true;
            }
          }

          @Override
          public void gotNo() {
          }
        }, 550);

    closeButton.setType(ButtonType.SUCCESS);
    //  closeButton.setIcon(IconType.PLUS);
  }*/

  private IsWidget getAddItems() {
    Button successButton = getSuccessButton(ITEMS);
    successButton.setIcon(IconType.PENCIL);
    successButton.addClickHandler(event -> editList());
    addTooltip(successButton, "Edit the items on list.");
    successButton.setEnabled(!myLists.isEmpty());
    return successButton;
  }

  private void editList() {
    Button closeButton = new DialogHelper(true).show(
        ADD_EDIT_ITEMS,
        Collections.emptyList(),
        new EditItem(controller).editItem(getCurrentSelection(myLists)),
        "OK",
        null,
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            myLists.redraw();
            return true;
          }

          @Override
          public void gotNo() {

          }
        }, 660, true);

    closeButton.setType(ButtonType.SUCCESS);
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
    addTooltip(learn, "Learn the list.");
    learn.setEnabled(!container.isEmpty());
    container.addButton(learn);
    return learn;
  }

  private void showLearnList(ListContainer container) {
    //if (!container.isEmpty()) {
    controller.showLearnList(getCurrentSelection(container).getID());
    //}
  }

  @NotNull
  private Button getDrillButton(ListContainer container) {
    Button drill = getSuccessButton(DRILL);
    drill.setType(ButtonType.INFO);

    drill.addClickHandler(event -> {
      //   if (!container.isEmpty()) {
      controller.showDrillList(getCurrentSelection(container).getID());
      // }
    });
    addTooltip(drill, "Drill the list.");
    drill.setEnabled(!container.isEmpty());
    container.addButton(drill);

    return drill;
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
   * @see #getButtons(ListContainer)
   */
  @NotNull
  private Button getAddButton() {
    final Button add = new Button("", IconType.PLUS);
    add.addClickHandler(event -> dialogHelper = doAdd());
    add.setType(ButtonType.SUCCESS);
    addTooltip(add, "Make a new list.");
    return add;
  }

  @NotNull
  private Button getRemoveButton() {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDelete(add, getCurrentSelection(myLists)));
    add.setType(ButtonType.DANGER);
    addTooltip(add, "Delete list.");
    add.setEnabled(!myLists.isEmpty());
    myLists.addButton(add);
    return add;
  }

  @NotNull
  private Button getRemoveVisitorButton(ListContainer visited) {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDeleteVisitor(add, getCurrentSelection(visited), visited));
    add.setType(ButtonType.DANGER);
    add.setEnabled(!visited.isEmpty());
    addTooltip(add, "Forget visited list.");
    visited.addButton(add);
    return add;
  }

  private void addTooltip(Widget add, String tip) {
    new TooltipHelper().addTooltip(add, tip);
  }

  private UserList<CommonShell> getCurrentSelection(ListContainer container) {
    return container.getCurrentSelection();
  }

  private void gotDelete(Button delete, UserList<CommonShell> currentSelection) {
    if (currentSelection != null) {
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
              removeFromLists(delete, myLists, currentSelection);
            } else {
              logger.warning("deleteList ---> did not do deleteList " + uniqueID);
            }
          }
        });
      }
    }
  }

  private void gotDeleteVisitor(Button delete, UserList<CommonShell> currentSelection, ListContainer container) {
    final int uniqueID = currentSelection.getID();

    delete.setEnabled(false);
    controller.getListService().removeVisitor(uniqueID, controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        delete.setEnabled(true);
        controller.handleNonFatalError("removing a visitor to a list", caught);
      }

      @Override
      public void onSuccess(Void result) {
        delete.setEnabled(true);
        removeFromLists(delete, container, currentSelection);
      }
    });
  }

  private void removeFromLists(Button delete, ListContainer listContainer, UserList<CommonShell> currentSelection) {
    if (listContainer == myLists) {
      String name = currentSelection.getName();
      names.remove(name);
    }

    int index = listContainer.getIndex(currentSelection);
    //logger.info("deleteList ---> did do deleteList " + uniqueID + " index " + index);
    listContainer.forgetItem(currentSelection);
    int numItems = listContainer.getNumItems();
    if (numItems == 0) {
      delete.setEnabled(false);
      listContainer.disableAll();
    } else {
      if (index == numItems) index = numItems - 1;
      UserList<CommonShell> at = listContainer.getAt(index);
      //logger.info("next is " + at.getName());
      listContainer.markCurrentExercise(at.getID());
    }
  }

  /**
   * @return
   * @see #getAddButton
   */
  private DialogHelper doAdd() {
    DivWidget contents = new DivWidget();
    CreateListDialog createListDialog = new CreateListDialog(this, controller);
    createListDialog.doCreate(contents);


    DialogHelper dialogHelper = new DialogHelper(true);
    Button closeButton = dialogHelper.show(
        CREATE_NEW_LIST,
        Collections.emptyList(),
        contents,
        "Add",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            boolean okToCreate = createListDialog.isOKToCreate(names);
            if (okToCreate) {
              createListDialog.doCreate();

            }
            return okToCreate;
          }

          @Override
          public void gotNo() {
          }
        }, 550);

    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setIcon(IconType.PLUS);

    return dialogHelper;
  }

  private CreateListDialog editDialog;

  private void doEdit() {
    DivWidget contents = new DivWidget();
    editDialog = new CreateListDialog(this, controller, myLists.getCurrentSelection());
    editDialog.doCreate(contents);

    DialogHelper dialogHelper = new DialogHelper(true);
    Button closeButton = dialogHelper.show(
        EDIT,
        Collections.emptyList(),
        contents,
        "Edit",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            boolean okToCreate = editDialog.isValidName();
            if (okToCreate) {
              editDialog.doEdit(myLists.getCurrentSelection(), myLists);
            }
            return okToCreate;
          }

          @Override
          public void gotNo() {
          }
        }, 550);

    closeButton.setType(ButtonType.SUCCESS);
  }

  private void doShare() {
    UserList<CommonShell> currentSelection = myLists.getCurrentSelection();
    String mailToList = new UserListSupport(controller).getMailToList(currentSelection);

    DivWidget contents = new DivWidget();
    String name = currentSelection.getName();
    logger.info("name " + name);
    NavLink w = new NavLink("Click here to share " + name + ".", mailToList);
    w.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);

    contents.add(w);

    DialogHelper dialogHelper = new DialogHelper(false);
    Button closeButton = dialogHelper.show(
        "Share List",
        Collections.emptyList(),
        contents,
        "OK",
        "Cancel",
        null
        , 250);

    closeButton.setType(ButtonType.SUCCESS);
  }

  /**
   * @param userList
   * @see CreateListDialog#addUserList
   */

  @Override
  public void madeIt(UserList userList) {
    dialogHelper.hide();
    //logger.info("\n\n\ngot made list");
    myLists.addExerciseAfter(null, userList);
    myLists.markCurrentExercise(userList.getID());
    myLists.enableAll();
    names.add(userList.getName());
  }

  @Override
  public void gotEdit() {
    //  logger.info("\n\n\ngot edit");
    editDialog.doEdit(myLists.getCurrentSelection(), myLists);
  }

  private class MyListContainer extends ListContainer {
    public MyListContainer() {
      super(ListView.this.controller, 20, true, "myLists", 15);
    }

    @Override
    protected void gotDoubleClickOn(UserList<CommonShell> selected) {
      showLearnList(this);
    }
  }
}
