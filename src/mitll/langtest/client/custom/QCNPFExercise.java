package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
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
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
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
public class QCNPFExercise extends GoodwaveExercisePanel {
  private static final String CHECKBOX_LABEL = "         ";//"Has defect";
  private String instance;
  private Set<String> incorrectSet = new HashSet<String>();
  private List<RequiresResize> toResize;
  public QCNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
    this.instance = instance;
  }

  @Override
  protected ASRScorePanel makeScorePanel(Exercise e, String instance) { return null;  }
  @Override
  protected void nextWasPressed(ListInterface listContainer, Exercise completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    if (!instance.equals("review")) {
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
    column.getElement().setId("QuestionContent");
    column.addStyleName("floatLeft");
    column.setWidth("100%");

    Heading heading = new Heading(4, "Defect?");
    heading.addStyleName("borderBottomQC");
    Panel row = new FlowPanel();
    row.add(heading);

    column.add(row);
    column.add(getEntry(e, "foreignLanguage", ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));
    column.add(getEntry(e, "transliteration", ExerciseFormatter.TRANSLITERATION, e.getTranslitSentence()));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence()));

    return column;
  }

  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }

  protected Widget getScoringAudioPanel(final Exercise e, String pathxxxx) {
    Panel column = new FlowPanel();
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

      column.add(getEntry(audio.getAudioRef(), cp, audioAnnotation)); // TODO add unique audio attribute id
    }
    return column;
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
   *
   * @param field
   * @param annotation
   * @return
   * @paramx label
   * @paramx value
   */
  private Widget getEntry(final String field, Widget content, ExerciseAnnotation annotation) {
    final FocusWidget commentEntry = makeCommentEntry(field, annotation);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    final Panel commentRow = new FlowPanel();
    final Panel qcCol = getQCCheckBox(field, commentEntry, alreadyMarkedCorrect, commentRow);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right

    Panel row;
    //if (count == 0) rowContainer.addStyleName("borderTopQC");
/*    if (count++ % 2 == 0) {
      row.addStyleName("greenBackground");
    }*/

      row = new FlowPanel();
      row.addStyleName("trueInlineStyle");
      qcCol.addStyleName("floatLeft");
      row.add(qcCol);
      row.add(content);


    Panel rowContainer = new FlowPanel();
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(row);
    rowContainer.add(commentRow);

    return rowContainer;
  }

  private Panel getQCCheckBox(String field, FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    final CheckBox checkBox = makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);

    Panel qcCol = new FlowPanel();
    Button child = new Button();
    child.setIcon(IconType.COMMENT);
    child.setSize(ButtonSize.MINI);
    qcCol.add(child);
   qcCol.addStyleName("qcRightBorder");

    qcCol.add(checkBox);
    return qcCol;
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
        final String comment = commentEntry.getText();
        addIncorrectComment(comment, field);
      }
    });
    return commentEntry;
  }

  /**
   * @see #getQCCheckBox
   * @param field
   * @param commentRow
   * @param commentEntry
   * @param alreadyMarkedCorrect
   * @return
   */
  private CheckBox makeCheckBox(final String field, final Panel commentRow, final FocusWidget commentEntry, boolean alreadyMarkedCorrect) {
    final CheckBox checkBox = new CheckBox(CHECKBOX_LABEL);
    checkBox.addStyleName("centeredRadio");
    checkBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Boolean isIncorrect = checkBox.getValue();
        if (!isIncorrect) {
          incorrectSet.remove(field);
          addCorrectAnnotation(field);
        }
        commentRow.setVisible(isIncorrect);
        commentEntry.setFocus(isIncorrect);
        if (isIncorrect) incorrectSet.add(field);
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

  private void addIncorrectComment(final String comment, final String field) {
    System.out.println(new Date() +" : post to server " + exercise.getID() +
      " field " + field + " commentLabel " + comment + " is incorrect");
    final long then = System.currentTimeMillis();
    service.addAnnotation(exercise.getID(), field, "incorrect", comment, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {
        long now = System.currentTimeMillis();
        System.out.println("\t" + new Date() +" : posted to server " + exercise.getID() +
          " field " + field + " commentLabel " + comment + " is incorrect, took " + (now-then) + " millis");

      }
    });
  }

  private void addCorrectAnnotation(String field) {
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
}
