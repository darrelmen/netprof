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

package mitll.langtest.client.exercise;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/16/14.
 */
public class SimplePagingContainer<T> implements RequiresResize {
  private final Logger logger = Logger.getLogger("SimplePagingContainer");

  private static final boolean DEBUG = false;
  public static final int MAX_WIDTH = 320;
  private static final int PAGE_SIZE = 10;   // TODO : make this sensitive to vertical real estate?
  private static final int VERTICAL_SLOP = 35;
  static final int ID_LINE_WRAP_LENGTH = 20;
  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 395;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PAGE_SIZE = 15f;
  protected final ExerciseController controller;
  ListDataProvider<T> dataProvider;
  protected CellTable<T> table;
  protected SingleSelectionModel<T> selectionModel;
  int verticalUnaccountedFor = 100;
  //  private static final boolean debug = false;

  protected SimplePagingContainer(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#addTableWithPager(ClickablePagingContainer)
   */
  public Panel getTableWithPager() {
    this.dataProvider = new ListDataProvider<T>();

    makeCellTable();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Create a SimplePager.
    final SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    Panel column = new FlowPanel();
    column.add(pager);
    column.add(table);
    table.addStyleName("floatLeft");

    setMaxWidth();
    return column;
  }

  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", MAX_WIDTH + "px");
  }

  private void makeCellTable() {
    this.table = makeCellTable(chooseResources());
    configureTable();
  }

  private CellTable<T> makeCellTable(CellTable.Resources o) {
    return new CellTable<T>(getPageSize(), o);
  }

  protected int getPageSize() { return PAGE_SIZE;  }

  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;

    if (controller.isRightAlignContent()) {   // so when we truncate long entries, the ... appears on the correct end
      //logger.info("simplePaging : chooseResources RTL - content");
      o = GWT.create(RTLTableResources.class);
    } else {
     // logger.info("simplePaging : chooseResources LTR - content");
      o = GWT.create(TableResources.class);
    }
    return o;
  }

  private void configureTable() {
    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
    table.setWidth("100%");
    table.setHeight("auto");

    // Add a selection model to handle user selection.
    addSelectionModel();
    // we don't want to listen for changes in the selection model, since that happens on load too -- we just want clicks

    addColumnsToTable();
  }

  protected void addSelectionModel() {
  }

  protected void addColumnsToTable() {
  }


  public void flush() {
    dataProvider.flush();
    table.setRowCount(getList().size());
  }

  /**
   * @param id2
   * @param header
   * @see PagingContainer#addColumnsToTable()
   */
  protected void addColumn(Column<T, SafeHtml> id2, Header<?> header) {
    table.addColumn(id2, header);
  }

  protected void clear() {
    List<T> list = getList();
    if (list == null) {
      if (table == null) {
        controller.logMessageOnServer("no table for " + this.getClass() + " for " + " user " + controller.getUser(),
            controller.getLanguage());
      } else {
        table.setRowCount(0);
        controller.logMessageOnServer("no list for " + this.getClass() + " for " + " user " + controller.getUser(),
            controller.getLanguage());
      }
    } else {
      list.clear();
      table.setRowCount(list.size());
    }
  }

  protected List<T> getList() {
    return dataProvider == null ? null : dataProvider.getList();
  }

  /**
   * @seex #selectItem(int)
   * @see mitll.langtest.client.list.PagingExerciseList#onResize()
   */
  public void onResize() {
    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      table.setPageSize(numRows);
      table.redraw();
    }
  }

  int getNumTableRowsGivenScreenHeight() {
    int header = getTableHeaderHeight();
    int pixelsAbove = header + verticalUnaccountedFor;
    if (table.getElement().getAbsoluteTop() > 0) {
      pixelsAbove = table.getElement().getAbsoluteTop() + VERTICAL_SLOP;
    }
    int leftOver = Window.getClientHeight() - pixelsAbove;
    if (DEBUG) {
      logger.info("getNumTableRowsGivenScreenHeight Got on resize window height " + Window.getClientHeight() +
          " header " + header + " result = " + leftOver + "( vert unaccount " +
          verticalUnaccountedFor + " vs absolute top " + table.getElement().getAbsoluteTop() + " pix above " + pixelsAbove +
          ")");
    }

    float rawRatio = ((float) leftOver) / (float) heightOfCellTableWith15Rows();
    float tableRatio = Math.min(MAX_PAGES, rawRatio);
    float ratio = DEFAULT_PAGE_SIZE * tableRatio;

/*    if (DEBUG) logger.debug("getNumTableRowsGivenScreenHeight : left over " + leftOver + " raw " + rawRatio +
      " table ratio " + tableRatio + " ratio " + ratio);*/

    ratio = adjustVerticalRatio(ratio);
    int attempt = (int) Math.floor(ratio);
    attempt--;
    int rows = Math.max(MIN_PAGE_SIZE, attempt);

    if (DEBUG) logger.info("getNumTableRowsGivenScreenHeight : rows " + rows);

    return rows;
  }

  float adjustVerticalRatio(float ratio) {
    return ratio;
  }

  private int heightOfCellTableWith15Rows() {
    return HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }

  private int getTableHeaderHeight() {
    return controller.getHeightOfTopRows();
  }

  protected void addItem(T item) {
    getList().add(item);
  }

  protected void scrollToVisible(int i) {
    int pageNum = i / table.getPageSize();
    int newIndex = pageNum * table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
    //  if (ClickablePagingContainer.DEBUG) logger.info("new start of prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
//        if (ClickablePagingContainer.DEBUG) logger.info("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
//            " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, table.getPageSize());
      }
    }
  }

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    PagingContainer.TableResources.TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    PagingContainer.RTLTableResources.TableStyle cellTableStyle();
  }
}
