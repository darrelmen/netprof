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
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.dialog.DialogEditorView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SessionStorage;
import mitll.langtest.client.scoring.IFocusable;
import mitll.langtest.shared.dialog.DialogExChangeResponse;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.LISTEN;
import static mitll.langtest.client.custom.INavigation.VIEWS.SCORES;

public class DialogEditor extends ListenViewHelper<EditorTurn> implements SessionManager, IFocusable, IEditableTurnContainer<EditorTurn> {
  private final Logger logger = Logger.getLogger("DialogEditor");

  private static final int FIXED_HEIGHT = 390;

  private final SessionStorage sessionStorage;
  private final boolean isInModal;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_ADD_TURN = false;

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
  void onUnload() {
    reloadDialog();
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
    boolean isFirst = i == 0 && columns == ITurnContainer.COLUMNS.LEFT || i == 1 && columns == ITurnContainer.COLUMNS.MIDDLE;
    EditorTurn widgets = new EditorTurn(
        clientExercise,
        columns,
        prevColumn,
        rightJustify,
        controller,
        this,
        this,
        getDialogID(),
        isFirst,
        this);

    return widgets;
  }

  @Override
  void gotClickOnPlay() {
    togglePlayState();

    if (isSessionGoingNow()) {
      playCurrentTurn();
    }
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
//    EditorTurn currentTurn = getCurrentTurn();
//    if (DEBUG) logger.info("gotTurnClick currentTurn  " + blurb(currentTurn));
  }

 /* @NotNull
  private String blurb(EditorTurn turn) {
    return turn.getExID() + " : " + turn.getText();
  }
*/

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
    int i = beforeChangeTurns();

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

    int exID = turn.getExID();

    if (DEBUG_ADD_TURN) {
      logger.info("addTurnForSameSpeaker : " +
          "\n\tcurrent turn " + exID +
          "\n\tcolumns      " + columns
      );
    }

    removeMarkCurrent();

    // if prev turn is null we're on the first turn
    controller
        .getDialogService()
        .addEmptyExercises(getDialogID(), exID, isLeftSpeaker(columns, getPrev(turn)), getAsyncForNewTurns(exID));
  }

  @Override
  public void addTurnForOtherSpeaker(EditorTurn editorTurn) {
    COLUMNS columns = editorTurn.getColumn();

    int exID = editorTurn.getExID();
    if (DEBUG_ADD_TURN) {
      logger.info("addTurnForOtherSpeaker : " +
          "\n\tcurrent turn " + exID +
          "\n\tcolumns      " + columns
      );
    }
    removeMarkCurrent();

    boolean isLeftSpeaker = isLeftSpeaker(columns, getPrev(editorTurn));
    controller.getDialogService().addEmptyExercises(getDialogID(), exID, !isLeftSpeaker, getAsyncForNewTurns(exID));
  }

  private boolean isLeftSpeaker(COLUMNS columns, EditorTurn prevTurn) {
    boolean isLeftSpeaker;
    if (isInterpreter) {
      if (columns == ITurnContainer.COLUMNS.MIDDLE) {
        isLeftSpeaker = (prevTurn == null || prevTurn.getColumn() == ITurnContainer.COLUMNS.LEFT);
      } else {
        isLeftSpeaker = columns == ITurnContainer.COLUMNS.LEFT;//duh
      }
    } else {
      isLeftSpeaker = columns == ITurnContainer.COLUMNS.LEFT;//duh
    }
    return isLeftSpeaker;
  }

  @Override
  public void deleteCurrentTurnOrPair(EditorTurn currentTurn) {
    logger.info("deleteCurrentTurnOrPair : " + "\n\tcurrent turn " + currentTurn.getExID());
    startDelete(currentTurn);

    if (isInterpreter) {
      EditorTurn prev = getPrev(currentTurn);
      if (prev == null) {
        logger.warning("no prev????");
      } else {
        startDelete(prev);
      }
    }

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
            gotDeleteResponse(ids);
          }
        });
  }

  //  private void gotDeleteResponse(List<Integer> ids) {
//    Set<EditorTurn> toRemove = new HashSet<>();
//
//    EditorTurn newCurrentTurnCandidate = null;
//
//    for (Integer exid : ids) {
//      EditorTurn newCurrentTurn = deleteTurn(exid, toRemove);
//      if (newCurrentTurnCandidate == null) newCurrentTurnCandidate = newCurrentTurn;
//    }
//
//    // we deleted the current turn!
//    final EditorTurn fnewCurrentTurnCandidate = newCurrentTurnCandidate;
//    final Set<EditorTurn> ftoRemove = toRemove;
//
//    // wait for animation to run before blowing it away...
//    com.google.gwt.user.client.Timer currentTimer = new Timer() {
//      @Override
//      public void run() {
//        ftoRemove.forEach(DialogEditor.this::removeFromContainer);
//
//        if (fnewCurrentTurnCandidate != null) {
//          logger.info("new current now " + fnewCurrentTurnCandidate.getExID() + " " + fnewCurrentTurnCandidate.getText());
//          removeMarkCurrent();
//          setCurrentTurn(fnewCurrentTurnCandidate);
//          markCurrent();
//          fnewCurrentTurnCandidate.grabFocus();
//        } else {
//          logger.info("not messing with current turn...");
//        }
//      }
//    };
//    currentTimer.schedule(500);
//  }

  @NotNull
  private AsyncCallback<DialogExChangeResponse> getAsyncForNewTurns(int exid) {
    return new AsyncCallback<DialogExChangeResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("adding new turns to dialog.", caught);
      }

      @Override
      public void onSuccess(DialogExChangeResponse result) {
        addTurns(result.getUpdated(), result.getChanged(), exid);
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
  private void addTurns(IDialog updated, List<ClientExercise> changed, int exid) {
    this.setDialog(updated);

    List<ClientExercise> updatedExercises = updated.getExercises();
    ClientExercise prev = getPrev(exid, updatedExercises);

    String left = getFirstSpeakerLabel(updated);
    String right = getSecondSpeakerLabel(updated);

    clearColumnTurnLists();

    for (ClientExercise clientExercise : changed) {
      COLUMNS currentCol = getColumnForEx(left, right, clientExercise);
      COLUMNS prevCol = prev == null ? ITurnContainer.COLUMNS.UNK : getColumnForEx(left, right, prev);
      EditorTurn turn = addTurn(turnContainer, clientExercise, currentCol, prevCol, updatedExercises.indexOf(clientExercise));
      turn.addStyleName("opacity-target");
      prev = clientExercise;
    }

    makeNextTheCurrentTurn(getNextTurn(exid));
  }

}
