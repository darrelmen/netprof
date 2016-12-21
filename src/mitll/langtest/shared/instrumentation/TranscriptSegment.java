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

package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/25/2014.
 */
public class TranscriptSegment implements IsSerializable {
  private float start;                  /// Start time in seconds
  private float end;                    /// End time in seconds
  private String event;                 /// Text to be displayed per event
  private float score;                  /// posterior score

  public TranscriptSegment() {
  }

  /**
   * Constructor
   *
   * @param s     start time in seconds
   * @param e     end time in seconds
   * @param name  event name (i.e. phone, word, etc.)
   * @param score
   */
  public TranscriptSegment(float s, float e, String name, float score) {
    start = s;
    end = e;
    event = name;
    this.score = score;
  }


  public float getStart() {
    return start;
  }

  public float getEnd() {
    return end;
  }

  public int getDuration() {
    return Math.round(end * 1000 - start * 1000);
  }

  /**
   * Event could be a word or a phone, generally.
   *
   * @return
   */
  public String getEvent() {
    return event;
  }

  public float getScore() {
    return score;
  }

  private float roundToHundredth(float totalHours) {
    return ((float) ((Math.round(totalHours * 100d)))) / 100f;
  }

  public String toString() {
    return "[" + roundToHundredth(start) + "-" + roundToHundredth(end) + "] " + event + " (" + roundToHundredth(score) + ")";
  }

  public float getFloatDuration() {
    return getEnd()-getStart();
  }
}
