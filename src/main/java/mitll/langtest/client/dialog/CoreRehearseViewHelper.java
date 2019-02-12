package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CoreRehearseViewHelper<T extends RecordDialogExercisePanel> extends RehearseViewHelper<T> {
  private final Logger logger = Logger.getLogger("CoreRehearseViewHelper");

  public static final String RED_RECORD_BUTTON = "Speak when you see the red record button.";

  // private Set<String> uniqueCoreVocab;
  private Map<String, ClientExercise> exidToShell = new HashMap<>();

  public CoreRehearseViewHelper(ExerciseController controller) {
    super(controller);
    rehearsalKey = "PerformViewKey";
    rehearsalPrompt = RED_RECORD_BUTTON;
  }

  public INavigation.VIEWS getView() {
    return INavigation.VIEWS.CORE_REHEARSE;
  }

  /**
   * don't have to filter on client...?
   *
   * @param dialog
   * @return
   * @see ListenViewHelper#showDialog
   */
  @NotNull
  @Override
  protected DivWidget getTurns(IDialog dialog) {

    dialog.getCoreVocabulary().forEach(clientExercise -> exidToShell.put(clientExercise.getOldID(), clientExercise));
//    uniqueCoreVocab = dialog.getCoreVocabulary()
//        .stream()
//        .map(CommonShell::getForeignLanguage)
//        .collect(Collectors.toSet());

    // uniqueCoreVocab.forEach(ex->logger.info("core " + ex));
    DivWidget turns = super.getTurns(dialog);
    Scheduler.get().scheduleDeferred(this::obscureRespTurns);
    return turns;
  }

  @Override
  protected boolean shouldShowScoreNow() {
    return false;
  }

  /**
   * OK, let's go - hide everything!
   *
   * @param clientExercise
   * @param isRight
   * @return
   * @see #addTurn
   */
  @NotNull
  @Override
  protected T getTurnPanel(ClientExercise clientExercise, COLUMNS columns) {
    T turnPanel = super.getTurnPanel(clientExercise, columns);
    if (columns == COLUMNS.MIDDLE) {
      if (!clientExercise.hasEnglishAttr()) {
        turnPanel.maybeSetObscure(exidToShell);
      }
    } else {
      turnPanel.reallyObscure();
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
    return INavigation.VIEWS.PERFORM;
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
//    getRespSeq().forEach(RecordDialogExercisePanel::obscureText);
    allTurns.forEach(RecordDialogExercisePanel::obscureText);
  }
}
