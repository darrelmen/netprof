package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingExerciseList extends ExerciseList {
  ListDataProvider<Exercise> dataProvider;
  CellTable<Exercise> table;

  public interface TableResources extends CellTable.Resources {

    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css" })
    TableStyle cellTableStyle();
  }

  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                             ExercisePanelFactory factory, boolean goodwaveMode, boolean arabicDataCollect, boolean showTurkToken) {
     super(currentExerciseVPanel,service,feedback,factory,goodwaveMode,arabicDataCollect,showTurkToken);

    // this.table = new CellTable<Exercise>();

    CellTable.Resources o = GWT.create(TableResources.class);
    this.table = new CellTable<Exercise>(15, o);
    System.out.println("selection model " +table.getSelectionModel());

    // Add a selection model to handle user selection.
    final SingleSelectionModel<Exercise> selectionModel = new SingleSelectionModel<Exercise>();
    table.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Exercise selected = selectionModel.getSelectedObject();
        System.out.println("selected " + selected);
      }
    });

    // table.getSelectionModel()

    Column<Exercise, String> id2 = new Column<Exercise, String>(new TextCell() {
      @Override
      public Set<String> getConsumedEvents() {
        Set<String> events = new HashSet<String>();
        events.add("click");
        return events;
      }
    }) {
      @Override
      public String getValue(Exercise object) {
        return object.getID();
      }

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, Exercise object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if ("click".equals(event.getType())) {
          System.out.println("loading " +object);
           loadExercise(object);
        }
      }
    };

     //id.setSortable(true);
     table.addColumn(id2, "Item");

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

  @Override
  protected void onLoad() {
    super.onLoad();
  }

  private void selectFirst() {
    SelectionModel<? super Exercise> selectionModel = table.getSelectionModel();
    Exercise object = currentExercises.get(0);
    System.out.println("selection model " + selectionModel + " ex " +object);
    selectionModel.setSelected(object, true);
  }

  @Override
  protected void addExerciseToList(Exercise exercise) {
    List<Exercise> list = dataProvider.getList();
    list.add(exercise);
    table.setRowCount(list.size());
 //   if (list.size() == 1)
    //super.addExerciseToList(e);    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Override
  protected void markCurrentExercise(int i) {
    int pstart = table.getPageStart();
    final int onPage = currentExercise - pstart;
    final int nextOnPage = i - pstart;

    System.out.println("mark current " + i + " page start " + pstart + " onPage " +onPage + " next " +nextOnPage);

    int visibleItemCount = table.getVisibleItemCount();
    System.out.println("visibleItemCount " + visibleItemCount);
/*    //if (visibleItemCount == 0) {
    Timer timer = new Timer() {
      @Override
      public void run() {
        TableRowElement currentHighlightRowElement = table.getRowElement(onPage);
        TableRowElement nextElement = table.getRowElement(nextOnPage);
        currentHighlightRowElement.removeClassName("highlighted");
        nextElement.addClassName("highlighted");
      }
    };
    timer.schedule(100);*/

  }
}
