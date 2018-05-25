package mitll.langtest.client.exercise;

import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.*;
import mitll.langtest.shared.project.SlimProject;

import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 6/14/17.
 */
public interface Services extends EventRegistration {
  List<SlimProject> getAllProjects();

  String getHost();


  AudioServiceAsync getAudioServiceAsyncForHost(String host);

  ScoringServiceAsync getScoringServiceAsyncForHost(String host);

  AudioServiceAsync getAudioService();

  Collection<AudioServiceAsync> getAllAudioServices();

  void tellHydraServerToRefreshProject(int projID);

  /**
   *
   * @return
   */
  ScoringServiceAsync getScoringService();


  LangTestDatabaseAsync getService();

  QCServiceAsync getQCService();

  UserServiceAsync getUserService();

  OpenUserServiceAsync getOpenUserService();

  ExerciseServiceAsync getExerciseService();

  ListServiceAsync getListService();

  DLIClassServiceAsync getDLIClassService();
}
