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
  private static final String INSTRUCTIONS = "Your speech is scored by a speech recognizer trained on speech from many native speakers. " +
      "The recognizer generates scores for each word and phonetic unit (see the color-coded transcript for details).";
  private static final String WARNING = "Repeat the phrase exactly as it is written. " +
    "<b>Saying something different or adding/omitting words could result in incorrect scores.</b>";
  private static final int CHART_HEIGHT = 100;

  private final PretestGauge ASRGauge;
  private final Panel phoneList;
  private final List<Float> scores = new ArrayList<Float>();
  private final SimplePanel chartPanel;

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

    Panel gaugePanel = new FlowPanel();
    gaugePanel.setHeight("100%");
    gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR_"+parent, "ASR", INSTRUCTIONS, null);
    gaugePanel.add(ASRGauge);


    gaugeCaptionPanel.add(gaugePanel);
		add(gaugeCaptionPanel);

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
    setASRGaugeValue(Math.min(100.0f, zeroToHundred));
    updatePhoneAccuracy(score.getPhoneScores());
    scores.add(score.getHydecScore());
    chartPanel.clear();
    chartPanel.add(doChart(showOnlyOneExercise));
  }

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
    return new ColumnChart(data, options);
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
