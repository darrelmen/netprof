package mitll.langtest.client.exercise;

import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.services.QCServiceAsync;
import mitll.langtest.client.services.ScoringServiceAsync;
import mitll.langtest.client.services.UserServiceAsync;

/**
 * Created by go22670 on 6/14/17.
 */
public interface Services extends EventRegistration {
  AudioServiceAsync getAudioService();

  /**
   *
   * @return
   */
  ScoringServiceAsync getScoringService();


  LangTestDatabaseAsync getService();

  QCServiceAsync getQCService();

  UserServiceAsync getUserService();

  ExerciseServiceAsync getExerciseService();

  ListServiceAsync getListService();
}
