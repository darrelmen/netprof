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

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.ISimpleTurn;
import mitll.langtest.client.scoring.ITurnPanel;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;

public abstract class TurnViewHelper<T extends ISimpleTurn>
    extends DialogView
    implements ContentView, IModeListener {
  public static final String I = "I";
  private final Logger logger = Logger.getLogger("TurnViewHelper");

  private static final int DELETE_DELAY = 500;
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
  private T currentTurn;
  private INavigation.VIEWS thisView;
  protected IDialog dialog;
  /**
   *
   */
  private final List<T> allTurns = new ArrayList<>();
  DivWidget dialogHeader;
  private DivWidget speakerRow;
  DivWidget overallFeedback;

  private boolean isInterpreter = false;
  private boolean isInterpreterMode = true;

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
        if (DEBUG) logger.info("showContent Got back dialog " + dialog);
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

  /**
   * @see #showContent(Panel, INavigation.VIEWS)
   */
  protected void clearTurnLists() {
    allTurns.clear();
  }

  void startDelete(ISimpleTurn currentTurn) {
    currentTurn.setDeleting(true);
    if (currentTurn instanceof UIObject) {
      ((UIObject) currentTurn).getElement().getStyle().setOpacity(0.5);
    }
  }

  protected int getDialogFromURL() {
    return new SelectionState().getDialog();
  }

  private Panel dialogContainer;

  /**
   * @param dialogID can be -1 if we just jump into a rehearse view without choosing a dialog first...
   * @param dialog
   * @param child
   * @see #showContent
   * @see #showContentForDialogInURL(Panel)
   */
  void showDialogGetRef(int dialogID, IDialog dialog, Panel child) {
    this.dialog = dialog;
    if (DEBUG) logger.info("showDialogGetRef : show dialog " + dialogID);
    if (dialog != null) {
      this.dialogContainer = child;
      isInterpreter = dialog.getKind() == DialogType.INTERPRETER;
      showDialog(dialogID, dialog, child);
    } else {
      controller.getNavigation().showView(INavigation.VIEWS.DIALOG);
    }
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

  private void removeFromContainer(T toRemove) {
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

  T getCurrentTurn() {
    return currentTurn;
  }

  /**
   * @param toMakeCurrent
   * @see #markFirstTurn()
   */
  void setCurrentTurn(T toMakeCurrent) {
    String s = toMakeCurrent == null ? " NO CURRENT " : toMakeCurrent.getExID() + " " + toMakeCurrent.getContent();
    if (DEBUG) logger.info("setCurrentTurn : toMakeCurrent " + s);
    this.currentTurn = toMakeCurrent;
  }

  /**
   * @see #ifOnLastJumpBackToFirst
   */
  void markFirstTurn() {
    if (!allTurns.isEmpty()) {
      setCurrentTurn(allTurns.get(0));

      if (DEBUG) logger.info("markFirstTurn : markCurrent ");

      markCurrent();
      makeVisible(currentTurn);
    }
  }

  void makeNextTheCurrentTurn(T fnext) {
    setCurrentTurn(fnext);

    Scheduler.get().scheduleDeferred(() -> {
      if (DEBUG) {
        logger.info("makeNextTheCurrentTurn : focus will be on " + (fnext == null ? "NULL" : fnext.getExID()));
      }

      markCurrent();

      if (fnext != null) {
        fnext.grabFocus();
      }
    });
  }

  T getNextTurn(int exid) {
    T current = getTurnByID(exid);

    T next = getCurrentTurn();
    if (current == null) {
      logger.warning("getNextTurn : can't find exid " + exid);
    } else {
      int i = allTurns.indexOf(current) + 1;
      next = allTurns.get(i);

      if (DEBUG_NEXT) {
        logger.info("getNextTurn : num turns " + allTurns.size() +
            "\n\texid    " + exid +
            "\n\tcurrent " + current.getExID() +
            "\n\tnext    " + next.getExID()
        );
      }
    }
    return next;
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

      // add header
      addDialogHeader(dialog, child);

      // add controls
      {
        DivWidget controlAndSpeakers = new DivWidget();
        styleControlRow(controlAndSpeakers);

        addControls(controlAndSpeakers);

        controlAndSpeakers.add(speakerRow = getSpeakerRow());

        child.add(controlAndSpeakers);
      }

      // add turns
      child.add(getTurns(dialog));

      overallFeedback = getOverallFeedback();

      child.add(overallFeedback = getOverallFeedback());
    }
  }

  @NotNull
  protected DivWidget getOverallFeedback() {
    DivWidget widget = new DivWidget();
    styleOverallFeedback(widget);
    return widget;
  }

  protected void addControls(DivWidget controlAndSpeakers) {
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

  DivWidget getControls() {
    return new DivWidget();
  }

  /**
   * @param dialog
   * @param child
   * @see #showDialog(int, IDialog, Panel)
   */
  protected void addDialogHeader(IDialog dialog, Panel child) {
    child.add(dialogHeader =
        new DialogHeader(controller, thisView, getPrevView(), getNextView(), this)
            .getHeader(dialog));
  }

  @Override
  public void gotDialog() {
    isInterpreterMode = false;
    clearTurnLists();
    showDialog(dialog.getID(), dialog, dialogContainer);
  }

  @Override
  public void gotInterpreter() {
    isInterpreterMode = true;
    clearTurnLists();
    showDialog(dialog.getID(), dialog, dialogContainer);
  }

  @Override
  public void setIsDialog(boolean val) {
    isInterpreterMode = !val;

    if (DEBUG) logger.info("isInterpreter " + val + " : is interpreter : " + isInterpreterMode);
  }

  /**
   * @return
   * @see #showDialog
   */
  @NotNull
  private DivWidget getSpeakerRow() {
    DivWidget rowOne = new DivWidget();
    rowOne.getElement().setId("speakerRow");

    Style style = rowOne.getElement().getStyle();
    style.setMarginTop(5, PX);
    style.setOverflow(Style.Overflow.HIDDEN);

    {
      String firstSpeakerLabel = isInterpreter ? getFirstSpeakerLabel() : ListenViewHelper.SPEAKER_A;
      rowOne.add(getLeftSpeaker(firstSpeakerLabel));
    }

    {
      String secondSpeakerLabel = isInterpreter ? getSecondSpeakerLabel() : ListenViewHelper.SPEAKER_B;
      rowOne.add(getRightSpeaker(secondSpeakerLabel));
    }

    {
      DivWidget middleSpeaker = getMiddleSpeaker();
      if (!isInterpreter || !isInterpreterMode) {
        middleSpeaker.setHeight("5px");
        middleSpeaker.setVisible(false);
      }
      rowOne.add(middleSpeaker);
    }

    return rowOne;
  }

  /**
   * @return
   */
  @NotNull
  String getFirstSpeakerLabel() {
    /*    String firstSpeaker = dialog.getSpeakers().isEmpty() ? null : dialog.getSpeakers().get(0);

    if (DEBUG) logger.info("getFirstSpeakerLabel first speaker " + firstSpeaker);

    if (!dialog.getExercises().isEmpty()) {
      ClientExercise next = dialog.getExercises().iterator().next();
      boolean hasEnglishAttr = next.hasEnglishAttr();

      if (hasEnglishAttr &&
          (firstSpeaker == null || getExerciseSpeaker(next).equalsIgnoreCase(firstSpeaker))) {
        firstSpeaker = ENGLISH_SPEAKER;
      }
    } else if (isInterpreter()) {
      firstSpeaker = ENGLISH_SPEAKER;
    }

    if (firstSpeaker == null) firstSpeaker = SPEAKER_A;
    if (DEBUG) {
      logger.info("getFirstSpeakerLabel 2 " +
          "first speaker " + firstSpeaker);
    }
    */

    return isInterpreterMode ? ENGLISH_SPEAKER : SPEAKER_A;
  }

  /**
   * TODO : allow english speaker to go second
   *
   * @return
   */
  @Nullable
  String getSecondSpeakerLabel() {
     /*   String secondSpeaker = dialog.getSpeakers().size() > 1 ? dialog.getSpeakers().get(1) : null;

    // OK guess from the language of the first turn
    if (!dialog.getExercises().isEmpty()) {
      ClientExercise next = dialog.getExercises().iterator().next();
      boolean hasEnglishAttr = next.hasEnglishAttr();

      if (hasEnglishAttr && !getExerciseSpeaker(next).equalsIgnoreCase(secondSpeaker)) {
        secondSpeaker = getProjectLangSpeaker();
      }
    } else if (isInterpreter()) {
      secondSpeaker = getProjectLangSpeaker();
    }

    if (secondSpeaker == null) secondSpeaker = ListenViewHelper.SPEAKER_B;
    return secondSpeaker;*/


    return isInterpreterMode ? getProjectLangSpeaker() : ListenViewHelper.SPEAKER_B;
  }

  @NotNull
  private String getProjectLangSpeaker() {
    return controller.getLanguageInfo().toDisplay() + " Speaker";
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
    right.add(w);
    right.addStyleName("bubble");
    right.addStyleName("rightbubble");
    right.addStyleName("floatRight");
    setPadding(right);
    right.getElement().getStyle().setBackgroundColor(RIGHT_BKG_COLOR);
    styleRightSpeaker(w);
    return right;
  }

  void styleOverallFeedback(DivWidget breadRow) {
    breadRow.getElement().setId("overallFeedbackRow");

    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(10, PX);
    style.setMarginBottom(10, PX);
    style.setClear(Style.Clear.BOTH);

    breadRow.addStyleName("cardBorderShadow");
  }

  @NotNull
  protected DivWidget getLeftSpeaker(String firstSpeaker) {
    Heading w = new Heading(4, firstSpeaker);

    DivWidget left = new DivWidget();
    left.add(w);

    styleLeftSpeaker(w);

    left.addStyleName("floatLeft");
    left.addStyleName("bubble");
    left.addStyleName("leftbubble");
    setPadding(left);

    left.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

    return left;
  }

  private void setPadding(DivWidget right) {
    right.getElement().getStyle().setPaddingLeft(PADDING_LOZENGE, PX);
    right.getElement().getStyle().setPaddingRight(PADDING_LOZENGE, PX);
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
  public DivWidget getTurns(IDialog dialog) {
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
    String left = getFirstSpeakerLabel();
    String right = getSecondSpeakerLabel();
    addTurnPerExercise(dialog, rowOne, left, right);
  }

  protected void styleTurnContainer(DivWidget rowOne) {
    rowOne.getElement().setId("turnContainer");
    rowOne.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    rowOne.getElement().getStyle().setMarginTop(10, PX);
    rowOne.addStyleName("cardBorderShadow");
    rowOne.getElement().getStyle().setMarginBottom(10, PX);
  }

  protected void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
    rowOne.clear();
    addTurnForEachExercise(rowOne, left, right, dialog.getExercises());
  }

  /**
   * Filters out english turns if just practicing as a dialog.
   *
   * @param rowOne
   * @param left
   * @param right
   * @param exercises
   */
  protected void addTurnForEachExercise(DivWidget rowOne, String left, String right, List<ClientExercise> exercises) {
    ClientExercise prev = null;
    int index = 0;

    if (DEBUG) logger.info("addTurnForEachExercise got " + exercises.size() + " : " + isInterpreterMode);

    if (!isInterpreterMode) {
      exercises = exercises
          .stream()
          .filter(ex -> !ex.hasEnglishAttr())
          .collect(Collectors.toList());
    }

    for (ClientExercise clientExercise : exercises) {
      ITurnContainer.COLUMNS prevCol = prev == null ? ITurnContainer.COLUMNS.UNK : getColumnForEx(left, right, prev);
      ITurnContainer.COLUMNS columnForEx = getColumnForEx(left, right, clientExercise);
      addTurn(rowOne, clientExercise, columnForEx, prevCol, index);
      prev = clientExercise;
      index++;
    }
  }

  ITurnContainer.COLUMNS getColumnForEx(String left, String right, ClientExercise clientExercise) {
    String speaker = clientExercise == null ? "" : clientExercise.getSpeaker();
    return (speaker.isEmpty()) ? ITurnContainer.COLUMNS.UNK : getColumnForSpeaker(left, right, speaker);
  }

  /**
   * TODO : make the column decision based on the dialog vs interpreter mode in the UI, not on speakers in the dialog
   *
   * @param left
   * @param right
   * @param speaker
   * @return
   * @see #getColumnForEx(String, String, ClientExercise)
   */
  @NotNull
  private ITurnContainer.COLUMNS getColumnForSpeaker(String left, String right, String speaker) {
    ITurnContainer.COLUMNS columns;

    if (isInterpreterMode) {
      if (speaker.equalsIgnoreCase(left) || speaker.equalsIgnoreCase(SPEAKER_A)) {
        columns = ITurnContainer.COLUMNS.LEFT;
      } else if (speaker.equalsIgnoreCase(right) || speaker.equalsIgnoreCase(SPEAKER_B)) {
        columns = ITurnContainer.COLUMNS.RIGHT;
      } else {
        columns = ITurnContainer.COLUMNS.MIDDLE;
        logger.info("getColumnForSpeaker : l " + left + " r " + right + " vs ex speaker : " + speaker + " => " + columns);
      }
    } else {
      if (speaker.equalsIgnoreCase(INTERPRETER) || speaker.equalsIgnoreCase(I)) {
        columns = ITurnContainer.COLUMNS.LEFT;
      } else {
        columns = ITurnContainer.COLUMNS.RIGHT;
      }
    }

    if (DEBUG || true) {
      logger.info("getColumnForSpeaker : l " + left + " r " + right + " vs ex speaker : " + speaker + " => " + columns);
    }

    return columns;
  }

  /**
   * @param rowOne
   * @param clientExercise
   * @param columns
   * @param index
   * @see #addTurnForEachExercise(DivWidget, String, String, List)
   */
  T addTurn(DivWidget rowOne,
            ClientExercise clientExercise,
            ITurnContainer.COLUMNS columns,
            ITurnContainer.COLUMNS prevColumn,
            int index) {
    T turn = getTurnPanel(clientExercise, columns, prevColumn, index);

    allTurns.add(index, turn);

    rowOne.insert(turn, index);

    return turn;
  }

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param index
   * @return
   * @see #addTurn
   */
  @NotNull
  T getTurnPanel(ClientExercise clientExercise, ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index) {
    T turn = reallyGetTurnPanel(clientExercise, columns, prevColumn, index);
    turn.addWidgets(true, false, PhonesChoices.HIDE, EnglishDisplayChoices.SHOW);
    return turn;
  }

  @NotNull
  protected abstract T reallyGetTurnPanel(ClientExercise clientExercise,
                                          ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index);

  /**
   * @param exid
   * @see DialogEditor#deleteCurrentTurnOrPair
   */
  private T deleteTurn(int exid, Set<T> toRemoves) {
    T toRemove = getTurnByID(exid);
    T newCurrentTurn = null;
    if (toRemove == null) {
      logger.warning("huh? can't find " + exid);

    } else {
      // fade it out for a second to show it disappearing...
      ((Widget) toRemove).addStyleName("opacity-out-target");

//      logger.info("deleteTurn removing current turn " + currentTurn.getExID());

      T prev = getPrev(toRemove);
      if (prev == null) {
        T next = getNext(toRemove);
        if (DEBUG) logger.info("deleteTurn now next " + (next == null ? " NULL " : next.getExID()));
        newCurrentTurn = next;
      } else {
        if (DEBUG) logger.info("deleteTurn now prev " + prev.getExID());
        newCurrentTurn = prev;
      }

      allTurns.remove(toRemove);

      removeFromAllPanels(toRemove);

      toRemoves.add(toRemove);
    }

    return newCurrentTurn;
  }

  void gotDeleteResponse(List<Integer> ids) {
    Set<T> toRemove = new HashSet<>();

    T newCurrentTurnCandidate = null;

    for (Integer exid : ids) {
      T newCurrentTurn = deleteTurn(exid, toRemove);
      if (newCurrentTurnCandidate == null) newCurrentTurnCandidate = newCurrentTurn;
    }

    // we deleted the current turn!
    final T fnewCurrentTurnCandidate = newCurrentTurnCandidate;
    final Set<T> ftoRemove = toRemove;

    // wait for animation to run before blowing it away...
    com.google.gwt.user.client.Timer currentTimer = new Timer() {
      @Override
      public void run() {
        ftoRemove.forEach(TurnViewHelper.this::removeFromContainer);

        if (fnewCurrentTurnCandidate != null) {
          logger.info("new current now " + fnewCurrentTurnCandidate.getExID());// + " " + fnewCurrentTurnCandidate.getText());
          removeMarkCurrent();
          setCurrentTurn(fnewCurrentTurnCandidate);
          markCurrent();
          fnewCurrentTurnCandidate.grabFocus();
        } else {
          logger.info("not messing with current turn...");
        }
      }
    };
    currentTimer.schedule(DELETE_DELAY);
  }

  protected void removeFromAllPanels(T toRemove) {
  }

  List<T> getAllTurns() {
    //   logger.info("getAllTurns : " + allTurns.size());
    return allTurns;
  }

//  protected void report(String prefix, List<T> allTurns) {
//    StringBuilder builder = new StringBuilder();
//    allTurns.forEach(turn -> builder.append(turn.getExID()).append(", "));
//    logger.info("report : " + prefix + " seq " + builder);
//  }

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

  private T getNext(T currentTurn) {
    List<T> seq = getAllTurns();
    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (DEBUG_NEXT) logger.info("getNext current of " + currentTurn.getExID() + //" " + currentTurn.getText() +
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

  void reloadDialog() {
    int projectID = controller.getProjectID();
    if (projectID != -1) {
      int dialogID = getDialogID();

      logger.info("onUnload - reload the dialog on hydra/score1");
      controller.getAudioService().reloadDialog(dialogID, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("reloading dialog.", caught);
        }

        @Override
        public void onSuccess(Void result) {
          logger.info("did reload on other server!");
          controller.getExerciseService().reloadDialog(projectID, dialogID, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("reloading dialog on netprof", caught);
            }

            @Override
            public void onSuccess(Void result) {
              logger.info("did reload on netprof.");
            }
          });
        }
      });
    }
  }

  @Nullable
  ClientExercise getPrev(int exid, List<ClientExercise> updatedExercises) {
    ClientExercise prev = null;
    for (ClientExercise turn : updatedExercises) {
      if (turn.getID() == exid) break;
      prev = turn;
    }
    return prev;
  }

  public void setCurrentTurnTo(T newTurn) {
    if (DEBUG_NEXT) logger.info("setCurrentTurnTo - " + newTurn.getExID());

    /*int i = */
    beforeChangeTurns();
    setCurrentTurn(newTurn);
    markCurrent();
    //   logger.info("setCurrentTurnTo ex #" + currentTurn.getExID());
    // afterChangeTurns(isPlaying);
  }

  public void gotForward(T editorTurn) {
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
  }

  protected int beforeChangeTurns() {
    int i = getIndexOfCurrentTurn();

    if (!makeNextVisible()) {
      if (DEBUG_NEXT) {
        logger.info("beforeChangeTurns : make header visible");
      }
      if (dialogHeader == null) {
        makeVisible(speakerRow);
        //  logger.info("no dialog header?");
      } else {
        makeVisible(dialogHeader);  // make the top header visible...
      }
    }
    return i;
  }

  /**
   * @see EditorTurn#deleteGotFocus
   */
  public void moveFocusToNext() {
    T next = getNext();
    if (next != null) {
      logger.info("moveFocusToNext - have " + next.getExID() + " grab focus.");
      next.grabFocus();
    }
    //else if (!getAllTurns().isEmpty()) {
    // logger.info("moveFocusToNext - to first - let's not!");
    //getAllTurns().get(0).grabFocus();
    // }
  }

  int getIndexOfCurrentTurn() {
    return getAllTurns().indexOf(currentTurn);
  }

  protected int getExID() {
    return currentTurn.getExID();// + " : " +currentTurn.getText();
  }

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

  /**
   * @return
   * @see #setNextTurnForSide()
   */
  boolean onLastTurn() {
    return isLast(currentTurn);
  }

  /**
   *
   */
  void makeCurrentTurnVisible() {
    if (!makeNextVisible()) {
      makeVisible(currentTurn);
    }
  }

  /**
   * @return true if did it
   */
  boolean makeNextVisible() {
    T next = getNext();
    if (next != null) {
      if (DEBUG_NEXT) logger.info("makeNextVisible " + next.getExID());// + " : " + next.getText());
      makeVisible((T) next);
      return true;
    } else {
      if (DEBUG_NEXT) logger.info("makeNextVisible - no next?");
      return false;
    }
  }

  boolean makePrevVisible() {
    T next = getPrev();
    if (next != null) {
      makeVisible((T) next);
      return true;
    } else return false;
  }

  /**
   * @see DialogEditor#gotTurnClick(EditorTurn)
   * @see #gotTurnClick(ITurnPanel)
   * @see #clearHighlightAndRemoveMark()
   * @see #setNextTurnForSide()
   * @see #playStopped()
   * @see #currentTurnPlayEnded()
   */
  void removeMarkCurrent() {
    //  if (DEBUG) logger.info("removeMarkCurrent on " + blurb());
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("removeMarkCurrent on " + currentTurn.getExID()));
//    logger.info("logException stack:\n" + exceptionAsString);

    currentTurn.removeMarkCurrent();
  }

  void markCurrent() {
    //  if (ListenViewHelper.DEBUG) logger.info("markCurrent on " + blurb());
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("markCurrent on " + currentTurn.getExID()));
//    logger.info("logException stack:\n" + exceptionAsString);
    currentTurn.markCurrent();
  }

  /**
   * @return null if on last turn
   */
  T getNext() {
    return getNext(this.currentTurn);
  }

  /**
   * @return null if there is no previous turn
   * @see
   */
  protected T getPrev() {
    return getPrev(this.currentTurn);
  }

  /**
   *
   */
  public boolean isInterpreter() {
    return isInterpreterMode;
  }
}
