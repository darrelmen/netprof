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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;

import java.util.logging.Logger;

/**
 * @see SimpleRecordAudioPanel#makePlayAudioPanel
 */
class FeedbackPostAudioRecordButton extends PostAudioRecordButton {
 // private final Logger logger = Logger.getLogger("FeedbackPostAudioRecordButton");

  private static final String STOP = "Stop";
  private static final String RECORD_BUTTON = "RecordButton";
  private final RecordingAudioListener simpleRecordAudioPanel;

  /**
   * @param exid
   * @param simpleRecordAudioPanel
   * @param controller
   * @see NoFeedbackRecordAudioPanel#makePlayAudioPanel
   */
  FeedbackPostAudioRecordButton(int exid, RecordingAudioListener simpleRecordAudioPanel, ExerciseController controller) {
    super(exid,
        controller,
        "",
        controller.getProps().doClickAndHold() ? "" : STOP,
        -1
    );
    this.simpleRecordAudioPanel = simpleRecordAudioPanel;
    setSize(ButtonSize.LARGE);

    getElement().setId("FeedbackPostAudioRecordButton" + exid);
  }

  @Override
  public void startRecording() {
    super.startRecording();
    simpleRecordAudioPanel.startRecording();

    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "startRecording");
  }

  /**
   * @param duration
   * @param abort
   * @return
   * @see RecordButton#stop(long, boolean)
   */
  @Override
  public boolean stopRecording(long duration, boolean abort) {
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "stopRecording");
    boolean b = super.stopRecording(duration, abort);
    if (b) {
      simpleRecordAudioPanel.stopRecording();
    }
    return b;
  }

  @Override
  protected AudioType getAudioType() {
    return AudioType.LEARN;
  }

  @Override
  protected int getDialogSessionID() {
    return simpleRecordAudioPanel.getDialogSessionID();
  }

  /**
   * @param result
   * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
   */
  @Override
  public void useResult(AudioAnswer result) {
    simpleRecordAudioPanel.useResult(result);
  }

  /**
   * @param exid
   * @param validity
   * @param dynamicRange
   * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
   * @see RecordingListener#stopRecording(long, boolean)
   */
  @Override
  public void useInvalidResult(int exid, Validity validity, double dynamicRange) {
    // logger.info("useInvalidResult " + validity);
    super.useInvalidResult(exid, validity, dynamicRange);
    simpleRecordAudioPanel.useInvalidResult(exid, validity == Validity.OK);
  }

  /**
   * @param validity
   * @see PostAudioRecordButton#startRecording
   * @see #gotPacketResponse
   * @see RecordingListener#stopRecording(long, boolean)
   */
  @Override
  public void usePartial(StreamResponse validity) {
    simpleRecordAudioPanel.usePartial(validity);
  }

  @Override
  public void gotAbort() {
    simpleRecordAudioPanel.gotAbort();
  }

  /**
   * @see PostAudioRecordButton#onPostFailure(long, int, String)
   */
  @Override
  protected void onPostFailure() {
    simpleRecordAudioPanel.onPostFailure();
  }

  /**
   * @see #stopRecording(long, boolean)
   */
  protected void gotShortDurationRecording() {
    // logger.info("gotShortDurationRecording");
    simpleRecordAudioPanel.gotShortDurationRecording();
  }
}
