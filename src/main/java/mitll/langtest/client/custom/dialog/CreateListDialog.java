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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.custom.QuizSpec;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

public class CreateListDialog<T extends UserList> extends CreateDialog<T> {
  private final Logger logger = Logger.getLogger("CreateListDialog");

  private static final String HEAR_ITEMS = "Hear Items";
  /**
   * @see #getPlayAudioCheck
   */
  private static final String PLAY_AUDIO = "Play Audio?";


  private static final String QUIZ_SIZE = "# Items";
  /**
   * @see #getDurationLabel
   */
  private static final String DURATION_MINUTES = "Duration (Min.)";
  private static final int DEFAULT_QUIZ_SIZE = 10;

  private static final int MIN_DURATION = 1;
  private static final int DEFAULT_DURATION = 1;
  private static final String PUBLIC_QUIZZES = "Public quizzes can be seen by all students.";
  private static final int DEFAULT_MIN_SCORE = 30;
  private static final int MAX_SCORE = 71;
  private static final int MIN_SCORE = 0;
  private static final String MIN_SCORE1 = "Min. Score to Adv.";
  private static final String DESCRIPTION_OPTIONAL = "Description";


  //  private static final String PLEASE_MARK_EITHER_PUBLIC_OR_PRIVATE = "Please mark either public or private.";
//  private static final String NAME_ALREADY_USED = "Name already used. Please choose another.";
  private static final int MAX_DURATION = 21;
  private static final int MAX_QUIZ_SIZE = 110;
  private static final int MIN_QUIZ_SIZE = 0;

  private static final String SHOW_AS_QUIZ = "Show as Quiz";
  /**
   *
   */
  private static final String CREATE_A_NEW_QUIZ = "Create a new quiz. (Items are chosen randomly.)";
  private static final String IS_A_QUIZ = "Is a quiz?";

  private static final String MAKE_A_QUIZ = "Make a quiz?";

  private static final String CONTENT_GROUP = "Content_Group";

  private static final String CLASS = "Course Info";
  //  private static final String TITLE = "Title";
  private FormField classBox;
  private RadioButton sentenceChoice;
  private RadioButton bothChoice;
  /**
   *
   */
  private boolean playAudio;

  private int quizSize = DEFAULT_QUIZ_SIZE;
  private int minScore = DEFAULT_MIN_SCORE;
  private int duration = DEFAULT_DURATION;

  private Heading modeDep;
  private boolean isQuiz = false;

  /**
   * @param listView
   * @param controller
   * @param current
   * @param isEdit
   * @see ListView#doEdit
   */
  public CreateListDialog(CreateComplete<T> listView, ExerciseController controller, T current, boolean isEdit, Set<String> names) {
    this(listView, controller, names, current, isEdit);

    this.isQuiz = current.getListType() == UserList.LIST_TYPE.QUIZ;
    this.minScore = current.getMinScore();
    this.duration = current.getDuration();
    this.playAudio = current.shouldShowAudio();
  }

  /**
   * @param listView
   * @param controller
   * @param names
   * @see ListView#doAdd
   */
  public CreateListDialog(CreateComplete<T> listView, ExerciseController controller, Set<String> names) {
    super(null, names, false, controller, listView);
  }

  private CreateListDialog(CreateComplete<T> listView, ExerciseController controller, Set<String> names, T current, boolean isEdit) {
    super(current, names, isEdit, controller, listView);
  }

  public CreateListDialog setIsQuiz(boolean isQuiz) {
    this.isQuiz = isQuiz;
    return this;
  }

  @Override
  protected void addFieldsBelowDescription(Panel child) {
    addClassBox(child);

    if (canMakeQuiz()) {
      if (isQuiz) {
        if (isEdit) {
          child.add(getQuizChoices());
          addEditOptions(child);
        } else {
          addQuizOptions(child);
        }
      } else {
        if (isEdit) {
          child.add(getQuizChoices());
          addEditOptions(child);
        }
      }
    }

    child.add(getPrivacyChoices());

    addWarningField(child);

    moveFocusToTitleLater();
  }

