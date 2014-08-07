package mitll.langtest.client.instrumentation;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.instrumentation.Event;

import java.util.List;

public class EventTable extends PagerTable {
  private static final int PAGE_SIZE = 5;
  private Widget lastTable = null;
  private Button closeButton;

  /**
   */
  public void show(final LangTestDatabaseAsync service) {
    showDialog(service);
  }

  void showDialog(final LangTestDatabaseAsync service) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Events");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 20;
    int top = (Window.getClientHeight()) / 20;
    dialogBox.setPopupPosition(left, top);

    service.getEvents(new AsyncCallback<List<Event>>() {
      public void onFailure(Throwable caught) {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("getEvents couldn't contact server");
        }
      }

      public void onSuccess(List<Event> result) {
        if (lastTable != null) {
          dialogVPanel.remove(lastTable);
          dialogVPanel.remove(closeButton);
        }

        Widget table = getTable(result);
        dialogVPanel.add(new Anchor(getURL2()));
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

  private SafeHtml getURL2() {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href='" +
      "downloadEvents" +
      "'" +
      ">");
    sb.appendEscaped("Download Excel");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }

  private Widget getTable(List<Event> result) {
    final CellTable<Event> table = new CellTable<Event>();
    table.setPageSize(PAGE_SIZE);
    int width = (int) (Window.getClientWidth() * 0.9);
    table.setWidth(width + "px");
    /*TextColumn<Event> id =*/ addColumns(table);

    // Create a data provider.
    ListDataProvider<Event> dataProvider = new ListDataProvider<Event>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<Event> list = dataProvider.getList();
    for (Event contact : result) {
      list.add(contact);
    }
    table.setRowCount(list.size());

    // We know that the data is sorted alphabetically by default.
//    table.getColumnSortList().push(id);

    // Create a SimplePager.
    // return getPagerAndTable(table, table, 10, 10);
    return getOldSchoolPagerAndTable(table, table, 10, 10);
  }

  private TextColumn<Event> addColumns(CellTable<Event> table) {
    TextColumn<Event> id = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return contact.getWidgetID();
      }
    };
    id.setSortable(true);
    table.addColumn(id, "ID");

    TextColumn<Event> lang = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return  contact.getWidgetType();
      }
    };
    lang.setSortable(true);
    table.addColumn(lang, "Type");

    TextColumn<Event> dialect = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" + contact.getExerciseID();
      }
    };
    dialect.setSortable(true);
    table.addColumn(dialect, "Exercise");

    TextColumn<Event> age = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" + contact.getContext();
      }
    };
    age.setSortable(true);
    table.addColumn(age, "Context");

    TextColumn<Event> gender = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" +contact.getCreatorID();
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "User ID");

    TextColumn<Event> hit = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" +contact.getHitID();
      }
    };
    hit.setSortable(true);
    table.addColumn(hit, "Hit ID");

    getDateColumn(table);
    return id;
  }

  private Column<Event, SafeHtml> getDateColumn(CellTable<Event> table) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<Event,SafeHtml> dateCol = new Column<Event, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(Event answer) {
        return getSafeHTMLForTimestamp(answer.getTimestamp());
      }
    };
    table.addColumn(dateCol, "Time");
    dateCol.setSortable(true);
    return dateCol;
  }
}