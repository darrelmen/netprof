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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.dialog.DialogEditorView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.custom.INavigation.VIEWS.LISTEN;
import static mitll.langtest.client.custom.INavigation.VIEWS.SCORES;

public class DialogEditor extends ListenViewHelper<EditorTurn> implements SessionManager {
  private final Logger logger = Logger.getLogger("DialogEditor");

  private static final int FIXED_HEIGHT = 390;

  private final SessionStorage sessionStorage;
  private final boolean isInModal;

  private static final boolean DEBUG = false;

  /**
   * @see DialogEditorView#editList
   **/
  public DialogEditor(ExerciseController controller, INavigation.VIEWS thisView, IDialog theDialog) {
    super(controller, thisView);
    setDialog(theDialog);
    isInModal = theDialog != null;
    this.sessionStorage = new SessionStorage(controller.getStorage(), "editorSession");
  }

  @Override
  protected boolean addPlayYourself() {
    return false;
  }

  @Override
  public String getSession() {
    return "" + sessionStorage.getSession();
  }

  /**
   * @param clientExercise
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @param index
   * @return
   * @see ListenViewHelper#reallyGetTurnPanel
   */
  @Override
  @NotNull
  protected EditorTurn makeTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn,
                                     boolean rightJustify, int index) {
    int i = getDialog().getExercises().indexOf(clientExercise);
    boolean isFirst = i == 0 && columns == COLUMNS.LEFT || i == 1 && columns == COLUMNS.MIDDLE;
    EditorTurn widgets = new EditorTurn(
        clientExercise,
        columns,
        prevColumn,
        rightJustify,
        controller,
        this,
        getDialogID(),
        isFirst,
        this);

//    widgets.getElement().setPropertyString("tabindex", "" + index);

    return widgets;
  }

  @Override
  void gotPlay() {
    setGotTurnClick(false);
    playCurrentTurn();
  }

  /**
   * If we're recording and we hit one of the forward/backward turns, stop recording right there...
   */
  @Override
  void gotBackward() {
    super.gotBackward();
    safeStopRecording();
  }

  private void safeStopRecording() {
    if (isRecording()) {
      getCurrentTurn().cancelRecording();
    }
  }

  private boolean isRecording() {
    return getCurrentTurn() != null && getCurrentTurn().isRecording();
  }

  /**
   * @param turn
   */
  @Override
  protected void gotTurnClick(EditorTurn turn) {
    //super.gotTurnClick(turn);

    setGotTurnClick(true);

    EditorTurn currentTurn = getCurrentTurn();

    boolean different = currentTurn != turn;

    if (DEBUG) {
      logger.info("currentTurn  " + currentTurn.getExID());
      logger.info("clicked turn " + turn.getExID());
      logger.info("different    " + different);
    }

    if (different) {
      removeMarkCurrent();
      setCurrentTurn(turn);
      markCurrent();
    }
//    playCurrentTurn();

    //  logger.info("gotClickOnTurn " + turn);
  }

  @Override
  protected void styleTurnContainer(DivWidget rowOne) {
    super.styleTurnContainer(rowOne);
    if (isInModal) {
      rowOne.getElement().getStyle().setOverflow(Style.Overflow.SCROLL);
      setFixedHeight(rowOne);
    }
  }

  private void setFixedHeight(DivWidget rowOne) {
    rowOne.setHeight(FIXED_HEIGHT + "px");
  }

  @Override
  protected void addDialogHeader(IDialog dialog, Panel child) {
    if (!isInModal) {
      super.addDialogHeader(dialog, child);
    }
  }

  /**
   * If on the last turn, either make a new pair of exercises for interpreter, or just a new exercise
   * for a simple dialog.
   *
   * @param editorTurn this is the turn after which we add the next turn or turn pair
   */
  @Override
  public void gotForward(EditorTurn editorTurn) {
    safeStopRecording();

    if (isLast(editorTurn)) {
      // make either one or two more turns and add to end of dialog
      addTurnForSameSpeaker(editorTurn);
    } else {
      super.gotForward(editorTurn);
      getCurrentTurn().grabFocus();
    }
  }

  @Override
  public void addTurnForSameSpeaker(EditorTurn turn) {
    COLUMNS columns = turn.getColumn();
    logger.info("addTurnForSameSpeaker : " +
        "\n\tcurrent turn " + turn +
        "\n\tcolumns      " + columns
    );

    // if prev turn is null we're on the first turn
    boolean isLeftSpeaker = isLeftSpeaker(columns, getPrev(turn));
    controller.getDialogService().addEmptyExercises(getDialogID(), turn.getExID(), isLeftSpeaker, getAsyncForNewTurns(turn.getExID()));
  }

  private boolean isLeftSpeaker(COLUMNS columns, EditorTurn prevTurn) {
    boolean isLeftSpeaker;
    if (isInterpreter) {
      if (columns == COLUMNS.MIDDLE) {
        isLeftSpeaker = (prevTurn == null || prevTurn.getColumn() == COLUMNS.LEFT);
      } else {
        isLeftSpeaker = columns == COLUMNS.LEFT;//duh
      }
    } else {
      isLeftSpeaker = columns == COLUMNS.LEFT;//duh
    }
    return isLeftSpeaker;
  }

  @Override
  public void addTurnForOtherSpeaker(EditorTurn editorTurn) {
    COLUMNS columns = editorTurn.getColumn();
    logger.info("addTurnForOtherSpeaker : " +
        "\n\tcurrent turn " + editorTurn.getExID() +
        "\n\tcolumns      " + columns
    );

    boolean isLeftSpeaker = isLeftSpeaker(columns, getPrev(editorTurn));
    controller.getDialogService().addEmptyExercises(getDialogID(), editorTurn.getExID(), !isLeftSpeaker, getAsyncForNewTurns(editorTurn.getExID()));
  }

  @Override
  public void deleteCurrentTurnOrPair(EditorTurn currentTurn) {
    logger.info("deleteCurrentTurnOrPair : " +
        "\n\tcurrent turn " + currentTurn
    );

    // todo :return both turns
    controller.getDialogService().deleteATurnOrPair(
        getDialog().getProjid(),
        getDialogID(),
        currentTurn.getExID(),
        new AsyncCallback<List<Integer>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("deleting turns in a dialog.", caught);
          }

          @Override
          public void onSuccess(List<Integer> ids) {
            ids.forEach(exid -> deleteTurn(exid));
            // TODO : remove one or two turns and change current turn to previous turn before deleted
            if (getCurrentTurn() == null) {
              logger.warning("huh? no current turn???");
            } else {
              getCurrentTurn().grabFocus();
            }
          }
        });
  }

  @NotNull
  private AsyncCallback<IDialog> getAsyncForNewTurns(int exid) {
    return new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("adding new turns to dialog.", caught);
      }

      @Override
      public void onSuccess(IDialog result) {
        addTurns(result, exid);
      }
    };
  }

  @Override
  protected int getDialogFromURL() {
    return getDialogID();
  }

  @NotNull
  protected INavigation.VIEWS getPrevView() {
    return SCORES;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return LISTEN;
  }

  /**
   * @see DialogEditorView.MyShownCloseListener#gotShown
   */
  public void grabFocus() {
    if (getCurrentTurn() != null) {
      // logger.info("give focus to turn for ex #" + getCurrentTurn().getExID());
      getCurrentTurn().grabFocus();
    }
  }

  /**
   * @param exercises
   * @param afterThisTurn
   * @see DialogEditor#getAsyncForNewTurns
   */
  private void addTurns(IDialog updated, int exid) {
    this.setDialog(updated);

    clearTurnLists();
    addAllTurns(getDialog(), turnContainer);

    List<EditorTurn> collect = allTurns.stream().filter(turn -> turn.getExID() == exid).collect(Collectors.toList());

    EditorTurn next = getCurrentTurn();
    if (collect.isEmpty()) {
      logger.warning("addTurns : can't find exid " + exid);
    } else {
      EditorTurn current = collect.get(0);

      int i = allTurns.indexOf(current) + 1;
      next = allTurns.get(i);

      logger.info("addTurns : num turns " + allTurns.size() +
          "\n\texid    " + exid +
          "\n\tcurrent " + current.getExID() +
          "\n\tnext    " + next.getExID()
      );
    }

    final EditorTurn fnext = next;

    setCurrentTurn(fnext);

    Scheduler.get().scheduleDeferred(() -> {
      logger.info("addTurns : focus will be on " + (fnext == null ? "NULL" : fnext.getExID()));
      markCurrent();
      fnext.grabFocus();
    });
  }
}
