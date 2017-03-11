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

package mitll.langtest.client.bootstrap;

import mitll.langtest.shared.exercise.SectionNode;

import java.util.*;

public class SectionNodeItemSorter {

  public List<String> getSorted(Set<String> keys) {
    List<String> items = new ArrayList<>(keys);
    boolean isInt = true;
    for (String item : items) {
      if (!isInt(item)) {
        isInt = false;
        break;
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
    } else {
      Collections.sort(items, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return compareTwo(o1, o2);
        }
      });
    }
    return items;
  }

  /**
   * @param sections
   * @return
   * @see FlexSectionExerciseList
   */
  public List<SectionNode> getSortedItems(Collection<SectionNode> sections) {
    List<SectionNode> items = new ArrayList<>(sections);
    boolean isInt = true;
    for (SectionNode item : items) {
      String name = item.getName();
      if (!isInt(name)) {
        isInt = false;
        break;
      }
    }
    if (isInt) {
      Collections.sort(items, new Comparator<SectionNode>() {
        @Override
        public int compare(SectionNode o1, SectionNode o2) {
          int first = Integer.parseInt(o1.getName());
          int second = Integer.parseInt(o2.getName());
          return first < second ? -1 : first > second ? +1 : 0;
        }
      });
    } else {
      sortWithCompoundKeys(items);
    }
    return items;
  }

  private boolean isInt(String name) {
    try {
      Integer.parseInt(name);

      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * @param items
   * @see #getSortedItems(Collection)
   */
  private void sortWithCompoundKeys(List<SectionNode> items) {
    Collections.sort(items, new Comparator<SectionNode>() {
      @Override
      public int compare(SectionNode sectionNode1, SectionNode sectionNode2) {
        String o1 = sectionNode1.getName();
        String o2 = sectionNode2.getName();
        return SectionNodeItemSorter.this.compareTwo(o1, o2);
      }
    });
  }

  private int compareTwo(String o1, String o2) {
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
      } else {
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
