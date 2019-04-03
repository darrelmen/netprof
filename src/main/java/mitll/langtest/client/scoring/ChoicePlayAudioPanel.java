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
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
class ChoicePlayAudioPanel<T extends HasID & AudioRefExercise> extends PlayAudioPanel {
  private final Logger logger = Logger.getLogger("ChoicePlayAudioPanel");

  private static final String REG = "Reg";

  private static final String FAST = "Fast";
  private static final String SLOW2 = "Slow";
  private static final String SLOW1 = SLOW2;

  private static final String IS_MALE = "isMale";
  private static final String IS_REG = "isReg";

  private final boolean includeContext;

  /**
   *
   * @see #playAndRemember
   * @see #addChoices(SplitDropdownButton, boolean, Button, boolean)
   * @see AlignmentFetcher#getRefAudio
   */
  private final AudioChangeListener listener;
  private Set<AudioAttribute> allPossible;
  private final int exid;
  private final Map<AudioAttribute, IconAnchor> attrToCheck = new HashMap<>();
  private final T exercise;

  /**
   * @see TwoColumnExercisePanel#getPlayAudioPanel
   * @see TwoColumnExercisePanel#getContext
   */
  ChoicePlayAudioPanel(T exercise,
                       ExerciseController exerciseController,
                       boolean includeContext,
                       AudioChangeListener listener) {
    super(null,
        "",
        null, exerciseController, exercise.getID(), false);
    this.exercise = exercise;
    this.includeContext = includeContext;
    this.listener = listener;
    this.exid = exercise.getID();
    // getElement().setId("ChoicePlayAudioPanel");
    addButtons(null);

    //   logger.info("made choice panel for " + exercise.getID());
// TODO : don't do this - leaves pointers to dead components unless removed...

    LangTest.EVENT_BUS.addHandler(AudioSelectedEvent.TYPE, authenticationEvent -> gotAudioSelected(authenticationEvent.getExid()));
  }

  private void gotAudioSelected(int exid) {
    if (exercise != null && exid != exercise.getID()) {
      //  logger.info("gotAudioSelected choosing different audio for " + exercise.getID());
      addChoices(null, includeContext, null, true);
    }
  }

  private SplitDropdownButton splitDropdownButton;

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

    //logger.info("makePlayButton " + getId() );

    addChoices(playButton, includeContext, actual, false);

