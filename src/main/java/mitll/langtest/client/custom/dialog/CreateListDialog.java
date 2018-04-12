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
 * © 2015 Massachusetts Institute of Technology.
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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.userlist.ListContainer;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.custom.TimeRange;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class CreateListDialog extends BasicDialog {
  private final Logger logger = Logger.getLogger("CreateListDialog");

  private static final String SHOW_AS_QUIZ = "Show as Quiz";
  private static final String CREATE_A_NEW_QUIZ = "Create a new quiz.";
  private static final String IS_A_QUIZ = "Is a quiz?";

  private static final String MAKE_A_QUIZ = "Make a quiz?";

  private static final String PUBLIC = "Public";
  private static final String PRIVATE = "Private";
  private static final String KEEP_LIST_PUBLIC_PRIVATE = "Keep Public/Private?";
  private static final String CREATE_NEW_LIST = "Create New List";
  private static final String TEXT_BOX = "TextBox";
  private static final String PUBLIC_PRIVATE_GROUP = "Public_Private_Group";

  private static final String PLEASE_FILL_IN_A_TITLE = "Please fill in a title";

  private static final String CLASS = "Course Info (optional)";
  private static final String TITLE = "Title";
  private static final String DESCRIPTION_OPTIONAL = "Description (optional)";
  private final CreateListComplete listView;
  private FormField titleBox;
  private final ExerciseController controller;
  private KeyPressHelper enterKeyButtonHelper;
  private TextArea theDescription;
  private FormField classBox;
  private RadioButton publicChoice, privateChoice;
  private UserList current = null;
  private boolean isEdit;
  private ControlGroup publicPrivateGroup;

  public CreateListDialog(CreateListComplete listView, ExerciseController controller, UserList current, boolean isEdit) {
    this(listView, controller);
    this.current = current;
    this.isEdit = isEdit;
  }

  public CreateListDialog(CreateListComplete listView, ExerciseController controller) {
    this.listView = listView;
    this.controller = controller;
  }

  /**
   * @param thirdRow
   * @seex
   * @seex mitll.langtest.client.custom.Navigation#getNavigation
   */
  public void doCreate(Panel thirdRow) {
    thirdRow.clear();
    Panel child = addEnterKeyBinding();
    thirdRow.add(child);
    zeroPadding(child);
    child.addStyleName("userListContainer");

    FluidRow row = new FluidRow();
    child.add(row);

    {
      titleBox = addControlFormField(row, TITLE);
      final TextBoxBase box = titleBox.box;
      if (current != null) box.setText(current.getName());
      box.getElement().setId("CreateListDialog_Title");
      box.addBlurHandler(event -> controller.logEvent(box, TEXT_BOX, CREATE_NEW_LIST, "Title = " + box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      theDescription = new TextArea();
      if (current != null) theDescription.setText(current.getDescription());
      final FormField description = getSimpleFormField(row, DESCRIPTION_OPTIONAL, theDescription, 1);
      description.box.getElement().setId("CreateListDialog_Description");
      description.box.addBlurHandler(event -> controller.logEvent(description.box, TEXT_BOX, CREATE_NEW_LIST, "Description = " + description.box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      classBox = addControlFormField(row, CLASS);
      if (current != null) classBox.setText(current.getClassMarker());
      classBox.box.getElement().setId("CreateListDialog_CourseInfo");
      classBox.box.addBlurHandler(event -> controller.logEvent(classBox.box, TEXT_BOX, CREATE_NEW_LIST, "CourseInfo = " + classBox.box.getValue()));
    }

    if (canMakeQuiz()) {
      child.add(getQuizChoices());
    }

    child.add(getPrivacyChoices());
    //  makeCreateButton(enterKeyButtonHelper, theDescription, classBox, publicChoice);
    Scheduler.get().scheduleDeferred(() -> titleBox.box.setFocus(true));
  }

  private boolean isQuiz = false;

  @NotNull
  private Widget getQuizChoices() {
    FluidRow row = new FluidRow();

    CheckBox checkBox = new CheckBox(isEdit ? SHOW_AS_QUIZ : CREATE_A_NEW_QUIZ);
    checkBox.addValueChangeHandler(event -> {
      isQuiz = checkBox.getValue();
/*      publicChoice.setValue(isQuiz);
      privateChoice.setValue(!isQuiz);*/
    });

    Panel hp = new HorizontalPanel();
    checkBox.addStyleName("leftFiveMargin");
    hp.add(checkBox);

    if (current != null) {
      boolean isQuiz = current.getListType() == UserList.LIST_TYPE.QUIZ;
      checkBox.setValue(isQuiz);
      this.isQuiz = isQuiz;
    }

    row.add(addControlGroupEntry(row, isEdit ? IS_A_QUIZ : MAKE_A_QUIZ, hp, ""));
    return row;
  }

  @NotNull
  private Widget getPrivacyChoices() {
    FluidRow row = new FluidRow();

    publicChoice = new RadioButton(PUBLIC_PRIVATE_GROUP, PUBLIC);
    publicChoice.addClickHandler(event -> gotClickOnPublic());
    RadioButton radioButton2 = new RadioButton(PUBLIC_PRIVATE_GROUP, PRIVATE);
    privateChoice = radioButton2;
    // students by default have private lists - ?
    {
      boolean isPrivate = getDefaultPrivacy();

      publicChoice.setValue(!isPrivate);
      radioButton2.setValue(isPrivate);
    }

    Panel hp = new HorizontalPanel();
    hp.add(publicChoice);
    radioButton2.addStyleName("leftFiveMargin");
    hp.add(radioButton2);

    row.add(publicPrivateGroup = addControlGroupEntry(row, KEEP_LIST_PUBLIC_PRIVATE, hp, ""));
    return row;
  }

  private void gotClickOnPublic() {

  }

  private boolean getDefaultPrivacy() {
    boolean isPrivate = controller.getUserState().getCurrent().isStudent();

    if (current != null) isPrivate = current.isPrivate();
    return isPrivate;
  }

  @NotNull
  private Panel addEnterKeyBinding() {
    enterKeyButtonHelper = new KeyPressHelper(true);
    return new DivWidget() {
      @Override
      protected void onUnload() {
        super.onUnload();
        enterKeyButtonHelper.removeKeyHandler();
      }
    };
  }

  /**
   * @see ListView#doAdd
   */
  public void doCreate() {
    gotCreate(enterKeyButtonHelper, theDescription, classBox, publicChoice);
  }

  /**
   * TODO: add time range widgets
   *
   * @param enterKeyButtonHelper
   * @param area
   * @param classBox
   * @param publicRadio
   */
  private void gotCreate(KeyPressHelper enterKeyButtonHelper,
                         TextArea area,
                         FormField classBox,
                         RadioButton publicRadio) {
    enterKeyButtonHelper.removeKeyHandler();
    TimeRange timeRange = new TimeRange();
    addUserList(titleBox, area, classBox, publicRadio.getValue(), getListType(), timeRange);
  }

  @NotNull
  private UserList.LIST_TYPE getListType() {
    boolean canMakeQuiz = canMakeQuiz();
    return canMakeQuiz && isQuiz ? UserList.LIST_TYPE.QUIZ : UserList.LIST_TYPE.NORMAL;
  }

  private boolean canMakeQuiz() {
    Collection<User.Permission> permissions = controller.getPermissions();
    return permissions.contains(User.Permission.TEACHER_PERM) || permissions.contains(User.Permission.PROJECT_ADMIN);
  }

  /**
   * @param names
   * @return
   * @see ListView#doAdd
   */
  public boolean isOKToCreate(Set<String> names) {
    boolean ret = true;

    if (!isValidName()) {
      ret = false;
    } else if (names.contains(titleBox.getSafeText())) {
      markError(titleBox, "Name already used. Please choose another.");
      ret = false;
    } else if ((!publicChoice.getValue() && !privateChoice.getValue())) {
      markErrorBlur(publicPrivateGroup,
          publicChoice,
          "",
          "Please mark either public or private.",
          Placement.TOP, true);
      ret = false;
    }
    return ret;
  }

  /**
   * @return
   * @see #isOKToCreate
   */
  public boolean isValidName() {
    return validateCreateList(titleBox);
  }

  /**
   * @param titleBox
   * @param area
   * @param classBox
   * @param isPublic
   * @param listType
   * @see #gotCreate
   */
  private void addUserList(final FormField titleBox, TextArea area, FormField classBox, boolean isPublic,
                           UserList.LIST_TYPE listType, TimeRange timeRange) {
    final String safeText = titleBox.getSafeText();
    logger.info("addUserList " + safeText);
    // UserList.LIST_TYPE normal = UserList.LIST_TYPE.NORMAL;
    controller.getListService().addUserList(
        safeText,
        sanitize(area.getText()),
        classBox.getSafeText(),
        isPublic,
        listType,
        timeRange,
        new AsyncCallback<UserList>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new list", caught);
          }

          @Override
          public void onSuccess(UserList result) {
            if (result == null) {
              markError(titleBox, "You already have a list named " + safeText);
//              Window.alert("You already have a list with that name.");
              //logger.info("NOIT SUCCESS " + result);
              //listView.madeIt(result);
            } else {
              //logger.info("Success " + result);
              listView.madeIt(result);
            }
          }
        });
  }

  private void zeroPadding(Panel createContent) {
    createContent.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    createContent.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  /**
   * @param titleBox
   * @return
   */
  private boolean validateCreateList(FormField titleBox) {
    if (titleBox.getSafeText().isEmpty()) {
      markError(titleBox, PLEASE_FILL_IN_A_TITLE);
      return false;
    } else {
      return true;
    }
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  /**
   * @param currentSelection
   * @param container
   * @see ListView#doEdit
   * @see ListView#gotEdit
   */
  public void doEdit(UserList<CommonShell> currentSelection, ListContainer container) {
    currentSelection.setName(titleBox.getSafeText());
    currentSelection.setDescription(sanitize(theDescription.getText()));
    currentSelection.setClassMarker(sanitize(classBox.getSafeText()));
    boolean aPrivate = !publicChoice.getValue();
    currentSelection.setPrivate(aPrivate);
    UserList.LIST_TYPE listType = getListType();
    currentSelection.setListType(listType);

    logger.info("doEdit is " +
        "\n\tprivate " + aPrivate +
        "\n\ttype    " + listType
    );

    controller.getListService().update(currentSelection, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("changing a list", caught);
      }

      @Override
      public void onSuccess(Void result) {
        container.redraw();
      }
    });
  }
}