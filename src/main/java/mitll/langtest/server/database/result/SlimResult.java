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

package mitll.langtest.server.database.result;

import java.util.Date;

/**
 * Created by go22670 on 4/13/17.
 */
public class SlimResult implements ISlimResult {
  private final boolean valid;
  private final float pronScore;
  private transient String jsonScore;
  private final int exID;
  private final int audioID;
  private final long modified;

  /**
   * @param exID
   * @param audioID
   * @param valid
   * @param jsonScore
   * @param pronScore
   * @param modified
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#fromSlickToSlim
   */
  public SlimResult(int exID, int audioID, boolean valid, String jsonScore, float pronScore, long modified) {
    this.exID = exID;
    this.audioID = audioID;
    this.valid = valid;
    this.jsonScore = jsonScore;
    this.pronScore = pronScore;
    this.modified = modified;
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public int getAudioID() {
    return audioID;
  }

  @Override
  public String getJsonScore() {
    return jsonScore;
  }

  /**
   * @param jsonScore
   * @see mitll.langtest.server.database.refaudio.SlickRefResultDAO#fromSlick
   */
  public void setJsonScore(String jsonScore) {
    this.jsonScore = jsonScore;
  }

  @Override
  public float getPronScore() {
    return pronScore;
  }

  @Override
  public int getExID() {
    return exID;
  }

  @Override
  public long getModified() {
    return modified;
  }

  public String toString() {
    return
        "ex " + exID +
            "\n\taudio     " + audioID +
            "\n\tscore     " + pronScore +
            "\n\tmodified  " + new Date(modified) +
            "\n\tjsonScore " + jsonScore;
  }
}
