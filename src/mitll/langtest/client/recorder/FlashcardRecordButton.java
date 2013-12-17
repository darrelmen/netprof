package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.flashcard.MyCustomIconType;

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
public class FlashcardRecordButton extends RecordButton {
  private static final int SPACE_CHAR = 32;
  private static final int HIDE_DELAY = 2500;
  private static final String SPACE_BAR = "space bar";
  private static final String NO_SPACE_WARNING = "Press and hold space bar or mouse button to begin recording, release to stop.";

  private boolean warnUserWhenNotSpace = true;
  private final boolean addKeyBinding;

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton(mitll.langtest.client.exercise.ExerciseController)
   * @param delay
   * @param recordingListener
   * @param warnNotASpace
   * @param addKeyBinding
   */
  public FlashcardRecordButton(int delay, RecordingListener recordingListener, boolean warnNotASpace, boolean addKeyBinding) {
    super(delay, recordingListener, true);
    this.addKeyBinding = addKeyBinding;
    this.warnUserWhenNotSpace = addKeyBinding && warnNotASpace;
   // setText(addKeyBinding ? getButtonText() : "Click and hold mouse button to record, release to stop.");
    DOM.setStyleAttribute(getElement(), "width", "460px");
    DOM.setStyleAttribute(getElement(), "height", "48px");
    DOM.setStyleAttribute(getElement(), "fontSize", "x-large");
    DOM.setStyleAttribute(getElement(), "fontFamily", "Arial Unicode MS, Arial, sans-serif");
    DOM.setStyleAttribute(getElement(), "verticalAlign", "middle");
    DOM.setStyleAttribute(getElement(), "lineHeight", "37px");

    initRecordButton();
  }

  protected void setupRecordButton(boolean addKeyBinding) {
    super.setupRecordButton(addKeyBinding);

    if (addKeyBinding) {
      addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (event.getNativeKeyCode() == SPACE_CHAR) {
            if (!mouseDown) {
              mouseDown = true;
              doClick();
            }
          } else if (warnUserWhenNotSpace) {
            warnNotASpace();
          }
        }
      });
      addKeyUpHandler(new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
          if (event.getNativeKeyCode() == SPACE_CHAR) {
            mouseDown = false;
            doClick();
          }
        }
      });
    }
    getFocus();
  }

  private void getFocus() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        setFocus(true);
      }
    });
  }

  protected void warnNotASpace() {
    showPopup(NO_SPACE_WARNING);
  }

  public void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(HIDE_DELAY);
  }

  protected boolean showInitialRecordImage() {
    showFirstRecordImage();
    return true;
  }

  protected void showFirstRecordImage() {
    setBaseIcon(MyCustomIconType.record1);
    setText("");
  }

  protected void showSecondRecordImage() {
    setBaseIcon(MyCustomIconType.record2);
    setText("");
  }

  protected void hideBothRecordImages() {
    initRecordButton();
    removeImage();
  }

  public void initRecordButton() {
    super.initRecordButton();
    setText(addKeyBinding ? SPACE_BAR : "Click and hold mouse button to record.");
    setType(ButtonType.PRIMARY);
    getFocus();
  }
}
