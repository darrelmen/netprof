/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonShell;
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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class ExercisePanel<L extends Shell, T extends CommonShell> extends VerticalPanel implements
    BusyPanel, PostAnswerProvider, ProvidesResize, RequiresResize {
  private final Logger logger = Logger.getLogger("ExercisePanel");

  private static final int CONTENT_SCROLL_HEIGHT = 220;
  private static final String PROMPT = "Read the following text and answer the question or questions below.";
  private final List<Widget> answers = new ArrayList<>();
  private final Set<Widget> completed = new HashSet<>();
  protected T exercise = null;
  final ExerciseController controller;
  protected final NavigationHelper navigationHelper;
  final ListInterface<L, T> exerciseList;
  private final Map<Integer, Set<Widget>> indexToWidgets = new HashMap<>();
  protected final String instance;
  boolean doNormalRecording;

  /**
   * @param e
   * @param controller
   * @param exerciseList
   * @param instance
   * @param enableNextOnlyWhenBothCompleted
   * @see ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.list.ListInterface#loadExercise
   */
  ExercisePanel(final T e,
                final ExerciseController controller,
                ListInterface<L, T> exerciseList,
                String instance,
                boolean doNormalRecording, boolean enableNextOnlyWhenBothCompleted) {
    this.exercise = e;
    this.controller = controller;
    this.exerciseList = exerciseList;
    //String message = instructionMessage;
    this.instance = instance;
    this.doNormalRecording = doNormalRecording;

    /*    logger.info("for " + e.getID() + " instance " + instance +
        " doNormal " + doNormalRecording);*/

    this.navigationHelper = getNavigationHelper(controller, enableNextOnlyWhenBothCompleted);

    addInstructions();
    add(getQuestionContentRTL(e));

    addQuestions(e, controller);

    // add next and prev buttons
    add(navigationHelper);
    navigationHelper.addStyleName("topMargin");
    //getElement().setId("ExercisePanel");
  }

  /**
   * Worry about RTL
   *
   * @param e
   * @return
   * @see #ExercisePanel
   */
  private Widget getQuestionContentRTL(T e) {
    HorizontalPanel hp = new HorizontalPanel();
    hp.getElement().setId("QuestionContentRTL");
    hp.setWidth("100%");
    boolean isRTL = isRTL(e);
    if (isRTL) {
      setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    }
    hp.setHorizontalAlignment(isRTL ? HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_LEFT);

    hp.add(getQuestionContent(e));

    return hp;
  }

  private NavigationHelper getNavigationHelper(ExerciseController controller, boolean enableNextOnlyWhenBothCompleted) {
    return new NavigationHelper(exercise, controller, this, exerciseList,
        true,
        true,
        enableNextOnlyWhenBothCompleted,
        showPrevButton());
  }

  protected boolean showPrevButton() {
    return false;
  }

  void addInstructions() {
    add(new Heading(4, PROMPT));
  }

  /**
   * @param e
   * @return
   * @see #ExercisePanel
   */
  protected Widget getQuestionContent(T e) {
    String content = getExerciseContent(e);

    HTML maybeRTLContent = getMaybeRTLContent(content);
    maybeRTLContent.addStyleName("rightTenMargin");
    maybeRTLContent.addStyleName("topMargin");

    return (content.length() > 200) ? getContentScroller(maybeRTLContent) : maybeRTLContent;
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
  protected HTML getMaybeRTLContent(String content) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction = rightAlignContent ? HasDirection.Direction.RTL : getDirection(content);

    HTML html = new HTML(content, direction);
//    html.setWidth("100%");
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

  protected boolean isRTL(T exercise) {
    return isRTLContent(exercise.getFLToShow());
  }

  private boolean isRTLContent(String content) {
    return controller.isRightAlignContent() || getDirection(content) == HasDirection.Direction.RTL;
  }

  private HasDirection.Direction getDirection(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content);
  }

  private boolean isPashto() {
    return controller.getLanguage().equalsIgnoreCase("Pashto");
  }

  public void onResize() {
    logger.info("Got onResize");
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
   * @param controller used in subclasses for audio control
   */
  private void addQuestions(T e, ExerciseController controller) {
    add(getQuestionPanel(e, controller, 1));
  }

  private Panel getQuestionPanel(T exercise, ExerciseController controller, int questionNumber) {
    Panel vp = new VerticalPanel();
    // add answer widget
    vp.add(getAnswerWidget(exercise, controller, questionNumber));
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
    if (objects == null) indexToWidgets.put(index, objects = new HashSet<>());
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

  Widget getAnswerWidget(final T exercise,
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
  void recordIncomplete(Widget answer) {
    if (!completed.remove(answer) && !completed.isEmpty()) {
      logger.info("recordIncomplete : huh? answer " + answer.getElement().getId() +
          " is not on list of size " + completed.size());
      for (Widget widget : completed) {
        logger.info("recordIncomplete : known : " + widget.getElement().getId());
      }
    }
    // logger.info("recordIncomplete : completed " + completed.size() + " vs total " + answers.size());
    enableNext();
  }

  /**
   * @param answer
   * @see mitll.langtest.client.exercise.WaveformPostAudioRecordButton#useResult(AudioAnswer)
   */
  void recordCompleted(Widget answer) {
    completed.add(answer);
/*    logger.info("recordCompleted : id " + answer.getElement().getId() +
        " completed " + completed.size() + " vs total " + answers.size());*/

    if (completed.size() > answers.size()) {
      logger.warning("recordCompleted huh? more complete " + completed.size() + " than answers " + answers.size());
    }

    enableNext();
  }

  protected void enableNext() {
//    logger.info("enableNext : answered " + completed.size() + " vs total " + answers.size());
    navigationHelper.enableNextButton(isCompleted());
  }

  protected boolean isCompleted() {
    boolean b = completed.size() == answers.size();
//    if (b) {
//      logger.info("isCompleted : answered " + completed.size() + " vs total " + answers.size() + " : " + b);
//    }
    return b;
  }

  void setButtonsEnabled(boolean val) {
    enableNext();
  }
}
