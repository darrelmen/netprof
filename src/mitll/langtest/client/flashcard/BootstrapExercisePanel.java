package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
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
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import java_cup.version;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends HorizontalPanel implements AudioAnswerListener {
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private static final int DELAY_MILLIS = 1000;
  private static final int DELAY_MILLIS_LONG = 3000;
  private static final int LONG_DELAY_MILLIS = 3500;
  private static final int DELAY_CHARACTERS = 40;
  private static final int HIDE_DELAY = 2500;

  private static final boolean NEXT_ON_BAD_AUDIO = false;
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";

  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  private static final int SET_SIZE = 10;
  private static final int MAX_SET_SIZE = 50;
  // public static final int CONTENT_COLUMNS = 12;

  private final Exercise exercise;

  private Heading recoOutput;
  protected SoundFeedback soundFeedback;
  protected final Widget cardPrompt;
  protected Panel recoOutputContainer;
  private final boolean addKeyBinding;
  private ExerciseController controller;
  private boolean continueToNext = true;
  private ControlState controlState;

  /**
   * @param e
   * @param service
   * @param controller
   * @param addKeyBinding
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @see mitll.langtest.client.custom.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, int feedbackHeight, boolean addKeyBinding,
                                final ControlState controlState) {
    this.addKeyBinding = addKeyBinding;
    this.exercise = e;
    this.controller = controller;
    this.controlState = controlState;
    //System.out.println("got " + controlState);
    // addStyleName("cardDisplayTable");
    //  addStyleName("minWidth");

    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    Widget helpRow = getHelpRow(controller);
    if (helpRow != null) add(helpRow);
    cardPrompt = getCardPrompt(e, controller);
    cardPrompt.getElement().setId("cardPrompt");
    //          addStyleName("floatLeft");
    Panel horiz = new HorizontalPanel();
    //DivWidget horiz = new DivWidget();
    //horiz.addStyleName("trueInlineStyle");
    // DivWidget horiz = new FluidRow();
    //horiz.addStyleName("displayTableRow");
    //   horiz.addStyleName("positionRelative");
    // addStyleName("positionRelative");
    //horiz.addStyleName("minWidthFifty");
    // add(horiz);
    //cardPrompt.addStyleName("floatLeft");
    Panel rightColumn = getRightColumn(controller, controlState);
    //rightColumn.addStyleName("floatRight");
    // rightColumn.addStyleName("twoDivsRow");

    // Panel leftColumn = new VerticalPanel();
    DivWidget contentMiddle = new DivWidget();
    DivWidget belowDiv = new DivWidget();
    // Panel contentMiddle = new VerticalPanel();

    //leftColumn.addStyleName("exerciseBackground");
    contentMiddle.addStyleName("cardBorderShadow");
    //leftColumn.addStyleName("floatLeft");
    // leftColumn.addStyleName("twoDivsRow");
    // contentMiddle.addStyleName("middleColumn");
    contentMiddle.addStyleName("minWidthFifty");

    //  Panel wrapper = getCenteredWrapper(cardPrompt);

    // contentMiddle.add(getCentered(cardPrompt));
    contentMiddle.add(cardPrompt);
    // leftColumn.setWidth("100%");

    //horiz.add(new Column(8,leftColumn));
    //horiz.add(new Column(2,rightColumn));
    Panel leftState = getLeftState();
    //leftState.addStyleName("leftColumn");
    horiz.add(leftState);
    Grid grid = new Grid(2, 1);
    grid.setWidget(0, 0, contentMiddle);
    grid.setWidget(1, 0, belowDiv);
    horiz.add(grid);
    horiz.add(rightColumn);
    add(horiz);

    //doDockLayout();
    //  setWidth("100%");

    // Panel leftState = getLeftState();
    addRecordingAndFeedbackWidgets(e, service, controller, feedbackHeight, contentMiddle);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
    getElement().setId("BootstrapExercisePanel");

    addWidgetsBelow(belowDiv);
    if (controlState.audioOn) {
      playRefLater();
    }
  }

  public void doDockLayout() {
    DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Style.Unit.PX);
    //dockLayoutPanel.addNorth(new Heading(6,"Try this set"), 2);
    //dockLayoutPanel.addSouth(new Heading(6, "Footer"), 2);
    //dockLayoutPanel.addEast(rightColumn, 4);
    DivWidget div = new DivWidget();
    div.setWidth("100px");
    div.setHeight("100px");
    div.add(new Heading(6, "East"));
    dockLayoutPanel.addEast(div, 400);
    //  Panel leftState = getLeftState();
    div = new DivWidget();
    div.setWidth("100px");
    div.setHeight("100px");

    div.add(new Heading(6, "West"));
    dockLayoutPanel.addWest(div, 400);
//    dockLayoutPanel.add(leftColumn);
    div = new DivWidget();
    div.setWidth("100px");
    div.setHeight("100px");

    div.add(new Heading(6, "Center"));
    dockLayoutPanel.add(div);
    //dockLayoutPanel.setWidth("500px");
    addStyleName("positionRelative");
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        getParent().addStyleName("positionRelative");
        getParent().getParent().addStyleName("positionRelative");
      }
    });


    // DivWidget dockContainer = new DivWidget();
    // DivWidget horiz = new FluidRow();
    //horiz.addStyleName("displayTableRow");
    //dockContainer.addStyleName("positionRelative");
    //dockContainer.add(dockLayoutPanel);
//    dockLayoutPanel.forceLayout();
    add(dockLayoutPanel);
  }

  public Panel getRightColumn(ExerciseController controller, ControlState controlState) {
    Panel rightColumn = new VerticalPanel();

    // rightColumn.addStyleName("rightColumn");
    if (!controller.getLanguage().equalsIgnoreCase("English")) {
      rightColumn.add(getShowGroup(controlState));
    }
    rightColumn.add(getAudioGroup(controlState));
    rightColumn.add(getFeedbackGroup(controlState));
    rightColumn.add(getSizes(controlState));

    rightColumn.addStyleName("leftTenMargin");
    return rightColumn;
  }

/*  protected void addLeftColumn(DockLayoutPanel dockLayoutPanel) {
    //Panel panel = getLeftState();
    //dockLayoutPanel.addWest(panel, 4);
   dockLayoutPanel.addWest(new Heading(6,"West"), 4);
  }*/

  protected Panel getLeftState() {
    return new Heading(6, "");
  }

  protected void addWidgetsBelow(Panel toAddTo) {
  }

  public Widget getSizes(final ControlState controlState) {
    ControlGroup group = new ControlGroup("SET SIZE");
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);

    for (int i = SET_SIZE; i < MAX_SET_SIZE; i += SET_SIZE) {
      final int ii = i;
      Button onButton = new Button("" + i);
      buttonGroup.add(onButton);

      onButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          controlState.setSetSize(ii);
          setSetSize(ii);
        }
      });
      onButton.setActive(controlState.getSetSize() == i);
    }

    return group;
  }

  protected void setSetSize(int i) {
  }

  private void playRefLater() {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        playRef();
      }
    });
  }

  public ControlGroup getAudioGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup("PLAY " + controller.getLanguage().toUpperCase());
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    w.add(buttonGroup);

    buttonGroup.setToggle(ToggleType.RADIO);
    Button onButton = new Button("On" + "");
    buttonGroup.add(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.audioOn) {
          playRefLater();
        }
        controlState.audioOn = true;
        System.out.println("now on " + controlState);

      }
    });
    onButton.setActive(controlState.audioOn);


    Button offButton = new Button("Off" + "");
    buttonGroup.add(offButton);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.audioOn = false;
        System.out.println("now off " + controlState);

      }
    });
    offButton.setActive(!controlState.audioOn);

    return group;
  }

  public ControlGroup getFeedbackGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup("FEEDBACK");
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    Button onButton = new Button("On" + "");
    buttonGroup.add(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.audioFeedbackOn = true;
        System.out.println("now on " + controlState);

      }
    });
    onButton.setActive(controlState.audioFeedbackOn);


    Button offButton = new Button("Off" + "");
    buttonGroup.add(offButton);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controlState.audioFeedbackOn = false;
        System.out.println("now off " + controlState);

      }
    });
    offButton.setActive(!controlState.audioFeedbackOn);

    return group;
  }

  public ControlGroup getShowGroup(final ControlState controlState) {
    ControlGroup group = new ControlGroup("SHOW");
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setVertical(true);
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    Button onButton = new Button(controller.getLanguage());
    buttonGroup.add(onButton);

    onButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.showState.equals(ControlState.FOREIGN)) {
          controlState.showState = ControlState.FOREIGN;
          System.out.println("now on " + controlState);
          showEnglishOrForeign();
        }

      }
    });
    onButton.setActive(controlState.showForeign() && !controlState.showBoth());


    Button offButton = new Button("English");
    buttonGroup.add(offButton);

    offButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.showState.equals(ControlState.ENGLISH)) {

          controlState.showState = ControlState.ENGLISH;
          System.out.println("now  " + controlState);
          showEnglishOrForeign();

        }
      }
    });
    offButton.setActive(controlState.showEnglish() && !controlState.showBoth());

    Button both = new Button("Both");
    buttonGroup.add(both);

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!controlState.showState.equals(ControlState.BOTH)) {

          controlState.showState = ControlState.BOTH;
          System.out.println("now  " + controlState);
          showEnglishOrForeign();

        }

      }
    });
    both.setActive(controlState.showBoth());


    return group;
  }

  /**
   * Make row for help question mark, right justify
   *
   * @param controller
   * @return
   */
  protected Widget getHelpRow(final ExerciseController controller) {
    FlowPanel helpRow = new FlowPanel();
    helpRow.addStyleName("floatRight");
    helpRow.addStyleName("helpPadding");

    // add help image on right side of row
    helpRow.add(getHelp(controller));
    return helpRow;
  }

  private Panel getHelp(final ExerciseController controller) {
    Nav div = new Nav();
    Dropdown menu = new Dropdown(controller.getGreeting());
    menu.setIcon(IconType.QUESTION_SIGN);
    NavLink help = new NavLink("Help");
    help.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    menu.add(help);
    NavLink widget = new NavLink("Log Out");
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.resetState();
      }
    });
    menu.add(widget);
    div.add(menu);
    return div;
  }

  /**
   * Make a row to show the question content (the prompt or stimulus)
   * and the space bar and feedback widgets beneath it.
   *
   * @param e
   * @return
   * @see #BootstrapExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, boolean, ControlState)
   */
  protected Panel getCardPrompt(Exercise e, ExerciseController controller) {
    // FluidRow questionRow = new FluidRow();
    //DivWidget questionRow = new DivWidget();
    // questionRow.addStyleName("cardDisplayTable");
    Panel questionContent = getQuestionContent(e);
    questionContent.addStyleName("cardContent");
    //Column contentContainer = new Column(CONTENT_COLUMNS, questionContent);
    //questionRow.add(questionContent);
    // questionRow.setWidth("50%");
    return questionContent;
  }

  private Widget english;
  private Widget foreign;

  protected Panel getQuestionContent(Exercise e) {
    String foreignSentence = e.getRefSentences().iterator().next();
    DivWidget div = new DivWidget();
    div.addStyleName("blockStyle");
    english = new Heading(1, e.getEnglishSentence());
    //  english = new HTML(e.getEnglishSentence());
    // english.setWidth("50%");
    div.add(english);

    Heading widgets = new Heading(1, foreignSentence);
    foreign = widgets;
    // foreign.setWidth("50%");

    widgets.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        playRefLater();
      }
    }, ClickEvent.getType());

    widgets.addDomHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        foreign.addStyleName("mouseOverHighlight");
     /*   PopupPanel popupPanel = new PopupPanel(true);
        Icon w = new Icon(IconType.VOLUME_UP);

        popupPanel.add(w);
        popupPanel.setAutoHideEnabled(true);
        popupPanel.showRelativeTo(foreign);*/
      }
    }, MouseOverEvent.getType());

    widgets.addDomHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        foreign.removeStyleName("mouseOverHighlight");
      }
    }, MouseOutEvent.getType());

    div.add(foreign);

    showEnglishOrForeign();

    return div;
  }

  private void showEnglishOrForeign() {
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

  DivWidget scoreFeedbackRow = new DivWidget();

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
  protected void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                                int feedbackHeight, Panel toAddTo) {
    // add answer widget to do the recording
    Widget answerAndRecordButtonRow = getAnswerAndRecordButtonRow(e, service, controller);
    toAddTo.add(answerAndRecordButtonRow);

    if (controller.getProps().showFlashcardAnswer()) {
      toAddTo.add(getRecoOutputRow());
    }

    //boolean classroomMode = controller.getProps().isClassroomMode();
    //System.out.println("classroom mode " + classroomMode);
    // DivWidget scoreFeedbackRow = audioScoreFeedback.getScoreFeedbackRow(feedbackHeight, classroomMode);
    //Heading pronunciation = new Heading(5, "Pronunciation");
    //Panel scoreFeedbackRow = audioScoreFeedback.getSimpleRow(pronunciation);
    //scoreFeedbackRow.setWidth("50%");
    scoreFeedbackRow.addStyleName("bottomFiveMargin");
    toAddTo.add(scoreFeedbackRow);
  }

  private RecordButtonPanel answerWidget;

  private Widget getAnswerAndRecordButtonRow(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1, controller.getProps().shouldAddRecordKeyBinding() || addKeyBinding);
    this.answerWidget = answerWidget;
    return getRecordButtonRow(answerWidget.getRecordButton());
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @see BootstrapExercisePanel#addRecordingAndFeedbackWidgets(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, com.google.gwt.user.client.ui.Panel)
   */
  protected Panel getRecordButtonRow(Widget recordButton) {
    Panel recordButtonRow = getCenteredWrapper(recordButton);

    recordButtonRow.getElement().setId("recordButtonRow");

    recordButtonRow.addStyleName("leftTenMargin");
    recordButtonRow.addStyleName("rightTenMargin");
    DOM.setStyleAttribute(recordButtonRow.getElement(), "marginRight", "10px");

    return recordButtonRow;
  }

  private Panel getCenteredWrapper(Widget recordButton) {
    Panel recordButtonRow = new FluidRow();
    // recordButtonRow.setWidth("50%");
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    return recordButtonRow;
  }

