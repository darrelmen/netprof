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
public class SimpleRecordPanel extends HorizontalPanel {
  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";
  private static final int DELAY_MILLIS = 20000;

  private boolean recording = false;
  private final Image recordImage;
  private final Image stopImage;
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
    final Panel outer = this;

    // make record button
    // make images
    recordImage = new Image("images/record.png");
    recordImage.setAltText("Record");
    stopImage = new Image("images/stop.png");
    stopImage.setAltText("Stop");

    final ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(recordImage);

    record_button.getElement().setId("record_button");
    record_button.setTitle("Record");

    playback.setHeight("30px"); // for audio controls to show
    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (recording) {
          recording = false;
          cancelTimer();
          stopClicked(record_button, controller, service, exercise, index, questionState, outer);
        } else {
          recording = true;
          playback.setWidget(new HTML(""));
          record_button.setResource(stopImage);
          record_button.setTitle("Stop");

          controller.startRecording();
          addRecordingMaxLengthTimeout(record_button, controller, service, exercise, index, questionState, outer);
        }
      }
    });
    SimplePanel recordButtonContainer = new SimplePanel();
    recordButtonContainer.setWidth("75px");
    recordButtonContainer.add(record_button);
    add(recordButtonContainer);

    SimplePanel spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("10px");
    add(spacer);

    // make audio feedback widget
    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");

    spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("100px") ;

    add(spacer);

    add(check);
    check.setVisible(false);

    spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("10px");
    add(spacer);

    // add playback html
    add(playback);
  }

  private Timer recordTimer;

  /**
   * Add a timer to automatically stop recording after 20 seconds.
   *
   *
   * @param record_button
   * @param controller
   * @param service
   * @param exercise
   * @param index
   * @param questionState
   * @param outer
   */
  private void addRecordingMaxLengthTimeout(final ImageAnchor record_button,
                                           final ExerciseController controller,
                                           final LangTestDatabaseAsync service,
                                           final Exercise exercise,
                                           final int index,final ExerciseQuestionState questionState,
                                            final Panel outer ) {
    cancelTimer();
    recordTimer = new Timer() {
      @Override
      public void run() {
        if (recording) {
          recording = false;

          stopClicked(record_button, controller, service, exercise, index, questionState, outer);
        }
      }
    };

    // Schedule the timer to run once in 20 seconds.
    recordTimer.schedule(DELAY_MILLIS);
  }

  private void cancelTimer() {
    if (recordTimer != null) {
      recordTimer.cancel();
    }
  }

  /**
   * Send the audio to the server.<br></br>
   *
   * Audio is a wav file, as a string, encoded base 64  <br></br>
   *
   * Report audio validity and show the audio widget that allows playback.     <br></br>
   *
   * Once audio is posted to the server, two pieces of information come back in the AudioAnswer: the audio validity<br></br>
   *  (false if it's too short, etc.) and a URL to the stored audio on the server. <br></br>
   *   This is used to make the audio playback widget.
   *
   * @see #SimpleRecordPanel
   * @param record_button
   * @param controller
   * @param service
   * @param exercise
   * @param index
   * @param questionState
   * @param outer
   */
  private void stopClicked(ImageAnchor record_button, ExerciseController controller, LangTestDatabaseAsync service,
                           Exercise exercise, int index, final ExerciseQuestionState questionState, final Panel outer) {
    record_button.setResource(recordImage);
    record_button.setTitle("Record");

    controller.stopRecording();

    service.writeAudioFile(controller.getBase64EncodedWavFile()
      ,exercise.getPlan(),exercise.getID(),""+index,""+controller.getUser(),new AsyncCallback<AudioAnswer>() {
      public void onFailure(Throwable caught) {}

      public void onSuccess(AudioAnswer result) {
        showAudioValidity(result.valid, questionState, outer);
        setAudioTag(result.path);
      }
    });
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

  private static class ImageAnchor extends Anchor {
    Image img = null;
    public ImageAnchor() {}
    public void setResource(Image img2) {
      if (this.img != null) {
        DOM.removeChild(getElement(),this.img.getElement());
      }
      this.img = img2;
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }
}
