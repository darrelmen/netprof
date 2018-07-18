package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

public class NoFeedbackRecordAudioPanel<T extends Shell & ScoredExercise> extends DivWidget implements RecordingAudioListener {
  private final Logger logger = Logger.getLogger("NoFeedbackRecordAudioPanel");
  protected final ExerciseController controller;
  protected final T exercise;
  protected PostAudioRecordButton postAudioRecordButton;
  protected RecorderPlayAudioPanel playAudioPanel;
  protected DivWidget recordFeedback;
  protected DivWidget scoreFeedback;

  /**
   * @see RecordDialogExercisePanel#addWidgets(boolean, boolean, PhonesChoices)
   * @param exercise
   * @param controller
   */
  public NoFeedbackRecordAudioPanel(T exercise, ExerciseController controller) {
    this.exercise = exercise;
    this.controller = controller;

    getElement().setId("NoFeedbackRecordAudioPanel_"+ exercise.getID());
  }

  protected void addWidgets() {
    //logger.info("addWidets");

    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    scoreFeedback.getElement().setId("scoreFeedback_" + exercise.getID());

    //long then = System.currentTimeMillis();
    //DivWidget col = new DivWidget();

    //add(this::makePlayAudioPanel);
    //makePlayAudioPanel();

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
  public RecorderPlayAudioPanel makePlayAudioPanel() {
    //long then = System.currentTimeMillis();

    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(postAudioRecordButton, controller, exercise);

    // TODO : JUST FOR NOW
    playAudioPanel.hidePlayButton();

//    long now = System.currentTimeMillis();
    // logger.info("took " + (now-then) + " for makeAudioPanel");
    return playAudioPanel;
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

  protected void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  @Override
  public void stopRecording() {
    logger.info("stopRecording...");
    playAudioPanel.setEnabled(true);
    playAudioPanel.hideRecord();
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

  @Override
  public void useResult(AudioAnswer result) {
   logger.info("useScoredResult " + result);
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

  public DivWidget getScoreFeedback() {
    return scoreFeedback;
  }
}
