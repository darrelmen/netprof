package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.StyleHelper;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Timer;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * <p/>
 * Two kinds of record buttons -- simple ones where clicking on the button starts and stop the recording, and there's a little
 * image next to it to give feedback that recording is occurring.
 * <p/>
 * The other kinds is the flashcard button, which records while the space bar is held down, (or maybe while the mouse button is held down).
 * It's feedback is the button itself flipping the record images.
 * <p/>
 * User: go22670
 * Date: 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordButton extends Button {
  private static final int PERIOD_MILLIS = 500;
  private static final String RECORD = "Record";
  private static final String STOP = "Stop";

  private boolean recording = false;
  private Timer recordTimer;
  private final int autoStopDelay;
  private boolean doClickAndHold;
  protected boolean mouseDown = false;

  private RecordingListener recordingListener;

  public static interface RecordingListener {
    void startRecording();
    void flip(boolean first);
    void stopRecording();
  }

  public RecordButton(int delay, boolean doClickAndHold, boolean addKeyBinding) {
    super(RECORD);
    this.doClickAndHold = doClickAndHold;
    //if (doClickAndHold) setText("Click and hold to record");
    this.autoStopDelay = delay;
    setType(ButtonType.PRIMARY);
    setupRecordButton(addKeyBinding);
    getElement().setId("record_button");
  }

  /**
   * @param delay
   * @param recordingListener
   * @param doClickAndHold
   */
  public RecordButton(int delay, RecordingListener recordingListener, boolean doClickAndHold) {
    this(delay, doClickAndHold, true);
    this.setRecordingListener(recordingListener);
  }

  protected void removeImage() {
    StyleHelper.removeStyle(icon, icon.getBaseIconType());
  }

  /**
   * @see #RecordButton(int, mitll.langtest.client.recorder.RecordButton.RecordingListener, boolean)
   * @param recordingListener
   */
  public void setRecordingListener(RecordingListener recordingListener) { this.recordingListener = recordingListener;  }

  protected void setupRecordButton(boolean addKeyBinding) {
    if (doClickAndHold) {
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
    } else {
      addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          doClick();
        }
      });
    }
  }

  /**
   * @see #setupRecordButton
   */
  protected void doClick() {
    if (isVisible() && isEnabled()) {
      startOrStopRecording();
    }
  }

  private void startOrStopRecording() {
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
   * @return if we want to flip images
   */
  protected boolean showInitialRecordImage() {
    setText(STOP);
    return true;
  }

  protected void showFirstRecordImage() {}
  protected void showSecondRecordImage() {}

  protected void hideBothRecordImages() {
    setText(RECORD);
  }

  public void initRecordButton() {
    setVisible(true);
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

    recordTimer.schedule(autoStopDelay);
  }

  private void cancelTimer() {
    if (recordTimer != null) {
      recordTimer.cancel();
    }
  }
}
