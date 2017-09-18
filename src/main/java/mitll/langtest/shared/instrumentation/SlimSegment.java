package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;

import static mitll.langtest.shared.analysis.SimpleTimeAndScore.SCALE;

public class SlimSegment implements IsSerializable {
  private String event;                 /// Text to be displayed per event
  private int score;                  /// posterior score

  public SlimSegment() {}

  public SlimSegment(String event, float score) {
    this.event = event;
    this.score = toInt(score);
  }

  protected int toInt(float value) {
    return (int) (value * SCALE);
  }

  protected float fromInt(int value) {
    return ((float) value) / SCALE;
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
    return fromInt(score);
  }
}
