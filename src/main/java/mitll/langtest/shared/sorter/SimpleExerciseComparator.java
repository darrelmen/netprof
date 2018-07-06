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

package mitll.langtest.shared.sorter;

import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import org.jetbrains.annotations.NotNull;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/9/15.
 */
public class SimpleExerciseComparator implements IExerciseComparator {
  private static final String A_SPACE = "a ";

  public int simpleCompare(CommonShell o1, CommonShell o2, boolean recordedLast, boolean sortByFL, String searchTerm) {
    return sortByFL ? compareByFL(o1, o2, searchTerm) : compareByEnglish(o1, o2, searchTerm);
  }

  protected <T extends CommonShell> int compareByEnglish(T o1, T o2, String searchTerm) {
    String english1 = o1.getEnglish();
    String english2 = o2.getEnglish();
    if (english1.isEmpty() && !english2.isEmpty()) {
      return -1;
    } else if (!english1.isEmpty() && english2.isEmpty()) {
      return +1;
    } else if (english1.isEmpty()) {
      return compareByFL(o1, o2, searchTerm);
    } else if (!searchTerm.isEmpty() && english1.equalsIgnoreCase(searchTerm) && !english2.equalsIgnoreCase(searchTerm)) {
      return -1;
    } else if (!searchTerm.isEmpty() && !english1.equalsIgnoreCase(searchTerm) && english2.equalsIgnoreCase(searchTerm)) {
      return +1;
    } else {
      int i = compareStrings(english1, english2);
      if (i == 0) {
        i = compareByFL(o1, o2, searchTerm);
      }
      return i;
    }
  }

  /**
   * Skip prefix of "a ".
   *
   * @param id1
   * @param id2
   * @return
   */
  public int compareStrings(String id1, String id2) {
    String t = id1.toLowerCase();
    if (ignoreFirst(t)) t = t.substring(2);
    t = dropPunct(t);

    String t1 = id2.toLowerCase();
    if (ignoreFirst(t1)) t1 = t1.substring(2);
    t1 = dropPunct(t1);

    return removePunct(t).compareTo(removePunct(t1));
  }

  private boolean ignoreFirst(String t) {
    return t.startsWith(A_SPACE);
  }

  protected String removePunct(String t) {
    return t.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "");
  }

  protected  <T extends CommonShell> int compareByFL(T o1, T o2, String searchTerm) {
    String fl1 = o1.getForeignLanguage();
    String fl2 = o2.getForeignLanguage();
    if (fl1.equalsIgnoreCase(searchTerm) && !fl2.equalsIgnoreCase(searchTerm)) {
      return -1;
    } else if (!fl1.equalsIgnoreCase(searchTerm) && fl2.equalsIgnoreCase(searchTerm)) {
      return +1;
    } else {
      return dropPunct(fl1).compareTo(dropPunct(fl2));
    }
  }

  @NotNull
  private String dropPunct(String t) {
    if (t.isEmpty()) {
      return t;
    } else {
      return (t.startsWith("\"") || t.startsWith("\\'") || t.startsWith("-") || t.startsWith("\\u007E")) ? t.substring(1) : t;
    }
  }
}