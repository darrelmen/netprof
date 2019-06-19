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
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.userlist.ImportBulk;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/14/17.
 */
class EditableExerciseList extends NPExerciseList<CommonShell, ClientExercise> implements FeedbackExerciseList {
  private final Logger logger = Logger.getLogger("EditableExerciseList");

  static final String SAFE_TEXT_REPLACEMENT = "&#39;";

  /**
   * @see #onClickAdd
   */
  private static final String THIS_IS_ALREADY_IN_THE_LIST = "This is already in the list.";
  private static final String PLEASE_ENTER_SOME_TEXT = "Please enter some text.";

  private final UserList<CommonShell> list;
  private SearchTypeahead searchTypeahead;
  private HTML message;

  /**
   * @param controller
   * @param right
   * @param instanceName
   * @param list
   * @see EditItem#makeExerciseList
   */
  EditableExerciseList(ExerciseController controller,
                       Panel right,
                       INavigation.VIEWS instanceName,
                       UserList<CommonShell> list) {
    super(right,
        controller,
        new ListOptions()
            .setInstance(instanceName)
            .setShowTypeAhead(false)
            .setSort(false)
        , 12);
    setUserListID(list.getID());
    this.list = list;
  }

  @Override
  public void addComponents() {
    super.addComponents();

    if (list.isEmpty()) delete.setEnabled(false);
    pagingContainer.setPageSize(8);
  }

  public int getListID() {
    return list.getID();
  }

  /**
   * @return
   * @see #addTableWithPager
   */
  protected DivWidget getOptionalWidget() {
    DivWidget widgets = new DivWidget();
    widgets.getElement().setId("northSouth");
    widgets.addStyleName("bottomFiveMargin");
    widgets.add(getAddButtonContainer());
    widgets.add(message = getFeedback());

    addListChangedListener((items, selectionID) -> enableRemove(!isEmpty()));

    return widgets;
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
    addW.getElement().setId("typeAheadContainer");
    addW.addStyleName("topMargin");
    addW.addStyleName("inlineFlex");

    {
      Button add = getAddButton();
      addW.add(getTypeahead(add));
      {

        DivWidget ac = new DivWidget();
        ac.addStyleName("leftFiveMargin");
        ac.add(add);
        addW.add(ac);
      }
    }
    message = getFeedback();
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
    delW.addStyleName("leftFiveMargin");
    delW.add(makeDeleteButton());
    return delW;
  }

  private Button delete;

