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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultManager {
  private static final int PAGE_SIZE = 8;
  private LangTestDatabaseAsync service;

  public ResultManager(LangTestDatabaseAsync s) {this.service = s;}

  private Widget lastTable = null;
  private Button closeButton;
/*
  public ResultManager(boolean gradingView) {

  }*/

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

        Widget table = getTable(result, false, true);
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

  public Widget getTable(List<Result> result, final boolean gradingView, boolean showQuestionColumn) {
    CellTable<Result> table = new CellTable<Result>();
    table.setWidth("1200px");
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
      public String getValue(Result answer) { return ""+answer.qid; }};
    experience.setSortable(true);
    table.addColumn(experience,"Q. #");
    }
/*    TextColumn<Result> answerText = new TextColumn<Result>() {
      @Override
      public String getValue(Result answer) {
        String answer1 = answer.answer;
        return gradingView ? (answer1.endsWith(".wav") ? "" : answer1) : answer1; }};

    answerText.setSortable(true);
    table.addColumn(answerText,"Answer");*/

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
      TextColumn<Result> valid = new TextColumn<Result>() {
        @Override
        public String getValue(Result answer) {
          return "" + answer.valid;
        }
      };
      valid.setSortable(true);

      table.addColumn(valid, "Is Valid Recording?");

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
  /*    final CheckboxCell correctCheckbox   = new CheckboxCell(true, true);
      //correctCheckbox.get
      final CheckboxCell incorrectCheckbox = new CheckboxCell(true, true);
      Column<Result,Boolean> correct = new Column<Result, Boolean>(correctCheckbox) {
       @Override
            public Boolean getValue(Result object) {
              return false;
            }
          };
      FieldUpdater<Result, Boolean> correctUpdater = new FieldUpdater<Result, Boolean>() {
        public void update(int index, Result object, Boolean value) {
          System.out.println("Got value " + index + " " + object + " " + value);
          //  incorrectCheckbox.setValue(getContext(),!value);
        }
      };
      correct.setFieldUpdater(correctUpdater);
      table.addColumn(correct,"Correct?");

      Column<Result,Boolean> incorrect = new Column<Result, Boolean>(incorrectCheckbox) {
        @Override
        public Boolean getValue(Result object) {
          return false;
        }
      };
      FieldUpdater<Result, Boolean> incorrectUpdater = new FieldUpdater<Result, Boolean>() {
        public void update(int index, Result object, Boolean value) {
          System.out.println("Got value " + index + " " + object + " " + value);
        }
      };
      incorrect.setFieldUpdater(incorrectUpdater);
      table.addColumn(correct,"Incorrect?");*/

      SelectionCell selectionCell = new SelectionCell(Arrays.asList("Ungraded", "Correct", "Incorrect"));
      Column<Result, String> col = new Column<Result, String>(selectionCell) {
        @Override
        public String getValue(Result object) {
          return "Ungraded";
        }
      };
      col.setFieldUpdater(new FieldUpdater<Result, String>() {
        public void update(int index, Result object, String value) {
          System.out.println("Got value " + index + " " + object + " " + value);
          service.addGrade(object.uniqueID,0,value.equals("Correct"),new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
              //To change body of implemented methods use File | Settings | File Templates.
            }

            public void onSuccess(Void result) {   // TODO show check box?
              //To change body of implemented methods use File | Settings | File Templates.
            }
          });
        }
      });
      table.addColumn(col, "Correct?");
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

    // We know that the data is sorted alphabetically by default.
   // table.getColumnSortList().push(id);

    // Create a SimplePager.
    SimplePager pager = new SimplePager();

    // Set the cellList as the display.
    pager.setDisplay(table);
    pager.setPageSize(PAGE_SIZE);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(pager);
    vPanel.add(table);
    return vPanel;
  }

  private SafeHtml getAudioTag(String result) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<audio preload=\"none\" controls=\"controls\" tabindex=\"0\">\n" +
      "<source type=\"audio/wav\" src=\"" +
      result +
      "\"></source>\n" +
      // "<source type=\"audio/ogg\" src=\"media/ac-LC1-009/ac-LC1-009-C.ogg\"></source>\n" +
      "Your browser does not support the audio tag.\n" +
      "</audio>");
    return sb.toSafeHtml();
  }
}
