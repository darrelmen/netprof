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
  private static final String SCORE_LOW_TRY_AGAIN = "Score low, try again.";

  //private static final int PROGRESS_BAR_WIDTH = 260;
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

  protected String audioPath;

  ExerciseController controller;
  T exercise;
  /**
   *
   */
  private DivWidget scoreFeedback;
  private ProgressBar progressBar;
  private boolean hasScoreHistory = false;
  ListInterface<CommonShell> listContainer;

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
                         List<CorrectAndScore> history, ListInterface<CommonShell> listContainer) {
    this.controller = controller;
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.exercise = exercise;
    this.listContainer = listContainer;

    getElement().setId("SimpleRecordAudioPanel");
    //   addStyleName("inlineFlex");
    setWidth("100%");
    addWidgets();
    showRecordingHistory(history);
    hasScoreHistory = history != null && !history.isEmpty();
    setVisible(hasScoreHistory);
  }

  private DivWidget recordFeedback;

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @return
   * @seex #AudioPanel
   * @see SimpleRecordAudioPanel#SimpleRecordAudioPanel(BusyPanel, ExerciseController, CommonExercise, List, ListInterface)
   */
  protected void addWidgets() {
    RecorderPlayAudioPanel playAudioPanel = makePlayAudioPanel();
    // playAudioPanel.setWidth("50%");
    // add(playAudioPanel);

    add(scoreFeedback = new DivWidget());

    add(getScoreHistory());

    recordFeedback = playAudioPanel.getRecordFeedback(getWaitCursor());
    recordFeedback.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
    scoreFeedback.add(recordFeedback);
    // scoreFeedback.setWidth("50%");
    progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    getProgressBarContainer(progressBar);

    // scoreFeedback.add(progressBar);
    addTooltip(progressBar, "Overall Score");
    scoreFeedback.getElement().setId("scoreFeedbackRow");
  }


  public Widget getWaitCursor() {
    return waitCursorHelper.getWaitCursor();
  }


  /**
   * Add score feedback to the right of the play button.
   *
   * @return
   * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
   */
  private Widget getProgressBarContainer(ProgressBar progressBar) {
/*      HTML label = new HTML("Score");
      label.addStyleName("topFiveMargin");
      label.addStyleName("leftTenMargin");
      label.addStyleName("floatLeft");*/
    //   Panel afterPlayWidget = new DivWidget();
    //    afterPlayWidget.add(label);
    // afterPlayWidget.add(progressBar);

    //  progressBar.setWidth(PROGRESS_BAR_WIDTH + "px");
    // progressBar.addStyleName("floatLeft");
    //progressBar.getElement().getStyle().setMarginTop(15, Style.Unit.PX);

    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);

    //afterPlayWidget.addStyleName("floatLeft");
    //afterPlayWidget.setVisible(false);
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
        controller,
        false);
    playAudioPanel.hidePlayButton();

    return playAudioPanel;
  }

  /**
   * @see TwoColumnExercisePanel#makeFirstRow
   * @return
   */
  public PostAudioRecordButton getPostAudioRecordButton() {
    return postAudioRecordButton;
  }

  /**
   * @return
   * @see RecorderPlayAudioPanel#RecorderPlayAudioPanel
   */
  @NotNull
  public ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID());
    addMiniScoreListener(historyPanel);
    historyPanel.addStyleName("floatRight");
    historyPanel.showChart();
    historyPanel.setWidth("50%");
    return historyPanel;
  }

  /**
   * @return
   */
/*  private IconAnchor getDownloadIcon() {
    IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    addTooltip(download, DOWNLOAD_YOUR_RECORDING);

    download.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(download, "DownloadUserAudio_Icon", exercise,
            "downloading audio file ");
      }
    });
    return download;
  }*/
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
   * @see #useResult(PretestScore, boolean, String)
   */
  private void scoreAudio(AudioAnswer result) {
    clearScoreFeedback();
    PretestScore pretestScore = result.getPretestScore();

    if (pretestScore.getHydecScore() > 0) {
      scoreFeedback.add(getWordTableContainer(pretestScore));
    } else {
      Heading w = new Heading(4, SCORE_LOW_TRY_AGAIN);
      w.addStyleName("floatLeft");
      scoreFeedback.add(w);
    }
    useResult(pretestScore, false, result.getPath());
  }

  @NotNull
  private DivWidget getWordTableContainer(PretestScore pretestScore) {
    DivWidget wordTableContainer = new DivWidget();
    wordTableContainer.getElement().setId("wordTableContainer");
    wordTableContainer.addStyleName("inlineFlex");
    wordTableContainer.addStyleName("floatLeft");

    wordTableContainer.add(getPlayButtonDiv());
    DivWidget scoreFeedbackDiv = new DivWidget();
    scoreFeedbackDiv.add(progressBar);
    Widget styledWordTable = new WordScoresTable().getStyledWordTable(pretestScore);
    scoreFeedbackDiv.add(styledWordTable);

    wordTableContainer.add(scoreFeedbackDiv);

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
    scoreAudio(result);
    // waitCursorHelper.showFinished();
    waitCursorHelper.cancelTimer();
    waitCursorHelper.setWhite();
    waitCursorHelper.show();
  }

  @Override
  public void useInvalidResult() {
    setVisible(hasScoreHistory);
    playAudioPanel.hidePlayButton();

    playAudioPanel.setEnabled(false);

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
    float hydecScore = result.getHydecScore();
    boolean isValid = hydecScore > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      float zeroToHundred = hydecScore * 100f;
      showScore(Math.min(100.0f, zeroToHundred));
      listContainer.setScore(exercise.getID(), hydecScore);
    } else {
      hideScore();
    }
  }

  private void showScore(double score) {
    double percent = score / 100d;
    progressBar.setPercent(100 * percent);
    progressBar.setText("" + Math.round(score));//(score));
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
    // logger.info("get ready to play " +path);
    path = CompressedAudio.getPath(path);
    if (path != null) {
      this.audioPath = path;
    }
    if (playAudioPanel != null) {
      // logger.info("startSong ready to play " +path);
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
