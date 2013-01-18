package mitll.langtest.client.recorder;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusWidget;

import java.util.Date;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class RecordButton {
  private boolean recording = false;
  private Timer recordTimer;
  private final FocusWidget record;
  private int autoStopDelay;
  private HandlerRegistration keyHandler;
  private boolean hasFocus = false;

  public RecordButton(FocusWidget recordButton, int delay) {
    this.autoStopDelay = delay;
    this.record = recordButton;
    recordButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });
    recordButton.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        //System.out.println(new Date() + " : recordButton GOT   focus !----> ");
        hasFocus = true;
      }
    });
    recordButton.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        hasFocus = false;
       // System.out.println(new Date() + " : recordButton LOST  focus !----> ");
      }
    });
    recordButton.setTitle("Press Return to record/stop recording");

    addKeyHandler();
  }

  private void addKeyHandler() {
    keyHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();

                                                       if (ne.getKeyCode() == KeyCodes.KEY_ENTER &&
                                                           event.getTypeInt() == 512 &&
                                                           "[object KeyboardEvent]".equals(ne.getString()) &&
                                                           !hasFocus) {
                                                         ne.preventDefault();

/*                                                         System.out.println(new Date() + " : Click handler : Got " + event + " type int " +
                                                             event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                             " native " + event.getNativeEvent() + " source " + event.getSource());*/
                                                         new Timer() {
                                                           @Override
                                                           public void run() {
                                                             doClick();
                                                           }
                                                         }.schedule(10);
                                                       }
                                                     }
                                                   });
    //System.out.println("creating handler for recording " + keyHandler);
  }

  public void onUnload() {
    //System.out.println("removing handler for recording " + keyHandler);
    keyHandler.removeHandler();
  }

  public void doClick() {
    if (record.isVisible() && record.isEnabled()) {
      if (recording) {
        cancelTimer();
        stop();
      } else {
        start();
        addRecordingMaxLengthTimeout();
      }
    }
  }

  private void start() {
    recording = true;
    startRecording();
    showRecording();
  }

  private void stop() {
    long now = System.currentTimeMillis();
  //  System.out.println("stop recording at " + now + " " + (now-then));
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
         // long now = System.currentTimeMillis();
        //  System.out.println("stop schedule timer at " + now + " " + (now-then));
          stop();
        }
      }
    };

    // Schedule the timer to run once in 20 seconds.
    //System.out.println("start schedule timer at " + (then = System.currentTimeMillis()));
    recordTimer.schedule(autoStopDelay);
  }
  //private long then = 0;
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
