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
  public String path;
  public Validity validity;

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
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String)
   * @param path
   * @param validity
   */
  public AudioAnswer(String path, Validity validity) { this.path = path; this.validity = validity; }
  public String toString() { return "Path " + path; }
}
