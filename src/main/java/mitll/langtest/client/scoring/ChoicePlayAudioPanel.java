package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
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

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
class ChoicePlayAudioPanel extends PlayAudioPanel {
  private final Logger logger = Logger.getLogger("ChoicePlayAudioPanel");

  private static final String FAST = "Fast";
  private static final String SLOW1 = "Slow";

  private static final String IS_MALE = "isMale";
  private static final String IS_REG = "isReg";
  public static final String MALE = "male";
  public static final String SLOW = "slow";

  private boolean includeContext;
  private AudioAttribute currentAudioAttr = null;
  private final AudioChangeListener listener;
  private Set<AudioAttribute> allPossible;
  private int exid;

  /**
   * @see TwoColumnExercisePanel#getPlayAudioPanel
   * @see TwoColumnExercisePanel#getContext
   */
  ChoicePlayAudioPanel(
      SoundManagerAPI soundManager,
      CommonExercise exercise,
      ExerciseController exerciseController,
      boolean includeContext,
      AudioChangeListener listener) {
    super(soundManager,null,
        "",
        null, exerciseController, exercise, false);
    this.includeContext = includeContext;
    this.listener = listener;
    this.exid = exercise.getID();
    // getElement().setId("ChoicePlayAudioPanel");
    addButtons(null);

/*    addPlayListener( new PlayListener() {
      public void playStarted() {
//          goodwaveExercisePanel.setBusy(true);
        // TODO put back busy thing?
//            postAudioRecordButton1.setEnabled(false);
           gotPlayStarted();
      }

      public void playStopped() {
        //  goodwaveExercisePanel.setBusy(false);
        //          postAudioRecordButton1.setEnabled(true);
      }
    });*/

    LangTest.EVENT_BUS.addHandler(AudioSelectedEvent.TYPE, authenticationEvent -> {
      gotAudioSelected(authenticationEvent.getExid());
    });
  }

/*
  private void gotPlayStarted() {
    logger.info("---> got play started ");
  }
*/

  /**
   * @param optionalToTheRight
   * @see PlayAudioPanel#PlayAudioPanel
   */
  @Override
  protected void addButtons(Widget optionalToTheRight) {
    playButton = makePlayButton(this);
  }

  private void gotAudioSelected(int exid) {
    if (exercise != null && exid != exercise.getID()) {
     // logger.info("gotAudioSelected choosing different audio for " + exercise.getID());
      addChoices(null, includeContext, null, true);
    }
  }

  private SplitDropdownButton splitDropdownButton;

