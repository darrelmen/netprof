package mitll.langtest.client.result;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TableSortHelper {

  private final Logger logger = Logger.getLogger("TableSortHelper");

  public static final String TIMESTAMP = "timestamp";

  private static final String DESC = "DESC";
  private static final String ASC = "ASC";


  private final Map<Column<?, ?>, String> colToField = new HashMap<>();
//
//  public TableSortHelper(Map<Column<?, ?>, String> colToField) {
//    this.colToField = colToField;
//  }

  /**
   * @param table
   * @return
   * @see #createProvider
   */
  public StringBuilder getColumnSortedState(CellTable<?> table) {
    final ColumnSortList sortList = table.getColumnSortList();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < sortList.size(); i++) {
      ColumnSortList.ColumnSortInfo columnSortInfo = sortList.get(i);
      Column<?, ?> column = columnSortInfo.getColumn();
      String s = colToField.get(column);
      if (s == null) {
        logger.warning("Can't find column " + column + "?");
      }
      builder.append(s + "_" + (columnSortInfo.isAscending() ? ASC : DESC) + ",");
    }
    if (!builder.toString().contains(TIMESTAMP)) {
      builder.append(TIMESTAMP + "_" + DESC);
    }
    return builder;
  }

  public Column<?, ?> getColumn(String name) {
    for (Map.Entry<Column<?, ?>, String> pair : colToField.entrySet()) {
      if (pair.getValue().equals(name)) {
        return pair.getKey();
      }
    }
    return null;
  }


  public void rememberColumn(Column<?,?> userid, String userid2) {
    colToField.put(userid, userid2);
  }
}
