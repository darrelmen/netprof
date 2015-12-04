/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.analysis;

import com.google.common.collect.MinMaxPriorityQueue;
import mitll.langtest.shared.analysis.WordAndScore;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 12/4/15.
 */
public class PhoneSessionInternal {
  final SummaryStatistics summaryStatistics = new SummaryStatistics();
  final SummaryStatistics summaryStatistics2 = new SummaryStatistics();

  private double mean;
  private double stdev;
  private double meanTime;
  private long count;
  private final long bin;
  private long start;
  private long end = 0;
  private MinMaxPriorityQueue<WordAndScore> queue;

  /**
   * @param bin
   * @see PhoneAnalysis#partition(List, List, Map, Map)
   */
  public PhoneSessionInternal(long bin) {
    this.bin = bin;
    this.queue = MinMaxPriorityQueue
        .maximumSize(10)
        .create();
  }

  public void addValue(float value, long timestamp, WordAndScore wordAndScore) {
    if (summaryStatistics.getN() == 0) {
      start = timestamp;
    }

    summaryStatistics.addValue(value);
    summaryStatistics2.addValue(timestamp);

    if (timestamp > end) end = timestamp;

    if (wordAndScore != null) {
      getQueue().add(wordAndScore);
    }
  }

  public void remember() {
    this.count = summaryStatistics.getN();

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

  public long getCount() {
    return count;
  }

  public long getN() {
    return summaryStatistics.getN();
  }

  public long getBin() {
    return bin;
  }

  public long getEnd() {
    return end;
  }

  public long getStart() {
    return start;
  }

  @Override
  public String toString() {
    return getCount() + " s " + getStart() + " - " + getEnd() + " mean " + getMean();
  }

  public MinMaxPriorityQueue<WordAndScore> getQueue() {
    return queue;
  }
}
