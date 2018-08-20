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
  private final Logger logger = Logger.getLogger("FeedbackPostAudioRecordButton");

  private static final String STOP = "Stop";
  private static final String RECORD_BUTTON = "RecordButton";
  private static final int DEFAULT_INDEX = 1;

  private final RecordingAudioListener simpleRecordAudioPanel;

  /**
   * @param exid
   * @param simpleRecordAudioPanel
   * @param controller
   * @see NoFeedbackRecordAudioPanel#makePlayAudioPanel
   */
  FeedbackPostAudioRecordButton(int exid, RecordingAudioListener simpleRecordAudioPanel,
                                ExerciseController controller) {
    super(
        exid,
        controller,
        DEFAULT_INDEX,
        true,
        "",
        controller.getProps().doClickAndHold() ? "" : STOP,
        -1,
        true);
    this.simpleRecordAudioPanel = simpleRecordAudioPanel;
    setSize(ButtonSize.LARGE);

    getElement().setId("FeedbackPostAudioRecordButton" + exid);
  }

  @Override
  public void startRecording() {
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "startRecording");
    super.startRecording();

    simpleRecordAudioPanel.startRecording();

  }

  /**
   * @see RecordButton#stop(long)
   * @param duration
   * @return
   */
  @Override
  public boolean stopRecording(long duration) {
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "stopRecording");
    boolean b = super.stopRecording(duration);
    if (b) {
      simpleRecordAudioPanel.stopRecording();
    }
    return b;
  }

  @Override
  protected AudioType getAudioType() {
    return AudioType.LEARN;
  }

  /**
   * @see RecordButton#flipImage
   * @param first
   */
  @Override
  public void flip(boolean first) {
    simpleRecordAudioPanel.flip(first);
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
   * @param validity
   * @param dynamicRange
   * @see RecordingListener#stopRecording(long)
   */
  @Override
  protected void useInvalidResult(Validity validity, double dynamicRange) {
    logger.info("useInvalidResult " + validity);
    super.useInvalidResult(validity, dynamicRange);
    simpleRecordAudioPanel.useInvalidResult(validity == Validity.OK);
  }

  protected void gotShortDurationRecording() {
    simpleRecordAudioPanel.gotShortDurationRecording();
  }
}
