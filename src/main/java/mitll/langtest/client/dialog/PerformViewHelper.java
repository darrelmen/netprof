package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.RehearseViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public class PerformViewHelper<T extends RecordDialogExercisePanel> extends RehearseViewHelper<T> {
  // private final Logger logger = Logger.getLogger("PerformViewHelper");
  private Set<String> uniqueCoreVocab;

  public PerformViewHelper(ExerciseController controller) {
    super(controller);
  }

  protected INavigation.VIEWS getView() {
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
    uniqueCoreVocab = dialog.getCoreVocabulary()
        .stream()
        .map(CommonShell::getForeignLanguage)
        .collect(Collectors.toSet());
    DivWidget turns = super.getTurns(dialog);
    Scheduler.get().scheduleDeferred(this::obscureRespTurns);
    return turns;
  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   */
  @NotNull
  @Override
  protected T getTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T turnPanel = super.getTurnPanel(clientExercise, columns);
    if (columns != COLUMNS.MIDDLE) {
      turnPanel.reallyObscure();
    }
    else {
//      turnPanel.maybeSetObscure(uniqueCoreVocab);

    }
    return turnPanel;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.REHEARSE;
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
    getPromptSeq().forEach(RecordDialogExercisePanel::restoreText);
    getRespSeq().forEach(RecordDialogExercisePanel::obscureText);
  }
}
