package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.custom.MyFlashcardExercisePanelFactory;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends HorizontalPanel implements AudioAnswerListener {
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
    "Do you have a flashblocker? Please add this site to its whitelist.</font>";

  protected static final int DELAY_MILLIS = 1000;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;

  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  private static final String FEEDBACK = "PLAY ON MISTAKE";
  private static final String ON = "On";
  private static final String OFF = "Off";
  private static final String SHOW = "SHOW";
  private static final String ENGLISH = "English";
  private static final String PLAY = "PLAY";
  private static final String BOTH = "Both";
  private static final int LEFT_MARGIN_FOR_FOREIGN_PHRASE = 17;

  private final CommonExercise exercise;

  private Heading recoOutput;
  private final MyFlashcardExercisePanelFactory.MySoundFeedback soundFeedback;
  Widget cardPrompt;
  private final boolean addKeyBinding;
  private final ExerciseController controller;
  private final ControlState controlState;
  private final Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;
  private SoundFeedback.EndListener endListener;
  protected final String instance;

  /**
   *
   *
   * @param e
   * @param service
   * @param controller
   * @param soundFeedback
   * @param endListener
   * @param instance
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#StatsPracticePanel(mitll.langtest.shared.CommonExercise)
   *
   */
  public BootstrapExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, boolean addKeyBinding,
                                final ControlState controlState,
                                MyFlashcardExercisePanelFactory.MySoundFeedback soundFeedback,
                                SoundFeedback.EndListener endListener,
                                String instance) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    this.endListener = endListener;
    this.instance = instance;
  //  addStyleName("centeringPractice");
   // System.out.println("BootstrapExercisePanel.instance = " + instance);

    controlState.setStorage(new KeyStorage(controller));

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    this.soundFeedback = soundFeedback;

    Panel contentMiddle = getMiddlePrompt(e);
    mainContainer = contentMiddle;

    DivWidget belowDiv = new DivWidget();
    belowDiv.addStyleName("topFiveMargin");
    Panel threePartContent = getThreePartContent(controlState, contentMiddle, belowDiv);
    add(threePartContent);

    if (controller.isRecordingEnabled()) {
      addRecordingAndFeedbackWidgets(e, service, controller, contentMiddle);
    }
    else {
      // TODO do something else like RapidRote
    }
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
    getElement().setId("BootstrapExercisePanel");

    addWidgetsBelow(belowDiv);
    if (controlState.isAudioOn() && mainContainer.isVisible()) {
      playRef();
    }
  }

  /**
   * Left side, middle content, and right side
   * @param controlState
   * @param contentMiddle
   * @param belowDiv
   * @return
   */
  private Panel getThreePartContent(ControlState controlState, Panel contentMiddle, DivWidget belowDiv) {
    Panel horiz = new HorizontalPanel();
    horiz.add(leftState = getLeftState());

    Grid grid = new Grid(2, 1);
    grid.setWidget(0, 0, contentMiddle);
    grid.setWidget(1, 0, belowDiv);
    horiz.add(grid);

    rightColumn = getRightColumn(controlState);
    horiz.add(rightColumn);
    return horiz;
  }

  private Panel getMiddlePrompt(CommonExercise e) {
    cardPrompt = getCardPrompt(e);
    cardPrompt.getElement().setId("cardPrompt");

    DivWidget contentMiddle = new DivWidget();
    contentMiddle.addStyleName("cardBorderShadow");
    contentMiddle.addStyleName("minWidthFifty");
    contentMiddle.add(cardPrompt);

    return contentMiddle;
  }

  protected void setMainContentVisible(boolean vis) {
    leftState.setVisible(vis);
    mainContainer.setVisible(vis);
    rightColumn.setVisible(vis);
  }

  private Panel getRightColumn(final ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    if (!isSiteEnglish()) {
      rightColumn.add(getShowGroup(controlState));
    }
    rightColumn.add(getAudioGroup(controlState));
    rightColumn.add(getFeedbackGroup(controlState));
    final Button shuffle = new Button("Shuffle");
    shuffle.setToggle(true);
    shuffle.setIcon(IconType.RANDOM);
    shuffle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean shuffleOn = !shuffle.isToggled();

        //System.out.println("shuffle onClick " + shuffleOn);
        controlState.setSuffleOn(shuffleOn);
        gotShuffleClick(shuffleOn);
      }
    });
    shuffle.setActive(controlState.isShuffle());

    rightColumn.add(shuffle);

    rightColumn.addStyleName("leftTenMargin");
    return rightColumn;
  }

  protected void gotShuffleClick(boolean b) {

  }

  protected Panel getLeftState() { return new Heading(6, ""); }
  protected void addWidgetsBelow(Panel toAddTo) {}

  /**
   * @see #getAudioGroup(ControlState)
   * @see #getQuestionContent
   */
  private void playRefLater() {
   // System.out.println("playRefLater... ---------- " + exercise.getID());
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        playRef();
      }
    });
  }

  /**
   * @see #getRightColumn(ControlState)
   * @param controlState
   * @return
   */
  private ControlGroup getAudioGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(PLAY + " " + controller.getLanguage().toUpperCase());
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);
    Button onButton = new Button(ON);
    onButton.getElement().setId(PLAY+"_On");
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.isAudioOn()) {
          playRefLater();
        }
        controlState.setAudioOn(true);
      }
    });
    onButton.setActive(controlState.isAudioOn());


    Button offButton = new Button(OFF);
    offButton.getElement().setId(PLAY+"_Off");
    controller.register(offButton, exercise.getID());

    buttonGroup.add(offButton);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioOn(false);
      }
    });
    offButton.setActive(!controlState.isAudioOn());

    return group;
  }

  private ControlGroup getFeedbackGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(FEEDBACK);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    Button onButton = makeGroupButton(buttonGroup, ON);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(true);
        //System.out.println("now on " + controlState);
      }
    });
    onButton.setActive(controlState.isAudioFeedbackOn());

    Button offButton = makeGroupButton(buttonGroup, OFF);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(false);
        //System.out.println("now off " + controlState);
      }
    });
    offButton.setActive(!controlState.isAudioFeedbackOn());

    return group;
  }

  private Button makeGroupButton(ButtonGroup buttonGroup,String title) {
    Button onButton = new Button(title);
    onButton.getElement().setId(FEEDBACK+"_"+title);
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);
    return onButton;
  }

  private ControlGroup getShowGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup(SHOW);
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setVertical(true);
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    buttonGroup.add(getOn(controlState));
    buttonGroup.add(getOff(controlState));
    buttonGroup.add(getBoth(controlState));

    return group;
  }

  private Button getOn(final ControlState controlState) {
    Button onButton = new Button(controller.getLanguage());
    onButton.getElement().setId("Show_On_" + controller.getLanguage());
    controller.register(onButton, exercise.getID());

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.getShowState().equals(ControlState.FOREIGN)) {
          controlState.setShowState(ControlState.FOREIGN);
          //System.out.println("now on " + controlState);
          showEnglishOrForeign();
        }

      }
    });
    onButton.setActive(controlState.showForeign() && !controlState.showBoth());
    return onButton;
  }

  private Button getOff(final ControlState controlState) {
    Button showEnglish = new Button(ENGLISH);
    showEnglish.getElement().setId("Show_English");
    controller.register(showEnglish, exercise.getID());

    showEnglish.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.getShowState().equals(ControlState.ENGLISH)) {
          controlState.setShowState(ControlState.ENGLISH);
          // System.out.println("now  " + controlState);
          showEnglishOrForeign();
        }
      }
    });
    showEnglish.setActive(controlState.showEnglish() && !controlState.showBoth());
    return showEnglish;
  }

  private Button getBoth(final ControlState controlState) {
    Button both = new Button(BOTH);
    both.getElement().setId("Show_Both_" + controller.getLanguage() + "_and_English");
    controller.register(both, exercise.getID());

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.getShowState().equals(ControlState.BOTH)) {
          controlState.setShowState(ControlState.BOTH);
         // System.out.println("now  " + controlState);
          showEnglishOrForeign();
        }
      }
    });
    both.setActive(controlState.showBoth());
    return both;
  }

  /**
   * Make a row to show the question content (the prompt or stimulus)
   * and the space bar and feedback widgets beneath it.
   *
   * @param e
   * @return
   * @see #BootstrapExercisePanel
   */
  private Panel getCardPrompt(CommonExercise e) {
    Panel questionContent = getQuestionContent(e);
    questionContent.addStyleName("cardContent");
    return questionContent;
  }

  private Widget english;
  private Widget foreign;

  /**
   * If there's no english sentence, we use the foreign language phrase
   * @param e
   * @return
   */
  private Panel getQuestionContent(CommonExercise e) {
    String foreignSentence = e.getForeignLanguage();

    String englishSentence = e.getEnglish();
    boolean usedForeign = false;
    if (englishSentence.isEmpty()) {
      englishSentence = foreignSentence;
      usedForeign = true;
    }
    Heading englishHeading = new Heading(1, englishSentence);
    englishHeading.getElement().setId("EnglishPhrase");
    FocusPanel widgets = new FocusPanel();
    widgets.add(englishHeading);
    english = widgets;
    english.getElement().setId("EnglishPhrase_container");

    DivWidget div = new DivWidget();
    div.addStyleName("blockStyle");
    div.add(english);

    foreign = getForeignLanguageContent(foreignSentence,  e.hasRefAudio());

    if (!usedForeign) {
      div.add(foreign);
    }
    if (isSiteEnglish()) {
      if (getRefAudioToPlay() != null) {
        addAudioBindings2(widgets);
      }
    }

    showEnglishOrForeign();

    return div;
  }

  private boolean isSiteEnglish() {
    return controller.getLanguage().equals("English");
  }

  /**
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param foreignSentence
   * @param hasRefAudio
   * @return
   */
  private Widget getForeignLanguageContent(String foreignSentence, boolean hasRefAudio) {
    Heading heading = new Heading(1, foreignSentence);
    heading.getElement().setId("FLPhrase");
    heading.getElement().getStyle().setMarginLeft(LEFT_MARGIN_FOR_FOREIGN_PHRASE, Style.Unit.PX);
    FocusPanel container = new FocusPanel();   // TODO : remove???
    container.getElement().setId("FLPhrase_container");

    Panel hp = new HorizontalPanel();
    hp.add(heading);
    if (hasRefAudio) {
      Icon w = new Icon(IconType.VOLUME_UP);
      w.setSize(IconSize.TWO_TIMES);
      Panel simple = new SimplePanel();
      simple.add(w);
      simple.addStyleName("leftTenMargin");
      hp.add(simple);
    }
    DivWidget centeringRow = getCenteringRow();
    centeringRow.add(hp);
    container.add(centeringRow);

    addAudioBindings2(container);
    return container;
  }

  DivWidget getCenteringRow() {
    DivWidget status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    return status;
  }

  /**
   * @see #getForeignLanguageContent(String, boolean)
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param focusPanel
   */
  private void addAudioBindings2(final FocusPanel focusPanel) {
    focusPanel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        playRefLater();
      }
    });
    focusPanel.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        focusPanel.addStyleName("mouseOverHighlight");
      }
    });
    focusPanel.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        focusPanel.removeStyleName("mouseOverHighlight");
      }
    });
    focusPanel.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        focusPanel.setFocus(false);
      }
    });
  }

  private void showEnglishOrForeign() {
   // System.out.println("show english or foreign " + controlState);
    if (controlState.showBoth()) {
      showBoth();
    } else if (controlState.showEnglish()) {
      english.setHeight("100%");
      english.setVisible(true);
      foreign.setVisible(false);
    } else if (controlState.showForeign()) {
      foreign.setHeight("100%");
      english.setVisible(false);
      foreign.setVisible(true);
    }
  }

  private void showBoth() {
    english.setHeight("50%");
    foreign.setHeight("50%");
    english.setVisible(true);
    foreign.setVisible(true);
  }

  private final DivWidget scoreFeedbackRow = new DivWidget();

  /**
   * Three rows below the stimulus word/expression:<p></p>
   * record space bar image <br></br>
   * reco feedback - whether the recorded audio was correct/incorrect, etc.  <br></br>
   * score feedback - pron score
   *
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see #BootstrapExercisePanel
   */
  void addRecordingAndFeedbackWidgets(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                      Panel toAddTo) {
    // add answer widget to do the recording
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(e, service, controller);
    toAddTo.add(answerAndRecordButtonRow);

    if (controller.getProps().showFlashcardAnswer()) {
      toAddTo.add(getRecoOutputRow());
    }

    scoreFeedbackRow.addStyleName("bottomFiveMargin");
    toAddTo.add(scoreFeedbackRow);
  }

  private RecordButtonPanel answerWidget;
  private Widget button;

  private Widget getAnswerAndRecordButtonRow(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    //System.out.println("BootstrapExercisePanel.getAnswerAndRecordButtonRow = " + instance);
    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1, addKeyBinding, instance);
    this.answerWidget = answerWidget;
    button = answerWidget.getRecordButton();

    return getRecordButtonRow(button);
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @see BootstrapExercisePanel#addRecordingAndFeedbackWidgets(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel)
   */
  Panel getRecordButtonRow(Widget recordButton) {
    Panel recordButtonRow = getCenteredWrapper(recordButton);
    recordButtonRow.getElement().setId("recordButtonRow");

    recordButtonRow.addStyleName("leftTenMargin");
    recordButtonRow.addStyleName("rightTenMargin");
    recordButtonRow.getElement().getStyle().setMarginRight(10, Style.Unit.PX);

    return recordButtonRow;
  }

  private Panel getCenteredWrapper(Widget recordButton) {
    Panel recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    return recordButtonRow;
  }

  /**
   * Center align the text feedback (correct/incorrect)
   *
   * @return
   */
  private Panel getRecoOutputRow() {
    recoOutput = new Heading(3, "Answer");
    recoOutput.addStyleName("cardHiddenText2");   // same color as background so text takes up space but is invisible
    recoOutput.getElement().getStyle().setColor("#ffffff");

    Panel recoOutputRow = new FluidRow();
    Panel recoOutputContainer = new Paragraph();
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, recoOutputContainer));

