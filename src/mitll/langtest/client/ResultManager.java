package mitll.langtest.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Show a dialog with all the results we've collected so far.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager {
  private static final int PAGE_SIZE = 12;
  protected static final String UNGRADED = "Ungraded";
  protected static final String SKIP = "Skip";
  private int pageSize = PAGE_SIZE;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  private final AudioTag audioTag = new AudioTag();

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param feedback
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback) {
    this.service = s;
    this.feedback = feedback;
  }

  public void setFeedback(GradingExercisePanel panel) {}
  public void setPageSize(int s) { this.pageSize = s; }

  private Widget lastTable = null;
  private Button closeButton;

  public void showResults() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Answers");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 10;
    int top  = (Window.getClientHeight()) / 10;
    dialogBox.setPopupPosition(left, top);
    Button w = new Button("Last Page");
    dialogVPanel.add(w);
    w.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        pager.lastPage();
      }
    });
    service.getNumResults(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Integer result) {
        populateTable(result, dialogVPanel, dialogBox);
      }
    });

    dialogBox.setWidget(dialogVPanel);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private void populateTable(int numResults, Panel dialogVPanel, DialogBox dialogBox) {
    if (lastTable != null) {
      dialogVPanel.remove(lastTable);
      dialogVPanel.remove(closeButton);
    }

    Widget table = getAsyncTable(numResults, false, true, new ArrayList<Grade>(), "", 1);
    dialogVPanel.add(table);
    dialogVPanel.add(closeButton);

    lastTable = table;
    dialogBox.show();
  }

  /**
   * @see mitll.langtest.client.ResultManager#showResults
   * @see GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param result
   * @param gradingView
   * @param showQuestionColumn
   * @param grades
   * @param grader
   * @param numGrades
   * @return
   */
  public Widget getTable(Collection<Result> result, final boolean gradingView, boolean showQuestionColumn,
                         Collection<Grade> grades, final String grader, int numGrades) {
    CellTable<Result> table = new CellTable<Result>();
    TextColumn<Result> id = addColumnsToTable(gradingView, showQuestionColumn, grades, grader, numGrades, table);

    // Create a data provider.
    List<Result> list = createProvider(result, table);

    // Add a ColumnSortEvent.ListHandler to connect sorting to the
    // java.util.List.
    addSorter(table, id, list);

    // Create a SimplePager.
    return getPager(table);
  }

  private List<Result> createProvider(Collection<Result> result, CellTable<Result> table) {
    ListDataProvider<Result> dataProvider = new ListDataProvider<Result>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<Result> list = dataProvider.getList();
    for (Result answer : result) {
      list.add(answer);
    }
    table.setRowCount(list.size());
    return list;
  }

  private CellTable<Result> actualTable;
  private Widget getAsyncTable(int numResults, final boolean gradingView, boolean showQuestionColumn,
                         Collection<Grade> grades, final String grader, int numGrades) {
    CellTable<Result> table = new CellTable<Result>();
    TextColumn<Result> id = addColumnsToTable(gradingView, showQuestionColumn, grades, grader, numGrades, table);
    table.setRowCount(numResults, true);
    table.setVisibleRange(0,15);
    createProvider(numResults, table);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the
    // AsyncDataPRrovider.
    ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(table);
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

    this.actualTable = table;
    // Create a SimplePager.
    return getPager(table);
  }

  private TextColumn<Result> addColumnsToTable(boolean gradingView, boolean showQuestionColumn, Collection<Grade> grades, String grader, int numGrades, CellTable<Result> table) {
    String gradingWidth = 700 + "px";
    if (!gradingView) {
      int i = (int)(Window.getClientWidth()*0.8f);
      table.setWidth(gradingView ? gradingWidth : i + "px");
    }
    TextColumn<Result> id = addUserPlanExercise(table);
    if (showQuestionColumn) {
      TextColumn<Result> experience = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return "" + answer.qid;
        }
      };
      experience.setSortable(true);
      table.addColumn(experience, "Q. #");
    }

    final AbstractCell<SafeHtml> progressCell = new AbstractCell<SafeHtml>("click") {
      @Override
      public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        if (value != null) {
          sb.append(value);
        }
      }
    };
    Column<Result,SafeHtml> audioFile = new Column<Result,SafeHtml>(progressCell) {
      @Override
      public SafeHtml getValue(Result answer) {
        if (answer.answer.endsWith(".wav")) {
          return audioTag.getAudioTag(answer.answer);
        }
        else {
          SafeHtmlBuilder sb = new SafeHtmlBuilder();
          sb.appendHtmlConstant(answer.answer);
          return sb.toSafeHtml();
        }
      }
    };
    audioFile.setSortable(true);

    table.addColumn(audioFile, "Answer");

    addResultColumn(grades, grader, numGrades, table);
    // Create a data provider.
    return id;
  }

  private AsyncDataProvider<Result> createProvider(final int numResults, CellTable<Result> table) {
    // ListDataProvider<Result> dataProvider = new ListDataProvider<Result>();
    AsyncDataProvider<Result> dataProvider = new AsyncDataProvider<Result>() {
      @Override
      protected void onRangeChanged(HasData<Result> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        service.getResults(start, end, new AsyncCallback<List<Result>>() {
          @Override
          public void onFailure(Throwable caught) {}
          @Override
          public void onSuccess(List<Result> result) {
            updateRowData(start, result);
          }
        });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);

    return dataProvider;
  }

  protected TextColumn<Result> addUserPlanExercise(CellTable<Result> table) {
    TextColumn<Result> id = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return "" + answer.userid;
      }
    };
    id.setSortable(true);
    table.addColumn(id, "User ID");

    TextColumn<Result> age = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return "" + answer.plan;
      }
    };
    age.setSortable(true);
    table.addColumn(age, "Plan");

    TextColumn<Result> gender = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return answer.id;
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "Exercise");
    return id;
  }

  protected void addResultColumn(Collection<Grade> grades, String grader, int numGrades, CellTable<Result> table) {
      TextColumn<Result> date = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return "" + new Date(answer.timestamp);
        }
      };
      date.setSortable(true);
      table.addColumn(date, "Time");

    TextColumn<Result> audioType = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return answer.isFastAndSlowAudio() ? "Fast and Slow" : "Regular";
      }
    };
    audioType.setSortable(true);
    table.addColumn(audioType, "Audio Type");

    TextColumn<Result> dur = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        float secs = ((float) answer.durationInMillis) / 1000f;
      //  System.out.println("Value " +answer.durationInMillis + " or " +secs);
        return "" + secs;
      }
    };
    dur.setSortable(true);
    table.addColumn(dur, "Duration (Sec)");
  }

  private SimplePager pager;
  private VerticalPanel getPager(CellTable<Result> table) {
    SimplePager pager = new SimplePager();
    this.pager = pager;

    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(pageSize);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(table);
    return vPanel;
  }

  private void addSorter(CellTable<Result> table, TextColumn<Result> id, List<Result> list) {
    ColumnSortEvent.ListHandler<Result> columnSortHandler = new ColumnSortEvent.ListHandler<Result>(list);
    columnSortHandler.setComparator(id,
      new Comparator<Result>() {
        public int compare(Result o1, Result o2) {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return +1;
            int res = o1.userid > o2.userid ? +1 :o1.userid < o2.userid ? -1 : 0;
            if (res == 0) {
              res = o1.plan.compareTo(o2.plan);
            }
            if (res == 0) {
              res = o1.id.compareTo(o2.id);
            }
            if (res == 0) {
              res =  o1.qid > o2.qid ? +1 :o1.qid < o2.qid ? -1 : 0;
            }
            if (res == 0) {
              res =  o1.timestamp > o2.timestamp ? +1 :o1.timestamp < o2.timestamp ? -1 : 0;
            }
            return res;
          }
          return -1;
        }
      });
    table.addColumnSortHandler(columnSortHandler);
  }
}
