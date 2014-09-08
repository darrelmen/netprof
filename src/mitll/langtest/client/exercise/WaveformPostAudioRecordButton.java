package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;

/**
* Tells playAudioPanel to be enabled/disabled in response to recording states
* User: go22670
* Date: 10/4/13
* Time: 11:46 PM
* To change this template use File | Settings | File Templates.
*/
public class WaveformPostAudioRecordButton extends PostAudioRecordButton {
  private static final String RECORD_BUTTON = "RecordButton";
  private final RecordAudioPanel recordAudioPanel;
  private PlayAudioPanel playAudioPanel;
  private final Panel parentPanel;
  private final String audioType;

  /**
   * @see RecordAudioPanel#makePostAudioRecordButton(String, String)
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton(String, String)
   * @param exercise
   * @param controller
   * @param widgets
   * @param recordAudioPanel
   * @param service
   * @param index
   * @param recordInResults
   * @param playButtonSuffix
   * @param stopButtonText
   */
  public WaveformPostAudioRecordButton(CommonExercise exercise,
                                       ExerciseController controller,
                                       Panel widgets,
                                       RecordAudioPanel recordAudioPanel, LangTestDatabaseAsync service, int index,
                                       boolean recordInResults, String playButtonSuffix, String stopButtonText, String audioType) {
    super(exercise, controller, service, index, recordInResults, playButtonSuffix, stopButtonText);
    this.recordAudioPanel = recordAudioPanel;
    this.parentPanel = widgets;
    getElement().setId("WaveformPostAudioRecordButton_" + index);
    this.audioType = audioType;
    addStyleName("minWidthRecordButton");
  }

  /**
   * So when we're recording reference audio for an item, we want to add the audio to the audio table and not
   * the results table.
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, int, int, int, boolean, String, boolean, boolean, boolean, boolean)
   */
  @Override
  protected boolean shouldAddToAudioTable() {
    return true;
  }

  long then;
  @Override
  public void startRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(true);
    }
    controller.logEvent(this, RECORD_BUTTON, getExercise().getID(), "startRecording");
    then = System.currentTimeMillis();
    super.startRecording();
    setPlayEnabled(false);
  }

  @Override
  public void flip(boolean first) {} // force not to be abstract

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  @Override
  public void stopRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(false);
    }
    long now = System.currentTimeMillis();

    controller.logEvent(this, RECORD_BUTTON, getExercise().getID(), "stopRecording, duration " + (now - then) + " millis");

    recordAudioPanel.getWaveform().setVisible(true);
    recordAudioPanel.getWaveform().setUrl(LangTest.LANGTEST_IMAGES + "animated_progress.gif");

    super.stopRecording();
  }

  /**
   * @see #postAudioFile(String)
   * @return
   */
  @Override
  protected String getAudioType() {
    return audioType;
  }

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @param result
   */
  @Override
  public void useResult(AudioAnswer result) {
    recordAudioPanel.getImagesForPath(result.getPath());
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordCompleted(recordAudioPanel);
    }
    setPlayEnabled(true);
  }

  @Override
  protected void useInvalidResult(AudioAnswer result) {
    super.useInvalidResult(result);

    System.out.println("WaveformPostAudioRecordButton : " + getElement().getId() + " : got invalid result " +result);
    recordAudioPanel.getWaveform().setVisible(false);
    recordAudioPanel.getSpectrogram().setVisible(false);
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordIncomplete(recordAudioPanel);
    }
  //  controller.logEvent(recordAudioPanel.getButton(), "recordButton", getExercise().getID(), "invalid recording " + result.getValidity());

    setPlayEnabled(false);
  }

  void setPlayEnabled(boolean val) {
    //System.out.println("setPlayEnabled -- " + getElement().getId() + " : valid audio ? " + hasValidAudio() + " enable " + val);
    playAudioPanel.setEnabled(val && hasValidAudio());
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String)
   * @param playAudioPanel
   */
  public void setPlayAudioPanel(PlayAudioPanel playAudioPanel) {
    this.playAudioPanel = playAudioPanel;
  }
}
