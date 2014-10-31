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
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/10/14.
 * TODOx : store state -- how many items have been done, where are we in the list
 * TODOx : so we can skip around in the list...? if get to end - get score of all the ones we've answered.
 * TODOx : concept of rounds explicit?
 * TODO : review table...?
 */
public class StatsFlashcardFactory extends ExercisePanelFactory implements RequiresResize {
  private Logger logger = Logger.getLogger("StatsFlashcardFactory");

  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String AVG_SCORE = "Pronunciation";
  private static final String START_OVER = "Start Over";

  private static final String SKIP_TO_END = "See your scores";
  private static final boolean ADD_KEY_BINDING = true;
  private static final String GO_BACK = "Go back";
  public static final String N_A = "N/A";

  private CommonExercise currentExercise;
  private final ControlState controlState;
  private List<CommonShell> allExercises;//, originalExercises;

  private final Map<String, Boolean> exToCorrect = new HashMap<String, Boolean>();
  private final Map<String, Double> exToScore = new HashMap<String, Double>();
  private final Set<Long> resultIDs = new HashSet<Long>();
  private String selectionID = "";
  private final String instance;
  final StickyState sticky;
  Panel scoreHistory;
  private Map<String, Collection<String>> selection;
  UserList ul;
  private Widget contentPanel;


  /**
   * @param service
   * @param feedback
   * @param controller
   * @param exerciseList
   * @param instance
   * @param ul
   * @see mitll.langtest.client.custom.content.AVPHelper#getFactory
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   */
  public StatsFlashcardFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                               ListInterface exerciseList, String instance, UserList ul) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
    this.instance = instance;
    this.ul = ul;
    // System.out.println("made factory ---------------------\n");
