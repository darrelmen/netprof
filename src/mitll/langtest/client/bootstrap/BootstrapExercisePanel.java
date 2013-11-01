package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundFeedback;
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
public class BootstrapExercisePanel extends FluidContainer {
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";
  private static final int HIDE_DELAY = 2500;

  private Column scoreFeedbackColumn;
  private List<FlashcardRecordButtonPanel> answerWidgets = new ArrayList<FlashcardRecordButtonPanel>();

  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private Heading recoOutput;
  private ProgressBar scoreFeedback = new ProgressBar();
  protected SoundFeedback soundFeedback;
  protected final Widget cardPrompt;

  /**
   * @param e
   * @param service
   * @param controller
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller, int feedbackHeight) {
    setStyleName("exerciseBackground");
    addStyleName("cardBorder");
    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    add(getHelpRow(controller));
    cardPrompt = getCardPrompt(e, controller);
    cardPrompt.getElement().setId("cardPrompt");
    add(cardPrompt);
    System.out.println("height " +feedbackHeight);
    addRecordingAndFeedbackWidgets(e, service, controller, feedbackHeight);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
  }

  /**
   * Make row for help question mark, right justify
   *
   * @param controller
   * @return
   */
  private Widget getHelpRow(final ExerciseController controller) {
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
   */
  protected Widget getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = getQuestionContent(e);
    Column contentContainer = new Column(12, questionContent);
    questionRow.add(contentContainer);
    return questionRow;
  }

  protected Widget getQuestionContent(Exercise e) {
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
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   */
  private void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                              int feedbackHeight) {
    // add answer widget to do the recording
    add(getAnswerAndRecordButtonRow(e, service, controller));

    if (controller.getProps().showFlashcardAnswer()) {
      add(getRecoOutputRow());
    }

    add(getScoreFeedbackRow(feedbackHeight));
  }

  protected Widget getAnswerAndRecordButtonRow(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    FlashcardRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1);
    this.answerWidgets.add(answerWidget);
    return getRecordButtonRow(answerWidget.getRecordButton());
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
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

  protected Panel recoOutputContainer;

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

  private SimplePanel feedbackDummyPanel;
  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   */
  private FluidRow getScoreFeedbackRow(int height) {
    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    scoreFeedbackColumn = new Column(6, 3, feedbackDummyPanel);
    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("feedbackRow");
    return feedbackRow;
  }

  protected FlashcardRecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                                ExerciseController controller, final int index) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, true);
  }

  /**
   * Not sure if this is actually necessary -- this is part of who gets the focus when the flashcard is inside
   * an internal frame in a dialog.
   *
   * @see BootstrapFlashcardExerciseList#grabFocus(BootstrapExercisePanel)
   */
  public void grabFocus() {
    for (FlashcardRecordButtonPanel widget : answerWidgets) {
      widget.getRecordButton().setFocus(true);
    }
  }

  /**
   * Show progress bar with score percentage, colored by score.
   * Note it has to be wide enough to hold the text "pronunciation score xxx %"
   *
   * @param score
   * @see FlashcardRecordButtonPanel#showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  public void showPronScoreFeedback(double score) {
    String pronunciationScore = PRONUNCIATION_SCORE;
    showScoreFeedback(pronunciationScore, score);
  }

  protected void showScoreFeedback(String pronunciationScore, double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(scoreFeedback);

    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText(pronunciationScore + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);
  }

  public void clearFeedback() { scoreFeedbackColumn.clear(); scoreFeedbackColumn.add(feedbackDummyPanel);}

  public void showPopup(String html) {
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

  @Override
  protected void onUnload() {
    for (FlashcardRecordButtonPanel answers : answerWidgets) {
      answers.onUnload();
    }
  }

  public Heading getRecoOutput() {
    return recoOutput;
  }

  public SoundFeedback getSoundFeedback() {
    return soundFeedback;
  }
}
