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
  protected final Logger logger = Logger.getLogger("ChoicePlayAudioPanel");
  private static final String IS_MALE = "isMale";
  private static final String IS_REG = "isReg";
  public static final String MALE = "male";
  public static final String SLOW = "slow";

  private ExerciseController controller;
  private CommonExercise exercise;
  private boolean includeContext = false;
  private int currentAudioID = -1;
  AudioChangeListener listener;
  private Set<Integer> allIDs = new HashSet<>();

  /**
   * @see TwoColumnExercisePanel#getPlayAudioPanel
   * @see TwoColumnExercisePanel#getContext
   */
  public ChoicePlayAudioPanel(
      SoundManagerAPI soundManager,
      CommonExercise exercise,
      ExerciseController exerciseController,
      boolean includeContext, AudioChangeListener listener) {
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
    this.includeContext = includeContext;
    this.exercise = exercise;
    this.controller = exerciseController;
    this.listener = listener;

    getElement().setId("ChoicePlayAudioPanel");

    LangTest.EVENT_BUS.addHandler(AudioSelectedEvent.TYPE, authenticationEvent -> {
      gotAudioSelected(authenticationEvent.getExid());
    });
  }

  private void gotAudioSelected(int exid) {
    if (exercise != null && exid != exercise.getID()) {
      // logger.info("gotAudioSelected choosing different audio for " + exercise.getID());
      addChoices(null, includeContext);
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

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        addChoices(playButton, includeContext);
      }
    });

    Widget widget = playButton.getWidget(0);
    Button actual = (Button) widget;
    actual.setEnabled(false);
    return actual;
  }

  private void configureButton2(SplitDropdownButton playButton) {
    playButton.addClickHandler(event -> {
      playAudio();
    });

    playButton.setIcon(PLAY);
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
    playButton.addStyleName("choiceplay");
  }


  private void addChoices(SplitDropdownButton playButton, boolean includeContext) {
    Collection<Long> preferredVoices = Collections.emptyList();
    boolean isMale = isMale();
    boolean isFemale = !isMale();
    boolean isReg = controller.getStorage().isTrue(IS_REG);
    boolean isSlow = !isReg;

    Map<MiniUser, List<AudioAttribute>> malesMap =
        exercise.getMostRecentAudio(true, preferredVoices, includeContext);
    Map<MiniUser, List<AudioAttribute>> femalesMap =
        exercise.getMostRecentAudio(false, preferredVoices, includeContext);

/*

    logger.info("addChoices For " + exercise.getID() + " " + exercise.getEnglish() + " "+
        " male " + isMale + " is reg " + isReg + " male map " + malesMap.size() + " female map " + femalesMap.size());

*/

    AudioAttribute toUse = null;
    AudioAttribute fallback = null;

    {
      AudioAttribute mr = getAtSpeed(malesMap, true);
      if (mr != null) {
        allIDs.add(mr.getUniqueID());
        if (playButton != null) addAudioChoice(playButton, true, true, mr);
        if (isMale && isReg) toUse = mr;
        else fallback = mr;
      }
    }
    {
      AudioAttribute ms = getAtSpeed(malesMap, false);
      if (ms != null) {
        allIDs.add(ms.getUniqueID());
        if (playButton != null) addAudioChoice(playButton, true, false, ms);
        if (isMale && isSlow) toUse = ms;
        if (fallback == null) fallback = ms;
      }
    }
    {
      AudioAttribute fr = getAtSpeed(femalesMap, true);
      if (fr != null) {
        allIDs.add(fr.getUniqueID());
        if (playButton != null) addAudioChoice(playButton, false, true, fr);
        if (isFemale && isReg) toUse = fr;
        if (fallback == null) fallback = fr;
      }
    }

    {
      AudioAttribute fs = getAtSpeed(femalesMap, false);
      if (fs != null) {
        allIDs.add(fs.getUniqueID());

        if (playButton != null) addAudioChoice(playButton, false, false, fs);
        if (isFemale && isSlow) toUse = fs;
        if (fallback == null) fallback = fs;
      }
    }

    if (toUse == null) toUse = fallback;
    boolean val = toUse != null;

    setEnabled(val);
    splitDropdownButton.getTriggerWidget().setEnabled(val);
    if (val) {
      currentAudioID = toUse.getUniqueID();
      listener.audioChanged(currentAudioID);
      logger.info("addChoices For exercise " + exercise.getID() + " current audio is " + currentAudioID);
      rememberAudio(toUse.getAudioRef());
    }
  }

  private boolean isMale() {
    KeyStorage storage = controller.getStorage();
    boolean hasGender = storage.hasValue(IS_MALE);
    return hasGender && storage.isTrue(IS_MALE) || (!hasGender && controller.getUserManager().isMale());
  }

  private void addAudioChoice(SplitDropdownButton playButton, boolean isMale1, boolean isReg, AudioAttribute mr) {
    NavLink widget = addAudioChoice(playButton, isMale1, isReg);
    widget.addClickHandler(getChoiceHandler(mr, isMale1, isReg));
  }

  @NotNull
  private NavLink addAudioChoice(SplitDropdownButton playButton, boolean isMale1, boolean isReg) {
    NavLink widget = new NavLink();//male + " " + regular + " Speed");
    playButton.add(widget);
    widget.setIcon(isMale1 ? IconType.MALE : IconType.FEMALE);
    widget.setIconSize(IconSize.TWO_TIMES);
    IconAnchor speed = new IconAnchor();
    speed.setBaseIcon(isReg ? MyCustomIconType.rabbit : MyCustomIconType.turtle);
    widget.add(speed);
    speed.addStyleName("leftFiveMargin");
    return widget;
  }

  @NotNull
  private ClickHandler getChoiceHandler(AudioAttribute mr, boolean isMale, boolean isReg) {
    return event -> playAndRemember(mr.getAudioRef(), isMale, isReg);
  }

  private void playAndRemember(String audioRef, boolean isMale, boolean isReg) {
    logger.info("playAndRemember " + audioRef);

    doPause();
    playAudio(audioRef);
    KeyStorage storage = controller.getStorage();
    storage.setBoolean(IS_MALE, isMale);
    storage.setBoolean(IS_REG, isReg);

    LangTest.EVENT_BUS.fireEvent(new AudioSelectedEvent(exercise == null ? -1 : exercise.getID()));
  }

  private AudioAttribute getAtSpeed(Map<MiniUser, List<AudioAttribute>> malesMap, boolean isReg) {
    Collection<List<AudioAttribute>> values = malesMap.values();

    for (List<AudioAttribute> attrs : values) {
      for (AudioAttribute audioAttribute : attrs) {
        if (isReg && audioAttribute.isRegularSpeed() || (!isReg && audioAttribute.isSlow())) {
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

  /**
   * @return
   * @see TwoColumnExercisePanel#getRefAudio
   */
  public int getCurrentAudioID() {
    return currentAudioID;
  }

  public Set<Integer> getAllAudioIDs() {
    return allIDs;
  }
}
