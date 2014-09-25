package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.*;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.TypeAhead;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.ResultAndTotal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
  private static final String CORRECT = "Correct";
  private static final String PRO_F_SCORE = "ProFScore";
  private static final String DURATION_SEC = "Duration (Sec)";
  private static final String AUDIO_TYPE = "Audio Type";
  private static final String USER_ID = "User";// ID";
  private static final String DESC = "DESC";
  private static final String ASC = "ASC";


  private static final String ANSWER = "answer";
  private static final String USERID = MonitorResult.USERID;
  private static final String ID = MonitorResult.ID;
  private static final String TEXT = MonitorResult.TEXT;
  private static final String CORRECT1 = MonitorResult.CORRECT;
  private static final String VALID = MonitorResult.VALID;
  private static final String DURATION_IN_MILLIS = MonitorResult.DURATION_IN_MILLIS;
  private static final String AUDIO_TYPE1 = MonitorResult.AUDIO_TYPE;
  private static final String PRON_SCORE = MonitorResult.PRON_SCORE;
  public static final String CLOSE = "Close";
  public static final int MAX_TO_SHOW = 15;

  private final boolean textResponse;
  private final EventRegistration eventRegistration;
  protected int pageSize = PAGE_SIZE;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private final AudioTag audioTag = new AudioTag();
  private final String nameForAnswer;
  private final Map<Column<?, ?>, String> colToField = new HashMap<Column<?, ?>, String>();
  private Collection<String> typeOrder;

  /**
   * @param s
   * @param feedback
   * @param nameForAnswer
   * @param eventRegistration
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback, String nameForAnswer,
                       PropertyHandler propertyHandler, Collection<String> typeOrder, EventRegistration eventRegistration) {
    this.service = s;
    this.feedback = feedback;
    this.nameForAnswer = nameForAnswer;
    textResponse = propertyHandler.isFlashcardTextResponse();
    this.typeOrder = typeOrder;
    this.eventRegistration = eventRegistration;
  }

  //private Widget lastTable = null;
 // private Button closeButton;
  Map<String,SuggestBox> typeToSuggest = new HashMap<String, SuggestBox>();
  private SuggestBox userIDSuggest, textSuggest;

  /**
   * @see mitll.langtest.client.LangTest.ResultsClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  public void showResults() {
    typeToSuggest = new HashMap<String, SuggestBox>();
    userIDSuggest = null;
    textSuggest = null;

    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(nameForAnswer + "s");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

   // final Button closeButton = ;

    final Panel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 40;
    int top = (Window.getClientHeight()) / 40;
    dialogBox.setPopupPosition(left, top);
    dialogVPanel.setWidth("100%");

    service.getNumResults(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Integer num) {
        populateTableOld(num, dialogVPanel, dialogBox,getCloseButton(dialogBox));
      }
    });

    dialogBox.setWidget(dialogVPanel);
  }

  private Button getCloseButton(final DialogBox dialogBox) {
    final Button closeButton = new Button(CLOSE);
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");
    eventRegistration.register(closeButton,"N/A","Close recordings dialog");
    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    return closeButton;
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
  private void populateTableOld(int numResults, Panel dialogVPanel, DialogBox dialogBox, Button closeButton) {
    dialogVPanel.clear();

    final Widget table = getAsyncTable(numResults);
    //    Widget table = createProvider(numResults);
    table.setWidth("100%");

    dialogVPanel.add(new Anchor(getURL2()));
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("search_container");
    dialogVPanel.add(hp);
    //boolean first = true;
    for (final String type : typeOrder) {
      SuggestBox user = new SuggestBox(new SuggestOracle() {
        @Override
        public void requestSuggestions(final Request request, final Callback callback) {
          service.getResultAlternatives(getUnitToValue(), getUserID(), getText(), type, new AsyncCallback<Collection<String>>() {
            @Override
            public void onFailure(Throwable caught) {}

            @Override
            public void onSuccess(Collection<String> result) {
              Collection<Suggestion> suggestions = new ArrayList<Suggestion>();
              for (String resp : result) suggestions.add(new MySuggestion(resp));
              Response resp = new Response(suggestions);

              callback.onSuggestionsReady(request, resp);
            }
          });

        }
      });

      user.addSelectionHandler(getHandler());
      user.addValueChangeHandler(getValueChangeHandler());
      typeToSuggest.put(type,user);
      ControlGroup controlGroup = TypeAhead.getControlGroup(type, user);
      hp.add(controlGroup);
    }

    userIDSuggest = new SuggestBox(new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        service.getResultAlternatives(getUnitToValue(), getUserID(), getText(), MonitorResult.USERID, new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Collection<String> result) {
            Collection<Suggestion> suggestions = new ArrayList<Suggestion>();
            for (String resp : result) suggestions.add(new MySuggestion(resp));
            Response resp = new Response(suggestions);

            callback.onSuggestionsReady(request, resp);
          }
        });

      }
    });
    userIDSuggest.addSelectionHandler(getHandler());
    userIDSuggest.addValueChangeHandler(getValueChangeHandler());


    ControlGroup controlGroup = TypeAhead.getControlGroup("User ID", userIDSuggest);
    hp.add(controlGroup);


    textSuggest = new SuggestBox(new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        service.getResultAlternatives(getUnitToValue(), getUserID(), getText(), MonitorResult.TEXT, new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Collection<String> result) {
            Collection<Suggestion> suggestions = new ArrayList<Suggestion>();
            for (String resp : result) suggestions.add(new MySuggestion(resp));
            Response resp = new Response(suggestions);

            callback.onSuggestionsReady(request, resp);
          }
        });

      }
    });
    textSuggest.addSelectionHandler(getHandler());
    textSuggest.addValueChangeHandler(getValueChangeHandler());

    ControlGroup controlGroup2 = TypeAhead.getControlGroup("Text",textSuggest);
    hp.add(controlGroup2);


    dialogVPanel.add(table);
    dialogVPanel.add(closeButton);

    //lastTable = table;
    dialogBox.show();
  }

  private ValueChangeHandler<String> getValueChangeHandler() {
    return new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        RangeChangeEvent.fire(cellTable, cellTable.getVisibleRange());              //2nd way
      }
    };
  }

  private SelectionHandler<SuggestOracle.Suggestion> getHandler() {
    return new SelectionHandler<SuggestOracle.Suggestion>() {
      @Override
      public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
        RangeChangeEvent.fire(cellTable, cellTable.getVisibleRange());              //2nd way
      }
    };
  }

  private String getText() {
    return textSuggest == null ? "" : textSuggest.getText();
  }

  private long getUserID() {
    return userIDSuggest == null ? -1 : userIDSuggest.getText().isEmpty() ? -1 : Long.parseLong(userIDSuggest.getText());
  }

  private HashMap<String, String> getUnitToValue() {
    HashMap<String, String> unitToValue = new HashMap<String, String>();
    for (String type : typeOrder) {
      SuggestBox suggestBox = typeToSuggest.get(type);
      if (suggestBox != null) {
        String text = suggestBox.getText();
        if (!text.isEmpty()) {
          unitToValue.put(type, text);
        }
      }
    }
    return unitToValue;
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

/*  private List<MonitorResult> createProvider(Collection<MonitorResult> result, CellTable<MonitorResult> table) {
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
  }*/

  CellTable<MonitorResult> cellTable;

  /**
   * @param numResults
   * @return
   * @see #populateTableOld
   */
  private Widget getAsyncTable(//Collection<MonitorResult> results
                               int numResults
  ) {
    cellTable = new CellTable<MonitorResult>();
    cellTable.setSelectionModel(new NoSelectionModel<MonitorResult>());
    addColumnsToTable(cellTable);
    cellTable.setRowCount(numResults, true);
    cellTable.setVisibleRange(0, MAX_TO_SHOW);
    createProvider(numResults, cellTable);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the
    // AsyncDataPRrovider.
    ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(cellTable);
    cellTable.addColumnSortHandler(columnSortHandler);

    Column<?, ?> time = getColumn(TIMESTAMP);
    cellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(time, false));

    // Create a SimplePager.
    return getPagerAndTable(cellTable);
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
    colToField.put(audioFile, ANSWER);
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
  private AsyncDataProvider<MonitorResult> createProvider(final int numResults, final CellTable<MonitorResult> table) {
    AsyncDataProvider<MonitorResult> dataProvider = new AsyncDataProvider<MonitorResult>() {
      @Override
      protected void onRangeChanged(HasData<MonitorResult> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //System.out.println("asking for " + start +"->" + end);

        StringBuilder builder = getColumnSortedState(table);
        HashMap<String, String> unitToValue = getUnitToValue();

        long userID = getUserID();
        String text = getText();
        System.out.println("req " + unitToValue + " user " + userID + " text " + text);

        service.getResults(start, end, builder.toString(), unitToValue, userID, text,new AsyncCallback<ResultAndTotal>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Can't contact server.");
          }
          @Override
          public void onSuccess(ResultAndTotal result) {
            updateRowData(start, result.results);
            cellTable.setRowCount(result.numTotal, true);
          }
        });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);

    return dataProvider;
  }

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
    colToField.put(id, USERID);

    TextColumn<MonitorResult> exercise = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.getId();
      }
    };
    exercise.setSortable(true);
    table.addColumn(exercise, "Exercise");
    colToField.put(exercise, ID);

    TextColumn<MonitorResult> fl = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.getForeignText();
      }
    };
    fl.setSortable(true);
    table.addColumn(fl, "Text");
    colToField.put(fl, TEXT);
    cellTable.setColumnWidth(fl,"180px");

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
      colToField.put(unit, /*"unit_" +*/ type);
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
      colToField.put(audioType, AUDIO_TYPE1);

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
      colToField.put(dur, DURATION_IN_MILLIS);

      TextColumn<MonitorResult> valid = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          return "" + answer.isValid();
        }
      };
      valid.setSortable(true);
      table.addColumn(valid, "Valid");
      colToField.put(valid, VALID);
    }

    TextColumn<MonitorResult> correct = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return "" + answer.isCorrect();
      }
    };
    correct.setSortable(true);
    table.addColumn(correct, CORRECT);
    colToField.put(correct, CORRECT1);

    TextColumn<MonitorResult> pronScore = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return "" + Math.round(100f * answer.getPronScore());
      }
    };
    pronScore.setSortable(true);
    table.addColumn(pronScore, PRO_F_SCORE);
    colToField.put(pronScore, PRON_SCORE);
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

  private static class MySuggestion implements SuggestOracle.Suggestion {
    private final String resp;

    public MySuggestion(String resp) {
      this.resp = resp;
    }

    @Override
    public String getDisplayString() {
      return resp;
    }

    @Override
    public String getReplacementString() {
      return resp;
    }
  }
}