  private Button makeDeleteButton() {
    EditableExerciseList widgets = this;

    delete = makeDeleteButtonItself();
    delete.addClickHandler(event -> {
      HasID currentSelection = pagingContainer.getCurrentSelection();
      if (currentSelection != null) {
        deleteItem(currentSelection.getID(), widgets, widgets, delete);
      }
      clearMessage();
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
    Button delete = new Button("");
    delete.setIcon(IconType.MINUS);
    delete.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
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

  private TextBox quickAddText;

  /**
   * @return
   * @see #getAddButtonContainer
   */
  private Typeahead getTypeahead(Button add) {
    quickAddText = getEntryTextBox();
    quickAddText.addKeyUpHandler(event -> {
      //  logger.info("getTypeahead got key up "+ quickAddText.getText());
      searchTypeahead.clearCurrentExercise();
      message.setText("");
      checkForKeyUpDown(event);
    });

    this.searchTypeahead = new SearchTypeahead(controller, add);
    return searchTypeahead.getTypeaheadUsing(quickAddText);
  }

  private void checkForKeyUpDown(KeyUpEvent event) {
    // arrow up down Paul suggestion.
    int keyCode = event.getNativeEvent().getKeyCode();
    if (keyCode == 40) {  // down
      loadNext();
    } else if (keyCode == 38) {
      loadPrev();
    }
  }

  /**
   * @see EditItem#grabFocus()
   */
  void grabFocus() {
    Scheduler.get().scheduleDeferred((Command) () -> quickAddText.setFocus(true));
  }

  private TextBox getEntryTextBox() {
    TextBox quickAddText = new TextBox();
    quickAddText.setMaxLength(100);
    quickAddText.setVisibleLength(40);
    quickAddText.addStyleName("topMargin");
    quickAddText.setWidth(235 + "px");
    quickAddText.getElement().getStyle().setProperty("fontFamily", "sans-serif");
    quickAddText.getElement().getStyle().setFontSize(18, Style.Unit.PX);
    return quickAddText;
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
    add.getElement().getStyle().setMarginTop(10, Style.Unit.PX);

    add.setEnabled(false);
    add.addClickHandler(event -> onClickAdd(add));
    return add;
  }


  private void onClickAdd(Button add) {
    add.setEnabled(false);

    //  logger.info("onClickAdd click add " + add);

    final CommonShell currentExercise = searchTypeahead.getCurrentExercise();
    if (currentExercise != null) {
      if (isOnList()) {
        message.setText(THIS_IS_ALREADY_IN_THE_LIST);
        enableButton(add);
      } else {   // not on the list
        message.setText("");

        //     logger.info("onClickAdd value after add is " + quickAddText.getText());

        controller.getListService().addItemToUserList(list.getID(), currentExercise.getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding an exercise to a list", caught);
            enableButton(add);
          }

          @Override
          public void onSuccess(Void result) {
            enableButton(add);
            showNewItem(currentExercise);
            clearTextBoxField();
            searchTypeahead.clearCurrentExercise();
          }
        });
      }
    } else {
      String safeText = getSafeText(quickAddText);

      //   logger.info("onClickAdd safeText " + safeText);

      if (safeText.trim().isEmpty()) {  // necessary?
        logger.info("\n\n\nempty box???");
        enableButton(add);
        message.setText(PLEASE_ENTER_SOME_TEXT);
      } else {
        checkIsValidPhrase(add, safeText);
      }
    }
  }

  private void clearTextBoxField() {
    quickAddText.setValue("", false);
  }

  /**
   * TODO : this should be simpler - does the exercise exist or not - we don't create new exercises anymore.
   *
   * @paramx add
   * @paramx safeText
   * @see #onClickAdd
   */
  private void checkIsValidPhrase(Button add, String safeText) {
    enableButton(add);

    if (false) logger.info("\tisValidForeignPhrase : checking phrase " + safeText + " on list " + list.getID());

    controller.getListService().newExercise(list.getID(), safeText,
        new AsyncCallback<CommonExercise>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding an exercise to a list", caught);
          }

          @Override
          public void onSuccess(CommonExercise newExercise) {
            if (newExercise == null) {
              // logger.info("onSuccess not in dict!");
              message.setText("This is not in our " + controller.getLanguageInfo().toDisplay() + " dictionary. Please edit.");

            } else {
              // logger.info("checkIsValidPhrase got " + newExercise.getID() + " dir " + newExercise.getDirectlyRelated());
              showNewItem(newExercise);
              clearTextBoxField();
              new ImportBulk().refreshExerciseIDsOnHydra(Collections.singletonList(newExercise), controller);
            }
          }
        });
  }

  private void enableButton(Button add) {
    add.setEnabled(true);
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

  /**
   * Wrap around!
   *
   * @see #loadNextExercise
   */
  @Override
  protected void onLastItem() {
    CommonShell first = getFirst();
    if (first != getCurrentExercise()) {
      loadByID(first.getID());
    }
  }

  private void showNewItem(CommonShell currentExercise) {
    list.addExercise(currentExercise);

    int before = getSize();
    addExercise(currentExercise);
    int after = getSize();
    //  logger.info("before " + before + " after " + after);
    enableRemove(true);

    gotClickOnItem(currentExercise);
    markCurrentExercise(currentExercise.getID());
  }

  private String getSafeText(TextBox box) {
    return sanitize(box.getText()).replaceAll(SAFE_TEXT_REPLACEMENT, "'");
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  protected int getNumTableRowsGivenScreenHeight() {
    return 12;
  }

  /**
   * Removes from 4 lists!  ??????
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
//        logger.info("deleteItem list size is " + exerciseList.getSize());
        editableExerciseList.enableRemove(exerciseList.getSize() > 0);
      }

      private void enableButton() {
        button.setEnabled(true);
      }
    });
  }

  private void enableRemove(boolean enabled) {
    delete.setEnabled(enabled);
  }
}
