package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.WavCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 * <p/>
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 *
 *  TODO : make PostAudioRecordButton extend this class.
 */
public abstract class SimplePostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private int reqid = 0;
  protected final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private String textToAlign;
  private String identifier;

  /**
   * @param controller
   * @param service
   * @param recordButtonTitle
   * @param stopButtonTitle
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   */
  public SimplePostAudioRecordButton(final ExerciseController controller, LangTestDatabaseAsync service,
                                     String recordButtonTitle, String stopButtonTitle, String textToAlign, String identifier) {
    super(controller.getRecordTimeout(), true, recordButtonTitle, stopButtonTitle);
    setRecordingListener(this);
    this.controller = controller;
    this.service = service;
    this.textToAlign = textToAlign;
    this.identifier = identifier;
    getElement().setId("PostAudioRecordButton");
    getElement().getStyle().setMarginTop(1, Style.Unit.PX);
    getElement().getStyle().setMarginBottom(1, Style.Unit.PX);
  }

  public void registerForEvents(String identifier) {
    controller.register(this, identifier);
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  public void stopRecording() {
    controller.stopRecording(new WavCallback() {
      @Override
      public void getBase64EncodedWavFile(String bytes) {
        postAudioFile(bytes);
      }
    });
  }

  protected void postAudioFile(String base64EncodedWavFile) {
    reqid++;

    service.getAlignment(base64EncodedWavFile,
        textToAlign,
        identifier,
        reqid,
        getAlignmentCallback());
  }

  protected AsyncCallback<AudioAnswer> getAlignmentCallback() {
    final long then = System.currentTimeMillis();

    return new AsyncCallback<AudioAnswer>() {
      public void onFailure(Throwable caught) {
        long now = System.currentTimeMillis();
        System.out.println("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis");

        logMessage("failed to post audio for " + controller.getUser());// + " exercise " + exercise.getID());
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
    };
  }

  protected boolean shouldAddToAudioTable() {
    return false;
  }

  protected String getAudioType() {
    return controller.getAudioType();
  }

  private Widget getOuter() {
    return this;
  }

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

  public void startRecording() {
    controller.startRecording();
  }

  protected void useInvalidResult(AudioAnswer result) {
    controller.logEvent(this, "recordButton", "N/A", "invalid recording " + result.getValidity());
  }

  public abstract void useResult(AudioAnswer result);

  public boolean hasValidAudio() {
    return validAudio;
  }

}
