package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.flashcard.AudioExerciseContent;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class ExercisePanel<T extends ExerciseShell> extends VerticalPanel implements
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
  private static final int ITEM_HEADER = 5;
  private static final int CONTENT_SCROLL_HEIGHT = 220;
  private static final String PROMPT = "Read the following text and answer the question or questions below.";
  private final List<Widget> answers = new ArrayList<Widget>();
  private final Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  protected final ExerciseController controller;
  private boolean enableNextOnlyWhenAllCompleted = true;
  protected final LangTestDatabaseAsync service;
  private final NavigationHelper navigationHelper;
  protected final ListInterface<Exercise> exerciseList;
  private final Map<Integer,Set<Widget>> indexToWidgets = new HashMap<Integer, Set<Widget>>();
  private TabPanel tabPanel = null;
  private final Map<Integer,Tab> indexToTab = new HashMap<Integer, Tab>();

  /**
   * @see ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.list.ListInterface#loadExercise
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public ExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller, ListInterface<Exercise> exerciseList) {
    this.exercise = e;
    System.out.println("ExercisePanel.ExercisePanel : exercise is " + exercise.getID());
    this.controller = controller;
    this.service = service;
    UserFeedback feedback = userFeedback;
    this.exerciseList = exerciseList;
    this.navigationHelper = getNavigationHelper(controller);
    //if (e.getQuestions().size() == 1) {
      addItemHeader(e);
    //}

    enableNextOnlyWhenAllCompleted = !isPashto();

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setWidth("100%");
    boolean rightAlignContent = controller.isRightAlignContent();
    if (rightAlignContent) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(rightAlignContent ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e, true));
    boolean showInstructions = !(e.getContent().toLowerCase().contains("listen") || controller.isDataCollectMode());   // hack
    if (showInstructions) {
      addInstructions();
    }
    add(hp);

    addQuestions(e, service, controller, 1);

    // add next and prev buttons
    add(navigationHelper);
    navigationHelper.addStyleName("topMargin");
    getElement().setId("ExercisePanel");
  }

  protected NavigationHelper<Exercise> getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper<Exercise>(exercise,controller, this, exerciseList, true, true);
  }

  protected void addInstructions() {  add(new Heading(4, PROMPT));  }

  /**
   * @see #ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, mitll.langtest.client.list.ListInterface)
   * @param e
   */
  protected void addItemHeader(Exercise e) {
    Heading w = new Heading(ITEM_HEADER, "Item " + e.getID());
    w.getElement().setId("ItemHeading");
    add(w);
  }

  private Widget getQuestionContent(Exercise e, boolean includeItemID) {
    String content = e.getContent();

    //System.out.println("getQuestionContent : content is " + content);
    if (content.contains("Listen")) {
      return new AudioExerciseContent().getQuestionContent(e, controller, includeItemID, false);
    }
    else {
      HTML maybeRTLContent = getMaybeRTLContent(content, true);
      maybeRTLContent.addStyleName("rightTenMargin");
      if (content.length() > 200) {
        //System.out.println("content length " + content.length() + " " + content);
        return getContentScroller(maybeRTLContent);
      } else {
        return maybeRTLContent;
      }
    }
  }

  Widget getContentScroller(HTML maybeRTLContent) {
    ScrollPanel scroller = new ScrollPanel(maybeRTLContent);
    scroller.getElement().setId("contentScroller");
    scroller.setHeight(CONTENT_SCROLL_HEIGHT + "px");
    return scroller;
  }

  protected HTML getMaybeRTLContent(String content, boolean requireAlignment) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }

    html.addStyleName("wrapword");
    if (isPashto()) {
      html.addStyleName("pashtofont");
    }
    else {
      html.addStyleName("xlargeFont");
    }
    return html;
  }

  private boolean isPashto() {
    return getLanguage().equalsIgnoreCase("Pashto");
  }

  public void onResize() {}
  public boolean isBusy() { return false; }

  @Override
  public void setBusy(boolean v) {}

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
    int n = englishQuestions.size();
    if (e.getQuestions().size() == 1) {
      Exercise.QAPair questionToShow = e.getQuestions().iterator().next();
      add(getQuestionPanel(e, service, controller, questionNumber, n, englishQuestions, flQuestions, questionToShow,this));
    }
    else {
      makeTabPanel(e, service, controller, questionNumber, englishQuestions, flQuestions, n);
      add(tabPanel);
    }
  }

  private void makeTabPanel(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber,
                            List<Exercise.QAPair> englishQuestions,
                            List<Exercise.QAPair> flQuestions,
                            int n) {
    tabPanel = new TabPanel();
    DOM.setStyleAttribute(tabPanel.getWidget(0).getElement(), "marginBottom", "0px");

    for (Exercise.QAPair pair : e.getQuestions()) {
      Tab tabPane = new Tab();
      tabPane.setHeading("Question #"+questionNumber);
      tabPanel.add(tabPane);
      indexToTab.put(questionNumber,tabPane);

      tabPane.add(getQuestionPanel(e, service, controller, questionNumber, n, englishQuestions, flQuestions, pair, tabPane));

      questionNumber++;
    }
    tabPanel.selectTab(0);
  }

  private Panel getQuestionPanel(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                 int questionNumber,
                                 int n,
                                 List<Exercise.QAPair> englishQuestions,
                                 List<Exercise.QAPair> flQuestions,
                                 Exercise.QAPair pair,
                                 HasWidgets toAddTo) {
    Exercise.QAPair engQAPair = questionNumber - 1 < n ? englishQuestions.get(questionNumber - 1) : null;
    Exercise.QAPair flQAPair  = questionNumber - 1 < n ? flQuestions.get(questionNumber - 1) : null;

    if (engQAPair != null) {
      getQuestionHeader(questionNumber,n, pair, engQAPair, flQAPair, false,toAddTo);
    }
    else {
      toAddTo.add(new Heading(6, ""));
    }
    // add question prompt
    Panel vp = new VerticalPanel();
    addQuestionPrompt(vp, exercise);

    // add answer widget
    vp.add(getAnswerWidget(exercise, service, controller, questionNumber));
    vp.addStyleName("userNPFContent2");
    return vp;
  }

  /**
   * @see #getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @param index
   * @param answerWidget
   */
  protected void addAnswerWidget(int index, Widget answerWidget) {
    answers.add(answerWidget);
    Set<Widget> objects = indexToWidgets.get(index);
    if (objects == null) indexToWidgets.put(index, objects = new HashSet<Widget>());
    objects.add(answerWidget);
 //   System.out.println("addAnswerWidget : now " + answers.size() + " expected, adding '" + answerWidget.getElement().getId() + "'");
  }

  protected boolean shouldShowAnswer() { return controller.isDemoMode();  }

  /**
   * @see #getQuestionPanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int, int, java.util.List, java.util.List, mitll.langtest.shared.Exercise.QAPair, com.google.gwt.user.client.ui.HasWidgets)
   * @param i
   * @param total
   * @param qaPair
   * @param englishPair
   * @param flQAPair
   * @param showAnswer
   * @param toAddTo
   */
  protected void getQuestionHeader(int i, int total,
                                   Exercise.QAPair qaPair,
                                   Exercise.QAPair englishPair,
                                   Exercise.QAPair flQAPair,
                                   boolean showAnswer, HasWidgets toAddTo) {
    getQuestionHeader(total, qaPair, showAnswer, true, toAddTo);
  }

  /**
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @see #getQuestionHeader
   * @param total
   * @param qaPair
   * @param showAnswer
   * @param addSpacerAfter
   */
  private void getQuestionHeader(int total, Exercise.QAPair qaPair, boolean showAnswer, boolean addSpacerAfter, HasWidgets toAddTo) {
    String question = qaPair.getQuestion();
    String prefix = (total == 1) ? ("Question : ") : "";

    if (showAnswer) {
      String answer = qaPair.getAnswer();

      toAddTo.add(new HTML("<br></br><b>" + prefix + "</b>" + question));
      if (qaPair.getAlternateAnswers().size() > 1) {
        int j = 1;
        for (String alternate : qaPair.getAlternateAnswers()) {
          toAddTo.add(new HTML("<b>Possible answer #" + j++ +
            " : " +
            TWO_SPACES +
            "</b>" + alternate));
        }
        if (addSpacerAfter) add(new HTML("<br></br>"));
      } else {
        toAddTo.add(new HTML("<b>Answer : " +
          TWO_SPACES +
          "</b>" + answer + (addSpacerAfter ? "<br></br>" : "")));
      }
    }
    else {
      String questionHeader = prefix + question;
      HTML maybeRTLContent = getMaybeRTLContent("<h4>" + questionHeader + "</h4>", false);
      DOM.setStyleAttribute(maybeRTLContent.getElement(), "marginTop", "0px");

      toAddTo.add(maybeRTLContent);
    }
  }

  /**
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @param vp
   * @param e
   */
  protected void addQuestionPrompt(Panel vp, Exercise e) {
    HTML prompt = new HTML(getQuestionPrompt(e.isPromptInEnglish()));
    prompt.getElement().setId("questionPrompt");
    prompt.addStyleName("marginBottomTen");
    vp.add(prompt);
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
    return (language == null || language.length() == 0) ? THE_FOREIGN_LANGUAGE : language;
  }

  /**
   * @see mitll.langtest.client.recorder.SimpleRecordExercisePanel#getQuestionPrompt(boolean)
   * @param promptInEnglish
   * @return
   */
  protected String getSpokenPrompt(boolean promptInEnglish) {
    String instructions = getInstructions();
    String studentPrompt = SPEAK_AND_RECORD_YOUR_ANSWER_IN + (promptInEnglish ? ENGLISH : getLanguage()) + " ";
    String teacherPrompt = TEACHER_PROMPT;
    return THREE_SPACES + (controller.isDataCollectMode() && !controller.isCRTDataCollectMode() ? teacherPrompt : studentPrompt) + instructions;
  }

  protected String getInstructions() {
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
    return instructions;
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
  public void postAnswers(final ExerciseController controller, final ExerciseShell completedExercise) {
    int i = 1;
    int user = controller.getUser();
    final Set<Widget> incomplete = new HashSet<Widget>();

    System.out.println("ExercisePanel.postAnswers " + completedExercise);

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
        service.addTextAnswer(user, exercise, i++, text, controller.getAudioType(), new AsyncCallback<Void>() {
          public void onFailure(Throwable caught) {
            controller.getFeedback().showErrorMessage("Server error", "Couldn't post answers for exercise.");
          }

          public void onSuccess(Void result) {
            incomplete.remove(tb);
            if (incomplete.isEmpty()) {
              System.out.println("ExercisePanel.loadNextExercise " + completedExercise.getID());

              exerciseList.loadNextExercise(completedExercise/*.getID()*/);
            }
            else {
              System.out.println("ExercisePanel.postAnswers " + incomplete.size() + " incomplete...");

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
    System.out.println("getAnswerWidget for " + exercise.getID() + " and " + index);
    boolean allowPaste = controller.isDemoMode();
    final TextBox answer = allowPaste ? new TextBox() : new NoPasteTextBox();
    answer.setWidth(ANSWER_BOX_WIDTH);
    answer.setFocus(true);
    if (!exercise.isPromptInEnglish()) {
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
    addAnswerWidget(index, answer);
    return answer;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (!answers.isEmpty()) {
      Widget widget = answers.get(0);
      if (widget instanceof FocusWidget) {
        ((FocusWidget) widget).setFocus(true); // TODO : not sure if this works as intended
      }
    }
  }

  /**
   * @see mitll.langtest.client.recorder.SimpleRecordPanel#showAudioValidity(mitll.langtest.shared.AudioAnswer.Validity, ExerciseQuestionState, com.google.gwt.user.client.ui.Widget)
   * @param answer
   */
  public void recordIncomplete(Widget answer) {
    if (!completed.remove(answer) && !completed.isEmpty()) {
      System.err.println("recordIncomplete : huh? answer " + answer.getElement().getId() +
        " is not on list of size " + completed.size());
      for (Widget widget : completed) {
        System.err.println("recordIncomplete : known : " + widget.getElement().getId());
      }
    }
   // System.out.println("recordIncomplete : completed " + completed.size() + " vs total " + answers.size());

    enableNext();
  }

  public void recordCompleted(Widget answer) {
    completed.add(answer);

    System.out.println("recordCompleted : id " + answer.getElement().getId()+
      " completed " + completed.size() + " vs total " + answers.size());

    if (completed.size() > answers.size()) {
      System.err.println("recordCompleted huh? more complete " + completed.size() + " than answers " + answers.size());
    }

    markTabsComplete();
    enableNext();
  }

  /**
   * If all the answer widgets within a question tab have been answered, put a little check mark icon
   * on the tab to indicate it's complete.
   */
  private void markTabsComplete() {
    for (Map.Entry<Integer, Set<Widget>> indexWidgetsPair : indexToWidgets.entrySet()) {
      boolean allComplete = true;
      Set<Widget> widgetsForTab = indexWidgetsPair.getValue();
      Integer tabIndex = indexWidgetsPair.getKey();
      //System.out.println("\trecordCompleted : checking " + tabIndex + " and " + widgetsForTab.size());

      for (Widget widget : widgetsForTab) {
        if (!completed.contains(widget)) {
          //System.out.println("\trecordCompleted : tab# " + tabIndex + " is *not* complete : " + widget.getElement().getId());
          allComplete = false;
          break;
        }
        else {
          //System.out.println("\trecordCompleted : tab# " + tabIndex + " is      complete : " + widget.getElement().getId());
        }
      }
      if (allComplete) {
        //System.out.println("\trecordCompleted : tab# " + tabIndex + " is complete");
        if (!indexToTab.isEmpty()) {
          indexToTab.get(tabIndex).setIcon(IconType.CHECK);
        }
      }
    }
  }

  protected void enableNext() {
    //System.out.println("enableNext : answered " + completed.size() + " vs total " + answers.size());
    boolean isComplete = isCompleted();
    navigationHelper.enableNextButton(isComplete);
  }

  protected boolean isCompleted() {
    boolean b = completed.size() == answers.size();
    //System.out.println("isCompleted : answered " + completed.size() + " vs total " + answers.size() + " : " + b);
    return b;
  }

  protected void enableNextButton(boolean val) {  navigationHelper.enableNextButton(val); }
  void setButtonsEnabled(boolean val) { navigationHelper.setButtonsEnabled(val);}
}
