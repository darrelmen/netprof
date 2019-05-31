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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.IPublicPrivate;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class ButtonMemoryItemContainer<T extends HasID & IPublicPrivate> extends MemoryItemContainer<T> {
  // private final Logger logger = Logger.getLogger("ButtonMemoryItemContainer");
  /**
   * @see #addIsPublic
   */
  protected static final String PUBLIC = "Public?";
  protected static final String NO = "No";
  protected static final String YES = "Yes";
  private final List<Button> buttons = new ArrayList<>();

  /**
   * @param controller
   * @param selectedUserKey
   * @param header
   * @param pageSize
   * @param shortPageSize
   * @see BasicUserContainer#BasicUserContainer
   */
  protected ButtonMemoryItemContainer(ExerciseController controller,
                                      String selectedUserKey,
                                      String header,
                                      int pageSize,
                                      int shortPageSize) {
    super(controller, selectedUserKey, header, pageSize, shortPageSize);
  }

  public void addButton(Button button) {
    buttons.add(button);
  }

  public void enableAll() {
    buttons.forEach(button -> button.setEnabled(true));
  }
  public void disableAll() {
    buttons.forEach(button -> button.setEnabled(false));
  }

  protected void addIsPublic() {
    Column<T, SafeHtml> diff = getPublic();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(PUBLIC));
    table.addColumnSortHandler(getPublicSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private Column<T, SafeHtml> getPublic() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(T shell) {
        return getSafeHtml(shell.isPrivate() ? NO : YES);
      }
    };
  }

  private ColumnSortEvent.ListHandler<T> getPublicSorted(Column<T, SafeHtml> englishCol, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(T::isPrivate));
    return columnSortHandler;
  }
}
