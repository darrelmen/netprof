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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;

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
    implements RequiresResize {
  private final Logger logger = Logger.getLogger("StatsFlashcardFactory");

  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String AVG_SCORE = "Pronunciation";
  private static final String START_OVER = "Start Over";

  private static final String SKIP_TO_END = "See your scores";
  private static final boolean ADD_KEY_BINDING = true;
  private static final String GO_BACK = "Go back";
  private static final String N_A = "N/A";

  private HasID currentExercise;
  private final ControlState controlState;
  private List<L> allExercises;

  private final Map<Integer, Boolean> exToCorrect = new HashMap<>();
  private final Map<Integer, Double> exToScore = new HashMap<>();

  private String selectionID = "";
  private final String instance;
  private final StickyState sticky;
  private Panel scoreHistory;
  private Map<String, Collection<String>> selection = new HashMap<>();
  private final UserList ul;
  private Widget contentPanel;

  /**
   * @param controller
   * @param exerciseList
   * @param instance
   * @param ul
   * @see mitll.langtest.client.custom.content.AVPHelper#getFactory
   */
  public StatsFlashcardFactory(ExerciseController controller,
                               ListInterface<L,T> exerciseList, String instance, UserList ul) {
    super(controller, exerciseList);
    controlState = new ControlState();
    this.instance = instance;
    this.ul = ul;
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
          //    logger.info("StatsFlashcardFactory : " + selectionID + " got new set of items from list. " + items.size());
          reset();
        }
      });
    }
    KeyStorage storage = new KeyStorage(controller) {
      @Override
      protected String getKey(String name) {
        return (selectionID.isEmpty() ? "" : selectionID + "_") + super.getKey(name); // in the context of this selection
      }
    };
    sticky = new StickyState(storage);
    controlState.setStorage(storage);
    // logger.info("setting shuffle --------------------- " +controlState.isShuffle()+ "\n");

    if (exerciseList != null) {
      exerciseList.simpleSetShuffle(controlState.isShuffle());
    }
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
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel
   */
  @Override
  public Panel getExercisePanel(T e) {
    currentExercise = e;
    sticky.storeCurrent(e);
    boolean recordingEnabled = controller.isRecordingEnabled();
//    if (!recordingEnabled) {
//      logger.warning("Recording is *not* enabled!");
//    }
    boolean hasModel = controller.getProjectStartupInfo().isHasModel();
    boolean showRecordingFlashcard = !hasModel || !recordingEnabled;

    return showRecordingFlashcard ?
        getNoRecordFlashcardPanel(e.getCommonAnnotatable()) :
        new StatsPracticePanel(e.getCommonAnnotatable(), exerciseList);
  }

  private FlashcardPanel<CommonAnnotatable> getNoRecordFlashcardPanel(final CommonAnnotatable e) {
    return new FlashcardPanel<CommonAnnotatable>(e,
        StatsFlashcardFactory.this.controller,
        ADD_KEY_BINDING,
        StatsFlashcardFactory.this.controlState,
        soundFeedback,
        soundFeedback.getEndListener(),
        StatsFlashcardFactory.this.instance,
        exerciseList) {

      @Override
      protected void gotShuffleClick(boolean b) {
        sticky.resetStorage();
        super.gotShuffleClick(b);
      }
    };
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
    latestResultID = -1;
    //resultIDs.clear();
    sticky.clearCurrent();
  }

  public int getCurrentExerciseID() {
    return sticky.getCurrentExerciseID();
  }

  /**
   * Pull state out of cache and re-populate correct, incorrect, and score history.
   *
   * @see SimpleChapterNPFHelper#getMyListLayout
   */
  public void populateCorrectMap() {
    String value = sticky.getCorrect();
    if (value != null && !value.trim().isEmpty()) {
      // logger.info("using correct map " + value);
      for (int ex : getIDsFromStorage(value)) {
        exToCorrect.put(ex, Boolean.TRUE);
      }
    }

    value = sticky.getIncorrect();
    if (value != null && !value.trim().isEmpty()) {
      //  logger.info("using incorrect map " + value);
      for (int ex : getIDsFromStorage(value)) {
        exToCorrect.put(ex, Boolean.FALSE);
      }
    }

    value = sticky.getScore();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : getIDsFroStorage(value)) {
        String[] split = pair.split("=");
        if (split.length == 2) {
          String s = split[0];
          int id = Integer.parseInt(s);
          exToScore.put(id, Double.parseDouble(split[1]));
        }
      }
    }
  }

  private Collection<Integer> getIDsFromStorage(String value) {
    String[] split = getIDsFroStorage(value);
    Collection<Integer> ids = new ArrayList<>();
    for (String ex : split) ids.add(Integer.parseInt(ex));
    return ids;
  }

  private String[] getIDsFroStorage(String value) {
    return value.split(",");
  }

  private long latestResultID = -1;
  private final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());

  public void resetStorage() {
    sticky.resetStorage();
  }

  public void setSelection(Map<String, Collection<String>> selection) {
    this.selection = selection;
  }

  public void setContentPanel(Widget contentPanel) {
    this.contentPanel = contentPanel;
  }

  /**
   * @see ExercisePanelFactory#getExercisePanel(Shell)
   */
  private class StatsPracticePanel extends BootstrapExercisePanel<CommonAnnotatable> {
    private Widget container;
    final SetCompleteDisplay completeDisplay = new SetCompleteDisplay();

    public StatsPracticePanel(CommonAnnotatable e, ListInterface<L,T> exerciseListToUse) {
      super(e,
          StatsFlashcardFactory.this.controller,
          ADD_KEY_BINDING,
          StatsFlashcardFactory.this.controlState,
          soundFeedback,
          null,
          StatsFlashcardFactory.this.instance,
          exerciseListToUse);
      soundFeedback.setEndListener(new SoundFeedback.EndListener() {
        @Override
        public void songStarted() {
        }

        @Override
        public void songEnded() {
          removePlayingHighlight();
        }
      });
    }

    @Override
    protected void recordingStarted() {
      soundFeedback.clear();
      removePlayingHighlight();
    }

    /**
     * @see #loadNextOnTimer(int)
     * @see #nextAfterDelay(boolean, String)
     * @see BootstrapExercisePanel#playRefAndGoToNext
     */
    @Override
    protected void loadNext() {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        exerciseList.loadNextExercise(currentExercise.getID());
      }
    }

    /**
     * @param b
     * @see FlashcardPanel#getShuffleButton(ControlState)
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
     * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
     */
    public void receivedAudioAnswer(final AudioAnswer result) {
      // logger.info("StatsPracticePanel.receivedAudioAnswer: result " + result);

      if (result.getValidity() == AudioAnswer.Validity.OK) {
        //resultIDs.add(result.getResultID());
        int id = exercise.getID();
        exToScore.put(id, result.getScore());
        exToCorrect.put(id, result.isCorrect());

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        for (Map.Entry<Integer, Boolean> pair : exToCorrect.entrySet()) {
          if (pair.getValue()) {
            builder.append(pair.getKey()).append(",");
          } else {
            builder2.append(pair.getKey()).append(",");
          }
        }
        sticky.storeCorrect(builder);
        sticky.storeIncorrect(builder2);

        StringBuilder builder3 = new StringBuilder();
        for (Map.Entry<Integer, Double> pair : exToScore.entrySet()) {
          builder3.append(pair.getKey()).append("=").append(pair.getValue()).append(",");
        }
        sticky.storeScore(builder3);

        setStateFeedback();

        latestResultID = result.getResultID();
        //logger.info("\tStatsPracticePanel.receivedAudioAnswer: latest now " + latestResultID);
      } else {

        //    logger.info("got invalid result " + result);
      }
      super.receivedAudioAnswer(result);
    }

    /**
     * Ask for history for those items that were actually practiced.
     *
     * @see #getSkipToEnd()
     * @see #gotClickOnNext()
     * @see #loadNext()
     * @see #nextAfterDelay(boolean, String)
     */
    public void onSetComplete() {
      if (!startOver.isVisible()) return;

      startOver.setVisible(false);
      seeScores.setVisible(false);
      setPrevNextVisible(false);

      sticky.resetStorage();
      if (exercise == null) {
        logger.warning("StatsPracticePanel.onSetComplete. : err : no exercise?");
      } else {
        sticky.storeCurrent(exercise);
      }

      final int user = controller.getUser();

      Set<Integer> copies = new HashSet<>(exToCorrect.keySet());
      if (copies.isEmpty()) {
        for (CommonShell t : allExercises) {
          copies.add(t.getID());
        }
      }

//      logger.info("StatsPracticePanel.onSetComplete. : calling  getUserHistoryForList for " + user +
//          " with " + exToCorrect + " and latest " + latestResultID + " and ids " + copies);

      controller.getService().getUserHistoryForList(user, copies, latestResultID, selection, ul == null ? -1 : ul.getID(), new AsyncCallback<AVPScoreReport>() {
        @Override
        public void onFailure(Throwable caught) {
          logger.warning("StatsPracticePanel.onSetComplete. : got failure " + caught);
        }

        @Override
        public void onSuccess(AVPScoreReport scoreReport) {
          // List<AVPHistoryForList> result = scoreReport.getAvpHistoryForLists();
          showFeedbackCharts(/*result,*/ scoreReport.getSortedHistory());
        }
      });
    }

    private void showFeedbackCharts(/*List<AVPHistoryForList> result,*/ final List<ExerciseCorrectAndScore> sortedHistory) {
      setMainContentVisible(false);
      contentPanel.removeStyleName("centerPractice");
      contentPanel.addStyleName("noWidthCenterPractice");
      HorizontalPanel widgets = new HorizontalPanel();
      container = widgets;
      scoreHistory = completeDisplay.getScoreHistory(sortedHistory, allExercises, controller);
      scoreHistory.add(getButtonsBelowScoreHistory());
      widgets.add(scoreHistory);

//      completeDisplay.addLeftAndRightCharts(result, exToScore.values(), getCorrect(), getIncorrect(), allExercises.size(), widgets);

      belowContentDiv.add(container);
    }

    /**
     * @return
     * @see #showFeedbackCharts
     */
    private Panel getButtonsBelowScoreHistory() {
      Panel child = new HorizontalPanel();

      final Button w = getIncorrectListButton();
      child.add(w);
      w.addStyleName("topFiveMargin");
      Button repeatButton = getRepeatButton();
      repeatButton.addStyleName("topFiveMargin");
      repeatButton.addStyleName("leftFiveMargin");

      child.add(repeatButton);

      DivWidget lefty = new DivWidget();
      // lefty.addStyleName("floatLeft");
      lefty.add(child);
      return lefty;
    }

    private Button getIncorrectListButton() {
      final Button w = new Button();
      w.setType(ButtonType.SUCCESS);
      w.setText(START_OVER);
      w.setIcon(IconType.REPEAT);

      w.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //   logger.info("----> click on " + START_OVER);
          w.setVisible(false);
          doIncorrectFirst();
        }
      });


      controller.register(w, N_A);
      return w;
    }

    /**
     * @see #getIncorrectListButton()
     */
    void doIncorrectFirst() {
      showFlashcardDisplay();

      currentExercise = null;
      reset();

      exerciseList.reload(selection);
      makeFlashcardButtonsVisible();

      sticky.resetStorage();
    }

    private int getCorrect() {
      int count = 0;
      for (Boolean val : exToCorrect.values()) {
        if (val) count++;
      }
      return count;
    }

    private int getIncorrect() {
      int count = 0;
      for (Boolean val : exToCorrect.values()) {
        if (!val) count++;
      }
      return count;
    }

    /**
     * @return
     * @see StatsFlashcardFactory.StatsPracticePanel#showFeedbackCharts
     */
    private Button getRepeatButton() {
      final Button w1 = new Button(GO_BACK);
      w1.setIcon(IconType.UNDO);
      w1.getElement().setId("AVP_DoWholeSetFromStart");
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
//          logger.info("getRepeatButton : click on " + GO_BACK);
          w1.setVisible(false);
          showFlashcardDisplay();

          startOver();
        }
      });
      controller.register(w1, N_A);
      return w1;
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
     * @see #getRepeatButton()
     * @see #getStartOver()
     */
    void startOver() {
      makeFlashcardButtonsVisible();

//      String lastID = "";
      int lastID = allExercises.isEmpty() ? -1 : allExercises.get(allExercises.size() - 1).getID();
//      for (L ex : allExercises) {
//        lastID = ex.getOldID();
//      }
//      String lastID = allExercises.get(allExercises.size() - 1).getOldID();
      int currentExerciseID = sticky.getCurrentExerciseID();

      //  logger.info("startOver : current " + currentExerciseID);

      if (currentExerciseID != -1 && currentExerciseID != lastID) {
        exerciseList.loadExercise(currentExerciseID);
      } else {
        reset();
        sticky.resetStorage();

        if (!allExercises.isEmpty()) {
          int first = allExercises.iterator().next().getID();
          exerciseList.loadExercise(first);
        }
      }
    }

    private void makeFlashcardButtonsVisible() {
      contentPanel.removeStyleName("noWidthCenterPractice");
      contentPanel.addStyleName("centerPractice");
      //   logger.info("makeFlashcardButtonsVisible ---- \n\n\n");

      startOver.setVisible(true);
      startOver.setEnabled(true);
      seeScores.setVisible(true);
      setPrevNextVisible(true);
    }

    /**
     * @param correct
     * @param feedback
     * @see #receivedAudioAnswer(AudioAnswer)
     * @see #showIncorrectFeedback
     */
    protected void nextAfterDelay(boolean correct, String feedback) {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        // logger.info("nextAfterDelay " + correct);
        loadNextOnTimer(CORRECT_DELAY);
      }
    }

    private Button startOver, seeScores;
    private Panel belowContentDiv;

    /**
     * @param toAddTo
     * @see mitll.langtest.client.flashcard.FlashcardPanel#FlashcardPanel
     */
    @Override
    protected void addRowBelowPrevNext(DivWidget toAddTo) {
      toAddTo.add(getSkipToEnd());
      toAddTo.add(getStartOver());
      belowContentDiv = toAddTo;
    }

    /**
     * @see FlashcardPanel#getNextButton()
     */
    @Override
    protected void gotClickOnNext() {
      abortPlayback();
      //logger.info("on last " + exerciseList.onLast());
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        //logger.info("load next " + exerciseList.getCurrentExerciseID());
        exerciseList.loadNext();
      }
    }

    private Button getStartOver() {
      startOver = new Button(START_OVER);
      startOver.getElement().setId("AVP_StartOver");

      startOver.setType(ButtonType.SUCCESS);
      startOver.setIcon(IconType.REPEAT);
      startOver.addStyleName("floatRight");
      startOver.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          abortPlayback();
          sticky.clearCurrent();

          startOver.setEnabled(false);
          startOver();
        }
      });
      new TooltipHelper().addTooltip(startOver, "Start over from the beginning.");

      controller.register(startOver, N_A);
      return startOver;
    }

    @Override
    protected void abortPlayback() {
      cancelTimer();
      soundFeedback.clear();
    }

    /**
     * @return
     * @see #addRowBelowPrevNext(com.github.gwtbootstrap.client.ui.base.DivWidget)
     */
    private Button getSkipToEnd() {
      seeScores = new Button(SKIP_TO_END);
      seeScores.setIcon(IconType.TROPHY);
      seeScores.getElement().setId("AVP_SkipToEnd");
      controller.register(seeScores, N_A);

      seeScores.addStyleName("leftFiveMargin");
      seeScores.setType(ButtonType.PRIMARY);
      seeScores.addStyleName("floatRight");
      seeScores.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          abortPlayback();
          seeScores.setEnabled(false);
          //currentExerciseID = currentExercise.getOldID();
          onSetComplete();
        }
      });
      new TooltipHelper().addTooltip(seeScores, SKIP_TO_END);
      return seeScores;
    }

    private Label remain, incorrectBox, correctBox, pronScore;

    protected Panel getLeftState() {
      Grid g = new Grid(4, 2);
      ControlGroup remaining = new ControlGroup(REMAINING);
      remaining.addStyleName("topFiveMargin");
      remain = new Label();
      remain.setType(LabelType.INFO);
      int row = 0;
      g.setWidget(row, 0, remaining);
      g.setWidget(row++, 1, remain);

      ControlGroup incorrect = new ControlGroup(INCORRECT);
      incorrect.addStyleName("topFiveMargin");

      incorrectBox = new Label();
      incorrectBox.setType(LabelType.WARNING);

      g.setWidget(row, 0, incorrect);
      g.setWidget(row++, 1, incorrectBox);

      ControlGroup correct = new ControlGroup(CORRECT);
      correct.addStyleName("topFiveMargin");

      correctBox = new Label();
      correctBox.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, correct);
      g.setWidget(row++, 1, correctBox);


      ControlGroup pronScoreGroup = new ControlGroup(AVG_SCORE);
      pronScoreGroup.addStyleName("topFiveMargin");

      pronScore = new Label();
      pronScore.setType(LabelType.SUCCESS);

      g.setWidget(row, 0, pronScoreGroup);
      pronScoreGroup.addStyleName("rightFiveMargin");
      g.setWidget(row++, 1, pronScore);

      setStateFeedback();
      g.addStyleName("rightTenMargin");
      return g;
    }

    /**
     * @see #getLeftState()
     * @see #receivedAudioAnswer(AudioAnswer)
     */
    private void setStateFeedback() {
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      int remaining = allExercises.size() - totalCorrect - totalIncorrect;
      remain.setText(remaining + "");
      incorrectBox.setText(totalIncorrect + "");
      correctBox.setText(totalCorrect + "");

      double total = 0;
      int count = 0;
      for (double score : exToScore.values()) {
        if (score > 0) {
          total += score;
          count++;
        }
      }
      if (count > 0) {
        total /= count;
      }

      // TODO : come back to the color coding ...
      LabelType type = total > 0.8 ? LabelType.SUCCESS :
          total > 0.5 ? LabelType.INFO : LabelType.WARNING;
      //  logger.info("type "+type + " score " + total);
      pronScore.setType(type);

      total *= 100;
      int itotal = (int) Math.ceil(total);

      pronScore.setText("" + itotal);
    }
  }
}
