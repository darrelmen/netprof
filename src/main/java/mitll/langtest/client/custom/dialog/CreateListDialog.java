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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.client.custom.userlist.ListContainer;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class CreateListDialog extends BasicDialog {
  private final Logger logger = Logger.getLogger("CreateListDialog");
  private static final int LIST_WIDTH = 60;

  private static final String ALL = "All";
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


  private static final String PLEASE_MARK_EITHER_PUBLIC_OR_PRIVATE = "Please mark either public or private.";
  private static final String NAME_ALREADY_USED = "Name already used. Please choose another.";
  private static final int MAX_DURATION = 21;
  private static final int MAX_QUIZ_SIZE = 110;
  private static final int MIN_QUIZ_SIZE = 0;

  private static final String SHOW_AS_QUIZ = "Show as Quiz";
  private static final String CREATE_A_NEW_QUIZ = "Create a new quiz. (Items are chosen randomly.)";
  private static final String IS_A_QUIZ = "Is a quiz?";

  private static final String MAKE_A_QUIZ = "Make a quiz?";

  private static final String PUBLIC = "Public";
  private static final String PRIVATE = "Private";
  private static final String KEEP_LIST_PUBLIC_PRIVATE = "Keep Public/Private?";
  private static final String CREATE_NEW_LIST = "Create New List";
  private static final String TEXT_BOX = "TextBox";
  private static final String PUBLIC_PRIVATE_GROUP = "Public_Private_Group";

  private static final String PLEASE_FILL_IN_A_TITLE = "Please fill in a title";

  private static final String CLASS = "Course Info";
  private static final String TITLE = "Title";
  private static final String DESCRIPTION_OPTIONAL = "Description";
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
  public CreateListDialog(CreateListComplete listView, ExerciseController controller, UserList current, boolean isEdit) {
    this(listView, controller);
    this.current = current;
    this.isEdit = isEdit;

    this.isQuiz = current.getListType() == UserList.LIST_TYPE.QUIZ;
    this.minScore = current.getMinScore();
    this.duration = current.getDuration();
    this.playAudio = current.shouldShowAudio();
  }

  /**
   * @param listView
   * @param controller
   * @see ListView#doAdd
   */
  public CreateListDialog(CreateListComplete listView, ExerciseController controller) {
    this.listView = listView;
    this.controller = controller;
  }

  public CreateListDialog setIsQuiz(boolean isQuiz) {
    this.isQuiz = isQuiz;
    return this;
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
      if (isEditing()) box.setText(current.getName());
      box.getElement().setId("CreateListDialog_Title");
      box.addBlurHandler(event -> controller.logEvent(box, TEXT_BOX, CREATE_NEW_LIST, "Title = " + box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      theDescription = new TextArea();
      theDescription.setPlaceholder("(optional)");
      if (isEditing()) theDescription.setText(current.getDescription());
      final FormField description = getSimpleFormField(row, DESCRIPTION_OPTIONAL, theDescription, 1);
      description.box.getElement().setId("CreateListDialog_Description");
      description.box.addBlurHandler(event -> controller.logEvent(description.box, TEXT_BOX, CREATE_NEW_LIST, "Description = " + description.box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      classBox = addControlFormField(row, CLASS);
      classBox.setHint("(optional)");
      if (isEditing()) classBox.setText(current.getClassMarker());
      classBox.box.getElement().setId("CreateListDialog_CourseInfo");
      classBox.box.addBlurHandler(event -> controller.logEvent(classBox.box, TEXT_BOX, CREATE_NEW_LIST, "CourseInfo = " + classBox.box.getValue()));
    }

    if (isQuiz) {
      if (isEdit) {
        child.add(getQuizChoices());
        addEditOptions(child);
      } else {
        addQuizOptions(child);
      }
    }
    else {
      if (isEdit) {
        child.add(getQuizChoices());
        addEditOptions(child);
      }
    }

    child.add(getPrivacyChoices());

    addWarningField(child);

    Scheduler.get().scheduleDeferred(() -> titleBox.box.setFocus(true));
  }

  private void addQuizOptions(Panel child) {
    child.add(getQuizChoices());

    createQuizOptions = new DivWidget();
    styleQuizOptions(createQuizOptions);
    checkQuizOptionsVisible();
    child.add(createQuizOptions);

    createQuizOptions.add(getQuizChoices(true));
  }

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

  private List<ListBox> allUnitChapter;

  @NotNull
  private Grid getQuizChoices(boolean isCreate) {
    Grid grid = new Grid(isCreate ? 4 : 2, 4);


    int col = 0;
    int row = 0;

    if (isCreate) {
      ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
      List<String> typeOrder = projectStartupInfo.getTypeOrder();

      if (typeOrder.size() > 2) typeOrder = typeOrder.subList(0, 2);
      for (String type : typeOrder) {
        grid.setWidget(row, col++, getLabel(type));
      }

      row++;
      col = 0;
      List<ListBox> all = new ArrayList<>();
      allUnitChapter = all;
      boolean first = true;
      for (String type : typeOrder) {
        ListBox listBox = getListBox(100);
        listBox.addChangeHandler(event -> gotChangeFor(type, listBox, all));
        all.add(listBox);
        grid.setWidget(row, col++, listBox);
        listBox.addItem(ALL);

        if (first) {
          addSorted(projectStartupInfo, listBox);
          first = false;
        }

      }


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

  private void addSorted(ProjectStartupInfo projectStartupInfo, ListBox listBox) {
    List<String> items = new ArrayList<>();
    projectStartupInfo.getSectionNodes().forEach(sectionNode -> items.add(sectionNode.getName()));
    new ItemSorter().getSortedItems(items).forEach(listBox::addItem);
  }

  private void gotChangeFor(String type, ListBox listBox, List<ListBox> all) {
    int i = all.indexOf(listBox);
    int nextIndex = i + 1;
    if (nextIndex < all.size()) {
      ListBox nextBox = all.get(nextIndex);

      String current = listBox.getSelectedValue();
      Pair pair = new Pair(type, current);

      ArrayList<Pair> pairs = new ArrayList<>();
      pairs.add(pair);
      controller.getExerciseService().getTypeToValues(new FilterRequest(0, pairs, -1),
          new AsyncCallback<FilterResponse>() {
            @Override
            public void onFailure(Throwable caught) {
              if (caught instanceof DominoSessionException) {
                logger.info("getTypeToValues : got " + caught);
              }
              controller.handleNonFatalError("no type to values", caught);
            }

            /**
             * fixes downstream selections that no longer make sense.
             * @param response
             */
            @Override
            public void onSuccess(FilterResponse response) {
              Map<String, Set<MatchInfo>> typeToValues = response.getTypeToValues();
              // logger.info("got " + typeToValues);
              List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();
              //   logger.info("got " + typeOrder );
              String key = typeOrder.get(nextIndex);
              // logger.info("key " + key );
              Set<MatchInfo> matchInfos = typeToValues.get(key);
              nextBox.clear();
              nextBox.addItem(ALL);
              if (matchInfos != null) {
                matchInfos.forEach(matchInfo -> nextBox.addItem(matchInfo.getValue()));
              }
            }
          });

    }
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
    if (current != null) {
      logger.info("got " + current.getID() + " " + current.shouldShowAudio());
      w.setValue(current.shouldShowAudio());
    } else logger.warning("getPlayAudioCheck no current list?");
    return w;
  }

  private boolean isEditing() {
    return current != null;
  }

  @NotNull
  private HTML getDurationLabel() {
    return getLabel(DURATION_MINUTES);
  }

  @NotNull
  private HTML getLabel(String html) {
    HTML w = new HTML(html);
    w.addStyleName("leftFiveMargin");
    w.setWidth("100px");
    return w;
  }

  @NotNull
  private HTML getQuizSizeLabel() {
    return getEditableLabel(QUIZ_SIZE);
  }

  @NotNull
  private HTML getEditableLabel(String quizSize) {
    HTML quiz_size = getLabel(quizSize);
    quiz_size.setVisible(current == null);
    return quiz_size;
  }

  private ListBox getSizeChoices() {
    ListBox w = getListBox();
    for (int i = MIN_QUIZ_SIZE; i < MAX_QUIZ_SIZE; i += 10) {
      w.addItem("" + i);
    }
    w.setSelectedValue("" + DEFAULT_QUIZ_SIZE);
    w.addChangeHandler(event -> gotListSelection(w.getValue()));
    w.setVisible(current == null);

    return w;
  }

  private ListBox getDurationChoices() {
    ListBox w = getListBox();

    for (int i = MIN_DURATION; i < MAX_DURATION; i++) {
      w.addItem("" + i);
    }
    w.setSelectedValue("" + (isEditing() ? (duration = current.getDuration()) : DEFAULT_DURATION));
    w.addChangeHandler(event -> gotDurationSelection(w.getValue()));
    return w;
  }

  private ListBox getMinScoreChoices() {
    ListBox w = getListBox();
    for (int i = MIN_SCORE; i < MAX_SCORE; i += 10) {
      w.addItem("" + i);
    }

    w.setSelectedValue("" + (isEditing() ? (minScore = current.getMinScore()) : DEFAULT_MIN_SCORE));

    w.addChangeHandler(event -> gotListSelection3(w.getValue()));
    return w;
  }

  @NotNull
  private ListBox getListBox() {
    return getListBox(LIST_WIDTH);
  }

  @NotNull
  private ListBox getListBox(int width) {
    ListBox w = new ListBox();
    w.setWidth(width + "px");
    w.addStyleName("topFiveMargin");
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

/*    if (!madeSelection && false) {
      duration = Math.max(1, quizSize / 10);

      Scheduler.get().scheduleDeferred(() ->
          {
            durationList.setSelectedValue("" + duration);
            durationList.setVisible(false);
            //  logger.info("2 duration sel " + durationList.getSelectedIndex());
            //   durationList.setVisible(true);
          }
      );
      Scheduler.get().scheduleDeferred(() ->
          {
            // logger.info("3 duration sel " + durationList.getSelectedIndex());
            durationList.setVisible(true);
          }
      );
//      logger.info("1 duration sel " + durationList.getSelectedIndex());
    }*/
    //   logger.info("got " + quizSize);
  }

  private void gotDurationSelection(String value) {
    duration = Integer.parseInt(value);
  }

  private void gotListSelection3(String value) {
    minScore = Integer.parseInt(value);
  }

  private DivWidget createQuizOptions, editQuizOptions;

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


  @NotNull
  private Widget getQuizChoices() {
    FluidRow row = new FluidRow();

    logger.info("getQuizChoices edit = " +isEdit);
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
  private Widget getPrivacyChoices() {
    FluidRow row = new FluidRow();

    publicChoice = new RadioButton(PUBLIC_PRIVATE_GROUP, PUBLIC);
    publicChoice.addClickHandler(event -> gotClickOnPublic());
    RadioButton radioButton2 = new RadioButton(PUBLIC_PRIVATE_GROUP, PRIVATE);
    privateChoice = radioButton2;
    privateChoice.addClickHandler(event -> gotClickOnPrivate());
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
    modeDep.setVisible(isQuiz);
  }

  private void gotClickOnPrivate() {
    modeDep.setVisible(false);
  }

  private boolean getDefaultPrivacy() {
    boolean isPrivate = controller.getUserState().getCurrent().isStudent();
    if (isEditing()) isPrivate = current.isPrivate();
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
    addUserList(titleBox, area, classBox, publicRadio.getValue(), getListType());
  }

  @NotNull
  private UserList.LIST_TYPE getListType() {
    boolean canMakeQuiz = canMakeQuiz();
    return canMakeQuiz && isQuiz ? UserList.LIST_TYPE.QUIZ : UserList.LIST_TYPE.NORMAL;
  }

  private boolean canMakeQuiz() {
    if (isEditing() && current.isFavorite()) {
      return false;
    } else {
      Collection<User.Permission> permissions = controller.getPermissions();
      return permissions.contains(User.Permission.TEACHER_PERM) || permissions.contains(User.Permission.PROJECT_ADMIN);
    }
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
      markError(titleBox, NAME_ALREADY_USED);
      ret = false;
    } else if ((!publicChoice.getValue() && !privateChoice.getValue())) {
      markErrorBlur(publicPrivateGroup,
          publicChoice,
          "",
          PLEASE_MARK_EITHER_PUBLIC_OR_PRIVATE,
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
   * @paramx duration
   * @see #gotCreate
   */
  private void addUserList(final FormField titleBox, TextArea area, FormField classBox, boolean isPublic,
                           UserList.LIST_TYPE listType) {
    final String safeText = titleBox.getSafeText();
    //  logger.info("addUserList " + safeText);
    Map<String, String> unitToChapter = new LinkedHashMap<>();
    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();


    if (allUnitChapter != null) {
      allUnitChapter.forEach(listBox -> unitToChapter.put(typeOrder.get(unitToChapter.size()), listBox.getSelectedValue()));
      // logger.info("addUserList " + unitToChapter);
    }
    controller.getListService().addUserList(
        safeText,
        sanitize(area.getText()),
        classBox.getSafeText(),
        isPublic,
        listType,
        quizSize,
        duration,
        minScore,
        playAudio,
        unitToChapter,
        new AsyncCallback<UserList>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new list", caught);
          }

          @Override
          public void onSuccess(UserList result) {
            if (result == null) {
              markError(titleBox, "You already have a list named " + safeText);
            } else {
              //   logger.info("addUserList onSuccess " + result);
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