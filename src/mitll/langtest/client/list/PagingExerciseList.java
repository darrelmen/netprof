package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link ExerciseShell#tooltip}
 * <p/>
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PagingExerciseList extends ExerciseList implements RequiresResize {
  private static final int MAX_LENGTH_ID = 27;
  protected static final int PAGE_SIZE = 15;   // TODO : make this sensitive to vertical real estate?
  protected int KLUDGE_AMT = 120;
  private ListDataProvider<ExerciseShell> dataProvider;
  private static final boolean DEBUG = false;
  private static final int ID_LINE_WRAP_LENGTH = 20;
  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PAGE_SIZE = 15f;
  private CellTable<ExerciseShell> table;
  protected ExerciseController controller;
  private Set<String> completed = new HashSet<String>();
   boolean isCRTDataMode;

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {}

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {}

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }
  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param controller
   * @param isCRTDataMode
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showTurkToken, boolean showInOrder, ExerciseController controller, boolean isCRTDataMode) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTurkToken, showInOrder, isCRTDataMode);
    this.controller = controller;
    this.isCRTDataMode = controller.getProps().isCRTDataCollectMode();
    addComponents();
  }

  public void setCompleted(Set<String> completed) {
    this.completed = completed;
    if (table != null) table.redraw(); // todo check this...
  }

  @Override
  public int getPercentComplete() {
    if (isCRTDataMode) {
      int i = (int) Math.ceil(100f * ((float) completed.size() / (float) currentExercises.size()));
      if (i > 100) i = 100;
      //System.out.println("completed " + completed.size() + " current " + currentExercises.size() + " " + i);
      return i;
    } else {
      return super.getPercentComplete();
    }
  }

  public int getComplete() {
    if (isCRTDataMode) {
      return completed.size();
    } else {
      return super.getComplete();
    }
  }

  protected void addComponents() {
    FlowPanel column = new FlowPanel();
    add(column);

    final TextBox typeahead = new TextBox();
    //column.add(typeahead);

    typeahead.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {

//        boolean b = typeahead.getText().length() > 0;
        String text = typeahead.getText();
        System.out.println("looking for '" + text+ "' (" +text.length()+ " chars)");
        loadExercises(getHistoryToken(""), text);

      }
    });

    addControlGroupEntry(column,"Search",typeahead);
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        typeahead.setFocus(true);
      }
    });

    addTableWithPager(column);
  }

  protected abstract void loadExercises(String selectionState, String prefix);

  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");

    Controls controls = new Controls();
    userGroup.add(new ControlLabel(label));
    controls.add(user);
    userGroup.add(controls);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected void addTableWithPager(Panel columnToAddTo) {
    makeCellTable();

    // Create a data provider.
    this.dataProvider = new ListDataProvider<ExerciseShell>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    columnToAddTo.add(pager);
    columnToAddTo.add(table);
  }

  protected CellTable<Exercise> makeCellTable() {
    CellTable.Resources o;

    if (controller.isRightAlignContent()) {   // so when we truncate long entries, the ... appears on the correct end
      o = GWT.create(RTLTableResources.class);
    }
    else {
      o = GWT.create(TableResources.class);
    }

    this.table = new CellTable<ExerciseShell>(PAGE_SIZE, o);

    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

    //  table.setWidth("100%", true);
    table.setWidth("100%");
    table.setHeight("auto");

    // Add a selection model to handle user selection.
    final SingleSelectionModel<ExerciseShell> selectionModel = new SingleSelectionModel<ExerciseShell>();
    table.setSelectionModel(selectionModel);
    // we don't want to listen for changes in the selection model, since that happens on load too -- we just want clicks

    addColumnsToTable(true);
    return null;
  }

  protected void addColumnsToTable(boolean consumeClicks) {
    if (isCRTDataMode) {
      Column<ExerciseShell, SafeHtml> id2 = getExerciseIdColumn2(consumeClicks);
      table.addColumn(id2);
    } else {
      Column<ExerciseShell, SafeHtml> id2 = getExerciseIdColumn(consumeClicks);
      id2.setCellStyleNames("alignLeft");
      table.addColumn(id2);
    }
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
   * @see #addColumnsToTable
   * @return
   */
  protected Column<ExerciseShell, SafeHtml> getExerciseIdColumn(final boolean consumeClicks) {
    return new Column<ExerciseShell, SafeHtml>(new MySafeHtmlCell(consumeClicks)) {

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, ExerciseShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          System.out.println("getExerciseIdColumn.onBrowserEvent : got click " + event);
          final ExerciseShell e = object;
          if (isExercisePanelBusy()) {
            tellUserPanelIsBusy();
            markCurrentExercise(currentExercise);
          } else {
            gotClickOnItem(e);
          }
        }
      }

      @Override
      public SafeHtml getValue(ExerciseShell object) {
        String columnText = object.getTooltip();
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  protected Column<ExerciseShell, SafeHtml> getExerciseIdColumn2(final boolean consumeClicks) {
    return new Column<ExerciseShell, SafeHtml>(new MySafeHtmlCell(consumeClicks)) {

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, ExerciseShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          System.out.println("getExerciseIdColumn.onBrowserEvent : got click " + event);
          final ExerciseShell e = object;
          if (isExercisePanelBusy()) {
            tellUserPanelIsBusy();
            markCurrentExercise(currentExercise);
          } else {
            gotClickOnItem(e);
          }
        }
      }

      @Override
      public SafeHtml getValue(ExerciseShell object) {
        String columnText = object.getTooltip();
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
        boolean complete = completed.contains(object.getID());
      //  System.out.println("check -- " + complete + " for " + object.getID() + " in " + completed.size() + " : " + completed);
        return new SafeHtmlBuilder().appendHtmlConstant(columnText +(complete?"&nbsp;<i class='icon-check'></i>":"")).toSafeHtml();
      }
    };
  }

  protected void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  protected String getHistoryToken(String id) { return "item=" +id; }

  protected void gotClickOnItem(final ExerciseShell e) {  pushNewItem(e.getID());  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList.MySetExercisesCallback#onSuccess
   */
  @Override
  protected ExerciseShell loadFirstExercise() {
    ExerciseShell exerciseShell = super.loadFirstExercise();
    selectFirst(exerciseShell);
    return exerciseShell;
  }

  @Override
  protected ExerciseShell findFirstExercise() {
    if (isCRTDataMode) {
      return getFirstNotCompleted();
    }
    else {
      return super.findFirstExercise();
    }
  }

  private ExerciseShell getFirstNotCompleted() {
    for (ExerciseShell es : currentExercises) {
      if (!completed.contains(es.getID())) return es;
    }
    return super.findFirstExercise();
  }

  protected void selectFirst(ExerciseShell toSelect) {
    table.getSelectionModel().setSelected(toSelect, true);
    table.redraw();
    onResize();
  }

  public void clear() {
    List<ExerciseShell> list = dataProvider.getList();
    List<ExerciseShell> copy = new ArrayList<ExerciseShell>();

    for (ExerciseShell es : list) copy.add(es);
    for (ExerciseShell es : copy) list.remove(es);
    table.setRowCount(list.size());
  }

  @Override
  public void flush() {
    dataProvider.flush();
    table.setRowCount(dataProvider.getList().size());
  }

  @Override
  protected void addExerciseToList(ExerciseShell exercise) {
    List<ExerciseShell> list = dataProvider.getList();
    list.add(exercise);
  }

  @Override
  public void onResize() {
    super.onResize();
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

  protected int getNumTableRowsGivenScreenHeight() {
    int header = getTableHeaderHeight();
    int leftOver = Window.getClientHeight() - header - getKludge();
    //System.out.println("Got on resize " + Window.getClientHeight() + " " + header + " result = " + leftOver);

    float rawRatio = ((float) leftOver) / (float) heightOfCellTableWith15Rows();
    float tableRatio = Math.min(MAX_PAGES, rawRatio);
    // System.out.println("left over " + leftOver + " raw " + rawRatio + " table ratio " + tableRatio);

    float ratio = DEFAULT_PAGE_SIZE * tableRatio;
    if (currentExercises != null) {
      ExerciseShell toLoad = currentExercises.get(0);

      if (toLoad.getID().length() > ID_LINE_WRAP_LENGTH) {
        ratio /= 2; // hack for long ids
      }
    }
    return Math.max(MIN_PAGE_SIZE, Math.round(ratio));
  }

  protected int getKludge() {
    return KLUDGE_AMT;
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
   * @see ExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param i
   */
  @Override
  protected void markCurrentExercise(int i) {
    if (currentExercises == null) return;
    ExerciseShell itemToSelect = currentExercises.get(i);
    if (DEBUG)  System.out.println(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID());
    table.getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      System.out.println("marking " +i +" out of " +table.getRowCount() + " page start " +table.getPageStart() +
        " end " + pageEnd);
    }

    int pageNum = i / table.getPageSize();
    int newIndex = pageNum *table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);
      if (DEBUG) System.out.println("new start of prev page " +newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0,Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
        if (DEBUG) System.out.println("new start of next newIndex " +newStart + "/" +newIndex +"/page = " + pageNum+
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
