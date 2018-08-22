package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 *
 * @see SimpleRecordAudioPanel#makePlayAudioPanel
 */
class RecorderPlayAudioPanel extends PlayAudioPanel {
  protected final Logger logger = Logger.getLogger("RecorderPlayAudioPanel");

  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final SafeUri firstRed = UriUtils.fromSafeConstant(FIRST_RED);

/*
 private static final String SECOND_RED = LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png";
  private static final SafeUri secondRed = UriUtils.fromSafeConstant(SECOND_RED);
  */

  private static final String RED_X = LangTest.LANGTEST_IMAGES + "redx32.png";
  private static final SafeUri RED_X_URL = UriUtils.fromSafeConstant(RED_X);

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Image recordImage1;
/*
  private Image recordImage2;
*/
  private Image redX;
  private final DownloadContainer downloadContainer;
  private boolean canRecord;

  /**
   * @param postAudioRecordButton1
   * @param controller
   * @param exercise
   * @see SimpleRecordAudioPanel#makePlayAudioPanel
   */
  RecorderPlayAudioPanel(final Button postAudioRecordButton1, ExerciseController controller, HasID exercise) {
    super(new PlayListener() {
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
        null, controller, exercise.getID(), true);

    downloadContainer = new DownloadContainer();
    getElement().setId("RecorderPlayAudioPanel");
  }

  private void configureButton(Button playButton) {
    playButton.addClickHandler(event -> doPlayPauseToggle());
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

  /**
   * @see NoFeedbackRecordAudioPanel#flip
   * @paramx first
   */
/*  public void flip(boolean first) {
    if (canRecord) {
      recordImage1.setVisible(first);
*//*
      recordImage2.setVisible(!first);
*//*
    }
  }*/

  void showFirstRecord() {
    if (canRecord) {
      logger.info("showFirstRecording " + exid);
      recordImage1.setVisible(true);
    } else {
      redX.setVisible(true);
    }
    downloadContainer.getDownloadContainer().setVisible(false);
  }

  /**
   * @see NoFeedbackRecordAudioPanel#stopRecording()
   */
  void hideRecord() {
    if (canRecord) {
      logger.info("hideRecord " + exid);
      recordImage1.setVisible(false);
    //  recordImage2.setVisible(false);
    } else {
      redX.setVisible(false);
    }
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
   * @param waitCursor null OK
   * @param canRecord
   * @return
   * @see SimpleRecordAudioPanel#addWidgets
   */
  DivWidget getRecordFeedback(Widget waitCursor, boolean canRecord) {
    DivWidget recordFeedback = new DivWidget();
    recordFeedback.addStyleName("inlineFlex");
    recordFeedback.addStyleName("floatLeft");
    recordFeedback.getElement().setId("recordFeedbackImageContainer");

    recordImage1 = new Image(firstRed);
    recordImage1.setVisible(false);
    recordImage1.setWidth("32px");

    recordImage1.addStyleName("hvr-pulse");
/*

    recordImage2 = new Image(secondRed);
    recordImage2.setVisible(false);
    recordImage2.setWidth("32px");
*/

    this.canRecord = canRecord;
    if (canRecord) {
      recordFeedback.add(recordImage1);
/*
      recordFeedback.add(recordImage2);
*/
      if (waitCursor != null) {
        recordFeedback.add(waitCursor);
      }
    } else {
      recordFeedback.add(redX = getRedX());
    }

    return recordFeedback;
  }

  @NotNull
  private Image getRedX() {
    Image image = new Image(RED_X_URL);
    image.setVisible(false);
    return image;
  }

  /**
   * @param audioPathToUse
   * @param id
   * @param user
   * @param host
   * @see SimpleRecordAudioPanel#setDownloadHref
   */
  void setDownloadHref(String audioPathToUse,
                       int id,
                       int user,
                       String host) {
    downloadContainer.setDownloadHref(audioPathToUse, id, user, host);
  }
  /**
   * @return
   * @see SimpleRecordAudioPanel#scoreAudio
   */
  Panel getDownloadContainer() {
    return downloadContainer.getDownloadContainer();
  }

  DownloadContainer getRealDownloadContainer() {
    return downloadContainer;
  }
}
