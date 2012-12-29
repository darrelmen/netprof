package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostAudioRecordButton extends RecordButton {
  private static final String RECORD = "record";
  private static final String STOP = "stop";
  private static final int AUTO_STOP_DELAY = 15000; // millis

  private int index;
  private int reqid = 0;
  private final Exercise exercise;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;

  public PostAudioRecordButton(Exercise exercise, ExerciseController controller, LangTestDatabaseAsync service, int index) {
    super(new Button(RECORD), AUTO_STOP_DELAY);

    this.index = index;
    this.exercise = exercise;
    this.controller = controller;
    this.service = service;
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  @Override
  protected void stopRecording() {
    controller.stopRecording();
    reqid++;
    service.writeAudioFile(controller.getBase64EncodedWavFile()
        , exercise.getPlan(), exercise.getID(),
        "" + index, "" + controller.getUser(),
        false, reqid, new AsyncCallback<AudioAnswer>() {
      public void onFailure(Throwable caught) {
        showPopup(AudioAnswer.Validity.INVALID.getPrompt());
      }

      /**
       * Feedback for when audio isn't valid for some reason.
       * @param toShow
       */
      private void showPopup(String toShow) {
        final PopupPanel popupImage = new PopupPanel(true);
        popupImage.add(new HTML(toShow));
        popupImage.showRelativeTo(getRecord());
        Timer t = new Timer() {
          @Override
          public void run() {
            popupImage.hide();
          }
        };
        t.schedule(3000);
      }

      public void onSuccess(AudioAnswer result) {
        System.out.println("PostAudioRecordButton : Got audio answer " + result);
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
      }
    });
  }

  protected void useInvalidResult(AudioAnswer result) {}

  @Override
  protected void startRecording() {
    controller.startRecording();
  }

  /**
   * So we don't want the button changing width when we change the text.
   */
  @Override
  protected void showRecording() {
    int w = getRecord().getOffsetWidth();
    ((Button) getRecord()).setText(STOP);
    if (getRecord().getOffsetWidth() < w) getRecord().setWidth(w + "px");
  }

  @Override
  protected void showStopped() {
    ((Button) getRecord()).setText(RECORD);
  }

  public void useResult(AudioAnswer result) {}
}
