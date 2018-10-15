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

import java.util.Date;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */
public class BestScore extends SimpleTimeAndScore implements Comparable<BestScore> {
  private int exid;
  private String fileRef;
  private String nativeAudio;
  private int resultID;
  private String json;
  private boolean isiPad;
  private boolean isFlashcard;

  public BestScore() {
  }

  /**
   * @param id
   * @param pronScore
   * @param timestamp
   * @param isFlashcard
   * @param nativeAudio
   * @param numInSession
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getUserToResults
   */
  public BestScore(int id,
                   float pronScore,
                   long timestamp,
                   int resultID,
                   String json,
                   boolean isiPad,
                   boolean isFlashcard,
                   String fileRef,
                   String nativeAudio,
                   long sessionStart,
                   int numInSession) {
    super(timestamp, (pronScore < 0) ? 0 : pronScore, sessionStart, numInSession);
    this.exid = id;
    this.resultID = resultID;
    this.json = json;
    this.isiPad = isiPad;
    this.isFlashcard = isFlashcard;
    this.fileRef = fileRef;
    this.nativeAudio = nativeAudio;
  }

  @Override
  public int compareTo(BestScore o) {
    int c = Integer.compare(getExId(), o.getExId());
    if (c == 0) return -1 * Long.compare(getTimestamp(), o.getTimestamp());
    else return c;
  }

  public int getExId() {
    return exid;
  }

  public int getResultID() {
    return resultID;
  }

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getTranscriptFromJSON
   * @return
   */
  public String getJson() {
    return json;
  }

  public boolean isiPad() {
    return isiPad;
  }

  String getFileRef() {
    return fileRef;
  }

  public boolean isFlashcard() {
    return isFlashcard;
  }

  /**
   * @return
   * @see WordScore#WordScore
   */
  public String getNativeAudio() {
    return nativeAudio;
  }

  public String toString() {
    return "ex " + getExId() + "/ res " + getResultID() +
        " : " + new Date(getTimestamp()) + " # " +
        //childCount +
        " : " + getScore() + " native " + nativeAudio + " ref " + fileRef;
  }
}