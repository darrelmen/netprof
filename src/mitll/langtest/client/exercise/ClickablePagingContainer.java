package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.Shell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/5/16.
 */
public class ClickablePagingContainer<T extends Shell> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("ClickablePagingContainer");

  private static final boolean DEBUG = false;
  private final Map<String, T> idToExercise = new HashMap<>();

  public ClickablePagingContainer(ExerciseController controller) {
    super(controller);
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

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<T>();
    table.setSelectionModel(selectionModel);
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
   * @see ListInterface#addExercise(Shell)
   */
  public void addExercise(T exercise) {
    idToExercise.put(exercise.getID(), exercise);
    getList().add(exercise);
  }

  /**
   * @param afterThisOne
   * @param exercise
   * @see mitll.langtest.client.list.PagingExerciseList#addExerciseAfter
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
