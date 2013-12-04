package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/2/13
* Time: 7:07 PM
* To change this template use File | Settings | File Templates.
*/
public class FormFieldOld {
  public final TextBox box;
  public final ControlGroup group;

  public FormFieldOld(final TextBox box, final ControlGroup group) {
    this.box = box;

    box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (box.getText().length() > 0) {
          group.setType(ControlGroupType.NONE);
        }
      }
    });

    this.group = group;
  }

  public String getText() { return box.getText(); }
  public String toString() { return "Form : " + box.getText(); }
}
