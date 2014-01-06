package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise extends NPFExercise {
  //private Map<String,AudioInfo> fieldToAudioInfo = new HashMap<String, AudioInfo>();
  private AudioInfo audioInfo;

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
   */
  private Widget getEntry(final String field, Widget content, ExerciseAnnotation annotation, boolean isAudio) {
    final Button commentButton = new Button();
    final TextBox commentEntryText = new TextBox();
    final PopupPanel commentPopup = new DecoratedPopupPanel();
    commentPopup.setAutoHideEnabled(true);

    HorizontalPanel hp = new HorizontalPanel();

    hp.add(commentEntryText);
    Button ok = new Button("OK");
    ok.setType(ButtonType.PRIMARY);
    ok.addStyleName("leftTenMargin");
    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.hide();
      }
    });
    hp.add(ok);

    Button clear = new Button("Clear");
    clear.setType(ButtonType.INFO);
    clear.addStyleName("leftFiveMargin");
    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentEntryText.setText("");
        commentComplete(commentEntryText, field, commentButton);
        commentPopup.hide();
      }
    });
    hp.add(clear);
    commentPopup.add(hp);

    FieldInfo fieldInfo = new FieldInfo(field);
    final FocusWidget commentEntry =
      configureCommentTextBox(fieldInfo.field, annotation, commentEntryText, commentPopup, commentButton);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();
    System.out.println("getEntry : field " +  field+ " annotation " + annotation + " correct " + alreadyMarkedCorrect);
    String comment = !alreadyMarkedCorrect ? annotation.comment : "";
    getQCWidget(commentButton,
      alreadyMarkedCorrect, commentPopup,
      comment,commentEntry);

    // content on left side, comment button on right

    Panel row = new HorizontalPanel();
    row.add(content);
    row.add(commentButton);
    FocusPanel focusWidget = new FocusPanel(row);
      showOrHideCommentButton(commentButton, alreadyMarkedCorrect);
    if (isAudio) {
      // TODO fixx this
      // fieldToAudioInfo.put(field,new AudioInfo(field, commentEntryText, commentButton, focusWidget, fieldInfo));
      audioInfo = new AudioInfo(field, commentEntryText, commentButton, /*focusWidget, */fieldInfo);
    }
    return row;
  }

  @Override
  protected void setAudioRef(String audioRef) {
    if (!audioRef.equals(audioInfo.audioRef)) {
      ExerciseAnnotation annotation = exercise.getAnnotation(audioRef);
      boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();

      showOrHideCommentButton(audioInfo.commentButton, alreadyMarkedCorrect);
      String comment = annotation == null ? "" : annotation.comment;
      setButtonTitle(audioInfo.commentButton, alreadyMarkedCorrect, comment);
      audioInfo.commentEntryText.setText(comment);
      audioInfo.fieldInfo.field = audioRef;
    }
  }

  // TODO : handle multiple audio infos!

  private static class AudioInfo {
    String audioRef;
    TextBox commentEntryText;
    Button commentButton;
   // FocusPanel focusWidget;
    FieldInfo fieldInfo;

    public AudioInfo(String audioRef, TextBox commentEntryText, Button commentButton,// FocusPanel focusWidget,
                     FieldInfo fieldInfo) {
      this.audioRef = audioRef;
      this.commentEntryText = commentEntryText;
      this.commentButton = commentButton;
     // this.focusWidget = focusWidget;
      this.fieldInfo = fieldInfo;
    }
  }

  private static class FieldInfo {
    String field;
    public FieldInfo(String field) { this.field = field;}
  }

  private void showOrHideCommentButton(UIObject commentButton, boolean isCorrect) {
    if (isCorrect) {
      showQC(commentButton);
    }
    else {
      showQCHasComment(commentButton);
    }
  }

  private Button getQCWidget(final Button commentButton,
                            final boolean alreadyMarkedCorrect, final PopupPanel commentPopup, String comment,
                            final FocusWidget commentEntry) {
    commentButton.setIcon(IconType.COMMENT);
    commentButton.setSize(ButtonSize.MINI);
    commentButton.addStyleName("leftFiveMargin");
    final Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    commentButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.showRelativeTo(commentButton);
        commentEntry.setFocus(true);
        tooltip.hide();
      }
    });

    showQC(commentButton);
    return commentButton;
  }

  private Tooltip setButtonTitle(Widget button, boolean isCorrect, String comment) {
    String tip = isCorrect ? "Add a comment" : "\""+ comment + "\"";
    return createAddTooltip(button, tip, Placement.RIGHT);
  }

  private void showQC(UIObject qcCol) {
    qcCol.addStyleName("comment-button-group-new");
  }

  private void showQCHasComment(UIObject child) {
    child.removeStyleName("comment-button-group-new");
  }

  /**
   * For this field configure the commentEntry box to post annotation on blur and enter
   *
   * @param field for this field
   * @param annotation fill in with existing annotation, if there is one
   * @param commentEntry comment box to configure
   * @paramx commentRow
   * @param commentButton
   * @return
   */
  private FocusWidget configureCommentTextBox(final String field, ExerciseAnnotation annotation,
                                              final TextBox commentEntry,
                                              final PopupPanel popup,
                                              final Widget commentButton) {
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        commentComplete(commentEntry, field, commentButton);
      }
    });
    commentEntry.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        int keyCode = event.getNativeEvent ().getKeyCode();
        if (keyCode == KeyCodes.KEY_ENTER) {
          commentComplete(commentEntry, field, commentButton);
          popup.hide();
        }
      }
    });
    return commentEntry;
  }

  private void commentComplete(TextBox commentEntry, String field, Widget commentButton) {
    String comment = commentEntry.getText();
    boolean isCorrect = comment.length() == 0;
    if (isCorrect) {
      addCorrectComment(field);
    } else {
      postIncorrect(commentEntry, field);
    }
    setButtonTitle(commentButton, isCorrect, comment);
    showOrHideCommentButton(commentButton,isCorrect);
    exercise.addAnnotation(field, isCorrect ? "correct" : "incorrect", comment);
  }

  private void postIncorrect(TextBox commentEntry, String field) {
    final String commentToPost = commentEntry.getText();
    addIncorrectComment(commentToPost, field);
  }
}
