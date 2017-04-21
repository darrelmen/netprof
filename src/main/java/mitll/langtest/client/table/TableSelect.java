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
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import mitll.langtest.client.list.MenuSectionWidget;
import mitll.langtest.client.list.SimpleSelectExerciseList;

import java.util.List;

@Deprecated
public class TableSelect {
//  private final Logger logger = Logger.getLogger("TableSelect");
  public static final String ALL = "All";

  private final ToolbarEventHandler handler = new ToolbarEventHandler();

  private Grid symbolGrid;

  private List<String> values;
  private int width;
  private DropdownButton sButton;

  private SimpleSelectExerciseList singleSelectExerciseList;
  private MenuSectionWidget menuSectionWidget;

  static {
    new TableSelect().register();
  }

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
    //  sButton.setSize(ButtonSize.MINI);
//    sButton.setIcon(IconType.KEYBOARD);
    //  sButton.setRightDropdown(true);

    this.values = values;
    this.values.add(0, ALL);
    this.singleSelectExerciseList = singleSelectExerciseList;
    this.menuSectionWidget = menuSectionWidget;

    int n = values.size();
    int size = n;

    int numRows = (int) Math.ceil((double) size / (double) width);
    if (numRows > 20) {
      numRows = 20;
      width = (int) Math.ceil((double) n / (double) 20);
    }

    this.width = width;
    int numcols = width;

    symbolGrid = new Grid(numRows, numcols);

//    String colWidth = (100 / numcols) + "%";
    // logger.info("rows " + numRows + " num cols " + numcols);
    int widthSoFar = 0;

    for (int r = 0; r < numRows; r++) {
      int numItemsInRow = size >= width ? width : size % width;
      for (int c = 0; c < numItemsInRow; c++) {
        String text = values.get(n - size);
        int width1 = getWidth(text.replaceAll(" ", "_").replaceAll("-", "_"), "bold 24px Arial");
        if (width1 > widthSoFar) {
       //   logger.info("new highest - for " + text + " got " + width1);
          widthSoFar = width1;
        }
        size--;
      }
    }
    size = n;
    for (int r = 0; r < numRows; r++) {
      //int numItemsInRow = symbol_values[r].length;
      int numItemsInRow = size >= width ? width : size % width;
      for (int c = 0; c < numItemsInRow; c++) {
        // if (r == 0) {
        //  symbolGrid.getColumnFormatter().setWidth(c, colWidth);
        // }
        String text = values.get(n - size);

        Label widget = new Label(text);
        widget.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
        widget.setWidth(widthSoFar + "px");
        symbolGrid.setWidget(r, c, widget);
        size--;
      }
    }
    symbolGrid.addStyleName("rte-symbol-picker-container");
    symbolGrid.addClickHandler(handler);
    sButton.add(symbolGrid);
    sButton.addStyleName("rte-picker-button");
    return sButton;
  }

  /**
   * @return
   * @see
   */
  public native int getWidth(String text, String font) /*-{
      return $wnd.jQuery.fn.textWidth(text, font)
  }-*/;

  public native void register() /*-{
      $wnd.jQuery.fn.textWidth = function (text, font) {
          if (!$wnd.jQuery.fn.textWidth.fakeEl) {
              $wnd.jQuery.fn.textWidth.fakeEl = $wnd.jQuery('<span>').hide().appendTo(document.body);
          }
          $wnd.jQuery.fn.textWidth.fakeEl.text(text || this.val() || this.text()).css('font', font || this.css('font'));
          return $wnd.jQuery.fn.textWidth.fakeEl.width();
      };
  }-*/;

  /**
   * We use an inner ToolbarEventHandler class to avoid exposing event methods on the
   * MultiRichTextToolbar itself.
   */
  private class ToolbarEventHandler implements ClickHandler/*, KeyUpHandler*/ {
    public void onClick(final ClickEvent event) {
      handleSymbolClick(event);
    }
  }

  private void handleSymbolClick(ClickEvent event) {
    Cell clickedCell = symbolGrid.getCellForEvent(event);
    int rowIndex = clickedCell.getRowIndex();
    int cellIndex = clickedCell.getCellIndex();

    String s = values.get((rowIndex * width) + cellIndex);
    //   logger.info("click on " + s);
    gotSelection(s);
  }

  private void gotSelection(String s) {
    sButton.setText(s);
    menuSectionWidget.gotSelection(s);
    singleSelectExerciseList.gotSelection();
  }
}

