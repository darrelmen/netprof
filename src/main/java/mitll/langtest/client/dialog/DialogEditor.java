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
import mitll.langtest.client.custom.dialog.DialogEditorView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

public class DialogEditor extends ListenViewHelper<EditorTurn> {
  private final Logger logger = Logger.getLogger("DialogEditor");

  private int dialogID;

  public DialogEditor(ExerciseController controller, INavigation.VIEWS thisView, int dialogID) {
    super(controller, thisView);
    this.dialogID = dialogID;
  }

  @Override
  @NotNull
  protected EditorTurn makeTurnPanel(ClientExercise clientExercise, COLUMNS columns, boolean rightJustify) {
    return new EditorTurn(
        clientExercise,
        columns,
        rightJustify, controller.getLanguageInfo(), controller,
        this,
        dialogID);
  }

  @Override
  protected void gotTurnClick(EditorTurn turn) {
    super.gotTurnClick(turn);
    markCurrent();
  }

  public int getDialogID() {
    return dialogID;
  }

  @Override
  protected void styleTurnContainer(DivWidget rowOne) {
    super.styleTurnContainer(rowOne);
    rowOne.getElement().getStyle().setOverflow(Style.Overflow.SCROLL);
    rowOne.setHeight("390px");
  }

  @Override
  protected void addDialogHeader(IDialog dialog, Panel child) {
  }

  /**
   * If on the last turn, either make a new pair of exercises for interpreter, or just a new exercise
   * for a simple dialog.
   */
  @Override
  public void gotForward() {
    if (onLastTurn()) {
      // make either one or two more turns and add to end of dialog
      boolean isLeftSpeaker = isInterpreter ? getPrevTurn().getColumns() == COLUMNS.LEFT : getCurrentTurn().getColumns() == COLUMNS.LEFT;

      controller.getDialogService().addEmptyExercises(dialogID, -1, isLeftSpeaker, new AsyncCallback<List<ClientExercise>>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(List<ClientExercise> result) {
          addTurns(result);
          gotForward();
          getCurrentTurn().grabFocus();
        }
      });
    } else {
      super.gotForward();
      getCurrentTurn().grabFocus();
    }
  }

  @Override
  protected int getDialogFromURL() {
    return dialogID;
  }

  @NotNull
  protected INavigation.VIEWS getPrevView() {
    return null;
  }

  @NotNull
  protected INavigation.VIEWS getNextView() {
    return null;
  }

  /**
   * @see DialogEditorView.MyShownCloseListener#gotShown
   */
  public void grabFocus() {
    logger.info("give focus to turn for ex #" + getCurrentTurn().getExID());
    getCurrentTurn().grabFocus();
  }
}
