package mitll.langtest.client.bootstrap;

import java.util.ArrayList;
import java.util.List;

import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.Exercise;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.media.client.Audio;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/6/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapExercisePanel extends FluidContainer {
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";
  private static final String LISTEN_TO_THIS_AUDIO = "Listen to this audio";
//  private static final String FEEDBACK_TIMES_SHOWN = "FeedbackTimesShown";
  private static final int HIDE_DELAY = 2500;

  private Column scoreFeedbackColumn;
  private List<FlashcardRecordButtonPanel> answerWidgets = new ArrayList<FlashcardRecordButtonPanel>();

  public Image correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
  public Image incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
  public static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. Do you have a flashblocker? Please add this site to its whitelist.</font>";

  private Heading recoOutput;
  private ProgressBar scoreFeedback = new ProgressBar();
  private SoundFeedback soundFeedback;
  protected final Widget cardPrompt;

  /**
   * @param e
   * @param service
   * @param controller
   * @see mitll.langtest.client.flashcard.FlashcardExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   */
  public BootstrapExercisePanel(final Exercise e, final LangTestDatabaseAsync service,
                                final ExerciseController controller) {
    setStyleName("exerciseBackground");
    addStyleName("cardBorder");
    HTML warnNoFlash = new HTML(WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);

    add(getHelpRow(controller));
    cardPrompt = getCardPrompt(e, controller);
    cardPrompt.getElement().setId("cardPrompt");
    add(cardPrompt);
    addRecordingAndFeedbackWidgets(e, service, controller);
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
    Widget questionContent = getQuestionContent(e, controller);
    Column contentContainer = new Column(12, questionContent);
    questionRow.add(contentContainer);
    return questionRow;
  }

  protected Widget getQuestionContent(Exercise e, ExerciseController controller) {
    int headingSize = 1;
    String stimulus = e.getEnglishSentence();
    String content = e.getContent();

    if (content == null) {
      content = stimulus;
    } else {
      stimulus = content;
    }

    if (content != null && !controller.isDataCollectMode()) {
      return makeFlashcardForCRT(e, controller, content);
    } else {
      Widget hero = new Heading(headingSize, stimulus);
      hero.addStyleName("marginRight");
      return hero;
    }
  }

  private Panel makeFlashcardForCRT(Exercise e, ExerciseController controller, String content) {
    Exercise.QAPair qaPair = e.getForeignLanguageQuestions().get(0);
    content = content.replaceAll("<p> &nbsp; </p>", "");
    String splitTerm = LISTEN_TO_THIS_AUDIO;
    String[] split = content.split(splitTerm);
    String prefix = split[0];
    HTML contentPrefix = getHTML(prefix, true, controller);
    contentPrefix.addStyleName("marginRight");

    Panel container = new FlowPanel();
    Heading child = new Heading(5, "Exercise " + e.getID());
    child.addStyleName("leftTenMargin");
    container.add(child);
    container.add(contentPrefix);

    // Todo : this is vulnerable to a variety of issues.
    if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
      Panel container2 = new FlowPanel();
      container2.addStyleName("rightFiveMargin");
      HTML prompt = getHTML("<h3 style='margin-right: 30px'>" + splitTerm + "</h3>", true, controller);
      container2.add(prompt);
      SimplePanel simplePanel = new SimplePanel();
      simplePanel.add(getAudioWidget(e));
      container2.add(simplePanel);

      String suffix = split[1];
      HTML contentSuffix = getHTML("<br/>" + // TODO: br is a hack
        "<h3 style='margin-right: 30px'>" + suffix + "</h3>", true, controller);
      contentSuffix.addStyleName("marginRight");
      container2.add(contentSuffix);
      container2.addStyleName("rightAlign");
      container.add(container2);
    }

    container.add(getHTML("<h2 style='margin-right: 30px'>" + qaPair.getQuestion() + "</h2>", true, controller));
    return container;
  }

  private Widget getAudioWidget(Exercise e) {
    String refAudio = e.getRefAudio();
    String type = refAudio.substring(refAudio.length() - 3);

    final Audio audio = getAudioNoFocus(refAudio, type);
    audio.addStyleName("floatRight");
    audio.addStyleName("rightFiveMargin");

    return audio;
  }

  private Audio getAudioNoFocus(String refAudio, String type) {
    final Audio audio = Audio.createIfSupported();
    audio.setControls(true);
    audio.addSource(refAudio, "audio/" + type);
    audio.addSource(refAudio.replace(".mp3", ".ogg"), "audio/ogg");
    audio.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        audio.setFocus(false);
      }
    });
    return audio;
  }

  private HTML getHTML(String content, boolean requireAlignment, ExerciseController controller) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
  //  html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }
    html.addStyleName("rightTenMargin");

    html.addStyleName("wrapword");
    return html;
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
  private void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    // add answer widget to do the recording
    FlashcardRecordButtonPanel answerWidget = getAnswerWidget(e, service, controller, 1);
    this.answerWidgets.add(answerWidget);

    FluidRow recordButtonRow = getRecordButtonRow(answerWidget.getRecordButton());
    add(recordButtonRow);
    recordButtonRow.getElement().setId("recordButtonRow");

    if (controller.getProps().showFlashcardAnswer()) {
      FluidRow recoOutputRow = getRecoOutputRow();
      add(recoOutputRow);
      recoOutputRow.getElement().setId("recoOutputRow");
    }

   // if (controller.getProps().showFlashcardAnswer()) {
      FluidRow feedbackRow = getScoreFeedbackRow();
      feedbackRow.getElement().setId("feedbackRow");
      add(feedbackRow);
    //}
  }

  /**
   * Center align the record button image.
   *
   * @param recordButton
   * @return
   */
  private FluidRow getRecordButtonRow(Widget recordButton) {
    FluidRow recordButtonRow = new FluidRow();
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
  private FluidRow getRecoOutputRow() {
    FluidRow recoOutputRow = new FluidRow();
    Paragraph paragraph2 = new Paragraph();
    paragraph2.addStyleName("alignCenter");

    recoOutputRow.add(new Column(12, paragraph2));
    recoOutput = new Heading(3, "Answer");
    getRecoOutput().addStyleName("cardHiddenText");   // same color as background so text takes up space but is invisible
    DOM.setStyleAttribute(getRecoOutput().getElement(), "color", "#ebebec");

    paragraph2.add(getRecoOutput());
    return recoOutputRow;
  }

  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   */
  private FluidRow getScoreFeedbackRow() {
    FluidRow feedbackRow = new FluidRow();
    SimplePanel widgets = new SimplePanel();
    widgets.setHeight(30 +"px");
    scoreFeedbackColumn = new Column(6, 3, widgets);
    feedbackRow.add(scoreFeedbackColumn);
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
   */
  public void showPronScoreFeedback(double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(scoreFeedback);

    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText(PRONUNCIATION_SCORE + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);
  }

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
