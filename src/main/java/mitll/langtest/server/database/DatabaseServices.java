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

package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.custom.IStateManager;
import mitll.langtest.server.database.exercise.ExerciseServices;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.database.report.ReportingServices;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.userlist.UserListServices;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.User;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DatabaseServices extends DAOContainer, ProjectServices, //AmasServices,
    UserListServices, ExerciseServices, ReportingServices {
  Database getDatabase();

  IAnalysis getAnalysis(int projectid);



  mitll.langtest.server.database.user.UserManagement getUserManagement();

  IUserSecurityManager getUserSecurityManager();

  void setUserSecurityManager(IUserSecurityManager userSecurityManager);

  IProjectManagement getProjectManagement();


  void logEvent(String exid, String context, int userid, String device, int projID);

  boolean logEvent(String id, String widgetType, String exid, String context, int userid, String device, int projID);

  CommonExercise getCustomOrPredefExercise(int projid, int id);

  DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport, ServletContext servletContext, boolean loadAll);

  ISection<CommonExercise> getSectionHelper(int projectid);
  ISection<IDialog> getDialogSectionHelper(int projectid);

  void markAudioDefect(AudioAttribute audioAttribute);

  /**
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet
   * @see mitll.langtest.server.services.ResultServiceImpl#getMonitorResults(int)
   * @see mitll.langtest.server.services.ResultServiceImpl#getResults(int, Map, int, String)
   * @param projid
   * @return
   */
  Collection<MonitorResult> getMonitorResults(int projid);

  List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults, int projid);

  void rememberScore(int projID, int resultID, PretestScore asrScoreForAudio, boolean isCorrect);

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#rememberAnswer
   * @param projID
   * @param answer
   * @param answerID
   */
  void recordWordAndPhoneInfo(int projID, AudioAnswer answer, int answerID);

  ServerProperties getServerProps();

  void stopDecode();

  void setStartupInfo(User userWhere);

  Collection<String> getTypeOrder(int projectid);

  IStateManager getStateManager();

  boolean isHasValidDB();

  String getDbConfig();

  JsonExport getJSONExport(int projectid);
}
