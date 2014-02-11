package mitll.langtest.client.flashcard;

/**
* Created by go22670 on 2/11/14.
*/
public class ControlState {
  static int count = 0;
  boolean audioOn = true;
  boolean audioFeedbackOn = true;
  boolean visualFeedbackOn;
  public static final String ENGLISH = "english";
  public static final String FOREIGN = "foreign";
  public static final String BOTH = "both";
  String showState = ENGLISH; // english/foreign/both
  boolean playStateOn = false;
                      int id;
  public ControlState() { id = count++;}
  public boolean showEnglish() { return showState.equals(ENGLISH) || showState.equals(BOTH);}
  public boolean showForeign() { return showState.equals(FOREIGN) || showState.equals(BOTH);}
  public boolean showBoth() { return  showState.equals(BOTH);}

  public String toString() {
    return "id " + id + " audio " + audioOn + " show " + showState;
  }
}
