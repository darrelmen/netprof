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

package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.*;
import java.util.logging.Logger;

public abstract class ClickablePagingContainer<T extends HasID> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("ClickablePagingContainer");

  private final Map<Integer, T> idToExercise = new HashMap<>();

  private static final boolean DEBUG = true;
  private static final boolean DEBUG_MARK_CURRENT = false;
  private static final boolean DEBUG_ADDING = false;

  public ClickablePagingContainer(ExerciseController controller) {
    super(controller);
  }

  public void redraw() {
    table.redraw();
  }

  private T getByID(int id) {
    for (T t : getList()) {
      if (t.getID() == id) {
        return t;
      }
    }
    return null;
  }

  /**
   * TODO : somehow put this behind an interface...?
   *
   * @param es
   * @see ListInterface#simpleRemove(int)
   */
  public void forgetItem(T es) {
    List<T> list = getList();
    int before = getList().size();

    if (!list.remove(es)) {
      T byID = getByID(es.getID());
      if (!list.remove(byID)) {
        logger.warning("forgetItem couldn't remove " + es);
//        for (T t : list) {
//          logger.info("\tnow has " + t.getOldID());
//        }
      } else {
        idToExercise.remove(es.getID());
      }
    } else {
      if (list.size() == before - 1) {
        //logger.info("\tPagingContainer : now has " + list.size()+ " items");
      } else {
        logger.warning("\tPagingContainer.forgetItem : now has " + list.size() + " items vs " + before);
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

  public List<T> getItems() {
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

  public Collection<Integer> getVisibleIDs() {
    return getIdsForRange(getVisibleRange());
  }

  /**
   * @param visibleRange
   * @return
   * @see FacetExerciseList#makePagingContainer
   */
  private Collection<Integer> getIdsForRange(Range visibleRange) {
    //  logger.info("getIdsForRange : range " + visibleRange);
    int start = visibleRange.getStart();
    int end = Math.min(getList().size(), start + visibleRange.getLength());

    List<Integer> visible = new ArrayList<>();
    for (int i = start; i < end; i++) {
      visible.add(getAt(i).getID());
    }
    //logger.info("ClickablePagingContainer.getIdsForRange : get from " + start + " to " + end + " vs " + getList().size()+ " : " + visible);
    return visible;
  }

  public T getAt(int i) {
    return getList().get(i);
  }

  public T getCurrentSelection() {
    return selectionModel.getSelectedObject();
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
  }

  abstract public void gotClickOnItem(final T e);

  @Override
  public void clear() {
    super.clear();
    idToExercise.clear();
  }

  public T byID(int id) {
    return idToExercise.get(id);
  }

  /**
   * @param exercise
   * @see ListInterface#addExercise
   * @see mitll.langtest.client.list.PagingExerciseList#addExercise(CommonShell)
   */
  public void addExercise(T exercise) {
    idToExercise.put(exercise.getID(), exercise);
    getList().add(exercise);
  }

  /**
   * @param afterThisOne null means put at the end
   * @param item
   * @see mitll.langtest.client.custom.userlist.ListView#madeIt
   */
  public void addItemAfter(T afterThisOne, T item) {
    if (DEBUG_ADDING) {
      logger.info("addItemAfter adding " + item + " current selection : " +getCurrentSelection());
    }
    List<T> list = getList();
    int before = list.size();

    idToExercise.put(item.getID(), item);
    int toUse = Math.max(-1, before - 1);

    {
      int i = afterThisOne == null ? toUse : list.indexOf(afterThisOne);
//    if (afterThisOne == null) {
//      list.add(0,)
//    }
      list.add(i + 1, item);
    }

    int after = list.size();

    if (DEBUG_ADDING) {
      logger.info("addItemAfter" +
          "\n\tbefore       " + before +
          "\n\tnow has      " + after +
          "\n\tafter adding " + item.getID() +
          "\n\tcurrent      " + getCurrentSelection()
      );
    }

    refresh();
    flush();

    markCurrent(item);

    if (before + 1 != after) logger.warning("addItemAfter didn't add " + item.getID());
  }

  private void markCurrent(T currentExercise) {
    if (currentExercise != null) {
      markCurrentExercise(currentExercise.getID());
    }
  }

  /**
   * @param itemID
   * @see mitll.langtest.client.list.PagingExerciseList#markCurrentExercise
   */
  public boolean markCurrentExercise(int itemID) {
    if (getList() == null || getList().isEmpty()) {
      return false;
    }

    if (idToExercise.isEmpty()) {
      getList().forEach(item -> idToExercise.put(item.getID(), item));
    }
    T found = idToExercise.get(itemID);
    if (DEBUG || DEBUG_MARK_CURRENT) {
      logger.info("markCurrentExercise for " + itemID + " in " + idToExercise.size() + " found " + found);
    }
    if (found == null) {
      return false;
    } else {
      boolean b = markCurrent(getIndex(found), found);
      if (DEBUG)  logger.info("markCurrentExercise : change visible range " + b);
      return b;
    }
  }

  private boolean markCurrent(int i, T itemToSelect) {
//    if (DEBUG) logger.info(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID() + " at " +i);
    getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG || DEBUG_MARK_CURRENT) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      logger.info("markCurrent " +
          "\n\tmarking    " + i +
          "\n\tout of     " + table.getRowCount() +
          "\n\tpage start " + table.getPageStart() +
          "\n\tend        " + pageEnd +
          "\n\titem       " + itemToSelect);
    }

    boolean b = scrollToVisible(i);
    table.redraw();
    return b;
  }

  private SelectionModel<? super T> getSelectionModel() {
    return table.getSelectionModel();
  }

  /**
   * @param currentExercise
   * @return true if did the resize
   * @see mitll.langtest.client.list.PagingExerciseList#onResize()
   */
  public boolean onResize(T currentExercise) {
    int numRows = getNumTableRowsGivenScreenHeight();
    //   logger.info("onResize size is " + numRows);
    if (table.getPageSize() != numRows) {
      if (DEBUG) logger.info("onResize size is " + numRows);// + " parent " + table.getParent());
      table.setPageSize(numRows);
      // table.redraw();
      markCurrent(currentExercise);
      return true;
    } else {
      return false;
    }
  }

  public void setPageSize(int pageSize) {
    logger.info("setPageSize: page size is " + pageSize);
    table.setPageSize(pageSize);
    table.redraw();
  }

  public Map<Integer, T> getIdToExercise() {
    return idToExercise;
  }

  public static class ClickableCell extends SafeHtmlCell {
    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<>();
      events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}