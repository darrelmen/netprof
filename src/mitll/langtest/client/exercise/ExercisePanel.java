package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.CommonExercise;

import java.util.*;

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
public abstract class ExercisePanel extends VerticalPanel implements
  BusyPanel, PostAnswerProvider, ProvidesResize, RequiresResize {
  private static final int CONTENT_SCROLL_HEIGHT = 220;
  private static final String PROMPT = "Read the following text and answer the question or questions below.";
  private final List<Widget> answers = new ArrayList<Widget>();
  private final Set<Widget> completed = new HashSet<Widget>();
  protected CommonExercise exercise = null;
  protected final ExerciseController controller;
  private final NavigationHelper navigationHelper;
  protected final ListInterface exerciseList;
  private final Map<Integer,Set<Widget>> indexToWidgets = new HashMap<Integer, Set<Widget>>();
  protected final String message;
  protected final String instance;

  /**
   * @see ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.list.ListInterface#loadExercise
   * @param e
   * @param service
   * @param controller
   * @param exerciseList
   * @param instructionMessage
   * @param instance
   */
  public ExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service,
                       final ExerciseController controller, ListInterface exerciseList, String instructionMessage, String instance) {
    this.exercise = e;
    this.controller = controller;
  //  this.service = service;
    this.exerciseList = exerciseList;
    this.message = instructionMessage;
    this.instance = instance;

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
    boolean showInstructions = !(getExerciseContent(e).toLowerCase().contains("listen"));   // hack
    if (showInstructions) {
      addInstructions();
    }
    add(hp);

    addQuestions(e, service, controller);

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
   * @see #ExercisePanel(CommonExercise, LangTestDatabaseAsync, ExerciseController, ListInterface, String, String)
   * @param e
   * @return
   */
  private Widget getQuestionContent(CommonExercise e) {
    String content = getExerciseContent(e);

    HTML maybeRTLContent = getMaybeRTLContent(content);
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
   * @return
   */
  protected HTML getMaybeRTLContent(String content) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      true && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    html.setWidth("100%");
    if (true && rightAlignContent) {
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
   */
  private void addQuestions(CommonExercise e, LangTestDatabaseAsync service, ExerciseController controller) {
      add(getQuestionPanel(e, service, controller, 1
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

  /**
   * @seex #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @paramx vp
   * @paramx e
   */
  protected void addQuestionPrompt(Panel vp) {
    HTML prompt = new HTML(getQuestionPrompt());
    prompt.getElement().setId("questionPrompt");
    prompt.addStyleName("marginBottomTen");
    vp.add(prompt);
  }

  protected String getQuestionPrompt() { return "";  }

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
  public abstract void postAnswers(final ExerciseController controller, final CommonExercise completedExercise); /*{
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

              exerciseList.loadNextExercise(completedExercise*//*.getID()*//*);
            }
            else {
              System.out.println("ExercisePanel.postAnswers " + incomplete.size() + " incomplete...");

            }
          }
        }
        );
      }
    }
  }*/

/*  private void showPopup(String toShow) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(navigationHelper.getNext());
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }*/

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

   // markTabsComplete();
    enableNext();
  }

  /**
   * If all the answer widgets within a question tab have been answered, put a little check mark icon
   * on the tab to indicate it's complete.
   */
/*  private void markTabsComplete() {
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
       // else {
          //System.out.println("\trecordCompleted : tab# " + tabIndex + " is      complete : " + widget.getElement().getId());
      //  }
      }
      if (allComplete) {
        //System.out.println("\trecordCompleted : tab# " + tabIndex + " is complete");
        if (!indexToTab.isEmpty()) {
          indexToTab.get(tabIndex).setIcon(IconType.CHECK);
        }
      }
    }
  }*/

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

  void setButtonsEnabled(boolean val) {
    enableNext();
  }
}
