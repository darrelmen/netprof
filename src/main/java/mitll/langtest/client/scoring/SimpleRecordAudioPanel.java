package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.list.WaitCursorHelper;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

/**
 * An ASR scoring panel with a record button.
 */
public class SimpleRecordAudioPanel<T extends CommonExercise> extends DivWidget implements RecordingAudioListener {
  public static final String SCORE_LOW_TRY_AGAIN = "Score low, try again.";
  private Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  private static final int PROGRESS_BAR_WIDTH = 260;
  private static final String DOWNLOAD_AUDIO = "downloadAudio";

  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";
  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";

  private static final int FIRST_STEP = 35;
  private static final int SECOND_STEP = 75;

  /**
   * TODO : limit connection here...
   */
  private final BusyPanel goodwaveExercisePanel;

  private PostAudioRecordButton postAudioRecordButton;
  private MyPlayAudioPanel playAudioPanel;
  private IconAnchor download;
  private Panel downloadContainer;
  private MiniScoreListener miniScoreListener;

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Image recordImage1;
  private Image recordImage2;
  private WaitCursorHelper waitCursorHelper;

  protected String audioPath;

  ExerciseController controller;
  T exercise;
  private DivWidget scoreFeedback;
  private ProgressBar progressBar;
  private boolean hasScoreHistory = false;

  /**
   * @param controller
   * @param exercise
   * @param history
   * @see TwoColumnExercisePanel#getRecordPanel
   */
  SimpleRecordAudioPanel(BusyPanel goodwaveExercisePanel,
                         ExerciseController controller,
                         T exercise,
                         List<CorrectAndScore> history) {
    this.controller = controller;
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.exercise = exercise;

    addWidgets();
    showRecordingHistory(history);
    hasScoreHistory = history != null && !history.isEmpty();
    setVisible(hasScoreHistory);
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @return
   * @seex #AudioPanel
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   */
  protected void addWidgets() {
    add(makePlayAudioPanel());
    add(scoreFeedback = new DivWidget());
    progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    getProgressBarContainer(progressBar);
    scoreFeedback.add(progressBar);
    addTooltip(progressBar, "Overall Score");
    scoreFeedback.getElement().setId("scoreFeedbackRow");
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

    progressBar.setWidth(PROGRESS_BAR_WIDTH + "px");
    // progressBar.addStyleName("floatLeft");
     progressBar.getElement().getStyle().setMarginTop(15, Style.Unit.PX  );

    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);

    //afterPlayWidget.addStyleName("floatLeft");
    //afterPlayWidget.setVisible(false);
    progressBar.setVisible(false);

    return progressBar;
  }

//  private void addToScoreFeedbackRow(Widget widget) {
//    scoreFeedback.add(widget);
//  }

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
  private PlayAudioPanel makePlayAudioPanel() {
    recordImage1 = new Image(UriUtils.fromSafeConstant(FIRST_RED));
    recordImage1.setVisible(false);
    recordImage1.setWidth("32px");
    recordImage2 = new Image(UriUtils.fromSafeConstant(SECOND_RED));
    recordImage2.setVisible(false);
    recordImage2.setWidth("32px");

    waitCursorHelper = new WaitCursorHelper();
    waitCursorHelper.hide();

    postAudioRecordButton = new FeedbackPostAudioRecordButton(exercise.getID(), this, controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());
    //postAudioRecordButton.addStyleName("rightFiveMargin");

    playAudioPanel = new MyPlayAudioPanel(controller.getSoundManager(), postAudioRecordButton);
    //  playAudioPanel.setVisible(false);
    playAudioPanel.hidePlayButton();
    return playAudioPanel;
  }

  public PostAudioRecordButton getPostAudioRecordButton() {
    return postAudioRecordButton;
  }

  private class MyPlayAudioPanel extends PlayAudioPanel {
    /**
     * @param soundManager
     * @param postAudioRecordButton1
     * @paramx xgoodwaveExercisePanel
     * @see #makePlayAudioPanel
     */
    public MyPlayAudioPanel(SoundManagerAPI soundManager,
                            final PostAudioRecordButton postAudioRecordButton1) {
      super(soundManager, new PlayListener() {
        public void playStarted() {
//          goodwaveExercisePanel.setBusy(true);
          // TODO put back busy thing?
          postAudioRecordButton1.setEnabled(false);
        }

        public void playStopped() {
          //  goodwaveExercisePanel.setBusy(false);
          postAudioRecordButton1.setEnabled(true);
        }
      }, "", null);

      getElement().setId("SimpleRecordAudio_MyPlayAudioPanel");
    }

    public void hidePlayButton() {
      playButton.setVisible(false);
    }

    public void showPlayButton() {
      playButton.setVisible(true);
    }

