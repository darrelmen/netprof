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
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link Exercise#tooltip}
 *
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingExerciseList extends ExerciseList {
  private static final int PAGE_SIZE = 15;   // TODO : make this sensitive to vertical real estate?
  private ListDataProvider<Exercise> dataProvider;
  private CellTable<Exercise> table;

  public interface TableResources extends CellTable.Resources {

    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {}

    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css" })
    TableStyle cellTableStyle();
  }

  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                             ExercisePanelFactory factory, boolean goodwaveMode, boolean arabicDataCollect, boolean showTurkToken) {
     super(currentExerciseVPanel,service,feedback,factory,goodwaveMode,arabicDataCollect,showTurkToken);

    CellTable.Resources o = GWT.create(TableResources.class);
    this.table = new CellTable<Exercise>(PAGE_SIZE, o);
    table.setWidth("100%",true);

    // Add a selection model to handle user selection.
    final SingleSelectionModel<Exercise> selectionModel = new SingleSelectionModel<Exercise>();
    table.setSelectionModel(selectionModel);

    Column<Exercise, SafeHtml> id2 = new Column<Exercise, SafeHtml>(new SafeHtmlCell() {
      @Override
      public Set<String> getConsumedEvents() {
        Set<String> events = new HashSet<String>();
        events.add("click");
        return events;
      }
    }) {
      @Override
      public SafeHtml getValue(Exercise object) {
      return getColumnToolTip(object.getID(), object.getTooltip());
      }

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, Exercise object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if ("click".equals(event.getType())) {
          loadExercise(object);
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
     this.dataProvider = new ListDataProvider<Exercise>();

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

  private void selectFirst() {
    SelectionModel<? super Exercise> selectionModel = table.getSelectionModel();
    Exercise object = currentExercises.get(0);
    selectionModel.setSelected(object, true);
  }

  @Override
  protected void addExerciseToList(Exercise exercise) {
    List<Exercise> list = dataProvider.getList();
    list.add(exercise);
    table.setRowCount(list.size());
  }

  @Override
  protected void markCurrentExercise(int i) {}
}
