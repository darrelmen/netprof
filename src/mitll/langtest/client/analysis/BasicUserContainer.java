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
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.user.MiniUser;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class BasicUserContainer<T extends MiniUser> extends SimplePagingContainer<T> {
  static final int TABLE_WIDTH = 420;
  private static final int MAX_LENGTH_ID = 13;
  private static final int PAGE_SIZE = 11;
  protected final Long selectedUser;
  protected final String selectedUserKey;
  protected final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  protected final Date now = new Date();
  private final Logger logger = Logger.getLogger("UserContainer");
  String header;

  private static final int ID_WIDTH = 130;
  private static final int SIGNED_UP = 95;
  private static final String SIGNED_UP1 = "Started";

  protected BasicUserContainer(ExerciseController controller,
                               String selectedUserKey) {
    super(controller);
    this.selectedUserKey = selectedUserKey;
    this.selectedUser = getSelectedUser(selectedUserKey);
    header = "Student";
  }

  protected String truncate(String columnText) {
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

  @Override
  protected void addColumnsToTable() {
    Column<T, SafeHtml> userCol = getUserColumn();
    userCol.setSortable(true);
    table.setColumnWidth(userCol, ID_WIDTH + "px");
    addColumn(userCol, new TextHeader(header));
    ColumnSortEvent.ListHandler<T> columnSortHandler = getUserSorter(userCol, getList());
    table.addColumnSortHandler(columnSortHandler);

    Column<T, SafeHtml> dateCol = getDateColumn();
    dateCol.setSortable(true);
    addColumn(dateCol, new TextHeader(SIGNED_UP1));
    table.setColumnWidth(dateCol, SIGNED_UP + "px");
    table.addColumnSortHandler(getDateSorter(dateCol, getList()));
  }

  /**
   * @param users
   * @return
   * @see StudentAnalysis#StudentAnalysis
   */
  public Panel getTableWithPager(final Collection<T> users) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.addStyleName("floatLeft");

    for (T user : users) {
      addItem(user);
    }
    flush();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (!users.isEmpty()) {
          if (selectedUser == null) {
            T next = users.iterator().next();
            table.getSelectionModel().setSelected(next, true);
            gotClickOnItem(next);
          } else {
            int i = 0;
            for (T userInfo : users) {
              if (userInfo.getId() == selectedUser) {
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
    selectionModel = new SingleSelectionModel<T>();
    table.setSelectionModel(selectionModel);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(UserContainer.LocalTableResources.class);
    return o;
  }

  protected ColumnSortEvent.ListHandler<T> getUserSorter(Column<T, SafeHtml> englishCol,
                                                                List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
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

  protected ColumnSortEvent.ListHandler<T> getDateSorter(Column<T, SafeHtml> englishCol,
                                                                List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
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

  protected SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  protected Long getSelectedUser(String selectedUserKey) {
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

  protected void storeSelectedUser(long selectedUser) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(selectedUserKey, "" + selectedUser);

    }
  }

  protected void addTooltip() {
    new TooltipHelper().addTooltip(table, "Click on a " +
        header +
        ".");
  }

  protected Column<T, SafeHtml> getUserColumn() {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(truncate(shell.getUserID()));
      }
    };
  }

  protected Column<T, SafeHtml> getDateColumn() {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
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
      public SafeHtml getValue(T shell) {
        String signedUp = format.format(
            //     new Date(shell.getUser().getTimestampMillis())
            new Date(shell.getTimestampMillis())
        );

        String format = BasicUserContainer.this.format.format(now);
        if (format.substring(format.length() - 2).equals(signedUp.substring(signedUp.length() - 2))) {
          signedUp = signedUp.substring(0, signedUp.length() - 4);
        }

        return getSafeHtml(signedUp);
      }
    };
  }

  protected void gotClickOnItem(final T user) {
    MiniUser user1 = user;//user.getUser();
    int id =  user1.getId();
//    overallBottom.clear();
//    AnalysisTab widgets = new AnalysisTab(exerciseServiceAsync, controller, id, learnTab, user1.getUserID(), MIN_RECORDINGS, overallBottom);
//    rightSide.clear();
//    rightSide.add(widgets);
    storeSelectedUser(id);
  }
}
