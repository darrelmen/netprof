/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by go22670 on 11/16/15.
 */
public class PhoneSession implements Serializable, Comparable<PhoneSession> {
  private double mean;
  private double stdev;
  private double meanTime;
  private long count;
  private long bin;
  private long start;
  private long end;
  private String phone;

  /**
   * @see mitll.langtest.server.database.analysis.PhoneAnalysis#getPhoneSessions(String, List, boolean)
   * @param phone
   * @param bin
   * @param count
   * @param mean
   * @param stdev
   * @param meanTime
   * @param start
   * @param end
   */
  public PhoneSession(String phone, long bin, long count, double mean, double stdev, double meanTime, long start, long end) {
    this.phone = phone;
    this.bin = bin;
    this.count = count;
    this.mean = mean;
    this.stdev = stdev;
    this.meanTime = meanTime;
    this.start = start;
    this.end = end;
  }

  public PhoneSession() {
  }

  public boolean doesOverlap(long start, long last) {
    return (getEnd() > start && getEnd() <= last)
        || (getStart() > start && getStart() <= last)
        || (getStart() < start && getEnd() > last);
  }

  public double getMean() {
    return mean;
  }

  public double getStdev() {
    return stdev;
  }

  private double getMeanTime() {
    return meanTime;
  }

  @Override
  public int compareTo(PhoneSession o) {
    return Double.valueOf(meanTime).compareTo(o.getMeanTime());
  }

  public long getCount() {
    return count;
  }

  public long getBin() {
    return bin;
  }


  public String toString() {
    return phone + " : " + new Date(bin) +
        " start " + new Date(start) +
        " end " + new Date(end) +
        " n " + count +
        " mean " + mean + "  stdev " + stdev + " time " + meanTime;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public long getMiddle() {
    return (start + end) / 2;
  }
}
