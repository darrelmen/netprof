package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/14/17.
 */
class EditableExerciseList extends NPExerciseList implements FeedbackExerciseList {
  private final Logger logger = Logger.getLogger("EditableExerciseList");

//  private static final String ADD = "Add";

  /**
   * @see #makeDeleteButton
   */
//  private static final String REMOVE_FROM_LIST = "Remove";// from list";

  private final EditItem editItem;
  private final UserList<CommonShell> list;
  private SearchTypeahead searchTypeahead;
  private TextBox quickAddText;
  private HTML message;

  /**
   * @param controller
   * @param editItem
   * @param right
   * @param instanceName
   * @param list
   * @see EditItem#makeExerciseList
   */
  EditableExerciseList(ExerciseController controller,
                       EditItem editItem,
                       Panel right,
                       String instanceName,
                       UserList<CommonShell> list) {
    super(right,
        controller,
        new ListOptions()
            .setInstance(instanceName)
            .setShowTypeAhead(false)
            .setSort(false), 12);
    this.editItem = editItem;
    this.list = list;

    if (list.isEmpty()) delete.setEnabled(false);
    pagingContainer.setPageSize(8);
  }

  /**
   * @return
   * @see #addTableWithPager
   */
  protected DivWidget getOptionalWidget() {
    DivWidget widgets = new DivWidget();
    widgets.getElement().setId("northSouth");
//    widgets.addStyleName("inlineFlex");
    widgets.addStyleName("bottomFiveMargin");
//
    widgets.add(getAddButtonContainer());
    widgets.add(message = getFeedback());

    addListChangedListener((items, selectionID) -> enableRemove(!isEmpty()));

    return widgets;
    // return getAddButtonContainer();
  }

  @Override
  public void clearMessage() {
    message.setText("");
  }

  /**
   * @return
   * @see #getOptionalWidget
   */
  @NotNull
  private DivWidget getAddButtonContainer() {
    DivWidget addW = new DivWidget();
    addW.getElement().setId("buttonContainer");
    //addW.addStyleName("floatLeft");
    addW.addStyleName("topMargin");
    addW.addStyleName("inlineFlex");

    Button add = getAddButton();

    addW.add(getTypeahead(add));
    {

      DivWidget ac = new DivWidget();
      ac.addStyleName("leftFiveMargin");
      ac.add(add);
      addW.add(ac);
    }
    message = getFeedback();
//    addW.add(message = getFeedback());
    addW.add(getRemoveButtonContainer());
    return addW;
  }

  /**
   * @return
   * @see #getOptionalWidget
   */
  @NotNull
  private DivWidget getRemoveButtonContainer() {
    DivWidget delW = new DivWidget();
    //delW.addStyleName("floatLeft");
    delW.addStyleName("leftFiveMargin");
    //delW.getElement().getStyle().setClear(Style.Clear.LEFT);
    delW.add(makeDeleteButton());
    return delW;
  }

  private Button delete;

  private Button makeDeleteButton() {
    EditableExerciseList widgets = this;

    delete = makeDeleteButtonItself();
    delete.addClickHandler(event -> {
      CommonShell currentSelection = pagingContainer.getCurrentSelection();
      if (currentSelection != null) {
        deleteItem(currentSelection.getID(), widgets, widgets, delete);
      }
    });
    return delete;
  }

  /**
   * TODO : why is this here?
   *
   * @return
   * @see #makeDeleteButton
   */
  private Button makeDeleteButtonItself() {
    Button delete = new Button("");//REMOVE_FROM_LIST);
    delete.setIcon(IconType.MINUS);
    delete.getElement().setId("Remove_from_list");
    // delete.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
    delete.setType(ButtonType.WARNING);
    //   delete.addStyleName("floatRight");
    if (controller == null) {
//      logger.warning("no controller??");
    } else {
      controller.register(delete, "", "");//"Remove from list " + ul.getID() + "/" + ul.getName());
    }
    return delete;
  }

  /**
   * @return
   * @see #getAddButtonContainer
   */
  private Typeahead getTypeahead(Button add) {
    quickAddText = new TextBox();
    quickAddText.setMaxLength(100);
    quickAddText.setVisibleLength(40);
    quickAddText.addStyleName("topMargin");
    quickAddText.setWidth(235 + "px");

    quickAddText.addKeyUpHandler(event -> searchTypeahead.clearCurrentExercise());
    this.searchTypeahead = new SearchTypeahead(controller, this, add);
    return searchTypeahead.getTypeaheadUsing(quickAddText);
  }

  private HTML getFeedback() {
    HTML message = new HTML();
    message.setHeight("20px");
    message.addStyleName("leftFiveMargin");
    message.addStyleName("bottomFiveMargin");
    message.addStyleName("serverResponseLabelError");
    message.getElement().getStyle().setClear(Style.Clear.LEFT);
    return message;
  }

  @NotNull
  private Button getAddButton() {
    Button add = new Button("", IconType.PLUS);
    add.setType(ButtonType.SUCCESS);

    add.setEnabled(false);
    add.addClickHandler(event -> onClickAdd(add));
    return add;
  }


