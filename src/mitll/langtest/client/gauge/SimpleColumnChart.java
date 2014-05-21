package mitll.langtest.client.gauge;

import com.google.gwt.user.client.ui.Widget;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.labels.YAxisLabels;

import java.util.List;

/**
 * Created by GO22670 on 5/21/2014.
 */
public class SimpleColumnChart {
  public Widget getChart(boolean showOnlyOneExercise, List<Float> scores,int height) {
    Chart chart = new Chart()
      .setType(Series.Type.COLUMN)
      .setChartTitleText("")
        // .setChartSubtitleText(subtitle)
        // .setMarginRight(10)
      .setOption("/credits/enabled", false)
      .setOption("/plotOptions/series/pointStart", 0)
      .setOption("/legend/enabled", false).setHeight(height);

    if (showOnlyOneExercise) chart.setBackgroundColor("#efefef");

    //  chart.setColors("#058DC7", "#50B432", "#ED561B", "#DDDF00", "#24CBE5", "#64E572", "#FF9655", "#FFF263", "#6AF9C4");

    String[] colors = new String[scores.size()];
    for (int i = 0; i < scores.size(); i++) {
      colors[i] = getColor(scores.get(i));
    }
    chart.setColors(colors);

    // addSeries(scores, chart, "name");
    // Float[] yValues = scores.toArray(new Float[0]);
    //Integer[] yValues = new Integer[scores.size()];
    for (int i = 0; i < scores.size(); i++) {
      int round = Math.round(scores.get(i) * 100);
      if (round == 0) round = 1;
      // yValues[i] = round;
      Integer [] single = new Integer[1];
      single[0] = round;
      Series series = chart.createSeries()
        .setName("Score #" +(i+1))
        .setPoints(single);
      chart.addSeries(series);
    }
/*    Series series = chart.createSeries()
     // .setName("Score")
      .setPoints(yValues);
    chart.addSeries(series);*/

/*    float verticalRange = setPlotBands(numScores, title, subtitle,
      pbCorrect, top, total, avg, chart);*/

    chart.getYAxis().setAxisTitleText("")
      //   .setAllowDecimals(true)
      .setMin(0)
      .setMax(100).setLabels(new YAxisLabels().setEnabled(false)).setGridLineWidth(0);

    chart.getXAxis().setAllowDecimals(false).setLabels(new XAxisLabels().setEnabled(false));//.setTickmarkPlacement(TickmarkPlacement);
    return chart;
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
  public String getColor(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f)  score = 0f;
    int red   = (int)Math.max(0,(255f - (Math.max(0, score-0.5)*2f*255f)));
    int green = (int)Math.min(255f, score*2f*255f);
    int blue  = 0;
    // System.out.println("s " +score + " red " + red + " green " + green + " b " +blue);
    return "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
    //return new Color(red, green, blue, BKG_ALPHA);
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
