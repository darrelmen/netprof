/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.recorder;

import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.WavCallback;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
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
  private final int index;
  private int reqid = 0;
  private Panel panel;
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  private boolean doFlashcardAudio = false;
  private boolean allowAlternates = false;
  private final String audioType;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#FlashcardRecordButtonPanel(mitll.langtest.client.flashcard.AudioAnswerListener, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.CommonExercise, int, String)
   */
  protected RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                              final CommonExercise exercise, final int index,
                              boolean doFlashcardAudio, String audioType, String recordButtonTitle){
    this.service = service;
    this.controller = controller;
    this.exercise = exercise;
    this.index = index;
    this.doFlashcardAudio = doFlashcardAudio;
    layoutRecordButton(recordButton = makeRecordButton(controller,recordButtonTitle));
    this.audioType = audioType;
  }

  protected RecordButton makeRecordButton(ExerciseController controller, String buttonTitle) {
    return new RecordButton(controller.getRecordTimeout(), this, false, controller.getProps());
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

  /**
   * @see mitll.langtest.client.recorder.RecordButton#start()
   */
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
    controller.stopRecording(new WavCallback() {
      @Override
      public void getBase64EncodedWavFile(String bytes) {
        postAudioFile(getPanel(), 1, bytes);
      }
    });
  }

  protected void postAudioFile(final Panel outer, final int tries, final String base64EncodedWavFile) {
    //System.out.println("RecordButtonPanel : postAudioFile " );
    final long then = System.currentTimeMillis();
    reqid++;
   // List<Integer> compressed = LZW.compress(base64EncodedWavFile);
    String device = controller.getBrowserInfo();
    final int len = base64EncodedWavFile.length();
    service.writeAudioFile(base64EncodedWavFile,
        exercise.getID(),
      index,
      controller.getUser(),
      reqid,
      false,
      audioType,
      doFlashcardAudio,
      true, false, controller.usingFlashRecorder(), "browser", device, allowAlternates, new AsyncCallback<AudioAnswer>() {
        public void onFailure(Throwable caught) {
          controller.logException(caught);
          if (tries > 0) {
            postAudioFile(outer, tries - 1, base64EncodedWavFile); // TODO : try one more time...  ???
          } else {
            recordButton.setEnabled(true);
            receivedAudioFailure();
            logMessage("failed to post " + getLog(then));
            Window.alert("writeAudioFile : stopRecording : Couldn't post answers for exercise.");
            new ExceptionHandlerDialog(caught);
          }
        }

        public void onSuccess(AudioAnswer result) {
          //System.out.println("postAudioFile : onSuccess " + result);

          if (reqid != result.getReqid()) {
            System.out.println("ignoring old answer " + result);
            return;
          }
          long now = System.currentTimeMillis();
          long diff = now - then;

          service.addRoundTrip(result.getResultID(), (int)diff, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
            }
          });
          recordButton.setEnabled(true);
          receivedAudioAnswer(result, outer);

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
  public RecordButton getRealRecordButton() { return recordButton; }
  protected void receivedAudioAnswer(AudioAnswer result, final Panel outer) {}
  protected void receivedAudioFailure() {}

  public void setAllowAlternates(boolean allowAlternates) {
    this.allowAlternates = allowAlternates;
  }
}
