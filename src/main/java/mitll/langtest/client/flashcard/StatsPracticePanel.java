package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.analysis.PolyglotChart;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.IShowStatus;
import mitll.langtest.client.download.SpeedChoices;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.CommonAnnotatable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.project.ProjectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @see ExercisePanelFactory#getExercisePanel(Shell)
 */
class StatsPracticePanel<L extends CommonShell, T extends CommonExercise> extends BootstrapExercisePanel<CommonAnnotatable> {
  private final Logger logger = Logger.getLogger("StatsFlashcardFactory");


  private static final long ONE_MIN = (60L * 1000L);
  private static final int CHART_HEIGHT = 120;
  private static final String TRY_AGAIN = "Try Again?";
  private static final String TIME_LEFT = "Time left";
  private static final String START_OVER_FROM_THE_BEGINNING = "Start over from the beginning.";


  private static final int INCORRECT_BEFORE_ADVANCE = 3;

//  private static final String LISTS = "Lists";
  /**
   * @see StatsPracticePanel#showTimeRemaining
   */
  private static final String TIMES_UP = "Times Up!";

  static final int MIN_POLYGLOT_SCORE = 35;

  private static final float MIN_SCORE_F = ((float) MIN_POLYGLOT_SCORE) / 100f;
  private static final int HEARTBEAT_INTERVAL = 1000;

  private static final int FEEDBACK_SLOTS = 4;
  private static final int FEEDBACK_SLOTS_POLYGLOT = 5;
  private static final int NEXT_EXERCISE_DELAY = 750;

  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String AVG_SCORE = "Pronunciation";
  /**
   *
   */
  private static final String START_OVER = "Start Over";

  /**
   * @see StatsPracticePanel#getSkipToEnd
   */
  private static final String SKIP_TO_END = "See your scores";
  private static final boolean ADD_KEY_BINDING = true;
  /**
   * @seex StatsFlashcardFactory.StatsPracticePanel#getRepeatButton
   */
//  private static final String GO_BACK = "Go back";
  private static final String N_A = "N/A";
  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;
  private static final int DRY_RUN_ROUND_TIME = DRY_RUN_MINUTES * 60 * 1000;
  private static final int ROUND_TIME = ROUND_MINUTES * 60 * 1000;


  private FlashcardContainer statsFlashcardFactory;
  private Widget container;
  final SetCompleteDisplay completeDisplay = new SetCompleteDisplay();
  private SpeedChoices speedChoices;
  private int count;
  StickyState sticky;

  public StatsPracticePanel(FlashcardContainer statsFlashcardFactory,
                            ControlState controlState,
                            ExerciseController controller,
                            MySoundFeedback soundFeedback,
                            PolyglotDialog.PROMPT_CHOICE prompt,
                            CommonAnnotatable e,
                            StickyState stickyState,
                            ListInterface<L, T> exerciseListToUse) {
    super(e,
        controller,
        ADD_KEY_BINDING,
        controlState,
        soundFeedback,
        null,
        "",//statsFlashcardFactory.instance,
        exerciseListToUse,
        prompt);
    this.sticky = stickyState;
    this.statsFlashcardFactory = statsFlashcardFactory;
    soundFeedback.setEndListener(new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        disableRecord();
      }

      @Override
      public void songEnded() {
        enableRecord();
        removePlayingHighlight();
      }
    });

    addWidgets(e, controller, controlState);

    if (isPolyglot()) {
      hideClickToFlip();
    }
//    this.count = statsFlashcardFactory.counter++;
  }

  private boolean isPolyglot() {
    return controller.getProjectStartupInfo().getProjectType() == ProjectType.POLYGLOT;
  }