/*    boolean sharedList = exerciseList == null;
    if (sharedList) {   // when does this happen??
      exerciseList = controller.getExerciseList();
    }*/

    if (exerciseList != null) { // TODO ? can this ever happen?
      exerciseList.addListChangedListener(new ListChangeListener<CommonShell>() {
        /**
         * @param items
         * @param selectionID
         * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst
         */
        @Override
        public void listChanged(List<CommonShell> items, String selectionID) {
          StatsFlashcardFactory.this.selectionID = selectionID;
          allExercises = items;
      //    System.out.println("StatsFlashcardFactory : " + selectionID + " got new set of items from list. " + items.size());
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
    // System.out.println("setting shuffle --------------------- " +controlState.isShuffle()+ "\n");

    if (exerciseList != null) {
      exerciseList.simpleSetShuffle(controlState.isShuffle());
    }
  }

  @Override
  public void onResize() {
    if (scoreHistory != null && scoreHistory instanceof RequiresResize) {
      ((RequiresResize) scoreHistory).onResize();
    } else {
      System.err.println("huh? score history doesn't implement requires resize????\\n\n");
    }
  }

  /**
   * @param e
   * @return
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel(mitll.langtest.shared.CommonExercise)
   */
  @Override
  public Panel getExercisePanel(CommonExercise e) {
    currentExercise = e;
    sticky.storeCurrent(e);
    return controller.getProps().isNoModel() || !controller.isRecordingEnabled() ? new FlashcardPanel(e,
        StatsFlashcardFactory.this.service,
        StatsFlashcardFactory.this.controller,
        ADD_KEY_BINDING,
        StatsFlashcardFactory.this.controlState,
        soundFeedback,
        soundFeedback.endListener, StatsFlashcardFactory.this.instance, exerciseList) {
      @Override
      protected void gotShuffleClick(boolean b) {
        sticky.resetStorage();
        super.gotShuffleClick(b);
      }
    } : new StatsPracticePanel(e, exerciseList);
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
    latestResultID = -1;
    resultIDs.clear();
    sticky.clearCurrent();
  }

  public String getCurrentExerciseID() {
    return sticky.getCurrentExerciseID();
  }

  /**
   * Pull state out of cache and re-populate correct, incorrect, and score history.
   *
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   */
  public void populateCorrectMap() {
    String value = sticky.getCorrect();
    if (value != null && !value.trim().isEmpty()) {
      // System.out.println("using correct map " + value);
      for (String ex : value.split(",")) {
        exToCorrect.put(ex, Boolean.TRUE);
      }
    }

    value = sticky.getIncorrect();
    if (value != null && !value.trim().isEmpty()) {
      //  System.out.println("using incorrect map " + value);
      for (String ex : value.split(",")) {
        exToCorrect.put(ex, Boolean.FALSE);
      }
    }

    value = sticky.getScore();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : value.split(",")) {
        String[] split = pair.split("=");
        if (split.length == 2) {
          exToScore.put(split[0], Double.parseDouble(split[1]));
        }
      }
    }
  }

  private long latestResultID = -1;
  private final MySoundFeedback soundFeedback = new MySoundFeedback();

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
   * @see #getExercisePanel(mitll.langtest.shared.CommonExercise)
   */
  private class StatsPracticePanel extends BootstrapExercisePanel {
    private Widget container;
    final SetCompleteDisplay completeDisplay = new SetCompleteDisplay();

    public StatsPracticePanel(CommonExercise e, ListInterface exerciseListToUse) {
      super(e,
          StatsFlashcardFactory.this.service,
          StatsFlashcardFactory.this.controller,
          ADD_KEY_BINDING,
          StatsFlashcardFactory.this.controlState,
          soundFeedback,
          soundFeedback.endListener, StatsFlashcardFactory.this.instance, exerciseListToUse);
    //  System.out.println("made " + this + " for " + e.getID());
    }

    @Override
    protected void recordingStarted() {
      soundFeedback.clear();
      removePlayingHighlight();
    }

    /**
     * @see #loadNextOnTimer(int)
     * @see #nextAfterDelay(boolean, String)
     * @see #playRefAndGoToNext(String)
     */
    @Override
    protected void loadNext() {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        exerciseList.loadNextExercise(currentExercise.getID());
      }
    }

    @Override
    protected void gotShuffleClick(boolean b) {
      sticky.resetStorage();
      super.gotShuffleClick(b);
    }

    /**
     * @param result
     * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, com.google.gwt.user.client.ui.Panel)
     */
    public void receivedAudioAnswer(final AudioAnswer result) {
      System.out.println("StatsPracticePanel.receivedAudioAnswer: result " + result);

      if (result.getValidity() == AudioAnswer.Validity.OK) {
        resultIDs.add(result.getResultID());
        exToScore.put(exercise.getID(), result.getScore());
        exToCorrect.put(exercise.getID(), result.isCorrect());

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        for (Map.Entry<String, Boolean> pair : exToCorrect.entrySet()) {
          if (pair.getValue()) {
            builder.append(pair.getKey()).append(",");
          } else {
            builder2.append(pair.getKey()).append(",");
          }
        }
        sticky.storeCorrect(builder);
        sticky.storeIncorrect(builder2);

        StringBuilder builder3 = new StringBuilder();
        for (Map.Entry<String, Double> pair : exToScore.entrySet()) {
          builder3.append(pair.getKey()).append("=").append(pair.getValue()).append(",");
        }
        sticky.storeScore(builder3);

        setStateFeedback();

        latestResultID = result.getResultID();
        //System.out.println("\tStatsPracticePanel.receivedAudioAnswer: latest now " + latestResultID);
      } else {
        logger.info("got invalid result " + result);
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
        System.err.println("StatsPracticePanel.onSetComplete. : err : no exercise?");
      }
      else {
        sticky.storeCurrent(exercise);
      }

      final int user = controller.getUser();

      Set<String> ids = exToCorrect.keySet();

      Set<String> copies = new HashSet<String>(ids);
      if (copies.isEmpty()) {
        for (CommonShell t : allExercises) {
          copies.add(t.getID());
        }
      }

/*      System.out.println("StatsPracticePanel.onSetComplete. : calling  getUserHistoryForList for " + user +
        " with " + exToCorrect + " and latest " + latestResultID + " and ids " +copies);*/

      service.getUserHistoryForList(user, copies, latestResultID, selection, ul == null ? -1 : ul.getUniqueID(), new AsyncCallback<AVPScoreReport>() {
        @Override
        public void onFailure(Throwable caught) {
          //System.err.println("StatsPracticePanel.onSetComplete. : got failure " + caught);
        }

        @Override
        public void onSuccess(AVPScoreReport scoreReport) {
          List<AVPHistoryForList> result = scoreReport.getAvpHistoryForLists();
          showFeedbackCharts(result, scoreReport.getSortedHistory());
        }
      });
    }

    private void showFeedbackCharts(List<AVPHistoryForList> result, final List<ExerciseCorrectAndScore> sortedHistory) {
      setMainContentVisible(false);

      contentPanel.removeStyleName("centerPractice");
      contentPanel.addStyleName("noWidthCenterPractice");
    //  System.out.println("showFeedbackCharts ---- \n\n\n");

      HorizontalPanel widgets = new HorizontalPanel();
      container = widgets;
      scoreHistory = completeDisplay.getScoreHistory(sortedHistory, allExercises, controller);
      scoreHistory.add(getButtonsBelowScoreHistory());
      widgets.add(scoreHistory);
      completeDisplay.addLeftAndRightCharts(result, exToScore, getCorrect(), getIncorrect(), allExercises.size(),widgets);
      belowContentDiv.add(container);
    }

    private Panel getButtonsBelowScoreHistory() {
      Panel child = new VerticalPanel();

      final Button w = getIncorrectListButton();
      child.add(w);
      w.addStyleName("topFiveMargin");
      Button repeatButton = getRepeatButton();
      repeatButton.addStyleName("topFiveMargin");

      child.add(repeatButton);
      return child;
    }

    private Button getIncorrectListButton() {
      final Button w = new Button();
      w.setType(ButtonType.SUCCESS);
      w.setText(START_OVER);
      w.setIcon(IconType.REPEAT);

      w.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //   System.out.println("----> click on " + START_OVER);
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
    protected void doIncorrectFirst() {
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

//          System.out.println("getRepeatButton : click on " + GO_BACK);

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

      String lastID = allExercises.get(allExercises.size() - 1).getID();
      String currentExerciseID = sticky.getCurrentExerciseID();

       System.out.println("startOver : current " + currentExerciseID);

      if (currentExerciseID != null && !currentExerciseID.isEmpty() && !currentExerciseID.equals(lastID)) {
        exerciseList.loadExercise(currentExerciseID);
      } else {
        reset();

        sticky.resetStorage();

        String first = allExercises.iterator().next().getID();
        exerciseList.loadExercise(first);
      }
    }

    private void makeFlashcardButtonsVisible() {
     contentPanel.removeStyleName("noWidthCenterPractice");
      contentPanel.addStyleName("centerPractice");

   //   System.out.println("makeFlashcardButtonsVisible ---- \n\n\n");

      startOver.setVisible(true);
      startOver.setEnabled(true);
      seeScores.setVisible(true);
      setPrevNextVisible(true);
    }

    /**
     * @param correct
     * @param feedback
     * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
     * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
     */
    protected void nextAfterDelay(boolean correct, String feedback) {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        int delayMillis = DELAY_MILLIS;
        loadNextOnTimer(delayMillis);
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

      //System.out.println("on last " + exerciseList.onLast());
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        //System.out.println("load next " + exerciseList.getCurrentExerciseID());

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
          //currentExerciseID = currentExercise.getID();
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
     * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
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
      //  System.out.println("type "+type + " score " + total);
      pronScore.setType(type);

      total *= 100;
      int itotal = (int) Math.ceil(total);

      pronScore.setText("" + itotal);
    }
  }

  public class MySoundFeedback extends SoundFeedback {
    public MySoundFeedback() {
      super(StatsFlashcardFactory.this.controller.getSoundManager());
    }

    public synchronized void queueSong(String song, SoundFeedback.EndListener endListener) {
      //System.out.println("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
      destroySound(); // if there's something playing, stop it!
      createSound(song, endListener);
    }

    public synchronized void queueSong(String song) {
      //System.out.println("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
      destroySound(); // if there's something playing, stop it!
      createSound(song, endListener);
    }

    public synchronized void clear() {
      //  System.out.println("\t stop playing current sound -------  "+ System.currentTimeMillis());
      destroySound(); // if there's something playing, stop it!

    }

    // TODO : remove this empty listener
    private final SoundFeedback.EndListener endListener = new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        //System.out.println("song started --------- "+ System.currentTimeMillis());
      }

      @Override
      public void songEnded() {
        //System.out.println("song ended   --------- " + System.currentTimeMillis());
      }
    };
  }
}
