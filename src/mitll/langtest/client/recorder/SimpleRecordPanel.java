/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 *
 * Has three parts -- record/stop button, audio validity feedback, and audio html 5 control to playback audio just posted to the server.
 *
 * On click on the stop button, posts audio to the server.
 *
 * Automatically stops recording after 20 seconds.
 *
 * @author Gordon Vidaver
 */
public class SimpleRecordPanel extends RecordButtonPanel {
  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";
  private Image check;
  private SimplePanel playback = new SimplePanel();
  private final AudioTag audioTag = new AudioTag();

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
	public SimpleRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    super(service, controller, exercise, questionState, index);

    playback.setHeight("30px"); // for audio controls to show

    // make audio feedback widget
    addValidityFeedback(index);

    // add playback html
    addPlayback();
  }

  private void addPlayback() {
    // add spacer
    addSpacer();

    add(playback);
  }

  protected void addValidityFeedback(int index) {
    SimplePanel spacer = new SimplePanel();

    // add spacer
    spacer.setHeight("24px");
    spacer.setWidth("110px") ;

    add(spacer);
   // SimplePanel spacer;
    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");
    check.setVisible(false);

    add(check);
  }

  private void addSpacer() {
    SimplePanel spacer;
    spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("10px");
    add(spacer);
  }

  @Override
  protected void nowRecording() {
    playback.setWidget(new HTML(""));
  }

  @Override
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {
    showAudioValidity(result.valid, questionState, outer);
    setAudioTag(result.path);
  }

  /**
   * @see #stopClicked(mitll.langtest.client.recorder.SimpleRecordPanel.ImageAnchor, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.shared.Exercise, int, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @param result
   */
  private void setAudioTag(String result) {
    playback.setWidget(new HTML(audioTag.getAudioTag(result)));
  }

  private void showAudioValidity(Boolean result, ExerciseQuestionState questionState, Panel outer) {
    check.setVisible(false);
    if (result) {
      check.setUrl(IMAGES_CHECKMARK);
      check.setAltText("Audio Saved");
      questionState.recordCompleted(outer);
    }
    else {
      check.setUrl(IMAGES_REDX_PNG);
      check.setAltText("Audio Invalid");
      questionState.recordIncomplete(outer);

      final PopupPanel popupImage = new PopupPanel(true);
      popupImage.add(new HTML("Audio too short, or too quiet.<br/>Please re-record."));
      popupImage.showRelativeTo(this);
    }
    check.setVisible(true);
  }
}
