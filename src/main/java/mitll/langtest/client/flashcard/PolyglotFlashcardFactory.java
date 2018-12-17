package mitll.langtest.client.flashcard;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.quiz.NewQuizHelper;
import mitll.langtest.shared.custom.QuizSpec;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * @param <L>
 * @param <T>
 * @see mitll.langtest.client.banner.PracticeHelper
 */
public class PolyglotFlashcardFactory<L extends CommonShell, T extends ClientExercise>
    extends StatsFlashcardFactory<L, T> implements PolyglotFlashcardContainer {
  private final Logger logger = Logger.getLogger("PolyglotFlashcardFactory");

  private static final String LISTS = "Lists";

  private static final int HEARTBEAT_INTERVAL = 1000;
  private Timer recurringTimer = null;
  private long roundTimeLeftMillis = -1;

  private PolyglotDialog.MODE_CHOICE mode = PolyglotDialog.MODE_CHOICE.NOT_YET;
  private boolean postedAudio;
  private QuizSpec quizSpec;
  // private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param exerciseList
   * @see HidePolyglotFactory
   */
  PolyglotFlashcardFactory(ExerciseController controller, ListInterface<L, T> exerciseList, INavigation.VIEWS instance) {
    super(controller, exerciseList, instance);

    controller.getListService().getQuizInfo(new SelectionState().getList(), new AsyncCallback<QuizSpec>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(QuizSpec result) {
        gotQuizInfo(result);
      }
    });
  }

  private void gotQuizInfo(QuizSpec result) {
    this.quizSpec = result;
  }

  /**
   * @param e
   * @return
   * @see mitll.langtest.client.list.ExerciseList#addExerciseWidget
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  @NotNull
  protected PolyglotPracticePanel<L, T> getFlashcard(T e) {
    return new PolyglotPracticePanel<L, T>(this,
        controlState,
        controller,
        soundFeedback,
        e,
        sticky,
        exerciseList, instance);
  }

  @NotNull
  protected String getKeyPrefix() {
    return "";
  }

  @Override
  public void startOver() {
    super.startOver();
    stopTimedRun();
  }

  protected int getCurrentExerciseID() {
    return -1;
  }

  @Override
  @NotNull
  StickyState getSticky() {
    return new StickyState(storage) {
      @Override
      protected boolean isCorrect(boolean correct, double score) {
        return (score * 100D) >= Integer.valueOf(quizSpec.getMinScore()).doubleValue();
      }
    };
  }

  /**
   * @see PolyglotPracticePanel#recordingStarted
   */
  @Override
  public void startTimedRun() {
    if (!isRoundTimerRunning()) {
     // logger.info("startTimedRun ->");
      reset();
      startRoundTimer(getIsDry());
      sticky.storeSession();
    } else {
//      logger.warning("startTimedRun timer is not running!");
    }
  }

  /**
   * @see #timerFired
   */
  private void sessionComplete() {
    if (controller.getProjectStartupInfo() != null) {  // could have logged out or gone up in lang hierarchy
      currentFlashcard.cancelAdvanceTimer();

      stopTimedRun();
      if (currentFlashcard.isTabVisible()) {
        currentFlashcard.onSetComplete();
      }
      ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(0);
    }
  }

  /**
   * @see #startOver
   * @see #sessionComplete
   */
  private void stopTimedRun() {
    //  logger.info("stopTimedRun");
    currentFlashcard.stopRecording();
    setBannerVisible(true);
    cancelRoundTimer();
  }

  private void setBannerVisible(boolean b) {
    controller.setBannerVisible(b);
  }

  @Override
  public boolean getIsDry() {
    return mode == PolyglotDialog.MODE_CHOICE.DRY_RUN;
  }

  /**
   * @param isDry
   */
  private void startRoundTimer(boolean isDry) {
    setBannerVisible(false);

    if (!isRoundTimerRunning()) {
      //logger.info("start round timer ");
      clearAnswerMemory();

      doSessionStart();

      makeTimer();

      if (currentFlashcard != null) {
        ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(roundTimeLeftMillis);
      } else {
        logger.warning("startRoundTimer : no current flashcard?");
      }
    } else {
      //   logger.info("startRoundTimer round " + recurringTimer + " " + recurringTimer.isRunning());
    }
  }

  private void doSessionStart() {
    int delayMillis = quizSpec.getRoundMinutes() * 60 * 1000;
    int timeRemainingMillis = Long.valueOf(sticky.getTimeRemainingMillis()).intValue();
    //logger.info("doSessionStart timeRemainingMillis " + timeRemainingMillis);
    roundTimeLeftMillis = timeRemainingMillis > 0 ? timeRemainingMillis : delayMillis;
  }

  /**
   * Every second decrement time remaining...
   */
  private void makeTimer() {
    recurringTimer = new Timer() {
      @Override
      public void run() {
        timerFired();
      }
    };
    recurringTimer.scheduleRepeating(HEARTBEAT_INTERVAL);
  }

  private void timerFired() {
    long l = roundTimeLeftMillis -= HEARTBEAT_INTERVAL;
    sticky.storeTimeRemaining(roundTimeLeftMillis);
    if (currentFlashcard != null && !postedAudio) {
      ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(l);
    }
/*    else {
      logger.info("Skip updated of time left at " + (roundTimeLeftMillis/1000));
    }*/
    if (l < 0) sessionComplete();
  }

  private boolean isRoundTimerRunning() {
    return (recurringTimer != null) && recurringTimer.isRunning();
  }

  private void cancelTimer() {
    if (recurringTimer != null) {
      recurringTimer.cancel();
    }
  }

  public void setMode(PolyglotDialog.MODE_CHOICE mode) {
    this.mode = mode;
  }

  @Override
  public PolyglotDialog.MODE_CHOICE getMode() {
    return mode;
  }

  @Override
  public long getSessionStartMillis() {
    return sticky.getSession();
  }

  /**
   * @return
   * @see PolyglotPracticePanel#maybeAdvance
   */
  @Override
  public boolean isInLightningRound() {
    return isRoundTimerRunning();
  }

  public void startQuiz() {
    sticky.startQuiz();
  }

  @Override
  public boolean isComplete() {
    return sticky.isComplete(getNumExercises());
  }

  @Override
  public long getRoundTimeLeftMillis() {
    return roundTimeLeftMillis;
  }

  protected void listChanged(List<L> items, String selectionID) {
    baseListChanged(items, selectionID);
    //  logger.info("listChanged : " + selectionID + " got new set of items from list. " + items.size());
    Scheduler.get().scheduleDeferred(() -> {
      if (sticky.inQuiz() && sticky.getTimeRemainingMillis() > 0 && hasListSelection()) {
        // inLightningRound = true;
        logger.info("startTimedRun on reload");
        startRoundTimer(getIsDry());
      } else {
        reset();
      }
    });
  }

  /**
   * @see #startTimedRun
   * @see #listChanged
   */
  @Override
  void reset() {
    super.reset();
    //logger.info("reset");
    cancelRoundTimer();
  }

  /**
   * @see #stopTimedRun
   * @see #reset
   */
  @Override
  public void cancelRoundTimer() {
    // logger.info("cancelRoundTimer");
    sticky.endQuiz();
    // if (roundTimer != null) roundTimer.cancel();
    cancelTimer();
    clearTimeRemaining();
  }

  private void clearTimeRemaining() {
    // logger.info("clearTimeRemaining");
    sticky.clearTimeRemaining();
    roundTimeLeftMillis = 0;
  }

  @Override
  public void postAudio() {
    // logger.info("postAudio ");
    postedAudio = true;
  }

  @Override
  public void addRoundTrip(long roundTripMillis) {
    //  logger.info("addRoundTrip ");
    postedAudio = false;
    roundTimeLeftMillis += roundTripMillis;
  }

  /**
   * @param listOverride
   * @see NewQuizHelper#gotQuizChoice
   */
  public void removeItemFromHistory(int listOverride) {
    String currrentToken = History.getToken();
    SelectionState selectionState = new SelectionState(currrentToken, false);

    if (selectionState.getItem() > 0 || listOverride > 0) {
      String listChoice = "";
      if (listOverride > -1) {
        listChoice = LISTS + "=" + listOverride;
      } else {
        Collection<String> lists = selectionState.getTypeToSection().get(LISTS);
        listChoice = lists == null || lists.isEmpty() ? "" : LISTS + "=" + lists.iterator().next();
      }
      //logger.info("lists " + lists);
      String historyToken = getBaseHistoryToken(selectionState) + listChoice;

      if (currrentToken.equalsIgnoreCase(historyToken)) {
        logger.info("removeItemFromHistory no push since no change to " + historyToken);
      } else {
        logger.info("removeItemFromHistory push new token with no item " + historyToken);
      }

      History.newItem(historyToken);
    }
  }
//
//  public void removeListFromHistory() {
//    History.newItem(getBaseHistoryToken(new SelectionState()));
//  }

  @NotNull
  private String getBaseHistoryToken(SelectionState selectionState) {
    return SelectionState.INSTANCE + "=" + selectionState.getView() +
        SelectionState.SECTION_SEPARATOR +
        SelectionState.PROJECT + "=" + selectionState.getProject() +//  controller.getProjectStartupInfo().getProjectid()
        SelectionState.SECTION_SEPARATOR;
  }

  private boolean hasListSelection() {
    SelectionState selectionState = new SelectionState(History.getToken(), false);
    Collection<String> lists = selectionState.getTypeToSection().get(LISTS);
    return lists != null && !lists.isEmpty();
  }

  private void clearAnswerMemory() {
    sticky.clearAnswers();
    // clearTimeRemaining();
  }

  public QuizSpec getQuizSpec() {
    return quizSpec;
  }
}
