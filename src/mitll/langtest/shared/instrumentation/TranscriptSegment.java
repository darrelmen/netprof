package mitll.langtest.shared.instrumentation;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by GO22670 on 3/25/2014.
 */
public class TranscriptSegment implements IsSerializable {
  private float start;                  /// Start time in seconds
  private float end;                    /// End time in seconds
  private String event;                 /// Text to be displayed per event
  private float score;                  /// posterior score

  public TranscriptSegment() {}

  /**
   * Constructor
   *
   * @param s     start time in seconds
   * @param e     end time in seconds
   * @param name  event name (i.e. phone, word, etc.)
   * @param score
   */
  public TranscriptSegment(float s, float e, String name, float score) {
    start = s;
    end = e;
    event = name;
    this.score = score;
  }


  public float getStart() {
    return start;
  }

  public float getEnd() {
    return end;
  }

  /**
   * Event could be a word or a phone, generally.
   * @return
   */
  public String getEvent() {
    return event;
  }

  public float getScore() {
    return score;
  }

  public String toString() {
    return "[" + start + "-" + end + "] " + event + "(" + score + ")";
  }
}
