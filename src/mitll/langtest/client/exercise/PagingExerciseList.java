package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link ExerciseShell#tooltip}
 * <p/>
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingExerciseList extends ExerciseList implements RequiresResize {
/*  private static final int MAX_LENGTH_ID = 27;
  protected static final int PAGE_SIZE = 15;   // TODO : make this sensitive to vertical real estate?
  private ListDataProvider<ExerciseShell> dataProvider;
  private static final boolean DEBUG = false;
  private static final int ID_LINE_WRAP_LENGTH = 20;
  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PAGE_SIZE = 15f;
  private CellTable<ExerciseShell> table;*/
  protected ExerciseController controller;
  protected PagingContainer<? extends ExerciseShell> pagingContainer;

/*
  public interface TableResources extends CellTable.Resources {
    */
/**
     * The styles applied to the table.
     *//*

    interface TableStyle extends CellTable.Style {}

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    */
/**
     * The styles applied to the table.
     *//*

    interface TableStyle extends CellTable.Style {}

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    TableStyle cellTableStyle();
  }
*/

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param controller
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showTurkToken, boolean showInOrder, ExerciseController controller) {
    super(currentExerciseVPanel, service, feedback, null, showTurkToken, showInOrder);
    this.controller = controller;
    addComponents();
  }

  protected void addComponents() {
    PagingContainer<? extends ExerciseShell> exerciseShellPagingContainer = makePagingContainer();
    System.out.println("made container " + exerciseShellPagingContainer);
    addTableWithPager(exerciseShellPagingContainer);
  }

  protected PagingContainer<? extends ExerciseShell> makePagingContainer() {
    final PagingExerciseList outer = this;
    PagingContainer<ExerciseShell> pagingContainer1 = new PagingContainer<ExerciseShell>(controller) {
      @Override
      protected void gotClickOnItem(ExerciseShell e) {
        outer.gotClickOnItem(e);
      }

      @Override
      protected void loadFirstExercise() {
        outer.loadFirstExercise();

        selectFirst();    //To change body of overridden methods use File | Settings | File Templates.
      }
    };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  protected void addTableWithPager(PagingContainer<? extends ExerciseShell> pagingContainer) {
    System.out.println("addTableWithPager container " + pagingContainer);

    Panel container = pagingContainer.addTableWithPager();
    add(container);
 /*   makeCellTable();

    // Create a data provider.
    this.dataProvider = new ListDataProvider<ExerciseShell>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    FlowPanel column = new FlowPanel();
    add(column);
    column.add(pager);
    column.add(table);*/
  }

/*  protected CellTable<Exercise> makeCellTable() {
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
  }*/

/*  protected void addColumnsToTable(boolean consumeClicks) {
    Column<ExerciseShell, SafeHtml> id2 = getExerciseIdColumn(consumeClicks);

    // this would be better, but want to consume clicks
  *//*  TextColumn<ExerciseShell> id2 = new TextColumn<ExerciseShell>() {
      @Override
      public String getValue(ExerciseShell exerciseShell) {
        String columnText =  exerciseShell.getTooltip();
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0,MAX_LENGTH_ID-3)+"...";

        return columnText;
      }
    };*//*

    id2.setCellStyleNames("alignLeft");
    table.addColumn(id2);
  }*/

  /**
   * @seex #addColumnsToTable
   * @return
   */
/*  private Column<ExerciseShell, SafeHtml> getExerciseIdColumn(final boolean consumeClicks) {
    return new Column<ExerciseShell, SafeHtml>(new SafeHtmlCell() {
        @Override
        public Set<String> getConsumedEvents() {
          Set<String> events = new HashSet<String>();
          if (consumeClicks) events.add(BrowserEvents.CLICK);
          return events;
        }
      }) {
        @Override
        public SafeHtml getValue(ExerciseShell object) {
          return getColumnToolTip(object.getTooltip());
        }

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

        private SafeHtml getColumnToolTip(String columnText) {
          if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0,MAX_LENGTH_ID-3)+"...";
          return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
        }
      };
  }*/

  protected void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  protected String getHistoryToken(String id) { return "item=" +id; }

  protected void gotClickOnItem(final ExerciseShell e) {
   // pushNewItem(e.getID());

    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(currentExercise);
    } else {
      pushNewItem(e.getID());
    }
  }

  /**
   * @see SectionExerciseList.MySetExercisesCallback#onSuccess
   */
 /* @Override
  protected void loadFirstExercise() {
    super.loadFirstExercise();
    selectFirst();
  }*/
/*
  protected void selectFirst() {
    table.getSelectionModel().setSelected(currentExercises.get(0), true);
    table.redraw();
    onResize();
  }*/

  public void clear() {
   /* List<ExerciseShell> list = dataProvider.getList();
    List<ExerciseShell> copy = new ArrayList<ExerciseShell>();

    for (ExerciseShell es : list) copy.add(es);
    for (ExerciseShell es : copy) list.remove(es);
    table.setRowCount(list.size());*/
    pagingContainer.clear();
  }

  @Override
  public void flush() {
/*    dataProvider.flush();
    table.setRowCount(dataProvider.getList().size());*/
    pagingContainer.flush();
  }

  @Override
  protected void addExerciseToList(ExerciseShell exercise) {
/*    List<ExerciseShell> list = dataProvider.getList();
    list.add(exercise);*/
    pagingContainer.addExerciseToList2(exercise);
  }

  @Override
  public void onResize() {
    super.onResize();
    pagingContainer.onResize(currentExercise);
/*    System.out.println("Got on resize " + Window.getClientHeight() + " " +
        getOffsetHeight() + " bodyheight = " + table.getBodyHeight() + " table offset height " + table.getOffsetHeight() + " parent height " + getParent().getOffsetHeight());*/
/*    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      //System.out.println("num rows now " + numRows);
      table.setPageSize(numRows);
      table.redraw();
      markCurrentExercise(currentExercise);
    }*/
  }

/*  protected int getNumTableRowsGivenScreenHeight() {
    int header = getTableHeaderHeight();
    int leftOver = Window.getClientHeight() - header - 100;

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
  }*/

/*  protected int heightOfCellTableWith15Rows() {
    return HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }*/
/*
  protected int getTableHeaderHeight() {
    return controller.getHeightOfTopRows();
  }*/

  /**
   * not sure how this happens, but need Math.max(0,...)
   *
   * @see ExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param i
   */
 @Override
  protected void markCurrentExercise(int i) {
   pagingContainer.markCurrentExercise(i);
 }
   /*
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
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
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
  }*/
}
