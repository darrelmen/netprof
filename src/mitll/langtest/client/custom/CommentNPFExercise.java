package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
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
  private Widget getEntry(String field, Widget content, ExerciseAnnotation annotation, boolean isAudio) {
    final Button commentButton = new Button();
    if (field.endsWith(".mp3")) field = field.replaceAll(".mp3",".wav");
    final MyTextBox commentEntryText = new MyTextBox();
    final MyPopup commentPopup = new MyPopup();
    commentPopup.setAutoHideEnabled(true);
    commentPopup.configure(commentEntryText, commentButton);
    commentPopup.setField(field);

    HorizontalPanel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(getOKButton(commentPopup));
    MyButton clearButton = getClearButton(commentEntryText, commentPopup);
    hp.add(clearButton);
    commentPopup.add(hp);

    configureCommentTextBox(annotation, commentEntryText, commentPopup);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();
    String comment = !alreadyMarkedCorrect ? annotation.comment : "";
/*    System.out.println("getEntry : field " + field + " annotation " + annotation +
      " correct " + alreadyMarkedCorrect + " comment '" + comment+
      "', fields " + exercise.getFields());*/

    getQCWidget(commentButton,
      alreadyMarkedCorrect, commentPopup,
      comment, commentEntryText);

    commentButton.addStyleName(isAudio ? "leftFiveMargin" : "leftTenMargin");

    // content on left side, comment button on right

    Panel row = new HorizontalPanel();
    row.add(content);
    row.add(commentButton);
    showOrHideCommentButton(commentButton, alreadyMarkedCorrect);
    if (isAudio) {
      audioInfo = new AudioInfo(field, commentEntryText, commentButton, commentPopup);
    }
    return row;
  }

  private Button getOKButton(final PopupPanel commentPopup) {
    Button ok = new Button("OK");
    ok.setType(ButtonType.PRIMARY);
    ok.addStyleName("leftTenMargin");
    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.hide();
      }
    });
    return ok;
  }

  private MyButton getClearButton(final MyTextBox commentEntryText,
                                  final PopupPanel commentPopup) {
    MyButton clear = new MyButton("Clear");
    clear.setType(ButtonType.INFO);
    clear.addStyleName("leftFiveMargin");
    clear.configure(commentEntryText, commentPopup);
    return clear;
  }

  private class MyButton extends Button {
    public MyButton(String title) {
      super(title);
    }

    public void configure( final MyTextBox commentEntryText,
                          final PopupPanel commentPopup) {
      addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          commentEntryText.setText("");
          commentPopup.hide();
        }
      });
    }
  }

  public class MyPopup extends DecoratedPopupPanel {
    private String field;

    public void setField(String field) {
      this.field = field;
    }

    public void configure(final TextBox commentBox, final Widget commentButton) {
      addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          System.out.println("onClose for " + field);
          commentComplete(commentBox, field, commentButton);
        }
      });
    }
  }

  @Override
  protected void setAudioRef(String audioRef) {
    String audioRef1 = audioInfo.audioRef;
    //System.out.println("set audio ref '" +audioRef +"' vs " + audioRef1);
    if (!audioRef.equals(audioRef1)) {

      ExerciseAnnotation annotation = exercise.getAnnotation(audioRef);
      boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.isCorrect();

      showOrHideCommentButton(audioInfo.commentButton, alreadyMarkedCorrect);
      String comment = annotation == null ? "" : annotation.comment;

/*      System.out.println("\t new audio ref " + audioRef +
        " comment '" + comment +"' correct " + alreadyMarkedCorrect + " anno " + annotation +
        " fields " + exercise.getFields());*/

      setButtonTitle(audioInfo.commentButton, alreadyMarkedCorrect, comment);
      audioInfo.commentEntryText.setText(comment);
      audioInfo.popup.setField(audioRef);
      audioInfo.audioRef = audioRef;
    }
  }

  private static class AudioInfo {
    String audioRef;
    MyTextBox commentEntryText;
    Button commentButton;
    MyPopup popup;

    public AudioInfo(String audioRef, MyTextBox commentEntryText, Button commentButton,
                     MyPopup popup) {
      this.audioRef = audioRef;
      this.commentEntryText = commentEntryText;
      this.commentButton = commentButton;
      this.popup = popup;
    }
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
   * @param annotation fill in with existing annotation, if there is one
   * @param commentEntry comment box to configure
   * @return
   */
  private FocusWidget configureCommentTextBox(ExerciseAnnotation annotation,
                                              final MyTextBox commentEntry,
                                              final PopupPanel popup) {
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.configure(popup);

    return commentEntry;
  }

  private class MyTextBox extends TextBox {
    public void configure( final PopupPanel popup) {

      addKeyPressHandler(new KeyPressHandler() {
        @Override
        public void onKeyPress(KeyPressEvent event) {
          int keyCode = event.getNativeEvent().getKeyCode();
          if (keyCode == KeyCodes.KEY_ENTER) {
            popup.hide();
          }
        }
      });
    }
  }

  private <T extends TextBox> void commentComplete(T commentEntry, String field, Widget commentButton) {
    String comment = commentEntry.getText();
    boolean isCorrect = comment.length() == 0;

    //System.out.println("commentComplete " + field + " comment '" + comment +"' correct = " +isCorrect);

    if (isCorrect) {
      addCorrectComment(field);
    } else {
      postIncorrect(commentEntry, field);
    }
    setButtonTitle(commentButton, isCorrect, comment);
    showOrHideCommentButton(commentButton,isCorrect);
    exercise.addAnnotation(field, isCorrect ? "correct" : "incorrect", comment);

   // System.out.println("\t annotations now " + exercise.getFields());
  }

  private void postIncorrect(TextBox commentEntry, String field) {
    final String commentToPost = commentEntry.getText();
    addIncorrectComment(commentToPost, field);
  }
}
