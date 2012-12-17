/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
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
public class SimpleRecordPanel extends RecordButtonPanel {
  private static final String IMAGES_CHECKMARK = LangTest.LANGTEST_IMAGES +"checkmark.png";
  private static final String IMAGES_REDX_PNG  = LangTest.LANGTEST_IMAGES +"redx.png";
  private Image check;
  private SimplePanel playback = new SimplePanel();
  private final AudioTag audioTag = new AudioTag();
  private final HTML resp = new HTML();

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
	public SimpleRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    super(service, controller, exercise, questionState, index);

    setSpacing(10);

    playback.setHeight("30px"); // for audio controls to show

    // make audio feedback widget
    addValidityFeedback(index);

    // add playback html
    addPlayback();

    add(resp);
  }

  private void addPlayback() {
    add(playback);
  }

  protected void addValidityFeedback(int index) {
    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");
    check.setVisible(false);

    if (!controller.isAutoCRTMode()) add(check);
  }

  @Override
  protected void startRecording() {
    super.startRecording();
    playback.setWidget(new HTML(""));
    resp.setText("");
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param result
   * @param questionState
   * @param outer
   */
  @Override
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
    showAudioValidity(result.validity, questionState, outer);
    setAudioTag(result.path);

    if (result.decodeOutput.length() > 0) {

      // i.e. autocrt -- revisit?
      double score = result.score;
      score *= 2.5;
      score -= 1.25;
      score = Math.max(0,score);
      score = Math.min(1.0,score);
      String percent = ((int) (score * 100)) + "%";
      if (result.score > 0.6) {
        resp.setHTML("Correct! Score for <font size=+1>" + result.decodeOutput + "</font> was " + percent);
        resp.setStyleName("correct");
      } else {
        resp.setHTML("Try again - score for <font size=+1>" + result.decodeOutput + "</font> was " + percent);
        resp.setStyleName("incorrect");
      }
    }
  }

  @Override
  protected void stopRecording() {
    super.stopRecording();
    if (controller.isAutoCRTMode())  {
      resp.removeStyleName("incorrect");
      resp.removeStyleName("correct");
      resp.setText("Scoring... please wait.");

    }
  }

  /**
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @param result
   */
  private void setAudioTag(String result) {
    playback.setWidget(new HTML(audioTag.getAudioTag(result)));
  }

  private void showAudioValidity(AudioAnswer.Validity result, ExerciseQuestionState questionState, Panel outer) {
    check.setVisible(false);
    if (result == AudioAnswer.Validity.OK) {
      if (!controller.isAutoCRTMode()) check.setUrl(IMAGES_CHECKMARK);
      check.setAltText("Audio Saved");
      questionState.recordCompleted(outer);
    }
    else {
      if (!controller.isAutoCRTMode()) check.setUrl(IMAGES_REDX_PNG);
      check.setAltText("Audio Invalid");
      questionState.recordIncomplete(outer);

      showPopup(result.getPrompt());
    }
    check.setVisible(true);
  }

  private void showPopup(String toShow) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(getRecordButton());
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }
}
