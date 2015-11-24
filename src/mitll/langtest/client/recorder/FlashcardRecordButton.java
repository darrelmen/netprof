/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import mitll.langtest.client.PopupHelper;
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
  private static final String PROMPT2 = "Click/space and hold to record";
  private static final String SPACE_BAR = PROMPT2;//"space bar";
  private static final String NO_SPACE_WARNING = "Press and hold space bar or mouse button to begin recording, release to stop.";
  private static final String PROMPT = "Click and hold to record";
  private static final int WIDTH_FOR_BUTTON = 360;

  private boolean warnUserWhenNotSpace = true;
  private final boolean addKeyBinding;
  private final ExerciseController controller;
  private Tooltip tooltip;

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
    super(delay, recordingListener, true, controller.getProps());

    if (addKeyBinding) {
      addKeyListener(controller, instance);
     // System.out.println("FlashcardRecordButton : " + instance + " key is  " + listener.getName());
    }
    this.controller = controller;

    this.addKeyBinding = addKeyBinding;
    this.warnUserWhenNotSpace = addKeyBinding && warnNotASpace;

    setWidth(WIDTH_FOR_BUTTON + "px");
    setHeight("48px");
    getElement().getStyle().setProperty("fontSize","x-large");
   // DOM.setStyleAttribute(getElement(), "fontSize", "x-large");
    getElement().getStyle().setProperty("fontFamily","Arial Unicode MS, Arial, sans-serif");

   // DOM.setStyleAttribute(getElement(), "fontFamily", "Arial Unicode MS, Arial, sans-serif");
    getElement().getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
    getElement().getStyle().setLineHeight(37, Style.Unit.PX);

    initRecordButton();

    getElement().setId("FlashcardRecordButton_" + instance);

    tooltip = new TooltipHelper().addTooltip(this, addKeyBinding ? NO_SPACE_WARNING : PROMPT);

//    System.out.println("FlashcardRecordButton : using " + getElement().getId());
  }

  protected void addKeyListener(ExerciseController controller, final String instance) {


  //     System.out.println("FlashcardRecordButton.addKeyListener : using " + getElement().getId() + " for " + instance);

    KeyPressHelper.KeyListener listener = new KeyPressHelper.KeyListener() {
      @Override
      public String getName() {
        return "FlashcardRecordButton_" + instance;
      }

      @Override
      public void gotPress(NativeEvent ne, boolean isKeyDown) {
        if (isKeyDown) {
          checkKeyDown(ne/*,this*/);
        } else {
          checkKeyUp(ne);
        }
      }

      public String toString() {
        return "KeyListener " + getName();
      }
    };
    controller.addKeyListener(listener);
  }

  private void checkKeyDown(NativeEvent event
  //    , KeyPressHelper.KeyListener listener
  ) {
    if (!shouldIgnoreKeyPress()) {
      boolean isSpace = checkIsSpace(event);
      //System.out.println("checkKeyDown got key press...");

      if (isSpace) {
        if (!mouseDown) {
          mouseDown = true;
          doClick();
        }
      } else if (warnUserWhenNotSpace) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyCodes.KEY_ALT || keyCode == KeyCodes.KEY_CTRL || keyCode == KeyCodes.KEY_ESCAPE || keyCode == KeyCodes.KEY_WIN_KEY) {
          //System.out.println("key code is " + keyCode);
        }
        else {
          //System.out.println("warn - key code is " + keyCode);
          if (keyCode == KeyCodes.KEY_LEFT) {
            gotLeftArrow();
            event.stopPropagation();
          }
          else if (keyCode == KeyCodes.KEY_RIGHT) {
            gotRightArrow();
            event.stopPropagation();
          }
          else {
            warnNotASpace();
          }
        }
      }
    }
    else {
    //  System.out.println("checkKeyDown ignoring key press... " + listener);
    }
  }

  protected void gotRightArrow() {}

  protected void gotLeftArrow()  {}

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

  protected boolean shouldIgnoreKeyPress() {
    boolean b = !isAttached() || checkHidden(getElement().getId()) || controller.getUser() == -1;

    //if (b) {
      //System.out.println("attached " + isAttached());
   //   System.out.println("hidden   " + checkHidden(getElement().getId()));
    //  System.out.println("user     " + controller.getUser());
   // }
    return b;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
  }

  private native boolean checkHidden(String id)  /*-{
    return $wnd.jQuery('#'+id).is(":hidden");
  }-*/;

  /**
   * @see #checkKeyDown(com.google.gwt.dom.client.NativeEvent)
   */
  private void warnNotASpace() { showPopup(NO_SPACE_WARNING);  }

  private void showPopup(String html) { new PopupHelper().showPopup(html); }

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
    setIcon(IconType.MICROPHONE);
  }

  public void initRecordButton() {
    super.initRecordButton();
    setText(addKeyBinding ? SPACE_BAR : PROMPT);
    setType(ButtonType.PRIMARY);
  }

  public void removeTooltip() {
    if (tooltip != null) {
      tooltip.remove(this);
//      tooltip.reconfigure();
    }
  }
}
