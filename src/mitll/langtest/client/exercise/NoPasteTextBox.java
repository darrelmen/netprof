package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Stops the user from cut-copy-paste into the text box.
 * <p></p>
 * Prevents googling for answers.
 */
class NoPasteTextBox extends TextBox {
  public NoPasteTextBox() {
    sinkEvents(Event.ONPASTE);
  }
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    if (event.getTypeInt() == Event.ONPASTE) {
      event.stopPropagation();
      event.preventDefault();
    }
  }
}
