/**
 * 
 */
package mitll.langtest.client.gauge;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.gchart.client.GChart;
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
  public static final int X_CHART_SIZE = 150;
  private static final String LISTENER_INSTRUCTIONS = "Listen to the Native Reference Speaker say the words shown. " +
      "Record yourself saying the words. Your score will be displayed on the gauge in the Scores section." +
      "You may record yourself multiple times." +
   //   "You will see your scores for each recording in the Exercise History section.</p>";
  "";
  private static final String HOVERTEXT_TEMPLATE = GChart.formatAsHovertext("${y}%");
//  private static final String PHONE_HOVERTEXT_TEMPLATE = GChart.formatAsHovertext("&nbsp;&nbsp;&nbsp;${x}%");

  private final PretestGauge ASRGauge;
  private final GChart exerciseHistoryChart;
  private Panel phoneList;
  //private final GChart phoneAccuracyChart;
  private final List<Float> scores = new ArrayList<Float>();

  private final float[][] colormap = RYB_COLOR_MAP;

  private static final float[][] RYB_COLOR_MAP = {
      {255f, 0f, 0f}, // red
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

    addStyleName("leftFiveMargin");
    setWidth("200px");
    CaptionPanel chartCaptionPanel = new CaptionPanel("Exercise History");

    VerticalPanel chartPanel = new VerticalPanel();
    chartPanel.setSpacing(5);
    chartPanel.setWidth("100%");

		exerciseHistoryChart = new GChart();
		chartPanel.add(exerciseHistoryChart);

    chartCaptionPanel.add(chartPanel);

    add(chartCaptionPanel);

    //phoneAccuracyChart = new GChart();
    CaptionPanel captionPanel = new CaptionPanel("Phone Accuracy");

    phoneList = new FlowPanel();
    phoneList.getElement().getStyle().setFloat(Style.Float.LEFT);

    captionPanel.add(phoneList);

    add(captionPanel);

    CaptionPanel gaugeCaptionPanel = new CaptionPanel("Score");
    FlowPanel gaugePanel = new FlowPanel();
		gaugePanel.setHeight("100%");
		gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR", INSTRUCTIONS);
    gaugePanel.add(ASRGauge);

    gaugeCaptionPanel.add(gaugePanel);
		add(gaugeCaptionPanel);

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

/*    phoneAccuracyChart.setChartSize(X_CHART_SIZE, 150);
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
    phoneAccuracyChart.getYAxis().setTickCount(0);*/

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

     // double i = 10f;

/*
      if (true) {
        //add one empty row for spacing
        phoneAccuracyChart.addCurve();
        phoneAccuracyChart.getCurve().getSymbol().setSymbolType(SymbolType.HBAR_SOUTHWEST);
        phoneAccuracyChart.getCurve().getSymbol().setBorderWidth(0);
        phoneAccuracyChart.getCurve().getSymbol().setBackgroundColor("white");
        phoneAccuracyChart.getCurve().getSymbol().setModelWidth(0);
        phoneAccuracyChart.getCurve().addPoint(0, i--);
      }
*/

     // if (phones.size() > i) phones = phones.subList(phones.size()-(int)i,phones.size());

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

/*      for (String phone : phones) {
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
			}*/
		}

		//phoneAccuracyChart.update();
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
   * @param score
   * @return
   * @deprecated
   */
  private String oldGetColor(float score){
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
