package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel extends ExercisePanel {
  private RecordAudioPanel audioPanel;
  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public WaveformExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller) {
    super(e,service,userFeedback,controller);
  }

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
    return audioPanel;
  }

  @Override
  public void onResize() {
    audioPanel.onResize();
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }


  /**
   * An ASR scoring panel with a record button.
   */
  private class RecordAudioPanel extends AudioPanel {
    private final int index;

    private PostAudioRecordButton postAudioRecordButton;

    /**
     * @param service
     * @paramx controller
     * @param index
     */
    public RecordAudioPanel(LangTestDatabaseAsync service, int index) {
      super(null, service, controller.getSoundManager(), true);
      this.index = index;
    }

    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toAdd) {
      postAudioRecordButton = new PostAudioRecordButton(exercise, controller, service, index) {
        private Timer t = null;

        @Override
        protected void startRecording() {
          super.startRecording();
          setButtonsEnabled(false);
        }

        /**
         * @see mitll.langtest.client.recorder.RecordButton#stop()
         */
        @Override
        protected void stopRecording() {
          setButtonsEnabled(true);

          waveform.setVisible(true);
          waveform.setUrl(LangTest.LANGTEST_IMAGES +"animated_progress.gif");

          super.stopRecording();
        }

        @Override
        public void useResult(AudioAnswer result) {
          if (t != null) t.cancel();
          getImagesForPath(wavToMP3(result.path));
          ExerciseQuestionState state = WaveformExercisePanel.this;
          state.recordCompleted(WaveformExercisePanel.this);
        }

        @Override
        protected void useInvalidResult(AudioAnswer result) {
          if (t != null) t.cancel();
          waveform.setVisible(false);
          spectrogram.setVisible(false);
          ExerciseQuestionState state = WaveformExercisePanel.this;
          state.recordIncomplete(WaveformExercisePanel.this);
        }
      };

      return new PlayAudioPanel(soundManager) {
        @Override
        protected void addButtons() {
          add(postAudioRecordButton.getRecord());
          super.addButtons();
        }
      };
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      postAudioRecordButton.onUnload();
    }
  }
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }
}
