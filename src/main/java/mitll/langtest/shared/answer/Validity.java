package mitll.langtest.shared.answer;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @see mitll.langtest.server.audio.AudioCheck.ValidityAndDur
 */
public enum Validity implements IsSerializable {
  OK("Audio OK."),
  TOO_SHORT("Press and hold to record, release to stop recording."),
  MIC_DISCONNECTED("Is your mic disconnected?"),
  TOO_QUIET("Audio too quiet. Check your mic settings or speak closer to the mic."),
  SNR_TOO_LOW("You are either speaking too quietly or the room is too noisy.<br/>Speak louder or closer to the mic or go to a quieter room."),
  TOO_LOUD("Audio too loud. Check your mic settings or speak farther from the mic."),
  INVALID("There was a problem posting the audio. Please record again.");
  private String prompt;

  Validity() {
  } // for gwt serialization

  Validity(String p) {
    prompt = p;
  }

  public String getPrompt() {
    return prompt;
  }
}
