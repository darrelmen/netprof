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

import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickUpdateDominoPair;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see
 */
public interface IUserExerciseDAO extends IDAO {
  /**
   * @see mitll.langtest.server.database.copy.ExerciseCopy#getOldToNewExIDs(DatabaseImpl, int)
   * @param projectid
   * @return
   */
  BothMaps getOldToNew(int projectid);

  /**
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addExercises(int, int, Map, IUserExerciseDAO, Collection, Collection, Map, Map, int)
   * @param projid
   * @return
   */
  Map<Integer, String> getIDToFL(int projid);

  /**
   * @see DBExerciseDAO#readExercises
   * @return
   */
/*
  IRefResultDAO getRefResultDAO();
*/

  /**
   * @see mitll.langtest.server.ScoreServlet#getJsonForAudio(HttpServletRequest, ScoreServlet.PostRequest, String, String)
   * @return
   */
  SlickExercise getUnknownExercise();

  /**
   * @see DatabaseImpl#updateProject(int, int, boolean)
   * @param old
   * @param newprojid
   * @param justTheseIDs
   * @return
   */
  boolean updateProjectChinese(int old, int newprojid, List<Integer> justTheseIDs);

  /**
   * @see mitll.langtest.server.database.copy.ExerciseCopy#reallyAddingUserExercises(int, Collection, IUserExerciseDAO, Map, List)
   * @param shared
   * @param projectID
   * @param typeOrder
   * @return
   */
  SlickExercise toSlick(Exercise shared, int projectID, Collection<String> typeOrder);

  /**
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addPredefExercises(int, IUserExerciseDAO, int, Collection, Collection, Map, Map, boolean)
   * @param shared
   * @param projectID
   * @param importUserIfNotSpecified
   * @param isContext
   * @param typeOrder
   * @return
   */
  SlickExercise toSlick(CommonExercise shared,
                        int projectID,
                        int importUserIfNotSpecified,
                        boolean isContext,
                        Collection<String> typeOrder);
/*
  int getAndRememberNumPhones(IPronunciationLookup lookup,
                              int exid,
                              String foreignlanguage, String transliteration);*/

  void addBulk(List<SlickExercise> bulk);

  /**
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @return
   * @seex IUserListManager#newExercise(int, CommonExercise, String)
   */
  int add(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder);

  int insert(SlickExercise UserExercise);

  List<CommonShell> getOnList(int listID, boolean shouldSwap);

  /**
   *
   * @param listID
   * @param shouldSwap
   * @return
   */
  List<CommonExercise> getCommonExercises(int listID, boolean shouldSwap);

  /**
   * @param exid
   * @param shouldSwap
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseByExID
   */
  CommonExercise getByExID(int exid, boolean shouldSwap);

  SlickExercise getByID(int exid);

  /**
   * @param oldid
   * @param projID
   * @return
   * @see SlickUserExerciseDAO#getTemplateExercise
   */
  CommonExercise getByExOldID(String oldid, int projID);

  /**
   * @see ProjectManagement#getExercise
   * @param exid
   * @return
   */
  int getProjectForExercise(int exid);

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile(String, AudioContext, boolean, String, String, DecoderOptions)
   * @param projID
   * @return
   */
  CommonExercise getTemplateExercise(int projID);

  /**
   * @see DatabaseImpl#afterDAOSetup
   * @param projID
   * @return
   */
  int ensureTemplateExercise(int projID);

  List<CommonExercise> getByProject(
      List<String> typeOrder,
      ISection<CommonExercise> sectionHelper,
      Project theProject,
      Map<Integer, ExerciseAttribute> allByProject,
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs, boolean isPredef);

  List<Integer> getUserDefinedByProjectExactMatch(int projid, int creator, String fl);

  /**
   *
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @param allByProject
   * @param exToAttrs
   * @param isPredef
   * @return
   */
  List<CommonExercise> getContextByProject(
      List<String> typeOrder,
      ISection<CommonExercise> sectionHelper,
      Project lookup,
      Map<Integer, ExerciseAttribute> allByProject,
      Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs,
      boolean isPredef);

  Collection<CommonExercise> getOverrides(boolean shouldSwap);

/*
  Collection<CommonExercise> getByExID(Collection<Integer> exids, boolean shouldSwap);
*/

  List<SlickExercise> getExercisesByIDs(Collection<Integer> exids);

  void deleteByExID(Collection<Integer> exids);

  /**
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#doUpdate
   */
  boolean update(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder);

  boolean updateModified(int exid);

  /**
   * @param exerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   * @see ProjectManagement#populateProjects
   */
  void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO);

  int getUnknownExerciseID();

  void deleteForProject(int projID);

  SlickExercise getByDominoID(int projID, int docID);

  Map<Integer,Integer> getDominoIDToExID(int docID);

  ExerciseDAOWrapper getDao();

  Map<Integer, SlickExercise> getDominoToSlickEx(int projectid);

  boolean areThereAnyUnmatched(int projID);
  Map<String, Integer> getNpToExID(int projid);
  int updateDominoBulk(List<SlickUpdateDominoPair> pairs);

  IAttributeJoin getExerciseAttributeJoin();

  IAttribute getExerciseAttribute();

  boolean isProjectEmpty(int projectid);

  IRelatedExercise getRelatedExercise();
  IRelatedExercise getRelatedCoreExercise();
}
