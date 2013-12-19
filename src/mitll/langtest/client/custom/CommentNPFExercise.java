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
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise extends NPFExercise {
 // private Set<String> incorrectSet = new HashSet<String>();
  // private int count = 0;
 // private List<RequiresResize> toResize;

  public CommentNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                            float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
    // this.instance = instance;
    //System.out.println("QCNPFExercise :  instance " +instance);
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

/*
  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }
*/

/*
  protected Widget getScoringAudioPanel(final Exercise e, String pathxxxx) {
    FlowPanel column = new FlowPanel();
    column.addStyleName("blockStyle");
    if (toResize == null) toResize = new ArrayList<RequiresResize>();

    for (AudioAttribute audio : e.getAudioAttributes()) {
      String audioRef = audio.getAudioRef();
      if (audioRef != null) {
        audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
      }
      ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller, false, false, scorePanel);
      audioPanel.setShowColor(true);
      audioPanel.getElement().setId("ASRScoringAudioPanel");
      audioPanel.setRefAudio(audioRef, e.getRefSentence());
      String name = "Reference" + " : " + audio.getDisplay();
      if (audio.isFast()) name = "Regular speed audio example";
      else if (audio.isSlow()) name = "Slow speed audio example";
      ResizableCaptionPanel cp = new ResizableCaptionPanel(name);
      cp.setContentWidget(audioPanel);

      column.add(entry); // TODO add unique audio attribute id
    }
    return column;
  }
*/


  protected Widget getAudioPanelContent(ASRScoringAudioPanel audioPanel, Exercise exercise, String audioRef) {
    ExerciseAnnotation audioAnnotation = exercise.getAnnotation(audioRef);

    Widget entry = getEntry(audioRef, audioPanel, audioAnnotation);
    return entry;
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
    final FocusWidget commentEntry = makeCommentEntry(field, annotation,commentRow);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    final Panel qcCol = getQCWidget(field, commentEntry, alreadyMarkedCorrect, commentRow);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right

    Panel row = new HorizontalPanel();
    row.setWidth("100%");
    qcCol.addStyleName("floatRight");
    // FocusPanel focusWidget = new FocusPanel(content);

    row.add(content);
    //content.addStyleName("floatLeft");

    row.add(qcCol);

    FocusPanel focusWidget = new FocusPanel(row);
    focusWidget.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
       // System.out.println("mouse over");
        if (!clickedFields.contains(field)) {
          qcCol.setVisible(true);
        }
      }
    });
    focusWidget.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
     //   System.out.println("mouse out");
        if (!clickedFields.contains(field)) {
          qcCol.setVisible(false);
        }
      }
    });

    Panel rowContainer = new FlowPanel();
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(focusWidget);
    rowContainer.add(commentRow);

    return rowContainer;
  }

  Set<String> clickedFields = new HashSet<String>();

  private Panel getQCWidget(final String field, final FocusWidget commentEntry,
                            final boolean alreadyMarkedCorrect, final Panel commentRow) {
    // final CheckBox checkBox = makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);

    //Panel qcCol = new FlowPanel();
    final Button child = new Button();
    child.setIcon(IconType.COMMENT);
    child.setSize(ButtonSize.MINI);
    child.addStyleName("rightFiveMargin");
    child.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        clickedFields.add(field);
        child.setVisible(true);
        child.removeStyleName("comment-button-group-new");
        reactToClick(field, commentRow, commentEntry);
      }
    });
    //qcCol.add(child);
/*
    child.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          System.out.println("mouse over 2");

          child.setVisible(true);
        }
      });
    child.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          System.out.println("mouse out 2");
          child.setVisible(false);
        }
      });*/
    if (alreadyMarkedCorrect) {
      child.setVisible(false);
      child.addStyleName("comment-button-group-new");
    } else {

    }
    //  qcCol.add(checkBox);
    return child;
  }

  private void populateCommentRow(FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    commentRow.setVisible(!alreadyMarkedCorrect);

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

  private FocusWidget makeCommentEntry(final String field, ExerciseAnnotation annotation, final Panel commentRow) {
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
      }
    });
    commentEntry.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        char keyPress = event.getCharCode();
        int key = (int) keyPress;
        if (keyPress == KeyCodes.KEY_ENTER || key == 0) {
          System.out.println("\tGot enter key " + keyPress);
          postIncorrect(commentEntry, field);
          commentRow.setVisible(false);
          commentEntry.setFocus(false);
        }
        else {
          System.out.println("\tdid not get enter key '" + keyPress + "' (" +key+
            ") vs " + KeyCodes.KEY_ENTER  + " event " + event );

        }
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
    service.addAnnotation(exercise.getID(), field, "incorrect", commentToPost, new AsyncCallback<Void>() {
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

  /**
   * @param field
   * @param commentRow
   * @param commentEntry
   * @paramx alreadyMarkedCorrect
   * @return
   * @see #getQCWidget
   */
/*  private CheckBox makeCheckBox(final String field, final com.google.gwt.user.client.ui.Panel commentRow,
                                final FocusWidget commentEntry, boolean alreadyMarkedCorrect) {
    final CheckBox checkBox = new CheckBox(CHECKBOX_LABEL);
    checkBox.addStyleName("centeredRadio");
    checkBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Boolean isIncorrect = checkBox.getValue();
        reactToClick(isIncorrect, field, commentRow, commentEntry);
      }
    });
    checkBox.setValue(!alreadyMarkedCorrect);
    return checkBox;
  }*/
  private void reactToClick(String field, Panel commentRow, FocusWidget commentEntry) {
 /*   if (!incorrect) {
      incorrectSet.remove(field);
      addCorrectAnnotation(field);
    }*/
    boolean visible = commentRow.isVisible();
    commentRow.setVisible(!visible);
    commentEntry.setFocus(!visible);
    //incorrectSet.add(field);
  }

/*  private void addCorrectAnnotation(String field) {
    System.out.println(new Date() + " : post to server " + exercise.getID() + " field " + field + " is correct");
    service.addAnnotation(exercise.getID(), field, "correct", "", new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }*/

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
