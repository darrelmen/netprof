package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.Validity;

/**
 *
 */
class StreamResponse {
  private final Validity validity;
  private final long streamTimestamp;

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
