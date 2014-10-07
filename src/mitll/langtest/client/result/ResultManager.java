package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.RangeChangeEvent;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.TypeAhead;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.ResultAndTotal;

import java.util.*;

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

  private final EventRegistration eventRegistration;
  protected int pageSize = PAGE_SIZE;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private final AudioTag audioTag = new AudioTag();
  private final String nameForAnswer;
  private final Map<Column<?, ?>, String> colToField = new HashMap<Column<?, ?>, String>();
  private Collection<String> typeOrder;
  int req = 0;

  /**
   * @param s
   * @param nameForAnswer
   * @param eventRegistration
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public ResultManager(LangTestDatabaseAsync s, UserFeedback feedback, String nameForAnswer,
                       PropertyHandler propertyHandler, Collection<String> typeOrder, EventRegistration eventRegistration) {
    this.service = s;
    this.feedback = feedback;
    this.nameForAnswer = nameForAnswer;
    this.typeOrder = typeOrder;
    this.eventRegistration = eventRegistration;
  }

  private Map<String, Typeahead> typeToSuggest = new HashMap<String, Typeahead>();
  private Typeahead userIDSuggest, textSuggest;

  /**
   * @see mitll.langtest.client.LangTest.ResultsClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   */
  public void showResults() {
    typeToSuggest = new HashMap<String, Typeahead>();
    userIDSuggest = null;
    textSuggest = null;
    req = 0;

    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(nameForAnswer + "s");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

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
        populateTable(num, dialogVPanel, dialogBox, getCloseButton(dialogBox));
      }
    });

    dialogBox.setWidget(dialogVPanel);
  }

  private Button getCloseButton(final DialogBox dialogBox) {
    final Button closeButton = new Button(CLOSE);
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");
    eventRegistration.register(closeButton, "N/A", "Close recordings dialog");
    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    return closeButton;
  }

  private SafeHtml getURL2() {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href='" + "downloadResults" + "'" + ">");
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
  private void populateTable(int numResults, Panel dialogVPanel, DialogBox dialogBox, Button closeButton) {
    dialogVPanel.clear();

    final Widget table = getAsyncTable(numResults);
    table.setWidth("100%");

    dialogVPanel.add(new Anchor(getURL2()));
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("search_container");
    dialogVPanel.add(hp);

    for (final String type : typeOrder) {
      Typeahead user = new Typeahead(new SuggestOracle() {
        @Override
        public void requestSuggestions(final Request request, final Callback callback) {
          final Map<String, String> unitToValue = getUnitToValue();
          //System.out.println(new Date() + " requestSuggestions got request for " + type + " : " + unitToValue);
          service.getResultAlternatives(unitToValue, getUserID(), getText(), type, new AsyncCallback<Collection<String>>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Collection<String> result) {
              System.out.println(new Date() + " request for " + type + " : " + unitToValue + " yielded " + result.size());
              makeSuggestionResponse(result, callback, request);
            }
          });
        }
      });
      final TextBox w = new TextBox();
      w.getElement().setId("TextBox_" + type);
      user.setWidget(w);
      configureTextBox(w);

      addCallbacks(user);
      typeToSuggest.put(type, user);
      ControlGroup controlGroup = TypeAhead.getControlGroup(type, user.asWidget());
      hp.add(controlGroup);
    }

    userIDSuggest = new Typeahead(new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        //System.out.println(new Date() + " requestSuggestions got request for userid " + getUnitToValue() + " " + getText() + " " + getUserID());

        service.getResultAlternatives(getUnitToValue(), getUserID(), getText(), MonitorResult.USERID, new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Collection<String> result) {
            System.out.println(new Date() + " requestSuggestions got request for userid " + getUnitToValue() + " " + getText() + " " + getUserID() + " yielded " + result.size());
            makeSuggestionResponse(result, callback, request);
          }
        });
      }
    });

    TextBox w = new TextBox();

    userIDSuggest.setWidget(w);
    addCallbacks(userIDSuggest);
    w.getElement().setId("TextBox_userIDSuggest");
    configureTextBox(w);

    ControlGroup controlGroup = TypeAhead.getControlGroup(USER_ID, userIDSuggest.asWidget());
    hp.add(controlGroup);

    textSuggest = new Typeahead(new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        //System.out.println(new Date() + " requestSuggestions got request for txt " + getUnitToValue() + " " + getText() + " " + getUserID());

        service.getResultAlternatives(getUnitToValue(), getUserID(), getText(), MonitorResult.TEXT, new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Collection<String> result) {
            System.out.println(new Date() + " requestSuggestions got request for text " + getUnitToValue() + " " + getText() + " " + getUserID() + " yielded " + result.size());
            makeSuggestionResponse(result, callback, request);

          }
        });
      }
    });
    TextBox w1 = new TextBox();
    w1.setPlaceholder("Word (type or paste) or Item ID");
    w1.setDirectionEstimator(true);
    textSuggest.setWidget(w1);
    configureTextBox(w1);
    addCallbacks(textSuggest);

    w1.getElement().setId("TextBox_textSuggest");

    ControlGroup controlGroup2 = TypeAhead.getControlGroup("Text", textSuggest.asWidget());
    hp.add(controlGroup2);

    dialogVPanel.add(table);
    dialogVPanel.add(closeButton);
    dialogBox.show();
  }

  private void makeSuggestionResponse(Collection<String> result, SuggestOracle.Callback callback, SuggestOracle.Request request) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<SuggestOracle.Suggestion>();
    for (String resp : result) suggestions.add(new MySuggestion(resp));
    callback.onSuggestionsReady(request, new SuggestOracle.Response(suggestions));
  }

  private void configureTextBox(final TextBox w) {
    w.addKeyUpHandler(getKeyUpHandler(w));
  }

  private void addCallbacks(final Typeahead user) {
    user.setUpdaterCallback(getUpdaterCallback());
  }

  /**
   * NOTE : we need both a redraw on key up and one on selection!
   *
   * @param w
   * @return
   */
  private KeyUpHandler getKeyUpHandler(final TextBox w) {
    return new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        System.out.println(new Date() + w.getId() + " KeyUpEvent event " + event + " item " + w.getText() + " " + w.getValue());
        //System.out.println("--> onKeyUp REDRAW ");
        redraw();
      }
    };
  }

  private Typeahead.UpdaterCallback getUpdaterCallback() {
    return new Typeahead.UpdaterCallback() {
      @Override
      public String onSelection(SuggestOracle.Suggestion selectedSuggestion) {
        String replacementString = selectedSuggestion.getReplacementString();
        System.out.println("UpdaterCallback " +//user.getId() +
            " got update " +
            " " +
            "--- > " + replacementString);

        // NOTE : we need both a redraw on key up and one on selection!
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            //System.out.println("--> onSelection REDRAW ");
            redraw();
          }
        });


        return replacementString;
      }
    };
  }

  private void redraw() {
    RangeChangeEvent.fire(cellTable, cellTable.getVisibleRange());              //2nd way
  }

  private String getText() {
    return textSuggest == null ? "" : getTextFromTypeahead(textSuggest);
  }

  private String getTextFromTypeahead(Typeahead textSuggest) {
    if (textSuggest == null) return "";

    else {
      TextBox widget = (TextBox) textSuggest.getWidget();
      //    System.out.println("checking " + widget.getElement().getId() + " " + widget.getText() +" " + widget.getValue());
      return widget.getValue();
    }
  }

  private long getUserID() {
    String textFromTypeahead = getTextFromTypeahead(userIDSuggest);
    return userIDSuggest == null ? -1 : textFromTypeahead.isEmpty() ? -1 : Long.parseLong(textFromTypeahead);
  }

  private Map<String, String> getUnitToValue() {
    Map<String, String> unitToValue = new HashMap<String, String>();
    for (String type : typeOrder) {
      Typeahead suggestBox = typeToSuggest.get(type);
      if (suggestBox != null) {
        String text = getTextFromTypeahead(suggestBox);
        if (!text.isEmpty()) {
          unitToValue.put(type, text);
        }
      }
    }
    return unitToValue;
  }

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
   * @see #populateTable
   */
  private Widget getAsyncTable(int numResults) {
    cellTable = new CellTable<MonitorResult>();
    cellTable.setSelectionModel(new NoSelectionModel<MonitorResult>());
    addColumnsToTable(cellTable);
    cellTable.setRowCount(numResults, true);
    cellTable.setVisibleRange(0, MAX_TO_SHOW);
    createProvider(numResults, cellTable);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the AsyncDataPRrovider.
    ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(cellTable);
    cellTable.addColumnSortHandler(columnSortHandler);

    Column<?, ?> time = getColumn(TIMESTAMP);
    cellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(time, false));
    cellTable.setWidth("100%", false);

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
   * Deals with out of order requests, or where the requests outpace the responses
   *
   * @param numResults
   * @param table
   * @return
   * @see #getAsyncTable(int)
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
        final Map<String, String> unitToValue = getUnitToValue();

        final long userID = getUserID();
        final String text = getText();

        int val = req++;
        System.out.println("getResults req " + unitToValue + " user " + userID + " text " + text + " val " + val);
        service.getResults(start, end, builder.toString(), unitToValue, userID, text, val, new AsyncCallback<ResultAndTotal>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Can't contact server.");
          }

          @Override
          public void onSuccess(final ResultAndTotal result) {
            if (result.req < req - 1) {
/*
              System.out.println("->>getResults ignoring response " + result.req + " vs " + req +
                  " --->req " + unitToValue + " user " + userID + " text '" + text + "' : got back " + result.results.size() + " of total " + result.numTotal);
*/
            } else {
              System.out.println("--->getResults req " + result.req +
                  " " + unitToValue + " user " + userID + " text '" + text + "' : got back " + result.results.size() + " of total " + result.numTotal);

              final int numTotal = result.numTotal;
              cellTable.setRowCount(numTotal, true);
              updateRowData(start, result.results);
            }
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
//          System.err.println("huh? answer is null??");
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
    cellTable.setColumnWidth(fl, "180px");

    for (final String type : typeOrder) {
      TextColumn<MonitorResult> unit = new TextColumn<MonitorResult>() {
        @Override
        public String getValue(MonitorResult answer) {
          Map<String, String> unitToValue = answer.getUnitToValue();
          return unitToValue == null ? "?" : unitToValue.get(type);
        }
      };
      unit.setSortable(true);
      table.addColumn(unit, type);
      colToField.put(unit, type);
    }

    return id;
  }

  /**
   * @param table to add columns to
   */
  protected void addResultColumn(CellTable<MonitorResult> table) {
    addNoWrapColumn(table);

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
    return ((float) ((Math.round(totalHours * 100d)))) / 100f;
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
