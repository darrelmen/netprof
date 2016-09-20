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

package mitll.langtest.client.userops;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.analysis.BasicUserContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.user.MiniUser;

import java.util.Comparator;
import java.util.List;

public class OpsUserContainer extends BasicUserContainer<MiniUser> {
  public OpsUserContainer(ExerciseController controller, String header) {
    super(controller, header);
  }

  @Override
  protected void addColumnsToTable() {
    super.addColumnsToTable();

    Column<MiniUser, SafeHtml> firstNameCol = getFirstCol();
    firstNameCol.setSortable(true);
    table.setColumnWidth(firstNameCol, ID_WIDTH + "px");
    addColumn(firstNameCol, new TextHeader("First"));
    ColumnSortEvent.ListHandler<MiniUser> columnSortHandler = getNameSorter(firstNameCol, getList());
    table.addColumnSortHandler(columnSortHandler);

    table.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(dateCol, true));
    table.setWidth("100%", true);

    addTooltip();
  }

  private Column<MiniUser, SafeHtml> getFirstCol() {
    return new Column<MiniUser, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, MiniUser object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(MiniUser shell) {
        return getSafeHtml(truncate(shell.getFirst()));
      }
    };
  }

  private ColumnSortEvent.ListHandler<MiniUser> getNameSorter(Column<MiniUser, SafeHtml> englishCol,
                                                              List<MiniUser> dataList) {
    ColumnSortEvent.ListHandler<MiniUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<MiniUser>() {
          public int compare(MiniUser o1, MiniUser o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getFirst().compareTo(o2.getFirst());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }
}
