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

package mitll.langtest.server.sorter;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/30/2014.
 */
public class ExerciseSorter extends SimpleSorter {
  private static final Logger logger = Logger.getLogger(ExerciseSorter.class);

  private Map<String, Integer> phoneToCount;

  /**
   * @see LangTestDatabaseImpl#sortExercises(String, List)
   * @param typeOrder
   */
  public ExerciseSorter(Collection<String> typeOrder) { super(typeOrder); }

  /**
   * @param typeOrder
   * @param phoneToCount
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
  public ExerciseSorter(Collection<String> typeOrder, Map<String, Integer> phoneToCount) {
    this(typeOrder);
    this.phoneToCount = phoneToCount;
  }

  /**
   * @see mitll.langtest.server.json.JsonExport#getJsonForSelection
   * @param toSort
   * @param phoneToCount
   */
  public void sortedByPronLengthThenPhone(List<? extends CommonExercise> toSort,final Map<String, Integer> phoneToCount) {
    Collections.sort(toSort, new Comparator<CommonExercise>() {
      @Override
      public int compare(CommonExercise o1, CommonExercise o2) {
        // items in same chapter alphabetical by tooltip
        return phoneCompFirst(o1, o2, phoneToCount);
      }
    });
  }

    /**
     * @param o1
     * @param o2
     * @return
     * @see mitll.langtest.server.database.ResultDAO#getSortedAVPHistoryByPhones
     */
  public int phoneCompByFirst(CommonExercise o1, CommonExercise o2) { return phoneCompFirst(o1, o2, phoneToCount);  }

  int spew = 0;
  /**
   * Compare and put shorter pronunciations before longer ones.
   * When they are the same length, compare phones, and when you get to a difference, use their relative occurrence
   * frequency, with rarer phones first.
   * If the phones have the same frequency, compare the phone names (ideally this is unlikely).
   * @param o1
   * @param o2
   * @param phoneToCount
   * @return
   */
  private int phoneCompFirst(CommonExercise o1, CommonExercise o2, final Map<String, Integer> phoneToCount) {
    List<String> pron1 = o1.getFirstPron();
    List<String> pron2 = o2.getFirstPron();

    // these cases should never happen - really defensive
    if (pron1 == null && pron2 != null) {
      logger.warn("missing pron?");
      return +1;
    }
    if (pron1 != null && pron2 == null) {
      logger.warn("missing pron?");
      return -1;
    }
    if (pron1 == null) {
      logger.warn("missing pron?");
      return o1.getForeignLanguage().toLowerCase().compareTo(o2.getForeignLanguage().toLowerCase());
    }

    int o1Num = pron1.size();
    int o2Num = pron2.size();

    int comp = new Integer(o1Num).compareTo(o2Num);
    if (comp != 0) {
      return comp;
    } else {
      int n = Math.min(o1Num, o2Num);
      for (int i = 0; i < n; i++) {
        String a = pron1.get(i);
        String b = pron2.get(i);
        if (!a.equals(b)) {
          Integer a1 = phoneToCount.get(a);
          if (a1 == null) {
            if (spew++ < 100) logger.error("phoneCompFirst huh? no phone count for " + a + " in " + phoneToCount.keySet() + " for " +o1.getID());
            a1 = -1;
          }
          Integer b1 = phoneToCount.get(b);
          if (b1 == null) {
            if (spew++ < 100) logger.error("phoneCompFirst huh? no phone count for " + b + " in " + phoneToCount.keySet() + " for " +o2.getID());
            b1 = -1;
          }
          int compt = a1.compareTo(b1);
          return compt == 0 ? a.compareTo(b) :compt;
        }
      }
      return 0;
    }
  }

}