package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Bar;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.StackProgressBar;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
public class StackedBarSectionWidget extends StackProgressBar implements SectionWidget {
  private static final String ANY = SectionExerciseList.ANY;
  private List<Bar> buttons = new ArrayList<Bar>();
  private ItemClickListener listener;
  private String type;
  String currentSelection;
  List<String> items = new ArrayList<String>();

  public StackedBarSectionWidget(String type, ItemClickListener listener) {
    this.type = type;
    this.listener = listener;
  //  setWidth("80%");
    //setStyleName(Constants.BTN_GROUP);

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

  @Override
  public String getCurrentSelection() {
    return currentSelection;
/*    for (Bar b : buttons) {
      if (b.isActive()) {
        //System.out.println("getCurrentSelection for " +type + "=" + b.getText());
        return b.getText().trim();
      }
    }
    return ANY;*/
  }

  @Override
  public String getFirstItem() {
    return items.get(0);
   // return buttons.iterator().next().getText().trim();
  }

/*  @Override
  public void selectFirstAfterAny() {
    currentSelection = items.get(1);
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    buttons.get(1).setActive(true);
  }*/

  @Override
  public void selectItem(Collection<String> section, boolean doToggle) {
    //currentSelection = section;
/*    for (Button b : buttons) {
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
    }*/
  }
/*

  @Override
  public void retainCurrentSelectionState(Collection<String> currentSelection) {
*/
/*    for (Button b : buttons) {
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
    if (!found) buttons.iterator().next().setActive(true);*//*

  }
*/

  /**
   * @see mitll.langtest.client.exercise.SectionExerciseList#populateListBox(mitll.langtest.client.exercise.SectionWidget, java.util.Map)
   * @param items
   * @param sectionToCount
   */
  @Override
  public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {
    System.out.println("populateTypeWidget : type = " +type + " with " +items.size() +" items.");
    this.items.clear();
    this.items.add(ANY);
    this.items.addAll(items);
    //selectFirstAfterAny();
    clear();
    Widget widgets = addButton(ANY);
    //widgets.setWidth("5%");

    int total = 0;
    for (Integer count : sectionToCount.values()) total += count;
    for (String item : items) {
      item = item.trim();
      Bar button = addButton(item);
      Integer count = sectionToCount.get(item);
      float ratio = (float) count / (float) total;
      int percent = Math.round(ratio * 100f);
      System.out.println("percent = " +percent);
      button.setPercent(percent);
    }
  }

  private Bar addButton(final String item) {
    MyBar widget = new MyBar();
    widget.setColor(Bar.Color.SUCCESS);
    widget.setText(item);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("got click on " + item);
       // selectItem(item, false);
        listener.gotClick(type, item);
      }
    });

    add(widget);
    // widget.setType(ButtonType.PRIMARY);
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

  private class MyBar extends Bar {
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }
  }
}
