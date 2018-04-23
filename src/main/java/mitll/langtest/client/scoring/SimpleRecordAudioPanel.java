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
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
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
  private final Logger logger = Logger.getLogger("SimpleRecordAudioPanel");
  public static final String MP3 = ".mp3";
  public static final String OGG = ".ogg";

  private static final float HUNDRED = 100.0f;

  /**
   * TODO : limit connection here...
   */
  private final BusyPanel goodwaveExercisePanel;

  private PostAudioRecordButton postAudioRecordButton;
  private RecorderPlayAudioPanel playAudioPanel;
  private MiniScoreListener miniScoreListener;

  private WaitCursorHelper waitCursorHelper = null;

  private String audioPath;

  private final ExerciseController controller;
  private final T exercise;
  /**
   *
   */
  private DivWidget scoreFeedback;
  private boolean hasScoreHistory;
  private final ListInterface<CommonShell, T> listContainer;
  private final boolean isRTL;
  private ScoreFeedbackDiv scoreFeedbackDiv;

  /**
   * @param controller
   * @param exercise
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
    this.isRTL = controller.isRightAlignContent();
    setWidth("100%");
    addWidgets();
    List<CorrectAndScore> scores = exercise.getScores();
//    logger.info("exercise " + exercise.getID() + " has\n\t" + scores + " scores");

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
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel
   */
  private void addWidgets() {
    //long then = System.currentTimeMillis();

    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    scoreFeedback.getElement().setId("scoreFeedback_" + exercise.getID());

    {
      DivWidget historyHoriz = new DivWidget();
      historyHoriz.addStyleName("inlineFlex");
      DivWidget spacer = new DivWidget();
      //spacer.getElement().setId("simpleSpacer");
      spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

      historyHoriz.add(spacer);
      historyHoriz.add(scoreHistory = getScoreHistory());
      col.add(historyHoriz);
    }

    recordFeedback = makePlayAudioPanel().getRecordFeedback(makeWaitCursor().getWaitCursor(), controller.shouldRecord());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

    scoreFeedback.add(recordFeedback);

    this.scoreFeedbackDiv = new ScoreFeedbackDiv(playAudioPanel, playAudioPanel.getRealDownloadContainer());

    add(col);

    // long now = System.currentTimeMillis();
    // logger.info("addWidgets "+ (now-then)+ " millis");
    //scoreFeedback.getElement().setId("scoreFeedbackRow");
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
    //long then = System.currentTimeMillis();
    makeWaitCursor();

    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(
        controller.getSoundManager(),
        postAudioRecordButton,
        controller, exercise);
    playAudioPanel.hidePlayButton();

//    long now = System.currentTimeMillis();
    // logger.info("took " + (now-then) + " for makeAudioPanel");
    return playAudioPanel;
  }

  private WaitCursorHelper makeWaitCursor() {
    if (waitCursorHelper == null) {
      waitCursorHelper = new WaitCursorHelper();
      waitCursorHelper.showFinished();
    }
    return waitCursorHelper;
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
    historyPanel.showChart(controller.getHost());
    historyPanel.setWidth("50%");
    return historyPanel;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(AudioAnswer)
   */
  private void setDownloadHref() {
    String audioPathToUse = audioPath.endsWith(OGG) ? audioPath.replaceAll(OGG, MP3) : audioPath;
    playAudioPanel.setDownloadHref(audioPathToUse, exercise.getID(), getUser(), controller.getHost());
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }

  /**
   * @param result
   * @param isRTL
   * @see #useResult
   */
  private void scoreAudio(AudioAnswer result, boolean isRTL) {
    clearScoreFeedback();
    PretestScore pretestScore = result.getPretestScore();
    scoreFeedback.add(scoreFeedbackDiv.getWordTableContainer(pretestScore, isRTL));
    useScoredResult(pretestScore, false, result.getPath());

    // Gotta remember the score on the exercise now...
    exercise.getScores().add(new CorrectAndScore(result));
//    logger.info("exercise " + exercise.getID() + " now has " + exercise.getScores().size() + " scores");
  }

  @Override
  public void startRecording() {
    //logger.info("startRecording...");
    setVisible(true);

    playAudioPanel.setEnabled(false);
    scoreFeedbackDiv.hideScore();

    if (goodwaveExercisePanel != null) goodwaveExercisePanel.setBusy(true);
    playAudioPanel.showFirstRecord();

    waitCursorHelper.showFinished();

    clearScoreFeedback();
    scoreHistory.setVisible(false);
  }

  private void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  /**
   * @see FeedbackPostAudioRecordButton#stopRecording
   */
  @Override
  public void stopRecording() {
    //  logger.info("stopRecording...");
    playAudioPanel.setEnabled(true);

    if (goodwaveExercisePanel != null) goodwaveExercisePanel.setBusy(false);
    playAudioPanel.hideRecord();

    scoreHistory.setVisible(true);

    waitCursorHelper.scheduleWaitTimer();
  }

  public void gotShortDurationRecording() {
    waitCursorHelper.showFinished();
    playAudioPanel.hideRecord();
    setVisible(true);
    scoreHistory.setVisible(true);
  }

  @Override
  public void useResult(AudioAnswer result) {
//    logger.info("useScoredResult " + result);
    waitCursorHelper.showFinished();
    setVisible(true);
    hasScoreHistory = true;

    playAudioPanel.showPlayButton();

    audioPath = result.getPath();
    setDownloadHref();
    scoreAudio(result, isRTL);
  }

  @Override
  public void useInvalidResult(boolean isValid) {
    //  logger.info("useInvalidResult " + isValid);

    waitCursorHelper.showFinished();
    setVisible(hasScoreHistory);


    if (!isValid) playAudioPanel.hidePlayButton();
    else playAudioPanel.showPlayButton();

    playAudioPanel.setEnabled(isValid);
  }

  @Override
  public void flip(boolean first) {
    playAudioPanel.flip(first);
  }

  /**
   * @param result
   * @param scoredBefore
   * @param path
   * @see #scoreAudio
   */
  private void useScoredResult(PretestScore result, boolean scoredBefore, String path) {
    float hydecScore = result.getHydecScore();
    boolean isValid = hydecScore > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      List<TranscriptSegment> transcriptSegments = result.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
      if (transcriptSegments != null && transcriptSegments.size() == 1) {
        float wordScore = transcriptSegments.get(0).getScore();
        //  logger.info("useScoredResult using word score " + wordScore + " instead of hydec score " + hydecScore);
        hydecScore = wordScore;
      }

      scoreFeedbackDiv.showScore(Math.min(HUNDRED, hydecScore * HUNDRED), result.isFullMatch(), false);
      listContainer.setScore(exercise.getID(), hydecScore);
    } else {
      scoreFeedbackDiv.hideScore();
    }
  }

  @Nullable
  private void getReadyToPlayAudio(String path) {
    //logger.info("getReadyToPlayAudio : get ready to play " +path);
    path = CompressedAudio.getPath(path);
    if (path != null) {
      this.audioPath = path;
    }
    if (playAudioPanel != null) {
      //logger.info("getReadyToPlayAudio startSong ready to play " +path);
      playAudioPanel.startSong(path, true);
    }
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
    } else {
      //logger.warning("scores is null?");
    }

    miniScoreListener.showChart(controller.getHost());
  }
}
