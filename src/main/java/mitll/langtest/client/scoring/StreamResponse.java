package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.Validity;

/**
 *
 */
class StreamResponse {
  private final Validity validity;
  private final long streamTimestamp;
  private final boolean streamStop;

  /**
   * @param validity
   * @param streamTimestamp
   * @param streamStop
   * @see JSONAnswerParser#getResponse
   */
  StreamResponse(Validity validity, long streamTimestamp, boolean streamStop) {
    this.validity = validity;
    this.streamTimestamp = streamTimestamp;
    this.streamStop = streamStop;
  }

  /**
   * @see RecordDialogExercisePanel#addWidgets
   * @return
   */
  public Validity getValidity() {
    return validity;
  }

  long getStreamTimestamp() {
    return streamTimestamp;
  }

  public boolean isStreamStop() {
    return streamStop;
  }

  public String toString() {
    return "Resp = " + validity + " at " + streamTimestamp + (streamStop ? " STOP" : "");
  }
}
