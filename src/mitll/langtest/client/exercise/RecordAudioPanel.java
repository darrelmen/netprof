package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.Exercise;

/**
 * An ASR scoring panel with a record button.
 */
public class RecordAudioPanel extends AudioPanel {
  private final int index;

  private PostAudioRecordButton postAudioRecordButton;
  private PlayAudioPanel playAudioPanel;
  protected Panel exercisePanel;

  public Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  public Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  protected Exercise exercise;

  /**
   *
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param showSpectrogram
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public RecordAudioPanel(Exercise exercise, ExerciseController controller, Panel widgets,
                          LangTestDatabaseAsync service, int index, boolean showSpectrogram) {
    super(service,
      // use full screen width
      // use keyboard
      controller, showSpectrogram,null);
    this.exercisePanel = widgets;
    this.index = index;
    this.exercise = exercise;
    addWidgets(null);
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
   * @param toAdd
   * @return
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
    WaveformPostAudioRecordButton myPostAudioRecordButton = makePostAudioRecordButton();
    postAudioRecordButton = myPostAudioRecordButton;
    playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, exercisePanel);
    myPostAudioRecordButton.setPlayAudioPanel(playAudioPanel);

    return playAudioPanel;
  }

  protected WaveformPostAudioRecordButton makePostAudioRecordButton() {
    return new WaveformPostAudioRecordButton(exercise, controller, exercisePanel, this, service, index, true) {
      @Override
      public void startRecording() {
        super.startRecording();
        showStart();
      }

      @Override
      public void stopRecording() {
        super.stopRecording();
        showStop();
      }

      @Override
      public void flip(boolean first) {
        flipRecordImages(first);
      }
    };
  }

  protected void showStop() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
  }

  protected void showStart() {
    recordImage1.setVisible(true);
  }

  protected void flipRecordImages(boolean first) {
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  public Button getButton() { return postAudioRecordButton; }

  private class MyPlayAudioPanel extends PlayAudioPanel {
    public MyPlayAudioPanel(Image recordImage1, Image recordImage2, final Panel panel) {
      super(RecordAudioPanel.this.soundManager, new PlayListener() {
        public void playStarted() {
          if (panel instanceof BusyPanel) {
            ((BusyPanel)panel).setBusy(true);
          }
          postAudioRecordButton.setEnabled(false);
        }

        public void playStopped() {
          if (panel instanceof BusyPanel) {
            ((BusyPanel)panel).setBusy(false);
          }
          postAudioRecordButton.setEnabled(true);
        }
      });
      add(recordImage1);
      recordImage1.setVisible(false);
      add(recordImage2);
      recordImage2.setVisible(false);
      //System.out.println("MyPlayAudioPanel : adding images to " + getElement().getId());
    }

    /**
     * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI)
     */
    @Override
    protected void addButtons() {
      if (postAudioRecordButton == null) System.err.println("huh? postAudioRecordButton is null???");
      else add(postAudioRecordButton);
      super.addButtons();
    }
  }
}
