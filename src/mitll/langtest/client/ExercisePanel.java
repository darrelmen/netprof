package mitll.langtest.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.DOM;
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
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/8/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExercisePanel extends VerticalPanel {
  private static final String ANSWER_BOX_WIDTH = "400px";
  private List<Widget> answers = new ArrayList<Widget>();
  private Set<Widget> completed = new HashSet<Widget>();
  protected Exercise exercise = null;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private Button next;
  /**
   * @see LangTest#loadExercise(mitll.langtest.shared.Exercise)
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
    hp.setWidth((LangTest.WIDTH-LangTest.EXERCISE_LIST_WIDTH-100) + "px");
    add(hp);

    int i = 1;
    this.next = new Button("Next");
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    for (Exercise.QAPair pair : e.getQuestions()) {
      Widget answerWidget = getAnswerWidget(e, i-1);
      String questionHeader = "Question #" + (i++) + " : " + pair.getQuestion();
      add(new HTML("<h4>" + questionHeader + "</h4>"));
      add(answerWidget);
      answers.add(answerWidget);
    }
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("500px", "20px");
    add(spacer);

    HorizontalPanel buttonRow = new HorizontalPanel();
    add(buttonRow);

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
  }

  /**
   * Record answers at the server.  For our purposes, add a row to the result table and possibly post
   * some audio and remember where it is.
   *
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

  protected Widget getAnswerWidget(Exercise exercise, int index) {
    GWT.log("getAnswerWidget for #" +index);
    final TextBox answer = new TextBox();
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

  protected void recordIncomplete(Widget answer) {
    completed.remove(answer);
    enableNext();
  }

  protected void recordCompleted(Widget answer) {
    completed.add(answer);
    enableNext();
  }

  private void enableNext() {
    next.setEnabled((completed.size() == answers.size()));
  }
}
