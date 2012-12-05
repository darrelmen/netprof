package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
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
public class ExercisePanel extends VerticalPanel implements ExerciseQuestionState, ProvidesResize, RequiresResize {
  private static final String ANSWER_BOX_WIDTH = "400px";
  private List<Widget> answers = new ArrayList<Widget>();
  private Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  protected ExerciseController controller;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private Button next;
  protected LangTestDatabaseAsync service;

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
    //VerticalPanel left = new VerticalPanel();
    add(new HTML("<h3>Item #" + e.getID() + "</h3>"));

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(getQuestionContent(e));
    add(hp);

    int i = 1;

    addQuestions(e, service, controller, i);

    SimplePanel spacer = new SimplePanel();
    spacer.setSize("50px", "20px");
    add(spacer);

    Panel buttonRow = getNextAndPreviousButtons(e, service, userFeedback, controller);
    add(buttonRow);
  }

  protected Widget getQuestionContent(Exercise e) {
    return new HTML(e.getContent());
  }

  public void onResize() {
  }

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
   * @param i
   */
  private void addQuestions(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int i) {
    List<Exercise.QAPair> englishQuestions = e.getEnglishQuestions();
    List<Exercise.QAPair> flQuestions = e.getForeignLanguageQuestions();
    int n = englishQuestions.size();
    //System.out.println("eng q " + englishQuestions);
    for (Exercise.QAPair pair : e.getQuestions()) {
      // add question header
      Exercise.QAPair engQAPair = englishQuestions.get(i - 1);

      getQuestionHeader(i, n, engQAPair, shouldShowAnswer(),!controller.isDemoMode());
      if (controller.isDemoMode()) {
        Exercise.QAPair flQAPair  = flQuestions.get(i - 1);
        getQuestionHeader(i, n, flQAPair, pair, shouldShowAnswer());
      }
      i++;
      // add question prompt
      VerticalPanel vp = new VerticalPanel();
      addQuestionPrompt(vp, e);

      // add answer widget
      Widget answerWidget = getAnswerWidget(e, service, controller, i-1);
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
              " : &nbsp;&nbsp;</b>" + alternate));
        }
        if (addSpacerAfter) add(new HTML("<br></br>"));
      } else {
        add(new HTML("<b>Answer : &nbsp;&nbsp;</b>" + answer + (addSpacerAfter ? "<br></br>" : "")));
      }
    }
    else {
      String questionHeader = prefix + question;
      add(new HTML("<h4>" + questionHeader + "</h4>"));
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
    return "&nbsp;&nbsp;&nbsp;Type your answer in " +(promptInEnglish ? "english" : " the foreign language") +" :";
  }

  protected String getSpokenPrompt(boolean promptInEnglish) {
    return "&nbsp;&nbsp;&nbsp;Speak and record your answer in " +(promptInEnglish ? "English" : " the foreign language") +" :";
  }

  protected int getQuestionPromptSpacer() {
    return 20;
  }

  private Panel getNextAndPreviousButtons(final Exercise e, final LangTestDatabaseAsync service,
                                          final UserFeedback userFeedback, final ExerciseController controller) {
    HorizontalPanel buttonRow = new HorizontalPanel();

    Button prev = new Button("Previous");
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        controller.loadPreviousExercise(e);
      }
    });
    prev.setEnabled(!controller.onFirst(e));
    buttonRow.add(prev);

    this.next = new Button("Next");
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    buttonRow.add(next);
    DOM.setElementAttribute(next.getElement(), "id", "nextButton");

    // send answers to server
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        postAnswers(service, userFeedback, controller, e);
      }
    });
    return buttonRow;
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
   * <br></br>
   * Loads next exercise after the post.
   *
   * @see ExercisePanel#getNextAndPreviousButtons(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
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
    incomplete.addAll(answers);
    for (final Widget tb : answers) {
      service.addAnswer(user, exercise, i++, ((TextBox)tb).getText(), "", new AsyncCallback<Void>() {
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

  /**
   * Remember to have the text default to RTL direction if the prompt is in the foreign (Arabic) language.
   * <br></br>
   * Keeps track of which text fields have been typed in, so we can enable/disable the next button.
   *
   * @see #addQuestions(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   * @param exercise here used to determine the prompt language (English/FL)
   * @param service used in subclasses
   * @param controller  used in subclasses
   * @param index of the question (0 for first, 1 for second, etc.) (used in subclasses)
   * @return widget that handles the answer
   */
  protected Widget getAnswerWidget(final Exercise exercise, final LangTestDatabaseAsync service, ExerciseController controller, final int index) {
  //  GWT.log("getAnswerWidget for #" +index);
    boolean allowPaste = controller.isDemoMode();
    final TextBox answer = allowPaste ? new TextBox() : new NoPasteTextBox();
    answer.setWidth(ANSWER_BOX_WIDTH);
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
      HorizontalPanel hp = new HorizontalPanel();
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
          service.getScoreForAnswer(exercise, index, answer.getText(), new AsyncCallback<Double>() {
            @Override
            public void onFailure(Throwable caught) {}
            @Override
            public void onSuccess(Double result) {
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
          });
        }
      });
      return hp;
    }
    return answer;
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
    //  GWT.log("Text box got " + event);
      super.onBrowserEvent(event);

      if (event.getTypeInt() == Event.ONPASTE) {
        event.stopPropagation();
        event.preventDefault();
      }
    }
  }
}
