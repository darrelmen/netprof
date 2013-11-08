package mitll.langtest.client.recorder;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class RecordButton {
  private static final int SPACE_CHAR = 32;
  private static final int PERIOD_MILLIS = 500;

  private boolean recording = false;
  private Timer recordTimer;
  private Widget record = null;
  private int autoStopDelay;
  private HandlerRegistration keyHandler;
 // private boolean hasFocus = false;
  private Image recordImage1, recordImage2;

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton(mitll.langtest.client.exercise.ExerciseController, RecordButtonPanel)
   * @param delay
   * @param addKeyHandler
   */
  public RecordButton(int delay, boolean addKeyHandler) {
    this.autoStopDelay = delay;
    if (addKeyHandler) keyHandler = addKeyHandler();
  }

  /**
   * @see RecordButtonPanel#makeRecordButton(mitll.langtest.client.exercise.ExerciseController, RecordButtonPanel)
   * @param recordButton
   * @param delay
   * @param recordImage1
   * @param recordImage2
   * @param addKeyHandler
   */
  public RecordButton(Widget recordButton, int delay, Image recordImage1, Image recordImage2, boolean addKeyHandler) {
    this(delay, addKeyHandler);
    this.record = recordButton;
    setupRecordButton(recordButton);
    this.recordImage1 = recordImage1;
    this.recordImage2 = recordImage2;
  }

  protected void setupRecordButton(Widget recordButton) {
    HasClickHandlers clickable = (HasClickHandlers) recordButton;
/*    HasFocusHandlers focusable = (HasFocusHandlers) recordButton;
    HasBlurHandlers blurable = (HasBlurHandlers) recordButton;*/
    clickable.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });
/*    focusable.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        hasFocus = true;
      }
    });
    blurable.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        hasFocus = false;
      }
    });*/
    if (keyHandler != null) recordButton.setTitle("Press the space bar to record/stop recording");
  }

  protected HandlerRegistration addKeyHandler() {
    return Event.addNativePreviewHandler(new
                                           Event.NativePreviewHandler() {

                                             @Override
                                             public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                               NativeEvent ne = event.getNativeEvent();
                                               if (ne.getKeyCode() == SPACE_CHAR &&
                                                 event.getTypeInt() == 512 &&
                                                 "[object KeyboardEvent]".equals(ne.getString())/* &&
                                                 !hasFocus*/) {
                                                 ne.preventDefault();
                                                 ne.stopPropagation();
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
  }

  public void onUnload() {
   // System.out.println("onUnload : removing handler for recording " + keyHandler);
    if (keyHandler != null) {
      System.out.println("\tremoving handler for recording " + keyHandler);
      keyHandler.removeHandler();
      keyHandler = null;
    }
  }

  public void doClick() {
    HasEnabled enabled = (HasEnabled) record;
    if (record == null || (record.isVisible() && enabled.isEnabled())) {
      startOrStopRecording();
    }
  }

  protected void startOrStopRecording() {
    if (recording) {
      cancelTimer();
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

  protected void showRecording() {
    if (recordImage1 != null) {
      recordImage1.setVisible(true);
      flipImage();
    }
    else {
      System.err.println("\n\n\n-----------> no record image so can't showRecording");// + " " + (now-then));
    }
  }

  private boolean first = true;
  private Timer t = null;

  private void flipImage() {
    t = new Timer() {
      @Override
      public void run() {
        if (first) {
          recordImage1.setVisible(false);
          recordImage2.setVisible(true);
        }
        else {
          recordImage1.setVisible(true);
          recordImage2.setVisible(false);
        }
        first = !first;
      }
    };
    t.scheduleRepeating(PERIOD_MILLIS);
  }

  protected void showStopped() {
    if (recordImage1 != null) {
      recordImage1.setVisible(false);
      recordImage2.setVisible(false);
      t.cancel();
    }
  }

  /**
   * Add a timer to automatically stop recording after 20 seconds.
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
    //System.out.println("start schedule timer at " + (then = System.currentTimeMillis()));
    recordTimer.schedule(autoStopDelay);
  }
  private void cancelTimer() {
    if (recordTimer != null) {
      recordTimer.cancel();
    }
  }
  protected abstract void stopRecording();

  public Widget getRecord() {  return record; }
}
