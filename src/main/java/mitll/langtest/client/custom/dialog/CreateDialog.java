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

import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.client.custom.userlist.ListView;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.INameable;
import mitll.langtest.shared.custom.IPublicPrivate;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.dialog.CreateDialogDialog.NO_SELECTION;

public abstract class CreateDialog<T extends INameable & IPublicPrivate> extends BasicDialog {
  private final Logger logger = Logger.getLogger("CreateDialog");

  private static final int TYPE_AND_VALUE_WIDTH = 220;
  private static final boolean DEBUG = false;

  static final String CREATE_NEW_LIST = "Create New List";
  static final String TEXT_BOX = "TextBox";
  private static final int DESC_WIDTH = 455;

  private static final String ALL = "All";
  private static final int LIST_WIDTH = 60;
  private static final String PLEASE_FILL_IN_A_TITLE = "Please fill in a title";


  private static final String PLEASE_MARK_EITHER_PUBLIC_OR_PRIVATE = "Please mark either public or private.";
  private static final String NAME_ALREADY_USED = "Name already used. Please choose another.";
  /**
   * @see #getPrivacyChoices
   */
  private static final String PUBLIC = "Public";
  private static final String PRIVATE = "Private";
  private static final String KEEP_LIST_PUBLIC_PRIVATE = "Keep Public/Private?";
  private static final String PUBLIC_PRIVATE_GROUP = "Public_Private_Group";

  private static final String TITLE = "Title";

  protected final ExerciseController<?> controller;

  private final Set<String> names;

  final boolean isEdit;
  FormField titleBox;
  RadioButton publicChoice;
  private RadioButton privateChoice;
  protected KeyPressHelper enterKeyButtonHelper;
  /**
   * @see #getUnitAndChapterSelections()
   */
  List<ListBox> allUnitChapter;
  List<ControlGroup> allUnitChapterGroup;
  TextArea theDescription;
  private final T current;
  final CreateComplete<T> listView;

  private ControlGroup publicPrivateGroup;

  /**
   *
   * @param current
   * @param names
   * @param isEdit
   * @param controller
   * @param listView
   */
  CreateDialog(T current, Set<String> names,
               boolean isEdit, ExerciseController controller,
               CreateComplete<T> listView
  ) {
    this.current = current;
    logger.info("CreateDialog Current " +current);
    this.names = names;
    this.isEdit = isEdit;
    this.controller = controller;
    this.listView = listView;
  }

  private DialogHelper dialogHelper;

  void setDialogHelper(DialogHelper dialogHelper) {
    this.dialogHelper = dialogHelper;
  }

