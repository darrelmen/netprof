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
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.exercise.MutableAnnotationExercise;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/8/14.
 */
public class CommentBox extends PopupContainer {
  //private final Logger logger = Logger.getLogger("CommentBox");

  private static final String COMMENT_BUTTON_GROUP_NEW = "comment-button-group-new";
  private final int exerciseID;
  private final CommentAnnotator commentAnnotator;
  private final EventRegistration registration;
  private MyPopup commentPopup;
  private final MutableAnnotationExercise annotationExercise;

  /**
   * @param exerciseID
   * @param registration
   * @param commentAnnotator
   * @see GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.exercise.CommonShell)
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   */
  public CommentBox(int exerciseID, EventRegistration registration, CommentAnnotator commentAnnotator,
                    MutableAnnotationExercise annotationExercise) {
    this.exerciseID = exerciseID;
    this.registration = registration;
    this.commentAnnotator = commentAnnotator;
    this.annotationExercise = annotationExercise;
  }

  /**
   * @param field      of the exerciseID to comment on
   * @param content    to wrap
   * @param annotation to get current comment from
   * @return three part widget -- content, comment button, and clear button
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getEntry
   * @see mitll.langtest.client.flashcard.FlashcardPanel#getFirstRow(mitll.langtest.client.exercise.ExerciseController)
   */
  public Widget getEntry(String field, Widget content, ExerciseAnnotation annotation) {
    final Button commentButton = new Button();

    if (field.endsWith(AudioTag.COMPRESSED_TYPE)) {
      field = field.replaceAll("." + AudioTag.COMPRESSED_TYPE, ".wav");
    } else if (field.endsWith(".mp3")) {
      field = field.replaceAll(".mp3", ".wav");
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
      "', fields " + exerciseID.getFields());*/

    configureCommentButton(commentButton,
        isCorrect, commentPopup,
        comment, commentEntryText);

    // content on left side, comment button on right

    Panel row = new HorizontalPanel();
    row.getElement().setId("comment_and_clear_container_for_" + field);
    if (content != null) row.add(content);
    row.add(commentButton);
    row.add(clearButton);

    showOrHideCommentButton(commentButton, clearButton, isCorrect);
    return row;
  }

  public boolean isPopupShowing() {
    return commentPopup.isShowing();
  }

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
    registration.register(clear, exerciseID, "clear comment");
    clear.addStyleName("leftFiveMargin");
    addTooltip(clear, "Clear comment");

    clear.setIcon(IconType.REMOVE);
    clear.setSize(ButtonSize.MINI);
    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentEntryText.setText("");
        showOrHideCommentButton(commentButton, clear, true);

        annotationExercise.addAnnotation(field, "correct", "");
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
    void configure(final TextBox commentBox, final Widget commentButton, final Widget clearButton) {
      addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          registration.logEvent(commentBox, "Comment_TextBox", exerciseID, "submit comment '" + commentBox.getValue() + "'");
          commentComplete(commentBox, field, commentButton, clearButton);
        }
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
    registration.register(commentButton, exerciseID, "show comment");

    Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    configurePopupButton(commentButton, commentPopup, commentEntry, tooltip);

    showQC(commentButton);
  }

  private Tooltip setButtonTitle(Widget button, boolean isCorrect, String comment) {
    String tip = isCorrect ? "Add a comment" : "\"" + comment + "\"";
    return addTooltip(button, tip);
  }

  private final Map<String, String> fieldToComment = new HashMap<>();

  /**
   * @param commentButton
   * @see #configureCommentButton(Button, boolean, PopupPanel, String, TextBox)
   */
  private void showQC(UIObject commentButton) {
    commentButton.addStyleName(COMMENT_BUTTON_GROUP_NEW);
    //  commentButton.removeStyleName("blueBackground");
  }

  /**
   * @param commentButton
   * @see #showOrHideCommentButton(UIObject, UIObject, boolean)
   */
  private void showQCHasComment(UIObject commentButton) {
    commentButton.removeStyleName(COMMENT_BUTTON_GROUP_NEW);
    // child.addStyleName("blueBackground");
  }

  /**
   * @param commentEntry
   * @param field
   * @param commentButton
   * @param clearButton
   * @see MyPopup#configure(com.github.gwtbootstrap.client.ui.TextBox, com.google.gwt.user.client.ui.Widget, com.google.gwt.user.client.ui.Widget)
   */
  private <T extends ValueBoxBase> void commentComplete(T commentEntry, String field, Widget commentButton, Widget clearButton) {
    String comment = commentEntry.getText();
    commentComplete(field, commentButton, clearButton, comment);
  }

  private void commentComplete(String field, Widget commentButton, Widget clearButton, String comment) {
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
      //System.out.println("\t commentComplete : annotations now " + exerciseID.getFields());
    }
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }
}
