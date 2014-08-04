package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.ScoreAndPath;

import java.util.*;

/**
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends HorizontalPanel implements BusyPanel, RequiresResize, ProvidesResize {
  private static final String REFERENCE = "";//" Reference";
  private static final String RECORD_YOURSELF = "Record";// Yourself";
  private static final String RELEASE_TO_STOP = "Release";// to Stop";
  public static final int HEADING_FOR_UNIT_LESSON = 4;
  private static final String CORRECT = "correct";
  private static final String INCORRECT = "incorrect";
  private static final int DEFAULT_USER = -1;
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private final ListInterface listContainer;
  private boolean isBusy = false;

  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  private Image recordImage1;
  private Image recordImage2;

  protected final CommonExercise exercise;
  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  protected ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;
  protected final NavigationHelper navigationHelper;
  private final float screenPortion;
  protected final String instance;
 // private static final boolean DEBUG = true;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   *
   * @param e             for this exercise
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param instance
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   */
  public GoodwaveExercisePanel(final CommonExercise e, final ExerciseController controller,
                               final ListInterface listContainer,
                               float screenPortion, boolean addKeyHandler, String instance) {
    this.exercise = e;
    this.controller = controller;
    this.service = controller.getService();
    this.screenPortion = screenPortion;
    this.instance = instance;
    setWidth("100%");
    addStyleName("inlineBlockStyle");
    getElement().setId("GoodwaveExercisePanel");
    final Panel center = new VerticalPanel();
    center.getElement().setId("GoodwaveVerticalCenter");
    center.addStyleName("floatLeft");
    // attempt to left justify

    ASRScorePanel widgets = makeScorePanel(e, instance);

    addQuestionContentRow(e, controller, center);

    // content is on the left side
    add(center);

    // score panel with gauge is on the right
    if (widgets != null && !controller.getProps().isNoModel() && controller.isRecordingEnabled()) {
      add(widgets);
    }
    if (controller.isRecordingEnabled()) {
      addUserRecorder(service, controller, center, screenPortion,e); // todo : revisit screen portion...
    }

    this.navigationHelper = getNavigationHelper(controller, listContainer, addKeyHandler);
    this.listContainer = listContainer;
    navigationHelper.addStyleName("topBarMargin");
    center.add(navigationHelper);
  }

  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                           final ListInterface listContainer, boolean addKeyHandler) {
    return new NavigationHelper(exercise, controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    }, listContainer, true, addKeyHandler,false);
  }

  public void wasRevealed() {}

  protected ASRScorePanel makeScorePanel(CommonExercise e, String instance) {
    ASRScorePanel widgets = new ASRScorePanel("GoodwaveExercisePanel_" + instance, controller, exercise.getID());
    scorePanel = widgets;
    return widgets;
  }

  protected void loadNext() {
    listContainer.loadNextExercise(exercise.getID());
  }

  protected void nextWasPressed(ListInterface listContainer, CommonShell completedExercise) {
    navigationHelper.enableNextButton(false);
    listContainer.loadNextExercise(completedExercise.getID());
  }

  protected void addQuestionContentRow(CommonExercise e, ExerciseController controller, Panel hp) {
     hp.add(getQuestionContent(e/*, (Panel) null*/));
  }

  public void setBusy(boolean v) {  this.isBusy = v;  }

  /**
   * For every question,
   * <ul>
   * <li>show the text of the question,  </li>
   * <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   * <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   *
   * @param service
   * @param controller    used in subclasses for audio control
   * @param screenPortion
   * @param exercise
   * @paramx i
   * @see #GoodwaveExercisePanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, float, boolean, String)
   */
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                 float screenPortion, CommonExercise exercise) {
    DivWidget div = new DivWidget();
    ScoringAudioPanel answerWidget = getAnswerWidget(service, controller, 1, screenPortion);
    String refAudio = exercise.getRefAudio();
    if (refAudio == null) {
      refAudio = exercise.getSlowAudioRef();
    }

    showRecordingHistory(exercise, answerWidget, refAudio);
    div.add(answerWidget);

    addGroupingStyle(div);
    toAddTo.add(div);
  }

  private void showRecordingHistory(CommonExercise exercise, ScoringAudioPanel answerWidget, String refAudio) {
    answerWidget.setRefAudio(refAudio);
    for (ScoreAndPath score : exercise.getScores()) {
      answerWidget.addScore(score);
    }
    answerWidget.setClassAvg(exercise.getAvgScore());
    answerWidget.showChart();
  }

  protected void addGroupingStyle(Widget div) {
    div.addStyleName("buttonGroupInset6");
  }

  /**
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @return
   */
  private Panel getUnitLessonForExercise() {
    Panel flow = new HorizontalPanel();
    flow.getElement().setId("getUnitLessonForExercise_unitLesson");
    flow.addStyleName("leftFiveMargin");
    //System.out.println("getUnitLessonForExercise " + exercise + " unit value " +exercise.getUnitToValue());

    for (String type : controller.getStartupInfo().getTypeOrder()) {
      Heading child = new Heading(HEADING_FOR_UNIT_LESSON, type, exercise.getUnitToValue().get(type));
      child.addStyleName("rightFiveMargin");
      flow.add(child);
    }
    return flow;
  }

  public void onResize() {
    if (contentAudio != null) {
      contentAudio.onResize();
    }
    if (answerAudio != null) {
      answerAudio.onResize();
    }
  }

  /**
   * Show the instructions and the audio panel.<br></br>
   * <p/>
   * Replace the html 5 audio tag with our fancy waveform widget.
   *
   * @param e for this exercise
   * @return the panel that has the instructions and the audio panel
   * @see #GoodwaveExercisePanel
   */
  Widget getQuestionContent(CommonExercise e/*, Panel addToList*/) {
    String content = e.getContent();
    String path = e.getRefAudio() != null ? e.getRefAudio() : e.getSlowAudioRef();

    final VerticalPanel vp = new VerticalPanel();
    vp.getElement().setId("getQuestionContent_verticalContainer");

    if (!e.getUnitToValue().isEmpty()) {
      Panel unitLessonForExercise = getUnitLessonForExercise();
      unitLessonForExercise.add(getItemHeader(e));
      vp.add(unitLessonForExercise);
    }
    vp.addStyleName("blockStyle");

    Widget questionContent = getQuestionContent(e, content);

    vp.add(questionContent);

    Widget scoringAudioPanel = getScoringAudioPanel(e, path);
    Panel div = new SimplePanel(scoringAudioPanel);
    div.getElement().setId("scoringAudioPanel_div");
    div.addStyleName("trueInlineStyle");
    div.addStyleName("floatLeft");
    addGroupingStyle(div);

    vp.add(div);
    return vp;
  }

  /**
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param e
   * @return
   */
  private Widget getItemHeader(CommonExercise e) {
    Heading w = new Heading(HEADING_FOR_UNIT_LESSON, "Item", e.getID());
    w.getElement().setId("ItemHeading");
    return w;
  }

  protected Widget getQuestionContent(CommonExercise e, String content) {
    Widget questionContent = new HTML(content);
    questionContent.getElement().setId("QuestionContent");
    questionContent.addStyleName("floatLeft");
    return questionContent;
  }

  /**
   *
   * @see #getQuestionContent
   * @param e
   * @param path
   * @return
   */
  protected Widget getScoringAudioPanel(final CommonExercise e, String path) {
    if (path != null) {
      path = wavToMP3(path);
    }
    contentAudio = getAudioPanel(path);
    contentAudio.setScreenPortion(screenPortion);
    return contentAudio;
  }

  private ASRScoringAudioPanel getAudioPanel(String path) {
    ASRScoringAudioPanel audioPanel = makeFastAndSlowAudio(path);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    return audioPanel;
  }

  protected ASRScoringAudioPanel makeFastAndSlowAudio(String path) {
    return new FastAndSlowASRScoringAudioPanel(exercise, path, service, controller, scorePanel);
  }

  protected String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  /**
   * @see mitll.langtest.client.custom.QCNPFExercise#makeCommentEntry(String, mitll.langtest.shared.ExerciseAnnotation)
   * @param commentToPost
   * @param field
   */
  protected void addIncorrectComment(final String commentToPost, final String field) {
/*    System.out.println(new Date() + " : post to server " + exercise.getID() +
      " field " + field + " commentLabel '" + commentToPost + "' is incorrect");*/
    addAnnotation(field, INCORRECT, commentToPost);
  }

  protected void addCorrectComment(final String field) {
  //  System.out.println(new Date() + " : post to server " + exercise.getID() + " field " + field + " is correct");
    addAnnotation(field, CORRECT, "");
  }

  private void addAnnotation(final String field, final String status, final String commentToPost) {
    service.addAnnotation(exercise.getID(), field, status, commentToPost, controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {
        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + exercise.getID() +
          " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);//, took " + (now - then) + " millis");
      }
    });
  }

  /**
   * @see mitll.langtest.client.custom.CommentNPFExercise#getEntry
   * @see mitll.langtest.client.custom.QCNPFExercise#getEntry
   * @param label
   * @param value
   * @param withWrap
   * @param includeLabel
   * @return
   */
  protected Panel getContentWidget(String label, String value, boolean withWrap, boolean includeLabel) {
    //System.out.println("label " + label + " value " + value);
    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    if (includeLabel) {
      InlineHTML labelWidget = new InlineHTML(label);
      labelWidget.addStyleName("Instruction-title");
      nameValueRow.add(labelWidget);
    }

    InlineHTML englishPhrase = new InlineHTML(value, WordCountDirectionEstimator.get().estimateDirection(value));
    englishPhrase.addStyleName(withWrap ? "Instruction-data-with-wrap" : "Instruction-data");
    if (label.contains("Meaning")) {
      englishPhrase.addStyleName("englishFont");
    }
    nameValueRow.add(englishPhrase);
    addTooltip(englishPhrase,label.replaceAll(":",""));
    englishPhrase.addStyleName("leftFiveMargin");
    return nameValueRow;
  }

  /**
   * @see mitll.langtest.client.custom.QCNPFExercise#populateCommentRow
   * @return
   */
  protected Label getCommentLabel() {
    final Label commentLabel = new Label("comment?");
    DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
    commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }

  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @param service
   * @param controller
   * @param index
   * @param screenPortion
   * @return
   * @see #addUserRecorder(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, float, mitll.langtest.shared.CommonExercise)
   */
  private ScoringAudioPanel getAnswerWidget(LangTestDatabaseAsync service, final ExerciseController controller, final int index, float screenPortion) {
    ScoringAudioPanel widgets = new ASRRecordAudioPanel(service, index, controller);
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(screenPortion);

    return widgets;
  }

  /**
   * An ASR scoring panel with a record button.
   */
  private class ASRRecordAudioPanel extends ASRScoringAudioPanel {
    private final int index;
    private PostAudioRecordButton postAudioRecordButton;
    private PlayAudioPanel playAudioPanel;
    IconAnchor download;

    /**
     * @param service
     * @param index
     * @param controller
     * @see GoodwaveExercisePanel#getAnswerWidget
     */
    public ASRRecordAudioPanel(LangTestDatabaseAsync service, int index, ExerciseController controller) {
      super(exercise.getForeignLanguage(), service, controller, scorePanel, REFERENCE, exercise.getID());
      this.index = index;
      getElement().setId("ASRRecordAudioPanel");
    }

    /**
     * So here we're trying to make the record and play buttons know about each other
     * to the extent that when we're recording, we can't play audio, and when we're playing
     * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
     *
     *
     * @param toTheRightWidget
     * @param buttonTitle
     * @param recordButtonTitle
     * @return
     * @see AudioPanel#getPlayButtons
     */
    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String audioType, String recordButtonTitle) {
      recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
      recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
      postAudioRecordButton = new MyPostAudioRecordButton(controller);
      DOM.setElementProperty(postAudioRecordButton.getElement(), "margin", "8px");
      playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, soundManager, postAudioRecordButton,
        GoodwaveExercisePanel.this, "");
      return playAudioPanel;
    }

    @Override
    protected void onUnload() {
      super.onUnload();
      navigationHelper.removeKeyHandler();
    }

    private class MyPlayAudioPanel extends PlayAudioPanel {
      public MyPlayAudioPanel(Image recordImage1, Image recordImage2, SoundManagerAPI soundManager,
                              final PostAudioRecordButton postAudioRecordButton1,
                              final GoodwaveExercisePanel goodwaveExercisePanel, String playSuffix) {
        super(soundManager, new PlayListener() {
          public void playStarted() {
            goodwaveExercisePanel.setBusy(true);
            postAudioRecordButton1.setEnabled(false);
          }

          public void playStopped() {
            goodwaveExercisePanel.setBusy(false);
            postAudioRecordButton1.setEnabled(true);
          }
        }, playSuffix, null);
        add(recordImage1);
        recordImage1.setVisible(false);
        add(recordImage2);
        recordImage2.setVisible(false);
        getElement().setId("GoodwaveExercisePanel_MyPlayAudioPanel");
      }

      /**
       * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI, String, com.google.gwt.user.client.ui.Widget)
       * @param optionalToTheRight
       */
      @Override
      protected void addButtons(Widget optionalToTheRight) {
        add(postAudioRecordButton);
        postAudioRecordButton.addStyleName("rightFiveMargin");
        super.addButtons(optionalToTheRight);

        download = new IconAnchor();
        download.getElement().setId("Download_user_audio_link");
        download.setTitle("Download what you just said.");
        download.setIcon(IconType.DOWNLOAD);
        download.setIconSize(IconSize.TWO_TIMES);
        download.addStyleName("leftFiveMargin");
        download.setVisible(false);
        add(download);
      }
    }

    public void setDownloadHref() {
      download.setVisible(true);

      download.setHref("downloadAudio?file=" +
          audioPath +
          "&" +
          "exerciseID=" +
          exerciseID +
          "&" +
          "userID=" +
          controller.getUser());
    }

    /**
     * @see #makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String, String, String)
     */
    private class MyPostAudioRecordButton extends PostAudioRecordButton {
      public MyPostAudioRecordButton(ExerciseController controller) {
        super(exercise, controller, ASRRecordAudioPanel.this.service, ASRRecordAudioPanel.this.index, true,
          RECORD_YOURSELF, RELEASE_TO_STOP);
      }

      @Override
      public void useResult(AudioAnswer result) {
        setResultID(result.getResultID());
        getImagesForPath(result.getPath());
        setDownloadHref();
      }

      @Override
      public void startRecording() {
        playAudioPanel.setEnabled(false);
        isBusy = true;
        controller.logEvent(this,"RecordButton",getExercise().getID(),"startRecording");

        super.startRecording();
        recordImage1.setVisible(true);
        download.setVisible(false);
      }

      @Override
      public void stopRecording() {
        controller.logEvent(this,"RecordButton",getExercise().getID(),"stopRecording");

        playAudioPanel.setEnabled(true);
        isBusy = false;
        super.stopRecording();
        recordImage1.setVisible(false);
        recordImage2.setVisible(false);

      }

      @Override
      public void flip(boolean first) {
        recordImage1.setVisible(first);
        recordImage2.setVisible(!first);
      }

      /**
       * @param result
       * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
       */
      @Override
      protected void useInvalidResult(AudioAnswer result) {
        super.useInvalidResult(result);
        playAudioPanel.setEnabled(false);
      }
    }
  }

  public boolean isBusy() { return isBusy;  }

  protected class FastAndSlowASRScoringAudioPanel extends ASRScoringAudioPanel {
    private static final String GROUP = "group";
    private static final String NO_REFERENCE_AUDIO = "No reference audio.";
    private static final String MALE = "Male";
    private static final String FEMALE = "Female";

    /**
     * @param exercise
     * @param path
     * @param service
     * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getAudioPanel
     */
    public FastAndSlowASRScoringAudioPanel(CommonExercise exercise,
                                           String path, LangTestDatabaseAsync service, ExerciseController controller1,
                                           ScoreListener scoreListener) {
      super(path,
        exercise.getForeignLanguage(),
        service,
        controller1,
        controller1.getProps().showSpectrogram(), scoreListener, 23, REFERENCE, exercise.getID());
    }

    /**
     * Add choices to control which audio cut is chosen/gets played.
     *
     * @return
     * @see AudioPanel#addWidgets
     */
    @Override
    protected Widget getAfterPlayWidget() {
      final Panel rightSide = new VerticalPanel();

      rightSide.getElement().setId("beforePlayWidget_verticalPanel");
      rightSide.addStyleName("leftFiveMargin");
      Collection<AudioAttribute> audioAttributes = exercise.getAudioAttributes();

//      boolean allSameDialect = allAudioSameDialect(audioAttributes);
/*      System.out.println("getAfterPlayWidget : for exercise " +exercise.getID() +
        " path "+ audioPath + " attributes were " + audioAttributes);*/

      if (audioAttributes.isEmpty()) {
        addNoRefAudioWidget(rightSide);
        return rightSide;
      }
      else {
        // add gender choices
        Map<MiniUser, List<AudioAttribute>> malesMap   = exercise.getMostRecentAudio(true);

        //System.out.println("males  " + malesMap);
        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false);

       // System.out.println("females  " + femalesMap);
        Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();

        List<MiniUser> maleUsers   = exercise.getSortedUsers(malesMap);
        boolean maleEmpty = maleUsers.isEmpty();
        List<MiniUser> femaleUsers = exercise.getSortedUsers(femalesMap);
        boolean femaleEmpty = femaleUsers.isEmpty();

        Widget container = null;
        if (!maleEmpty || !femaleEmpty || !defaultUserAudio.isEmpty()) {
          //container = getDropDown(rightSide, allSameDialect, malesMap, femalesMap, maleUsers, femaleUsers);
          container = getGenderChoices(rightSide, malesMap, femalesMap, defaultUserAudio);
        }
        final Collection<AudioAttribute> initialAudioChoices = maleEmpty ?
          femaleEmpty ? audioAttributes : femalesMap.get(femaleUsers.get(0)) : malesMap.get(maleUsers.get(0));

        addRegularAndSlow(rightSide, initialAudioChoices);

        Panel leftAndRight = new HorizontalPanel();

        if (container != null) {
          leftAndRight.add(container);
        }
        leftAndRight.add(rightSide);
        return leftAndRight;
      }
    }

