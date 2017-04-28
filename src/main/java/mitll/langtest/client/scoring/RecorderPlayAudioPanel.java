package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.exercise.CommonExercise;

/**
 * Created by go22670 on 4/5/17.
 */
class RecorderPlayAudioPanel extends PlayAudioPanel {
  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Image recordImage1;
  private Image recordImage2;
  private IconAnchor download;
  private Panel downloadContainer;
  private int exid;
  private ExerciseController controller;

  /**
   * @param soundManager
   * @param postAudioRecordButton1
   * @see SimpleRecordAudioPanel#makePlayAudioPanel
   */
  RecorderPlayAudioPanel(SoundManagerAPI soundManager,
                                final PostAudioRecordButton postAudioRecordButton1,
                                CommonExercise exercise,
                                ExerciseController exerciseController) {
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
        },
        "",
        null);
    this.exid = exercise.getID();
    this.controller = exerciseController;
    downloadContainer = addDownloadAudioWidget();
    getElement().setId("RecorderPlayAudioPanel");
  }

  private void configureButton(Button playButton) {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });

//    logger.info("configureButton " + playButton.getElement().getId());
    playButton.setIcon(PLAY);
    playButton.setType(ButtonType.INFO);
    playButton.setSize(ButtonSize.LARGE);
    playButton.getElement().setId("PlayAudioPanel_playButton_recorder");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
  }

  void showPlayButton() {
    playButton.setVisible(true);
  }
  void hidePlayButton() {
    playButton.setVisible(false);
  }

  public void flip(boolean first) {
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  void showFirstRecord() {
    recordImage1.setVisible(true);
    downloadContainer.setVisible(false);
  }

  void hideRecord() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
  }

  /**
   * @param optionalToTheRight
   * @see PlayAudioPanel#PlayAudioPanel
   */
  @Override
  protected void addButtons(Widget optionalToTheRight) {
    playButton = makePlayButton(this);
  }

  /**
   * @param toAddTo
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton(DivWidget toAddTo) {
    Button playButton = new Button(playLabel);
    configureButton(playButton);
    return playButton;
  }

  /**
   * @return
   * @see SimpleRecordAudioPanel#scoreAudio
   */
  Panel getDownloadContainer() {
    return downloadContainer;
  }

  /**
   * @param waitCursor
   * @return
   * @see SimpleRecordAudioPanel#addWidgets
   */
  DivWidget getRecordFeedback(Widget waitCursor) {
    DivWidget recordFeedback = new DivWidget();
    recordFeedback.addStyleName("inlineFlex");
    recordFeedback.addStyleName("floatLeft");
    recordFeedback.getElement().setId("recordFeedbackImageContainer");

    recordImage1 = new Image(UriUtils.fromSafeConstant(FIRST_RED));
    recordImage1.setVisible(false);
    recordImage1.setWidth("32px");
    recordImage2 = new Image(UriUtils.fromSafeConstant(SECOND_RED));
    recordImage2.setVisible(false);
    recordImage2.setWidth("32px");

    recordFeedback.add(recordImage1);
    recordFeedback.add(recordImage2);
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

  /**
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
        controller.logEvent(download, "DownloadUserAudio_Icon", exid,
            "downloading audio file ");
      }
    });
    return download;
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  void setDownloadHref(String audioPathToUse,
                       int id,
                       int user) {
    downloadContainer.setVisible(true);
    String href = DOWNLOAD_AUDIO +
        "?file=" +
        audioPathToUse +
        "&" +
        "exerciseID=" +
        id +
        "&" +
        "userID=" +
        user;
    download.setHref(href);
  }
}
