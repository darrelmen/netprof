package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MiniUser;

import java.util.*;

/**
 * Created by go22670 on 10/16/15.
 */
public class FastAndSlowASRScoringAudioPanel extends ASRScoringAudioPanel {
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
   * @param service
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getAudioPanel
   */
  public FastAndSlowASRScoringAudioPanel(CommonExercise exercise,
                                         String path, LangTestDatabaseAsync service, ExerciseController controller1,
                                         ScoreListener scoreListener,
                                         String instance
                                         ) {
    super(path,
        exercise.getForeignLanguage(),
        service,
        controller1,
        controller1.getProps().showSpectrogram(), scoreListener, 23, REFERENCE, exercise.getID(), exercise, instance);
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
      //logger.info("getAfterPlayWidget : for ex " + exercise.getID() + " found " + audioAttributes);

      // first choice here is for default audio (where we don't know the gender)
      final Collection<AudioAttribute> initialAudioChoices = maleEmpty ?
          femaleEmpty ? audioAttributes : femalesMap.get(femaleUsers.get(0)) : malesMap.get(maleUsers.get(0));
      //System.out.println("getAfterPlayWidget.initialAudioChoices  " + initialAudioChoices);

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

          if (choice.equals(M)) {
            audioChoices = malesMap.values().iterator().next();
          } else if (choice.equals(F)) {
            audioChoices = femalesMap.values().iterator().next();
          } else {
            audioChoices = defaultAudioSet;
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
    controller.register(onButton, exerciseID);
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
/*      System.out.println("getAfterPlayWidget : for exercise " +exercise.getID() +
        " path "+ audioPath + " attributes were " + audioAttributes);*/

    RadioButton regular = null;
    AudioAttribute regAttr = null;
    RadioButton slow = null;
    AudioAttribute slowAttr = null;
    for (final AudioAttribute audioAttribute : audioAttributes) {
      String display = audioAttribute.getDisplay();
      // System.out.println("attri " + audioAttribute + " display " +display);
      final RadioButton radio = new RadioButton(GROUP + "_" + exerciseID + "_" + instance, display);
      radio.getElement().setId("Radio_" + display);
      if (audioAttribute.isRegularSpeed()) {
        regular = radio;
        regAttr = audioAttribute;
      } else if (audioAttribute.isSlow()) {  // careful not to get context sentence audio ...
        slow = radio;
        slowAttr = audioAttribute;
      }
    }

    if (regular != null) {
      //System.out.println("addRegularAndSlow regular " + regular);

      addAudioRadioButton(vp, regular);
      final AudioAttribute innerRegAttr = regAttr;
      final RadioButton innerRegular = regular;
      regular.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAudio(innerRegAttr);
          controller.logEvent(innerRegular, RADIO_BUTTON, exerciseID, SELECTED_AUDIO + innerRegAttr.getAudioRef());
        }
      });
      regular.setValue(true);
    }
    if (slow != null) {
      //System.out.println("addRegularAndSlow slow " + slow);
      addAudioRadioButton(vp, slow);
      final AudioAttribute innerSlowAttr = slowAttr;
      final RadioButton innerSlow = slow;
      slow.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAudio(innerSlowAttr);
          controller.logEvent(innerSlow, RADIO_BUTTON, exerciseID, SELECTED_AUDIO + innerSlowAttr.getAudioRef());
        }
      });
      if (regular == null)
        slow.setValue(true);
    }
    AudioAttribute firstAttr = (regular != null) ? regAttr : slowAttr;
    if ((regular == null) && (slow == null))
      System.err.println("no radio choice got selected??? ");

    else {
      //System.out.println("GoodwaveExercisePanel.addRegularAndSlow showing " +firstAttr);
      final AudioAttribute ffirstAttr = firstAttr;
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          showAudio(ffirstAttr);
        }
      });
    }
    if (firstAttr == null)
      System.err.println("huh? no attribute ");
  }

  /**
   * @param vp
   * @see AudioPanel AudioPanel#getAfterPlayWidget
   */
  protected void addNoRefAudioWidget(Panel vp) {
    vp.add(new Label(NO_REFERENCE_AUDIO));
  }

  @Override
  protected boolean hasAudio(CommonExercise exercise) {
    return !exercise.getAudioAttributes().isEmpty();
  }

  protected void addAudioRadioButton(Panel vp, RadioButton fast) {
    vp.add(fast);
  }

  private void showAudio(AudioAttribute audioAttribute) {
    doPause();    // if the audio is playing, stop it
    getImagesForPath(audioAttribute.getAudioRef());
  }
}