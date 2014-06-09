package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;

/**
* Created by go22670 on 2/11/14.
*/
public class ControlState {
  private static final String SHOW_STATE = "showState";
  private static final String AUDIO_ON = "audioOn";
  private static final String AUDIO_FEEDBACK_ON = "audioFeedbackOn";
  private static final String SHUFFLE_ON = "shuffleOn";
  public static final String TRUE_VALUE = Boolean.TRUE.toString();
  private static int count = 0;
  private boolean audioOn = true;
  private boolean audioFeedbackOn = true;
  private boolean shuffleOn = false;
  public static final String ENGLISH = "english";
  public static final String FOREIGN = "foreign";
  public static final String BOTH = "both";
  private String showState = ENGLISH; // english/foreign/both

  private KeyStorage storage = null;
  private final int id;

  public ControlState() { id = count++;}
  public boolean showEnglish() { return showState.equals(ENGLISH) || showState.equals(BOTH);}
  public boolean showForeign() { return showState.equals(FOREIGN) || showState.equals(BOTH);}
  public boolean showBoth() { return  showState.equals(BOTH);}

  /**
   * @see BootstrapExercisePanel#BootstrapExercisePanel
   * @param storage
   */
  public void setStorage(KeyStorage storage) {
    this.storage = storage;

    String showState1 = storage.getValue(SHOW_STATE);
    if (showState1.equals(FOREIGN)) showState = FOREIGN;
    else if (showState1.equals(BOTH)) showState = BOTH;

    String audioOnKey = storage.getValue(AUDIO_ON);
    audioOn = audioOnKey.equalsIgnoreCase(TRUE_VALUE);

    String audioFeedbackOnKey = storage.getValue(AUDIO_FEEDBACK_ON);
    audioFeedbackOn = audioFeedbackOnKey.equalsIgnoreCase(TRUE_VALUE);

    String shuffleOnKey = storage.getValue(SHUFFLE_ON);
    shuffleOn = shuffleOnKey.equalsIgnoreCase(TRUE_VALUE);
  }

  public void setAudioOn(boolean audioOn) {
    this.audioOn = audioOn;
    storeValue(AUDIO_ON, audioOn);
  }

  public void setAudioFeedbackOn(boolean audioFeedbackOn) {
    this.audioFeedbackOn = audioFeedbackOn;
    storeValue(AUDIO_FEEDBACK_ON, audioFeedbackOn);
  }

  public void setSuffleOn(boolean shuffleOn) {
    this.shuffleOn = shuffleOn;
    storeValue(SHUFFLE_ON,shuffleOn);
  }

  public void setShowState(String showState) {
    this.showState = showState;
    if (storage != null) storage.storeValue(SHOW_STATE, showState);
  }

  public void storeValue(String slot,boolean shuffleOn) {
    if (storage != null) {
      storage.storeValue(slot, Boolean.toString(shuffleOn));
    }
  }

  public String getShowState() {
    return showState;
  }

  public boolean isAudioOn() {
    return audioOn;
  }
  public boolean isAudioFeedbackOn() { return audioFeedbackOn;  }
  public boolean isShuffle() { return shuffleOn;  }

  public String toString() {
    return "ControlState : id " + id + " audio " + audioOn + " show " + showState + " shuffle " + shuffleOn;
  }
}
