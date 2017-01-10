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

package mitll.langtest.client.table;

import com.github.gwtbootstrap.client.ui.DropdownButton;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import mitll.langtest.client.list.MenuSectionWidget;
import mitll.langtest.client.list.SimpleSelectExerciseList;

import java.util.List;

public class ListSelect {
  //  private final Logger logger = Logger.getLogger("TableSelect");
  public static final String ALL = "All";
  private List<String> values;
  private DropdownButton sButton;
  private SimpleSelectExerciseList singleSelectExerciseList;
  private MenuSectionWidget menuSectionWidget;

  /**
   * @param values
   * @param width
   * @param singleSelectExerciseList
   * @param menuSectionWidget
   * @param initialSelection
   * @return
   */
  public DropdownButton makeSymbolButton(List<String> values,
                                         int width,
                                         SimpleSelectExerciseList singleSelectExerciseList,
                                         MenuSectionWidget menuSectionWidget,
                                         String initialSelection) {
    sButton = new DropdownButton(initialSelection);
    sButton.setIconSize(IconSize.DEFAULT);

    this.values = values;
    this.values.add(0, ALL);
    this.singleSelectExerciseList = singleSelectExerciseList;
    this.menuSectionWidget = menuSectionWidget;

/*    FixedHeightPickList fixedHeightPickList = new FixedHeightPickList();
    fixedHeightPickList.addContributorDF(sButton, values, new Selection() {
      @Override
      public void gotSelection(String s) {
        reallyGotSelection(s);
      }
    });*/


    ListBox box = new ListBox();
    for (String value : values) {
      box.addItem(value);
    }
    box.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        String selectedValue = box.getSelectedValue();
        reallyGotSelection(selectedValue);
      }
    });

    sButton.add(box);
    sButton.addStyleName("rte-picker-button");
    return sButton;
  }



  public interface Selection {
    void gotSelection(String s);
  }

  private void reallyGotSelection(String s) {
    sButton.setText(s);
    menuSectionWidget.gotSelection(s);
    singleSelectExerciseList.gotSelection();
  }
}

