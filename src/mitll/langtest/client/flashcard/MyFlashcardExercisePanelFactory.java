package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import org.moxieapps.gwt.highcharts.client.Chart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 2/10/14.
 * TODOx : store state -- how many items have been done, where are we in the list
 * TODOx : so we can skip around in the list...? if get to end - get score of all the ones we've answered.
 * TODOx : concept of rounds explicit?
 * TODO : review table...?
 */
public class MyFlashcardExercisePanelFactory extends ExercisePanelFactory {
  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String AVG_SCORE = "Pronunciation";
  private static final String START_OVER = "Start Over";

  private static final String SKIP_TO_END = "See your scores";
  private static final int TABLE_WIDTH = 2 * 275;
  private static final boolean ADD_KEY_BINDING = true;
 // public static final String JUMP_TO_THE_END = "Jump to end and see your scores.";
 private static final String JUMP_TO_THE_END = "See your scores.";

  private CommonExercise currentExercise;
  private final ControlState controlState;
  private List<CommonShell> allExercises;

  private final Map<String,Boolean> exToCorrect = new HashMap<String, Boolean>();
  private final Map<String,Double>   exToScore = new HashMap<String, Double>();
  //private final Set<String> skipped = new HashSet<String>();
  private final Set<Long> resultIDs = new HashSet<Long>();
  private String selectionID = "";
  private final String instance;
  StickyState sticky;

  /**
   * @see mitll.langtest.client.custom.content.AVPHelper#getFactory
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param feedback
   * @param controller
   * @param exerciseList
   * @param instance
   */
  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface exerciseList, String instance) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
    this.instance = instance;

   // System.out.println("made factory ---------------------\n");
    boolean sharedList = exerciseList == null;
    if (sharedList) {   // when does this happen??
      exerciseList = controller.getExerciseList();
    }

    exerciseList.addListChangedListener(new ListChangeListener<CommonShell>() {
      @Override
      public void listChanged(List<CommonShell> items, String selectionID) {
        MyFlashcardExercisePanelFactory.this.selectionID = selectionID;
        allExercises = items;
        System.out.println("MyFlashcardExercisePanelFactory : " + selectionID + " got new set of items from list. " + items.size());
        reset();
      }
    });
    KeyStorage storage = new KeyStorage(controller) {
      @Override
      protected String getKey(String name) {
        return (selectionID.isEmpty() ? "":selectionID + "_") + super.getKey(name); // in the context of this selection
      }
    };
    sticky = new StickyState(storage);
    controlState.setStorage(storage);
   // System.out.println("setting shuffle --------------------- " +controlState.isShuffle()+ "\n");

    if (!sharedList) {
      exerciseList.simpleSetShuffle(controlState.isShuffle());
    }
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel(mitll.langtest.shared.CommonExercise)
   * @param e
   * @return
   */
  @Override
  public Panel getExercisePanel(CommonExercise e) {
    currentExercise = e;
    sticky.storeCurrent(e);
    return controller.getProps().isNoModel() || !controller.isRecordingEnabled() ? new FlashcardPanel(e,
        MyFlashcardExercisePanelFactory.this.service,
        MyFlashcardExercisePanelFactory.this.controller,
        ADD_KEY_BINDING,
        MyFlashcardExercisePanelFactory.this.controlState,
        soundFeedback,
        soundFeedback.endListener, MyFlashcardExercisePanelFactory.this.instance, exerciseList) {
      @Override
      protected void gotShuffleClick(boolean b) {
        sticky.resetStorage();
        super.gotShuffleClick(b);
      }
    } : new StatsPracticePanel(e,exerciseList);
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
    //skipped.clear();
    latestResultID = -1;
    resultIDs.clear();
  }

  public String getCurrentExerciseID() { return sticky.getCurrentExerciseID(); }

  /**
   * Pull state out of cache and re-populate correct, incorrect, and score history.
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   */
  public void populateCorrectMap() {
    String value = sticky.getCorrect();
    if (value != null && !value.trim().isEmpty()) {
     // System.out.println("using correct map " + value);
      for (String ex : value.split(",")) {
        exToCorrect.put(ex,Boolean.TRUE);
      }
    }

    value = sticky.getIncorrect();
    if (value != null && !value.trim().isEmpty()) {
    //  System.out.println("using incorrect map " + value);
      for (String ex : value.split(",")) {
        exToCorrect.put(ex,Boolean.FALSE);
      }
    }

    value = sticky.getScore();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : value.split(",")) {
        String[] split = pair.split("=");
        if (split.length == 2) {
          exToScore.put(split[0],Double.parseDouble(split[1]));
        }
      }
    }

