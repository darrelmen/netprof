/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.StyleHelper;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.scoring.NoFeedbackRecordAudioPanel;
import mitll.langtest.client.scoring.RecordDialogExercisePanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.answer.Validity;

import java.util.logging.Logger;

public class RecordButton extends Button {
  private final Logger logger = Logger.getLogger("RecordButton");

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
   */
  private static final String RECORD1 = "Record      ";
  public static final String STOP1 = "Recording...";

  private static final String WINDOWS = "Win32";
  private final String RECORD;
  private final String STOP;

  private boolean recording = false;
  private Timer recordTimer;
  private final int autoStopDelay;
  private final boolean doClickAndHold;
  protected boolean mouseDown = false;

  private RecordingListener recordingListener;
  private final PropertyHandler propertyHandler;
  private final int afterStopDelayMillis;

  private long started = 0;

  /**
   * don't actually stop recording until a little while after end of recording
   */
  private Timer afterStopTimer = null;

  private static final boolean DEBUG = false;

  public interface RecordingListener {
    /**
     * @see #start()
     */
    void startRecording();

    /**
     * @param duration
     * @param abort
     * @return
     * @see RecordButton#stop(long, boolean)
     */
    boolean stopRecording(long duration, boolean abort);
  }

  /**
   * @param delay
   * @param doClickAndHold
   * @param buttonText
   * @param stopButtonText
   * @param propertyHandler
   * @seex mitll.langtest.client.scoring.SimplePostAudioRecordButton#SimplePostAudioRecordButton(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.services.LangTestDatabaseAsync, String, String, String, String)
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#PostAudioRecordButton
   */
  protected RecordButton(int delay, boolean doClickAndHold, String buttonText, String stopButtonText,
                         PropertyHandler propertyHandler) {
    super(buttonText);
    RECORD = buttonText;
//    logger.info("RecordButton button text " + buttonText );
    STOP = stopButtonText;
    this.propertyHandler = propertyHandler;
    afterStopDelayMillis = propertyHandler.getAfterStopDelayMillis();

    this.doClickAndHold = doClickAndHold;
    this.autoStopDelay = delay;
    setType(ButtonType.DANGER);
    setIcon(IconType.MICROPHONE);
    setupRecordButton();
    getElement().setId("record_button");
  }

  /**
   * @param delay
   * @param recordingListener
   * @param doClickAndHold
   * @param propertyHandler
   * @see mitll.langtest.client.recorder.RecordButtonPanel#makeRecordButton
   */
  public RecordButton(int delay, RecordingListener recordingListener, boolean doClickAndHold, PropertyHandler propertyHandler) {
    this(delay, doClickAndHold, RECORD1, STOP1, propertyHandler);
    this.setRecordingListener(recordingListener);
  }

  /**
   * MUST BE PROTECTED - no IDEA no!
   */
  protected void removeImage() {
    StyleHelper.removeStyle(icon, icon.getBaseIconType());
  }

