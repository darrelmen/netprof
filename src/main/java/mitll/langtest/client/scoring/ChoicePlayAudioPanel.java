package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 4/5/17.
 */
class ChoicePlayAudioPanel extends PlayAudioPanel {
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
  CommonExercise exercise;
  SpeedStorage speedStorage;

  /**
   * @param soundManager
   * @param postAudioRecordButton1
   * @paramx xgoodwaveExercisePanel
   * @see SimpleRecordAudioPanel#makePlayAudioPanel
   */
  public ChoicePlayAudioPanel(SimpleRecordAudioPanel simpleRecordAudioPanel,
                              SoundManagerAPI soundManager,
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
    }, "", null);
    this.exid = exercise.getID();
    this.exercise = exercise;
    this.controller = exerciseController;
    firstRow.add(getRecordFeedback(simpleRecordAudioPanel.getWaitCursor()));
    downloadContainer = addDownloadAudioWidget();
    firstRow.add(simpleRecordAudioPanel.getScoreHistory());

    getElement().setId("SimpleRecordAudio_MyPlayAudioPanel");
  }

  /**
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton() {
    SplitDropdownButton playButton = new SplitDropdownButton(playLabel);

    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });

    playButton.setIcon(PLAY);
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");

    Widget widget = playButton.getWidget(0);
    Button actual = (Button) widget;
    actual.setEnabled(false);
    return actual;
  }

  AudioAttribute mr = null;
  AudioAttribute ms = null;
  AudioAttribute fr = null;
  AudioAttribute fs = null;

  private void addChoices(SplitDropdownButton playButton) {
    speedStorage = new SpeedStorage(controller, "speedChoice");
    Collection<Long> preferredVoices = Collections.emptyList();
    Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);

    mr = getAtSpeed(malesMap, true);
    ms = getAtSpeed(malesMap, false);

    Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);

    fr = getAtSpeed(femalesMap, true);
    fs = getAtSpeed(femalesMap, false);

    if (mr != null) {
      NavLink widget = new NavLink("Male Regular Speed");
      playButton.add(widget);
      widget.addClickHandler(getChoiceHandler(mr,true));
    }   if (ms != null) {
      NavLink widget = new NavLink("Male Slow Speed");
      playButton.add(widget);
      widget.addClickHandler(getChoiceHandler(ms,false));
    }  if (fr != null) {
      NavLink widget = new NavLink("Female Slow Speed");
      playButton.add(widget);
      widget.addClickHandler(getChoiceHandler(fr,false));
    }  if (fs != null) {
      NavLink widget = new NavLink("Female Slow Speed");
      playButton.add(widget);
      widget.addClickHandler(getChoiceHandler(fs,false));
    }
  }

  @NotNull
  private ClickHandler getChoiceHandler(AudioAttribute mr, boolean isReg) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

        playAndRemember(mr.getAudioRef(), isReg);
      }
    };
  }

  private void playAndRemember(String audioRef, boolean shouldKeepAudio) {
    playAudio(audioRef);
    speedStorage.storeIsRegular(shouldKeepAudio);
  }

  private AudioAttribute getAtSpeed(Map<MiniUser, List<AudioAttribute>> malesMap, boolean isReg) {
    for (List<AudioAttribute> attrs : malesMap.values()) {
      for (AudioAttribute audioAttribute : attrs) {
        if (isReg && audioAttribute.isRegularSpeed() || !isReg && audioAttribute.isSlow()) {
          return audioAttribute;

        }
      }
    }
    return null;
  }

  public void hidePlayButton() {
    playButton.setVisible(false);
  }

  public void showPlayButton() {
    playButton.setVisible(true);
  }

  public void flip(boolean first) {
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  public void showFirstRecord() {
    recordImage1.setVisible(true);
    downloadContainer.setVisible(false);
  }

  public void hideRecord() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
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

    this.firstRow = firstRow;
    //   firstRow.add(getRecordFeedback());
    // firstRow.add(downloadContainer = addDownloadAudioWidget());
    // downloadContainer = addDownloadAudioWidget();
    //f//irstRow.add(simpleRecordAudioPanel.getScoreHistory());
  }

  DivWidget firstRow;

  /**
   * @return
   * @see SimpleRecordAudioPanel#scoreAudio
   */
  public Panel getDownloadContainer() {
    return downloadContainer;
  }

  private DivWidget getRecordFeedback(Widget waitCursor) {
    DivWidget recordFeedback = new DivWidget();
    recordFeedback.addStyleName("inlineFlex");
    recordFeedback.getElement().setId("recordFeedbackImageContainer");
    //   recordFeedback.setWidth("32px");

    recordImage1 = new Image(UriUtils.fromSafeConstant(FIRST_RED));
    recordImage1.setVisible(false);
    recordImage1.setWidth("32px");
    recordImage2 = new Image(UriUtils.fromSafeConstant(SECOND_RED));
    recordImage2.setVisible(false);
    recordImage2.setWidth("32px");

    recordFeedback.add(recordImage1);
    recordFeedback.add(recordImage2);
    //Widget waitCursor = simpleRecordAudioPanel.waitCursorHelper.getWaitCursor();
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

  public void setDownloadHref(String audioPathToUse,
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
    //downloadAnchor.setHref(href);
  }

}
