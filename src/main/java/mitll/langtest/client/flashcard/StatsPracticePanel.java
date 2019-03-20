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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.IShowStatus;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * @see ExercisePanelFactory#getExercisePanel
 */
class StatsPracticePanel<L extends CommonShell, T extends ClientExercise> extends BootstrapExercisePanel<L, T> {
  private final Logger logger = Logger.getLogger("StatsPracticePanel");

  /**
   *
   */
  private static final int BUTTON_RIGHT_MARGIN = 8;
  /**
   * @see #getSummaryStartOver
   */
  private static final String TRY_AGAIN = "Try Again?";
  private static final String START_OVER_FROM_THE_BEGINNING = "Start over from the beginning.";

  private static final int FEEDBACK_SLOTS = 4;

  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String AVG_SCORE = "Pronunciation";
  /**
   * @see #getStartOver
   */
  private static final String START_OVER = "Start Over";

  /**
   * @see #getSkipToEnd
   */
  private static final String SKIP_TO_END = "See your scores";
  private static final boolean ADD_KEY_BINDING = true;

  private static final String N_A = "N/A";

  private final FlashcardContainer statsFlashcardFactory;
  final StickyState sticky;
  private Label remain, incorrectBox, correctBox, pronScore;

  /**
   * @param statsFlashcardFactory
   * @param controlState
   * @param controller
   * @param soundFeedback
   * @param e
   * @param stickyState
   * @param exerciseListToUse
   * @see StatsFlashcardFactory#getFlashcard
   */
  StatsPracticePanel(FlashcardContainer statsFlashcardFactory,
                     ControlState controlState,
                     ExerciseController controller,
                     MySoundFeedback soundFeedback,
                     T e,
                     StickyState stickyState,
                     ListInterface<L, T> exerciseListToUse) {
    super(e,
        controller,
        ADD_KEY_BINDING,
        controlState,
        soundFeedback,
        null,
        exerciseListToUse
    );
    this.sticky = stickyState;
    this.statsFlashcardFactory = statsFlashcardFactory;
    addWidgets(e, controller, controlState);
    wifiTimer = getWifiTimer();
  }

  private final Timer wifiTimer;

  @Override
  protected String getDeviceValue() {
    return controller.getBrowserInfo();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    wifiTimer.cancel();
  }

  @NotNull
  IShowStatus getOnSpeedChoiceMade() {
    return this::maybePlayRef;
  }

  private void maybePlayRef() {
    if (isAudioOn()) {
      playRef();
    }
//    else logger.info("audio set to off - so not playing.");
  }

  @Override
  void recordingStarted() {
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
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    // logger.info("StatsPracticePanel.receivedAudioAnswer: result " + result);
    if (result.isValid()) {
      sticky.storeAnswer(result);
      setStateFeedback();
    }
    super.receivedAudioAnswer(result);
  }

  /**
   * Turn off for now?
   */
  protected void playCorrectDing() {
  }

  /**
   * Ask for history for those items that were actually practiced.
   *
   * @see #getSkipToEnd()
   * @see #gotClickOnNext()
   * @see #loadNext()
   */
  public void onSetComplete() {
    startOver.setVisible(false);
    seeScores.setVisible(false);
    setPrevNextVisible(false);

    setListHeaderVisible(false);

    sticky.resetStorage();
    if (exercise == null) {
      logger.warning("StatsPracticePanel.onSetComplete. : err : no exercise?");
    } else {
      rememberCurrentExercise();
    }

    //  logger.info("onSetComplete - show charts!");
    showFeedbackCharts();
  }

  private void setListHeaderVisible(boolean sortBoxVisible) {
    if (exerciseList instanceof PracticeFacetExerciseList) {
      ((PracticeFacetExerciseList) exerciseList).setSortBoxVisible(sortBoxVisible);
      ((PracticeFacetExerciseList) exerciseList).setListHeaderVisible(sortBoxVisible);
    }
  }

  @Override
  protected void rememberCurrentExercise() {
    sticky.storeCurrent(exercise);
  }

  /**
   * @see #onSetComplete
   */
  private void showFeedbackCharts() {
    setMainContentVisible(false);
    statsFlashcardFactory.styleContent(false);

    Panel widgets = new DivWidget();

    {
      AnalysisTab scoreHistory = getScoreHistory();
      scoreHistory.add(getButtonsBelowScoreHistory());

      {
        DivWidget w = new DivWidget();
        w.addStyleName("floatRight");

        {
          HTML test = new HTML("");
          w.add(test);
          test.setHeight("60px");
        }

        w.setWidth("100%");
        scoreHistory.add(w);
      }

      widgets.add(scoreHistory);
    }
    belowContentDiv.clear();
    belowContentDiv.add(widgets);
  }

  AnalysisTab getScoreHistory() {
    int list = new SelectionState(History.getToken(), false).getList();
    // logger.info("getScoreHistory list id  " + list);
    return new AnalysisTab(controller, true, -1, () -> 0, INavigation.VIEWS.LEARN, list);
  }

