package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 *
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private final int index;
  private int reqid = 0;
  private CommonExercise exercise;
  protected final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final boolean recordInResults;
  private final String audioType;
  /**
   * @see GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param recordInResults
   * @param audioType
   * @param recordButtonTitle
   * @param stopButtonTitle
   */
  public PostAudioRecordButton(CommonExercise exercise, final ExerciseController controller, LangTestDatabaseAsync service,
                               int index, boolean recordInResults, String audioType, String recordButtonTitle, String stopButtonTitle) {
    super(controller.getRecordTimeout(), true, true, recordButtonTitle, stopButtonTitle);
    setRecordingListener(this);
    this.index = index;
    this.exercise = exercise;
    this.controller = controller;
    this.service = service;
    this.recordInResults = recordInResults;
    this.audioType = audioType;
  //  System.out.println("PostAudioRecordButton : Audio type " + audioType);
    getElement().setId("PostAudioRecordButton");
    controller.register(this, exercise.getID());
  }

  public void setExercise(CommonExercise exercise) { this.exercise = exercise; }
  public CommonExercise getExercise() { return exercise; }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  public void stopRecording() {
    controller.stopRecording();
    reqid++;
    final long then = System.currentTimeMillis();
    service.writeAudioFile(controller.getBase64EncodedWavFile(),
      "plan",//exercise.getPlan(),
      exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      true,//!exercise.isPromptInEnglish(),
      audioType,
      false, recordInResults,
      new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          System.out.println("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis");

          logMessage("failed to post audio for " + controller.getUser() + " exercise " + exercise.getID());
          showPopup(AudioAnswer.Validity.INVALID.getPrompt());
        }

        /**
         * Feedback for when audio isn't valid for some reason.
         * @param toShow
         */
        private void showPopup(String toShow) {
          final PopupPanel popupImage = new PopupPanel(true);
          popupImage.add(new HTML(toShow));
          popupImage.showRelativeTo(getOuter());
          Timer t = new Timer() {
            @Override
            public void run() {
              popupImage.hide();
            }
          };
          t.schedule(3000);
        }

        public void onSuccess(AudioAnswer result) {
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          System.out.println("PostAudioRecordButton : Got audio answer " + result);
          if (result.getReqid() != reqid) {
            System.out.println("ignoring old response " + result);
            return;
          }
          if (result.getValidity() == AudioAnswer.Validity.OK) {
            validAudio = true;
            useResult(result);
          } else {
            validAudio = false;
            showPopup(result.getValidity().getPrompt());
            useInvalidResult(result);
          }
          if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
            logRoundtripTime(result, roundtrip);
          }
        }
      });
  }

  protected String getAudioType() { return controller.getAudioType(); }

  private Widget getOuter() { return this; }

  private void logRoundtripTime(AudioAnswer result, long roundtrip) {
    String message = "PostAudioRecordButton : (success) User #" + controller.getUser() +
      " post audio took " + roundtrip + " millis, audio dur " +
      result.getDurationInMillis() + " millis, " +
      " " + ((float) roundtrip / (float) result.getDurationInMillis()) + " roundtrip/audio duration ratio.";
    logMessage(message);
  }

  private void logMessage(String message) {
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  public void startRecording() {  controller.startRecording();  }

  protected abstract void useInvalidResult(AudioAnswer result);
  public abstract void useResult(AudioAnswer result);

  public boolean hasValidAudio() { return validAudio; }

}
