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

package mitll.langtest.client.custom.dialog;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;

import java.util.List;

/**
 * TODOx : add public/private column, and to dialog itself!
 *
 * @param <T>
 */
public class DialogContainer<T extends IDialog> extends SummaryDialogContainer<T> {
  public static final String DIALOGS = "dialogs";
  //private final Logger logger = Logger.getLogger("DialogContainer");
  private static final String ID = "ID";

  /**
   * @param controller
   */
  DialogContainer(ExerciseController<?> controller) {
    super(controller, DIALOGS, 10);
  }

  @Override
  protected void addColumnsToTable() {
    List<T> list = getList();

    addID(list);
    addUnit(list, 10);
    addChapter(list, 10);
    int maxLengthId = getMaxLengthId();
    addItemID(list, maxLengthId);

    addEnglish(list, maxLengthId); //50
    addOrientation(list, maxLengthId);

    Column<T, SafeHtml> dateCol = addDateCol(list);

    addIsPublic();

    table.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(dateCol, true));
  }

  private void addID(List<T> list) {
    Column<T, SafeHtml> userCol = getItemIDColumn();
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader(ID));
    table.addColumnSortHandler(getUserSorter(userCol, list));
  }

  private Column<T, SafeHtml> getItemIDColumn() {
    Column<T, SafeHtml> column = new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getNoWrapContent("" + shell.getID());
      }
    };
    column.setSortable(true);

    return column;
  }
}
