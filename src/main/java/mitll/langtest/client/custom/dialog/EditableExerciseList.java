package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.ButtonGroupSectionWidget;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/14/17.
 */
class EditableExerciseList extends NPExerciseList<ButtonGroupSectionWidget> {
  private final Logger logger = Logger.getLogger("EditableExerciseList");

  /**
   * @see #makeDeleteButton(UserList)
   */
  private static final String REMOVE_FROM_LIST = "Remove from list";

  private EditItem editItem;
  //  private final boolean includeAddItem;
  // private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);
  private UserList<CommonShell> list;

  /**
   * @param controller
   * @param editItem
   * @param right
   * @param instanceName
   * @param list
   * @paramx includeAddItem
   * @see EditItem#makeExerciseList
   */
  public EditableExerciseList(ExerciseController controller,
                              EditItem editItem,
                              Panel right,
                              String instanceName,
                              //  boolean includeAddItem,
                              UserList<CommonShell> list) {
    super(right, GWT.create(ExerciseService.class),
        controller.getFeedback(), controller,
        true, instanceName, false, false, ActivityType.EDIT);
    this.editItem = editItem;
    this.list = list;

    if (list.isEmpty()) delete.setEnabled(false);

  }

  protected DivWidget getOptionalWidget() {
    DivWidget widgets = new DivWidget();

    DivWidget addW = new DivWidget();
    addW.addStyleName("floatLeftList");
    Button add = getAddButton();
    addW.add(add);
    widgets.add(addW);

    DivWidget delW = new DivWidget();
    delW.addStyleName("floatLeftList");
    delW.addStyleName("leftFiveMargin");
    Button deleteButton = makeDeleteButton();
    delW.add(deleteButton);
    widgets.add(delW);

    addListChangedListener(new ListChangeListener<CommonShell>() {
      @Override
      public void listChanged(List<CommonShell> items, String selectionID) {

        logger.warning("got list changed - list is " + isEmpty());
        enableRemove(!isEmpty());
      }
    });
    return widgets;
  }

  //private CommonExercise currentExercise = null;

  @NotNull
  private Button getAddButton() {
    Button add = new Button("Add", IconType.PLUS);

    final ListInterface<CommonShell> outer = this;
    add.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onClickAdd(outer);
      }
    });
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  NewUserExercise newExercise;

  private void onClickAdd(ListInterface<CommonShell> outer) {
    //CommonExercise newItem = currentExercise == null ? editItem.getNewItem() : currentExercise;

    //if (newExercise.isCompleted()) {
    CommonExercise newItem = editItem.getNewItem();
    newExercise = new NewUserExercise(controller, newItem, "newExercise", list);

    DivWidget container = new DivWidget();
    Panel widgets1 = newExercise.addNew(outer, container);
    container.add(widgets1);

    showModal(newExercise, container);
    // }
  }

  private void showModal(NewUserExercise newExercise, DivWidget container) {
    final Modal modal = new Modal(true);
    modal.setWidth(750);
    modal.setHeight(750 + "px");
    modal.setMaxHeigth(750 + "px");
    modal.setTitle("Create new item");
    modal.add(container);
    newExercise.setModal(modal);
    modal.show();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        newExercise.setFocus();
      }
    });
  }

  private  Button delete;
  private Button makeDeleteButton() {

    EditableExerciseList widgets = this;

    delete = makeDeleteButtonItself();


    delete.addClickHandler(event -> {
      CommonShell currentSelection = pagingContainer.getCurrentSelection();
      if (currentSelection != null) {
//          logger.info(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);
        NewUserExercise newExercise = new NewUserExercise(controller, null, "newExercise", list);
        newExercise.deleteItem(currentSelection.getID(),  widgets, null, widgets);
      }
    });
   // delete.addStyleName("topFiftyMargin");
    return delete;
  }

  /**
   * TODO : why is this here?
   *
   * @return
   * @see #makeDeleteButton
   */
  private  Button makeDeleteButtonItself() {
    Button delete = new Button(REMOVE_FROM_LIST);
    delete.getElement().setId("Remove_from_list");
   // delete.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    delete.setType(ButtonType.WARNING);
 //   delete.addStyleName("floatRight");
    // if (ul == null) logger.warning("no user list");
    // else
    if (controller == null) logger.warning("no controller??");
    else {
      controller.register(delete, "", "");//"Remove from list " + ul.getID() + "/" + ul.getName());
    }
    return delete;
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        reloadExercises();
      }
    });
  }

  public void enableRemove(boolean enabled) {
    delete.setEnabled(enabled);
  }

/*
  @Override
  protected void askServerForExercise(int itemID) {
    if (itemID == EditItem.NEW_EXERCISE_ID) {
      useExercise(editItem.getNewItem());
    } else {
      //     logger.info("EditItem.makeExerciseList - askServerForExercise = " + itemID);
      super.askServerForExercise(itemID);
    }
  }
*/

/*  @Override
  public List<CommonShell> rememberExercises(List<CommonShell> result) {
    clear();
    boolean addNewItem = includeAddItem;

    for (final CommonShell es : result) {
      logger.info("Adding " + es.getID() + " : " + es.getClass());
      addExercise(es);
      if (includeAddItem && es.getID() == EditItem.NEW_EXERCISE_ID) {
        addNewItem = false;
      }
    }

    if (addNewItem) {
      CommonExercise newItem = editItem.getNewItem();

      logger.info("Adding " + newItem.getID() + " : " + newItem.getClass());

      addExercise(newItem);  // TODO : fix this
    }
    flush();
    return result;
  }*/
}
