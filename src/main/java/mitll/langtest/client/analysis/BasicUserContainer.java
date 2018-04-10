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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.user.SimpleUser;

import java.util.Collection;
import java.util.List;

/**
 * Short page size is for laptops...
 *
 * @param <T>
 */
public class BasicUserContainer<T extends SimpleUser> extends MemoryItemContainer<T> {
  protected static final int NAME_WIDTH = 130;
  private static final int MAX_LENGTH = 11;
  private static final int TABLE_WIDTH = 600;
  private static final int FIRST_WIDTH = 90;
  private static final int LAST_WIDTH = 100;
  private static final String FIRST = "First";

  public BasicUserContainer(ExerciseController controller,
                            String selectedUserKey,
                            String header) {
    super(controller, selectedUserKey, header, 10, 7);
  }

  @Override
  public Panel getTableWithPager(Collection<T> users) {
    Panel tableWithPager = super.getTableWithPager(users);
    tableWithPager.getElement().getStyle().setProperty("minHeight", 250 + "px");
    return tableWithPager;
  }

  /**
   * @see SimplePagingContainer#configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<T> list = getList();
    addItemID(list, 20);

    addFirstName(list);
    addLastName(list);

    addDateCol(list);

    table.setWidth("100%", true);

   // addTooltip();
  }

  /**
   * @param o1
   * @param o2
   * @return
   */
  protected int getIDCompare(T o1, T o2) {
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

  int getFirstCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getFirst().compareTo(o2.getFirst());

        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }

  private int getLastCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getLast().compareTo(o2.getLast());
        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }

  int getNameCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getName().compareTo(o2.getName());
        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }

  protected String getItemLabel(T shell) {
    return shell.getUserID();
  }

  protected int getDateCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return Long.compare(o1.getTimestampMillis(), o2.getTimestampMillis());
      }
    }
    return -1;
  }

  public Long getItemDate(T shell) {
    return shell.getTimestampMillis();
  }

  protected int getMaxLengthId() {
    return MAX_LENGTH;
  }

  protected int getMaxTableWidth() {
    return TABLE_WIDTH;
  }

  void addFirstName(List<T> list) {
    Column<T, SafeHtml> userCol = new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(shell.getFirst());
      }
    };
    userCol.setSortable(true);
    table.setColumnWidth(userCol, FIRST_WIDTH + "px");
    addColumn(userCol, new TextHeader(FIRST));
    table.addColumnSortHandler(getFirstSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<T> getFirstSorter(Column<T, SafeHtml> englishCol,
                                                        List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getFirstCompare);
    return columnSortHandler;
  }

  void addLastName(List<T> list) {
    Column<T, SafeHtml> userCol = new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(shell.getLast());
      }
    };
    userCol.setSortable(true);
    table.setColumnWidth(userCol, LAST_WIDTH + "px");
    addColumn(userCol, new TextHeader("Last"));
    table.addColumnSortHandler(getLastSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<T> getLastSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getLastCompare);
    return columnSortHandler;
  }

}
