package mitll.langtest.client.user;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.User;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class UserTable extends PagerTable {
  public static final String REGISTERED_USERS = "Registered Users";
  private Logger logger = Logger.getLogger("UserTable");

  private static final int PAGE_SIZE = 5;

  private static final String USER_ID = "User ID";
  private static final int PAGE_SIZE1 = 5;
  private static final int INSET_PERCENT = 50;
  private static final String IP_PREFIX = "127.0.0.1/Mozilla/5.0 ";
  private static final String PERMISSIONS = "Perm.";//issions";
  private static final String QUALITY_CONTROL = "QUALITY_CONTROL";
  private static final String RECORD_AUDIO = "RECORD_AUDIO";
  private static final String C_DEVELOPER = "CONTENT";//_DEVELOPER";

  private Widget lastTable = null;
  private Button closeButton;
  private final PropertyHandler props;
  private boolean isAdmin;
  // private final LangTestDatabaseAsync service;

  /**
   * @param props
   * @see mitll.langtest.client.LangTest.UsersClickHandler#onClick(ClickEvent)
   */
  public UserTable(PropertyHandler props, boolean isAdmin) {
    this.props = props;
    this.isAdmin = isAdmin;
  }

  /**
   * @see mitll.langtest.client.LangTest.UsersClickHandler
   */
  public void showUsers(final LangTestDatabaseAsync service) {
    showDialog(service);
  }

  private void showDialog(final LangTestDatabaseAsync service) {
    // Create the resetEmailPopup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText(REGISTERED_USERS);

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();

    int left = (Window.getClientWidth()) / INSET_PERCENT;
    int top = (Window.getClientHeight()) / INSET_PERCENT;
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

        Widget table = getTable(result, service, getDownloadAnchor());

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
    return getAnchorHTML("downloadUsers", "Download Excel");
  }

  private Widget getTable(List<User> result, final LangTestDatabaseAsync service, Widget rightOfPager) {
    final CellTable<User> table = new CellTable<User>();
    table.setPageSize(PAGE_SIZE);
    int width = (int) (Window.getClientWidth() * 0.9);
    table.setWidth(width + "px");
    TextColumn<User> id = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getId();
      }
    };
    id.setSortable(true);
    table.addColumn(id, "ID");

    addUserIDColumns(table);

    TextColumn<User> dialect = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getDialect();
      }
    };
    dialect.setSortable(true);
    table.addColumn(dialect, "Dialect");

    TextColumn<User> age = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getAge();
      }
    };
    age.setSortable(true);
    table.addColumn(age, "Age");

    TextColumn<User> gender = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.getGender() == 0 ? "male" : "female";
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "Gender");

    TextColumn<User> perm = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getPermissions().toString().replaceAll(QUALITY_CONTROL, "QC").replaceAll(RECORD_AUDIO, "RECORD");
      }
    };
    perm.setSortable(true);
    table.addColumn(perm, PERMISSIONS);

    TextColumn<User> complete = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.isComplete() ? "Yes" : ("No (" + Math.round(100 * contact.getCompletePercent()) +
            "%)");
      }
    };
    complete.setSortable(true);
    table.addColumn(complete, "Items Complete?");

    TextColumn<User> items = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getNumResults();
      }
    };
    items.setSortable(true);
    table.addColumn(items, "Num " + props.getNameForAnswer() + "s");

    TextColumn<User> rate = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + roundToHundredth(contact.getRate());
      }
    };
    rate.setSortable(true);
    table.addColumn(rate, "Rate (sec)");

    TextColumn<User> ipaddr = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        String ipaddr1 = contact.getIpaddr();
        if (ipaddr1 == null) {
          return "Unknown";
        } else {
          //  System.out.println("got " + ipaddr1);
          int at = ipaddr1.lastIndexOf("at");

          ipaddr1 = at == -1 ? ipaddr1 : ipaddr1.substring(0, at);
          if (ipaddr1.startsWith(IP_PREFIX)) {
            ipaddr1 = ipaddr1.substring(IP_PREFIX.length());
          }
          return ipaddr1;
        }
      }
    };

    ipaddr.setSortable(true);
    table.addColumn(ipaddr, "IP Addr");

    getDateColumn(table);

    TextColumn<User> kind = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return (contact.getUserKind() == User.Kind.CONTENT_DEVELOPER ? C_DEVELOPER : contact.getUserKind().toString());
      }
    };
    table.addColumn(kind, "Type");

    TextColumn<User> emailH = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.getEmailHash() == null ? "NO" : "YES";
      }
    };
    table.addColumn(emailH, "Email?");

    TextColumn<User> passH = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.getPasswordHash() == null ? "NO" : "YES";
      }
    };
    table.addColumn(passH, "Pass?");

    TextColumn<User> device = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.getDevice();
      }
    };
    table.addColumn(device, "Device");


    if (isAdmin) {
      CheckboxCell checkboxCell = new CheckboxCell(true, false);

      Column<User, Boolean> checkColumn = new Column<User, Boolean>(checkboxCell) {
        @Override
        public Boolean getValue(User object) {
          return object.isEnabled() || (!object.isCD());
        }
      };

      checkColumn.setFieldUpdater(new FieldUpdater<User, Boolean>() {
        @Override
        public void update(int index, User object, Boolean value) {
//          logger.info("update " + object.getUserID() + " " + value);
          service.changeEnabledFor((int) object.getId(), value, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(Void result) {

            }
          });
        }
      });


      table.addColumn(checkColumn, "Enabled?");
      table.setColumnWidth(checkColumn, 40, Style.Unit.PX);
    } else {
      TextColumn<User> enabled = new TextColumn<User>() {
        @Override
        public String getValue(User contact) {
          return contact.isEnabled() ? "Yes" : "No";
        }
      };
      table.addColumn(enabled, "Enabled");
    }

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
              return (o2 != null) ? (int) (o1.getId() - o2.getId()) : 0;
            }
            return -1;
          }
        });
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

    // Create a SimplePager.
    // return getPagerAndTable(table, table, 10, 10);
    return getOldSchoolPagerAndTable(table, table, PAGE_SIZE1, PAGE_SIZE1, rightOfPager);
  }

  private void getDateColumn(CellTable<User> table) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<User, SafeHtml> dateCol = new Column<User, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(User answer) {
        return getSafeHTMLForTimestamp(answer.getTimestampMillis());
      }
    };
    table.addColumn(dateCol, "Time");
    dateCol.setSortable(true);
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

  private void addUserIDColumns(CellTable<User> table) {
    TextColumn<User> userID = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getUserID();
      }
    };
    userID.setSortable(true);
    table.addColumn(userID, USER_ID);
  }
}