/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.Shell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 * <p>
 * Subclassed to provide for audio recording and playback {@linkxx mitll.langtest.client.recorder.SimpleRecordExercisePanel} and
 * grading of answers {@linkx mitll.langtest.client.grading.GradingExercisePanel}
 * <p>
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExercisePanel<L extends Shell, T extends Shell> extends VerticalPanel implements
    BusyPanel, PostAnswerProvider, ProvidesResize, RequiresResize {
  private final Logger logger = Logger.getLogger("ExercisePanel");

  private static final int CONTENT_SCROLL_HEIGHT = 220;
  private static final String PROMPT = "Read the following text and answer the question or questions below.";
  private final List<Widget> answers = new ArrayList<Widget>();
  private final Set<Widget> completed = new HashSet<Widget>();
  protected T exercise = null;
  final ExerciseController controller;
  private final NavigationHelper<L> navigationHelper;
  final ListInterface<L> exerciseList;
  private final Map<Integer, Set<Widget>> indexToWidgets = new HashMap<Integer, Set<Widget>>();
  final String message;
  final String instance;

  /**
   * @param e
   * @param service
   * @param controller
   * @param exerciseList
   * @param instructionMessage
   * @param instance
   * @see ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.list.ListInterface#loadExercise
   */
  ExercisePanel(final T e, final LangTestDatabaseAsync service,
                final ExerciseController controller,
                ListInterface<L> exerciseList, String instructionMessage,
                String instance) {
    this.exercise = e;
    this.controller = controller;
    this.exerciseList = exerciseList;
    this.message = instructionMessage;
    this.instance = instance;

    this.navigationHelper = getNavigationHelper(controller);

    // attempt to left justify
    //HorizontalPanel hp = ;
   // boolean showInstructions = !(getExerciseContent(e).toLowerCase().contains("listen"));   // hack
   // if (showInstructions) {
      addInstructions();
   // }
    add(getQuestionContent(e, controller));

    addQuestions(e, service, controller);

    // add next and prev buttons
    add(navigationHelper);
    navigationHelper.addStyleName("topMargin");
    getElement().setId("ExercisePanel");
  }

  /**
   * Worry about RTL
   * @param e
   * @param controller
   * @return
   */
  private Widget getQuestionContent(T e, ExerciseController controller) {
    HorizontalPanel hp = new HorizontalPanel();
    hp.setWidth("100%");
    boolean rightAlignContent = controller.isRightAlignContent();
    if (rightAlignContent) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(rightAlignContent ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    return hp;
  }

  private NavigationHelper<L> getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper<L>(exercise, controller, this, exerciseList, true, true, true);
  }

  void addInstructions() {
    add(new Heading(4, PROMPT));
  }

  /**
   * @param e
   * @return
   * @see #ExercisePanel(T, LangTestDatabaseAsync, ExerciseController, ListInterface, String, String)
   */
  private Widget getQuestionContent(T e) {
    String content = getExerciseContent(e);

    HTML maybeRTLContent = getMaybeRTLContent(content);
    maybeRTLContent.addStyleName("rightTenMargin");
    maybeRTLContent.addStyleName("topMargin");

    return (content.length() > 200)  ? getContentScroller(maybeRTLContent) : maybeRTLContent;
  }

  protected abstract String getExerciseContent(T e);

  Widget getContentScroller(HTML maybeRTLContent) {
    ScrollPanel scroller = new ScrollPanel(maybeRTLContent);
    scroller.getElement().setId("contentScroller");
    scroller.setHeight(CONTENT_SCROLL_HEIGHT + "px");
    return scroller;
  }

  /**
   * @param content
   * @return
   * @see #getQuestionContent
   */
  private HTML getMaybeRTLContent(String content) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
        rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
    html.setWidth("100%");
    if (rightAlignContent) {
      html.addStyleName("rightAlign");
    }

    html.addStyleName("wrapword");
    if (isPashto()) {
      html.addStyleName("pashtofont");
    } else {
      html.addStyleName("xlargeFont");
    }
    return html;
  }

  private boolean isPashto() {
    return controller.getLanguage().equalsIgnoreCase("Pashto");
  }

  public void onResize() {
  }

  public boolean isBusy() {
    return false;
  }

  @Override
  public void setBusy(boolean v) {
  }

  /**
   * For every question,
   * <ul>
   * <li>show the text of the question,  </li>
   * <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   * <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   *
   * @param e
   * @param service
   * @param controller used in subclasses for audio control
   */
  private void addQuestions(T e, LangTestDatabaseAsync service, ExerciseController controller) {
    add(getQuestionPanel(e, service, controller, 1
    ));
  }

  private Panel getQuestionPanel(T exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                 int questionNumber) {
    Panel vp = new VerticalPanel();
    // add answer widget
    vp.add(getAnswerWidget(exercise, service, controller, questionNumber));
    vp.addStyleName("userNPFContent2");
    return vp;
  }

  /**
   * @param index
   * @param answerWidget
   * @see #getAnswerWidget
   */
  void addAnswerWidget(int index, Widget answerWidget) {
    answers.add(answerWidget);
    Set<Widget> objects = indexToWidgets.get(index);
    if (objects == null) indexToWidgets.put(index, objects = new HashSet<Widget>());
    objects.add(answerWidget);
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
   * <br></br>
   * Loads next exercise after the post.
   *
   * @param controller
   * @param completedExercise
   * @see NavigationHelper#getNextAndPreviousButtons
   */
  @Override
  public abstract void postAnswers(final ExerciseController controller, final HasID completedExercise);

  Widget getAnswerWidget(final T exercise, final LangTestDatabaseAsync service,
                         ExerciseController controller, final int index) {
    return null;
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
   * @param answer
   */
  public void recordIncomplete(Widget answer) {
    if (!completed.remove(answer) && !completed.isEmpty()) {
      logger.warning("recordIncomplete : huh? answer " + answer.getElement().getId() +
          " is not on list of size " + completed.size());
      for (Widget widget : completed) {
        logger.warning("recordIncomplete : known : " + widget.getElement().getId());
      }
    }
    // logger.info("recordIncomplete : completed " + completed.size() + " vs total " + answers.size());
    enableNext();
  }

  /**
   * @param answer
   * @see mitll.langtest.client.exercise.WaveformPostAudioRecordButton#useResult(mitll.langtest.shared.AudioAnswer)
   */
  public void recordCompleted(Widget answer) {
    completed.add(answer);

    logger.info("recordCompleted : id " + answer.getElement().getId() +
        " completed " + completed.size() + " vs total " + answers.size());

    if (completed.size() > answers.size()) {
      logger.warning("recordCompleted huh? more complete " + completed.size() + " than answers " + answers.size());
    }

    enableNext();
  }

  private void enableNext() {
    //logger.info("enableNext : answered " + completed.size() + " vs total " + answers.size());
    boolean isComplete = isCompleted();
    navigationHelper.enableNextButton(isComplete);
  }

  private boolean isCompleted() {
    boolean b = completed.size() == answers.size();
    if (b) {
      logger.info("isCompleted : answered " + completed.size() + " vs total " + answers.size() + " : " + b);
    }
    return b;
  }

  void setButtonsEnabled(boolean val) {
    enableNext();
  }
}
