package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise extends NPFExercise {
  private static final String NO_REFERENCE_AUDIO = "No reference audio";
  private static final String M = "M";
  private static final String F = "F";

  private AudioAttribute defaultAudio, maleAudio, femaleAudio;
  private PlayAudioPanel contextPlay;

  public CommentNPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer,
                            float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  /**
   * @param e
   * @param content
   * @return
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   */
  @Override
  protected Widget getQuestionContent(CommonExercise e, String content) {
    Panel column = new FlowPanel();
    column.getElement().setId("QuestionContent");
    column.setWidth("100%");

    column.add(getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));

    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, translitSentence));
    }

    String english = e.getMeaning() != null && !e.getMeaning().trim().isEmpty() ? e.getMeaning() : e.getEnglish();
    if (!english.isEmpty() && !english.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, english));
    }

    String context = e.getContext() != null && !e.getContext().trim().isEmpty() ? e.getContext() : "";
    if (!context.isEmpty()) {
      Widget entry = getEntry(e, QCNPFExercise.CONTEXT, ExerciseFormatter.CONTEXT, context);
      Panel hp = new HorizontalPanel();
      addGenderChoices(e, hp);

      hp.add(entry);
      hp.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      column.add(hp);
    }

    return column;
  }

  private void addGenderChoices(CommonExercise e, Panel hp) {
    String path = null;

    long maleTime = 0, femaleTime = 0;
    for (AudioAttribute audioAttribute : e.getAudioAttributes()) {
      if (audioAttribute.getAudioType().startsWith("context")) {
        if (audioAttribute.getUser().isDefault()) {
          defaultAudio = audioAttribute;
        } else if (audioAttribute.getUser().isMale()) {
          if (audioAttribute.getTimestamp() > maleTime) {
            maleAudio = audioAttribute;
            maleTime = audioAttribute.getTimestamp();
          }
        } else if (audioAttribute.getTimestamp() > femaleTime) {
          femaleAudio = audioAttribute;
          femaleTime = audioAttribute.getTimestamp();
        }
      }
    }

    AudioAttribute toUse = maleAudio != null ? maleAudio : femaleAudio != null ? femaleAudio : defaultAudio;
    path = toUse == null ? null : toUse.getAudioRef();
    if (path != null) {
      //System.out.println("adding context play option " + path);
      contextPlay = new PlayAudioPanel(controller, path)
          .setPlayLabel("")
          .setPauseLabel("")
          .setMinWidth(12);
      contextPlay.getPlayButton().getElement().getStyle().setMarginTop(-6, Style.Unit.PX);

      hp.add(contextPlay);

      List<String> choices = new ArrayList<String>();
      if (maleAudio != null) choices.add(M);
      if (femaleAudio != null) choices.add(F);
      if (defaultAudio != null) choices.add("Default"); //better not happen

      hp.add(getShowGroup(choices));
    }
  }

  private DivWidget getShowGroup(List<String> choices) {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    boolean first =true;
    for (final String choice : choices) {
      Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (choice.equals(M)) {
            //System.out.println("playing male audio! " + maleAudio.getAudioRef());
            contextPlay.playAudio(maleAudio.getAudioRef());
          } else if (choice.equals(F)) {
            //System.out.println("playing female audio! " + femaleAudio.getAudioRef());
            contextPlay.playAudio(femaleAudio.getAudioRef());
          } else {
            contextPlay.playAudio(defaultAudio.getAudioRef());
          }
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = false;
    }

    Style style = w.getElement().getStyle();
    style.setMarginTop(-6, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    return w;
  }

  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title.equals(M) ? "" : title.equals(F) ? "" : title);
    onButton.getElement().setId("Choice_" + title);
    controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    return onButton;
  }

  private Widget getEntry(CommonExercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getEntry(field, getContentWidget(label, value, true, false), annotation);
  }

  /**
   * @see #getAudioPanel
   * @param path
   * @return
   */
  @Override
  protected ASRScoringAudioPanel makeFastAndSlowAudio(final String path) {
    return new FastAndSlowASRScoringAudioPanel(exercise, path, service, controller, scorePanel) {
      @Override
      protected void addAudioRadioButton(Panel vp, RadioButton fast, String audioPath) {
        vp.add(getEntry(audioPath, fast, exercise.getAnnotation(audioPath)));
      }

      @Override
      protected void addNoRefAudioWidget(Panel vp) {
        Widget entry = getEntry("refAudio", "ReferenceAudio", CommentNPFExercise.NO_REFERENCE_AUDIO, exercise.getAnnotation("refAudio"));
        entry.setWidth("500px");
        vp.add(entry);
      }
    };
  }

  /**
   * @param field of the exercise to comment on
   * @param content to wrap
   * @param annotation to get current comment from
   * @return three part widget -- content, comment button, and clear button
   */
  private Widget getEntry(String field, Widget content, ExerciseAnnotation annotation) {
    final Button commentButton = new Button();

    if (field.endsWith(AudioTag.COMPRESSED_TYPE)) {
      field = field.replaceAll("." + AudioTag.COMPRESSED_TYPE,".wav");
    }

    final HidePopupTextBox commentEntryText = new HidePopupTextBox();
    commentEntryText.getElement().setId("CommentEntryTextBox_for_"+field);
    commentEntryText.setVisibleLength(60);

    Button clearButton = getClearButton(commentEntryText, commentButton, field);
    final MyPopup commentPopup = makeCommentPopup(field, commentButton, commentEntryText, clearButton);
    commentPopup.addAutoHidePartner(commentButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    configureTextBox(annotation != null ? annotation.comment : null, commentEntryText, commentPopup);

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
  private MyPopup makeCommentPopup(String field, Button commentButton, HidePopupTextBox commentEntryText, Button clearButton) {
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
   * @param commentEntryText
   * @param commentButton
   * @param field
   * @return
   */
  private Button getClearButton(final HidePopupTextBox commentEntryText,
                                  final Widget commentButton, final String field) {
    final Button clear = new Button("");
    clear.getElement().setId("CommentNPFExercise_"+field);
    controller.register(clear, exercise.getID(), "clear comment");
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
     * @see mitll.langtest.client.custom.CommentNPFExercise#makeCommentPopup(String, com.github.gwtbootstrap.client.ui.Button, HidePopupTextBox, com.github.gwtbootstrap.client.ui.Button)
     * @param commentBox
     * @param commentButton
     * @param clearButton
     */
    public void configure(final TextBox commentBox, final Widget commentButton, final Widget clearButton) {
      addCloseHandler(new CloseHandler<PopupPanel>() {
        @Override
        public void onClose(CloseEvent<PopupPanel> event) {
          controller.logEvent(commentBox,"Comment_TextBox",exercise.getID(),"submit comment '" +commentBox.getValue()+
            "'");
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
                                      final TextBox commentEntry) {
    commentButton.setIcon(IconType.COMMENT);
    commentButton.setSize(ButtonSize.MINI);
    commentButton.addStyleName("leftTenMargin");
    commentButton.getElement().setId("CommentNPFExercise_comment");
    controller.register(commentButton, exercise.getID(), "show comment");

    final Tooltip tooltip = setButtonTitle(commentButton, alreadyMarkedCorrect, comment);
    configurePopupButton(commentButton, commentPopup, commentEntry, tooltip);

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

  private final Map<String,String> fieldToComment = new HashMap<String,String>();

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
//      System.out.println("commentComplete " + field + " comment '" + comment +"' correct = " +isCorrect);

      setButtonTitle(commentButton, isCorrect, comment);
      showOrHideCommentButton(commentButton, clearButton, isCorrect);
      if (isCorrect) {
        addCorrectComment(field);
      }
      else {
        addIncorrectComment(comment,field);
      }
      //System.out.println("\t commentComplete : annotations now " + exercise.getFields());
    }
  }
}
