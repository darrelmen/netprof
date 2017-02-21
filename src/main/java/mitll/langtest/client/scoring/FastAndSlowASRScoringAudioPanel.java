/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioAttributeExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/16/15.
 */
public class FastAndSlowASRScoringAudioPanel<T extends CommonExercise & AudioAttributeExercise> extends ASRScoringAudioPanel<T> {
  private Logger logger = null;

  public static final int RIGHT_MARGIN = 23;

  private static final String DEFAULT = "Default";

  private static final String GROUP = "group";
  private static final String NO_REFERENCE_AUDIO = "No reference audio.";
  private static final String RADIO_BUTTON = "RadioButton";
  private static final String SELECTED_AUDIO = "Selected audio ";
  private static final String REFERENCE = "";

  private static final String M = "M";
  private static final String F = "F";

  /**
   * @param exercise
   * @param path
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getAudioPanel
   */
  protected FastAndSlowASRScoringAudioPanel(T exercise,
                                            String path,
                                            ExerciseController controller1,
                                            String instance
  ) {
    super(path,
        exercise.getForeignLanguage(),
        exercise.getTransliteration(),

        controller1,
        controller1.getProps().showSpectrogram(), RIGHT_MARGIN, REFERENCE, exercise, instance);
//        Result.AUDIO_TYPE_PRACTICE);
  }

  /**
   * Add choices to control which audio cut is chosen/gets played.
   * <p>
   * Be careful not to assume all audio associated with an item is either fast or slow speed audio.
   * It could be context sentence audio.
   *
   * @return
   * @see AudioPanel#addWidgets
   */
  @Override
  protected Widget getAfterPlayWidget() {
   // logger = Logger.getLogger("FastAndSlowASRScoringAudioPanel");
    final Panel rightSide = new VerticalPanel();

    rightSide.getElement().setId("beforePlayWidget_verticalPanel");
    rightSide.addStyleName("leftFiveMargin");
    boolean foundSpeed = exercise.getRegularSpeed() != null || exercise.getSlowSpeed() != null;
    if (!foundSpeed) {
      addNoRefAudioWidget(rightSide);
      return rightSide;
    } else {
      // add gender choices
      Set<Long> preferredVoices = controller.getProps().getPreferredVoices();
      Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
      Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);
      Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();

      List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
      boolean maleEmpty = maleUsers.isEmpty();
      List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
      boolean femaleEmpty = femaleUsers.isEmpty();

      Widget container = null;
      if (!maleEmpty || !femaleEmpty || !defaultUserAudio.isEmpty()) {
        container = getGenderChoices(rightSide, malesMap, femalesMap, defaultUserAudio, instance);
      }
      Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();
      // first choice here is for default audio (where we don't know the gender)
      final Collection<AudioAttribute> initialAudioChoices = maleEmpty ?
          femaleEmpty ? audioAttributes : femalesMap.get(femaleUsers.get(0)) : malesMap.get(maleUsers.get(0));

      addRegularAndSlow(rightSide, initialAudioChoices, instance);

      Panel leftAndRight = new HorizontalPanel();

      if (container != null) {
        leftAndRight.add(container);
      }
      leftAndRight.add(rightSide);
      return leftAndRight;
    }
  }

  /**
   * @param rightSide
   * @param malesMap
   * @param femalesMap
   * @param defaultAudioSet
   * @param instance
   * @return
   * @see AudioPanel#getAfterPlayWidget
   */
  private Widget getGenderChoices(final Panel rightSide,
                                  final Map<MiniUser, List<AudioAttribute>> malesMap,
                                  final Map<MiniUser, List<AudioAttribute>> femalesMap,
                                  final Collection<AudioAttribute> defaultAudioSet,
                                  final String instance) {
    ButtonToolbar w = new ButtonToolbar();
    w.getElement().setId("GenderChoices");

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    List<String> choices = new ArrayList<String>();

    if (!malesMap.isEmpty()) {
      choices.add(M);
    }
    if (!femalesMap.isEmpty()) {
      choices.add(F);
    }
    if (choices.size() != 2 && !defaultAudioSet.isEmpty()) {
      choices.add(DEFAULT);
    }

    boolean first = true;
    for (final String choice : choices) {
      Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          rightSide.clear();
          Collection<AudioAttribute> audioChoices;

          switch (choice) {
            case M:
              audioChoices = malesMap.values().iterator().next();
              break;
            case F:
              audioChoices = femalesMap.values().iterator().next();
              break;
            default:
              audioChoices = defaultAudioSet;
              break;
          }
          addRegularAndSlow(rightSide, audioChoices, instance);
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = false;
    }

    Style style = w.getElement().getStyle();
    int topToUse = 10;
    style.setMarginTop(topToUse, Style.Unit.PX);
    style.setMarginBottom(topToUse, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    return w;
  }

  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title.equals(M) ? "" : title.equals(F) ? "" : title);

    onButton.getElement().setId("Choice_" + title);
    controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    return onButton;
  }

  /**
   * @param vp
   * @param audioAttributes
   * @param instance
   * @see AudioPanel AudioPanel#getAfterPlayWidget
   * @see #getGenderChoices(Panel, Map, Map, Collection, String)
   */
  private void addRegularAndSlow(Panel vp, Collection<AudioAttribute> audioAttributes, String instance) {
/*      System.out.println("getAfterPlayWidget : for exercise " +exercise.getOldID() +
        " path "+ audioPath + " attributes were " + audioAttributes);*/
    RadioButton regular = null;
    AudioAttribute regAttr = null;

    RadioButton slow = null;
    AudioAttribute slowAttr = null;

    for (final AudioAttribute audioAttribute : audioAttributes) {
      if (!audioAttribute.isValid()) continue;
      String display = audioAttribute.getDisplay();

      // System.out.println("attri " + audioAttribute + " display " +display);
      final RadioButton radio = new RadioButton(GROUP + "_" + exercise.getID() + "_" + instance, display);
      radio.getElement().setId("Radio_" + display);
      if (audioAttribute.isRegularSpeed()) {
        regular = radio;
        regAttr = audioAttribute;
      } else if (audioAttribute.isSlow()) {  // careful not to get context sentence audio ...
        slow = radio;
        slowAttr = audioAttribute;
      }
    }

    boolean choseRegularSpeed = isRegularSpeed();
    if (regular != null) {
      addAudioRadioButton(vp, regular, regAttr);
      final AudioAttribute innerRegAttr = regAttr;
      final RadioButton innerRegular = regular;
      regular.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAudio(innerRegAttr);
          storeIsRegular(true);
          controller.logEvent(innerRegular, RADIO_BUTTON, exercise.getID(), SELECTED_AUDIO + innerRegAttr.getAudioRef());
        }
      });
      regular.setValue(choseRegularSpeed);
    }

    if (slow != null) {
      addAudioRadioButton(vp, slow, slowAttr);
      final AudioAttribute innerSlowAttr = slowAttr;
      final RadioButton innerSlow = slow;
      slow.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAudio(innerSlowAttr);
          storeIsRegular(false);
          controller.logEvent(innerSlow, RADIO_BUTTON, exercise.getID(), SELECTED_AUDIO + innerSlowAttr.getAudioRef());
        }
      });
      if (regular == null || !choseRegularSpeed)
        slow.setValue(true);
    }
    AudioAttribute firstAttr = ((regular != null && choseRegularSpeed) || slowAttr == null) ? regAttr : slowAttr;

    if ((regular == null) && (slow == null)) {
      // logger.warning("no radio choice got selected??? ");
    } else {
      //System.out.println("GoodwaveExercisePanel.addRegularAndSlow showing " +firstAttr);
      final AudioAttribute ffirstAttr = firstAttr;
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          showAudio(ffirstAttr);
        }
      });
    }
