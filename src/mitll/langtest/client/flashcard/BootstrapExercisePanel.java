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
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.KeyStorage;
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
 // private static final String PRONUNCIATION_SCORE = "Pronunciation score ";

  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  private static final String FEEDBACK = "PLAY WORD ON ERROR";
  private static final String ON = "On";
  private static final String OFF = "Off";
  private static final String SHOW = "SHOW";
  private static final String ENGLISH = "English";
  private static final String PLAY = "PLAY";
  private static final String BOTH = "Both";

  private final CommonExercise exercise;

  private Heading recoOutput;
  final SoundFeedback soundFeedback;
  Widget cardPrompt;
  private final boolean addKeyBinding;
  private final ExerciseController controller;
  private final ControlState controlState;
  private final Panel mainContainer;
  private Panel leftState;
  private Panel rightColumn;

  /**
   *
   *
   * @param e
   * @param service
   * @param controller
   * @see mitll.langtest.client.custom.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, long)
   */
  public BootstrapExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, boolean addKeyBinding,
                                final ControlState controlState) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    controlState.setStorage(new KeyStorage(controller));

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    DivWidget contentMiddle = getMiddlePrompt(e);
    mainContainer = contentMiddle;

    DivWidget belowDiv = new DivWidget();
    Panel horiz = getThreePartContent(controller, controlState, contentMiddle, belowDiv);

    add(horiz);

    addRecordingAndFeedbackWidgets(e, service, controller, contentMiddle);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
    getElement().setId("BootstrapExercisePanel");

    addWidgetsBelow(belowDiv);
    if (controlState.isAudioOn()) {
      playRefLater();
    }
  }

  /**
   * Left side, middle content, and right side
   * @param controller
   * @param controlState
   * @param contentMiddle
   * @param belowDiv
   * @return
   */
  private Panel getThreePartContent(ExerciseController controller, ControlState controlState, DivWidget contentMiddle,
                                    DivWidget belowDiv) {
    Panel horiz = new HorizontalPanel();
    horiz.add(leftState = getLeftState());

    Grid grid = new Grid(2, 1);
    grid.setWidget(0, 0, contentMiddle);
    grid.setWidget(1, 0, belowDiv);
    horiz.add(grid);

    rightColumn = getRightColumn(controller, controlState);
    horiz.add(rightColumn);
    return horiz;
  }

  private DivWidget getMiddlePrompt(CommonExercise e) {
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

  private Panel getRightColumn(ExerciseController controller, ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    if (!controller.getLanguage().equalsIgnoreCase("English")) {
      rightColumn.add(getShowGroup(controlState));
    }
    rightColumn.add(getAudioGroup(controlState));
    rightColumn.add(getFeedbackGroup(controlState));

    rightColumn.addStyleName("leftTenMargin");
    return rightColumn;
  }

  protected Panel getLeftState() { return new Heading(6, ""); }
  protected void addWidgetsBelow(Panel toAddTo) {}

  /**
   * @see #getAudioGroup(ControlState)
   * @see #getQuestionContent
   */
  private void playRefLater() {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        playRef();
      }
    });
  }

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

    Button onButton = new Button(ON);
    onButton.getElement().setId(FEEDBACK+"_On");
    controller.register(onButton, exercise.getID());
    buttonGroup.add(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.setAudioFeedbackOn(true);
        //System.out.println("now on " + controlState);
      }
    });
    onButton.setActive(controlState.isAudioFeedbackOn());

    Button offButton = new Button(OFF);
    buttonGroup.add(offButton);
    offButton.getElement().setId(FEEDBACK+"_Off");
    controller.register(offButton, exercise.getID());
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
    both.getElement().setId("Show_Both_"+controller.getLanguage()+"_and_English");
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
  Panel getCardPrompt(CommonExercise e) {
    Panel questionContent = getQuestionContent(e);
    questionContent.addStyleName("cardContent");
    return questionContent;
  }

  private Widget english;
  private Widget foreign;

  Panel getQuestionContent(CommonExercise e) {
    String foreignSentence = e.getForeignLanguage(); //e.getRefSentences().iterator().next();
    DivWidget div = new DivWidget();
    div.addStyleName("blockStyle");

    String englishSentence = e.getEnglish();
    boolean usedForeign = false;
    if (englishSentence.isEmpty()) {
      englishSentence = foreignSentence;
      usedForeign = true;
    }
    english = new Heading(1, englishSentence);
    english.getElement().setId("EnglishPhrase");
    div.add(english);

    foreign = getForeignLanguageContent(foreignSentence);

    if (usedForeign) {

    }
    else {
      div.add(foreign);
    }
    if (controller.getLanguage().equals("English")) {
      if (getRefAudioToPlay() != null) {
        addAudioBindings(english);
      }
    }

    showEnglishOrForeign();

    return div;
  }

  private Widget getForeignLanguageContent(String foreignSentence) {
    Heading widgets = new Heading(1, foreignSentence);
    widgets.getElement().setId("FLPhrase");
    addAudioBindings(widgets);
    return widgets;
  }

  PopupPanel popupPanel;
  private void addAudioBindings(final Widget widgets) {
    widgets.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        playRefLater();
      }
    }, ClickEvent.getType());

    widgets.addDomHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        widgets.addStyleName("mouseOverHighlight");
        popupPanel = new PopupPanel(true);
        Icon w = new Icon(IconType.VOLUME_UP);
        popupPanel.add(w);
        int x = widgets.getAbsoluteLeft();
        int y = widgets.getAbsoluteTop()+5;
        popupPanel.setPopupPosition(x,y);
        popupPanel.show();
      }
    }, MouseOverEvent.getType());

    widgets.addDomHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        popupPanel.hide();
        popupPanel = null;
        widgets.removeStyleName("mouseOverHighlight");
      }
    }, MouseOutEvent.getType());
  }

  private void showEnglishOrForeign() {
    System.out.println("show english or foreign " + controlState);
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
   // System.out.println("BootstrapExercisePanel.addRecordingAndFeedbackWidgets");
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
//    System.out.println("BootstrapExercisePanel.getAnswerAndRecordButtonRow");

    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1, addKeyBinding);
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
    DOM.setStyleAttribute(recordButtonRow.getElement(), "marginRight", "10px");

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
    DOM.setStyleAttribute(recoOutput.getElement(), "color", "#ffffff");

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
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   */
  RecordButtonPanel getAnswerWidget(final CommonExercise exercise, LangTestDatabaseAsync service,
                                    ExerciseController controller, final int index, final boolean addKeyBinding) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, "avp") {
      @Override
      protected RecordButton makeRecordButton(final ExerciseController controller, String buttonTitle) {
        return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, addKeyBinding) {
          @Override
          protected void start() {
            controller.logEvent(this,"AVP_RecordButton",exercise.getID(),"Start_Recording");
            super.start();
          }

          @Override
          public void stop() {
            controller.logEvent(this,"AVP_RecordButton",exercise.getID(),"Stop_Recording");
            super.stop();
          }
        };
      }
    };
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

  private SoundFeedback getSoundFeedback() {
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
    getSoundFeedback().playCorrect();
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
    if (result.isSaidAnswer()/* && result.isCorrect()*/) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(score);
    }
    //boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();
    System.out.println("showIncorrectFeedback : " +
      //"playing synonym audio " + exercise.getSynonymAudioRefs()+
      " result " + result + " score " + score + " has ref " + hasRefAudio
      //+ " hasSynonymAudio " + hasSynonymAudio
    );

    String correctPrompt = getCorrectDisplay();
    if (hasRefAudio) {
 /*     if (hasSynonymAudio) {
        List<String> toPlay = new ArrayList<String>(exercise.getSynonymAudioRefs());
        //  System.out.println("showIncorrectFeedback : playing " + toPlay);
        if (controlState.isAudioFeedbackOn()) {
          playAllAudio(correctPrompt, toPlay);
        } else {
          nextAfterDelay(result.isCorrect(), correctPrompt);
        }
      } else {*/
        if (controlState.isAudioFeedbackOn()) {
          String path = getRefAudioToPlay();
          if (path == null) {
            getSoundFeedback().playIncorrect(); // this should never happen
          } else {
            playRefAndGoToNext(correctPrompt, path);
          }
        } else {
          getSoundFeedback().playIncorrect(); // this should never happen

          goToNextAfter(1000);
        }
   //   }
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

  private String getRefAudioToPlay() {
    String path = exercise.getRefAudio();
    if (path == null) {
      path = exercise.getSlowAudioRef(); // fall back to slow audio
    }
    return path;
  }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   * @param correctPrompt
   * @param path
   */
  private void playRefAndGoToNext(String correctPrompt, String path) {
    path = getPath(path);
    final String fcorrectPrompt = correctPrompt;

    getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
      @Override
      public void songEnded() {
        goToNextItem(fcorrectPrompt);
      }
    }, false);
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
    path = getPath(path);
    final Widget textWidget = controller.getLanguage().equalsIgnoreCase("English") ? english : foreign;
    textWidget.addStyleName("playingAudioHighlight");
    getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
      @Override
      public void songEnded() {
        textWidget.removeStyleName("playingAudioHighlight");
      }
    });
  }

  private String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  /**
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  private void tryAgain() {
    getSoundFeedback().playIncorrect();

    Timer t = new Timer() {
      @Override
      public void run() {
        initRecordButton();
      }
    };
    int incorrectDelay = DELAY_MILLIS_LONG;
    t.schedule(incorrectDelay);
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  /**
   * @param infoToShow
   * @param toPlay
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
/*  private void playAllAudio(final String infoToShow, final List<String> toPlay) {
    if (toPlay.isEmpty()) {
      goToNextItem(infoToShow);
    } else {
      String path = toPlay.get(0);
      if (path == null) {
        System.err.println("skipping first null audio!");
        goToNextItem(infoToShow);
      } else {
        path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;

        System.out.println("playAllAudio : " + toPlay.size() + " playing " + path);
        getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
          @Override
          public void songEnded() {
            toPlay.remove(0);

            System.out.println("\tplayAllAudio : songEnded " + toPlay.size() + " items left.");

            if (!toPlay.isEmpty()) {
              playAllAudio(infoToShow, toPlay);
            } else {
              goToNextItem(infoToShow);
            }
          }
        }, false);
      }
    }
  }*/

  /**
   * @param infoToShow longer text means longer delay while user reads it
   * @see #playAllAudio(String, java.util.List)
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  void goToNextItem(String infoToShow) {
    int delay = getFeedbackLengthProportionalDelay(infoToShow);
    System.out.println("goToNextItem : using delay " + delay + " info " + infoToShow);
    goToNextAfter(delay);
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

/*    if (refSentence == null || refSentence.length() == 0) {
      List<Exercise.QAPair> questions = exercise.getForeignLanguageQuestions();
      refSentence = getAltAnswers(questions);
      List<Exercise.QAPair> eq = exercise.getEnglishQuestions();
      translit = "<br/>(" + getAltAnswers(eq) + " )";
    }
    boolean hasSynonyms = !exercise.getSynonymSentences().isEmpty();
    if (hasSynonyms) {
      refSentence = "";
      for (int i = 0; i < exercise.getSynonymSentences().size(); i++) {
        String synonym = exercise.getSynonymSentences().get(i);
        String translit2 = exercise.getSynonymTransliterations().get(i);
        refSentence += synonym + "(" + translit2 + ") or ";
      }
      refSentence = refSentence.substring(0, refSentence.length() - " or ".length());
    }*/
  //  return refSentence + (hasSynonyms ? "" : translit);
    return refSentence + (false ? "" : translit);
  }

/*  private String getAltAnswers(List<Exercise.QAPair> questions) {
    Exercise.QAPair qaPair = questions.get(0);
    StringBuilder b = new StringBuilder();
    for (String alt : qaPair.getAlternateAnswers()) {
      if (alt.trim().length() > 0) {
        b.append(alt).append(", ");
      }
    }
    if (b.length() > 0) {
      return b.toString().substring(0, b.length() - 2);
    } else return "";
  }*/

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
        loadNextOnTimer(DELAY_MILLIS);
      } else {
        initRecordButton();
        clearFeedback();
      }
    }
  }

  protected void loadNextOnTimer(final int delay) {
    Timer t = new Timer() {
      @Override
      public void run() {
        currentTimer = null;
        loadNext();
      }
    };
    currentTimer = t;
    t.schedule(delay);
  }

  protected void cancelTimer() {
    if (currentTimer != null) currentTimer.cancel();
  }

  void initRecordButton() {  answerWidget.initRecordButton();  }

  /**
   * @see #nextAfterDelay(boolean, String)
   */
  protected void loadNext() {
    System.out.println("loadNext after " + exercise.getID());
    controller.getExerciseList().loadNextExercise(exercise);
  }
}
