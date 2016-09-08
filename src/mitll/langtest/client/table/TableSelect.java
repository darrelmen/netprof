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
import java.util.logging.Logger;

public class TableSelect {
  private final Logger logger = Logger.getLogger("TableSelect");
  public static final String ALL = "All";

  /**
   * The size of the color grid. This should ideally
   * match the number of B/W and/or Primary colors defined
   * in Color factory which should ideally be the same to ensure
   * layout in a single row.
   */
  // public static final int COLOR_GRID_COL_SIZE = 8;

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
                                         MenuSectionWidget menuSectionWidget, String initialSelection) {
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
 *     if (!$.fn.textWidth.fakeEl) $.fn.textWidth.fakeEl = $('<span>').hide().appendTo(document.body);
 $.fn.textWidth.fakeEl.text(text || this.val() || this.text()).css('font', font || this.css('font'));
 return $.fn.textWidth.fakeEl.width();


 */
//  private Grid createColorGrid(boolean bkgrnd) {
//
//    String[] bwCodes = (bkgrnd) ? ColorFactory.BG_BW_COLOR_CODES : ColorFactory.FG_BW_COLOR_CODES;
//
//    int bwRows = (int) Math.ceil(((float) bwCodes.length) / COLOR_GRID_COL_SIZE);
//    int primaryRows = (int) Math.ceil(((float) ColorFactory.PRIMARY_COLOR_CODES.length) / COLOR_GRID_COL_SIZE);
//    int gradientRows = (int) Math.ceil(((float) ColorFactory.GRADED_COLOR_CODES.length) / COLOR_GRID_COL_SIZE);
//    int totalRows = bwRows + primaryRows + gradientRows;
//    Grid colorGrid = new Grid(totalRows, COLOR_GRID_COL_SIZE);
//    colorGrid.setStyleName("rte-color-picker-grid");
//    int updateRow = -1;
//
//    updateRow = setBackgrounds(colorGrid, updateRow, bwCodes);
//    updateRow = setBackgrounds(colorGrid, updateRow, ColorFactory.PRIMARY_COLOR_CODES);
//    updateRow = setBackgrounds(colorGrid, updateRow, ColorFactory.GRADED_COLOR_CODES);
//
//    colorGrid.addClickHandler(handler);
//    return colorGrid;
//  }

  /**
   * Change the backgrounds for each color in the grid.
   *
   * @param colorGrid The grid to update.
   * @param startRow  The row to start with.
   * @param colors    The set of colors to update.
   * @return Return the index of the last row created.
   */
//  private int setBackgrounds(Grid colorGrid, int updateRow, String[] colors) {
//    for (int i = 0; i < colors.length; i++) {
//      int currCol = i % COLOR_GRID_COL_SIZE;
//      if (currCol == 0) {
//        updateRow++;
//      }
//      Element cellElt = colorGrid.getCellFormatter().getElement(updateRow, currCol);
//      if (colors[i] != null) {
//        cellElt.getStyle().setBackgroundColor(colors[i]);
//        cellElt.getStyle().setColor(colors[i]);
//      } else {
//        Label xLbl = new Label("X");
//        xLbl.getElement().getStyle().setBorderColor("#000000");
//        xLbl.getElement().getStyle().setBorderStyle(BorderStyle.DASHED);
//        xLbl.getElement().getStyle().setBorderWidth(1.0, Unit.PX);
//        colorGrid.setWidget(updateRow, currCol, xLbl);
//      }
//    }
//    colorGrid.getRowFormatter().addStyleName(updateRow, "rte-color-picker-offset-row");
//    return updateRow;
//  }

  /**
   * We use an inner ToolbarEventHandler class to avoid exposing event methods on the
   * MultiRichTextToolbar itself.
   */
  private class ToolbarEventHandler implements ClickHandler/*, KeyUpHandler*/ {
    public void onClick(final ClickEvent event) {
      handleSymbolClick(event);
    }
  }

/*    private void handleNavLinkClick(ClickEvent event, Widget sender, CommentableRichTextArea lastFocused) {

      NavLink theLink = (NavLink) ((sender instanceof NavLink) ? sender : sender.getParent());
      // look for an ancestor dropdown button.
      Widget dropdownWidget = theLink;
      while (dropdownWidget != null && (!(dropdownWidget instanceof DropdownButton))) {
        dropdownWidget = dropdownWidget.getParent();
      }

      RichTextArea.Formatter formatter = lastFocused.getFormatter();
      String linkTxt = theLink.getText().trim();
      lastFocused.setFocus(true);
      if (dropdownWidget == fontDBtn) {
        formatter.setFontName(linkTxt);
      } else if (dropdownWidget == fontSizeDBtn) {
        formatter.setFontSize(fontSizeMap.get(theLink));
      } else if (dropdownWidget == advFormattingDBtn) {
        if (NO_WRAP_LINK.equals(linkTxt)) {
          lastFocused.addNoWrap();
        } else if (WRAP_LINK.equals(linkTxt)) {
          lastFocused.addNormalWrap();
        } else if (LTR_LINK.equals(linkTxt)) {
          lastFocused.setDirection(HasDirection.Direction.LTR);
        } else if (RTL_LINK.equals(linkTxt)) {
          lastFocused.setDirection(HasDirection.Direction.RTL);
        }
      } else if (dropdownWidget == removeDBtn) {
        if (RM_ALL_LINK.equals(linkTxt)) {
          lastFocused.removeFormattingFromDocument();
        } else if (RM_SEL_LINK.equals(linkTxt)) {
          lastFocused.removeFormattingFromSelection(false, false) ;
        } else if (RM_SEL_KEEPCMT_LINK.equals(linkTxt)) {
          lastFocused.removeFormattingFromSelection(true, false) ;
        } else if (RM_SEL_KEEPCMTBR_LINK.equals(linkTxt)) {
          lastFocused.removeFormattingFromSelection(true, true) ;
        }
      }
    }*/

  private void handleSymbolClick(ClickEvent event) {
    Cell clickedCell = symbolGrid.getCellForEvent(event);
    int rowIndex = clickedCell.getRowIndex();
    int cellIndex = clickedCell.getCellIndex();

    String s = values.get((rowIndex * width) + cellIndex);
    //   logger.info("click on " + s);

    sButton.setText(s);
    menuSectionWidget.gotSelection(s);
    singleSelectExerciseList.gotSelection();
  }

/*
  private String getClickedColor(Grid clickedGrid, ClickEvent event) {
    Cell clickedCell = clickedGrid.getCellForEvent(event);
    return clickedCell.getElement().getStyle().getBackgroundColor();
  }
*/

/*
  public String getSelection() {
    return sButton.getText();
  }
*/
}

