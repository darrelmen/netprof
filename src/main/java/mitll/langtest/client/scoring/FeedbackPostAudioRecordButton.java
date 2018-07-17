package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;

/**
 * @see SimpleRecordAudioPanel#makePlayAudioPanel
 */
class FeedbackPostAudioRecordButton extends PostAudioRecordButton {
  //private final Logger logger = Logger.getLogger("FeedbackPostAudioRecordButton");
  private static final String STOP = "Stop";
  private static final String RECORD_BUTTON = "RecordButton";

  private final RecordingAudioListener simpleRecordAudioPanel;

  /**
   * @see SimpleRecordAudioPanel#makePlayAudioPanel
   * @param exid
   * @param simpleRecordAudioPanel
   * @param controller
   */
  FeedbackPostAudioRecordButton(int exid,
                                RecordingAudioListener simpleRecordAudioPanel, ExerciseController controller) {
    super(
        exid,
        controller,
        1,
        true,
        "",
        controller.getProps().doClickAndHold() ? "" : STOP,
        -1,
        true);
    this.simpleRecordAudioPanel = simpleRecordAudioPanel;
    setSize(ButtonSize.LARGE);

    getElement().setId("FeedbackPostAudioRecordButton"+ exid);

  }

  @Override
  public void startRecording() {
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "startRecording");
    super.startRecording();
    simpleRecordAudioPanel.startRecording();
  }

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

  @Override
  public void flip(boolean first) {
    simpleRecordAudioPanel.flip(first);
  }

  /**
   * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
   * @param result
   */
  @Override
  public void useResult(AudioAnswer result) {  simpleRecordAudioPanel.useResult(result);  }

  /**
   * @param result
   * @see RecordingListener#stopRecording(long)
   */
  @Override
  protected void useInvalidResult(AudioAnswer result) {
    super.useInvalidResult(result);
    simpleRecordAudioPanel.useInvalidResult(result.isValid());
  }

  protected void gotShortDurationRecording() {
    simpleRecordAudioPanel.gotShortDurationRecording();
  }
}