/*    private Widget getDropDown(Panel rightSide, boolean allSameDialect,
                                 Map<MiniUser, List<AudioAttribute>> malesMap,
                                 Map<MiniUser, List<AudioAttribute>> femalesMap,
                                 List<MiniUser> maleUsers,
                                 List<MiniUser> femaleUsers) {
      Dropdown dropdown = new Dropdown("Choose Speaker");
      dropdown.addStyleName("leftFiveMargin");
      Node child = dropdown.getElement().getChild(0);
      AnchorElement.as(child).getStyle().setFontWeight(Style.FontWeight.BOLDER);
      addChoices(rightSide, malesMap, dropdown, maleUsers, MALE*//*, !allSameDialect*//*);
      addChoices(rightSide, femalesMap, dropdown, femaleUsers, FEMALE*//*, !allSameDialect*//*);

      NavPills container = new NavPills();
      container.add(dropdown);
      container.getElement().getStyle().setMarginTop(8, Style.Unit.PX);
      return container;
    }*/

 /*   private boolean allAudioSameDialect(Collection<AudioAttribute> audioAttributes) {
      boolean allSameDialect = true;
      String last = "";
      for (AudioAttribute audioAttribute : audioAttributes) {
        MiniUser user = audioAttribute.getUser();
        if (user != null) {
          if (!last.isEmpty() && !user.getDialect().equalsIgnoreCase(last)) {
            allSameDialect = false;
            break;
          }
          last = user.getDialect();
        } else {
          System.err.println("allAudioSameDialect : no user for " + audioAttribute);
        }
      }
      return allSameDialect;
    }
*/
    private Widget getGenderChoices(final Panel rightSide,
                                    final Map<MiniUser, List<AudioAttribute>> malesMap,
                                    final Map<MiniUser, List<AudioAttribute>> femalesMap,
                                    final Collection<AudioAttribute> defaultAudioSet) {
      ButtonToolbar w = new ButtonToolbar();
      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.setToggle(ToggleType.RADIO);
      w.add(buttonGroup);

      boolean first =true;

      List<String> choices = new ArrayList<String>();

      if (!malesMap.isEmpty()) {
        choices.add(M);
      }
      if (!femalesMap.isEmpty()) {
        choices.add(F);
      }
      if (!defaultAudioSet.isEmpty()) {
        choices.add("Default");
      }

      for (final String choice : choices) {
        Button choice1 = getChoice(choice, first, new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            rightSide.clear();
            Collection<AudioAttribute> audioChoices;

            if (choice.equals(M)) {
              // System.out.println("playing male audio! " + maleAudio.getAudioRef());
              //contextPlay.playAudio(maleAudio.getAudioRef());
              audioChoices = malesMap.values().iterator().next();

            } else if (choice.equals(F)) {
              // System.out.println("playing female audio! " + femaleAudio.getAudioRef());
              audioChoices = femalesMap.values().iterator().next();

            } else {
              audioChoices = defaultAudioSet;

            }
            addRegularAndSlow(rightSide, audioChoices);
          }
        });
        buttonGroup.add(choice1);
        if (choice.equals(M)) choice1.setIcon(IconType.MALE);
        else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
        first = false;
      }

      Style style = w.getElement().getStyle();
      style.setMarginTop(10, Style.Unit.PX);
      style.setMarginBottom(0, Style.Unit.PX);
      style.setMarginLeft(5, Style.Unit.PX);

      return w;
    }

    private static final String M = "M";
    private static final String F = "F";

    private Button getChoice(String title, boolean isActive, ClickHandler handler) {
      Button onButton = new Button(title.equals(M) ? "" : title.equals(F) ? "" : title);

      onButton.getElement().setId("Choice_" + title);
      controller.register(onButton, exercise.getID());
      onButton.addClickHandler(handler);
      onButton.setActive(isActive);
      return onButton;
    }

    /**
     * @seex #getDropDown(com.google.gwt.user.client.ui.Panel, boolean, java.util.Map, java.util.Map, java.util.List, java.util.List)
     * @param rightSide
     * @param malesMap
     * @param dropdown
     * @param maleUsers
     * @param title
     * @paramx includeDialect
     */
