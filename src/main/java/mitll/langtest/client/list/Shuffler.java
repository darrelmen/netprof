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

package mitll.langtest.client.list;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.RandomAccess;

class Shuffler {
  private static final int SHUFFLE_THRESHOLD = 5;

  /**
   *
   * @param list
   */
  static void shuffle(List<?> list) {
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
