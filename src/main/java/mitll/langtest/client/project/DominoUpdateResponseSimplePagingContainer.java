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
