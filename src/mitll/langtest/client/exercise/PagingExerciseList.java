package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
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
public class PagingExerciseList extends ExerciseList implements RequiresResize {
  private static final int PAGE_SIZE = 15;   // TODO : make this sensitive to vertical real estate?
  private ListDataProvider<ExerciseShell> dataProvider;
  public static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390;
  private static final float MAX_PAGES = 2f;
  private static final int MIN_PAGE_SIZE = 3;
  private static final float DEFAULT_PATH_SIZE = 15f;
  private CellTable<ExerciseShell> table;

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

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param arabicDataCollect
   * @param showTurkToken
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean arabicDataCollect,
                            boolean showTurkToken, boolean showInOrder) {
    super(currentExerciseVPanel, service, feedback, null, arabicDataCollect, showTurkToken, showInOrder);
    CellTable.Resources o = GWT.create(TableResources.class);
    this.table = new CellTable<ExerciseShell>(PAGE_SIZE, o);
    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

    table.setWidth("100%", true);
    table.setHeight("auto");

    // Add a selection model to handle user selection.
    final SingleSelectionModel<ExerciseShell> selectionModel = new SingleSelectionModel<ExerciseShell>();
    table.setSelectionModel(selectionModel);

    Column<ExerciseShell, SafeHtml> id2 = new Column<ExerciseShell, SafeHtml>(new SafeHtmlCell() {
      @Override
      public Set<String> getConsumedEvents() {
        Set<String> events = new HashSet<String>();
        events.add("click");
        return events;
      }
    }) {
      @Override
      public SafeHtml getValue(ExerciseShell object) {
        return getColumnToolTip(object.getID(), object.getTooltip());
      }

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, ExerciseShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if ("click".equals(event.getType())) {
          final ExerciseShell e = object;
          if (isExercisePanelBusy()) {
            Window.alert("Exercise busy.");
            markCurrentExercise(currentExercise);
          } else {
            Timer timer = new Timer() {
              @Override
              public void run() {
                loadExercise(e);
              }
            };
            timer.schedule(100);
          }
        }
      }

      private SafeHtml getColumnToolTip(String columnText, String toolTipText) {
        String htmlConstant = "<html>" + "<head><style>" +
            "A.tip { TEXT-DECORATION: none; color:#1776B3}" +
            "A.tip:hover  {CURSOR:default;}" +
            "A.tip span   {DISPLAY:none}" +
            "A.tip span p {background:#d30300;color:#fff;font-weight:500;border-radius:5px;padding:5px;font-size:12px}" +
            "A.tip:hover span {border:1px solid #e6e3e5;DISPLAY: block;Z-INDEX: 1000; PADDING: 0px 10px 0px 10px;" +
            //"POSITION:absolute;float:left;background:#ffffd1;   TEXT-DECORATION: none}" +
            "POSITION:absolute;background:#ffffd1;   TEXT-DECORATION: none}" +
            "</style></head>" +
            "<body>" +
            "<a href=\"#\" class=\"tip\">" + columnText + "<span>" + toolTipText + "</span></a>" + "</body>" + "</html>";
        return new SafeHtmlBuilder().appendHtmlConstant(htmlConstant).toSafeHtml();
      }
    };

    table.addColumn(id2);

    // Create a data provider.
    this.dataProvider = new ListDataProvider<ExerciseShell>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);

    add(pager);
    add(table);
  }

  @Override
  protected void loadFirstExercise() {
    super.loadFirstExercise();
    selectFirst();
  }

  protected void selectFirst() {
    table.getSelectionModel().setSelected(currentExercises.get(0), true);
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
      int header = 625 - HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
      int leftOver = Window.getClientHeight()-header;
      float rawRatio = ((float) leftOver) / (float)HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
      float tableRatio = Math.min(MAX_PAGES, rawRatio);
     // System.out.println("left over " + leftOver + " raw " + rawRatio + " table ratio " + tableRatio);

      float ratio = DEFAULT_PATH_SIZE * tableRatio;
      int numRows = Math.max(MIN_PAGE_SIZE, Math.round(ratio));
      if (table.getPageSize() != numRows) {
       // System.out.println("num rows now " + numRows);
        table.setPageSize(numRows);
        table.redraw();
        markCurrentExercise(currentExercise);
      }
  }

  /**
   * @see ExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param i
   */
  @Override
  protected void markCurrentExercise(int i) {
    ExerciseShell itemToSelect = currentExercises.get(i);
   // System.out.println(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect + " with clicked on " + clickedOn);
    table.getSelectionModel().setSelected(itemToSelect, true);
    int pageEnd = table.getPageStart() + table.getPageSize();
/*    System.out.println("marking " +i +" out of " +table.getRowCount() + " page start " +table.getPageStart() +
        " end " + pageEnd);*/

    if (i < table.getPageStart()) {
      int newStart = Math.max(0, table.getPageStart() - table.getPageSize());
    //   System.out.println("new start of prev page " +newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      if (i >= pageEnd) {
        int newStart = Math.min(table.getRowCount() - table.getPageSize(), pageEnd);
      //  System.out.println("new start of next page " +newStart + " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, table.getPageSize());
      }
    }
    table.redraw();
  }
}
