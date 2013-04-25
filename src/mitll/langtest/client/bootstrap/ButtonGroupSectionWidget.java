package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* A group of buttons that need to maintain
* User: GO22670
* Date: 4/8/13
* Time: 11:19 AM
* To change this template use File | Settings | File Templates.
*/
class ButtonGroupSectionWidget implements SectionWidget {
  private List<Button> buttons = new ArrayList<Button>();
  private Button clearButton;
  private String type;
  private Widget label;
  private Map<String, Collection<Button>> nameToButton = new HashMap<String,Collection<Button>>();
  private String currentSelection = null;
  private List<Panel> rows = new ArrayList<Panel>();

  public ButtonGroupSectionWidget(String type) {
    this.type = type;
  }

  public void addRow(Panel row) { this.rows.add(row); }

  /**
   * @see FlexSectionExerciseList#addColumnButton
   * @see FlexSectionExerciseList#makeSectionWidget
   * @see FlexSectionExerciseList#makeSubgroupButton
   * @param b
   */
  @Override
  public void addButton(Button b) {
    String name = b.getText().trim();
    if (name.equals(SectionExerciseList.ANY)) {
      addClearButton(b);
    } else {
      this.buttons.add(b);
      Collection<Button> buttonsAtName = nameToButton.get(name);
      if (buttonsAtName == null) {
        nameToButton.put(name, buttonsAtName = new ArrayList<Button>());
      }
      buttonsAtName.add(b);
    }
  }

  @Override
  public void addLabel(Widget label, String color) {
    this.label = label;
    System.out.println("label is " + label + " color " +color);
    this.color = color;
  }

  String color;

  private void addClearButton(Button b) {
    clearButton = b;
    clearButton.setEnabled(false);
  }

  @Override
  public String getCurrentSelection() {
    if (currentSelection == null) {
      currentSelection = getCurrentSelectionInternal();
    }
    return currentSelection;
  }

  private String getCurrentSelectionInternal() {
    System.out.println("getCurrentSelection for " + type + " checking " + buttons.size() + " buttons.");
    StringBuilder builder = new StringBuilder();
    Set<String> unique = new HashSet<String>();
    List<String> inOrder = new ArrayList<String>();
    for (Button b : buttons) {
      if (b.isActive()) {
        String name = b.getText().trim();
        if (!unique.contains(name)) {
          unique.add(name);
          inOrder.add(name);
        }
      }
    }
    for (String name : inOrder) {
      builder.append(name);
      builder.append(",");
    }
    if (builder.length() > 0) {
      return builder.toString();
    } else {
      return SectionExerciseList.ANY;
    }
  }

/*  private boolean isAnythingSelected() {
    for (Button b : buttons) if (b.isActive()) return true;
    return false;
  }*/

  /**
   * @see FlexSectionExerciseList#populateListBoxAfterSelection(java.util.Map)
   * @param inSet
   */
  @Override
  public void enableInSet(Collection<String> inSet) {
    System.out.println("enableInSet for " + type + " : " + inSet);

    for (Button b : buttons) {
      String trim = b.getText().trim();
      b.setEnabled(inSet.contains(trim));
    }
  }

  /**
   * @see FlexSectionExerciseList#populateListBoxAfterSelection(java.util.Map)
   */
  @Override
  public void enableAll() {
    System.out.println("enableAll for " + type);

    for (Button b : buttons) {
      b.setEnabled(true);
    }
  }

  @Override
  public String getFirstItem() {
    return buttons.iterator().next().getText().trim();
  }

  @Override
  public void selectItem(Collection<String> sections, boolean doToggle) {
    System.out.println("selectItem " + type + "="+sections);

    if (sections.size() == 1 && sections.iterator().next().equals(SectionExerciseList.ANY)) {
      clearAll();
    } else {
      boolean anythingSelected = false;
      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = nameToButton.get(toSelect);
        if (buttonsAtName == null) {
          System.err.println(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        }
        else {
          for (Button b : buttonsAtName) {
            boolean active = !doToggle || !b.isActive();
            b.setActive(active);
            //String backgroundColor = DOM.getElementAttribute(b.getElement(), "backgroundColor");
         /*   Element element = label.getElement();
            if (element != null) {
              DOM.setElementAttribute(element, "background-color", color);
            }*/
            if (label != null) {
//              label.addStyleName(color);

              for (Panel p : rows) p.addStyleName(color);
            }

            anythingSelected |= active;
          }
        }
      }
      currentSelection = null;

   // boolean anythingSelected = isAnythingSelected();
   /*     if (didSelect && !anythingSelected) {
        System.err.println(">>>> selectItem " + type + "=" + sections + " but nothing selected?");
      }
*/
      setClearButtonState(sections, anythingSelected);
    }
  }

  private void clearAll() {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
      }
    }
    currentSelection = null;

    System.out.println("disable clear button for type " +type + " checking " +buttons.size() + " buttons");

    clearButton.setEnabled(false);
//    label.removeStyleName(color);
    for (Panel p : rows) p.removeStyleName(color);


  }

  private void setClearButtonState(Collection<String> sections, boolean anythingSelected) {
    if (clearButton != null) {
      System.out.println("\tselectItem for type " + type + "=" + sections + " set clear button.");
      clearButton.setEnabled(anythingSelected);
      if (!anythingSelected) {
        System.out.println("\tselectItem clear color");

        for (Panel p : rows) p.removeStyleName(color);
      }

    }
    else {
      System.err.println("clear button is not set? ");
    }
  }

  @Override
  public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {}
  @Override
  public Widget getWidget() {  return null; }
}
