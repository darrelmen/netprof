package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.ComboRecordPanel;
import mitll.langtest.client.recorder.FeedbackRecordPanel;
import mitll.langtest.client.recorder.SimpleRecordExercisePanel;
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
  protected ListInterface exerciseList;

  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public ExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller, ListInterface exerciseList) {
    this.service = service;
    this.userFeedback = userFeedback;
    this.controller = controller;
    this.exerciseList = exerciseList;
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#makeExercisePanel
   * @param e
   * @return
   */
  public Panel getExercisePanel(Exercise e) {
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      if (controller.isAutoCRTMode() && !e.isPromptInEnglish()) {
        if (controller.getProps().isIncludeFeedback()) {
          return new FeedbackRecordPanel(e, service, userFeedback, controller);
        }
        else {
          return new ComboRecordPanel(e, service, userFeedback, controller);
        }
      } else {
        return new SimpleRecordExercisePanel(e, service, userFeedback, controller, exerciseList);
      }
    } else {
      return new ExercisePanel(e, service, userFeedback, controller, exerciseList);
    }
  }
}
