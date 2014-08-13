package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/19/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyPressHelper {
  private final boolean removeOnEnter;
  private HandlerRegistration keyHandler;
  private Map<String,KeyListener> listeners = new HashMap<String, KeyListener>();

  /**
   * @see mitll.langtest.client.dialog.ModalInfoDialog
   */
  public KeyPressHelper() {
    removeOnEnter = true;
  }

  /**
   * @see mitll.langtest.client.custom.CreateListDialog#doCreate(com.google.gwt.user.client.ui.Panel)
   * @param removeOnEnter
   */
  public KeyPressHelper(boolean removeOnEnter) {
    this.removeOnEnter = removeOnEnter;
  }

  public KeyPressHelper(boolean removeOnPress, boolean hearAllEvents) {
    this(removeOnPress);
    makeKeyHandler();
  }

  public void addKeyHandler(final Button button) {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int keyCode = ne.getKeyCode();
                                                     boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
                                                     if (isEnter && event.getTypeInt() == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {
                                                       ne.preventDefault();
                                                       ne.stopPropagation();
                                                       userHitEnterKey(button);
                                                     }
                                                   }
                                                 });
    //System.out.println("addKeyHandler made click handler " + keyHandler);
  }

  public int getSize() { return listeners.size(); }

  public void addKeyHandler(KeyListener handler) {
    listeners.put(handler.getName(), handler);
    if (listeners.size() > 3) {
      System.out.println("addKeyHandler added  " + handler.getName() + " now " + this);
    }
  }

  protected void makeKeyHandler() {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int typeInt = event.getTypeInt();

                                                     if ((typeInt == 0x00080 || // keydown
                                                       typeInt == 0x00200) // keyup
                                                       &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {
   /*                                                    if (false) {
                                                         System.out.println(new Date() +
                                                           " : getNextAndPreviousButtons - key handler : " + keyHandler +
                                                           " Got " + event + " type int " +
                                                           typeInt + " assoc " + event.getAssociatedType() +
                                                           " native " + event.getNativeEvent() +
                                                           " source " + event.getSource());
                                                       }*/

                                                       gotEvent(ne, typeInt == 0x00080);
                                                     }
                                                   }
                                                 });
  }

  public boolean removeKeyHandler(String name) {
    KeyListener remove = listeners.remove(name);

    return remove != null;
  }

  private void gotEvent(NativeEvent ne, boolean isKeyDown) {
    for (KeyListener keyPressHandler : listeners.values()) {
      keyPressHandler.gotPress(ne, isKeyDown);
    }
  }

  public static interface KeyListener {
    String getName();

    void gotPress(NativeEvent ne, boolean isKeyDown);
  }

  public void removeKeyHandler() {
    if (keyHandler == null) {
      System.err.println("\nEnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
    } //else {
      //System.out.println("EnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
   // }
    if (keyHandler != null) {
      keyHandler.removeHandler();
      keyHandler = null;
    }
  }

  public void userHitEnterKey(Button button) {
    //System.out.println("\tEnterKeyButtonHelper.userHitEnterKey " + keyHandler);
    if (removeOnEnter) removeKeyHandler();
    button.fireEvent(new ButtonClickEvent());
  }

  public static class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  public String toString() { return "KeyPressHelper : " + listeners; }
}
