/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.PolyglotChart;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.download.SpeedChoices;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.QuizSpec;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class PolyglotPracticePanel<L extends CommonShell, T extends ClientExercise> extends StatsPracticePanel<L, T> {
  public static final int TOP_MARGIN = -15;
  private final Logger logger = Logger.getLogger("PolyglotPracticePanel");

  private static final String ARROW_KEY_TIP = "<i><b>Space</b> to record. <b>Arrow keys</b> to advance or go back.</i>";

  private static final String ALL_DONE = "All done!";

  /**
   * @see #onSetComplete
   */
  private static final List<String> messages = Arrays.asList(
      "You've recorded all the items. ",
      "Click on a dot in the chart to jump back and re-record.",
      "Or to see your overall score click See Your Scores."
  );
  private static final boolean DO_AUTOLOAD = true;

  private static final int FEEDBACK_SLOTS_POLYGLOT = 5;
  private static final int NEXT_EXERCISE_DELAY = 850;
  private static final int INCORRECT_BEFORE_ADVANCE = 3;

  private static final int MINUTE = 60 * 1000;
  private static final int CHART_HEIGHT = 120;
  private static final String TIME_LEFT = "Time left";

  private static final long ONE_MIN = (60L * 1000L);
  private SpeedChoices speedChoices;
  private Label timeLeft;

  private final PolyglotFlashcardContainer polyglotFlashcardContainer;
  private int wrongCount = 0;
  private float minScore;
  private int minPolyScore;
  boolean showAudio;
  QuizSpec quizSpec;
  private final INavigation.VIEWS instance;

  private static final boolean DEBUG = false;

  /**
   * @param statsFlashcardFactory
   * @param controlState
   * @param controller
   * @param soundFeedback
   * @param e
   * @param stickyState
   * @param exerciseListToUse
   * @param instance
   * @see PolyglotFlashcardFactory#getFlashcard
   */
  PolyglotPracticePanel(PolyglotFlashcardContainer statsFlashcardFactory,
                        ControlState controlState, ExerciseController controller,
                        MySoundFeedback soundFeedback,
                        T e,
                        StickyState stickyState,
                        ListInterface<L, T> exerciseListToUse, INavigation.VIEWS instance) {
    super(statsFlashcardFactory, controlState, controller, soundFeedback, e, stickyState, exerciseListToUse);
    this.polyglotFlashcardContainer = statsFlashcardFactory;
    this.instance = instance;

    if (this.polyglotFlashcardContainer.getQuizSpec() == null) {
      int chosenList = getChosenList();
      logger.info("PolyglotPracticePanel chosen list " + chosenList);

      controller.getListService().getQuizInfo(chosenList, new AsyncCallback<QuizSpec>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(QuizSpec result) {
          gotQuizInfo(result);
        }
      });
    } else {
      this.quizSpec = this.polyglotFlashcardContainer.getQuizSpec();
      gotQuizInfo(quizSpec);
    }

//    double d = Math.floor((double) minPolyScore) / 100D;
    //  this.minScore = Double.valueOf(d).floatValue();
    // this.minPolyScore = minPolyScore;
    //  this.showAudio = showAudio;
  }

  private void gotQuizInfo(QuizSpec result) {
    //  logger.info("gotQuiz " +result);
    int minScore = result.getMinScore();
    double d = Math.floor((double) minScore) / 100D;

    this.minScore = Double.valueOf(d).floatValue();
    minPolyScore = minScore;
    this.quizSpec = result;
    realAddWidgets(exercise, controller, controlState);
  }

  @Override
  void addWidgets(T e, ExerciseController controller, ControlState controlState) {
  }

  private void realAddWidgets(T e, ExerciseController controller, ControlState controlState) {
    super.addWidgets(e, controller, controlState);
    hideClickToFlip();
  }

  /**
   * Remember to preload the audio!
   * Show a previous recording if we have it cached...
   *
   * @param exerciseID
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see FlashcardPanel#addWidgets
   */
  @Override
  protected void addRecordingAndFeedbackWidgets(int exerciseID,
                                                ExerciseController controller, Panel toAddTo) {
    super.addRecordingAndFeedbackWidgets(exerciseID, controller, toAddTo);
    AudioAnswer answer = sticky.getLastAnswer(exerciseID);
    if (answer != null) {
      {
        double score = answer.getScore();
        showRecoFeedback(score, answer.getPretestScore(), isCorrect(answer.isCorrect(), score));
      }

      playAudioPanel.startSong(CompressedAudio.getPath(answer.getPath()), DO_AUTOLOAD);
    }
  }

  private PolyglotChart<L> chart;

  /**
   * @param toAddTo
   * @see #addWidgets
   */
  @Override
  protected void addRowBelowPrevNext(DivWidget toAddTo) {
    chart = getChart(quizSpec.getRoundMinutes() * MINUTE);
    toAddTo.add(chart);
    super.addRowBelowPrevNext(toAddTo);
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

  private int getChosenList() {
    return new SelectionState(History.getToken(), false).getList();
  }

  /**
   * Get session start...
   *
   * @return
   * @see #getAnswerWidget
   */
  @Override
  protected String getDeviceValue() {
    long sessionStartMillis = polyglotFlashcardContainer.getSessionStartMillis();
    if (sessionStartMillis == 0L) logger.warning("session start is 0?");

    String s = "" + sessionStartMillis;
    // logger.info("getDeviceValue  " + s);
    return s;
  }

  String getDeviceTypeValue() {
    return "" + polyglotFlashcardContainer.getNumExercises();
  }

  /**
   * @see BootstrapExercisePanel#getAnswerWidget
   */
  @Override
  protected void recordingStarted() {
//    if (polyglotFlashcardContainer.getMode() != PolyglotDialog.MODE_CHOICE.NOT_YET) {
      polyglotFlashcardContainer.startTimedRun();
//    } else {
//      logger.info("recordingStarted : ignore start time run ? mode " + polyglotFlashcardContainer.getMode());
//    }
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
    return score >= minScore;
  }

  protected boolean isCorrect(boolean correct, double score) {
    boolean b = (score * 100D) >= minPolyScore;
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
      //logger.info("getRefAudioToPlay no speed choices ");
      return null;
    } else {
      boolean regular = speedChoices.isRegular();

      String path = regular ? exercise.getRefAudio() : exercise.getSlowAudioRef();
      //  logger.info("getRefAudioToPlay play audio " + path);
      if (path == null) {
        path = regular ? exercise.getSlowAudioRef() : exercise.getRefAudio(); // fall back to slow audio
      }
      return path;
    }
  }

  @Override
  AnalysisTab getScoreHistory() {
    AnalysisTab widgets = super.getScoreHistory();
    widgets.getElement().getStyle().setMarginTop(TOP_MARGIN, Style.Unit.PX);
    return widgets;
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

  /**
   * @param duration
   * @return
   * @see #addRowBelowPrevNext
   */
  @NotNull
  private PolyglotChart<L> getChart(long duration) {
    PolyglotChart<L> pChart = new PolyglotChart<L>(controller, controller.getMessageHelper(), exerciseList);
    pChart.addStyleName("topFiveMargin");
    pChart.addStyleName("bottomFiveMargin");
    pChart.addChart(sticky.getAnswers(), duration);
    pChart.setWidth("100%");
    pChart.setHeight(CHART_HEIGHT + "px");
    pChart.addStyleName("floatLeftAndClear");
    pChart.setIdToEx(exerciseList.getIdToExercise());
    pChart.setTimeToAnswer(sticky.getTimeToAnswer());
    return pChart;
  }

  protected void gotSeeScoresClick() {
    stopTimerShowBanner();
    super.gotSeeScoresClick();
  }

  private void stopTimerShowBanner() {
    controller.setBannerVisible(true);
    if (DEBUG) logger.info("PolyglotPracticePanel : stopTimerShowBanner");
    polyglotFlashcardContainer.cancelRoundTimer();
  }

  void reallyStartOver() {
    if (instance == INavigation.VIEWS.PRACTICE) {
      polyglotFlashcardContainer.showDrill();
      super.reallyStartOver();
    } else {
      controller.setBannerVisible(true);
      polyglotFlashcardContainer.showQuiz();
    }
  }

  @Override
  void flipCard() {
  }

  /**
   * @param l
   * @see PolyglotFlashcardFactory#sessionComplete
   */
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
    controller.setBannerVisible(true);
    if (roundTimeLeftMillis > 0 && polyglotFlashcardContainer.isComplete()) {
      new ModalInfoDialog(ALL_DONE, messages);
      chart.setData(sticky.getAnswers());

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

  /**
   * Actively forget...
   */
  @Override
  protected void rememberCurrentExercise() {
    sticky.clearCurrent();
  }

  public void postedAudio() {
    polyglotFlashcardContainer.postAudio();
  }

  /**
   * @param result
   */
  @Override
  public void receivedAudioAnswer(AudioAnswer result) {
    super.receivedAudioAnswer(result);
    polyglotFlashcardContainer.addRoundTrip(result.getRoundTripMillis());
  }
}
