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

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
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
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public abstract class MemoryItemContainer<T extends HasID> extends ClickablePagingContainer<T> {
  private final Logger logger = Logger.getLogger("MemoryItemContainer");

  private static final int TABLE_WIDTH = 420;
  private static final int MAX_LENGTH_ID = 13;

  private static final int PAGE_SIZE = 11;
  private final Long selectedUser;
  private final String selectedUserKey;
  private final String header;

  private static final int SIGNED_UP = 95;
  private static final String SIGNED_UP1 = "Started";
  private static final int STUDENT_WIDTH = 300;
  public static final String SELECTED_USER = "selectedUser";
  static final int ID_WIDTH = 130;
  private int idWidth = ID_WIDTH;
  int pageSize = PAGE_SIZE;
  private final String todayYear;
  private final String todaysDate;

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");

  public MemoryItemContainer(ExerciseController controller, String selectedUserKey, String header) {
    this(controller, selectedUserKey, header, PAGE_SIZE);
  }

  /**
   * @param controller
   * @param selectedUserKey
   * @param header
   * @param pageSize
   * @see BasicUserContainer#BasicUserContainer
   */
  public MemoryItemContainer(ExerciseController controller,
                             String selectedUserKey,
                             String header, int pageSize) {
    super(controller);
    this.selectedUserKey = selectedUserKey;
    this.selectedUser = getSelectedUser(selectedUserKey);
    this.header = header;
    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - 2);
    this.pageSize = pageSize;
  }

  /**
   * @param controller
   * @param header
   * @param idWidth
   * @see mitll.langtest.client.analysis.BasicUserContainer#BasicUserContainer
   */
  MemoryItemContainer(ExerciseController controller, String header, int idWidth) {
    super(controller);
    this.selectedUserKey = getSelectedUserKey(controller, header);
    this.selectedUser = getSelectedUser(selectedUserKey);
    this.header = header;
    this.idWidth = idWidth;
    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - 2);
  }

  DivWidget getTable(Collection<T> users, String title, String subtitle) {
    return getStudentContainer(getTableWithPager(users), title, subtitle);
  }

  private DivWidget getStudentContainer(Panel tableWithPager, String title, String subtitle) {
    Heading students = subtitle.isEmpty() ?
        new Heading(3, title) :
        new Heading(3, title, subtitle);

    students.setWidth(STUDENT_WIDTH + "px");
    students.getElement().getStyle().setMarginBottom(2, Style.Unit.PX);

    DivWidget leftSide = new DivWidget();
    leftSide.getElement().setId("studentDiv");
    leftSide.addStyleName("floatLeft");
    if (!title.isEmpty()) {
      leftSide.add(students);
    }
    leftSide.add(tableWithPager);
    return leftSide;
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + SELECTED_USER;
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

  private String truncate(String columnText) {
    int maxLengthId = getMaxLengthId();
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }

  protected int getMaxLengthId() {
    return MAX_LENGTH_ID;
  }

  /**
   * @return
   * @see SimplePagingContainer#makeCellTable
   */
  protected int getPageSize() {
    return isShort() ? 8 : pageSize;
  }

  private boolean isShort() {
    return Window.getClientHeight() < 822;
  }

  protected Column<T, SafeHtml> dateCol;

  /**
   * @param sortEnglish
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    {
      Column<T, SafeHtml> userCol = getItemColumn();
      userCol.setSortable(true);
      table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader(header));
      table.addColumnSortHandler(getUserSorter(userCol, getList()));
    }

    {
      dateCol = getDateColumn();
      dateCol.setSortable(true);
      addColumn(dateCol, new TextHeader(getDateColHeader()));
      table.setColumnWidth(dateCol, 100 + "px");
      table.addColumnSortHandler(getDateSorter(dateCol, getList()));
    }
  }

  protected int getIdWidth() {
    return idWidth;
  }

  @NotNull
  protected String getDateColHeader() {
    return SIGNED_UP1;
  }

  /**
   * @param users
   * @return
   * @see StudentAnalysis#StudentAnalysis
   */
  public Panel getTableWithPager(final Collection<T> users) {
    // logger.info("getTableWithPager" + users.size());
    Panel tableWithPager = getTableWithPager(new ListOptions());
    // logger.info("getTableWithPager tableWithPager ");

    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.addStyleName("floatLeftAndClear");

    int i = 0;
    int index = 0;
    T userToSelect = null;
    for (T user : users) {
      addItem(user);

      if (selectedUser != null && user.getID() == selectedUser) {
        index = i;
        userToSelect = user;
      }
      i++;
    }

    flush();

    if (index > 0) {
      scrollIntoView(index, false);
    }

    makeInitialSelection(users, userToSelect);
    return tableWithPager;
  }

  /**
   * @param users
   * @param userToSelect
   * @see #getTableWithPager
   */
  protected void makeInitialSelection(Collection<T> users, T userToSelect) {
    if (users.isEmpty()) {
      return;
    }
    final T finalUser = userToSelect;

    logger.info("makeInitialSelection make initial selection from " + users.size() + " to select " + userToSelect);

    Scheduler.get().scheduleDeferred(() -> {
          if (selectedUser == null || finalUser == null) {
            T next = users.iterator().next();
            //        logger.info("\t makeInitialSelection make initial selection " + next);
            table.getSelectionModel().setSelected(next, true);
            gotClickOnItem(next);
          } else {
//            if (finalUser != null) {
            table.getSelectionModel().setSelected(finalUser, true);
            gotClickOnItem(finalUser);
            //          } else {
            //          logger.warning("makeInitialSelection no initial user?");
            //      }
          }
        }
    );
  }

  /**
   * @param i
   * @param doRedraw
   * @see #getTableWithPager
   */
  private void scrollIntoView(int i, boolean doRedraw) {
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
        if (doRedraw) {
          table.redraw();
        }
      }
    }
    //  i++;
  }

  @Override
  protected void setMaxWidth() {
    int tableWidth = getMaxTableWidth();
    table.getElement().getStyle().setProperty("maxWidth", tableWidth + "px");
  }

  protected int getMaxTableWidth() {
    return TABLE_WIDTH;
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

  private ColumnSortEvent.ListHandler<T> getUserSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            return getNameCompare(o1, o2);
          }
        });
    return columnSortHandler;
  }

  protected abstract int getNameCompare(T o1, T o2);

  private ColumnSortEvent.ListHandler<T> getDateSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            return getDateCompare(o1, o2);
          }
        });
    return columnSortHandler;
  }

  protected abstract int getDateCompare(T o1, T o2);

  protected SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  /**
   * @param selectedUserKey
   * @return
   * @see #MemoryItemContainer(ExerciseController, String, int)
   */
  private Long getSelectedUser(String selectedUserKey) {
    if (selectedUserKey == null) return null;
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

  /**
   * @param selectedUser
   * @see #gotClickOnItem
   */
  private void storeSelectedUser(long selectedUser) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(selectedUserKey, "" + selectedUser);
    }
  }

  protected void addTooltip() {
    new TooltipHelper().addTooltip(table, "Click on a " + header + ".");
  }

  private Column<T, SafeHtml> getItemColumn() {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

//      @Override
//      public String getCellStyleNames(Cell.Context context, T object) {
//        return shouldHighlight(object) ? "tableRowUserCurrentColor" : "";
//      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(getTruncatedItemLabel(shell));
      }
    };
  }

