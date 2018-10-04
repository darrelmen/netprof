package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.dialog.IDialogSession;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class SessionContainer<T extends IDialogSession> extends MemoryItemContainer<T> implements ReqCounter {
  private static final int FIRST_WIDTH = 90;

  private final DivWidget rightSide;
  private final DivWidget overallBottom;

  private int req = 0;

  @Override
  public int getReq() {
    return req;
  }

  private T lastSelected = null;

  /**
   * @param controller
   * @param rightSide
   * @param overallBottom
   * @see SessionAnalysis#getTable
   */
  SessionContainer(ExerciseController controller,
                   DivWidget rightSide,
                   DivWidget overallBottom) {
    super(controller, "dialogSession", "Session", 10, 10);
    this.rightSide = rightSide;
    this.overallBottom = overallBottom;
  }

  public void gotClickOnItem(final T selectedUser) {
    if (lastSelected != selectedUser) {
      //logger.info("gotClickOnItem " + selectedUser.getUserID());
      lastSelected = selectedUser;
      changeSelectedUser(selectedUser);
    }
  }

  private void changeSelectedUser(T selectedUser) {
    super.gotClickOnItem(selectedUser);

    Scheduler.get().scheduleDeferred(() -> {
      rightSide.clear();

      rightSide.add(new AnalysisTab(controller,
          overallBottom,
          selectedUser.getView().toString(),
          false,
          this,
          INavigation.VIEWS.STUDY,
          new AnalysisRequest()
              .setUserid(controller.getUser())
              .setMinRecordings(0)
              .setListid(-1)
              .setReqid(req++)
              .setDialogID(new SelectionState().getDialog())
              .setDialogSessionID(selectedUser.getID()), 850));
    });


  }

  /**
   * @see SimplePagingContainer#configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<T> list = getList();
    addItemID(list, 20);
    //addFirstName(list);
    addCurrent(list);

    table.getColumnSortList().push(addDateCol(list));

    table.setWidth("100%", true);
  }

  protected int getIdWidth() {
    return 45;
  }

  private void addCurrent(List<T> list) {
    Column<T, SafeHtml> current = getCurrent();
    addColumn(current, new TextHeader("Score"));
    table.setColumnWidth(current, 45 + "px");
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

/*
  private void addFirstName(List<T> list) {
    Column<T, SafeHtml> userCol = getClickable(shell -> shell.getView().toString());
    table.setColumnWidth(userCol, FIRST_WIDTH + "px");
    addColumn(userCol, new TextHeader("Phase"));
    table.addColumnSortHandler(getFirstSorter(userCol, list));
  }
*/

/*  private ColumnSortEvent.ListHandler<T> getFirstSorter(Column<T, SafeHtml> englishCol,
                                                        List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getFirstCompare);
    return columnSortHandler;
  }*/

/*
  private int getFirstCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getView().toString().compareTo(o2.getView().toString());

        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }
*/

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
