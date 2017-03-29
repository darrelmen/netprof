package mitll.langtest.server.database.project;

import mitll.langtest.server.database.exercise.Project;

import java.util.Collection;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ProjectServices {
  void rememberProject(int projectid);

  void rememberProject(int userid, int projectid);

  void forgetProject(int userid);

  Project getProjectForUser(int userid);

  Project getProject(int projectid);

  Collection<Project> getProjects();

  void configureProject(Project project);


  String getLanguage(int projectid);
}
