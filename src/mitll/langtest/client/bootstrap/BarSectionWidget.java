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
  @Override
  public String getCurrentSelection() {
    for (Button b : buttons) {
      if (b.isActive()) {
        return b.getText().trim();
      }
    }
    return ANY;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public String getFirstItem() {
    return buttons.iterator().next().getText().trim();
  }

  @Override
  public void selectFirstAfterAny() {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    buttons.get(1).setActive(true);
  }

  @Override
  public void selectItem(String section) {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    for (Button b : buttons) {
      if (b.getText().equals(section)) {
        b.setActive(true);
        break;
      }
    }
  }

  @Override
  public void retainCurrentSelectionState(String currentSelection) {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    boolean found = false;
    for (Button b : buttons) {
      if (b.getText().equals(currentSelection)) {
        b.setActive(true);
        found = true;
        break;
      }
    }
    if (!found) buttons.iterator().next().setActive(true);
  }

  @Override
  public void populateTypeWidget(Collection<String> items) {
    System.out.println("populateTypeWidget : " +items.size());
    buttons.clear();
    clear();
    addButton(ANY);
    for (final String item : items) {
      addButton(item);
    }
  }

  private void addButton(final String item) {
    Button widget = new Button(item, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("got click on " + item);
      }
    });
    add(widget);
    widget.setType(ButtonType.PRIMARY);
    buttons.add(widget);
  }

  @Override
  public Widget getWidget() {
    return this;
  }
}
