package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.ArrayList;
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
public class QCNPFExercise extends NPFExercise {
  private String instance;
  private Set<String> incorrectSet = new HashSet<String>();
  int count = 0;
  List<RequiresResize> toResize;

  public QCNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
    this.instance = instance;
    System.out.println("QCNPFExercise :  instance " +instance);
  }

  @Override
  protected ASRScorePanel makeScorePanel(Exercise e, String instance) { return null;  }
  @Override
  protected Panel makeAddToList(Exercise e, ExerciseController controller) { return null; }
  @Override
  public void wasRevealed() {}

  @Override
  protected void nextWasPressed(ListInterface listContainer, Exercise completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    if (!instance.equals("review")) {
     // System.out.println("\n\n\n\n\tnextWasPressed : add completed " + completedExercise.getID());

      listContainer.addCompleted(completedExercise.getID());
      service.markReviewed(completedExercise.getID(), incorrectSet.isEmpty(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {}
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
    Panel column = new FlowPanel();
    Heading heading = new Heading(4, "Defect?");
    FluidRow row = new FluidRow();
    column.add(row);
    row.add(heading);


    Widget foreignLanguage = getEntry(e, "foreignLanguage", ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence());

    column.add(foreignLanguage);

    column.add(getEntry(e, "transliteration", ExerciseFormatter.TRANSLITERATION, e.getTranslitSentence()));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence()));

    column.getElement().setId("QuestionContent");
    column.addStyleName("floatLeft");
    column.setWidth("100%");
    return column;
  }

  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }

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
      toResize.add(cp);
      ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

      column.add(getEntry(e.getID(), audio.getAudioRef(), cp, audioAnnotation)); // TODO add unique audio attribute id
    }
    return column;
  }

  private Widget getEntry(Exercise e, final String field, final String label, String value) {
    ExerciseAnnotation currentAnnotation = e.getAnnotation(field);
    return getEntry(e, field, label, value, currentAnnotation);
  }

  private Widget getEntry(Exercise e, final String field, final String label, String value, ExerciseAnnotation annotation) {
    Panel nameValueRow = getContentWidget(label, value);
    return getEntry(e.getID(), field, nameValueRow, annotation);
  }

  /**
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
    Panel rowContainer = new FlowPanel();
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");

    Panel row = new FlowPanel();
    row.addStyleName("trueInlineStyle");
    rowContainer.add(row);
    FlowPanel qcCol = new FlowPanel();
    qcCol.addStyleName("floatLeft");

     qcCol.addStyleName("qcRightBorder");
    if (count == 0) rowContainer.addStyleName("borderTopQC");
    if (count++ % 2 == 0) {
      row.addStyleName("greenBackground");
    }
    final Panel commentRow = new FlowPanel();

    final FocusWidget commentEntry = makeCommentEntry(field, annotation);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");

    final Label commentLabel = new Label("comment?");
    final CheckBox checkBox = makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);
    qcCol.add(checkBox);
    row.add(qcCol);
    commentRow.setVisible(!alreadyMarkedCorrect);
    rowContainer.add(commentRow);

    DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
    commentLabel.addStyleName("ImageOverlay");

    commentRow.add(commentLabel);
    commentRow.add(commentEntry);

    row.add(content);
    return rowContainer;
  }

  private FocusWidget makeCommentEntry(final String field,ExerciseAnnotation annotation) {
    final TextBox commentEntry = new TextBox();
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        System.out.println(new Date() +" : post to server " + exercise.getID() +
          " field " + field + " commentLabel " + commentEntry.getText() + " is incorrect");
        final long then = System.currentTimeMillis();
        service.addAnnotation(exercise.getID(), field, "incorrect", commentEntry.getText(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void result) {
            long now = System.currentTimeMillis();
            System.out.println("\t" + new Date() +" : posted to server " + exercise.getID() +
              " field " + field + " commentLabel " + commentEntry.getText() + " is incorrect, took " + (now-then) + " millis");

          }
        });
      }
    });
    return commentEntry;
  }

  private CheckBox makeCheckBox(final String field, final Panel commentRow, final FocusWidget commentEntry, boolean alreadyMarkedCorrect) {
    final CheckBox checkBox = new CheckBox("Has defect");
    checkBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!checkBox.getValue()) {
          incorrectSet.remove(field);
          System.out.println(new Date() +" : post to server " + exercise.getID() + " field " + field + " is correct");
          service.addAnnotation(exercise.getID(), field, "correct", "", new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
            }
          });
        }
         commentRow.setVisible(checkBox.getValue());
        commentEntry.setFocus(checkBox.getValue());
        incorrectSet.add(field);

      }
    });
    checkBox.setValue(!alreadyMarkedCorrect);
    return checkBox;
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
