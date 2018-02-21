/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectType;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/14.
 * TODOx : store state -- how many items have been done, where are we in the list
 * TODOx : so we can skip around in the list...? if get to end - get score of all the ones we've answered.
 * TODOx : concept of rounds explicit?
 * TODO : review table...?
 */
public class StatsFlashcardFactory<L extends CommonShell, T extends CommonExercise>
    extends ExercisePanelFactory<L, T>
    implements RequiresResize, FlashcardContainer {
  private final Logger logger = Logger.getLogger("StatsFlashcardFactory");

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

  private static final boolean DEBUG = false;

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

  private final ControlState controlState;
  private List<L> allExercises;

  private final Map<Integer, Boolean> exToCorrect = new HashMap<>();
  private final Map<Integer, Double> exToScore = new HashMap<>();

  private String selectionID = "";
  private final String instance;
  private final StickyState sticky;
  private Panel scoreHistory;
  private Map<String, Collection<String>> selection = new HashMap<>();
  //private final UserList ul;
  private Widget contentPanel;
  private boolean isPolyglot;
  private boolean inLightningRound = false;
  private Timer roundTimer = null;
  private Timer recurringTimer = null;
  private long roundTimeLeftMillis = -1;
  private long sessionStartMillis = 0;
  private static final int DRY_RUN_MINUTES = 1;
  private static final int ROUND_MINUTES = 10;
  private static final int DRY_RUN_ROUND_TIME = DRY_RUN_MINUTES * 60 * 1000;
  private static final int ROUND_TIME = ROUND_MINUTES * 60 * 1000;
  private Map<Integer, AudioAnswer> exToLatest = new LinkedHashMap<>();
  private StatsPracticePanel currentFlashcard = null;
  private NewContentChooser navigation;
  private KeyStorage storage;
  private final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());

  /**
   * @param controller
   * @param exerciseList
   * @param instance
   * @see mitll.langtest.client.banner.PracticeHelper#getFactory
   */
  public StatsFlashcardFactory(ExerciseController controller,
                               ListInterface<L, T> exerciseList,
                               String instance) {
    super(controller, exerciseList);
    controlState = new ControlState();
    this.instance = instance;
    //  this.ul = ul;
    if (exerciseList != null) { // TODO ? can this ever happen?
      exerciseList.addListChangedListener(new ListChangeListener<L>() {
        /**
         * @param items
         * @param selectionID
         * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst
         */
        @Override
        public void listChanged(List<L> items, String selectionID) {
          StatsFlashcardFactory.this.selectionID = selectionID;
          allExercises = items;
          //     logger.info("StatsFlashcardFactory : " + selectionID + " got new set of items from list. " + items.size());
          reset();
          cancelRoundTimer();
        }
      });
    }

    storage = new KeyStorage(controller) {
      @Override
      protected String getKey(String name) {
        return (selectionID.isEmpty() ? "" : selectionID + "_") + super.getKey(name); // in the context of this selection
      }
    };
    isPolyglot = isPolyglot(controller);

    sticky = new StickyState(storage) {
      @Override
      protected boolean isCorrect(boolean correct, double score) {
        return isPolyglot ? (score * 100D) >= StatsFlashcardFactory.MIN_POLYGLOT_SCORE : correct;
      }
    };
    controlState.setStorage(storage);
    // logger.info("setting shuffle --------------------- " +controlState.isShuffle()+ "\n");


//    logger.info("is poly glot " + isPolyglot);
    if (exerciseList != null) {
      exerciseList.simpleSetShuffle(controlState.isShuffle());
    }
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

  private PolyglotDialog.MODE_CHOICE mode = PolyglotDialog.MODE_CHOICE.NOT_YET;
  private PolyglotDialog.PROMPT_CHOICE prompt = PolyglotDialog.PROMPT_CHOICE.NOT_YET;

  @Override
  public boolean getIsDry() {
    return mode == PolyglotDialog.MODE_CHOICE.DRY_RUN;
  }

  private boolean isPolyglot(ExerciseController controller) {
    return controller.getProjectStartupInfo().getProjectType() == ProjectType.POLYGLOT;
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
            currentFlashcard.showTimeRemaining(0);
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
            currentFlashcard.showTimeRemaining(l);
          }
        }
      };
      recurringTimer.scheduleRepeating(HEARTBEAT_INTERVAL);

      if (currentFlashcard != null) {
        currentFlashcard.showTimeRemaining(roundTimeLeftMillis);
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

  private void clearAnswerMemory() {
    exToLatest.clear();
  }

  @Override
  public void onResize() {
    if (scoreHistory != null && scoreHistory instanceof RequiresResize) {
      ((RequiresResize) scoreHistory).onResize();
    } else {
      logger.warning("huh? score history doesn't implement requires resize????\\n\n");
    }
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
    boolean recordingEnabled = controller.isRecordingEnabled();

    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    boolean hasModel = (projectStartupInfo != null) && projectStartupInfo.isHasModel();

    boolean showRecordingFlashcard = hasModel && recordingEnabled;

    Panel widgets = showRecordingFlashcard ?
        currentFlashcard = new StatsPracticePanel<L, T>(this,
            controlState,
            controller,
            soundFeedback,
            prompt,
            e.getCommonAnnotatable(),
            sticky,
            exerciseList) :
        getNoRecordFlashcardPanel(e.getCommonAnnotatable());
    if (!showRecordingFlashcard) {
      logger.info("getExercisePanel no recording ");
      currentFlashcard = null;
    } else if (mode != PolyglotDialog.MODE_CHOICE.NOT_YET) {
      // logger.info("startTimedRun is " + mode);
      // if (isPolyglot) startTimedRun();
    } else {
      if (DEBUG) logger.info("mode is " + mode);
    }

    return widgets;
  }

  private FlashcardPanel<CommonAnnotatable> getNoRecordFlashcardPanel(final CommonAnnotatable e) {
    return new FlashcardPanel<CommonAnnotatable>(e,
        StatsFlashcardFactory.this.controller,
        ADD_KEY_BINDING,
        StatsFlashcardFactory.this.controlState,
        soundFeedback,
        soundFeedback.getEndListener(),
        StatsFlashcardFactory.this.instance,
        exerciseList, prompt) {

      @Override
      protected void gotShuffleClick(boolean b) {
        sticky.resetStorage();
        super.gotShuffleClick(b);
      }
    };
  }

  private void reset() {
    sticky.reset();
/*
    exToCorrect.clear();
    exToScore.clear();
    sticky.clearCurrent();
*/
  }

  public int getCurrentExerciseID() {
    return sticky.getCurrentExerciseID();
  }

  /**
   * Pull state out of cache and re-populate correct, incorrect, and score history.
   *
   * @see mitll.langtest.client.banner.PracticeHelper#getMyListLayout
   */
  public void populateCorrectMap() {
    sticky.populateCorrectMap();
  }

  public void resetStorage() {
    sticky.resetStorage();
  }

  public void setSelection(Map<String, Collection<String>> selection) {
    this.selection = selection;
  }

  public void setContentPanel(Widget contentPanel) {
    this.contentPanel = contentPanel;
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

  private int counter = 0;

  public void setNavigation(NewContentChooser navigation) {
    this.navigation = navigation;
  }

  private int getFirstExercise() {
    return allExercises.iterator().next().getID();
  }

  @Override
  public long getSessionStartMillis() {
    return sessionStartMillis;
  }

  @Override
  public boolean isInLightningRound() {
    return inLightningRound;
  }

  public void reload() {
    exerciseList.reload(selection);
  }

  @Override
  public void startOver() {
    int lastID = allExercises.isEmpty() ? -1 : allExercises.get(allExercises.size() - 1).getID();
    int currentExerciseID = sticky.getCurrentExerciseID();
//logger.info("startOver : current " + currentExerciseID + " = " + statsFlashcardFactory.mode);

    if (currentExerciseID != -1 && currentExerciseID != lastID) {
      exerciseList.loadExercise(currentExerciseID);
    } else {
      reallyStartOver();
    }

    if (isPolyglot(controller)) {
/*       if (mode == PolyglotDialog.MODE_CHOICE.NOT_YET) {
        stopTimedRun();
      } else {
        stopTimedRun();
       // startTimedRun();
      }*/

      stopTimedRun();
    }
  }

  private void reallyStartOver() {
    sticky.reset();
    sticky.resetStorage();
    loadFirstExercise();
  }

  public void loadFirstExercise() {
    if (!allExercises.isEmpty()) {
      exerciseList.loadExercise(getFirstExercise());
    }
  }

  @Override
  public long getRoundTimeLeftMillis() {
    return roundTimeLeftMillis;
  }

  @Override
  public int getNumExercises() {
    return allExercises.size();
  }

  public void showDrill() {
    navigation.showDrill();
  }

  public void styleContent(boolean showCard) {
    if (showCard) {
      contentPanel.removeStyleName("noWidthCenterPractice");
      contentPanel.addStyleName("centerPractice");
    } else {
      contentPanel.removeStyleName("centerPractice");
      contentPanel.addStyleName("noWidthCenterPractice");
    }
  }
}