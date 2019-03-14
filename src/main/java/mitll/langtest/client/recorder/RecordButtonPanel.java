/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.recorder;

import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import org.jetbrains.annotations.NotNull;

import static mitll.langtest.client.scoring.PostAudioRecordButton.MIN_DURATION;

public abstract class RecordButtonPanel implements RecordButton.RecordingListener {
  //private final Logger logger = Logger.getLogger("RecordButtonPanel");

  protected final RecordButton recordButton;
  private final ExerciseController controller;
  private Panel panel;
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));

  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#FlashcardRecordButtonPanel
   */
  protected RecordButtonPanel(final ExerciseController controller, String recordButtonTitle) {
    this.controller = controller;
    layoutRecordButton(recordButton = makeRecordButton(controller, recordButtonTitle));
  }

  protected RecordButton makeRecordButton(ExerciseController controller, String buttonTitle) {
    return new RecordButton(controller.getRecordTimeout(), this, false, controller.getProps());
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
    // panel.getElement().setId("recordButtonPanel");
    addImages();
  }

  public void initRecordButton() {
    recordButton.initRecordButton();
  }

  protected void addImages() {
    panel.add(recordImage1);
    recordImage1.setVisible(false);
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#start()
   */
  public void startRecording() {
    if (tooltip != null) tooltip.hide();
    recordImage1.setVisible(true);
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
   * @param abort
   * @return true if valid duration
   * @see #RecordButtonPanel
   */
  public boolean stopRecording(long duration, boolean abort) {
    recordImage1.setVisible(false);

    // logger.info("stopRecording : got stop recording " + duration);
    if (duration > MIN_DURATION) {
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

  @NotNull
  protected String getDeviceType() {
    return "browser";
  }

  protected String getDevice() {
    return controller.getBrowserInfo();
  }

  public Widget getRecordButton() {
    return recordButton;
  }

  public RecordButton getRealRecordButton() {
    return recordButton;
  }

  protected void postedAudio() {
  }

  /**
   * @param result
   * @seex #onPostSuccess
   */
  protected abstract void receivedAudioAnswer(AudioAnswer result);

  public void setAllowAlternates(boolean allowAlternates) {
    /*this.allowAlternates = allowAlternates;*/
  }
}
