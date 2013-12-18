/**
 *
 */
package mitll.langtest.client.recorder;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.flashcard.ScoreFeedback;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Has three parts -- record/stop button, audio validity feedback, and audio html 5 control to playback audio just posted to the server. <br></br>
 * <p/>
 * On click on the stop button, posts audio to the server. <br></br>
 * <p/>
 * Automatically stops recording after 20 seconds.
 *
 * @author Gordon Vidaver
 */
public class AutoCRTRecordPanel extends SimpleRecordPanel {
  protected ScoreFeedback scoreFeedback;
  private SoundFeedback soundFeedback;
  int feedbackWidth;
  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel
   * @see mitll.langtest.client.flashcard.CombinedResponseFlashcard
   */
  public AutoCRTRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                            final Exercise exercise, final ExerciseQuestionState questionState, final int index, int feedbackWidth) {
    super(service, controller, exercise, questionState, index);
    /*getPanel().*/getElement().setId("AutoCRTRecordPanel");
    this.feedbackWidth = feedbackWidth;
  }

  public void setSoundFeedback(SoundFeedback soundFeedback) {  this.soundFeedback = soundFeedback;  }
  public void setScoreFeedback(ScoreFeedback scoreFeedback) {  this.scoreFeedback = scoreFeedback;  }

  @Override
  public void startRecording() {
    super.startRecording();
    scoreFeedback.clearFeedback();
  }

  /**
   * @param result        from server about the audio we just posted
   * @param questionState so we keep track of which questions have been answered
   * @param outer
   * @see RecordButtonPanel#stopRecording()
   */
  @Override
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
    super.receivedAudioAnswer(result, questionState, outer);
    scoreFeedback.showCRTFeedback(result.getScore(), soundFeedback, "Score ", true, feedbackWidth);
  }

  @Override
  public void stopRecording() {
    scoreFeedback.setWaiting();
    super.stopRecording();
  }
}