/*    @Override
  protected void onUnload() {
    super.onUnload();
    cancelRoundTimer();
  }*/

  @Override
  protected String getDeviceValue() {
    String s = isPolyglot() ? "" + statsFlashcardFactory.getSessionStartMillis() : controller.getBrowserInfo();
    //     logger.info("getDeviceValue  " + s);
    return s;
  }

  @Override
  protected void showEnglishOrForeign() {
    if (isPolyglot()) {
      showBoth();
    } else {
      super.showEnglishOrForeign();
    }
  }

  @Override
  protected void addControlsBelowAudio(ControlState controlState, Panel rightColumn) {
    if (isPolyglot()) {
      speedChoices = new SpeedChoices(sticky.getStorage(), getOnSpeedChoiceMade(), true);
      // logger.info("speedChoices " + speedChoices);
      rightColumn.add(speedChoices.getSpeedChoices());
    } else {
      super.addControlsBelowAudio(controlState, rightColumn);
    }
  }

  @NotNull
  private IShowStatus getOnSpeedChoiceMade() {
    return this::maybePlayRef;
  }

  private void maybePlayRef() {
    if (isAudioOn()) {
      playRef();
    }
  }

  String getRefAudioToPlay() {
    if (isPolyglot()) {
      boolean regular = speedChoices.isRegular();
      String path = regular ? exercise.getRefAudio() : exercise.getSlowAudioRef();
      if (path == null) {
        path = regular ? exercise.getSlowAudioRef() : exercise.getRefAudio(); // fall back to slow audio
      }
      return path;
    } else {
      return super.getRefAudioToPlay();
    }
  }

  public int getCounter() {
    return count;
  }
