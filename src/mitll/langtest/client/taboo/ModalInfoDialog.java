package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import java.util.Collection;
import java.util.Collections;

public class ModalInfoDialog {
  public ModalInfoDialog(String title, String message) {
    this(title, Collections.singleton(message));
  }

  public ModalInfoDialog(String title, Collection<String> message) {
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
        // langTest.setTabooFactory(userID, isGiver, false);
      }
    });
    modal.add(begin);

    modal.show();
  }
}