package mitll.langtest.client.result;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.MonitorResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show a dialog with all the results we've collected so far.
 * <p/>
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager extends PagerTable {
  private static final int PAGE_SIZE = 12;
  //  protected static final String UNGRADED = "Ungraded";
//  protected static final String SKIP = "Skip";
//  protected static final int GRADING_WIDTH = 700;
  protected static final String TIMESTAMP = "timestamp";
  public static final String CORRECT = "Correct";
  public static final String PRO_F_SCORE = "ProFScore";
  public static final String DURATION_SEC = "Duration (Sec)";
  public static final String AUDIO_TYPE = "Audio Type";
  public static final String USER_ID = "User";// ID";
  public static final String DESC = "DESC";
  public static final String ASC = "ASC";

  private final boolean textResponse;
  protected int pageSize = PAGE_SIZE;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private final AudioTag audioTag = new AudioTag();
  private final String nameForAnswer;
  private final Map<Column<?, ?>, String> colToField = new HashMap<Column<?, ?>, String>();
  Collection<String> typeOrder;

  /**
   * @param s
   * @param feedback
   * @param nameForAnswer
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback, String nameForAnswer, PropertyHandler propertyHandler, Collection<String> typeOrder) {
    this.service = s;
    this.feedback = feedback;
    this.nameForAnswer = nameForAnswer;
    textResponse = propertyHandler.isFlashcardTextResponse();
    this.typeOrder = typeOrder;
  }

  private Widget lastTable = null;
  private Button closeButton;

  /**
   * @see mitll.langtest.client.LangTest.ResultsClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  public void showResults() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(nameForAnswer + "s");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 40;
    int top = (Window.getClientHeight()) / 40;
    dialogBox.setPopupPosition(left, top);
    dialogVPanel.setWidth("100%");

    service.getMonitorResults(new AsyncCallback<Collection<MonitorResult>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<MonitorResult> results) {
        populateTableOld(results, dialogVPanel, dialogBox);
      }
    });

    //  service.getExerciseIds();

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
   *
   * @xdeprecated not ready
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

    service.getMonitorResults(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Integer result) {
        populateTable(MonitorResult, *//*dialogVPanel,*//* dialogBox);
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

  /**
   * @param numResults
   * @param dialogVPanel
   * @param dialogBox
   * @see #showResults()
   */
  private void populateTableOld(Collection<MonitorResult> numResults, Panel dialogVPanel, DialogBox dialogBox) {
    if (lastTable != null) {
      dialogVPanel.remove(lastTable);
      dialogVPanel.remove(closeButton);
    }

    Widget table = getAsyncTable(numResults);
    //   Widget table = createProvider(numResults);
    table.setWidth("100%");

    dialogVPanel.add(new Anchor(getURL2()));

    dialogVPanel.add(table);
    dialogVPanel.add(closeButton);

    lastTable = table;
    dialogBox.show();
  }

/*  private void populateTable(int numResults, //Panel dialogVPanel,
                             //   DialogBox dialogBox
                             Modal dialogBox
  ) {
   *//* if (lastTable != null) {
      dialogVPanel.remove(lastTable);
      dialogVPanel.remove(closeButton);
    }*//*

    Widget table = getAsyncTable(numResults, true, new ArrayList<Grade>(),-1, 1);
//    dialogVPanel.add(table);
  //  dialogVPanel.add(closeButton);

//    lastTable = table;

    dialogBox.add(table);
    dialogBox.show();
  }*/

  private List<MonitorResult> createProvider(Collection<MonitorResult> result, CellTable<MonitorResult> table) {
    ListDataProvider<MonitorResult> dataProvider = new ListDataProvider<MonitorResult>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<MonitorResult> list = dataProvider.getList();
    for (MonitorResult answer : result) {
      list.add(answer);
    }
    table.setRowCount(list.size());
    return list;
  }

  /**
   * @param results
   * @return
   * @see #populateTableOld
   */
  private Widget getAsyncTable(Collection<MonitorResult> results) {
    CellTable<MonitorResult> table = new CellTable<MonitorResult>();
    addColumnsToTable(table);
    table.setRowCount(results.size(), true);
    table.setVisibleRange(0, 15);
    createProvider(results, table);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the
    // AsyncDataPRrovider.
    ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(table);
    table.addColumnSortHandler(columnSortHandler);

    Column<?, ?> time = getColumn(TIMESTAMP);
    table.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(time, false));

    // Create a SimplePager.
    return getPagerAndTable(table);
  }

  private Column<?, ?> getColumn(String name) {
    for (Map.Entry<Column<?, ?>, String> pair : colToField.entrySet()) {
      if (pair.getValue().equals(name)) {
        return pair.getKey();
      }
    }
    return null;
  }

  /**
   * @param table
   * @return
   * @seex #getResultCellTable
   * @seex #getAsyncTable(int)
   */

  private TextColumn<MonitorResult> addColumnsToTable(CellTable<MonitorResult> table) {
    TextColumn<MonitorResult> id = addUserPlanExercise(table);

    final AbstractCell<SafeHtml> progressCell = new AbstractCell<SafeHtml>("click") {
      @Override
      public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        if (value != null) {
          sb.append(value);
        }
      }
    };
    Column<MonitorResult, SafeHtml> audioFile = new Column<MonitorResult, SafeHtml>(progressCell) {
      @Override
      public SafeHtml getValue(MonitorResult answer) {
        String answer1 = answer.getAnswer();
        if (answer1.endsWith(".wav")) {
          return audioTag.getAudioTag(answer1);
        } else {
          SafeHtmlBuilder sb = new SafeHtmlBuilder();
          sb.appendHtmlConstant(answer1);
          return sb.toSafeHtml();
        }
      }
    };
    audioFile.setSortable(true);

    table.addColumn(audioFile, nameForAnswer);
    colToField.put(audioFile, "answer");
