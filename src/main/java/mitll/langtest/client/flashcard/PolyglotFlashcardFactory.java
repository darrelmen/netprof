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
  private Logger logger = Logger.getLogger("PolyglotFlashcardFactory");

  private static final String LISTS = "Lists";

  private static final int HEARTBEAT_INTERVAL = 1000;
  private Timer recurringTimer = null;
  /**
   *
   */
  private long roundTimeLeftMillis = -1;

  private boolean postedAudio;
  private QuizSpec quizSpec;

  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param exerciseList
   * @see HidePolyglotFactory
   */
  public PolyglotFlashcardFactory(ExerciseController controller, ListInterface<L, T> exerciseList,
                                  INavigation.VIEWS instance) {
    super(controller, exerciseList, instance);
    getQuizSpec(controller, new SelectionState().getList());
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
    return new HidePolyglotPanel<L, T>(this,
        controlState,
        controller,
        soundFeedback,
        e,
        sticky,
        exerciseList,
        instance
    );
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
      if (DEBUG) logger.info("startTimedRun ->");
      reset();
      startRoundTimer();
      sticky.storeSession();
    } else {
      if (DEBUG) logger.info("startTimedRun timer is not running!");
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
    if (DEBUG) logger.info("stopTimedRun");
    currentFlashcard.stopRecording();
    setBannerVisible(true);
    cancelRoundTimer();
  }

  private void setBannerVisible(boolean b) {
    controller.setBannerVisible(b);
  }

  /**
   *
   */
  private void startRoundTimer() {
    setBannerVisible(false);

    if (!isRoundTimerRunning()) {
      if (DEBUG) logger.info("startRoundTimer start round timer ");
      clearAnswerMemory();

      doSessionStart();

      makeTimer();

      if (currentFlashcard != null) {
        ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(roundTimeLeftMillis);
      } else {
        if (logger == null) {
          logger = Logger.getLogger("PolyglotFlashcardFactory");
        }
        logger.warning("startRoundTimer : no current flashcard?");
      }
    }
    //else {
      //   logger.info("startRoundTimer round " + recurringTimer + " " + recurringTimer.isRunning());
   // }
  }

  private void doSessionStart() {
    int delayMillis = quizSpec.getRoundMinutes() * 60 * 1000;
    int timeRemainingMillis = Long.valueOf(sticky.getTimeRemainingMillis()).intValue();
    if (DEBUG) logger.info("doSessionStart sticky timeRemainingMillis " + timeRemainingMillis);
    roundTimeLeftMillis = timeRemainingMillis > 0 ? timeRemainingMillis : delayMillis;
  }

  /**
   * Every second decrement time remaining...
   */
  private void makeTimer() {
    if (DEBUG) logger.info("makeTimer sticky timeRemainingMillis " + sticky.getTimeRemainingMillis());
    recurringTimer = new Timer() {
      @Override
      public void run() {
        timerFired();
      }
    };
    recurringTimer.scheduleRepeating(HEARTBEAT_INTERVAL);
  }

  private void timerFired() {
    long newTimeRemaining = roundTimeLeftMillis -= HEARTBEAT_INTERVAL;
    sticky.storeTimeRemaining(roundTimeLeftMillis);
    if (currentFlashcard != null && !postedAudio) {
      ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(newTimeRemaining);
    }
/*    else {
      logger.info("Skip updated of time left at " + (roundTimeLeftMillis/1000));
    }*/
    if (newTimeRemaining < 0) {
      sessionComplete();
    }
  }

  private boolean isRoundTimerRunning() {
    return (recurringTimer != null) && recurringTimer.isRunning();
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

  /**
   * @see NewQuizHelper#gotQuizChoice
   */
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

  /**
   * @param items
   * @param selectionID
   * @see StatsFlashcardFactory#StatsFlashcardFactory(ExerciseController, ListInterface, INavigation.VIEWS)
   */
  protected void listChanged(List<L> items, String selectionID) {
    baseListChanged(items, selectionID);
    if (DEBUG) logger.info("listChanged : " + selectionID + " got new set of items from list. " + items.size());

    Scheduler.get().scheduleDeferred(this::maybeRestartQuiz);
  }

  private void maybeRestartQuiz() {
    if (sticky.inQuiz() && sticky.getTimeRemainingMillis() > 0 && hasListSelection()) {
      if (DEBUG) logger.info("startTimedRun on reload");
      startRoundTimer();
    } else {
      reset();
    }
  }

  /**
   * @see #startTimedRun
   * @see #listChanged
   */
  @Override
  void reset() {
    super.reset();
    if (DEBUG) logger.info("reset");
    cancelRoundTimer();
  }

  /**
   * @see #stopTimedRun
   * @see #reset
   */
  @Override
  public void cancelRoundTimer() {
    if (DEBUG) logger.info("cancelRoundTimer");

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
//    logger.info("cancelRoundTimer : logException stack " + exceptionAsString);

    sticky.endQuiz();
    cancelTimer();
    clearTimeRemaining();
  }

  private void cancelTimer() {
    if (recurringTimer != null) {
      recurringTimer.cancel();
    }
  }

  private void clearTimeRemaining() {
    sticky.clearTimeRemaining();

    if (DEBUG) logger.info("PolyglotFlashcardFactory.clearTimeRemaining now " +sticky.getTimeRemainingMillis());

    roundTimeLeftMillis = 0;
  }

  @Override
  public void postAudio() {
    postedAudio = true;
  }

  @Override
  public void addRoundTrip(long roundTripMillis) {
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

        getQuizSpec(controller, listOverride);
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

  private void getQuizSpec(ExerciseController controller, int list) {
    if (DEBUG) logger.info("getQuizSpec (" + History.getToken() + ") chosen list " + list);
    controller.getListService().getQuizInfo(list, new AsyncCallback<QuizSpec>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("get quiz info", caught);
      }

      @Override
      public void onSuccess(QuizSpec result) {
        gotQuizInfo(result);
      }
    });
  }

  @NotNull
  private String getBaseHistoryToken(SelectionState selectionState) {
    return SelectionState.INSTANCE + "=" + selectionState.getView() +
        SelectionState.SECTION_SEPARATOR +
        SelectionState.PROJECT + "=" + selectionState.getProject() +
        SelectionState.SECTION_SEPARATOR;
  }

  private boolean hasListSelection() {
    SelectionState selectionState = new SelectionState(History.getToken(), false);
    Collection<String> lists = selectionState.getTypeToSection().get(LISTS);
    return lists != null && !lists.isEmpty();
  }

  private void clearAnswerMemory() {
    sticky.clearAnswers();
  }

  public QuizSpec getQuizSpec() {
    return quizSpec;
  }
}
