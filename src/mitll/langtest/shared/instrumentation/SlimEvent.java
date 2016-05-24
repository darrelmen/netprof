package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 4/7/16.
 */
public class SlimEvent implements IsSerializable, Comparable<SlimEvent> {
  private int userID;
  private long timestamp;

  public SlimEvent() {}
  public SlimEvent(int userID, long timestamp) {
    this.userID = userID;
    this.timestamp = timestamp;
  }

  @Override
  public int compareTo(SlimEvent o) {
    return timestamp < o.timestamp ? -1 : timestamp > o.timestamp ? +1 : 0;
  }

  public int getUserID() {
    return userID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
