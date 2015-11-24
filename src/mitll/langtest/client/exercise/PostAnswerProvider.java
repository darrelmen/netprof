/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

import mitll.langtest.shared.CommonExercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/7/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PostAnswerProvider {
  void postAnswers(ExerciseController controller, CommonExercise completedExercise);
}
