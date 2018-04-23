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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/19/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyPressHelper {
  private static final String OBJECT_KEYBOARD_EVENT = "[object KeyboardEvent]";
  private final Logger logger = Logger.getLogger("KeyPressHelper");

  private final boolean removeOnEnter;
  private HandlerRegistration keyHandler;
  private final Map<String, KeyListener> listeners = new HashMap<>();

  /**
   * @see mitll.langtest.client.dialog.ModalInfoDialog
   */
  KeyPressHelper() {
    removeOnEnter = true;
  }

  /**
   * @param removeOnEnter
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate(com.google.gwt.user.client.ui.Panel)
   */
  public KeyPressHelper(boolean removeOnEnter) {
    this.removeOnEnter = removeOnEnter;
  }

  /**
   * @param removeOnPress
   * @param hearAllEvents
   * @see mitll.langtest.client.LangTest
   */
  public KeyPressHelper(boolean removeOnPress, boolean hearAllEvents) {
    this(removeOnPress);
    makeKeyHandler();
  }

  public void addKeyHandler(final Button button) {
    keyHandler = Event.addNativePreviewHandler(event -> {
      NativeEvent ne = event.getNativeEvent();
      int keyCode = ne.getKeyCode();
      boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
      if (isEnter && event.getTypeInt() == 512 &&
          OBJECT_KEYBOARD_EVENT.equals(ne.getString())) {
        ne.preventDefault();
        ne.stopPropagation();
        userHitEnterKey(button);
      }
    });
    //logger.info("addKeyHandler made key handler " + keyHandler);
  }

  public int getSize() {
    return listeners.size();
  }

  public void addKeyHandler(KeyListener handler) {
    String name = handler.getName();
    KeyListener put = listeners.put(name, handler);
    boolean already = put != null;
//    logger.info("addKeyHandler " + name + " now " + listeners.size() + " already " + already);
    if (listeners.size() > 3) {
      logger.info("addKeyHandler added  " + name + " now " + this);
    }
  }

  public boolean removeKeyHandler(KeyListener listener) {
    int before = listeners.size();
    boolean b = listeners.remove(listener.getName()) != null;
  //  logger.info("removeKeyHandler now " + listeners.size() + " listeners vs " + before);
    return b;
  }

  public void clearListeners() {
    listeners.clear();
  }

  private void makeKeyHandler() {
    keyHandler = Event.addNativePreviewHandler(event -> {
      NativeEvent ne = event.getNativeEvent();
      int typeInt = event.getTypeInt();

      if ((typeInt == 0x00080 || // keydown
          typeInt == 0x00200) // keyup
          &&
          OBJECT_KEYBOARD_EVENT.equals(ne.getString())) {
        gotEvent(ne, typeInt == 0x00080);
      }
    });
  }

  private void gotEvent(NativeEvent ne, boolean isKeyDown) {
    for (KeyListener keyPressHandler : listeners.values()) {
      //   logger.info("KeyPressHelper " + keyPressHandler + " getting " + ne + " down " +isKeyDown);
      keyPressHandler.gotPress(ne, isKeyDown);
    }
  }

  public interface KeyListener {
    String getName();

    void gotPress(NativeEvent ne, boolean isKeyDown);
  }

  public void removeKeyHandler() {
    if (keyHandler == null) {
      //logger.warning("\nEnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
    } //else {
    // logger.info("EnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
    // }
    if (keyHandler != null) {
      //logger.info("KeyPressHelper : removeKeyHandler : " + keyHandler);
      keyHandler.removeHandler();
      keyHandler = null;
    }
  }

  public void userHitEnterKey(Button button) {
    //logger.info("\tEnterKeyButtonHelper.userHitEnterKey " + keyHandler);
    if (removeOnEnter) removeKeyHandler();
    button.fireEvent(new ButtonClickEvent());
  }

  public static class ButtonClickEvent extends ClickEvent {
    /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  public String toString() {
    return "KeyPressHelper : " + listeners;
  }
}
