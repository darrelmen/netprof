package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.PopupPanel;

/**
* Created by go22670 on 8/13/14.
*/
public class HidePopupTextBox extends TextBox {
  public void configure(final PopupPanel popup) {
    addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        int keyCode = event.getNativeEvent().getKeyCode();
        if (keyCode == KeyCodes.KEY_ENTER) {
         // System.out.println("HidePopupTextBox : got key press on " + getElement().getId());
          popup.hide();
          onEnter();
        }
      }
    });
  }
  protected void onEnter() {
  }
}
