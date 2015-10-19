package mitll.langtest.client.gauge;

/**
 * Created by GO22670 on 5/21/2014.
 */
public class SimpleColumnChart {
/*  private static final float HALF = 2f;
  public static final float GRAPH_MAX = 100f;*/

  /**
   *
   * @param showOnlyOneExercise
   * @param scores
   * @param height
   * @return
   */
/*  public Widget getChart(boolean showOnlyOneExercise, List<Float> scores, int height) {
    Chart chart = new Chart()
      .setType(Series.Type.COLUMN)
      .setChartTitleText("")
      .setOption("/credits/enabled", false)
      .setOption("/plotOptions/series/pointStart", 0)
      .setOption("/legend/enabled", false).setHeight(height);

    if (showOnlyOneExercise) chart.setBackgroundColor("#efefef");

    String[] colors = new String[scores.size()];
    for (int i = 0; i < scores.size(); i++) {
      colors[i] = getColor(scores.get(i));
    }
    chart.setColors(colors);

    for (int i = 0; i < scores.size(); i++) {
      int round = Math.round(scores.get(i) * 100);
      if (round == 0) round = 1;
      Integer [] single = new Integer[1];
      single[0] = round;
      Series series = chart.createSeries()
        .setName("Score #" +(i+1))
        .setPoints(single);
      chart.addSeries(series);
    }
*//*    chart.getYAxis().setPlotBands(
      getAvgScore(classAvg*100,chart)
    );*//*

    chart.getYAxis().setAxisTitleText("")
      .setMin(0)
      .setMax(100).setLabels(new YAxisLabels().setEnabled(false)).setGridLineWidth(0);

    chart.getXAxis().setAllowDecimals(false).setLabels(new XAxisLabels().setEnabled(false));
    return chart;
  }*/

/*  private <T extends SetScore> PlotBand getAvgScore(float avg, Chart chart) {
    return getPlotBand(avg, chart, "#2031ff", LeaderboardPlot.AVERAGE);
  }*/

/*  private PlotBand getPlotBand(float pbCorrect, Chart chart, String color, String labelText) {
    Range range = getRange(pbCorrect);
    PlotBand personalBest = chart.getYAxis().createPlotBand()
      .setColor(color)
      .setFrom(range.from)
      .setTo(range.to);

    personalBest.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(labelText));
    return personalBest;
  }

  private Range getRange(float pbCorrect) {
    float from = under(pbCorrect);
    float to = over(pbCorrect);
    if (pbCorrect > GRAPH_MAX - HALF) {
      to = GRAPH_MAX;
      from = GRAPH_MAX - 2 * HALF;
    }
    if (pbCorrect < HALF) {
      to = 2 * HALF;
      from = 0;
    }
    return new Range(from, to);
  }

  private float over(float pbCorrect) {
    return pbCorrect + HALF;
  }

  private float under(float pbCorrect) {
    return pbCorrect - HALF;
  }

  private static class Range {
    float from, to;

    public Range(float from, float to) {
      this.from = from;
      this.to = to;
    }
  }*/

  /**
   * This gives a smooth range red->yellow->green:
   *  on green 0->255 over score 0->0.5, 255 for > 0.5 and
   *  on red from 255->0 over 0.5->1 in score, 255 for < 0.5
   *
   *  NOTE : this is the same as in audio.image.TranscriptImage
   * @param score
   * @return color in # hex rgb format
   */
  public static String getColor(float score) {
    if (score > 1.0) score = 1.0f;
    if (score < 0f)  score = 0f;
    int red   = (int)Math.max(0,(255f - (Math.max(0, score-0.5)*2f*255f)));
    int green = (int)Math.min(255f, score*2f*255f);
    int blue  = 0;
    // System.out.println("s " +score + " red " + red + " green " + green + " b " +blue);
    return "#" + getHexNumber(red) + getHexNumber(green) + getHexNumber(blue);
    //return new Color(red, green, blue, BKG_ALPHA);
  }

  private static String getHexNumber(int number){
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
