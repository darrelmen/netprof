package mitll.langtest.server.load;

import mitll.langtest.shared.AudioAnswer;
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
  CommonExercise getExercise(String id, long userID, boolean isFlashcardReq);

  CommonExercise getFirstExercise();
  void addToAudioTable(int user, CommonExercise exercise1, AudioAnswer audioAnswer);
}
