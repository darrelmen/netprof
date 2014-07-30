package mitll.langtest.client.gauge;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.pretest.PretestGauge;
import mitll.langtest.client.scoring.ScoreListener;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.ScoreAndPath;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * ASR Scoring panel -- shows phonemes.
 * @author gregbramble
 */
public class ASRScorePanel extends FlowPanel implements ScoreListener {
  public static final int NUM_TO_SHOW = 5;
  private static final String INSTRUCTIONS = "Your speech is scored by a speech recognizer trained on speech from many native speakers. " +
      "The recognizer generates scores for each word and phonetic unit (see the color-coded transcript for details).";
  private static final String WARNING = "Repeat the phrase exactly as it is written. " +
    "<b>Saying something different or adding/omitting words could result in incorrect scores.</b>";

  private static final String SOUND_ACCURACY = "Sound Accuracy";
  private static final String SCORE_HISTORY = "Score History";
  private static final String SCORE = "Score";
  private static final int HEIGHT = 18;
  private static final int ROW_LEFT_MARGIN = 18 + 5;
  public static final String PLAY_REFERENCE = "Play Reference";

  private final PretestGauge ASRGauge;
  private final Panel phoneList;
  private final List<ScoreAndPath> scores2 = new ArrayList<ScoreAndPath>();
  private final SimplePanel chartPanel;
  private final SimpleColumnChart chart = new SimpleColumnChart();
  private float classAvg = 0f;
  private String refAudio;
  private ExerciseController controller;
  private String exerciseID;

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
	public ASRScorePanel(String parent, ExerciseController controller, String exerciseID){
    this.controller = controller;
    this.exerciseID = exerciseID;
    addStyleName("leftFiveMargin");
    addStyleName("floatRight");
    getElement().setId("ASRScorePanel");
    CaptionPanel chartCaptionPanel = new CaptionPanel(SCORE_HISTORY);

    chartPanel = new SimplePanel();
    chartCaptionPanel.add(chartPanel);

    add(chartCaptionPanel);
  //  add(new Heading(4,SCORE_HISTORY));
    add(chartPanel);

   CaptionPanel captionPanel = new CaptionPanel(SOUND_ACCURACY);

    phoneList = new FlowPanel();
    phoneList.getElement().getStyle().setFloat(Style.Float.LEFT);

    captionPanel.add(phoneList);
    //add(new Heading(4,SOUND_ACCURACY));
    //add(phoneList);
    add(captionPanel);

    CaptionPanel gaugeCaptionPanel = new CaptionPanel(SCORE);
  //  add(new Heading(4,SCORE));

    Panel gaugePanel = new FlowPanel();
    gaugePanel.setHeight("100%");
    gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR_"+parent, "ASR", INSTRUCTIONS, null);
    gaugePanel.add(ASRGauge);

    gaugeCaptionPanel.add(gaugePanel);
    add(gaugeCaptionPanel);
    //add(gaugePanel);

    Panel instructionsPanel = new FlowPanel();
    add(instructionsPanel);
    HTML child = new HTML(WARNING);
    instructionsPanel.add(child);
    child.addStyleName("wrapword");
    child.addStyleName("maxWidth");
  }

