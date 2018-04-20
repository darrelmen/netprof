/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.recorder;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ExceptionHandlerDialog.getExceptionAsString;
import static mitll.langtest.client.scoring.PostAudioRecordButton.MIN_DURATION;

/**
 * Just a single record button for the UI component.
 * <br></br>
 * Posts audio when stop button is clicked.
 * <br></br>
 * Calls {@see #receivedAudioAnswer} when the audio has been posted to the server.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/29/12
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class RecordButtonPanel implements RecordButton.RecordingListener {
  private final Logger logger = Logger.getLogger("RecordButtonPanel");
  //public static final String NETWORK_ISSUE = "Network issue : Couldn't post audio. Please try again.";

  protected final RecordButton recordButton;
  private final ExerciseController controller;
  private final int exerciseID;
  private final int index;
  private int reqid = 0;
  private Panel panel;
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  private boolean doFlashcardAudio, doAlignment;
  private boolean allowAlternates = false;
  private final AudioType audioType;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#FlashcardRecordButtonPanel
   */
  protected RecordButtonPanel(final ExerciseController controller,
                              final int exerciseID,
                              final int index,
                              boolean doFlashcardAudio,
                              AudioType audioType,
                              String recordButtonTitle,
                              boolean doAlignment) {
    this.controller = controller;
    this.exerciseID = exerciseID;
    this.index = index;
    this.doFlashcardAudio = doFlashcardAudio;
    this.doAlignment = doAlignment;
    this.audioType = audioType;
    layoutRecordButton(recordButton = makeRecordButton(controller, recordButtonTitle));
  }

  protected RecordButton makeRecordButton(ExerciseController controller, String buttonTitle) {
    return new RecordButton(controller.getRecordTimeout(), this, false, controller.getProps());
  }

  /**
   * @param first
   * @see RecordButton#flipImage
   */
  public void flip(boolean first) {
    recordImage1.setVisible(!first);
    recordImage2.setVisible(first);
  }

  /**
   * @see RecordButtonPanel#RecordButtonPanel
   */
  private void layoutRecordButton(Widget button) {
    SimplePanel recordButtonContainer = new SimplePanel(button);
    recordButtonContainer.setWidth("75px");
    Panel hp = new HorizontalPanel();
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
   * @return
   * @seex FeedbackRecordPanel#getAnswerWidget
   */
  private Panel getPanel() {
    return this.panel;
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#start()
   */
  public void startRecording() {
    if (tooltip != null) tooltip.hide();
    recordImage1.setVisible(true);
    controller.startRecording();
  }

  /**
   * Send the audio to the server.<br></br>
   * <p>
   * Audio is a wav file, as a string, encoded base 64  <br></br>
   * <p>
   * Report audio validity and show the audio widget that allows playback.     <br></br>
   * <p>
   * Once audio is posted to the server, two pieces of information come back in the AudioAnswer: the audio validity<br></br>
   * (false if it's too short, etc.) and a URL to the stored audio on the server. <br></br>
   * This is used to make the audio playback widget.
   *
   * @param duration
   * @return true if valid duration
   * @see #RecordButtonPanel
   */
  public boolean stopRecording(long duration) {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);

    // logger.info("stopRecording : got stop recording " + duration);
    if (duration > MIN_DURATION) {
      controller.stopRecording(bytes -> postAudioFile(getPanel(), bytes));
      return true;
    } else {
      initRecordButton();
      showPopup(Validity.TOO_SHORT.getPrompt(), recordButton);
      return false;
    }
  }

  private PopupPanel tooltip;

  protected void showPopup(String html, Widget button) {
    tooltip = new PopupHelper().showPopup(html, button, BootstrapExercisePanel.HIDE_DELAY);
  }

  /**
   * TODO : add timeSpent, typeToSelection
   *
   * @param outer
   * @param base64EncodedWavFile
   * @see #postAudioFile
   * @see #stopRecording
   */
  private void postAudioFile(final Panel outer, final String base64EncodedWavFile) {
    reqid++;

    final int len = base64EncodedWavFile.length();

    AudioContext audioContext = new AudioContext(reqid,
        controller.getUser(),
        controller.getProjectStartupInfo().getProjectid(),
        controller.getProjectStartupInfo().getLanguage(),
        exerciseID,
        index,
        audioType);

    DecoderOptions decoderOptions = new DecoderOptions()
        .setDoDecode(doFlashcardAudio)
        .setDoAlignment(doAlignment)
        .setRecordInResults(true)
        .setRefRecording(false)
        .setAllowAlternates(allowAlternates);

    final long then = System.currentTimeMillis();
    controller.getAudioService().writeAudioFile(base64EncodedWavFile,
        audioContext,
        controller.usingFlashRecorder(),
        getDeviceType(),
        getDevice(),
        decoderOptions,
        new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            onPostFailure(caught, then, len);
          }

          public void onSuccess(AudioAnswer result) {
            onPostSuccess(result, then, outer, len);
          }
        });
  }

  @NotNull
  protected String getDeviceType() {
    return "browser";
  }

  protected String getDevice() {
    String browserInfo = controller.getBrowserInfo();
    logger.info("GetDevice " + browserInfo);
    return browserInfo;
  }

  /**
   * @param result
   * @param then
   * @param outer
   * @param len
   * @see #postAudioFile
   */
  private void onPostSuccess(AudioAnswer result, long then, Panel outer, int len) {
    //System.out.println("postAudioFile : onSuccess " + result);
    if (reqid != result.getReqid()) {
      return;
    }
    long diff = System.currentTimeMillis() - then;

    recordButton.setEnabled(true);
    receivedAudioAnswer(result, outer);

    Scheduler.get().scheduleDeferred(() -> addRoundTrip(result, (int) diff));

    if (diff > 1000) {
      Scheduler.get().scheduleDeferred(() -> logMessage("long round trip : posted " + getLog(then, len), false));
    }
  }

  private void addRoundTrip(AudioAnswer result, int diff) {
    controller.getScoringService().addRoundTrip(result.getResultID(), diff, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("adding round trip to recording", caught);
      }

      @Override
      public void onSuccess(Void result) {
        // logger.info("couldn't post round trip.");
      }
    });
  }

  private void logMessage(String message, boolean sendEmail) {
    controller.getService().logMessage(message, sendEmail, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("couldn't log message " + caught.getMessage());
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  /**
   * @param caught
   * @param then
   * @param len
   * @see #postAudioFile
   */
  private void onPostFailure(Throwable caught, long then, int len) {
    controller.logException(caught);
    recordButton.setEnabled(true);
    String stackTrace = getExceptionAsString(caught);
    logMessage("postAudioFile : failed to post " + getLog(then, len) + "\n" + stackTrace, true);
    //   Window.alert(NETWORK_ISSUE);
    //  new ExceptionHandlerDialog(caught);
    receivedAudioAnswer(new AudioAnswer(), getPanel());
  }


  private String getLog(long then, int len) {
    long now = System.currentTimeMillis();
    long diff = now - then;
    return "audio for user " + controller.getUser() + " for exercise " + exerciseID + " took " + diff + " millis to post " +
        len + " characters or " + (len / diff) + " char/milli";
  }

  public Widget getRecordButton() {
    return recordButton;
  }

  public RecordButton getRealRecordButton() {
    return recordButton;
  }

  /**
   * @param result
   * @param outer
   * @see #onPostSuccess
   */
  protected abstract void receivedAudioAnswer(AudioAnswer result, final Panel outer);

  public void setAllowAlternates(boolean allowAlternates) {
    this.allowAlternates = allowAlternates;
  }
}
