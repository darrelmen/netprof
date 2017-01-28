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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An ASR scoring panel with a record button.
 */
public class ASRRecordAudioPanel<T extends CommonShell & AudioRefExercise & ScoredExercise>
    extends ASRScoringAudioPanel<T> {
  static final String DOWNLOAD_AUDIO = "downloadAudio";

  private static final String REFERENCE = "";
  private static final String RECORD_YOURSELF = "Record";
  private static final String RELEASE_TO_STOP = "Release";
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";

  /**
   * TODO : limit connection here...
   */
  private GoodwaveExercisePanel goodwaveExercisePanel;
  private final int index;
  private PostAudioRecordButton postAudioRecordButton;
  private MyPlayAudioPanel playAudioPanel;
  private IconAnchor download;
  private Anchor downloadAnchor;
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
  ASRRecordAudioPanel(GoodwaveExercisePanel goodwaveExercisePanel,
                      ExerciseController controller,
                      T exercise,
                      String instance) {
    super(exercise.getForeignLanguage(),
        exercise.getTransliteration(), controller,
        goodwaveExercisePanel.scorePanel,
        REFERENCE, exercise, instance);
    this.goodwaveExercisePanel = goodwaveExercisePanel;
    this.index = 1;
    getElement().setId("ASRRecordAudioPanel");
  }

  // TODO : add a subset of the ASRScorePanel.
//
//  public MiniScoreListener getScoreListener() {
//    return new ASRScorePanel("scorer",controller,exerciseID);
//  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @param toTheRightWidget
   * @param buttonTitle
   * @param recordButtonTitle
   * @return
   * @see AudioPanel#getPlayButtons
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String recordButtonTitle) {
    recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
    recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
    postAudioRecordButton = new MyPostAudioRecordButton(controller);
    postAudioRecordButton.getElement().getStyle().setMargin(8, Style.Unit.PX);
    playAudioPanel = new MyPlayAudioPanel(recordImage1,
        recordImage2, soundManager, postAudioRecordButton,
        goodwaveExercisePanel);
    return playAudioPanel;
  }

  protected void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript,
                           boolean scoredBefore, String path) {
    super.useResult(result, wordTranscript, phoneTranscript, scoredBefore, path);
    if (result.getHydecScore() > 0) {
      float zeroToHundred = result.getHydecScore() * 100f;
      //etASRGaugeValue(Math.min(100.0f, zeroToHundred));
      playAudioPanel.showDynamicRange(Math.min(100.0f, zeroToHundred));
    } else {
      playAudioPanel.hideScore();
    }
  }

  private class MyPlayAudioPanel extends PlayAudioPanel {
    public MyPlayAudioPanel(Image recordImage1, Image recordImage2, SoundManagerAPI soundManager,
                            final PostAudioRecordButton postAudioRecordButton1,
                            final GoodwaveExercisePanel goodwaveExercisePanel) {
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
      add(recordImage1);
      recordImage1.setVisible(false);
      add(recordImage2);
      recordImage2.setVisible(false);
      getElement().setId("GoodwaveExercisePanel_MyPlayAudioPanel");
    }

    /**
     * @param optionalToTheRight
     * @see PlayAudioPanel#PlayAudioPanel(SoundManagerAPI, String, Widget)
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      add(postAudioRecordButton);
      postAudioRecordButton.addStyleName("rightFiveMargin");
      super.addButtons(optionalToTheRight);

      addDownloadAudioWidget();

      scoreBar = getAfterPlayWidget();
      add(scoreBar);
    }

    Widget scoreBar;
    private ProgressBar progressBar;

    /**
     * Add dynamic range feedback to the right of the play button.
     *
     * @return
     * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
     */
    protected Widget getAfterPlayWidget() {
      HTML w = new HTML("Score");
      w.addStyleName("leftTenMargin");
      w.addStyleName("topBarMargin");
      Panel afterPlayWidget = new HorizontalPanel();

      afterPlayWidget.add(w);
      progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
      afterPlayWidget.add(progressBar);

      progressBar.setWidth("300px");
      Style style = progressBar.getElement().getStyle();
      style.setMarginLeft(5, Style.Unit.PX);
      progressBar.addStyleName("topBarMargin");

      afterPlayWidget.setVisible(false);
      return afterPlayWidget;
    }

    private static final int MIN_VALID_DYNAMIC_RANGE = 30;
    private static final int MIN_GOOD_DYNAMIC_RANGE  = 70;

    /**
     * Set the value on the progress bar to reflect the dynamic range we measure on the audio.
     *
     * @param result
     * @see #useResult(AudioAnswer)
     */
    public void showDynamicRange(double dynamicRange) {
      //   double dynamicRange = result.getDynamicRange();
      double percent = dynamicRange / 100d;
      String color = SimpleColumnChart.getColor((float) percent);

      logger.info("percent " + percent + " color " + color);

      progressBar.setPercent(100 * percent);
      progressBar.setText("" + Math.round(dynamicRange));//(dynamicRange));
      progressBar.setColor(dynamicRange > MIN_GOOD_DYNAMIC_RANGE ?
          ProgressBarBase.Color.SUCCESS : dynamicRange > MIN_VALID_DYNAMIC_RANGE ?
         ProgressBarBase.Color.WARNING :
          ProgressBarBase.Color.DANGER);

   //   progressBar.getElement().getStyle().setBackgroundColor(color);

      scoreBar.setVisible(true);
    }

    public void hideScore() {
      scoreBar.setVisible(false);
    }

    private void addDownloadAudioWidget() {
      downloadContainer = new DivWidget();
      downloadContainer.setWidth("40px");

      DivWidget north = new DivWidget();
      north.add(getDownloadIcon());
      downloadContainer.add(north);

//      DivWidget south = new DivWidget();
//      south.add(getDownloadAnchor());
//      downloadContainer.add(south);
      downloadContainer.setVisible(false);
      downloadContainer.addStyleName("leftFiveMargin");

      add(downloadContainer);
    }
  }

  private Anchor getDownloadAnchor() {
    downloadAnchor = new Anchor();
    downloadAnchor.setHTML("<span><font size=-1>Download</font></span>");
    addTooltip(downloadAnchor, DOWNLOAD_YOUR_RECORDING);

    downloadAnchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadAnchor,
            "DownloadUserAudio_Anchor",
            exercise,
            "downloading audio file " + audioPath);
      }
    });
    return downloadAnchor;
  }

  private IconAnchor getDownloadIcon() {
    download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    download.getElement().getStyle().setMarginLeft(19, Style.Unit.PX);

    addTooltip(download, DOWNLOAD_YOUR_RECORDING);

    download.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(download, "DownloadUserAudio_Icon", exercise,
            "downloading audio file " + audioPath);
      }
    });
    return download;
  }

  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @see #useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
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
    downloadAnchor.setHref(href);
  }

  protected int getUser() {
    return controller.getUserState().getUser();
  }

  /**
   * @see AudioPanel#makePlayAudioPanel(Widget, String, String)
   */
  private class MyPostAudioRecordButton extends PostAudioRecordButton {
    MyPostAudioRecordButton(ExerciseController controller) {
      super(//goodwaveExercisePanel.getLocalExercise().getID(),
          exerciseID,
          controller,
          ASRRecordAudioPanel.this.index,
          true,
          RECORD_YOURSELF,
          controller.getProps().doClickAndHold() ? RELEASE_TO_STOP : "Stop"
      );
//            RECORD_YOURSELF, controller.getProps().doClickAndHold() ? RELEASE_TO_STOP : "Stop", Result.AUDIO_TYPE_PRACTICE);
    }

    @Override
    public void useResult(AudioAnswer result) {
      setResultID(result.getResultID());
      getImagesForPath(result.getPath());
      setDownloadHref();
    }

    @Override
    public void startRecording() {
      playAudioPanel.setEnabled(false);
      playAudioPanel.hideScore();
      goodwaveExercisePanel.setBusy(true);
      controller.logEvent(this, "RecordButton", getExerciseID(), "startRecording");

      super.startRecording();
      recordImage1.setVisible(true);
      downloadContainer.setVisible(false);
    }

    @Override
    public void stopRecording(long duration) {
      controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");

      playAudioPanel.setEnabled(true);
      goodwaveExercisePanel.setBusy(false);
      super.stopRecording(duration);
      recordImage1.setVisible(false);
      recordImage2.setVisible(false);
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
