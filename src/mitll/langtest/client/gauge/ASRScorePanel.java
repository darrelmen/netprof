/**
 * 
 */
package mitll.langtest.client.gauge;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.AnnotationLocation;
import com.googlecode.gchart.client.GChart.SymbolType;
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
  private static final String INSTRUCTIONS = "The ASR method uses a speech recognizer to compare the student recording to a model trained with hundreds of native speakers. " +
      "It generates scores for each word and phonetic unit (see the color-coded transcript for details).";
  private static final int X_CHART_SIZE = 150;
  private static final String LISTENER_INSTRUCTIONS = "Listen to the Native Reference Speaker say the words shown. " +
      "Record yourself saying the words. Your score will be displayed on the gauge in the Scores section." +
      "You may record yourself multiple times." +
   //   "You will see your scores for each recording in the Exercise History section.</p>";
  "";
  private static final String HOVERTEXT_TEMPLATE = GChart.formatAsHovertext("${y}%");
  private static final String PHONE_HOVERTEXT_TEMPLATE = GChart.formatAsHovertext("&nbsp;&nbsp;&nbsp;${x}%");

  private final PretestGauge ASRGauge;
  private final GChart exerciseHistoryChart;
  private final GChart phoneAccuracyChart;
  private final List<Float> scores = new ArrayList<Float>();

  private final float[][] colormap = RYB_COLOR_MAP;

  private static final float[][] RYB_COLOR_MAP = {{255f, 0f, 0f}, // red
      {255f, 32f, 0f},
      {255f, 64f, 0f},
      {255f, 128f, 0f},
      {255f, 192f, 0f},
      {255f, 255f, 0f}, // yellow
      {192f, 255f, 0f},
      {128f, 255f, 0f},
      {64f, 255f, 0f},
      {32f, 255f, 0f},
      {0f, 255f, 0f}};  // green

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
	public ASRScorePanel(){
    setWidth("200px");
    CaptionPanel chartCaptionPanel = new CaptionPanel("Charts");

    VerticalPanel chartPanel = new VerticalPanel();
    chartPanel.setSpacing(5);
    chartPanel.setWidth("100%");

		exerciseHistoryChart = new GChart();
		chartPanel.add(exerciseHistoryChart);

    phoneAccuracyChart = new GChart();
    HorizontalPanel hp = new HorizontalPanel();
    SimplePanel w = new SimplePanel();
    w.setWidth("10px");
    hp.add(w);
    hp.add(phoneAccuracyChart);
    chartPanel.add(hp);

    chartCaptionPanel.add(chartPanel);

		add(chartCaptionPanel);

    CaptionPanel guageCaptionPanel = new CaptionPanel("Scores");
    FlowPanel gaugePanel = new FlowPanel();
		gaugePanel.setHeight("100%");
		gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR", INSTRUCTIONS);
    gaugePanel.add(ASRGauge);

		guageCaptionPanel.add(gaugePanel);
		add(guageCaptionPanel);

    CaptionPanel instructionsCaptionPanel = new CaptionPanel("Help");
    FlowPanel instructionsPanel = new FlowPanel();
    Label instructions = new Label(LISTENER_INSTRUCTIONS);
    instructions.addStyleName("leftAlign");
    instructionsPanel.add(instructions);
    instructionsCaptionPanel.add(instructionsPanel);
    add(instructionsCaptionPanel);
	}

  @Override
  public void onLoad() {
    initialize();
  }

	//call this after adding the widget to the page
  private void initialize() {
    ASRGauge.createCanvasElement();
    initGauge(ASRGauge);

    exerciseHistoryChart.setChartSize(X_CHART_SIZE, 50);
    exerciseHistoryChart.setChartTitleThickness(20);
    exerciseHistoryChart.setChartTitle("<html>Exercise History</html>");

    exerciseHistoryChart.getXAxis().setTickThickness(0);
    exerciseHistoryChart.getXAxis().setTickLength(0);
    exerciseHistoryChart.getXAxis().setAxisVisible(false);
    exerciseHistoryChart.getXAxis().setTickCount(0);

    exerciseHistoryChart.getYAxis().setTickLabelThickness(0);
    exerciseHistoryChart.getYAxis().setTickLength(0);
    exerciseHistoryChart.getYAxis().setAxisVisible(false);
    exerciseHistoryChart.getYAxis().setTickCount(0);
    exerciseHistoryChart.getYAxis().setAxisMin(0);
    exerciseHistoryChart.getYAxis().setAxisMax(100);

    phoneAccuracyChart.setChartSize(X_CHART_SIZE, 150);
    phoneAccuracyChart.setChartTitleThickness(20);
    phoneAccuracyChart.setChartTitle("<html>Phone Accuracy</html>");

    phoneAccuracyChart.getXAxis().setTickThickness(0);
    phoneAccuracyChart.getXAxis().setTickLength(0);
    phoneAccuracyChart.getXAxis().setAxisVisible(false);
    phoneAccuracyChart.getXAxis().setTickCount(0);
    phoneAccuracyChart.getXAxis().setAxisMin(0);
    phoneAccuracyChart.getXAxis().setAxisMax(100);

    phoneAccuracyChart.getYAxis().setTickLabelThickness(0);
    phoneAccuracyChart.getYAxis().setTickLength(0);
    phoneAccuracyChart.getYAxis().setAxisVisible(false);
    phoneAccuracyChart.getYAxis().setTickCount(0);

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
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#useResult
   * @param score
   */
  public void gotScore(PretestScore score) {
    float zeroToHundred = score.getHydecScore() * 100f;
    //System.out.println("ASRScorePanel : " +score + " hydec " + score.getHydecScore() + " " +zeroToHundred);
    setASRGaugeValue(Math.min(100.0f, zeroToHundred));
    updatePhoneAccuracy(score.getPhoneScores());
    scores.add(score.getHydecScore());
    updateExerciseHistory(scores);
  }

  //TODO: Add a curve on each reload instead of clearing and reloading all curves on each record
  private void updateExerciseHistory(List<Float> scores){
		exerciseHistoryChart.clearCurves();

		if(scores != null){
			float i = 1.0f;			//TODO: Improve spacing, fix view

			int max = scores.size();
			int numScores = 10;

			if(max < 10){
				numScores = max;
			}

			//only show the last ten exercises or less
			for(int index = (max - numScores); index < max; index++){
			  // These saved scores have already been transformed.

				float score = scores.get(index);

				exerciseHistoryChart.addCurve();
				exerciseHistoryChart.getCurve().getSymbol().setSymbolType(SymbolType.VBAR_SOUTHEAST);
				exerciseHistoryChart.getCurve().getSymbol().setBorderWidth(0);
				exerciseHistoryChart.getCurve().getSymbol().setBackgroundColor(getColor(score));
				exerciseHistoryChart.getCurve().getSymbol().setModelWidth(0.3);
				exerciseHistoryChart.setChartSize(X_CHART_SIZE, 50);

				double rounded_score = Math.max(0.01, Math.round(score * 100.0f));
        //System.out.println("using score " + score + "/" + rounded_score);
        int intScore = (int) rounded_score;
        exerciseHistoryChart.getCurve().addPoint(i, intScore);
        exerciseHistoryChart.getCurve().setHovertextTemplate(HOVERTEXT_TEMPLATE);
        i++;
			}

			while(i < 10.0){
				exerciseHistoryChart.addCurve();
				exerciseHistoryChart.getCurve().getSymbol().setSymbolType(SymbolType.VBAR_SOUTHEAST);
				exerciseHistoryChart.getCurve().getSymbol().setBorderWidth(0);
				exerciseHistoryChart.getCurve().getSymbol().setBackgroundColor("white");
				exerciseHistoryChart.getCurve().getSymbol().setModelWidth(0.3);
				exerciseHistoryChart.getCurve().addPoint(i, 0.0f);

				i++;
			}
		}

    try {

      exerciseHistoryChart.update();
    } catch (Exception e) {
      GWT.log("got exception " +e.getMessage());
    }
  }

  /**
   * @see #gotScore
   * @param phoneAccuracies
   */
  private void updatePhoneAccuracy(final Map<String, Float> phoneAccuracies){
		phoneAccuracyChart.clearCurves();

		if(phoneAccuracies != null){
			List<String> phones = new ArrayList<String>(phoneAccuracies.keySet());
			Collections.sort(phones, new Comparator<String>(){
				public int compare(String key1, String key2){
					float value1 = phoneAccuracies.get(key1);
					float value2 = phoneAccuracies.get(key2);

					return ((value1 < value2) ? 1 : -1);
				}
			});

      double i = 10f;

      if (true) {
        //add one empty row for spacing
        phoneAccuracyChart.addCurve();
        phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
        phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
        phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor("white");
        phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0);
        phoneAccuracyChart.getCurve().addPoint(0, i--);
      }

      if (phones.size() > i) phones = phones.subList(phones.size()-(int)i,phones.size());

      for (String phone : phones) {
        phoneAccuracyChart.addCurve();
				phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
				phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
				phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor(getColor(phoneAccuracies.get(phone)));
				phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0.3);
        float x = phoneAccuracies.get(phone) * 100.0f;
        if (x < 1) x = 1;
        phoneAccuracyChart.getCurve().addPoint((int)x, i--);
				phoneAccuracyChart.getCurve().getPoint().setAnnotationText(phone);
				phoneAccuracyChart.getCurve().getPoint().setAnnotationLocation(AnnotationLocation.WEST);
        phoneAccuracyChart.getCurve().getPoint().setAnnotationXShift(-10);
        phoneAccuracyChart.getCurve().setHovertextTemplate(PHONE_HOVERTEXT_TEMPLATE);
        phoneAccuracyChart.getCurve().getSymbol().setHoverLocation(
            AnnotationLocation.EAST);

        if (i < 0) break;
			}

			while(i > 0.0){
				phoneAccuracyChart.addCurve();
				phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
				phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
				phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor("white");
				phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0);
				phoneAccuracyChart.getCurve().addPoint(0, i--);
			}
		}

		phoneAccuracyChart.update();
	}


	private String getColor(float score){
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
	}

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
