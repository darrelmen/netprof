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

package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/16/15.
 */
public class PhoneSession implements Serializable, Comparable<PhoneSession> {
  private static final int TOTAL_THRESHOLD = 200;
  private static final int TOO_MANY_SESSIONS = 15;

  private double mean;
  private double stdev;
  private double meanTime;
  private long count;
  private long bin;
  private long start;
  private long end;
  private String phone;
  private List<WordAndScore> examples;

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
  public PhoneSession(String phone, long bin, long count, double mean, double stdev, double meanTime, long start, long end,
                      List<WordAndScore> examples) {
    this.phone = phone;
    this.bin = bin;
    this.count = count;
    this.mean = mean;
    this.stdev = stdev;
    this.meanTime = meanTime;
    this.start = start;
    this.end = end;
    this.examples = examples;
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

  /**
   * Choose this series if there are more than one sessions and less than 15.
   * And if any of the sessions are bigger than 50 (necessary?)
   * And the total recordings is > 200.
   *
   * @param size
   * @param total
   * @param anyBigger
   * @return
   */
  public static boolean chooseThisSize(int size, int total, boolean anyBigger) {
    return
        size > 1 &&
            size < TOO_MANY_SESSIONS &&
            anyBigger &&
            total > TOTAL_THRESHOLD;
  }

  @Override
  public int compareTo(PhoneSession o) {
    return Double.valueOf(meanTime).compareTo(o.getMeanTime());
  }

  public long getCount() {
    return count;
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

  public List<WordAndScore> getExamples() {
    return examples;
  }
}
