package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.exercise.Shell;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

public class NoFeedbackRecordAudioPanel<T extends Shell & ScoredExercise> extends DivWidget
    implements RecordingAudioListener {
//  private final Logger logger = Logger.getLogger("NoFeedbackRecordAudioPanel");

  protected final ExerciseController controller;
  protected final T exercise;
  private PostAudioRecordButton postAudioRecordButton;
  RecorderPlayAudioPanel playAudioPanel;
  DivWidget recordFeedback;
  DivWidget scoreFeedback;
  private SessionManager sessionManager;

  /**
   * @param exercise
   * @param controller
   * @see RecordDialogExercisePanel#addWidgets(boolean, boolean, PhonesChoices)
   */
  NoFeedbackRecordAudioPanel(T exercise, ExerciseController controller, SessionManager sessionManager) {
    this.exercise = exercise;
    this.controller = controller;
    this.sessionManager = sessionManager;

    getElement().setId("NoFeedbackRecordAudioPanel_" + exercise.getID());
  }

  protected void addWidgets() {
    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    scoreFeedback.getElement().setId("scoreFeedback_" + exercise.getID());
    recordFeedback = makePlayAudioPanel().getRecordFeedback(null, controller.shouldRecord());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
    scoreFeedback.add(recordFeedback);

    add(col);
    // long now = System.currentTimeMillis();
    // logger.info("addWidgets "+ (now-then)+ " millis");
    //scoreFeedback.getElement().setId("scoreFeedbackRow");
  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @return
   * @seex #addWidgets
   * @see AudioPanel#getPlayButtons
   */
  RecorderPlayAudioPanel makePlayAudioPanel() {
    //long then = System.currentTimeMillis();
    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller) {

      /**
       * @see RecordButtonPanel#postAudioFile
       * @return
       */
      @Override
      protected String getDevice() {
        return getDeviceValue();
      }
    };
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(postAudioRecordButton, controller, exercise);

    // TODO : JUST FOR NOW
    playAudioPanel.hidePlayButton();

//    long now = System.currentTimeMillis();
    // logger.info("took " + (now-then) + " for makeAudioPanel");
    return playAudioPanel;
  }

  private String getDeviceValue() {
    return sessionManager.getSession();
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  PostAudioRecordButton getPostAudioRecordButton() {
    return postAudioRecordButton;
  }

  @Override
  public void startRecording() {
    //logger.info("startRecording...");
    setVisible(true);
    playAudioPanel.setEnabled(false);
    playAudioPanel.showFirstRecord();
    clearScoreFeedback();
  }

  void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  /**
   * @see FeedbackPostAudioRecordButton#stopRecording(long)
   */
  @Override
  public void stopRecording() {
//    logger.info("stopRecording...");
    playAudioPanel.setEnabled(true);
    playAudioPanel.hideRecord();
  }

  public void cancelRecording() {
    postAudioRecordButton.cancelRecording();
  }

  public void gotShortDurationRecording() {
    playAudioPanel.hideRecord();
    setVisible(true);
    //  scoreHistory.setVisible(true);
  }

  @Override
  public void flip(boolean first) {
    playAudioPanel.flip(first);
  }

  /**
   *
   * @param result IGNORED HERE
   */
  @Override
  public void useResult(AudioAnswer result) {
    //logger.info("useScoredResult " + result);
    setVisible(true);
    playAudioPanel.showPlayButton();
  }

  @Override
  public void useInvalidResult(boolean isValid) {
    //  logger.info("useInvalidResult " + isValid);
    if (!isValid) playAudioPanel.hidePlayButton();
    else playAudioPanel.showPlayButton();

    playAudioPanel.setEnabled(isValid);
  }

  DivWidget getScoreFeedback() {
    return scoreFeedback;
  }
}
