package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class TimeRange implements IsSerializable {
  private long start = System.currentTimeMillis();
  private long end = start + (50L * 365 * 24 * 60 * 60 * 1000);

  public TimeRange() {
  }

  public TimeRange(long start, long end) {
    this.start = start;
    this.end = end;
  }

  public String toString() {
    return "[" +
        new Date(start) +
        "-" +
        new Date(end) +
        "]";
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }
}
