/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseAnnotation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 9/8/14.
 */
public class CommentBox extends PopupContainer {
  private static final String COMMENT_BUTTON_GROUP_NEW = "comment-button-group-new";
  private final CommonExercise exercise;
  private final CommentAnnotator commentAnnotator;
  private final EventRegistration registration;
  private MyPopup commentPopup;

  /**
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getQuestionContent(mitll.langtest.shared.CommonExercise, String)
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param registration
   * @param commentAnnotator
   */
  public CommentBox(CommonExercise exercise, EventRegistration registration, CommentAnnotator commentAnnotator) {
    this.exercise = exercise;
    this.registration = registration;
    this.commentAnnotator = commentAnnotator;
  }

  /**
   * @param field      of the exercise to comment on
   * @param content    to wrap
   * @param annotation to get current comment from
   * @return three part widget -- content, comment button, and clear button
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getEntry
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   */
  public Widget getEntry(String field, Widget content, ExerciseAnnotation annotation) {
    final Button commentButton = new Button();
    //commentButton.getElement().setId("commentButton_for_"+field);

    if (field.endsWith(AudioTag.COMPRESSED_TYPE)) {
      field = field.replaceAll("." + AudioTag.COMPRESSED_TYPE, ".wav");
    }

    final HidePopupTextBox commentEntryText = new HidePopupTextBox();
    commentEntryText.getElement().setId("CommentEntryTextBox_for_" + field);
    commentEntryText.setVisibleLength(60);

    Button clearButton = getClearButton(commentEntryText, commentButton, field);
    commentPopup = makeCommentPopup(field, commentButton, commentEntryText, clearButton);
    commentPopup.addAutoHidePartner(commentButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    configureTextBox(annotation != null ? annotation.getComment() : null, commentEntryText, commentPopup);

    boolean isCorrect = annotation == null || annotation.getStatus() == null || annotation.isCorrect();
    String comment = !isCorrect ? annotation.getComment() : "";
/*    System.out.println("getEntry : field " + field + " annotation " + annotation +
      " correct " + isCorrect + " comment '" + comment+
      "', fields " + exercise.getFields());*/

    configureCommentButton(commentButton,
        isCorrect, commentPopup,
        comment, commentEntryText);

    // content on left side, comment button on right

    Panel row = new HorizontalPanel();
    row.getElement().setId("comment_and_clear_container_for_"+field);
    if (content != null) row.add(content);
    row.add(commentButton);
    row.add(clearButton);

    showOrHideCommentButton(commentButton, clearButton, isCorrect);
    return row;
  }

  public boolean isPopupShowing() { return commentPopup.isShowing(); }

  /**
   * @param field
   * @param commentButton
   * @param commentEntryText
   * @param clearButton
   * @return
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   */
  private MyPopup makeCommentPopup(String field, Button commentButton,
                                   HidePopupTextBox commentEntryText, Button clearButton) {
    final MyPopup commentPopup = new MyPopup();
    commentPopup.setAutoHideEnabled(true);
    commentPopup.configure(commentEntryText, commentButton, clearButton);
    commentPopup.setField(field);

    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(getOKButton(commentPopup, null));

    commentPopup.add(hp);
    return commentPopup;
  }

  /**
   * Post correct comment to server when clear button is clicked.
   *
   * @param commentEntryText
   * @param commentButton
   * @param field
   * @return
   */
  private Button getClearButton(final HidePopupTextBox commentEntryText,
                                final Widget commentButton, final String field) {
    final Button clear = new Button("");
    clear.getElement().setId("CommentNPFExercise_" + field);
    registration.register(clear, exercise.getID(), "clear comment");
    clear.addStyleName("leftFiveMargin");
    addTooltip(clear, "Clear comment");

    clear.setIcon(IconType.REMOVE);
    clear.setSize(ButtonSize.MINI);
    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentEntryText.setText("");
        showOrHideCommentButton(commentButton, clear, true);
        exercise.addAnnotation(field, "correct", "");
        setButtonTitle(commentButton, true, "");
        commentAnnotator.addCorrectComment(field);
      }
    });
    return clear;
  }

  private class MyPopup extends DecoratedPopupPanel {
    private String field;

    public void setField(String field) {
      this.field = field;
    }

    /**
     * @param commentBox
     * @param commentButton
     * @param clearButton
     */
    public void configure(final TextBox commentBox, final Widget commentButton, final Widget clearButton) {
      addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          registration.logEvent(commentBox, "Comment_TextBox", exercise.getID(), "submit comment '" + commentBox.getValue() +
              "'");
          commentComplete(commentBox, field, commentButton, clearButton);
        }
      });
    }
  }

  private void showOrHideCommentButton(UIObject commentButton, UIObject clearButton, boolean isCorrect) {
    if (isCorrect) {
      showQC(commentButton);
    } else {
      showQCHasComment(commentButton);
    }
    clearButton.setVisible(!isCorrect);
  }

  /**
   * @param commentButton
   * @param alreadyMarkedCorrect
   * @param commentPopup
   * @param comment
   * @param commentEntry
   * @return
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   */
  private void configureCommentButton(final Button commentButton,
                                      final boolean alreadyMarkedCorrect, final PopupPanel commentPopup, String comment,
                                      final TextBox commentEntry) {
    commentButton.setIcon(IconType.COMMENT);
    commentButton.setSize(ButtonSize.MINI);
    commentButton.addStyleName("leftTenMargin");
    commentButton.getElement().setId("CommentNPFExercise_comment");
    registration.register(commentButton, exercise.getID(), "show comment");

    final Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    configurePopupButton(commentButton, commentPopup, commentEntry, tooltip);

    showQC(commentButton);
  }

  private Tooltip setButtonTitle(Widget button, boolean isCorrect, String comment) {
    String tip = isCorrect ? "Add a comment" : "\"" + comment + "\"";
    return addTooltip(button, tip);
  }

  private final Map<String, String> fieldToComment = new HashMap<String, String>();

  private void showQC(UIObject qcCol)           { qcCol.addStyleName(COMMENT_BUTTON_GROUP_NEW);  }
  private void showQCHasComment(UIObject child) {
    child.removeStyleName(COMMENT_BUTTON_GROUP_NEW);
  }

  /**
   * @param commentEntry
   * @param field
   * @param commentButton
   * @param clearButton
   * @see MyPopup#configure(com.github.gwtbootstrap.client.ui.TextBox, com.google.gwt.user.client.ui.Widget, com.google.gwt.user.client.ui.Widget)
   */
  private <T extends TextBox> void commentComplete(T commentEntry, String field, Widget commentButton, Widget clearButton) {
    String comment = commentEntry.getText();
    String previous = fieldToComment.get(field);
    if (previous == null || !previous.equals(comment)) {
      fieldToComment.put(field, comment);
      boolean isCorrect = comment.length() == 0;
//      System.out.println("commentComplete " + field + " comment '" + comment +"' correct = " +isCorrect);

      setButtonTitle(commentButton, isCorrect, comment);
      showOrHideCommentButton(commentButton, clearButton, isCorrect);
      if (isCorrect) {
        commentAnnotator.addCorrectComment(field);
      } else {
        commentAnnotator.addIncorrectComment(comment, field);
      }
      //System.out.println("\t commentComplete : annotations now " + exercise.getFields());
    }
  }

  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }
}
