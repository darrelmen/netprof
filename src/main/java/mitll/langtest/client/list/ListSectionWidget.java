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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.table.ListBoxSelect;
import mitll.langtest.client.table.TableSelect;
import mitll.langtest.shared.exercise.SectionNode;

import java.util.*;
import java.util.logging.Logger;

public class ListSectionWidget implements SectionWidget {
  private final Logger logger = Logger.getLogger("ListSectionWidget");

  public static final int RIGHT_MARGIN = 15;

  private final String type;
  private ListBox listBox;
  private final Collection<SectionNode> nodes;
  private final SimpleSelectExerciseList singleSelectExerciseList;
  Set<String> allValuesOfType;

  /**
   * @param type
   * @param nodes
   * @param singleSelectExerciseList
   * @see SimpleSelectExerciseList#addChoiceWidgets
   */
  ListSectionWidget(String type,
                    Collection<SectionNode> nodes,
                    SimpleSelectExerciseList singleSelectExerciseList,
                    Set<String> allValuesOfType) {
    this.type = type;
    this.nodes = nodes;
    this.singleSelectExerciseList = singleSelectExerciseList;
    this.allValuesOfType =allValuesOfType;
    logger.info("ListSectionWidget made menu " + type + " with " + nodes.size());
  }

  private int num = 0;
  private final DivWidget toolbar = new DivWidget();

  /**
   * @param container
   * @param label
   * @param values
   * @see SimpleSelectExerciseList#addChoiceRow
   */
  void addChoices(Panel container,
                  String label,
                  List<String> values) {
    Panel horizontalPanel = new HorizontalPanel();
    horizontalPanel.getElement().getStyle().setMarginRight(RIGHT_MARGIN, Style.Unit.PX);

    // add label
    Heading child = new Heading(5, label);
    child.getElement().getStyle().setMarginTop(15, Style.Unit.PX);

    horizontalPanel.add(child);
    horizontalPanel.add(getLeftButton(values));
    horizontalPanel.add(toolbar);
    horizontalPanel.add(getRightButton(values));

    this.num = values.size();

    addChoices(values, "All");
    container.add(horizontalPanel);
  }

  private Button getLeftButton(final List<String> values) {
    Button left = new Button("", IconType.CARET_LEFT);
    left.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String currentSelection = getCurrentSelection();
        int i = values.indexOf(currentSelection);
        int toUse = (i > 0) ? i - 1 : values.size() - 1;
        selectAtIndex(toUse, values);

      }
    });
//    left.addStyleName("rightFiveMargin");
    left.addStyleName("leftFiveMargin");
    left.addStyleName("topMargin");
    return left;
  }

  private Button getRightButton(final List<String> values) {
    Button right = new Button("", IconType.CARET_RIGHT);
    right.addStyleName("leftFiveMargin");
    right.addStyleName("topMargin");

    right.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String currentSelection = getCurrentSelection();
        int i = values.indexOf(currentSelection);
        // if (i < values.size() - 1) {
        int index = (i < values.size() - 1) ? i + 1 : 0;
        //  logger.info("Selecting " + (i+1) + " " + item);
        selectAtIndex(index, values);
        // }
        // else {
        //logger.info("not selecting next " + i + " " + currentSelection);
        //}
      }
    });
    return right;
  }

  private void selectAtIndex(int index, List<String> values) {
    String item = values.get(index);
    selectItem(item);
    gotSelection(item);
    singleSelectExerciseList.gotSelection();
  }

  private void addChoices(List<String> values, String initialChoice) {
    this.listBox = new ListBoxSelect().makeSymbolButton(values, 4, singleSelectExerciseList, this, initialChoice);
    toolbar.clear();
    toolbar.add(listBox);
    listBox.addStyleName("leftFiveMargin");
  }


  @Override
  public String getCurrentSelection() {
    return listBox.getSelectedValue().trim();
  }

  @Override
  public void clearSelectionState() {
    listBox.setSelectedValue("All");
  }

  @Override
  public void clearAll() {
    clearSelectionState();
  }

  @Override
  public void clearEnabled() {
    clearSelectionState();
  }

  @Override
  public void enableAll() {
  }

  @Override
  public void addButton(Button b) {
  }

  @Override
  public void addLabel(Widget label, String color) {
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public boolean hasOnlyOne() {
    return num == 1;
  }

  @Override
  public List<String> getCurrentSelections() {
    return Collections.singletonList(getCurrentSelection());
  }

  @Override
  public void selectFirst() {
    clearSelectionState();
  }

  public boolean selectItem(String item) {
    listBox.setSelectedValue(item);
    return true;
  }

  private ListSectionWidget childWidget;

  public void addChild(ListSectionWidget value) {
    childWidget = value;
  }

  public void gotSelection(String s) {
    gotSelection(Collections.singleton(s));
  }

  /**
   * Foolish = don't have to do this exhaustive thing
   * @param possibleValues
   * @see #gotSelection(String)
   */
  private void gotSelection(Collection<String> possibleValues) {
    logger.info("gotSelection " + type + " : " + possibleValues);
    boolean isAll = !possibleValues.isEmpty() && possibleValues.iterator().next().equals(TableSelect.ALL);

    logger.info("gotSelection examine " + nodes.size() + " nodes");
    Set<String> uniquePossible = new HashSet<>();

    List<String> sorted = new ArrayList<>();
    ItemSorter itemSorter = new ItemSorter();

    /**
     * Go through each node and add it's children values...?
     */
    for (SectionNode node : nodes) {
      if (isAll || possibleValues.contains(node.getName())) {
        List<String> temp = new ArrayList<>();
        for (SectionNode n : node.getChildren()) {
          if (uniquePossible.add(n.getName())) {
            temp.add(n.getName());
          }
        }

        List<String> sectionsInType = itemSorter.getSortedItems(temp);
        sorted.addAll(sectionsInType);
      }
    }

    // try to decide if a child selection is still valid given upstream selection
    if (childWidget != null) {
      String currentSelection = childWidget.getCurrentSelection();
      boolean stillValid = uniquePossible.contains(currentSelection);
      childWidget.addChoices(sorted, stillValid ? currentSelection : TableSelect.ALL);
      logger.info("\tgotSelection recurse on " + childWidget + " with " +uniquePossible.size());
      childWidget.gotSelection(uniquePossible);
    }
  }

  public String toString() {
    return "sectionWidget " + type;
  }
}
