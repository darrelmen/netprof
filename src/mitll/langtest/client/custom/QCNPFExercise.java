package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.AudioPanel;
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
  private static final String MARK_FOR_LL_REVIEW = "Mark for review by Lincoln Laboratory.";
  //public static final int DEFAULT_USER = -1;
  public static final int DEFAULT_MALE_ID = -2;
  public static final int DEFAULT_FEMALE_ID = -3;
  private static MiniUser DEFAULT_MALE = new MiniUser(DEFAULT_MALE_ID, 30, 0, "default", "default", "Male");
  private static MiniUser DEFAULT_FEMALE = new MiniUser(DEFAULT_FEMALE_ID, 30, 1, "default", "default", "Female");

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
   * @see GoodwaveExercisePanel#addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.CommonExercise)
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
    addTooltip(attention, MARK_FOR_LL_REVIEW);
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
   * @see #checkBoxWasClicked(boolean, String, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.FocusWidget)
   * @see #markReviewed(mitll.langtest.client.list.ListInterface, mitll.langtest.shared.CommonShell)
   * @param completedExercise
   */
  private void markReviewed(final CommonShell completedExercise) {
    boolean allCorrect = incorrectFields.isEmpty();
    //System.out.println("markReviewed : exercise " + completedExercise.getID() + " instance " + instance + " allCorrect " + allCorrect);

    service.markReviewed(completedExercise.getID(), allCorrect, controller.getUser(),
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void result) {
          //System.out.println("\tmarkReviewed.onSuccess exercise " + completedExercise.getID() + " marked reviewed!");
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
   * @param exercise
   */
  @Override
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                 float screenPortion, CommonExercise exercise) {}

  /**
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
    //  if (!e.getAudioAttributes().isEmpty()) {
    //    widgets.addStyleName("floatRight");
    //  }
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

  protected Widget getScoringAudioPanel(final CommonExercise e) {
    if (!e.hasRefAudio()) {
      ExerciseAnnotation refAudio = e.getAnnotation(REF_AUDIO);
      Panel column = new FlowPanel();
      column.addStyleName("blockStyle");
      column.add(getCommentWidget(REF_AUDIO, new Label(NO_AUDIO_RECORDED), refAudio));
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

  /**
   * For all the users, show a tab for each audio cut they've recorded (regular and slow speed).
   * Special logic included for default users so a QC person can set the gender.
   * @param e
   * @param tabPanel
   * @param malesMap
   * @param maleUsers
   */
  private void addTabsForUsers(CommonExercise e, TabPanel tabPanel, Map<MiniUser, List<AudioAttribute>> malesMap, List<MiniUser> maleUsers) {
    int me = controller.getUser();
    for (MiniUser user : maleUsers) {
      String tabTitle = getUserTitle(me, user);

      RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle);
      tabPanel.add(tabAndContent.getTab().asTabLink());
      tabs.add(tabAndContent);

      // TODO : when do we need this???
      tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);

      boolean allHaveBeenPlayed = true;

      List<AudioAttribute> audioAttributes = malesMap.get(user);
      for (AudioAttribute audio : audioAttributes) {
        if (!audio.isHasBeenPlayed()) allHaveBeenPlayed = false;
        Pair panelForAudio1 = getPanelForAudio(e, audio);

        Widget panelForAudio = panelForAudio1.entry;
        tabAndContent.addWidget(panelForAudio1.audioPanel);
        toResize.add(panelForAudio1.audioPanel);

        if (user.isDefault()) {    // add widgets to mark gender on default audio
          Panel vp = new VerticalPanel();
          Panel hp = new HorizontalPanel();

          final Button next = getNextButton();
          hp.add(getGenderGroup(tabAndContent, audio, next, audioAttributes));
          hp.add(next);
          next.setVisible(false);
          vp.add(hp);
          vp.add(panelForAudio);
          tabAndContent.getContent().add(vp);
        } else {
          tabAndContent.getContent().add(panelForAudio);
        }
        if (audio.isHasBeenPlayed()) {
          audioWasPlayed.add(panelForAudio1.audioPanel);
        }
      }

      if (allHaveBeenPlayed) {
        tabAndContent.getTab().setIcon(IconType.CHECK_SIGN);
      }
    }
  }

  private Button getNextButton() {
    final Button next = new Button("Next");
    next.setType(ButtonType.SUCCESS);
    next.setIcon(IconType.ARROW_RIGHT);
    next.addStyleName("leftFiveMargin");
    next.addStyleName("topMargin");
    next.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        next.setEnabled(false);
        loadNext();
      }
    });
    return next;
  }

  /**
   * TODO : this may be undone if someone deletes the audio cut - since this essentially masks out old score
   * default audio.
   *
   * TODO:  add list of all audio by this user.
   * @param tabAndContent
   * @param audio
   * @param next
   * @param allByUser
   * @return
   * @see #addTabsForUsers(mitll.langtest.shared.CommonExercise, com.github.gwtbootstrap.client.ui.TabPanel, java.util.Map, java.util.List)
   */
  private DivWidget getGenderGroup(final RememberTabAndContent tabAndContent, final AudioAttribute audio, final Button next, final List<AudioAttribute> allByUser) {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    final Button onButton = makeGroupButton(buttonGroup, "MALE");

    if (audio.getExid() == null) {
      audio.setExid(exercise.getID());
    }
    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onButton.setEnabled(false);

        service.markGender(audio, true, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            onButton.setEnabled(true);
          }

          @Override
          public void onSuccess(Void result) {
            audio.setUser(DEFAULT_MALE);
            showGenderChange(onButton, tabAndContent, allByUser, next);
          }
        });
      }
    });

    final Button offButton = makeGroupButton(buttonGroup, "FEMALE");

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        offButton.setEnabled(false);
        service.markGender(audio, false, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            offButton.setEnabled(true);
          }

          @Override
          public void onSuccess(Void result) {
            audio.setUser(DEFAULT_FEMALE);
            showGenderChange(offButton, tabAndContent, allByUser, next);
          }
        });
      }
    });

    return w;
  }

  protected void showGenderChange(Button onButton, RememberTabAndContent tabAndContent, List<AudioAttribute> allByUser, Button next) {
    onButton.setEnabled(true);
    tabAndContent.getTab().setHeading(getTabLabelFromAudio(allByUser));
    next.setVisible(true);
  }

  protected String getTabLabelFromAudio(List<AudioAttribute> allByUser) {
    StringBuilder builder = new StringBuilder();
    AudioAttribute last = allByUser.get(allByUser.size() - 1);
    for (AudioAttribute audioAttribute : allByUser) {
      builder.append(audioAttribute.getUser().isDefault() ? GoodwaveExercisePanel.DEFAULT_SPEAKER : audioAttribute.isMale() ? "Male":"Female");
      if (audioAttribute != last) builder.append("/");
    }
    return builder.toString();
  }

  private Button makeGroupButton(ButtonGroup buttonGroup,String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId("MaleFemale"+"_"+title);
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);
    return onButton;
  }

  private String getUserTitle(int me, MiniUser user) {
    return (user.isDefault()) ? GoodwaveExercisePanel.DEFAULT_SPEAKER : (user.getId() == me) ? "by You (" +user.getUserID()+ ")" : getUserTitle(user);
  }

  private String getUserTitle(MiniUser user) {
    return (user.isMale() ? "Male" :"Female")+
      (controller.getProps().isAdminView() ?" (" + user.getUserID() + ")" :"") +
      " age " + user.getAge();
  }

  /**
   * Keep track of all audio elements -- have they all been played? If so, we can enable the approve & next buttons
   * Also, when all audio for a tab have been played, change tab icon to check
   *
   * @param e
   * @param audio
   * @return both the comment widget and the audio panel
   * @see #addTabsForUsers(mitll.langtest.shared.CommonExercise, com.github.gwtbootstrap.client.ui.TabPanel, java.util.Map, java.util.List)
   */
  private Pair getPanelForAudio(final CommonExercise e, final AudioAttribute audio) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller,
      controller.getProps().showSpectrogram(), scorePanel, 70, audio.isRegularSpeed()?" Regular speed":" Slow speed", e.getID());
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        //System.out.println("playing audio " + audio.getAudioRef() + " has " +tabs.size() + " tabs, now " + audioWasPlayed.size()  + " played");
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
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

    Widget entry = getCommentWidget(audio.getAudioRef(), audioPanel, audioAnnotation);
    return new Pair(entry, audioPanel);
  }

  private static class Pair {
    Widget entry;
    AudioPanel audioPanel;

    public Pair(Widget entry, AudioPanel audioPanel) {
      this.entry = entry;
      this.audioPanel = audioPanel;
    }

  }

  private Widget getEntry(CommonExercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field), true);
  }

  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation, boolean includeLabel) {
    return getCommentWidget(field, getContentWidget(label, value, true, includeLabel), annotation);
  }

  /**
   * @param field
   * @param annotation
   * @return
   */
  private Widget getCommentWidget(final String field, Widget content, ExerciseAnnotation annotation) {
    final FocusWidget commentEntry = makeCommentEntry(field, annotation);

    boolean alreadyMarkedCorrect = annotation == null || annotation.status == null || annotation.status.equals("correct");
    if (!alreadyMarkedCorrect) {
      incorrectFields.add(field);
    }
    final Panel commentRow = new FlowPanel();
    commentRow.getElement().setId("QCNPFExercise_commentRow_"+field);

    final Widget qcCol = getQCCheckBox(field, commentEntry, alreadyMarkedCorrect, commentRow);
    qcCol.getElement().setId("QCNPFExercise_qcCol_"+field);

    populateCommentRow(commentEntry, alreadyMarkedCorrect, commentRow);

    // comment to left, content to right

    Panel row = new FlowPanel();
    row.getElement().setId("QCNPFExercise_row_"+field);

    row.addStyleName("trueInlineStyle");
    qcCol.addStyleName("floatLeft");
    row.add(qcCol);
    row.add(content);

    Panel rowContainer = new FlowPanel();
    rowContainer.getElement().setId("QCNPFExercise_rowContainer_"+field);
    rowContainer.addStyleName("topFiveMargin");
    rowContainer.addStyleName("blockStyle");
    rowContainer.add(row);
    rowContainer.add(commentRow);

    // why????
  //  rowContainer.setWidth("650px");

    return rowContainer;
  }

  private Widget getQCCheckBox(String field, FocusWidget commentEntry, boolean alreadyMarkedCorrect, Panel commentRow) {
    return makeCheckBox(field, commentRow, commentEntry, alreadyMarkedCorrect);
  }

  /**
   * @see #getCommentWidget(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
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
   * @see #getCommentWidget(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
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

    //System.out.println("checkBoxWasClicked : instance = '" +instance +"'");

    if (isCourseContent()) {
      String id = exercise.getID();
     // System.out.println("\tcheckBoxWasClicked : instance = '" +instance +"'");
      if (instance.equalsIgnoreCase(Navigation.CLASSROOM)) {
        STATE state = incorrectFields.isEmpty() ? STATE.UNSET : STATE.DEFECT;
        exercise.setState(state);
        listContainer.setState(id, state);
     //   System.out.println("\tcheckBoxWasClicked : state now = '" +state +"'");

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
