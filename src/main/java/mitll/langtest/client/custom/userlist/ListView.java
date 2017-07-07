package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.dialog.CreateListComplete;
import mitll.langtest.client.custom.dialog.CreateListDialog;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
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
  private final Logger logger = Logger.getLogger("ListView");

  private final ExerciseController controller;
  private ListContainer myLists;

  public ListView(ExerciseController controller) {
    this.controller = controller;
  }


  public void showContent(Panel listContent, String instanceName) {

    SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");

    com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(animated);

    listContent.clear();
    //listContent.add(waitCursor);

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

      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        Panel tableWithPager = (myLists = new ListContainer(controller, 20, true,"myLists")).getTableWithPager(result);
        result.forEach(list -> {
          if (list.getUserID() == controller.getUser()) {
            names.add(list.getName());
          }
        });
        addPagerAndHeader(tableWithPager, "Your Lists", left);
        left.add(getButtons(myLists));
      }
    });

    listService.getListsForUser(false, true, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        ListContainer listContainer = new ListContainer(controller, 10, false,"visited");
        Panel tableWithPager = listContainer.getTableWithPager(result);
        addPagerAndHeader(tableWithPager, "Visited", top);
        top.add(getLDButtons(listContainer));
      }
    });


    listService.getLists(new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        ListContainer listContainer = new ListContainer(controller, 10, false,"others");
        Panel tableWithPager = listContainer.getTableWithPager(result);
        addPagerAndHeader(tableWithPager, "Other's Lists", bottom);
        bottom.add(getLDButtons(listContainer));
      }
    });

  }

  private void addPagerAndHeader(Panel tableWithPager, String visited, DivWidget top) {
    top.add(new Heading(5, visited));
    top.add(tableWithPager);
    tableWithPager.getElement().getStyle().setClear(Style.Clear.BOTH);
    tableWithPager.setWidth("100%");
  }

  private Set<String> names = new HashSet<>();

  @NotNull
  private DivWidget getButtons(ListContainer container) {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("topFiveMargin");
    buttons.add(getAddButton());
    buttons.add(getRemoveButton());

    addDrillAndLearn(buttons, container);

    return buttons;
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
    String learn1 = "Learn";
    Button learn = getSuccessButton(learn1);
    learn.addClickHandler(event -> controller.showLearnList(getCurrentSelection(container).getID()));
    return learn;
  }

  @NotNull
  private Button getDrillButton(ListContainer container) {
    Button drill = getSuccessButton("Drill");
    drill.addClickHandler(event -> controller.showDrillList(getCurrentSelection(container).getID()));
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

  @NotNull
  private Button getAddButton() {
    final Button add = new Button("", IconType.PLUS);
    add.addClickHandler(event -> dialogHelper = doAdd(add));
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  @NotNull
  private Button getRemoveButton() {
    final Button add = new Button("", IconType.MINUS);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDelete(add, getCurrentSelection(myLists)));
    add.setType(ButtonType.DANGER);
    return add;
  }

  private UserList<CommonShell> getCurrentSelection(ListContainer container) {
    return container.getCurrentSelection();
  }

  private void gotDelete(Button delete, UserList<CommonShell> currentSelection) {
    final int uniqueID = currentSelection.getID();
    if (currentSelection.getUserID() == controller.getUser()) {
      controller.logEvent(delete, "Button", currentSelection.getName(), "Delete");

      if (currentSelection.isFavorite()) {
        Window.alert("Can't delete your favorites list.");
      } else {
        controller.getListService().deleteList(uniqueID, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("delete list call failed?");
          }

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              removeFromLists(currentSelection);
            } else {
              logger.warning("deleteList ---> did not do deleteList " + uniqueID);
            }
          }
        });
      }
    } else {
/*      controller.getListService().removeVisitor(uniqueID, controller.getUser(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(Void result) {
          removeFromLists(currentSelection);
        }
      });*/
    }
  }

  private void removeFromLists(UserList<CommonShell> currentSelection) {
    int index = myLists.getIndex(currentSelection);
    //logger.info("deleteList ---> did do deleteList " + uniqueID + " index " + index);
    myLists.forgetItem(currentSelection);
    UserList<CommonShell> at = myLists.getAt(index);
    //logger.info("next is " + at.getName());
    myLists.markCurrentExercise(at.getID());
    names.remove(currentSelection.getName());
  }

  private DialogHelper doAdd(Button add) {
    DivWidget contents = new DivWidget();
    CreateListDialog createListDialog = new CreateListDialog(this, controller);
    createListDialog.doCreate(contents);
    DialogHelper dialogHelper = new DialogHelper(true);
    Button closeButton = dialogHelper.show(
        "Create New List",
        Collections.emptyList(),
        contents,
        "Add",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            logger.info("\n\ngotYes");

            boolean okToCreate = createListDialog.isOKToCreate(names);
            //    add.setEnabled(okToCreate);
            logger.info("\n\ngotYes = okToCreate " + okToCreate);
            if (okToCreate) {
              createListDialog.doCreate();
            }

            return okToCreate;
          }

          @Override
          public void gotNo() {
            //   add.setEnabled(true);
            logger.info("\n\ngotNo");

          }
        }, 550);
    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setIcon(IconType.PLUS);

    return dialogHelper;
  }

  /**
   * @param userList
   * @see CreateListDialog#addUserList
   */

  @Override
  public void madeIt(UserList userList) {
    dialogHelper.hide();
    logger.info("\n\n\ngot made list");
    myLists.addExerciseAfter(null, userList);
    myLists.markCurrentExercise(userList.getID());
    names.add(userList.getName());
    //   myLists.redraw();
  }
}
