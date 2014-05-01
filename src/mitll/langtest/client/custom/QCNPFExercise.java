package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
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
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.STATE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  public static final String CONTEXT = "context";

  private static final String REF_AUDIO = "refAudio";
  private static final String APPROVED = "Approve Item";
  private static final String NO_AUDIO_RECORDED = "No Audio Recorded.";
  private static final String COMMENT = "Comment";

  private static final String COMMENT_TOOLTIP = "Comments are optional.";
  private static final String CHECKBOX_TOOLTIP = "Check to indicate this field has a defect.";
  private static final String APPROVED_BUTTON_TOOLTIP = "Indicate item has no defects.";
  private static final String APPROVED_BUTTON_TOOLTIP2 = "Item has been marked with a defect";
  private static final String ATTENTION_LL = "Attention LL";
  private static final String MARK_FOR_LL_REVIEW = "Mark for LL review.";

  private Set<String> incorrectFields;
  private List<RequiresResize> toResize;
  private Set<Widget> audioWasPlayed;
  private final ListInterface listContainer;
  private Button approvedButton;
  private Tooltip approvedTooltip;
  private Tooltip nextTooltip;

  public QCNPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);

    this.listContainer = listContainer;
  }

  @Override
  protected ASRScorePanel makeScorePanel(CommonExercise e, String instance) {
    audioWasPlayed = new HashSet<Widget>();
    toResize = new ArrayList<RequiresResize>();

    incorrectFields = new HashSet<String>();
    return null;
  }

  /**
   * @see #addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float)
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise, com.google.gwt.user.client.ui.Panel)
   * @param div
   */
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
    NavigationHelper navHelper = new NavigationHelper(exercise, controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    }, listContainer, true, addKeyHandler) {
      /**
       * So only allow next button when all audio has been played
       * @param exercise
       */
      @Override
      protected void enableNext(CommonExercise exercise) {
        boolean allPlayed = audioWasPlayed.size() == toResize.size();
        next.setEnabled(allPlayed);
      }
    };

    nextTooltip = addTooltip(navHelper.getNext(), audioWasPlayed.size() == toResize.size() ? "Click to indicate item has been reviewed." : "Item has uninspected audio.");

    if (!instance.contains(Navigation.REVIEW) && !instance.contains(Navigation.COMMENT)) {
      approvedButton = addApprovedButton(listContainer, navHelper);
      if (!controller.getProps().isNoModel()) {
        addAttnLLButton(listContainer, navHelper);
      }
    }
    setApproveButtonState();
    return navHelper;
  }

  /**
   * @see #getNavigationHelper(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, boolean)
   * @param listContainer
   * @param widgets
   * @return
   */
  private Button addApprovedButton(final ListInterface listContainer, NavigationHelper widgets) {
    Button approved = new Button(APPROVED);
    approved.getElement().setId("approve");
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

  private Button addAttnLLButton(final ListInterface listContainer, NavigationHelper widgets) {
    Button attention = new Button(ATTENTION_LL);
    attention.getElement().setId("attention");
    attention.addStyleName("leftFiveMargin");
    widgets.add(attention);
    attention.setType(ButtonType.WARNING);
    attention.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        markAttentionLL(listContainer, exercise);
      }
    });
    /*approvedTooltip = */addTooltip(attention, MARK_FOR_LL_REVIEW);
    return attention;
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
      markReviewed(completedExercise);
      boolean allCorrect = incorrectFields.isEmpty();

      listContainer.setState(completedExercise.getID(), allCorrect ? STATE.APPROVED : STATE.DEFECT);
      listContainer.redraw();
    }
  }

  /**
   * So if the attention LL button has been pressed, clicking next should not step on that setting
   * @param listContainer
   * @param completedExercise
   */
  private void markAttentionLL(ListInterface listContainer, CommonShell completedExercise) {
    if (isCourseContent()) {
      service.markState(completedExercise.getID(), STATE.ATTN_LL, controller.getUser(),
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Void result) { }
        });

      listContainer.setSecondState(completedExercise.getID(), STATE.ATTN_LL);
      listContainer.redraw();
    }
  }

  private boolean isCourseContent() {
    return !instance.equals(Navigation.REVIEW) && !instance.equals(Navigation.COMMENT);
  }

  /**
   * @see #markReviewed(mitll.langtest.client.list.ListInterface, mitll.langtest.shared.CommonShell)
   * @param completedExercise
   */
  private void markReviewed(final CommonShell completedExercise) {
    boolean allCorrect = incorrectFields.isEmpty();
    System.out.println("markReviewed : exercise " + completedExercise.getID() + " instance " + instance + " allCorrect " + allCorrect);

    service.markReviewed(completedExercise.getID(), allCorrect, controller.getUser(),
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
          System.out.println("\tmarkReviewed.onSuccess exercise " + completedExercise.getID() + " marked reviewed!");
        }
      }
    );
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
      Heading widgets = new Heading(5, "Changed", e.getModifiedDate().toString());
      if (!e.getAudioAttributes().isEmpty()) {
        widgets.addStyleName("floatRight");
      }
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

  private List<RememberTabAndContent> tabs;

  protected Widget getScoringAudioPanel(final CommonExercise e, String pathxxxx) {
    if (!e.hasRefAudio()) {
      ExerciseAnnotation refAudio = e.getAnnotation(REF_AUDIO);
      Panel column = new FlowPanel();
      column.addStyleName("blockStyle");
      column.add(getEntry(REF_AUDIO, new Label(NO_AUDIO_RECORDED), refAudio));
      return column;
    } else {
      Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getUserMap(true);
      Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getUserMap(false);

      List<MiniUser> maleUsers = exercise.getSortedUsers(malesMap);
      List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);

      tabs = new ArrayList<RememberTabAndContent>();

      TabPanel tabPanel = new TabPanel();
      addTabsForUsers(e, tabPanel, malesMap, maleUsers);
      addTabsForUsers(e, tabPanel, femalesMap, femaleUsers);

      if (!maleUsers.isEmpty() || !femaleUsers.isEmpty()) {
        tabPanel.selectTab(0);
      }
      return tabPanel;
    }
  }

  private void addTabsForUsers(CommonExercise e, TabPanel tabPanel, Map<MiniUser, List<AudioAttribute>> malesMap, List<MiniUser> maleUsers) {
    for (MiniUser user : maleUsers) {
      String tabTitle = (user.isMale() ? "Male" :"Female")+
        (controller.getProps().isAdminView() ?" (" + user.getUserID() + ")" :"") +
        " age " + user.getAge();

      RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle);
      tabPanel.add(tabAndContent.tab.asTabLink());
      tabs.add(tabAndContent);

      // TODO : when do we need this???
      tabAndContent.content.getElement().getStyle().setMarginRight(70, Style.Unit.PX);

      boolean allHaveBeenPlayed = true;

      for (AudioAttribute audio : malesMap.get(user)) {
        if (!audio.isHasBeenPlayed()) allHaveBeenPlayed = false;
        Pair panelForAudio1 = getPanelForAudio(e, audio, tabAndContent);
        Widget panelForAudio = panelForAudio1.entry;
        tabAndContent.content.add(panelForAudio);
        if (audio.isHasBeenPlayed()) {
          audioWasPlayed.add(panelForAudio1.audioPanel);
        }
      }

      if (allHaveBeenPlayed) {
        tabAndContent.tab.setIcon(IconType.CHECK_SIGN);
      }
    }
  }


  /**
   * Keep track of all audio elements -- have they all been played? If so, we can enable the approve & next buttons
   * Also, when all audio for a tab have been played, change tab icon to check
   *
   * @param e
   * @param audio
   * @return
   */
  private Pair getPanelForAudio(final CommonExercise e, final AudioAttribute audio,RememberTabAndContent tabAndContent) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller,
      controller.getProps().showSpectrogram(), scorePanel, 70, audio.isFast()?" Regular speed":" Slow speed", e.getID());
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        System.out.println("playing audio " + audio.getAudioRef() + " has " +tabs.size() + " tabs, now " + audioWasPlayed.size()  + " played");
        //if (audioWasPlayed.size() == toResize.size()) {
          // all components played
          setApproveButtonState();
       // }
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", e.getID(), audio.getAudioRef());
      }

      @Override
      public void playStopped() {

      }
    });
    tabAndContent.addWidget(audioPanel);
    toResize.add(audioPanel);
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

    Widget entry = getEntry(audio.getAudioRef(), audioPanel, audioAnnotation);
    // TODO add unique audio attribute id
    return new Pair(entry,audioPanel);
  }

  private static class Pair {
    Widget entry, audioPanel;

    public Pair(Widget entry, Widget audioPanel) {
      this.entry = entry;
      this.audioPanel = audioPanel;
    }

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
    if (!alreadyMarkedCorrect) {
      incorrectFields.add(field);
    }
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

  /**
   * @see #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   * @param field
   * @param annotation
   * @return
   */
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
      incorrectFields.add(field);
    }
    else {
      incorrectFields.remove(field);
      addCorrectComment(field);
    }

    System.out.println("checkBoxWasClicked : instance = '" +instance +"'");

    if (isCourseContent()) {
      String id = exercise.getID();
      System.out.println("\tcheckBoxWasClicked : instance = '" +instance +"'");
      if (instance.equalsIgnoreCase(Navigation.CLASSROOM)) {
        STATE state = incorrectFields.isEmpty() ? STATE.UNSET : STATE.DEFECT;
        exercise.setState(state);
        listContainer.setState(id, state);
        System.out.println("\tcheckBoxWasClicked : state now = '" +state +"'");

        listContainer.redraw();
      }
      else {
        System.out.println("\tcheckBoxWasClicked : ignoring instance = '" +instance +"'");
      }

      setApproveButtonState();
      markReviewed(exercise);
    }
  }

  /**
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   */
  private void setApproveButtonState() {
    boolean allCorrect = incorrectFields.isEmpty();
    boolean allPlayed = audioWasPlayed.size() == toResize.size();

    //System.out.println("\tsetApproveButtonState : allPlayed= '" +allPlayed +"' allCorrect " + allCorrect + " audio played " + audioWasPlayed.size() + " total " + toResize.size());

    if (approvedButton != null) {   // comment tab doesn't have it...!
      approvedButton.setEnabled(allCorrect && allPlayed);

      approvedTooltip.setText(!allPlayed ? "Not all audio has been reviewed" : allCorrect ? APPROVED_BUTTON_TOOLTIP : APPROVED_BUTTON_TOOLTIP2);
      approvedTooltip.reconfigure();
    }

    if (navigationHelper != null) { // this called before nav helper exists
      navigationHelper.enableNextButton(allPlayed);
      nextTooltip.setText(!allPlayed ? "Not all audio has been reviewed" : allCorrect ? APPROVED_BUTTON_TOOLTIP : APPROVED_BUTTON_TOOLTIP2);
      nextTooltip.reconfigure();
    }
  }
}
