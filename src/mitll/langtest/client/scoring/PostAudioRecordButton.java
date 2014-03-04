package mitll.langtest.client.scoring;

import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 *
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private static final String WARNING = "Scores are only meaningful if you read the words as they are written.<br/> " +
    "Saying something different or adding/omitting words would make the score meaningless and inaccurate.";
  private boolean validAudio = false;
  private int index;
  private int reqid = 0;
  private Exercise exercise;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final boolean recordInResults;

  /**
   * @see GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param recordInResults
   */
  public PostAudioRecordButton(Exercise exercise, final ExerciseController controller, LangTestDatabaseAsync service,
                               int index, boolean recordInResults) {
    super(controller.getRecordTimeout(), false, true);
    setRecordingListener(this);
    this.index = index;
    this.exercise = exercise;
    this.controller = controller;
    this.service = service;
    this.recordInResults = recordInResults;
    getElement().setId("PostAudioRecordButton");
    doOneTimeWarning();
  }

  public void doOneTimeWarning() {
    if (showMessage()) {
      final DecoratedPopupPanel commentPopup = new DecoratedPopupPanel();
      commentPopup.setAutoHideEnabled(true);

      Panel hp = new HorizontalPanel();
      //  Label child = new Label(WARNING);
      //  child.setType(LabelType.WARNING);
      HTML child = new HTML(WARNING);
      hp.add(child);

      commentPopup.add(hp);
      final Widget outer = this;
      addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          if (showMessage()) {
            commentPopup.showRelativeTo(outer);
          }
        }
      });

      commentPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          if (Storage.isLocalStorageSupported()) {
            Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

            localStorageIfSupported.setItem(getLocalStorageKey(), "shown");
            if (showMessage()) {
              System.err.println("----------------> huh? should not show again");
            }
          }
        }
      });
    }
  }

  private boolean showMessage() {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();

      String memory = localStorageIfSupported.getItem(getLocalStorageKey());
      return (memory == null);
    }
    return false;
  }

  private String getLocalStorageKey() {
    return "PostAudioRecordButton_" + controller.getLanguage() + "_" + controller.getUser();
  }

  @Override
  public void flip(boolean first) {}

  public void setExercise(Exercise exercise) { this.exercise = exercise; }
  public Exercise getExercise() { return exercise; }

  private Widget getOuter() { return this; }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
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
      false, recordInResults, new AsyncCallback<AudioAnswer>() {
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

          System.out.println("PostAudioRecordButton : Got audio answer " + result);
          if (result.reqid != reqid) {
            System.out.println("ignoring old response " + result);
            return;
          }
          if (result.validity == AudioAnswer.Validity.OK) {
            validAudio = true;
            useResult(result);
          } else {
            validAudio = false;
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
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {}
    });
  }

  public void startRecording() {  controller.startRecording();  }

  protected abstract void useInvalidResult(AudioAnswer result);
  public abstract void useResult(AudioAnswer result);

  public boolean hasValidAudio() { return validAudio; }

}
