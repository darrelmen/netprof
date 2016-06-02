/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.StyleHelper;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.AudioAnswer;

import java.util.logging.Logger;

/**
 * Basically a click handler and a timer to click stop recording, if the user doesn't.
 * <p/>
 * Two kinds of record buttons -- simple ones where clicking on the button starts and stop the recording, and there's a little
 * image next to it to give feedback that recording is occurring.
 * <p/>
 * The other kinds is the flashcard button, which records while the space bar is held down, (or maybe while the mouse button is held down).
 * It's feedback is the button itself flipping the record images.
 * <p/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/7/12
 * Time: 5:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordButton extends Button {
  private final Logger logger = Logger.getLogger("RecordButton");

  private static final int PERIOD_MILLIS = 500;
  public static final String RECORD1 = "Record      ";
  public static final String STOP1 = "Recording...";
  private static final String WINDOWS = "Win32";
  private final String RECORD;
  private final String STOP;


  private boolean recording = false;
  private Timer recordTimer;
  private final int autoStopDelay;
  private final boolean doClickAndHold;
  boolean mouseDown = false;

  private RecordingListener recordingListener;
  private final PropertyHandler propertyHandler;
  private Timer afterStopTimer = null;
  int afterStopDelayMillis = 50;

  public interface RecordingListener {
    void startRecording();
    void flip(boolean first);
    void stopRecording();
  }

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#PostAudioRecordButton
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#SimplePostAudioRecordButton(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.LangTestDatabaseAsync, String, String, String, String)
   * @param delay
   * @param doClickAndHold
   * @param buttonText
   * @param stopButtonText
   * @param propertyHandler
   */
  protected RecordButton(int delay, boolean doClickAndHold, String buttonText, String stopButtonText, PropertyHandler propertyHandler) {
    super(buttonText);
    RECORD = buttonText;
    STOP = stopButtonText;
    this.propertyHandler = propertyHandler;
    this.doClickAndHold = doClickAndHold;
    this.autoStopDelay = delay;
    setType(ButtonType.PRIMARY);
    setIcon(IconType.MICROPHONE);
    this.afterStopDelayMillis = propertyHandler.getAfterStopDelayMillis();

    setupRecordButton();
    getElement().setId("record_button");
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#makeRecordButton
   * @param delay
   * @param recordingListener
   * @param doClickAndHold
   * @param propertyHandler
   */
  public RecordButton(int delay, RecordingListener recordingListener, boolean doClickAndHold, PropertyHandler propertyHandler) {
    this(delay, doClickAndHold, RECORD1, STOP1, propertyHandler);
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
   * @see #RecordButton(int, mitll.langtest.client.recorder.RecordButton.RecordingListener, boolean, mitll.langtest.client.PropertyHandler)
   * @param recordingListener
   */
  protected void setRecordingListener(RecordingListener recordingListener) { this.recordingListener = recordingListener;  }

  private void setupRecordButton() {
    if (doClickAndHold) {
      addMouseDownHandler(new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
          if (!mouseDown) {
            mouseDown = true;
            doClick();
          }
          else {
            logger.info("ignoring mouse down since mouse already down " + mouseDown);
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

  private void gotMouseOut() {
    if (mouseDown) {
      mouseDown = false;
//      logger.info("got mouse out " + mouseDown);
      doClick();
    }
  }
  /**
   * @see #setupRecordButton
   */
  void doClick() {
    if (isVisible() && isEnabled()) {
      startOrStopRecording();
    }
  }

  /**
   * Delay end of recording by some number of milliseconds
   * Wait after the user releases the button, since it seems to get cut off...
   * @see #doClick()
   */
  private void startOrStopRecording() {
    if (afterStopTimer != null && afterStopTimer.isRunning()) {
      afterStopTimer.cancel();
    }
    if (isRecording()) {
      cancelTimer();

      afterStopTimer = new Timer() {
        @Override
        public void run() {
          stop();
        }
      };
      afterStopTimer.schedule(propertyHandler.getAfterStopDelayMillis());

    } else {
      start();
      addRecordingMaxLengthTimeout();
    }
  }

  /**
   * @see #startOrStopRecording()
   */
  protected void start() {
    recording = true;
    showRecording();
    recordingListener.startRecording();
  }

  /**
   * @see #startOrStopRecording()
   */
  protected void stop() {
    recording = false;
    showStopped();
    recordingListener.stopRecording();
  }

  /**
   * @see #start()
   */
  private void showRecording() {
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

  private void showStopped() {
    setIcon(IconType.MICROPHONE);

    if (t != null) {
      hideBothRecordImages();
      t.cancel();
    }
  }

  /**
   * @see #showRecording()
   * @return if we want to flip images
   */
  boolean showInitialRecordImage() {
    setText(STOP);
    return true;
  }

  /**
   * @see #flipImage()
   */
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

  /**
   * @see #startOrStopRecording()
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
   *
   * @param validity
   * @return true if showed the popup
   */
  public boolean checkAndShowTooLoud(AudioAnswer.Validity validity) {
    if (getPlatform().contains(WINDOWS) && validity == AudioAnswer.Validity.TOO_LOUD) {
      showTooLoud();
      return true;
    }
    else return false;
  }

  void removeTooltip() {}
  private void showTooLoud() {
    final RecordButton widget = this;
    removeTooltip();
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        new BasicDialog().showPopover(widget,
            null,
            propertyHandler.getTooLoudMessage(), Placement.RIGHT);
      }
    });
  }
}