/*
    TextColumn<MonitorResult> score = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        if (answer == null) {
          System.err.println("huh? answer is null??");
          return "";
        }
        else {
          return "" + roundToHundredth(answer.getPronScore());
        }
      }
    };
    score.setSortable(true);
    table.addColumn(score, "Score");
    colToField.put(score, "score");*/

    addResultColumn(table);
    return id;
  }

  /**
   * @paramx numResults
   * @param table
   * @return
   * @seex #getAsyncTable(int)
   * @seex #getAsyncTable(int)
   */
/*
  private AsyncDataProvider<MonitorResult> createProvider(final int numResults, final CellTable<MonitorResult> table) {
    AsyncDataProvider<MonitorResult> dataProvider = new AsyncDataProvider<MonitorResult>() {
      @Override
      protected void onRangeChanged(HasData<MonitorResult> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //System.out.println("asking for " + start +"->" + end);

        StringBuilder builder = getColumnSortedState(table);
        service.getResults(start, end, builder.toString(), new AsyncCallback<List<MonitorResult>>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Can't contact server.");
          }
          @Override
          public void onSuccess(List<MonitorResult> result) {
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
*/
  private StringBuilder getColumnSortedState(CellTable<MonitorResult> table) {
    final ColumnSortList sortList = table.getColumnSortList();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < sortList.size(); i++) {
      ColumnSortList.ColumnSortInfo columnSortInfo = sortList.get(i);
      Column<?, ?> column = columnSortInfo.getColumn();
      builder.append(colToField.get(column) + "_" + (columnSortInfo.isAscending() ? ASC : DESC) + ",");
    }
    if (!builder.toString().contains(TIMESTAMP)) {
      builder.append(TIMESTAMP + "_" + DESC);
    }
    return builder;
  }

  /**
   * @param table
   * @return
   * @see #addColumnsToTable(com.google.gwt.user.cellview.client.CellTable)
   */
  protected TextColumn<MonitorResult> addUserPlanExercise(CellTable<MonitorResult> table) {
    TextColumn<MonitorResult> id = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        if (answer == null) {
          System.err.println("huh? answer is null??");
          return "";
        } else {
          return "" + answer.getUserid();
        }
      }
    };
    id.setSortable(true);
    table.addColumn(id, USER_ID);
    colToField.put(id, "userid");

    TextColumn<MonitorResult> exercise = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.getId();
      }
    };
    exercise.setSortable(true);
    table.addColumn(exercise, "Exercise");
    colToField.put(exercise, "id");

    TextColumn<MonitorResult> fl = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.getForeignText();
      }
    };
    fl.setSortable(true);
    table.addColumn(fl, "Text");
    colToField.put(fl, "fl");

    for (final String type : typeOrder) {
      TextColumn<MonitorResult> unit = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          Map<String, String> unitToValue = answer.getUnitToValue();
          return unitToValue == null ? "?" :unitToValue.get(type);
        }
      };
      unit.setSortable(true);
      table.addColumn(unit, type);
      colToField.put(unit, "unit_" + type);
    }

    return id;
  }

  /**
   * @param table to add columns to
   */
  protected void addResultColumn(CellTable<MonitorResult> table) {
    addNoWrapColumn(table);

    if (!textResponse) {
      TextColumn<MonitorResult> audioType = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          return answer.getAudioType().equals("avp") ? "flashcard" : answer.getAudioType();
        }
      };
      audioType.setSortable(true);
      table.addColumn(audioType, AUDIO_TYPE);
      colToField.put(audioType, "audioType");

      TextColumn<MonitorResult> dur = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          float secs = ((float) answer.getDurationInMillis()) / 1000f;
          //  System.out.println("Value " +answer.durationInMillis + " or " +secs);
          return "" + roundToHundredth(secs);
        }
      };
      dur.setSortable(true);
      table.addColumn(dur, DURATION_SEC);
      colToField.put(dur, "durationInMillis");

      TextColumn<MonitorResult> valid = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          return "" + answer.isValid();
        }
      };
      valid.setSortable(true);
      table.addColumn(valid, "Valid");
      colToField.put(valid, "valid");
    }

    TextColumn<MonitorResult> correct = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return "" + answer.isCorrect();
      }
    };
    correct.setSortable(true);
    table.addColumn(correct, CORRECT);
    colToField.put(correct, "correct");

    TextColumn<MonitorResult> pronScore = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return "" + Math.round(100f * answer.getPronScore());
      }
    };
    pronScore.setSortable(true);
    table.addColumn(pronScore, PRO_F_SCORE);
    colToField.put(pronScore, "pronScore");
  }

  private void addNoWrapColumn(CellTable<MonitorResult> table) {
    Column<MonitorResult, SafeHtml> dateCol = getDateColumn(table);
    colToField.put(dateCol, TIMESTAMP);
  }

  private Column<MonitorResult, SafeHtml> getDateColumn(CellTable<MonitorResult> table) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<MonitorResult, SafeHtml> dateCol = new Column<MonitorResult, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(MonitorResult answer) {
        return getSafeHTMLForTimestamp(answer.getTimestamp());
      }
    };
    table.addColumn(dateCol, "Time");
    dateCol.setSortable(true);
    return dateCol;
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

  private Panel getPagerAndTable(CellTable<MonitorResult> table) {
    return getOldSchoolPagerAndTable(table, table, pageSize, 1000);
  }
}
