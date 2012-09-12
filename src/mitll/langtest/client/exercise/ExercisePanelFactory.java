package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.SimpleRecordExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanelFactory {
  protected LangTestDatabaseAsync service;
  protected UserFeedback userFeedback;
  protected ExerciseController controller;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param service
   * @param userFeedback
   * @param controller
   */
  public ExercisePanelFactory( final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    this.service =service;
    this.userFeedback = userFeedback;
    this.controller = controller;
  }

  public Panel getExercisePanel(Exercise e) {
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      return new SimpleRecordExercisePanel(e, service, userFeedback, controller);
    } else {
      return new ExercisePanel(e, service, userFeedback, controller);
    }
  }
}
