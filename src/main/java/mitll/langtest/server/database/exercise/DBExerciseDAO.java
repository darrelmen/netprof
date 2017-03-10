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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static mitll.langtest.server.database.userexercise.SlickUserExerciseDAO.SOUND;

public class DBExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(DBExerciseDAO.class);
  private SlickUserExerciseDAO userExerciseDAO;
  private SlickProject project;
  private Project fullProject;

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#setExerciseDAO
   */
  public DBExerciseDAO(
      ServerProperties serverProps,
      IUserListManager userListManager,
      boolean addDefects,
      SlickUserExerciseDAO userExerciseDAO,
      Project fullProject
  ) {
    super(serverProps, userListManager, addDefects, fullProject.getProject().language(), fullProject.getProject().id());
    this.userExerciseDAO = userExerciseDAO;
    this.project = fullProject.getProject();
    this.fullProject = fullProject;
  }

  /**
   * TODO : remove duplicate
   *
   * @return
   */
  @Override
  public List<String> getTypeOrder() {
    List<String> typeOrder = getSectionHelper().getTypeOrder();
    if (typeOrder.isEmpty()) {
      typeOrder = new ArrayList<>();
      String first = project.first();
      String second = project.second();
      if (first != null && !first.isEmpty()) typeOrder.add(first);
      if (second != null && !second.isEmpty()) typeOrder.add(second);
      // typeOrder = project.getTypeOrder();
    }
    return typeOrder;
  }

  /**
   * I don't think we're doing user exercise mask-out overrides anymore...
   *
   * @param removes
   */
  protected void addOverlays(Collection<Integer> removes) {
  }

  public Map<Integer, String> getIDToFL(int projid) {
    return userExerciseDAO.getIDToFL(projid);
  }

  /**
   * Does join with related exercise table - maybe better way to do this in scala side?
   *
   * @return
   * @see #getRawExercises()
   */
  @Override
  List<CommonExercise> readExercises() {
    try {
      List<String> typeOrder = new ArrayList<>();

      typeOrder.add(project.first());

      if (!project.second().isEmpty()) typeOrder.add(project.second());
      typeOrder.add(SOUND);
      if (SlickUserExerciseDAO.ADD_PHONE_LENGTH) {
        typeOrder.add(SlickUserExerciseDAO.DIFFICULTY);
      }

      int projid = project.id();

      Map<Integer, ExercisePhoneInfo> exerciseToPhoneForProject =
          userExerciseDAO.getRefResultDAO().getExerciseToPhoneForProject(projid);

      logger.info("readExercises" +
          "\n\tread " + exerciseToPhoneForProject.size() + " ExercisePhoneInfo" +
          "\n\tfor " + projid +
          "\n\ttype order " + typeOrder);

      List<CommonExercise> allNonContextExercises =
          userExerciseDAO.getByProject(projid, typeOrder, getSectionHelper(), exerciseToPhoneForProject, fullProject);

      Map<Integer, ExerciseAttribute> allByProject = userExerciseDAO.getIDToPair(projid);

      logger.info("found " + allByProject.size() + " attributes");

      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs = userExerciseDAO.getAllJoinByProject(projid);

      for (CommonExercise exercise : allNonContextExercises) {
        int id = exercise.getID();
        Collection<SlickExerciseAttributeJoin> slickExerciseAttributeJoins = exToAttrs.get(id);

        List<ExerciseAttribute> attributes = new ArrayList<>();
        if (slickExerciseAttributeJoins != null) {
          for (SlickExerciseAttributeJoin join : slickExerciseAttributeJoins) {
            ExerciseAttribute attribute = allByProject.get(join.attrid());
            attributes.add(attribute);
          }
          exercise.setAttributes(attributes);
          //   logger.info("now " + exercise.getID() + "  " + exercise.getAttributes());
        }
      }
      logger.info("readExercises project " + project +
          " readExercises got " + allNonContextExercises.size() + " predef exercises");

      Collection<SlickRelatedExercise> related = userExerciseDAO.getAllRelated(projid);

//      logger.info(prefix + " readExercises got " + related.size() + " related exercises;");
      Map<Integer, CommonExercise> idToEx = getIDToExercise(allNonContextExercises);

      Map<Integer, CommonExercise> idToContext =
          getIDToExercise(userExerciseDAO.getContextByProject(projid, typeOrder, getSectionHelper(), exerciseToPhoneForProject, fullProject));

      // logger.info(prefix + " idToContext " + idToContext.size());

      int attached = 0;
      int c = 0;

      String prefix = "Project " + project.name();
      for (SlickRelatedExercise relatedExercise : related) {
        CommonExercise root = idToEx.get(relatedExercise.exid());
        if (root != null) {
          CommonExercise context = idToContext.get(relatedExercise.contextexid());
          if (context != null) {
            root.getMutable().addContextExercise(context);
            attached++;
          } else if (c++ < 2) {
            logger.warn("1 " + prefix + " didn't attach " + relatedExercise + "" + " for\n" + root);
          }
        } else if (c++ < 10) {
          logger.warn("2 " + prefix + " didn't attach " + relatedExercise + "" +
              " for, e.g. " + allNonContextExercises.iterator().next());
        }
      }
      logger.info(prefix + " Read " + allNonContextExercises.size() + " exercises from database, attached " + attached);
      return allNonContextExercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  private Map<Integer, CommonExercise> getIDToExercise(Collection<CommonExercise> allExercises) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise ex : allExercises) {
      idToEx.put(ex.getID(), ex);
    }
    return idToEx;
  }

  public void markSafeUnsafe(Set<Integer> safe, Set<Integer> unsafe) {
    userExerciseDAO.getDao().updateCheckedBulk(safe, true);
    userExerciseDAO.getDao().updateCheckedBulk(unsafe, false);
  }

  public String toString() {
    return "DBExerciseDAO for " + project;
  }
}
