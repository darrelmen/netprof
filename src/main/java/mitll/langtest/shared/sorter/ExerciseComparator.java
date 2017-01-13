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

import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import org.jetbrains.annotations.NotNull;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/9/15.
 */
public class ExerciseComparator {
//  protected final Collection<String> typeOrder;
//  public ExerciseComparator(Collection<String> typeOrder) { this.typeOrder = typeOrder; }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
  }

  /**
   *
   * @param o1
   * @param o2
   * @param recordedLast
   * @return
   */
  public int simpleCompare(CommonShell o1, CommonShell o2, boolean recordedLast) {
    if (recordedLast) {
      if (o1.getState() != STATE.RECORDED && o2.getState() == STATE.RECORDED) {
        return +1;
      } else if (o1.getState() == STATE.RECORDED && o2.getState() != STATE.RECORDED) {
        return -1;
      }
    }

    // items in same chapter alphabetical by tooltip
    return tooltipComp(o1, o2);
  }

/*  public int compare(CommonExercise o1, CommonExercise o2, boolean recordedLast) {
    if (recordedLast) {
      if (o1.getState() != STATE.RECORDED && o2.getState() == STATE.RECORDED) {
        return +1;
      } else if (o1.getState() == STATE.RECORDED && o2.getState() != STATE.RECORDED) {
        return -1;
      }
    }

    // compare first by hierarchical order - unit, then chapter, etc.
    int i = getTypeOrder(o1, o2);

    // items in same chapter alphabetical by tooltip
    return (i == 0) ? tooltipComp(o1, o2) : i;
  }*/

/*  private int getTypeOrder(CommonExercise o1, CommonExercise o2) {
    int i = 0;
    for (String type : typeOrder) {
      String type1 = o1.getUnitToValue().get(type);
      String type2 = o2.getUnitToValue().get(type);
      boolean t1Null = type1 == null;
      boolean t2Null = type2 == null;
      if (t1Null) {
        i = t2Null ? 0 : +1;
      } else if (t2Null) {
        i = -1;
      } else {
        i = type1.compareTo(type2);
      }

      if (i != 0) {
        break;
      }
    }
    return i;
  }*/

  protected <T extends CommonShell> int tooltipComp(T o1, T o2) {
    String id1 = o1.getEnglish();
    String id2 = o2.getEnglish();
    return compareStrings(id1, id2);
  }

  public int compareStrings(String id1, String id2) {
    String t = id1.toLowerCase();
    if (t.startsWith("a ")) t = t.substring(2);

    t = dropPunct(t);
    String t1 = id2.toLowerCase();
    if (t1.startsWith("a ")) t1 = t1.substring(2);
    t1 = dropPunct(t1);

    return removePunct(t).compareTo(removePunct(t1));
  }

  @NotNull
  private String dropPunct(String t) {
    String first = t.substring(0,1);
    if (first.equals("\"") || first.equals("\\'")) {
      t = t.substring(1);
    }
    return t;
  }
}
