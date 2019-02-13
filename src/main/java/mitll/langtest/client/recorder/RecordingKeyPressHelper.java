package mitll.langtest.client.recorder;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;

import java.util.logging.Logger;

public class RecordingKeyPressHelper {
  private final Logger logger = Logger.getLogger("KeyPressHelper");
  private static final boolean DEBUG = false;

  private ExerciseController controller;
  private Widget widget;
  private KeyPressDelegate delegate;
  private mitll.langtest.client.dialog.KeyPressHelper.KeyListener listener = null;

  public RecordingKeyPressHelper(Widget widget,
                                 KeyPressDelegate delegate,
                                 ExerciseController controller) {
    this.widget = widget;
    this.delegate = delegate;
    this.controller = controller;
  }

  public void removeListener() {
    if (listener != null) {
      controller.removeKeyListener(listener);
      listener = null;
    } else {
      //logger.info("removeListener no listener ");
    }
  }


  public void addKeyListener(ExerciseController controller) {
    // if (DEBUG) logger.info("FlashcardRecordButton.addKeyListener : using  for " + instance);
    listener = new mitll.langtest.client.dialog.KeyPressHelper.KeyListener() {
      @Override
      public String getName() {
        return "handler";
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
  }

  /**
   * Remember to prevent default on space event or the window will jump (why?)
   *
   * @param event
   */
  private void checkKeyDown(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      boolean isSpace = checkIsSpace(event);
      if (isSpace) {
        event.preventDefault();  // critical!
        if (DEBUG) logger.info("checkKeyDown got space " + event);
        delegate.gotSpaceBar();
//        if (!mouseDown) {
//          mouseDown = true;
//          doClick();
//        }

      } else {
        int keyCode = event.getKeyCode();
        if (DEBUG) logger.info("checkKeyDown key code is " + keyCode);

        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            stopProp(event);
            delegate.gotLeftArrow();
            break;
          case KeyCodes.KEY_RIGHT:
            stopProp(event);
            delegate.gotRightArrow();
            break;
          case KeyCodes.KEY_UP:
            stopProp(event);
            delegate.gotUpArrow();
            break;
          case KeyCodes.KEY_DOWN:
            stopProp(event);
            delegate.gotDownArrow();
            break;
          case KeyCodes.KEY_ENTER:
            stopProp(event);
            delegate.gotEnter();
            break;
        }
      }
    } else {
      if (DEBUG) logger.info("ignore key down.");
    }
  }

  private void stopProp(NativeEvent event) {
    event.stopPropagation();
    event.preventDefault();
  }


  private void checkKeyUp(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      if (checkIsSpace(event)) {
        delegate.gotSpaceBarKeyUp();

//        if (!mouseDown) {
//          logger.warning("huh? mouse down = false");
//        } else {
//          mouseDown = false;
//          doClick();
//        }
      }
    } else {
      logger.info("checkKeyUp ignore key up.");
    }
  }

  private boolean checkIsSpace(NativeEvent event) {
    return event.getKeyCode() == KeyCodes.KEY_SPACE;
  }

  protected boolean shouldIgnoreKeyPress() {
    boolean notAttached = !widget.isAttached();
    if (notAttached) {
      logger.info("shouldIgnoreKeyPress not attached? " + widget.getElement().getId());
      removeListener();
    }
    boolean hidden = checkHidden(widget.getElement().getId());
    if (hidden) {
      //    logger.info("shouldIgnoreKeyPress : hidden");
      removeListener();
      delegate.stopRecordingSafe();
    }
    boolean noUser = controller.getUser() == -1;
    if (noUser) logger.info("noUser");
    boolean b = notAttached || hidden || noUser;
    return b;
  }

  private native boolean checkHidden(String id)  /*-{
      return $wnd.jQuery('#' + id).is(":hidden");
  }-*/;

}