    /**
     * @param optionalToTheRight
     * @see PlayAudioPanel#PlayAudioPanel
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      DivWidget firstRow = new DivWidget();
      firstRow.addStyleName("inlineFlex");
      add(firstRow);

      playButton = makePlayButton();
     // firstRow.add(playButton = makePlayButton());

      firstRow.add(getRecordFeedback());
     // firstRow.add(downloadContainer = addDownloadAudioWidget());
      downloadContainer = addDownloadAudioWidget();
      firstRow.add(getScoreHistory());
    }

    public Panel getDownloadContainer() { return downloadContainer; }

    private DivWidget getRecordFeedback() {
      DivWidget recordFeedback = new DivWidget();
      recordFeedback.addStyleName("inlineFlex");
      recordFeedback.getElement().setId("recordFeedbackImageContainer");
   //   recordFeedback.setWidth("32px");
      recordFeedback.add(recordImage1);
      recordFeedback.add(recordImage2);
      Widget waitCursor = waitCursorHelper.getWaitCursor();
      //waitCursor.setVisible(false);
      recordFeedback.add(waitCursor);
      return recordFeedback;
    }

    private DivWidget addDownloadAudioWidget() {
      DivWidget downloadContainer = new DivWidget();

      downloadContainer.getElement().setId("downloadContainer");

      downloadContainer.add(download = getDownloadIcon());
      downloadContainer.setVisible(false);
      downloadContainer.addStyleName("leftFiveMargin");

      downloadContainer.addStyleName("rightFiveMargin");
      downloadContainer.addStyleName("topFiveMargin");

      return downloadContainer;
    }
  }

  /**
   * @return
   * @see MyPlayAudioPanel#addButtons(Widget)
   */
  @NotNull
  private ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID());
    addMiniScoreListener(historyPanel);
    historyPanel.addStyleName("floatRight");
    historyPanel.showChart();
    return historyPanel;
  }

  /**
   *
   * @return
   */
  private IconAnchor getDownloadIcon() {
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
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * @seex #useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   */
  private void setDownloadHref() {
    downloadContainer.setVisible(true);

    String audioPathToUse = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;

    String href = DOWNLOAD_AUDIO +
        "?file=" +
        audioPathToUse +
        "&" +
        "exerciseID=" +
        exercise.getID() +
        "&" +
        "userID=" +
        getUser();
    download.setHref(href);
    //downloadAnchor.setHref(href);
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }
  //private int reqid = 0;

  /**
   * @param result
   * @see #useResult(PretestScore, boolean, String)
   */
  private void scoreAudio(AudioAnswer result) {
//    logger.info("use " + result);
    clearScoreFeedback();
    scoreFeedback.add(progressBar);
    if (result.getPretestScore().getHydecScore() > 0) {
      Widget styledWordTable = new WordScoresTable().getStyledWordTable(result.getPretestScore());
      DivWidget wordTableContainer = new DivWidget();
      wordTableContainer.getElement().setId("wordTableContainer");
      wordTableContainer.addStyleName("inlineFlex");
      wordTableContainer.addStyleName("floatLeft");

      DivWidget divForPlay = new DivWidget();

      Button playButton = playAudioPanel.getPlayButton();
      playButton.addStyleName("topFiveMargin");

      divForPlay.add(playButton);
      wordTableContainer.add(divForPlay);

      wordTableContainer.add(styledWordTable);

      Panel downloadContainer = playAudioPanel.getDownloadContainer();
      downloadContainer.addStyleName("topFiveMargin");
      wordTableContainer.add(downloadContainer);

      scoreFeedback.add(wordTableContainer);
    } else {
      Heading w = new Heading(4, SCORE_LOW_TRY_AGAIN);
      w.addStyleName("floatLeft");

      scoreFeedback.add(w);
    }
    useResult(result.getPretestScore(), false, result.getPath());
  }

  private void clearScoreFeedback() {
    scoreFeedback.clear();
  }


  @Override
  public void startRecording() {
    setVisible(true);

    playAudioPanel.setEnabled(false);
    hideScore();

    goodwaveExercisePanel.setBusy(true);

    recordImage1.setVisible(true);

    downloadContainer.setVisible(false);

    waitCursorHelper.hide();

    clearScoreFeedback();
  }

  @Override
  public void stopRecording() {
    playAudioPanel.setEnabled(true);

    goodwaveExercisePanel.setBusy(false);

    recordImage1.setVisible(false);
    recordImage2.setVisible(false);

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

//    playAudioPanel.setVisible(true);
    playAudioPanel.showPlayButton();

    audioPath = result.getPath();
    setDownloadHref();
    scoreAudio(result);
    waitCursorHelper.showFinished();
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
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  /**
   * @param result
   * @param scoredBefore
   * @param path
   */
  private void useResult(PretestScore result, boolean scoredBefore, String path) {
    boolean isValid = result.getHydecScore() > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      float zeroToHundred = result.getHydecScore() * 100f;
      showScore(Math.min(100.0f, zeroToHundred));
    } else {
      hideScore();
    }
  }

  void showScore(double score) {
    double percent = score / 100d;
    progressBar.setPercent(100 * percent);
    progressBar.setText("" + Math.round(score));//(score));
    progressBar.setColor(
        score > SECOND_STEP ?
            ProgressBarBase.Color.SUCCESS :
            score > FIRST_STEP ?
                ProgressBarBase.Color.WARNING :
                ProgressBarBase.Color.DANGER);

    //scoreBar.setVisible(true);
  progressBar.setVisible(true);
    //scores.setVisible(true);
  }

  void hideScore() {
    //scoreBar.setVisible(false);
    progressBar.setVisible(false);

    //scores.setVisible(false);
  }


  @Nullable
  private String getReadyToPlayAudio(String path) {
    // logger.info("get ready to play " +path);
    path = getPath(path);
    if (path != null) {
      this.audioPath = path;
    }
    if (playAudioPanel != null) {
      // logger.info("startSong ready to play " +path);
      playAudioPanel.startSong(path);
    }
    return path;
  }

  private String getPath(String path) {
    return CompressedAudio.getPath(path);
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
