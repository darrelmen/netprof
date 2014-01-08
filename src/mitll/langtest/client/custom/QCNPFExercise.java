package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
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

import java.util.*;

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
  protected void addQuestionContentRow(Exercise e, ExerciseController controller, Panel hp) {
    super.addQuestionContentRow(e, controller, hp);
    hp.addStyleName("questionContentPadding");
  }

  @Override
  protected void nextWasPressed(ListInterface listContainer, Exercise completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    if (!instance.equals("review")) {
      listContainer.addCompleted(completedExercise.getID());
      markReviewed(completedExercise);
    }
  }

  private void markReviewed(Exercise completedExercise) {
    service.markReviewed(completedExercise.getID(), incorrectSet.isEmpty(), controller.getUser(),
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
        }
      });
  }

  /**
   * No user recorder for QC
   * @param service
   * @param controller    used in subclasses for audio control
   * @param toAddTo
   * @param screenPortion
   */
  @Override
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                 float screenPortion) {}

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
      ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller,
        false, scorePanel, 93);
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
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getEntry(field, getContentWidget(label, value, true), annotation);
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
    qcCol.addStyleName("qcRightBorder");

    qcCol.add(checkBox);
    return qcCol;
  }

  /**
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   * @param commentEntry
   * @param alreadyMarkedCorrect
   * @param commentRow
   */
  private void populateCommentRow(FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    commentRow.setVisible(!alreadyMarkedCorrect);

    final Label commentLabel = getCommentLabel();

    commentRow.add(commentLabel);
    commentRow.add(commentEntry);
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
          addCorrectComment(field);
        }
        commentRow.setVisible(isIncorrect);
        commentEntry.setFocus(isIncorrect);
        if (isIncorrect) incorrectSet.add(field);
      }
    });
    checkBox.setValue(!alreadyMarkedCorrect);
    return checkBox;
  }
}