  /**
   * @param toAddTo
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton(DivWidget toAddTo) {
    SplitDropdownButton playButton = new SplitDropdownButton(playLabel);
    playButton.setSize(ButtonSize.LARGE);

    splitDropdownButton = playButton;
    playButton.getElement().setId("splitPlayAudio");
    toAddTo.add(playButton);
    configureButton2(playButton);

    // Scheduler.get().scheduleDeferred(() -> addChoices(playButton, includeContext));

    Widget widget = playButton.getWidget(0);
    Button actual = (Button) widget;
    actual.setEnabled(false);

    addChoices(playButton, includeContext, actual, false);

    return actual;
  }

  private void configureButton2(SplitDropdownButton playButton) {
    playButton.addClickHandler(event -> playAudio());

    playButton.setIcon(PLAY);
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
    playButton.addStyleName("choiceplay");
  }

  /**
   * Shows check mark next to current audio choice.
   *
   *
   * @param playButton     null if just getting an update event
   * @param includeContext
   * @param tellListener
   * @see #gotAudioSelected
   * @see #makePlayButton
   */
  private void addChoices(SplitDropdownButton playButton, boolean includeContext, Button actual, boolean tellListener) {
    // what we want to match, as closely as possible
    boolean isMale = isMale();
    boolean isFemale = !isMale;
    boolean isReg = isSpeedReg();
    boolean isSlow = !isReg;

    Collection<Integer> preferredVoices = Collections.emptyList(); // bogus for now...
    Map<MiniUser, List<AudioAttribute>> malesMap =
        exercise.getMostRecentAudio(true, preferredVoices, includeContext);
    Map<MiniUser, List<AudioAttribute>> femalesMap =
        exercise.getMostRecentAudio(false, preferredVoices, includeContext);

/*
    logger.info("addChoices for exercise " + exercise.getID() + " " + exercise.getEnglish() + " " +
        "\n\tmale       " + isMale +
        "\n\tis reg     " + isReg +
        "\n\tmale map   " + malesMap.size() +
        "\n\tfemale map " + femalesMap.size());*/

    AudioAttribute toUse = null;
    AudioAttribute fallback = null;
    AudioAttribute genderFallback = null;

    allPossible = new HashSet<>();

    boolean hasPlayButton = playButton != null;
    {
      AudioAttribute mr = getAtSpeed(malesMap, true);
      if (mr != null) {
        allPossible.add(mr);
        if (hasPlayButton) addAudioChoice(playButton, true, true, mr);
        if (isMale && isReg) toUse = mr;
        else if (isMale) genderFallback = mr;
        else fallback = mr;
      } else {
        //logger.info("no male reg in " +malesMap);
      }
    }

    AudioAttribute ms = getAtSpeed(malesMap, false);
    if (ms != null) {
      allPossible.add(ms);
      if (hasPlayButton) addAudioChoice(playButton, true, false, ms);
      if (isMale && isSlow) toUse = ms;
      else if (isMale) genderFallback = ms;
      if (fallback == null) fallback = ms;
    }

    AudioAttribute fr = getAtSpeed(femalesMap, true);
    if (fr != null) {
      allPossible.add(fr);
      if (hasPlayButton) addAudioChoice(playButton, false, true, fr);
      if (isFemale && isReg) toUse = fr;
      else if (isFemale) genderFallback = fr;
      if (fallback == null) fallback = fr;
    }

    AudioAttribute fs = getAtSpeed(femalesMap, false);
    if (fs != null) {
      allPossible.add(fs);

      if (hasPlayButton) addAudioChoice(playButton, false, false, fs);
      if (isFemale && isSlow) toUse = fs;
      else if (isFemale) genderFallback = fs;
      if (fallback == null) fallback = fs;
    }

    // try to match gender, if possible.
    if (toUse == null) {
      if (genderFallback == null) {
        toUse = fallback;
      } else {
        toUse = genderFallback;
      }
    }
    boolean hasAnyAudio = toUse != null;

    if (actual != null) actual.setEnabled(hasAnyAudio);

    splitDropdownButton.getTriggerWidget().setEnabled(hasAnyAudio);
    if (hasAnyAudio) {
      if (currentAudioAttr != null) {
        attrToWidget.get(currentAudioAttr).setVisible(false);
      }

      currentAudioAttr = toUse;

      attrToWidget.get(currentAudioAttr).setVisible(true);

      // logger.info("addChoices current audio is " + toUse.getUniqueID() + " : " + toUse.getAudioType() + " : " + toUse.getRealGender());
      if (tellListener) {
     //   logger.info("addChoices tellListener : current audio is " + toUse.getUniqueID() + " : " + toUse.getAudioType() + " : " + toUse.getRealGender());
        listener.audioChangedWithAlignment(toUse.getUniqueID(), toUse.getDurationInMillis(), toUse.getAlignmentOutput());
      }
      rememberAudio(toUse.getAudioRef());
    } else {
      // logger.info("addChoices has no audio for " + exercise.getID() + " context " + includeContext);
    }
  }

  private boolean isSpeedReg() {
    return controller.getStorage().isTrue(IS_REG);
  }

  private boolean isMale() {
    KeyStorage storage = controller.getStorage();
    boolean hasGender = storage.hasValue(IS_MALE);
    return hasGender && storage.isTrue(IS_MALE) || (!hasGender && controller.getUserManager().isMale());
  }

  private Map<AudioAttribute, IconAnchor> attrToWidget = new HashMap<>();

