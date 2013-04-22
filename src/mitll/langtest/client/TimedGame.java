package mitll.langtest.client;

import java.util.ArrayList;
import java.util.List;

public class TimedGame {
  private final LangTest langTest;

  public TimedGame(LangTest langTest) {
    this.langTest = langTest;
  }

  void showTimedGameHelp(PropertyHandler props) {
    List<String> msgs = new ArrayList<String>();
    msgs.add("Practice your vocabulary by saying the matching " + props.getLanguage() + " phrase.");
    String duration;// = "one " + "minute";
    int secs = props.getGameTimeSeconds();
    if (secs < 60) {
      duration = secs + " seconds";
    } else {
      int min = secs / 60;
      boolean even = secs % 60 == 0;
      duration = min + " minute" + (min > 1 ? "s" : "") + (even ? "" : (secs - (min * 60)) + " secs");
    }
    msgs.add("See how many you can get right in " +
      duration +
      "!");
    msgs.add("Press and hold the " + LangTest.RECORDING_KEY + " to record.");
    msgs.add("Release to stop recording.");
    msgs.add("Ready to start the clock?");
    DialogHelper dialogHelper = new DialogHelper(false);
    dialogHelper.showErrorMessage("Beat the clock!", msgs, "Yes!", new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        langTest.gotUser(-1);
      }

      @Override
      public void gotNo() {

      }
    });
  }
}