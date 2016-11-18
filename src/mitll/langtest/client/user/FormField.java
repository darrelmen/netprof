package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 11/18/16.
 */
public class FormField {
  public final TextBoxBase box;
  public final ControlGroup group;

  public FormField(final TextBoxBase box, final ControlGroup group, final int minLength) {
    this.box = box;

    box.addKeyUpHandler(event -> {
      if (box.getText().length() >= minLength) {
        group.setType(ControlGroupType.NONE);
      }
    });

    this.group = group;
  }

  public void setVisible(boolean visible) {
    group.setVisible(visible);
  }

  public String getSafeText() {
    return sanitize(box.getText());
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  public boolean isEmpty() {
    return getSafeText().isEmpty();
  }

/*
  void setRightSide(Widget rightSide) {
    Widget rightSide1 = rightSide;
  }
*/

  public ControlGroup getGroup() {
    return group;
  }

  public FocusWidget getWidget() {
    return box;
  }

  public String toString() {
    return "FormField value " + getSafeText();
  }
}
