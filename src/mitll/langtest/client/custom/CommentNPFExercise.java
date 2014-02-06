package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
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
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise extends NPFExercise {
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

  private Widget getEntry(Exercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getEntry(field, getContentWidget(label, value, true), annotation);
  }

  @Override
  protected ASRScoringAudioPanel makeFastAndSlowAudio(final String path) {
    return new FastAndSlowASRScoringAudioPanel(exercise, path, service, controller, scorePanel) {
      @Override
      protected void addAudioRadioButton(Panel vp, RadioButton fast, String audioPath) {
        vp.add(getEntry(audioPath, fast, exercise.getAnnotation(audioPath)));
      }
    };
  }

  /**
   * @param field
   * @param annotation
   * @return
   */
  private Widget getEntry(String field, Widget content, ExerciseAnnotation annotation) {
    final Button commentButton = new Button();

    if (field.endsWith(AudioTag.COMPRESSED_TYPE)) field = field.replaceAll("." +
        AudioTag.COMPRESSED_TYPE,".wav");
    final MyTextBox commentEntryText = new MyTextBox();
    Button clearButton = getClearButton(commentEntryText, commentButton, field);

    final MyPopup commentPopup = makeCommentPopup(field, commentButton, commentEntryText, clearButton);
    commentPopup.addAutoHidePartner(commentButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    configureCommentTextBox(annotation, commentEntryText, commentPopup);

    boolean isCorrect = annotation == null || annotation.status == null || annotation.isCorrect();
    String comment = !isCorrect ? annotation.comment : "";
/*    System.out.println("getEntry : field " + field + " annotation " + annotation +
      " correct " + isCorrect + " comment '" + comment+
      "', fields " + exercise.getFields());*/

    configureCommentButton(commentButton,
      isCorrect, commentPopup,
      comment, commentEntryText);

    // content on left side, comment button on right

    Panel row = new HorizontalPanel();
    row.add(content);
    row.add(commentButton);
    row.add(clearButton);

    showOrHideCommentButton(commentButton, clearButton, isCorrect);
    return row;
  }

  /**
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   * @param field
   * @param commentButton
   * @param commentEntryText
   * @param clearButton
   * @return
   */
  private MyPopup makeCommentPopup(String field, Button commentButton, MyTextBox commentEntryText, Button clearButton) {
    final MyPopup commentPopup = new MyPopup();
    commentPopup.setAutoHideEnabled(true);
    commentPopup.configure(commentEntryText, commentButton, clearButton);
    commentPopup.setField(field);

    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(getOKButton(commentPopup));

    commentPopup.add(hp);
    return commentPopup;
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

  private Button getClearButton(final MyTextBox commentEntryText,
                                  final Widget commentButton, final String field) {
    final Button clear = new Button("");
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
        addCorrectComment(field);
      }
    });
    return clear;
  }

  public class MyPopup extends DecoratedPopupPanel {
    private String field;

    public void setField(String field) {
      this.field = field;
    }

    /**
     * @see mitll.langtest.client.custom.CommentNPFExercise#getEntry
     * @param commentBox
     * @param commentButton
     * @param clearButton
     */
    public void configure(final TextBox commentBox, final Widget commentButton, final Widget clearButton) {
      addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          commentComplete(commentBox, field, commentButton, clearButton);
        }
      });
    }
  }

  private void showOrHideCommentButton(UIObject commentButton, UIObject clearButton, boolean isCorrect) {
    if (isCorrect) {
      showQC(commentButton);
    }
    else {
      showQCHasComment(commentButton);
    }
    clearButton.setVisible(!isCorrect);
  }

  /**
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   * @param commentButton
   * @param alreadyMarkedCorrect
   * @param commentPopup
   * @param comment
   * @param commentEntry
   * @return
   */
  private void configureCommentButton(final Button commentButton,
                                      final boolean alreadyMarkedCorrect, final PopupPanel commentPopup, String comment,
                                      final FocusWidget commentEntry) {
    commentButton.setIcon(IconType.COMMENT);
    commentButton.setSize(ButtonSize.MINI);
    commentButton.addStyleName("leftTenMargin");

    final Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    commentButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean visible = commentPopup.isShowing();

        if (visible) {// fix for bug that Wade found -- if we click off of popup, it dismisses it,
                      // but if that click is on the button, it would immediately shows it again
          //System.out.println("popup visible " + visible);
          commentPopup.hide();
        } else {
          commentPopup.showRelativeTo(commentButton);
          commentEntry.setFocus(true);
          tooltip.hide();
        }
      }
    });

    showQC(commentButton);
  }

  private Tooltip setButtonTitle(Widget button, boolean isCorrect, String comment) {
    String tip = isCorrect ? "Add a comment" : "\""+ comment + "\"";
    return addTooltip(button, tip);
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

  private Map<String,String> fieldToComment = new HashMap<String,String>();

  /**
   * @see mitll.langtest.client.custom.CommentNPFExercise.MyPopup#configure(com.github.gwtbootstrap.client.ui.TextBox, com.google.gwt.user.client.ui.Widget, com.google.gwt.user.client.ui.Widget)
   * @param commentEntry
   * @param field
   * @param commentButton
   * @param clearButton
   */
  private <T extends TextBox> void commentComplete(T commentEntry, String field, Widget commentButton, Widget clearButton) {
    String comment = commentEntry.getText();
    String previous = fieldToComment.get(field);
    if (previous == null || !previous.equals(comment)) {
      fieldToComment.put(field, comment);
      boolean isCorrect = comment.length() == 0;

      System.out.println("commentComplete " + field + " comment '" + comment +"' correct = " +isCorrect);

      if (isCorrect) {
        addCorrectComment(field);
      } else {
        postIncorrect(commentEntry, field);
      }
      setButtonTitle(commentButton, isCorrect, comment);
      showOrHideCommentButton(commentButton, clearButton, isCorrect);
      exercise.addAnnotation(field, isCorrect ? "correct" : "incorrect", comment);
      System.out.println("\t commentComplete : annotations now " + exercise.getFields());
    }
    else {
      System.out.println("commentComplete " + field + " comment '" + comment +"' same as previous, so ignoring");

    }
  }

  private void postIncorrect(TextBox commentEntry, String field) {
    final String commentToPost = commentEntry.getText();
    addIncorrectComment(commentToPost, field);
  }
}
