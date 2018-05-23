package mitll.langtest.client.project;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.exercise.DominoUpdateItem;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

class DominoUpdateResponseSimplePagingContainer extends MemoryItemContainer<DominoUpdateItem> {
  /**
   * @param controller
   * @param selectedUserKey
   * @see ResponseModal#getUnmatchedRows2
   */
  public DominoUpdateResponseSimplePagingContainer(ExerciseController controller,
                                                   String selectedUserKey) {
    super(controller, selectedUserKey, "Domino ID", 10, 7);
  }

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<DominoUpdateItem> list = getList();
    addItemID(list, 15);
    addNP(list, 15);
    addExerciseID(list, 15);
    addEnglish(list, 15);
    addFL(list, 40);
    addMessage(list, 15);
  }

  private void addNP(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getNPColumn(maxLength, DominoUpdateItem::getNetprofID);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, "Netprof ID");
    table.addColumnSortHandler(getNPSorter(userCol, list));
  }

  private void addEnglish(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getNPColumn(maxLength, DominoUpdateItem::getEnglish);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, "English");
    table.addColumnSortHandler(getEnglishSorter(userCol, list));
  }

  private void addFL(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getNPColumn(maxLength, DominoUpdateItem::getForeignLanguage);
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 200 + "px");
    addColumn(userCol, "Vocabulary");
    table.addColumnSortHandler(getFLorter(userCol, list));
  }

  private void addMessage(List<DominoUpdateItem> list, int maxLength) {
    Column<DominoUpdateItem, SafeHtml> userCol = getNPColumn(maxLength, shell -> shell.getChangedFields().isEmpty() ? "" : shell.getChangedFields().toString());
    userCol.setSortable(true);
    table.setColumnWidth(userCol, 100 + "px");
    addColumn(userCol, "Message");
    table.addColumnSortHandler(getMessageSorter(userCol, list));
  }

  private Column<DominoUpdateItem, SafeHtml> getNPColumn(int maxLength, GetSafe getSafe) {
    return new Column<DominoUpdateItem, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, DominoUpdateItem object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(DominoUpdateItem shell) {
        return getNoWrapContent(truncate(getSafe.getSafe(shell), maxLength));
      }
    };
  }

  interface GetSafe {
    String getSafe(DominoUpdateItem shell);
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
    Column<DominoUpdateItem, SafeHtml> userCol = getNPColumn(maxLength, shell -> "" + shell.getId());
    userCol.setSortable(true);
    table.setColumnWidth(userCol, getIdWidth() + "px");
    addColumn(userCol, "Exercise ID");
    table.addColumnSortHandler(getExSorter(userCol, list));
  }

/*
  private String getS(DominoUpdateItem i) {
    return "" + i.getId();
  }
*/

  private ColumnSortEvent.ListHandler<DominoUpdateItem> getExSorter(Column<DominoUpdateItem, SafeHtml> englishCol,
                                                                    List<DominoUpdateItem> dataList) {
    ColumnSortEvent.ListHandler<DominoUpdateItem> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparingInt(DominoUpdateItem::getId));
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
