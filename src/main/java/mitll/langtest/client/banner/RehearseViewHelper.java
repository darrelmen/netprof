package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class RehearseViewHelper<T extends RecordDialogExercisePanel<ClientExercise>> extends ListenViewHelper<T> {
  private final Logger logger = Logger.getLogger("RehearseViewHelper");


  private static final boolean DEBUG = false;

  private static final int DELAY_MILLIS = 100;

  private ProgressBar scoreProgress;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    super(controller, viewContainer, myView);
    controller.registerStopDetected(this::silenceDetected);
  }

  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    super.showContent(listContent, instanceName, fromClick);

    DivWidget breadRow = new DivWidget();
    breadRow.setWidth("100%");
    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(60, Style.Unit.PX);
    style.setMarginLeft(135, Style.Unit.PX);
    style.setClear(Style.Clear.BOTH);

    breadRow.getElement().setId("breadRow");
    breadRow.add(showScoreFeedback());

    listContent.add(breadRow);
  }

  private Image smiley = new Image();

  private DivWidget showScoreFeedback() {
    DivWidget container = new DivWidget();

    container.setId("feedbackContainerAndBar");

    {
      DivWidget iconContainer = new DivWidget();
      iconContainer.addStyleName("floatLeft");

      iconContainer.add(smiley);
      setSmiley(smiley, 0.8);

      container.add(iconContainer);
    }

    {
      DivWidget scoreContainer = new DivWidget();
      scoreContainer.addStyleName("floatLeft");

      scoreContainer.addStyleName("topMargin");
      scoreContainer.addStyleName("leftFiveMargin");
      scoreContainer.add(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
      styleProgressBarContainer(scoreProgress);

      scoreContainer.setWidth("73%");

      container.setWidth("78%");
      container.getElement().getStyle().setMarginLeft(20, Style.Unit.PCT);
      container.add(scoreContainer);
    }

    return container;
  }

  private void styleProgressBarContainer(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    style.setHeight(25, Style.Unit.PX);
    style.setFontSize(16, Style.Unit.PX);

    progressBar.setWidth("49%");
    progressBar.setVisible(false);
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
          timer.schedule(DELAY_MILLIS);
        }
      }
    }
  }

  private final Map<Integer, Float> exToScore = new HashMap<>();

  @Override
  public void addScore(int exid, float score) {
    exToScore.put(exid, score);
    showScore(isRightSpeakerSet() ? leftTurnPanels.size() : rightTurnPanels.size());
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
      // showScore();
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

  private void showScore(int expected) {
    int num = exToScore.values().size();

    if (num == expected) {
      double total = 0D;
      for (Float score : exToScore.values()) {
        total += score;
      }

      logger.info("showScore showing " + num);
      total /= (float) num;
      logger.info("showScore total   " + total);

      double percent = total * 100;
      double round = percent;// Math.max(percent, 30);
      if (percent == 0d) round = 100d;
      scoreProgress.setPercent(num == 0 ? 100 : percent);
      scoreProgress.setVisible(true);
      scoreProgress.setText("Score " + Math.round(percent) + "%");
      new ScoreProgressBar(false).setColor(scoreProgress, total, round, false);
      setSmiley(smiley, total);
    }

  }

  private void setSmiley(Image smiley, double total) {
    String choice;

    if (total < 0.3) {
      choice = "frowning.png";
    } else if (total < 0.4) {
      choice = "confused.png";
    } else if (total < 0.5) {
      choice = "thinking.png";
    } else if (total < 0.6) {
      choice = "neutral.png";
    } else if (total < 0.7) {
      choice = "smiling.png";
    } else {
      choice = "grinning.png";
    }

    //   smiley = new Image(LangTest.LANGTEST_IMAGES + choice);
    smiley.setUrl(LangTest.LANGTEST_IMAGES + choice);
  }

}
