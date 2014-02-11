package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
* Tells playAudioPanel to be enabled/disabled in response to recording states
* User: go22670
* Date: 10/4/13
* Time: 11:46 PM
* To change this template use File | Settings | File Templates.
*/
public class WaveformPostAudioRecordButton extends PostAudioRecordButton {
  private RecordAudioPanel recordAudioPanel;
  private PlayAudioPanel playAudioPanel;
  private Panel parentPanel;

  /**
   * @see RecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
   * @param exercise
   * @param controller
   * @param widgets
   * @param recordAudioPanel
   * @param service
   * @param index
   * @param recordInResults
   */
  public WaveformPostAudioRecordButton(Exercise exercise,
                                       ExerciseController controller,
                                       Panel widgets,
                                       RecordAudioPanel recordAudioPanel, LangTestDatabaseAsync service, int index,
                                       boolean recordInResults) {
    super(exercise, controller, service, index, recordInResults);
    this.recordAudioPanel = recordAudioPanel;
    this.parentPanel = widgets;
    getElement().setId("WaveformPostAudioRecordButton_" +index);
  }

  @Override
  public void startRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(true);
    }
    super.startRecording();
    setPlayEnabled(false);
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  @Override
  public void stopRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(false);
    }
    recordAudioPanel.getWaveform().setVisible(true);
    recordAudioPanel.getWaveform().setUrl(LangTest.LANGTEST_IMAGES + "animated_progress.gif");

    super.stopRecording();
  }

  @Override
  public void useResult(AudioAnswer result) {
    recordAudioPanel.getImagesForPath(result.path);
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordCompleted(recordAudioPanel);
    }
    setPlayEnabled(true);
  }

  @Override
  protected void useInvalidResult(AudioAnswer result) {
    System.out.println(getElement().getId() + " : got invalid result " +result);
    recordAudioPanel.getWaveform().setVisible(false);
    recordAudioPanel.getSpectrogram().setVisible(false);
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordIncomplete(recordAudioPanel);
    }

    setPlayEnabled(false);
  }

  public void setPlayEnabled(boolean val) {
    //System.out.println("setPlayEnabled -- " + getElement().getId() + " : valid audio ? " + hasValidAudio() );
    playAudioPanel.setEnabled(val && hasValidAudio());
  }

  /**
   * @see mitll.langtest.client.exercise.RecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
   * @param playAudioPanel
   */
  public void setPlayAudioPanel(PlayAudioPanel playAudioPanel) {
    this.playAudioPanel = playAudioPanel;
  }
}
