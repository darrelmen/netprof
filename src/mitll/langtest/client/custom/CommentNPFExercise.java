package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise extends NPFExercise {
  private Set<String> clickedFields;

  public CommentNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                            float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  /**
   * @param e
   * @param content
   * @return
   * @see #getQuestionContent(mitll.langtest.shared.Exercise, com.google.gwt.user.client.ui.Panel)
   */
  @Override
  protected Widget getQuestionContent(Exercise e, String content) {
    clickedFields = new HashSet<String>();

    Panel column = new FlowPanel();
    column.getElement().setId("QuestionContent");
    column.setWidth("100%");

    column.add(getEntry(e, "foreignLanguage", ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));
    column.add(getEntry(e, "transliteration", ExerciseFormatter.TRANSLITERATION, e.getTranslitSentence()));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence()));

    return column;
  }

  /**
   * @see #getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
   * @param audioPanel
   * @param exercise
   * @param audioRef
   * @return
   */
  protected Widget getAudioPanelContent(ASRScoringAudioPanel audioPanel, Exercise exercise, String audioRef) {
    return getEntry(audioRef, audioPanel, exercise.getAnnotation(audioRef),true);
  }

  private Widget getEntry(Exercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getEntry(field, getContentWidget(label, value, true), annotation,false);
  }

  /**
   * TODO after edit, clear annotation -- where do we edit? in edit window
   *
   * @param field
   * @param annotation
   * @return
   * @paramx commentToLeft
   * @paramx label
   * @paramx value
   */
  private Widget getEntry(final String field, Widget content, ExerciseAnnotation annotation, boolean isAudio) {
    final Panel commentRow = new FlowPanel();
    final Button commentButton = new Button();
    final TextBox commentEntryText = new TextBox();

    FieldInfo fieldInfo = new FieldInfo(field);
    final FocusWidget commentEntry = makeCommentEntry(fieldInfo, annotation,commentEntryText,commentRow,commentButton);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();
    System.out.println("getEntry : field " +  field+ " annotation " + annotation + " correct " + alreadyMarkedCorrect);
    getQCWidget(commentButton,field, commentEntry, alreadyMarkedCorrect, commentRow,
        !alreadyMarkedCorrect ? annotation.comment : "");

    populateCommentRow(commentEntry, commentRow);

    // comment to left, content to right

    Panel row = new HorizontalPanel();
    row.setWidth("100%");
    commentButton.addStyleName("floatRight");
    row.add(content);
    content.setWidth("100%");

    row.add(commentButton);

    FocusPanel focusWidget = new FocusPanel(row);
    HandlerRegistration mouseOver = getMouseOver(field, commentButton, focusWidget);
    HandlerRegistration mouseOut = getMouseOut(field, commentButton, focusWidget);

    Panel rowContainer = new FlowPanel();
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(focusWidget);
    rowContainer.add(commentRow);
    rowContainer.setWidth("100%");

    showOrHideCommentButton(field, commentButton, alreadyMarkedCorrect);
    if (isAudio) audioInfo = new AudioInfo(field, commentEntryText, commentButton,focusWidget,mouseOver,mouseOut,fieldInfo);
    return rowContainer;
  }

  private HandlerRegistration getMouseOut(final String field, final Button commentButton, FocusPanel focusWidget) {
    return focusWidget.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          if (!inClickedFields(field)) {
            hideQC(commentButton);
          }
        }
      });
  }

  private HandlerRegistration getMouseOver(final String field, final Button commentButton, FocusPanel focusWidget) {
    return focusWidget.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          if (!inClickedFields(field)) {
            System.out.println("getMouseOver : can't find " + field + " in " + clickedFields);
            showQC(commentButton);
          }
        }
      });
  }

  private boolean inClickedFields(String field) {
    return clickedFields.contains(field) || clickedFields.contains(field.replaceAll(".wav",".mp3"));
  }

  @Override
  protected void setAudioRef(String audioRef) {
    if (!audioRef.equals(audioInfo.audioRef)) {
      ExerciseAnnotation annotation = exercise.getAnnotation(audioRef);
      boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();

      showOrHideCommentButton(audioRef, audioInfo.commentButton, alreadyMarkedCorrect);
      String comment = annotation == null ? "" : annotation.comment;
      setButtonTitle(audioInfo.commentButton, alreadyMarkedCorrect, comment);
      audioInfo.commentEntryText.setText(comment);

      audioInfo.mouseOver.removeHandler();
      audioInfo.mouseOut.removeHandler();

      audioInfo.mouseOver = getMouseOver(audioRef, audioInfo.commentButton, audioInfo.focusWidget);
      audioInfo.mouseOut  = getMouseOut(audioRef, audioInfo.commentButton, audioInfo.focusWidget);
      audioInfo.fieldInfo.field = audioRef;
      System.out.println("clickedFields now "+ clickedFields);
    }
  }

  private AudioInfo audioInfo;

  private static class AudioInfo {
    String audioRef;
    TextBox commentEntryText;
    Button commentButton;
    FocusPanel focusWidget;
    HandlerRegistration mouseOver;
    HandlerRegistration mouseOut;
    FieldInfo fieldInfo;

    public AudioInfo(String audioRef, TextBox commentEntryText, Button commentButton, FocusPanel focusWidget,
                     HandlerRegistration mouseOver, HandlerRegistration mouseOut, FieldInfo fieldInfo) {
      this.audioRef = audioRef;
      this.commentEntryText = commentEntryText;
      this.commentButton = commentButton;
      this.focusWidget = focusWidget;
      this.mouseOut = mouseOut;
      this.mouseOver = mouseOver;
      this.fieldInfo = fieldInfo;
    }
  }

  private static class FieldInfo {
    String field;
    public FieldInfo(String field) { this.field = field;}
  }

  private void showOrHideCommentButton(String field, Button commentButton, boolean alreadyMarkedCorrect) {
    if (alreadyMarkedCorrect) {
      hideQC(commentButton);
      clickedFields.remove(field);
    }
    else {
      showQCHasComment(commentButton);
      clickedFields.add(field);
    }
  }

  private Button getQCWidget(final Button child,final String field, final FocusWidget commentEntry,
                            final boolean alreadyMarkedCorrect, final Panel commentRow, String comment) {
    child.setIcon(IconType.COMMENT);
    child.setSize(ButtonSize.MINI);
    child.addStyleName("rightFiveMargin");
    child.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        clickedFields.add(field);
        showQCHasComment(child);
        reactToClick( commentRow, commentEntry);
      }
    });

    setButtonTitle(child, alreadyMarkedCorrect, comment);
    return child;
  }

  private void setButtonTitle(Button child, boolean alreadyMarkedCorrect, String comment) {
    if (alreadyMarkedCorrect) {
      child.setTitle("Add a comment");
    } else {
      child.setTitle(comment);
    }
  }

  private void showQC(Panel qcCol) {
    qcCol.removeStyleName("comment-button-group-new-hide");
    qcCol.addStyleName("comment-button-group-new");
  }

  private void hideQC(Panel qcCol) {
    qcCol.addStyleName("comment-button-group-new-hide");
    qcCol.removeStyleName("comment-button-group-new");
  }

  private void showQCHasComment(Button child) {
    child.removeStyleName("comment-button-group-new-hide");
    child.removeStyleName("comment-button-group-new");
  }

  private void populateCommentRow(FocusWidget commentEntry, Panel commentRow) {
    commentRow.setVisible(false);
    commentRow.add(getCommentLabel());
    commentRow.add(commentEntry);
  }

  private FocusWidget makeCommentEntry(final FieldInfo fieldInfo, ExerciseAnnotation annotation,
                                       final TextBox commentEntry,final Panel commentRow,
                                       final Button commentButton) {
    //final TextBox commentEntry = new TextBox();
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        postIncorrect(commentEntry, fieldInfo.field);
        commentButton.setTitle(commentEntry.getText());
      }
    });
    commentEntry.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        char keyPress = event.getCharCode();
        int key = (int) keyPress;
        if (keyPress == KeyCodes.KEY_ENTER || key == 0) {
       //   System.out.println("\tGot enter key " + keyPress);
          commentRow.setVisible(false);
          commentEntry.setFocus(false);
          postIncorrect(commentEntry, fieldInfo.field);
          commentButton.setTitle(commentEntry.getText());
        }
      /*  else {
          System.out.println("\tdid not get enter key '" + keyPress + "' (" +key+
            ") vs " + KeyCodes.KEY_ENTER  + " event " + event );
        }*/
      }
    });
    return commentEntry;
  }

  private void postIncorrect(TextBox commentEntry, String field) {
    final String commentToPost = commentEntry.getText();
    addIncorrectComment(commentToPost, field);
  }

  private void reactToClick( Panel commentRow, FocusWidget commentEntry) {
    boolean visible = commentRow.isVisible();
    commentRow.setVisible(!visible);
    commentEntry.setFocus(!visible);
  }
}
