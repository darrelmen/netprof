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

import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.MyCustomIconType;

import java.util.logging.Logger;

import static mitll.langtest.client.scoring.PostAudioRecordButton.MIN_DURATION;

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
public class FlashcardRecordButton extends RecordButton {
  private final Logger logger = Logger.getLogger("FlashcardRecordButton");

  /**
   * @see #initRecordButton
   */
  private static final String PROMPT = "Press and hold to record";
  private static final int WIDTH_FOR_BUTTON = 360;

  private final ExerciseController controller;
  private static final boolean DEBUG = false;
  private static int count = 0;
  private final String name;
  private int id = 0;

  /**
   * @param delay
   * @param recordingListener
   * @param addKeyBinding
   * @param controller
   * @param instance
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton
   */
  public FlashcardRecordButton(int delay,
                               RecordingListener recordingListener,
                               boolean addKeyBinding,
                               ExerciseController controller,
                               final String instance) {
    super(delay, recordingListener, true, controller.getProps());
    id = count++;
    name = "FlashcardRecordButton_";

    if (addKeyBinding) {
      addKeyListener(controller, instance);
      // logger.info("FlashcardRecordButton : " + instance + " key is  " + listener.getName());
    }
    this.controller = controller;

    setWidth(WIDTH_FOR_BUTTON + "px");
    setHeight("48px");
    Style style = getElement().getStyle();
    style.setProperty("fontSize", "x-large");
    style.setProperty("fontFamily", "Arial Unicode MS, Arial, sans-serif");

    style.setVerticalAlign(Style.VerticalAlign.MIDDLE);
    style.setLineHeight(37, Style.Unit.PX);

    initRecordButton();

    getElement().setId("FlashcardRecordButton_" + instance + "_" + id);
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    removeListener();

    if (isRecording()) {
      logger.info("stop recording since detach!");
      stop(MIN_DURATION + 1);
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();

  //  logger.info("onUnload ---> ");
    removeListener();
    stopRecordingSafe();
  }

  private void removeListener() {
    if (listener != null) {
      controller.removeKeyListener(listener);
      listener = null;
    } else {
      //logger.info("removeListener no listener ");
    }
  }

  private KeyPressHelper.KeyListener listener = null;

  private void addKeyListener(ExerciseController controller, final String instance) {
    if (DEBUG) logger.info("FlashcardRecordButton.addKeyListener : using  for " + instance);
    listener = new KeyPressHelper.KeyListener() {
      @Override
      public String getName() {
        return name;
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


  private void checkKeyDown(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      boolean isSpace = checkIsSpace(event);
      if (isSpace) {
        if (DEBUG) logger.info("checkKeyDown got space " + event);
        if (!mouseDown) {
          mouseDown = true;
          doClick();
        }
      } else {
        int keyCode = event.getKeyCode();
        if (DEBUG) logger.info("checkKeyDown key code is " + keyCode);

        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            stopProp(event);
            gotLeftArrow();
            break;
          case KeyCodes.KEY_RIGHT:
            stopProp(event);
            gotRightArrow();
            break;
          case KeyCodes.KEY_UP:
            stopProp(event);
            gotUpArrow();
            break;
          case KeyCodes.KEY_DOWN:
            stopProp(event);
            gotDownArrow();
            break;
          case KeyCodes.KEY_ENTER:
            stopProp(event);
            gotEnter();
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

  protected void gotRightArrow() {
  }

  protected void gotLeftArrow() {
  }

  protected void gotUpArrow() {
  }

  protected void gotDownArrow() {
  }

  protected void gotEnter() {
  }

  private void checkKeyUp(NativeEvent event) {
    if (!shouldIgnoreKeyPress()) {
      if (checkIsSpace(event)) {
        if (!mouseDown) {
          logger.warning("huh? mouse down = false");
        } else {
          mouseDown = false;
          doClick();
        }
      }
    } else {
      logger.info("checkKeyUp ignore key up.");
    }
  }

  private boolean checkIsSpace(NativeEvent event) {
    return event.getKeyCode() == KeyCodes.KEY_SPACE;
  }

  protected boolean shouldIgnoreKeyPress() {
    boolean notAttached = !isAttached();
    if (notAttached) {
      logger.info("shouldIgnoreKeyPress not attached? " + getElement().getId() + " = " + id);
      removeListener();
    }
    boolean hidden = checkHidden(getElement().getId());
    if (hidden) {
      logger.info("shouldIgnoreKeyPress : hidden");
      removeListener();
      stopRecordingSafe();
    }
    boolean noUser = controller.getUser() == -1;
    if (noUser) logger.info("noUser");
    boolean b = notAttached || hidden || noUser;
    return b;
  }

  private native boolean checkHidden(String id)  /*-{
      return $wnd.jQuery('#' + id).is(":hidden");
  }-*/;

  protected boolean showInitialRecordImage() {
    showFirstRecordImage();
    return true;
  }

  void showFirstRecordImage() {
    setBaseIcon(MyCustomIconType.record1);
    setText("");
  }

/*  protected void showSecondRecordImage() {
    setBaseIcon(MyCustomIconType.record2);
    setText("");
  }*/

  protected void hideBothRecordImages() {
    initRecordButton();
    removeImage();
    setIcon(IconType.MICROPHONE);
  }

  public void initRecordButton() {
    super.initRecordButton();
    setText("");
    setType(ButtonType.DANGER);
  }

  protected String getPrompt() {
    return PROMPT;
  }
}