/*    public String toString() {
    return "Stats # " + count;
  }*/

  @Override
  protected boolean showScoreFeedback(AudioAnswer result) {
    return result.isSaidAnswer() || isPolyglot();
  }

  @Override
  protected void recordingStarted() {
    if (statsFlashcardFactory.getMode() != PolyglotDialog.MODE_CHOICE.NOT_YET) {
      // logger.info("startTimedRun is " + mode);
      if (isPolyglot()) statsFlashcardFactory.startTimedRun();
    }
    cancelAdvanceTimer();
    stopPlayback();
    removePlayingHighlight();
  }

  /**
   * @see #timerFired
   * @see BootstrapExercisePanel#playRefAndGoToNext
   */
  @Override
  protected void loadNext() {
    if (exerciseList.onLast()) {
      timer.cancelTimer();
      //   cancelRoundTimer();
      onSetComplete();
    } else {
      exerciseList.loadNext();
    }
  }

  /**
   * @param b
   * @see FlashcardPanel#getShuffleButton
   */
  @Override
  protected void gotShuffleClick(boolean b) {
    sticky.resetStorage();
    super.gotShuffleClick(b);
  }

  protected void gotAutoPlay(boolean b) {
    if (b) {
      //   logger.info("gotAutoPlay got click...");
      playRefAndGoToNextIfSet();
    } else {
      //   logger.info("gotAutoPlay abortPlayback");
      abortPlayback();
    }
  }

  /**
   * @param result
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, Panel)
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    // logger.info("StatsPracticePanel.receivedAudioAnswer: result " + result);
    if (result.getValidity() == Validity.OK) {
      int id = exercise.getID();
/*      statsFlashcardFactory.exToScore.put(id, result.getScore());
      statsFlashcardFactory.exToCorrect.put(id, isCorrect(result.isCorrect(), result.getScore()));

      StringBuilder builder = new StringBuilder();
      StringBuilder builder2 = new StringBuilder();
      for (Map.Entry<Integer, Boolean> pair : statsFlashcardFactory.exToCorrect.entrySet()) {
        if (pair.getValue()) {
          builder.append(pair.getKey()).append(",");
        } else {
          builder2.append(pair.getKey()).append(",");
        }
      }
      sticky.storeCorrect(builder);
      sticky.storeIncorrect(builder2);

      StringBuilder builder3 = new StringBuilder();
      for (Map.Entry<Integer, Double> pair : statsFlashcardFactory.exToScore.entrySet()) {
        builder3.append(pair.getKey()).append("=").append(pair.getValue()).append(",");
      }
      sticky.storeScore(builder3);


      statsFlashcardFactory.exToLatest.put(id, result);
      */
      sticky.storeAnswer(result, id);
      setStateFeedback();

    }
    super.receivedAudioAnswer(result);
  }

  protected boolean isCorrect(boolean correct, double score) {
    boolean b = isPolyglot() ? (score * 100D) >= StatsFlashcardFactory.MIN_POLYGLOT_SCORE : correct;
    // logger.info("isCorrect " + correct + "  " + score + " " + b);
    return b;
  }

  private int wrongCount = 0;

  protected void maybeAdvance(double score) {
    if (statsFlashcardFactory.isInLightningRound()) {
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

  /**
   * Turn off for now?
   */
  protected void playCorrectDing() {
  }

  /**
   * Don't play the incorrect sound.
   */
  protected void playIncorrect() {
    if (!isPolyglot()) super.playIncorrect();
  }

  /**
   * Ask for history for those items that were actually practiced.
   *
   * @see #getSkipToEnd()
   * @see #gotClickOnNext()
   * @see #loadNext()
   */
  public void onSetComplete() {
    // if (!startOver.isVisible()) return;
    startOver.setVisible(false);
    seeScores.setVisible(false);
    setPrevNextVisible(false);

    sticky.resetStorage();
    if (exercise == null) {
      logger.warning("StatsPracticePanel.onSetComplete. : err : no exercise?");
    } else {
      sticky.storeCurrent(exercise);
    }

    showFeedbackCharts();
  }

  /**
   * TODO: get last session...
   *
   * @paramx sortedHistory
   */
  private void showFeedbackCharts() {
    setMainContentVisible(false);
    statsFlashcardFactory.styleContent(false);

    Panel widgets = new DivWidget();

    container = widgets;

    AnalysisTab scoreHistory = new AnalysisTab(controller, isPolyglot(), -1, () -> 0);

    scoreHistory.add(getButtonsBelowScoreHistory());
    widgets.add(scoreHistory);
    belowContentDiv.clear();
    belowContentDiv.add(container);
  }

  /**
   * @return
   * @see #showFeedbackCharts
   */
  private Panel getButtonsBelowScoreHistory() {
    Panel child = new HorizontalPanel();

    final Button w = getSummaryStartOver();
    child.add(w);
    w.addStyleName("topFiveMargin");

    DivWidget lefty = new DivWidget();
    lefty.add(child);
    return lefty;
  }

  private Button getSummaryStartOver() {
    final Button w = new Button();
    w.setType(ButtonType.SUCCESS);
    w.setText(TRY_AGAIN);
    w.setIcon(IconType.REPEAT);

    w.addClickHandler(event -> {
      w.setVisible(false);
      if (isPolyglot()) {
        doStartOver();
      } else {
        doIncorrectFirst();
      }
    });

    controller.register(w, N_A);
    return w;
  }

  /**
   * @see #getSummaryStartOver()
   */
  void doIncorrectFirst() {
    showFlashcardDisplay();

    //currentExercise = null;
    sticky.reset();

    statsFlashcardFactory.reload();
//    exerciseList.reload(statsFlashcardFactory.selection);
    makeFlashcardButtonsVisible();

    sticky.resetStorage();
  }

/*
  private int getCorrect() {
    int count = 0;
    for (Boolean val : sticky.getCorrectValues()) {
      if (val) count++;
    }
    return count;
  }

  private int getIncorrect() {
    int count = 0;
    for (Boolean val : sticky.getCorrectValues()) {
      if (!val) count++;
    }
    return count;
  }
*/

  /**
   * @return
   * @see StatsPracticePanel#showFeedbackCharts
   */
/*    private Button getRepeatButton() {
    final Button w1 = new Button(GO_BACK);
    w1.setIcon(IconType.UNDO);
    w1.getElement().setId("AVP_DoWholeSetFromStart");
    w1.setType(ButtonType.PRIMARY);
    w1.addClickHandler(event -> doGoBack(w1));
    controller.register(w1, N_A);
    return w1;
  }*/
/*   private void doGoBack(Button w1) {
    w1.setVisible(false);
    sticky.clearCurrent();
    showFlashcardDisplay();
    startOver();
  }*/
  private void showFlashcardDisplay() {
    abortPlayback();
    belowContentDiv.clear();
    belowContentDiv.add(getSkipToEnd());
    belowContentDiv.add(getStartOver());
    setMainContentVisible(true);
  }

  /**
   * If we're coming back to the cards at the end, we want to start over from the start,
   * otherwise, we want to pick back up where we left off.
   *
   * @seex #getRepeatButton()
   * @see #doStartOver
   */
  void startOver() {
    makeFlashcardButtonsVisible();

    statsFlashcardFactory.startOver();
    /*int lastID = statsFlashcardFactory.allExercises.isEmpty() ? -1 : statsFlashcardFactory.allExercises.get(statsFlashcardFactory.allExercises.size() - 1).getID();
    int currentExerciseID = sticky.getCurrentExerciseID();
//logger.info("startOver : current " + currentExerciseID + " = " + statsFlashcardFactory.mode);

    if (currentExerciseID != -1 && currentExerciseID != lastID) {
      exerciseList.loadExercise(currentExerciseID);
    } else {
      reallyStartOver();
    }

    if (isPolyglot()) {
*//*       if (mode == PolyglotDialog.MODE_CHOICE.NOT_YET) {
        stopTimedRun();
      } else {
        stopTimedRun();
       // startTimedRun();
      }*//*

      statsFlashcardFactory.stopTimedRun();
    }*/
  }

/*  private void reallyStartOver() {
    sticky.reset();
    sticky.resetStorage();

    statsFlashcardFactory.loadFirstExercise();
  }*/

  private void makeFlashcardButtonsVisible() {
//    statsFlashcardFactory.contentPanel.removeStyleName("noWidthCenterPractice");
//    statsFlashcardFactory.contentPanel.addStyleName("centerPractice");
    statsFlashcardFactory.styleContent(true);

    startOver.setVisible(true);
    startOver.setEnabled(true);
    seeScores.setVisible(true);
    setPrevNextVisible(true);
  }

  private Button startOver, seeScores;
  private Panel belowContentDiv;

  /**
   * @param toAddTo
   * @see FlashcardPanel#FlashcardPanel
   */
  @Override
  protected void addRowBelowPrevNext(DivWidget toAddTo) {
    if (isPolyglot()) {
      // String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
      //  logger.info("logException stack " + exceptionAsString);
      toAddTo.add(getChart(statsFlashcardFactory.getIsDry() ? DRY_RUN_ROUND_TIME : ROUND_TIME));
    }

    DivWidget buttons = new DivWidget();
    buttons.setWidth("100%");
    toAddTo.add(buttons);

    buttons.add(getSkipToEnd());
    buttons.add(startOver = getStartOver());

    belowContentDiv = toAddTo;
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

  /**
   * @see FlashcardPanel#getNextButton()
   */
  @Override
  protected void gotClickOnNext() {
    abortPlayback();
    if (exerciseList.onLast()) {
      onSetComplete();
    } else {
      //logger.info("load next " + exerciseList.getCurrentExerciseID());
      exerciseList.loadNext();
    }
  }

  private Button getStartOver() {
    Button startOver = new Button(START_OVER);
    startOver.getElement().setId("AVP_StartOver");

    startOver.setType(ButtonType.SUCCESS);
    startOver.setIcon(IconType.REPEAT);
    startOver.addStyleName("floatRight");
    startOver.addClickHandler(event -> doStartOver());
    new TooltipHelper().addTooltip(startOver, START_OVER_FROM_THE_BEGINNING);

    controller.register(startOver, N_A);
    return startOver;
  }

  private void doStartOver() {
    abortPlayback();
    sticky.clearCurrent();
    startOver.setEnabled(false);
    if (isPolyglot()) {
      statsFlashcardFactory.showDrill();
    } else {
      startOver();
    }
  }

  @Override
  protected void abortPlayback() {
    timer.cancelTimer();
//    statsFlashcardFactory.soundFeedback.clear();
    stopPlayback();
  }

  /**
   * @return
   * @see #addRowBelowPrevNext(DivWidget)
   */
  private Button getSkipToEnd() {
    seeScores = new Button(SKIP_TO_END);
    seeScores.setIcon(IconType.TROPHY);
    seeScores.getElement().setId("AVP_SkipToEnd");
    controller.register(seeScores, N_A);

    seeScores.addStyleName("leftFiveMargin");
    seeScores.setType(ButtonType.PRIMARY);
    seeScores.addStyleName("floatRight");
    seeScores.addClickHandler(event -> gotSeeScoresClick());
    new TooltipHelper().addTooltip(seeScores, SKIP_TO_END);
    return seeScores;
  }

  @Override
  void flipCard() {
    if (!isPolyglot()) {
      super.flipCard();
    }
  }

  private void gotSeeScoresClick() {
    abortPlayback();
    seeScores.setEnabled(false);
    statsFlashcardFactory.cancelRoundTimer();
    onSetComplete();
  }

  private Label remain, incorrectBox, correctBox, pronScore, timeLeft;

  /**
   * @return
   * @see #getThreePartContent(ControlState, Panel, DivWidget, DivWidget)
   */
  protected Panel getLeftState() {
    Grid g = new Grid(isPolyglot() ? FEEDBACK_SLOTS_POLYGLOT : FEEDBACK_SLOTS, 2);

    int row = 0;
    {
      ControlGroup remaining = new ControlGroup(REMAINING);
      remaining.addStyleName("topFiveMargin");
      remain = new Label();
      remain.setType(LabelType.INFO);
      g.setWidget(row, 0, remaining);
      g.setWidget(row++, 1, remain);
    }

    {
      ControlGroup incorrect = new ControlGroup(INCORRECT);
      incorrect.addStyleName("topFiveMargin");

      incorrectBox = new Label();
      incorrectBox.setType(LabelType.WARNING);

      g.setWidget(row, 0, incorrect);
      g.setWidget(row++, 1, incorrectBox);
    }

    {
      ControlGroup correct = new ControlGroup(CORRECT);
      correct.addStyleName("topFiveMargin");

      correctBox = new Label();
      correctBox.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, correct);
      g.setWidget(row++, 1, correctBox);
    }

    {
      ControlGroup pronScoreGroup = new ControlGroup(AVG_SCORE);
      pronScoreGroup.addStyleName("topFiveMargin");

      pronScore = new Label();
      pronScore.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, pronScoreGroup);
      g.setWidget(row++, 1, pronScore);
    }

    if (isPolyglot()) {
      ControlGroup pronScoreGroup = new ControlGroup(TIME_LEFT);

      timeLeft = new Label();
      timeLeft.setType(LabelType.SUCCESS);
      timeLeft.setWidth("40px");

      g.setWidget(row, 0, pronScoreGroup);
      g.setWidget(row++, 1, timeLeft);

      showTimeRemaining(statsFlashcardFactory.getRoundTimeLeftMillis());
    }

    setStateFeedback();
    g.addStyleName("rightTenMargin");
    return g;
  }

  /**
   * @see #getLeftState()
   * @see #receivedAudioAnswer
   */
  private void setStateFeedback() {
    int totalCorrect = sticky.getCorrectCount();
    int totalIncorrect = sticky.getIncorrectCount();
    int remaining = statsFlashcardFactory.getNumExercises() - totalCorrect - totalIncorrect;
    remain.setText(remaining + "");
    incorrectBox.setText(totalIncorrect + "");
    correctBox.setText(totalCorrect + "");

    double total = 0;
    int count = 0;
    for (double score : sticky.getScores()) {
      if (score > 0) {
        total += score;
        count++;
      }
    }
    if (count > 0) {
      total /= count;
    }

    // TODO : come back to the color coding ...
    LabelType type =
        total > 0.8 ? LabelType.SUCCESS :
            total > 0.5 ? LabelType.INFO :
                LabelType.WARNING;
    //  logger.info("type "+type + " score " + total);
    pronScore.setType(type);

    total *= 100;
    int itotal = (int) Math.ceil(total);

    pronScore.setText("" + itotal);
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