    return actual;
  }

  private void configureButton2(SplitDropdownButton playButton) {
    playButton.addClickHandler(event ->
        {
          loadAndPlay();
          controller.logEvent(playButton, "playButton", exid, currentAudioAttr == null ? "unknown file?" : currentAudioAttr.getAudioRef());
        }
    );

    playButton.setIcon(PLAY);
    playButton.setType(ButtonType.INFO);

    // playButton.getElement().setId("PlayAudioPanel_playButton");

    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
    playButton.addStyleName("choiceplay");
  }

  /**
   * Shows check mark next to current audio choice.
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

    List<AudioAttribute> maleAudio = exercise.getMostRecentAudioEasy(true, includeContext);
    List<AudioAttribute> femaleAudio = exercise.getMostRecentAudioEasy(false, includeContext);

/*
    if (exercise.getID() == 8159 ) {
      logger.info("addChoices for exercise " + exercise.getID() +
          //" eng '" + exercise.getEnglish() + "' = '" + exercise.getForeignLanguage() +
          "'" +
          "\n\tmale       " + isMale +
          "\n\tis reg     " + isReg +
          "\n\tmale map   " + maleAudio.size() + " : " + maleAudio +
          "\n\tfemale map " + femaleAudio.size()

      );
      exercise.getAudioAttributes().forEach(audioAttribute -> logger.info("\t" + audioAttribute));
    }
*/

    AudioAttribute toUse = null;
    AudioAttribute fallback = null;
    AudioAttribute genderFallback = null;

    allPossible = new HashSet<>();

    boolean hasPlayButton = playButton != null;
    {
      AudioAttribute mr = simpleGetAtSpeed(maleAudio, true);
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

    /**
     * DLI doesn't want slow speed context, for some reason.
     */
    AudioAttribute ms = includeContext ? null : simpleGetAtSpeed(maleAudio, false);
    if (ms != null) {
      allPossible.add(ms);
      if (hasPlayButton) addAudioChoice(playButton, true, false, ms);
      if (isMale && isSlow) toUse = ms;
      else if (isMale) genderFallback = ms;
      if (fallback == null) fallback = ms;
    }

    AudioAttribute fr = simpleGetAtSpeed(femaleAudio, true);
    if (fr != null) {
      allPossible.add(fr);
      if (hasPlayButton) addAudioChoice(playButton, false, true, fr);
      if (isFemale && isReg) toUse = fr;
      else if (isFemale) genderFallback = fr;
      if (fallback == null) fallback = fr;
    }

    AudioAttribute fs = includeContext ? null : simpleGetAtSpeed(femaleAudio, false);
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
      markCurrentAudio(toUse);

      // logger.info("addChoices current audio is " + toUse.getUniqueID() + " : " + toUse.getAudioType() + " : " + toUse.getRealGender());
      if (tellListener) {
        //   logger.info("addChoices tellListener : current audio is " + toUse.getUniqueID() + " : " + toUse.getAudioType() + " : " + toUse.getRealGender());
        listener.audioChangedWithAlignment(toUse.getUniqueID(), toUse.getDurationInMillis(), toUse.getAlignmentOutput());
      }
      rememberAudio(toUse.getAudioRef());
    } else {
     logger.info("addChoices has no audio for " + exercise.getID() + " context " + includeContext);
    }
  }

  private boolean isSpeedReg() {
    KeyStorage storage = controller.getStorage();
    return !storage.hasValue(IS_REG) || storage.isTrue(IS_REG);
  }

  private boolean isMale() {
    KeyStorage storage = controller.getStorage();
    boolean hasGender = storage.hasValue(IS_MALE);
    return hasGender && storage.isTrue(IS_MALE) || (!hasGender && controller.getUserManager().isMale());
  }

  /**
   * @param playButton
   * @param isMale1
   * @param isReg
   * @param mr
   */
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
      speed.setText(isReg ? REG : SLOW2);
    }

    {
      IconAnchor check = new IconAnchor();
      check.setIcon(IconType.OK);
      check.setIconSize(IconSize.TWO_TIMES);
      check.setVisible(false);
      check.addStyleName("leftFiveMargin");

      widget.add(check);

      attrToCheck.put(attr, check);
    }

    return widget;
  }

  /**
   * @param mr
   * @param isMale
   * @param isReg
   * @return
   */
  @NotNull
  private ClickHandler getChoiceHandler(AudioAttribute mr, boolean isMale, boolean isReg) {
    return event -> playAndRemember(isMale, isReg, mr);
  }

  private void playAndRemember(boolean isMale, boolean isReg, AudioAttribute mr) {
/*    logger.info("playAndRemember " + mr.getUniqueID() +
        "\n\tref    " + mr.getAudioRef() +
        "\n\tisMale " + isMale + " isReg " + isReg + " durationInMillis " + mr.getDurationInMillis());*/

    markCurrentAudio(mr);

    doPause();
    listener.audioChanged(mr.getUniqueID(), mr.getDurationInMillis());

    loadAndPlayOrPlayAudio(mr);

    rememberAudioChoice(isMale, isReg);

    Scheduler.get().scheduleDeferred(this::tellOtherPanels);

    controller.logEvent("choicePlay", "Button", "" + exid,
        "choose " + (isMale ? "Male" : "Female") + (isReg ? " Reg" : " Slow"));
  }

  private void markCurrentAudio(AudioAttribute toUse) {
    // logger.info("markCurrentAudio " +toUse);
    if (currentAudioAttr != null) {
      attrToCheck.get(currentAudioAttr).setVisible(false);
    }

    currentAudioAttr = toUse;
    attrToCheck.get(currentAudioAttr).setVisible(true);
  }

  private void rememberAudioChoice(boolean isMale, boolean isReg) {
    KeyStorage storage = controller.getStorage();
    storage.setBoolean(IS_MALE, isMale);
    storage.setBoolean(IS_REG, isReg);
  }

  private void tellOtherPanels() {
    LangTest.EVENT_BUS.fireEvent(new AudioSelectedEvent(exercise == null ? -1 : exercise.getID()));
  }

  @Nullable
  private AudioAttribute simpleGetAtSpeed(List<AudioAttribute> attrs, boolean isReg) {
    for (AudioAttribute audioAttribute : attrs) {
      if (isReg && audioAttribute.isRegularSpeed() || (!isReg && audioAttribute.isSlow())) {
        String audioRef = audioAttribute.getAudioRef();
        if (audioRef.startsWith(FAST) || audioRef.startsWith(SLOW1)) {
          logger.info("getAtSpeed Skip " + audioAttribute);
        } else {
          return audioAttribute;
        }
      }
    }
    return null;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#getReqAudio
   */
  @Override
  public Collection<Integer> getAllAudioIDs() {
    Set<Integer> allIDs = new HashSet<>();
    allPossible.forEach(audioAttribute -> allIDs.add(audioAttribute.getUniqueID()));
    return allIDs;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#getRefAudio
   */
  @Override
  public Set<AudioAttribute> getAllPossible() {
    return allPossible;
  }
}
