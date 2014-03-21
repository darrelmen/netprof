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
import mitll.langtest.client.flashcard.LeaderboardPlot;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
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
class MyFlashcardExercisePanelFactory extends ExercisePanelFactory {
  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String REPEAT_THIS_SET = "Start Over";
  private static final String CORRECT_NBSP = "Correct&nbsp;%";
  private static final String SKIP_THIS_ITEM = "Skip this item";

  private static final String RANK = "Rank";
  private static final String NAME = "Name";
  private static final String SCORE = "Score";
  public static final String SCORE_SUBTITLE = "score %";
  public static final String CORRECT_SUBTITLE = "% correct";

  private CommonExercise currentExercise;
  private final ControlState controlState;
  private List<CommonShell> allExercises;
  private int totalExercises = 0;
  private final Map<String,Boolean> exToCorrect = new HashMap<String, Boolean>();
  private final Map<String,Double>   exToScore = new HashMap<String, Double>();

  /**
   * @see NPFHelper#setFactory
   * @param service
   * @param feedback
   * @param controller
   * @param exerciseList
   */
  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
    if (exerciseList == null) exerciseList = controller.getExerciseList();
    exerciseList.addListChangedListener(new ListChangeListener<CommonShell>() {
      @Override
      public void listChanged(List<CommonShell> items) {
        allExercises = items;
        totalExercises = allExercises.size();
//        System.out.println("got new set of items from list." + items.size());
        reset();
      }
    });
  }

  @Override
  public Panel getExercisePanel(CommonExercise e) {
    currentExercise = e;
    return new StatsPracticePanel(e);
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
    totalExercises = allExercises.size();
  }

  private class StatsPracticePanel extends BootstrapExercisePanel {
    private Panel container;
    public StatsPracticePanel(CommonExercise e) {
      super(e, MyFlashcardExercisePanelFactory.this.service,
        MyFlashcardExercisePanelFactory.this.controller, 40, false, MyFlashcardExercisePanelFactory.this.controlState);
    }

    @Override
    protected void loadNext() {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        exerciseList.loadNextExercise(currentExercise.getID());
      }
    }

    private long latestResultID;
    /**
     * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
     * @param result
     */
    public void receivedAudioAnswer(final AudioAnswer result) {
      System.out.println("StatsPracticePanel.receivedAudioAnswer: result " + result);

      if (result.validity != AudioAnswer.Validity.OK) {
        super.receivedAudioAnswer(result);
        return;
      }
      exToScore.put(currentExercise.getID(),   result.getScore());
      exToCorrect.put(currentExercise.getID(), result.isCorrect());
      setStateFeedback();

      latestResultID = result.getResultID();
      super.receivedAudioAnswer(result);
    }

    /**
     * @see #loadNext()
     * @see #nextAfterDelay(boolean, String)
     */
    public void onSetComplete() {
      skip.setVisible(false);
      final int user = controller.getUser();

      Set<String> ids = exToCorrect.keySet();

      System.out.println("StatsPracticePanel.onSetComplete. : calling  getUserHistoryForList for " + user +
        " with " + exToCorrect);
      Set<String> copies = new HashSet<String>(ids);
      if (copies.isEmpty()) {
        for (CommonShell t : allExercises) {
          copies.add(t.getID());
        }
      }

      service.getUserHistoryForList(user, copies, latestResultID, new AsyncCallback<List<AVPHistoryForList>>() {
        @Override
        public void onFailure(Throwable caught) {
          System.out.println("StatsPracticePanel.onSetComplete. : got failure " + caught);
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

/*      for (Session s : result) {
        System.out.println("\tonSetComplete.onSuccess : result " + s);
      }*/

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
    }

    private Chart makeCorrectChart(List<AVPHistoryForList> result, AVPHistoryForList sessionAVPHistoryForList) {
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      int all = totalCorrect + totalIncorrect;
      System.out.println("onSetComplete.onSuccess : results " + result + " " +totalExercises +
        " all " +all + " correct " + totalCorrect + " inc " + totalIncorrect);

      return makeChart(totalCorrect, all, sessionAVPHistoryForList);
    }

    private Chart makePronChart(double avgScore, AVPHistoryForList sessionAVPHistoryForListScore) {
      String pronunciation = "Pronunciation " + toPercent(avgScore);
      Chart chart2 = new LeaderboardPlot().getChart(sessionAVPHistoryForListScore, pronunciation, SCORE_SUBTITLE);
      scaleCharts(chart2);
      return chart2;
    }

    private Chart makeChart(int totalCorrect, int all, AVPHistoryForList sessionAVPHistoryForList) {
      String correct = totalCorrect +" Correct (" + toPercent(totalCorrect, all) + ")";
      Chart chart  = new LeaderboardPlot().getChart(sessionAVPHistoryForList, correct, CORRECT_SUBTITLE);

      scaleCharts(chart);

      return chart;
    }

    private void scaleCharts(Chart chart) {
      float ratio = needToScale();
      if (ratio <1) {
        chart.setWidth(Math.round(400f*ratio));
        chart.setHeight(300);
      }
      else {
        chart.addStyleName("chartDim");
      }
    }

    private float needToScale() {
      int constantTables = 2 * 235;
      float width = (float) Window.getClientWidth()- constantTables;
      return width/(1250- constantTables);
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
      boolean scale =  needToScale() <1;

      List<AVPHistoryForList.UserScore> scores = sessionAVPHistoryForList.getScores();
      int size = scale ? Math.min(5,scores.size()):scores.size();
      for (int i = 0; i < size; i++) {
        HTMLPanel row = new HTMLPanel("tr","");
        if (i % 2 == 0) row.addStyleName("tableAltRowColor");
        HTMLPanel col = new HTMLPanel("td","");

        AVPHistoryForList.UserScore userScore = scores.get(i);
        col.add(new HTML(bold(userScore,""+userScore.getIndex())));
      //    ""+(i + 1))));
        row.add(col);
        col = new HTMLPanel("td","");
        HTML widget = new HTML("<b>"+ userScore.getUser()+"</b>");
        widget.addStyleName(userScore.isCurrent()? "tableRowUserCurrentColor":"tableRowUserColor");
        col.add(widget);

        row.add(col);
        col = new HTMLPanel("td","");

        String html = "" + Math.round(userScore.getScore());
        html = bold(userScore,html);
        col.add(new HTML(html));

        row.add(col);
        table.add(row);
      }
      return table;
    }

    private String bold(AVPHistoryForList.UserScore score,String html) {
      return score.isCurrent() ? "<b>"+html+"</b>" : html;
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
     * @see #showFeedbackCharts
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

    private Button getRepeatButton() {
      Button w1 = new Button(REPEAT_THIS_SET);
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setMainContentVisible(true);
          belowContentDiv.remove(container);
          reset();

          skip.setVisible(true);
          exerciseList.loadExercise(allExercises.iterator().next().getID());
        }
      });
      return w1;
    }

    /**
     * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
     * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
     * @param correct
     * @param feedback
     */
    protected void nextAfterDelay(boolean correct, String feedback) {
      //System.out.println("nextAfterDelay correct " + correct);
      if (exerciseList.onLast()) {
        onSetComplete();
      }
      else {
        //System.out.println("\tnextAfterDelay not on last");

        loadNextOnTimer(DELAY_MILLIS);
      }
    }

    private Button skip;
    private Panel belowContentDiv;

    /**
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel
     * @param toAddTo
     */
    @Override
    protected void addWidgetsBelow(Panel toAddTo) {
      skip = new Button(SKIP_THIS_ITEM);
      skip.setType(ButtonType.INFO);
      skip.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          skip.setEnabled(false);
          totalExercises--;
          cancelTimer();
          loadNext();
        }
      });
      toAddTo.add(skip);
      belowContentDiv = toAddTo;
    }

    private Label remain, incorrectBox, correctBox;

    protected Panel getLeftState() {
      Grid g = new Grid(3, 2);
      ControlGroup remaining = new ControlGroup(REMAINING);
      remaining.addStyleName("topFiveMargin");
      remain = new Label();
      remain.setType(LabelType.INFO);
      g.setWidget(0, 0, remaining);
      g.setWidget(0, 1, remain);

      ControlGroup incorrect = new ControlGroup(INCORRECT);
      incorrect.addStyleName("topFiveMargin");

      incorrectBox = new Label();
      incorrectBox.setType(LabelType.WARNING);

      g.setWidget(1, 0, incorrect);
      g.setWidget(1, 1, incorrectBox);

      ControlGroup correct = new ControlGroup(CORRECT);
      correct.addStyleName("topFiveMargin");

      correctBox = new Label();
      correctBox.setType(LabelType.SUCCESS);

      g.setWidget(2, 0, correct);
      g.setWidget(2, 1, correctBox);

      setStateFeedback();
      g.addStyleName("rightTenMargin");
      return g;
    }

    private void setStateFeedback() {
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      int remaining = totalExercises - totalCorrect - totalIncorrect;
      remain.setText(remaining + "");
      incorrectBox.setText(totalIncorrect + "");
      correctBox.setText(totalCorrect + "");
    }
  }
}
