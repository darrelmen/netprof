package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.github.gwtbootstrap.client.ui.event.HideEvent;
import com.github.gwtbootstrap.client.ui.event.HideHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import java.util.Collection;
import java.util.Collections;

public class ModalInfoDialog {
  public ModalInfoDialog(String title, String message) {
    this(title, Collections.singleton(message), null);
  }

  public ModalInfoDialog(String title, String message, HiddenHandler handler) {
    this(title, Collections.singleton(message), handler);
  }

  public ModalInfoDialog(String title, Collection<String> message, HiddenHandler handler) {
    final Modal modal = new Modal(true);
    modal.setTitle(title);
    for (String m : message) {
      Heading w = new Heading(4);
      w.setText(m);
      modal.add(w);
    }

    final Button begin = new Button("OK");
    begin.setType(ButtonType.PRIMARY);
    begin.setEnabled(true);
    begin.setFocus(true);

    begin.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
      }
    });
    modal.add(begin);

    if (handler != null) {
      modal.addHiddenHandler(handler);
    }
    modal.show();
  }
}