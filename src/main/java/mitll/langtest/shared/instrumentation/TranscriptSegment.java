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

package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.jetbrains.annotations.NotNull;

public class TranscriptSegment extends SlimSegment implements IsSerializable, Comparable<TranscriptSegment> {
  private int start;                  // Start time in seconds
  private int end;                    // End time in seconds
  private String displayEvent = "";

  public TranscriptSegment() {
  }

  /**
   * Constructor
   *
   * @param s           start time in seconds
   * @param e           end time in seconds
   * @param name        event name (i.e. phone, word, etc.)
   * @param score
   * @param displayName
   * @seex mitll.langtest.server.scoring.ParseResultJson#getNetPronImageTypeToEndTimes
   */
  public TranscriptSegment(float s, float e, String name, float score, String displayName) {
    super(name, score);
    this.start = toInt(s);
    this.end = toInt(e);
    this.displayEvent = displayName;
  }

  /**
   * @param segment
   */
  public TranscriptSegment(TranscriptSegment segment) {
    this(segment.getStart(), segment.getEnd(), segment.getEvent(), segment.getScore(), segment.getDisplayEvent());
  }

  public SlimSegment toSlim() { return new SlimSegment(getEvent(),getScore()); }

  public float getStart() {
    return fromInt(start);
  }

  public float getEnd() {
    return fromInt(end);
  }

  public boolean contains(double pos) {
    return pos >= getStart() && pos < getEnd();
  }

  public int getDuration() {
    return Math.round(end - start);
  }

  public float getFloatDuration() {
    return getEnd() - getStart();
  }

  @Override
  public int compareTo(@NotNull TranscriptSegment o) {
    return Integer.compare(start, o.start);
  }

  private float roundToHundredth(float totalHours) {
    return ((float) ((Math.round(totalHours * 100d)))) / 100f;
  }

  public String getDisplayEvent() {
    return displayEvent;
  }

  public boolean isIn(TranscriptSegment other) {
    return getStart() >= other.getStart() && getEnd() <= other.getEnd();
  }

  public TranscriptSegment setEvent(String str) {
    this.event = str;
    return this;
  }

  public String toString() {
    return "[" + roundToHundredth(getStart()) + "-" + roundToHundredth(getEnd()) + "] '" +
        getEvent() + "' " + roundToHundredth(getScore()) + "" //+
        //" @ " + index
        ;
  }

}
