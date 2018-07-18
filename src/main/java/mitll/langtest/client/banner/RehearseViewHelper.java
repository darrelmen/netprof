package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.WavEndCallback;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper<T extends RecordDialogExercisePanel<ClientExercise>> extends ListenViewHelper<T> {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");


  private static final boolean DEBUG = true;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    super(controller, viewContainer, myView);
    controller.registerStopDetected(this::silenceDetected);
  }

  private void silenceDetected() {
    //  logger.info("silenceDetected got silence");
    for (T recordDialogExercisePanel : bothTurns) {
      if (recordDialogExercisePanel.isRecording()) {
        //    logger.info("\tsilenceDetected recordDialogExercisePanel is recording");
        if (recordDialogExercisePanel.stopRecording()) {
          Timer timer = new Timer() {
            @Override
            public void run() {
//            logger.warning("waited " + (System.currentTimeMillis() - then) + " for a response");
              // setSplash(CHECK_NETWORK_WIFI);
              //controller.setHasNetworkProblem(true);
              currentTurnPlayEnded();
            }
          };
          timer.schedule(500);
        }
      }
    }
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
  }

  protected void speakerOneCheck(Boolean value) {
    logger.info("speaker one now " + value);

    rightSpeakerBox.setValue(!value);
    setPlayButtonToPlay();
  }

  protected void speakerTwoCheck(Boolean value) {
    logger.info("speaker two now " + value);

    leftSpeakerBox.setValue(!value);
    setPlayButtonToPlay();
  }

  /**
   * @param clientExercise
   * @param isRight
   * @return
   * @see #getTurnPanel
   */
  @NotNull
  @Override
  protected T reallyGetTurnPanel(ClientExercise clientExercise, boolean isRight) {
//    return super.reallyGetTurnPanel(clientExercise);
    // logger.info("making record dialog ex panel for " + clientExercise.getID());
    T widgets =
        (T) new RecordDialogExercisePanel<ClientExercise>(clientExercise, controller, null, alignments, this);
    // widgets.addWidgets(true,false,PhonesChoices.HIDE);
    widgets.setIsRight(isRight);

    return widgets;
  }

  protected void currentTurnPlayEnded() {
    if (DEBUG) logger.info("currentTurnPlayEnded - turn " + currentTurn.getExID());
    List<T> seq = getSeq();

    boolean isCurrentPrompt = seq.contains(currentTurn);

    int i2 = bothTurns.indexOf(currentTurn);
    int nextOtherSide = i2 + 1;

    logger.info("seq " + seq.size() +
        "\n\tleft  " + isLeftSpeakerSet() +
        "\n\tright " + isRightSpeakerSet() +
        "\n\tboth " + bothTurns.size() +
        "\n\t is current playing " + isCurrentPrompt +
        "\n\ti2    " + i2 +
        "\n\tnext " + nextOtherSide
    );

    if (nextOtherSide < bothTurns.size()) {
      T nextTurn = bothTurns.get(nextOtherSide);
      removeMarkCurrent();
      currentTurn = nextTurn;
      markCurrent();

      if (isCurrentPrompt) {
        logger.info("currentTurnPlayEnded - startRecording " + currentTurn.getExID());
        currentTurn.startRecording();
      } else {
        logger.info("currentTurnPlayEnded - play current " + currentTurn.getExID());
        playCurrentTurn();
      }
    } else {
      removeMarkCurrent();
      currentTurn = seq.get(0);
      markCurrent();
    }

  /*  int i = seq.indexOf(currentTurn);
    int i1 = i + 1;
    if (i1 > seq.size() - 1) {
      if (DEBUG) logger.info("OK stop");
      removeMarkCurrent();
      currentTurn = seq.get(0);
      markCurrent();
    } else {
      currentTurn = seq.get(i1);
      // playCurrentTurn();
    }*/
  }

}
