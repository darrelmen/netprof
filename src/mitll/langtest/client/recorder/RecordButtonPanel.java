package mitll.langtest.client.recorder;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
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
  public static final String RECORD_PNG = LangTest.LANGTEST_IMAGES +"record.png";
  public static final String STOP_PNG   = LangTest.LANGTEST_IMAGES +"stop.png";
  private final Image recordImage;
  private final Image stopImage;
  private ImageAnchor recordButton;
  private LangTestDatabaseAsync service;
  private ExerciseController controller;
  private Exercise exercise;
  private ExerciseQuestionState questionState;
  private int index;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    final RecordButtonPanel outer = this;
    this.service = service;
    this.controller = controller;
    this.exercise = exercise;
    this.questionState = questionState;
    this.index = index;
    // make record button
    // make images
    recordImage = new Image(RECORD_PNG);
    recordImage.setAltText("Record");
    stopImage = new Image(STOP_PNG);
    stopImage.setAltText("Stop");

    // add record button
    recordButton = new ImageAnchor();
    recordButton.setResource(recordImage);

    recordButton.getElement().setId("record_button");
    recordButton.setTitle("Record");

    RecordButton rb = new RecordButton(recordButton) {
      @Override
      protected void stopRecording() {
        outer.stopRecording();
      }

      @Override
      protected void startRecording() {
        outer.startRecording();
      }

      @Override
      protected void showRecording() {
        outer.showRecording();
      }

      @Override
      protected void showStopped() {
        outer.showStopped();
      }
    };
    SimplePanel recordButtonContainer = new SimplePanel();
    recordButtonContainer.setWidth("75px");
    recordButtonContainer.add(recordButton);
    add(recordButtonContainer);
  }

  protected void startRecording() {
    controller.startRecording();
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
   */
  protected void stopRecording() {
    controller.stopRecording();
    final Panel outer = this;
    service.writeAudioFile(controller.getBase64EncodedWavFile()
        , exercise.getPlan(), exercise.getID(), "" + index, "" + controller.getUser(), controller.isAutoCRTMode(),
        new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            Window.alert("Server error : Couldn't post answers for exercise.");
          }
          public void onSuccess(AudioAnswer result) {
            receivedAudioAnswer(result, questionState, outer);
          }
        });
  }

  protected void showRecording() {
    recordButton.setResource(stopImage);
    recordButton.setTitle("Stop");
  }

  protected void showStopped() {
    recordButton.setResource(recordImage);
    recordButton.setTitle("Record");
  }

  protected Widget getRecordButton() { return recordButton; }

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
