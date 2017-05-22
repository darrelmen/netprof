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

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.result.AudioTag;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.flashcard.FlashcardPanel;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.exercise.ExerciseAnnotation.TYPICAL;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/8/14.
 */
public class CommentBox extends PopupContainerFactory {
  private final Logger logger = Logger.getLogger("CommentBox");

  private static final int ENTRY_VISIBLE_LENGTH = 70;

  private static final int MAX_LENGTH = 500;

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String CLEAR_COMMENT = "Clear comment";

  private static final String COMMENT_BUTTON_GROUP_NEW = "comment-button-group-new";
  private final int exerciseID;
  private final CommentAnnotator commentAnnotator;
  private final EventRegistration registration;
  private DecoratedPopupPanel commentPopup;
  private final MutableAnnotationExercise annotationExercise;
  private final boolean tooltipOnRight;

  /**
   * @param exerciseID
   * @param registration
   * @param commentAnnotator
   * @see GoodwaveExercisePanel#getQuestionContent
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   */
  public CommentBox(int exerciseID, EventRegistration registration, CommentAnnotator commentAnnotator,
                    MutableAnnotationExercise annotationExercise, boolean tooltipOnRight) {
    this.exerciseID = exerciseID;
    this.registration = registration;
    this.commentAnnotator = commentAnnotator;
    this.annotationExercise = annotationExercise;
    this.tooltipOnRight = tooltipOnRight;
  }

  /**
   * @param field
   * @param content
   * @param annotation
   * @return
   * @seex CommentNPFExercise#getContext
   */
/*  public Widget getNoPopup(String field,
                           Widget content,
                           final ExerciseAnnotation annotation,
                           AnnotationExercise toSet) {
    TextBox commentEntryText = new TextBox();
    Panel commentAndOK = new HorizontalPanel();

    Button ok = new Button("Comment");
    ok.setType(ButtonType.SUCCESS);
    ok.addStyleName("leftTenMargin");

    final String initialText = annotation != null ? annotation.getComment() : "";
    Button cancel = new Button("Cancel");
    cancel.setType(ButtonType.WARNING);
    cancel.addStyleName("leftTenMargin");
    cancel.addClickHandler(clickEvent -> {
      commentAndOK.setVisible(false);
      commentEntryText.setText(initialText);
    });

    commentEntryText.setVisibleLength(ENTRY_VISIBLE_LENGTH);
    final Button commentButton = new Button();
    commentButton.addClickHandler(clickEvent -> {
      commentAndOK.setVisible(true);
      commentEntryText.setFocus(true);
    });

    if (!initialText.isEmpty()) {
      commentEntryText.setText(initialText);
      if (commentEntryText.getVisibleLength() < initialText.length()) {
        commentEntryText.setVisibleLength(ENTRY_VISIBLE_LENGTH);
      }
    }

//    commentEntryText.addStyleName("topFiveMargin");
    commentAndOK.add(commentEntryText);
    commentAndOK.add(ok);
    commentAndOK.add(cancel);
    commentAndOK.setVisible(false);

    Button clearButton = getClearButton(commentEntryText, commentButton, field);
    clearButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        commentAndOK.setVisible(false);
      }
    });

    boolean isCorrect = annotation == null || annotation.getStatus() == null || annotation.isCorrect();
    String comment = !isCorrect ? annotation.getComment() : "";
//    logger.info("getEntry : field " + field + " annotation " + annotation +
//        " correct " + isCorrect + " comment '" + comment +
//        "'");
    styleCommentButton(commentButton);

    *//*Tooltip tooltip =*//*
    setButtonTitle(commentButton, isCorrect, comment);
    //showQC(commentButton);

    // content on left side, comment button on right
    DivWidget row2 = new DivWidget();
    row2.add(content);
    row2.add(commentAndOK);

    Panel row = getCommentAndButtonsRow(field, row2, commentButton, clearButton);
    showOrHideCommentButton(commentButton, clearButton, isCorrect);

    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentAndOK.setVisible(false);
        commentComplete(commentEntryText, field, commentButton, clearButton);
        String text = sanitize(commentEntryText.getText());

        ExerciseAnnotation toUse = annotation;
        if (annotation != null) {
          annotation.setComment(text);
          annotation.setStatus(TYPICAL.INCORRECT.toString());
          logger.info("anno now " + annotation);
        } else {
          toUse = new ExerciseAnnotation(TYPICAL.INCORRECT.toString(), text);
        }
        toSet.getFieldToAnnotation().put(field, toUse);
      }
    });

    return row;

  }*/

  private Button commentButton;
  private Button clearButton;
  private Widget theContent;

  public void showButtons() {
    commentButton.setVisible(true);
    clearButton.setVisible(hasComment);
  }

  public void hideButtons() {
    commentButton.setVisible(false);
    clearButton.setVisible(false);
  }

  private boolean hasComment = false;

