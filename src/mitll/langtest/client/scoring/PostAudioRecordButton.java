package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.SimpleRecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 *
 * This is not a widget itself, but a helper.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
 // private static final String RECORD = "Record";
 // private static final String STOP = "Stop";

  private int index;
  private int reqid = 0;
  private Exercise exercise;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;

  /**
   * @see GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @paramx record1
   * @paramx record2
   */
  public PostAudioRecordButton(Exercise exercise, final ExerciseController controller, LangTestDatabaseAsync service,
                               int index, boolean addKeyHandler) {
    super(controller.getRecordTimeout());
    setRecordingListener(this);
    this.index = index;
    this.exercise = exercise;
    this.controller = controller;
    this.service = service;
  }

  @Override
  public void flip(boolean first) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setExercise(Exercise exercise) { this.exercise = exercise; }

  private Widget getOuter() { return this; }
  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
//  @Override
  public void stopRecording() {
    controller.stopRecording();
    reqid++;
    final long then = System.currentTimeMillis();
    service.writeAudioFile(controller.getBase64EncodedWavFile(),
      exercise.getPlan(),
      exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      !exercise.isPromptInEnglish(),
      controller.getAudioType(),
      false, new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          System.out.println("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis");

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

          //System.out.println("PostAudioRecordButton : Got audio answer " + result);
          if (result.reqid != reqid) {
            System.out.println("ignoring old response " + result);
            return;
          }
          if (result.validity == AudioAnswer.Validity.OK) {
            useResult(result);
          } else {
            showPopup(result.validity.getPrompt());
            useInvalidResult(result);
          }
          if (controller.isLogClientMessages()) {
            logRoundtripTime(result, roundtrip);
          }
        }
      });
  }

  private void logRoundtripTime(AudioAnswer result, long roundtrip) {
    String message = "PostAudioRecordButton : (success) User #" + controller.getUser() +
      " post audio took " + roundtrip + " millis, audio dur " +
      result.durationInMillis + " millis, " +
      " " + ((float) roundtrip / (float) result.durationInMillis) + " roundtrip/audio duration ratio.";
    //System.out.println(message);
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
       // Window.alert("trouble contacting server.");
      }

      @Override
      public void onSuccess(Void result) {}
    });
  }

  protected abstract void useInvalidResult(AudioAnswer result);

  public void startRecording() {
    controller.startRecording();
  }

  public abstract void useResult(AudioAnswer result);
}
