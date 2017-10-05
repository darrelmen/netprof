package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRHistoryPanel;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonAudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;

import static mitll.langtest.client.download.DownloadContainer.getDownloadAudio;

/**
 * An ASR scoring panel with a record button.
 */
public class ASRRecordAudioPanel<T extends CommonExercise> extends ASRScoringAudioPanel<T> {
  private static final String REFERENCE = "";
  private static final String RECORD_YOURSELF = "Record";
  private static final String RELEASE_TO_STOP = "Release";
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";
  private static final String FIRST_RED  = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";

  public static final int FIRST_STEP  = 35;
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

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Image recordImage1;
  private Image recordImage2;

  /**
   * @param controller
   * @param exercise
   * @param instance
   * @see GoodwaveExercisePanel#getAnswerWidget
   */
  ASRRecordAudioPanel(BusyPanel goodwaveExercisePanel,
                      ExerciseController controller,
                      T exercise,
                      String instance) {
    super(exercise.getForeignLanguage(),
        exercise.getTransliteration(), controller,
        null,
        REFERENCE, exercise, instance);
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.index = 1;
    //   this.historyPanel =
    getElement().setId("ASRRecordAudioPanel");
  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @param toTheRightWidget
   * @param buttonTitle
   * @param recordButtonTitle
   * @param exercise
   * @return
   * @see AudioPanel#getPlayButtons
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String recordButtonTitle, CommonAudioExercise exercise) {
    recordImage1 = new Image(UriUtils.fromSafeConstant(FIRST_RED));
    recordImage1.setVisible(false);
    recordImage2 = new Image(UriUtils.fromSafeConstant(SECOND_RED));
    recordImage2.setVisible(false);

    postAudioRecordButton = new MyPostAudioRecordButton(controller);
    postAudioRecordButton.getElement().getStyle().setMargin(8, Style.Unit.PX);
    playAudioPanel = new MyPlayAudioPanel(soundManager, postAudioRecordButton, controller, this.exercise);
    return playAudioPanel;
  }