  /**
   * @param field         of the exerciseID to comment on
   * @param content       to wrap
   * @param annotation    to get current comment from
   * @param showInitially
   * @param isRTL
   * @return three part widget -- content, comment button, and clear button
   * @seex mitll.langtest.client.custom.exercise.CommentNPFExercise#getEntry
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   */
  public DivWidget getEntry(String field, Widget content, ExerciseAnnotation annotation, boolean showInitially, boolean isRTL) {
    this.theContent = content;

    field = fixAudioField(field);
    final HidePopupTextBox commentEntryText = getCommentBox(field);

    final Button commentButton = new Button();
    Button clearButton = getClearButton(commentEntryText, commentButton, field);

    this.commentButton = commentButton;
    this.clearButton = clearButton;

    commentPopup = makeCommentPopup(field, commentButton, commentEntryText, clearButton);
    commentPopup.addAutoHidePartner(commentButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    configureTextBox(annotation != null ? annotation.getComment() : null, commentEntryText, commentPopup);

    boolean isCorrect = annotation == null || annotation.getStatus() == null || annotation.isCorrect();
    String comment = !isCorrect ? annotation.getComment() : "";
    hasComment = !isCorrect;
/*    System.out.println("getEntry : field " + field + " annotation " + annotation +
      " correct " + isCorrect + " comment '" + comment+
      "', fields " + exerciseID.getFields());*/

    configureCommentButton(commentButton,
        isCorrect,
        commentPopup,
        comment,
        commentEntryText);

    // content on left side, comment button on right

    DivWidget row = getCommentAndButtonsRow(field, content, commentButton, clearButton, isRTL);
    row.setWidth("100%");
    showOrHideCommentButton(commentButton, clearButton, isCorrect);

    if (!showInitially) {
      commentButton.setVisible(false);
      clearButton.setVisible(false);
    }

    return row;
  }

  private DivWidget getCommentAndButtonsRow(String field, Widget content, Button commentButton, Button clearButton, boolean isRTL) {
    DivWidget row = new DivWidget();
    row.getElement().setId("comment_and_clear_container_for_" + field);
    if (content != null) {
      row.add(content);
      if (isRTL) {
        content.addStyleName("floatRight");
      } else {
        content.addStyleName("floatLeft");
      }
    }
    if (isRTL) {
      row.addStyleName("floatRight");
    } else {
      row.addStyleName("floatLeft");
    }

    row.add(commentButton);
    commentButton.addStyleName("floatLeft");

    row.add(clearButton);
    clearButton.addStyleName("floatLeft");

    return row;
  }

  private HidePopupTextBox getCommentBox(String field) {
    final HidePopupTextBox commentEntryText = new HidePopupTextBox();
    commentEntryText.getElement().setId("CommentEntryTextBox_for_" + field);
    commentEntryText.setVisibleLength(60);
    return commentEntryText;
  }

  private String fixAudioField(String field) {
    if (field.endsWith(AudioTag.COMPRESSED_TYPE)) {
      field = field.replaceAll("." + AudioTag.COMPRESSED_TYPE, WAV);
    } else if (field.endsWith(MP3)) {
      field = field.replaceAll(MP3, WAV);
    }
    return field;
  }

  /**
   * @return
   * @see FlashcardPanel#otherReasonToIgnoreKeyPress
   */
  public boolean isPopupShowing() {
    return commentPopup.isShowing();
  }

  /**
   * @param field
   * @param commentButton
   * @param commentEntryText
   * @param clearButton
   * @return
   * @see #getEntry(String, Widget, ExerciseAnnotation, boolean, boolean)
   */
  private DecoratedPopupPanel makeCommentPopup(String field,
                                               Button commentButton,
                                               HidePopupTextBox commentEntryText,
                                               Button clearButton) {
    DecoratedPopupPanel popup;
    final MyPopup commentPopup = new MyPopup();
    commentPopup.setAutoHideEnabled(true);
    commentPopup.configure(commentEntryText, commentButton, clearButton);
    commentPopup.setField(field);
    popup = commentPopup;

    Panel hp = new HorizontalPanel();
    hp.getElement().setId("comment_container");
    hp.add(commentEntryText);
    hp.add(getOKButton(popup, null));

    popup.add(hp);
    return popup;
  }

  /**
   * Post correct comment to server when clear button is clicked.
   *
   * @param commentEntryText
   * @param commentButton
   * @param field
   * @return
   * @see #getEntry(String, Widget, ExerciseAnnotation, boolean, boolean)
   */
  private Button getClearButton(final TextBox commentEntryText,
                                final Widget commentButton,
                                final String field) {
    final Button clear = new Button("");
    clear.getElement().setId("CommentNPFExercise_" + field);
    registration.register(clear, exerciseID, "clear comment");
    clear.addStyleName("leftFiveMargin");
    addTooltip(clear, CLEAR_COMMENT);

    clear.setIcon(IconType.REMOVE);
    clear.setSize(ButtonSize.MINI);
    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentEntryText.setText("");
        showOrHideCommentButton(commentButton, clear, true);

        annotationExercise.addAnnotation(field, TYPICAL.CORRECT.toString(), "");
        setButtonTitle(commentButton, true, "");
        commentAnnotator.addCorrectComment(field);
      }
    });
    return clear;
  }

  public Widget getTheContent() {
    return theContent;
  }

  private class MyPopup extends DecoratedPopupPanel {
    private String field;

    public void setField(String field) {
      this.field = field;
    }

    @Override
    protected void onLoad() {
      logger.info("got load on " + getElement().getId());
    }

    /**
     * @param commentBox
     * @param commentButton
     * @param clearButton
     * @see #makeCommentPopup(String, Button, HidePopupTextBox, Button)
     */
    void configure(final TextBox commentBox, final Widget commentButton, final Widget clearButton) {
      addCloseHandler(event ->
      {
        registration.logEvent(commentBox, "Comment_TextBox", exerciseID, "submit comment '" + commentBox.getValue() + "'");
        commentComplete(commentBox, field, commentButton, clearButton);
      });
    }
  }

  /**
   * @param commentButton
   * @param clearButton
   * @param isCorrect
   * @see #commentComplete
   */
  private void showOrHideCommentButton(UIObject commentButton, UIObject clearButton, boolean isCorrect) {
    if (isCorrect) {
      showQC(commentButton);
    } else {
      showQCHasComment(commentButton);
    }
    clearButton.setVisible(!isCorrect);
    hasComment = !isCorrect;
  }

  /**
   * @param commentButton
   * @param alreadyMarkedCorrect
   * @param commentPopup
   * @param comment
   * @param commentEntry
   * @return
   * @see #getEntry(String, Widget, ExerciseAnnotation, boolean, boolean)
   */
  private void configureCommentButton(final Button commentButton,
                                      final boolean alreadyMarkedCorrect,
                                      final PopupPanel commentPopup,
                                      String comment,
                                      final TextBox commentEntry) {
    styleCommentButton(commentButton);

    Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    configurePopupButton(commentButton, commentPopup, commentEntry, tooltip);

    showQC(commentButton);
  }

  private void styleCommentButton(Button commentButton) {
    commentButton.setIcon(IconType.COMMENT);
    commentButton.setSize(ButtonSize.MINI);
    commentButton.addStyleName("leftTenMargin");
    commentButton.getElement().setId("CommentNPFExercise_comment");
    registration.register(commentButton, exerciseID, "show comment");
  }

  private final Map<String, String> fieldToComment = new HashMap<>();

  /**
   * @param commentButton
   * @see #configureCommentButton(Button, boolean, PopupPanel, String, TextBox)
   */
  private void showQC(UIObject commentButton) {
    commentButton.addStyleName(COMMENT_BUTTON_GROUP_NEW);
  }

  /**
   * @param commentButton
   * @see #showOrHideCommentButton(UIObject, UIObject, boolean)
   */
  private void showQCHasComment(UIObject commentButton) {
    commentButton.removeStyleName(COMMENT_BUTTON_GROUP_NEW);
  }

  /**
   * @param commentEntry
   * @param field
   * @param commentButton
   * @param clearButton
   * @see MyPopup#configure(com.github.gwtbootstrap.client.ui.TextBox, com.google.gwt.user.client.ui.Widget, com.google.gwt.user.client.ui.Widget)
   */
  private <T extends ValueBoxBase> void commentComplete(T commentEntry, String field, Widget commentButton, Widget clearButton) {
    commentComplete(field, commentButton, clearButton, sanitize(commentEntry.getText()));
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  private void commentComplete(String field, Widget commentButton, Widget clearButton, String comment) {
    String previous = fieldToComment.get(field);
    comment = normalize(comment);
    if (previous == null || !previous.equals(comment)) {
      fieldToComment.put(field, comment);
      boolean isCorrect = comment.isEmpty();

      logger.info("commentComplete " + field + " comment '" + comment + "' correct = " + isCorrect);

      setButtonTitle(commentButton, isCorrect, comment);
      showOrHideCommentButton(commentButton, clearButton, isCorrect);
      if (isCorrect) {
        commentAnnotator.addCorrectComment(field);
      } else {
        commentAnnotator.addIncorrectComment(comment, field);
      }

      // remember to update the exercise itself
      annotationExercise.addAnnotation(field, isCorrect ? TYPICAL.CORRECT.toString() : TYPICAL.INCORRECT.toString(), comment);
      //logger.info("\t commentComplete : annotations now " + exerciseID.getFields());
    }
  }


  private String normalize(String comment) {
    comment = comment.trim();
    if (comment.length() > MAX_LENGTH) comment = comment.substring(0, MAX_LENGTH);
    return comment;
  }

  private Tooltip setButtonTitle(Widget button, boolean isCorrect, String comment) {
    String tip = isCorrect ? "Add a comment" : "\"" + comment + "\"";
    return addTooltip(button, tip);
  }

  private Tooltip addTooltip(Widget w, String tip) {
    TooltipHelper tooltipHelper = new TooltipHelper();
    return tooltipOnRight ?
        tooltipHelper.addTooltip(w, tip) :
        tooltipHelper.createAddTooltip(w, tip, Placement.LEFT);
  }
}
