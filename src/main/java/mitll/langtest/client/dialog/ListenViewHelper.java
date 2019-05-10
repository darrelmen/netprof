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
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.*;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper<T extends ITurnPanel>
    extends DialogView
    implements ContentView, PlayListener, IListenView, ITurnContainer<T> {
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  private static final String ENGLISH_SPEAKER = "English Speaker";

  private static final String INTERPRETER = "Interpreter";
  static final String SPEAKER_A = "A";
  static final String SPEAKER_B = "B";

  private static final int INTERPRETER_WIDTH = 165;//235;
  private static final int PADDING_LOZENGE = 14;

  private static final String MIDDLE_COLOR = "#00800059";


  public enum COLUMNS {LEFT, MIDDLE, RIGHT, UNK}

  private static final String VALUE = "value";
  private static final String SLIDER_MAX = "100";
  private static final String MAX = "max";
  private static final String MIN = "min";


  private static final String SLIDER_MIN = "0";
  private static final String TYPE = "type";
  private static final String RANGE = "range";
  private static final String INPUT = "input";

  private static final String RIGHT_BKG_COLOR = "#4aa8eeb0";
  private static final String LEFT_COLOR = "#e7e6ec";

  final ExerciseController controller;
  final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

  final List<T> allTurns = new ArrayList<>();
  private final List<T> promptTurns = new ArrayList<>();
  final List<T> leftTurnPanels = new ArrayList<>();
  private final List<T> middleTurnPanels = new ArrayList<>();
  private final List<T> rightTurnPanels = new ArrayList<>();

  private T currentTurn;

  private ComplexWidget slider;
  /**
   * @see #setPlayButtonToPlay()
   * @see #setPlayButtonToPause()
   */
  private Button playButton;
  private Button playYourselfButton;

  private boolean doRehearse = true;


  private DivWidget dialogHeader;

  /**
   *
   */
  //protected int dialogID;
  private IDialog dialog;
  boolean isInterpreter = false;

  private INavigation.VIEWS prev, next;
  private INavigation.VIEWS thisView;
  private boolean gotTurnClick = false;
  private int clickedTurn = -1;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PLAY = false;

  /**
   * @param controller
   * @param thisView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public ListenViewHelper(ExerciseController controller, INavigation.VIEWS thisView) {
    this.controller = controller;
    this.thisView = thisView;
    this.prev = thisView.getPrev();
    this.next = thisView.getNext();
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

  public IDialog getDialog() {
    return dialog;
  }

  public void setDialog(IDialog dialog) {
    this.dialog = dialog;
  }

  protected int getDialogID() {
    return dialog == null ? -1 : dialog.getID();
  }

  void clearTurnLists() {
    promptTurns.clear();
    allTurns.clear();
    leftTurnPanels.clear();
    middleTurnPanels.clear();
    rightTurnPanels.clear();
    currentTurn = null;
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
      List<RefAudioGetter> refAudioGetters = new ArrayList<>(allTurns);
      if (DEBUG) logger.info("showDialogGetRef : Get ref audio for " + refAudioGetters.size());
      getRefAudio(refAudioGetters.iterator());
    } else {
      logger.info("showDialogGetRef no dialog for " + dialogID);
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
  private void showDialog(int dialogID, IDialog dialog, Panel child) {
    if (dialog == null) {
      child.add(new HTML("hmmm can't find dialog #" + dialogID + " in database"));
    } else {
      child.clear();  // dangerous???

      addDialogHeader(dialog, child);

      DivWidget controlAndSpeakers = new DivWidget();
      styleControlRow(controlAndSpeakers);
      child.add(controlAndSpeakers);

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

      controlAndSpeakers.add(getSpeakerRow(dialog));

      child.add(getTurns(dialog));

      child.add(addEditorButton());
    }
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

  @NotNull
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("speakerRow");
    rowOne.getElement().getStyle().setMarginTop(5, PX);

    rowOne.add(getLeftSpeaker(getFirstSpeakerLabel(dialog)));
    rowOne.add(getRightSpeaker(getSecondSpeakerLabel(dialog)));

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
  private String getSecondSpeakerLabel(IDialog dialog) {
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

    if (secondSpeaker == null) secondSpeaker = SPEAKER_B;
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
  private String getFirstSpeakerLabel(IDialog dialog) {
    //  logger.info("getFirstSpeakerLabel for dialog " + dialog.getID());
    //  dialog.getAttributes().forEach(exerciseAttribute -> logger.info(exerciseAttribute.toString()));

    List<ExerciseAttribute> properties = dialog.getAttributes()
        .stream()
        .filter(exerciseAttribute -> (exerciseAttribute.getProperty() != null))
        .sorted(Comparator.comparing(Pair::getProperty))
        .collect(Collectors.toList());

    // properties.forEach(p -> logger.info(p.toString()));

    String firstSpeaker = dialog.getSpeakers().isEmpty() ? null : dialog.getSpeakers().get(0);

    logger.info("getFirstSpeakerLabel first speaker " + firstSpeaker);
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
    logger.info("getFirstSpeakerLabel 2 " +
        "first speaker " + firstSpeaker);
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
  private DivWidget getRightSpeaker(String secondSpeaker) {
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
  private DivWidget getLeftSpeaker(String firstSpeaker) {
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

  @NotNull
  DivWidget getLeftSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    rightDiv.add(checkBox);
    return rightDiv;
  }

  CheckBox addRightSpeaker(DivWidget rowOne, String label) {
    CheckBox checkBox = new CheckBox(label, true);

    // setRightTurnInitialValue(checkBox);
    styleRightSpeaker(checkBox);

    //   checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));

    rowOne.add(getRightSpeakerDiv(checkBox));
    return checkBox;
  }

  private void styleRightSpeaker(UIObject checkBox) {
    styleLabel(checkBox);
  }

  private void styleLabel(UIObject checkBox) {
    checkBox.getElement().getStyle().setFontSize(32, PX);
  }

  @NotNull
  DivWidget getRightSpeakerDiv(CheckBox checkBox) {
    DivWidget rightDiv = new DivWidget();
    rightDiv.add(checkBox);
    return rightDiv;
  }

//  private void setLeftTurnSpeakerInitial(CheckBox checkBox) {
//    checkBox.setValue(true);
//  }
//
//  void setRightTurnInitialValue(CheckBox checkBox) {
//    checkBox.setValue(true);
//  }

//  void speakerOneCheck(Boolean value) {
//    if (!value && !isRightSpeakerSelected()) {
//      setRightSpeaker();
//    }
//  }

//  private void setRightSpeaker() {
//    setRightSpeaker(true);
//  }

//  void setRightSpeaker(boolean value) {
//    if (rightSpeakerBox != null) {
//      rightSpeakerBox.setValue(value);
//    }
//  }
//
//  void speakerTwoCheck(Boolean value) {
//    if (!value && !isLeftSpeakerSelected()) {
//      selectLeftSpeaker();
//    }
//  }
//
//  private void selectLeftSpeaker() {
//    setLeftSpeaker(true);
//  }

//  void setLeftSpeaker(boolean val) {
//    if (leftSpeakerBox != null)
//      leftSpeakerBox.setValue(val);
//  }
//
//  private Boolean isLeftSpeakerSelected() {
//    return leftSpeakerBox == null || leftSpeakerBox.getValue();
//  }
//
//  private Boolean isRightSpeakerSelected() {
//    return rightSpeakerBox == null || rightSpeakerBox.getValue();
//  }

  DivWidget turnContainer;

  /**
   * @param dialog
   * @return
   * @see #showDialog
   */
  @NotNull
  DivWidget getTurns(IDialog dialog) {
    DivWidget rowOne = new DivWidget();

    this.turnContainer = rowOne;

    logger.info("getTurns : dialog    " + dialog);

    if (DEBUG) logger.info("getTurns : exercises " + dialog.getExercises().size());

    styleTurnContainer(rowOne);

    addAllTurns(dialog, rowOne);

    markFirstTurn();

    return rowOne;
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

  private void addTurnForEachExercise(DivWidget rowOne, String left, String right, List<ClientExercise> exercises) {
    ClientExercise prev = null;
    int index = 0;
    for (ClientExercise clientExercise : exercises) {
      COLUMNS currentCol = getColumnForEx(left, right, clientExercise);
      COLUMNS prevCol = prev == null ? COLUMNS.UNK : getColumnForEx(left, right, prev);
      addTurn(rowOne, currentCol, clientExercise, prevCol, index);
      prev = clientExercise;
      index++;
    }
  }

  private COLUMNS getColumnForEx(String left, String right, ClientExercise clientExercise) {
    String speaker = clientExercise == null ? "" : clientExercise.getSpeaker();
    //List<ExerciseAttribute> collect = getSpeakerAttributes(clientExercise);
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

  //
//  private String getSpeaker(List<ExerciseAttribute> collect) {
//    return collect.get(0).getValue();
//  }
//
//  @NotNull
//  private List<ExerciseAttribute> getSpeakerAttributes(ClientExercise clientExercise) {
//    return clientExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(DialogMetadata.SPEAKER.name())).collect(Collectors.toList());
//  }

  /**
   * @param rowOne
   * @param columns
   * @param clientExercise
   * @param index
   * @see #addTurnForEachExercise(DivWidget, String, String, List)
   */
  private void addTurn(DivWidget rowOne, COLUMNS columns, ClientExercise clientExercise, COLUMNS prevColumn, int index) {
    T turn = getTurnPanel(clientExercise, columns, prevColumn, index);

    if (columns == COLUMNS.RIGHT) {
      rightTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.LEFT) {
      leftTurnPanels.add(turn);
      promptTurns.add(turn);
    } else if (columns == COLUMNS.MIDDLE) {
      middleTurnPanels.add(turn);
    }

    allTurns.add(turn);

    rowOne.add(turn);
  }

  /**
   * @param exid
   * @see DialogEditor#deleteCurrentTurnOrPair
   */
  void deleteTurn(int exid) {
    List<T> collect = allTurns.stream().filter(t -> t.getExID() == exid).limit(1).collect(Collectors.toList());
    if (collect.isEmpty()) {
      logger.warning("huh? can't find " + exid);
    } else {
      T toRemove = collect.get(0);

      T currentTurn = getCurrentTurn();

      if (toRemove == currentTurn) {
        logger.info("deleteTurn removing current turn " + currentTurn.getExID());

        T prev = getPrev();
        if (prev == null) {
          T next = getNext();
          logger.info("deleteTurn now next " + (next == null ? " NULL " : next.getExID()));

          setCurrentTurn(next);
        } else {
          logger.info("deleteTurn now prev " + prev.getExID());
          setCurrentTurn(prev);
        }
        T currentTurn1 = getCurrentTurn();
        logger.info("deleteTurn now current turn " + (currentTurn1 == null ? "NULL" : currentTurn1.getExID()));
      }

      allTurns.remove(toRemove);
      leftTurnPanels.remove(toRemove);
      rightTurnPanels.remove(toRemove);
      promptTurns.remove(toRemove);
      middleTurnPanels.remove(toRemove);

      boolean remove = turnContainer.remove(toRemove);
      if (!remove) {
        logger.warning("deleteTurn : didn't remove turn " + toRemove);
      }
    }
  }

  private void markFirstTurn() {
    if (!allTurns.isEmpty()) {
      setCurrentTurn(allTurns.get(0));
      logger.info("markFirstTurn : markCurrent ");
      markCurrent();
      makeVisible(currentTurn);
    }
  }

  /**
   * @param toMakeCurrent
   * @see #markFirstTurn()
   */
  void setCurrentTurn(T toMakeCurrent) {
    this.currentTurn = toMakeCurrent;
  }

  T getCurrentTurn() {
    return currentTurn;
  }

  private void makeVisible(T currentTurn) {
    currentTurn.makeVisible();
  }

  void makeVisible(UIObject currentTurn) {
    if (currentTurn == null) {
      logger.warning("makeVisible: no current turn...");
    } else {
      Element element = currentTurn.getElement();
      element.scrollIntoView();
    }
  }

  private void getRefAudio(final Iterator<RefAudioGetter> iterator) {
    if (iterator.hasNext()) {
      // RefAudioGetter next = iterator.next();
      //logger.info("getRefAudio asking next panel...");

//      if (false) {
//        logger.info("getRefAudio : skip stale req for panel...");
//      } else {
      iterator.next().getRefAudio(() -> {
        if (iterator.hasNext()) {
          //     logger.info("\tgetRefAudio panel complete...");
          //   final int reqid = next.getReq();
          if (true) {
            if (Scheduler.get() != null) {
              Scheduler.get().scheduleDeferred(() -> {
                // if (true) {
                getRefAudio(iterator);
                // }
                //else {
//              /
                // }
              });
            }
          }
        } else {
          //   logger.info("\tgetRefAudio all panels complete...");
        }
      });
    }
    // }
  }

  /**
   * @param isRight
   * @param clientExercise
   * @param prevColumn
   * @param index
   * @return
   * @see #addTurn
   */
  @NotNull
  T getTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    T turn = reallyGetTurnPanel(clientExercise, columns, prevColumn, index);
    turn.addWidgets(true, false, PhonesChoices.HIDE, EnglishDisplayChoices.SHOW);
    if (!turn.addPlayListener(this)) logger.warning("didn't add the play listener...");
    turn.addClickHandler(event -> gotTurnClick(turn));
    return turn;
  }

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param index
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  T reallyGetTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    boolean rightJustify = columns == COLUMNS.MIDDLE &&
        thisView == INavigation.VIEWS.LISTEN && clientExercise.hasEnglishAttr();

    T widgets = makeTurnPanel(clientExercise, columns, prevColumn, rightJustify, index);
    widgets.asWidget().getElement().getStyle().setProperty("tabindex", "" + index);
    return widgets;
  }

  /**
   * TODO how to do this without casting????
   *
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param index
   * @return
   */
  @NotNull
  protected T makeTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, boolean rightJustify, int index) {
    T t = (T) new TurnPanel(
        clientExercise,
        controller,
        null,
        alignments,
        this,
        columns,
        rightJustify);

    return t;
  }

  /**
   * @param turn
   */
  void gotTurnClick(T turn) {
    logger.info("gotTurnClick " + turn.getExID());

    setGotTurnClick(true);
    removeMarkCurrent();
    setCurrentTurn(turn);
    playCurrentTurn();
  }

  private boolean isGotTurnClick() {
    return gotTurnClick;
  }

  void setGotTurnClick(boolean gotTurnClick) {
    this.gotTurnClick = gotTurnClick;
  }

  /**
   * TODO add playback rate
   * <p>
   * Prev, play, next buttons
   *
   * @return
   */
  @NotNull
  DivWidget getControls() {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("controls");
    alignCenter(rowOne);

    {
      Button widgets = new Button("", IconType.BACKWARD, event -> gotBackward());
      widgets.addStyleName("leftFiveMargin");
      rowOne.add(widgets);
    }

    {
      Button widgets1 = new Button("", IconType.PLAY, event -> gotPlay());
      widgets1.setSize(ButtonSize.LARGE);
      widgets1.addStyleName("leftFiveMargin");
      rowOne.add(widgets1);
      playButton = widgets1;
    }

    {
      Button widgets1 = new Button("Yourself", IconType.PLAY, event -> gotPlayYourself());
      widgets1.setActive(false);
      widgets1.setEnabled(false);

      widgets1.setSize(ButtonSize.LARGE);
      widgets1.addStyleName("leftFiveMargin");
      rowOne.add(widgets1);
      playYourselfButton = widgets1;
    }

    {
      Button widgets2 = new Button("", IconType.FORWARD, event -> gotForward(null));
      widgets2.addStyleName("leftFiveMargin");
      rowOne.add(widgets2);
    }

    {
      Icon w = new Icon(IconType.VOLUME_UP);
      w.addStyleName("leftTenMargin");
      rowOne.add(w);
      rowOne.add(slider = getSlider());
    }

    return rowOne;
  }

  private void alignCenter(DivWidget rowOne) {
    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
  }

  /**
   * Gotta make one. Nothing in gwt bootstrap...
   *
   * @return
   */
  @NotNull
  private ComplexWidget getSlider() {
    ComplexWidget input = new ComplexWidget(INPUT);

    input.getElement().setPropertyString(TYPE, RANGE);
    input.getElement().setPropertyString(MIN, SLIDER_MIN);
    input.getElement().setPropertyString(MAX, SLIDER_MAX);
    input.getElement().setPropertyString(VALUE, SLIDER_MAX);
    input.addDomHandler(event -> gotSliderChange(), ChangeEvent.getType());
    input.setWidth("150px");
    input.addStyleName("leftFiveMargin");
    return input;
  }

  private void gotSliderChange() {
    controller.getSoundManager().setVolume(getVolume());
  }

  @Override
  public int getVolume() {
    return slider.getElement().getPropertyInt(VALUE);
  }

  @Override
  public void moveFocusToNext() {
    T next = getNext();
    if (next != null) {
      next.grabFocus();
    } else if (!getAllTurns().isEmpty()) {
      getAllTurns().get(0).grabFocus();
    }
  }

  @Override
  public int getDialogSessionID() {
    return -1;
  }

  @NotNull
  protected INavigation.VIEWS getPrevView() {
    return prev;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return next;
  }

  void gotBackward() {
    setPlayButtonToPlay();

    List<T> seq = getAllTurns();

    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;

    boolean isPlaying = currentTurn.doPause();

    clearHighlightAndRemoveMark();

    if (!makePrevVisible()) {
      //makeVisible(currentTurn);
      //  logger.info("gotBackward make current turn visible " + currentTurn.getExID() + " at " + i);
      makeVisible(seq.get(seq.size() - 1));
      // turnContainer.getElement().setScrollTop(300);
      //   turnContainer.getParent().getElement().setScrollTop(0);
    }

    if (i1 < 0) {
      setCurrentTurn(seq.get(seq.size() - 1));
    } else {
      setCurrentTurn(seq.get(i1));
    }
    markCurrent();


    if (isPlaying) {
      if (getView() == INavigation.VIEWS.LISTEN) {
        playCurrentTurn();
      }
    }
  }


  @Override
  public void gotForward(EditorTurn editorTurn) {
    boolean isPlaying = currentTurn.doPause();

    int i = beforeChangeTurns();

    // maybe do wrap
    {
      int i1 = i + 1;
      List<T> seq = getAllTurns();
      if (i1 > seq.size() - 1) {
        setCurrentTurn(seq.get(0));
      } else {
        setCurrentTurn(seq.get(i1));
      }
    }

    afterChangeTurns(isPlaying);
  }

  //@Override
  public void setCurrentTurnTo(T newTurn) {
    boolean isPlaying = currentTurn.doPause();
    int i = beforeChangeTurns();
    setCurrentTurn(newTurn);
    //   logger.info("setCurrentTurnTo ex #" + currentTurn.getExID());
    afterChangeTurns(isPlaying);
  }

  @Override
  public boolean isInterpreter() {
    return dialog.getKind() == DialogType.INTERPRETER;
  }

  private int beforeChangeTurns() {
    setPlayButtonToPlay();

    int i = getAllTurns().indexOf(currentTurn);

    if (DEBUG) logger.info("beforeChangeTurns " + i + " : " + getExID());

    clearHighlightAndRemoveMark();

    if (!makeNextVisible()) {
      //  logger.info("gotForward : make current turn visible!");
      makeVisible(dialogHeader);  // make the top header visible...
    }
    return i;
  }

  private int getExID() {
    return currentTurn.getExID();// + " : " +currentTurn.getText();
  }

  private void afterChangeTurns(boolean isPlaying) {
    // logger.info("afterChangeTurns isPlaying " + isPlaying);
    markCurrent();
    if (isPlaying) playCurrentTurn();
  }

  /**
   * @see ITurnContainer#gotForward(EditorTurn)
   * @see #gotBackward()
   */
  private void clearHighlightAndRemoveMark() {
    if (DEBUG) logger.info("clearHighlight on " + getExID());

    currentTurn.resetAudio();
    currentTurn.clearHighlight();
    removeMarkCurrent();
  }

  /**
   * @see #getControls
   */
  void gotPlay() {
    setGotTurnClick(false);
    logger.info("gotPlay got click on play ");
    setHearYourself(false);
    gotRehearse();

    //  if (!setTurnToPromptSide()) {
    ifOnLastJumpBackToFirst();
    //  }

    playCurrentTurn();
  }

  protected boolean isDoRehearse() {
    return doRehearse;
  }

  void gotPlayYourself() {
    setGotTurnClick(false);
    logger.info("gotPlay got click on play yourself");

    gotHearYourself();
    //  if (!setTurnToPromptSide()) {
    ifOnLastJumpBackToFirst();
    //  }

    playCurrentTurn();
  }

  void setHearYourself(boolean enabled) {
    playYourselfButton.setEnabled(enabled);
  }

  protected void gotRehearse() {
    doRehearse = true;
  }

  protected void gotHearYourself() {
    doRehearse = false;
    if (DEBUG) logger.info("gotHearYourself : doRehearse = " + doRehearse);

  }

  void ifOnLastJumpBackToFirst() {
    T currentTurn = getCurrentTurn();
    boolean last = isLast(currentTurn);
    if (last) logger.info("OK, on last - let's consider going back to start");
    if (currentTurn != null && !currentTurn.hasCurrentMark()) {
      markFirstTurn();
    }
  }

  /**
   * @return true if changed turn to next one
   * @see #gotPlay
   */
