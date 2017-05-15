package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.WaitCursorHelper;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

/**
 * An ASR scoring panel with a record button.
 */
public class SimpleRecordAudioPanel<T extends CommonExercise> extends DivWidget implements RecordingAudioListener {
  private Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  private static final String OGG = ".ogg";
  /**
   * @see #scoreAudio
   */
  private static final String SCORE_LOW_TRY_AGAIN = "Score low, try again.";

  public static final int FIRST_STEP = 35;
  public static final int SECOND_STEP = 75;

  /**
   * TODO : limit connection here...
   */
  private final BusyPanel goodwaveExercisePanel;

  private PostAudioRecordButton postAudioRecordButton;
  private RecorderPlayAudioPanel playAudioPanel;
  private MiniScoreListener miniScoreListener;

  private WaitCursorHelper waitCursorHelper;

  private String audioPath;

  ExerciseController controller;
  T exercise;
  /**
   *
   */
  private DivWidget scoreFeedback;
  private ProgressBar progressBar;
  private boolean hasScoreHistory = false;
  private ListInterface<CommonShell,T> listContainer;
  boolean isRTL;

  /**
   * @param controller
   * @param exercise
   * @param history
   * @param listContainer
   * @see TwoColumnExercisePanel#getRecordPanel
   */
  SimpleRecordAudioPanel(BusyPanel goodwaveExercisePanel,
                         ExerciseController controller,
                         T exercise,
                         List<CorrectAndScore> history,
                         ListInterface<CommonShell,T> listContainer) {
    this.controller = controller;
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.exercise = exercise;
    this.listContainer = listContainer;

    getElement().setId("SimpleRecordAudioPanel");
    this.isRTL = new ClickableWords<>().isRTL(exercise);
    setWidth("100%");
    addWidgets();
    showRecordingHistory(history);
    hasScoreHistory = history != null && !history.isEmpty();
    setVisible(hasScoreHistory);
  }

  private DivWidget recordFeedback;
  private Widget  scoreHistory;

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @return
   * @seex #AudioPanel
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel(BusyPanel, ExerciseController, CommonExercise, List, ListInterface)
   */
  protected void addWidgets() {
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

    add(col);//scoreFeedback = new DivWidget());

    recordFeedback = makePlayAudioPanel().getRecordFeedback(getWaitCursor());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");

    scoreFeedback.add(recordFeedback);

    progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    styleTheProgressBar(progressBar);
    addTooltip(progressBar, "Overall Score");

    scoreFeedback.getElement().setId("scoreFeedbackRow");
  }

  private Widget getWaitCursor() {
    return waitCursorHelper.getWaitCursor();
  }

  /**
   * Add score feedback to the right of the play button.
   *
   * @return
   * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
   */
  private Widget styleTheProgressBar(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    progressBar.setVisible(false);
    return progressBar;
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
    waitCursorHelper.hide();

    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(//this,
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
  public ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID());
    addMiniScoreListener(historyPanel);
    historyPanel.showChart();
    historyPanel.setWidth("50%");
    return historyPanel;
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
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
    scoreFeedback.add(getWordTableContainer(pretestScore, isRTL));
    useResult(pretestScore, false, result.getPath());
  }

  @NotNull
  private DivWidget getWordTableContainer(PretestScore pretestScore, boolean isRTL) {
    DivWidget wordTableContainer = new DivWidget();
    wordTableContainer.getElement().setId("wordTableContainer");
    wordTableContainer.addStyleName("inlineFlex");
    wordTableContainer.addStyleName("floatLeft");

    wordTableContainer.add(getPlayButtonDiv());

    if (pretestScore.getHydecScore() > 0) {
      DivWidget scoreFeedbackDiv = new DivWidget();
      scoreFeedbackDiv.add(progressBar);

      Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget = new HashMap<>();
      scoreFeedbackDiv.add(new WordScoresTable()
          .getStyledWordTable(pretestScore, playAudioPanel, typeToSegmentToWidget, isRTL));
      SegmentHighlightAudioControl listener = new SegmentHighlightAudioControl(typeToSegmentToWidget);
      playAudioPanel.setListener(listener);

      wordTableContainer.add(scoreFeedbackDiv);
      logger.info("getWordTableContainer heard " + pretestScore.getRecoSentence());
    } else {
      Heading w = new Heading(4, SCORE_LOW_TRY_AGAIN);
      w.addStyleName("leftFiveMargin");
      wordTableContainer.add(w);
    }

    Panel downloadContainer = playAudioPanel.getDownloadContainer();
    downloadContainer.addStyleName("topFiveMargin");
    wordTableContainer.add(downloadContainer);

    return wordTableContainer;
  }

  @NotNull
  private DivWidget getPlayButtonDiv() {
    DivWidget divForPlay = new DivWidget();

    Widget playButton = playAudioPanel.getPlayButton();
    playButton.addStyleName("topFiveMargin");

    divForPlay.add(playButton);
    return divForPlay;
  }

  @Override
  public void startRecording() {
    setVisible(true);

    playAudioPanel.setEnabled(false);
    hideScore();

    goodwaveExercisePanel.setBusy(true);
    playAudioPanel.showFirstRecord();

    waitCursorHelper.hide();

    clearScoreFeedback();
    scoreHistory.setVisible(false);
  }

  private void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  @Override
  public void stopRecording() {
    playAudioPanel.setEnabled(true);

    goodwaveExercisePanel.setBusy(false);
    playAudioPanel.hideRecord();

    waitCursorHelper.show();
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

    waitCursorHelper.cancelTimer();
    waitCursorHelper.setWhite();
    waitCursorHelper.show();
  }

  @Override
  public void useInvalidResult(boolean isValid) {
    setVisible(hasScoreHistory);

//    logger.info("useInvalidResult " + isValid);

    if (!isValid) playAudioPanel.hidePlayButton();
    else playAudioPanel.showPlayButton();
    playAudioPanel.setEnabled(isValid);

    waitCursorHelper.showFinished();
    waitCursorHelper.hide();
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
      showScore(Math.min(100.0f, hydecScore * 100f));
      listContainer.setScore(exercise.getID(), hydecScore);
    } else {
      hideScore();
    }
  }

  private void showScore(double score) {
    double percent = score / 100d;
    progressBar.setPercent(100 * percent);
    progressBar.setText("" + Math.round(score));
    progressBar.setColor(
        score > SECOND_STEP ?
            ProgressBarBase.Color.SUCCESS :
            score > FIRST_STEP ?
                ProgressBarBase.Color.WARNING :
                ProgressBarBase.Color.DANGER);

    progressBar.setVisible(true);
  }

  private void hideScore() {
    progressBar.setVisible(false);
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
    miniScoreListener.showChart();
  }
}
