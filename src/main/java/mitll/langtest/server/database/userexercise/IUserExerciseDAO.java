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

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExercise;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see
 */
public interface IUserExerciseDAO extends IDAO {
  SlickExercise getUnknownExercise();

  /**
   * @see IUserListManager#newExercise(int, CommonExercise, String)
   * @param userExercise
   * @param isOverride
   * @param isContext
   * @param typeOrder
   * @return
   */
  int add(CommonExercise userExercise, boolean isOverride, boolean isContext, Collection<String> typeOrder);

  void addContextToExercise(int exid, int contextid, int projid);

  List<CommonShell> getOnList(int listID);

  List<CommonExercise> getCommonExercises(int listID);
  /**
   * @param exid
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseByExID
   */
  CommonExercise getByExID(int exid);
  SlickExercise getByID(int exid);

  /**
   * @see BaseExerciseDAO#addNewExercises
   * @param oldid
   * @param projID
   * @return
   */
  CommonExercise getByExOldID(String oldid, int projID);

  int getProjectForExercise(int exid);

  CommonExercise getTemplateExercise(int projID);

  int ensureTemplateExercise(int projID);

  Collection<CommonExercise> getAllUserExercises(int projid);

  List<SlickExercise> getDeletedFor(int projid);

  Collection<CommonExercise> getOverrides();

  Collection<CommonExercise> getByExID(Collection<Integer> exids);

  void deleteByExID(Collection<Integer> exids);

  /**
   * @see mitll.langtest.server.domino.ProjectSync#doUpdate
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @return
   */
  boolean update(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder);
  boolean updateModified(int exid);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   * @see ProjectManagement#populateProjects(PathHelper, ServerProperties, LogAndNotify, DatabaseImpl)
   * @param exerciseDAO
   */
  void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO);

  int addAttribute(int projid, long now, int userid, ExerciseAttribute attribute);

  int getUnknownExerciseID();

  void deleteForProject(int projID);

  int deleteRelated(int related);
}
