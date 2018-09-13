package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.events.SeriesClickEventHandler;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import java.util.*;
import java.util.logging.Logger;

public class BasicTimeSeriesPlot<T extends CommonShell> extends TimeSeriesPlot implements ExerciseLookup<T> {
  private static final int HIDE_DELAY = 5000;
  private final Logger logger = Logger.getLogger("BasicTimeSeriesPlot");

  private static final String SCORE = "Score";

  static final String I_PAD_I_PHONE = "iPad/iPhone";
  static final String VOCAB_PRACTICE = "Vocab Practice";
  protected static final String LEARN = "Last Recording";
  /**
   * @see AnalysisPlot#addChart
   */
  static final String CUMULATIVE_AVERAGE = "Average";

  private final Set<String> toShowExercise =
      new HashSet<>(Arrays.asList(I_PAD_I_PHONE, VOCAB_PRACTICE, LEARN, CUMULATIVE_AVERAGE));

  private static final int X_OFFSET_LEGEND = 65;
  private static final int Y_OFFSET_FOR_LEGEND = 25;

  private final Map<Long, Integer> timeToId = new TreeMap<>();
  private Map<Integer, T> idToEx = new TreeMap<>();

  final ExceptionSupport exceptionSupport;

  BasicTimeSeriesPlot(ExceptionSupport exceptionSupport) {
    this.exceptionSupport = exceptionSupport;
  }

  /**
   * @param title
   * @param addLegend
   * @return
   * @see AnalysisPlot#getChart(String, String, String, UserPerformance)
   */
  Chart getHighchartChart(String title, boolean addLegend) {
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setChartTitleText(title)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)

