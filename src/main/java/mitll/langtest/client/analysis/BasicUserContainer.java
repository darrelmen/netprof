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

package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.UserInfo;

public class BasicUserContainer<T extends UserInfo> extends MemoryItemContainer<T> {
  BasicUserContainer(ExerciseController controller,
                     String selectedUserKey,
                     String header) {
    super(controller, selectedUserKey, header, 10, 10);
  }

  /**
   * @param o1
   * @param o2
   * @return
   */
  protected int getIDCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return o1.getUserID().compareTo(o2.getUserID());
      }
    }
    return -1;
  }

   int getFirstCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getFirst().compareTo(o2.getFirst());

        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }

  int getLastCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        int i = o1.getLast().compareTo(o2.getLast());
        return i == 0 ? getDateCompare(o1, o2) : i;
      }
    }
    return -1;
  }

  protected String getItemLabel(T shell) {
    return shell.getUserID();
  }

  protected int getDateCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return Long.compare(o1.getTimestampMillis(), o2.getTimestampMillis());
      }
    }
    return -1;
  }

  public Long getItemDate(T shell) {
    return shell.getTimestampMillis();
  }
}
