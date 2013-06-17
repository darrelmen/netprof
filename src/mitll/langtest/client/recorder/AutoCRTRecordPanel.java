/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 *
 * Has three parts -- record/stop button, audio validity feedback, and audio html 5 control to playback audio just posted to the server. <br></br>
 *
 * On click on the stop button, posts audio to the server. <br></br>
 *
 * Automatically stops recording after 20 seconds.
 *
 * @author Gordon Vidaver
 */
public class AutoCRTRecordPanel extends SimpleRecordPanel {
  private final HTML resp = new HTML();

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.recorder.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
	public AutoCRTRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                            final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    super(service, controller, exercise, questionState, index);
    getPanel().add(resp);
  }

  @Override
  protected void addValidityFeedback(int index) {}

  @Override
  public void startRecording() {
    super.startRecording();
    resp.setText("");
  }
  /**
   * @see RecordButtonPanel#stopRecording()
   * @param result from server about the audio we just posted
   * @param questionState so we keep track of which questions have been answered
   * @param outer
   */
  @Override
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
    super.receivedAudioAnswer(result,questionState,outer);

    if (result.decodeOutput.length() > 0) {
      // i.e. autocrt -- revisit?
      showAutoCRTFeedback(result);
    }
    else if (result.getScore() != -1) {
      resp.setHTML("I couldn't understand that, please record again.");
    }
  }

  private void showAutoCRTFeedback(AudioAnswer result) {
    double score = result.getScore();
    score *= 2.5;
    score -= 1.25;
    score = Math.max(0,score);
    score = Math.min(1.0,score);
    String percent = ((int) (score * 100)) + "%";
    if (result.getScore() > 0.6) {
      resp.setHTML("Correct! Score for <font size=+1>" + result.decodeOutput + "</font> was " + percent);
      resp.setStyleName("correct");
    } else {
      resp.setHTML("Try again - score for <font size=+1>" + result.decodeOutput + "</font> was " + percent);
      resp.setStyleName("incorrect");
    }
  }

  @Override
  public void stopRecording() {
    super.stopRecording();
    if (controller.isAutoCRTMode())  {
      resp.removeStyleName("incorrect");
      resp.removeStyleName("correct");
      resp.setText("Scoring... please wait.");
    }
  }
}
