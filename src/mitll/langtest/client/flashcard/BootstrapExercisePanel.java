package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FluidContainer {
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private Heading recoOutput;
  protected SoundFeedback soundFeedback;
  protected final Widget cardPrompt;
  protected ScoreFeedback audioScoreFeedback = new ScoreFeedback(false);
  protected Panel recoOutputContainer;
  private final boolean addKeyBinding;

  /**
   *
   * @param e
   * @param service
   * @param controller
   * @param addKeyBinding
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @see mitll.langtest.client.custom.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, int feedbackHeight, boolean addKeyBinding) {
    this.addKeyBinding = addKeyBinding;
    //setStyleName("exerciseBackground");
   // addStyleName("cardBorder");
    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    Widget helpRow = getHelpRow(controller);
    if (helpRow != null) add(helpRow);
    cardPrompt = getCardPrompt(e, controller);
    cardPrompt.getElement().setId("cardPrompt");
    //add(cardPrompt);
    Panel horiz = new FlowPanel();
    add(horiz);
    cardPrompt.addStyleName("floatLeft");
    VerticalPanel controls = new VerticalPanel();
    controls.addStyleName("floatRight");

    ControlGroup group = new ControlGroup("AUDIO");
    ButtonToolbar w = new ButtonToolbar();
    group.add(w);
    controls.add(group);
    ButtonGroup widget = new ButtonGroup();
    widget.setToggle(ToggleType.RADIO);
    w.add(widget);
    Button widget2 = new Button("On" + "");
    widget.add(widget2);
    w.add(widget);
    Button widget1 = new Button("Off" + "");
    widget.add(widget1);
    widget1.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

      }
    });
    widget2.setActive(true);
    widget2.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

      }
    });

    controls.addStyleName("leftTenMargin");
    controls.addStyleName("floatRight");

    VerticalPanel leftColumn = new VerticalPanel();
    leftColumn.addStyleName("exerciseBackground");
    leftColumn.addStyleName("cardBorder");
    leftColumn.addStyleName("floatLeft");
    leftColumn.add(cardPrompt);

    horiz.add(leftColumn);
    horiz.add(controls);

    addRecordingAndFeedbackWidgets(e, service, controller, feedbackHeight, leftColumn);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
    getElement().setId("BootstrapExercisePanel");
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
   *
   * @param e
   * @return
   */
  protected FlowPanel getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    //FlowPanel questionRow = new FlowPanel();
    Widget questionContent = getQuestionContent(e);
   Column contentContainer = new Column(12, questionContent);
    questionRow.add(contentContainer);
    return questionRow;
  }

  private Widget getQuestionContent(Exercise e) {
    String stimulus = e.getEnglishSentence();
    String content = e.getContent();

    if (content != null) {
      stimulus = content;
    }

    Widget hero = new Heading(5, stimulus);
    hero.addStyleName("marginRight");
    return hero;
  }

  /**
   * Three rows below the stimulus word/expression:<p></p>
   * record space bar image <br></br>
   * reco feedback - whether the recorded audio was correct/incorrect, etc.  <br></br>
   * score feedback - pron score
   *
   *
   *
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param toAddTo
   * @see #BootstrapExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, boolean)
   */
  protected void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                                int feedbackHeight, Panel toAddTo) {
    // add answer widget to do the recording
    toAddTo.add(getAnswerAndRecordButtonRow(e, service, controller));

    if (controller.getProps().showFlashcardAnswer()) {
      toAddTo.add(getRecoOutputRow());
    }

    boolean classroomMode = controller.getProps().isClassroomMode();
    System.out.println("classroom mode " + classroomMode);
    toAddTo.add(audioScoreFeedback.getScoreFeedbackRow(feedbackHeight, classroomMode));
  }

  protected Widget getAnswerAndRecordButtonRow(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    RecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1, controller.getProps().shouldAddRecordKeyBinding() || addKeyBinding);
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
  protected FluidRow getRecordButtonRow(Widget recordButton) {
    FluidRow recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    recordButtonRow.getElement().setId("recordButtonRow");
    return recordButtonRow;
  }

  /**
   * Center align the text feedback (correct/incorrect)
   *
   * @return
   */
  private FluidRow getRecoOutputRow() {
    FluidRow recoOutputRow = new FluidRow();
    recoOutputContainer = new Paragraph();
    recoOutputContainer.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, recoOutputContainer));
    recoOutput = new Heading(3, "Answer");
    recoOutput.addStyleName("cardHiddenText");   // same color as background so text takes up space but is invisible
    DOM.setStyleAttribute(recoOutput.getElement(), "color", "#ebebec");

    recoOutputContainer.add(recoOutput);
    recoOutputRow.getElement().setId("recoOutputRow");
    return recoOutputRow;
  }

  /**
   * @see #getAnswerAndRecordButtonRow(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @param addKeyBinding
   * @return
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
   *
   * @param score
   * @param scorePrefix
   * @see FlashcardRecordButtonPanel#showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  public void showPronScoreFeedback(double score, String scorePrefix) {
    audioScoreFeedback.showScoreFeedback(scorePrefix, score, false, !addKeyBinding);
  }

  public void clearFeedback() {
    audioScoreFeedback.clearFeedback();
  }

  public Heading getRecoOutput() {
    return recoOutput;
  }

  public SoundFeedback getSoundFeedback() {
    return soundFeedback;
  }
}
