/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.analysis;

import mitll.langtest.client.common.MessageHelper;
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

public abstract class BasicTimeSeriesPlot<T extends CommonShell> extends TimeSeriesPlot
    implements ExerciseLookup<T> {
  private final Logger logger = Logger.getLogger("BasicTimeSeriesPlot");

  private static final int HIDE_DELAY = 5000;

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

  final ExceptionSupport exceptionSupport;
  final ICommonShellCache<T> commonShellCache;

  private static final boolean WARN_ABOUT_MISSING_EXERCISE_ID = false;

  BasicTimeSeriesPlot(ExceptionSupport exceptionSupport, MessageHelper messageHelper) {
    this.exceptionSupport = exceptionSupport;
    this.commonShellCache = new CommonShellCache<>(messageHelper);
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

  abstract protected void gotClickOnExercise(int exid, long nearestXAsLong);

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
    T commonShell = exerciseID == null ? null : getShell(exerciseID);
    if (commonShell == null && WARN_ABOUT_MISSING_EXERCISE_ID) {
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
   * @param chart
   * @param seriesTitle
   * @param data
   * @return
   * @see AnalysisPlot#addCumulativeAverage(Collection, Chart, String, boolean)
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
    String english = getEnglish(commonShell, foreignLanguage);

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

            dateToShow +

            (showEx ?
                getTooltipHint()
                : "")
        ;
  }

  private String getEnglish(CommonShell commonShell, String foreignLanguage) {
    String english = commonShell == null ? "" : commonShell.getEnglish();
    if (english.equalsIgnoreCase(foreignLanguage) &&
        commonShell != null &&
        !commonShell.getMeaning().isEmpty())
      english = commonShell.getMeaning();
    return english;
  }

  @NotNull
  String getFLTooltip(String foreignLanguage) {
    return "<span style='font-size:200%'>" + foreignLanguage + "</span>";
  }

  @NotNull
  String getTooltipHint() {
    return "<br/><b>Click to hear vs. reference</b>";
  }

  boolean shouldShowExercise(String seriesName) {
    return toShowExercise.contains(seriesName);
  }

  public void setIdToEx(Map<Integer, T> idToEx) {
    commonShellCache.setIdToEx(idToEx);
  }

  public boolean isKnown(int exid) {
    return commonShellCache.isKnown(exid);
  }

  /**
   * @param time
   * @param id
   */
  void addTimeToExID(long time, int id) {
    timeToId.put(time, id);
  }

  public T getShell(int id) {
    return commonShellCache.getShell(id);
  }
}