  void checkEnter(KeyUpEvent event) {
    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
      gotEnter();
    }
  }

  private void gotEnter() {
    if (isOKToCreate()) {
      dialogHelper.close();
    }
  }

  boolean isOKToCreate() {
    if (isOKToCreate(names)) {
      doCreate();
      return true;
    } else {
      //logger.info("doAdd dialog not valid ");
      return false;
    }
  }

  private interface TypeToValuesCompleted {
    void completed();
  }

  private void gotChangeFor(String type,
                            ListBox listBox,
                            List<ListBox> all,
                            boolean addAll,
                            TypeToValuesCompleted completed) {
    int i = all.indexOf(listBox);
    int nextIndex = i + 1;
    if (nextIndex < all.size()) {
      ListBox nextBox = all.get(nextIndex);

      Pair pair = new Pair(type, listBox.getSelectedValue());
      List<Pair> pairs = new ArrayList<>();
      pairs.add(pair);

      controller.getExerciseService().getTypeToValues(new FilterRequest(0, pairs, -1),
          new AsyncCallback<FilterResponse>() {
            @Override
            public void onFailure(Throwable caught) {
              if (caught instanceof DominoSessionException) {
                logger.info("getTypeToValues : got " + caught);
              }
              controller.handleNonFatalError("no type to values", caught);
              completed.completed();
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
              if (addAll) {
                nextBox.addItem(ALL);
              }
              if (matchInfos != null) {
                matchInfos.forEach(matchInfo -> nextBox.addItem(matchInfo.getValue()));
              }
              completed.completed();
            }
          });
    }
  }

  protected abstract void doCreate();

  /**
   * @param currentSelection
   * @param container
   * @see ContentEditorView#doEdit
   */
  public abstract void doEdit(T currentSelection, ButtonMemoryItemContainer<T> container);

  /**
   * @param thirdRow
   * @seex
   * @seex mitll.langtest.client.custom.Navigation#getNavigation
   * @see ListView#doAdd
   */
  public void doCreate(Panel thirdRow) {
    thirdRow.clear();
    Panel child = addEnterKeyBinding();

//    logger.info("doCreate on " + thirdRow.getClass());

    thirdRow.add(child);
    zeroPadding(child);
    child.addStyleName("userListContainer");

    {
      FluidRow row = new FluidRow();
      child.add(row);
      titleBox = addTitle(row);
    }

    addDescription(child);

    addFieldsBelowDescription(child);
  }

  protected void addFieldsBelowDescription(Panel child) {

  }

  /**
   * @param names
   * @return
   * @see ListView#doAdd
   */
  protected boolean isOKToCreate(Set<String> names) {
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

  boolean isEditing() {
    return current != null;
  }

  public T getCurrent() {
    return current;
  }

  @NotNull
  protected HTML getLabel(String html) {
    HTML w = new HTML(html);
    w.addStyleName("leftFiveMargin");
    w.setWidth("100px");
    return w;
  }

  @NotNull
  ListBox getListBox() {
    return getListBox(LIST_WIDTH);
  }

  @NotNull
  ListBox getListBox(int width) {
    ListBox w = new ListBox();
    w.setWidth(width + "px");
    w.addStyleName("topFiveMargin");
    return w;
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

  private void zeroPadding(Panel createContent) {
    createContent.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    createContent.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  /**
   * @return
   * @see #isOKToCreate
   */
  boolean isValidName() {
    return validateCreateList(titleBox);
  }

  /**
   * @return
   */
  @NotNull
  Widget getPrivacyChoices() {
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

    Panel row = new FlowPanel();
    row.add(publicPrivateGroup = addControlGroupEntry(row, KEEP_LIST_PUBLIC_PRIVATE, hp, ""));
    return row;
  }

  protected abstract void gotClickOnPublic();

  protected abstract void gotClickOnPrivate();

  /**
   * @param projectStartupInfo
   * @param listBox
   * @see #getQuizChoices(boolean)
   */
  private void addSorted(ProjectStartupInfo projectStartupInfo, ListBox listBox) {
    List<String> items = new ArrayList<>();
    projectStartupInfo.getSectionNodes().forEach(sectionNode -> {
      String name = sectionNode.getName();
      if (name.isEmpty()) {
        logger.warning("huh? section node name is empty for " + sectionNode);
      } else {
        items.add(name);
      }
    });
    logger.info("addSorted "+items);
    new ItemSorter().getSortedItems(items).forEach(listBox::addItem);
  }

  void moveFocusToTitleLater() {
    Scheduler.get().scheduleDeferred(() -> titleBox.box.setFocus(true));
  }

  protected FormField addTitle(FluidRow row) {
    FormField titleBox = addControlFormField(row, "", "", getMaxTitleLength(), getTitleWidth());
    titleBox.getWidget().getElement().getStyle().setMarginTop(10, Style.Unit.PX);

    final TextBoxBase box = titleBox.box;
    titleBox.setHint(TITLE);
    if (isEditing()) box.setText(getCurrent().getName());
    box.getElement().setId("CreateListDialog_Title");
    box.addKeyUpHandler(this::checkEnter);
    box.addBlurHandler(event -> controller.logEvent(box, TEXT_BOX, CREATE_NEW_LIST, "Title = " + box.getValue()));
    return titleBox;
  }

  protected int getTitleWidth() {
    return -1;
  }

  protected int getMaxTitleLength() {
    return 30;
  }

  protected void addDescription(Panel child) {
    FluidRow row;
    row = new FluidRow();
    child.add(row);

    theDescription = new TextArea();
    theDescription.setWidth(DESC_WIDTH + "px");

    theDescription.setPlaceholder(getDescriptionOptional() + " (optional)");
    theDescription.addKeyUpHandler(this::checkEnter);

    setDescription(getCurrent());
//    if (isEditing()) theDescription.setText(getCurrent().getDescription());
    final FormField description = getSimpleFormField(row, "", theDescription, 1, "");

    description.box.getElement().setId("CreateListDialog_Description");
    description.box.addBlurHandler(event -> controller.logEvent(description.box, TEXT_BOX, CREATE_NEW_LIST, "Description = " + description.box.getValue()));
  }

  protected abstract void setDescription(T current);

  @NotNull
  protected abstract String getDescriptionOptional();


  private boolean getDefaultPrivacy() {
    boolean isPrivate = true;
    if (isEditing()) {
      //    T current = getCurrent();
//      logger.info("current " + current);
//      logger.info("priv    " + current.isPrivate());

      isPrivate = getCurrent().isPrivate();
    }
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

  String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  /**
   * @return
   * @see CreateListDialog#addUserList
   */
  @NotNull
  Map<String, String> getUnitAndChapterSelections() {
    Map<String, String> unitToChapter = new LinkedHashMap<>();
    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();

    if (allUnitChapter != null) {
      allUnitChapter.forEach(listBox -> unitToChapter.put(typeOrder.get(unitToChapter.size()), listBox.getSelectedValue()));
      logger.info("getUnitAndChapterSelections " + unitToChapter);
    }
    return unitToChapter;
  }

  protected class RowAndCol {
    private final Grid grid;
    private int col;
    private int row;

    RowAndCol(Grid grid, int col, int row) {
      this.grid = grid;
      this.col = col;
      this.row = row;
    }

    public int getCol() {
      return col;
    }

    public int getRow() {
      return row;
    }

    /**
     * @param addAll
     * @param unit
     * @param chapter
     * @return
     */
    public RowAndCol invoke(boolean addAll, String unit, String chapter) {
      ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
      List<String> typeOrder = projectStartupInfo.getTypeOrder();

      if (typeOrder.size() > 2) typeOrder = typeOrder.subList(0, 2);
      for (String type : typeOrder) {
        grid.setWidget(row, col++, getLabel(type));
      }

      logger.info("invoke " +
          "\n\tunit    " + unit +
          "\n\tchapter " + chapter);

      row++;
      col = 0;
      List<ListBox> all = new ArrayList<>();
      allUnitChapter = all;
      allUnitChapterGroup = new ArrayList<>();

      boolean first = true;

      for (String type : typeOrder) {
        ListBox listBox = getListBox(TYPE_AND_VALUE_WIDTH);
        listBox.addChangeHandler(event -> gotChangeFor(type, listBox, all, addAll, () ->
            {
            }// logger.info("OK, type->values done for " + type)
        ));
        all.add(listBox);

        ControlGroup e = new ControlGroup();
        e.add(listBox);

        allUnitChapterGroup.add(e);

        grid.setWidget(row, col++, e);

        if (addAll) {
          listBox.addItem(ALL);
        } else {
          listBox.addItem(NO_SELECTION + " Please Choose a " + type + " --");
        }

        if (first) {
          addSorted(projectStartupInfo, listBox);
          first = false;
        }
        int i1 = typeOrder.indexOf(type);
        logger.info("invoke For " + type + " : " + i1);

        if (unit != null && i1 == 0) {
          logger.info("invoke For " + type + " : " + i1 + " select unit " + unit );

          listBox.setSelectedValue(unit);
        }
      }


      if (chapter != null && typeOrder.size() > 1) {
        String type = typeOrder.get(0);
        gotChangeFor(type, allUnitChapter.get(0), all, addAll, () -> {
          //  logger.info("OK, type->values done for " + type);
          setChapter(chapter, allUnitChapter.get(1));
        });
      }

      return this;
    }
  }

  /**
   * Gotta wait until type->values has come back.
   *
   * @param chapter
   * @param listBox
   */
  private void setChapter(String chapter, ListBox listBox) {
    if (DEBUG) logger.info("setChapter chapter has " + listBox.getVisibleItemCount() + " choices...");
    listBox.setSelectedValue(chapter);
    String selectedValue = listBox.getSelectedValue();

    if (selectedValue == null) {
      int itemCount = listBox.getItemCount();
      if (DEBUG) logger.info("invoke no " + chapter + " in " + selectedValue + " : " + itemCount);
      for (int i = 0; i < itemCount; i++) {
        String value = listBox.getValue(i);
        if (DEBUG) logger.info("invoke (" + i + ") no " + chapter + " in " + value);
      }
    } else {
      if (DEBUG) logger.info("invoke no " + chapter + " selected is " + selectedValue);
    }
  }
}
