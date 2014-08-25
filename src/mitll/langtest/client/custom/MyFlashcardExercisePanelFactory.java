package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.FlashcardPanel;
import mitll.langtest.client.flashcard.LeaderboardPlot;
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
  private static final String CORRECT_NBSP = "Correct&nbsp;%";
  private static final String SKIP_THIS_ITEM = "Skip this item";

  private static final String RANK = "Rank";
  private static final String NAME = "Name";
  private static final String SCORE = "Score";
  private static final String SCORE_SUBTITLE = "score %";
  private static final String CORRECT_SUBTITLE = "% correct";
  private static final int ROWS_IN_TABLE = 7;
  private static final String SKIP_TO_END = "See your scores";
  private static final int TABLE_WIDTH = 2 * 275;
  private static final int HORIZ_SPACE_FOR_CHARTS = (1250 - TABLE_WIDTH);


  private static final boolean ADD_KEY_BINDING = true; // TODO : work on key binding...


  private CommonExercise currentExercise;
  private final ControlState controlState;
  private List<CommonShell> allExercises;

  private final Map<String,Boolean> exToCorrect = new HashMap<String, Boolean>();
  private final Map<String,Double>   exToScore = new HashMap<String, Double>();
  private final Set<String> skipped = new HashSet<String>();
  private final Set<Long> resultIDs = new HashSet<Long>();
  private String selectionID = "";
  private final String instance;
  StickyState sticky;

  /**
   * @see mitll.langtest.client.custom.AVPHelper#getFactory(mitll.langtest.client.list.PagingExerciseList, String, boolean)
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
    } : new StatsPracticePanel(e);
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
    skipped.clear();
    latestResultID = -1;
    resultIDs.clear();
  }

  public String getCurrentExerciseID() { return sticky.getCurrentExerciseID(); }

  /**
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

    value = sticky.getSkipped();
    if (value != null && !value.trim().isEmpty()) {
      for (String pair : value.split(",")) {
        String trim = pair.trim();
        if (!trim.isEmpty()) {
          skipped.add(trim);
        }
      }
    }
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
    private Panel container;
    public StatsPracticePanel(CommonExercise e) {
      super(e,
        MyFlashcardExercisePanelFactory.this.service,
        MyFlashcardExercisePanelFactory.this.controller,
        ADD_KEY_BINDING,
        MyFlashcardExercisePanelFactory.this.controlState,
        soundFeedback,
        soundFeedback.endListener, MyFlashcardExercisePanelFactory.this.instance, exerciseList);
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
     * @see #addWidgetsBelow(com.google.gwt.user.client.ui.Panel)
     * @see #loadNext()
     * @see #nextAfterDelay(boolean, String)
     */
    public void onSetComplete() {
      skip.setVisible(false);
      startOver.setVisible(false);
      seeScores.setVisible(false);

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
      container = new HorizontalPanel();

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

      belowContentDiv.add(container);
      belowContentDiv.add(getRepeatButton());

      sticky.resetStorage();
    }

    private Chart makeCorrectChart(List<AVPHistoryForList> result, AVPHistoryForList sessionAVPHistoryForList) {
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      int all = totalCorrect + totalIncorrect;
      System.out.println("onSetComplete.onSuccess : results " + result + " " +(allExercises.size()-skipped.size()) +
        " all " +all + " correct " + totalCorrect + " inc " + totalIncorrect);

      return makeChart(totalCorrect, all, sessionAVPHistoryForList);
    }

    private Chart makePronChart(double avgScore, AVPHistoryForList sessionAVPHistoryForListScore) {
      String pronunciation = "Pronunciation " + toPercent(avgScore);
      Chart chart2 = new LeaderboardPlot().getChart(sessionAVPHistoryForListScore, pronunciation, SCORE_SUBTITLE);
      scaleCharts(chart2);
      return chart2;
    }

    /**
     * @see #makeCorrectChart(java.util.List, mitll.langtest.shared.flashcard.AVPHistoryForList)
     * @param totalCorrect
     * @param all
     * @param sessionAVPHistoryForList
     * @return
     */
    private Chart makeChart(int totalCorrect, int all, AVPHistoryForList sessionAVPHistoryForList) {
      String suffix = getSkippedSuffix();
      String correct = totalCorrect +" of " + all+ " Correct (" + toPercent(totalCorrect, all) + ")" + suffix;
      Chart chart  = new LeaderboardPlot().getChart(sessionAVPHistoryForList, correct, CORRECT_SUBTITLE);

      scaleCharts(chart);

      return chart;
    }

    private String getSkippedSuffix() {
      int attempted = getCorrect()+getIncorrect();
      int skipped =  allExercises.size()-attempted;
      String suffix = "";
      if (skipped > 0 && attempted > 0) {
        suffix += ", "+skipped + " skipped";
      }
      return suffix;
    }

    private void scaleCharts(Chart chart) {
      float yRatio = needToScaleY();

      boolean neither = true;

      int chartWidth = (Window.getClientWidth()-TABLE_WIDTH)/2 -10;
      chart.setWidth(chartWidth);

      if (yRatio < 1) {
        chart.setHeight(Math.min(400f, Math.round(400f * yRatio)));
        neither = false;
      }

      if (neither) chart.addStyleName("chartDim");
    }

    private float needToScaleX() {
      float width = (float) Window.getClientWidth()- TABLE_WIDTH;
      return width/ HORIZ_SPACE_FOR_CHARTS;
    }

    private float needToScaleY() {
      float height = (float) Window.getClientHeight();
      return height/707;
    }

    /**
     * Make a three column table -- rank, name, and score
     * @param sessionAVPHistoryForList
     * @param scoreColHeader
     * @return
     */
    private Table makeTable(AVPHistoryForList sessionAVPHistoryForList, String scoreColHeader) {
      Table table = new Table();
      table.getElement().setId("LeaderboardTable");
      TableHeader w = new TableHeader(RANK);
      table.add(w);
      table.add(new TableHeader(NAME));
      table.add(new TableHeader(scoreColHeader));
      boolean scale =  needToScaleX() <1;

      List<AVPHistoryForList.UserScore> scores = sessionAVPHistoryForList.getScores();
      int size = scale ? Math.min(ROWS_IN_TABLE, scores.size()) : scores.size();

/*      if (scale) System.out.println("scale! client " +Window.getClientWidth()+
        " : using " + size + " vs " + scores.size());*/

      int used = 0;
      for (int i = 0; i < scores.size(); i++) {
        AVPHistoryForList.UserScore userScore = scores.get(i);
        if (used++ < size || userScore.isCurrent()) {
          HTMLPanel row = new HTMLPanel("tr", "");
          if (i % 2 == 0) row.addStyleName("tableAltRowColor");

          // add index col
          HTMLPanel col = new HTMLPanel("td", "");
          col.add(new HTML(bold(userScore, "" + userScore.getIndex())));
          row.add(col);

          // add user name col
          col = new HTMLPanel("td", "");
          HTML widget = new HTML("<b>" + userScore.getUser() + "</b>");
          widget.addStyleName(userScore.isCurrent() ? "tableRowUserCurrentColor" : "tableRowUserColor");
          col.add(widget);
          row.add(col);

          // add score
          col = new HTMLPanel("td", "");
          String html = "" + Math.round(userScore.getScore());
          html = bold(userScore, html);
          col.add(new HTML(html));
          row.add(col);

          table.add(row);
        }
      }
      return table;
    }

    private String bold(AVPHistoryForList.UserScore score, String html) {
      return score.isCurrent() ? "<b>" + html + "</b>" : html;
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
     * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#showFeedbackCharts
     * @return
     */
    private double getAvgScore() {
      double count = 0;
      float num = 0f;
      for (Double val : exToScore.values()) {
        if (val > 0) {
          count += val;
          num++;
        }
      }
      return count/num;
    }

    private String toPercent(int numer, int denom) {
      return ((int) ((((float)numer) * 100f) / denom)) + "%";
    }
    private String toPercent(double num) {
      return ((int) (num * 100f)) + "%";
    }

    /**
     * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#showFeedbackCharts
     * @return
     */
    private Button getRepeatButton() {
      final Button w1 = new Button(START_OVER);
      w1.getElement().setId("AVP_DoWholeSetFromStart");
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          cancelTimer();
          soundFeedback.clear();
          w1.setVisible(false);
          setMainContentVisible(true);
          belowContentDiv.remove(container);

          startOver();
        }
      });
      controller.register(w1, currentExercise.getID());
      return w1;
    }

    void startOverAndForgetScores() {
      System.out.println(START_OVER + " : set of ids is "+resultIDs.size());
      service.setAVPSkip(resultIDs,new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {
          /*System.out.println("!setAVPSkip success!");*/
        }
      });
    }

    void startOver() {
      reset();

      sticky.resetStorage();

      skip.setVisible(true);
      startOver.setVisible(true);
      seeScores.setVisible(true);
      String first = allExercises.iterator().next().getID();
      //exerciseList.checkAndAskServer(first);
      exerciseList.loadExercise(first);
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

    private Button skip, startOver, seeScores;
    private Panel belowContentDiv;

    /**
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel
     * @param toAddTo
     */
    @Override
    protected void addWidgetsBelow(Panel toAddTo) {
      toAddTo.add(getSkipItem());
      toAddTo.add(getSkipToEnd());
      toAddTo.add(getStartOver());

      belowContentDiv = toAddTo;
    }

    private Button getSkipItem() {
      skip = new Button(SKIP_THIS_ITEM);
      skip.getElement().setId("AVP_Skip_Item");
      skip.setType(ButtonType.INFO);
      skip.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          cancelTimer();
          soundFeedback.clear();

          skip.setEnabled(false);

          StringBuilder builder = new StringBuilder();
          for (String id : skipped) {
            builder.append(id).append(",");
          }

          sticky.storeSkipped(builder);
          skipped.add(currentExercise.getID());
          loadNext();
        }
      });
      new TooltipHelper().addTooltip(skip, "Skip to the next item.");

      controller.register(skip, currentExercise.getID());
      return skip;
    }

    private Button getStartOver() {
      startOver = new Button(START_OVER);
      startOver.getElement().setId("AVP_StartOver");

      startOver.setType(ButtonType.PRIMARY);
      startOver.addStyleName("floatRight");
      startOver.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          cancelTimer();
          soundFeedback.clear();

          startOver.setEnabled(false);
          startOverAndForgetScores();

          startOver();
        }
      });
      new TooltipHelper().addTooltip(startOver, "Start over from the beginning.");

      controller.register(startOver, currentExercise.getID());
      return startOver;
    }

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
          cancelTimer();
          seeScores.setEnabled(false);

          onSetComplete();
        }
      });
      new TooltipHelper().addTooltip(seeScores, "Jump to end and see your scores.");
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
      int remaining = allExercises.size() - skipped.size() - totalCorrect - totalIncorrect;
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
