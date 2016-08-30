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

package mitll.langtest.client.instrumentation;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.instrumentation.Event;

import java.util.Collection;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class EventTable extends PagerTable {
  private static final int PAGE_SIZE = 5;
  private Widget lastTable = null;
  private Button closeButton;

  /**
   * @see mitll.langtest.client.InitialUI.EventsClickHandler#onClick(ClickEvent)
   */
  public void show(final LangTestDatabaseAsync service) {
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

    service.getEvents(new AsyncCallback<Collection<Event>>() {
      public void onFailure(Throwable caught) {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("getEvents couldn't contact server");
        }
      }

      public void onSuccess(Collection<Event> result) {
        if (lastTable != null) {
          dialogVPanel.remove(lastTable);
          dialogVPanel.remove(closeButton);
        }

        Widget table = getTable(result, getDownloadAnchor());
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

  @Override
  protected SafeHtml getURL2() {
    return getAnchorHTML("downloadEvents", "Download Excel");
  }

  private Widget getTable(Collection<Event> result, Widget rightOfPager) {
    final CellTable<Event> table = new CellTable<Event>();
    table.setPageSize(PAGE_SIZE);
    int width = (int) (Window.getClientWidth() * 0.9);
    table.setWidth(width + "px");
    addColumns(table);

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

    return getOldSchoolPagerAndTable(table, table, 10, 10, rightOfPager);
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
        return contact.getWidgetType();
      }
    };
    lang.setSortable(true);
    table.addColumn(lang, "Type");

    TextColumn<Event> dialect = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" + contact.getExid();
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
        return "" + contact.getUserID();
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "User ID");

/*    TextColumn<Event> hit = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" +contact.getHitID();
      }
    };
    hit.setSortable(true);
    table.addColumn(hit, "Hit ID");*/

    TextColumn<Event> device = new TextColumn<Event>() {
      @Override
      public String getValue(Event contact) {
        return "" + contact.getDevice();
      }
    };
    device.setSortable(true);
    table.addColumn(device, "Device");

    getDateColumn(table);
    return id;
  }

  private Column<Event, SafeHtml> getDateColumn(CellTable<Event> table) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<Event, SafeHtml> dateCol = new Column<Event, SafeHtml>(cell) {
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