/*    value = sticky.getSkipped();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : value.split(",")) {
        String trim = pair.trim();
        if (!trim.isEmpty()) {
          skipped.add(trim);
        }
      }
    }*/
  }

  private long latestResultID = -1;
  private final MySoundFeedback soundFeedback = new MySoundFeedback();

  public void resetStorage() {
    sticky.resetStorage();
  }

  /**
   * @see #getExercisePanel(mitll.langtest.shared.CommonExercise)
   */
  private class StatsPracticePanel extends BootstrapExercisePanel {
    String currentExerciseID;
    private Widget container;
    SetCompleteDisplay completeDisplay = new SetCompleteDisplay();
    public StatsPracticePanel(CommonExercise e, ListInterface exerciseListToUse) {
      super(e,
        MyFlashcardExercisePanelFactory.this.service,
        MyFlashcardExercisePanelFactory.this.controller,
        ADD_KEY_BINDING,
        MyFlashcardExercisePanelFactory.this.controlState,
        soundFeedback,
        soundFeedback.endListener, MyFlashcardExercisePanelFactory.this.instance, exerciseListToUse);
    }

    @Override
    protected void recordingStarted() {
      soundFeedback.clear();
      removePlayingHighlight();
    }

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
     * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
     * @param result
     */
    public void receivedAudioAnswer(final AudioAnswer result) {
      //System.out.println("StatsPracticePanel.receivedAudioAnswer: result " + result);

      if (result.getValidity() != AudioAnswer.Validity.OK) {
        super.receivedAudioAnswer(result);
        return;
      }
      resultIDs.add(result.getResultID());
      exToScore.put(currentExercise.getID(), result.getScore());
      exToCorrect.put(currentExercise.getID(), result.isCorrect());

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

      StringBuilder builder3= new StringBuilder();
      for (Map.Entry<String, Double> pair : exToScore.entrySet()) {
          builder3.append(pair.getKey()).append("=").append(pair.getValue()).append(",");
      }
      sticky.storeScore(builder3);

      setStateFeedback();

      latestResultID = result.getResultID();
      //System.out.println("\tStatsPracticePanel.receivedAudioAnswer: latest now " + latestResultID);

      super.receivedAudioAnswer(result);
    }

    /**
     * @see #getSkipToEnd()
     * @see #loadNext()
     * @see #nextAfterDelay(boolean, String)
     */
    private void onSetComplete() {
      //skip.setVisible(false);
      startOver.setVisible(false);
      seeScores.setVisible(false);
      setPrevNextVisible(false);

      sticky.resetStorage();

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

      service.getUserHistoryForList(user, copies, latestResultID, new AsyncCallback<List<AVPHistoryForList>>() {
        @Override
        public void onFailure(Throwable caught) {
          System.err.println("StatsPracticePanel.onSetComplete. : got failure " + caught);
        }

        @Override
        public void onSuccess(List<AVPHistoryForList> result) {
          showFeedbackCharts(result);
        }
      });
      // TODO : maybe add table showing results per word
    }

    private void showFeedbackCharts(List<AVPHistoryForList> result) {
      setMainContentVisible(false);
      container = completeDisplay.showFeedbackCharts(result,exToScore,getCorrect(),getIncorrect(),allExercises.size());
    /*  container = new HorizontalPanel();

      // add left chart and table
      AVPHistoryForList sessionAVPHistoryForList = result.get(0);
      Chart chart = makeCorrectChart(result, sessionAVPHistoryForList);
      container.add(chart);
      container.add(makeTable(sessionAVPHistoryForList, CORRECT_NBSP));

      // add right chart and table
      AVPHistoryForList sessionAVPHistoryForListScore = result.get(1);
      Chart chart2 = makePronChart(getAvgScore(), sessionAVPHistoryForListScore);
      container.add(chart2);
      container.add(makeTable(sessionAVPHistoryForListScore, SCORE));
*/
      belowContentDiv.add(container);
      belowContentDiv.add(getRepeatButton());

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
     * @see MyFlashcardExercisePanelFactory.StatsPracticePanel#showFeedbackCharts
     * @return
     */
    private Button getRepeatButton() {
      final Button w1 = new Button("Go back");
      w1.setIcon(IconType.UNDO);
      w1.getElement().setId("AVP_DoWholeSetFromStart");
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          abortPlayback();
          w1.setVisible(false);
          setMainContentVisible(true);
          belowContentDiv.remove(container);

          startOver();
        }
      });
      controller.register(w1, currentExercise.getID());
      return w1;
    }

    /**
     * Don't count these items in the scoring for this user.
     */
    void startOverAndForgetScores() {
      System.out.println(START_OVER + " : set of ids is "+resultIDs.size());
      service.setAVPSkip(resultIDs,new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {
         // System.out.println("!setAVPSkip success!");
        }
      });
    }

    void startOver() {
      //skip.setVisible(true);
      startOver.setVisible(true);
      seeScores.setVisible(true);
      setPrevNextVisible(true);

      String lastID = allExercises.get(allExercises.size() - 1).getID();
      if (currentExerciseID != null && !currentExerciseID.equals(lastID)) {
        exerciseList.loadExercise(currentExerciseID);
      } else {
        reset();

        sticky.resetStorage();

        String first = allExercises.iterator().next().getID();
        //exerciseList.checkAndAskServer(first);
        exerciseList.loadExercise(first);
      }
    }

    /**
     * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
     * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
     * @param correct
     * @param feedback
     */
    protected void nextAfterDelay(boolean correct, String feedback) {
      if (exerciseList.onLast()) {
        onSetComplete();
      }
      else {
        loadNextOnTimer(/*correct ? 100 :*/ DELAY_MILLIS);
      }
    }

    private Button /*skip,*/ startOver, seeScores;
    private Panel belowContentDiv;

    /**
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel
     * @param toAddTo
     */
    @Override
    protected void addRowBelowPrevNext(DivWidget toAddTo) {
      //toAddTo.add(getSkipItem());
      toAddTo.add(getSkipToEnd());
      toAddTo.add(getStartOver());

      belowContentDiv = toAddTo;
    }


    @Override
    protected void gotClickOnNext() {
      abortPlayback();
      if (exerciseList.onLast()) {
        onSetComplete();
      }
      else {
        exerciseList.loadNext();
      }
    }


    /*private Button getSkipItem() {
      skip = new Button(SKIP_THIS_ITEM);
      skip.getElement().setId("AVP_Skip_Item");
      skip.setType(ButtonType.INFO);
      skip.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          cancelTimer();
          soundFeedback.clear();

          skip.setEnabled(false);

       *//*   StringBuilder builder = new StringBuilder();
          for (String id : skipped) {
            builder.append(id).append(",");
          }

          sticky.storeSkipped(builder);
          skipped.add(currentExercise.getID());*//*
          loadNext();
        }
      });
      new TooltipHelper().addTooltip(skip, "Skip to the next item.");

      controller.register(skip, currentExercise.getID());
      return skip;
    }*/

    private Button getStartOver() {
      startOver = new Button(START_OVER);
      startOver.getElement().setId("AVP_StartOver");

      startOver.setType(ButtonType.PRIMARY);
      startOver.addStyleName("floatRight");
      startOver.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          abortPlayback();
          currentExerciseID = null;
          startOver.setEnabled(false);
          startOverAndForgetScores();

          startOver();
        }
      });
      new TooltipHelper().addTooltip(startOver, "Start over from the beginning.");

      controller.register(startOver, currentExercise.getID());
      return startOver;
    }

    protected void abortPlayback() {
      cancelTimer();
      soundFeedback.clear();
    }

    /**
     * @see #addRowBelowPrevNext(com.github.gwtbootstrap.client.ui.base.DivWidget)
     * @return
     */
    private Button getSkipToEnd() {
      seeScores = new Button(SKIP_TO_END);
      seeScores.getElement().setId("AVP_SkipToEnd");
      controller.register(seeScores, currentExercise.getID());

      seeScores.addStyleName("leftFiveMargin");
      seeScores.setType(ButtonType.PRIMARY);
      seeScores.addStyleName("floatRight");
      seeScores.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          abortPlayback();
          seeScores.setEnabled(false);
          currentExerciseID = currentExercise.getID();
          onSetComplete();
        }
      });
      new TooltipHelper().addTooltip(seeScores, JUMP_TO_THE_END);
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
      int remaining = allExercises.size()
          //- skipped.size()
          - totalCorrect - totalIncorrect;
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
      super(MyFlashcardExercisePanelFactory.this.controller.getSoundManager());
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
