/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ReviewScoringPanel;
import mitll.langtest.client.services.ResultService;
import mitll.langtest.client.services.ResultServiceAsync;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.ResultAndTotal;
import mitll.langtest.shared.result.MonitorResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Show a dialog with all the results we've collected so far.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager extends PagerTable {
  private final Logger logger = Logger.getLogger("ResultManager");

  private static final String YES = "Yes";
  private static final String NO = "No";

  private static final int PAGE_SIZE = 7;
  private static final String TIMESTAMP = "timestamp";
  private static final String CORRECT = "Correct";
  private static final String PRO_F_SCORE = "Score";
  private static final String DURATION_SEC = "Dur (s)";
  private static final String AUDIO_TYPE = "Audio Type";
  private static final String USER_ID = "User";
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
  private static final String DEVICE = MonitorResult.DEVICE;
  private static final String CLOSE = "Close";
  private static final int MAX_TO_SHOW = PAGE_SIZE;

  private final EventRegistration eventRegistration;
  private final ResultServiceAsync resultServiceAsync = GWT.create(ResultService.class);

  private final AudioTag audioTag = new AudioTag();
  private final String nameForAnswer;
  private final Map<Column<?, ?>, String> colToField = new HashMap<Column<?, ?>, String>();
  private final Collection<String> typeOrder;
  private int req = 0;
  private final ExerciseController controller;

  private CellTable<MonitorResult> cellTable;
  private Panel reviewContainer;

