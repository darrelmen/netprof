package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.FocusWidget;

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

  public void setText(String text) {
    box.setText(text);
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  public boolean isEmpty() {
    return getSafeText().isEmpty();
  }

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
