package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.Answer;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackRecordPanel extends AmasExercisePanel {
  private Logger logger = Logger.getLogger("FeedbackRecordPanel");

  private static final String CHECK_ANSWER = "Check Answer";
  public static final int CONTENT_WIDTH = 535;

  /**
   * We don't want to grab the focus since we also have a list search box which would magically lose the focus.
   */
  private static final boolean GET_FOCUS = false;
  private static final String WERE_YOU_RIGHT = "Were you right?";
  private static final String AUDIO = "Audio";
  private List<TextResponse> textResponses;
  private QuizScorePanel quizScorePanel;
  private Set<Integer> selfScoreQuestions = new HashSet<Integer>();

  /**
   * @param e
   * @param service
   * @param controller
   * @see mitll.langtest.client.amas.AutoCRTChapterNPFHelper#getFactory
   */
  public FeedbackRecordPanel(AmasExerciseImpl e, LangTestDatabaseAsync service, ExerciseController controller,
                             ResponseExerciseList exerciseList,
                             QuizScorePanel quizScorePanel) {
    super(e, service, controller, exerciseList);
    getElement().setId("FeedbackRecordPanel");
    this.quizScorePanel = quizScorePanel;
    getScores(true);

    if (!controller.getProps().isAdminView()) {
      int clientWidth = Window.getClientWidth();
      int value = (int) (((float) clientWidth) * 0.638);
      value = Math.min(920, value);
      getElement().getStyle().setWidth(value, Style.Unit.PX);
    }
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    if (!controller.getProps().isAdminView()) {
      int clientWidth = Window.getClientWidth();
      int value = (int) (((float) clientWidth) * 0.09);
      value = Math.min(130, value);
      getParent().getElement().getStyle().setMarginLeft(value, Style.Unit.PX);
    }
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   * @see mitll.langtest.client.exercise.NavigationHelper#clickNext
   */
  @Override
  public void postAnswers(ExerciseController controller, Shell completedExercise) {
    if (isCompleted()) {
      exerciseList.loadNextExercise(completedExercise);
    } else {
      selectFirstIncomplete();
    }
  }

  /**
   * TODO : Kind of hacky to look for Listen in the content!
   *
   * @param e
   * @return
   * @see #AmasExercisePanel
   */
  protected Widget getQuestionContent(AmasExerciseImpl e) {
    String content = e.getContent();

    if (logger == null) {
      logger = Logger.getLogger("FeedbackRecordPanel");
    }
    Boolean listening = e.getUnitToValue().containsValue("Listening");

    if (listening) {
      int index = exerciseList.getIndex(e.getID());
      return new AudioExerciseContent().getQuestionContent(e, controller, true, false, content, index, exerciseList.getSize());
    } else {
      Widget maybeRTLContent = getMaybeRTLContent(content, CONTENT_WIDTH);
      maybeRTLContent.addStyleName("rightTenMargin");
      return getContentScroller(maybeRTLContent);
    }
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   * @see #getQuestionPanel
   */
  @Override
  protected Widget getAnswerWidget(AmasExerciseImpl exercise, final LangTestDatabaseAsync service, ExerciseController controller,
                                   final int index) {
    if (logger == null) {
      logger = Logger.getLogger("FeedbackRecordPanel");
    }
    if (textResponses == null) {
      textResponses = new ArrayList<TextResponse>();
    }
    String responseType = controller.getProps().getResponseType();

    // add the text input or audio input control
    AnswerPanel answerPanel = new AnswerPanel();

    Panel col = new VerticalPanel();
    col.getElement().setId("answerColumn");
    Widget widget = answerPanel.addComboAnswer(exercise, service, controller, index, responseType, CHECK_ANSWER, GET_FOCUS);
    col.add(widget);

    List<QAPair> questions = exercise.getForeignLanguageQuestions();

    // add the student self score section
    // show the key answers, and maybe the grade=1 answers?
    answerPanel.answers = showAnswers(questions.get(index - 1), col, "", "");
    answerPanel.answers.setVisible(false);

    // add student were you right section.
    col.add(answerPanel.getStudentAnswer(service, index));

    return col;
  }

  /**
   * @see #FeedbackRecordPanel(AmasExerciseImpl, LangTestDatabaseAsync, ExerciseController, ListInterface, QuizScorePanel)
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel
   */
  private void getScores(final boolean firstTime) {
    service.getScoresForUser(exerciseList.getTypeToSelection(), controller.getUser(), exerciseList.getIDs(), new AsyncCallback<QuizCorrectAndScore>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("didn't do scores?");
      }

      @Override
      public void onSuccess(QuizCorrectAndScore correctAndScores) {
        quizScorePanel.setScores(correctAndScores.getCorrectAndScoreCollection());
        if (firstTime) {
          makeTabStateReflectHistory(correctAndScores);
        }
      }
    });
  }

  private void makeTabStateReflectHistory(QuizCorrectAndScore correctAndScores) {
    for (CorrectAndScore cs : correctAndScores.getCorrectAndScoreCollection()) {
      if (cs.getId().equals(exercise.getID())) {
        int qid = cs.getQid();
    //    logger.info("makeTabStateReflectHistory found " + cs.getId() + " :  " + qid + " : " + cs);
        markTabComplete(qid);
      }
    }
    selectFirstIncomplete();
  }

  /**
   * @see #recordCompleted
   */
  @Override
  protected void enableNext() {
    boolean wasSelfScored = selfScoreQuestions.contains(currentTab);

    navigationHelper.enableNextButton(wasSelfScored);

    if (isCompleted()) {
      exerciseList.setState(exercise.getID(), STATE.APPROVED);
      exerciseList.redraw();
    }
  }

  /**
   * TODO : unnecessary? or should we turn return binding back on?
   */
  @Override
  protected void onUnload() {
    super.onUnload();
    for (TextResponse textResponse : textResponses) textResponse.onUnload();
  }

  private class AnswerPanel {
    private Widget answers;
    private Button yesChoice, noChoice;

    private Panel userAnswer;
    private long currentResultID;

    private Button getChoice(String title, ClickHandler handler) {
      Button onButton = new Button(title);

      onButton.getElement().setId("Choice_" + title);
      controller.register(onButton, exercise.getID());
      onButton.addClickHandler(handler);
      onButton.setActive(false);
      return onButton;
    }

    /**
     * @param exercise
     * @param service
     * @param controller
     * @param index
     * @param responseType
     * @param buttonTitle
     * @param getFocus
     * @return
     * @see #getAnswerWidget
     */
    private Widget addComboAnswer(final AmasExerciseImpl exercise, final LangTestDatabaseAsync service,
                                  final ExerciseController controller, final int index, String responseType,
                                  String buttonTitle, boolean getFocus) {
      if (responseType.equalsIgnoreCase(AUDIO)) {
        PressAndHoldExercisePanel autoCRTRecordPanel = new PressAndHoldExercisePanel(exercise.getID(),
            service,
            controller,
            new MySoundFeedback(controller), "Feedback", index, exerciseList.getTypeToSelection()) {
          @Override
          protected void showPronScoreFeedback(double score) {
            answers.setVisible(true);
            userAnswer.setVisible(true);
          }

          @Override
          public void receivedAudioAnswer(AudioAnswer result) {
            super.receivedAudioAnswer(result);
            currentResultID = result.getResultID();
            yesChoice.setActive(false);
            noChoice.setActive(false);
          }
        };

        return addAudioAnswer(autoCRTRecordPanel);
      } else { // TEXT!
        return doText(exercise, service, controller, index, buttonTitle, getFocus);
      }
    }

    private Widget addAudioAnswer(PressAndHoldExercisePanel autoCRTRecordPanel) {
      Panel panel = autoCRTRecordPanel;
      panel.setWidth("100%");
      FluidContainer outerContainer = new FluidContainer();
      outerContainer.add(panel);
      outerContainer.addStyleName("floatLeft");
      outerContainer.getElement().setId("FeedbackRecordPanel_outerContainer");
      outerContainer.getElement().getStyle().setMarginRight(10, Style.Unit.PX);
      ScoreFeedback scoreFeedback = addScoreFeedback(outerContainer, autoCRTRecordPanel);
      autoCRTRecordPanel.setScoreFeedback(scoreFeedback);
      return outerContainer;
    }

    /**
     * Allow to student to grade themselves.
     *
     * @param service
     * @see #getAnswerWidget
     */
    public Panel getStudentAnswer(final LangTestDatabaseAsync service, final int questionIndex) {
      final Panel userAnswer = new HorizontalPanel();
      userAnswer.getElement().setId("userAnswer");

      this.userAnswer = userAnswer;
      final Widget child = new Heading(4, WERE_YOU_RIGHT);
      child.addStyleName("topMargin");
      userAnswer.add(child);
      userAnswer.setVisible(false);

      ButtonToolbar toolbar = new ButtonToolbar();
      toolbar.getElement().setId("WasIRight");
      styleToolbar(toolbar);

      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.setToggle(ToggleType.RADIO);
      toolbar.add(buttonGroup);

      addAnswerWidget(questionIndex, userAnswer);

      final AsyncCallback<Void> async = new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(Void aVoid) {
          getScores(false);
        }
      };

      Button choice1 = getChoice(service, questionIndex, userAnswer, async, true, "Yes");
      choice1.setType(ButtonType.SUCCESS);
      buttonGroup.add(choice1);
      yesChoice = choice1;

      Button choice2 = getChoice(service, questionIndex, userAnswer, async, false, "No");
      buttonGroup.add(choice2);
      choice2.setType(ButtonType.DANGER);
      noChoice = choice2;

      userAnswer.add(toolbar);
      return userAnswer;
    }

    private Button getChoice(final LangTestDatabaseAsync service, final int questionIndex, final Panel userAnswer,
                             final AsyncCallback<Void> async, final boolean correct, String label) {
      Button choice1 = getChoice(label, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          service.addStudentAnswer(currentResultID, correct, async);
          selfScoreQuestions.add(questionIndex);
          recordCompleted(userAnswer);
          enableNext();
        }
      });
      return choice1;
    }

    private void styleToolbar(ButtonToolbar toolbar) {
      Style style = toolbar.getElement().getStyle();
      style.setMarginTop(6, Style.Unit.PX);
      style.setMarginBottom(10, Style.Unit.PX);
      style.setMarginLeft(5, Style.Unit.PX);
    }

    /**
     * @param outerContainer
     * @return
     * @see #addAudioAnswer
     */
    private ScoreFeedback addScoreFeedback(Panel outerContainer, PressAndHoldExercisePanel autoCRTRecordPanel) {
      ScoreFeedback scoreFeedback = new ScoreFeedback(true);
      Panel widgets = addScoreFeedback(scoreFeedback);
      autoCRTRecordPanel.getRecoOutputContainer().add(widgets);
      getFeedbackContainer(outerContainer);
      return scoreFeedback;
    }

    /**
     * @param scoreFeedback
     * @paramx container
     * @see #addScoreFeedback
     */
    private Panel addScoreFeedback(ScoreFeedback scoreFeedback) {
      SimplePanel simplePanel = new SimplePanel(scoreFeedback.getFeedbackImage());
      simplePanel.getElement().setId("feedbackImageContainer");
      simplePanel.addStyleName("floatLeft");

      Panel scoreFeedbackRow = scoreFeedback.getSimpleRow(simplePanel, 40);
      scoreFeedback.getScoreFeedback().setWidth(Window.getClientWidth() * 0.5 + "px");
      scoreFeedback.getScoreFeedback().addStyleName("topBarMargin");

      return scoreFeedbackRow;
    }

    /**
     * @param exercise
     * @param service
     * @param controller
     * @param index
     * @param buttonTitle
     * @param getFocus
     * @return
     * @see #addComboAnswer
     */
    private Panel doText(Shell exercise, final LangTestDatabaseAsync service, final ExerciseController controller,
                         int index,
                         String buttonTitle, boolean getFocus) {
      final TextResponse textResponse = getTextResponse(controller);
      textResponses.add(textResponse);

      Panel outerContainer = new DivWidget();
      outerContainer.getElement().setId("textAnswerColumn");

      outerContainer.addStyleName(controller.isRightAlignContent() ? "floatRight": "floatLeft");
      outerContainer.addStyleName("rightTenMargin");

      Panel row1 = new VerticalPanel();
      row1.getElement().setId("text_row1");
      outerContainer.add(row1);
      Widget widget = textResponse.addWidgets(row1, exercise.getID(), service, controller, false, true, false, index,
          buttonTitle, getFocus);
      widget.getElement().setId("textResponse_" + index);

      Panel row2 = new FluidRow();
      row2.getElement().setId("row2");

      outerContainer.add(row2);

      getFeedbackContainer(row2);

      return outerContainer;
    }

    /**
     * @param controller
     * @return
     * @see #doText
     */
    private TextResponse getTextResponse(final ExerciseController controller) {
      final TextResponse textResponse = new TextResponse(controller.getUser(), exerciseList.getTypeToSelection()) {
        /**
         * @see #getScoreForGuess
         * @paramx answerGiven
         * @param result
         */
        @Override
        protected void gotScoreForGuess(Answer result) {
          super.gotScoreForGuess(result);
          currentResultID = result.getResultID();
          yesChoice.setActive(false);
          noChoice.setActive(false);
        }
      };
      textResponse.setAnswerPostedCallback(
          new TextResponse.AnswerPosted() {
            @Override
            public void answerPosted() {
              answers.setVisible(true);
              userAnswer.setVisible(true);
            }
          });
      return textResponse;
    }

    private static final String WARN_NO_FLASH = "<font color='red'>Flash is not activated. " +
        "Do you have a flashblocker? Please add this site to its whitelist.</font>";

    private void getFeedbackContainer(Panel container) {
      HTML warnNoFlash = new HTML(WARN_NO_FLASH);
      warnNoFlash.setVisible(false);
      DivWidget row3 = new DivWidget();
      container.add(row3);
      row3.getElement().setId("warnNoFlash_row");
      row3.add(warnNoFlash);
    }
  }
}