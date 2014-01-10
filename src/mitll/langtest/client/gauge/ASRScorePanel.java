/**
 * 
 */
package mitll.langtest.client.gauge;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import mitll.langtest.client.pretest.PretestGauge;
import mitll.langtest.client.scoring.ScoreListener;
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
  private static final String INSTRUCTIONS = "The ASR method uses a speech recognizer to compare the student " +
    "recording to a model trained with hundreds of native speakers. " +
      "It generates scores for each word and phonetic unit (see the color-coded transcript for details).";
  private static final String LISTENER_INSTRUCTIONS = "Listen to the Native Reference Speaker say the words shown. " +
      "Record yourself saying the words. Your score will be displayed on the gauge in the Scores section." +
      "You may record yourself multiple times." +
   //   "You will see your scores for each recording in the Exercise History section.</p>";
  "";
  public static final int CHART_HEIGHT = 120;
  private static final boolean SHOW_HELP = false;

  private final PretestGauge ASRGauge;
  private Panel phoneList;
  private final List<Float> scores = new ArrayList<Float>();

  private SimplePanel chartPanel;
  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
	public ASRScorePanel(String parent){
    addStyleName("leftFiveMargin");
    addStyleName("floatRight");
    getElement().setId("ASRScorePanel");
    CaptionPanel chartCaptionPanel = new CaptionPanel("Score History");

    chartPanel = new SimplePanel();
    chartCaptionPanel.add(chartPanel);

    add(chartCaptionPanel);

    CaptionPanel captionPanel = new CaptionPanel("Phone Accuracy");

    phoneList = new FlowPanel();
    phoneList.getElement().getStyle().setFloat(Style.Float.LEFT);

    captionPanel.add(phoneList);

    add(captionPanel);

    CaptionPanel gaugeCaptionPanel = new CaptionPanel("Score");
    FlowPanel gaugePanel = new FlowPanel();
		gaugePanel.setHeight("100%");
		gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR_"+parent, "ASR", INSTRUCTIONS);
    gaugePanel.add(ASRGauge);

    gaugeCaptionPanel.add(gaugePanel);
		add(gaugeCaptionPanel);

    CaptionPanel instructionsCaptionPanel = new CaptionPanel("Help");
    FlowPanel instructionsPanel = new FlowPanel();

    Label instructions = new Label(LISTENER_INSTRUCTIONS);
    instructions.addStyleName("leftAlign");
    instructionsPanel.add(instructions);
    instructionsCaptionPanel.add(instructionsPanel);
    if (SHOW_HELP) add(instructionsCaptionPanel);
	}

  //call this after adding the widget to the page
  @Override
  public void onLoad() {
    ASRGauge.createCanvasElement();
    initGauge(ASRGauge);
  }

  private void initGauge(PretestGauge gauge) {
    try {
      gauge.initialize();
    } catch (Exception e) {
      GWT.log("gauge.initialize : of " + gauge + " : Got exception " + e.getMessage());
    }
  }

  private void setASRGaugeValue(float v) { ASRGauge.setValue(v); }

  /**
   *
   * @param score
   * @param showOnlyOneExercise
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#useResult
   */
  public void gotScore(PretestScore score, boolean showOnlyOneExercise) {
    float zeroToHundred = score.getHydecScore() * 100f;
    //System.out.println("ASRScorePanel : " +score + " hydec " + score.getHydecScore() + " " +zeroToHundred);
    setASRGaugeValue(Math.min(100.0f, zeroToHundred));
    updatePhoneAccuracy(score.getPhoneScores());
    scores.add(score.getHydecScore());
    chartPanel.clear();
    chartPanel.add(doChart(showOnlyOneExercise));
  }

/*  private ColumnChart doChartOld() {
    Options options = Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setGridlineColor("white");
    AxisOptions options1 = AxisOptions.create();
    options.setColors("green", "red");

    options1.setMaxValue(1);
    options.setVAxisOptions(options1);
   // options.setTitle("ExerciseHistory");

    //labelAxes(options,"Recording","Score");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Recording");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Score #0");

    data.addRows(scores.size());

    for (int i = 0; i < scores.size(); i++) {
      data.addRow();
      String value = "" + i;
      if (value.length() > 4) value = value.substring(0,4);
      data.setValue(i, 0, value);
      int round = Math.round(scores.get(i) * 100);
      //Float value1 = round;
      data.setValue(i,1, round);
    }

    return new ColumnChart(data,options);
  }*/

  private ColumnChart doChart(boolean showOnlyOneExercise) {
    Options options = Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setGridlineColor(showOnlyOneExercise ? "#efefef" : "white");
    options.setHeight(CHART_HEIGHT);
    String[] colors = new String[scores.size()];
    for (int i = 0; i < scores.size(); i++) {
      colors[i] = getColor(scores.get(i));
    }
    options.setColors(colors);

    AxisOptions options1 = AxisOptions.create();
    options1.setMinValue(0);
    options1.setMaxValue(100);
    options1.setTextPosition("none");
    options.setVAxisOptions(options1);

    AxisOptions options2 = AxisOptions.create();
    options2.setTextPosition("none");
    options.setHAxisOptions(options2);
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Recording");

    for (int i = 0; i < scores.size(); i++) {
      data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Score #"+(i+1));
    }

    data.addRows(1);
    data.setValue(0, 0, ""+1);

    for (int i = 0; i < scores.size(); i++) {
      int round = Math.round(scores.get(i) * 100);
      data.setValue(0,i+1, round);
    }

    if (showOnlyOneExercise) {
      options.setBackgroundColor("#efefef");
    }
    ColumnChart columnChart = new ColumnChart(data, options);

    return columnChart;
  }

