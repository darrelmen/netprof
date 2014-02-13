package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/25/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardRecordButtonPanel extends RecordButtonPanel implements RecordButton.RecordingListener {
  private final AudioAnswerListener exercisePanel;

  /**
   *
   * @param exercisePanel
   * @param service
   * @param controller
   * @param exercise
   * @param index
   * @see BootstrapExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, boolean)
   */
  public FlashcardRecordButtonPanel(AudioAnswerListener exercisePanel, LangTestDatabaseAsync service,
                                    ExerciseController controller, Exercise exercise, int index) {
    super(service, controller, exercise, null, index, true);

    this.exercisePanel = exercisePanel;
   // recordButton.setTitle("Press and hold the space bar or mouse button to record");
    recordButton.setTitle("Press and hold the mouse button to record");
  }

  private IconAnchor waiting;
  private IconAnchor correctIcon;
  private IconAnchor incorrect;

  @Override
  protected void addImages() {
    waiting = new IconAnchor();
    correctIcon = new IconAnchor();
    incorrect = new IconAnchor();

    waiting.setBaseIcon(MyCustomIconType.waiting);
    waiting.setVisible(false);

    correctIcon.setBaseIcon(MyCustomIconType.correct);
    correctIcon.setVisible(false);

    incorrect.setBaseIcon(MyCustomIconType.incorrect);
    incorrect.setVisible(false);
  }

  @Override
  public Widget getRecordButton() {
    Widget recordButton1 = super.getRecordButton();
    Panel hp = new FlowPanel();
    hp.add(recordButton1);
    hp.add(waiting);
    hp.add(correctIcon);
    hp.add(incorrect);
    return hp;
  }

  protected RecordButton makeRecordButton(ExerciseController controller) {
    return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, true);  // TODO : fix later in classroom?
  }

  @Override
  public void initRecordButton() {
    super.initRecordButton();
    correctIcon.setVisible(false);
    incorrect.setVisible(false);
    waiting.setVisible(false);
  }

  /**
   * Deal with three cases: <br></br>
   * * the audio was invalid in some way : too short, too quiet, too loud<br></br>
   * * the audio was the correct response<br></br>
   * * the audio was incorrect<br></br><p></p>
   * <p/>
   * And then move on to the next item.
   *
   * @param result        response from server
   * @param questionState ignored here
   * @param outer         ignored here
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile
   */
  @Override
  protected void receivedAudioAnswer(final AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
    boolean correct = result.isCorrect();
    //final double score = result.getScore();
    recordButton.setVisible(false);
    waiting.setVisible(false);
    if (correct) {
      correctIcon.setVisible(true);
    } else {
      incorrect.setVisible(true);
    }

    exercisePanel.receivedAudioAnswer(result);
  }

  /**
   * @see #postAudioFile(com.google.gwt.user.client.ui.Panel, int)
   */
  @Override
  protected void receivedAudioFailure() {
    recordButton.setBaseIcon(MyCustomIconType.enter);
  }

  @Override
  public void flip(boolean first) {}

  @Override
  public void stopRecording() {
    super.stopRecording();
    recordButton.setVisible(false);
    waiting.setVisible(true);
  }
}
