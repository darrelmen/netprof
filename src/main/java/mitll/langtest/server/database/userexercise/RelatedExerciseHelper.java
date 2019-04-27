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

import mitll.langtest.server.database.dialog.DialogDAO;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import mitll.npdata.dao.userexercise.RelatedExerciseDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RelatedExerciseHelper implements IRelatedExercise {

  private static final Logger logger = LogManager.getLogger(RelatedExerciseHelper.class);

  private final RelatedExerciseDAOWrapper daoWrapper;

  /**
   * @param daoWrapper
   */
  RelatedExerciseHelper(RelatedExerciseDAOWrapper daoWrapper) {
    this.daoWrapper = daoWrapper;
  }

  public void createTable() {
    daoWrapper.createTable();
  }

  public String getName() {
    return daoWrapper.getName();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return daoWrapper.updateProject(oldID, newprojid) > 0;
  }

  @Override
  public Collection<SlickExercise> getContextExercises(int exid) {
    return daoWrapper.contextExercises(exid);
  }


  @Override
  public void addBulkRelated(List<SlickRelatedExercise> relatedExercises) {
    daoWrapper.addBulk(relatedExercises);
  }

  @Override
  public int getParentForContextID(int contextID) {
    return daoWrapper.parentForContextID(contextID);
  }

  @Override
  public Collection<SlickRelatedExercise> getAllRelated(int projid) {
    return daoWrapper.allByProject(projid);
  }

  @Override
  public void deleteForProject(int projID) {
    daoWrapper.deleteForProject(projID);
  }

  /**
   * @param related
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#doContextDeletes
   */
  @Override
  public int deleteRelated(int related) {
    return daoWrapper.deleteRelated(related);
  }

  @Override
  public boolean deleteAndFixForEx(int exid) {
    return daoWrapper.deleteAndFix(exid) > 0;
  }

  //  @Override
  public boolean insertAfter(int after, int exid) {
    return daoWrapper.insertAfter(after, exid) > 0;
  }

  @Override
  public int deleteRelatedForDialog(int dialog) {
    return daoWrapper.deleteRelatedForDialog(dialog);
  }

  /**
   * Final link could either be one with two pointers to same node or with a next pointer to which there are no prev pointers.
   *
   * @param projid
   * @return
   * @see mitll.langtest.server.database.dialog.DialogDAO#getDialogs
   */
  @Override
  public Map<Integer, List<SlickRelatedExercise>> getDialogIDToRelated(int projid) {
    Map<Integer, List<SlickRelatedExercise>> dialogToRelations = daoWrapper.byProjectForDialog(projid);

    Map<Integer, List<SlickRelatedExercise>> orderedDialogToRelations = new HashMap<>();

    dialogToRelations.forEach((k, v) -> {
      //Map<Integer, SlickRelatedExercise> exToRelation = new HashMap<>();

      Map<Integer, SlickRelatedExercise> contextExToRelation = new HashMap<>();

      List<SlickRelatedExercise> finalOneList = new ArrayList<>();

      Set<Integer> prevPointers = new HashSet<>();

      v.forEach(slickRelatedExercise -> {
        logger.info("for dialog " + k + " : " + slickRelatedExercise);

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
        logger.info("1 final link is " + prev);
      } else {
        prev = finalOneList.get(0);
        logger.info("2 final link is " + prev);
      }

      {
        List<SlickRelatedExercise> inOrder = new ArrayList<>(v.size());

        if (contextExToRelation.isEmpty()) {
          if (prev != null) {
            inOrder.add(prev);
            logger.info(k + " : 1 add link " + prev);
          }
        } else {
//        SlickRelatedExercise prev = contextExToRelation.get(-1);
          while (prev != null) {
            inOrder.add(prev);
            logger.info(k + " : 2 add link " + prev);
            // int exid = slickRelatedExercise.exid();
            prev = contextExToRelation.get(prev.exid());
            // inOrder.add(prev);
          }

          Collections.reverse(inOrder);
          for (int i = 0; i < inOrder.size(); i++) logger.info("\t#" + i + " : " + inOrder.get(i));
        }

        orderedDialogToRelations.put(k, inOrder);
      }
    });

    return orderedDialogToRelations;
  }
}
