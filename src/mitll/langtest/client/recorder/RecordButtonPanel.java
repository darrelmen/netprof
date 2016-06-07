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
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.scoring.AudioContext;

import java.util.Collection;
import java.util.Map;

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
public class RecordButtonPanel implements RecordButton.RecordingListener {
  protected final RecordButton recordButton;
  private final LangTestDatabaseAsync service;
  private final ExerciseController controller;
  private final String exerciseID;
  private final int index;
  private int reqid = 0;
  private Panel panel;
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  private boolean doFlashcardAudio = false;
  private boolean allowAlternates = false;

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#FlashcardRecordButtonPanel
   */
  protected RecordButtonPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                              final String exerciseID, final int index,
                              boolean doFlashcardAudio, String audioType, String recordButtonTitle
      , Map<String, Collection<String>> typeToSelection) {
    this.service = service;
    this.controller = controller;
    this.exerciseID = exerciseID;
    this.index = index;
    this.doFlashcardAudio = doFlashcardAudio;
    layoutRecordButton(recordButton = makeRecordButton(controller, recordButtonTitle));
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
  private void layoutRecordButton(Widget button) {
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

  /**
   * TODO : add timeSpent, typeToSelection
   *
   * @param outer
   * @param tries
   * @param base64EncodedWavFile
   */
  private void postAudioFile(final Panel outer, final int tries, final String base64EncodedWavFile) {
    //System.out.println("RecordButtonPanel : postAudioFile " );
    final long then = System.currentTimeMillis();
    reqid++;
    // List<Integer> compressed = LZW.compress(base64EncodedWavFile);
    String device = controller.getBrowserInfo();
    final int len = base64EncodedWavFile.length();

    AudioContext audioContext = new AudioContext(reqid, controller.getUser(), exerciseID, index, getAudioType());

    service.writeAudioFile(base64EncodedWavFile,
        audioContext,
        controller.usingFlashRecorder(), "browser", device,
        doFlashcardAudio,
        true, false, allowAlternates, new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            controller.logException(caught);
            if (tries > 0) {
              postAudioFile(outer, tries - 1, base64EncodedWavFile); // TODO : try one more time...  ???
            } else {
              recordButton.setEnabled(true);
              // receivedAudioFailure();
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

            service.addRoundTrip(result.getResultID(), (int) diff, new AsyncCallback<Void>() {
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
              logMessage("posted " + getLog(then));
            }
          }

          private String getLog(long then) {
            long now = System.currentTimeMillis();
            long diff = now - then;
            return "audio for user " + controller.getUser() + " for exercise " + exerciseID + " took " + diff + " millis to post " +
                len + " characters or " + (len / diff) + " char/milli";
          }
        });
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

  /**
   * Like not what you want - should be based on tab it's recorded in.
   * @return
   */
  private AudioType getAudioType() {
    return controller.getAudioType();
  }

  public Widget getRecordButton() {
    return recordButton;
  }

  public RecordButton getRealRecordButton() {
    return recordButton;
  }

  protected void receivedAudioAnswer(AudioAnswer result, final Panel outer) {
  }

  public void setAllowAlternates(boolean allowAlternates) {
    this.allowAlternates = allowAlternates;
  }
}
