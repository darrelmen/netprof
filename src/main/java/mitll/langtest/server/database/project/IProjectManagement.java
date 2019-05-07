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

package mitll.langtest.server.database.project;

import mitll.hlt.domino.server.extern.importers.ImportResult;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.server.services.OpenUserServiceImpl;
import mitll.langtest.server.services.ProjectServiceImpl;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.OOV;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.OOVInfo;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.User;
import org.apache.commons.fileupload.FileItem;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IProjectManagement {
  /**
   * @see DatabaseImpl#populateProjects(int)
   * @param projID
   */
  void populateProjects(int projID);

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

  void addSingleProject(ExerciseDAO<CommonExercise> excelImport);

  CommonExercise getExercise(int projectid, int id);

  /**
   * Only for testing
   * @param id
   * @return
   */
  CommonExercise getExercise(int id);

  List<CommonExercise> getExercises(int projectid, boolean onlyOne);

  Project getProject(int projectid, boolean onlyOne);
  IProject getIProject(int projectid, boolean onlyOne);
  Project getProjectByName(String name);
  List<Project> getProjectByLanguage(Language name);
  Project getProductionByLanguage(Language language);
  List<Project> getMatchingProjects(Language languageMatchingGroup, boolean isPoly);
  boolean exists(int projectid);

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

  int getProjectIDForLanguage(Language language);

  /**
   * @see OpenUserServiceImpl#setProject
   * @param projid
   */
  void configureProjectByID(int projid);

  void clearStartupInfo(User userWhere);

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
   * @return
   */
  ImportInfo getImportFromDomino(int projID);

  /**
   * @see mitll.langtest.server.domino.ProjectSync#getDominoForLanguage
   * @return
   */
  List<ImportProjectInfo> getVocabProjects();

  Map<String,Integer> getNpToDomino(int dominoProjectID);

  ImportResult doDominoImport(int dominoID, FileItem item, Collection<String> typeOrder, int userID);

  ImportResult doDominoImport(int dominoID, File excelFile, Collection<String> typeOrder, int userID);

  OOVInfo checkOOV(int id, int num, int offset);

  void updateOOV(List<OOV> updates, int user);

 // boolean doDominoImport(int dominoID, String path);
}
