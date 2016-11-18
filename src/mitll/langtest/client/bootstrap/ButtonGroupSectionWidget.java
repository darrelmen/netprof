/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;

import java.util.*;
import java.util.logging.Logger;

/**
 * A group of buttons that need to maintain
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/8/13
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class ButtonGroupSectionWidget implements SectionWidget {
  private final Logger logger = Logger.getLogger("ButtonGroupSectionWidget");
  public static final String ITEM_SEPARATOR = "&#44";

  private Button clearButton;
  private final String type;

  private String currentSelection = null;
  private final List<Panel> rows = new ArrayList<Panel>();
  private final Set<Button> selected = new HashSet<Button>();
  private final ButtonContainer buttons = new ButtonContainer();
  private String color;
  //private final boolean debug = false;

  ButtonGroupSectionWidget(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean hasOnlyOne() {
    return false;
  }

  @Override
  public List<String> getCurrentSelections() {
    return null;
  }

  /**
   * @param row
   * @see FlexSectionExerciseList#addButtonGroup
   * @see FlexSectionExerciseList#addButtonRow
   * @see FlexSectionExerciseList#addColumnButton
   */
  void addRow(Panel row) {
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
   * @see mitll.langtest.client.amas.ButtonBarSectionWidget#selectItem(String)
   * @see mitll.langtest.client.list.SectionWidgetContainer#getCurrentSelection
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
  //  logger.info("ButtonGroupSectionWidget.getCurrentSelectionInternal for " + type + " checking " + selected.size() + " buttons.");
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
    for (Iterator iter = inOrder.iterator();iter.hasNext();) {
      builder.append(iter.next());
      if (iter.hasNext()) builder.append(ITEM_SEPARATOR);
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
  void simpleSelectItem(Collection<String> sections) {
  //  logger.info("ButtonGroupSectionWidget: simpleSelectItem " + this + " : " + type + "=" + sections);
    clearSelectionState();
    if (isClearSelection(sections)) {
      clearSelection();
    } else {
      for (String toSelect : sections) {
        Collection<Button> buttonsAtName = buttons.getButtonsForName(toSelect);
        if (buttonsAtName == null) {
          logger.warning(">>>> selectItem " + type + "=" + toSelect + " unknown button?");
        } else {
          for (Button b : buttonsAtName) {
            toggleButton(true, b);
          }
        }
      }
    }
  }

  /**
   * @param sections
   * @see FlexSectionExerciseList#getSectionWidgetContainer
   */
  void selectItem(Collection<String> sections) {
    //logger.info("ButtonGroupSectionWidget: selectItem " + this + " : " + type + "='" + sections +"' (" + sections.size()+ ")");
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
/*      if (count != sections.size()) {
        if (debug) System.out.println("Note : selectItem " +
          " : " + type + "=" + sections + " (" + sections.size() +
          ") but only " + count + " selected");
      }*/
      // disableChildrenOfSiblings(buttonsByName);

      if (childGroup != null) {
        childGroup.recurseShowEnabled();
      }
/*      else {
        if (debug) System.out.println("selectItem " +
          " : " + type + "=" + sections + " (" + sections.size() +
          ") no child group.");
      }*/
      currentSelection = null;   // this just means we have to reset this on the next call to getCurrentSelection

      if (count > 0) {
/*        if (debug) System.out.println("ButtonGroupSectionWidget: selectItem " + this +
          " row is selected since " + count + " items are selected.");*/
        for (Panel p : rows) p.addStyleName(color);
      }
      // boolean anythingSelected = isAnythingSelected();
   /*     if (didSelect && !anythingSelected) {
        logger.warning(">>>> selectItem " + type + "=" + sections + " but nothing selected?");
      } */
      setClearButtonState(count > 0);
    }
  }

  private boolean isClearSelection(Collection<String> sections) {
    return sections.size() == 1 && sections.iterator().next().equals(HistoryExerciseList.ANY);
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
          //if (debug) System.out.println("ButtonGroupSectionWidget: unselecting " + unselectCandidate.getSafeText());

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
    //   if (active) logger.info("\ttoggleButton " + b.getSafeText() + " is active");
    //  else  logger.info("\t\ttoggleButton " + b.getSafeText() + " is inactive");
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
     // if (debug) System.out.println("enableChildrenButtons : " + this + " child group " + childGroup + " and recursing over " + buttonChildren.size() + " children");
      childGroup.buttons.rememberEnabled(buttonChildren, active);

      // recurse!
      for (FlexSectionExerciseList.ButtonWithChildren childButton : buttonChildren) {
       // if (debug) System.out.println("\tenableChildrenButtons : " + this + " recurse " + childButton);

        enableChildrenButtons(childButton, active);
      }
    } else {
      // logger.info("enableChildrenButtons : " + this + " no children of " + bb);
    }
    return childGroup;
  }

  private void recurseShowEnabled() {
    buttons.showEnabled();
    FlexSectionExerciseList.ButtonWithChildren firstButton = buttons.getFirstButton();
    if (firstButton != null && firstButton.getButtonChildren() != null && !firstButton.getButtonChildren().isEmpty()) {
      FlexSectionExerciseList.ButtonWithChildren firstChild = firstButton.getButtonChildren().iterator().next();
     // logger.info("recurseShowEnabled : " + this + " recurse on " + firstButton.getButtonGroup());

      firstChild.getButtonGroup().recurseShowEnabled();
    }
  }

  /**
   * Clear selection or enable all buttons.
   *
   * @see #selectItem(java.util.Collection)
   */
  public void clearAll() {
//    logger.info("clearAll : ---------> disable clear button for type " + type + " checking " + buttons + " buttons <----------- ");
    // manager the selected set
    clearSelection();
    clearSelectionState();

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
      //  logger.info("\tsetClearButtonState  : selectItem clear color");
        for (Panel p : rows) p.removeStyleName(color);
      }
    } else {
      logger.warning("clear button is not set? ");
    }
  }

  /**
   * @see mitll.langtest.client.list.SectionWidgetContainer#clearEnabled
   */
  public void clearEnabled() {
 //   logger.info("clear enabled for " +type);
    buttons.clearEnabled();
  }

  public String toString() {
    return "Group " + type + " with " + buttons;
  }
}
