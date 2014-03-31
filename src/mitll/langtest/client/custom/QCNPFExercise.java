package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
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
  private static final String DEFECT = "Defect?";

  public static final String FOREIGN_LANGUAGE = "foreignLanguage";
  public static final String TRANSLITERATION = "transliteration";
  public static final String ENGLISH = "english";

  private static final String REF_AUDIO = "refAudio";
  private static final String APPROVED = "Approve Item";
  private static final String NO_AUDIO_RECORDED = "No Audio Recorded.";
  private static final String SLOW_SPEED_AUDIO_EXAMPLE = "Slow speed audio example";
  private static final String REGULAR_SPEED_AUDIO_EXAMPLE = "Regular speed audio example";
  private static final String REFERENCE = "Reference";
  private static final String COMMENT = "Comment";

  private static final String COMMENT_TOOLTIP = "Comments are optional.";
  private static final String CHECKBOX_TOOLTIP = "Check to indicate this field has a defect.";
  private static final String APPROVED_BUTTON_TOOLTIP = "Indicate item has no defects.";
  private static final String APPROVED_BUTTON_TOOLTIP2 = "Item has been marked with a defect";

  private final Set<String> incorrectSet = new HashSet<String>();
  private List<RequiresResize> toResize;
  private final ListInterface listContainer;
  private Button approvedButton;
  private Tooltip approvedTooltip;

  public QCNPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);

    this.listContainer = listContainer;
  }

  @Override
  protected ASRScorePanel makeScorePanel(CommonExercise e, String instance) { return null;  }

  @Override
  protected void addGroupingStyle(Widget div) {
    div.addStyleName("buttonGroupInset7");
  }

  @Override
  protected void addQuestionContentRow(CommonExercise e, ExerciseController controller, Panel hp) {
    super.addQuestionContentRow(e, controller, hp);
    hp.addStyleName("questionContentPadding");
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, float, boolean, String)
   * @param controller
   * @param listContainer
   * @param addKeyHandler
   * @return
   */
  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                           final ListInterface listContainer, boolean addKeyHandler) {
    NavigationHelper widgets = new NavigationHelper(exercise, controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    }, listContainer, true, addKeyHandler) {
      @Override
      protected void enableNext(CommonExercise exercise) {}
    };

    if (!instance.contains(Navigation.REVIEW) && !instance.contains(Navigation.COMMENT)) {
      approvedButton = addApprovedButton(listContainer, widgets);
    }

    return widgets;
  }

  private Button addApprovedButton(final ListInterface listContainer, NavigationHelper widgets) {
    Button approved = new Button(APPROVED);
    approved.addStyleName("leftFiveMargin");
    widgets.add(approved);
    approved.setType(ButtonType.PRIMARY);
    approved.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markReviewed(listContainer, exercise);
      }
    });
    approvedTooltip = addTooltip(approved, APPROVED_BUTTON_TOOLTIP);
    return approved;
  }

  @Override
  protected void nextWasPressed(ListInterface listContainer, CommonShell completedExercise) {
    //System.out.println("nextWasPressed : load next exercise " + completedExercise.getID() + " instance " +instance);
    super.nextWasPressed(listContainer, completedExercise);
    markReviewed(listContainer, completedExercise);
  }

  /**
   * @see #addApprovedButton(mitll.langtest.client.list.ListInterface, mitll.langtest.client.exercise.NavigationHelper)
   * @see #nextWasPressed(mitll.langtest.client.list.ListInterface, mitll.langtest.shared.CommonShell)
   * @param listContainer
   * @param completedExercise
   */
  private void markReviewed(ListInterface listContainer, CommonShell completedExercise) {
    if (isCourseContent()) {
      listContainer.addCompleted(completedExercise.getID());
      markReviewed(completedExercise);
    }
  }

  private boolean isCourseContent() {
    return !instance.equals(Navigation.REVIEW) && !instance.equals(Navigation.COMMENT);
  }

  /**
   * @see #markReviewed(mitll.langtest.client.list.ListInterface, mitll.langtest.shared.CommonShell)
   * @param completedExercise
   */
  private void markReviewed(CommonShell completedExercise) {
    System.out.println("markReviewed : exercise " + completedExercise.getID() + " instance " + instance);

    service.markReviewed(completedExercise.getID(), incorrectSet.isEmpty(), controller.getUser(),
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void result) {}
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
  protected Widget getQuestionContent(CommonExercise e, String content) {
    Panel column = new FlowPanel();
    column.getElement().setId("QCNPFExercise_QuestionContent");
    column.addStyleName("floatLeft");
    column.setWidth("100%");

    Panel row = new FlowPanel();
    row.add(getComment());

    column.add(row);

    if (e.getModifiedDate() != null && e.getModifiedDate().getTime() != 0) {
      Heading widgets = new Heading(5, "Changed",e.getModifiedDate().toString());
      widgets.addStyleName("floatRight");
      row.add(widgets);
    }

    column.add(getEntry(e, FOREIGN_LANGUAGE, ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));
    column.add(getEntry(e, TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, e.getTransliteration()));
    column.add(getEntry(e, ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, e.getEnglish()));

    return column;
  }

  private Heading getComment() {
    boolean isComment = instance.equals(Navigation.COMMENT);
    String columnLabel = isComment ? COMMENT : DEFECT;
    Heading heading = new Heading(4, columnLabel);
    heading.addStyleName("borderBottomQC");
    if (isComment) heading.setWidth("90px");
    return heading;
  }

  public void onResize() {
    super.onResize();
    for (RequiresResize rr : toResize) rr.onResize();
  }

  protected Widget getScoringAudioPanel(final CommonExercise e, String pathxxxx) {
    Panel column = new FlowPanel();
    column.addStyleName("blockStyle");
    if (toResize == null) toResize = new ArrayList<RequiresResize>();

    for (AudioAttribute audio : e.getAudioAttributes()) {
      String audioRef = audio.getAudioRef();
      if (audioRef != null) {
        audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
      }
      ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller,
        controller.getProps().showSpectrogram(), scorePanel, 93, "", e.getID());
      audioPanel.setShowColor(true);
      audioPanel.getElement().setId("ASRScoringAudioPanel");
      String name = REFERENCE + " : " + audio.getDisplay();
      if (audio.isFast()) name = REGULAR_SPEED_AUDIO_EXAMPLE;
      else if (audio.isSlow()) name = SLOW_SPEED_AUDIO_EXAMPLE;
      ResizableCaptionPanel cp = new ResizableCaptionPanel(name);
      cp.setContentWidget(audioPanel);
      toResize.add(cp);
      ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

      column.add(getEntry(audio.getAudioRef(), cp, audioAnnotation)); // TODO add unique audio attribute id
    }
    if (!e.hasRefAudio()) {
      ExerciseAnnotation refAudio = e.getAnnotation(REF_AUDIO);
      column.add(getEntry(REF_AUDIO, new Label(NO_AUDIO_RECORDED), refAudio));
    }
    return column;
  }

  private Widget getEntry(CommonExercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field), true);
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation, boolean includeLabel) {
    return getEntry(field, getContentWidget(label, value, true, includeLabel), annotation);
  }

  /**
   * @param field
   * @param annotation
   * @return
   */
  private Widget getEntry(final String field, Widget content, ExerciseAnnotation annotation) {
    final FocusWidget commentEntry = makeCommentEntry(field, annotation);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    final Panel commentRow = new FlowPanel();
    final Widget qcCol = getQCCheckBox(field, commentEntry, alreadyMarkedCorrect, commentRow);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right

    Panel row = new FlowPanel();
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

  private Widget getQCCheckBox(String field, FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    return makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);
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

  private FocusWidget makeCommentEntry(final String field, ExerciseAnnotation annotation) {
    final TextBox commentEntry = new TextBox();
    commentEntry.getElement().setId("QCNPFExercise_Comment_TextBox_"+field);
    commentEntry.addStyleName("topFiveMargin");
    if (annotation != null) {
      commentEntry.setText(annotation.comment);
    }
    commentEntry.setVisibleLength(100);
    commentEntry.setWidth("400px");

    commentEntry.addStyleName("leftFiveMargin");
    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        addIncorrectComment(commentEntry.getText(), field);
      }
    });
    addTooltip(commentEntry, COMMENT_TOOLTIP);
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
  private CheckBox makeCheckBox(final String field, final Panel commentRow, final FocusWidget commentEntry,
                                boolean alreadyMarkedCorrect) {
    boolean isComment = instance.equals(Navigation.COMMENT);

    final CheckBox checkBox = new CheckBox("");
    checkBox.getElement().setId("CheckBox_"+field);
    checkBox.addStyleName(isComment? "wideCenteredRadio" :"centeredRadio");
    checkBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        checkBoxWasClicked(checkBox.getValue(), field, commentRow, commentEntry);
      }
    });
    checkBox.setValue(!alreadyMarkedCorrect);
    if (!isComment) {
      addTooltip(checkBox, CHECKBOX_TOOLTIP);
    }
    return checkBox;
  }

  private void checkBoxWasClicked(boolean isIncorrect, String field, Panel commentRow, FocusWidget commentEntry) {
    commentRow.setVisible(isIncorrect);
    commentEntry.setFocus(isIncorrect);

    if (isIncorrect) {
      incorrectSet.add(field);
    }
    else {
      incorrectSet.remove(field);
      addCorrectComment(field);
    }

    if (isCourseContent()) {
      String id = exercise.getID();
      if (incorrectSet.isEmpty()) {
        listContainer.removeCompleted(id);
      }
      else {
        listContainer.addCompleted(id);
      }

      if (approvedButton != null) {
        approvedButton.setEnabled(incorrectSet.isEmpty());

        approvedTooltip.setText(incorrectSet.isEmpty() ? APPROVED_BUTTON_TOOLTIP : APPROVED_BUTTON_TOOLTIP2);
        approvedTooltip.reconfigure();
      }
      markReviewed(exercise);
    }
  }
}
