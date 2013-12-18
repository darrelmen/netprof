/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
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
  private static final String IMAGES_CHECKMARK = LangTest.LANGTEST_IMAGES +"checkmark32.png";
  private static final String IMAGES_REDX_PNG  = LangTest.LANGTEST_IMAGES +"redx32.png";
  private static final int PLAYBACK_WIDTH = 300;
  public static final int POPUP_TIMEOUT = 3000;
  private Image check;
  private SimplePanel playback = new SimplePanel();
  private final AudioTag audioTag = new AudioTag();

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
	public SimpleRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    super(service, controller, exercise, questionState, index, false);
    setSpacing(10);

    getElement().setId("SimpleRecordPanel_panel");

    // make audio feedback widget
    addValidityFeedback(index);

    // add playback html
    playback.setHeight("30px"); // for audio controls to show
    playback.addStyleName("leftFiveMargin");
    playback.setWidth(PLAYBACK_WIDTH + "px");
    playback.getElement().setId("SimpleRecordPanel_playback");
    add(playback);
  }

  protected void addValidityFeedback(int index) {
    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");
    check.setVisible(false);

    add(check);
  }

  @Override
  public void startRecording() {
    super.startRecording();
    playback.setWidget(new HTML(""));
    if (check != null) check.setVisible(false);
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param result from server about the audio we just posted
   * @param questionState so we keep track of which questions have been answered
   * @param outer
   */
  @Override
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
    showAudioValidity(result.validity, questionState, outer);
    setAudioTag(result.path);
  }

  /**
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @param result
   */
  private void setAudioTag(String result) {
    playback.setWidget(new HTML(audioTag.getAudioTag(result)));
  }

  private void showAudioValidity(AudioAnswer.Validity result, ExerciseQuestionState questionState, Widget outer) {
    check.setVisible(false);
    if (result == AudioAnswer.Validity.OK) {
      check.setUrl(IMAGES_CHECKMARK);
      check.setAltText("Audio Saved");
      if (questionState != null) {
        questionState.recordCompleted(outer);
      }
    }
    else {
      check.setUrl(IMAGES_REDX_PNG);
      check.setAltText("Audio Invalid");
      if (questionState != null) {
        questionState.recordIncomplete(outer);
      }

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
    t.schedule(POPUP_TIMEOUT);
  }
}
