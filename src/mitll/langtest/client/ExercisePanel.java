package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Note that for text input answers, the user is prevented from cut-copy-paste.
 *
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanel extends VerticalPanel implements ExerciseQuestionState {
  private static final String ANSWER_BOX_WIDTH = "400px";
  private List<Widget> answers = new ArrayList<Widget>();
  private Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private Button next;
  /**
   * @see ExercisePanelFactory#getExericsePanel
   * @see ExerciseList#loadExercise
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public ExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller) {
    this.exercise = e;
    add(new HTML("<h3>Item #" + e.getID() + "</h3>"));

    // attempt to left justify
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hp.add(new HTML(e.getContent()));
    hp.setWidth((Window.getClientWidth()-LangTest.EXERCISE_LIST_WIDTH-100) + "px");
    add(hp);

    int i = 1;
    this.next = new Button("Next");
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    for (Exercise.QAPair pair : e.getQuestions()) {
      // add question header
      String questionHeader = "Question #" + (i++) + " : " + pair.getQuestion();
      add(new HTML("<h4>" + questionHeader + "</h4>"));

      // add question prompt
      VerticalPanel vp = new VerticalPanel();
      vp.add(new HTML(getQuestionPrompt(e)));
      SimplePanel spacer = new SimplePanel();
      spacer.setSize("500px", "20px");
      vp.add(spacer);

      // add answer widget
      Widget answerWidget = getAnswerWidget(e, service, controller, i-1);
      vp.add(answerWidget);
      answers.add(answerWidget);

      add(vp);
    }
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("500px", "20px");
    add(spacer);

    HorizontalPanel buttonRow = getNextAndPreviousButtons(e, service, userFeedback, controller);
    add(buttonRow);
  }

  private HorizontalPanel getNextAndPreviousButtons(final Exercise e, final LangTestDatabaseAsync service,
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

  protected String getQuestionPrompt(Exercise e) {
    return "&nbsp;&nbsp;&nbsp;Type your answer in " +(e.promptInEnglish ? "english" : " the foreign language") +" :";
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
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
          userFeedback.showErrorMessage("Couldn't post answers for exercise.");
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

  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index) {
  //  GWT.log("getAnswerWidget for #" +index);
    final TextBox answer = new NoPasteTextBox();
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
    next.setEnabled((completed.size() == answers.size()));
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
