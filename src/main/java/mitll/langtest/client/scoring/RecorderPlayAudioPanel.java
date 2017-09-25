package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;

/**
 * Created by go22670 on 4/5/17.
 *
 * @see SimpleRecordAudioPanel#makePlayAudioPanel
 */
class RecorderPlayAudioPanel extends PlayAudioPanel {
  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Image recordImage1;
  private Image recordImage2;
  private final DownloadContainer downloadContainer;

  /**
   * @param soundManager
   * @param postAudioRecordButton1
   * @see SimpleRecordAudioPanel#makePlayAudioPanel
   */
  RecorderPlayAudioPanel(SoundManagerAPI soundManager, final Button postAudioRecordButton1) {
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

    downloadContainer = new DownloadContainer();
    getElement().setId("RecorderPlayAudioPanel");
  }

  private void configureButton(Button playButton) {
    playButton.addClickHandler(event -> doClick());
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
    downloadContainer.getDownloadContainer().setVisible(false);
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
   * @param ignoredDiv
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton(DivWidget ignoredDiv) {
    Button playButton = new Button(playLabel);
    configureButton(playButton);
    return playButton;
  }

  /**
   * @return
   * @see SimpleRecordAudioPanel#scoreAudio
   */
  Panel getDownloadContainer() {
    return downloadContainer.getDownloadContainer();
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

  /**
   * @see SimpleRecordAudioPanel#setDownloadHref
   * @param audioPathToUse
   * @param id
   * @param user
   * @param host
   */
  void setDownloadHref(String audioPathToUse,
                       int id,
                       int user,
                       String host) {
    downloadContainer.setDownloadHref(audioPathToUse, id, user, host);
  }

  public DownloadContainer getRealDownloadContainer() {
    return downloadContainer;
  }
}
