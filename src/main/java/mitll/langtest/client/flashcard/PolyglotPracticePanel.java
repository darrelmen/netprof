package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.PolyglotChart;
import mitll.langtest.client.download.SpeedChoices;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonAnnotatable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

public class PolyglotPracticePanel<L extends CommonShell, T extends CommonExercise> extends StatsPracticePanel<L, T> {

  private static final int MIN_POLYGLOT_SCORE = 35;

  private static final float MIN_SCORE_F = ((float) MIN_POLYGLOT_SCORE) / 100f;

  //  private static final int FEEDBACK_SLOTS = 4;
  private static final int FEEDBACK_SLOTS_POLYGLOT = 5;
  private static final int NEXT_EXERCISE_DELAY = 750;
  private static final int INCORRECT_BEFORE_ADVANCE = 3;

  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;
  private static final int DRY_RUN_ROUND_TIME = DRY_RUN_MINUTES * 60 * 1000;
  private static final int ROUND_TIME = ROUND_MINUTES * 60 * 1000;
  private static final int CHART_HEIGHT = 120;
  private static final String TIME_LEFT = "Time left";

  private static final long ONE_MIN = (60L * 1000L);
//  private static final String TRY_AGAIN = "Try Again?";
 // private static final String START_OVER_FROM_THE_BEGINNING = "Start over from the beginning.";


  /**
   * @seex StatsPracticePanel#showTimeRemaining
   */
  private static final String TIMES_UP = "Times Up!";

  //private static final int MIN_POLYGLOT_SCORE = 35;



  private Label timeLeft;

  private final PolyglotFlashcardContainer polyglotFlashcardContainer;
  private int wrongCount = 0;

  public PolyglotPracticePanel(PolyglotFlashcardContainer statsFlashcardFactory,
                               ControlState controlState, ExerciseController controller, MySoundFeedback soundFeedback, PolyglotDialog.PROMPT_CHOICE prompt, CommonAnnotatable e, StickyState stickyState, ListInterface exerciseListToUse) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, prompt, e, stickyState, exerciseListToUse);
    this.polyglotFlashcardContainer = statsFlashcardFactory;

    realAddWidgets(e, controller, controlState);
  }


  @Override
  void addWidgets(CommonAnnotatable e, ExerciseController controller, ControlState controlState) {
//    super.addWidgets(e, controller, controlState);
//    hideClickToFlip();
  }

  //  @Override
  private void realAddWidgets(CommonAnnotatable e, ExerciseController controller, ControlState controlState) {
    super.addWidgets(e, controller, controlState);
    hideClickToFlip();
  }

  @Override
  protected void addRowBelowPrevNext(DivWidget toAddTo) {
    // String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
    //  logger.info("logException stack " + exceptionAsString);
    toAddTo.add(getChart(polyglotFlashcardContainer.getIsDry() ? DRY_RUN_ROUND_TIME : ROUND_TIME));

    super.addRowBelowPrevNext(toAddTo);
  }

  @Override
  protected void showEnglishOrForeign() {
    showBoth();

  }

  @Override
  protected void addControlsBelowAudio(ControlState controlState, Panel rightColumn) {
    speedChoices = new SpeedChoices( getOnSpeedChoiceMade(), true);
    rightColumn.add(speedChoices.getSpeedChoices());
  }

  @Override
  protected String getDeviceValue() {
    String s = "" + polyglotFlashcardContainer.getSessionStartMillis();
    //     logger.info("getDeviceValue  " + s);
    return s;
  }

  String getDeviceTypeValue() {
    return ""+polyglotFlashcardContainer.getNumExercises();
  }

  @Override
  protected boolean showScoreFeedback(AudioAnswer result) {
    return true;
  }

  @Override
  protected void recordingStarted() {
    if (polyglotFlashcardContainer.getMode() != PolyglotDialog.MODE_CHOICE.NOT_YET) {
      // logger.info("startTimedRun is " + mode);
      polyglotFlashcardContainer.startTimedRun();
    }
    super.recordingStarted();
  }

  protected void maybeAdvance(double score) {
    if (polyglotFlashcardContainer.isInLightningRound()) {
      if (isCorrect(score)) {
        timer.scheduleIn(NEXT_EXERCISE_DELAY);
        wrongCount = 0;
      } else {
        wrongCount++;
        if (wrongCount == INCORRECT_BEFORE_ADVANCE) {
          timer.scheduleIn(NEXT_EXERCISE_DELAY);
          wrongCount = 0;
        }
      }
    }
  }

  private boolean isCorrect(double score) {
    return score >= MIN_SCORE_F;
  }

  protected boolean isCorrect(boolean correct, double score) {
    boolean b = (score * 100D) >= PolyglotFlashcardFactory.MIN_POLYGLOT_SCORE;
    // logger.info("isCorrect " + correct + "  " + score + " " + b);
    return b;
  }

  protected void playIncorrect() {
  }

  @Override
  String getRefAudioToPlay() {
    boolean regular = speedChoices.isRegular();
    String path = regular ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    if (path == null) {
      path = regular ? exercise.getSlowAudioRef() : exercise.getRefAudio(); // fall back to slow audio
    }
    return path;

  }

  @Override
  AnalysisTab getScoreHistory() {
    return new AnalysisTab(controller, true, -1, () -> 0);
  }

  void gotTryAgain() {
    doStartOver();

  }

  @NotNull
  Grid getGrid() {
    return new Grid(FEEDBACK_SLOTS_POLYGLOT, 2);
  }

  void addMoreStats(Grid g, int row) {
    ControlGroup pronScoreGroup = new ControlGroup(TIME_LEFT);

    timeLeft = new Label();
    timeLeft.setType(LabelType.SUCCESS);
    timeLeft.setWidth("40px");

    g.setWidget(row, 0, pronScoreGroup);
    g.setWidget(row++, 1, timeLeft);

    showTimeRemaining(polyglotFlashcardContainer.getRoundTimeLeftMillis());

  }

  @NotNull
  private PolyglotChart getChart(long duration) {
    PolyglotChart pChart = new PolyglotChart(controller);
    pChart.addStyleName("topFiveMargin");
    pChart.addStyleName("bottomFiveMargin");
    pChart.addChart(sticky.getAnswers(), duration);
    pChart.setWidth("100%");
    pChart.setHeight(CHART_HEIGHT + "px");
    pChart.addStyleName("floatLeftAndClear");
    return pChart;
  }

  protected void gotSeeScoresClick() {
    polyglotFlashcardContainer.cancelRoundTimer();
    super.gotSeeScoresClick();
  }

  void reallyStartOver() {
    polyglotFlashcardContainer.showDrill();

  }

  @Override
  void flipCard() {

  }
  public void showTimeRemaining(long l) {
    String value = TIMES_UP;
    if (l > 0) {
      long min = l / ONE_MIN;
      long sec = (l - (min * ONE_MIN)) / 1000;
      value = "0" + min + ":" + (sec < 10 ? "0" : "") + sec;
      if (min == 0) {
        if (sec < 30) {
          timeLeft.setType(LabelType.IMPORTANT);
        } else {
          timeLeft.setType(LabelType.WARNING);
        }
      } else {
        timeLeft.setType(LabelType.SUCCESS);
      }
    } else {
      value = "";
    }
    timeLeft.setText(value);
    //   logger.info("showTimeRemaining : time left " + l);
  }
}