/*  public Chart doChart2() {
    final Chart chart = new Chart()
      .setType(Series.Type.COLUMN)
    //  .setChartTitleText("Monthly Average Rainfall")
    //  .setChartSubtitleText("Source: WorldClimate.com")
      .setColumnPlotOptions(new ColumnPlotOptions()
        .setPointPadding(0.2)
        .setBorderWidth(0)
      )
  *//*    .setLegend(new Legend()
        .setLayout(Legend.Layout.VERTICAL)
        .setAlign(Legend.Align.LEFT)
        .setVerticalAlign(Legend.VerticalAlign.TOP)
        .setX(100)
        .setY(70)
        .setFloating(true)
        .setBackgroundColor("#FFFFFF")
        .setShadow(true)
      )*//*
      .setToolTip(new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            return toolTipData.getXAsString() + " : " + toolTipData.getYAsLong() + "%";
          }
        })
      );

    chart.getXAxis()
      .setCategories("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    chart.getYAxis()
      .setAxisTitleText("Rainfall (mm)")
      .setMin(0);

    chart.addSeries(chart.createSeries()
      .setName("Tokyo")
      .setPoints(new Number[] { 49.9, 71.5, 106.4, 129.2, 144.0, 176.0, 135.6, 148.5, 216.4, 194.1, 95.6, 54.4 })
    );
    chart.addSeries(chart.createSeries()
      .setName("New York")
      .setPoints(new Number[] { 83.6, 78.8, 98.5, 93.4, 106.0, 84.5, 105.0, 104.3, 91.2, 83.5, 106.6, 92.3 })
    );
    chart.addSeries(chart.createSeries()
      .setName("London")
      .setPoints(new Number[] { 48.9, 38.8, 39.3, 41.4, 47.0, 48.3, 59.0, 59.6, 52.4, 65.2, 59.3, 51.2 })
    );
    chart.addSeries(chart.createSeries()
      .setName("Berlin")
      .setPoints(new Number[] { 42.4, 33.2, 34.5, 39.7, 52.6, 75.5, 57.4, 60.4, 47.6, 39.1, 46.8, 51.1 })
    );

    return chart;
  }*/

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

      VerticalPanel vp = new VerticalPanel();
      HorizontalPanel hp = new HorizontalPanel();
      vp.add(hp);
      hp.setSpacing(5);
      hp.getElement().getStyle().setFloat(Style.Float.LEFT);

      int c = 0;
      for (String phone : phones) {
        HorizontalPanel p = getPhone(phoneAccuracies, phone);
        hp.add(p);
        if (++c % 5 == 0) {
          hp = new HorizontalPanel();
          hp.setSpacing(5);
          vp.add(hp);
        }
      }

       phoneList.add(vp);
		}
  }

  private HorizontalPanel getPhone(Map<String, Float> phoneAccuracies, String phone) {
    HorizontalPanel p = new HorizontalPanel();

    p.getElement().getStyle().setFloat(Style.Float.LEFT);

    SimplePanel left = new SimplePanel();
    left.setWidth("5px");
    left.setHeight("5px");
    p.add(left);
    HTML child = new HTML(phone);
    Float score = phoneAccuracies.get(phone);
    p.getElement().getStyle().setBackgroundColor(getColor(score));
    float x = score * 100.0f;
    child.setTitle(((int) x ) +"%");
    p.add(child);
    SimplePanel r  = new SimplePanel();
    r.setWidth("5px");
    r.setHeight("5px");

    p.add(r);
    return p;
  }

  private String getColor(float score){
    return getColor2(score);
	}

  /**
   * This gives a smooth range red->yellow->green:
   *  on green 0->255 over score 0->0.5, 255 for > 0.5 and
   *  on red from 255->0 over 0.5->1 in score, 255 for < 0.5
   *
   *  NOTE : this is the same as in audio.image.TranscriptImage
   * @param score
   * @return color in # hex rgb format
   */
  private String getColor2(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f)  score = 0f;
    int red   = (int)Math.max(0,(255f - (Math.max(0, score-0.5)*2f*255f)));
    int green = (int)Math.min(255f, score*2f*255f);
    int blue  = 0;
    // System.out.println("s " +score + " red " + red + " green " + green + " b " +blue);
    return "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
    //return new Color(red, green, blue, BKG_ALPHA);
  }

  /**
   * Does some interpolation, but it's buggy for now.
   * @paramx score
   * @return
   *x @deprecated
   */
/*  private String oldGetColor(float score){
	  if (score > 1.0) {
	    Window.alert("ERROR: getColor: score > 1");
	    return "#000000";
	  }
		float nf = Math.max(score, 0.0f) * (float) (colormap.length - 2);
		int idx = (int) Math.floor(nf);
		int[] color = {0, 0, 0};
		for (int cc = 0; cc < 3; cc++){
			color[cc] = Math.round((colormap[idx + 1][cc] - colormap[idx][cc]) * (nf - (float) idx) + colormap[idx][cc]);
		}

		return "#" + getHexNumber(color[0]) + getHexNumber(color[1]) + getHexNumber(color[2]);
  }*/

	private String getHexNumber(int number){
		String hexString = Integer.toHexString(number).toUpperCase();

		if(hexString.length() == 0){
			return "00";
		}
		else if(hexString.length() == 1){
			return "0" + hexString;
		}
		else{
			return hexString;
		}
	}
}