        .setScatterPlotOptions(new ScatterPlotOptions()
            .setMarker(new Marker()
                .setRadius(5)
                .setHoverState(new Marker()
                    .setEnabled(true)
                    .setLineColor(new Color(100, 100, 100))
                )
            )
            .setHoverStateMarker(new Marker()
                .setEnabled(false)
            ))
        .setToolTip(getToolTip())
        .setSeriesPlotOptions(new SeriesPlotOptions()
            .setSeriesClickEventHandler(getSeriesClickEventHandler())
        );
    if (addLegend) {
      chart.setLegend(new Legend()
          .setLayout(Legend.Layout.VERTICAL)
          .setAlign(Legend.Align.LEFT)
          .setVerticalAlign(Legend.VerticalAlign.TOP)
          .setX(X_OFFSET_LEGEND)
          .setY(Y_OFFSET_FOR_LEGEND)
          .setBorderWidth(1)
          .setFloating(true)
          .setBackgroundColor("#FFFFFF")
      );
    }
    return chart;
  }

  private SeriesClickEventHandler getSeriesClickEventHandler() {
    return clickEvent -> {
      long nearestXAsLong = clickEvent.getNearestXAsLong();
      return gotClickAt(nearestXAsLong);
    };
  }

  protected boolean gotClickAt(long nearestXAsLong) {
    Integer exid = timeToId.get(nearestXAsLong);
    if (exid != null) {
      gotClickOnExercise(exid, nearestXAsLong);
    } else {
      logger.info("getSeriesClickEventHandler no point at " + nearestXAsLong);
    }
    return true;
  }

  protected void gotClickOnExercise(int exid, long nearestXAsLong) {
  }

  /**
   * @return
   * @see #getHighchartChart(String, boolean)
   */
  private ToolTip getToolTip() {
    return new ToolTip()
        .setHideDelay(getHideDelay())
        .setFormatter(toolTipData -> {
          try {
            long xAsLong = toolTipData.getXAsLong();
            Integer exerciseID = timeToId.get(xAsLong);
           // if (exerciseID == null) logger.warning("getToolTip no ex at " + xAsLong);
            T commonShell = getCommonShellAtTime(exerciseID, xAsLong);
            return getTooltip(toolTipData, exerciseID, commonShell);
          } catch (Exception e) {
            logger.warning("getToolTip " + e.getMessage());
            exceptionSupport.logException(e);
            return "";
          }
        });
  }

  protected int getHideDelay() {
    return HIDE_DELAY;
  }

  protected T getCommonShellAtTime(Integer exerciseID, long xAsLong) {
    T commonShell = exerciseID == null ? null : getIdToEx().get(exerciseID);
    if (commonShell == null) {
      logger.info("getCommonShellAtTime no ex found " + exerciseID);
    }
    return commonShell;
  }

  void configureYAxis(Chart chart, String title) {
    chart.getYAxis()
        .setAxisTitleText(title)
        .setMin(0)
        .setMax(100);
  }

  void setYAxisTitle(Chart chart, String title) {
    chart.getYAxis().setAxisTitleText(title);
  }

  /**
   * @see AnalysisPlot#addCumulativeAverage(Collection, Chart, String, boolean)
   * @param chart
   * @param seriesTitle
   * @param data
   * @return
   */
  Series getSplineSeries(Chart chart, String seriesTitle, Number[][] data) {
    return chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.SPLINE);
  }

  Series getScatterSeries(Chart chart, String prefix, Number[][] data) {
    return chart.createSeries()
        .setName(prefix)
        .setPoints(data)
        .setOption("color", "#00B800")
        .setType(Series.Type.SCATTER);
  }

  /**
   * On mouse over, show info about the answer.
   * <p>
   * So this tooltip could either be for a group or an exercise
   *
   * @param toolTipData
   * @param exid
   * @param commonShell
   * @return
   * @see #getToolTip()
   */
  protected String getTooltip(ToolTipData toolTipData, Integer exid, T commonShell) {
 //  logger.info("getTooltip for " + exid + " series " + toolTipData.getSeriesName() + " shell " + commonShell);
    return getExerciseTooltip(toolTipData, commonShell, toolTipData.getSeriesName());
  }

  @NotNull
  private String getExerciseTooltip(ToolTipData toolTipData, T commonShell, String seriesName) {
    boolean showEx = shouldShowExercise(seriesName);

    // logger.info("getExerciseTooltip for " + showEx + " series " + toolTipData.getSeriesName() + " shell " + commonShell);
    String foreignLanguage = commonShell == null ? "" : commonShell.getFLToShow();
    String english = commonShell == null ? "" : commonShell.getEnglish();
    if (english.equalsIgnoreCase(foreignLanguage) &&
        commonShell != null &&
        !commonShell.getMeaning().isEmpty())
      english = commonShell.getMeaning();

    String englishTool = (english == null || english.equals("N/A")) ? "" : "<br/>" + english;
    String dateToShow = getDateToShow(toolTipData);

    return
        (showEx ?
            getFLTooltip(foreignLanguage) +
                englishTool
            : "")
            +
            "<br/>" +
            SCORE +
            " <b>" + toolTipData.getYAsLong() + "</b>%" +

            "<br/>" +

            dateToShow  +

            (showEx ?
                getTooltipHint()
                : "")
        ;
  }

  @NotNull
  String getFLTooltip(String foreignLanguage) {
    return "<span style='font-size:200%'>" + foreignLanguage + "</span>";
  }

  @NotNull
  String getTooltipHint() {    return "<br/><b>Click to hear vs. reference</b>";
  }

  boolean shouldShowExercise(String seriesName) {
    return toShowExercise.contains(seriesName);
  }

  /**
   * @return
   * @seex #getShell
   */
  Map<Integer, T> getIdToEx() {
    return idToEx;
  }

  public void setIdToEx(Map<Integer, T> idToEx) {
    this.idToEx = idToEx;
  }

  public boolean isKnown(int exid) {
    return idToEx.containsKey(exid);
  }

  void addTimeToExID(long time, int id) {
    timeToId.put(time, id);
  }

  /**
   * @param commonShell
   * @see AnalysisPlot#populateExerciseMap
   */
  void rememberExercise(T commonShell) {
    idToEx.put(commonShell.getID(), commonShell);
  }

  public T getShell(int id) {
    return getIdToEx().get(id);
  }
}
