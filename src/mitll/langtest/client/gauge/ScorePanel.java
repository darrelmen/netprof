/**
 * 
 */
package mitll.langtest.client.gauge;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.AnnotationLocation;
import com.googlecode.gchart.client.GChart.SymbolType;
import mitll.langtest.client.scoring.ScoreListener;
import mitll.langtest.client.pretest.PretestGauge;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author gregbramble
 * TODO : make a separate version for ASR (get rid of all the if (scoreWithASR) stuff)
 */
public class ScorePanel extends FlowPanel implements ScoreListener {
	private PretestGauge DTWGauge;
	private PretestGauge ASRGauge;
	private FlowPanel chartPanel, gaugePanel, instructionsPanel;
	private CaptionPanel guageCaptionPanel, chartCaptionPanel, instructionsCaptionPanel;
	private GChart exerciseHistoryChart,  phoneAccuracyChart;
	private boolean scoreWithASR = false; // MultiRefRepeatExercises don't score with ASR
	float[][] colormap = {{255f, 0f, 0f}, {255f, 32f, 0f}, {255f, 64f, 0f}, {255f, 128f, 0f}, {255f, 192f, 0f}, {255f, 255f, 0f},             
			{192f, 255f, 0f}, {128f, 255f, 0f}, {64f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f},
			{0f, 255f, 0f}, {0f, 255f, 0f}, {0f, 255f, 0f}};

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   * @param scoreWithASR
   */
	public ScorePanel(boolean scoreWithASR){
		this.scoreWithASR = scoreWithASR;

		//setWidth("100%");
    setWidth("200px");
		chartCaptionPanel = new CaptionPanel("Charts");

		chartPanel = new FlowPanel();
		chartPanel.setWidth("100%");
		//chartPanel.setHeight((windowHeight-scorePanelHeight) + "px");

		exerciseHistoryChart = new GChart();
		chartPanel.add(exerciseHistoryChart);
		chartPanel.add(new HTML("<BR>"));
		
		if(scoreWithASR){
			//chartPanel.setHeight((windowHeight-scorePanelHeight) + "px");
			phoneAccuracyChart = new GChart();
			chartPanel.add(phoneAccuracyChart);
			chartPanel.add(new HTML("<BR>"));
		}

		chartCaptionPanel.add(chartPanel);

		add(chartCaptionPanel);

		guageCaptionPanel = new CaptionPanel("Scores");
		gaugePanel = new FlowPanel();
		gaugePanel.setHeight("100%");
		gaugePanel.setWidth("100%");
		if(scoreWithASR){
			ASRGauge = new PretestGauge("ASR", "The ASR method uses a speech recognizer to compare the student recording to an English model trained with hundreds of native speakers. " +
					"It generates scores for each word and phonetic unit (see the color-coded transcript for details).");
			gaugePanel.add(ASRGauge);
		}
		DTWGauge = new PretestGauge("DTW", "The DTW method compares the reference audio (top panel) to the student's recording (bottom panel) to compute an acoustic match. " +
				"This method is language independent.");

		gaugePanel.add(DTWGauge);
		guageCaptionPanel.add(gaugePanel);
		add(guageCaptionPanel);
		if(!scoreWithASR){
			instructionsCaptionPanel = new CaptionPanel("Help");
			instructionsPanel = new FlowPanel();
			HTML instructions = new HTML("<p>Listen to the Native Reference Speaker say the red words shown at the top. " +
			        "Record yourself saying the red words. Your score will be displayed on the gauge in the Scores section.</p>" +
			        "<p>You may record yourself saying the red words multiple times. " +
			        "You will see your scores for each recording in the Exercise History section.</p>");
			instructionsPanel.add(instructions);
			instructionsCaptionPanel.add(instructionsPanel);
			add(instructionsCaptionPanel);
		}
		//addBottom(gaugePanel);

  //  initialize();
	}

  @Override
  public void onLoad() {
    initialize();
  }

	//call this after adding the widget to the page
	public void initialize(){
		if(scoreWithASR){
			ASRGauge.createCanvasElement();
      initGauge(ASRGauge);
    }

		DTWGauge.createCanvasElement();
    initGauge(DTWGauge);

		exerciseHistoryChart.setChartSize(150, 50);
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

		if(scoreWithASR){
			phoneAccuracyChart.setChartSize(150, 150);
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

	}

  private void initGauge(PretestGauge gauge) {
    try {
      gauge.initialize();
    } catch (Exception e) {
      GWT.log("gauge.initialize : of " + gauge + " : Got exception " + e.getMessage());
    }
  }

  private PretestGauge getASRGauge(){
		return ASRGauge;
	}

  private void setASRGaugeValue(float v) { ASRGauge.setValue(v); }

  private PretestGauge getDTWGauge(){
		return DTWGauge;
	}

  private void setDTWGaugeValue(float v) { DTWGauge.setValue(v); }

  List<Float> scores = new ArrayList<Float>();
  public void gotScore(PretestScore score) {
    float transformedSVScore = score.getTransformedSVScore();


    if (transformedSVScore != -1) {
      setDTWGaugeValue(transformedSVScore *100.0f);
      scores.add(transformedSVScore);
    }
    updateExerciseHistory(scores);
    //updateExerciseHistory(score.getHistoricalScores());
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
				exerciseHistoryChart.setChartSize(150, 50);
				
				float rounded_score = Math.round(score * 100.0f); 
        //exerciseHistoryChart.getCurve().addPoint(i, rounded_score);
        exerciseHistoryChart.getCurve().addPoint(i, rounded_score == 0.0f ? 0.001f : rounded_score);
        //exerciseHistoryChart.getCurve().getPoint().setAnnotationText(Integer.toString(rounded_score));
        //exerciseHistoryChart.getCurve().getPoint().setAnnotationLocation(AnnotationLocation.SOUTH);
        //exerciseHistoryChart.getCurve().getPoint().setAnnotationYShift(-5);

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
   * Only for ASR
   * @param phoneAccuracies
   */
  private void updatePhoneAccuracy(final Map<String, Float> phoneAccuracies){
		phoneAccuracyChart.clearCurves();

		if(phoneAccuracies != null){
			float i = 10.0f;			//TODO: Improve spacing, fix view

			ArrayList<String> phones = new ArrayList<String>(phoneAccuracies.keySet());
			Collections.sort(phones, new Comparator<String>(){
				public int compare(String key1, String key2){
					float value1 = phoneAccuracies.get(key1);
					float value2 = phoneAccuracies.get(key2);

					return ((value1 < value2) ? 1 : -1);
				}
			});

			//add one empty row for spacing
			phoneAccuracyChart.addCurve();
			phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
			phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
			phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor("white");
			phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0.3);
			phoneAccuracyChart.getCurve().addPoint(1, i);

			i--;

			//TODO: Remove sil?
			for(String phone: phones){
				phoneAccuracyChart.addCurve();
				phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
				phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
				phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor(getColor(phoneAccuracies.get(phone)));
				phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0.3);
				phoneAccuracyChart.getCurve().addPoint(phoneAccuracies.get(phone) * 100.0f, i);
				phoneAccuracyChart.getCurve().getPoint().setAnnotationText(phone);
				phoneAccuracyChart.getCurve().getPoint().setAnnotationLocation(AnnotationLocation.EAST);
				phoneAccuracyChart.getCurve().getPoint().setAnnotationXShift(5);

				i--;
			}

			while(i > 0.0){
				phoneAccuracyChart.addCurve();
				phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
				phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
				phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor("white");
				phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0.3);
				phoneAccuracyChart.getCurve().addPoint(1, i);

				i--;
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