  private void addClassBox(Panel child) {
    FluidRow row;
    row = new FluidRow();
    child.add(row);

    classBox = addControlFormField(row);
    classBox.setHint(CLASS + " (optional)");
    classBox.box.addKeyUpHandler(this::checkEnter);
    if (isEditing()) classBox.setText(getCurrent().getClassMarker());
    classBox.box.getElement().setId("CreateListDialog_CourseInfo");
    classBox.box.addBlurHandler(event -> controller.logEvent(classBox.box, TEXT_BOX, CREATE_NEW_LIST, "CourseInfo = " + classBox.box.getValue()));
  }

  @NotNull
  @Override
  protected String getDescriptionOptional() {
    return DESCRIPTION_OPTIONAL;
  }

  private void addQuizOptions(Panel child) {
    child.add(getQuizChoices());

    DivWidget createQuizOptions = new DivWidget();
    styleQuizOptions(createQuizOptions);
    checkQuizOptionsVisible();
    child.add(createQuizOptions);

    createQuizOptions.add(getQuizChoices(true));
  }

  /**
   * @param child
   */
  private void addEditOptions(Panel child) {
    editQuizOptions = new DivWidget();
    styleQuizOptions(editQuizOptions);
    editQuizOptions.add(getQuizChoices(false));
    child.add(editQuizOptions);

    editQuizOptions.setVisible(isQuiz);
  }


  private void styleQuizOptions(DivWidget quizOptions) {
    quizOptions.addStyleName("leftFiveMargin");
    quizOptions.addStyleName("rightFiveMargin");
    quizOptions.addStyleName("url-box");
  }


  @NotNull
  private Grid getQuizChoices(boolean isCreate) {
    Grid grid = new Grid(isCreate ? 4 : 2, 4);


    int col = 0;
    int row = 0;

    if (isCreate) {
      RowAndCol rowAndCol = new RowAndCol(grid, col, row).invoke(true, null, null);
      col = rowAndCol.getCol();
      row = rowAndCol.getRow();

      grid.setWidget(row, col++, getContentChoices());

      row++;
      col = 0;

      grid.setWidget(row, col++, getQuizSizeLabel());
    }

    grid.setWidget(row, col++, getDurationLabel());
    grid.setWidget(row, col++, getLabel(MIN_SCORE1));
    grid.setWidget(row, col++, getHearLabel());

    row++;
    col = 0;
    if (isCreate) {
      grid.setWidget(row, col++, getSizeChoices());
    }
    grid.setWidget(row, col++, getDurationChoices());
    grid.setWidget(row, col++, getMinScoreChoices());
    grid.setWidget(row, col++, getPlayAudioCheck());
    return grid;
  }

  @NotNull
  private HTML getHearLabel() {
    HTML label;
    label = getLabel(HEAR_ITEMS);
    label.addStyleName("leftTenMargin");
    return label;
  }

  @NotNull
  private CheckBox getPlayAudioCheck() {
    CheckBox w = new CheckBox(PLAY_AUDIO);
    w.addStyleName("leftFiveMargin");
    w.addValueChangeHandler(event -> playAudio = w.getValue());
    if (getCurrent() != null) {
      // logger.info("got " + current.getID() + " " + current.shouldShowAudio());
      w.setValue(getCurrent().shouldShowAudio());
    } else {
      logger.info("getPlayAudioCheck no current list?");
    }
    return w;
  }

  @NotNull
  private HTML getDurationLabel() {
    return getLabel(DURATION_MINUTES);
  }

  @NotNull
  private HTML getQuizSizeLabel() {
    return getEditableLabel(QUIZ_SIZE);
  }

  @NotNull
  private HTML getEditableLabel(String quizSize) {
    HTML quiz_size = getLabel(quizSize);
    quiz_size.setVisible(getCurrent() == null);
    return quiz_size;
  }

  private ListBox getSizeChoices() {
    ListBox w = getListBox();
    for (int i = MIN_QUIZ_SIZE; i < MAX_QUIZ_SIZE; i += 10) {
      w.addItem("" + i);
    }
    w.setSelectedValue("" + DEFAULT_QUIZ_SIZE);
    w.addChangeHandler(event -> gotListSelection(w.getValue()));
    w.setVisible(getCurrent() == null);

    return w;
  }

