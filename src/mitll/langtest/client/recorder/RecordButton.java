package mitll.langtest.client.recorder;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AttachDetachException;
import com.google.gwt.user.client.ui.FocusWidget;
import lm.K;

import java.util.Date;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class RecordButton {
  private static final int DELAY_MILLIS = 20000; // default

  private boolean recording = false;
  private Timer recordTimer;
  private final FocusWidget record;
  private int autoStopDelay;

  public RecordButton(FocusWidget recordButton) {
    this(recordButton, DELAY_MILLIS);
  }
  public RecordButton(FocusWidget recordButton, int delay) {
    this.autoStopDelay = delay;
    this.record = recordButton;
    recordButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
/*        System.out.println(new Date() + " : RecordButton : Got click " + event + " type int " +
            " assoc " + event.getAssociatedType() +
            " native " + event.getNativeEvent() + " source " + event.getSource());*/
        doClick();
      }
    });
    recordButton.setTitle("Press Return to record/stop recording");

    logHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();

                                                       if (ne.getKeyCode() == KeyCodes.KEY_ENTER &&
                                                           event.getTypeInt() == 512 &&
                                                           "[object KeyboardEvent]".equals(ne.getString())) {
                                                         ne.preventDefault();

                                                         System.out.println(new Date() + " : Click handler : Got " + event + " type int " +
                                                             event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                             " native " + event.getNativeEvent() + " source " + event.getSource());
                                                         doClick();
                                                       }
                                                     }
                                                   });
  }

  private HandlerRegistration logHandler;
  public void onUnload() {
    logHandler.removeHandler();
  }

  private void doClick() {
    if (recording) {
      cancelTimer();
      // TODO : worry about issue where seems to stop recording too early sometimes
/*      new Timer() {
        @Override
        public void run() {
          stop();
        }
      }.schedule(10);*/
      stop();
    } else {
      start();

      addRecordingMaxLengthTimeout();
    }
  }

  private void start() {
    recording = true;
    startRecording();
    showRecording();
  }

  private void stop() {
    recording = false;
    showStopped();
    stopRecording();
  }

  protected abstract void startRecording();
  protected abstract void showRecording();
  protected abstract void showStopped();

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
          stop();
        }
      }
    };

    // Schedule the timer to run once in 20 seconds.
    recordTimer.schedule(autoStopDelay);
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
