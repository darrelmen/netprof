package mitll.langtest.client.exercise;

import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.*;
import mitll.langtest.shared.project.SlimProject;

import java.util.List;

/**
 * Created by go22670 on 6/14/17.
 */
public interface Services extends EventRegistration {
  List<SlimProject> getAllProjects();

  AudioServiceAsync getAudioServiceAsyncForHost(String host);

  ScoringServiceAsync getScoringServiceAsyncForHost(String host);

  AudioServiceAsync getAudioService();

  /**
   *
   * @return
   */
  ScoringServiceAsync getScoringService();


  String getHost();

  LangTestDatabaseAsync getService();

  QCServiceAsync getQCService();

  UserServiceAsync getUserService();

  ExerciseServiceAsync getExerciseService();

  ListServiceAsync getListService();
}
