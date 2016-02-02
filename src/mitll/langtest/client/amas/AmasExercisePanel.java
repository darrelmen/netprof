package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.*;
import java.util.logging.Logger;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 * <p>
 * Subclassed to provide for audio recording and playback {@link mitll.langtest.client.recorder.SimpleRecordExercisePanel} and
 * grading of answers {@linkx mitll.langtest.client.grading.GradingExercisePanel}
 * <p>
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AmasExercisePanel extends VerticalPanel implements
    PostAnswerProvider<AmasExerciseImpl>, ProvidesResize, RequiresResize, ExerciseQuestionState {
  private Logger logger = Logger.getLogger("AmasExercisePanel");

  private static final int QUESTION_WIDTH = 700;
  private static final int CONTENT_SCROLL_HEIGHT = 200;

  private static final String PROMPT  = "Read the following text and answer the question below.";
  private static final String PROMPT2 = "Read the following text and answer the questions below.";

  private final List<Widget> answers = new ArrayList<Widget>();

  private Set<Tab> completedTabs = new HashSet<>();

  protected AmasExerciseImpl exercise = null;
  protected final ExerciseController controller;

  protected final LangTestDatabaseAsync service;
  protected final NavigationHelper navigationHelper;
  protected final ResponseExerciseList<AmasExerciseImpl> exerciseList;
  protected final Map<Integer, Widget> indexToWidget = new HashMap<Integer, Widget>();

  protected Map<Integer, Tab> indexToTab = new TreeMap<Integer, Tab>();
  private TabPanel tabPanel;
  protected Integer currentTab = 1;

  /**
   * @param e
   * @param service
   * @param controller
   * @param exerciseList
   * @see FeedbackRecordPanel#FeedbackRecordPanel
   */
  protected AmasExercisePanel(final AmasExerciseImpl e, final LangTestDatabaseAsync service,
                              final ExerciseController controller, ResponseExerciseList exerciseList) {
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
    Boolean listening = e.getUnitToValue().containsValue("Listening");

    int numQuestions = e.getQuestions().size();
    if (!listening) {
      addInstructions(numQuestions);
    }
    add(hp);

    addQuestions(e, service, controller);

    // add next and prev buttons
    add(navigationHelper);
    navigationHelper.addStyleName("topMargin");
    getElement().setId("AmasExercisePanel");
  }

  private NavigationHelper getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper(exercise, controller, this, exerciseList, true, true);
  }

  private void addInstructions(int numQuestions) {
    Heading prompt = new Heading(4, numQuestions == 1 ? PROMPT : PROMPT2);
    DivWidget widgets = new DivWidget();
    Heading itemHeader = getItemHeader(exerciseList.getIndex(exercise.getID()), exerciseList.getSize());
    widgets.add(itemHeader);
    itemHeader.addStyleName("floatLeft");
    widgets.add(prompt);
    prompt.addStyleName("floatRight");

    add(widgets);
  }

  /**
   * @param e
   * @return
   * @see #AmasExercisePanel(AmasExerciseImpl, LangTestDatabaseAsync, ExerciseController, ListInterface)
   */
  protected abstract Widget getQuestionContent(AmasExerciseImpl e);

  protected Widget getContentScroller(Widget maybeRTLContent) {
    ScrollPanel scroller = new ScrollPanel(maybeRTLContent);

    scroller.setAlwaysShowScrollBars(true);
    scroller.getElement().getStyle().setBorderColor("black");
    scroller.getElement().getStyle().setBorderWidth(1, Style.Unit.PX);
    scroller.getElement().getStyle().setBorderStyle(Style.BorderStyle.SOLID);
    scroller.getElement().setId("contentScroller");
    scroller.setHeight(CONTENT_SCROLL_HEIGHT + "px");

    maybeRTLContent.getParent().getElement().getStyle().setPaddingRight(5, Style.Unit.PX);

    return scroller;
  }

  /**
   * @param content
   * @param width
   * @return
   * @see #getQuestionContent(mitll.langtest.shared.AmasExerciseImpl)
   * @see #getQuestionHeader
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getQuestionContent
   */
  protected Widget getMaybeRTLContent(String content, int width) {
    content = content.replaceAll("<p> &nbsp; </p>", "").replaceAll("<h4>","").replaceAll("</h4>","");

    Widget widget = new Heading(4, content);
    widget.getElement().setId("questionContentContainer");
    widget.getElement().getStyle().setLineHeight(2, Style.Unit.EM);
    widget.setWidth(width + "px");
    if (controller.isRightAlignContent()) {
      widget.addStyleName("rightAlign");
    }

    widget.addStyleName("wrapword");
    widget.addStyleName("rightTenMargin");
    widget.addStyleName("leftTenMargin");
    widget.addStyleName(controller.getLanguage().toLowerCase());

    return widget;
  }

  private Heading getItemHeader(int index, int totalInQuiz) {
    String text = AudioExerciseContent.ITEM + " #" + (index + 1) + " of " + totalInQuiz;
    Heading child = new Heading(5, text);
    child.getElement().setId("default_item_id_" + index + "_" + totalInQuiz);
    child.addStyleName("leftTenMargin");
    child.addStyleName("floatLeft");
    child.addStyleName("rightFiveMargin");
    return child;
  }

  public void onResize() {}

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
   * @param controller     used in subclasses for audio control
   * @paramx questionNumber
   */
  private void addQuestions(AmasExerciseImpl e, LangTestDatabaseAsync service, ExerciseController controller) {
    List<QAPair> englishQuestions = e.getEnglishQuestions();
    int n = englishQuestions.size();
    if (e.getQuestions().size() == 1) {
      QAPair questionToShow = e.getQuestions().iterator().next();
      Panel questionPanel = getQuestionPanel(e, service, controller, 1, n, englishQuestions, questionToShow
      //    , this
      );
      add(questionPanel);
      addTab(1, new Tab());
    } else {
      add(tabPanel = makeTabPanel(e, service, controller,  englishQuestions, n));
    }
  }

  /**
   * TODO : how to make tab panel more obvious
   *
   * @param e
   * @param service
   * @param controller
   * @paramx questionNumber
   * @param englishQuestions
   * @param n
   * @return
   * @see #addQuestions
   */
  private TabPanel makeTabPanel(AmasExerciseImpl e, final LangTestDatabaseAsync service, ExerciseController controller,
                                List<QAPair> englishQuestions,
                                int n) {
    int questionNumber = 1;
    TabPanel tabPanel = new TabPanel();
    tabPanel.getWidget(0).getElement().getStyle().setMarginBottom(0, Style.Unit.PX);

    for (QAPair pair : e.getQuestions()) {
      Tab tabPane = new Tab();
      tabPane.setHeading("Question #" + questionNumber);
      tabPanel.add(tabPane);
      addTab(questionNumber, tabPane);
      tabPane.add(getQuestionPanel(e, service, controller, questionNumber++, n, englishQuestions, pair
      ));
    }

    tabPanel.addShownHandler(new TabPanel.ShownEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShownEvent shownEvent) {
        for (Map.Entry<Integer, Tab> pair : indexToTab.entrySet()) {
          if (pair.getValue().asTabLink() == shownEvent.getTarget()) {
            currentTab = pair.getKey();
            enableNext();
          }
        }
      }
    });

    tabPanel.selectTab(0);
    return tabPanel;
  }


  /**
   * @see #makeTabPanel
   * @param exercise
   * @param service
   * @param controller
   * @param questionNumber
   * @param n
   * @param englishQuestions
   * @param pair
   * @paramx toAddTo
   * @return
   */
  private Panel getQuestionPanel(AmasExerciseImpl exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                 int questionNumber,
                                 int n,
                                 List<QAPair> englishQuestions,
                                 QAPair pair) {
    QAPair engQAPair = questionNumber - 1 < n ? englishQuestions.get(questionNumber - 1) : null;

    Panel vp = new VerticalPanel();
    if (engQAPair != null) {
      getQuestionHeader(n, pair, shouldShowAnswer(), vp);
    } else {
      vp.add(new Heading(6, ""));
    }

    // add answer widget
    vp.add(getAnswerWidget(exercise, service, controller, questionNumber));
    vp.addStyleName("userNPFContent2");
    return vp;
  }

  /**
   * @param total
   * @param qaPair
   * @param showAnswer
   * @see #getQuestionPanel
   */
  private void getQuestionHeader(int total, QAPair qaPair, boolean showAnswer, HasWidgets toAddTo) {
    String question = qaPair.getQuestion();
    String prefix = (total == 1) ? ("Question : ") : "";

    if (showAnswer) {
      showAnswers(qaPair, toAddTo, prefix, question);
    } else {
      Widget maybeRTLContent = getMaybeRTLContent(prefix + question, QUESTION_WIDTH);
      maybeRTLContent.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
      toAddTo.add(maybeRTLContent);
    }
  }

  /**
   * @param qaPair
   * @param toAddTo
   * @param prefix
   * @param question
   * @return
   * @see #getQuestionHeader
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getAnswerWidget
   */
  protected Widget showAnswers(QAPair qaPair, HasWidgets toAddTo, String prefix, String question) {
    return new ShowAnswers(controller.getLanguage().toLowerCase()).showAnswers(qaPair, toAddTo, prefix, question);
  }

  /**
   * @param index
   * @param answerWidget
   * @see #getAnswerWidget(mitll.langtest.shared.AmasExerciseImpl, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#getStudentAnswer(LangTestDatabaseAsync, int)
   */
  protected void addAnswerWidget(int index, Widget answerWidget) {
    answers.add(answerWidget);
    indexToWidget.put(index, answerWidget);
  }

  protected abstract Widget getAnswerWidget(final AmasExerciseImpl exercise, final LangTestDatabaseAsync service,
                                            ExerciseController controller, final int index);

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
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addComboAnswer(AmasExerciseImpl, LangTestDatabaseAsync, ExerciseController, int, String, String, boolean)
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#getTextResponse(ExerciseController)
   */
  public void recordCompleted(Widget answer) {
    markTabComplete(getIndex(answer));
    enableNext();
  }

  /**
   * @see #recordCompleted(Widget)
   * @param answer
   * @return
   */
  private int getIndex(Widget answer) {
    for (Map.Entry<Integer, Widget> indexWidgetsPair : indexToWidget.entrySet()) {
      Widget widgetsForTab = indexWidgetsPair.getValue();
      if (widgetsForTab == answer) return indexWidgetsPair.getKey();
    }
    logger.warning("couldn't find " +answer.getElement().getId());
    return -1;
  }

  protected void markTabComplete(Integer tabIndex) {
   // logger.info("markTabComplete " +tabIndex);
    Tab tab = getTab(tabIndex);
   // logger.info("markTabComplete tab " +tab);

    tab.setIcon(IconType.CHECK);
    completedTabs.add(tab);

    if (completedTabs.size() > answers.size()) {
      logger.warning("recordCompleted huh? more complete " + completedTabs.size() + " than answers " + answers.size());
    }
  }

  /**
   * @see FeedbackRecordPanel#getScores
   */
  protected void selectFirstIncomplete() {
  //  logger.info("selectFirstIncomplete checking " +indexToTab.size() + " tabs");
    for (Map.Entry<Integer, Tab> pair : indexToTab.entrySet()) {
      if (!completedTabs.contains(pair.getValue())) {
        final Integer key = pair.getKey();
     //   logger.info("\tselectFirstIncomplete selecting tab #" +key);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            selectTab(key-1);
          }
        });
        break;
      }
    }
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#postAnswers(ExerciseController, AmasExerciseImpl)
   * @param index
   */
  private void selectTab(int index) {
    if (tabPanel != null) {
      tabPanel.selectTab(index);
    }
  //  else {
     // logger.warning("Tab panel is not defined???? \n\n\n");
    //}
  }

  private void addTab(int questionNumber, Tab tabPane) {
    indexToTab.put(questionNumber, tabPane);
  }

  protected Tab getTab(Integer tabIndex) {
    return indexToTab.get(tabIndex);
  }

  protected void enableNext() {
    navigationHelper.enableNextButton(isCompleted());
  }

  protected boolean isCompleted() {return completedTabs.size() == answers.size();  }

  private boolean shouldShowAnswer() {
    return controller.getProps().isDemoMode();
  }
}
