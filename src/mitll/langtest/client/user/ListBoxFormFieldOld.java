package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ListBox;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/2/13
* Time: 7:08 PM
* To change this template use File | Settings | File Templates.
*/
public class ListBoxFormFieldOld {
  public final ListBox box;

  public ListBoxFormFieldOld(ListBox box) {
    this.box = box;
  }

  public String getValue() {
    return box.getItemText(box.getSelectedIndex());
  }

  public String toString() { return "Box: "+ getValue(); }
}
