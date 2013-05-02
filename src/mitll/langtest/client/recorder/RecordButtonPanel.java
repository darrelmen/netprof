package mitll.langtest.client.recorder;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.ExceptionHandlerDialog;
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
public class RecordButtonPanel {
  protected static final String RECORD_PNG = LangTest.LANGTEST_IMAGES +"record.png";
  private static final String STOP_PNG   = LangTest.LANGTEST_IMAGES +"stop.png";
  private static final String RECORD = "Record";
  private static final String STOP = "Stop";
  protected final Image recordImage;
  protected final Image stopImage;
  protected ImageAnchor recordButton;
  private LangTestDatabaseAsync service;
  protected ExerciseController controller;
  private Exercise exercise;
  private ExerciseQuestionState questionState;
  private int index;
  private RecordButton rb;
  private int reqid = 0;
  protected Panel panel;

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
    recordImage.setAltText(RECORD);
    stopImage = new Image(STOP_PNG);
    stopImage.setAltText(STOP);

    // add record button
    makeRecordButton();

    recordButton.getElement().setId("record_button");
    recordButton.setTitle(RECORD);

    this.rb = makeRecordButton(controller, outer);
    layoutRecordButton();
  }

  protected RecordButton makeRecordButton(final ExerciseController controller, final RecordButtonPanel outer) {
    return new RecordButton(recordButton, controller.getRecordTimeout()) {
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
  }

  protected Anchor makeRecordButton() {
    recordButton = new ImageAnchor();
    recordButton.setResource(recordImage);
    return recordButton;
  }

  protected void layoutRecordButton() {
    SimplePanel recordButtonContainer = new SimplePanel();
    recordButtonContainer.setWidth("75px");
    recordButtonContainer.add(recordButton);
    HorizontalPanel hp = new HorizontalPanel() {
      @Override
      protected void onUnload() {
        RecordButtonPanel.this.onUnload();
      }
    };
    hp.add(recordButtonContainer);
    this.panel = hp;
  }

  public void onUnload() {
    rb.onUnload();
  }

  public Panel getPanel() {
     return this.panel;
  }

  public void startRecording() {
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
   * @see #RecordButtonPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseQuestionState, int)
   */
  public void stopRecording() {
    System.out.println("stop recording... ");
    controller.stopRecording();
    final Panel outer = getPanel();

    reqid++;
    service.writeAudioFile(controller.getBase64EncodedWavFile(),
      exercise.getPlan(),
      exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      !exercise.promptInEnglish,
      controller.getAudioType(),
      controller.isFlashCard(),
      new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          recordButton.setEnabled(true);
          receivedAudioFailure();

          Window.alert("writeAudioFile : stopRecording : Couldn't post answers for exercise.");
          new ExceptionHandlerDialog(caught);
        }

        public void onSuccess(AudioAnswer result) {
          if (reqid != result.reqid) {
            System.out.println("ignoring old answer " + result);
            return;
          }
          recordButton.setEnabled(true);
          receivedAudioAnswer(result, questionState, outer);
        }
      });
  }

  public void showRecording() {
    recordButton.setResource(stopImage);
    recordButton.setTitle(STOP);
  }

  public void showStopped() {
    recordButton.setResource(recordImage);
    recordButton.setTitle(RECORD);
  }

  public FocusWidget getRecordButton() { return recordButton; }

  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {}
  protected void receivedAudioFailure() {}

  public static class ImageAnchor extends Anchor {
    private Image img = null;
    public ImageAnchor() {}
    //public void show() { setVisible(true); }
    //public void hide() { setVisible(false); }
    public void setResource(Image img2) {
      if (this.img != null) {
        DOM.removeChild(getElement(), this.img.getElement());
      }
      this.img = img2;
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }
}
