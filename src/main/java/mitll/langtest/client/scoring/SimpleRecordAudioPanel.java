package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.WaitCursorHelper;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

/**
 * An ASR scoring panel with a record button.
 */
public class SimpleRecordAudioPanel<T extends CommonExercise> extends DivWidget implements RecordingAudioListener {
  private Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  public static final String OGG = ".ogg";

  /**
   * TODO : limit connection here...
   */
  private final BusyPanel goodwaveExercisePanel;

  private PostAudioRecordButton postAudioRecordButton;
  private RecorderPlayAudioPanel playAudioPanel;
  private MiniScoreListener miniScoreListener;

  private WaitCursorHelper waitCursorHelper;

  private String audioPath;

  private final ExerciseController controller;
  private final T exercise;
  /**
   *
   */
  private DivWidget scoreFeedback;
  private boolean hasScoreHistory = false;
  private final ListInterface<CommonShell, T> listContainer;
  private final boolean isRTL;
  private ScoreFeedbackDiv scoreFeedbackDiv;

  /**
   * @param controller
   * @param exercise
   * @paramx history
   * @param listContainer
   * @see TwoColumnExercisePanel#getRecordPanel
   */
  SimpleRecordAudioPanel(BusyPanel goodwaveExercisePanel,
                         ExerciseController controller,
                         T exercise,

                         ListInterface<CommonShell, T> listContainer) {
    this.controller = controller;
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.exercise = exercise;
    this.listContainer = listContainer;

    getElement().setId("SimpleRecordAudioPanel");
    this.isRTL = new ClickableWords<>().isRTL(exercise);
    setWidth("100%");
    addWidgets();
    List<CorrectAndScore> scores = exercise.getScores();
    showRecordingHistory(scores);
    hasScoreHistory = scores != null && !scores.isEmpty();
    setVisible(hasScoreHistory);
  }

  private DivWidget recordFeedback;
  private Widget scoreHistory;

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @return
   * @seex #AudioPanel
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel(BusyPanel, ExerciseController, CommonExercise, List, ListInterface)
   */
  private void addWidgets() {
    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    {
      DivWidget historyHoriz = new DivWidget();
      historyHoriz.addStyleName("inlineFlex");
      DivWidget spacer = new DivWidget();
      spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

      historyHoriz.add(spacer);
      historyHoriz.add(scoreHistory = getScoreHistory());
      col.add(historyHoriz);
    }

    add(col);

    recordFeedback = makePlayAudioPanel().getRecordFeedback(waitCursorHelper.getWaitCursor());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

    scoreFeedback.add(recordFeedback);

    this.scoreFeedbackDiv = new ScoreFeedbackDiv(playAudioPanel, playAudioPanel.getRealDownloadContainer());
    scoreFeedback.getElement().setId("scoreFeedbackRow");
  }

