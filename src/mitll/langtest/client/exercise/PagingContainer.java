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
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;
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
public class PagingContainer extends SimplePagingContainer<CommonShell> {
  private final Logger logger = Logger.getLogger("PagingContainer");

  private static final int MAX_LENGTH_ID = 17;
  private static final boolean DEBUG = false;
  private final Map<String, CommonShell> idToExercise = new HashMap<String, CommonShell>();
  private final boolean isRecorder;
  private ExerciseComparator sorter;
  private boolean english;

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
    english = controller.getLanguage().equals("English");
  }

  public void redraw() {
    table.redraw();
  }

  private CommonShell getByID(String id) {
    for (CommonShell t : getList()) {
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
  public void forgetExercise(CommonShell es) {
    List<CommonShell> list = getList();
    int before = getList().size();
    //System.out.println("PagingContainer.forgetExercise, before size = " + before + " : "+ es);

    if (!list.remove(es)) {
      if (!list.remove(getByID(es.getID()))) {
        logger.warning("forgetExercise couldn't remove " + es);
        for (CommonShell t : list) {
          System.out.println("\tnow has " + t.getID());
        }
      } else {
        idToExercise.remove(es.getID());
      }
    } else {
      if (list.size() == before - 1) {
        //System.out.println("\tPagingContainer : now has " + list.size()+ " items");
      } else {
        logger.warning("\tPagingContainer.forgetExercise : now has " + list.size() + " items vs " + before);
      }
      idToExercise.remove(es.getID());
    }
    redraw();
  }

  public void setUnaccountedForVertical(int v) {
    verticalUnaccountedFor = v;
  }

  public List<CommonShell> getExercises() {
    return getList();
  }

  public int getSize() {
    return getList().size();
  }

  public boolean isEmpty() {
    return getList().isEmpty();
  }

  public CommonShell getFirst() {
    return getAt(0);
  }

  public int getIndex(CommonShell t) {
    return getList().indexOf(t);
  }

  public CommonShell getAt(int i) {
    return getList().get(i);
  }

  public CommonShell getCurrentSelection() {
    return selectionModel.getSelectedObject();
  }

  protected void addColumnsToTable() {
    Column<CommonShell, SafeHtml> englishCol = getEnglishColumn();
    englishCol.setSortable(true);

    String language = controller.getLanguage();
    addColumn(englishCol, new TextHeader("English"));


    Column<CommonShell, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);

    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(flColumn, new TextHeader(headerForFL));


    List<CommonShell> dataList = getList();

    ColumnSortEvent.ListHandler<CommonShell> columnSortHandler = getEnglishSorter(englishCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

    ColumnSortEvent.ListHandler<CommonShell> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(englishCol);

    table.setWidth("100%", true);

    // Set the width of each column.
    table.setColumnWidth(englishCol, 50.0, Style.Unit.PCT);
    table.setColumnWidth(flColumn, 50.0, Style.Unit.PCT);
  }

  private ColumnSortEvent.ListHandler<CommonShell> getFLSorter(Column<CommonShell, SafeHtml> flColumn, List<CommonShell> dataList) {
    ColumnSortEvent.ListHandler<CommonShell> columnSortHandler2 = new ColumnSortEvent.ListHandler<CommonShell>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<CommonShell>() {
          public int compare(CommonShell o1, CommonShell o2) {
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

  private ColumnSortEvent.ListHandler<CommonShell> getEnglishSorter(Column<CommonShell, SafeHtml> englishCol, List<CommonShell> dataList) {
    ColumnSortEvent.ListHandler<CommonShell> columnSortHandler = new ColumnSortEvent.ListHandler<CommonShell>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<CommonShell>() {
          public int compare(CommonShell o1, CommonShell o2) {
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
    selectionModel = new SingleSelectionModel<CommonShell>();
    table.setSelectionModel(selectionModel);
  }

  /**
   * @return
   * @see #addColumnsToTable()
   */
  private Column<CommonShell, SafeHtml> getEnglishColumn() {
    return new Column<CommonShell, SafeHtml>(new MySafeHtmlCell(true)) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, CommonShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(CommonShell shell) {
        String columnText = shell.getEnglish();
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
    /*        if (controller.getAudioType().equals(Result.AUDIO_TYPE_RECORDER)) {
              isSet = recorded;
            }
*/
/*            if (isSet) {
              System.out.println(table.getParent().getParent().getElement().getId()+" shell " + shell.getID() + " state " + state + "/" + shell.getSecondState()+
                " defect " +isDefect +
                " fixed " + isFixed + " recorded " + recorded);
           }*/

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

  private Column<CommonShell, SafeHtml> getFLColumn() {
    return new Column<CommonShell, SafeHtml>(new MySafeHtmlCell(true)) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, CommonShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(CommonShell shell) {
        String toShow = shell.getForeignLanguage();
        if (english) {
          toShow = shell.getMeaning();
        //  logger.info("meaning for fl " + toShow);
        }
        String columnText = truncate(toShow);
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  protected void gotClickOnItem(final CommonShell e) {
  }

  @Override
  public void clear() {
    super.clear();
    idToExercise.clear();
  }

  public CommonShell byID(String id) {
    return idToExercise.get(id);
  }

  /**
   * @param exercise
   * @see mitll.langtest.client.list.PagingExerciseList#addExercise(CommonShell)
   */
  public void addExercise(CommonShell exercise) {
    idToExercise.put(exercise.getID(), exercise);
    getList().add(exercise);
    //System.out.println("data now has "+list.size() + " after adding " + exercise.getID());
  }

  /**
   * @param afterThisOne
   * @param exercise
   * @see mitll.langtest.client.list.PagingExerciseList#addExerciseAfter(CommonShell, CommonShell)
   */
  public void addExerciseAfter(CommonShell afterThisOne, CommonShell exercise) {
    //System.out.println("addExercise adding " + exercise);
    List<CommonShell> list = getList();
    int before = list.size();
    String id = exercise.getID();
    idToExercise.put(id, exercise);
    int i = list.indexOf(afterThisOne);
    list.add(i + 1, exercise);
    int after = list.size();
    // System.out.println("data now has "+ after + " after adding " + exercise.getID());
    if (before + 1 != after) logger.warning("didn't add " + exercise.getID());
  }

  public Set<String> getKeys() {
    return idToExercise.keySet();
  }

  private void markCurrent(CommonShell currentExercise) {
    if (currentExercise != null) {
      markCurrentExercise(currentExercise.getID());
    }
  }

  protected float adjustVerticalRatio(float ratio) {
    if (dataProvider != null && getList() != null && !getList().isEmpty()) {
      CommonShell toLoad = getList().get(0);

      if (toLoad.getID().length() > ID_LINE_WRAP_LENGTH) {
        ratio /= 2; // hack for long ids
      }
    }

    return ratio;
  }


  public void markCurrentExercise(String itemID) {
    if (getList() == null || getList().isEmpty()) return;

    CommonShell t = idToExercise.get(itemID);
    markCurrent(getList().indexOf(t), t);
  }

  private void markCurrent(int i, CommonShell itemToSelect) {
    if (DEBUG) System.out.println(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID());
    table.getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      System.out.println("marking " + i + " out of " + table.getRowCount() + " page start " + table.getPageStart() +
          " end " + pageEnd);
    }

    int pageNum = i / table.getPageSize();
    int newIndex = pageNum * table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
      if (DEBUG) System.out.println("new start of prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
        if (DEBUG) System.out.println("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
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
  public void onResize(CommonShell currentExercise) {
    //System.out.println("PagingContainer : onResize");

    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      table.setPageSize(numRows);
      table.redraw();
      markCurrent(currentExercise);
    }
  }

  private static class MySafeHtmlCell extends SafeHtmlCell {
    private final boolean consumeClicks;

    public MySafeHtmlCell(boolean consumeClicks) {
      this.consumeClicks = consumeClicks;
    }

    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<String>();
      if (consumeClicks) events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}
