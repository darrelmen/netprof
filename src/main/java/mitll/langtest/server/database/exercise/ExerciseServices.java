package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;

import java.util.List;
import java.util.Set;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ExerciseServices {
  CommonExercise duplicateExercise(CommonExercise exercise);

  @Deprecated
  boolean deleteItem(int exid, int projectid);

  void reloadExercises(int projectid);

  CommonExercise editItem(CommonExercise userExercise, boolean keepAudio);

  Set<Integer> getIDs(int projectid);

  CommonExercise getExercise(int projectid, int id);

  List<CommonExercise> getExercises(int projectid);

  ExerciseDAO<CommonExercise> getExerciseDAO(int projectid);

  String getLanguage(CommonExercise ex);
}
