package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioRefExercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ContextAudioChoices {
  private final ExerciseController controller;
  private final AudioRefExercise e;
  private final int id;
  private Logger logger = Logger.getLogger("ContextAudioChoices");

  //  private static final String HIGHLIGHT_START = "<span style='background-color:#5bb75b;color:black'>"; //#5bb75b
//  private static final String HIGHLIGHT_END = "</span>";

  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String DEFAULT = "Default";

  private static final String NO_REFERENCE_AUDIO = "No reference audio";
  private static final String M = "M";
  private static final String F = "F";
  public static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";
  public static final String SPACE_REGEX = " ";
  private static final String REF_AUDIO = "refAudio";

  private AudioAttribute defaultAudio, maleAudio, femaleAudio;
  private PlayAudioPanel contextPlay;

  public ContextAudioChoices(ExerciseController controller,
                             AudioRefExercise e, int id) {
    this.controller = controller;
    this.e = e;
    this.id = id;
  }

  /**
   * For context audio!
   * <p>
   * From all the reference audio recorded, show male and female, and default (unknown gender) audio.
   * TODO : why show default audio if we have both male and female ???
   * Choose latest recording, unless we have a preferred user id match.
   *
   * @param e
   * @param hp
   * @see #getContext
   */
  public void addGenderChoices(Panel hp) {
    // first, choose male and female voices
    long maleTime = 0, femaleTime = 0;

    logger.info("Add gender choices to " + e + " with " + e.getAudioAttributes().size());

    Set<Long> preferredUsers = controller.getProps().getPreferredVoices();
    for (AudioAttribute audioAttribute : e.getAudioAttributes()) {
      if (audioAttribute.isContextAudio()) {
        logger.info("addGenderChoices : adding context audio " + audioAttribute);
        long user = audioAttribute.getUser().getID();
        if (user == -1) {
          defaultAudio = audioAttribute;
        } else if (audioAttribute.getUser().isMale()) {
          if (audioAttribute.getTimestamp() > maleTime) {
            if (maleAudio == null || !preferredUsers.contains((long) maleAudio.getUser().getID())) {
              maleAudio = audioAttribute;
              maleTime = audioAttribute.getTimestamp();
            }
          }
        } else if (audioAttribute.getTimestamp() > femaleTime) {
          if (femaleAudio == null || !preferredUsers.contains((long) femaleAudio.getUser().getID())) {
            femaleAudio = audioAttribute;
            femaleTime = audioAttribute.getTimestamp();
          }
        }
      } else {
        logger.info("skipping non-context " + audioAttribute);
      }
    }

    addPlayAndVoiceChoices(hp);
  }

  /**
   * choose male first if multiple choices
   *
   * @param hp
   */
  private void addPlayAndVoiceChoices(Panel hp) {
    AudioAttribute toUse = maleAudio != null ? maleAudio : femaleAudio != null ? femaleAudio : defaultAudio;
    String path = toUse == null ? null : toUse.getAudioRef();
    logger.info("addPlayAndVoiceChoices choosing to play " + toUse);
    logger.info("addPlayAndVoiceChoices path             " + path);
    if (path != null) {
      contextPlay = new PlayAudioPanel(controller, path, false)
          .setPlayLabel("")
          .setPauseLabel("")
          .setMinWidth(12);
      contextPlay.getPlayButton().getElement().getStyle().setMarginTop(-6, Style.Unit.PX);

      hp.add(contextPlay);

      List<String> choices = new ArrayList<>();
      if (maleAudio != null) choices.add(M);
      if (femaleAudio != null) choices.add(F);
      if (defaultAudio != null && (maleAudio == null || femaleAudio == null)) {
        //logger.info("Adding default choice since found " + defaultAudio);
        choices.add(DEFAULT); //better not happen
      }

      hp.add(getShowGroup(choices));
    }
  }

  /**
   * @param choices
   * @return
   * @see #addPlayAndVoiceChoices
   */
  private DivWidget getShowGroup(Collection<String> choices) {
    ButtonToolbar buttonToolbar = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonToolbar.add(buttonGroup);

    boolean first = true;
    for (final String choice : choices) {
      Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          contextPlay.playAudio(getAudioRef(choice));
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = false;
    }

    Style style = buttonToolbar.getElement().getStyle();
    style.setMarginTop(-6, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    return buttonToolbar;
  }

  private String getAudioRef(String choice) {
    String audioRef;
    switch (choice) {
      case M:
        audioRef = maleAudio.getAudioRef();
        break;
      case F:
        audioRef = femaleAudio.getAudioRef();
        break;
      default:
        audioRef = defaultAudio.getAudioRef();
        break;
    }
    return audioRef;
  }

  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title.equals(M) ? "" : title.equals(F) ? "" : title);
    onButton.getElement().setId("Choice_" + title);
    controller.register(onButton, id);
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    onButton.getElement().getStyle().setZIndex(0);
    return onButton;
  }
}
