package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.list.WaitCursorHelper;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
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
public class SimpleRecordAudioPanel<T extends CommonExercise> extends DivWidget {
  public static final int PROGRESS_BAR_WIDTH = 260;
  private Logger logger = Logger.getLogger("SimpleRecordAudioPanel");

  private static final String DOWNLOAD_AUDIO = "downloadAudio";

  private static final String RECORD_YOURSELF = "";//Record";
  //  private static final String RELEASE_TO_STOP = "Release";
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";
  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";

  public static final int FIRST_STEP = 35;
  public static final int SECOND_STEP = 75;

  /**
   * TODO : limit connection here...
   */
  private final BusyPanel goodwaveExercisePanel;
  private final int index;
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
  WaitCursorHelper waitCursorHelper;

  protected String audioPath;

  ExerciseController controller;
  T exercise;
  private DivWidget scoreFeedback;

  /**
   * @param controller
   * @param exercise
   * @param instance
   * @param history
   * @see TwoColumnExercisePanel#getRecordPanel
   */
  SimpleRecordAudioPanel(BusyPanel goodwaveExercisePanel,
                         ExerciseController controller,
                         T exercise,
                         String instance, List<CorrectAndScore> history) {
    this.controller = controller;
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.index = 1;
    this.exercise = exercise;

    addWidgets("", "");
    showRecordingHistory(history);
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param playButtonSuffix
   * @param recordButtonTitle
   * @return
   * @seex #AudioPanel
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel
   */
  protected void addWidgets(String playButtonSuffix, String recordButtonTitle) {
    add(makePlayAudioPanel());
    add(scoreFeedback = new DivWidget());
  }

  public void addMinicoreListener(MiniScoreListener l) {
    this.miniScoreListener = l;
  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @return
   * @paramx toTheRightWidget
   * @paramx buttonTitle
   * @paramx recordButtonTitle
   * @see AudioPanel#getPlayButtons
   * @see #addWidgets(String, String)
   */
  protected PlayAudioPanel makePlayAudioPanel() {
    recordImage1 = new Image(UriUtils.fromSafeConstant(FIRST_RED));
    recordImage1.setVisible(false);
    recordImage2 = new Image(UriUtils.fromSafeConstant(SECOND_RED));
    recordImage2.setVisible(false);
    waitCursorHelper = new WaitCursorHelper();

    postAudioRecordButton = new MyPostAudioRecordButton(controller);
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.addStyleName("rightFiveMargin");

    playAudioPanel = new MyPlayAudioPanel(controller.getSoundManager(), postAudioRecordButton);
    return playAudioPanel;
  }

  /**
   * @param result
   * @param scoredBefore
   * @param path
   */
  protected void useResult(PretestScore result, boolean scoredBefore, String path) {
//    super.useResult(result, wordTranscript, phoneTranscript, scoredBefore, path);
    boolean isValid = result.getHydecScore() > 0;
    if (!scoredBefore && miniScoreListener != null && isValid) {
      miniScoreListener.gotScore(result, path);
    }
    getReadyToPlayAudio(path);
    if (isValid) {
      float zeroToHundred = result.getHydecScore() * 100f;
      playAudioPanel.showScore(Math.min(100.0f, zeroToHundred));
    } else {
      playAudioPanel.hideScore();
    }
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

  private class MyPlayAudioPanel extends PlayAudioPanel {
    private DivWidget recordFeedback = new DivWidget();
    private Widget scoreBar;
    private ProgressBar progressBar;
    DivWidget scores;

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

    /**
     * @param optionalToTheRight
     * @see PlayAudioPanel#PlayAudioPanel
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      DivWidget firstRow = new DivWidget();
      firstRow.addStyleName("topFiveMargin");
      firstRow.addStyleName("inlineFlex");
      add(firstRow);

      firstRow.add(playButton = makePlayButton());

      firstRow.add(postAudioRecordButton);

      firstRow.add(recordFeedback = getRecordFeedback());
      firstRow.add(downloadContainer = addDownloadAudioWidget());

      scores = new DivWidget();

      scores.add(scoreBar = getAfterPlayWidget(progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT)));

      firstRow.add(scores);

      ASRHistoryPanel scoreHistory = getScoreHistory();
      firstRow.add(scoreHistory);
    }

    private DivWidget getRecordFeedback() {
      recordFeedback = new DivWidget();
      recordFeedback.addStyleName("inlineFlex");
      recordFeedback.getElement().setId("recordFeedbackImageContainer");
      recordFeedback.setWidth("32px");
      recordFeedback.add(recordImage1);
      recordFeedback.add(recordImage2);
      recordFeedback.add(waitCursorHelper.getWaitCursor());
      return recordFeedback;
    }


    /**
     * Add score feedback to the right of the play button.
     *
     * @return
     * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
     */
    Widget getAfterPlayWidget(ProgressBar progressBar) {
/*      HTML label = new HTML("Score");
      label.addStyleName("topFiveMargin");
      label.addStyleName("leftTenMargin");
      label.addStyleName("floatLeft");*/

      Panel afterPlayWidget = new DivWidget();

      //    afterPlayWidget.add(label);
      afterPlayWidget.add(progressBar);

      progressBar.setWidth(PROGRESS_BAR_WIDTH + "px");
      progressBar.addStyleName("floatLeft");

      Style style = progressBar.getElement().getStyle();
      style.setMarginTop(5, Style.Unit.PX);
      style.setMarginLeft(5, Style.Unit.PX);
      style.setMarginBottom(0, Style.Unit.PX);

      afterPlayWidget.addStyleName("floatLeft");
      afterPlayWidget.setVisible(false);

      return afterPlayWidget;
    }


    /**
     * Set the value on the progress bar to reflect the dynamic range we measure on the audio.
     *
     * @param score
     * @see #useResult
     */
    void showScore(double score) {
      double percent = score / 100d;
      String color = SimpleColumnChart.getColor((float) percent);

      logger.info("percent " + percent + " color " + color);

      progressBar.setPercent(100 * percent);
      progressBar.setText("" + Math.round(score));//(score));
      progressBar.setColor(
          score > SECOND_STEP ?
              ProgressBarBase.Color.SUCCESS :
              score > FIRST_STEP ?
                  ProgressBarBase.Color.WARNING :
                  ProgressBarBase.Color.DANGER);

      scoreBar.setVisible(true);
      scores.setVisible(true);
    }

    void hideScore() {
      scoreBar.setVisible(false);
      scores.setVisible(false);
    }

    private DivWidget addDownloadAudioWidget() {
      DivWidget downloadContainer = new DivWidget();
      // downloadContainer.setWidth("40px");

      DivWidget north = new DivWidget();
      north.add(download = getDownloadIcon());
      downloadContainer.add(north);
      downloadContainer.setVisible(false);
      downloadContainer.addStyleName("leftFiveMargin");
      return downloadContainer;
    }
  }

  /**
   * @see MyPlayAudioPanel#addButtons(Widget)
   * @return
   */
  @NotNull
  private ASRHistoryPanel getScoreHistory() {
    ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exercise.getID());
    addMinicoreListener(historyPanel);
    historyPanel.addStyleName("floatRight");
    historyPanel.showChart();
    return historyPanel;
  }

  private IconAnchor getDownloadIcon() {
    IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    download.getElement().getStyle().setMarginLeft(19, Style.Unit.PX);

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

  /**
   * @see AudioPanel#makePlayAudioPanel(Widget, String, String)
   */
  private class MyPostAudioRecordButton extends PostAudioRecordButton {
    MyPostAudioRecordButton(ExerciseController controller) {
      super(
          exercise.getID(),
          controller,
          SimpleRecordAudioPanel.this.index,
          true,
          RECORD_YOURSELF,
          controller.getProps().doClickAndHold() ? "" : "Stop",
          -1,
          true);
    }

    @Override
    public void useResult(AudioAnswer result) {
      audioPath = result.getPath();
      // path = getReadyToPlayAudio(path);

      setDownloadHref();
      scoreAudio(result);
      waitCursorHelper.showFinished();
    }

    @Override
    public void startRecording() {
      controller.logEvent(this, "RecordButton", getExerciseID(), "startRecording");

      playAudioPanel.setEnabled(false);
      playAudioPanel.hideScore();

      goodwaveExercisePanel.setBusy(true);

      super.startRecording();

      recordImage1.setVisible(true);
      downloadContainer.setVisible(false);
      waitCursorHelper.hide();
    }

    @Override
    public void stopRecording(long duration) {
      controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");

      playAudioPanel.setEnabled(true);
      goodwaveExercisePanel.setBusy(false);
      super.stopRecording(duration);

      recordImage1.setVisible(false);
      recordImage2.setVisible(false);
      waitCursorHelper.show();
    }

    @Override
    protected void postAudioFile(String base64EncodedWavFile) {
      super.postAudioFile(base64EncodedWavFile);
      waitCursorHelper.scheduleWaitTimer();
    }

    @Override
    protected AudioType getAudioType() {
      return AudioType.LEARN;
    }

    @Override
    public void flip(boolean first) {
      recordImage1.setVisible(first);
      recordImage2.setVisible(!first);
    }

    /**
     * @param result
     * @see RecordingListener#stopRecording(long)
     */
    @Override
    protected void useInvalidResult(AudioAnswer result) {
      super.useInvalidResult(result);
      playAudioPanel.setEnabled(false);
      waitCursorHelper.showFinished();
      waitCursorHelper.hide();
    }
  }

  int reqid = 0;

  /**
   * @param result
   * @see #useResult(PretestScore, boolean, String)
   */
  private void scoreAudio(AudioAnswer result) {
    logger.info("use " + result);

    scoreFeedback.clear();
    if (result.getPretestScore().getHydecScore() > 0) {
      scoreFeedback.add(new WordScoresTable().getStyledWordTable(result.getPretestScore()));
    } else {
      scoreFeedback.add(new Heading(4, "Score low, try again."));
    }
    useResult(result.getPretestScore(), false, result.getPath());
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
    }
    miniScoreListener.showChart();
  }
}