/*    recoOutput = new Heading(3, "Answer");
    recoOutput.addStyleName("cardHiddenText");   // same color as background so text takes up space but is invisible
    recoOutput.getElement().getStyle().setProperty("color", "#ebebec");*/

    recoOutputContainer.add(recoOutput);
    recoOutputRow.getElement().setId("recoOutputRow");

    return recoOutputRow;
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @param addKeyBinding
   * @param instance
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   */
  RecordButtonPanel getAnswerWidget(final CommonExercise exercise, LangTestDatabaseAsync service,
                                    ExerciseController controller, final int index, final boolean addKeyBinding, String instance) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, "avp", instance) {
      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
       // System.out.println("makeRecordButton : using " + instance);
        return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, addKeyBinding, controller, BootstrapExercisePanel.this.instance) {
          @Override
          protected void start() {
            controller.logEvent(this, "AVP_RecordButton", exercise.getID(), "Start_Recording");
            super.start();
            recordingStarted();
          }

          @Override
          public void stop() {
            controller.logEvent(this, "AVP_RecordButton", exercise.getID(), "Stop_Recording");
            super.stop();
          }
        };
      }
    };
  }

  protected void recordingStarted() {

  }

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @see
   */
  void showPronScoreFeedback(double score) {
    scoreFeedbackRow.add(showScoreFeedback("Score ", score));
  }


  /**
   * @param pronunciationScore
   * @param score
   * @seex #showCRTFeedback(Double, mitll.langtest.client.sound.SoundFeedback, String, boolean)
   * @paramx centerVertically
   * @paramx useShortWidth
   * @see BootstrapExercisePanel#showPronScoreFeedback(double)
   */
  private ProgressBar showScoreFeedback(String pronunciationScore, double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    ProgressBar scoreFeedback = new ProgressBar();
    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText(pronunciationScore + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);

/*    if (centerVertically) {
      DOM.setStyleAttribute(scoreFeedback.getElement(), "marginTop", "18px");
      DOM.setStyleAttribute(scoreFeedback.getElement(), "marginBottom", "10px");
    }
    DOM.setStyleAttribute(scoreFeedback.getElement(), "marginLeft", "10px");*/
    return scoreFeedback;
  }

  void clearFeedback() {  scoreFeedbackRow.clear(); }
  private Heading getRecoOutput() {
    return recoOutput;
  }

  private MyFlashcardExercisePanelFactory.MySoundFeedback getSoundFeedback() {
    return soundFeedback;
  }

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer
   * @param result
   */
  public void receivedAudioAnswer(final AudioAnswer result) {
    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;
    boolean correct = result.isCorrect();
    final double score = result.getScore();

    boolean badAudioRecording = result.getValidity() != AudioAnswer.Validity.OK;
    System.out.println("receivedAudioAnswer: correct " + correct + " pron score : " + score +
      " has ref " + hasRefAudio + " bad audio " + badAudioRecording + " result " + result);

    String feedback = "";
    if (badAudioRecording) {
      showPopup(result.getValidity().getPrompt(), button);
      initRecordButton();
      clearFeedback();
    } else if (correct) {
      showCorrectFeedback(score);
    } else {   // incorrect!!
      feedback = showIncorrectFeedback(result, score, hasRefAudio);
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      System.out.println("\treceivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html, Widget button) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.showRelativeTo(button);

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(HIDE_DELAY);
  }

  private void showCorrectFeedback(double score) {
    showPronScoreFeedback(score);
    getSoundFeedback().queueSong(SoundFeedback.CORRECT);
  }

  /**
   * If there's reference audio, play it and wait for it to finish.
   *
   * @param result
   * @param score
   * @param hasRefAudio
   * @see #receivedAudioAnswer
   */
  private String showIncorrectFeedback(AudioAnswer result, double score, boolean hasRefAudio) {
    if (result.isSaidAnswer()) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(score);
    }
   // System.out.println("showIncorrectFeedback : result " + result + " score " + score + " has ref " + hasRefAudio);

    String correctPrompt = getCorrectDisplay();
    if (hasRefAudio) {
      if (controlState.isAudioFeedbackOn()) {
        String path = getRefAudioToPlay();
        if (path == null) {
          playIncorrect(); // this should never happen
        } else if (!preventFutureTimerUse) {
          playRefAndGoToNext(path);
        }
      } else {
        playIncorrect();
        goToNextAfter(1000);
      }
    } else {
      tryAgain();
    }

    if (controller.getProps().isDemoMode()) {
      correctPrompt = "Heard: " + result.getDecodeOutput() + "<p>" + correctPrompt;
    }
    Heading recoOutput = getRecoOutput();
    if (recoOutput != null && controlState.isAudioFeedbackOn()) {
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
    }
    return correctPrompt;
  }

  private void playIncorrect() {
    getSoundFeedback().queueSong(SoundFeedback.INCORRECT);
  }

  /**
   * @see #playRef()
   * @return
   */
  private String getRefAudioToPlay() {
    //System.out.println(getElement().getId() + " playing audio for " +exercise.getID());
    String path = exercise.getRefAudio();
    if (path == null) {
      path = exercise.getSlowAudioRef(); // fall back to slow audio
    }
    return path;
  }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   * @paramx correctPrompt
   * @param path
   */
  private void playRefAndGoToNext(String path) {
    path = getPath(path);

    getSoundFeedback().queueSong(path, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        endListener.songStarted();
      }

      @Override
      public void songEnded() {
        endListener.songEnded();
        loadNext();
      }
    });
  }

  /**
   * @see #playRefLater()
   */
  private void playRef() {
    String refAudioToPlay = getRefAudioToPlay();

    if (refAudioToPlay != null) {
      playRef(refAudioToPlay);
    }
  }


  /**
   * @see #playRef()
   * @param path
   */
  private void playRef(String path) {
  //  System.out.println("playRef... ---------- " + exercise.getID() + " path " + path );

    path = getPath(path);
    final Widget textWidget = isSiteEnglish() ? english : foreign;
    getSoundFeedback().queueSong(path, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        textWidget.addStyleName("playingAudioHighlight");
        endListener.songStarted();
      }

      @Override
      public void songEnded() {
        removePlayingHighlight(textWidget);
        endListener.songEnded();
      }
    });
  }

  protected void removePlayingHighlight() {
    final Widget textWidget = isSiteEnglish() ? english : foreign;
    textWidget.removeStyleName("playingAudioHighlight");
  }

  protected void removePlayingHighlight(Widget textWidget) {
    textWidget.removeStyleName("playingAudioHighlight");
  }

  private String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  private String ensureForwardSlashes(String wavPath) { return wavPath.replaceAll("\\\\", "/"); }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  private void tryAgain() {
    playIncorrect();

    Timer t = new Timer() {
      @Override
      public void run() {
        initRecordButton();
      }
    };
    int incorrectDelay = DELAY_MILLIS_LONG;
    t.schedule(incorrectDelay);
  }

  private void goToNextAfter(int delay) {
    loadNextOnTimer(controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : delay);
  }

  private int getFeedbackLengthProportionalDelay(String feedback) {
    int mult1 = feedback.length() / DELAY_CHARACTERS;
    int mult = Math.max(3, mult1);
    return mult * DELAY_MILLIS;
  }

  private String getCorrectDisplay() {
    String refSentence = exercise.getForeignLanguage();
    String translit = exercise.getTransliteration().length() > 0 ? "<br/>(" + exercise.getTransliteration() + ")" : "";
    return refSentence + translit;
  }

  private Timer currentTimer = null;
  /**
   * @param correct
   * @param feedback
   * @see #receivedAudioAnswer(mitll.langtest.shared.AudioAnswer)
   */
  protected void nextAfterDelay(boolean correct, String feedback) {
    if (NEXT_ON_BAD_AUDIO) {
      System.out.println("doing nextAfterDelay : correct " + correct + " feedback " + feedback);
      // Schedule the timer to run once in 1 seconds.
      Timer t = new Timer() {
        @Override
        public void run() {
          loadNext();
        }
      };
      int incorrectDelay = DELAY_MILLIS_LONG;
      if (!feedback.isEmpty()) {
        int delay = getFeedbackLengthProportionalDelay(feedback);
        incorrectDelay += delay;
        System.out.println("nextAfterDelay Delay is " + incorrectDelay + " len " + feedback.length());
      }
      t.schedule(controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : correct ? DELAY_MILLIS : incorrectDelay);
    } else {
      System.out.println("doing nextAfterDelay : correct " + correct + " feedback " + feedback);

      if (correct) {
        // go to next item
        loadNextOnTimer(100);//DELAY_MILLIS);
      } else {
        initRecordButton();
        clearFeedback();
      }
    }
  }

  protected void loadNextOnTimer(final int delay) {
    if (!preventFutureTimerUse) {
     // if (delay > 100) {
     //   System.out.println("loadNextOnTimer ----> load next on " + delay);
     // }
      Timer t = new Timer() {
        @Override
        public void run() {
          currentTimer = null;
          loadNext();
        }
      };
      currentTimer = t;
      t.schedule(delay);
    } else {
      System.out.println("\n\n\n----> ignoring next ");
    }
  }

  private boolean preventFutureTimerUse = false;
  protected void cancelTimer() {
    removePlayingHighlight();

    preventFutureTimerUse = true;
    if (currentTimer != null) currentTimer.cancel();
  }
  private void initRecordButton() {  answerWidget.initRecordButton();  }

  /**
   * @see #nextAfterDelay(boolean, String)
   */
  protected void loadNext() {
   // System.out.println("loadNext after " + exercise.getID());
    controller.getExerciseList().loadNextExercise(exercise);
  }
}