//    if (firstAttr == null)
//      logger.warning("huh? no attribute ");
  }

  private boolean isRegularSpeed() {
    return isRegularSpeed(getStorageKey());
  }

  private boolean isRegularSpeed(String selectedUserKey) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String item = localStorageIfSupported.getItem(selectedUserKey);
      //    logger.info("isRegularSpeed value for " + selectedUserKey + "='" + item+ "'");
      if (item != null) {
        return item.toLowerCase().equals("true");
      } else {
        storeIsRegular(true);
        return true;
      }
    }
    // else {
    return false;
    // }
  }

  private String getStorageKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + "audioSpeed";
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":" + instance + ":";
  }

  private void storeIsRegular(boolean shouldKeepAudio) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getStorageKey(), "" + shouldKeepAudio);
    }
  }

  private String getStorageKey() {
    return getStorageKey(controller, controller.getLanguage());
  }

  /**
   * @param vp
   * @see AudioPanel AudioPanel#getAfterPlayWidget
   */
  protected void addNoRefAudioWidget(Panel vp) {
    vp.add(new Label(NO_REFERENCE_AUDIO));
  }

  @Override
  protected boolean hasAudio(T exercise) {
    return !exercise.getAudioAttributes().isEmpty();
  }

  protected void addAudioRadioButton(Panel vp, RadioButton fast, AudioAttribute audioAttribute) {
    vp.add(fast);
  }

  private void showAudio(AudioAttribute audioAttribute) {
    doPause();    // if the audio is playing, stop it
    getImagesForPath(audioAttribute.getAudioRef());
  }
}