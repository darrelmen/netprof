package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 4/23/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableSectionExerciseList extends FlexSectionExerciseList {
  public TableSectionExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                  UserFeedback feedback, boolean showTurkToken, boolean showInOrder,
                                  boolean showListBox, ExerciseController controller) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox, controller);
    setWidth("100%");
  }

  public void doInitial(final Map<String, Collection<String>> typeToSection) {
    service.getNumExercisesForSelectionState(typeToSection, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Integer result) {
        System.out.println("num exercises for " + typeToSection + " is " + result);
        removeComponents();
        FlowPanel p = new FlowPanel();
        p.setWidth("100%");
        Widget asyncTable = getAsyncTable(typeToSection, result);
        p.add(asyncTable);
        add(p);
      }
    });
  }

  @Override
  public void clear() {
    System.out.println("remove all components ");

    removeComponents();
  }

  protected void noSectionsGetExercises(long userID) {
    Map<String, Collection<String>> objectObjectMap = Collections.emptyMap();
    doInitial(objectObjectMap);
  }

    @Override
  protected void addTableWithPager() {  }

  private CellTable<Exercise> table;
  private Widget getAsyncTable(Map<String, Collection<String>> typeToSection,int numResults) {
    CellTable<Exercise> table = makeCellTable();
    table.setWidth("100%");
    table.setRowCount(numResults, true);
    int numRows = getNumTableRowsGivenScreenHeight();

    table.setVisibleRange(0,Math.min(numResults,numRows));
    createProvider(typeToSection, numResults,table);

    this.table = table;

    Panel pagerAndTable = getPagerAndTable(table);
    pagerAndTable.setWidth("100%");
    return pagerAndTable;
  }

  private AsyncDataProvider<Exercise> createProvider(final Map<String, Collection<String>> typeToSection,
                                                     final int numResults, CellTable<Exercise> table) {
    AsyncDataProvider<Exercise> dataProvider = new AsyncDataProvider<Exercise>() {
      @Override
      protected void onRangeChanged(HasData<Exercise> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //System.out.println("asking for " + start +"->" + end);
        final int fend = end;
        service.getFullExercisesForSelectionState(typeToSection, start, end, new AsyncCallback<List<Exercise>>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Can't contact server.");
          }

          @Override
          public void onSuccess(List<Exercise> result) {
            System.out.println("createProvider : onSuccess for " + start + "->" + fend + " got " + result.size());

            updateRowData(start, result);
            onResize();
          }
        });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);

    return dataProvider;
  }

  private Panel getPagerAndTable(CellTable<Exercise> table) {
    return new PagerTable().getPagerAndTable(table, table, PAGE_SIZE, 1000);
  }

  @Override
  protected CellTable<Exercise> makeCellTable() {
    CellTable<Exercise> table = new CellTable<Exercise>(PAGE_SIZE);

    table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

     table.setWidth("100%", true);
    table.setHeight("auto");

    // Add a selection model to handle user selection.
    final SingleSelectionModel<Exercise> selectionModel = new SingleSelectionModel<Exercise>();
    table.setSelectionModel(selectionModel);

    addColumnsToTable(table);

    return table;

  }

  protected void addColumnsToTable(CellTable<Exercise> table) {
    TextColumn<Exercise> english = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        return "" + exercise.getEnglishSentence();
      }
    };
    english.setSortable(true);
    table.addColumn(english, "Word/Expression");
    table.setColumnWidth(english,33, Style.Unit.PCT);

    TextColumn<Exercise> flword = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        return "" + exercise.getRefSentence();
      }
    };
    flword.setSortable(true);
    table.addColumn(flword, controller.getLanguage());
    table.setColumnWidth(flword, 33, Style.Unit.PCT);

    TextColumn<Exercise> translit = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        return "" + exercise.getTranslitSentence();
      }
    };
    translit.setSortable(true);
    table.addColumn(translit, "Transliteration");
    table.setColumnWidth(translit,33, Style.Unit.PCT);
  }


  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override

  protected void loadExercises(Map<String, Collection<String>> typeToSection, final String item) {
    System.out.println("loadExercises " + typeToSection + " and item '" + item + "'");
    doInitial(typeToSection);
  }

  @Override
  public void onResize() {
    setScrollPanelWidth();

    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      System.out.println("onResize : num rows now " + numRows);
      table.setPageSize(numRows);
      table.redraw();
    }
  }

  @Override
  protected int heightOfCellTableWith15Rows() {
    return 700;
  }
}
