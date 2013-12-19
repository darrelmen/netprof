package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Modal;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.Grade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show a dialog with all the results we've collected so far.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager extends PagerTable {
  private static final int PAGE_SIZE = 12;
  protected static final String UNGRADED = "Ungraded";
  protected static final String SKIP = "Skip";
  public static final int GRADING_WIDTH = 700;
  private final boolean textResponse;
  protected int pageSize = PAGE_SIZE;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  private final AudioTag audioTag = new AudioTag();
  private String nameForAnswer;
  private Map<Column<?,?>,String> colToField = new HashMap<Column<?,?>, String>();

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param feedback
   * @param nameForAnswer
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback, String nameForAnswer, PropertyHandler propertyHandler) {
    this.service = s;
    this.feedback = feedback;
    this.nameForAnswer = nameForAnswer;
    textResponse = propertyHandler.isFlashcardTextResponse();
  }

  public void setPageSize(int s) { this.pageSize = s; }

  private Widget lastTable = null;
  private Button closeButton;

  /**
   * @see mitll.langtest.client.LangTest#makeLogoutParts
   */
  public void showResults() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(nameForAnswer+"s");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 40;
    int top  = (Window.getClientHeight()) / 40;
    dialogBox.setPopupPosition(left, top);
    dialogVPanel.setWidth("100%");
    //dialogBox.setWidth((int)((float)Window.getClientWidth()*0.85f) + "px");
    //  dialogBox.setWidth("100%");

    service.getNumResults(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Integer result) {
        populateTableOld(result, dialogVPanel, dialogBox);
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
   * Experimental
   * @deprecated not ready
   */
/*  private void showResultsNew() {
    // Create the popup dialog box
    //   final DialogBox dialogBox = new DialogBox();
    final Modal dialogBox = new Modal(false);
    //  dialogBox.setText(nameForAnswer+"s");
    dialogBox.setTitle(nameForAnswer+"s");
    dialogBox.setCloseVisible(true);
    dialogBox.setKeyboard(false);
    // Enable glass background.
    // dialogBox.setGlassEnabled(true);

 *//*   closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");*//*

    //final VerticalPanel dialogVPanel = new VerticalPanel();
*//*
    int left = (Window.getClientWidth()) / 40;
    int top  = (Window.getClientHeight()) / 160;
    // dialogBox.setPopupPosition(left, top);
    dialogVPanel.setWidth("100%");*//*
    //  dialogBox.setWidth((int)((float)Window.getClientWidth()*0.9f) + "px");

    service.getNumResults(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Integer result) {
        populateTable(result, *//*dialogVPanel,*//* dialogBox);
      }
    });

    // dialogBox.setWidget(dialogVPanel);
   // dialogBox.add(dialogVPanel);

    // Add a handler to send the name to the server
 *//*   closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });*//*
  }*/

  private SafeHtml getURL2() {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href='" +
      "downloadResults" +
      "'" +
      ">");
    sb.appendEscaped("Download Excel");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }

  private void populateTableOld(int numResults, Panel dialogVPanel, DialogBox dialogBox) {
    if (lastTable != null) {
      dialogVPanel.remove(lastTable);
      dialogVPanel.remove(closeButton);
    }

    Widget table = getAsyncTable(numResults, !textResponse, new ArrayList<Grade>(), -1, 1);
    table.setWidth("100%");

    dialogVPanel.add(new Anchor(getURL2()));

    dialogVPanel.add(table);
    dialogVPanel.add(closeButton);

    lastTable = table;
    dialogBox.show();
  }

  private void populateTable(int numResults, //Panel dialogVPanel,
                             //   DialogBox dialogBox
                             Modal dialogBox
  ) {
   /* if (lastTable != null) {
      dialogVPanel.remove(lastTable);
      dialogVPanel.remove(closeButton);
    }*/

    Widget table = getAsyncTable(numResults, true, new ArrayList<Grade>(),-1, 1);
//    dialogVPanel.add(table);
  //  dialogVPanel.add(closeButton);

//    lastTable = table;

    dialogBox.add(table);
    dialogBox.show();
  }

  /**
   * @see GradingExercisePanel#showResults
   * @param result
   * @param showQuestionColumn
   * @param grades
   * @param grader
   * @param numGrades
   * @return
   */
  public Widget getTable(Collection<Result> result, boolean showQuestionColumn,
                         Collection<Grade> grades, final int grader, int numGrades) {
    CellTable<Result> table = getResultCellTable(result, showQuestionColumn, grades, grader, numGrades);
    return getPagerAndTable(table);
  }

  protected CellTable<Result> getResultCellTable(Collection<Result> result,
                                                 boolean showQuestionColumn, Collection<Grade> grades, int grader, int numGrades) {
    CellTable<Result> table = new CellTable<Result>();
    TextColumn<Result> id = addColumnsToTable(showQuestionColumn, grades, grader, numGrades, table);

    // Create a data provider.
    List<Result> list = createProvider(result, table);

    // Add a ColumnSortEvent.ListHandler to connect sorting to the
    // java.util.List.
    addSorter(table, id, list);
    return table;
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

  private Widget getAsyncTable(int numResults, boolean showQuestionColumn,
                         Collection<Grade> grades, final int grader, int numGrades) {
    CellTable<Result> table = new CellTable<Result>();
    TextColumn<Result> id = addColumnsToTable(showQuestionColumn, grades, grader, numGrades, table);
    table.setRowCount(numResults, true);
    table.setVisibleRange(0,15);
    createProvider(numResults, table);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the
    // AsyncDataPRrovider.
    ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(table);
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

    // Create a SimplePager.
    return getPagerAndTable(table);
  }

  private TextColumn<Result> addColumnsToTable(boolean showQuestionColumn, Collection<Grade> grades,
                                               int grader, int numGrades, CellTable<Result> table) {
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

    table.addColumn(audioFile, nameForAnswer);
    colToField.put(audioFile,"answer");

    addResultColumn(grades, grader, numGrades, table);
    return id;
  }

  /**
   * @see #getAsyncTable(int, boolean, java.util.Collection, int, int)
   * @param numResults
   * @param table
   * @return
   */
  private AsyncDataProvider<Result> createProvider(final int numResults, final CellTable<Result> table) {
    AsyncDataProvider<Result> dataProvider = new AsyncDataProvider<Result>() {
      @Override
      protected void onRangeChanged(HasData<Result> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //System.out.println("asking for " + start +"->" + end);

        StringBuilder builder = getColumnSortedState(table);
        service.getResults(start, end, builder.toString(), new AsyncCallback<List<Result>>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Can't contact server.");
          }
          @Override
          public void onSuccess(List<Result> result) {
//            System.out.println("createProvider : onSuccess for " + start +"->" + fend + " got " + result.size());
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

  private StringBuilder getColumnSortedState(CellTable<Result> table) {
    final ColumnSortList sortList = table.getColumnSortList();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < sortList.size(); i++) {
      ColumnSortList.ColumnSortInfo columnSortInfo = sortList.get(i);
      Column<?, ?> column = columnSortInfo.getColumn();
      builder.append(colToField.get(column) +"_"+(columnSortInfo.isAscending()?"ASC":"DESC")+",");
    }
    return builder;
  }

  protected TextColumn<Result> addUserPlanExercise(CellTable<Result> table) {
    TextColumn<Result> id = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        if (answer == null) {
          System.err.println("huh? answer is null??");
          return "";
        }
        else {
          return "" + answer.userid;
        }
      }
    };
    id.setSortable(true);
    table.addColumn(id, "User ID");
    colToField.put(id, "userid");

    TextColumn<Result> exercise = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return answer.id;
      }
    };
    exercise.setSortable(true);
    table.addColumn(exercise, "Exercise");
    colToField.put(exercise,"id");

    return id;
  }

  /**
   *
   * @param grader used in GradingResultManager subclass
   * @param grader used in GradingResultManager subclass
   * @param numGrades used in GradingResultManager subclass
   * @param table to add columns to
   */
  protected void addResultColumn(Collection<Grade> grades, int grader, int numGrades, CellTable<Result> table) {
    addNoWrapColumn(table);

    if (!textResponse) {
      TextColumn<Result> audioType = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return answer.getAudioType();// ? "Fast and Slow" : "Regular";
        }
      };
      audioType.setSortable(true);
      table.addColumn(audioType, "Audio Type");
      colToField.put(audioType, "audioType");

      TextColumn<Result> dur = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          float secs = ((float) answer.durationInMillis) / 1000f;
          //  System.out.println("Value " +answer.durationInMillis + " or " +secs);
          return "" + roundToHundredth(secs);
        }
      };
      dur.setSortable(true);
      table.addColumn(dur, "Duration (Sec)");
      colToField.put(dur, "durationInMillis");

      TextColumn<Result> valid = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return "" + answer.valid;
        }
      };
      valid.setSortable(true);
      table.addColumn(valid, "Valid");
      colToField.put(valid,"valid");
    }

    TextColumn<Result> gradeInfo = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        if (answer.gradeInfo.endsWith(",")) {
          return answer.gradeInfo.substring(0, answer.gradeInfo.length() - 1);
        } else {
          return answer.gradeInfo;
        }
      }
    };
    table.addColumn(gradeInfo, "Grades");
    colToField.put(gradeInfo, "gradeInfo");

    TextColumn<Result> correct = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return "" + answer.isCorrect();
      }
    };
    correct.setSortable(true);
    table.addColumn(correct, "Correct");
    colToField.put(correct, "correct");

    TextColumn<Result> pronScore = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        return "" + roundToHundredth(answer.getPronScore());
      }
    };
    pronScore.setSortable(true);
    table.addColumn(pronScore, "ProFScore");
    colToField.put(pronScore, "pronScore");
  }

  private void addNoWrapColumn(CellTable<Result> table) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<Result,SafeHtml> dateCol = new Column<Result, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(Result answer) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        sb.appendHtmlConstant("<div style='white-space: nowrap;'><span>" +
          new Date(answer.timestamp)+
          "</span>" );

        sb.appendHtmlConstant("</div>");
        return sb.toSafeHtml();
      }
    };
    table.addColumn(dateCol, "Time");
    dateCol.setSortable(true);
    colToField.put(dateCol,"timestamp");

  }

/*  private void addNoWrapColumn2(CellTable<Result> table,String label) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<Result,SafeHtml> dateCol = new Column<Result, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(Result answer) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        sb.appendHtmlConstant("<div style='white-space: nowrap;'><span>" +
         answer.getStimulus()+
          "</span>" );

        sb.appendHtmlConstant("</div>");
        return sb.toSafeHtml();
      }
    };
    table.addColumn(dateCol, label);
  }*/

  private float roundToHundredth(double totalHours) {
    return ((float)((Math.round(totalHours*100))))/100f;
  }

  private Panel getPagerAndTable(CellTable<Result> table) {
     return getOldSchoolPagerAndTable(table, table, pageSize, 1000);
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
