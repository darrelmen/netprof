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
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;

import java.util.List;

/**
 * A waveform record button and a play audio button.
 */
public class RecordAudioPanel extends AudioPanel {
  public static final String RECORD = "Record";
  public static final String STOP = "Stop";
  private final int index;

  private PostAudioRecordButton postAudioRecordButton;
  private PlayAudioPanel playAudioPanel;
  protected final Panel exercisePanel;

  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  protected CommonExercise exercise;

  /**
   *
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param showSpectrogram
   * @param audioType
   * @see mitll.langtest.client.custom.NewUserExercise.CreateFirstRecordAudioPanel#CreateFirstRecordAudioPanel(mitll.langtest.shared.CommonExercise, com.google.gwt.user.client.ui.Panel, boolean)
   */
  public RecordAudioPanel(CommonExercise exercise, ExerciseController controller, Panel widgets,
                          LangTestDatabaseAsync service, int index, boolean showSpectrogram, String audioType) {
    super(service,
      // use full screen width
      // use keyboard
      controller, showSpectrogram,
      null // no gauge panel
      , 1.0f, 23, exercise.getID());
    this.exercisePanel = widgets;
    this.index = index;
    this.exercise = exercise;

    AudioAttribute attribute =
      audioType.equals(Result.AUDIO_TYPE_REGULAR) ? exercise.getRecordingsBy(controller.getUser(), true) :
        audioType.equals(Result.AUDIO_TYPE_SLOW) ? exercise.getRecordingsBy(controller.getUser(), false) : null;
/*    System.out.println("RecordAudioPanel for " + exercise.getID() +
      " audio type " + audioType + " ref " + exercise.getRefAudio() + " path " + attribute);*/
    if (attribute != null) {
      this.audioPath = attribute.getAudioRef();
    }

    addWidgets("", audioType);
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons
   * @param toAdd
   * @param playButtonSuffix
   * @return
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toAdd, String playButtonSuffix, String audioType) {
    WaveformPostAudioRecordButton myPostAudioRecordButton = makePostAudioRecordButton(audioType);
    postAudioRecordButton = myPostAudioRecordButton;

    System.out.println("makePlayAudioPanel : audio type " + audioType + " suffix '" +playButtonSuffix +"'");
    playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, exercisePanel, playButtonSuffix);
    myPostAudioRecordButton.setPlayAudioPanel(playAudioPanel);

    return playAudioPanel;
  }

  public void clickStop() {
    postAudioRecordButton.clickStop();
  }

  public boolean isRecording() {
    return postAudioRecordButton.isRecording();
  }

  protected WaveformPostAudioRecordButton makePostAudioRecordButton(String audioType) {
    return new WaveformPostAudioRecordButton(exercise, controller, exercisePanel, this, service, index, true, RECORD, STOP, audioType) {
      /**
       * @see mitll.langtest.client.recorder.RecordButton#start()
       */
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
  public Button getPlayButton() { return playAudioPanel.getPlayButton(); }

  public void setEnabled(boolean val) {
    //System.out.println("RecordAudioPanel.setEnabled " + val);
    postAudioRecordButton.setEnabled(val);
    if (postAudioRecordButton.hasValidAudio()) playAudioPanel.setEnabled(val);
  }

  //public void addPlayListener(PlayListener playListener) {  playAudioPanel.addPlayListener(playListener);  }

  public void setExercise(CommonExercise exercise) {
    this.exercise = exercise;
    postAudioRecordButton.setExercise(exercise);
  }

  /**
   * A play button that controls the state of the record button.
   */
  private class MyPlayAudioPanel extends PlayAudioPanel {
    public MyPlayAudioPanel(Image recordImage1, Image recordImage2, final Panel panel, String suffix) {
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
      }, suffix);
      add(recordImage1);
      recordImage1.setVisible(false);
      add(recordImage2);
      recordImage2.setVisible(false);
      getElement().setId("MyPlayAudioPanel");
    }

    /**
     * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI, String)
     */
    @Override
    protected void addButtons() {
      if (postAudioRecordButton == null) System.err.println("huh? postAudioRecordButton is null???");
      else add(postAudioRecordButton);
      super.addButtons();
    }
  }
}
