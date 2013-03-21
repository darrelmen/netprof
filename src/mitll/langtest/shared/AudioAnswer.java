package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 7/6/12
* Time: 7:01 PM
* To change this template use File | Settings | File Templates.
*/
public class AudioAnswer implements IsSerializable {
  public int reqid;
  public String path;
  public Validity validity;
  public String decodeOutput = "";
  public double score = -1;
  public int durationInMillis;

  public enum Validity implements IsSerializable {
    OK("Audio OK."),
    TOO_SHORT("Audio too short. Record again."),
    TOO_QUIET("Audio too quiet. Check your mic settings or speak louder."),
    INVALID("There was a problem with the audio. Please record again.");
    private String prompt;

    Validity() {} // for gwt serialization
    Validity(String p) {
      prompt = p;
    }
    public String getPrompt() { return prompt; }
  }

  public AudioAnswer() {}

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @param path
   * @param validity
   * @param reqid
   * @param duration
   */
  public AudioAnswer(String path, Validity validity, int reqid, int duration) {
    this.path = path;
    this.validity = validity;
    this.reqid = reqid;
    this.durationInMillis = duration;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @param path
   * @param validity
   * @param decodeOutput
   * @param score
   * @param reqid
   */
  public AudioAnswer(String path, Validity validity, String decodeOutput, double score, int reqid, int duration) {
    this(path, validity, reqid, duration);
    this.decodeOutput = decodeOutput;
    this.score = score;
  }

  public void setDecodeOutput(String decodeOutput) { this.decodeOutput = decodeOutput; }
  public void setScore(double score) { this.score = score; }

  public String toString() { return "Path " + path + " id " +reqid + " validity " + validity; }
}
