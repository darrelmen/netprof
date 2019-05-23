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
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.IFocusable;
import mitll.langtest.client.scoring.SimpleTurn;
import mitll.langtest.shared.dialog.DialogExChangeResponse;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.custom.INavigation.VIEWS.CORE_EDITOR;
import static mitll.langtest.client.custom.INavigation.VIEWS.LISTEN;
import static mitll.langtest.client.dialog.ITurnContainer.COLUMNS.UNK;

public class CoreVocabEditor extends TurnViewHelper<CoreEditorTurn> implements IFocusable, IFocusListener, IEditableTurnContainer<CoreEditorTurn> {
  private final Logger logger = Logger.getLogger("CoreVocabEditor");

  private final boolean isInModal;

  public CoreVocabEditor(ExerciseController controller, INavigation.VIEWS thisView, IDialog theDialog) {
    super(controller, thisView);
    setDialog(theDialog);
    isInModal = theDialog != null;
  }

  @Override
  protected int getDialogFromURL() {
    return getDialogID();
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return LISTEN;
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
    rowOne.setHeight(390 + "px");
  }

  @Override
  protected void addDialogHeader(IDialog dialog, Panel child) {
    child.add(dialogHeader =
        new DialogHeader(controller,
            INavigation.VIEWS.CORE_EDITOR, isInModal ? null : getPrevView(), isInModal ? null : getNextView()).getHeader(dialog));
  }

  @Override
  public void gotBlur() {

  }

  @Override
  public void gotFocus() {

  }

  @Override
  public void gotKey(KeyUpEvent event) {

  }

  @NotNull
  public DivWidget getTurns(IDialog dialog) {
    DivWidget leftRight = new DivWidget();
    leftRight.setWidth("100%");
    leftRight.addStyleName("inlineFlex");

    {
      TurnViewHelper<SimpleTurn> leftHelper = getLeftHelper();
      DivWidget turns = leftHelper.getTurns(dialog);
      turns.setWidth("60%");
      leftRight.add(turns);
    }

//    leftRight.add(coreVocabTurns);
    DivWidget coreVocabTurns = super.getTurns(dialog);
    coreVocabTurns.setWidth("40%");
    styleTurnContainer(coreVocabTurns);
    leftRight.add(coreVocabTurns);

//
//    List<ClientExercise> coreVocabulary = dialog.getCoreVocabulary();
//    List<ClientExercise> toUse = coreVocabulary.isEmpty() ? new ArrayList<>() : coreVocabulary;
//    if (toUse.isEmpty()) toUse.add(new Exercise());
//
//    allTurns.clear();
//    addTurnForEachCoreExercise(right, coreVocabulary);

/*
    Language languageInfo = controller.getLanguageInfo();
    boolean isInterpreter = dialog.getKind() == DialogType.INTERPRETER;

    dialog.getCoreVocabulary().forEach(vocab -> {
      right.add(new MySimpleTurn(vocab, languageInfo, isInterpreter));
    });

    logger.info("found " + dialog.getCoreVocabulary().size());
    if (dialog.getCoreVocabulary().isEmpty()) {
      SimpleTurn w = new MySimpleTurn(new Exercise(), languageInfo, isInterpreter);
      right.add(w);
      w.addWidgets(true, false, PhonesChoices.SHOW, EnglishDisplayChoices.SHOW);

    }*/
    return leftRight;
  }

