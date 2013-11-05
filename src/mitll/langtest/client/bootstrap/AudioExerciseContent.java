package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Heading;
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
  private static final String LISTEN_TO_THIS_AUDIO = "Listen to this audio";
  private boolean rightAlignContent;
  private String responseType;

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
    String splitTerm = LISTEN_TO_THIS_AUDIO;
    String[] split = content.split(splitTerm);
    String prefix = split[0];

    HTML contentPrefix = getHTML(prefix, true);
    contentPrefix.addStyleName("marginRight");
    if (rightAlignContent) contentPrefix.addStyleName("rightAlign");

    Panel container = new FlowPanel();

    if (includeExerciseID) {
      Heading child = new Heading(5, "Exercise " + e.getID());
      child.addStyleName("leftTenMargin");
      container.add(child);
    }
    container.add(contentPrefix);

    // Todo : this is vulnerable to a variety of issues.
    if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
      Panel container2 = new FlowPanel();
      container2.addStyleName("rightFiveMargin");
      HTML prompt = getHTML("<h3 style='margin-right: 30px'>" + splitTerm + "</h3>", true);
      container2.add(prompt);
      prompt.getElement().setId("prompt");
      SimplePanel simplePanel = new SimplePanel();
      simplePanel.getElement().setId("audioWidgeContainer");

      simplePanel.add(getAudioWidget(e));
      container2.add(simplePanel);

      String suffix = split[1];

      // edit the content to reflect the response type
      if (responseType.equals("Both")) {
        suffix = suffix.replace("answer the question below","answer the question below<br/>both written and spoken");
      } else if (responseType.equals("Text")) {
        suffix = suffix.replace("answer the question", "type your answer to the question");
      }
      HTML contentSuffix = getHTML("<br/>" + // TODO: br is a hack
        "<h3 style='margin-right: 30px'><p word-wrap:break-word;>" + suffix + "</p></h3>", true);
      contentSuffix.addStyleName("marginRight");
      container2.add(contentSuffix);
      container2.addStyleName("rightAlign");
      container.add(container2);
    }

    if (showQuestion) {
      container.add(getHTML("<h2 style='margin-right: 30px'>" + qaPair.getQuestion() + "</h2>", true));
    }
    return container;
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
    return audio;
  }

  /**
   * @see #makeFlashcardForCRT
   * @param content text we want to align
   * @param requireAlignment so you can override it for certain widgets and not make it RTL
   * @return HTML that has it's text-align set consistent with the language (RTL for arabic, etc.)
   */
  private HTML getHTML(String content, boolean requireAlignment) {
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    //  html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }
    html.addStyleName("rightTenMargin");

    html.addStyleName("wrapword");
    return html;
  }
}
