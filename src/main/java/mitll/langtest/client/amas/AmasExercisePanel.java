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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.media.client.Audio;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;

import java.util.*;
import java.util.logging.Logger;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.<br></br>
 * <p>
 * Subclassed to provide for audio recording and playback and
 * grading of answers {@linkx mitll.langtest.client.grading.GradingExercisePanel}
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/23/16
 */
abstract class AmasExercisePanel extends VerticalPanel implements
    PostAnswerProvider, ProvidesResize, RequiresResize, ExerciseQuestionState {
  private static final String LISTEN_TO_THIS = "Listen to this audio and answer the question below";
  private final Logger logger = Logger.getLogger("AmasExercisePanel");

  private static final int QUESTION_WIDTH = 700;
  private static final int CONTENT_SCROLL_HEIGHT = 200;

  private static final String PROMPT_PREFIX = "Read the following text and answer the";
  private static final String PROMPT = PROMPT_PREFIX + " question below.";
  private static final String PROMPT2 = PROMPT_PREFIX + " questions below.";

  private final List<Widget> answers = new ArrayList<>();
  private final Set<Tab> completedTabs = new HashSet<>();

  AmasExerciseImpl exercise = null;
  final ExerciseController controller;
  final AmasNavigationHelper amasNavigationHelper;
  final ResponseExerciseList exerciseList;
  private final Map<Integer, Widget> indexToWidget = new HashMap<>();

  private final Map<Integer, Tab> indexToTab = new TreeMap<>();
  private TabPanel tabPanel;
  Integer currentTab = 1;

  /**
   * @param e
   * @param controller
   * @param exerciseList
   * @see FeedbackRecordPanel#FeedbackRecordPanel
   */
  AmasExercisePanel(final AmasExerciseImpl e,
                    final ExerciseController controller, ResponseExerciseList exerciseList) {
    this.exercise = e;
    this.controller = controller;
    this.exerciseList = exerciseList;
    this.amasNavigationHelper = getNavigationHelper(controller);

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
    } else if (e.getOrient() != null) {
      addInstructions("Orientation : " + e.getOrient(), true);
      logger.info("AmasExercisePanel for " + e.getOldID() + " audio " + e.getAudioURL());
      add(new Heading(4, LISTEN_TO_THIS));
      add(getAudioTag(e));
    }
    add(hp);

    addQuestions(e, controller.getService(), controller);

    // add next and prev buttons
    add(amasNavigationHelper);
    amasNavigationHelper.addStyleName("topMargin");
    getElement().setId("AmasExercisePanel");
  }

  /**
   *
   * @param e
   * @return
   */
  private Audio getAudioTag(AmasExerciseImpl e) {
    Audio ifSupported = Audio.createIfSupported();
    ifSupported.setControls(true);  // this is absolutely necessary - if missing, won't show up at all!
    String audioURL = e.getAudioURL();
    ifSupported.setSrc(audioURL);
    String suffix = audioURL.substring(audioURL.length()-3);
    ifSupported.getAudioElement().setAttribute("type","audio/"+suffix);
    return ifSupported;
  }

  private AmasNavigationHelper getNavigationHelper(ExerciseController controller) {
    return new AmasNavigationHelper(exercise, controller, this, exerciseList, true, true);
  }

  /**
   * @param numQuestions
   * @see #AmasExercisePanel(AmasExerciseImpl, ExerciseController, ResponseExerciseList)
   */
  private void addInstructions(int numQuestions) {
    addInstructions(numQuestions == 1 ? PROMPT : PROMPT2, false);
  }

  private void addInstructions(String text, boolean useLanguage) {
    DivWidget idInfoOnLeft = new DivWidget();
    Widget itemHeader = AudioExerciseContent.getItemHeader(exerciseList.getIndex(exercise.getID()),
        exerciseList.getSize(), exercise.getOldID());
    idInfoOnLeft.add(itemHeader);
    itemHeader.addStyleName("floatLeftAndClear");

    Heading promptOnRight = new Heading(4, text);

    if (useLanguage) {
      setLanguageSpecificFont(promptOnRight);
    }
    idInfoOnLeft.add(promptOnRight);
    promptOnRight.addStyleName("floatRight");

    add(idInfoOnLeft);
  }

  /**
   * @param e
   * @return
   * @see #AmasExercisePanel
   */
  protected abstract Widget getQuestionContent(AmasExerciseImpl e);

  Widget getContentScroller(Widget maybeRTLContent) {
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
   * @see #getQuestionContent
   * @see #getQuestionHeader
   * @see mitll.langtest.client.amas.FeedbackRecordPanel#getQuestionContent
   */
  Widget getMaybeRTLContent(String content, int width) {
    content = content.replaceAll("<p> &nbsp; </p>", "").replaceAll("<h4>", "").replaceAll("</h4>", "");

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
    setLanguageSpecificFont(widget);

    return widget;
  }

  private void setLanguageSpecificFont(Widget widget) {
    widget.addStyleName(controller.getLanguage().toLowerCase());

    String fontFamily = controller.getProps().getFontFamily();
    if (!fontFamily.isEmpty()) {
      logger.info("using font family " +fontFamily);
      widget.getElement().getStyle().setProperty("fontFamily",fontFamily);
    }
    else {
      logger.info("not using font family property");
    }
  }

/*  private Heading getItemHeader(int index, int totalInQuiz, String id) {
    String text = AudioExerciseContent.ITEM + " #" + (index + 1) + " of " + totalInQuiz + " : " + id;
    Heading child = new Heading(5, text);
    child.getElement().setId("default_item_id_" + index + "_" + totalInQuiz);
    child.addStyleName("leftTenMargin");
    child.addStyleName("floatLeft");
    child.addStyleName("rightFiveMargin");
    return child;
  }*/

  public void onResize() {
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
      add(tabPanel = makeTabPanel(e, service, controller, englishQuestions, n));
    }
  }

  /**
   * TODO : how to make tab panel more obvious
   *
   * @param e
   * @param service
   * @param controller
   * @param englishQuestions
   * @param n
   * @return
   * @paramx questionNumber
   * @see #addQuestions
   */
  private TabPanel makeTabPanel(AmasExerciseImpl e, final LangTestDatabaseAsync service, ExerciseController controller,
                                List<QAPair> englishQuestions,
                                int n) {
    int questionNumber = 1;
    TabPanel tabPanel = new TabPanel();
    tabPanel.getWidget(0).getElement().getStyle().setMarginBottom(0, Style.Unit.PX);

    // logger.info("for " +e.getOldID() + " got " + e.getQuestions().size());
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
   * @param exercise
   * @param service
   * @param controller
   * @param questionNumber
   * @param n
   * @param englishQuestions
   * @param pair
   * @return
   * @paramx toAddTo
   * @see #makeTabPanel
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
   * @see mitll.langtest.client.amas.FeedbackRecordPanel#getAnswerWidget
   */
  Widget showAnswers(QAPair qaPair, HasWidgets toAddTo, String prefix, String question) {
    return new ShowAnswers(controller.getLanguage().toLowerCase()).showAnswers(qaPair, toAddTo, prefix, question);
  }

  /**
   * @param index
   * @param answerWidget
   * @see #getAnswerWidget
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#getStudentAnswer(LangTestDatabaseAsync, int)
   */
  void addAnswerWidget(int index, Widget answerWidget) {
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
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#addComboAnswer(AmasExerciseImpl, LangTestDatabaseAsync, ExerciseController, int, String, String, boolean)
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#getTextResponse(ExerciseController)
   */
  public void recordCompleted(Widget answer) {
    markTabComplete(getIndex(answer));
    enableNext();
  }

  /**
   * @param answer
   * @return
   * @see #recordCompleted(Widget)
   */
  private int getIndex(Widget answer) {
    for (Map.Entry<Integer, Widget> indexWidgetsPair : indexToWidget.entrySet()) {
      Widget widgetsForTab = indexWidgetsPair.getValue();
      if (widgetsForTab == answer) return indexWidgetsPair.getKey();
    }
    logger.warning("couldn't find " + answer.getElement().getId());
    return -1;
  }

  void markTabComplete(Integer tabIndex) {
    logger.info("markTabComplete " + tabIndex);
    Tab tab = getTab(tabIndex);
    if (tab == null) logger.warning("no tab with index " + tabIndex);
    else {
      logger.info("markTabComplete tab " + tab);

      tab.setIcon(IconType.CHECK);
      completedTabs.add(tab);

      if (completedTabs.size() > answers.size()) {
        logger.warning("recordCompleted huh? more complete " + completedTabs.size() + " than answers " + answers.size());
      }
    }
  }

  /**
   * @see FeedbackRecordPanel#getScores
   */
  void selectFirstIncomplete() {
    //  logger.info("selectFirstIncomplete checking " +indexToTab.size() + " tabs");
    for (Map.Entry<Integer, Tab> pair : indexToTab.entrySet()) {
      if (!completedTabs.contains(pair.getValue())) {
        final Integer key = pair.getKey();
        //   logger.info("\tselectFirstIncomplete selecting tab #" +key);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            selectTab(key - 1);
          }
        });
        break;
      }
    }
  }

  /**
   * @param index
   * @see mitll.langtest.client.amas.FeedbackRecordPanel#postAnswers
   */
  private void selectTab(int index) {
    if (tabPanel != null) {
      tabPanel.selectTab(index);
    }
  }

  private void addTab(int questionNumber, Tab tabPane) {
    indexToTab.put(questionNumber, tabPane);
  }

  private Tab getTab(Integer tabIndex) {
    return indexToTab.get(tabIndex);
  }

  void enableNext() {
    amasNavigationHelper.enableNextButton(isCompleted());
  }

  boolean isCompleted() {
    return completedTabs.size() == answers.size();
  }

  private boolean shouldShowAnswer() {
    return controller.getProps().isDemoMode();
  }
}
