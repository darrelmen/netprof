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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.dialog.IDialogSession;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class SessionContainer<T extends IDialogSession>
    extends MemoryItemContainer<T> implements ReqCounter {
  private final Logger logger = Logger.getLogger("SessionContainer");

  private static final String SCORE = "Score";
  private static final int ITEM_COLUMN_WIDTH = 450;

  private final DivWidget rightSide;
  private final DivWidget overallBottom;

  private int req = 0;
  private int user = -1;

  @Override
  public int getReq() {
    return req;
  }

  private T lastSelected = null;

  /**
   * @param controller
   * @param overallBottom
   * @param rightSide
   * @see TwoColumnAnalysis#getTable
   */
  SessionContainer(ExerciseController controller,
                   DivWidget overallBottom,
                   DivWidget rightSide,
                   int user) {
    super(controller, "dialogSession", "Session", 10, 10);
    this.overallBottom = overallBottom;
    this.rightSide = rightSide;
    this.user = user;
  }

  @NotNull
  @Override
  protected ListOptions getListOptions() {
    return super.getListOptions().setCompact(true);
  }

  @Override
  protected void makeInitialSelectionFromSet(Collection<T> users, T userToSelect) {
    if (!users.isEmpty()) {
      T current = null;
      for (T user : users) {
        current = user;
      }
      //   logger.info("makeInitialSelectionFromSet " + current);
      makeInitialSelection(current, current);
    }
  }

  public void gotClickOnItem(final T selectedUser) {
    if (lastSelected != selectedUser) {
      //   logger.info("gotClickOnItem " + selectedUser.getView());
      lastSelected = selectedUser;
      changeSelectedUser(selectedUser);
    }
  }

  /**
   * Change the session the right side changes.
   * @param selectedItem
   */
  private void changeSelectedUser(T selectedItem) {
    //   logger.info("changeSelectedUser " + selectedUser);
    super.gotClickOnItem(selectedItem);

    Scheduler.get().scheduleDeferred(() -> {
      rightSide.clear();
      rightSide.add(getAnalysisTab(selectedItem, user));
    });
  }

  @NotNull
  private AnalysisTab getAnalysisTab(T selectedUser, int user) {
   // logger.info("getAnalysisTab " + selectedUser + " " + user);
    return new DialogSessionAnalysisTab<T>(this.controller,
        selectedUser,
        overallBottom,
        this,
        req++,
        user
    ).setItemColumnWidth(ITEM_COLUMN_WIDTH);
  }

  /**
   * @see SimplePagingContainer#configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<T> list = getList();
    addItemID(list, 20);
    addCurrent(list);

    table.getColumnSortList().push(addDateCol(list));
    //table.setWidth("100%", true);
  }

  protected int getIdWidth() {
    return 35;
  }

  protected int getDateWidth() {
    return 70;
  }

  private void addCurrent(List<T> list) {
    Column<T, SafeHtml> current = getCurrent();
    addColumn(current, new TextHeader(SCORE));
    table.setColumnWidth(current, 25 + "px");
    table.addColumnSortHandler(getCurrentSorter(current, list));
  }

  private Column<T, SafeHtml> getCurrent() {
    return getClickable(this::getCurrentText);
  }

  @NotNull
  private String getCurrentText(T shell) {
    return "" + Math.round(shell.getScore() * 100);
  }

  private ColumnSortEvent.ListHandler<T> getCurrentSorter(Column<T, SafeHtml> englishCol,
                                                          List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              return Float.compare(o1.getScore(), o2.getScore());
            }
          }
          return -1;
        });
    return columnSortHandler;
  }


  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d h:mm:ss a");

  @Override
  protected String getFormattedDateString(Long itemDate) {
    return format.format(new Date(itemDate));
  }

  @Override
  protected int getIDCompare(IDialogSession o1, IDialogSession o2) {
    return Integer.compare(o1.getID(), o2.getID());
  }

  @Override
  protected int getDateCompare(IDialogSession o1, IDialogSession o2) {
    return Long.compare(o1.getModified(), o2.getModified());
  }

  @Override
  protected String getItemLabel(IDialogSession shell) {
    return shell.getView().toString();
  }

  @Override
  protected Long getItemDate(IDialogSession shell) {
    return shell.getModified();
  }
}
