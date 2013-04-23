package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
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
public class ListBoxSectionWidget extends ListBox implements SectionWidget {
  public String getCurrentSelection() {
    int selectedIndex = getSelectedIndex();
    return getItemText(selectedIndex);
  }

  @Override
  public void addButton(Button b) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void addLabel(Widget label, String color) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void addLabel(Widget label) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getFirstItem() {
    return getItemText(0);  // first is Any
  }

  public void selectFirstAfterAny() {
    setSelectedIndex(1); // not any, which is the first list item
  }

  public void selectItem(Collection<String> section, boolean doToggle) {

    for (int i = 0; i < getItemCount(); i++) {
      String itemText = getItemText(i);
      if (itemText.equals(section)) {
        setSelectedIndex(i);
        break;
      }
    }
  }

  /**
   * Don't change the selection unless it's not available in this list box.
   * @seex SectionExerciseList#populateListBox(String, java.util.Collection)
   * @param currentSelection
   */
  public void retainCurrentSelectionState( Collection<String> currentSelection) {
    int itemCount = getItemCount();

    // retain current selection state
    if (itemCount > 0) {
      boolean foundMatch = false;
      for (int i = 0; i < itemCount; i++) {
        if (getItemText(i).equals(currentSelection)) {
          setSelectedIndex(i);
          foundMatch = true;
          break;
        }
      }
      if (!foundMatch) {
        setSelectedIndex(0);
      }
    }
  }


  public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {
    clear();

    addItem(SectionExerciseList.ANY);
    for (String section : items) {
      addItem(section);
    }
  }

  @Override
  public Widget getWidget() {
    return this;
  }

  @Override
  public void enableInSet(Collection<String> inSet) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void enableAll() {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
