package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PolyglotFlashcardFactory<L extends CommonShell, T extends CommonExercise>
    extends StatsFlashcardFactory<L, T>
    implements PolyglotFlashcardContainer {

  private final Logger logger = Logger.getLogger("PolyglotFlashcardFactory");


  static final int MIN_POLYGLOT_SCORE = 35;
  private static final int HEARTBEAT_INTERVAL = 1000;
  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;
  private static final int DRY_RUN_ROUND_TIME = PolyglotFlashcardFactory.DRY_RUN_MINUTES * 60 * 1000;
  private static final int ROUND_TIME = PolyglotFlashcardFactory.ROUND_MINUTES * 60 * 1000;
  private boolean inLightningRound = false;
  private Timer roundTimer = null;
  private Timer recurringTimer = null;
  private long roundTimeLeftMillis = -1;
  private long sessionStartMillis = 0;
  private PolyglotDialog.MODE_CHOICE mode = PolyglotDialog.MODE_CHOICE.NOT_YET;
  PolyglotDialog.PROMPT_CHOICE prompt = PolyglotDialog.PROMPT_CHOICE.NOT_YET;

  public PolyglotFlashcardFactory(ExerciseController controller, ListInterface exerciseList, String instance) {
    super(controller, exerciseList, instance);
  }


  /**
   * @param e
   * @return
   * @see mitll.langtest.client.list.ExerciseList#addExerciseWidget
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  @Override
  public Panel getExercisePanel(T e) {
    sticky.storeCurrent(e);

    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    boolean hasModel = (projectStartupInfo != null) && projectStartupInfo.isHasModel();

    boolean showRecordingFlashcard = hasModel && controller.isRecordingEnabled();

    Panel widgets = showRecordingFlashcard ?
        currentFlashcard = getCurrentFlashcard(e) :
        getNoRecordFlashcardPanel(e.getCommonAnnotatable());
    if (!showRecordingFlashcard) {
//      logger.info("getExercisePanel no recording ");
      currentFlashcard = null;
    }

    return widgets;
  }

  @NotNull
  PolyglotPracticePanel getCurrentFlashcard(T e) {
    return new PolyglotPracticePanel(this,
        controlState,
        controller,
        soundFeedback,
        prompt,
        e.getCommonAnnotatable(),
        sticky,
        exerciseList);
  }

  @Override
  public void startOver() {
    super.startOver();
    stopTimedRun();

  }

  @Override
  @NotNull
  StickyState getSticky() {
    return new StickyState(storage) {
      @Override
      protected boolean isCorrect(boolean correct, double score) {
        return (score * 100D) >= PolyglotFlashcardFactory.MIN_POLYGLOT_SCORE;
      }
    };
  }

  /**
   *
   */
  @Override
  public void startTimedRun() {
    if (!inLightningRound) {
      inLightningRound = true;
      reset();
      startRoundTimer(getIsDry());
    }
  }

  private void stopTimedRun() {
    inLightningRound = false;
    cancelRoundTimer();
  }

  @Override
  public boolean getIsDry() {
    return mode == PolyglotDialog.MODE_CHOICE.DRY_RUN;
  }

  private void startRoundTimer(boolean isDry) {
    if (isRoundTimerNotRunning()) {
      clearAnswerMemory();

      roundTimer = new Timer() {
        @Override
        public void run() {
          logger.info("startRoundTimer ----> at " + System.currentTimeMillis());
          if (controller.getProjectStartupInfo() != null) {  // could have logged out or gone up in lang hierarchy
            currentFlashcard.cancelAdvanceTimer();
            stopTimedRun();
            if (currentFlashcard.isTabVisible()) {
              currentFlashcard.onSetComplete();
            }
            //       cancelRoundTimer();
            ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(0);
            //     inLightningRound = false;
          }
        }
      };

      {
        int delayMillis = isDry ? DRY_RUN_ROUND_TIME : ROUND_TIME;
        roundTimer.schedule(delayMillis);
        roundTimeLeftMillis = delayMillis;
        sessionStartMillis = System.currentTimeMillis();
      }

      recurringTimer = new Timer() {
        @Override
        public void run() {
          long l = roundTimeLeftMillis -= HEARTBEAT_INTERVAL;
          //     logger.info("show time remaining on " + currentFlashcard);
          if (currentFlashcard != null) {
            ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(l);
          }
        }
      };
      recurringTimer.scheduleRepeating(HEARTBEAT_INTERVAL);

      if (currentFlashcard != null) {
        ((PolyglotPracticePanel) currentFlashcard).showTimeRemaining(roundTimeLeftMillis);
      } else {
        logger.warning("no current flashcard?");
      }
    }
  }

  private boolean isRoundTimerNotRunning() {
    return (roundTimer == null) || !roundTimer.isRunning();
  }

  @Override
  public void cancelRoundTimer() {
    logger.info("cancel round timer -");
    if (roundTimer != null) roundTimer.cancel();
    if (recurringTimer != null) recurringTimer.cancel();
    roundTimeLeftMillis = 0;
  }

  public void setMode(PolyglotDialog.MODE_CHOICE mode, PolyglotDialog.PROMPT_CHOICE prompt) {
    this.mode = mode;
    this.prompt = prompt;

    //  logger.info("setMode : prompt is " + prompt);
    if (prompt == PolyglotDialog.PROMPT_CHOICE.PLAY) controlState.setAudioOn(true);
    else if (prompt == PolyglotDialog.PROMPT_CHOICE.DONT_PLAY) controlState.setAudioOn(false);
  }

  @Override
  public PolyglotDialog.MODE_CHOICE getMode() {
    return mode;
  }

  @Override
  public long getSessionStartMillis() {
    return sessionStartMillis;
  }

  @Override
  public boolean isInLightningRound() {
    return inLightningRound;
  }

  @Override
  public long getRoundTimeLeftMillis() {
    return roundTimeLeftMillis;
  }

  @Override
  void reset() {
    super.reset();
    cancelRoundTimer();

  }

  private void clearAnswerMemory() {
    sticky.clearAnswers();
  }
}
