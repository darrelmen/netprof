package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.Validity;


public class StreamResponse {
  private Validity validity;
  private long streamTimestamp;

  public StreamResponse(Validity validity, long streamTimestamp) {
    this.validity = validity;
    this.streamTimestamp = streamTimestamp;
  }

  public Validity getValidity() {
    return validity;
  }

  public long getStreamTimestamp() {
    return streamTimestamp;
  }

  public String toString() {
    return "Resp = " + validity + " at " + streamTimestamp;
  }
}
