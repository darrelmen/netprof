/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.bootstrap;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/6/13
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ItemSorter {
  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList
   * @param sections
   * @return
   */
  public List<String> getSortedItems(Collection<String> sections) {
    List<String> items = new ArrayList<String>(sections);
    boolean isInt = true;
    for (String item : items) {
      try {
        Integer.parseInt(item);
      } catch (NumberFormatException e) {
        isInt = false;
      }
    }
    if (isInt) {
      Collections.sort(items, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = Integer.parseInt(o1);
          int second = Integer.parseInt(o2);
          return first < second ? -1 : first > second ? +1 : 0;
        }
      });
    }
    else {
      sortWithCompoundKeys(items);
    }
    return items;
  }

  /**
   * @see #getSortedItems(java.util.Collection)
   * @param items
   */
  private void sortWithCompoundKeys(List<String> items) {
    Collections.sort(items, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        boolean firstHasSep = o1.contains("-");
        boolean secondHasSep = o2.contains("-");
        String left1 = o1;
        String left2 = o2;
        String right1 = "";
        String right2 = "";

        if (firstHasSep) {
          String[] first = o1.split("-");
          left1 = first[0];
          if (first.length == 1) {
            System.err.println("huh? couldn't split " + o1);
            right1 = "";
          }
          else {
            right1 = first[1];
          }
        } else if (o1.contains(" ")) {
          firstHasSep = true;
          String[] first = o1.split("\\s");
          left1 = first[0];
          right1 = first[1];
        }

        if (secondHasSep) {
          String[] second = o2.split("-");
          left2 = second[0];
          right2 = second[1];
        } else if (o2.contains(" ")) {
          secondHasSep = true;
          String[] second = o2.split("\\s");
          left2 = second[0];
          right2 = second[1];
        }

        if (firstHasSep || secondHasSep) {
          int leftCompare = getIntCompare(left1, left2);
          if (leftCompare != 0) {
            return leftCompare;
          } else {
            return getIntCompare(right1, right2);
          }
        } else {
          return getIntCompare(o1, o2);
        }
      }
    });
  }

  private int getIntCompare(String first, String second) {
    if (first.length() > 0 && !Character.isDigit(first.charAt(0))) {
      return first.compareToIgnoreCase(second);
    } else {
      try {
        int r1 = Integer.parseInt(first);
        int r2 = Integer.parseInt(second);
        return r1 < r2 ? -1 : r1 > r2 ? +1 : 0;
      } catch (NumberFormatException e) {
        return first.compareToIgnoreCase(second);
      }
    }
  }
}