  private void addMiniScoreListener(MiniScoreListener l) {
    this.miniScoreListener = l;
  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @return
   * @see AudioPanel#getPlayButtons
   * @see #addWidgets
   */
  private RecorderPlayAudioPanel makePlayAudioPanel() {
    waitCursorHelper = new WaitCursorHelper();
    waitCursorHelper.showFinished();

    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(
        controller.getSoundManager(),
        postAudioRecordButton,
        exercise,
        controller
    );
    playAudioPanel.hidePlayButton();

    return playAudioPanel;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  PostAudioRecordButton getPostAudioRecordButton() {
    return postAudioRecordButton;
  }

  /**
   * @return
   * @see RecorderPlayAudioPanel#RecorderPlayAudioPanel
   * @see #addWidgets
   */
  @NotNull
  private ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID());
    addMiniScoreListener(historyPanel);
    historyPanel.showChart();
    historyPanel.setWidth("50%");
    return historyPanel;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(AudioAnswer)
   */
  private void setDownloadHref() {
    String audioPathToUse = audioPath.endsWith(OGG) ? audioPath.replaceAll(OGG, ".mp3") : audioPath;
    playAudioPanel.setDownloadHref(audioPathToUse, exercise.getID(), getUser());
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }

  /**
   * @param result
   * @param isRTL
   * @see #useResult(PretestScore, boolean, String)
   */
  private void scoreAudio(AudioAnswer result, boolean isRTL) {
    clearScoreFeedback();
    PretestScore pretestScore = result.getPretestScore();
    scoreFeedback.add(scoreFeedbackDiv.getWordTableContainer(pretestScore, isRTL));
    useResult(pretestScore, false, result.getPath());

    // Gotta remember the score on the exercise now...

    CorrectAndScore hydecScore = new CorrectAndScore(pretestScore.getHydecScore(),  result.getPath());
    hydecScore.setScores(pretestScore.getTypeToSegments());
    hydecScore.setJson(pretestScore.getJson());

    exercise.getScores().add(hydecScore);
  }

  @Override
  public void startRecording() {
    logger.info("startRecording...");
    setVisible(true);

    playAudioPanel.setEnabled(false);
    scoreFeedbackDiv.hideScore();

    goodwaveExercisePanel.setBusy(true);
    playAudioPanel.showFirstRecord();

    waitCursorHelper.showFinished();

    clearScoreFeedback();
    scoreHistory.setVisible(false);
  }

  private void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  @Override
  public void stopRecording() {
    logger.info("stopRecording...");

    playAudioPanel.setEnabled(true);

    goodwaveExercisePanel.setBusy(false);
    playAudioPanel.hideRecord();

    waitCursorHelper.scheduleWaitTimer();
    scoreHistory.setVisible(true);
  }

  @Override
  public void postAudioFile() {
    waitCursorHelper.scheduleWaitTimer();
  }

  @Override
  public void useResult(AudioAnswer result) {
    setVisible(true);
    hasScoreHistory = true;

    playAudioPanel.showPlayButton();

    audioPath = result.getPath();
    setDownloadHref();
    scoreAudio(result, isRTL);

//    waitCursorHelper.cancelTimer();
//    waitCursorHelper.setWhite();
//    waitCursorHelper.show();

    waitCursorHelper.showFinished();
  }

  @Override
  public void useInvalidResult(boolean isValid) {
    setVisible(hasScoreHistory);

//    logger.info("useInvalidResult " + isValid);

    if (!isValid) playAudioPanel.hidePlayButton();
    else playAudioPanel.showPlayButton();
    playAudioPanel.setEnabled(isValid);

    waitCursorHelper.showFinished();
//    waitCursorHelper.hide();
  }

  @Override
  public void flip(boolean first) {
    playAudioPanel.flip(first);
  }

  /**
   * @param result
   * @param scoredBefore
   * @param path
   */
  private void useResult(PretestScore result, boolean scoredBefore, String path) {
    //  logger.info("useResult path " +path);
    float hydecScore = result.getHydecScore();
    boolean isValid = hydecScore > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      scoreFeedbackDiv.showScore(Math.min(100.0f, hydecScore * 100f));
      listContainer.setScore(exercise.getID(), hydecScore);
    } else {
      scoreFeedbackDiv.hideScore();
    }
  }

  @Nullable
  private String getReadyToPlayAudio(String path) {
    //logger.info("getReadyToPlayAudio : get ready to play " +path);
    path = CompressedAudio.getPath(path);
    if (path != null) {
      this.audioPath = path;
    }
    if (playAudioPanel != null) {
      //logger.info("getReadyToPlayAudio startSong ready to play " +path);
      playAudioPanel.startSong(path);
    }
    return path;
  }

  /**
   * @param scores
   * @see #SimpleRecordAudioPanel
   */
  private void showRecordingHistory(List<CorrectAndScore> scores) {
    if (scores != null) {
      for (CorrectAndScore score : scores) {
        miniScoreListener.addScore(score);
      }
      setVisible(hasScoreHistory);
    }
    else logger.warning("scores is null?");
    miniScoreListener.showChart();
  }
}
