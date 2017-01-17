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
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/1/13
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
class AudioExerciseContent {
  private static final boolean SHOW_ID = true;
  private final Logger logger = Logger.getLogger("AudioExerciseContent");
  private static final String QUESTION = "question";
  private static final String QUESTION_HEADING = "h4";
  private static final String ITEM = "Passage";

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
   * @see mitll.langtest.client.amas.FeedbackRecordPanel#getQuestionContent
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
  private Panel makeFlashcardForCRT(AmasExerciseImpl e, String content, boolean includeExerciseID,
                                    boolean showQuestion, int index, int totalInQuiz) {
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
      logger.warning("huh? no fl questions for " + e);
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
   * @see #makeFlashcardForCRT
   */
  private void addAudioRow(AmasExerciseImpl e, String content, boolean includeExerciseID, Panel container,
                           int index, int totalInQuiz) {
    Panel horiz = new FlowPanel();
    horiz.getElement().setId("item_and_content");
    container.add(horiz);
    logger.info("for " + e.getOldID() + " include " + includeExerciseID);

    if (includeExerciseID && e.getOrient() == null) {
      DivWidget itemHeaderContainer = new DivWidget();
     // itemHeaderContainer.setWidth("100%");
      itemHeaderContainer.setHeight("20px");
      itemHeaderContainer.getElement().setId("itemHeaderContainer");
      Heading child = getItemHeader(index, totalInQuiz, e.getOldID());
      itemHeaderContainer.add(child);
      horiz.add(itemHeaderContainer);
    }

    content = content.replaceAll("h2", "h4");
    content = changeAudioPrompt(content, e.getQuestions().size() > 1);

    Widget contentFromPrefix = getContentFromPrefix(e.getAudioURL() == null ? content : "");

    if (rightAlignContent) {
      contentFromPrefix.addStyleName("floatRight");
    }

    horiz.add(contentFromPrefix);
  }

  /**
   * @see AmasExercisePanel#addInstructions
   * @param index
   * @param totalInQuiz
   * @param id
   * @return
   */
  public static Heading getItemHeader(int index, int totalInQuiz, String id) {
    String text = ITEM + " #" + (index + 1) + " of " + totalInQuiz + (SHOW_ID ?" : " + id : "");
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

  /**
   * @see #addAudioRow(AmasExerciseImpl, String, boolean, Panel, int, int)
   * @param suffix
   * @param isPlural
   * @return
   */
  private String changeAudioPrompt(String suffix, boolean isPlural) {
    if (responseType.equals(PropertyHandler.TEXT)) {
      suffix = suffix.replace("answer the " + QUESTION, "type your answer to the " + QUESTION);
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
        requireAlignment && rightAlignContent ?
            HasDirection.Direction.RTL :
            WordCountDirectionEstimator.get().estimateDirection(content);

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