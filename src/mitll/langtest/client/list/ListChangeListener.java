/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.list;

import java.util.List;

/**
 * Created by go22670 on 2/11/14.
 */
public interface ListChangeListener<T> {
  /**
   * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   * @param items
   * @param selectionID
   */
  public void listChanged(List<T> items, String selectionID);
}
