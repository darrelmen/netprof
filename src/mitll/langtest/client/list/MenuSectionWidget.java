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
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.*;

public class MenuSectionWidget implements SectionWidget {
  private final String type;
  private ListBox child1 = new ListBox();

  public MenuSectionWidget(String type) {
    this.type = type;
  }

  public void addChoices(Panel container,
                         String label,
                         Collection<String> values,
                         SimpleSelectExerciseList singleSelectExerciseList) {
    Panel horizontalPanel = new HorizontalPanel();
    horizontalPanel.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    // add label
    Heading child = new Heading(5, label);
    child.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
    horizontalPanel.add(child);

    child1.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        singleSelectExerciseList.gotSelection();
      }
    });

    horizontalPanel.add(child1);

    Set<String> unique = new TreeSet<String>(values);

    child1.addItem("All");
    Style style = child1.getElement().getStyle();
    style.setMarginTop(10, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    for (String value : unique) {
      child1.addItem(value);
    }
    container.add(horizontalPanel);
  }

  @Override
  public String getCurrentSelection() {
    return child1.getSelectedValue();
  }

  @Override
  public void clearSelectionState() {
    child1.setSelectedValue("All");
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
    return child1.getItemCount() == 1;
  }

  @Override
  public List<String> getCurrentSelections() {
    return Collections.singletonList(getCurrentSelection());
  }

  @Override
  public void selectFirst() {
    clearSelectionState();
  }

  public void selectItem(String item) {
    child1.setSelectedValue(item);
  }
}