  private void onClickAdd(Button add) {
    add.setEnabled(false);
    if (searchTypeahead.getCurrentExercise() != null) {
      if (isOnList()) {
        // TODO : warn user already added.
        message.setText("This is already in the list.");
        enableButton(add);
      } else {
        message.setText("");
        controller.getListService().addItemToUserList(list.getID(), searchTypeahead.getCurrentExercise().getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding an exercise to a list", caught);
            enableButton(add);
          }

          @Override
          public void onSuccess(Void result) {
            enableButton(add);
            showNewItem(searchTypeahead.getCurrentExercise());
          }
        });
      }
    } else {
      String safeText = getSafeText(quickAddText);
      if (safeText.trim().isEmpty()) {
        enableButton(add);
        message.setText("Please enter some text.");
      } else {
        checkIsValidPhrase(add, safeText);
      }
    }
  }

  /**
   * TODO : this should be simpler - does the exercise exist or not - we don't create new exercises anymore.
   * @param add
   * @param safeText
   */
  private void checkIsValidPhrase(Button add, String safeText) {
    controller.getScoringService().isValidForeignPhrase(safeText, "", new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("checking exercise validity", caught);
        enableButton(add);
      }

      @Override
      public void onSuccess(Boolean result) {
        enableButton(add);
/*        logger.info("\tisValidForeignPhrase : checking phrase " + foreignLang.getSafeText() +
        " before adding/changing " + newUserExercise + " -> " + result);*/
        if (result) {
          controller.getListService().newExercise(
              list.getID(),
              makeNewExercise(safeText),
              new AsyncCallback<CommonExercise>() {
                @Override
                public void onFailure(Throwable caught) {
                  controller.handleNonFatalError("adding an exercise to a list", caught);
                }

                @Override
                public void onSuccess(CommonExercise newExercise) {
                  showNewItem(newExercise);
                }
              });
        } else {
          message.setText("The item " +
              " text is not in our " + controller.getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  private void enableButton(Button add) {
    add.setEnabled(true);
  }

  @NotNull
  private CommonExercise makeNewExercise(String safeText) {
    CommonExercise newItem = editItem.getNewItem();
    newItem.getMutable().setForeignLanguage(safeText);
    newItem.getMutable().setEnglish("");
    newItem.getMutable().setMeaning("");
    return newItem;
  }

  private boolean isOnList() {
    boolean found = false;
    if (searchTypeahead.getCurrentExercise() == null) return false;

    List<CommonShell> exercises = list.getExercises();
    for (CommonShell shell : exercises) {
      if (shell.getID() == searchTypeahead.getCurrentExercise().getID()) {
        found = true;
        break;
      }
    }
    return found;
  }

  private void showNewItem(CommonShell currentExercise) {
    list.addExercise(currentExercise);

//    int before = getSize();
    addExercise(currentExercise);
    //   int after = getSize();

//    logger.info("before " + before + " after " + after);
    enableRemove(true);

    gotClickOnItem(currentExercise);
    markCurrentExercise(currentExercise.getID());
  }

  private String getSafeText(TextBox box) {
    return sanitize(box.getText()).replaceAll("&#39;", "'");
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }


  protected int getNumTableRowsGivenScreenHeight() {
    return 12;
  }

  /**
   * Removes from 4 lists!
   *
   * @param exid
   * @param exerciseList
   * @param editableExerciseList
   * @param button
   * @paramx uniqueID
   * @see EditableExerciseList#makeDeleteButton
   */
  private void deleteItem(final int exid,
                          final PagingExerciseList<?, ?> exerciseList,
                          final EditableExerciseList editableExerciseList,
                          Button button) {
    button.setEnabled(false);
    controller.getListService().deleteItemFromList(list.getID(), exid, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("removing an exercise from a list", caught);
        enableButton();
      }

      @Override
      public void onSuccess(Boolean result) {
        enableButton();
        if (!result) {
          logger.warning("deleteItem huh? id " + exid + " not in list " + list);
        }

        exerciseList.forgetExercise(exid);

        if (!list.removeAndCheck(exid)) {
          logger.warning("deleteItem huh? didn't remove the item " + exid + " from " + list.getID() +
              " now " + list.getExercises().size());
        }
//        if (learnContainer != null && learnContainer.getReloadable() != null) {
//          learnContainer.getReloadable().redraw();   // TODO : or reload???
//        }
        logger.info("deleteItem list size is " + exerciseList.getSize());
        editableExerciseList.enableRemove(exerciseList.getSize() > 0);
      }

      private void enableButton() {
        button.setEnabled(true);
      }
    });
  }

/*
  @Override
  protected void onLastItem() {
  //  new ModalInfoDialog("Complete", "List complete!", hiddenEvent -> reloadExercises());
    showListComplete();
  }*/

  private void enableRemove(boolean enabled) {
    delete.setEnabled(enabled);
  }
}
