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
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts by chapters, then by tooltip...
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/9/15.
 */
public class SimpleSorter extends ExerciseComparator {
  protected final Collection<String> typeOrder;
  SimpleSorter(Collection<String> typeOrder) {
    this.typeOrder = typeOrder;
  }

  /**
   * what if the item has no unit/chapter info?
   * what if we have a mixed list of user and predef items?
   *
   * @param toSort
   * @param recordedLast
   * @return
   * @see LangTestDatabaseImpl#sortExercises
   */
  public void getSortedByUnitThenAlpha(List<? extends CommonShell> toSort, final boolean recordedLast) {
    if (typeOrder.isEmpty()) {
      sortByTooltip(toSort);
    } else {
      Collections.sort(toSort, new Comparator<CommonShell>() {
        @Override
        public int compare(CommonShell o1, CommonShell o2) {
          return SimpleSorter.this.simpleCompare(o1, o2, recordedLast);
        }
      });
    }
  }

  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * NOTE:  be careful to use collation order when it's not "english-foreign language"
   *
   * @param exerciseShells
   * @see mitll.langtest.server.database.AudioExport#writeZipJustOneAudio(OutputStream, SectionHelper, Collection, String, String)
   */
  public <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
    Collections.sort(exerciseShells, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return tooltipComp(o1, o2);
      }
    });
  }
}
