package mitll.langtest.client.user;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
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
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.User;

import java.util.Comparator;
import java.util.List;

public class UserTable extends PagerTable {
  private static final int PAGE_SIZE = 5;
  private Widget lastTable = null;
  private Button closeButton;

  /**
   * @see mitll.langtest.client.LangTest#getLogout()
   */
  public void showUsers(final LangTestDatabaseAsync service, int userid) {
    showDialog(service);
  }

  protected void showDialog(final LangTestDatabaseAsync service) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Registered Users");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / 20;
    int top = (Window.getClientHeight()) / 20;
    dialogBox.setPopupPosition(left, top);

    service.getUsers(new AsyncCallback<List<User>>() {
      public void onFailure(Throwable caught) {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("getUsers couldn't contact server");
        }
      }

      public void onSuccess(List<User> result) {
        if (lastTable != null) {
          dialogVPanel.remove(lastTable);
          dialogVPanel.remove(closeButton);
        }

        Widget table = getTable(result, service);
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
      "downloadUsers" +
      //   name +
      "'" +
      ">");
    sb.appendEscaped("Download Excel");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }

  private Widget getTable(List<User> result, final LangTestDatabaseAsync service) {
    final CellTable<User> table = new CellTable<User>();
    table.setPageSize(PAGE_SIZE);
    int width = (int) (Window.getClientWidth() * 0.9);
    table.setWidth(width + "px");
    TextColumn<User> id = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.id;
      }
    };
    id.setSortable(true);
    table.addColumn(id, "ID");

    addUserIDColumns(service, table);

    TextColumn<User> lang = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.nativeLang;
      }
    };
    lang.setSortable(true);
    table.addColumn(lang, "Lang");

    TextColumn<User> dialect = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.dialect;
      }
    };
    dialect.setSortable(true);
    table.addColumn(dialect, "Dialect");

    TextColumn<User> age = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.age;
      }
    };
    age.setSortable(true);
    table.addColumn(age, "Age");

    TextColumn<User> gender = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.gender == 0 ? "male" : "female";
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "Gender");

    TextColumn<User> experience = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        int experience1 = contact.experience;
        String exp = "" + experience1 + " months";
        if (contact.getDemographics() != null) {
          exp = contact.getDemographics().toString();
        }
        String prefix = "user id " + contact.id + " has ";
        if (exp.startsWith(prefix)) {
          exp = exp.substring(prefix.length());
        }
        return exp;
      }
    };
    experience.setSortable(true);
    table.addColumn(experience, "Experience");

    TextColumn<User> items = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getNumResults();
      }
    };
    items.setSortable(true);
    table.addColumn(items, "Num Results");

    TextColumn<User> rate = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + roundToHundredth(contact.getRate());
      }
    };
    rate.setSortable(true);
    table.addColumn(rate, "Rate(sec)");

    TextColumn<User> ipaddr = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        String ipaddr1 = contact.ipaddr;
        int at = ipaddr1.lastIndexOf("at");

        ipaddr1 = at == -1 ? "" : ipaddr1.substring(0, at);
        return ipaddr1;
      }
    };

    ipaddr.setSortable(true);
    table.addColumn(ipaddr, "IP Addr");

/*    TextColumn<User> password = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.password;
      }
    };
    password.setSortable(true);
    table.addColumn(password, "Password");*/

    TextColumn<User> date = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getTimestamp();
      }
    };
    date.setSortable(true);
    table.addColumn(date, "Time");

    // Create a data provider.
    ListDataProvider<User> dataProvider = new ListDataProvider<User>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<User> list = dataProvider.getList();
    for (User contact : result) {
      list.add(contact);
    }
    table.setRowCount(list.size());

    // Add a ColumnSortEvent.ListHandler to connect sorting to the
    // java.util.List.
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(
      list);
    columnSortHandler.setComparator(id,
      new Comparator<User>() {
        public int compare(User o1, User o2) {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            return (o2 != null) ? (int) (o1.id - o2.id) : 0;
          }
          return -1;
        }
      });
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

    // Create a SimplePager.
    // return getPagerAndTable(table, table, 10, 10);
    return getOldSchoolPagerAndTable(table, table, 10, 10);
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

  protected void addUserIDColumns(final LangTestDatabaseAsync service, CellTable<User> table) {
    TextColumn<User> userID = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.userID;
      }
    };
    userID.setSortable(true);
    table.addColumn(userID, "User ID");
  }
}