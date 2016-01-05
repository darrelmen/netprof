package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioExerciseContent {
 // private Logger logger = Logger.getLogger("AudioExerciseContent");

  private static final String QUESTION = "question";
  private static final String QUESTION_HEADING = "h4";
  public static final String ITEM = "Passage";

  private boolean rightAlignContent;
  private String responseType;
  private String language;

  /**
   * @param e
   * @param controller
   * @param includeExerciseID
   * @param showQuestion
   * @param content
   * @param index
   * @param totalInQuiz
   * @return
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getQuestionContent
   */
  public Widget getQuestionContent(AmasExerciseImpl e, ExerciseController controller, boolean includeExerciseID,
                                   boolean showQuestion, String content, int index, int totalInQuiz) {
    rightAlignContent = controller.isRightAlignContent();
    language = controller.getLanguage().toLowerCase();
    responseType = controller.getProps().getResponseType();
    return makeFlashcardForCRT(e, content, includeExerciseID, showQuestion, index, totalInQuiz);
  }

  /**
   * Do hacky stuff to take the content and splice in an html 5 audio widget to play back audio.
   *
   * @param e
   * @param content
   * @param showQuestion
   * @param index
   * @param totalInQuiz
   * @return
   */
  private Panel makeFlashcardForCRT(AmasExerciseImpl e, String content, boolean includeExerciseID, boolean showQuestion, int index, int totalInQuiz) {
    Panel container = new FlowPanel();
    container.getElement().setId("makeFlashcardForCRT_container");
    addAudioRow(e, content, includeExerciseID, container, index, totalInQuiz);

    QAPair qaPair = getQaPair(e);
    if (showQuestion && qaPair != null) {
      container.add(getMaybeRTLContent("<" +
          QUESTION_HEADING +
          " style='margin-right: 30px'>" + qaPair.getQuestion() + "</" +
          QUESTION_HEADING +
          ">", true));
    }
    return container;
  }

  private QAPair getQaPair(AmasExerciseImpl e) {
    List<QAPair> foreignLanguageQuestions = e.getForeignLanguageQuestions();
    QAPair qaPair = foreignLanguageQuestions.isEmpty() ? null : foreignLanguageQuestions.get(0);
    if (foreignLanguageQuestions.isEmpty()) {
      System.err.println("huh? no fl questions for " + e);
    }
    return qaPair;
  }

  /**
   * @param e
   * @param content
   * @param includeExerciseID
   * @param container
   * @param index
   * @param totalInQuiz
   * @see #getContentFromPrefix(String)
   * @seex #getAudioDiv
   * @see #makeFlashcardForCRT
   */
  private void addAudioRow(AmasExerciseImpl e, String content, boolean includeExerciseID, Panel container, int index, int totalInQuiz) {
    Panel horiz = new FlowPanel();
    horiz.getElement().setId("item_and_content");
    container.add(horiz);
    if (includeExerciseID) {
      DivWidget itemHeaderContainer = new DivWidget();
     // itemHeaderContainer.setWidth("100%");
      itemHeaderContainer.setHeight("20px");
      itemHeaderContainer.getElement().setId("itemHeaderContainer");
      Heading child = getItemHeader(index, totalInQuiz);
      itemHeaderContainer.add(child);
      horiz.add(itemHeaderContainer);
    }

    content = content.replaceAll("h2", "h4");
    boolean isPlural = e.getQuestions().size() > 1;
    content = changeAudioPrompt(content, isPlural);

    HTML contentFromPrefix = getContentFromPrefix(content);

    if (rightAlignContent) {
      contentFromPrefix.addStyleName("floatRight");
    }

    horiz.add(contentFromPrefix);
  }

  private Heading getItemHeader(int index, int totalInQuiz) {
    String text = ITEM + " #" + (index + 1) + " of " + totalInQuiz;
    Heading child = new Heading(5, text);
    child.getElement().setId("audio_exercise_item_header");
    child.addStyleName("leftTenMargin");
    child.addStyleName("floatLeft");
    child.addStyleName("rightFiveMargin");
    return child;
  }

  /**
   * @param prefix
   * @return
   * @see #addAudioRow
   */
  private HTML getContentFromPrefix(String prefix) {
    HTML contentPrefix = getMaybeRTLContent(prefix, true);
    contentPrefix.addStyleName("marginRight");
    contentPrefix.addStyleName("wrapword");
    if (rightAlignContent) contentPrefix.addStyleName("rightAlign");
    return contentPrefix;
  }

  private String changeAudioPrompt(String suffix, boolean isPlural) {
    if (responseType.equals(PropertyHandler.TEXT)) {
      String question = QUESTION;
      suffix = suffix.replace("answer the " + question, "type your answer to the " + question);
    }
    if (isPlural) {
      suffix = suffix.replace(QUESTION, "questions");
    }
    return suffix;
  }

  /**
   * @param content          text we want to align
   * @param requireAlignment so you can override it for certain widgets and not make it RTL
   * @return HTML that has it's text-align set consistent with the language (RTL for arabic, etc.)
   * @see #makeFlashcardForCRT
   * @see #getContentFromPrefix
   */
  private HTML getMaybeRTLContent(String content, boolean requireAlignment) {
    HasDirection.Direction direction =
        requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);
    content = content
        .replaceAll("<p> &nbsp; </p>", "")
        .replaceAll("<h4>", "<div><h4 style='margin-left:0px' class='" + language + "'>")
        .replaceAll("</h4>", "</h4></div>");

    HTML html = new HTML(content, direction);
    html.getElement().setId("maybeRTL_textContent");

    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
      html.addStyleName("rightTenMargin");
    }
    else {
      html.addStyleName("leftTenMargin");
    }
    html.addStyleName(language);

    html.addStyleName("wrapword");
    return html;
  }
}