  private void addAudioChoice(SplitDropdownButton playButton, boolean isMale1, boolean isReg, AudioAttribute mr) {
    NavLink widgets = addAudioChoice(isMale1, isReg, mr);
    playButton.add(widgets);

    widgets.addClickHandler(getChoiceHandler(mr, isMale1, isReg));
  }

  /**
   * Shows the currently chosen audio choice.
   *
   * @param isMale1
   * @param isReg
   * @param attr
   * @return
   */
  @NotNull
  private NavLink addAudioChoice(boolean isMale1, boolean isReg, AudioAttribute attr) {
    NavLink widget = new NavLink();
    widget.setIcon(isMale1 ? IconType.MALE : IconType.FEMALE);
    widget.setIconSize(IconSize.TWO_TIMES);

    {
      IconAnchor speed = new IconAnchor();
      speed.setBaseIcon(isReg ? MyCustomIconType.rabbit : MyCustomIconType.turtle);
      speed.addStyleName("leftFiveMargin");
      widget.add(speed);
    }

    {
      IconAnchor check = new IconAnchor();
      check.setIcon(IconType.OK);
      check.setIconSize(IconSize.TWO_TIMES);
      check.setVisible(false);
      check.addStyleName("leftFiveMargin");

      widget.add(check);

      attrToWidget.put(attr, check);
    }

    return widget;
  }

  @NotNull
  private ClickHandler getChoiceHandler(AudioAttribute mr, boolean isMale, boolean isReg) {
    return event -> playAndRemember(mr.getUniqueID(), mr.getAudioRef(), mr.getDurationInMillis(), isMale, isReg, mr);
  }

  private void playAndRemember(int audioID, String audioRef, long durationInMillis, boolean isMale, boolean isReg, AudioAttribute mr) {
    //   logger.info("playAndRemember " + audioID + " " + audioRef + " isMale " + isMale + " isReg " + isReg + " durationInMillis " + durationInMillis);

    if (currentAudioAttr != null) {
      attrToWidget.get(currentAudioAttr).setVisible(false);
    }
    currentAudioAttr = mr;
    attrToWidget.get(currentAudioAttr).setVisible(true);

    doPause();
    listener.audioChanged(audioID, durationInMillis);

    playAudio(audioRef);

    rememberAudioChoice(isMale, isReg);

    Scheduler.get().scheduleDeferred(this::tellOtherPanels);

    controller.logEvent("choicePlay", "Button", "" + exid,
        "choose " + (isMale ? "Male" : "Female") + (isReg ? " Reg" : " Slow"));
  }

  private void rememberAudioChoice(boolean isMale, boolean isReg) {
    KeyStorage storage = controller.getStorage();
    storage.setBoolean(IS_MALE, isMale);
    storage.setBoolean(IS_REG, isReg);
  }

  private void tellOtherPanels() {
    LangTest.EVENT_BUS.fireEvent(new AudioSelectedEvent(exercise == null ? -1 : exercise.getID()));
  }

  private AudioAttribute getAtSpeed(Map<MiniUser, List<AudioAttribute>> malesMap, boolean isReg) {
    Collection<List<AudioAttribute>> values = malesMap.values();

    for (List<AudioAttribute> attrs : values) {
      for (AudioAttribute audioAttribute : attrs) {
        if (isReg && audioAttribute.isRegularSpeed() || (!isReg && audioAttribute.isSlow())) {
          if (audioAttribute.getAudioRef().startsWith(FAST) || audioAttribute.getAudioRef().startsWith(SLOW1)) {
            logger.info("getAtSpeed Skip " + audioAttribute);
          } else {
            return audioAttribute;
          }
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
   * @return
   * @see TwoColumnExercisePanel#getRefAudio
   */
  AudioAttribute getCurrentAudioAttr() {
    return currentAudioAttr;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#getReqAudio
   */
  Set<Integer> getAllAudioIDs() {
    Set<Integer> allIDs = new HashSet<>();
    allPossible.forEach(audioAttribute -> allIDs.add(audioAttribute.getUniqueID()));
    return allIDs;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#getRefAudio
   */
  Set<AudioAttribute> getAllPossible() {
    return allPossible;
  }
}
