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

import mitll.langtest.client.analysis.AnalysisPlot;

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

  private int mean;
  private int stdev;
  private long meanTime;
  private long count = 0;

  private long start;
  private long end;
  private String phone;
  private long sessionStart = -100;

  private static final int SCALE = 1000;

  /**
   * @param phone
   * @param bin
   * @param count
   * @param mean
   * @param stdev
   * @param meanTime
   * @param start
   * @param end
   * @see mitll.langtest.server.database.analysis.PhoneAnalysis#getPhoneSessions
   */
  public PhoneSession(String phone,
                      long bin,
                      long count,
                      double mean,
                      double stdev,
                      long meanTime,
                      long start, long end) {
    this.phone = phone;
    this.sessionStart = bin;
    this.count = count;
    this.mean = toInt(mean);
    this.stdev = toInt(stdev);
    this.meanTime = meanTime;
    this.start = start;
    this.end = end;
  }

  protected int toInt(double value) {
    return (int) (value * SCALE);
  }

  private double fromInt(int value) {
    return ((double) value) / SCALE;
  }

  /**
   * For RPC
   */
  public PhoneSession() {
  }

  /**
   * @see AnalysisPlot#getPeriods
   * @param start
   * @param last
   * @return
   */
  public boolean doesOverlap(long start, long last) {
    return (getEnd() > start && getEnd() <= last)
        || (getStart() > start && getStart() <= last)
        || (getStart() < start && getEnd() > last);
  }

  public double getMean() {
    return fromInt(mean);
  }

  public double getStdev() {
    return fromInt(stdev);
  }

  private long getMeanTime() {
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

  /**
   * sort by time -
   *
   * @param o
   * @return
   */
  @Override
  public int compareTo(PhoneSession o) {
    return Long.compare(meanTime, o.getMeanTime());
  }

  public long getCount() {
    return count;
  }

  public long getStart() {
    return start;
  }
  public long getEnd() {
    return end;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.TimeSeriesPlot#getSessionTime(PhoneSession, PhoneSession)
   */
  public long getMiddle() {
    return (start + end) / 2;
  }

/*  public List<WordAndScore> getExamples() {
    return examples;
  }*/

  public long getSessionStart() {
    return sessionStart;
  }

  public String getPhone() {
    return phone;
  }

  public String toString() {
    return
        "session " + sessionStart+
            "\n\tphone " + phone + " : " +
//            "\n\tat    " + new Date(bin) +
            //          "\n\tstart " + new Date(start) +
            //        "\n\tend   " + new Date(end) +
            "\n\tn     " + count +
            //      "\n\tmean  " + mean +
            //    "\n\tstdev " + stdev +
            "\n\ttime  " + new Date(meanTime)
        //+
        //"\n\texamples  " + examples.size()
        ;
  }
}
