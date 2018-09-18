package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;

import java.util.List;
import java.util.Set;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ExerciseServices {
  /**
   *
   * @param userExercise
   * @param keepAudio
   * @return
   */
  void editItem(ClientExercise userExercise, boolean keepAudio);

  Set<Integer> getIDs(int projectid);

  CommonExercise getExercise(int projectid, int id);

  List<CommonExercise> getExercises(int projectid, boolean onlyOne);

  ExerciseDAO<CommonExercise> getExerciseDAO(int projectid);

  String getLanguage(CommonExercise ex);
  Language getLanguageEnum(CommonExercise ex);
}