  protected void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript,
                           boolean scoredBefore, String path) {
    super.useResult(result, wordTranscript, phoneTranscript, scoredBefore, path);
    if (result.getHydecScore() > 0) {
      float zeroToHundred = result.getHydecScore() * 100f;
      //etASRGaugeValue(Math.min(100.0f, zeroToHundred));
      playAudioPanel.showScore(Math.min(100.0f, zeroToHundred));
    } else {
      playAudioPanel.hideScore();
    }
  }

  private class MyPlayAudioPanel extends PlayAudioPanel {
    private DivWidget recordFeedback = new DivWidget();
    private Widget scoreBar;
    private ProgressBar progressBar;
    DivWidget scores;

    /**
     * @param soundManager
     * @param postAudioRecordButton1
     * @param controller
     * @param exercise
     * @paramx xgoodwaveExercisePanel
     * @see AudioPanel#makePlayAudioPanel
     */
    public MyPlayAudioPanel(SoundManagerAPI soundManager,
                            final PostAudioRecordButton postAudioRecordButton1,
                            ExerciseController controller, CommonExercise exercise) {
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
      }, "", null, controller, exercise, true);
      getElement().setId("GoodwaveExercisePanel_MyPlayAudioPanel");
    }

    /**
     * @param optionalToTheRight
     * @see PlayAudioPanel#PlayAudioPanel
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      add(postAudioRecordButton);
      postAudioRecordButton.addStyleName("rightFiveMargin");

      super.addButtons(optionalToTheRight);

      add(recordFeedback = new DivWidget());
      recordFeedback.setWidth("32px");
      recordFeedback.add(recordImage1);
      recordFeedback.add(recordImage2);

      downloadContainer = addDownloadAudioWidget();

      progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
      scoreBar = getAfterPlayWidget(progressBar);
      scoreBar.addStyleName("floatLeft");

      scores = new DivWidget();

      scores.add(scoreBar);

      ASRHistoryPanel historyPanel = new ASRHistoryPanel(controller, exerciseID);
      addMinicoreListener(historyPanel);
      historyPanel.addStyleName("floatLeft");
      scores.add(historyPanel);
      historyPanel.showChart(controller.getHost());

      add(scores);
    }


    /**
     * Add score feedback to the right of the play button.
     *
     * @return
     * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
     */
    Widget getAfterPlayWidget(ProgressBar progressBar) {
      HTML label = new HTML("Score");
      label.addStyleName("topFiveMargin");
      label.addStyleName("leftTenMargin");
      label.addStyleName("floatLeft");
      Panel afterPlayWidget = new DivWidget();

      afterPlayWidget.add(label);
      afterPlayWidget.add(progressBar);

      progressBar.setWidth("300px");
      progressBar.addStyleName("floatLeft");

      Style style = progressBar.getElement().getStyle();
      style.setMarginTop(5, Style.Unit.PX);
      style.setMarginLeft(5, Style.Unit.PX);
      style.setMarginBottom(0, Style.Unit.PX );

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
      //   double score = result.getDynamicRange();
      double percent = score / 100d;
      String color = SimpleColumnChart.getColor((float) percent);

      logger.info("percent " + percent + " color " + color);

      progressBar.setPercent(100 * percent);
      progressBar.setText("" + Math.round(score));//(score));
      progressBar.setColor(score > SECOND_STEP ?
          ProgressBarBase.Color.SUCCESS : score > FIRST_STEP ?
          ProgressBarBase.Color.WARNING :
          ProgressBarBase.Color.DANGER);

      //   progressBar.getElement().getStyle().setBackgroundColor(color);

      scoreBar.setVisible(true);
      scores.setVisible(true);
    }

    void hideScore() {
      scoreBar.setVisible(false);
      scores.setVisible(false);
    }

    private DivWidget addDownloadAudioWidget() {
      DivWidget downloadContainer = new DivWidget();
      downloadContainer.setWidth("40px");

      DivWidget north = new DivWidget();
      north.add(download = getDownloadIcon());
      downloadContainer.add(north);
      downloadContainer.setVisible(false);
      downloadContainer.addStyleName("leftFiveMargin");
      return downloadContainer;
    }
  }

  private IconAnchor getDownloadIcon() {
    IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    download.getElement().getStyle().setMarginLeft(19, Style.Unit.PX);

    addTooltip(download, DOWNLOAD_YOUR_RECORDING);

    download.addClickHandler(event -> controller.logEvent(download, "DownloadUserAudio_Icon", exercise,
        "downloading audio file " + audioPath));
    return download;
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
   */
  private void setDownloadHref() {
    String audioPathToUse = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;

    downloadContainer.setVisible(true);

    String href = getDownloadAudio(controller.getHost()) +
        "?file=" +
        audioPathToUse +
        "&" +
        "exerciseID=" +
        exercise.getID() +
        "&" +
        "userID=" +
        getUser();
    download.setHref(href);
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }

  /**
   * @see AudioPanel#makePlayAudioPanel(Widget, String, String, AudioRefExercise)
   */
  private class MyPostAudioRecordButton extends PostAudioRecordButton {
    MyPostAudioRecordButton(ExerciseController controller) {
      super(
          exerciseID,
          controller,
          ASRRecordAudioPanel.this.index,
          true,
          RECORD_YOURSELF,
          controller.getProps().doClickAndHold() ? RELEASE_TO_STOP : "Stop",
          93, false);
    }

    @Override
    public void useResult(AudioAnswer result) {
      setResultID(result.getResultID());
      getImagesForPath(result.getPath());
      setDownloadHref();
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
    }

    @Override
    public boolean stopRecording(long duration) {
      controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");

      playAudioPanel.setEnabled(true);
      goodwaveExercisePanel.setBusy(false);
      boolean b = super.stopRecording(duration);

      recordImage1.setVisible(false);
      recordImage2.setVisible(false);

      return b;
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
    }
  }
}
