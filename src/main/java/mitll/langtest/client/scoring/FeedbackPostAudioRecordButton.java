package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;

/**
 * @see AudioPanel#makePlayAudioPanel(Widget, String, String)
 */
class FeedbackPostAudioRecordButton extends PostAudioRecordButton {
  private RecordingAudioListener simpleRecordAudioPanel;

  FeedbackPostAudioRecordButton(int exid,
                                RecordingAudioListener simpleRecordAudioPanel, ExerciseController controller) {
    super(
        exid,
        controller,
        1,
        true,
        "",
        controller.getProps().doClickAndHold() ? "" : "Stop",
        -1,
        true);
    this.simpleRecordAudioPanel = simpleRecordAudioPanel;
  }


  @Override
  public void startRecording() {
    controller.logEvent(this, "RecordButton", getExerciseID(), "startRecording");
    super.startRecording();
    simpleRecordAudioPanel.startRecording();
  }

  @Override
  public void stopRecording(long duration) {
    controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");
    super.stopRecording(duration);
    simpleRecordAudioPanel.stopRecording();
  }

  @Override
  protected void postAudioFile(String base64EncodedWavFile) {
    super.postAudioFile(base64EncodedWavFile);
    simpleRecordAudioPanel.postAudioFile();
  }

  @Override
  protected AudioType getAudioType() {
    return AudioType.LEARN;
  }

  @Override
  public void flip(boolean first) {
    simpleRecordAudioPanel.flip(first);
  }

  @Override
  public void useResult(AudioAnswer result) {
    simpleRecordAudioPanel.useResult(result);
  }

  /**
   * @param result
   * @see RecordingListener#stopRecording(long)
   */
  @Override
  protected void useInvalidResult(AudioAnswer result) {
    super.useInvalidResult(result);
    simpleRecordAudioPanel.useInvalidResult();
  }
}