  private ListBox getDurationChoices() {
    ListBox w = getListBox();

    for (int i = MIN_DURATION; i < MAX_DURATION; i++) {
      w.addItem("" + i);
    }
    w.setSelectedValue("" + (isEditing() ? (duration = getCurrent().getDuration()) : DEFAULT_DURATION));
    w.addChangeHandler(event -> gotDurationSelection(w.getValue()));
    return w;
  }

  private ListBox getMinScoreChoices() {
    ListBox w = getListBox();
    for (int i = MIN_SCORE; i < MAX_SCORE; i += 10) {
      w.addItem("" + i);
    }

    w.setSelectedValue("" + (isEditing() ? (minScore = getCurrent().getMinScore()) : DEFAULT_MIN_SCORE));

    w.addChangeHandler(event -> gotListSelection3(w.getValue()));
    return w;
  }

  private void checkQuizOptionsVisible() {
    // createQuizOptions.setVisible(isQuiz && current == null);
    if (editQuizOptions != null) {
      editQuizOptions.setVisible(isQuiz && isEditing());
    }
  }


  private void gotListSelection(String value) {
    quizSize = Integer.parseInt(value);
  }

  private void gotDurationSelection(String value) {
    duration = Integer.parseInt(value);
  }

  private void gotListSelection3(String value) {
    minScore = Integer.parseInt(value);
  }

  private DivWidget editQuizOptions;

  private void addWarningField(Panel child) {
    FluidRow row;
    row = new FluidRow();
    child.add(row);
    modeDep = new Heading(4, PUBLIC_QUIZZES);
    modeDep.setHeight(14 + "px");
    modeDep.getElement().getStyle().setColor("blue");
    modeDep.addStyleName("leftFiveMargin");
    modeDep.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
    modeDep.setVisible(false);
    row.add(modeDep);
  }

  @Override
  protected void gotClickOnPublic() {
    modeDep.setVisible(isQuiz);
  }

  @Override
  protected void gotClickOnPrivate() {
    modeDep.setVisible(false);
  }

  @NotNull
  private Widget getQuizChoices() {
    FluidRow row = new FluidRow();

    //   logger.info("getQuizChoices edit = " + isEdit);
    CheckBox checkBox = new CheckBox(isEdit ? SHOW_AS_QUIZ : CREATE_A_NEW_QUIZ);
    checkBox.addValueChangeHandler(event -> {
      isQuiz = checkBox.getValue();
      checkQuizOptionsVisible();
    });
    checkBox.setValue(isQuiz);

    Panel hp = new HorizontalPanel();
    checkBox.addStyleName("leftFiveMargin");
    hp.add(checkBox);

//    if (isEditing()) {
//      boolean isQuiz = current.getListType() == UserList.LIST_TYPE.QUIZ;
//      checkBox.setValue(isQuiz);
//      this.isQuiz = isQuiz;
//    }

    row.add(addControlGroupEntry(row, isEdit ? IS_A_QUIZ : MAKE_A_QUIZ, hp, ""));
    return row;
  }

  @NotNull
  private Widget getContentChoices() {
    FluidRow row = new FluidRow();

    RadioButton vocabChoice = new RadioButton(CONTENT_GROUP, "Items");
    // vocabChoice.addClickHandler(event -> gotClickOnPublic());

    RadioButton radioButton2 = new RadioButton(CONTENT_GROUP, "Sentences");
    sentenceChoice = radioButton2;

    bothChoice = new RadioButton(CONTENT_GROUP, "Both");

    //privateChoice.addClickHandler(event -> gotClickOnPrivate());
    // students by default have private lists - ?
    {
      // boolean isPrivate = getDefaultPrivacy();

      vocabChoice.setValue(true);
      // radioButton2.setValue(isPrivate);
    }

    Panel hp = new VerticalPanel();
    hp.addStyleName("inlineFlex");
    hp.add(vocabChoice);
    //radioButton2.addStyleName("leftFiveMargin");
    hp.add(sentenceChoice);
    hp.add(bothChoice);

    ControlGroup contentChoiceGroup;
    row.add(contentChoiceGroup = addControlGroupEntry(row, "Content Type", hp, ""));
    contentChoiceGroup.getElement().getStyle().setMarginTop(-23, Style.Unit.PX);
    return row;
  }

