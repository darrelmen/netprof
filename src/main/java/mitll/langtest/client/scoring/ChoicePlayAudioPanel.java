package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.MyCustomIconType;
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
  public static final String IS_MALE = "isMale";
  public static final String IS_REG = "isReg";
  public static final String MALE = "male";
  public static final String SLOW = "slow";
  private int exid;
  private ExerciseController controller;
  private CommonExercise exercise;

  /**
   * @paramx soundManager
   * @paramx postAudioRecordButton1
   * @paramx xgoodwaveExercisePanel
   * @see TwoColumnExercisePanel#getPlayAudioPanel(CommonExercise, AudioAttribute)
   */
  public ChoicePlayAudioPanel(
      SoundManagerAPI soundManager,
      CommonExercise exercise,
      ExerciseController exerciseController) {
    super(soundManager, new PlayListener() {
          public void playStarted() {
//          goodwaveExercisePanel.setBusy(true);
            // TODO put back busy thing?
//            postAudioRecordButton1.setEnabled(false);
          }

          public void playStopped() {
            //  goodwaveExercisePanel.setBusy(false);
            //          postAudioRecordButton1.setEnabled(true);
          }
        },
        "",
        null);
    this.exid = exercise.getID();
    this.exercise = exercise;
    this.controller = exerciseController;

    getElement().setId("ChoicePlayAudioPanel");
  }

  /**
   * @param toAddTo
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton(DivWidget toAddTo) {
    SplitDropdownButton playButton = new SplitDropdownButton(playLabel);
    playButton.getElement().setId("splitPlayAudio");
    toAddTo.add(playButton);
    configureButton2(playButton);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        addChoices(playButton);
      }
    });

    Widget widget = playButton.getWidget(0);
    Button actual = (Button) widget;
    actual.setEnabled(false);
    return actual;
  }

  private void configureButton2(SplitDropdownButton playButton) {
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
  }

  private void addChoices(SplitDropdownButton playButton) {
    Collection<Long> preferredVoices = Collections.emptyList();
    Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
    Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);

    KeyStorage storage = controller.getStorage();
    boolean hasGender = storage.hasValue(IS_MALE);
    boolean isMale = hasGender && storage.getValue(IS_MALE).equals(MALE) || controller.getUserManager().isMale();
    boolean isSlow = storage.getValue(IS_REG).equals(SLOW);

    AudioAttribute toUse = null;
    AudioAttribute fallback = null;

    {
      AudioAttribute mr = getAtSpeed(malesMap, true);
      if (mr != null) {
        if (playButton != null) addAudioChoice(playButton, true, true, mr);
        if (isMale && !isSlow) toUse = mr;
        else fallback = mr;
      }
    }
    {
      AudioAttribute ms = getAtSpeed(malesMap, false);
      if (ms != null) {
        if (playButton != null) addAudioChoice(playButton, true, false, ms);
        if (isMale && isSlow) toUse = ms;
        if (fallback == null) fallback = ms;
      }
    }
    {
      AudioAttribute fr = getAtSpeed(femalesMap, true);
      if (fr != null) {
        if (playButton != null) addAudioChoice(playButton, false, true, fr);
        if (!isMale && !isSlow) toUse = fr;
        if (fallback == null) fallback = fr;
      }
    }

    {
      AudioAttribute fs = getAtSpeed(femalesMap, false);
      if (fs != null) {
        if (playButton != null) addAudioChoice(playButton, false, false, fs);
        if (!isMale && !isSlow) toUse = fs;
        if (fallback == null) fallback = fs;
      }
    }

    if (toUse == null) toUse = fallback;
    setEnabled(toUse != null);
    if (toUse != null) {
      loadAudio(toUse.getAudioRef());
      current = toUse;
    }
  }

  private void addAudioChoice(SplitDropdownButton playButton, boolean isMale1, boolean isReg, AudioAttribute mr) {
    NavLink widget = addAudioChoice(playButton, isMale1, isReg);
    widget.addClickHandler(getChoiceHandler(mr, isMale1, isReg));
  }

  @NotNull
  private NavLink addAudioChoice(SplitDropdownButton playButton, boolean isMale1, boolean isReg) {
   // String male = isMale1 ? "Male" : "Female";
   // String regular = isReg ? "Regular" : "Slow";
    NavLink widget = new NavLink();//male + " " + regular + " Speed");
    playButton.add(widget);

//    if (isMale1) {
//    IconAnchor w = new IconAnchor();
//    w.setIconSize(IconSize.THREE_TIMES);
//    w.setIcon(isMale1 ? IconType.MALE : IconType.FEMALE);
//    widget.add(w);

    widget.setIcon(isMale1 ? IconType.MALE : IconType.FEMALE);
    widget.setIconSize(IconSize.TWO_TIMES);
    //  }

//    if (!isReg) {
    //    widget.setBaseIcon(isReg?MyCustomIconType.rabbit:          MyCustomIconType.turtle);

    IconAnchor speed = new IconAnchor();
    //speed.setIconSize(IconSize.THREE_TIMES);
    speed.setBaseIcon(isReg ? MyCustomIconType.rabbit : MyCustomIconType.turtle);
    widget.add(speed);
    speed.addStyleName("leftFiveMargin");
    //  }
playButton.getMenuWiget().getElement().getStyle().clearProperty("minWidth");
//    playButton.getElement().getStyle().setProperty("minWidth","80px");
    return widget;
  }

  private AudioAttribute current = null;

  private boolean matchesUserChoices() {
    if (current != null) {
      KeyStorage storage = controller.getStorage();
      return (!storage.hasValue(IS_MALE) || current.isMale() == storage.isTrue(IS_MALE)) &&
          (!storage.hasValue(IS_REG) || current.isRegularSpeed() == storage.isTrue(IS_REG));
    } else return false;
  }

  @Override
  protected void startPlaying() {
/*    if (!matchesUserChoices()) {
      addChoices(null);
    }*/
    super.startPlaying();
  }

  @NotNull
  private ClickHandler getChoiceHandler(AudioAttribute mr, boolean isMale, boolean isReg) {
    return event -> playAndRemember(mr.getAudioRef(), isMale, isReg);
  }

  private void playAndRemember(String audioRef, boolean isMale, boolean isReg) {
    playAudio(audioRef);
    controller.getStorage().setBoolean(IS_MALE, isMale);
    controller.getStorage().setBoolean(IS_REG, isReg);
    //speedStorage.storeIsSet(shouldKeepAudio);
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


  /**
   * @param optionalToTheRight
   * @see PlayAudioPanel#PlayAudioPanel
   */
  @Override
  protected void addButtons(Widget optionalToTheRight) {
    playButton = makePlayButton(this);
  }
}
