/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.RootPanel;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Date;

public class  Example  {

  public void onModuleLoad() {
    RootPanel.get().add(createChart());
  }

  public Chart createChart() {

    final Chart chart = new Chart()
        .setType(Series.Type.SPLINE)
        .setChartTitleText("Snow depth in the Vikjafjellet mountain, Norway")
        .setChartSubtitleText("An example of irregular time data in Highcharts JS")
        .setToolTip(new ToolTip()
                .setFormatter(new ToolTipFormatter() {
                  public String format(ToolTipData toolTipData) {
                    return "<b>" + toolTipData.getSeriesName() + "</b><br/>" +
                        DateTimeFormat.getFormat("d. MMMM").format(
                            new Date(toolTipData.getXAsLong())
                        ) + ": " + toolTipData.getYAsDouble() + " m";
                  }
                })
        );

    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME)
        .setDateTimeLabelFormats(new DateTimeLabelFormats()
            .setMonth("%e. %b")
            .setYear("%b")  // don't display the dummy year
        );

    chart.getYAxis()
        .setAxisTitleText("Snow depth (m)")
        .setMin(0);

    chart.addSeries(chart.createSeries()
            .setName("Winter 2007-2008")
            .setPoints(new Number[][]{
                {getTime("1970-10-27"), 0},
                {getTime("1970-11-10"), 0.6},
                {getTime("1970-11-18"), 0.7},
                {getTime("1970-12-2"), 0.8},
                {getTime("1970-12-9"), 0.6},
                {getTime("1970-12-16"), 0.6},
                {getTime("1970-12-28"), 0.67},
                {getTime("1971-1-1"), 0.81},
                {getTime("1971-1-8"), 0.78},
                {getTime("1971-1-12"), 0.98},
                {getTime("1971-1-27"), 1.84},
                {getTime("1971-2-10"), 1.80},
                {getTime("1971-2-18"), 1.80},
                {getTime("1971-2-24"), 1.92},
                {getTime("1971-3-4"), 2.49},
                {getTime("1971-3-11"), 2.79},
                {getTime("1971-3-15"), 2.73},
                {getTime("1971-3-25"), 2.61},
                {getTime("1971-4-2"), 2.76},
                {getTime("1971-4-6"), 2.82},
                {getTime("1971-4-13"), 2.8},
                {getTime("1971-5-3"), 2.1},
                {getTime("1971-5-26"), 1.1},
                {getTime("1971-6-9"), 0.25},
                {getTime("1971-6-12"), 0}
            })
    );

    chart.addSeries(chart.createSeries()
            .setName("Winter 2008-2009")
            .setPoints(new Number[][]{
                {getTime("1970-10-18"), 0},
                {getTime("1970-10-26"), 0.2},
                {getTime("1970-12-1"), 0.47},
                {getTime("1970-12-11"), 0.55},
                {getTime("1970-12-25"), 1.38},
                {getTime("1971-1-8"), 1.38},
                {getTime("1971-1-15"), 1.38},
                {getTime("1971-2-1"), 1.38},
                {getTime("1971-2-8"), 1.48},
                {getTime("1971-2-21"), 1.5},
                {getTime("1971-3-12"), 1.89},
                {getTime("1971-3-25"), 2.0},
                {getTime("1971-4-4"), 1.94},
                {getTime("1971-4-9"), 1.91},
                {getTime("1971-4-13"), 1.75},
                {getTime("1971-4-19"), 1.6},
                {getTime("1971-5-25"), 0.6},
                {getTime("1971-5-31"), 0.35},
                {getTime("1971-6-7"), 0}
            })
    );

    chart.addSeries(chart.createSeries()
            .setName("Winter 2009-2010")
            .setPoints(new Number[][]{
                {getTime("1970-10-9"), 0},
                {getTime("1970-10-14"), 0.15},
                {getTime("1970-11-28"), 0.35},
                {getTime("1970-12-12"), 0.46},
                {getTime("1971-1-1"), 0.59},
                {getTime("1971-1-24"), 0.58},
                {getTime("1971-2-1"), 0.62},
                {getTime("1971-2-7"), 0.65},
                {getTime("1971-2-23"), 0.77},
                {getTime("1971-3-8"), 0.77},
                {getTime("1971-3-14"), 0.79},
                {getTime("1971-3-24"), 0.86},
                {getTime("1971-4-4"), 0.8},
                {getTime("1971-4-18"), 0.94},
                {getTime("1971-4-24"), 0.9},
                {getTime("1971-5-16"), 0.39},
                {getTime("1971-5-21"), 0}
            })
    );

    return chart;
  }

  private long getTime(String date) {
    return dateTimeFormat.parse(date).getTime();
  }

  static final DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat("yyyy-MM-dd");
}
