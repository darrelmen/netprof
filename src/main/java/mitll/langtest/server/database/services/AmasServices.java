package mitll.langtest.server.database.services;

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.amas.AmasExerciseImpl;

import java.util.List;

/**
 * Created by go22670 on 3/8/17.
 */
public interface AmasServices {
  List<AmasExerciseImpl> getAMASExercises();

  AmasExerciseImpl getAMASExercise(int id);

  SectionHelper<AmasExerciseImpl> getAMASSectionHelper();
}