/*    private void addChoices(final Panel rightSide,
                            final Map<MiniUser, List<AudioAttribute>> malesMap,
                            final Dropdown dropdown, List<MiniUser> maleUsers, String title//, final boolean includeDialect
    ) {
      int count = 0;
      for (final MiniUser male : maleUsers) {
        if (male.getId() != DEFAULT_USER) count++;
        NavLink widget2 = new NavLink(getChoiceTitle(title, male*//*,includeDialect*//*));
        widget2.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            respondToGenderChoice(rightSide, malesMap, male, dropdown);
          }
        });
        dropdown.add(widget2);
        if (count == 1) break; // evil
      }
    }*/

    /**
     * @see #addChoices
     * @param rightSide
     * @param malesMap
     * @param male
     * @param dropdown
     */
    private void respondToGenderChoice(Panel rightSide, Map<MiniUser, List<AudioAttribute>> malesMap, MiniUser male, Dropdown dropdown) {
      rightSide.clear();
      addRegularAndSlow(rightSide, malesMap.get(male));
      dropdown.setText(getChoiceTitle(male.isMale() ? MALE : FEMALE, male/*, includeDialect*/));
    }

    private String getChoiceTitle(String title, MiniUser male//, boolean includeDialect
    ) {
      if (male.getId() == DEFAULT_USER) { // default user
        return DEFAULT_SPEAKER;
      } else {
        return title +
          " " +
         // (includeDialect ? male.getDialect() : "") +
          (controller.getProps().isAdminView() ? " (" + male.getUserID() + ")" : "")
          //+" " + "age " + male.getAge()
          ;
      }
    }

    /**
     * @see #getAfterPlayWidget()
     * @see #getGenderChoices(com.google.gwt.user.client.ui.Panel, java.util.Map, java.util.Map, java.util.Collection)
     * @see #respondToGenderChoice(com.google.gwt.user.client.ui.Panel, java.util.Map, mitll.langtest.shared.MiniUser, com.github.gwtbootstrap.client.ui.Dropdown)
     * @param vp
     * @param audioAttributes
     */
    private void addRegularAndSlow(Panel vp, Collection<AudioAttribute> audioAttributes) {
      RadioButton first = null;
      AudioAttribute firstAttr = null;

/*      System.out.println("getAfterPlayWidget : for exercise " +exercise.getID() +
        " path "+ audioPath + " attributes were " + audioAttributes);*/

      RadioButton regular = null;
      for (final AudioAttribute audioAttribute : audioAttributes) {
        String display = audioAttribute.getDisplay();
       // System.out.println("attri " + audioAttribute + " display " +display);
        final RadioButton radio = new RadioButton(GROUP + "_" + exercise.getID() + "_"+instance, display);
        radio.getElement().setId("Radio_"+ display);
        if (audioAttribute.isRegularSpeed()) {
          regular = radio;
        }
        if (first == null) {
          first = radio;
          firstAttr = audioAttribute;
        }
        addAudioRadioButton(vp, radio, audioAttribute.getAudioRef());

        radio.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            showAudio(audioAttribute);
            controller.logEvent(radio, "RadioButton", exerciseID, "Selected audio " + audioAttribute.getAudioRef());
          }
        });
      }

      if (regular != null) {
        regular.setValue(true);
      } else if (first != null) {
        first.setValue(true);
      } else if (!audioAttributes.isEmpty()) {
        System.err.println("no radio choice got selected??? ");
      }

      if (firstAttr != null) {
       // System.out.println("GoodwaveExercisePanel.addRegularAndSlow showing " +firstAttr);
        final AudioAttribute ffirstAttr = firstAttr;
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            showAudio(ffirstAttr);
          }
        });
      } else System.err.println("huh? no attribute ");
    }

    protected void addNoRefAudioWidget(Panel vp) { vp.add(new Label(NO_REFERENCE_AUDIO)); }
    protected boolean hasAudio() {   return !exercise.getAudioAttributes().isEmpty();   }
    protected void addAudioRadioButton(Panel vp, RadioButton fast, String audioPath) { vp.add(fast); }

    private void showAudio(AudioAttribute audioAttribute) {
      doPause();    // if the audio is playing, stop it

     // System.out.println("GoodwaveExercisePanel.showAudio " +audioAttribute);
      getImagesForPath(audioAttribute.getAudioRef());
    }
  }
}
