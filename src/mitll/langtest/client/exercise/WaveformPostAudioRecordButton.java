package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
* Created with IntelliJ IDEA.
* User: go22670
* Date: 10/4/13
* Time: 11:46 PM
* To change this template use File | Settings | File Templates.
*/
public class WaveformPostAudioRecordButton extends PostAudioRecordButton {
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

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
   */
  public WaveformPostAudioRecordButton(Exercise exercise,
                                       ExerciseController controller,
                                       Panel widgets,
                                       RecordAudioPanel recordAudioPanel, LangTestDatabaseAsync service, int index) {
    super(exercise, controller, service, index);
    this.recordAudioPanel = recordAudioPanel;
    this.parentPanel = widgets;
    getElement().setId("WaveformPostAudioRecordButton");
  }

  @Override
  public void startRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(true);
    }
    super.startRecording();
    playAudioPanel.setPlayEnabled(false);
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
    recordAudioPanel.getWaveform().setVisible(true);
    recordAudioPanel.getWaveform().setUrl(LangTest.LANGTEST_IMAGES + "animated_progress.gif");

    super.stopRecording();

    playAudioPanel.setPlayEnabled(true);
  }

  @Override
  public void useResult(AudioAnswer result) {
    recordAudioPanel.getImagesForPath(wavToMP3(result.path));
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordCompleted(recordAudioPanel);
    }
  }

  @Override
  protected void useInvalidResult(AudioAnswer result) {
    recordAudioPanel.getWaveform().setVisible(false);
    recordAudioPanel.getSpectrogram().setVisible(false);
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordIncomplete(recordAudioPanel);
    }
  }

  public void setPlayAudioPanel(PlayAudioPanel playAudioPanel) {
    this.playAudioPanel = playAudioPanel;
  }

  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }
}
