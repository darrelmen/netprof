package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/19/13
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnterKeyButtonHelper {
  private final boolean removeOnEnter;
  private HandlerRegistration keyHandler;

  public EnterKeyButtonHelper() { removeOnEnter = true;}
  public EnterKeyButtonHelper(boolean removeOnEnter) { this.removeOnEnter = removeOnEnter; }

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

  public void addKeyHandler(final Button button, Modal container) {
   // addKeyHandler(button);
    container.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        removeKeyHandler();
      }
    });

    container.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        addKeyHandler(button);
      }
    });
  }

    public void removeKeyHandler() {
    if (keyHandler == null) {
      System.err.println("\nEnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
    } else {
      //System.out.println("EnterKeyButtonHelper : removeKeyHandler : " + keyHandler);
    }
    if (keyHandler != null) {
      keyHandler.removeHandler();
      keyHandler = null;
    }
  }

  private void userHitEnterKey(Button button) {
    //System.out.println("\tEnterKeyButtonHelper.userHitEnterKey " + keyHandler);
    if (removeOnEnter) removeKeyHandler();
    button.fireEvent(new ButtonClickEvent());
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }
}
