package mitll.langtest.client.exercise;

import java.util.Comparator;

/**
 * Created by go22670 on 3/22/17.
 */
public interface ExerciseContainer<T> {
  void sortBy(Comparator<T> comp);

 // void forgetExercise(T es);
}
