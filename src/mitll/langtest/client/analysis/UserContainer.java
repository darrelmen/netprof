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
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.User;
import mitll.langtest.shared.analysis.UserInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class UserContainer extends SimplePagingContainer<UserInfo> {
  private final Logger logger = Logger.getLogger("UserContainer");

  private static final String SIGNED_UP1 = "Started";

  private static final int MAX_LENGTH_ID = 13;

  private static final int ID_WIDTH = 130;
  public static final int TABLE_WIDTH = 420;
  private static final int SIGNED_UP = 95;
  private static final String CURRENT = "Curr.";
  private static final int CURRENT_WIDTH = 60;
  private static final int DIFF_WIDTH = 55;
  private static final int INITIAL_SCORE_WIDTH = 75;
  private static final String DIFF_COL_HEADER = "+/-";
  private static final int MIN_RECORDINGS = 5;

  private static final int PAGE_SIZE = 11;

  private final ShowTab learnTab;
  private final DivWidget rightSide;
  private final DivWidget overallBottom;
  private final LangTestDatabaseAsync service;
  private final Long selectedUser;
  private final String selectedUserKey;

  /**
   * @param controller
   * @param rightSide
   * @see StudentAnalysis#StudentAnalysis(LangTestDatabaseAsync, ExerciseController, ShowTab)
   */
  public UserContainer(LangTestDatabaseAsync service, ExerciseController controller,
                       DivWidget rightSide,
                       DivWidget overallBottom,
                       ShowTab learnTab,
                       String selectedUserKey
  ) {
    super(controller, false);
    this.rightSide = rightSide;
    this.learnTab = learnTab;
    this.service = service;
    this.overallBottom = overallBottom;
    this.selectedUserKey = selectedUserKey;
    this.selectedUser = getSelectedUser(selectedUserKey);
  }

  private String truncate(String columnText) {
    if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
    return columnText;
  }

  /**
   * @see SimplePagingContainer#makeCellTable()
   * @return
   */
  protected int getPageSize() {
    return isShort() ? 8:PAGE_SIZE;
  }

  private boolean isShort() {
    return Window.getClientHeight() < 822;
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
          if (selectedUser == null) {
            UserInfo next = users.iterator().next();
            table.getSelectionModel().setSelected(next, true);
            gotClickOnItem(next);
          } else {
            int i = 0;
            for (UserInfo userInfo : users) {
              if (userInfo.getUser().getId() == selectedUser) {
            //    logger.info("found previous selection - " + userInfo + " : " + i);
                table.getSelectionModel().setSelected(userInfo, true);
                gotClickOnItem(userInfo);

                final int index = i;
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                  public void execute() {
                    scrollIntoView(index);
                  }
                });

                break;
              }
              i++;
            }
          }
        } else {
          logger.warning("huh? users is empty???");
        }
      }
    });

    return tableWithPager;
  }

  private void scrollIntoView(int i) {
    int pageSize = table.getPageSize();
    int pageNum = i / pageSize;
    int newIndex = pageNum * pageSize;

    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
//                  if (DEBUG) logger.info("new start of " + i+ " " + pageNum + " size " + pageSize +
//                      " prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, pageSize);
    } else {
      int pageEnd = table.getPageStart() + pageSize;
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - pageSize, newIndex));   // not sure how this happens, but need Math.max(0,...)
//        if (DEBUG) logger.info("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
//            " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, pageSize);
        table.redraw();
      }
    }
    i++;
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
    addColumn(userCol, new TextHeader("Student"));
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = getUserSorter(userCol, getList());
    table.addColumnSortHandler(columnSortHandler);

    Column<UserInfo, SafeHtml> dateCol = getDateColumn();
    dateCol.setSortable(true);
    addColumn(dateCol, new TextHeader(SIGNED_UP1));
    table.setColumnWidth(dateCol, SIGNED_UP + "px");
    table.addColumnSortHandler(getDateSorter(dateCol, getList()));

    Column<UserInfo, SafeHtml> num = getNum();
    num.setSortable(true);
    addColumn(num, new TextHeader("#"));
    table.addColumnSortHandler(getNumSorter(num, getList()));
    table.setColumnWidth(num, 50 + "px");

    Column<UserInfo, SafeHtml> start = getStart();
    start.setSortable(true);
    addColumn(start, new TextHeader("Initial Score"));
    table.setColumnWidth(start, INITIAL_SCORE_WIDTH + "px");

    table.addColumnSortHandler(getStartSorter(start, getList()));

    Column<UserInfo, SafeHtml> current = getCurrent();
    current.setSortable(true);
    addColumn(current, new TextHeader(CURRENT));
    table.setColumnWidth(current, CURRENT_WIDTH + "px");

    table.addColumnSortHandler(getCurrentSorter(current, getList()));

    Column<UserInfo, SafeHtml> diff = getDiff();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(DIFF_COL_HEADER));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, DIFF_WIDTH + "px");

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
        return getSafeHtml(truncate(shell.getUser().getUserID()));
      }
    };
  }

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final Date now = new Date();

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
        String signedUp = format.format(
            //     new Date(shell.getUser().getTimestampMillis())
            new Date(shell.getTimestampMillis())
        );

        String format = UserContainer.this.format.format(now);
        if (format.substring(format.length() - 2).equals(signedUp.substring(signedUp.length() - 2))) {
          signedUp = signedUp.substring(0, signedUp.length() - 4);
        }

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
    User user1 = user.getUser();
    int id = (int) user1.getId();
    overallBottom.clear();
    AnalysisTab widgets = new AnalysisTab(service, controller, id, learnTab, user1.getUserID(), MIN_RECORDINGS, overallBottom);
    rightSide.clear();
    rightSide.add(widgets);
    storeSelectedUser(user.getUser().getId());
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Long getSelectedUser(String selectedUserKey) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String item = localStorageIfSupported.getItem(selectedUserKey);
      if (item != null) {
        try {
          return Long.parseLong(item);
        } catch (NumberFormatException e) {
          logger.warning("got " + e);
          return null;

        }
      }
    }
    // else {
    return null;
    // }
  }

  private void storeSelectedUser(long selectedUser) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(selectedUserKey, "" + selectedUser);

    }
  }

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "../exercise/ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
