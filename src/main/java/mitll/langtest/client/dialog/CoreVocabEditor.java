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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.IFocusable;
import mitll.langtest.client.scoring.SimpleTurn;
import mitll.langtest.shared.dialog.DialogExChangeResponse;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.custom.INavigation.VIEWS.CORE_EDITOR;
import static mitll.langtest.client.custom.INavigation.VIEWS.LISTEN;
import static mitll.langtest.client.dialog.ITurnContainer.COLUMNS.UNK;

public class CoreVocabEditor extends TurnViewHelper<CoreEditorTurn>
    implements IFocusable, /*IFocusListener,*/ IEditableTurnContainer<CoreEditorTurn>, ITurnContainer<CoreEditorTurn> {
  private final Logger logger = Logger.getLogger("CoreVocabEditor");
  private static final boolean DEBUG_BLUR = false;

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


  TurnViewHelper<SimpleTurn> leftHelper;

  @NotNull
  public DivWidget getTurns(IDialog dialog) {
    if (getDialog() == null) {
      logger.info("getTurns set dialog " + dialog);
      setDialog(dialog);
    }
    DivWidget leftRight = new DivWidget();
    leftRight.setWidth("100%");
    leftRight.addStyleName("inlineFlex");

    {
      TurnViewHelper<SimpleTurn> leftHelper = getLeftHelper();
      leftHelper.setDialog(dialog);
      this.leftHelper = leftHelper;
      DivWidget turns = leftHelper.getTurns(dialog);
      turns.setWidth("60%");
      leftRight.add(turns);
    }

    DivWidget coreVocabTurns = super.getTurns(dialog);
    coreVocabTurns.setWidth("40%");
    styleTurnContainer(coreVocabTurns);
    leftRight.add(coreVocabTurns);

    return leftRight;
  }

   void setHighlights() {
    Set<String> allText = new HashSet<>();
    getAllTurns().forEach(turn -> allText.add(turn.getContent()));
//    leftHelper.getAllTurns().forEach(SimpleTurn::restoreText);

  //  logger.info("core " +allText);
    leftHelper.getAllTurns().forEach(turn -> {
      turn.restoreText();
      turn.maybeObscure(allText);
      turn.obscureText();
    });
  }

  private TurnViewHelper<SimpleTurn> getLeftHelper() {
    return new TurnViewHelper<SimpleTurn>(controller, CORE_EDITOR) {
      @Override
      protected void addTurnPerExercise(IDialog dialog, DivWidget rowOne, String left, String right) {
        rowOne.clear();
        List<ClientExercise> exercises = dialog.getExercises();
        exercises = exercises.stream().filter(exercise ->
            !exercise.hasEnglishAttr() && !exercise.getForeignLanguage().isEmpty()).collect(Collectors.toList());
        addTurnForEachExercise(rowOne, left, right, exercises);
      }

      @NotNull
      @Override
      SimpleTurn getTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
        SimpleTurn turnPanel = super.getTurnPanel(clientExercise, columns, prevColumn, index);

        IDialog dialog = getDialog();
        if (dialog == null) {
          logger.warning("no dialog?");
        } else {
          turnPanel.maybeSetObscure(this.dialog.getCoreVocabulary());
          turnPanel.obscureText();
        }

        return turnPanel;
      }

      @NotNull
      @Override
      protected SimpleTurn reallyGetTurnPanel(ClientExercise clientExercise, ITurnContainer.COLUMNS columns, ITurnContainer.COLUMNS prevColumn, int index) {
        return new SimpleTurn(clientExercise, columns, false, controller.getLanguageInfo());
      }
    };
  }

  @Override
  public void addTurnForSameSpeaker(CoreEditorTurn editorTurn) {
    int exID = editorTurn.getExID();
    int index = allTurns.indexOf(editorTurn);
    controller.getDialogService().addEmptyCoreExercise(getDialogID(), exID, new AsyncCallback<DialogExChangeResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("adding core vocab to a dialog.", caught);
      }

      @Override
      public void onSuccess(DialogExChangeResponse result) {
        addTurns(result.getUpdated(), result.getChanged(), index + 1, turnContainer, exID);
      }
    });
  }

  @Override
  public void addTurnForOtherSpeaker(CoreEditorTurn editorTurn) {

  }

  @Override
  public void deleteCurrentTurnOrPair(CoreEditorTurn currentTurn) {
    int exID = currentTurn.getExID();
    //logger.info("deleteCurrentTurnOrPair : " + "\n\tcurrent turn " + exID);
    startDelete(currentTurn);

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
              gotDeleteResponse(Collections.singletonList(exID));
              setHighlights();
            } else logger.warning("huh?");
          }
        });
  }

  /**
   * @param exercises
   * @param afterThisTurn
   * @see #getAsyncForNewTurns
   */
  private void addTurns(IDialog updated, List<ClientExercise> changed, int index, DivWidget turnContainer, int exid) {
    this.setDialog(updated);

    for (ClientExercise clientExercise : changed) {
      CoreEditorTurn turn = addTurn(turnContainer, clientExercise, UNK, UNK, index);
      turn.addStyleName("opacity-target");
    }
    makeNextTheCurrentTurn(getNextTurn(exid));
  }

  protected void addControls(DivWidget controlAndSpeakers) {
  }

  @Override
  public void grabFocus() {
    //if (!getAllTurns().isEmpty()) getAllTurns().iterator().next().grabFocus();

    if (getCurrentTurn() != null) {
      // logger.info("give focus to turn for ex #" + getCurrentTurn().getExID());
      getCurrentTurn().grabFocus();
      // markCurrent();
    }
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

    if (coreVocabulary == null) {
      logger.warning("huh? core vocab is null???");
    } else {
      coreVocabulary.forEach(clientExercise -> logger.info("Got " + clientExercise.getID() + " " + clientExercise.getForeignLanguage()));
      List<ClientExercise> toUse = coreVocabulary.isEmpty() ? new ArrayList<>() : coreVocabulary;
      if (toUse.isEmpty()) toUse.add(new Exercise());

      addTurnForEachExercise(rowOne, left, right, toUse);
    }
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

  @Override
  public void gotForward(CoreEditorTurn editorTurn) {
    if (isLast(editorTurn)) {
      // make either one or two more turns and add to end of dialog
      addTurnForSameSpeaker(editorTurn);
    } else {
      super.gotForward(editorTurn);
      getCurrentTurn().grabFocus();
    }
  }

  @Override
  public boolean isInterpreter() {
    return dialog.getKind() == DialogType.INTERPRETER;
  }

  @Override
  public int getVolume() {
    return 0;
  }

  @Override
  public void gotBlur(CoreEditorTurn widgets) {
    boolean last = isLast(widgets);
    if (last) {
      if (widgets.isDeleting()) {
        if (DEBUG_BLUR) logger.info("gotBlur ignore blur of " + widgets.getExID());

      } else {
//        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("gotBlur"));
//        logger.info("logException stack " + exceptionAsString);

        if (DEBUG_BLUR) {
          logger.info("gotBlur got blur of '" + //widgets.getText() +
              " : " + widgets.isDeleting());
        }

        moveFocusToNext();
      }
    }
  }
}
