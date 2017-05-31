package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;

import java.util.logging.Logger;

/**
 * @see AudioPanel#makePlayAudioPanel(Widget, String, String)
 */
class FeedbackPostAudioRecordButton extends PostAudioRecordButton {
  private final Logger logger = Logger.getLogger("FeedbackPostAudioRecordButton");

  private static final String STOP = "Stop";

  private RecordingAudioListener simpleRecordAudioPanel;

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
  }


  @Override
  public void startRecording() {
    controller.logEvent(this, "RecordButton", getExerciseID(), "startRecording");
    super.startRecording();
    simpleRecordAudioPanel.startRecording();
  }

  @Override
  public boolean stopRecording(long duration) {
    controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");
    boolean b = super.stopRecording(duration);
    if (b) {
      simpleRecordAudioPanel.stopRecording();
    }
    return b;
  }

/*  @Override
  protected void postAudioFile(String base64EncodedWavFile) {
    super.postAudioFile(base64EncodedWavFile);
    simpleRecordAudioPanel.postAudioFile();
  }*/

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
