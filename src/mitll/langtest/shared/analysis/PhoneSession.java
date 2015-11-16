package mitll.langtest.shared.analysis;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.io.Serializable;

/**
 * Created by go22670 on 11/16/15.
 */
public class PhoneSession implements Serializable {
  transient SummaryStatistics summaryStatistics = new SummaryStatistics();
  transient SummaryStatistics summaryStatistics2 = new SummaryStatistics();
  private double mean;
  private double stdev;
  private long timestamp;
  private double meanTime;

  public PhoneSession() {}

  public void addValue(float value, long timestamp) {
    summaryStatistics.addValue(value);
    summaryStatistics2.addValue(timestamp);
  }

  public void remember() {
    this.mean = summaryStatistics.getMean();
    this.stdev = summaryStatistics.getStandardDeviation();
    this.meanTime = summaryStatistics2.getMean();
  }

  public double getMean() {
    return mean;
  }

  public double getStdev() {
    return stdev;
  }

  public double getMeanTime() {
    return meanTime;
  }
}