/*  private Panel getCentered(Widget toAdd) {
    Panel row = new FluidRow();
    // Paragraph paragraph = new Paragraph();
    // paragraph.addStyleName("alignCenter");
    row.add(new Column(12, toAdd));
    // paragraph.add(toAdd);
    return row;
  }*/

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
    recoOutputContainer = new Paragraph();
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, recoOutputContainer));

    recoOutputContainer.add(recoOutput);
    recoOutputRow.getElement().setId("recoOutputRow");
    // recoOutputRow.setWidth("50%");

    return recoOutputRow;
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @param addKeyBinding
   * @return
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   */
  protected RecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                              ExerciseController controller, final int index, boolean addKeyBinding) {
    if (addKeyBinding) {
      return new FlashcardRecordButtonPanel(this, service, controller, exercise, index);
    } else {
      return new FlashcardRecordButtonPanel(this, service, controller, exercise, index) {

        @Override
        protected RecordButton makeRecordButton(ExerciseController controller) {
          return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, false);  // TODO : fix later in classroom?
        }
      };
    }
  }

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @param scorePrefix
   * @see
   */
  protected void showPronScoreFeedback(double score, String scorePrefix) {
    // audioScoreFeedback.showScoreFeedback(scorePrefix, score, false, !addKeyBinding);


    scoreFeedbackRow.add(showScoreFeedback("Score ", score));
  }


  /**
   * @param pronunciationScore
   * @param score
   * @seex #showCRTFeedback(Double, mitll.langtest.client.sound.SoundFeedback, String, boolean)
   * @paramx centerVertically
   * @paramx useShortWidth
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showPronScoreFeedback(double, String)
   */
  public ProgressBar showScoreFeedback(String pronunciationScore, double score) {//}}, boolean centerVertically, boolean useShortWidth) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    ProgressBar scoreFeedback = new ProgressBar();
    //   scoreFeedbackColumn.clear();
    // scoreFeedbackColumn.add(scoreFeedback);
    //double val = useShortWidth ? 300 : Math.min(Window.getClientWidth() * 0.8, Window.getClientWidth() * 0.5);
    //scoreFeedback.setWidth((int)val + "px");

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

  private void clearFeedback() {
    scoreFeedbackRow.clear();
  }

  private Heading getRecoOutput() {
    return recoOutput;
  }

  private SoundFeedback getSoundFeedback() {
    return soundFeedback;
  }

  public void receivedAudioAnswer(final AudioAnswer result) {
    String path = exercise.getRefAudio() != null ? exercise.getRefAudio() : exercise.getSlowAudioRef();
    final boolean hasRefAudio = path != null;
    boolean correct = result.isCorrect();
    final double score = result.getScore();

    System.out.println("receivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);

    String feedback = "";
    boolean badAudioRecording = result.validity != AudioAnswer.Validity.OK;
    if (badAudioRecording) {
      showPopup(result.validity.getPrompt());
      nextAfterDelay(correct, "");
    } else if (correct) {
      showCorrectFeedback(score);
    } else {   // incorrect!!
      feedback = showIncorrectFeedback(result, score, hasRefAudio);
    }
    if (!badAudioRecording && (correct || !hasRefAudio)) {
      System.out.println("receivedAudioAnswer: correct " + correct + " pron score : " + score + " has ref " + hasRefAudio);
      nextAfterDelay(correct, feedback);
    }
  }

  /**
   * @param html
   * @see #receivedAudioAnswer
   */
  private void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(HIDE_DELAY);
  }

  private void showCorrectFeedback(double score) {
    showPronScoreFeedback(score, PRONUNCIATION_SCORE);
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
    if (result.isSaidAnswer() && result.isCorrect()) { // if they said the right answer, but poorly, show pron score
      showPronScoreFeedback(score, PRONUNCIATION_SCORE);
    }
    boolean hasSynonymAudio = !exercise.getSynonymAudioRefs().isEmpty();
    System.out.println("showIncorrectFeedback : playing synonym audio " + exercise.getSynonymAudioRefs()
      + " result " + result + " score " + score + " has ref " + hasRefAudio +
      " hasSynonymAudio " + hasSynonymAudio);

    String correctPrompt = getCorrectDisplay();
    if (hasRefAudio && continueToNext) {
      //System.out.println("has ref " + continueToNext);
      if (hasSynonymAudio) {
        List<String> toPlay = new ArrayList<String>(exercise.getSynonymAudioRefs());
        //  System.out.println("showIncorrectFeedback : playing " + toPlay);
        //  if (controlState.audioOn) {
        if (controlState.audioFeedbackOn) {
          playAllAudio(correctPrompt, toPlay);
        } else {
          nextAfterDelay(result.isCorrect(), correctPrompt);
        }
        //  } else {
        //if (controlState.showEnglish()) {
        // showBoth();
        //}
        //  if (!controlState.audioFeedbackOn) {
        //     loadNext();
        //  }
        //goToNextItem(correctPrompt);
        // }
      } else {
        // if (controlState.audioOn) {
        if (controlState.audioFeedbackOn) {
          String path = getRefAudioToPlay();
          if (path == null) {
            getSoundFeedback().playIncorrect(); // this should never happen
          } else {
            playRefAndGoToNext(correctPrompt, path);
          }
        } else {
          getSoundFeedback().playIncorrect(); // this should never happen

          goToNextAfter(1000);
          //loadNext();
        }
        //  } else {
        //  if (controlState.showEnglish()) {
        //  showBoth();
        // }
        //   if (controlState.audioFeedbackOn) {
        //      goToNextItem(correctPrompt);
        //  } else {
//            loadNext();
        //     goToNextAfter(1000);

        //   }
        //   }
      }
    } else {
      tryAgain();
    }

    if (controller.getProps().isDemoMode()) {
      correctPrompt = "Heard: " + result.decodeOutput + "<p>" + correctPrompt;
    }
    Heading recoOutput = getRecoOutput();
    if (recoOutput != null && controlState.audioFeedbackOn) {
      recoOutput.setText(correctPrompt);
      DOM.setStyleAttribute(recoOutput.getElement(), "color", "#000000");
    }
    return correctPrompt;
  }

  private String getRefAudioToPlay() {
    String path = exercise.getRefAudio();
    //System.out.println("getRefAudioToPlay : regular " + path);
    if (path == null) {
      path = exercise.getSlowAudioRef(); // fall back to slow audio
      //System.out.println("\tgetRefAudioToPlay : slow " + path);
    }
    return path;
  }

  private void playRefAndGoToNext(String correctPrompt, String path) {
    path = getPath(path);
    // System.out.println("playRefAndGoToNext : playing " + path);

    final String fcorrectPrompt = correctPrompt;

    getSoundFeedback().createSound(path, new SoundFeedback.EndListener() {
      @Override
      public void songEnded() {
        goToNextItem(fcorrectPrompt);
      }
    }, false);
  }

  private void playRef() {
    String refAudioToPlay = getRefAudioToPlay();

    if (refAudioToPlay == null) {
      getSoundFeedback().playIncorrect(); // this should never happen
    } else {
      playRef(refAudioToPlay);
    }
  }

  private void playRef(String path) {
    path = getPath(path);
    //System.out.println("showIncorrectFeedback : playing " + path);

    getSoundFeedback().createSound(path);
  }

  private String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  private void tryAgain() {
    getSoundFeedback().playIncorrect();

    Timer t = new Timer() {
      @Override
      public void run() {
        initRecordButton();
        //clearFeedback();
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
  private void playAllAudio(final String infoToShow, final List<String> toPlay) {
    String path = toPlay.get(0);
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

  /**
   * @param infoToShow longer text means longer delay while user reads it
   * @see #playAllAudio(String, java.util.List)
   * @see #showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  protected void goToNextItem(String infoToShow) {
    if (continueToNext) {
      int delay = getFeedbackLengthProportionalDelay(infoToShow);
      System.out.println("goToNextItem : using delay " + delay + " info " + infoToShow);
      goToNextAfter(delay);
    } else {
      initRecordButton();
    }
  }

  private void goToNextAfter(int delay) {
    Timer t = new Timer() {
      @Override
      public void run() {
        loadNext();
      }
    };
    t.schedule(controller.getProps().isDemoMode() ? LONG_DELAY_MILLIS : delay);
  }

  private int getFeedbackLengthProportionalDelay(String feedback) {
    int mult1 = feedback.length() / DELAY_CHARACTERS;
    int mult = Math.max(3, mult1);
    return mult * DELAY_MILLIS;
  }

  private String getCorrectDisplay() {
    String refSentence = exercise.getRefSentence();
    String translit = exercise.getTranslitSentence().length() > 0 ? "<br/>(" + exercise.getTranslitSentence() + ")" : "";

    if (refSentence == null || refSentence.length() == 0) {
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
    }
    return //"Answer: " +
      refSentence + (hasSynonyms ? "" : translit);
  }

  private String getAltAnswers(List<Exercise.QAPair> questions) {
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
  }

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
      if (correct) {
        // go to next item
        Timer t = new Timer() {
          @Override
          public void run() {
            loadNext();
          }
        };
        t.schedule(DELAY_MILLIS);
      } else {
        initRecordButton();
        clearFeedback();
      }
    }
  }

  private void initRecordButton() {
    answerWidget.initRecordButton();
  }

  protected void loadNext() {
    System.out.println("loadNext after " + exercise.getID());
    controller.getExerciseList().loadNextExercise(exercise);
  }
}