  /**
   * @see RecordAudioPanel#clickStop
   */
  public void clickStop() {
    if (isRecording()) {
      fireEvent(new ButtonClickEvent());
    }
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {
  }

  /**
   * @param recordingListener
   * @see #RecordButton(int, mitll.langtest.client.recorder.RecordButton.RecordingListener, boolean, PropertyHandler)
   */
  protected void setRecordingListener(RecordingListener recordingListener) {
    this.recordingListener = recordingListener;
  }

  private void setupRecordButton() {
    if (doClickAndHold) {
      addMouseDownHandler(event -> {
        if (!mouseDown) {
          // logger.info("gotMouseDown  " + mouseDown);
          mouseDown = true;
          doClick(event);
        } else {
          logger.info("setupRecordButton ignoring mouse down since mouse already down ");
        }
        event.preventDefault();
      });

      addMouseUpHandler(event -> {
        // logger.info("gotMouseUp  " + mouseDown);
        if (mouseDown) {
          mouseDown = false;
          doClick(event);
        } else {
          logger.info("setupRecordButton ignoring mouse up since mouse already up");
        }
      });

      addMouseOutHandler(event -> gotMouseOut());
    } else {
      addClickHandler(this::doClick);
    }
  }

  private void gotMouseOut() {
    if (mouseDown) {
      mouseDown = false;
      doClick(null);
    }
  }

  /**
   * NOTE : Can't be private or package private - IDEA mistake...
   *
   * @param clickEvent
   * @see #setupRecordButton
   */
  protected void doClick(MouseEvent<?> clickEvent) {
    if (isVisible() && isEnabled()) {
      if (clickEvent != null) {
        clickEvent.stopPropagation();
      }
      startOrStopRecording();
    }
  }

  /**
   * Delay end of recording by some number of milliseconds
   * Wait after the user releases the button, since it seems to get cut off...
   *
   * @return true if started, false if stopped
   * @see #doClick
   */
  public boolean startOrStopRecording() {
    //long enter = System.currentTimeMillis();
//    if (last  > 0) {
//      logger.info("startOrStopRecording at " + enter + " millis after dur " +  (enter-last));
//    }
//    else {
//      logger.info("startOrStopRecording at " + enter + " millis");
//    }
    //   last = enter;

    if (isRecording()) {
      stopRecording();
      return false;
    } else {
      startRecordingWithTimer();
      return true;
    }
  }

  private void startRecordingWithTimer() {
    recording = true;
    started = System.currentTimeMillis();

    cancelAfterStopTimer();
    //  logger.info("startRecordingWithTimer started = " + started);
    start();
    addRecordingMaxLengthTimeout();
  }

  public void stopRecordingSafe() {
    if (isRecording()) {
      stopRecording();
      // return true;
    } else {
/*      boolean running = afterStopTimer != null && afterStopTimer.isRunning();
      boolean running1 = recordTimer != null && recordTimer.isRunning();
      logger.info("stopRecordingSafe : not currently recording " +
          "\n\tafter stop running = " + running +
          "\n\trecord timer       = " + running1
      );*/

      //return false;
    }
  }

  /**
   * @see #startOrStopRecording
   */
  private void stopRecording() {
    long now = System.currentTimeMillis();
    stopRecordingFirstStep();
    long duration = now - started;

    if (DEBUG) {
      logger.info("stopRecording : ui time between button clicks = " + duration + " millis, " +
          "\n\tdelay ");
    }

    afterStopTimer = new Timer() {
      @Override
      public void run() {
        stop(duration, false);
      }
    };
    afterStopTimer.schedule(afterStopDelayMillis);
  }

  public boolean isRecording() {
    return recording;
  }

  protected void stopRecordingFirstStep() {
    // logger.info("stopRecordingFirstStep : ");
    recording = false;

    cancelAfterStopTimer();
    cancelTimer();
  }

  private void cancelAfterStopTimer() {
    if (afterStopTimer != null && afterStopTimer.isRunning()) {
      afterStopTimer.cancel();
    }
  }

  /**
   * @see #startOrStopRecording()
   */
  protected void start() {
    showRecording();
    recordingListener.startRecording();
  }

  /**
   * Stop with abort so we know when to not expect a result (score or anything) to be returned.
   *
   * @see RecordDialogExercisePanel#cancelRecording()
   * @see NoFeedbackRecordAudioPanel#cancelRecording
   */
  public void cancelRecording() {
    if (isRecording()) {
//      logger.info("Abort recording!");
      stopRecordingFirstStep();
      stop(0, true);
    }
  }

  /**
   * @param duration
   * @param abort
   * @see #stopRecording()
   * @see #cancelRecording
   * @see #addRecordingMaxLengthTimeout()
   * @see #startOrStopRecording
   */
  protected void stop(long duration, boolean abort) {
//    long now = System.currentTimeMillis();
    // long duration2 = now - started;
    // logger.info("startOrStopRecording after stop delay = " + duration2 + " millis, vs " + duration);
    showStopped();
    recordingListener.stopRecording(duration, abort);
  }

  /**
   * @see #start()
   */
  private void showRecording() {
    setIcon(IconType.STOP);
    showInitialRecordImage();
  }

  /**
   * @see #stop
   */
  private void showStopped() {
    setIcon(IconType.MICROPHONE);
    hideBothRecordImages();
  }

  /**
   * @return if we want to flip images
   * @see #showRecording()
   */
  void showInitialRecordImage() {
    setText(STOP);
  }

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
          stop(autoStopDelay, false);
        }
      }
    };

    recordTimer.schedule(autoStopDelay);
  }

  /**
   * @see #startOrStopRecording
   */
  private void cancelTimer() {
    if (recordTimer != null) {
      recordTimer.cancel();
    }
  }

  protected static native String getPlatform() /*-{
      return window.navigator.platform;
  }-*/;

  /**
   * @param validity
   * @return true if showed the popup
   */
  public boolean checkAndShowTooLoud(Validity validity) {
    if (getPlatform().contains(WINDOWS) && validity == Validity.TOO_LOUD) {
      showTooLoud();
      return true;
    } else {
      return false;
    }
  }

  private void showTooLoud() {
    final RecordButton widget = this;
    Scheduler.get().scheduleDeferred(() -> new BasicDialog().showPopover(widget,
        null,
        propertyHandler.getTooLoudMessage(), Placement.RIGHT));
  }
}
