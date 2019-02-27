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

  /**
   * @param table
   * @return
   * @see mitll.langtest.client.analysis.WordContainerAsync#createProvider
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
      String s1 = columnSortInfo.isAscending() ? ASC : DESC;
   //   logger.info("col " + columnSortInfo + " s " + s + " = " + s1 );
      builder.append(s + "_" + s1 + ",");
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
