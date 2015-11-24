/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.list;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.RandomAccess;

/**
 * Created by GO22670 on 6/10/2014.
 */
public class Shuffler {
  private static final int SHUFFLE_THRESHOLD = 5;

  public static void shuffle(List<?> list) {
    Random rnd = r;
    if (rnd == null)
      r = rnd = new Random(); // harmless race.
    shuffle(list, rnd);
  }

  private static Random r;

  private static void shuffle(List<?> list, Random rnd) {
    int size = list.size();
    if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
      for (int i = size; i > 1; i--)
        swap(list, i - 1, rnd.nextInt(i));
    } else {
      Object arr[] = list.toArray();

      // Shuffle array
      for (int i = size; i > 1; i--)
        swap(arr, i - 1, rnd.nextInt(i));

      // Dump array back into list
      ListIterator it = list.listIterator();
      for (Object anArr : arr) {
        it.next();
        it.set(anArr);
      }
    }
  }

  /**
   * Swaps the two specified elements in the specified array.
   */
  private static void swap(Object[] arr, int i, int j) {
    Object tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }

  /**
   * Swaps the elements at the specified positions in the specified list.
   * (If the specified positions are equal, invoking this method leaves
   * the list unchanged.)
   *
   * @param list The list in which to swap elements.
   * @param i    the index of one element to be swapped.
   * @param j    the index of the other element to be swapped.
   * @throws IndexOutOfBoundsException if either <tt>i</tt> or <tt>j</tt>
   *                                   is out of range (i &lt; 0 || i &gt;= list.size()
   *                                   || j &lt; 0 || j &gt;= list.size()).
   * @since 1.4
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void swap(List<?> list, int i, int j) {
    // instead of using a raw type here, it's possible to capture
    // the wildcard but it will require a call to a supplementary
    // private method
    final List l = list;
    l.set(i, l.set(j, l.get(i)));
  }
}