  /**
   * @return
   * @see #showFeedbackCharts
   */
  private Panel getButtonsBelowScoreHistory() {
    DivWidget child = new DivWidget();
    child.addStyleName("floatRight");

    {
      final Button w = getSummaryStartOver();
      child.add(w);
      w.addStyleName("topFiveMargin");
    }

    DivWidget lefty = new DivWidget();
    lefty.add(child);
    lefty.setWidth("100%");
    lefty.addStyleName("bottomFiveMargin");
    return lefty;
  }

  /**
   * @return
   */
  private Button getSummaryStartOver() {
    final Button w = new Button();
    w.getElement().getStyle().setMarginRight(BUTTON_RIGHT_MARGIN, Style.Unit.PX);
    w.setType(ButtonType.SUCCESS);
    w.setText(TRY_AGAIN);
    w.setIcon(IconType.REPEAT);

    w.addClickHandler(event -> {
      w.setVisible(false);
      gotTryAgain();
    });

    controller.register(w, N_A);
    return w;
  }

  void gotTryAgain() {
    setListHeaderVisible(true);

    doIncorrectFirst();
  }

  /**
   * @see #getSummaryStartOver()
   */
  private void doIncorrectFirst() {
    showFlashcardDisplay();
    sticky.reset();

    statsFlashcardFactory.reload();
    makeFlashcardButtonsVisible();

    sticky.resetStorage();
  }

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
  private void startOver() {
    // logger.info("startOver!");
    makeFlashcardButtonsVisible();
    statsFlashcardFactory.startOver();
  }

  private void makeFlashcardButtonsVisible() {
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
   * @see FlashcardPanel#addWidgets
   */
  @Override
  void addRowBelowPrevNext(DivWidget toAddTo) {
    DivWidget buttons = new DivWidget();
    buttons.setWidth("100%");
    toAddTo.add(buttons);

    buttons.add(getSkipToEnd());
    buttons.add(startOver = getStartOver());
    buttons.add(addSubtitle());

    belowContentDiv = toAddTo;
  }

  private Label wifiStatus;

  private Label addSubtitle() {
    wifiStatus = new Label(InitialUI.CHECK_NETWORK_WIFI);
    wifiStatus.addStyleName("rightFiveMargin");
    wifiStatus.addStyleName("floatRight");
    wifiStatus.setType(LabelType.WARNING);
    wifiStatus.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    wifiStatus.setVisible(false);
    return wifiStatus;
  }


  @NotNull
  private Timer getWifiTimer() {
    Timer timer = new Timer() {
      @Override
      public void run() {
        //logger.warning("waited " + (System.currentTimeMillis() - then) + " for a response");
        boolean hasNetworkProblem = controller.isHasNetworkProblem();
        if (hasNetworkProblem) {
          logger.info("getWifiTimer network problem?");
        }

        if (wifiStatus != null) {
          wifiStatus.setVisible(hasNetworkProblem);
        }
      }
    };
    timer.scheduleRepeating(1000);
    return timer;
  }

  /**
   * @see FlashcardPanel#getNextButton
   */
  @Override
  protected void gotClickOnNext() {
    abortPlayback();
    loadNext();
  }

  private Button getStartOver() {
    Button startOver = new Button(START_OVER);
    startOver.getElement().setId("AVP_StartOver");

    startOver.setType(ButtonType.SUCCESS);
    startOver.setIcon(IconType.REPEAT);
    startOver.addStyleName("floatRight");
    startOver.addClickHandler(event -> {
      startOver.setEnabled(false);
      doStartOver();
    });
    new TooltipHelper().addTooltip(startOver, START_OVER_FROM_THE_BEGINNING);

    controller.register(startOver, N_A);
    return startOver;
  }

  void doStartOver() {
    abortPlayback();
    sticky.clearCurrent();
    startOver.setEnabled(false);

    reallyStartOver();
  }

  void reallyStartOver() {
    startOver();
  }

  @Override
  protected void abortPlayback() {
    timer.cancelTimer();
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

  void gotSeeScoresClick() {
    abortPlayback();
    seeScores.setEnabled(false);
    onSetComplete();
  }

  /**
   * @return
   * @see #getThreePartContent(ControlState, Panel, DivWidget, DivWidget)
   */
  protected Panel getLeftState() {
    Grid g = getGrid();

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
      ControlGroup correct = new ControlGroup(CORRECT);
      correct.addStyleName("topFiveMargin");

      correctBox = new Label();
      correctBox.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, correct);
      g.setWidget(row++, 1, correctBox);
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
      ControlGroup pronScoreGroup = new ControlGroup(AVG_SCORE);
      pronScoreGroup.addStyleName("topFiveMargin");

      pronScore = new Label();
      pronScore.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, pronScoreGroup);
      g.setWidget(row++, 1, pronScore);
    }

    addMoreStats(g, row);

    setStateFeedback();
    g.addStyleName("rightTenMargin");
    return g;
  }

  void addMoreStats(Grid g, int row) {
  }

  @NotNull
  Grid getGrid() {
    return new Grid(FEEDBACK_SLOTS, 2);
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
}
