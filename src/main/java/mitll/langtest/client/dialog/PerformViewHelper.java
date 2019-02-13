package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

public class PerformViewHelper<T extends RecordDialogExercisePanel> extends RehearseViewHelper<T> {
  // private final Logger logger = Logger.getLogger("PerformViewHelper");

  private static final String RED_RECORD_BUTTON = "Speak when you see the red record button.";

 // private Set<String> uniqueCoreVocab;

  public PerformViewHelper(ExerciseController controller) {
    super(controller, INavigation.VIEWS.PERFORM);
    rehearsalKey = "PerformViewKey";
    rehearsalPrompt = RED_RECORD_BUTTON;
  }

  @Override
  public boolean isRehearse() {
    return false;
  }

  public INavigation.VIEWS getView() {
    return INavigation.VIEWS.PERFORM;
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

//  @Override
//  protected boolean shouldShowScoreNow() {
//    return false;
//  }

  /**
   * OK, let's go - hide everything!
   *
   * @param clientExercise
   * @param isRight
   * @return
   */
  @NotNull
  @Override
  protected T getTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T turnPanel = super.getTurnPanel(clientExercise, columns);
    turnPanel.reallyObscure();
    return turnPanel;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.CORE_REHEARSE;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.SCORES;
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
//    getPromptSeq().forEach(RecordDialogExercisePanel::obscureText);
//    getRespSeq().forEach(RecordDialogExercisePanel::restoreText);
    allTurns.forEach(RecordDialogExercisePanel::obscureText);
  }
}
