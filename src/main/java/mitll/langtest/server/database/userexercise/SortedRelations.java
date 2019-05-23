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

package mitll.langtest.server.database.userexercise;

import mitll.npdata.dao.SlickRelatedExercise;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SortedRelations {

  @NotNull
  public Map<Integer, List<SlickRelatedExercise>> getDialogIDToRelated(Map<Integer, List<SlickRelatedExercise>> dialogToRelations) {
    Map<Integer, List<SlickRelatedExercise>> orderedDialogToRelations = new HashMap<>();

    Set<Integer> dialogIDs = new TreeSet<>(dialogToRelations.keySet());

    dialogIDs.forEach(dialogID -> {
      List<SlickRelatedExercise> slickRelatedExercises = dialogToRelations.get(dialogID);

      Map<Integer, SlickRelatedExercise> contextExToRelation = new HashMap<>();

      List<SlickRelatedExercise> finalOneList = new ArrayList<>();

      Set<Integer> prevPointers = new HashSet<>();

      slickRelatedExercises.forEach(slickRelatedExercise -> {
//        logger.info("getDialogIDToRelated : for dialog " + dialogID + " : " + slickRelatedExercise);

        boolean isLast = slickRelatedExercise.exid() == slickRelatedExercise.contextexid();
        if (isLast) {
          finalOneList.add(slickRelatedExercise);
        }

        prevPointers.add(slickRelatedExercise.exid());

//        exToRelation.put(slickRelatedExercise.exid(), slickRelatedExercise);
        if (!isLast) {
          contextExToRelation.put(slickRelatedExercise.contextexid(), slickRelatedExercise);
        }
      });

      SlickRelatedExercise prev = null;
      if (finalOneList.isEmpty()) {
        Set<Integer> nextPointers = new HashSet<>(contextExToRelation.keySet());
        nextPointers.removeAll(prevPointers);

        if (!nextPointers.isEmpty()) {
          prev = contextExToRelation.get(nextPointers.iterator().next());
        }
        //  logger.info("getDialogIDToRelated 1 final link is " + prev);
      } else {
        prev = finalOneList.get(0);
        //  logger.info("getDialogIDToRelated 2 final link is " + prev);
      }

      {
        List<SlickRelatedExercise> inOrder = new ArrayList<>(slickRelatedExercises.size());

        if (contextExToRelation.isEmpty()) {
          if (prev != null) {
            inOrder.add(prev);
            // logger.info("getDialogIDToRelated " + dialogID + " : 1 add link " + prev);
          }
        } else {
//        SlickRelatedExercise prev = contextExToRelation.get(-1);
          while (prev != null) {
            inOrder.add(prev);
            // logger.info("getDialogIDToRelated " + dialogID + " : 2 add link " + prev);
            // int exid = slickRelatedExercise.exid();
            prev = contextExToRelation.get(prev.exid());
            // inOrder.add(prev);
          }

          Collections.reverse(inOrder);
//          for (int i = 0; i < inOrder.size(); i++) logger.info("\t#" + i + " : " + inOrder.get(i));
        }

        orderedDialogToRelations.put(dialogID, inOrder);
      }
    });

    return orderedDialogToRelations;
  }
}
