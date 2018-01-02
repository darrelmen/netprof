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

package mitll.langtest.server.database.project;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.server.services.OpenUserServiceImpl;
import mitll.langtest.server.services.ProjectServiceImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.List;

public interface IProjectManagement {
  /**
   * @see DatabaseImpl#populateProjects()
   */
  void populateProjects();

  void rememberProject(int id);

  ExerciseDAO<CommonExercise> setDependencies();

  /**
   * @see ProjectServiceImpl#delete
   * @param projid
   */
  void forgetProject(int projid);

  void refreshProjects();

  Project getProjectForUser(int userid);

  void stopDecode();

  void addSingleProject(ExerciseDAO<CommonExercise> jsonExerciseDAO);

  CommonExercise getExercise(int projectid, int id);

  List<CommonExercise> getExercises(int projectid);

  Project getProject(int projectid);
  Project getProjectByName(String name);

  Collection<Project> getProjects();

  Collection<Project> getProductionProjects();

  Project getFirstProject();

  void setStartupInfo(User userWhere, int projid);

  /**
   * @see ProjectServices#configureProject
   * @param project
   * @param configureEvenRetired
   * @param forceReload
   * @return number of exercises in the project
   */
  int configureProject(Project project, boolean configureEvenRetired, boolean forceReload);

  /**
   * @see OpenUserServiceImpl#setProject
   * @param projid
   */
  void configureProjectByID(int projid);

  /**
   * @see LangTestDatabaseImpl#getStartupInfo
   * @return
   */
  List<SlimProject> getNestedProjectInfo();

  /**
   * Use file path to lookup in result table to get user id of who recorded it.
   * @param requestURI
   * @return
   */
  int getUserForFile(String requestURI);

  /**
   * @see ProjectServiceImpl#addPending
   * @param projID
   * @param dominoID
   * @param sinceInUTC
   * @return
   */
  ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC);

  List<ImportProjectInfo> getVocabProjects();
}
