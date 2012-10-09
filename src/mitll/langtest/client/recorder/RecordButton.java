package mitll.langtest.client.recorder;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class RecordButton {
  private static final int DELAY_MILLIS = 20000;

  private boolean recording = false;
  private Timer recordTimer;
  private final FocusWidget record;

  public RecordButton(FocusWidget recordButton) {
    this.record = recordButton;
    recordButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (recording) {
          //System.err.println("got click -- 1");
          recording = false;
          cancelTimer();
          showStopped();
          stopRecording();
        } else {
          //System.err.println("got click -- 2");

          recording = true;
          startRecording();
          showRecording();

          addRecordingMaxLengthTimeout();
        }
      }
    });
  }

  protected void startRecording() {}
  protected void showRecording() {}
  protected void showStopped() {}

  /**
   * Add a timer to automatically stop recording after 20 seconds.
   *
   *
   * @paramx record_button
   * @paramx controller
   * @paramx service
   * @paramx exercise
   * @paramx index
   * @paramx questionState
   * @paramx outer
   */
  private void addRecordingMaxLengthTimeout() {
    cancelTimer();
    recordTimer = new Timer() {
      @Override
      public void run() {
        if (recording) {
          recording = false;
          stopRecording();
        }
      }
    };

    // Schedule the timer to run once in 20 seconds.
    recordTimer.schedule(DELAY_MILLIS);
  }

  private void cancelTimer() {
    if (recordTimer != null) {
      recordTimer.cancel();
    }
  }
  protected abstract void stopRecording();

  public FocusWidget getRecord() {
    return record;
  }
}