  //call this after adding the widget to the page
  @Override
  public void onLoad() {
   // System.out.println("looking for dom element id " + id + " width " + canvas.getOffsetWidth() );
    ASRGauge.createCanvasElementOld();
    initGauge(ASRGauge);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        addPlayer();
      }
    });
  }

  private void initGauge(PretestGauge gauge) {
    try {
      gauge.initialize();
    } catch (Exception e) {
      System.err.println("gauge.initialize : of " + gauge + " : Got exception " + e.getMessage());
    }
  }

  private void setASRGaugeValue(float v) { ASRGauge.setValue(v); }

  /**
   *
   * @param score
   * @param showOnlyOneExercise
   * @param path
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#useResult
   */
  public void gotScore(PretestScore score, boolean showOnlyOneExercise, String path) {
    float hydecScore = score.getHydecScore();
    float zeroToHundred = hydecScore * 100f;
    setASRGaugeValue(Math.min(100.0f, zeroToHundred));
    updatePhoneAccuracy(score.getPhoneScores());
    addScore(new ScoreAndPath(hydecScore,path));
    chartPanel.clear();
    showChart(showOnlyOneExercise);
    addPlayer();
  }

  private native void addPlayer() /*-{
    $wnd.basicMP3Player.init();
  }-*/;

  @Override
  public void addScore(ScoreAndPath hydecScore) {  scores2.add(hydecScore);  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#setClassAvg(float)
   * @param classAvg
   */
  @Override
  public void setClassAvg(float classAvg) { this.classAvg = classAvg; }

  @Override
  public void setRefAudio(String refAudio) {  this.refAudio = refAudio;  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#showChart()
   * @see mitll.langtest.client.gauge.ASRScorePanel#gotScore(mitll.langtest.shared.scoring.PretestScore, boolean, String)
   * @param showOnlyOneExercise
   */
  @Override
  public void showChart(boolean showOnlyOneExercise) {
    VerticalPanel vp = new VerticalPanel();
    List<ScoreAndPath> scoreAndPaths = scores2;
    AudioTag audioTag = new AudioTag();

    if (scores2.size() > NUM_TO_SHOW) {
      scoreAndPaths = scores2.subList(scores2.size() - NUM_TO_SHOW, scores2.size());
    }

    TooltipHelper tooltipHelper = new TooltipHelper();
    for (ScoreAndPath scoreAndPath : scoreAndPaths) {
      int i = scores2.indexOf(scoreAndPath);
      Panel hp = getAudioAndScore(audioTag, tooltipHelper, scoreAndPath, "Score", "Score #" + (i+1));
      vp.add(hp);
    }

    if (classAvg > 0) {
      vp.add(getClassAverage(tooltipHelper, !scoreAndPaths.isEmpty()));
    }

    if (refAudio != null && !scoreAndPaths.isEmpty()) { // show audio to compare yours against
      Panel hp = getRefAudio(audioTag);

      vp.add(hp);
    }
    chartPanel.add(vp);
  }

  private Panel getRefAudio(AudioTag audioTag) {
    Widget audioWidget = getAudioWidget(new ScoreAndPath(classAvg, refAudio), PLAY_REFERENCE);
    makeChildGreen(audioWidget);

    Panel hp = new HorizontalPanel();
    hp.add(audioWidget);
    HTML child = new HTML("Default Reference");
    child.addStyleName("leftFiveMargin");
    hp.add(child);
    return hp;
  }

  private Panel getClassAverage(TooltipHelper tooltipHelper, boolean addLeftMargin) {
    String prefix = /*classAvg < 0.001f ? "No Class Avg Yet" :*/ "Class Avg";
    ScoreAndPath scoreAndPath = new ScoreAndPath(classAvg, "");
    if (classAvg < 0.001f) {
//      classAvg = 1f;
    }
    else {
      prefix += " " + toPercent(scoreAndPath) +"%";
    }
    //System.out.println("class avg " + classAvg);

    Widget widget = makeRow2(tooltipHelper, scoreAndPath, prefix, Placement.BOTTOM);
    if (addLeftMargin) {
      widget.getElement().getStyle().setMarginLeft(ROW_LEFT_MARGIN, Style.Unit.PX);
    }
    Panel hp2 = new HorizontalPanel();
    hp2.add(widget);
    InlineHTML w1 = new InlineHTML("Class Avg");
    w1.getElement().getStyle().setColor("gray");
    w1.getElement().getStyle().setFontWeight(Style.FontWeight.LIGHTER);
    w1.getElement().getStyle().setProperty("fontSize", "smaller");
    w1.getElement().getStyle().setMarginLeft(3, Style.Unit.PX);
    hp2.add(w1);
    return hp2;
  }

  private Panel getAudioAndScore(AudioTag audioTag, TooltipHelper tooltipHelper, ScoreAndPath scoreAndPath, String prefix, String title) {
    return getAudioAndScore(audioTag, tooltipHelper, scoreAndPath, prefix, Placement.BOTTOM, title, false);
  }

  private Panel getAudioAndScore(AudioTag audioTag, TooltipHelper tooltipHelper, ScoreAndPath scoreAndPath, String prefix,
                                Placement placement, String title, boolean backgroundGreen) {
    Widget w = getAudioWidget(scoreAndPath, title);
    if (backgroundGreen) {
      makeChildGreen(w);
    }

    Widget row = makeRow(tooltipHelper, scoreAndPath, prefix, placement, true);
    row.addStyleName("leftFiveMargin");

    Panel hp = new HorizontalPanel();
    hp.add(w);
    hp.add(row);
    return hp;
  }

  private void makeChildGreen(Widget w) {
    Node child = w.getElement().getChild(0);
    Element as = Element.as(child);
    as.getStyle().setBackgroundColor("green");
  }

  private Anchor getAudioWidget(ScoreAndPath scoreAndPath, String title) {
    return new PlayAudioWidget().getAudioWidgetWithEventRecording(scoreAndPath.getPath(), title, exerciseID, controller);
  }

  private Widget makeRow(TooltipHelper tooltipHelper, ScoreAndPath scoreAndPath, String prefix, Placement right, boolean showPercent) {
    Widget row = new DivWidget();
    int iScore = toPercent(scoreAndPath);
    row.setWidth(Math.max(3, iScore) + "px");
    row.setHeight(HEIGHT + "px");
    row.getElement().getStyle().setBackgroundColor(chart.getColor(scoreAndPath.getScore()));
    row.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
    tooltipHelper.createAddTooltip(row, prefix + (showPercent ? " " + iScore + "%" : ""), right);
    return row;
  }

  private int toPercent(ScoreAndPath scoreAndPath) {
    return (int) (100f * scoreAndPath.getScore());
  }

  private Widget makeRow2(TooltipHelper tooltipHelper, ScoreAndPath scoreAndPath, String prefix, Placement placement) {
    Widget row = new DivWidget();
    int iScore = toPercent(scoreAndPath);
    row.setWidth(iScore + "px");
    row.setHeight(HEIGHT + "px");
    row.getElement().getStyle().setBackgroundColor(chart.getColor(scoreAndPath.getScore()));
    row.getElement().getStyle().setMarginTop(2, Style.Unit.PX);

    tooltipHelper.createAddTooltip(row, prefix, placement);
    return row;
  }

  /**
   * @see mitll.langtest.client.scoring.ScoreListener#gotScore
   * @param phoneAccuracies
   */
  private void updatePhoneAccuracy(final Map<String, Float> phoneAccuracies){
    phoneList.clear();

		if(phoneAccuracies != null){
			List<String> phones = new ArrayList<String>(phoneAccuracies.keySet());
			Collections.sort(phones, new Comparator<String>(){
				public int compare(String key1, String key2){
					float value1 = phoneAccuracies.get(key1);
					float value2 = phoneAccuracies.get(key2);

					return ((value1 < value2) ? 1 : -1);
				}
			});

      Panel vp = new VerticalPanel();
      HorizontalPanel hp = new HorizontalPanel();
      vp.add(hp);
      hp.setSpacing(5);
      hp.getElement().getStyle().setFloat(Style.Float.LEFT);

      int c = 0;
      for (String phone : phones) {
        hp.add(getPhone(phoneAccuracies, phone));
        if (++c % 5 == 0) {
          hp = new HorizontalPanel();
          hp.setSpacing(5);
          vp.add(hp);
        }
      }

       phoneList.add(vp);
		}
  }

  private Panel getPhone(Map<String, Float> phoneAccuracies, String phone) {
    HorizontalPanel p = new HorizontalPanel();

    p.getElement().getStyle().setFloat(Style.Float.LEFT);

    SimplePanel left = new SimplePanel();
    left.setWidth("5px");
    left.setHeight("5px");
    p.add(left);
    HTML child = new HTML(phone);
    Float score = phoneAccuracies.get(phone);
    p.getElement().getStyle().setBackgroundColor(chart.getColor(score));
    float x = score * 100.0f;
    child.setTitle(((int) x ) +"%");
    p.add(child);
    SimplePanel r  = new SimplePanel();
    r.setWidth("5px");
    r.setHeight("5px");

    p.add(r);
    return p;
  }
}
