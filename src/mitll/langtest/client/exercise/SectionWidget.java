package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 4/3/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SectionWidget {
  String getCurrentSelection();
  void clearSelectionState();

  void selectItem(Collection<String> section, boolean doToggle);

  void clearAll();
  void enableAll();

  void addButton(Button b);
  void addLabel(Widget label, String color);
}
