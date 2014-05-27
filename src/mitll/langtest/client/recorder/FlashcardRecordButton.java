package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
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
  private static final int HIDE_DELAY = 2500;
  private static final String PROMPT2 = "Click/space and hold to record";
  private static final String SPACE_BAR = PROMPT2;//"space bar";
  private static final String NO_SPACE_WARNING = "Press and hold space bar or mouse button to begin recording, release to stop.";
  private static final String PROMPT = "Click and hold to record";
  private static final int WIDTH_FOR_BUTTON = 360;

  private boolean warnUserWhenNotSpace = true;
  private final boolean addKeyBinding;
  private ExerciseController controller;

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton
   * @param delay
   * @param recordingListener
   * @param warnNotASpace
   * @param addKeyBinding
   * @param controller
   * @param instance
   */
  public FlashcardRecordButton(int delay, RecordingListener recordingListener, boolean warnNotASpace,
                               boolean addKeyBinding, ExerciseController controller, final String instance) {
    super(delay, recordingListener, true);

    if (addKeyBinding) {
      KeyPressHelper.KeyListener listener = new KeyPressHelper.KeyListener() {
        @Override
        public String getName() {
          return "FlashcardRecordButton_" + instance;
        }

        @Override
        public void gotPress(NativeEvent ne, boolean isKeyDown) {
          if (isKeyDown) {
            checkKeyDown(ne);
          } else {
            checkKeyUp(ne);
          }
        }

        public String toString() {
          return "KeyListener " + getName();
        }
      };
      controller.addKeyListener(listener);
     // System.out.println("FlashcardRecordButton : " + instance + " key is  " + listener.getName());
    }
    this.controller = controller;

    this.addKeyBinding = addKeyBinding;
    this.warnUserWhenNotSpace = addKeyBinding && warnNotASpace;

    setWidth(WIDTH_FOR_BUTTON + "px");
    setHeight("48px");
    DOM.setStyleAttribute(getElement(), "fontSize", "x-large");
    DOM.setStyleAttribute(getElement(), "fontFamily", "Arial Unicode MS, Arial, sans-serif");
    getElement().getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
    getElement().getStyle().setLineHeight(37, Style.Unit.PX);

    initRecordButton();

    getElement().setId("FlashcardRecordButton_" + instance);

    new TooltipHelper().addTooltip(this, addKeyBinding ? NO_SPACE_WARNING : PROMPT);

//    System.out.println("FlashcardRecordButton : using " + getElement().getId());
  }

  protected void checkKeyDown(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      boolean isSpace = checkIsSpace(event);

      if (isSpace) {
        if (!mouseDown) {
          mouseDown = true;
          doClick();
        }
      } else if (warnUserWhenNotSpace) {
        warnNotASpace();
      }
    }
  }

  protected void checkKeyUp(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      boolean isSpace = checkIsSpace(event);

      if (isSpace) {
        mouseDown = false;
        doClick();
      }
    }
  }

  private boolean checkIsSpace(NativeEvent event) {
    int keyCode = event.getKeyCode();
    return keyCode == KeyCodes.KEY_SPACE;
  }

  private boolean shouldIgnoreKeyPress() {
    return !isAttached() || checkHidden(getElement().getId()) || controller.getUser() == -1;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
  }

  private native boolean checkHidden(String id)  /*-{
    return $wnd.jQuery('#'+id).is(":hidden");
  }-*/;

  private void warnNotASpace() { showPopup(NO_SPACE_WARNING);  }

  private void showPopup(String html) {
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
    setText(addKeyBinding ? SPACE_BAR : PROMPT);
    setType(ButtonType.PRIMARY);
    //getFocus();
  }
/*
  private void getFocus() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        setFocus(true);
      }
    });
  }*/
}
