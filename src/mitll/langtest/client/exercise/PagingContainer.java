package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingContainer<T extends ExerciseShell> {
  private static final int MAX_LENGTH_ID = 27;
  protected static final int PAGE_SIZE = 15;   // TODO : make this sensitive to vertical real estate?
  private ListDataProvider<T> dataProvider;
  private static final boolean DEBUG = false;
  private static final int ID_LINE_WRAP_LENGTH = 20;
  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PAGE_SIZE = 15f;
  private CellTable<T> table;
  protected ExerciseController controller;
  private int verticalUnaccountedFor = 100;
  private Set<String> completed = new HashSet<String>();

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   * @param controller
   * @param verticalUnaccountedFor
   */
  public PagingContainer(ExerciseController controller, int verticalUnaccountedFor) {
    this.controller = controller;
    this.verticalUnaccountedFor = verticalUnaccountedFor;
  }

  public void addCompleted(String id) {
    completed.add(id);
    refresh();

    System.out.println("addCompleted : completed " + id +  " now " + getCompleted().size());

  }
  public void refresh() {
    table.redraw();
  }

  public Set<String> getCompleted() { return completed; }

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }

  public Panel getTableWithPager() {
    makeCellTable();

    // Create a data provider.
    this.dataProvider = new ListDataProvider<T>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    FlowPanel column = new FlowPanel();
    column.add(pager);
    column.add(table);

    return column;
  }

  private CellTable<T> makeCellTable() {
    CellTable.Resources o = chooseResources();

    this.table = makeCellTable(o);

    configureTable();
    return null;
  }

  private CellTable.Resources chooseResources() {
    CellTable.Resources o;

    if (controller.isRightAlignContent()) {   // so when we truncate long entries, the ... appears on the correct end
      o = GWT.create(RTLTableResources.class);
    } else {
      o = GWT.create(TableResources.class);
    }
    return o;
  }

  public com.github.gwtbootstrap.client.ui.CellTable<T> makeBootstrapCellTable(com.github.gwtbootstrap.client.ui.CellTable.Resources resources) {
    com.github.gwtbootstrap.client.ui.CellTable<T> bootstrapCellTable = createBootstrapCellTable(resources);
    this.table = bootstrapCellTable;
    configureTable();

    return bootstrapCellTable;
  }

  private void configureTable() {
    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);
    table.setWidth("100%");
    table.setHeight("auto");

    // Add a selection model to handle user selection.
    final SingleSelectionModel<T> selectionModel = new SingleSelectionModel<T>();
    table.setSelectionModel(selectionModel);
    // we don't want to listen for changes in the selection model, since that happens on load too -- we just want clicks

    addColumnsToTable();
  }

  private CellTable<T> makeCellTable(CellTable.Resources o) {
    return new CellTable<T>(PAGE_SIZE, o);
  }

  private com.github.gwtbootstrap.client.ui.CellTable<T> createBootstrapCellTable(com.github.gwtbootstrap.client.ui.CellTable.Resources o) {
    return new com.github.gwtbootstrap.client.ui.CellTable<T>(PAGE_SIZE, o);
  }

  private void addColumnsToTable() {
    System.out.println("addColumnsToTable : completed " + controller.showCompleted() +  " now " + getCompleted().size());

    Column<T, SafeHtml> id2;
  //  if (controller.showCompleted()) {
      id2 = getExerciseIdColumn2(true);
  //  } else {
   //   id2 = getExerciseIdColumn(true);
   //   id2.setCellStyleNames("alignLeft");
   // }
    table.addColumn(id2);

    // this would be better, but want to consume clicks
  /*  TextColumn<ExerciseShell> id2 = new TextColumn<ExerciseShell>() {
      @Override
      public String getValue(ExerciseShell exerciseShell) {
        String columnText =  exerciseShell.getTooltip();
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0,MAX_LENGTH_ID-3)+"...";

        return columnText;
      }
    };*/
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#enableNext()
   * @param completed
   */
  public void setCompleted(Set<String> completed) {
    this.completed = completed;
    if (table != null) table.redraw(); // todo check this...
  }

  /**
   * @return
   * @see #addColumnsToTable
   */
/*
  private Column<T, SafeHtml> getExerciseIdColumn(final boolean consumeClicks) {
    return new Column<T, SafeHtml>(new SafeHtmlCell() {
      @Override
      public Set<String> getConsumedEvents() {
        Set<String> events = new HashSet<String>();
        if (consumeClicks) events.add(BrowserEvents.CLICK);
        return events;
      }
    }) {
      @Override
      public SafeHtml getValue(T object) {
        return getColumnToolTip(object.getTooltip());
      }

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          System.out.println("getExerciseIdColumn.onBrowserEvent : got click " + event);
          final T e = object;
       */
/*   if (isExercisePanelBusy()) {
            tellUserPanelIsBusy();
            markCurrentExercise(currentExercise);
          } else {*//*

          gotClickOnItem(e);
          // }
        }
      }

      private SafeHtml getColumnToolTip(String columnText) {
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }
*/

  protected Column<T, SafeHtml> getExerciseIdColumn2(final boolean consumeClicks) {
    return new Column<T, SafeHtml>(new MySafeHtmlCell(consumeClicks)) {

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          System.out.println("getExerciseIdColumn.onBrowserEvent : got click " + event);
          final T e = object;
          gotClickOnItem(e);
        }
      }

      @Override
      public SafeHtml getValue(ExerciseShell object) {
        if (!controller.showCompleted()) {
          return getColumnToolTip(object.getTooltip());
        }
        else {
        String columnText = object.getTooltip();
        String html = object.getID();
        if (columnText != null) {
          if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
          boolean complete = completed.contains(object.getID());
          // System.out.println("check -- " + complete + " for " + object.getID() + " in " + completed.size() + " : " + completed);
          html = (complete ? "<i class='icon-check'></i>&nbsp;" : "") + columnText;
        }
        return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
        }
      }

      private SafeHtml getColumnToolTip(String columnText) {
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  protected void gotClickOnItem(final T e) {}

  public T selectFirst() {
    if (dataProvider.getList().isEmpty()) return null;
    T first = dataProvider.getList().get(0);
    table.getSelectionModel().setSelected(first, true);
    table.redraw();
    onResize(0);
    return first;
  }

  public void clear() {
    List<T> list = dataProvider.getList();
    List<T> copy = new ArrayList<T>();

    for (T es : list) copy.add(es);
    for (T es : copy) list.remove(es);
    table.setRowCount(list.size());
  }

  public void flush() {
    dataProvider.flush();
    table.setRowCount(dataProvider.getList().size());
  }

  public <S extends ExerciseShell> void addAndFlush(S exercise) {
    addExerciseToList2(exercise);
    flush();
  }

  public <S extends ExerciseShell> void addExerciseToList2(S exercise) {
    List<T> list = dataProvider.getList();
    list.add((T) exercise);  // TODO : can't remember how I avoid this
  }

  public void onResize(int currentExercise) {
    // super.onResize();
/*    System.out.println("Got on resize " + Window.getClientHeight() + " " +
        getOffsetHeight() + " bodyheight = " + table.getBodyHeight() + " table offset height " + table.getOffsetHeight() + " parent height " + getParent().getOffsetHeight());*/
    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      //System.out.println("num rows now " + numRows);
      table.setPageSize(numRows);
      table.redraw();
      markCurrentExercise(currentExercise);
    }
  }

  public int getNumTableRowsGivenScreenHeight() {
    int header = getTableHeaderHeight();
    int leftOver = Window.getClientHeight() - header - verticalUnaccountedFor;

    //System.out.println("Got on resize " + Window.getClientHeight() + " " + header + " result = " + leftOver);

    float rawRatio = ((float) leftOver) / (float) heightOfCellTableWith15Rows();
    float tableRatio = Math.min(MAX_PAGES, rawRatio);
    // System.out.println("left over " + leftOver + " raw " + rawRatio + " table ratio " + tableRatio);

    float ratio = DEFAULT_PAGE_SIZE * tableRatio;
    if (dataProvider != null && dataProvider.getList() != null) {
      if (!dataProvider.getList().isEmpty()) {
        T toLoad = dataProvider.getList().get(0);

        if (toLoad.getID().length() > ID_LINE_WRAP_LENGTH) {
          ratio /= 2; // hack for long ids
        }
      }
    }
    return Math.max(MIN_PAGE_SIZE, Math.round(ratio));
  }

  protected int heightOfCellTableWith15Rows() {
    return HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }

  protected int getTableHeaderHeight() {
    return controller.getHeightOfTopRows();
  }

  /**
   * not sure how this happens, but need Math.max(0,...)
   *
   * @param i
   * @seex ExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   */
  public void markCurrentExercise(int i) {
    if (dataProvider.getList() == null || dataProvider.getList().isEmpty()) return;
    T itemToSelect = dataProvider.getList().get(i);
    if (DEBUG) System.out.println(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID());
    table.getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      System.out.println("marking " + i + " out of " + table.getRowCount() + " page start " + table.getPageStart() +
        " end " + pageEnd);
    }

    int pageNum = i / table.getPageSize();
    int newIndex = pageNum * table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
      if (DEBUG) System.out.println("new start of prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
        if (DEBUG) System.out.println("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
          " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, table.getPageSize());
      }
    }
    table.redraw();
  }

  private static class MySafeHtmlCell extends SafeHtmlCell {
    private final boolean consumeClicks;

    public MySafeHtmlCell(boolean consumeClicks) {
      this.consumeClicks = consumeClicks;
    }

    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<String>();
      if (consumeClicks) events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}
