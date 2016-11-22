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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.User;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class UserTable extends PagerTable {
  private Logger logger = Logger.getLogger("UserTable");

  private static final String AGE = "Age";
  private static final int IP_ADDR_MAX_LENGTH = 20;

  private static final String REGISTERED_USERS = "Registered Users";

  private static final int PAGE_SIZE = 5;

  private static final String USER_ID = "User ID";
  private static final int PAGE_SIZE1 = 8;
  private static final int INSET_PERCENT = 50;
  private static final String IP_PREFIX = "127.0.0.1/Mozilla/5.0 ";
  private static final String PERMISSIONS = "Perm.";//issions";
  private static final String QUALITY_CONTROL = "QUALITY_CONTROL";
  private static final String RECORD_AUDIO = "RECORD_AUDIO";
  private static final String C_DEVELOPER = "CONTENT";

  private Widget lastTable = null;
  private Button closeButton;
  private final PropertyHandler props;
  private final boolean isAdmin;

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

        dialogVPanel.add(new HTML("Click on a column to sort."));
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

  private Widget getTable(List<User> users, final LangTestDatabaseAsync service, Widget rightOfPager) {
    final CellTable<User> table = getTable();

    List<User> list = getDataProvider(users, table);

    TextColumn<User> id = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getId();
      }
    };
    id.setSortable(true);
    table.addColumn(id, "ID");

    addUserIDColumns(table, list);

    TextColumn<User> dialect = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getDialect();
      }
    };
    dialect.setSortable(true);
    table.addColumn(dialect, "Dialect");
    table.addColumnSortHandler(getDialectSorter(dialect, list));

    TextColumn<User> age = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getAge();
      }
    };
    age.setSortable(true);
    table.addColumn(age, AGE);
    table.addColumnSortHandler(getAgeSorter(age, list));

    TextColumn<User> gender = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.getGender() == 0 ? "male" : "female";
      }
    };
    gender.setSortable(true);
    table.addColumn(gender, "Gender");
    table.addColumnSortHandler(getGenderSorter(gender, list));

    TextColumn<User> perm = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getPermissions().toString().replaceAll(QUALITY_CONTROL, "QC").replaceAll(RECORD_AUDIO, "RECORD");
      }
    };
    perm.setSortable(true);
    table.addColumn(perm, PERMISSIONS);
    table.addColumnSortHandler(getPermSorter(perm, list));

    TextColumn<User> complete = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return contact.isComplete() ? "Yes" : ("No (" + Math.round(100 * contact.getCompletePercent()) + "%)");
      }
    };
    complete.setSortable(true);
    table.addColumn(complete, "Items Complete?");
    table.addColumnSortHandler(getCompleteSorter(complete, list));

    TextColumn<User> items = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getNumResults();
      }
    };
    items.setSortable(true);
    table.addColumn(items, "Num " + props.getNameForAnswer() + "s");
    table.addColumnSortHandler(getNumRecordingsSorter(items,list));

    TextColumn<User> rate = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + roundToHundredth(contact.getRate());
      }
    };
    rate.setSortable(true);
    table.addColumn(rate, "Rate (sec)");
    table.addColumnSortHandler(getRateSorter(rate,list));

    TextColumn<User> ipaddr = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return getIPAddr(contact);
      }
    };

    ipaddr.setSortable(true);
    table.addColumn(ipaddr, "IP Addr");
    table.addColumnSortHandler(getIPSorter(ipaddr,list));

    getDateColumn(table,list);

    TextColumn<User> kind = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return (contact.getUserKind() == User.Kind.CONTENT_DEVELOPER ? C_DEVELOPER : contact.getUserKind().toString());
      }
    };
    table.addColumn(kind, "Type");
    kind.setSortable(true);
    table.addColumnSortHandler(getKindSorter(kind,list));


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
    device.setSortable(true);
    table.addColumnSortHandler(getDeviceSorter(device,list));

    if (isAdmin) {
      addAdminCol(service, table);
    } else {
      TextColumn<User> enabled = new TextColumn<User>() {
        @Override
        public String getValue(User contact) {
          return contact.isEnabled() ? "Yes" : "No";
        }
      };
      table.addColumn(enabled, "Enabled");
    }

    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(list);
    columnSortHandler.setComparator(id,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              return (o2 != null) ? (int) (o1.getId() - o2.getId()) : 0;
            }
            return -1;
          }
        });
    table.addColumnSortHandler(columnSortHandler);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(id);

  //  table.setWidth("100%", true);

    // Create a SimplePager.
    // return getPagerAndTable(table, table, 10, 10);
    return getOldSchoolPagerAndTable(table, table, PAGE_SIZE1, PAGE_SIZE1, rightOfPager);
  }

  private String getIPAddr(User contact) {
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
      return ipaddr1.length() > IP_ADDR_MAX_LENGTH ? (ipaddr1.substring(0, IP_ADDR_MAX_LENGTH) +"...") : ipaddr1;
    }
  }

  private CellTable<User> getTable() {
    final CellTable<User> table = new CellTable<User>();
    table.setPageSize(PAGE_SIZE);
    int width = (int) (Window.getClientWidth() * 0.9);
    table.setWidth(width + "px");
    return table;
  }

  private List<User> getDataProvider(List<User> users, CellTable<User> table) {
    // Create a data provider.
    ListDataProvider<User> dataProvider = new ListDataProvider<User>();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);

    // Add the data to the data provider, which automatically pushes it to the
    // widget.
    List<User> list = dataProvider.getList();
    for (User contact : users) {
      list.add(contact);
    }
    table.setRowCount(list.size());
    return list;
  }

  private void addAdminCol(final LangTestDatabaseAsync service, CellTable<User> table) {
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
  }

  private void getDateColumn(CellTable<User> table, List<User> list) {
    SafeHtmlCell cell = new SafeHtmlCell();
    Column<User, SafeHtml> dateCol = new Column<User, SafeHtml>(cell) {
      @Override
      public SafeHtml getValue(User answer) {
        return getSafeHTMLForTimestamp(answer.getTimestampMillis());
      }
    };
    table.addColumn(dateCol, "Time");
    dateCol.setSortable(true);
    table.addColumnSortHandler(getTimeSorter(dateCol,list));
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

  private void addUserIDColumns(CellTable<User> table, List<User> list) {
    TextColumn<User> userID = new TextColumn<User>() {
      @Override
      public String getValue(User contact) {
        return "" + contact.getUserID();
      }
    };
    userID.setSortable(true);
    table.addColumn(userID, USER_ID);
    table.addColumnSortHandler(getUserIDSorter(userID, list));
  }

  private ColumnSortEvent.ListHandler<User> getAgeSorter(TextColumn<User> englishCol,
                                                         List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = Integer.valueOf(o1.getAge()).compareTo(o2.getAge());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getGenderSorter(TextColumn<User> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = Integer.valueOf(o1.getGender()).compareTo(o2.getGender());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<User> getUserIDSorter(TextColumn<User> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getUserID().compareTo(o2.getUserID());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getDialectSorter(TextColumn<User> englishCol,
                                                             List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = o1.getDialect().compareTo(o2.getDialect());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getDeviceSorter(TextColumn<User> englishCol,
                                                             List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = o1.getDevice().compareTo(o2.getDevice());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getKindSorter(TextColumn<User> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = o1.getUserKind().compareTo(o2.getUserKind());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getIPSorter(TextColumn<User> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = o1.getIpaddr().compareTo(o2.getIpaddr());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getPermSorter(TextColumn<User> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = o1.getPermissions().toString().compareTo(o2.getPermissions().toString());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getTimeSorter(Column<User, SafeHtml> englishCol,
                                                            List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getNumRecordingsSorter(TextColumn<User> englishCol,
                                                          List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = Integer.valueOf(o1.getNumResults()).compareTo(o2.getNumResults());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getRateSorter(TextColumn<User> englishCol,
                                                                   List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Float.valueOf(o1.getRate()).compareTo(o2.getRate());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<User> getCompleteSorter(TextColumn<User> englishCol,
                                                          List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                int i = Float.valueOf(o1.getCompletePercent()).compareTo(o2.getCompletePercent());
                if (i == 0) i = Long.valueOf(o1.getId()).compareTo(o2.getId());
                return i;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }
}