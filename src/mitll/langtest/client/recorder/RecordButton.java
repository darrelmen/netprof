package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.StyleHelper;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
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
  public static final String RECORD1 = "Record      ";
  public static final String STOP1 = "Recording...";
  private final String RECORD;
  private final String STOP;

  private boolean recording = false;
  private Timer recordTimer;
  private final int autoStopDelay;
  private final boolean doClickAndHold;
  protected boolean mouseDown = false;

  private RecordingListener recordingListener;

  public static interface RecordingListener {
    void startRecording();
    void flip(boolean first);
    void stopRecording();
  }

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#PostAudioRecordButton(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.LangTestDatabaseAsync, int, boolean, String, String)
   * @param delay
   * @param doClickAndHold
   * @param buttonText
   * @param stopButtonText
   */
  protected RecordButton(int delay, boolean doClickAndHold, String buttonText, String stopButtonText) {
    super(buttonText);
    RECORD = buttonText;
    STOP = stopButtonText;
    //if (STOP.equalsIgnoreCase("stop")) new Exception().printStackTrace();
    this.doClickAndHold = doClickAndHold;
/*    if (doClickAndHold) {
      setTitle(FlashcardRecordButtonPanel.PRESS_AND_HOLD_THE_MOUSE_BUTTON_TO_RECORD);
    }*/
    this.autoStopDelay = delay;
    setType(ButtonType.PRIMARY);
    setIcon(IconType.MICROPHONE);

    setupRecordButton();
    getElement().setId("record_button");
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#makeRecordButton
   * @param delay
   * @param recordingListener
   * @param doClickAndHold
   */
  public RecordButton(int delay, RecordingListener recordingListener, boolean doClickAndHold) {
    this(delay, doClickAndHold, RECORD1, STOP1);
    this.setRecordingListener(recordingListener);
  }

  void removeImage() {  StyleHelper.removeStyle(icon, icon.getBaseIconType());  }

  public boolean isRecording() {
    return recording;
  }

  public void clickStop() {
    if (isRecording()) {
      fireEvent(new ButtonClickEvent());
    }
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {}

  /**
   * @see #RecordButton(int, mitll.langtest.client.recorder.RecordButton.RecordingListener, boolean)
   * @param recordingListener
   */
  protected void setRecordingListener(RecordingListener recordingListener) { this.recordingListener = recordingListener;  }

  void setupRecordButton() {
    if (doClickAndHold) {
      addMouseDownHandler(new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
          if (!mouseDown) {
            mouseDown = true;
            doClick();
          }
          else {
            System.out.println("ignoring mouse down since mouse already down " + mouseDown);
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

      addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          /*if (mouseDown) {
            mouseDown = false;
            doClick();
          }*/
          gotMouseOut();
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

  protected void gotMouseOut() {
    if (mouseDown) {
      mouseDown = false;
      System.out.println("got mouse out " + mouseDown);
      doClick();
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
    if (isRecording()) {
      cancelTimer();
      stop();
    } else {
      start();
      addRecordingMaxLengthTimeout();
    }
  }

  protected void start() {
    recording = true;
    showRecording();
    recordingListener.startRecording();
  }

  protected void stop() {
    recording = false;
    showStopped();
    recordingListener.stopRecording();
  }

  void showRecording() {
    setIcon(IconType.STOP);

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

  void showStopped() {
    setIcon(IconType.MICROPHONE);

    if (t != null) {
      hideBothRecordImages();
      t.cancel();
    }
  }

  /**
   * @return if we want to flip images
   */
  boolean showInitialRecordImage() {
    setText(STOP);
    return true;
  }

  void showFirstRecordImage() {}
  void showSecondRecordImage() {}

  void hideBothRecordImages() {
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
        if (isRecording()) {
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
