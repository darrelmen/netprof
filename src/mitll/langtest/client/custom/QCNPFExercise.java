package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.AudioAttribute;
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
public class QCNPFExercise extends NPFExercise {
  String instance;
  Set<String> incorrectSet = new HashSet<String>();

  public QCNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
    this.instance = instance;
    System.out.println("QCNPFExercise :  instance " +instance);
  }

  @Override
  protected void nextWasPressed(ListInterface listContainer, Exercise completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    if (!instance.equals("review")) {
     // System.out.println("\n\n\n\n\tnextWasPressed : add completed " + completedExercise.getID());

      listContainer.addCompleted(completedExercise.getID());
      service.markReviewed(completedExercise.getID(), incorrectSet.isEmpty(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
        }
      });
    }
  }

  /**
   * No user recorder for QC
   * @param service
   * @param controller    used in subclasses for audio control
   * @param toAddTo
   * @param screenPortion
   */
  @Override
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo, float screenPortion) {}

  /**
   * @param e
   * @param content
   * @return
   */
  @Override
  protected Widget getQuestionContent(Exercise e, String content) {
    FlowPanel column = new FlowPanel();
    column.addStyleName("blockStyle");

    ExerciseAnnotation foreignLanguage = e.getAnnotation("foreignLanguage");
    ExerciseAnnotation english = e.getAnnotation("english");
    ExerciseAnnotation translit = e.getAnnotation("translit");
    column.add(getEntry(e, "foreignLanguage", ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence(), foreignLanguage));
    column.add(getEntry(e, "transliteration", ExerciseFormatter.TRANSLITERATION, e.getTranslitSentence(), translit));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence(), english));

    column.getElement().setId("QuestionContent");
    column.addStyleName("floatLeft");
    return column;
  }

  protected Widget getScoringAudioPanel(final Exercise e, String pathxxxx) {
    FlowPanel column = new FlowPanel();
    column.addStyleName("blockStyle");

    for (AudioAttribute audio : e.getAudioAttributes()) {
      String audioRef = audio.getAudioRef();
      if (audioRef != null) {
        audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
      }
      ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller, false, false, scorePanel);
      audioPanel.setShowColor(true);
      audioPanel.getElement().setId("ASRScoringAudioPanel");
      audioPanel.setRefAudio(audioRef, e.getRefSentence());
      ResizableCaptionPanel cp = new ResizableCaptionPanel("Reference" + " : " + audio.getDisplay());
      cp.setContentWidget(audioPanel);
      ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

      column.add(getEntry(e.getID(), audio.getAudioRef(), cp, audioAnnotation)); // TODO add unique audio attribute id
    }
    return column;
  }

  private Widget getEntry(Exercise e, final String field, final String label, String value, ExerciseAnnotation annotation) {
    Panel nameValueRow = getContentWidget(label, value);
    return getEntry(e.getID(), field, nameValueRow, annotation);
  }

  /**
   * TODOx add annotation info to exercise
   * TODOx mark this on the radio
   * TODOx fill in incorrect fields with annotation info (if in qc or in edit mode?)
   * TODOx post annotation to server
   * TODO after edit, clear annotation -- where do we edit? in edit window
   *
   *
   * @param id
   * @param field
   * @param annotation
   * @return
   * @paramx label
   * @paramx value
   */
  private Widget getEntry(String id,
                          final String field, Widget content, ExerciseAnnotation annotation) {
    Panel row = new HorizontalPanel();
    FlowPanel qcCol = new FlowPanel();
    qcCol.addStyleName("blockStyle");

    //System.out.println("For  " + id + " and " + field + " anno " + annotation);

    String group = "QC_" + id + "_" + field;
    RadioButton correct = new RadioButton(group, "Correct");
    qcCol.add(correct);
    final FlowPanel commentRow = new FlowPanel();
    final Label comment = new Label("comment?");
    final TextBox commentEntry = new TextBox();
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    correct.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentRow.setVisible(false);
        // send to server
        incorrectSet.remove(field);
        //System.out.println("post to server " + exercise.getID() + " field " + field + " is correct");
        service.addAnnotation(exercise.getID(), field, "correct", "", new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
      }
    });
    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    correct.setValue(alreadyMarkedCorrect);
    commentRow.setVisible(!alreadyMarkedCorrect);

    RadioButton incorrect = new RadioButton(group, "Incorrect");
    qcCol.add(incorrect);

    incorrect.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentRow.setVisible(true);
        commentEntry.setFocus(true);
        incorrectSet.add(field);

      }
    });
    incorrect.setValue(!alreadyMarkedCorrect);

    row.add(qcCol);
    qcCol.addStyleName("qcRightBorder");
    qcCol.addStyleName("rightFiveMargin");

    FlowPanel nameValueComment = new FlowPanel();
    nameValueComment.addStyleName("blockStyle");

    //Panel nameValueRow = getContentWidget(label, value);

    nameValueComment.add(content);

    DOM.setStyleAttribute(comment.getElement(), "backgroundColor", "#ff0000");
    comment.setVisible(true);
    comment.addStyleName("ImageOverlay");

    commentRow.addStyleName("trueInlineStyle");
    commentRow.add(comment);

    commentEntry.addStyleName("leftFiveMargin");
    commentRow.add(commentEntry);

    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        //System.out.println("post to server " + exercise.getID() + " field " + field + " comment " + commentEntry.getText() + " is incorrect");

        service.addAnnotation(exercise.getID(), field, "incorrect", commentEntry.getText(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
      }
    });
    nameValueComment.add(commentRow);
    row.add(nameValueComment);
    return row;
  }

  private Panel getContentWidget(String label, String value) {
    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    InlineHTML foreignPhrase = new InlineHTML(label);
    foreignPhrase.addStyleName("Instruction-title");
    nameValueRow.add(foreignPhrase);

    InlineHTML englishPhrase = new InlineHTML(value);
    englishPhrase.addStyleName("Instruction-data");
    nameValueRow.add(englishPhrase);
    englishPhrase.addStyleName("leftFiveMargin");
    return nameValueRow;
  }
}
