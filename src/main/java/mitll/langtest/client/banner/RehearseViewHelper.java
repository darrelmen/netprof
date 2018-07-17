package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.CheckBox;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.DialogExercisePanel;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper extends ListenViewHelper {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");


  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    super(controller,viewContainer,myView);
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
  }

  protected void speakerOneCheck(Boolean value) {
    logger.info("speaker one now " + value);
    leftSpeaker = value;
    rightSpeakerBox.setValue(rightSpeaker = !value);
    setPlayButtonToPlay();
  }

  protected void speakerTwoCheck(Boolean value) {
    logger.info("speaker two now " + value);
    rightSpeaker = value;
    leftSpeakerBox.setValue(leftSpeaker = !value);
    setPlayButtonToPlay();
  }

  /**
   * @see #getTurnPanel
   * @param clientExercise
   * @param isRight
   * @return
   */
  @NotNull
  @Override
  protected DialogExercisePanel<ClientExercise> reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
//    return super.reallyGetTurnPanel(clientExercise);
    logger.info("making record dialog ex panel for " + clientExercise.getID());
    RecordDialogExercisePanel<ClientExercise> widgets = new RecordDialogExercisePanel<>(clientExercise, controller, null, alignments, this);
   // widgets.addWidgets(true,false,PhonesChoices.HIDE);
    widgets.setIsRight(isRight);

    return widgets;
  }

  protected void currentTurnPlayEnded() {
    if (DEBUG) logger.info("currentTurnPlayEnded - turn " + currentTurn.getExID());
    List<DialogExercisePanel> seq = getSeq();

    int i2 = bothTurns.indexOf(currentTurn);

    int nextOtherSide=i2+1;
    if (nextOtherSide<bothTurns.size()-1) {
      RecordDialogExercisePanel dialogExercisePanel = (RecordDialogExercisePanel)bothTurns.get(nextOtherSide);
      dialogExercisePanel.startRecording();
    }
    else {
      removeMarkCurrent();
      currentTurn = seq.get(0);
      markCurrent();
    }

    int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (i1 > seq.size() - 1) {
      if (DEBUG) logger.info("OK stop");
      removeMarkCurrent();
      currentTurn = seq.get(0);
      markCurrent();
    } else {
      currentTurn = seq.get(i1);
     // playCurrentTurn();
    }
  }

}
