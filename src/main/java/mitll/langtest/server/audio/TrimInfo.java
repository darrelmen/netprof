package mitll.langtest.server.audio;

/**
 * Created by go22670 on 2/23/17.
 */
public class TrimInfo {
  private final double duration;
  private final boolean didTrim;

  TrimInfo() {
    duration = 0;
    didTrim = false;
  }

  TrimInfo(double duration, boolean didTrim) {
    this.duration = duration;
    this.didTrim = didTrim;
  }

  public double getDuration() {
    return duration;
  }

  public boolean didTrim() {
    return didTrim;
  }

  public String toString() {
    return " dur " + duration + " did trim " + didTrim;
  }
}
