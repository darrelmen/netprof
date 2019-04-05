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

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;

import java.util.List;

public class DialogContainer<T extends IDialog> extends ButtonMemoryItemContainer<T> {
  DialogContainer(ExerciseController<?> controller) {
    super(controller, "netprof" + ":" + controller.getUser() + ":" + "dialogs", "Dialogs",
        20, 10);
  }

  @Override
  protected void addColumnsToTable() {
    List<T> list = getList();

    addUnit(list, 10);
    addChapter(list, 10);
    addItemID(list, getMaxLengthId());

    addEnglish(list, 50);
    addOrientation(list, 50);

    addDateCol(list);
  }

  /**
   * @param list
   * @param maxLength
   */
  private void addEnglish(List<T> list, int maxLength) {
    Column<T, SafeHtml> userCol = getEquivColumn(maxLength);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader("English"));
    table.addColumnSortHandler(getSorter(userCol, list));
  }

  private Column<T, SafeHtml> getEquivColumn(int maxLength) {
    return getTruncatedCol(maxLength, this::getEquivValue);
  }

  private ColumnSortEvent.ListHandler<T> getSorter(Column<T, SafeHtml> englishCol,
                                                   List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getEquivCompare);
    return columnSortHandler;
  }

  private int getEquivCompare(T o1, T o2) {
    int i = o1.getEnglish().compareTo(o2.getEnglish());
    return i == 0 ? o1.getForeignLanguage().compareTo(o2.getForeignLanguage()) : i;
  }

  private String getEquivValue(T thing) {
    return thing.getEnglish();
  }


  /**
   * @param list
   * @param maxLength
   */
  private void addOrientation(List<T> list, int maxLength) {
    Column<T, SafeHtml> userCol = getOrientColumn(maxLength);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader("Orientation"));
    table.addColumnSortHandler(getOrientSorter(userCol, list));
  }

  private Column<T, SafeHtml> getOrientColumn(int maxLength) {
    return getTruncatedCol(maxLength, this::getOrientValue);
  }

  private ColumnSortEvent.ListHandler<T> getOrientSorter(Column<T, SafeHtml> englishCol,
                                                         List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getOrientCompare);
    return columnSortHandler;
  }

  private int getOrientCompare(T o1, T o2) {
    int i = o1.getOrientation().compareTo(o2.getOrientation());
    return i == 0 ? o1.getForeignLanguage().compareTo(o2.getForeignLanguage()) : i;
  }

  private String getOrientValue(T thing) {
    return thing.getOrientation();
  }

  /**
   * @param list
   * @param maxLength
   */
  private void addUnit(List<T> list, int maxLength) {
    Column<T, SafeHtml> userCol = getUnitColumn(maxLength);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader("Unit"));
    table.addColumnSortHandler(getUnitSorter(userCol, list));
  }

  private Column<T, SafeHtml> getUnitColumn(int maxLength) {
    return getTruncatedCol(maxLength, this::getUnitValue);
  }

  private String getUnitValue(T thing) {
    return thing.getUnit();
  }

  private ColumnSortEvent.ListHandler<T> getUnitSorter(Column<T, SafeHtml> englishCol,
                                                       List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getUnitCompare);
    return columnSortHandler;
  }

  private int getUnitCompare(T o1, T o2) {
    int i = o1.getUnit().compareTo(o2.getUnit());
    return i == 0 ? getChapterCompare(o1, o2) : i;
  }


  /**
   * @param list
   * @param maxLength
   */
  private void addChapter(List<T> list, int maxLength) {
    Column<T, SafeHtml> userCol = getChapterColumn(maxLength);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, new TextHeader("Chapter"));
    table.addColumnSortHandler(getChapterSorter(userCol, list));
  }

  private Column<T, SafeHtml> getChapterColumn(int maxLength) {
    return getTruncatedCol(maxLength, this::getChapterValue);
  }

  private String getChapterValue(T thing) {
    return thing.getChapter();
  }

  private ColumnSortEvent.ListHandler<T> getChapterSorter(Column<T, SafeHtml> englishCol,
                                                          List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getChapterCompare);
    return columnSortHandler;
  }

  private int getChapterCompare(T o1, T o2) {
    int i = o1.getChapter().compareTo(o2.getChapter());
    return i == 0 ? o1.getForeignLanguage().compareTo(o2.getForeignLanguage()) : i;
  }


  protected String getDateColHeader() {
    return "Modified";
  }

  protected int getMaxLengthId() {
    return 100;
  }

  @Override
  protected int getIDCompare(IDialog o1, IDialog o2) {
    return Integer.compare(o1.getID(), o2.getID());
  }

  @Override
  protected int getDateCompare(IDialog o1, IDialog o2) {
    return Long.compare(o1.getModified(), o2.getModified());
  }

  @Override
  protected String getItemLabel(IDialog shell) {
    return shell.getForeignLanguage();
  }

  @Override
  protected Long getItemDate(IDialog shell) {
    return shell.getModified();
  }
}
