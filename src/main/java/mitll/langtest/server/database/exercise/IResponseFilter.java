package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IResponseFilter {
  FilterResponse getTypeToValues(FilterRequest request, int projid, int userID);

  List<CommonExercise> filter(ExerciseListRequest request, List<CommonExercise> exercises, int projid);

  @NotNull
  List<CommonExercise> getCommonExercisesWithoutEnglish(List<CommonExercise> exercises);

  List<CommonExercise> getExercisesForSelectionState(ExerciseListRequest request, int projid);
}
