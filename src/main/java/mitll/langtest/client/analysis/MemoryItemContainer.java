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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public abstract class MemoryItemContainer<T extends HasID> extends ClickablePagingContainer<T> {
  private final Logger logger = Logger.getLogger("MemoryItemContainer");

  private static final int TABLE_WIDTH = 420;
  private static final int MAX_LENGTH_ID = 13;

  /**
   *
   */
  private final String selectedUserKey;
  private final String header;

  private static final String SIGNED_UP1 = "Started";

  static final String SELECTED_USER = "selectedUser";

  private static final int ID_WIDTH = 100;
  private int idWidth = ID_WIDTH;
  private int pageSize;
  private final String todayYear;
  private final String todaysDate;

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");
  private int shortPageSize;// = 8;
  Column<T, SafeHtml> dateCol;

  /**
   * @param controller
   * @param selectedUserKey
   * @param header
   * @param pageSize
   * @param shortPageSize
   * @see BasicUserContainer#BasicUserContainer
   */
  protected MemoryItemContainer(ExerciseController controller,
                                String selectedUserKey,
                                String header,
                                int pageSize,
                                int shortPageSize) {
    super(controller);
    this.selectedUserKey = selectedUserKey;
    this.header = header;
    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - 2);

    this.pageSize = pageSize;
    this.shortPageSize = shortPageSize;
  }

  DivWidget getTable(Collection<T> users, String title, String subtitle) {
    DivWidget leftSide = new DivWidget();
    leftSide.getElement().setId("studentDiv");
    leftSide.addStyleName("floatLeft");

    {
/*
      DivWidget headerRow = new DivWidget();
      headerRow.setWidth("100%");
      if (!title.isEmpty()) {
        headerRow.add(getStudentsHeader(title, subtitle));
      }
      leftSide.add(headerRow);
*/

/*
      IsWidget rightOfHeader = getRightOfHeader();
      if (rightOfHeader != null) {
        headerRow.add(rightOfHeader);
      }
      */
      IsWidget belowHeader = getBelowHeader();
      if (belowHeader != null) {
        leftSide.add(belowHeader);
      }
    }

    addTable(users, leftSide);
    return leftSide;
  }

  protected void addTable(Collection<T> users, DivWidget leftSide) {
    leftSide.add(getTableWithPager(users));
  }

  @NotNull
  private Heading getStudentsHeader(String title, String subtitle) {
    Heading students = subtitle.isEmpty() ?
        new Heading(3, title) :
        new Heading(3, title, subtitle);

    students.getElement().getStyle().setMarginBottom(2, Style.Unit.PX);
    students.addStyleName("floatLeft");
    students.setWidth("100%");
    return students;
  }

  private IsWidget getRightOfHeader() {
    return null;
  }

  protected IsWidget getBelowHeader() {
    return null;
  }

  protected String truncate(String columnText) {
    return truncate(columnText, getMaxLengthId());
  }

  @NotNull
  protected String truncate(String columnText, int maxLengthId) {
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
    return isOnLaptop() ? shortPageSize : pageSize;
  }

  /**
   * @return
   */
  private boolean isOnLaptop() {
    return Window.getClientHeight() < 822;
  }


  /**
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<T> list = getList();
    addItemID(list, getMaxLengthId());
    addDateCol(list);
  }

  /**
   * @param list
   * @param maxLength
   */
  protected void addItemID(List<T> list, int maxLength) {
    Column<T, SafeHtml> userCol = getItemColumn(maxLength);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader(header));
    table.addColumnSortHandler(getUserSorter(userCol, list));
  }

  protected void addDateCol(List<T> list) {
    dateCol = getDateColumn();
    dateCol.setSortable(true);
    addColumn(dateCol, new TextHeader(getDateColHeader()));
    table.setColumnWidth(dateCol, 100 + "px");
    table.addColumnSortHandler(getDateSorter(dateCol, list));
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

    populateTable(users);
    return tableWithPager;
  }

  /**
   * @param users
   * @see #getTableWithPager
   */
  void populateTable(Collection<T> users) {
    int i = 0;
    int index = 0;
    T userToSelect = null;
    getList().clear();

    Long selectedUser = getSelectedUser(selectedUserKey);

    for (T user : users) {
      addItem(user);

      if (selectedUser != null && user.getID() == selectedUser) {
        index = i;
        userToSelect = user;
        logger.info("populateTable Selected user found  "+ selectedUser + " at " +index + " out of " + users.size());
      }
      i++;
    }

    flush();

   // if (index > 0) {
      logger.info("populateTable scroll to " +index);
      scrollIntoView(index, false);
   // }
  //  else {
  //    logger.info("populateTable index = " +index);
   // }

    if (!users.isEmpty()) {
      makeInitialSelection(users.iterator().next(), userToSelect);
    }
  }

  /**
   * @param userToSelect
   * @paramx users
   * @see #getTableWithPager
   */
  private void makeInitialSelection(T firstUser, T userToSelect) {
    //logger.info("makeInitialSelection make initial selection : select " + userToSelect);
    Scheduler.get().scheduleDeferred(() -> selectAndClick((userToSelect == null) ? firstUser : userToSelect));
  }

  private void selectAndClick(T firstUser) {
    table.getSelectionModel().setSelected(firstUser, true);
    gotClickOnItem(firstUser);
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
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
  }

  private ColumnSortEvent.ListHandler<T> getUserSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getIDCompare);
    return columnSortHandler;
  }

  protected abstract int getIDCompare(T o1, T o2);

  private ColumnSortEvent.ListHandler<T> getDateSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getDateCompare);
    return columnSortHandler;
  }

  protected abstract int getDateCompare(T o1, T o2);

  protected SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  /**
   * @param selectedUserKey
   * @return
   * @see #MemoryItemContainer
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
    return null;
  }

  /**
   * @param selectedUser
   * @see #gotClickOnItem
   */
  private void storeSelectedUser(long selectedUser) {
    //logger.info("storeSelectedUser " + selectedUserKey + " = " + selectedUser);
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(selectedUserKey, "" + selectedUser);
    }
    if (selectedUser != getSelectedUser(selectedUserKey)) {
      logger.warning("huh? stored " + selectedUserKey + " but got " + getSelectedUser(selectedUserKey));
    }
  }

  void addTooltip() {
    new TooltipHelper().addTooltip(table, "Click on a " + header + ".");
  }

  private Column<T, SafeHtml> getItemColumn(int maxLength) {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(getTruncatedItemLabel(shell, maxLength));
      }
    };
  }

  private String getTruncatedItemLabel(T shell, int maxLength) {
    return truncate(getItemLabel(shell), maxLength);
  }

  protected abstract String getItemLabel(T shell);

  /**
   * @return
   * @see #addDateCol
   */
  private Column<T, SafeHtml> getDateColumn() {
    return new Column<T, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
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

        return getSafeHtml("<span style='white-space:nowrap;'>" + signedUp + "</span>");
      }
    };
  }

  protected abstract Long getItemDate(T shell);

  protected void checkGotClick(T object, NativeEvent event) {
    if (isClick(event)) {
      gotClickOnItem(object);
    }
  }

  private boolean isClick(NativeEvent event) {
    return BrowserEvents.CLICK.equals(event.getType());
  }

  public void gotClickOnItem(final T user) {
    storeSelectedUser(user.getID());
  }
}
