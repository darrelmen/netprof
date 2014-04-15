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
import mitll.langtest.shared.CommonExercise;

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
public class RecordButtonPanel implements RecordButton.RecordingListener {
  protected final RecordButton recordButton;
  private final LangTestDatabaseAsync service;
  private final ExerciseController controller;
  private final CommonExercise exercise;
  private final ExerciseQuestionState questionState;
  private final int index;
  private int reqid = 0;
  private Panel panel;
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  private boolean doFlashcardAudio = false;
  private final String audioType;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  protected RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                              final CommonExercise exercise, final ExerciseQuestionState questionState, final int index,
                              boolean doFlashcardAudio, String audioType, String recordButtonTitle){
    this.service = service;
    this.controller = controller;
    this.exercise = exercise;
    this.questionState = questionState;
    this.index = index;
    this.doFlashcardAudio = doFlashcardAudio;
    layoutRecordButton(recordButton = makeRecordButton(controller,recordButtonTitle));
    this.audioType = audioType;
  }

  protected RecordButton makeRecordButton(ExerciseController controller, String buttonTitle) {
    return new RecordButton(controller.getRecordTimeout(), this, false, false);
  }

  public void flip(boolean first) {
    recordImage1.setVisible(!first);
    recordImage2.setVisible(first);
  }

  /**
   * @see RecordButtonPanel#RecordButtonPanel
   */
  void layoutRecordButton(Widget button) {
    SimplePanel recordButtonContainer = new SimplePanel(button);
    recordButtonContainer.setWidth("75px");
    HorizontalPanel hp = new HorizontalPanel();
    hp.add(recordButtonContainer);
    this.panel = hp;
    panel.getElement().setId("recordButtonPanel");
    addImages();
  }

  public void initRecordButton() {
    recordButton.initRecordButton();
  }

  protected void addImages() {
    panel.add(recordImage1);
    recordImage1.setVisible(false);
    panel.add(recordImage2);
    recordImage2.setVisible(false);
  }

  /**
   * @seex FeedbackRecordPanel#getAnswerWidget
   * @return
   */
  public Panel getPanel() {
     return this.panel;
  }

  public void startRecording() {
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
   * @see #RecordButtonPanel
   */
  public void stopRecording() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
    controller.stopRecording();
    postAudioFile(getPanel(), 1);
  }

  private void postAudioFile(final Panel outer, final int tries) {
    //System.out.println("RecordButtonPanel : postAudioFile " );
    final long then = System.currentTimeMillis();
    reqid++;
    String base64EncodedWavFile = controller.getBase64EncodedWavFile();
    final int len = base64EncodedWavFile.length();
    service.writeAudioFile(base64EncodedWavFile,
      "plan",//exercise.getPlan(),
      exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      false,//!exercise.isPromptInEnglish(),
      audioType,
      doFlashcardAudio,
      true, false, new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          controller.logException(caught);
          if (tries > 0) {
            postAudioFile(outer, tries - 1); // try one more time...
          } else {
            recordButton.setEnabled(true);
            receivedAudioFailure();
            logMessage("failed to post " + getLog(then));
            Window.alert("writeAudioFile : stopRecording : Couldn't post answers for exercise.");
            new ExceptionHandlerDialog(caught);
          }
        }

        public void onSuccess(AudioAnswer result) {
          System.out.println("postAudioFile : onSuccess " + result);

          if (reqid != result.getReqid()) {
            System.out.println("ignoring old answer " + result);
            return;
          }
          recordButton.setEnabled(true);
          receivedAudioAnswer(result, questionState, outer);

          long now = System.currentTimeMillis();
          long diff = now - then;
          if (diff > 1000) {
            logMessage("posted "+ getLog(then));
          }
        }

        private String getLog(long then) {
          long now = System.currentTimeMillis();
          long diff = now - then;
          return "audio for user " + controller.getUser() + " for exercise " + exercise.getID() + " took " + diff + " millis to post " +
            len + " characters or " + (len / diff) + " char/milli";
        }
      });
  }

  void logMessage(String message) {
    service.logMessage(message,new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {}
    });
  }

  protected String getAudioType() { return controller.getAudioType();  }

  public Widget getRecordButton() { return recordButton; }
/*  public Widget getActualRecordButton() { return recordButton; }
  public void setRecordButtonEnabled(boolean val) { recordButton.setEnabled(val); }*/

  protected void receivedAudioAnswer(AudioAnswer result, final ExerciseQuestionState questionState, final Panel outer) {}
  protected void receivedAudioFailure() {}
}
