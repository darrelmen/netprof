package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
  private ListContainer container;

  public ListView(ExerciseController controller) {
    this.controller = controller;
  }


  public void showContent(Panel listContent, String instanceName) {

    SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");

    com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(animated);

    listContent.clear();
    listContent.add(waitCursor);
/*    controller.getListService().getNumLists(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Integer result) {
        populate(listContent, result);
      }
    });*/

    controller.getListService().getLists(new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        listContent.clear();
        Panel tableWithPager = (container = new ListContainer(controller)).getTableWithPager(result);
        result.stream().forEach(list -> {
          if (list.getUserID() == controller.getUser()) {
            names.add(list.getName());
          }
        });
        listContent.add(tableWithPager);
        tableWithPager.getElement().getStyle().setClear(Style.Clear.BOTH);
        tableWithPager.setWidth("100%");

        DivWidget buttons = getButtons();
        listContent.add(buttons);
      }
    });
  }

  private Set<String> names = new HashSet<>();

  @NotNull
  private DivWidget getButtons() {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("topFiveMargin");
    buttons.add(getAddButton());
    buttons.add(getRemoveButton());

    Button learn = new Button("Learn");
    learn.setType(ButtonType.SUCCESS);
    learn.addStyleName("leftFiveMargin");
    buttons.add(learn);
    learn.addClickHandler(event -> controller.showLearnList(getCurrentSelection().getID()));

    Button drill = new Button("Drill");
    drill.setType(ButtonType.SUCCESS);
    buttons.add(drill);
    drill.addClickHandler(event -> controller.showDrillList(getCurrentSelection().getID()));
    drill.addStyleName("leftFiveMargin");

    return buttons;
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
    add.addClickHandler(event -> gotDelete(add, getCurrentSelection()));
    add.setType(ButtonType.DANGER);
    return add;
  }

  private UserList<CommonShell> getCurrentSelection() {
    return container.getCurrentSelection();
  }

  private void gotDelete(Button delete, UserList<CommonShell> currentSelection) {
    final int uniqueID = currentSelection.getID();
    if (currentSelection.getUserID() == controller.getUser()) {
      controller.logEvent(delete, "Button", currentSelection.getName(), "Delete");

      if (currentSelection.isFavorite()) {
        Window.alert("Can't delete your favorites list.");
      }
      else {
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
    }
    else {
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
    int index = container.getIndex(currentSelection);
    //logger.info("deleteList ---> did do deleteList " + uniqueID + " index " + index);
    container.forgetItem(currentSelection);
    UserList<CommonShell> at = container.getAt(index);
    //logger.info("next is " + at.getName());
    container.markCurrentExercise(at.getID());
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
    container.addExerciseAfter(null, userList);
    container.markCurrentExercise(userList.getID());
    names.add(userList.getName());
    //   container.redraw();
  }
}
