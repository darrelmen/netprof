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
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.ObscureRecordDialogExercisePanel;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

public class PerformViewHelper<T extends ObscureRecordDialogExercisePanel> extends RehearseViewHelper<T> {
  // private final Logger logger = Logger.getLogger("PerformViewHelper");
  /**
   * @see mitll.langtest.client.banner.NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   * @param controller
   * @param thisView
   */
  public PerformViewHelper(ExerciseController controller, INavigation.VIEWS thisView) {
    super(controller, thisView);
    rehearsalKey = "PerformViewKey";
  }

  /**
   * don't have to filter on client...?
   *
   * @param dialog
   * @return
   */
  @NotNull
  @Override
  protected DivWidget getTurns(IDialog dialog) {
    DivWidget turns = super.getTurns(dialog);
    Scheduler.get().scheduleDeferred(this::obscureRespTurns);
    return turns;
  }

  /**
   * OK, let's go - hide everything!
   *
   * @param isRight
   * @param clientExercise
   * @param prevColumn
   * @param index
   * @return
   */
  @NotNull
  @Override
  protected T getTurnPanel(ClientExercise clientExercise, COLUMNS columns, COLUMNS prevColumn, int index) {
    T turnPanel = super.getTurnPanel(clientExercise, columns, prevColumn, index);
    turnPanel.obscureTextAndPhones();
    return turnPanel;
  }

  protected T makeRecordingTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    return (T) new ObscureRecordDialogExercisePanel(clientExercise, controller,
        null, alignments, this, this, columns);
  }

  /**
   * Move current turn to first turn when we switch who is the prompting speaker.
   */
  @Override
  protected void gotSpeakerChoice() {
    super.gotSpeakerChoice();
    obscureRespTurns();
  }

  @Override
  protected void clearScores() {
    super.clearScores();
    obscureRespTurns();
  }

  private void obscureRespTurns() {
    allTurns.forEach(ObscureRecordDialogExercisePanel::obscureText);
  }
}
