/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.user.client.Event;

/**
 * Stops the user from cut-copy-paste into the text box.
 * <p></p>
 * Prevents googling for answers.
 */
public class NoPasteTextBox extends TextBox {
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
