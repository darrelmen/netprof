package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
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
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 *
 * Subclassed to provide for audio recording and playback {@link mitll.langtest.client.SimpleRecordExercisePanel} and
 * grading of answers {@link mitll.langtest.client.grading.GradingExercisePanel}
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanel extends VerticalPanel implements BusyPanel, ExerciseQuestionState, ProvidesResize, RequiresResize {
  private static final String ANSWER_BOX_WIDTH = "400px";
  private static final String REPEAT_ONCE = "<i>Repeat the phrase once at normal speed.</i>";
  private static final String REPEAT_TWICE = "<i>Repeat the phrase twice, first at normal and then at slow speed.</i>";
  private static final String TWO_SPACES = "&nbsp;&nbsp;";
  private static final String THREE_SPACES = "&nbsp;&nbsp;&nbsp;";
  private static final String TEACHER_PROMPT = "Record the phrase above by clicking the record button, speak, and then stop when finished. ";
  private static final String LEFT_ARROW_TOOLTIP = "Press the left arrow key to go to the previous item.";
  private static final String RIGHT_ARROW_TOOLTIP = "Press enter to go to the next item.";
  private static final String THE_FOREIGN_LANGUAGE = " the foreign language";
  private static final String ENGLISH = "English";
  private static final String TYPE_YOUR_ANSWER_IN = "Type your answer in ";
  private static final String SPEAK_AND_RECORD_YOUR_ANSWER_IN = "Speak and record your answer in ";
  private List<Widget> answers = new ArrayList<Widget>();
  private Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  protected ExerciseController controller;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private Button prev,next;
  private HandlerRegistration keyHandler;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  private boolean debug = false;

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
    addItemHeader(e);

    enableNextOnlyWhenAllCompleted = !getLanguage().equalsIgnoreCase("Pashto");

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    boolean rightAlignContent = controller.isRightAlignContent();
    if (rightAlignContent) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(rightAlignContent ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    add(hp);

    int i = 1;

    addQuestions(e, service, controller, i);

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("100%", "20px");
    add(spacer);

    boolean includeKeyHandler = /*!controller.isAutoCRTMode() ||*/ controller.isCollectAudio();
    Panel buttonRow = getNextAndPreviousButtons(e, service, userFeedback, controller, includeKeyHandler);
    add(buttonRow);
  }

  protected void addItemHeader(Exercise e) {
    // add(new HTML("<h3>Item #" + e.getID() + "</h3>"));
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
    //System.out.println("content alignment guess is " + direction);

    HTML html = new HTML(content, direction);
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
        //add(new HTML("<br></br>"));
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
    //System.out.println("question prompt " + e.promptInEnglish + " for " +e);
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

  private Panel getNextAndPreviousButtons(final Exercise e, final LangTestDatabaseAsync service,
                                          final UserFeedback userFeedback, final ExerciseController controller, boolean useKeyHandler) {
    HorizontalPanel buttonRow = new HorizontalPanel();

    this.prev = new Button("Previous");
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev(controller, e);
      }
    });
    boolean onFirst = !controller.onFirst(e);
    prev.setEnabled(onFirst);
    if (useKeyHandler) prev.setTitle(LEFT_ARROW_TOOLTIP);

    buttonRow.add(prev);
    prev.setVisible(!controller.isMinimalUI() || !controller.isPromptBeforeNextItem());

    this.next = new Button(getNextButtonText());
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    buttonRow.add(next);
    if (true) next.setTitle(RIGHT_ARROW_TOOLTIP);

    DOM.setElementAttribute(next.getElement(), "id", "nextButton");

    // send answers to server
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickNext(service, userFeedback, controller, e);
      }
    });

    // TODO : revisit in the context of text data collections
    addKeyHandler(e, service, userFeedback, controller, useKeyHandler);

    return buttonRow;
  }

  protected String getNextButtonText() {
    return "Next";
  }

  private void addKeyHandler(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                             final ExerciseController controller, final boolean useKeyHandler) {
   // if (useKeyHandler) {
      keyHandler = Event.addNativePreviewHandler(new
                                                     Event.NativePreviewHandler() {

                                                       @Override
                                                       public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                         NativeEvent ne = event.getNativeEvent();
                                                         int keyCode = ne.getKeyCode();
                                                         boolean isLeft = keyCode == KeyCodes.KEY_LEFT;
                                                      //   boolean isRight = keyCode == KeyCodes.KEY_RIGHT;
                                                         boolean isEnter = keyCode == KeyCodes.KEY_ENTER;

                                                      //   System.out.println("key code is " +keyCode);
                                                         if (((useKeyHandler && isLeft) || isEnter) && event.getTypeInt() == 512 &&
                                                             "[object KeyboardEvent]".equals(ne.getString())) {
                                                           ne.preventDefault();

                                                           if (debug) {
                                                             System.out.println(new Date() +
                                                                 " : getNextAndPreviousButtons - key handler : " + keyHandler +
                                                                 " Got " + event + " type int " +
                                                                 event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                                 " native " + event.getNativeEvent() +
                                                                 " source " + event.getSource());
                                                           }

                                                           if (isLeft) {
                                                             clickPrev(controller, e);
                                                           } else {
                                                             clickNext(service, userFeedback, controller, e);
                                                           }
                                                         }
                                                       }
                                                     });
      //System.out.println("getNextAndPreviousButtons made click handler " + keyHandler);
   // }
  }

  /**
   * Ignore clicks or keyboard activity when the widget is not enabled.
   * @see #getNextAndPreviousButtons(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, boolean)
   * @see #addKeyHandler(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, boolean)
   * @param service
   * @param userFeedback
   * @param controller
   * @param e
   */
  protected void clickNext(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                         final ExerciseController controller, final Exercise e) {
    if (next.isEnabled() && next.isVisible()) {
      if (controller.isMinimalUI() && !controller.isGrading() && controller.isPromptBeforeNextItem()) {
        showConfirmNextDialog(service, userFeedback, controller, e);
      } else {
        postAnswers(service, userFeedback, controller, e);
      }
    }
    else {
      System.out.println("clickNext " +keyHandler+ " ignoring next");
    }
  }

  /**
   * Paul wanted a dialog that asks the user to confirm they want to move on to the next item.
   * @param service
   * @param userFeedback
   * @param controller
   * @param e
   */
  private void showConfirmNextDialog(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                     final ExerciseController controller, final Exercise e) {
    final DialogBox dialogBox;
    Button yesButton;

    dialogBox = new DialogBox();
    //DOM.setStyleAttribute(dialogBox.getElement(), "backgroundColor", "#ABCDEF");
    dialogBox.setText("Please confirm");

    yesButton = new Button("Yes");
    yesButton.getElement().setId("yesButton");
    yesButton.setFocus(true);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new Label("Are you ready to move on to the next item?"));

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    HorizontalPanel hp = new HorizontalPanel();
    hp.setSpacing(5);
    dialogVPanel.add(hp);
    hp.add(yesButton);
    Button no = new Button("No");
    no.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    hp.add(no);
    dialogBox.setWidget(dialogVPanel);

    yesButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        postAnswers(service, userFeedback, controller, e);
        dialogBox.hide();
      }
    });
    dialogBox.center();
  }

  private void clickPrev(ExerciseController controller, Exercise e) {
    if (prev.isEnabled() && prev.isVisible()) {
    //  System.out.println("clickPrev " +keyHandler+ " click on prev " + prev);
      controller.loadPreviousExercise(e);
    }
    else {
      System.out.println("clickPrev " +keyHandler+ " ignoring click prev.");
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();
   // System.out.println("onUnload : doing unload of prev/next handler " +keyHandler);
    if (keyHandler != null) keyHandler.removeHandler();
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
   * <br></br>
   * Loads next exercise after the post.
   *
   * @see ExercisePanel#getNextAndPreviousButtons(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, boolean)
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  protected void postAnswers(LangTestDatabaseAsync service, final UserFeedback userFeedback,
                             final ExerciseController controller, final Exercise completedExercise) {
    int i = 1;
    int user = controller.getUser();
    final Set<Widget> incomplete = new HashSet<Widget>();

    boolean allHaveText = true;
    for (final Widget tb : answers) {
      String text = ((HasValue<String>) tb).getValue();
      if (text.length() == 0) allHaveText = false;
    }
    if (!allHaveText) {
      showPopup("Please answer all questions.");
    } else {
      incomplete.addAll(answers);
      for (final Widget tb : answers) {
        String text = ((HasValue<String>) tb).getValue();
        service.addTextAnswer(user, exercise, i++, text, new AsyncCallback<Void>() {
          public void onFailure(Throwable caught) {
            userFeedback.showErrorMessage("Server error", "Couldn't post answers for exercise.");
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
    popupImage.showRelativeTo(next);
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
    enableNextButton((completed.size() == answers.size()));
  }

  public void enableNextButton(boolean val) {
    next.setEnabled(val);
  }

  public void setButtonsEnabled(boolean val) {
    prev.setEnabled(val);
    next.setEnabled(val);
  }

  /**
   * Stops the user from cut-copy-paste into the text box.
   * <p></p>
   * Prevents googling for answers.
   */
  private static class NoPasteTextBox extends TextBox {
    public NoPasteTextBox() {
      sinkEvents(Event.ONPASTE);
    }
    public void onBrowserEvent(Event event) {
      super.onBrowserEvent(event);

      if (event.getTypeInt() == Event.ONPASTE) {
        event.stopPropagation();
        event.preventDefault();
      }
    }
  }
}
