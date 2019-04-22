/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.custom.userlist;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static mitll.langtest.shared.custom.UserList.LIST_TYPE.QUIZ;

/**
 * Created by go22670 on 7/6/17.
 */
public class ListContainer<T extends UserList<CommonShell>> extends ButtonMemoryItemContainer<T> {
  //private final Logger logger = Logger.getLogger("ListContainer");

  private static final String DESCRIPTION = "Description";
  private static final String CLASS = "Class";
  private static final String CREATOR = "Creator";
  private static final String NUM_ITEMS = "#";
  private static final int DESC_WIDTH = 180;
  private static final int DESC_MAX_LENGTH = 25;//30;
  private final boolean slim;
  private final boolean addOwnerToDescrip;
  private final boolean addTeacherCol;

  /**
   * @param controller
   * @param pageSize
   * @param slim
   * @param storageID
   * @param shortPageSize
   * @see ListView#addVisitedTable
   */
  ListContainer(ExerciseController controller, int pageSize, boolean slim, String storageID, int shortPageSize,
                boolean addOwnerToDescrip, boolean addTeacherCol) {
    super(controller, "netprof" + ":" + controller.getUser() + ":" + storageID, "List",
        pageSize, shortPageSize);
    this.slim = slim;
    this.addOwnerToDescrip = addOwnerToDescrip;
    this.addTeacherCol = addTeacherCol;
  }

  @NotNull
  protected String getDateColHeader() {
    return "Date";
  }

  protected int getIdWidth() {
    return 200;
  }

  @Override
  protected void setMaxWidth() {
  }

  @Override
  protected int getMaxLengthId() {
    return 45;
  }

  /**
   * @see #configureTable
   */
  @Override
  protected void addColumnsToTable() {
    List<T> list = getList();
    addItemID(list, getMaxLengthId());
    addNum();
    addDateCol(list);

    addDescrip();

    if (slim) {
      addIsPublic();
      if (canMakeQuiz()) {
        addIsQuiz();
      }
    } else {
      addClass();
      addOwner();
    }

    if (addTeacherCol) {
      addIsTeacher();
    }
  }

  protected Column<T, SafeHtml> getItemColumn(int maxLength) {
    return getTruncatedCol2(maxLength, this::getItemLabel);
  }

  /**
   * Is sortable.
   *
   * @param maxLength
   * @param getSafe
   * @return
   */
  private Column<T, SafeHtml> getTruncatedCol2(int maxLength, GetSafe<T> getSafe) {
    Column<T, SafeHtml> column = new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        String truncate = truncate(getSafe.getSafe(shell), maxLength);
        return shell.getListType() == UserList.LIST_TYPE.QUIZ ? getNoWrapContentBlue(truncate) : getNoWrapContent(truncate);
      }
    };
    column.setSortable(true);

    return column;
  }

  private boolean canMakeQuiz() {
    Collection<Permission> permissions = controller.getPermissions();
    return permissions.contains(Permission.TEACHER_PERM) || permissions.contains(Permission.PROJECT_ADMIN);
  }

  @Override
  protected int getIDCompare(T o1, T o2) {
    return o1.getName().compareTo(o2.getName());
  }

  @Override
  protected int getDateCompare(T o1, T o2) {
    return Long.compare(o1.getModified(), o2.getModified());
  }

  @Override
  protected String getItemLabel(T shell) {
    return shell.getName();
  }

  @Override
  public Long getItemDate(T shell) {
    return shell.getModified();
  }

  private void addNum() {
    Column<T, SafeHtml> diff = getNum();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(NUM_ITEMS));
    table.addColumnSortHandler(getNumSorter(diff, getList()));
    table.setColumnWidth(diff, 40 + "px");
  }

  private void addDescrip() {
    Column<T, SafeHtml> diff = getDescription();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(DESCRIPTION));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, DESC_WIDTH + "px");
  }

  private void addClass() {
    Column<T, SafeHtml> diff = getListClass();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(CLASS));
    table.addColumnSortHandler(getClassSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

  private void addOwner() {
    Column<T, SafeHtml> diff = getOwner();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(CREATOR));
    table.addColumnSortHandler(getOwnerSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

//  private void addIsPublic() {
//    Column<T, SafeHtml> diff = getPublic();
//    diff.setSortable(true);
//    addColumn(diff, new TextHeader(PUBLIC));
//    table.addColumnSortHandler(getPublicSorted(diff, getList()));
//    table.setColumnWidth(diff, 50 + "px");
//  }

  private void addIsTeacher() {
    Column<T, SafeHtml> diff = getTeacher();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("By Teacher?"));
    table.addColumnSortHandler(getTeacherSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private void addIsQuiz() {
    Column<T, SafeHtml> diff = getQuiz();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Quiz?"));
    table.addColumnSortHandler(getQuizSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private Column<T, SafeHtml> getNum() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml("" + shell.getNumItems());
      }
    };
  }


  private Column<T, SafeHtml> getDescription() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        String owner = addOwnerToDescrip && (shell.getUserID() != controller.getUser()) ? "(" + shell.getFirstInitialName() + ") " : "";
        String description = owner + shell.getDescription();
        //   logger.info("Desc " + description + " length " + description.length());
        String truncate = truncate(description, DESC_MAX_LENGTH);
        //   logger.info("truncate " + truncate + " length " + truncate.length());
        return getNoWrapContent(truncate);
      }
    };
  }

  private Column<T, SafeHtml> getListClass() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(truncate(shell.getClassMarker()));
      }
    };
  }

  private Column<T, SafeHtml> getOwner() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        String firstInitialName = shell.getFirstInitialName();
        return getSafeHtml(truncate(firstInitialName, 15));
      }
    };
  }

  private Column<T, SafeHtml> getTeacher() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(shell.isTeacher() ? YES : NO);
      }
    };
  }

  private Column<T, SafeHtml> getQuiz() {
    return new Column<T, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(shell.getListType() == QUIZ ? YES : NO);
      }
    };
  }

  private ColumnSortEvent.ListHandler<T> getNumSorter(Column<T, SafeHtml> englishCol,
                                                      List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getNumItems));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<T> getDiffSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getDescription));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<T> getOwnerSorted(Column<T, SafeHtml> englishCol,
                                                        List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getFirstInitialName));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<T> getTeacherSorted(Column<T, SafeHtml> englishCol,
                                                          List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::isTeacher));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<T> getQuizSorted(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getListType));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<T> getClassSorted(Column<T, SafeHtml> englishCol,
                                                        List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getClassMarker));
    return columnSortHandler;
  }
}
