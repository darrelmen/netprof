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

package mitll.langtest.client.project;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.DominoUpdateItem;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

class DominoUpdateResponseSimplePagingContainer extends MemoryItemContainer<DominoUpdateItem> {
  private static final String DOMINO_ID = "Domino ID";
  private static final String NETPROF_ID = "Netprof ID";
  private static final String ENGLISH = "English";
  private static final String VOCABULARY = "Vocabulary";
  private static final String MESSAGE = "Message";

  /**
   * @param controller
   * @param selectedUserKey
   * @see ResponseModal#getReportTab(String, Collection)
   */
   DominoUpdateResponseSimplePagingContainer(ExerciseController controller,
                                                   String selectedUserKey) {
    super(controller, selectedUserKey, DOMINO_ID, 10, 7);
  }

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<DominoUpdateItem> list = getList();
    addItemID(list, 15);
    addNP(list, 15);
    addExerciseID(list, 15);
    addIsContext(list, 10);
    addEnglish(list, 15);
    addFL(list, 40);
    addMessage(list, 40);
  }

  protected int getIdWidth() {
    return 60;
  }

  private void addNP(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, DominoUpdateItem::getNetprofID);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 60 + "px");
    addColumn(userCol, NETPROF_ID);
    table.addColumnSortHandler(getNPSorter(userCol, list));
  }

  private void addEnglish(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, DominoUpdateItem::getEnglish);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, ENGLISH);
    table.addColumnSortHandler(getEnglishSorter(userCol, list));
  }

  private void addFL(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, DominoUpdateItem::getForeignLanguage);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 200 + "px");
    addColumn(userCol, VOCABULARY);
    table.addColumnSortHandler(getFLorter(userCol, list));
  }

  private void addMessage(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, shell -> {
      List<String> changedFields = shell.getChangedFields();
      String s = changedFields.toString();
      return changedFields.isEmpty() ? "" : s.substring(1, s.length() - 2);
    });
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 200 + "px");
    addColumn(userCol, MESSAGE);
    table.addColumnSortHandler(getMessageSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getNPSorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                    List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, (o1, o2) -> o1.getNetprofID().compareToIgnoreCase(o2.getNetprofID()));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getEnglishSorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                         List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, (o1, o2) -> o1.getEnglish().compareToIgnoreCase(o2.getEnglish()));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getFLorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                   List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, (o1, o2) -> o1.getForeignLanguage().compareToIgnoreCase(o2.getForeignLanguage()));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getMessageSorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                         List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, (o1, o2) -> o1.getChangedFields().toString()
        .compareToIgnoreCase(o2.getChangedFields().toString()));
    return columnSortHandler;
  }


  private void addExerciseID(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, shell -> "" + shell.getExerciseID());
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 50 + "px");
    addColumn(userCol, "ID");
    table.addColumnSortHandler(getExSorter(userCol, list));
  }

  private void addIsContext(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getTruncatedCol(maxLength, shell -> shell.isContext() ? "Yes" : "No");
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 50 + "px");
    addColumn(userCol, "Context?");
    table.addColumnSortHandler(getExSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getExSorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                    List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparingInt(DominoUpdateItem::getExerciseID));
    return columnSortHandler;
  }

  protected int getIDCompare(DominoUpdateItem o1, DominoUpdateItem o2) {
    return Integer.compare(o1.getDominoID(), o2.getDominoID());
  }

  @Override
  protected int getDateCompare(DominoUpdateItem o1, DominoUpdateItem o2) {
    return 0;
  }

  @Override
  protected String getItemLabel(DominoUpdateItem shell) {
    return "" + shell.getDominoID();
  }

  @Override
  protected Long getItemDate(DominoUpdateItem shell) {
    return null;
  }
}
