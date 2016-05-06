/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.list;

import java.util.Collection;

/**
 * Created by go22670 on 2/11/14.
 */
public interface ListChangeListener<T> {
  /**
   * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst
   * @param items
   * @param selectionID
   */
  void listChanged(Collection<T> items, String selectionID);
}
