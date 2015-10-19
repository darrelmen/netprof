package mitll.langtest.shared.analysis;

import java.util.Date;

/**
 * Created by go22670 on 10/19/15.
 */
public class BestScore implements Comparable<BestScore> {
  private String id;
  private long timestamp;
  private float pronScore;
  private int count;

  public BestScore(String id, float pronScore, long timestamp) {
    this.id = id;
    this.pronScore = (pronScore < 0) ? 0 : pronScore;
    this.timestamp = timestamp;
  }

  @Override
  public int compareTo(BestScore o) {
    int c = getId().compareTo(o.getId());
    if (c == 0) return -1 * Long.valueOf(getTimestamp()).compareTo(o.getTimestamp());
    else return c;
  }

  public String toString() {
    return getId() + " : " + new Date(getTimestamp()) + " # " + count + " : " + pronScore;
  }

  public float getScore() {
    return pronScore;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  public String toCSV() {
//    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
//    String s = df.format(timestamp) + ",";
    String s = "";
    return getId() + "," + s + timestamp + "," + count + "," + pronScore;
  }

  public String getId() {
    return id;
  }
}

