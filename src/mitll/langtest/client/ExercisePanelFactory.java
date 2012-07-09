package mitll.langtest.client;

import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanelFactory {
  private LangTestDatabaseAsync service;
  private UserFeedback userFeedback;
  private ExerciseController controller;

  public ExercisePanelFactory( final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    this.service =service;
    this.userFeedback = userFeedback;
    this.controller = controller;
  }

  public ExercisePanel getExericsePanel(Exercise e) {
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      return new SimpleRecordExercisePanel(e, service, userFeedback, controller);
    } else {
      return new ExercisePanel(e, service, userFeedback, controller);
    }
  }
}
