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
 * Created by go22670 on 9/16/14.
 */
public class SimplePagingContainer<T> implements RequiresResize {
  private final Logger logger = Logger.getLogger("SimplePagingContainer");

  public static final int MAX_WIDTH = 320;
  private static final int PAGE_SIZE = 10;   // TODO : make this sensitive to vertical real estate?
  private static final int VERTICAL_SLOP = 35;
  protected static final int ID_LINE_WRAP_LENGTH = 20;
  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PAGE_SIZE = 15f;
//  private static final boolean debug = false;
  protected final ExerciseController controller;
  protected ListDataProvider<T> dataProvider;
  protected CellTable<T> table;
  protected SingleSelectionModel<T> selectionModel;
  protected int verticalUnaccountedFor = 100;

  public SimplePagingContainer(ExerciseController controller) {
    this.controller = controller;
  }
  /**
   * @see mitll.langtest.client.list.PagingExerciseList#addTableWithPager(mitll.langtest.client.exercise.PagingContainer)
   * @return
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

    table.getElement().getStyle().setProperty("maxWidth", MAX_WIDTH + "px");
    return column;
  }

  protected void makeCellTable() {
    CellTable.Resources o = chooseResources();

    this.table = makeCellTable(o);

    configureTable();
  }

  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;

    if (controller.isRightAlignContent()) {   // so when we truncate long entries, the ... appears on the correct end
      o = GWT.create(RTLTableResources.class);
    } else {
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

  protected void addSelectionModel() {}

  protected void addColumnsToTable() {}

  private CellTable<T> makeCellTable(CellTable.Resources o) {
    return new CellTable<T>(PAGE_SIZE, o);
  }

  public void flush() {
    dataProvider.flush();
    table.setRowCount(getList().size());
  }

  /**
   * @see PagingContainer#addColumnsToTable()
   * @param id2
   * @param header
   */
  protected void addColumn(Column<T, SafeHtml> id2, Header<?> header) {
    table.addColumn(id2, header);
  }

  public void clear() {
    List<T> list = getList();
    list.clear();

    table.setRowCount(list.size());
  }

  protected List<T> getList() { return dataProvider.getList();  }

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

  protected int getNumTableRowsGivenScreenHeight() {
    int header = getTableHeaderHeight();
    int pixelsAbove = header + verticalUnaccountedFor;
    if (table.getElement().getAbsoluteTop() > 0) {
      pixelsAbove = table.getElement().getAbsoluteTop() + VERTICAL_SLOP;
    }
    int leftOver = Window.getClientHeight() - pixelsAbove;

/*
    if (debug) System.out.println("getNumTableRowsGivenScreenHeight Got on resize window height " + Window.getClientHeight() +
       " header " + header + " result = " + leftOver + "( vert unaccount " +
       verticalUnaccountedFor+ " vs absolute top " + table.getElement().getAbsoluteTop()+ " pix above " + pixelsAbove+
       ")");
*/

    float rawRatio = ((float) leftOver) / (float) heightOfCellTableWith15Rows();
    float tableRatio = Math.min(MAX_PAGES, rawRatio);
    float ratio = DEFAULT_PAGE_SIZE * tableRatio;

/*    if (debug) System.out.println("getNumTableRowsGivenScreenHeight : left over " + leftOver + " raw " + rawRatio +
      " table ratio " + tableRatio + " ratio " + ratio);*/

    ratio = adjustVerticalRatio(ratio);
    int rows = Math.max(MIN_PAGE_SIZE, (int)Math.floor(ratio));

/*
    if (debug) System.out.println("getNumTableRowsGivenScreenHeight : rows " + rows);
*/
    return rows;
  }

  protected float adjustVerticalRatio(float ratio) {
    return ratio;
  }

  private int heightOfCellTableWith15Rows() { return HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;  }

  private int getTableHeaderHeight() { return controller.getHeightOfTopRows(); }

  public void addItem(T item) {
    getList().add(item);
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
