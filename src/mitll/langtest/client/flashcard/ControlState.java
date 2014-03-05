package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.Bootstrap;
import mitll.langtest.client.custom.KeyStorage;
import org.apache.xpath.operations.Bool;

/**
* Created by go22670 on 2/11/14.
*/
public class ControlState {
  static int count = 0;
  private boolean audioOn = true;
  private boolean audioFeedbackOn = true;
  public static final String ENGLISH = "english";
  public static final String FOREIGN = "foreign";
  public static final String BOTH = "both";
  private String showState = ENGLISH; // english/foreign/both
 // boolean playStateOn = false;
  private KeyStorage storage = null;
  private int id;

  public ControlState() { id = count++;}
  public boolean showEnglish() { return showState.equals(ENGLISH) || showState.equals(BOTH);}
  public boolean showForeign() { return showState.equals(FOREIGN) || showState.equals(BOTH);}
  public boolean showBoth() { return  showState.equals(BOTH);}

  public void setStorage(KeyStorage storage) {
    this.storage = storage;

    String showState1 = storage.getValue("showState");
    if (showState1.equals(FOREIGN)) showState = FOREIGN;
    else if (showState1.equals(BOTH)) showState = BOTH;

    String audioOnKey = storage.getValue("audioOn");
    String anObject = Boolean.TRUE.toString();
    //System.out.println("audioOn " + audioOnKey + " vs " + anObject);
    audioOn = audioOnKey.equalsIgnoreCase(anObject);

    String audioFeedbackOnKey = storage.getValue("audioFeedbackOn");
    audioFeedbackOn = audioFeedbackOnKey.equalsIgnoreCase(anObject);
  }

  public void setAudioOn(boolean audioOn) {
    this.audioOn = audioOn;
    if (storage != null) storage.storeValue("audioOn", Boolean.toString(audioOn));
  }

  public void setAudioFeedbackOn(boolean audioFeedbackOn) {
    this.audioFeedbackOn = audioFeedbackOn;
    if (storage != null) storage.storeValue("audioFeedbackOn", Boolean.toString(audioFeedbackOn));
  }

  public void setShowState(String showState) {
    this.showState = showState;
    if (storage != null) storage.storeValue("showState", showState);
  }

  public String getShowState() {
    return showState;
  }

  public boolean isAudioOn() {
    return audioOn;
  }

  public boolean isAudioFeedbackOn() {
    return audioFeedbackOn;
  }

  public String toString() {  return "ControlState : id " + id + " audio " + audioOn + " show " + showState;  }
}
