package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.media.client.Audio;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExerciseContent {
  //private static final String LISTEN_TO_THIS_AUDIO = "Listen to this audio";
  private static final String QUESTION_HEADING = "h4";
  //private static final String AUDIO_PROMPT_HEADING = "h4";
  private boolean rightAlignContent;
  private String responseType;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanel#getQuestionContent
   * @param e
   * @param controller
   * @param includeExerciseID
   * @param showQuestion
   * @return
   */
  public Widget getQuestionContent(Exercise e, ExerciseController controller, boolean includeExerciseID, boolean showQuestion) {
    rightAlignContent = controller.isRightAlignContent();
    responseType = controller.getProps().getResponseType();
    String stimulus = e.getEnglishSentence();
    String content = e.getContent();

    if (content == null) {
      content = stimulus;
    }
    return makeFlashcardForCRT(e, content,includeExerciseID, showQuestion);
  }

  /**
   * Do hacky stuff to take the content and splice in an html 5 audio widget to play back audio.
   *
   *
   * @param e
   * @param content
   * @param showQuestion
   * @return
   */
  private Panel makeFlashcardForCRT(Exercise e, String content, boolean includeExerciseID, boolean showQuestion) {
    Exercise.QAPair qaPair = e.getForeignLanguageQuestions().get(0);

    Panel container = new FlowPanel();

    //addThreeRow(e, content, includeExerciseID, container);
    addAudioRow(e, content, includeExerciseID, container);

    if (showQuestion) {
      container.add(getMaybeRTLContent("<" +
        QUESTION_HEADING +
        " style='margin-right: 30px'>" + qaPair.getQuestion() + "</" +
        QUESTION_HEADING +
        ">", true));
    }
    return container;
  }

  private void addAudioRow(Exercise e, String content, boolean includeExerciseID, Panel container) {
    if (includeExerciseID) {
      Heading child = new Heading(5, "Exercise " + e.getID());
      child.addStyleName("leftTenMargin");
      container.add(child);
    }
    content = content.replaceAll("h2","h4");
    content = changeAudioPrompt(content,false);
    HTML contentFromPrefix = getContentFromPrefix(content);
/*
    ScrollPanel scroller = new ScrollPanel(contentFromPrefix);
    scroller.getElement().setId("scroller");
    container.add(scroller);*/
    container.add(contentFromPrefix);

    if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
      container.add(getAudioDiv(e));
    }
  }

/*  private void addThreeRow(Exercise e, String content, boolean includeExerciseID, Panel container) {
    if (includeExerciseID) {
      Heading child = new Heading(5, "Exercise " + e.getID());
      child.addStyleName("leftTenMargin");
      container.add(child);
    }
    String[] split = content.split(LISTEN_TO_THIS_AUDIO);
    HTML contentPrefix = getContentFromPrefix(split[0]);
    container.add(contentPrefix);

    // Todo : this is vulnerable to a variety of issues.
    if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
      makeThreeRowAudio(e, split[1], container);
    }
  }*/

  private HTML getContentFromPrefix(String prefix) {
    HTML contentPrefix = getMaybeRTLContent(prefix, true);
    contentPrefix.addStyleName("marginRight");
    if (rightAlignContent) contentPrefix.addStyleName("rightAlign");
    return contentPrefix;
  }

/*  private void makeThreeRowAudio(Exercise e, String suffix, Panel container) {
    Panel container2 = new FlowPanel();
    container2.addStyleName("rightFiveMargin");
    HTML prompt = getMaybeRTLContent("<" +
      AUDIO_PROMPT_HEADING +
      " style='margin-right: 30px'>" + LISTEN_TO_THIS_AUDIO + "</" +
      AUDIO_PROMPT_HEADING +
      ">", true);
    container2.add(prompt);
    prompt.getElement().setId("prompt");

    container2.add(getAudioDiv(e));

    // edit the content to reflect the response type
    suffix = changeAudioPrompt(suffix,true);

    HTML contentSuffix = getMaybeRTLContent("<br/>" + // TODO: br is a hack
      "<" +
      AUDIO_PROMPT_HEADING +
      " style='margin-right: 30px'><p word-wrap:break-word;>" + suffix + "</p></" +
      AUDIO_PROMPT_HEADING +
      ">", true);
    contentSuffix.addStyleName("marginRight");
    container2.add(contentSuffix);
    container2.addStyleName("rightAlign");
    container.add(container2);
  }*/

  private String changeAudioPrompt(String suffix, boolean addBreak) {
    if (responseType.equals("Both")) {
      suffix = suffix.replace("answer the question below","answer the question below" +
        (addBreak?  "<br/>": " ") +
        "both written and spoken");
    } else if (responseType.equals("Text")) {
      suffix = suffix.replace("answer the question", "type your answer to the question");
    }
    return suffix;
  }

  /**
   * @see #addAudioRow(mitll.langtest.shared.Exercise, String, boolean, com.google.gwt.user.client.ui.Panel)
   * @param e
   * @return
   */
  private SimplePanel getAudioDiv(Exercise e) {
    SimplePanel simplePanel = new SimplePanel();
    simplePanel.getElement().setId("audioWidgetContainer");

    simplePanel.add(getAudioWidget(e));
    return simplePanel;
  }


  /**
   * Make an html 5 audio widget using the exercise's ref audio field.
   *
   * @param e
   * @return
   */
  private Widget getAudioWidget(Exercise e) {
    String refAudio = e.getRefAudio();
    String type = refAudio.substring(refAudio.length() - 3);

    final Audio audio = getAudioNoFocus(refAudio, type);
    audio.addStyleName("floatRight");
    audio.addStyleName("rightFiveMargin");

    System.out.println("getting audio widget for " + e.getID());

    return audio;
  }

  /**
   * Make both mp3 and ogg types
   * @param refAudio
   * @param type
   * @return
   */
  private Audio getAudioNoFocus(String refAudio, String type) {
    final Audio audio = Audio.createIfSupported();
    audio.setControls(true);
    audio.addSource(refAudio, "audio/" + type);
    audio.addSource(refAudio.replace(".mp3", ".ogg"), "audio/ogg");
    audio.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        audio.setFocus(false);
      }
    });
    audio.setPreload(MediaElement.PRELOAD_AUTO);
    return audio;
  }

  /**
   * @see #makeFlashcardForCRT
   * @param content text we want to align
   * @param requireAlignment so you can override it for certain widgets and not make it RTL
   * @return HTML that has it's text-align set consistent with the language (RTL for arabic, etc.)
   */
  private HTML getMaybeRTLContent(String content, boolean requireAlignment) {
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }
    html.addStyleName("rightTenMargin");

    html.addStyleName("wrapword");
    html.getElement().setId("textContent");
    return html;
  }
}
