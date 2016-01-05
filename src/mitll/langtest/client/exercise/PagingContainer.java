/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingContainer<T extends Shell> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("PagingContainer");

  private static final int MAX_LENGTH_ID = 17;
  private static final boolean DEBUG = false;
  private final Map<String, T> idToExercise = new HashMap<>();
  private final boolean isRecorder;
  private final ExerciseComparator sorter;

  /**
   * @param controller
   * @param verticalUnaccountedFor
   * @param isRecorder
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
   */
  public PagingContainer(ExerciseController controller, int verticalUnaccountedFor, boolean isRecorder) {
    super(controller);
    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
    this.verticalUnaccountedFor = verticalUnaccountedFor;
    this.isRecorder = isRecorder;
  }

  public void redraw() {
    table.redraw();
  }

  private T getByID(String id) {
    for (T t : getList()) {
      if (t.getID().equals(id)) {
        return t;
      }
    }
    return null;
  }

  /**
   * @param es
   * @see mitll.langtest.client.list.PagingExerciseList#simpleRemove(String)
   */
  public void forgetExercise(T es) {
    List<T> list = getList();
    int before = getList().size();

    if (!list.remove(es)) {
      if (!list.remove(getByID(es.getID()))) {
        logger.warning("forgetExercise couldn't remove " + es);
//        for (T t : list) {
//          logger.info("\tnow has " + t.getID());
//        }
      } else {
        idToExercise.remove(es.getID());
      }
    } else {
      if (list.size() == before - 1) {
        //logger.info("\tPagingContainer : now has " + list.size()+ " items");
      } else {
        logger.warning("\tPagingContainer.forgetExercise : now has " + list.size() + " items vs " + before);
      }
      idToExercise.remove(es.getID());
    }
    redraw();
  }

  /**
   * @param v
   * @see mitll.langtest.client.list.PagingExerciseList#setUnaccountedForVertical(int)
   */
  public void setUnaccountedForVertical(int v) {
    verticalUnaccountedFor = v;
  }

  public List<T> getExercises() {
    return getList();
  }

  public int getSize() {
    return getList().size();
  }

  public boolean isEmpty() {
    return getList().isEmpty();
  }

  public T getFirst() {
    return getAt(0);
  }

  public int getIndex(T t) {
    return getList().indexOf(t);
  }

  public T getAt(int i) {
    return getList().get(i);
  }

  public T getCurrentSelection() {
    return selectionModel.getSelectedObject();
  }

  protected void addColumnsToTable() {
    Column<T, SafeHtml> englishCol = getEnglishColumn();
    englishCol.setSortable(true);

    addColumn(englishCol, new TextHeader(ENGLISH));

    Column<T, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);

    String language = controller.getLanguage();
    String headerForFL = language.equals(ENGLISH) ? "Meaning" : language;
    addColumn(flColumn, new TextHeader(headerForFL));

    List<T> dataList = getList();

    ColumnSortEvent.ListHandler<T> columnSortHandler = getEnglishSorter(englishCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

    ColumnSortEvent.ListHandler<T> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(englishCol);

    table.setWidth("100%", true);

    // Set the width of each column.
    table.setColumnWidth(englishCol, 50.0, Style.Unit.PCT);
    table.setColumnWidth(flColumn,   50.0, Style.Unit.PCT);
  }

  private ColumnSortEvent.ListHandler<T> getFLSorter(Column<T, SafeHtml> flColumn, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler2 = new ColumnSortEvent.ListHandler<T>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                String id1 = o1.getForeignLanguage();
                String id2 = o2.getForeignLanguage();
                return id1.toLowerCase().compareTo(id2.toLowerCase());
              }
            }
            return -1;
          }
        });
    return columnSortHandler2;
  }

  private ColumnSortEvent.ListHandler<T> getEnglishSorter(Column<T, SafeHtml> englishCol, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return sorter.simpleCompare(o1, o2, isRecorder);
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<T>();
    table.setSelectionModel(selectionModel);
  }

  /**
   * @return
   * @see #addColumnsToTable()
   */
  private Column<T, SafeHtml> getEnglishColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        String columnText = getEnglishText(shell);
        if (!controller.showCompleted()) {
          return getColumnToolTip(columnText);
        } else {
          String html = shell.getID();
          if (columnText != null) {
            columnText = truncate(columnText);
            STATE state = shell.getState();

            boolean isDefect = state == STATE.DEFECT;
            boolean isFixed = state == STATE.FIXED;
            boolean isLL = shell.getSecondState() == STATE.ATTN_LL;
            boolean isRerecord = shell.getSecondState() == STATE.RECORDED;

            boolean hasSecondState = isLL || isRerecord;
            boolean recorded = state == STATE.RECORDED;
            boolean approved = state == STATE.APPROVED || recorded;

            boolean isSet = isDefect || isFixed || approved;
            String icon =
                approved ? "icon-check" :
                    isDefect ? "icon-bug" :
                        isFixed ? "icon-thumbs-up" :
                            "";

            html = (isSet ?
                "<i " +
                    (isDefect ? "style='color:red'" :
                        isFixed ? "style='color:green'" :
                            "") +
                    " class='" +
                    icon +
                    "'></i>" +

                    "&nbsp;" : "") + columnText + (hasSecondState ?
                "&nbsp;<i " +
                    (isLL ? "style='color:gold'" : "") +
                    " class='" +
                    (isLL ? "icon-warning-sign" : "icon-microphone") +
                    "'></i>" : "");

          }
          return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
        }
      }

      private SafeHtml getColumnToolTip(String columnText) {
        columnText = truncate(columnText);
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  private String truncate(String columnText) {
    if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
    return columnText;
  }

  /**
   * @return
   * @see #addColumnsToTable()
   */
  private Column<T, SafeHtml> getFLColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        String columnText = truncate(getFLText(shell));
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  protected void gotClickOnItem(final T e) {
  }

  @Override
  public void clear() {
    super.clear();
    idToExercise.clear();
  }

  public T byID(String id) {
    return idToExercise.get(id);
  }

  /**
   * @param exercise
   * @see ListInterface#addExercise(mitll.langtest.shared.exercise.Shell)
   */
  public void addExercise(T exercise) {
    idToExercise.put(exercise.getID(), exercise);
    getList().add(exercise);
  }

  /**
   * @param afterThisOne
   * @param exercise
   * @see mitll.langtest.client.list.PagingExerciseList#addExerciseAfter(T, T)
   */
  public void addExerciseAfter(T afterThisOne, T exercise) {
    //logger.info("addExercise adding " + exercise);
    List<T> list = getList();
    int before = list.size();
    String id = exercise.getID();
    idToExercise.put(id, exercise);
    int i = list.indexOf(afterThisOne);
    list.add(i + 1, exercise);
    int after = list.size();
    // logger.info("data now has "+ after + " after adding " + exercise.getID());
    if (before + 1 != after) logger.warning("didn't add " + exercise.getID());
  }

  public Set<String> getKeys() {
    return idToExercise.keySet();
  }

  private void markCurrent(T currentExercise) {
    if (currentExercise != null) {
      markCurrentExercise(currentExercise.getID());
    }
  }

  protected float adjustVerticalRatio(float ratio) {
    if (dataProvider != null && getList() != null && !getList().isEmpty()) {
      T toLoad = getList().get(0);

      if (toLoad.getID().length() > ID_LINE_WRAP_LENGTH) {
        ratio /= 2; // hack for long ids
      }
    }

    return ratio;
  }

  public void markCurrentExercise(String itemID) {
    if (getList() == null || getList().isEmpty()) return;

    T t = idToExercise.get(itemID);
    markCurrent(getList().indexOf(t), t);
  }

  private void markCurrent(int i, T itemToSelect) {
    if (DEBUG) logger.info(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID());
    table.getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      logger.info("marking " + i + " out of " + table.getRowCount() + " page start " + table.getPageStart() +
          " end " + pageEnd);
    }

    int pageNum = i / table.getPageSize();
    int newIndex = pageNum * table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
      if (DEBUG) logger.info("new start of prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
        if (DEBUG) logger.info("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
            " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, table.getPageSize());
      }
    }
    table.redraw();
  }

  /**
   * @param currentExercise
   * @see mitll.langtest.client.list.PagingExerciseList#onResize()
   */
  public void onResize(T currentExercise) {
    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      table.setPageSize(numRows);
      table.redraw();
      markCurrent(currentExercise);
    }
  }

  public static class ClickableCell extends SafeHtmlCell {
    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<String>();
      events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}
