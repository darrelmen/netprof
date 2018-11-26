package mitll.langtest.server.database.project;

import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.domino.IProjectSync;
import mitll.langtest.server.services.OpenUserServiceImpl;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectInfo;

import java.util.Collection;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ProjectServices {
  /**
   * @see mitll.langtest.server.database.copy.CreateProject#createProject(DAOContainer, ProjectServices, ProjectInfo, int)
   * @param projectid
   */
  void rememberProject(int projectid);

  /**
   * @see mitll.langtest.server.rest.RestUserManagement#tryToLogin
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setProject
   * @param userid
   * @param projectid
   */
  void rememberUsersCurrentProject(int userid, int projectid);

  Project getProjectForUser(int userid);

  Project getProject(int projectid);

  Collection<Project> getProjects();

  /**
   * @see mitll.langtest.server.services.MyRemoteServiceServlet#getProjectIDFromUser(int)
   * @param project
   * @param forceReload
   * @return number of exercises in the project
   */
  int configureProject(Project project, boolean forceReload);

  String getLanguage(int projectid);
  Language getLanguageEnum(int projectid);
  
  IProjectSync getProjectSync();
}
