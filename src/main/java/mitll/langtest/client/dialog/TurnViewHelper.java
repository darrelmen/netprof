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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.ITurnPanel;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

public abstract class TurnViewHelper<T extends ITurnPanel>
    extends DialogView
    implements ContentView, ITurnContainer<T> {
  private final Logger logger = Logger.getLogger("TurnViewHelper");

  private static final String ENGLISH_SPEAKER = "English Speaker";

  private static final String INTERPRETER = "Interpreter";
  static final String SPEAKER_A = "A";
  static final String SPEAKER_B = "B";
  private static final int INTERPRETER_WIDTH = 165;//235;
  private static final int PADDING_LOZENGE = 14;

  private static final String MIDDLE_COLOR = "#00800059";
  private static final String RIGHT_BKG_COLOR = "#4aa8eeb0";
  private static final String LEFT_COLOR = "#e7e6ec";

  protected final ExerciseController controller;

  protected INavigation.VIEWS prev, next;
  private INavigation.VIEWS thisView;
  protected IDialog dialog;
  final List<T> allTurns = new ArrayList<>();
  DivWidget dialogHeader, speakerRow;

  /**
   *
   */
  boolean isInterpreter = false;
  DivWidget turnContainer;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_NEXT = false;


  TurnViewHelper(ExerciseController controller, INavigation.VIEWS thisView) {
    this.controller = controller;
    this.thisView = thisView;
    this.prev = thisView.getPrev();
    this.next = thisView.getNext();
  }

  @NotNull
  protected INavigation.VIEWS getPrevView() {
    return prev;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return next;
  }

  /**
   * @param listContent
   * @param instanceName IGNORED HERE
   * @see NewContentChooser#showView(INavigation.VIEWS, boolean, boolean)
   */
  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    clearTurnLists();
    showContentForDialogInURL(listContent);
  }

  private void showContentForDialogInURL(Panel listContent) {
    int dialogFromURL = getDialogFromURL();
    controller.getDialogService().getDialog(dialogFromURL, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting a dialog", caught);
      }

      @Override
      public void onSuccess(IDialog dialog) {
        logger.info("showContent Got back dialog " + dialog);
        showDialogGetRef(dialogFromURL, dialog, listContent);
      }
    });
  }


  @Override
  public boolean isInterpreter() {
    return dialog.getKind() == DialogType.INTERPRETER;
  }

  public IDialog getDialog() {
    return dialog;
  }

  public void setDialog(IDialog dialog) {
    this.dialog = dialog;
  }

  protected int getDialogID() {
    return dialog == null ? -1 : dialog.getID();
  }

  protected void clearTurnLists() {
    allTurns.clear();
  }

  protected int getDialogFromURL() {
    return new SelectionState().getDialog();
  }

  /**
   * @param dialogID can be -1 if we just jump into a rehearse view without choosing a dialog first...
   * @param dialog
   * @param child
   * @see #showContent
   */
  void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    this.dialog = dialog;
    if (DEBUG) logger.info("showDialogGetRef : show dialog " + dialogID);
    if (dialog != null) {
      isInterpreter = dialog.getKind() == DialogType.INTERPRETER;
      showDialog(dialogID, dialog, child);

    } else {
      logger.info("showDialogGetRef no dialog for " + dialogID);
      Label child1 = new Label(LabelType.INFO, "Please choose a dialog first under Dialogs.");
      child1.getElement().getStyle().setFontSize(16, PX);
      child1.addStyleName("topFiveMargin");
      child1.addStyleName("floatLeft");
      child1.getElement().getStyle().setClear(Style.Clear.BOTH);
      child.add(child1);

      {
        Button widgets = new Button("Click here to choose a dialog", IconType.EYE_OPEN);
        widgets.setType(ButtonType.INFO);
        widgets.addClickHandler(event -> gotJumpToDialog());
        widgets.addStyleName("leftFiveMargin");
        widgets.addStyleName("topFiveMargin");
        widgets.addStyleName("floatLeft");
        widgets.getElement().getStyle().setClear(Style.Clear.BOTH);
        child.add(widgets);
      }
    }
  }

  private void gotJumpToDialog() {
    controller.getNavigation().showView(INavigation.VIEWS.DIALOG);
  }

  /**
   * @param exid
   * @return
   * @see DialogEditor#getNextTurn(int)
   */
  T getTurnByID(int exid) {
    List<T> collect = allTurns.stream().filter(turn -> turn.getExID() == exid).collect(Collectors.toList());
    return collect.isEmpty() ? null : collect.get(0);
  }

  void removeFromContainer(T toRemove) {
    boolean remove = turnContainer.remove(toRemove);
    if (!remove) {
      logger.warning("deleteTurn : didn't remove turn " + toRemove);
    }
  }

  void makeVisible(T currentTurn) {
    currentTurn.makeVisible();
  }

  /**
   * @param currentTurn
   * @see #beforeChangeTurns()
   */
  void makeVisible(UIObject currentTurn) {
    if (currentTurn == null) {
      logger.info("makeVisible: no current turn...");
    } else {
      Element element = currentTurn.getElement();
      element.scrollIntoView();
    }
  }

  /**
   * Main method for showing the three sections
   * <p>
   * NOTE: ASSUMES the english speaker goes first!
   *
   * @param dialogID
   * @param dialog
   * @param child
   * @see #showDialogGetRef
   */
  protected void showDialog(int dialogID, IDialog dialog, Panel child) {
    if (dialog == null) {
      child.add(new HTML("hmmm can't find dialog #" + dialogID + " in database"));
    } else {
      child.clear();  // dangerous???

      addDialogHeader(dialog, child);

      DivWidget controlAndSpeakers = new DivWidget();
      styleControlRow(controlAndSpeakers);

      {
        DivWidget outer = new DivWidget();
        outer.addStyleName("inlineFlex");
        outer.setWidth("100%");

        {
          DivWidget controls = getControls();
          controls.setWidth("100%");

          // only if flags
          outer.add(controls);
        }
        controlAndSpeakers.add(outer);
      }

      controlAndSpeakers.add(speakerRow = getSpeakerRow(dialog));

      child.add(controlAndSpeakers);

      child.add(getTurns(dialog));

      child.add(addEditorButton());
    }
  }

  DivWidget getControls() {
    return new DivWidget();
  }

  @NotNull
  private DivWidget addEditorButton() {
    return new DivWidget();
  }

  /**
   * @param dialog
   * @param child
   * @see #showDialog(int, IDialog, Panel)
   */
  protected void addDialogHeader(IDialog dialog, Panel child) {
    child.add(dialogHeader = new DialogHeader(controller, thisView, getPrevView(), getNextView()).getHeader(dialog));
  }

  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("speakerRow");

    Style style = rowOne.getElement().getStyle();
    style.setMarginTop(5, PX);
    style.setOverflow(Style.Overflow.HIDDEN);

    {
      String firstSpeakerLabel = isInterpreter ? getFirstSpeakerLabel(dialog) : ListenViewHelper.SPEAKER_A;
      rowOne.add(getLeftSpeaker(firstSpeakerLabel));
    }

    {
      String secondSpeakerLabel = isInterpreter ? getSecondSpeakerLabel(dialog) : ListenViewHelper.SPEAKER_B;
      rowOne.add(getRightSpeaker(secondSpeakerLabel));
    }

    {
      DivWidget middleSpeaker = getMiddleSpeaker();
      if (!isInterpreter) {
        middleSpeaker.setHeight("5px");
        middleSpeaker.setVisible(false);
      }
      rowOne.add(middleSpeaker);
    }

    return rowOne;
  }

  /**
   * TODO : allow english speaker to go second
   *
   * @param dialog
   * @return
   */
  @Nullable
  String getSecondSpeakerLabel(IDialog dialog) {
    String secondSpeaker = dialog.getSpeakers().size() > 2 ? dialog.getSpeakers().get(2) : null;

    // OK guess from the language of the first turn
    if (!dialog.getExercises().isEmpty()) {
      ClientExercise next = dialog.getExercises().iterator().next();
      boolean hasEnglishAttr = next.hasEnglishAttr();

      if (hasEnglishAttr && !getExerciseSpeaker(next).equalsIgnoreCase(secondSpeaker)) {
        secondSpeaker = getProjectLangSpeaker();
      }
    } else if (dialog.getKind() == DialogType.INTERPRETER) {
      secondSpeaker = getProjectLangSpeaker();
    }

    if (secondSpeaker == null) secondSpeaker = ListenViewHelper.SPEAKER_B;
    return secondSpeaker;
  }

  @NotNull
  private String getProjectLangSpeaker() {
    return controller.getLanguageInfo().toDisplay() + " Speaker";
  }

  /**
   * TODO : allow english speaker to go second
   *
   * @param dialog
   * @return
   */
  @NotNull
  String getFirstSpeakerLabel(IDialog dialog) {
    //  logger.info("getFirstSpeakerLabel for dialog " + dialog.getID());
//    dialog.getAttributes().forEach(exerciseAttribute -> logger.info(exerciseAttribute.toString()));
//
//    List<ExerciseAttribute> properties = dialog.getAttributes()
//        .stream()
//        .filter(exerciseAttribute -> (exerciseAttribute.getProperty() != null))
//        .sorted(Comparator.comparing(Pair::getProperty))
//        .collect(Collectors.toList());
//
//    properties.forEach(p -> logger.info(p.toString()));

    String firstSpeaker = dialog.getSpeakers().isEmpty() ? null : dialog.getSpeakers().get(0);

    if (DEBUG) logger.info("getFirstSpeakerLabel first speaker " + firstSpeaker);

    if (!dialog.getExercises().isEmpty()) {
      ClientExercise next = dialog.getExercises().iterator().next();
      boolean hasEnglishAttr = next.hasEnglishAttr();

      if (hasEnglishAttr &&
          (firstSpeaker == null || getExerciseSpeaker(next).equalsIgnoreCase(firstSpeaker))) {
        firstSpeaker = ENGLISH_SPEAKER;
      }
    } else if (dialog.getKind() == DialogType.INTERPRETER) {
      firstSpeaker = ENGLISH_SPEAKER;
    }

    if (firstSpeaker == null) firstSpeaker = SPEAKER_A;
    if (DEBUG) {
      logger.info("getFirstSpeakerLabel 2 " +
          "first speaker " + firstSpeaker);
    }
    return firstSpeaker;
  }

  @NotNull
  private DivWidget getMiddleSpeaker() {
    Heading w = new Heading(4, INTERPRETER);

    DivWidget middle = new DivWidget();
    middle.addStyleName("bubble");
    middle.setWidth(INTERPRETER_WIDTH + "px");
    middle.setHeight("44px");
    middle.getElement().getStyle().setMarginTop(0, PX);
    middle.getElement().getStyle().setMarginBottom(0, PX);
    middle.getElement().getStyle().setProperty("marginLeft", "auto");
    middle.getElement().getStyle().setProperty("marginRight", "auto");
    setPadding(middle);
    middle.add(w);
    styleLabel(w);

    middle.getElement().getStyle().setBackgroundColor(MIDDLE_COLOR);
    return middle;
  }

  @NotNull
  protected DivWidget getRightSpeaker(String secondSpeaker) {
    Heading w = new Heading(4, secondSpeaker);

    DivWidget right = new DivWidget();
    // right.setWidth(SPEAKER_WIDTH +            "px");
    right.add(w);
    right.addStyleName("bubble");
    right.addStyleName("rightbubble");
    right.addStyleName("floatRight");
    setPadding(right);
    //left.addStyleName("leftFiveMargin");
    right.getElement().getStyle().setBackgroundColor(RIGHT_BKG_COLOR);
    styleRightSpeaker(w);
    return right;
  }

  @NotNull
  protected DivWidget getLeftSpeaker(String firstSpeaker) {
    Heading w = new Heading(4, firstSpeaker);

    DivWidget left = new DivWidget();
    left.add(w);
    // left.setWidth(SPEAKER_WIDTH +            "px");

    styleLeftSpeaker(w);

    left.addStyleName("floatLeft");
    left.addStyleName("bubble");
    left.addStyleName("leftbubble");
    setPadding(left);

    //left.addStyleName("leftFiveMargin");
    left.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

    return left;
  }

  private void setPadding(DivWidget right) {
    right.getElement().getStyle().setPaddingLeft(PADDING_LOZENGE, PX);
    right.getElement().getStyle().setPaddingRight(PADDING_LOZENGE, PX);
  }

  private String getExerciseSpeaker(ClientExercise next) {
    List<ExerciseAttribute> speaker = next
        .getAttributes()
        .stream()
        .filter(attr -> attr.getProperty().equalsIgnoreCase(DialogMetadata.SPEAKER.toString())).collect(Collectors.toList());
    return speaker.isEmpty() ? "" : speaker.get(0).getValue();
  }

  private void styleControlRow(DivWidget rowOne) {
    rowOne.addStyleName("cardBorderShadow");
    Style style = rowOne.getElement().getStyle();
    style.setProperty("position", "sticky");
    style.setTop(0, PX);
    style.setMarginTop(10, PX);
    style.setMarginBottom(10, PX);
    style.setZIndex(1000);
    style.setOverflow(Style.Overflow.HIDDEN);
  }

  private void styleLeftSpeaker(UIObject checkBox) {
    styleLabel(checkBox);
  }

  private void styleRightSpeaker(UIObject checkBox) {
    styleLabel(checkBox);
  }

  private void styleLabel(UIObject checkBox) {
    checkBox.getElement().getStyle().setFontSize(32, PX);
  }

  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  protected DivWidget getTurns(IDialog dialog) {
    TurnViewHelper outer = this;
    DivWidget rowOne = new DivWidget() {
      @Override
      protected void onUnload() {
        super.onUnload();
        outer.onUnload();
      }
    };

    this.turnContainer = rowOne;

    if (DEBUG) logger.info("getTurns : dialog    " + dialog);

    if (DEBUG) logger.info("getTurns : exercises " + dialog.getExercises().size());

    styleTurnContainer(rowOne);

    addAllTurns(dialog, rowOne);

    return rowOne;
  }

  void onUnload() {

  }

  void addAllTurns(IDialog dialog, DivWidget rowOne) {
    String left = getFirstSpeakerLabel(dialog); //speakers.get(0);
    String right = getSecondSpeakerLabel(dialog);//speakers.get(2);
/*    logger.info("for speaker " + left + " got " + speakerToEx.get(left).size());
    logger.info("for speaker " + middle + " got " + middleTurns.size());
    logger.info("for speaker " + right + " got " + speakerToEx.get(right).size());*/

    addTurnPerExercise(dialog, rowOne, left, right);
  }

  protected void styleTurnContainer(DivWidget rowOne) {
    rowOne.getElement().setId("turnContainer");
    rowOne.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    rowOne.getElement().getStyle().setMarginTop(10, PX);
    rowOne.addStyleName("cardBorderShadow");
    rowOne.getElement().getStyle().setMarginBottom(10, PX);
  }

  private void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
    rowOne.clear();
    addTurnForEachExercise(rowOne, left, right, dialog.getExercises());
  }

  protected void addTurnForEachExercise(DivWidget rowOne, String left, String right, List<ClientExercise> exercises) {
    ClientExercise prev = null;
    int index = 0;
    for (ClientExercise clientExercise : exercises) {
      COLUMNS currentCol = getColumnForEx(left, right, clientExercise);
      COLUMNS prevCol = prev == null ? COLUMNS.UNK : getColumnForEx(left, right, prev);
      addTurn(rowOne, clientExercise, currentCol, prevCol, index);
      prev = clientExercise;
      index++;
    }

    // populateColumnTurnLists();

    // addToColumnPanelLists(columns, turn);
  }

