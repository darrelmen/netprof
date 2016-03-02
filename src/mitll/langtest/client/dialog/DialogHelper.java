/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DialogHelper {
  private final boolean doYesAndNo;

  public interface CloseListener {
    void gotYes();

    void gotNo();
  }

  public DialogHelper(boolean doYesAndNo) {
    this.doYesAndNo = doYesAndNo;
  }

  public DialogHelper(String title, Collection<String> msgs, CloseListener listener) {
    this.doYesAndNo = true;
    show(title, msgs, "Yes", listener);
  }


  public void showErrorMessage(String title, String msg) {
    List<String> msgs = new ArrayList<String>();
    msgs.add(msg);
    showErrorMessage(title, msgs);
  }

  private void showErrorMessage(String title, Collection<String> msgs) {
    show(title, msgs, "Close", null);
  }

/*  public void show(String title, String msg, String buttonName, final CloseListener listener) {
    show(title, Collections.singletonList(msg), buttonName, listener);
  }*/

  public void show(String title, Collection<String> msgs, final CloseListener listener) {
    show(title, msgs, "Yes", listener);
  }

  /**
   * Note : depends on bootstrap
   *
   * @param title
   * @param msgs
   */
  private void show(String title, Collection<String> msgs, String buttonName, final CloseListener listener) {
    //final DialogBox dialogBox = new DialogBox();
    final Modal dialogBox = new Modal();
    Button closeButton;

    // dialogBox.setGlassEnabled(true);
    dialogBox.setTitle("<b>" + title + "</b>");

    closeButton = new Button(buttonName);
    closeButton.setType(ButtonType.PRIMARY);

    // closeButton.getElement().setId("closeButton");
    closeButton.setFocus(true);

    FluidContainer container = new FluidContainer();

    for (String msg : msgs) {
      FluidRow row = new FluidRow();
      Column column = new Column(12);
      row.add(column);
      Heading para = new Heading(4);
      para.setText(msg);

      column.add(para);
      container.add(row);
    }

    // add buttons
    FluidRow row = new FluidRow();
    if (doYesAndNo) {
      row.add(new Column(4, closeButton));
      row.add(new Column(4, new Heading(4)));
      Button noButton = new Button("No");
      noButton.setType(ButtonType.INVERSE);

      row.add(new Column(4, noButton));
      noButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          dialogBox.hide();
          if (listener != null) listener.gotNo();
        }
      });
    } else {
      row.add(new Column(2, 6, closeButton));
    }
    container.add(row);
    FluidRow w = new FluidRow();
    w.add(new Column(12, new Heading(6)));
    container.add(w);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        if (listener != null) listener.gotYes();
      }
    });

   /* dialogBox.setWidget(container);
    dialogBox.center();*/
    dialogBox.add(container);
    dialogBox.show();
  }
}