package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 *
 * Subclassed to provide for audio recording and playback {@linkxx mitll.langtest.client.recorder.SimpleRecordExercisePanel} and
 * grading of answers {@linkx mitll.langtest.client.grading.GradingExercisePanel}
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanel extends VerticalPanel implements
  BusyPanel, ExerciseQuestionState, PostAnswerProvider, ProvidesResize, RequiresResize {
   private static final int CONTENT_SCROLL_HEIGHT = 220;
  private static final String PROMPT = "Read the following text and answer the question or questions below.";
  private final List<Widget> answers = new ArrayList<Widget>();
  private final Set<Widget> completed = new HashSet<Widget>();
  protected CommonExercise exercise = null;
  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  private final NavigationHelper navigationHelper;
  protected final ListInterface exerciseList;
  private final Map<Integer,Set<Widget>> indexToWidgets = new HashMap<Integer, Set<Widget>>();
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
  public ExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller, ListInterface exerciseList) {
    this.exercise = e;
    this.controller = controller;
    this.service = service;
    this.exerciseList = exerciseList;
    this.navigationHelper = getNavigationHelper(controller);

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setWidth("100%");
    boolean rightAlignContent = controller.isRightAlignContent();
    if (rightAlignContent) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(rightAlignContent ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    boolean showInstructions = !(getExerciseContent(e).toLowerCase().contains("listen"));// || controller.isDataCollectMode());   // hack
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

  protected NavigationHelper getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper(exercise,controller, this, exerciseList, true, true, true);
  }

  protected void addInstructions() {  add(new Heading(4, PROMPT));  }

  /**
   * @see #ExercisePanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, ExerciseController, mitll.langtest.client.list.ListInterface)
   * @param e
   * @return
   */
  private Widget getQuestionContent(CommonExercise e) {
    String content = getExerciseContent(e);

    HTML maybeRTLContent = getMaybeRTLContent(content, true);
    maybeRTLContent.addStyleName("rightTenMargin");
    maybeRTLContent.addStyleName("topMargin");
    if (content.length() > 200) {
      return getContentScroller(maybeRTLContent);
    } else {
      return maybeRTLContent;
    }
  }

  protected String getExerciseContent(CommonExercise e) { return e.getContent();  }

  Widget getContentScroller(HTML maybeRTLContent) {
    ScrollPanel scroller = new ScrollPanel(maybeRTLContent);
    scroller.getElement().setId("contentScroller");
    scroller.setHeight(CONTENT_SCROLL_HEIGHT + "px");
    return scroller;
  }

  /**
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   * @param content
   * @param requireAlignment
   * @return
   */
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
    return controller.getLanguage().equalsIgnoreCase("Pashto");
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
  private void addQuestions(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller, int questionNumber) {
      add(getQuestionPanel(e, service, controller, questionNumber
      ));
  }

  private Panel getQuestionPanel(CommonExercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                 int questionNumber) {
    // add question prompt
    Panel vp = new VerticalPanel();
    addQuestionPrompt(vp);

    // add answer widget
    vp.add(getAnswerWidget(exercise, service, controller, questionNumber));
    vp.addStyleName("userNPFContent2");
    return vp;
  }

  /**
   * @see #getAnswerWidget(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @param index
   * @param answerWidget
   */
  protected void addAnswerWidget(int index, Widget answerWidget) {
    answers.add(answerWidget);
    Set<Widget> objects = indexToWidgets.get(index);
    if (objects == null) indexToWidgets.put(index, objects = new HashSet<Widget>());
    objects.add(answerWidget);
  }

  //protected boolean shouldShowAnswer() { return controller.isDemoMode();  }

  /**
   * @see #getQuestionPanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int, int, java.util.List, java.util.List, mitll.langtest.shared.CommonExercise.QAPair, com.google.gwt.user.client.ui.HasWidgets)
   * @param i
   * @param total
   * @param qaPair
   * @param englishPair
   * @param flQAPair
   * @param showAnswer
   * @param toAddTo
   */
/*  protected void getQuestionHeader(int i, int total,
                                   Exercise.QAPair qaPair,
                                   Exercise.QAPair englishPair,
                                   Exercise.QAPair flQAPair,
                                   boolean showAnswer, HasWidgets toAddTo) {
    getQuestionHeader(total, qaPair, showAnswer, true, toAddTo);
  }*/

  /**
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @see #getQuestionHeader
   * @param total
   * @param qaPair
   * @param showAnswer
   * @param addSpacerAfter
   */
/*  private void getQuestionHeader(int total, Exercise.QAPair qaPair, boolean showAnswer, boolean addSpacerAfter, HasWidgets toAddTo) {
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
  }*/

  /**
   * @seex #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @paramx vp
   * @paramx e
   */
  private void addQuestionPrompt(Panel vp) {
    HTML prompt = new HTML(getQuestionPrompt(true));
    prompt.getElement().setId("questionPrompt");
    prompt.addStyleName("marginBottomTen");
    vp.add(prompt);
  }

  protected String getQuestionPrompt(boolean promptInEnglish) {
    return "";
  }

/*  protected String getWrittenPrompt(boolean promptInEnglish) {
    return THREE_SPACES +
        TYPE_YOUR_ANSWER_IN +(promptInEnglish ? ENGLISH : getLanguage()) +" :";
  }*/

 /* private String getLanguage() {
    String language = controller.getLanguage();
    return (language == null || language.length() == 0) ? THE_FOREIGN_LANGUAGE : language;
  }*/

/*  @Override
  protected void onUnload() {
    super.onUnload();
   // System.out.println("onUnload : doing unload of prev/next handler " +keyHandler);
    navigationHelper.removeKeyHandler();
  }*/

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
  public void postAnswers(final ExerciseController controller, final CommonExercise completedExercise) {
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

  protected Widget getAnswerWidget(final CommonExercise exercise, final LangTestDatabaseAsync service,
                                   ExerciseController controller, final int index) { return null; }
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

  /**
   * @see mitll.langtest.client.exercise.WaveformPostAudioRecordButton#useResult(mitll.langtest.shared.AudioAnswer)
   * @param answer
   */
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
    if (b) {
      System.out.println("isCompleted : answered " + completed.size() + " vs total " + answers.size() + " : " + b);
    }
    return b;
  }

  //protected void enableNextButton(boolean val) {  navigationHelper.enableNextButton(val); }
  void setButtonsEnabled(boolean val) {
   // navigationHelper.setButtonsEnabled(val);
    enableNext();
  }
}