  private TurnViewHelper<SimpleTurn> getLeftHelper() {

    return new TurnViewHelper<SimpleTurn>(controller, CORE_EDITOR) {
      @NotNull
      @Override
      protected SimpleTurn reallyGetTurnPanel(ClientExercise clientExercise, ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index) {
        return new SimpleTurn(clientExercise, columns, false);
      }

      @Override
      protected void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
        rowOne.clear();
        List<ClientExercise> exercises = dialog.getExercises();
        exercises = exercises.stream().filter(exercise ->
            !exercise.hasEnglishAttr() && !exercise.getForeignLanguage().isEmpty()).collect(Collectors.toList());
        addTurnForEachExercise(rowOne, left, right, exercises);
      }

    };
  }


  @Override
  public void addTurnForSameSpeaker(CoreEditorTurn editorTurn) {
    controller.getDialogService().addEmptyCoreExercise(getDialogID(), -1, new AsyncCallback<DialogExChangeResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("adding core vocab to a dialog.", caught);
      }

      @Override
      public void onSuccess(DialogExChangeResponse result) {
        addTurns(result.getUpdated(), result.getChanged(), -1, turnContainer);
      }
    });
  }

  @Override
  public void addTurnForOtherSpeaker(CoreEditorTurn editorTurn) {

  }

  @Override
  public void deleteCurrentTurnOrPair(CoreEditorTurn currentTurn) {
    int exID = currentTurn.getExID();
    logger.info("deleteCurrentTurnOrPair : " + "\n\tcurrent turn " + exID);
    currentTurn.getElement().getStyle().setOpacity(0.5);

    controller.getDialogService().deleteCoreExercise(
        getDialogID(),
        exID,
        new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("deleting turns in a dialog.", caught);
          }

          @Override
          public void onSuccess(Boolean resp) {
            if (resp) {
              gotDeleteResponse(exID);
            } else logger.warning("huh?");
          }
        });
  }

  private void gotDeleteResponse(int exid) {
    Set<CoreEditorTurn> toRemove = new HashSet<>();

    CoreEditorTurn newCurrentTurnCandidate = null;


    CoreEditorTurn newCurrentTurn = deleteTurn(exid, toRemove);
    if (newCurrentTurnCandidate == null) newCurrentTurnCandidate = newCurrentTurn;


    // we deleted the current turn!
    final CoreEditorTurn fnewCurrentTurnCandidate = newCurrentTurnCandidate;
    final Set<CoreEditorTurn> ftoRemove = toRemove;

    // wait for animation to run before blowing it away...
    com.google.gwt.user.client.Timer currentTimer = new Timer() {
      @Override
      public void run() {
        ftoRemove.forEach(CoreVocabEditor.this::removeFromContainer);

        if (fnewCurrentTurnCandidate != null) {
          logger.info("new current now " + fnewCurrentTurnCandidate.getExID());// + " " + fnewCurrentTurnCandidate.getText());
//          removeMarkCurrent();
//          setCurrentTurn(fnewCurrentTurnCandidate);
//          markCurrent();
          fnewCurrentTurnCandidate.grabFocus();
        } else {
          logger.info("not messing with current turn...");
        }
      }
    };
    currentTimer.schedule(500);
  }

  /**
   * @param exercises
   * @param afterThisTurn
   * @see DialogEditor#getAsyncForNewTurns
   */
  private void addTurns(IDialog updated, List<ClientExercise> changed, int exid, DivWidget turnContainer) {
    this.setDialog(updated);

    List<ClientExercise> updatedExercises = updated.getExercises();

    for (ClientExercise clientExercise : changed) {
      CoreEditorTurn turn = addTurn(turnContainer, clientExercise, UNK, UNK, updatedExercises.indexOf(clientExercise));
      turn.addStyleName("opacity-target");
    }

    // makeNextTheCurrentTurn(getNextTurn(exid));
  }

 /* private void addTurnForEachCoreExercise(DivWidget rowOne, List<ClientExercise> exercises) {
    int index = 0;
    logger.info("addTurnForEachExercise got " + exercises.size());
    for (ClientExercise clientExercise : exercises) {
      addCoreTurn(rowOne, clientExercise, index);
      index++;
    }
  }*/

  /**
   * @param rowOne
   * @param clientExercise
   * @param index
   * @see #addTurnForEachExercise(DivWidget, String, String, List)
   */
/*  private CoreEditorTurn addCoreTurn(DivWidget rowOne,
                                     ClientExercise clientExercise,
                                     int index) {
    CoreEditorTurn turn = getCoreTurnPanel(clientExercise);

    allTurns.add(index, turn);

    rowOne.insert(turn, index);

    return turn;
  }*/

/*  @NotNull
  CoreEditorTurn getCoreTurnPanel(ClientExercise clientExercise) {
    CoreEditorTurn turn = new CoreEditorTurn(
        controller,
        this,
        clientExercise,
        controller.getLanguageInfo(),
        false,
        getDialogID());
    turn.addWidgets(true, false, PhonesChoices.HIDE, EnglishDisplayChoices.SHOW);
    return turn;
  }*/
  protected void addControls(DivWidget controlAndSpeakers) {
  }

  @Override
  public void grabFocus() {
    if (!getAllTurns().isEmpty()) getAllTurns().iterator().next().grabFocus();
  }

  /**
   * Don't show english.
   *
   * @param dialog
   * @param rowOne
   * @param left
   * @param right
   */
  @Override
  protected void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
    rowOne.clear();
    List<ClientExercise> coreVocabulary = dialog.getCoreVocabulary();
    List<ClientExercise> toUse = coreVocabulary.isEmpty() ? new ArrayList<>() : coreVocabulary;
    if (toUse.isEmpty()) toUse.add(new Exercise());

    addTurnForEachExercise(rowOne, left, right, toUse);
  }


  @NotNull
  @Override
  protected CoreEditorTurn reallyGetTurnPanel(ClientExercise clientExercise, ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index) {
    return new CoreEditorTurn(
        controller,
        this,
        clientExercise,
        controller.getLanguageInfo(),
        false,
        getDialogID());
  }
}