//  void populateColumnTurnLists() {
//    allTurns.forEach(turn -> addToColumnPanelLists(
//        (turn.isLeft() ? COLUMNS.LEFT : turn.isRight() ? COLUMNS.RIGHT : COLUMNS.MIDDLE), turn));
//  }

  COLUMNS getColumnForEx(String left, String right, ClientExercise clientExercise) {
    String speaker = clientExercise == null ? "" : clientExercise.getSpeaker();
    if (speaker.isEmpty()) {
      logger.info("getColumnForEx : no speaker, ex = " + clientExercise);
      return COLUMNS.UNK;
    } else {
      return getColumnForSpeaker(left, right, speaker);
    }
  }

  @NotNull
  private COLUMNS getColumnForSpeaker(String left, String right, String speaker) {
    COLUMNS columns;

    if (speaker.equalsIgnoreCase(left) || speaker.equalsIgnoreCase(SPEAKER_A)) {
      columns = COLUMNS.LEFT;
    } else if (speaker.equalsIgnoreCase(right) || speaker.equalsIgnoreCase(SPEAKER_B)) {
      columns = COLUMNS.RIGHT;
    } else {
      columns = COLUMNS.MIDDLE;
    }

    //  logger.info("getColumnForSpeaker : l " + left + " r " + right + " vs " + speaker + " => " + columns);
    return columns;
  }

  /**
   * @param rowOne
   * @param clientExercise
   * @param columns
   * @param index
   * @see #addTurnForEachExercise(DivWidget, String, String, List)
   */
  T addTurn(DivWidget rowOne, ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    T turn = getTurnPanel(clientExercise, columns, prevColumn, index);

    allTurns.add(index, turn);

    rowOne.insert(turn, index);

    return turn;
  }

  @NotNull
  T getTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    T turn = reallyGetTurnPanel(clientExercise, columns, prevColumn, index);
    turn.addWidgets(true, false, PhonesChoices.HIDE, EnglishDisplayChoices.SHOW);
    return turn;
  }

  @NotNull
  protected abstract T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index);

  List<T> getAllTurns() {
    return allTurns;
  }

  protected void report(String prefix, List<T> allTurns) {
    StringBuilder builder = new StringBuilder();
    allTurns.forEach(turn -> builder.append(turn.getExID()).append(", "));
    logger.info("report : " + prefix + " seq " + builder);
  }

  /**
   * @return
   * @see #getSession
   */
  public INavigation.VIEWS getView() {
    return thisView;
  }

  public boolean isLast(T currentTurn) {
    List<T> seq = getAllTurns();
    return seq.indexOf(currentTurn) == seq.size() - 1;
  }

  T getNext(T currentTurn) {
    List<T> seq = getAllTurns();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (DEBUG_NEXT) logger.info("getNext current of " + currentTurn.getExID() + " " + currentTurn.getText() +
        " : " + i + " next " + i1);

    if (i1 > seq.size() - 1) {
      return null;
    } else {
      T widgets = seq.get(i1);
      if (DEBUG_NEXT) logger.info("getNext current at " + i1 + " will be ex #" + widgets.getExID());
      return widgets;
    }
  }

  T getPrev(T currentTurn) {
    List<T> seq = getAllTurns();

    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;
    return i1 < 0 ? null : seq.get(i1);
  }
}
