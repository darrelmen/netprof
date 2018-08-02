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

  private static final boolean DEBUG = true;

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
/*

  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    super.showContent(listContent, instanceName, fromClick);

    DivWidget breadRow = getOverallFeedback();

    listContent.add(breadRow);
  }
*/

  @NotNull
  private DivWidget getOverallFeedback() {
    DivWidget breadRow = new DivWidget();
    breadRow.setWidth("100%");
    Style style = breadRow.getElement().getStyle();
    style.setMarginTop(60, Style.Unit.PX);
    style.setMarginLeft(135, Style.Unit.PX);
    style.setBottom(10, Style.Unit.PX);
    style.setClear(Style.Clear.BOTH);
    style.setPosition(Style.Position.FIXED);

    breadRow.getElement().setId("breadRow");
    breadRow.add(showScoreFeedback());
    return breadRow;
  }

  @Override
  protected void showDialogGetRef(IDialog dialog, DivWidget child) {
    super.showDialogGetRef(dialog, child);
    child.add(getOverallFeedback());
  }

  protected void gotGoBack() {
    controller.getNavigation().show(INavigation.VIEWS.LISTEN);
  }

  protected void gotGoForward() {
    controller.getNavigation().show(INavigation.VIEWS.REHEARSE);
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
      container.getElement().getStyle().setMarginLeft(20, Style.Unit.PCT);
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
    ;
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

  protected void currentTurnPlayEnded() {
    if (directClick) {
      directClick = false;
      markCurrent();
    } else {
      if (DEBUG) logger.info("currentTurnPlayEnded (rehearse) - turn " + currentTurn.getExID());
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
    // smiley.setVisible(true);
  }

}
