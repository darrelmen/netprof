package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.PolyglotChart;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.download.SpeedChoices;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonAnnotatable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PolyglotPracticePanel<L extends CommonShell, T extends CommonExercise> extends StatsPracticePanel<L, T> {
  //private final Logger logger = Logger.getLogger("PolyglotPracticePanel");

  private static final String ARROW_KEY_TIP = "<i><b>Space</b> to record. <b>Arrow keys</b> to advance or go back.</i>";

  private static final String ALL_DONE = "All done!";

  private static final String COMPLETE = "You've recorded all the items. " +
      "Use the arrow keys to review or re-record as desired, or click See Your Scores.";

  private static final int MIN_POLYGLOT_SCORE = 35;

  private static final float MIN_SCORE_F = ((float) MIN_POLYGLOT_SCORE) / 100f;

  private static final int FEEDBACK_SLOTS_POLYGLOT = 5;
  private static final int NEXT_EXERCISE_DELAY = 750;
  private static final int INCORRECT_BEFORE_ADVANCE = 3;

  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;
  private static final int MINUTE = 60 * 1000;
  private static final int DRY_RUN_ROUND_TIME = DRY_RUN_MINUTES * MINUTE;
  private static final int ROUND_TIME = ROUND_MINUTES * MINUTE;
  private static final int CHART_HEIGHT = 120;
  private static final String TIME_LEFT = "Time left";

  private static final long ONE_MIN = (60L * 1000L);
  private Label timeLeft;

  private final PolyglotFlashcardContainer polyglotFlashcardContainer;
  private int wrongCount = 0;

  PolyglotPracticePanel(PolyglotFlashcardContainer statsFlashcardFactory,
                        ControlState controlState, ExerciseController controller,
                        MySoundFeedback soundFeedback,
                        PolyglotDialog.PROMPT_CHOICE prompt, CommonAnnotatable e, StickyState stickyState,
                        ListInterface<L, T> exerciseListToUse) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, prompt, e, stickyState, exerciseListToUse);
    this.polyglotFlashcardContainer = statsFlashcardFactory;
    realAddWidgets(e, controller, controlState);
  }

  @Override
  void addWidgets(CommonAnnotatable e, ExerciseController controller, ControlState controlState) {
  }

  protected void realAddWidgets(CommonAnnotatable e, ExerciseController controller, ControlState controlState) {
    super.addWidgets(e, controller, controlState);
    hideClickToFlip();
  }

  /**
   * Remember to preload the audio!
   *
   * @param exerciseID
   * @param controller used in subclasses for audio control
   * @param toAddTo
   */
  @Override
  protected void addRecordingAndFeedbackWidgets(int exerciseID,
                                                ExerciseController controller, Panel toAddTo) {
    super.addRecordingAndFeedbackWidgets(exerciseID, controller, toAddTo);

    AudioAnswer answer = sticky.getAnswer(exerciseID);
    if (answer != null) {
      showRecoFeedback(answer.getScore(), answer.getPretestScore(), isCorrect(answer.isCorrect(), answer.getScore()));
      playAudioPanel.startSong(CompressedAudio.getPath(answer.getPath()), false);
    }
  }

  @Override
  protected void addRowBelowPrevNext(DivWidget toAddTo) {
    // String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
    //  logger.info("logException stack " + exceptionAsString);
    toAddTo.add(getChart(getRoundTime()));
    super.addRowBelowPrevNext(toAddTo);
  }

  private int getRoundTime() {
    return polyglotFlashcardContainer.getRoundTimeMinutes(polyglotFlashcardContainer.getIsDry()) * MINUTE;
  }

  @Override
  protected void showEnglishOrForeign() {
    showBoth();
  }

  @Override
  protected void addControlsBelowAudio(ControlState controlState, Panel rightColumn) {
    speedChoices = new SpeedChoices(getOnSpeedChoiceMade(), true);
    rightColumn.add(speedChoices.getSpeedChoices());
  }

  @Override
  protected String getDeviceValue() {
    String s = "" + polyglotFlashcardContainer.getSessionStartMillis();
    //     logger.info("getDeviceValue  " + s);
    return s;
  }

  String getDeviceTypeValue() {
    return "" + polyglotFlashcardContainer.getNumExercises();
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

  /**
   * @param score
   * @param isFullMatch
   * @see #showCorrectFeedback
   */
  protected void maybeAdvance(double score, boolean isFullMatch) {
    if (polyglotFlashcardContainer.isInLightningRound()) {
      if (isFullMatch && isCorrect(score)) {
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

  /**
   * @return null if we're not allowed to play audio.
   * @see #playRef
   */
  @Override
  String getRefAudioToPlay() {
    if (speedChoices == null) {
      return null;
    } else {
      boolean regular = speedChoices.isRegular();
      String path = regular ? exercise.getRefAudio() : exercise.getSlowAudioRef();
      if (path == null) {
        path = regular ? exercise.getSlowAudioRef() : exercise.getRefAudio(); // fall back to slow audio
      }
      return path;
    }
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
    //logger.info("got " +exerciseList.getIdToExercise().size());
    pChart.setIdToEx(exerciseList.getIdToExercise());
    pChart.setTimeToID(sticky.getTimeToID());
    return pChart;
  }

  protected void gotSeeScoresClick() {
    controller.setBannerVisible(true);
    polyglotFlashcardContainer.cancelRoundTimer();
    super.gotSeeScoresClick();
  }

  void reallyStartOver() {
    if (instance.equalsIgnoreCase(INavigation.VIEWS.DRILL.toString())) {
      polyglotFlashcardContainer.showDrill();
    } else {
      polyglotFlashcardContainer.showQuiz();
    }
    super.reallyStartOver();
  }

  @Override
  void flipCard() {
  }

  void showTimeRemaining(long l) {
    String value;
    if (l > 0) {
      long min = l / ONE_MIN;
      long sec = (l - (min * ONE_MIN)) / 1000;
      String prefix = min < 10 ? "0" : "";
      value = prefix + min + ":" + (sec < 10 ? "0" : "") + sec;
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

  public void onSetComplete() {
    long roundTimeLeftMillis = polyglotFlashcardContainer.getRoundTimeLeftMillis();
    //logger.info("onSetComplete  " + roundTimeLeftMillis);
    if (roundTimeLeftMillis > 0 && polyglotFlashcardContainer.isComplete()) {
      new ModalInfoDialog(ALL_DONE, COMPLETE);
    } else {
      super.onSetComplete();
    }
  }

  void playRefOnError() {
  }

  @Override
  String getKeyBindings() {
    return ARROW_KEY_TIP;
  }
}
