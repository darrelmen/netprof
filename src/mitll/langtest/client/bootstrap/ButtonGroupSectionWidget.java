package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

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

  private Map<String, Collection<Button>> nameToButton = new HashMap<String, Collection<Button>>();
  private String currentSelection = null;
  private List<Panel> rows = new ArrayList<Panel>();
  private Set<Button> enabled = new HashSet<Button>();
  private Set<Button> disabled = new HashSet<Button>();
  private Set<Button> selected = new HashSet<Button>();
  private Map<String, SectionWidget> typeToBox;
  private boolean debug = true;

  public ButtonGroupSectionWidget(String type, Map<String,SectionWidget> typeToBox) {
    this.type = type;
    this.typeToBox = typeToBox;
  }

  public String getType() {return type;}

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
      enabled.add(b);
    }
  }

  @Override
  public void addLabel(Widget label, String color) {
    this.color = color;
  }

  private String color;

  private void addClearButton(Button b) {
    clearButton = b;
    clearButton.setEnabled(false);
  }

  /**
   * @see SectionExerciseList#getCurrentSelection(String)
   * @return
   */
  @Override
  public String getCurrentSelection() {
    if (currentSelection == null) {
      currentSelection = getCurrentSelectionInternal();
    }
    else {
     // System.out.println("ButtonGroupSectionWidget returning cached selection state...");
    }
    return currentSelection;
  }

  public void clearSelectionState() { currentSelection = null; }

  /**
   * @see #getCurrentSelection()
   * @return a comma separated list of selected units, chapters, or sections
   */
  private String getCurrentSelectionInternal() {
  //  System.out.println("ButtonGroupSectionWidget.getCurrentSelectionInternal for " + type + " checking " + buttons.size() + " buttons.");
    StringBuilder builder = new StringBuilder();
    Set<String> unique = new HashSet<String>();
    List<String> inOrder = new ArrayList<String>();
    for (Button b : buttons) {
      if (b.isActive()) {
        String name = b.getText().trim();
    //    System.out.println("\tButtonGroupSectionWidget.getCurrentSelectionInternal button " +name + " is active!");

        if (!unique.contains(name)) {
          unique.add(name);
          inOrder.add(name);
        }
      }
      else {
     //   System.out.println("\tButtonGroupSectionWidget.getCurrentSelectionInternal button " + b.getText() + " is inactive!");
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

  /**
   * @see #clearAll()
   */
  public void enableAll() {
    System.out.println("enableAll for " + this);
    enabled.addAll(buttons);
    disabled.clear();
    showEnabled();
  }

  @Override
  public String getFirstItem() {
    return getFirstButton().getText().trim();
  }

  private FlexSectionExerciseList.ButtonWithChildren getFirstButton() {
    if (buttons.isEmpty()) return null;
    return (FlexSectionExerciseList.ButtonWithChildren) buttons.iterator().next();
  }

  @Override
  public void selectItem(Collection<String> section, boolean doToggle) {
    throw new IllegalArgumentException("don't call me!");
  }

  public void selectItem(String item, Map<String, SectionWidget> typeToBox) {
    List<String> selections = new ArrayList<String>();
    System.out.println("selectItem on " + item);
    selections.add(item);
    selectItem(selections, true, typeToBox);
  }

  /**
   * Partially implemented -- maybe return to this later
   * @param button
   * @param doToggle
   * @param typeToBox
   */
  public void selectButton(FlexSectionExerciseList.ButtonWithChildren button, boolean doToggle, Map<String, SectionWidget> typeToBox) {
    boolean active = toggleButton(doToggle, button);
  //  System.out.println("selectButton on " + button);

    // enable the children
    ButtonGroupSectionWidget childGroup = enableChildrenButtons(typeToBox, button, active);

    if (childGroup != null) {
      childGroup.showEnabled();
    }
    currentSelection = null;   // this just means we have to reset this on the next call to getCurrentSelection

    // have the row show that there are selections
    if (active) {
      for (Panel p : rows) p.addStyleName(color);
    }

    // make sure the clear button is consistent
    setClearButtonState(active);

    // recurse!
    // select path to root...

    FlexSectionExerciseList.ButtonWithChildren parentButton = button.getParentButton();
    if (parentButton != null) {
      System.out.println("recurse on " + parentButton);
      parentButton.getButtonGroup().selectButton(parentButton, doToggle, typeToBox);
    }
  }

  /**
   * @see FlexSectionExerciseList#addClickHandlerToButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, String, ButtonGroupSectionWidget)
   * @see FlexSectionExerciseList#selectItem(String, java.util.Collection)
   * @param sections
   * @param doToggle
   * @param typeToBox
   */
  public void selectItem(Collection<String> sections, boolean doToggle, Map<String,SectionWidget> typeToBox) {
     if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this+
       " toggle = " + doToggle +
     		 " : " + type + "="+sections);

    if (sections.size() == 1 && sections.iterator().next().equals(SectionExerciseList.ANY)) {
      clearAll();
    } else {
      boolean anythingSelected = false;
      ButtonGroupSectionWidget childGroup = null;
      int count = 0;

      // clear existing selections when not toggling
      if (!doToggle) {
        clearExistingSelections(sections);
      }

      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = nameToButton.get(toSelect);
        if (buttonsAtName == null) {
          System.err.println(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        }
        else {
          for (Button b : buttonsAtName) {
            boolean active = toggleButton(doToggle, b);
            anythingSelected |= active;
            if (active) count++;
            childGroup = enableChildrenButtons(typeToBox, (FlexSectionExerciseList.ButtonWithChildren) b, active);
          }
        }
      }
      if (count != sections.size() && !doToggle) {
        System.out.println("Note : selectItem doToggle " + doToggle + " : " + type + "=" + sections + " (" + sections.size()+
          ") but only " + count + " selected");
      }

      if (childGroup != null) {
        recurseShowEnabled(childGroup);
      }
      currentSelection = null;   // this just means we have to reset this on the next call to getCurrentSelection

      if (anythingSelected) {
        for (Panel p : rows) p.addStyleName(color);
      }
   // boolean anythingSelected = isAnythingSelected();
   /*     if (didSelect && !anythingSelected) {
        System.err.println(">>>> selectItem " + type + "=" + sections + " but nothing selected?");
      }
*/
      setClearButtonState(anythingSelected);
    }
  }

  private void clearExistingSelections(Collection<String> sections) {
    Set<Button> toSelectSet = new HashSet<Button>();
    for (String toSelect : sections) {
      Collection<Button> buttonsAtName = nameToButton.get(toSelect);

      toSelectSet.addAll(buttonsAtName);
    }
    for (Collection<Button> allButtonsAtName : nameToButton.values()) {
      for (Button unselectCandidate : allButtonsAtName) {
        if (!toSelectSet.contains(unselectCandidate)) {
          if (unselectCandidate.isActive()) {
            if (debug) System.out.println("ButtonGroupSectionWidget: unselecting " + unselectCandidate.getText());

            unselectCandidate.setActive(false);
            selected.remove(unselectCandidate);
          }
        }
      }
    }
  }

  private boolean toggleButton(boolean doToggle, Button b) {
    boolean active = !doToggle || !b.isActive();
    b.setActive(active);
    if (active) selected.add(b); else selected.remove(b);
 //   if (active) System.out.println("\ttoggleButton " + b.getText() + " is active");
  //  else  System.out.println("\t\ttoggleButton " + b.getText() + " is inactive");

    return active;
  }

  /**
   * @see #selectButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, boolean, java.util.Map)
   * @see #selectItem(java.util.Collection, boolean, java.util.Map)
   * @param typeToBox
   * @param bb
   * @param active
   * @return
   */
  private ButtonGroupSectionWidget enableChildrenButtons(Map<String, SectionWidget> typeToBox,
                                                         FlexSectionExerciseList.ButtonWithChildren bb, boolean active) {
    ButtonGroupSectionWidget childGroup = null;
    List<FlexSectionExerciseList.ButtonWithChildren> buttonChildren = bb.getButtonChildren();
    if (!buttonChildren.isEmpty()) {
      childGroup = getButtonGroup(typeToBox, bb);
      System.out.println("enableChildrenButtons : " + this + " child group " + childGroup + " and recursing over " + buttonChildren.size() + " children");

      childGroup.rememberEnabled(buttonChildren, active);

      // recurse!
      for (FlexSectionExerciseList.ButtonWithChildren childButton : buttonChildren) {
        enableChildrenButtons(typeToBox, childButton, active);
      }
    }
    return childGroup;
  }

  private ButtonGroupSectionWidget getButtonGroup(Map<String, SectionWidget> typeToBox, FlexSectionExerciseList.ButtonWithChildren bb) {
    ButtonGroupSectionWidget childGroup;
    String typeOfChildren = bb.getTypeOfChildren();
    System.out.println("enableChildrenButtons : " + this + " for " + bb + " type of children " + typeOfChildren);
    childGroup = (ButtonGroupSectionWidget) typeToBox.get(typeOfChildren);
    return childGroup;
  }

  private void recurseShowEnabled(ButtonGroupSectionWidget childGroup) {
    childGroup.showEnabled();
    if (childGroup.getFirstButton() != null && childGroup.getFirstButton().getButtonGroup() != null) {
      System.out.println("recurseShowEnabled : " + this + " recurse on " + childGroup.getFirstButton().getButtonGroup());

      recurseShowEnabled(childGroup.getFirstButton().getButtonGroup());
    }
  }

  /**
   * Clear selection or enable all buttons.
   *
   * @see #selectItem(java.util.Collection, boolean, java.util.Map)
   */
  private void clearAll() {
  /*  if (disabled.isEmpty()) {
      System.out.println("clearAll : " + this + " skipping clear since no buttons disabled.");
      return;
    }*/
    for (Button b : selected) {
      b.setActive(false);
    }
    selected.clear();
 /*   for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
      }
    }*/
    currentSelection = null;

    System.out.println("clearAll : ---------> disable clear button for type " +type + " checking " +buttons.size() + " buttons <----------- ");

    clearButton.setEnabled(false);
    for (Panel p : rows) p.removeStyleName(color);

    disabled.clear();
    enabled.addAll(buttons);

    showEnabled();

    FlexSectionExerciseList.ButtonWithChildren next = (FlexSectionExerciseList.ButtonWithChildren) buttons.iterator().next();

    if (next.hasChildren()) {
      typeToBox.get(next.getTypeOfChildren()).enableAll();
    }
  }

  private void setClearButtonState(boolean anythingSelected) {
    if (clearButton != null) {
      clearButton.setEnabled(anythingSelected);
      if (!anythingSelected) {
        System.out.println("\tsetClearButtonState  : selectItem clear color");
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

  /**
   * @see #clearAll()
   * @see #enableAll()
   * @see #recurseShowEnabled(ButtonGroupSectionWidget)
   * @see #selectButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, boolean, java.util.Map)
   */
  private void showEnabled() {
    System.out.println(this + " : showEnabled " + enabled.size() + " enabled, " + disabled.size() + " disabled");

    for (Button b : enabled) {
      b.setEnabled(true);
    }
    for (Button b : disabled) {
      b.setEnabled(false);
    }
  }

  private void rememberEnabled(List<FlexSectionExerciseList.ButtonWithChildren> buttonChildren, boolean isEnable) {
    System.out.println(this + " rememberEnabled " + enabled.size() + " enabled, " + disabled.size() + " disabled " + buttonChildren + " : to enable = " + isEnable);

    if (isEnable) {
      if (enabled.size() == buttons.size()) {
        enabled.clear();
      }
      enabled.addAll(buttonChildren);
    }
    else {
      enabled.removeAll(buttonChildren);
    }
    if (enabled.isEmpty()) enabled.addAll(buttons);
    disabled.clear();
    disabled.addAll(buttons);
    disabled.removeAll(enabled);
    System.out.println(this + " rememberEnabled now " + enabled.size() + " enabled, " + disabled.size() + " disabled");
  }

  public String toString() { return "Group " + type + " with " + buttons.size() + " buttons"; }
}
