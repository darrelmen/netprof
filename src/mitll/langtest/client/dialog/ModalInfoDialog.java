package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModalInfoDialog {
  private final KeyPressHelper enterKeyButtonHelper = new KeyPressHelper();
  private int messageHeading = 5;

  public ModalInfoDialog(String title, String message) {
    this(title, message, null);
  }

  public ModalInfoDialog(String title, String message, HiddenHandler handler) {
    this(title, Collections.singleton(message), null, handler);
  }

  public ModalInfoDialog(String title, Collection<String> messages, Widget widget, HiddenHandler handler) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    for (String m : messages) {
      Heading w = new Heading(messageHeading);
      w.setText(m);
      modal.add(w);
    }
    if (widget != null) modal.add(widget);
    modal.add(new Heading(4));

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

    enterKeyButtonHelper.addKeyHandler(begin);
    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
      }
    });
    modal.add(begin);

    if (handler != null) {
      System.out.println("\tModalInfoDialog.added hidden handler ");

      modal.addHiddenHandler(handler);
    }
    modal.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        enterKeyButtonHelper.removeKeyHandler();
      }
    });
    modal.show();
  }
}