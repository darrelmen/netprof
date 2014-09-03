package mitll.langtest.server;

import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;

/**
 * Created by go22670 on 9/2/14.
 */
public interface LoadTesting {
  CommonExercise getRandomExercise();

  /*long addAnonUser();

    ExerciseListWrapper getExerciseIDs(int userID);
  */
  CommonExercise getExercise(String id, long userID);

/*  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID);

  ImageResponse getImageForAudioFile(String audioFile, String exerciseID);

  PretestScore getASRScoreForAudio(String testAudioFile, String sentence, String exerciseID);

  Collection<UserList> getListsForUser(long userid, boolean onlyCreated, boolean visited);*/
}
