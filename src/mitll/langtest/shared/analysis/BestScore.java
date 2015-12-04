/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.util.Date;

/**
 * Created by go22670 on 10/19/15.
 */
public class BestScore extends SimpleTimeAndScore implements Comparable<BestScore> {
  private String exid;
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
   * @see mitll.langtest.server.database.analysis.Analysis#getUserToResults
   */
  public BestScore(String id, float pronScore, long timestamp, int resultID, String json, boolean isiPad,
                   boolean isFlashcard, String fileRef, String nativeAudio) {
    super(timestamp, (pronScore < 0) ? 0 : pronScore);
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
    int c = getExId().compareTo(o.getExId());
    if (c == 0) return -1 * Long.valueOf(getTimestamp()).compareTo(o.getTimestamp());
    else return c;
  }

  public String toString() {
    return "ex " + getExId() + "/ res " + getResultID() +
        " : " + new Date(getTimestamp()) + " # " +
        //count +
        " : " + getScore();
  }

  public String getExId() {
    return exid;
  }

  public int getResultID() {
    return resultID;
  }

  public String getJson() {
    return json;
  }

  public boolean isiPad() {
    return isiPad;
  }

  public String getFileRef() {
    return fileRef;
  }

  public boolean isFlashcard() {
    return isFlashcard;
  }

  public String getNativeAudio() {
    return nativeAudio;
  }
}

