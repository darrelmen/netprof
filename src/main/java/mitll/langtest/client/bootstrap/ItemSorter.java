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

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/6/13
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ItemSorter implements Comparator<String>, IsSerializable {
  private static final String PREFIX = ">";

  public ItemSorter() {
  }

  /**
   * @param sections
   * @return
   * @see mitll.langtest.client.bootstrap
   */
  public List<String> getSortedItems(Collection<String> sections) {
    List<String> items = new ArrayList<>(sections);

    if (areAllInt(items)) {
      items.sort(this::compareInts);
    } else {
      sortWithCompoundKeys(items);
    }

    return items;
  }

  private boolean areAllInt(List<String> items) {
    boolean isInt = true;
    for (String item : items) {
      isInt = checkIsInt(dropGreater(item));
      if (!isInt) break;
    }
    return isInt;
  }

  @Override
  public int compare(String o1, String o2) {
    try {
      if (o1.length() > 0 && o2.length() > 0) {
        if (checkIsInt(o1) && checkIsInt(o2)) {
          return compareInts(o1, o2);
        } else {
          return compoundCompare(o1, o2);
        }
      } else {
        return compoundCompare(o1, o2);
      }
    } catch (Exception e) {
      return compoundCompare(o1, o2);
    }
  }

  private int compoundCompare(String o1, String o2) {
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
//        System.err.println("huh? couldn't split " + o1);
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

  private int compareInts(String o1, String o2) {
    int first = Integer.parseInt(dropGreater(o1));
    int second = Integer.parseInt(dropGreater(o2));
    return Integer.compare(first, second);
  }

  private String dropGreater(String item) {
    if (item.startsWith(PREFIX)) item = item.substring(1, item.length());
    return item;
  }

  /**
   * @param items
   * @see #getSortedItems(java.util.Collection)
   */
  private void sortWithCompoundKeys(List<String> items) {
    items.sort(this::compoundCompare);
  }

  private int getIntCompare(String first, String second) {
    if (first.length() > 0 && !Character.isDigit(first.charAt(0))) {
      return first.compareToIgnoreCase(second);
    } else {
      try {
        if (checkIsInt(first) && checkIsInt(second)) {
          int r1 = Integer.parseInt(first);
          int r2 = Integer.parseInt(second);
          return Integer.compare(r1, r2);
        }
        else {
          return first.compareToIgnoreCase(second);
        }
      } catch (NumberFormatException e) {
        return first.compareToIgnoreCase(second);
      }
    }
  }

  private boolean checkIsInt(String o1) {
    boolean isInt = true;
    for (int i = 0; i < o1.length() && isInt; i++) {
      if (!Character.isDigit(o1.charAt(i))) {
        isInt = false;
      }
    }
    return isInt;
  }
}
