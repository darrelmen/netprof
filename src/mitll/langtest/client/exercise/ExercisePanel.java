package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 *
 * Subclassed to provide for audio recording and playback {@link mitll.langtest.client.recorder.SimpleRecordExercisePanel} and
 * grading of answers {@link mitll.langtest.client.grading.GradingExercisePanel}
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanel extends VerticalPanel implements
  BusyPanel, ExerciseQuestionState, PostAnswerProvider, ProvidesResize, RequiresResize {
  private static final String ANSWER_BOX_WIDTH = "400px";
  private static final String REPEAT_ONCE = "<i>Repeat the phrase once at normal speed.</i>";
  private static final String REPEAT_TWICE = "<i>Repeat the phrase twice, first at normal and then at slow speed.</i>";
  private static final String TWO_SPACES = "&nbsp;&nbsp;";
  private static final String THREE_SPACES = "&nbsp;&nbsp;&nbsp;";
  private static final String TEACHER_PROMPT = "Record the phrase above by clicking the record button, speak, and then stop when finished. ";
  private static final String THE_FOREIGN_LANGUAGE = " the foreign language";
  private static final String ENGLISH = "English";
  private static final String TYPE_YOUR_ANSWER_IN = "Type your answer in ";
  private static final String SPEAK_AND_RECORD_YOUR_ANSWER_IN = "Speak and record your answer in ";
  private List<Widget> answers = new ArrayList<Widget>();
  private Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  protected ExerciseController controller;
  private boolean enableNextOnlyWhenAllCompleted = true;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  protected NavigationHelper navigationHelper;

  /**
   * @see ExercisePanelFactory#getExercisePanel
   * @see ExerciseList#loadExercise
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public ExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller) {
    this.exercise = e;
    this.controller = controller;
    this.service = service;
    this.feedback = userFeedback;
    this.navigationHelper = getNavigationHelper(controller);
    addItemHeader(e);

    enableNextOnlyWhenAllCompleted = !getLanguage().equalsIgnoreCase("Pashto");

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setWidth("100%");
    boolean rightAlignContent = controller.isRightAlignContent();
    if (rightAlignContent) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(rightAlignContent ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    boolean showInstructions = !(e.getContent().toLowerCase().contains("listen") || controller.isDataCollectMode());   // hack
    if (showInstructions) {
      addInstructions();
    }
    add(hp);

    int i = 1;

    addQuestions(e, service, controller, i);

    SimplePanel spacer = makeSpacer();
    add(spacer);

    // add next and prev buttons
    add(navigationHelper);
  }

  private SimplePanel makeSpacer() {
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("100%", "20px");
    return spacer;
  }

  protected NavigationHelper getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper(exercise,controller, this);
  }

  protected void addInstructions() {
    add(new Heading(4, "Read the following text and answer the question or questions below."));
  }

  protected void addItemHeader(Exercise e) {
     add(new Heading(4,"Item #" + e.getID()));
  }

  private Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    HTML html = getHTML(content, true);
    return html;
  }

  private HTML getHTML(String content, boolean requireAlignment) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }

    html.addStyleName("wrapword");
    if (getLanguage().equalsIgnoreCase("Pashto")) {
      html.addStyleName("pashtofont");
    }
    else {
      html.addStyleName("xlargeFont");
    }
    return html;
  }

  public void onResize() {}
  public boolean isBusy() { return false; }

  /**
   * For every question,
   * <ul>
   *  <li>show the text of the question,  </li>
   *  <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   *  <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   * @param questionNumber
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber) {
    List<Exercise.QAPair> englishQuestions = e.getEnglishQuestions();
    List<Exercise.QAPair> flQuestions = e.getForeignLanguageQuestions();
    List<Exercise.QAPair> questionsToShow = e.promptInEnglish ? englishQuestions : flQuestions;
    int n = englishQuestions.size();
    //System.out.println("eng q " + englishQuestions);
    for (Exercise.QAPair pair : e.getQuestions()) {
      // add question header
      Exercise.QAPair engQAPair = questionNumber - 1 < n ? questionsToShow.get(questionNumber - 1) : null;

      if (engQAPair != null) {
        getQuestionHeader(questionNumber, n, engQAPair, shouldShowAnswer(),!controller.isDemoMode());
      }
      else {
        add(new Heading(6,""));
      }
      if (controller.isDemoMode()) {
        Exercise.QAPair flQAPair  = flQuestions.get(questionNumber - 1);
        getQuestionHeader(questionNumber, n, flQAPair, pair, shouldShowAnswer());
      }
      questionNumber++;
      // add question prompt
      VerticalPanel vp = new VerticalPanel();
      addQuestionPrompt(vp, e);

      // add answer widget
      Widget answerWidget = getAnswerWidget(e, service, controller, questionNumber -1);
      vp.add(answerWidget);
      answers.add(answerWidget);

      add(vp);
    }
  }

  protected boolean shouldShowAnswer() {
    return controller.isDemoMode();
  }

  protected void getQuestionHeader(int i, int total, Exercise.QAPair qaPair, Exercise.QAPair pair, boolean showAnswer) {
    getQuestionHeader(i,total,qaPair,showAnswer,true);
  }

  /**
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @see #getQuestionHeader(int, int, mitll.langtest.shared.Exercise.QAPair, boolean, boolean)
   * @param i
   * @param total
   * @param qaPair
   * @param showAnswer
   * @param addSpacerAfter
   */
  private void getQuestionHeader(int i, int total, Exercise.QAPair qaPair, boolean showAnswer, boolean addSpacerAfter) {
    String question = qaPair.getQuestion();
    String prefix = "Question" +
        (total > 1 ? " #" + i : "") +
        " : ";

    if (showAnswer) {
      String answer = qaPair.getAnswer();

      add(new HTML("<br></br><b>" + prefix + "</b>" + question));
      if (qaPair.getAlternateAnswers().size() > 1) {
        int j = 1;
        for (String alternate : qaPair.getAlternateAnswers()) {
          add(new HTML("<b>Possible answer #" + j++ +
              " : " +
              TWO_SPACES +
              "</b>" + alternate));
        }
        if (addSpacerAfter) add(new HTML("<br></br>"));
      } else {
        add(new HTML("<b>Answer : " +
            TWO_SPACES +
            "</b>" + answer + (addSpacerAfter ? "<br></br>" : "")));
      }
    }
    else {
      String questionHeader = prefix + question;
      add(getHTML("<h4>" + questionHeader + "</h4>",false));
    }
  }

  private void addQuestionPrompt(Panel vp, Exercise e) {
    vp.add(new HTML(getQuestionPrompt(e.promptInEnglish)));
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("50px", getQuestionPromptSpacer() + "px");
    vp.add(spacer);
  }

  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getWrittenPrompt(promptInEnglish);
  }

  protected String getWrittenPrompt(boolean promptInEnglish) {
    return THREE_SPACES +
        TYPE_YOUR_ANSWER_IN +(promptInEnglish ? ENGLISH : getLanguage()) +" :";
  }

  private String getLanguage() {
    String language = controller.getLanguage();
    String lang = (language == null || language.length() == 0) ? THE_FOREIGN_LANGUAGE : language;
    return lang;
  }

  protected String getSpokenPrompt(boolean promptInEnglish) {
    String instructions = ":";
    String prefix = "<br></br>" + THREE_SPACES;
    if (controller.getAudioType().equals(Result.AUDIO_TYPE_FAST_AND_SLOW)) {
      instructions = prefix +REPEAT_TWICE;
    }
    else if (controller.getAudioType().equals(Result.AUDIO_TYPE_REGULAR)) {
      instructions = prefix +REPEAT_ONCE;
    }
    else if (!controller.isCRTDataCollectMode()) {
      System.out.println("unknown audio type " + controller.getAudioType());
    }
    String studentPrompt = SPEAK_AND_RECORD_YOUR_ANSWER_IN + (promptInEnglish ? ENGLISH : getLanguage()) + " ";
    String teacherPrompt = TEACHER_PROMPT;
    return THREE_SPACES + (controller.isDataCollectMode() && !controller.isCRTDataCollectMode() ? teacherPrompt : studentPrompt) + instructions;
  }

  protected int getQuestionPromptSpacer() {
    return 20;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
   // System.out.println("onUnload : doing unload of prev/next handler " +keyHandler);
    navigationHelper.removeKeyHandler();
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
   * <br></br>
   * Loads next exercise after the post.
   *
   * @see NavigationHelper#getNextAndPreviousButtons
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(final ExerciseController controller, final Exercise completedExercise) {
    int i = 1;
    int user = controller.getUser();
    final Set<Widget> incomplete = new HashSet<Widget>();

    boolean allHaveText = true;
    for (final Widget tb : answers) {
      String text = ((HasValue<String>) tb).getValue();
      if (text.length() == 0) allHaveText = false;
    }
    if (!allHaveText) {
      showPopup("Please answer " + (answers.size() == 1 ? "the question." : "all questions."));
    } else {
      incomplete.addAll(answers);
      for (final Widget tb : answers) {
        String text = ((HasValue<String>) tb).getValue();
        service.addTextAnswer(user, exercise, i++, text, new AsyncCallback<Void>() {
          public void onFailure(Throwable caught) {
            controller.getFeedback().showErrorMessage("Server error", "Couldn't post answers for exercise.");
          }

          public void onSuccess(Void result) {
            incomplete.remove(tb);
            if (incomplete.isEmpty()) {
              controller.loadNextExercise(completedExercise);
            }
          }
        }
        );
      }
    }
  }

  private void showPopup(String toShow) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(navigationHelper.getNext());
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }

  /**
   * Remember to have the text default to RTL direction if the prompt is in the foreign (Arabic) language.
   * <br></br>
   * Keeps track of which text fields have been typed in, so we can enable/disable the next button.<br></br>
   *
   * If we're in autoCRT mode {@link mitll.langtest.client.exercise.ExerciseController#isAutoCRTMode()} then we
   * add a check answer button after the text box to allow the user to see if they answered correctly.
   *
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @param exercise here used to determine the prompt language (English/FL)
   * @param service used in subclasses
   * @param controller  used in subclasses
   * @param index of the question (0 for first, 1 for second, etc.) (used in subclasses)
   * @return widget that handles the answer
   */
  protected Widget getAnswerWidget(final Exercise exercise, final LangTestDatabaseAsync service,
                                   ExerciseController controller, final int index) {
  //  GWT.log("getAnswerWidget for #" +index);
    boolean allowPaste = controller.isDemoMode();
    final TextBox answer = allowPaste ? new TextBox() : new NoPasteTextBox();
    answer.setWidth(ANSWER_BOX_WIDTH);
    answer.setFocus(true);
    if (!exercise.promptInEnglish) {
      answer.setDirection(HasDirection.Direction.RTL);
    }
    if (enableNextOnlyWhenAllCompleted) {  // make sure user entered in answers for everything
      answer.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          if (answer.getText().length() > 0) {
            recordCompleted(answer);
          } else {
            recordIncomplete(answer);
          }
        }
      });
    }

    if (controller.isAutoCRTMode()) {
      return getAutoCRTCheckAnswerWidget(exercise, service, index, answer);
    }
    else {
      return answer;
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    Widget widget = answers.get(0);
    if (widget instanceof FocusWidget) {
      ((FocusWidget)widget).setFocus(true);
    }
  }

  private HorizontalPanel getAutoCRTCheckAnswerWidget(final Exercise exercise, final LangTestDatabaseAsync service,
                                                      final int index, final TextBox answer) {
    HorizontalPanel hp = new TextValue();
    hp.setSpacing(5);
    hp.add(answer);
    final Button check = new Button("Check Answer");
    check.setEnabled(false);
    hp.add(check);
    final Label resp = new Label();
    hp.add(resp);

    answer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        resp.setText("");
        check.setEnabled(answer.getText().length() > 0);
      }
    });

    check.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        check.setEnabled(false);
        service.getScoreForAnswer(exercise, index, answer.getText(), new AsyncCallback<Double>() {
          @Override
          public void onFailure(Throwable caught) {
            check.setEnabled(true);
          }
          @Override
          public void onSuccess(Double result) {
            check.setEnabled(true);
            showAutoCRTScore(result, resp);
          }
        });
      }
    });
    return hp;
  }

  private void showAutoCRTScore(Double result, Label resp) {
    result *= 2.5;
    result -= 1.25;
    result = Math.max(0,result);
    result = Math.min(1.0,result);
    String percent = ((int) (result * 100)) + "%";
    if (result > 0.6) {
      resp.setText("Correct! Score was " + percent);
      resp.setStyleName("correct");
    }
    else {
      resp.setText("Try again - score was " + percent);
      resp.setStyleName("incorrect");
    }
  }

  private static class TextValue extends HorizontalPanel implements HasValue<String> {
    private String value;
    @Override
    public String getValue() { return value; }

    @Override
    public void setValue(String value) { this.value = value; }

    @Override
    public void setValue(String value, boolean fireEvents) {  this.value = value; }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) { return null; }
  }

  public void recordIncomplete(Widget answer) {
    completed.remove(answer);
    enableNext();
  }

  public void recordCompleted(Widget answer) {
    completed.add(answer);
    enableNext();
  }

  private void enableNext() {
    navigationHelper.enableNextButton((completed.size() == answers.size()));
  }

  protected void enableNextButton(boolean val) {
    navigationHelper.enableNextButton(val);
  }

  protected void setButtonsEnabled(boolean val) {
    navigationHelper.setButtonsEnabled(val);
  }

}
