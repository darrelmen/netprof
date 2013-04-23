package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 4/3/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class BarSectionWidget extends ButtonGroup implements SectionWidget {
  private static final String ANY = SectionExerciseList.ANY;
  private List<Button> buttons = new ArrayList<Button>();
  private ItemClickListener listener;
  private String type;

  public BarSectionWidget(String type, ItemClickListener listener) {
    this.type = type;
    this.listener = listener;
  //  setWidth("80%");
    addStyleName("floatLeft");
  }

  @Override
  public String getCurrentSelection() {
    for (Button b : buttons) {
      if (b.isActive()) {
        //System.out.println("getCurrentSelection for " +type + "=" + b.getText());
        return b.getText().trim();
      }
    }
    return ANY;
  }

  @Override
  public String getFirstItem() {
    return buttons.iterator().next().getText().trim();
  }

/*  @Override
  public void selectFirstAfterAny() {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    buttons.get(1).setActive(true);
  }
*/
  @Override
  public void selectItem(Collection<String> section, boolean doToggle) {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    for (Button b : buttons) {
      if (b.getText().trim().equals(section)) {
        b.setActive(true);
        break;
      }
    }
  }

/*  @Override
  public void retainCurrentSelectionState(Collection<String> currentSelection) {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    boolean found = false;
    for (Button b : buttons) {
      if (b.getText().trim().equals(currentSelection)) {
        b.setActive(true);
        found = true;
        break;
      }
    }
    if (!found) buttons.iterator().next().setActive(true);
  }*/

  /**
   * @see SectionExerciseList#populateListBox(mitll.langtest.client.exercise.SectionWidget, java.util.Map)
   * @param items
   * @param sectionToCount
   */
  @Override
  public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {
    System.out.println("populateTypeWidget : type = " +type + " with " +items.size() +" items.");
    buttons.clear();
    clear();
    Button widgets = addButton(ANY);
    //widgets.setWidth("5%");

    int total = 0;
    for (Integer count : sectionToCount.values()) total += count;
    for (String item : items) {
      item = item.trim();
      Button button = addButton(item);
      Integer count = sectionToCount.get(item);
      float ratio = (float) count / (float) total;
      int i = Math.round (ratio * 100f);
      button.setWidth(i + "%");
    }
  }

  private Button addButton(final String item) {
    Button widget = new Button(item, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("got click on " + item);
       // selectItem(item, false);
        listener.gotClick(type, item);
      }
    });
    add(widget);
    widget.setType(ButtonType.PRIMARY);
    buttons.add(widget);
    return widget;
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

/*  @Override
  public void addClearButton(Button b) {
    //To change body of implemented methods use File | Settings | File Templates.
  }*/
}
