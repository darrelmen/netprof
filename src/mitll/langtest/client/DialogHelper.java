package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;
import java.util.List;

public class DialogHelper {
  private boolean doYesAndNo;
  public interface CloseListener {
    void gotYes();
    void gotNo();
  }

  public DialogHelper(boolean doYesAndNo) {
    this.doYesAndNo = doYesAndNo;
  }

  public void showErrorMessage(String title, String msg) {
    List<String> msgs = new ArrayList<String>();
    msgs.add(msg);
    showErrorMessage(title, msgs);
  }

  public void showErrorMessage(String title, List<String> msgs) {
    showErrorMessage(title, msgs, "Close", null);
  }

  /**
   * Note : depends on bootstrap
   *
   * @param title
   * @param msgs
   */
  public void showErrorMessage(String title, List<String> msgs, String buttonName, final CloseListener listener) {
    final DialogBox dialogBox;
    Button closeButton;

    dialogBox = new DialogBox();
    dialogBox.setGlassEnabled(true);
    dialogBox.setHTML("<b>" + title + "</b>");

    closeButton = new Button(buttonName);
    closeButton.setType(ButtonType.PRIMARY);

    closeButton.getElement().setId("closeButton");
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
      row.add(new Column(4,closeButton));
      row.add(new Column(4,new Heading(4)));
      Button noButton = new Button("No");
      row.add(new Column(4,noButton));
      noButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          dialogBox.hide();
          if (listener != null) listener.gotNo();
        }
      });
    }
    else {
      row.add(new Column(2,6,closeButton));
    }
    container.add(row);

    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        if (listener != null) listener.gotYes();
      }
    });
    dialogBox.center();

    dialogBox.setWidget(container);
  }
}