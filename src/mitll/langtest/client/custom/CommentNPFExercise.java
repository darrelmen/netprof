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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.Date;
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
    Panel column = new FlowPanel();
    column.getElement().setId("QuestionContent");
    column.setWidth("100%");

    clickedFields = new HashSet<String>();

    column.add(getEntry(e, "foreignLanguage", ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));
    column.add(getEntry(e, "transliteration", ExerciseFormatter.TRANSLITERATION, e.getTranslitSentence()));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence()));

    return column;
  }

  protected Widget getAudioPanelContent(ASRScoringAudioPanel audioPanel, Exercise exercise, String audioRef) {
    ExerciseAnnotation audioAnnotation = exercise.getAnnotation(audioRef);

    return getEntry(audioRef, audioPanel, audioAnnotation);
  }

  private Widget getEntry(Exercise e, final String field, final String label, String value) {
    ExerciseAnnotation currentAnnotation = e.getAnnotation(field);
    return getEntry(field, label, value, currentAnnotation);
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    Panel nameValueRow = getContentWidget(label, value);
    return getEntry(field, nameValueRow, annotation);
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
  private Widget getEntry(final String field, Widget content, ExerciseAnnotation annotation) {
    final Panel commentRow = new FlowPanel();
    final Button commentButton = new Button();

    final FocusWidget commentEntry = makeCommentEntry(field, annotation,commentRow,commentButton);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    System.out.println("field " +  field+
        " annotation " + annotation + " correct " + alreadyMarkedCorrect);
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
    focusWidget.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        if (!clickedFields.contains(field)) {
          showQC(commentButton);

        }
      }
    });
    focusWidget.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        if (!clickedFields.contains(field)) {
          hideQC(commentButton);
        }
      }
    });

    Panel rowContainer = new FlowPanel();
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(focusWidget);
    rowContainer.add(commentRow);
    rowContainer.setWidth("100%");

    if (alreadyMarkedCorrect) {
      hideQC(commentButton);
    }
    else {
      showQCHasComment(commentButton);
      clickedFields.add(field);
    }
    return rowContainer;
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

    if (alreadyMarkedCorrect) {
      child.setTitle("Add a comment");
    } else {
      child.setTitle(comment);
    }
    return child;
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

    final Label commentLabel = getCommentLabel();

    commentRow.add(commentLabel);
    commentRow.add(commentEntry);
  }

  private Label getCommentLabel() {
    final Label commentLabel = new Label("comment?");
    DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
    commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }

  private FocusWidget makeCommentEntry(final String field, ExerciseAnnotation annotation, final Panel commentRow,
                                       final Button commentButton) {
    final TextBox commentEntry = new TextBox();
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        postIncorrect(commentEntry, field);
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
          postIncorrect(commentEntry, field);
          commentRow.setVisible(false);
          commentEntry.setFocus(false);
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

  protected void addIncorrectComment(final String commentToPost, final String field) {
    System.out.println(new Date() + " : post to server " + exercise.getID() +
      " field " + field + " commentLabel " + commentToPost + " is incorrect");
    final long then = System.currentTimeMillis();
    service.addAnnotation(exercise.getID(), field, "incorrect", commentToPost, controller.getUser(),new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
        long now = System.currentTimeMillis();
        System.out.println("\t" + new Date() + " : posted to server " + exercise.getID() +
          " field " + field + " commentLabel " + commentToPost + " is incorrect, took " + (now - then) + " millis");

      }
    });
  }

  private void reactToClick( Panel commentRow, FocusWidget commentEntry) {
    boolean visible = commentRow.isVisible();
    commentRow.setVisible(!visible);
    commentEntry.setFocus(!visible);
  }

  private Panel getContentWidget(String label, String value) {
    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    InlineHTML foreignPhrase = new InlineHTML(label);
    foreignPhrase.addStyleName("Instruction-title");
    nameValueRow.add(foreignPhrase);

    InlineHTML englishPhrase = new InlineHTML(value);
    englishPhrase.addStyleName("Instruction-data-with-wrap");
    nameValueRow.add(englishPhrase);
    englishPhrase.addStyleName("leftFiveMargin");
    return nameValueRow;
  }
}