//  private Map<String, Typeahead> typeToSuggest = new HashMap<String, Typeahead>();
//  private Typeahead userIDSuggest, textSuggest;
  // ResultTypeAhead resultTypeAhead;

  /**
   * @param nameForAnswer
   * @param eventRegistration
   * @paramx s
   * @seex mitll.langtest.client.initial.InitialUI.ResultsClickHandler#onClick
   */
  public ResultManager(String nameForAnswer,
                       Collection<String> typeOrder,
                       EventRegistration eventRegistration,
                       ExerciseController controller) {
    this.nameForAnswer = nameForAnswer;
    this.typeOrder = typeOrder;
    this.eventRegistration = eventRegistration;
    this.controller = controller;
  }

  /**
   * @see UserMenu.ResultsClickHandler
   */
  public void showResults() {
    req = 0;

    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(nameForAnswer + "s");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = (Window.getClientWidth()) / 200;
    //int top = (Window.getClientHeight()) / 200;
    dialogBox.setPopupPosition(left, 55);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");

    resultServiceAsync.getNumResults(new AsyncCallback<Integer>() {
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

  /**
   * @param dialogBox
   * @return
   * @see #showResults()
   */
  private Button getCloseButton(final DialogBox dialogBox) {
    final Button closeButton = new Button(CLOSE);
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButtonLessTopMargin");
    eventRegistration.register(closeButton, "N/A", "Close recordings dialog");
    // Add a handler to send the name to the server
    closeButton.addClickHandler(event -> dialogBox.hide());
    return closeButton;
  }

  @Override
  protected SafeHtml getURL2() {
    return getAnchorHTML("downloadResults", "Download Excel");
  }

  /**
   * @param numResults
   * @param dialogVPanel
   * @param dialogBox
   * @see #showResults
   */
  private void populateTable(int numResults, Panel dialogVPanel, DialogBox dialogBox,
                             Button closeButton) {
    dialogVPanel.clear();

    cellTable = new CellTable<>();
    ResultTypeAhead resultTypeAhead = new ResultTypeAhead(typeOrder, cellTable, resultServiceAsync);
    Widget table = getAsyncTable(numResults, getDownloadAnchor(), resultTypeAhead);
    table.setWidth("100%");

    dialogVPanel.add(resultTypeAhead.getSearchBoxes());
    dialogVPanel.add(table);
    dialogVPanel.add(reviewContainer); // made in asynctable
    dialogVPanel.add(closeButton);

    dialogBox.show();
  }

  /**
   * Also shows on the bottom a widget for review.
   *
   * @param numResults
   * @return
   * @see #populateTable
   */
  private Widget getAsyncTable(int numResults, Widget rightOfPager, ResultTypeAhead resultTypeAhead) {
    reviewContainer = new HorizontalPanel();
    reviewContainer.addStyleName("topFiveMargin");
    reviewContainer.addStyleName("border");

    final SingleSelectionModel<MonitorResult> selectionModel = new SingleSelectionModel<>();
    cellTable.setSelectionModel(selectionModel);
    cellTable.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        respondToClick(selectionModel.getSelectedObject());
      }
    });

    addColumnsToTable(cellTable);
    cellTable.setRowCount(numResults, true);
    cellTable.setVisibleRange(0, MAX_TO_SHOW);

    createProvider(numResults, cellTable, resultTypeAhead);

    // Add a ColumnSortEvent.AsyncHandler to connect sorting to the AsyncDataPRrovider.
    cellTable.addColumnSortHandler(new ColumnSortEvent.AsyncHandler(cellTable));

    Column<?, ?> time = getColumn(TIMESTAMP);
    cellTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(time, false));
    cellTable.setWidth("100%", false);

    return getPagerAndTable(cellTable, rightOfPager);
  }

  /**
   * Don't do alignment with invalid audio.
   *
   * @param selectedObject
   */
  private void respondToClick(MonitorResult selectedObject) {
    reviewContainer.clear();
    int exID = selectedObject.getExID();

    if (selectedObject.getDurationInMillis() > 100 && selectedObject.isValid()) {
      // logger.info("audio type " + audioType);
      String foreignText = selectedObject.getForeignText();

      ReviewScoringPanel w = new ReviewScoringPanel(selectedObject.getAnswer(), foreignText, "", controller,
          exID, // no exercise!
          "instance");

      w.setResultID(selectedObject.getUniqueID());

      Panel vert = new VerticalPanel();
      vert.add(w);
      vert.add(w.getBelow());

      reviewContainer.add(vert);
      reviewContainer.add(w.getTables());
    } else {
      AudioPanel w = new AudioPanel<>(selectedObject.getAnswer(), controller, false, 10, "",
          null, exID, "instance");
      reviewContainer.add(w);
    }
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
   * Deals with out of order requests, or where the requests outpace the responses
   *
   * @param numResults
   * @param table
   * @return
   * @see #getAsyncTable
   */
  private void createProvider(final int numResults,
                              final CellTable<MonitorResult> table,
                              ResultTypeAhead resultTypeAhead) {
    AsyncDataProvider<MonitorResult> dataProvider = new AsyncDataProvider<MonitorResult>() {
      @Override
      protected void onRangeChanged(HasData<MonitorResult> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //logger.info("createProvider asking for " + start +"->" + end);

        StringBuilder builder = getColumnSortedState(table);
        final Map<String, String> unitToValue = resultTypeAhead.getUnitToValue();

        int val = req++;
        // logger.info("getResults req " + unitToValue + " user " + userID + " text " + text + " val " + val);
        //   logger.info("got " + builder.toString());

        resultServiceAsync.getResults(
            start,
            end,
            builder.toString(),
            unitToValue,
            resultTypeAhead.getText(),
            val,
            new AsyncCallback<ResultAndTotal>() {
              @Override
              public void onFailure(Throwable caught) {
                Window.alert("Can't contact server.");
                logger.warning("Got  " + caught);
              }

              @Override
              public void onSuccess(final ResultAndTotal result) {
                if (result.req < req - 1) {
/*
              logger.info("->>getResults ignoring response " + result.req + " vs " + req +
                  " --->req " + unitToValue + " user " + userID + " text '" + text + "' : got back " + result.results.size() + " of total " + result.numTotal);
*/
                } else {
                  final int numTotal = result.numTotal;
                  cellTable.setRowCount(numTotal, true);
                  updateRowData(start, result.results);
                  if (numTotal > 0) {
                    MonitorResult object = result.results.get(0);
/*
                    logger.info("--->getResults req " + result.req +
                            " " + unitToValue + " user " + userID + " text '" + text + "' : " +
                            "got back " + result.results.size() + " of total " + result.numTotal + " selecting "+ object);
*/
                    cellTable.getSelectionModel().setSelected(object, true);
                  }
                }
              }
            });
      }
    };

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);
  }

  /**
   * @param table
   * @return
   * @see #createProvider
   */
  private StringBuilder getColumnSortedState(CellTable<MonitorResult> table) {
    final ColumnSortList sortList = table.getColumnSortList();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < sortList.size(); i++) {
      ColumnSortList.ColumnSortInfo columnSortInfo = sortList.get(i);
      Column<?, ?> column = columnSortInfo.getColumn();
      String s = colToField.get(column);
      if (s == null) {
        logger.warning("Can't find column " + column + "?");
      }
      builder.append(s + "_" + (columnSortInfo.isAscending() ? ASC : DESC) + ",");
    }
    if (!builder.toString().contains(TIMESTAMP)) {
      builder.append(TIMESTAMP + "_" + DESC);
    }
    return builder;
  }


  /**
   * @param table
   * @return
   * @seex #getResultCellTable
   * @see #getAsyncTable
   */
  private void addColumnsToTable(CellTable<MonitorResult> table) {
    addUserPlanExercise(table);

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
          //  return PlayAudioWidget.getAudioTagHTML(answer1,"answer_to_"+answer.getID() + "_by_"+answer.getUserid()+"_"+answer.getRealID());
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
  }

  /**
   * @param table
   * @return
   * @see #addColumnsToTable(com.google.gwt.user.cellview.client.CellTable)
   */
  private void addUserPlanExercise(CellTable<MonitorResult> table) {
    addUserID(table);
    addExercise(table);
    addText(table);
    addTypes(table);
  }

  private void addTypes(CellTable<MonitorResult> table) {
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
  }

  private void addText(CellTable<MonitorResult> table) {
    Column<MonitorResult, SafeHtml> fl = new Column<MonitorResult, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(MonitorResult answer) {
        String foreignText = answer != null ? answer.getForeignText() : "";
        return getNoWrapContent(foreignText);
      }
    };

    fl.setSortable(true);
    table.addColumn(fl, "Text");
    colToField.put(fl, TEXT);
    cellTable.setColumnWidth(fl, "180px");
  }

  private void addExercise(CellTable<MonitorResult> table) {
    TextColumn<MonitorResult> exercise = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer == null ? "Unk" : "" + answer.getExID();
      }
    };
    exercise.setSortable(true);
    table.addColumn(exercise, "Ex.");
    colToField.put(exercise, ID);
  }

  private void addUserID(CellTable<MonitorResult> table) {
    TextColumn<MonitorResult> userid = new TextColumn<MonitorResult>() {
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
    userid.setSortable(true);
    table.addColumn(userid, USER_ID);
    colToField.put(userid, USERID);
  }

  /**
   * @param table to add columns to
   */
  private void addResultColumn(CellTable<MonitorResult> table) {
    addNoWrapColumn(table);

    TextColumn<MonitorResult> audioType = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return /*answer.getAudioType().equals("avp") ? "flashcard" :*/ answer.getAudioType().toString();
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
        return answer.isValid() ? YES : NO;
      }
    };
    valid.setSortable(true);
    table.addColumn(valid, "Valid");
    colToField.put(valid, VALID);

    TextColumn<MonitorResult> validity = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.getValidity();
      }
    };
    validity.setSortable(true);
    table.addColumn(validity, "Validity");
    colToField.put(validity, "Validity");

    TextColumn<MonitorResult> dynamicRange = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return "" + roundToHundredth(answer.getSnr() < 0 ? 0 : answer.getSnr());
      }
    };
    dynamicRange.setSortable(true);
    table.addColumn(dynamicRange, "Dynamic Range");
    colToField.put(dynamicRange, "Dynamic Range");

    TextColumn<MonitorResult> correct = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.isCorrect() ? YES : NO;
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

    Column<MonitorResult, SafeHtml> type = new Column<MonitorResult, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(MonitorResult answer) {
        return getNoWrapContent(answer.getDevice());
      }
    };

    type.setSortable(true);
    table.addColumn(type, DEVICE);
    colToField.put(type, DEVICE);

    TextColumn<MonitorResult> wFlash = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        return answer.isWithFlash() ? YES : NO;
      }
    };
    wFlash.setSortable(true);
    table.addColumn(wFlash, "w/Flash");
    colToField.put(wFlash, "withFlash");

    TextColumn<MonitorResult> processDur = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        float secs = ((float) answer.getProcessDur()) / 1000f;
        return "" + roundToHundredth(secs);
      }
    };
    processDur.setSortable(true);
    table.addColumn(processDur, "Process");
    colToField.put(processDur, "Process");


    TextColumn<MonitorResult> rtDur = new TextColumn<MonitorResult>() {
      @Override
      public String getValue(MonitorResult answer) {
        float secs = ((float) answer.getRoundTripDur()) / 1000f;
        return "" + roundToHundredth(secs);
      }
    };
    rtDur.setSortable(true);
    table.addColumn(rtDur, "RT");
    colToField.put(rtDur, "RT");
  }

  private void addNoWrapColumn(CellTable<MonitorResult> table) {
    colToField.put(getDateColumn(table), TIMESTAMP);
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

  private Panel getPagerAndTable(CellTable<MonitorResult> table, Widget rightOfPager) {
    return getOldSchoolPagerAndTable(table, table, PAGE_SIZE, 1000, rightOfPager);
  }
}
