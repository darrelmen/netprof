package mitll.langtest.client.recorder;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Just a single record button for the UI component.
 * <br></br>
 * Posts audio when stop button is clicked.
 * <br></br>
 * Calls {@see #receivedAudioAnswer} when the audio has been posted to the server.
 *
 * User: go22670
 * Date: 8/29/12
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordButtonPanel extends HorizontalPanel {
  private static final int DELAY_MILLIS = 20000;

  private boolean recording = false;
  private Timer recordTimer;

  private final Image recordImage;
  private final Image stopImage;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    final Panel outer = this;

    // make record button
    // make images
    recordImage = new Image("images/record.png");
    recordImage.setAltText("Record");
    stopImage = new Image("images/stop.png");
    stopImage.setAltText("Stop");

    // add record button
    Widget record_button = getRecordButton(service, controller, exercise, questionState, index, outer);
    SimplePanel recordButtonContainer = new SimplePanel();
    recordButtonContainer.setWidth("75px");
    recordButtonContainer.add(record_button);
    add(recordButtonContainer);
  }


  private Widget getRecordButton(final LangTestDatabaseAsync service, final ExerciseController controller,
                                 final Exercise exercise, final ExerciseQuestionState questionState,
                                 final int index, final Panel outer) {
    final ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(recordImage);

    record_button.getElement().setId("record_button");
    record_button.setTitle("Record");

    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (recording) {
          //System.err.println("got click -- 1");
          recording = false;
          cancelTimer();
          stopClicked(record_button, controller, service, exercise, index, questionState, outer);
        } else {
          //System.err.println("got click -- 2");

          recording = true;
          //playback.setWidget(new HTML(""));
          nowRecording();
          record_button.setResource(stopImage);
          record_button.setTitle("Stop");

          controller.startRecording();
          addRecordingMaxLengthTimeout(record_button, controller, service, exercise, index, questionState, outer);
        }
      }
    });
    return record_button;
  }

  protected void nowRecording() {}

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
   * @see #getRecordButton(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseQuestionState, int, com.google.gwt.user.client.ui.Panel)
   * @see #addRecordingMaxLengthTimeout(mitll.langtest.client.recorder.SimpleRecordPanel.ImageAnchor, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.shared.Exercise, int, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
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
        receivedAudioAnswer(result, questionState, outer);
   /*     showAudioValidity(result.valid, questionState, outer);
        setAudioTag(result.path);*/
      }
    });
  }

  protected void receivedAudioAnswer(AudioAnswer result,  final ExerciseQuestionState questionState, final Panel outer) {}

  private static class ImageAnchor extends Anchor {
    Image img = null;
    public ImageAnchor() {}
    public void setResource(Image img2) {
      if (this.img != null) {
        DOM.removeChild(getElement(), this.img.getElement());
      }
      this.img = img2;
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }
}
