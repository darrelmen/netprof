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

  public void setWidget(Widget widget) {
    this.widget = widget;
  }
}
