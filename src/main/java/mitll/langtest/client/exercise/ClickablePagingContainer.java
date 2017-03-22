/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.Shell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/5/16.
 */
public abstract class ClickablePagingContainer<T extends HasID> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("ClickablePagingContainer");

  private static final boolean DEBUG = false;
  private final Map<Integer, T> idToExercise = new HashMap<>();

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
   * @param es
   * @see ListInterface#simpleRemove(int)
   */
  //@Override
  public void forgetExercise(T es) {
    List<T> list = getList();
    int before = getList().size();

    if (!list.remove(es)) {
      if (!list.remove(getByID(es.getID()))) {
        logger.warning("forgetExercise couldn't remove " + es);
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

  public T getNext() {
    T currentSelection = getCurrentSelection();
    if (currentSelection != null) {
      int index = getIndex(currentSelection);
      return getAt((index == getSize() - 1) ? 0 : index + 1);
    } else {
      return null;
    }
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<T>();
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
   * @see ListInterface#addExercise(Shell)
   * @see mitll.langtest.client.list.PagingExerciseList#addExercise(CommonShell)
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
//    String id = exercise.getOldID();
    idToExercise.put(exercise.getID(), exercise);
    int i = list.indexOf(afterThisOne);
    list.add(i + 1, exercise);
    int after = list.size();
    // logger.info("data now has "+ after + " after adding " + exercise.getOldID());
    if (before + 1 != after) logger.warning("didn't add " + exercise.getID());
  }

  public Set<Integer> getKeys() {
    return idToExercise.keySet();
  }

  private void markCurrent(T currentExercise) {
    if (currentExercise != null) {
      markCurrentExercise(currentExercise.getID());
    }
  }

  public void markCurrentExercise(int itemID) {
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

    scrollToVisible(i);
    table.redraw();
  }

  public void clearSelection() {
    SelectionModel<? super T> selectionModel = table.getSelectionModel();
    T currentSelection = getCurrentSelection();
    if (currentSelection != null) {
      selectionModel.setSelected(currentSelection, false);
    }
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