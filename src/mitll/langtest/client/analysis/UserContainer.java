package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.analysis.UserInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class UserContainer extends SimplePagingContainer<UserInfo> {
  //private final Logger logger = Logger.getLogger("UserContainer");

  private static final int ID_WIDTH = 130;
  private static final int TABLE_WIDTH = 460;

  private final ShowTab learnTab;
  private final DivWidget rightSide;
  private final LangTestDatabaseAsync service;

  /**
   * @param controller
   * @param rightSide
   * @see StudentAnalysis#StudentAnalysis(LangTestDatabaseAsync, ExerciseController, ShowTab)
   */
  public UserContainer(LangTestDatabaseAsync service, ExerciseController controller, DivWidget rightSide, ShowTab learnTab
  ) {
    super(controller);
    this.rightSide = rightSide;
    this.learnTab = learnTab;
    this.service = service;
  }

  protected int getPageSize() {
    return 25;
  }


  /**
   * @param users
   * @return
   * @see StudentAnalysis#StudentAnalysis
   */
  public Panel getTableWithPager(final Collection<UserInfo> users) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.addStyleName("floatLeft");

    for (UserInfo user : users) {
      addItem(user);
    }
    flush();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (!users.isEmpty()) {
          UserInfo next = users.iterator().next();
          table.getSelectionModel().setSelected(next, true);
          gotClickOnItem(next);
        }
      }
    });

    return tableWithPager;
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", TABLE_WIDTH + "px");
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<UserInfo>();
    table.setSelectionModel(selectionModel);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  private ColumnSortEvent.ListHandler<UserInfo> getUserSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
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

  private ColumnSortEvent.ListHandler<UserInfo> getDateSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<UserInfo> getNumSorter(Column<UserInfo, SafeHtml> englishCol,
                                                             List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getNum()).compareTo(o2.getNum());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserInfo> getStartSorter(Column<UserInfo, SafeHtml> englishCol,
                                                               List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getStart()).compareTo(o2.getStart());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<UserInfo> getCurrentSorter(Column<UserInfo, SafeHtml> englishCol,
                                                                 List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getCurrent()).compareTo(o2.getCurrent());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<UserInfo> getDiffSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getDiff()).compareTo(o2.getDiff());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  @Override
  protected void addColumnsToTable() {
    Column<UserInfo, SafeHtml> userCol = getUserColumn();
    userCol.setSortable(true);
    table.setColumnWidth(userCol, ID_WIDTH + "px");
    addColumn(userCol, new TextHeader("Students"));
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = getUserSorter(userCol, getList());
    table.addColumnSortHandler(columnSortHandler);

    Column<UserInfo, SafeHtml> dateCol = getDateColumn();
    dateCol.setSortable(true);
    addColumn(dateCol, new TextHeader("Signed Up At"));
    table.setColumnWidth(dateCol, 100 + "px");
    table.addColumnSortHandler(getDateSorter(dateCol, getList()));

    Column<UserInfo, SafeHtml> num = getNum();
    num.setSortable(true);
    addColumn(num, new TextHeader("#"));
    table.addColumnSortHandler(getNumSorter(num, getList()));
    table.setColumnWidth(num, 50 + "px");

    Column<UserInfo, SafeHtml> start = getStart();
    start.setSortable(true);
    addColumn(start, new TextHeader("Initial Score"));
    table.setColumnWidth(start, 75 + "px");

    table.addColumnSortHandler(getStartSorter(start, getList()));

    Column<UserInfo, SafeHtml> current = getCurrent();
    current.setSortable(true);
    addColumn(current, new TextHeader("Current"));
    table.setColumnWidth(current, 75 + "px");

    table.addColumnSortHandler(getCurrentSorter(current, getList()));

    Column<UserInfo, SafeHtml> diff = getDiff();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Diff"));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, 75 + "px");

    table.getColumnSortList().push(dateCol);

    table.setWidth("100%", true);

    new TooltipHelper().addTooltip(table, "Click on a student.");
  }

  private Column<UserInfo, SafeHtml> getUserColumn() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml(shell.getUser().getUserID());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getDateColumn() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public boolean isDefaultSortAscending() {
        return false;
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        String signedUp = DateTimeFormat.getFormat("MMM d, yy").format(
            //     new Date(shell.getUser().getTimestampMillis())
            new Date(shell.getTimestampMillis())
        );

        return getSafeHtml(signedUp);
      }
    };
  }

  private Column<UserInfo, SafeHtml> getStart() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getStart());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getCurrent() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getCurrent());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getDiff() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getDiff());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getNum() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getNum());
      }
    };
  }

  private void gotClickOnItem(final UserInfo user) {
    AnalysisTab widgets = new AnalysisTab(service, controller, (int) user.getUser().getId(), learnTab, user.getUser().getUserID(), 5);
    rightSide.clear();
    rightSide.add(widgets);
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
