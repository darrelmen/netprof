package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Does fancy flashing record bulb while recording.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel extends ExercisePanel {
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private boolean isBusy = false;
  private RecordAudioPanel audioPanel;
  private Image recordImage1;
  private Image recordImage2;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public WaveformExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller) {
    super(e, service, userFeedback, controller);
    getElement().setId("WaveformExercisePanel");
  }

  public void setBusy(boolean v) { this.isBusy = v; }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.exercise.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    audioPanel = new RecordAudioPanel(service, index);
    addAnswerWidget(index,this);
    return audioPanel;
  }

  @Override
  public void onResize() {
    audioPanel.onResize();
  }

  /**
   * @see ExercisePanel#addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   * @param promptInEnglish
   * @return
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }

  /**
   * An ASR scoring panel with a record button.
   */
  private class RecordAudioPanel extends AudioPanel {
    private final int index;

    private PostAudioRecordButton postAudioRecordButton;
    private PlayAudioPanel playAudioPanel;
    /**
     * @param service
     * @param index
     * @see WaveformExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
     */
    public RecordAudioPanel(LangTestDatabaseAsync service, int index) {
      super(null, service,
        // use full screen width
        true, // use keyboard
        controller, null);
      this.index = index;
      getElement().setId("RecordAudioPanel");
    }

    /**
     * @see AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
     * @param toAdd
     * @return
     */
    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
      recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
      recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
      System.out.println( "making play audio panel with " + recordImage1 + " and " + recordImage2);

      MyPostAudioRecordButton myPostAudioRecordButton = new MyPostAudioRecordButton(this, service, index);
      postAudioRecordButton = myPostAudioRecordButton;
      playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, WaveformExercisePanel.this);
      myPostAudioRecordButton.setPlayAudioPanel(playAudioPanel);

      return playAudioPanel;
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      postAudioRecordButton.onUnload();
    }

    private class MyPlayAudioPanel extends PlayAudioPanel {
      public MyPlayAudioPanel(Image recordImage1, Image recordImage2, final WaveformExercisePanel panel) {
        super(RecordAudioPanel.this.soundManager, new PlayListener() {
          public void playStarted() {
            panel.setBusy(true);
            panel.setButtonsEnabled(false);
            ((HasEnabled)RecordAudioPanel.this.postAudioRecordButton.getRecord()).setEnabled(false);
          }

          public void playStopped() {
            panel.setBusy(false);
            panel.setButtonsEnabled(true);
            ((HasEnabled)RecordAudioPanel.this.postAudioRecordButton.getRecord()).setEnabled(true);
          }
        });
        add(recordImage1);
        recordImage1.setVisible(false);
        add(recordImage2);
        recordImage2.setVisible(false);
      }

      /**
       * @see PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI)
       */
      @Override
      protected void addButtons() {
        if (postAudioRecordButton == null) System.err.println("huh? postAudioRecordButton is null???");
        else add(postAudioRecordButton.getRecord());
        super.addButtons();
      }
    }
  }

  public boolean isBusy() {
    return isBusy;
  }

  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  private class MyPostAudioRecordButton extends PostAudioRecordButton {
    private Timer t = null;
    private RecordAudioPanel widgets;
    private PlayAudioPanel playAudioPanel;

    public MyPostAudioRecordButton(RecordAudioPanel widgets, LangTestDatabaseAsync service, int index) {
      super(exercise, controller, service, index, recordImage1, recordImage2);
      this.widgets = widgets;
    }

    @Override
    protected void startRecording() {
      setBusy(true);
      super.startRecording();
      setButtonsEnabled(false);
      playAudioPanel.setPlayEnabled(false);
    }

    /**
     * @see mitll.langtest.client.recorder.RecordButton#stop()
     */
    @Override
    protected void stopRecording() {
      setButtonsEnabled(true);
      widgets.getWaveform().setVisible(true);
      widgets.getWaveform().setUrl(LangTest.LANGTEST_IMAGES + "animated_progress.gif");

      super.stopRecording();

      playAudioPanel.setPlayEnabled(true);
      setBusy(false);
    }

    @Override
    public void useResult(AudioAnswer result) {
      if (t != null) t.cancel();
      widgets.getImagesForPath(wavToMP3(result.path));
      ExerciseQuestionState state = WaveformExercisePanel.this;
      state.recordCompleted(WaveformExercisePanel.this);
    }

    @Override
    protected void useInvalidResult(AudioAnswer result) {
      if (t != null) t.cancel();
      widgets.getWaveform().setVisible(false);
      widgets.getSpectrogram().setVisible(false);
      ExerciseQuestionState state = WaveformExercisePanel.this;
      state.recordIncomplete(WaveformExercisePanel.this);
    }

    public void setPlayAudioPanel(PlayAudioPanel playAudioPanel) {
      this.playAudioPanel = playAudioPanel;
    }
  }
}
