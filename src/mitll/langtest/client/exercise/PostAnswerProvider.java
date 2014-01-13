package mitll.langtest.client.exercise;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/7/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PostAnswerProvider/*<T extends ExerciseShell>*/ {
  /*<T extends ExerciseShell>*/ void postAnswers(ExerciseController controller, ExerciseShell completedExercise);
}