//  protected boolean shouldHighlight(T object) {
//    return false;
//  }

  protected String getTruncatedItemLabel(T shell) {
    return truncate(getItemLabel(shell));
  }

  protected abstract String getItemLabel(T shell);

  private Column<T, SafeHtml> getDateColumn() {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (isClick(event)) gotClickOnItem(object);
      }

      @Override
      public boolean isDefaultSortAscending() {
        return false;
      }

      @Override
      public SafeHtml getValue(T shell) {
        Date date = new Date(getItemDate(shell));
        String signedUp = format.format(date);

        // drop year if this year
        if (signedUp.equals(todaysDate)) {
          signedUp = todayTimeFormat.format(date);
        } else if (todayYear.equals(signedUp.substring(signedUp.length() - 2))) {
          signedUp = signedUp.substring(0, signedUp.length() - 4);
        }

        return getSafeHtml(signedUp);
      }
    };
  }

  public abstract Long getItemDate(T shell);

  protected void checkGotClick(T object, NativeEvent event) {
    if (isClick(event)) {
      gotClickOnItem(object);
    }
    if (isDoubleClick(event)) {
      logger.info("got double!");
    }
  }

  private boolean isClick(NativeEvent event) {
    return BrowserEvents.CLICK.equals(event.getType());
  }

  private boolean isDoubleClick(NativeEvent event) {
    return BrowserEvents.DBLCLICK.equals(event.getType());
  }

  public void gotClickOnItem(final T user) {
    storeSelectedUser(user.getID());
  }

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
