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
import mitll.langtest.client.scoring.IRecordDialogTurn;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

  private static final int DELAY_MILLIS = 20;

  private ProgressBar scoreProgress;
  /**
   *
   */
  private Image smiley = new Image();

  private final Map<Integer, Float> exToScore = new HashMap<>();
  private List<IRecordDialogTurn> recordDialogTurns = new ArrayList<>();

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  RehearseViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    super(controller, viewContainer, myView);
    controller.registerStopDetected(this::silenceDetected);
  }

  private DivWidget overallFeedback;

  /**
   * @param dialog
   * @param child
   */
  @Override
  protected void showDialogGetRef(IDialog dialog, Panel child) {
    super.showDialogGetRef(dialog, child);
    child.add(overallFeedback = getOverallFeedback());
  }

  @NotNull
  private DivWidget getOverallFeedback() {
    DivWidget breadRow = new DivWidget();
    //breadRow.setWidth("100%");
    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(10, Style.Unit.PX);
    style.setMarginLeft(135, Style.Unit.PX);
    style.setMarginBottom(10, Style.Unit.PX);
    style.setClear(Style.Clear.BOTH);
    //   style.setPosition(Style.Position.FIXED);

    breadRow.getElement().setId("breadRow");
    breadRow.add(showScoreFeedback());
    return breadRow;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.LISTEN;
  }

  @NotNull
  @Override
  protected INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.REHEARSE;
  }

  private boolean directClick = false;

  @Override
  protected void gotCardClick(T turn) {
    directClick = true;
    super.gotCardClick(turn);
  }

  /**
   * @return
   */
  private DivWidget showScoreFeedback() {
    DivWidget container = new DivWidget();

    container.setId("feedbackContainerAndBar");

    {
      DivWidget iconContainer = new DivWidget();
      iconContainer.addStyleName("floatLeft");

      iconContainer.add(smiley);
      smiley.setVisible(false);
      container.add(iconContainer);
    }

    {
      DivWidget scoreContainer = new DivWidget();
      scoreContainer.addStyleName("rehearseScoreContainer");
      scoreContainer.addStyleName("floatLeft");

      scoreContainer.addStyleName("topMargin");
      scoreContainer.addStyleName("leftFiveMargin");
      scoreContainer.add(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
      styleProgressBarContainer(scoreProgress);

      scoreContainer.setWidth("73%");

      container.setWidth("78%");
      Style style = container.getElement().getStyle();
      style.setOverflow(Style.Overflow.HIDDEN);
      style.setMarginLeft(20, Style.Unit.PCT);
      style.setMarginBottom(20, Style.Unit.PX);
      container.add(scoreContainer);
    }

    return container;
  }

/*  @NotNull
  private DivWidget getProgressBar(double score, boolean isFullMatch) {
    DivWidget widgets = new ScoreProgressBar().showScore(score * 100, isFullMatch);
    widgets.setHeight("25px");
    return widgets;
  }*/

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
          //  currentTurnPlayEnded();


          Timer timer = new Timer() {
            @Override
            public void run() {
              currentTurnPlayEnded();
            }
          };
          timer.schedule(DELAY_MILLIS);

        }
        break;
      }
    }
  }

  /**
   * Forget about scores after showing them...
   *
   * @param exid
   * @param score
   * @param recordDialogTurn
   */
  @Override
  public void addScore(int exid, float score, IRecordDialogTurn recordDialogTurn) {
    exToScore.put(exid, score);
    recordDialogTurns.add(recordDialogTurn);

    if (showScore(isRightSpeakerSet() ? leftTurnPanels.size() : rightTurnPanels.size())) {
      recordDialogTurns.forEach(IRecordDialogTurn::showScoreInfo);
    }
  }

  protected void setRightTurnInitialValue(CheckBox checkBox) {
  }

  protected void speakerOneCheck(Boolean value) {
    rightSpeakerBox.setValue(!value);
    setPlayButtonToPlay();
  }

  protected void speakerTwoCheck(Boolean value) {
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
        (T) new RecordDialogExercisePanel<ClientExercise>(clientExercise, controller, null, alignments, this, isRight);
    // widgets.addWidgets(true,false,PhonesChoices.HIDE);
    // widgets.setIsRight(isRight);

    return widgets;
  }

  @Override
  protected void gotPlay() {
    super.gotPlay();

    if (onFirstTurn()) {
      clearScores();
    }
  }

  private void clearScores() {
    smiley.setVisible(false);
    scoreProgress.setVisible(false);
    exToScore.clear();

    bothTurns.forEach(IRecordDialogTurn::clearScoreInfo);
    recordDialogTurns.clear();
  }

  /**
   *
   */
  protected void currentTurnPlayEnded() {
    if (directClick) {
      directClick = false;
      markCurrent();
    } else {
      T currentTurn = getCurrentTurn();
      if (DEBUG) logger.info("currentTurnPlayEnded (rehearse) - turn " + currentTurn.getExID());
      List<T> seq = getSeq();

      boolean isCurrentPrompt = seq.contains(currentTurn);

      int i2 = bothTurns.indexOf(currentTurn);
      int nextOtherSide = i2 + 1;

      if (DEBUG) {
        logger.info("currentTurnPlayEnded" +
            "\n\t seq  " + seq.size() +
            "\n\tleft  " + isLeftSpeakerSet() +
            "\n\tright " + isRightSpeakerSet() +
            "\n\tboth  " + bothTurns.size() +
            "\n\t is current playing " + isCurrentPrompt +
            "\n\ti2    " + i2 +
            "\n\tnext  " + nextOtherSide
        );
      }

      if (nextOtherSide < bothTurns.size()) {
        T nextTurn = bothTurns.get(nextOtherSide);

        if (DEBUG) logger.info("currentTurnPlayEnded next is " + nextTurn.getId() + "  at " + nextOtherSide);
        removeMarkCurrent();
        setCurrentTurn(nextTurn);
        markCurrent();
        makeCurrentTurnVisible();
        if (isCurrentPrompt) {
          if (DEBUG) logger.info("currentTurnPlayEnded - startRecording " + nextTurn.getExID());
          nextTurn.startRecording();
        } else {
          if (DEBUG) logger.info("currentTurnPlayEnded - play current " + nextTurn.getExID());
          playCurrentTurn();
        }
      } else {
//        removeMarkCurrent();
        // currentTurn = seq.get(0);
        //   markCurrent();
      }
    }
  }

  private boolean showScore(int expected) {
    int num = exToScore.values().size();

    if (num == expected) {
      double total = getTotal();
      // logger.info("showScore showing " + num);
      total /= (float) num;
      //  logger.info("showScore total   " + total);

      double percent = total * 100;
      double round = percent;// Math.max(percent, 30);
      if (percent == 0d) round = 100d;
      scoreProgress.setPercent(num == 0 ? 100 : percent);
      scoreProgress.setVisible(true);
      scoreProgress.setText("Score " + Math.round(percent) + "%");

      makeVisible(overallFeedback);

      new ScoreProgressBar(false).setColor(scoreProgress, total, round);

      setSmiley(smiley, total);

      smiley.setVisible(true);

      return true;
    } else return false;
  }


  private double getTotal() {
    double total = 0D;
    for (Float score : exToScore.values()) {
      total += score;
    }
    return total;
  }

  @Override
  public void setSmiley(Image smiley, double total) {
    String choice;

    if (total < 0.3) {
      choice = "frowningEmoticon.png";
    } else if (total < 0.4) {
      choice = "confusedEmoticon.png";
    } else if (total < 0.5) {
      choice = "thinkingEmoticon.png";
    } else if (total < 0.6) {
      choice = "neutralEmoticon.png";
    } else if (total < 0.7) {
      choice = "smilingEmoticon.png";
    } else {
      choice = "grinningEmoticon.png";
    }

    smiley.setUrl(LangTest.LANGTEST_IMAGES + choice);
  }
}
