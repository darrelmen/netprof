/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;

import java.util.*;

/**
 * A group of buttons that need to maintain
 * User: GO22670
 * Date: 4/8/13
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
class ButtonGroupSectionWidget implements SectionWidget {
  private Button clearButton;
  private final String type;

  private String currentSelection = null;
  private final List<Panel> rows = new ArrayList<Panel>();
  private final Set<Button> selected = new HashSet<Button>();
  private final ButtonContainer buttons = new ButtonContainer();
  private String color;
  private final boolean debug = false;

  public ButtonGroupSectionWidget(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  /**
   * @param row
   * @see FlexSectionExerciseList#addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see FlexSectionExerciseList#addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
   * @see FlexSectionExerciseList#addColumnButton(com.google.gwt.user.client.ui.FlowPanel, String, ButtonGroupSectionWidget)
   */
  public void addRow(Panel row) {
    this.rows.add(row);
  }

  /**
   * @param b
   * @see FlexSectionExerciseList#addColumnButton
   * @see FlexSectionExerciseList#makeSectionWidget
   * @see FlexSectionExerciseList#makeSubgroupButton
   */
  @Override
  public void addButton(Button b) {
    String name = b.getText().trim();
    if (name.equals(HistoryExerciseList.ANY)) {
      addClearButton(b);
    } else {
      this.buttons.add(b, name);
    }
  }

  @Override
  public void addLabel(Widget label, String color) {
    this.color = color;
  }

  private void addClearButton(Button b) {
    clearButton = b;
    clearButton.setEnabled(false);
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#getCurrentSelection(String)
   * @return
   */
  @Override
  public String getCurrentSelection() {
    if (currentSelection == null) {
      currentSelection = getCurrentSelectionInternal();
    }
    return currentSelection;
  }

  public void clearSelectionState() {
    currentSelection = null;
  }

  /**
   * Look at the buttons to determine what has been clicked.
   *
   * @return a comma separated list of selected units, chapters, or sections
   * @see #getCurrentSelection()
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
      return HistoryExerciseList.ANY;
    }
  }

  /**
   * @param sections
   * @see FlexSectionExerciseList#addClickHandlerToButton(mitll.langtest.client.bootstrap.FlexSectionExerciseList.ButtonWithChildren, String, ButtonGroupSectionWidget) (String, java.util.Collection)
   */
  public void simpleSelectItem(Collection<String> sections) {
    clearSelectionState();
    if (isClearSelection(sections)) {
      clearSelection();
    } else {
      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = buttons.getButtonsForName(toSelect);
        if (buttonsAtName == null) {
          System.err.println(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        } else {
          for (Button b : buttonsAtName) {
            toggleButton(true, b);
          }
        }
      }
    }
  }

  private boolean isClearSelection(Collection<String> sections) {
    return sections.size() == 1 && sections.iterator().next().equals(HistoryExerciseList.ANY);
  }

  /**
   * @param sections
   * @see FlexSectionExerciseList#selectItem(String, java.util.Collection)
   */
  public void selectItem(Collection<String> sections) {
    if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this + " : " + type + "=" + sections);

    if (isClearSelection(sections)) {
      clearAll();
    } else {

      clearExistingSelections(sections);

      Collection<Button> buttonsByName = buttons.getButtonsByName(sections);

      // recurseShowParentEnabled();

      // recurse down the tree, selecting as we go
      int count = 0;
      ButtonGroupSectionWidget childGroup = null;
      for (Button b : buttonsByName) {
        boolean active = toggleButton(false, b);
        if (active) count++;
        FlexSectionExerciseList.ButtonWithChildren buttonWithChildren = (FlexSectionExerciseList.ButtonWithChildren) b;
        if (buttonWithChildren.isEnabled() && hasEnabledPathToParent(buttonWithChildren)) {
          childGroup = enableChildrenButtons(buttonWithChildren, active);
        }
      }
      if (count != sections.size()) {
        if (debug) System.out.println("Note : selectItem " +
          " : " + type + "=" + sections + " (" + sections.size() +
          ") but only " + count + " selected");
      }
      // disableChildrenOfSiblings(buttonsByName);

      if (childGroup != null) {
        childGroup.recurseShowEnabled();
      } else {
        if (debug) System.out.println("selectItem " +
          " : " + type + "=" + sections + " (" + sections.size() +
          ") no child group.");
      }
      currentSelection = null;   // this just means we have to reset this on the next call to getCurrentSelection

      if (count > 0) {
        if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this +
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

  private boolean hasEnabledPathToParent(FlexSectionExerciseList.ButtonWithChildren buttonWithChildren) {
    boolean enabledPath = true;
    FlexSectionExerciseList.ButtonWithChildren parent = buttonWithChildren.getParentButton();
    if (parent != null) {
      enabledPath = parent.isEnabled();
      if (enabledPath) {
        enabledPath = hasEnabledPathToParent(parent);
      }
    }
    return enabledPath;
  }

  /**
   * @see #selectItem(Collection)
   * @param sections
   */
  private void clearExistingSelections(Collection<String> sections) {
    Collection<Button> toSelectSet = buttons.getButtonsByName(sections);
    for (Button unselectCandidate : buttons.getButtons()) {
      if (!toSelectSet.contains(unselectCandidate)) {
        if (unselectCandidate.isActive()) {
          if (debug) System.out.println("ButtonGroupSectionWidget: unselecting " + unselectCandidate.getText());

          unselectCandidate.setActive(false);
          selected.remove(unselectCandidate);
        }
      }
    }
  }

  /**
   * @param doToggle
   * @param b
   * @return
   * @see #selectItem(java.util.Collection)
   */
  private boolean toggleButton(boolean doToggle, Button b) {
    boolean active = !doToggle || !b.isActive();
    b.setActive(active);
    if (active) selected.add(b);
    else selected.remove(b);
    //   if (active) System.out.println("\ttoggleButton " + b.getText() + " is active");
    //  else  System.out.println("\t\ttoggleButton " + b.getText() + " is inactive");

    return active;
  }

  /**
   * @param bb
   * @param active
   * @return
   * @paramx typeToBox
   * @see #selectItem(java.util.Collection)
   */
  private ButtonGroupSectionWidget enableChildrenButtons(FlexSectionExerciseList.ButtonWithChildren bb, boolean active) {
    ButtonGroupSectionWidget childGroup = null;
    List<FlexSectionExerciseList.ButtonWithChildren> buttonChildren = bb.getButtonChildren();
    if (!buttonChildren.isEmpty()) {
      childGroup = buttonChildren.iterator().next().getButtonGroup();
      if (debug) System.out.println("enableChildrenButtons : " + this + " child group " + childGroup + " and recursing over " + buttonChildren.size() + " children");
      childGroup.buttons.rememberEnabled(buttonChildren, active);

      // recurse!
      for (FlexSectionExerciseList.ButtonWithChildren childButton : buttonChildren) {
        if (debug) System.out.println("\tenableChildrenButtons : " + this + " recurse " + childButton);

        enableChildrenButtons(childButton, active);
      }
    } else {
      // System.out.println("enableChildrenButtons : " + this + " no children of " + bb);
    }
    return childGroup;
  }

  private void recurseShowEnabled() {
    buttons.showEnabled();
    FlexSectionExerciseList.ButtonWithChildren firstButton = buttons.getFirstButton();
    if (firstButton != null && firstButton.getButtonChildren() != null && !firstButton.getButtonChildren().isEmpty()) {
      FlexSectionExerciseList.ButtonWithChildren firstChild = firstButton.getButtonChildren().iterator().next();
      System.out.println("recurseShowEnabled : " + this + " recurse on " + firstButton.getButtonGroup());

      firstChild.getButtonGroup().recurseShowEnabled();
    }
  }

  /**
   * Clear selection or enable all buttons.
   *
   * @see #selectItem(java.util.Collection)
   */
  public void clearAll() {
    //System.out.println("clearAll : ---------> disable clear button for type " + type + " checking " + buttons + " buttons <----------- ");
    // manager the selected set
    clearSelection();
    currentSelection = null;

    // set the clear button state
    clearButton.setEnabled(false);
    // color the row to show no selection
    for (Panel p : rows) p.removeStyleName(color);

    // show all buttons enabled
    enableAll();
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
    buttons.enableAll();
  }

  private void setClearButtonState(boolean anythingSelected) {
    if (clearButton != null) {
      clearButton.setEnabled(anythingSelected);
      if (!anythingSelected) {
        System.out.println("\tsetClearButtonState  : selectItem clear color");
        for (Panel p : rows) p.removeStyleName(color);
      }
    } else {
      System.err.println("clear button is not set? ");
    }
  }

  /**
   * @see FlexSectionExerciseList#clearEnabled(String)
   */
  public void clearEnabled() {
    buttons.clearEnabled();
  }

  public void selectItem(Collection<String> section, boolean doToggle) {
    throw new IllegalArgumentException("don't call me!");
  }

  public String toString() {
    return "Group " + type + " with " + buttons;
  }
}
