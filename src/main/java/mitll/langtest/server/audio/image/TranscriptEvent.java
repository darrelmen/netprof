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

package mitll.langtest.server.audio.image;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class TranscriptEvent implements Comparable<TranscriptEvent> {
  private static final NumberFormat format = new DecimalFormat("#.#####");

  public TranscriptEvent(float s, float e, String name) {
    this(s, e, name, 0);
  }

  /**
   * Constructor
   *
   * @param s     start time in seconds
   * @param e     end time in seconds
   * @param name  event name (i.e. phone, word, etc.)
   * @param score
   */
  public TranscriptEvent(float s, float e, String name, float score) {
    start = s;
    end = e;
    event = name;
    this.score = score;
  }

  // Data Members
  private float start;                  /// Start time in seconds
  private float end;                    /// End time in seconds
  private String event;                 /// Text to be displayed per event
  private float score;                  /// posterior score

  @Override
  public int compareTo(TranscriptEvent o) {
    return Float.compare(start, o.start);
  }

  public float getStart() {
    return start;
  }

  public float getEnd() {
    return end;
  }

  public String getEvent() {
    return event;
  }

  public float getScore() {
    return score;
  }

  public String toString() {
    return "[" + format.format(start) + "-" + format.format(end) + "] " + event + "(" + format.format(score) + ")";
  }

  public void setEnd(float end) {
    this.end = end;
  }

  public void setStart(float start) {
    this.start = start;
  }
}