  @Override
  protected void setDescription(T current) {
    if (isEditing()) theDescription.setText(getCurrent().getDescription());
  }

  /**
   * @see ListView#doAdd
   */
  @Override
  protected void doCreate() {
    gotCreate(enterKeyButtonHelper, theDescription, classBox, publicChoice);
  }

  /**
   * TODO: add time range widgets
   *
   * @param enterKeyButtonHelper
   * @param area
   * @param classBox
   * @param publicRadio
   * @see #doCreate()
   */
  private void gotCreate(KeyPressHelper enterKeyButtonHelper,
                         TextArea area,
                         FormField classBox,
                         RadioButton publicRadio) {
    enterKeyButtonHelper.removeKeyHandler();
    addUserList(titleBox, area, classBox, publicRadio.getValue(), getListType());
  }

  @NotNull
  private UserList.LIST_TYPE getListType() {
    boolean canMakeQuiz = canMakeQuiz();
    return canMakeQuiz && isQuiz ? UserList.LIST_TYPE.QUIZ : UserList.LIST_TYPE.NORMAL;
  }

  private boolean canMakeQuiz() {
    if (isEditing() && getCurrent().isFavorite()) {
      return false;
    } else {
      Collection<Permission> permissions = controller.getPermissions();
      return permissions.contains(Permission.TEACHER_PERM) || permissions.contains(Permission.PROJECT_ADMIN);
    }
  }


  /**
   * @param titleBox
   * @param area
   * @param classBox
   * @param isPublic
   * @param listType
   * @paramx duration
   * @see #gotCreate
   */
  private void addUserList(final FormField titleBox,
                           TextArea area,
                           FormField classBox,
                           boolean isPublic,
                           UserList.LIST_TYPE listType) {
    final String safeText = titleBox.getSafeText();
    //  logger.info("addUserList " + safeText);

    controller.getListService().addUserList(
        safeText,
        sanitize(area.getText()),
        classBox.getSafeText(),
        isPublic,
        listType,
        quizSize,
        getQuizSpec(),
        getUnitAndChapterSelections(),
        new AsyncCallback<T>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new list", caught);
          }

          @Override
          public void onSuccess(T result) {
            if (result == null) {
              markError(titleBox, "You already have a list named " + safeText);
            } else {
              //   logger.info("addUserList onSuccess " + result);
              listView.madeIt(result);
            }
          }
        });
  }

  @NotNull
  private QuizSpec getQuizSpec() {
    QuizSpec quizSpec = new QuizSpec(duration, minScore, playAudio, true, "");

    if (sentenceChoice != null && sentenceChoice.getValue()) {
      quizSpec.setExercisetypes(QuizSpec.EXERCISETYPES.SENTENCES);
    } else if (bothChoice != null && bothChoice.getValue()) {
      quizSpec.setExercisetypes(QuizSpec.EXERCISETYPES.BOTH);
    }

    return quizSpec;
  }

  /**
   * @param currentSelection
   * @param container
   * @see ListView#doEdit
   * @see ListView#gotEdit
   */
  @Override
  public void doEdit(T currentSelection, ButtonMemoryItemContainer<T> container) {
    currentSelection.setName(titleBox.getSafeText());
    currentSelection.setDescription(sanitize(theDescription.getText()));
    currentSelection.setClassMarker(sanitize(classBox.getSafeText()));
    {
      boolean aPrivate = !publicChoice.getValue();
      currentSelection.setPrivate(aPrivate);
    }
    UserList.LIST_TYPE listType = getListType();
    currentSelection.setListType(listType);
    currentSelection.setDuration(duration);
    currentSelection.setMinScore(minScore);
    currentSelection.setShowAudio(playAudio);

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