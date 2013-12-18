package mitll.langtest.client.recorder;

import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
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
public class RecordButtonPanel extends HorizontalPanel implements RecordButton.RecordingListener {
  protected RecordButton recordButton;
  protected LangTestDatabaseAsync service;
  protected ExerciseController controller;

  private Exercise exercise;
  private ExerciseQuestionState questionState;
  private int index;
  private int reqid = 0;
  private Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  private boolean doFlashcardAudio = false;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index,
                           boolean doFlashcardAudio){
    this.service = service;
    this.controller = controller;
    this.exercise = exercise;
    this.questionState = questionState;
    this.index = index;
    this.doFlashcardAudio = doFlashcardAudio;
    // make record button
    recordImage1.getElement().setId("recordImage1_"+ index);
    recordImage2.getElement().setId("recordImage2_"+ index);

    layoutRecordButton(recordButton = makeRecordButton(controller));
  }

  protected RecordButton makeRecordButton(ExerciseController controller) {
    return new RecordButton(controller.getRecordTimeout(), this, false);
  }

  public void flip(boolean first) {
    System.out.println("RecordButtonPanel : flip " + first);
    recordImage1.setVisible(!first);
    recordImage2.setVisible(first);
  }

  /**
   * @see RecordButtonPanel#RecordButtonPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseQuestionState, int, boolean)
   */
  private Panel layoutRecordButton(Widget button) {
    SimplePanel recordButtonContainer = new SimplePanel(button);  // TODO : can we remove wrapper?
    recordButtonContainer.setWidth("75px");
    recordButtonContainer.getElement().setId("recordButtonContainer");

    getElement().setId("recordButtonPanel");
    add(recordButtonContainer);
    addImages(this);
    return this;
  }

  protected void addImages(Panel container) {
    container.add(recordImage1);
    recordImage1.setVisible(false);
    container.add(recordImage2);
    recordImage2.setVisible(false);
  }

  public void startRecording() {
//    System.out.println("RecordButtonPanel : startRecording " );

    recordImage1.setVisible(true);
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
   * @see #RecordButtonPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseQuestionState, int, boolean)
   */
  public void stopRecording() {
    //System.out.println("RecordButtonPanel : stopRecording " );

    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
    controller.stopRecording();
    postAudioFile(this, 1);
  }

  private void postAudioFile(final Panel outer, final int tries) {
    //System.out.println("RecordButtonPanel : postAudioFile " );

    reqid++;
    service.writeAudioFile(controller.getBase64EncodedWavFile(),
      exercise.getPlan(),
      exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      !exercise.isPromptInEnglish(),
      controller.getAudioType(),
      doFlashcardAudio,
      new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          controller.logException(caught);
          if (tries > 0) {
            postAudioFile(outer, tries - 1); // try one more time...
          } else {
            recordButton.setEnabled(true);
            receivedAudioFailure();

            Window.alert("writeAudioFile : stopRecording : Couldn't post answers for exercise.");
            new ExceptionHandlerDialog(caught);
          }
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

  public Widget getRecordButton() { return recordButton; }
  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {}
  protected void receivedAudioFailure() {}
}
