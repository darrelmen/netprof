package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.flashcard.MyCustomIconType;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 *
 * Two kinds of record buttons -- simple ones where clicking on the button starts and stop the recording, and there's a little
 * image next to it to give feedback that recording is occurring.
 *
 * The other kinds is the flashcard button, which records while the space bar is held down, (or maybe while the mouse button is held down).
 * It's feedback is the button itself flipping the record images.
 *
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordButton extends Button {
  private static final int PERIOD_MILLIS = 500;
  private static final String RECORD = "Record";
  private static final String STOP = "Stop";

  protected boolean recording = false;
  private Timer recordTimer;
  private final int autoStopDelay;
  private RecordingListener recordingListener;

  public void setRecordingListener(RecordingListener recordingListener) {
    this.recordingListener = recordingListener;
  }

  public static interface RecordingListener {
    void startRecording();
    void flip(boolean first);
    void stopRecording();
  }

  public RecordButton(int delay) {
    super(RECORD);
    this.autoStopDelay = delay;
    setType(ButtonType.PRIMARY);
    setBaseIcon(MyCustomIconType.record);
    setupRecordButton();
    getElement().setId("record_button");
  }

  /**
   *
   *
   * @param delay
   * @param recordingListener

   */
  public RecordButton( int delay, RecordingListener recordingListener) {
    this(delay);
    this.setRecordingListener(recordingListener);
  }

  protected boolean mouseDown = false;
  protected void setupRecordButton() {
    addMouseDownHandler(new MouseDownHandler() {
      @Override
      public void onMouseDown(MouseDownEvent event) {
        if (!mouseDown) {
          mouseDown = true;
          doClick();
        }
      }
    });

    addMouseUpHandler(new MouseUpHandler() {
      @Override
      public void onMouseUp(MouseUpEvent event) {
        mouseDown = false;
        doClick();
      }
    });

    addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        System.out.println("recordButton " + getElement().getId()+
          " got focus");
      }
    });

    addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        System.out.println("recordButton " + getElement().getId()+ " got blur");
        getFocus();
      }
    });
    getFocus();
  }

  private void getFocus() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        setFocus(true);
      }
    });
  }

  /**
   * @seex mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton(mitll.langtest.client.exercise.ExerciseController, RecordButtonPanel, boolean)
   * @seex RecordButton#gotKeyPress(com.google.gwt.user.client.Event.NativePreviewEvent, com.google.gwt.dom.client.NativeEvent)
   * @seex RecordButton#setupRecordButton(com.google.gwt.user.client.ui.Widget)
   */
  protected void doClick() {
    //System.err.println("recordButton " + getElement().getId()+ " doClick");
    if (isVisible() && isEnabled()) {
      startOrStopRecording();
    }
  }

  private void startOrStopRecording() {
    //System.err.println("recordButton " + getElement().getId()+ " startOrStopRecording");
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
    showRecording();
    recordingListener.startRecording();
  }

  private void stop() {
    recording = false;
    showStopped();
    recordingListener.stopRecording();
  }

  protected void showRecording() {
    if (showInitialRecordImage()) {
      flipImage();
    }
  }

  private boolean first = true;
  private Timer t = null;

  private void flipImage() {
    t = new Timer() {
      @Override
      public void run() {
        if (first) {
          showSecondRecordImage();
          recordingListener.flip(true);
        } else {
          showFirstRecordImage();
          recordingListener.flip(false);
        }
        first = !first;
      }
    };
    t.scheduleRepeating(PERIOD_MILLIS);
  }

  protected void showStopped() {
    if (t != null) {
      hideBothRecordImages();
      t.cancel();
    }
  }

  /**
   *
   * @return if we want to flip images
   */
  protected boolean showInitialRecordImage() {
    setBaseIcon(MyCustomIconType.stop);
    return true;
  }

  protected void showFirstRecordImage() {}

  protected void showSecondRecordImage() {}

  protected void hideBothRecordImages() {
    setBaseIcon(MyCustomIconType.record);
  }

  public void initRecordButton() {
    setVisible(true);
    setBaseIcon(MyCustomIconType.record);
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
}
