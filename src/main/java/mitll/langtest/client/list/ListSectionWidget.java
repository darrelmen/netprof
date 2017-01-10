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
import mitll.langtest.shared.SectionNode;

import java.util.*;
import java.util.logging.Logger;

public class ListSectionWidget implements SectionWidget {
  private final Logger logger = Logger.getLogger("MenuSectionWidget");

  private final String type;
  private ListBox listBox;
  private final Collection<SectionNode> nodes;
  private final SimpleSelectExerciseList singleSelectExerciseList;

  /**
   * @param type
   * @param nodes
   * @param singleSelectExerciseList
   */
  ListSectionWidget(String type,
                    Collection<SectionNode> nodes,
                    SimpleSelectExerciseList singleSelectExerciseList) {
    this.type = type;
    this.nodes = nodes;
    this.singleSelectExerciseList = singleSelectExerciseList;
    //  logger.info("made menu " + type + " with " + nodes.size());
  }

  private int num = 0;
 // private final ButtonToolbar toolbar = new ButtonToolbar();
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
    horizontalPanel.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    // add label
    Heading child = new Heading(5, label);
    child.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
    horizontalPanel.add(child);

    Button left = getLeftButton(values);
    horizontalPanel.add(left);

    horizontalPanel.add(toolbar);

    Button right = getRightButton(values, left);
    horizontalPanel.add(right);

    this.num = values.size();

    //addGridChoices(values, "All");
    addChoices(values,"All");

    container.add(horizontalPanel);
  }

  private Button getLeftButton(final List<String> values) {
    Button left = new Button("", IconType.CARET_LEFT);
    left.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String currentSelection = getCurrentSelection();
        int i = values.indexOf(currentSelection);
        if (i > 0) {
          String item = values.get(i - 1);
          selectItem(item);
          gotSelection(item);
          singleSelectExerciseList.gotSelection();

        }
      }
    });
//    left.addStyleName("rightFiveMargin");
    left.addStyleName("leftFiveMargin");
    left.addStyleName("topMargin");
    return left;
  }

  private Button getRightButton(final List<String> values, Button left) {
    Button right = new Button("", IconType.CARET_RIGHT);
    right.addStyleName("leftFiveMargin");
    right.addStyleName("topMargin");

    right.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String currentSelection = getCurrentSelection();
        int i = values.indexOf(currentSelection);
        if (i < values.size() - 1) {
          String item = values.get(i + 1);
          logger.info("Selecting " + (i+1) + " " + item);
          selectItem(item);
          gotSelection(item);
          singleSelectExerciseList.gotSelection();
        }
        else {
          logger.info("not selecting next " + i + " " + currentSelection);

        }
      }
    });
    return right;
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
   * @param possibleValues
   * @see #gotSelection(String)
   */
  private void gotSelection(Collection<String> possibleValues) {
    logger.info("gotSelection " + type + " : " + possibleValues);
    boolean isAll = !possibleValues.isEmpty() && possibleValues.iterator().next().equals(TableSelect.ALL);

    logger.info("examine " + nodes.size() + " nodes");
    Set<String> possible = new HashSet<>();

    List<String> sorted = new ArrayList<>();
    ItemSorter itemSorter = new ItemSorter();

    for (SectionNode node : nodes) {
      if (isAll || possibleValues.contains(node.getName())) {
        List<String> temp = new ArrayList<>();
        for (SectionNode n : node.getChildren()) {
          if (possible.add(n.getName())) {
            temp.add(n.getName());
          }
        }

        List<String> sectionsInType = itemSorter.getSortedItems(temp);
//        List<String> sectionsInType = new ItemSorter().getSortedItems(getLabels(node.getChildren()));
//        possible.addAll(sectionsInType);
        sorted.addAll(sectionsInType);
      }
    }
    if (childWidget != null) {
      String currentSelection = childWidget.getCurrentSelection();

      boolean stillValid = possible.contains(currentSelection);
      // ArrayList<String> values = new ArrayList<>(possible);
//      childWidget.addGridChoices(sorted, stillValid ? currentSelection : TableSelect.ALL);
      childWidget.addChoices(sorted, stillValid ? currentSelection : TableSelect.ALL);


//      if (!stillValid) {
//        logger.info("\tgotSelection reset choice on " + childWidget + " since it's current is " + currentSelection +
//            " is not in " + possible);
//      }
//      logger.info("\tgotSelection recurse on " + childWidget);
      childWidget.gotSelection(possible);
    }
  }


  public String toString() {
    return "sectionWidget " + type;
  }
}
