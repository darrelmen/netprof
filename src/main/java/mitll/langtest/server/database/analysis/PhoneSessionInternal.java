/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.analysis;

import com.google.common.collect.MinMaxPriorityQueue;
import mitll.langtest.shared.analysis.WordAndScore;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/4/15.
 */
public class PhoneSessionInternal {
  private final SummaryStatistics summaryStatistics = new SummaryStatistics();
  private final SummaryStatistics summaryStatistics2 = new SummaryStatistics();

  private double mean;
  private double stdev;
  private double meanTime;
  private long count = 0;
  private final long bin;
  private long start;
  private long end = 0;
  private MinMaxPriorityQueue<WordAndScore> queue;

  /**
   * @param bin
   * @see PhoneAnalysis#partition(List, List, Map, Map)
   */
  PhoneSessionInternal(long bin) {
    this.bin = bin;
    this.queue = MinMaxPriorityQueue
        .maximumSize(10)
        .create();
  }

  void addValue(float value, long timestamp, WordAndScore wordAndScore) {
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

  double getMean() {
    return mean;
  }

  double getStdev() {
    return stdev;
  }

  double getMeanTime() {
    return meanTime;
  }

  public long getCount() {
    return count;
  }

  public long getN() {
    return summaryStatistics.getN();
  }

  long getBin() {
    return bin;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  MinMaxPriorityQueue<WordAndScore> getQueue() {
    return queue;
  }

  @Override
  public String toString() {
    return getCount() + " s " + getStart() + " - " + getEnd() + " mean " + getMean();
  }
}
