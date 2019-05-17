/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import static mitll.langtest.client.LangTest.RED_X_URL;

/**
 * Created by go22670 on 4/5/17.
 *
 * @see SimpleRecordAudioPanel#makePlayAudioPanel
 */
public class RecorderPlayAudioPanel extends PlayAudioPanel {
  //private final Logger logger = Logger.getLogger("RecorderPlayAudioPanel");

  private static final String FIRST_RED = LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png";
  private static final SafeUri firstRed = UriUtils.fromSafeConstant(FIRST_RED);
  private static final String HEIGHT = 19 + "px";
  private static final String BORDER_RADIUS = 18 + "px";

  private static final String RED_ACTIVE_COLOR = "#da4f49";
  /**
   * @see RecordDialogExercisePanel#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  static final String BLUE_INACTIVE_COLOR = "#0171bc";

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  private Widget recordImage1;
  private final boolean useMicrophoneIcon;
  /**
   *
   */
  private Image redX;
  private final DownloadContainer downloadContainer;

  /**
   * @param postAudioRecordButton1
   * @param controller
   * @param exercise
   * @param useMicrophoneIcon
   * @see NoFeedbackRecordAudioPanel#makePlayAudioPanel
   */
  RecorderPlayAudioPanel(final Button postAudioRecordButton1, ExerciseController controller, HasID exercise, boolean useMicrophoneIcon) {
    super(new PlayListener() {
            public void playStarted() {
              // TODO put back busy thing?
              postAudioRecordButton1.setEnabled(false);
            }

            public void playStopped() {
              postAudioRecordButton1.setEnabled(true);
            }
          },
        "",
        null, controller, exercise.getID(), true);

    downloadContainer = new DownloadContainer();
    this.useMicrophoneIcon = useMicrophoneIcon;
    getElement().setId("RecorderPlayAudioPanel");
  }

  public void showPlayButton() {
    playButton.setVisible(true);
  }

  void hidePlayButton() {
    playButton.setVisible(false);
  }

  void showFirstRecord() {
    if (controller.shouldRecord()) {
      //   logger.info("showFirstRecording " + exid + " red recording signal now visible!");
      showFeedback();
    } else {
      redX.setVisible(true);
    }
    downloadContainer.getDownloadContainer().setVisible(false);
  }

  private void showFeedback() {
    recordImage1.addStyleName("hvr-pulse");
    if (useMicrophoneIcon) {
      Style style1 = recordImage1.getElement().getStyle();
      //   style1.setBackgroundColor("lightBlue");
      style1.setBackgroundColor(RED_ACTIVE_COLOR);
    } else {
      recordImage1.setVisible(true);
    }
  }

  private void hideFeedback() {
    recordImage1.removeStyleName("hvr-pulse");

    if (useMicrophoneIcon) {
      Style style1 = recordImage1.getElement().getStyle();
      style1.setBackgroundColor(BLUE_INACTIVE_COLOR);//"lightBlue");
      //  style1.setBackgroundColor("blue");
//    style1.setBackgroundColor("#da4f49");
    } else {
      recordImage1.setVisible(false);
    }
  }

  /**
   * @see NoFeedbackRecordAudioPanel#stopRecording()
   */
  void hideRecord() {
    if (controller.shouldRecord()) {
      // logger.info("hideRecord " + exid);
      hideFeedback();
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
    Button playButton = getNewButton();

    configureButton(playButton);
    return playButton;
  }

  /**
   * @param playButton
   * @see #makePlayButton(DivWidget)
   */
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

  /**
   * @param waitCursor null OK
   * @return
   * @see SimpleRecordAudioPanel#addWidgets
   */
  DivWidget getRecordFeedback(Widget waitCursor) {
    DivWidget recordFeedback = new DivWidget();
    recordFeedback.addStyleName("inlineFlex");
    recordFeedback.addStyleName("floatLeft");
    recordFeedback.getElement().setId("recordFeedbackImageContainer");

    recordImage1 = useMicrophoneIcon ? getRecordImage() : new Image(firstRed);

    hideFeedback();
    if (!useMicrophoneIcon) {
      recordImage1.setWidth("32px");
    }

    //  recordImage1.addStyleName("hvr-pulse");

    if (controller.shouldRecord()) {
      recordFeedback.add(recordImage1);
      if (waitCursor != null) {
        recordFeedback.add(waitCursor);
      }
    } else {
      recordFeedback.add(redX = getRedX());
    }

    return recordFeedback;
  }

  /**
   * For when we want just the microphone to show...
   *
   * @return
   */
  @NotNull
  private Widget getRecordImage() {
    Icon icon = new Icon(IconType.MICROPHONE);
    Style style = icon.getElement().getStyle();

    style.setMarginLeft(4, Style.Unit.PX);

    style.setColor("white");
    icon.setSize(IconSize.LARGE);

    DivWidget container = new DivWidget();
    container.getElement().setId("micContainer");
    container.setHeight(HEIGHT);
    container.setWidth(HEIGHT);

    Style style1 = container.getElement().getStyle();

    {
      style1.setPadding(8, Style.Unit.PX);
      style1.setProperty("borderRadius", BORDER_RADIUS);
      style1.setMarginTop(-5, Style.Unit.PX);
      style1.setMarginLeft(2, Style.Unit.PX);
      style1.setMarginRight(5, Style.Unit.PX);
      style1.setMarginBottom(5, Style.Unit.PX);
    }

    container.add(icon);
    style1.setBackgroundColor("lightBlue");
//    style1.setBackgroundColor("#da4f49");
    return useMicrophoneIcon ? container : new Image(firstRed);
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

  DownloadContainer getRealDownloadContainer() {
    return downloadContainer;
  }
}
