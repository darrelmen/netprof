package mitll.langtest.client;

import com.google.gwt.cell.client.*;
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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager {
  private static final int PAGE_SIZE = 12;
  private int pageSize = PAGE_SIZE;
  private LangTestDatabaseAsync service;
  private UserFeedback feedback;
  private GradingExercisePanel panel;
  private Set<Integer> remainingResults = new HashSet<Integer>();

  /**
   * @see GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, ExerciseController, int)
   * @param s
   * @param feedback
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback) {
    this.service = s;
    this.feedback = feedback;
  }

  public void setFeedback(GradingExercisePanel panel) {
    this.panel = panel;
  }

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
    dialogVPanel.setWidth("1200px");
    dialogBox.setWidth("1200px");

    int left = (Window.getClientWidth()) / 10;
    int top  = (Window.getClientHeight()) / 10;
    dialogBox.setPopupPosition(left, top);

    service.getResults(new AsyncCallback<List<Result>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(List<Result> result) {
        if (lastTable != null) {
          dialogVPanel.remove(lastTable);
          dialogVPanel.remove(closeButton);
        }

        Widget table = getTable(result, false, true, new ArrayList<Grade>());
        dialogVPanel.add(table);
        dialogVPanel.add(closeButton);

        lastTable = table;
        dialogBox.show();
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

  /**
   * @see GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, ExerciseController, int)
   * @param result
   * @param gradingView
   * @param showQuestionColumn
   * @param grades
   * @return
   */
  public Widget getTable(Collection<Result> result, final boolean gradingView, boolean showQuestionColumn,
                         Collection<Grade> grades) {
    remainingResults.clear();

/*    for (Result r: result) {
      remainingResults.add(r.uniqueID);
    }*/

    CellTable<Result> table = new CellTable<Result>();
    table.setWidth(gradingView ? "1000px" : "1200px");
    TextColumn<Result> id = null;
    if (!gradingView) {
      id = new TextColumn<Result>() {
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
    }
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
          SafeHtml audioTag = getAudioTag(answer.answer);
          return audioTag;
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

    if (!gradingView) {
      TextColumn<Result> date = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return "" + new Date(answer.timestamp);
        }
      };
      date.setSortable(true);
      table.addColumn(date, "Time");
    }
    else {
      final Map<Integer,Integer> resultToGrade = new HashMap<Integer, Integer>();
      for (Grade g : grades) resultToGrade.put(g.resultID,g.grade);

     // System.out.println("made r->g : " + resultToGrade);

      SelectionCell selectionCell = new SelectionCell(Arrays.asList("Ungraded", "1", "2", "3", "4", "5", "Skip"));
      Column<Result, String> col = new Column<Result, String>(selectionCell) {
        @Override
        public String getValue(Result object) {
          Integer grade = resultToGrade.get(object.uniqueID);
          String s = grade == null ? "Ungraded" : grade == -1 ? "Ungraded" : grade == -2 ? "Skip" : "" + grade;
         // System.out.println("current grade for : " + object + " is " + s);

          return s;
        }
      };
      col.setFieldUpdater(new FieldUpdater<Result, String>() {
        public void update(int index, final Result object, String value) {
          int grade = -1;
          if (value.equals("Ungraded")) grade = -1;
          else if (value.equals("Skip")) grade = -2;
          else {
            try {
              grade = Integer.parseInt(value);
            } catch (NumberFormatException e) {
              System.err.println("setFieldUpdater : couldn't parse " + value +"??");
            }
          }
         // System.out.println("adding grade " + grade + " for : " + object);
          resultToGrade.put(object.uniqueID,grade);
          addGrade(object, grade);
        }
      });
      table.addColumn(col, "Grade");
    }
    // Create a data provider.
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

    // Add a ColumnSortEvent.ListHandler to connect sorting to the
    // java.util.List.
    addSorter(table, id, list);

    // We know that the data is sorted alphabetically by default.
   // table.getColumnSortList().push(id);

    // Create a SimplePager.
    return getPager(table);
  }

  private VerticalPanel getPager(CellTable<Result> table) {
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(pageSize);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(table);
    return vPanel;
  }

  private void addGrade(final Result object, int grade) {
    service.addGrade(object.uniqueID, object.id, grade,true,new AsyncCallback<Integer>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Integer result) {   // TODO show check box?
        feedback.showStatus("Now "+result + " graded answers.");
        remainingResults.remove(object.uniqueID);
        if (remainingResults.isEmpty()) {
         // panel.recordCompleted(panel);
        }
      }
    });
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

  /**
   * @see #getTable(java.util.Collection, boolean, boolean, java.util.Collection)
   * @param result
   * @return
   */
  private SafeHtml getAudioTag(String result) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<audio preload=\"none\" controls=\"controls\" tabindex=\"0\">\n" +
        "<source type=\"audio/wav\" src=\"" + result + "\"></source>\n" +
        "<source type=\"audio/mp3\" src=\"" + result.replace(".wav",".mp3") + "\"></source>\n" +
        // "<source type=\"audio/ogg\" src=\"media/ac-LC1-009/ac-LC1-009-C.ogg\"></source>\n" +
      "Your browser does not support the audio tag.\n" +
      "</audio>");
    return sb.toSafeHtml();
  }
}
