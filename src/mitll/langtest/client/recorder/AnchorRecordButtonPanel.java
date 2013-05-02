package mitll.langtest.client.recorder;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/12/13
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnchorRecordButtonPanel extends RecordButtonPanel {
  public AnchorRecordButtonPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise, ExerciseQuestionState questionState, int index) {
    super(service, controller, exercise, questionState, index);
  }


 /* protected Anchor makeRecordButton() {
    recordButton = new ImageAnchor();
    recordButton.setResource(recordImage);
    return recordButton;
  }*/
}
