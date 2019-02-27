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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasUnitChapter;
import mitll.npdata.dao.SlickExercisePhone;
import mitll.npdata.dao.SlickUpdateDominoPair;

import java.util.List;
import java.util.Set;

/**
 * Container for exercises for the site.
 * Mainly we want to be able to add and remove exercies, and add overlays of user exercises
 * Also attaches audio (join) to exercises.
 * <p>
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/8/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseDAO<T extends CommonShell & HasUnitChapter> extends SimpleExerciseDAO<T> {
  /**
   * @param userExerciseDAO
   * @param projid
   * @param isMyProject is the data on this host? e.g. hydra2 may only have some projects and audio
   * @see mitll.langtest.server.database.project.ProjectManagement#setDependencies
   */
  void setDependencies(IUserExerciseDAO userExerciseDAO,
                       AddRemoveDAO addRemoveDAO,
                       IAudioDAO audioDAO,
                       int projid,
                       Database database, boolean isMyProject);

  List<CommonExercise> getUserDefinedByProjectExactMatch(String fl, int userIDFromSession);

  void markSafeUnsafe(Set<Integer> safe, Set<Integer> unsafe, long dictTimestamp);

  /**
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercisePhoneInfoFromDict
   * @param id
   * @param count
   */
  void updatePhones(int id, int count);

  /**
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises
   * @param pairs
   */
  void updatePhonesBulk(List<SlickExercisePhone> pairs);

  int updateDominoBulk(List<SlickUpdateDominoPair> pairs);

  int getExIDForDominoID(int projID, int dominoID);

  int getParentFor(int exid);

  boolean refresh(int exid);

 // void bulkImport();
}
