package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.HideEvent;
import com.github.gwtbootstrap.client.ui.event.HideHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class ModalInfoDialog {
  public ModalInfoDialog(String title, String message) {
    this(title, Collections.singleton(message), null);
  }

  public ModalInfoDialog(String title, Collection<String> messages) {
    this(title, messages, null);
  }

  public ModalInfoDialog(String title, String message, HiddenHandler handler) {
    this(title, Collections.singleton(message), handler);
  }


  private HandlerRegistration keyHandler;

  private void addKeyHandler(final Button button) {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int keyCode = ne.getKeyCode();
                                                     boolean isEnter = keyCode == KeyCodes.KEY_ENTER;
                                                     if (isEnter && event.getTypeInt() == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {

                                                  //     System.out.println("ModalInfoDialog : addKeyHandler got click target " +  ne.getEventTarget());
                                                    //   ne.getEventTarget();
                                                       ne.preventDefault();
                                                       ne.stopPropagation();
                                                       userHitEnterKey(button);
                                                     }
                                                   }
                                                 });
     System.out.println("addKeyHandler made click handler " + keyHandler);
  }

  public void removeKeyHandler() {
    if (keyHandler == null) {
      System.err.println("\nModalInfoDialog : removeKeyHandler : " + keyHandler);

    } else {
      System.out.println("ModalInfoDialog : removeKeyHandler : " + keyHandler);
    }
    if (keyHandler != null) {
      keyHandler.removeHandler();
    }
  }

  private void userHitEnterKey(Button button) {
    System.out.println("\tModalInfoDialog.userHitEnterKey " + keyHandler);
   // removeKeyHandler();
    button.fireEvent(new ButtonClickEvent());
  }

  private class ButtonClickEvent extends ClickEvent{
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }


  public ModalInfoDialog(String title, Collection<String> messages, HiddenHandler handler) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    for (String m : messages) {
      Heading w = new Heading(4);
      w.setText(m);
      modal.add(w);
    }

    final Button begin = new Button("OK");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);
    begin.setFocus(true);

    // Set focus on the widget. We have to use a deferred command or a
    // timer since GWT will lose it again if we set it in-line here
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        begin.setFocus(true);
      }
    });

    addKeyHandler(begin);
/*
    begin.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        System.out.println("\n\n\n\tModalInfoDialog.got key up!!!! : key code: " + event.getNativeKeyCode());
      }
    });
*/

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        removeKeyHandler();
        modal.hide();
      }
    });
    modal.add(begin);

    if (handler != null) {
      modal.addHiddenHandler(handler);
    }

    System.out.println(new Date() +" ModalInfoDialog.showing...");

    modal.show();
  }
}