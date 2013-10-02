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

  /**
   * @see FlexSectionExerciseList#addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see FlexSectionExerciseList#addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
   * @see FlexSectionExerciseList#addColumnButton(com.google.gwt.user.client.ui.FlowPanel, String, ButtonGroupSectionWidget)
   *
   * @param row
   */
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
    return currentSelection;
  }

  public void clearSelectionState() { currentSelection = null; }

  /**
   * Look at the buttons to determine what has been clicked.
   *
   * @see #getCurrentSelection()
   * @return a comma separated list of selected units, chapters, or sections
   */
  private String getCurrentSelectionInternal() {
  //  System.out.println("ButtonGroupSectionWidget.getCurrentSelectionInternal for " + type + " checking " + buttons.size() + " buttons.");
    StringBuilder builder = new StringBuilder();
    Set<String> unique = new HashSet<String>();
    List<String> inOrder = new ArrayList<String>();
    for (Button b : selected) {
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

  /**
   * @see FlexSectionExerciseList#addClickHandlerToButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, String, ButtonGroupSectionWidget)
   * @param item
   * @param typeToBox
   */
/*  public void selectItem(String item, Map<String, SectionWidget> typeToBox) {
    System.out.println(this + " selectItem on " + item);
    selectItem(Collections.singleton(item), true, typeToBox);
  }*/

  /**
   * @seex #selectButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, boolean, java.util.Map)
   * @paramx button
   * @paramx doToggle
   * @paramx typeToBox
   */
/*  public void selectButton(FlexSectionExerciseList.ButtonWithChildren button, boolean doToggle, Map<String, SectionWidget> typeToBox) {
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
  }*/

  /**
   * @see FlexSectionExerciseList#addClickHandlerToButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, String, ButtonGroupSectionWidget) (String, java.util.Collection)
   *
   * @param sections
   */
  public void simpleSelectItem(Collection<String> sections) {
    clearSelectionState();
    if (sections.size() == 1 && sections.iterator().next().equals(SectionExerciseList.ANY)) {
      clearSelection();
    } else {
      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = nameToButton.get(toSelect);
        if (buttonsAtName == null) {
          System.err.println(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        }
        else {
          for (Button b : buttonsAtName) {
            toggleButton(true, b);
          }
        }
      }
    }
  }
    /**
     * @seex #selectItem(String, java.util.Map)
     * @see FlexSectionExerciseList#selectItem(String, java.util.Collection)
     * @param sections
     * @param typeToBox
     */
  //  public void selectItem(Collection<String> sections, boolean doToggle, Map<String,SectionWidget> typeToBox) {
    public void selectItem(Collection<String> sections, Map<String, SectionWidget> typeToBox) {
      if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this+
     		 " : " + type + "="+sections);

    if (sections.size() == 1 && sections.iterator().next().equals(SectionExerciseList.ANY)) {
      clearAll();
    } else {
      ButtonGroupSectionWidget childGroup = null;
      int count = 0;

      // clear existing selections when not toggling
     // if (!doToggle) {  // i.e. when called from restoreSelectionState...?
        clearExistingSelections(sections);
    //  }

      Set<Button> buttonsByName = getButtonsByName(sections);

      // get parent of each selected button
      // get siblings
      // get children of siblings
      // deselect children, recursivel

      for (Button b : buttonsByName) {
       // if (b.isActive()) {
        if (debug) System.out.println("selectItem " +  b);
        for (FlexSectionExerciseList.ButtonWithChildren sibling : ((FlexSectionExerciseList.ButtonWithChildren) b).getSiblings()) {

          if (debug) System.out.println("\tselectItem sibling " +  sibling);
          for (FlexSectionExerciseList.ButtonWithChildren toDisable : sibling.getButtonChildren()) {
            if (debug) System.out.println("\t\tselectItem sibling child " +  toDisable);

            disableButton(toDisable);
          }
        }
       // }
      }
        //
/*      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = nameToButton.get(toSelect);
        if (buttonsAtName == null) {
          System.err.println(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        }
        else {*/
          for (Button b : buttonsByName) {
            boolean active = toggleButton(false, b);
            if (active) count++;
            childGroup = enableChildrenButtons(typeToBox, (FlexSectionExerciseList.ButtonWithChildren) b, active);
          }
  //      }
    //  }
      if (count != sections.size() /*&& !doToggle*/) {
        if (debug) System.out.println("Note : selectItem " +
          //"doToggle " + /*doToggle + */
          " : " + type + "=" + sections + " (" + sections.size()+
          ") but only " + count + " selected");
      }

      if (childGroup != null) {
        childGroup.recurseShowEnabled();
      }
      else {
        if (debug) System.out.println("selectItem " +
          //"doToggle " + doToggle +
          " : " + type + "=" + sections + " (" + sections.size()+
          ") no child group.");
      }
      currentSelection = null;   // this just means we have to reset this on the next call to getCurrentSelection

      if (count > 0) {
        if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this+
          " row is selected since " + count + " items are selected.");
        for (Panel p : rows) p.addStyleName(color);
      }
   // boolean anythingSelected = isAnythingSelected();
   /*     if (didSelect && !anythingSelected) {
        System.err.println(">>>> selectItem " + type + "=" + sections + " but nothing selected?");
      } */
      setClearButtonState(count > 0);
    }
  }

  private void clearExistingSelections(Collection<String> sections) {
    Set<Button> toSelectSet = getButtonsByName(sections);
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

  private Set<Button> getButtonsByName(Collection<String> sections) {
    Set<Button> toSelectSet = new HashSet<Button>();
    for (String toSelect : sections) {
      Collection<Button> buttonsAtName = nameToButton.get(toSelect);

      toSelectSet.addAll(buttonsAtName);
    }
    return toSelectSet;
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
   * @see #selectItem(java.util.Collection, java.util.Map)
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
        System.out.println("\tenableChildrenButtons : " + this + " recurse " + childButton);

        enableChildrenButtons(typeToBox, childButton, active);
      }
    }
    return childGroup;
  }

  private void disableButton(FlexSectionExerciseList.ButtonWithChildren toDisable) {
    //disableButton(toDisable);
    ButtonGroupSectionWidget buttonGroup = toDisable.getButtonGroup();
    if (buttonGroup == null) {
      System.out.println("disableButton : " + this + " huh? " + toDisable + " has no group?");
    }
    else {
      buttonGroup.disableButtonInGroup(toDisable);
    }
    for (FlexSectionExerciseList.ButtonWithChildren child : toDisable.getButtonChildren()) {
      disableButton(child);
    }
  }

  private ButtonGroupSectionWidget getButtonGroup(Map<String, SectionWidget> typeToBox, FlexSectionExerciseList.ButtonWithChildren bb) {
    ButtonGroupSectionWidget childGroup;
    String typeOfChildren = bb.getTypeOfChildren();
    //System.out.println("getButtonGroup : " + this + " for " + bb + " type of children " + typeOfChildren);
    childGroup = (ButtonGroupSectionWidget) typeToBox.get(typeOfChildren);
    return childGroup;
  }

  /**
   * @see #enableChildrenButtons(java.util.Map, mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, boolean)
   * @param buttonChildren
   * @param isEnable
   */
  private void rememberEnabled(List<FlexSectionExerciseList.ButtonWithChildren> buttonChildren, boolean isEnable) {
    System.out.println(this + " rememberEnabled " + enabled.size() + " enabled, " + disabled.size() + " disabled children = " + buttonChildren + " : to enable = " + isEnable);

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

  private void disableButtonInGroup(Button button) {
    disabled.add(button);
    enabled.remove(button);
  }

  private void recurseShowEnabled() {
    showEnabled();
    if (getFirstButton() != null && getFirstButton().getButtonChildren() != null && !getFirstButton().getButtonChildren().isEmpty()) {
      FlexSectionExerciseList.ButtonWithChildren firstChild = getFirstButton().getButtonChildren().iterator().next();
      /*if (firstChild) {
        System.out.println("recurseShowEnabled : " + this + " recurse on " + getFirstButton().getButtonGroup() + " button " + getFirstButton());

      } else {*/
        System.out.println("recurseShowEnabled : " + this + " recurse on " + getFirstButton().getButtonGroup());

        firstChild.getButtonGroup().recurseShowEnabled();
      //}
    }
    else {
      System.out.println("\trecurseShowEnabled : " + this + " has no children.");
    }
  }

  /**
   * Clear selection or enable all buttons.
   *
   * @see #selectItem(java.util.Collection, java.util.Map)
   */
  private void clearAll() {
    clearSelection();
    currentSelection = null;

    System.out.println("clearAll : ---------> disable clear button for type " +type + " checking " +buttons.size() + " buttons <----------- ");

    clearButton.setEnabled(false);
    for (Panel p : rows) p.removeStyleName(color);

    enableAll();

    FlexSectionExerciseList.ButtonWithChildren next = (FlexSectionExerciseList.ButtonWithChildren) buttons.iterator().next();

    if (next.hasChildren()) {
      SectionWidget sectionWidget = typeToBox.get(next.getTypeOfChildren());
     // if (sectionWidget == this) System.err.println("\n\n\nwhy so complicated???");
      sectionWidget.enableAll();
    }
  }

  private void clearSelection() {
    for (Button b : selected) {
      b.setActive(false);
    }
    selected.clear();
  }

  /**
   * @see #clearAll()
   */
  public void enableAll() {
    System.out.println("----> enableAll for " + this);
    enabled.addAll(buttons);
    disabled.clear();
    showEnabled();
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
   * @seex #recurseShowEnabled(ButtonGroupSectionWidget)
   * @seex #selectButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, boolean, java.util.Map)
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

  public String toString() { return "Group " + type + " with " + buttons.size() + " buttons"; }
}
