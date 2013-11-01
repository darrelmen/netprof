package mitll.langtest.client.recorder;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackRecordPanel extends SimpleRecordExercisePanel {
  public FeedbackRecordPanel(Exercise e, LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller) {
    super(e, service, userFeedback, controller);
  }

  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    return new  AutoCRTRecordPanel(service, controller,exercise, this, index).getPanel();
  }

  @Override
  protected String getInstructions() {
    return "";
  }
}
