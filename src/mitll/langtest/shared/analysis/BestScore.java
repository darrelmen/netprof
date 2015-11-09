package mitll.langtest.shared.analysis;

import java.util.Date;

/**
 * Created by go22670 on 10/19/15.
 */
public class BestScore implements Comparable<BestScore> {
  private final String id;
  private final long timestamp;
  private final float pronScore;
  private final String fileRef;
  private final String nativeAudio;
  private final int resultID;
  private String json;
  private boolean isiPad;
  private boolean isFlashcard;

  /**
   * @param id
   * @param pronScore
   * @param timestamp
   * @param isFlashcard
   * @param nativeAudio
   * @see mitll.langtest.server.database.analysis.Analysis#getBestForQuery
   */
  public BestScore(String id, float pronScore, long timestamp, int resultID, String json, boolean isiPad,
                   boolean isFlashcard, String fileRef, String nativeAudio) {
    this.id = id;
    this.pronScore = (pronScore < 0) ? 0 : pronScore;
    this.timestamp = timestamp;
    this.resultID = resultID;
    this.json = json;
    this.isiPad = isiPad;
    this.isFlashcard = isFlashcard;
    this.fileRef = fileRef;
    this.nativeAudio = nativeAudio;
  }

  @Override
  public int compareTo(BestScore o) {
    int c = getId().compareTo(o.getId());
    if (c == 0) return -1 * Long.valueOf(getTimestamp()).compareTo(o.getTimestamp());
    else return c;
  }

  public String toString() {
    return "ex " + getId() + "/ res " + getResultID() +
        " : " + new Date(getTimestamp()) + " # " +
        //count +
        " : " + pronScore;
  }

  public float getScore() {
    return pronScore;
  }

  public long getTimestamp() {
    return timestamp;
  }

/*
  public void setCount(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }
*/

/*  public String toCSV() {
//    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
//    String s = df.format(timestamp) + ",";
    //String s = "";
    return getId() + "," + //s +
        timestamp + "," +
        //count + "," +
        pronScore;
  }*/

  public String getId() {
    return id;
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

