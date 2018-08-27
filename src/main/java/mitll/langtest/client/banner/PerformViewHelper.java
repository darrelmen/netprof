package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PerformViewHelper<T extends RecordDialogExercisePanel<ClientExercise>> extends RehearseViewHelper<T> {
  private final Logger logger = Logger.getLogger("PerformViewHelper");

  private Set<String> uniqueCoreVocab;

  PerformViewHelper(ExerciseController controller) {
    super(controller);
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
    List<ClientExercise> coreVocabulary = dialog.getCoreVocabulary();

    Map<String, String> unitToValue = dialog.getUnitToValue();
    String unitDialogIsIn = unitToValue.get(Dialog.UNIT);

    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();
    String unit = typeOrder.get(0);
    String chapter = typeOrder.size() > 1 ? typeOrder.get(1) : null;

    logger.info("getTurns match dialog " + unitToValue + " ");
    uniqueCoreVocab = coreVocabulary
        .stream()
        .filter(clientExercise -> clientExercise.getUnitToValue().get(unit).equalsIgnoreCase(unitDialogIsIn))
        .map(CommonShell::getForeignLanguage)
        .collect(Collectors.toSet());

    logger.info("getTurns uniqueCoreVocab " + uniqueCoreVocab + " ");

    DivWidget turns = super.getTurns(dialog);

    Scheduler.get().scheduleDeferred(this::obscureRespTurns);
    return turns;
  }

//  @NotNull
//  @Override
//  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
//    T widgets = super.reallyGetTurnPanel(clientExercise, isRight);
//    widgets.maybeSetObscure(uniqueCoreVocab);
//    return widgets;
//  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   */
  @NotNull
  @Override
  protected T getTurnPanel(ClientExercise clientExercise, boolean isRight) {
    T turnPanel = super.getTurnPanel(clientExercise, isRight);
    turnPanel.maybeSetObscure(uniqueCoreVocab);
    return turnPanel;
  }

  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
    // logger.info("making record dialog ex panel for " + clientExercise.getID());
    T turnPanel =
        (T) new RecordDialogExercisePanel<ClientExercise>(clientExercise, controller,
            null, alignments, this, this, isRight);

    exToTurn.put(clientExercise.getID(), turnPanel);
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

  private void obscureRespTurns() {
    getSeq().forEach(RecordDialogExercisePanel::restoreText);
    getRespSeq().forEach(RecordDialogExercisePanel::obscureText);
  }
}