//  boolean setTurnToPromptSide() {
//    Boolean leftSpeakerSet = false;//isLeftSpeakerSet();
//    Boolean rightSpeakerSet = true;//isRightSpeakerSet();
//    if (leftSpeakerSet && rightSpeakerSet) {
//      if (DEBUG) logger.info("setTurnToPromptSide both speakers ");
//      return false;
//    } else if (
//        leftSpeakerSet && !leftTurnPanels.contains(currentTurn) ||  // current turn is not the prompt set
//            rightSpeakerSet && !rightTurnPanels.contains(currentTurn)
//    ) {
//      if (DEBUG) logger.info("setTurnToPromptSide setNextTurnForSide ");
//
//      setNextTurnForSide();
//      return true;
//    } else {
//      return false;
//    }
//  }

  /**
   * TODO : not sure if this is right?
   * Wrap around if on last turn.
   */
  void setNextTurnForSide() {
    removeMarkCurrent();
    int i = allTurns.indexOf(currentTurn);

    if (currentTurn == null) {
      logger.warning("setNextTurnForSide no current turn");
    } else {
      if (DEBUG) logger.info("setNextTurnForSide current turn for ex " + getExID());
    }

    int nextIndex = (i + 1 == allTurns.size()) ? 0 : i + 1;

    if (DEBUG) logger.info("setNextTurnForSide " + i + " next " + nextIndex);

    setCurrentTurn(allTurns.get(nextIndex));
  }


  boolean onLastTurn() {
    return isLast(currentTurn);
  }

  protected boolean isLast(T currentTurn) {
    List<T> seq = getAllTurns();
    return seq.indexOf(currentTurn) == seq.size() - 1;
  }

  boolean onFirstPromptTurn() {
    return getPromptSeq().indexOf(currentTurn) == 0;
  }

  /**
   * Spoken or prompt sequence
   *
   * @return
   */
  List<T> getPromptSeq() {
    if (isInterpreter) {
      return promptTurns;
    } else {
      boolean leftSpeaker = isLeftSpeakerSet();
      boolean rightSpeaker = isRightSpeakerSet();
      List<T> ts = (leftSpeaker && !rightSpeaker) ? leftTurnPanels : (!leftSpeaker && rightSpeaker) ? rightTurnPanels : allTurns;
      // logger.info("getPromptSeq " + ts.size());
      report(ts);
      return ts;
    }
  }

  private List<T> getAllTurns() {
    return allTurns;
  }

  private void report(List<T> allTurns) {
    StringBuilder builder = new StringBuilder();
    allTurns.forEach(turn -> builder.append(turn.getExID()).append(", "));
    logger.info("seq " + builder);
  }

  /**
   * The other RESPONDING side of the conversation.
   *
   * @return
   */
  List<T> getRespSeq() {
    if (isInterpreter) return middleTurnPanels;
    else {
      boolean leftSpeaker = isLeftSpeakerSet();
      boolean rightSpeaker = isRightSpeakerSet();
      return leftSpeaker ? rightTurnPanels : rightSpeaker ? leftTurnPanels : null;
    }
  }

  boolean isMiddle(T turn) {
    return middleTurnPanels.contains(turn);
  }

  boolean isFirstMiddle(T turn) {
    return middleTurnPanels.indexOf(turn) == 0;
  }

  boolean isFirstPrompt(T turn) {
    return leftTurnPanels.indexOf(turn) == 0;
  }

  Boolean isLeftSpeakerSet() {
    return true;//isLeftSpeakerSelected();
  }

  Boolean isRightSpeakerSet() {
    return false;//sRightSpeakerSelected();
  }

  /**
   * @see #gotTurnClick
   * @see #gotPlay()
   */
  void playCurrentTurn() {
    if (currentTurn != null) {
      if (DEBUG_PLAY) logger.info("playCurrentTurn " + blurb());
      boolean didPause = currentTurn.doPlayPauseToggle();
      if (didPause) {
        if (DEBUG_PLAY) logger.info("playCurrentTurn did pause " + blurb());
        setPlayButtonToPlay();
      } else {
        if (DEBUG_PLAY) {
          logger.info("playCurrentTurn maybe did play " + blurb());
        }
        currentTurn.markCurrent();
        currentTurn.showNoAudioToPlay();
      }
    } else {
      logger.warning("playCurrentTurn no current turn?");
    }
  }

  private String blurb() {
    return getExID() + " '" + getCurrentTurn().getText() + "'";
  }

  /**
   * @see HeadlessPlayAudio#startPlaying
   */
  @Override
  public void playStarted() {
    if (currentTurn != null) {
      if (DEBUG) {
        logger.info("playStarted - turn " + blurb());
      }
      setPlayButtonToPause();
      markCurrent();
    }
  }

  /**
   * When the prompt finishes playing, move on to the next turn.
   *
   * @seex mitll.langtest.client.sound.PlayAudioPanel#setPlayLabel
   * @see HeadlessPlayAudio#songFinished
   */
  @Override
  public void playStopped() {
    if (currentTurn != null) {
      if (DEBUG) {
        logger.info("playStopped for turn " + blurb());
      }

      setPlayButtonToPlay();

      if (isGotTurnClick()) {
        logger.info("playStopped ignore since click?");
      } else {
        removeMarkCurrent();
        currentTurnPlayEnded(false);
      }
    } else {
      logger.info("playStopped - no current turn.");
    }
  }

  /**
   * @return
   * @see #getSession
   */
  public INavigation.VIEWS getView() {
    return thisView;
  }

  /**
   * @param wasRecording
   * @see #playStopped
   */
  void currentTurnPlayEnded(boolean wasRecording) {
    if (DEBUG) {
      logger.info("currentTurnPlayEnded (listen) - turn " + getExID() + " gotTurnClick " + gotTurnClick);
    }

    if (gotTurnClick) {
      setGotTurnClick(false);
    } else {
      T next = getNext();
      if (DEBUG && next != null) {
        logger.info("currentTurnPlayEnded next turn " + next.getExID());
      }
      makeNextVisible();

      if (next == null) {
        if (DEBUG) logger.info("currentTurnPlayEnded OK stop");
      } else {
        removeMarkCurrent();
        setCurrentTurn(next);
        playCurrentTurn();
      }
    }
  }

  void makeCurrentTurnVisible() {
    makeVisible(currentTurn);
  }

  private boolean makeNextVisible() {
    T next = getNext();
    if (next != null) {
      makeVisible((T) next);
      return true;
    } else {
      return false;
    }
  }

  private boolean makePrevVisible() {
    T next = getPrev();
    if (next != null) {
      makeVisible((T) next);
      return true;
    } else return false;
  }


  /**
   * @seex #setPlayButtonIcon
   * @see #playStarted
   */
  void setPlayButtonToPause() {
    getPlayButtonToUse().setIcon(IconType.PAUSE);
    sessionGoingNow = true;
  }

  /**
   * @see #playCurrentTurn()
   */
  void setPlayButtonToPlay() {
    getPlayButtonToUse().setIcon(IconType.PLAY);
    sessionGoingNow = false;
  }

  private Button getPlayButtonToUse() {
    return doRehearse ? this.playButton : playYourselfButton;
  }

  private boolean sessionGoingNow;

  boolean isSessionGoingNow() {
    return sessionGoingNow;
  }

  /**
   * @see DialogEditor#gotTurnClick(EditorTurn)
   * @see #gotTurnClick(ITurnPanel)
   * @see #clearHighlightAndRemoveMark()
   * @see #setNextTurnForSide()
   * @see #playStopped()
   * @see #currentTurnPlayEnded(boolean)
   */
  void removeMarkCurrent() {
    if (DEBUG) logger.info("removeMarkCurrent on " + blurb());

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("removeMarkCurrent on " + currentTurn.getExID()));
//    logger.info("logException stack:\n" + exceptionAsString);

    currentTurn.removeMarkCurrent();
  }

  void markCurrent() {
    if (DEBUG) logger.info("markCurrent on " + blurb());
    currentTurn.markCurrent();
  }

  /**
   * @return null if on last turn
   */
  T getNext() {
    return getNext(this.currentTurn);
  }

  private T getNext(T currentTurn) {
    List<T> seq = getAllTurns();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (DEBUG) logger.info("getNext current " + i + " next " + i1);

    if (i1 > seq.size() - 1) {
      return null;
    } else {
      T widgets = seq.get(i1);
      if (DEBUG) logger.info("getNext current at " + i1 + " will be ex #" + widgets.getExID());
      return widgets;
    }
  }

//  T getNextTurn() {
//    int i = getAllTurns().indexOf(currentTurn) + 1;
//    return i == getAllTurns().size() ? null : getAllTurns().get(i);
//  }


//  private T getPrev() {
//    List<T> seq = getAllTurns();
//    int i = seq.indexOf(currentTurn);
//    int i1 = i - 1;
//
//    if (i1 > -1) {
//      return seq.get(i1);
//    } else {
//      return null;
//    }
//  }
//

  /**
   * @return null if there is no previous turn
   * @see
   */
  protected T getPrev() {
    return getPrev(this.currentTurn);
  }

//  protected T getPrev(T currentTurn) {
//    getAllTurns().stream().filter()
//  }

  T getPrev(T currentTurn) {
    List<T> seq = getAllTurns();

    int i = seq.indexOf(currentTurn);
    int i1 = i - 1;
    return i1 < 0 ? null : seq.get(i1);
  }

  /**
   * TODO : don't put this here
   *
   * @param exidForTurn
   * @param columns
   */

  @Override
  public void addTurnForSameSpeaker(T editorTurn) {

  }

  @Override
  public void addTurnForOtherSpeaker(T editorTurn) {

  }

  @Override
  public void deleteCurrentTurnOrPair(T currentTurn) {
  }
}
