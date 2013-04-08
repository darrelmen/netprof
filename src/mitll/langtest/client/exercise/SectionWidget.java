package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 4/3/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SectionWidget {
  String getCurrentSelection();
  String getFirstItem();
  void selectFirstAfterAny();
  void selectItem(String section, boolean doToggle);
  void retainCurrentSelectionState(String currentSelection);
  void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount);
  public Widget getWidget();

  void enableInSet(Collection<String> inSet);

  void enableAll();
}
