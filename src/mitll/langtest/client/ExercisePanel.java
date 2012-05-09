package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
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
  private List<TextBox> answers = new ArrayList<TextBox>();
  private Set<TextBox> completed = new HashSet<TextBox>();
  private Exercise exercise = null;

  public ExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller) {
    this.exercise = e;
    add(new HTML("<h3>Item #" + e.getID() + "</h3>"));
    add(new HTML(e.getContent()));
    int i = 1;
    final Button submit = new Button("Submit");
    submit.setEnabled(false);
    for (Exercise.QAPair pair : e.getQuestions()) {
      add(new HTML("<h4>Question #" + (i++) + " : " + pair.getQuestion() + "</h3>"));
      final TextBox answer = new TextBox();
      answer.setWidth(ANSWER_BOX_WIDTH);
      //answer.addStyleName("input");
      answers.add(answer);
      answer.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          if (answer.getText().length() > 0) { completed.add(answer); } else { completed.remove(answer); }
          submit.setEnabled((completed.size() == answers.size()));
        }
      });
      add(answer);
    }
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("500px", "20px");
    add(spacer);

    HorizontalPanel buttonRow = new HorizontalPanel();
    //buttonRow.addStyleName("paddedHorizontalPanel");
    add(buttonRow);
    buttonRow.add(submit);

    final Button next = new Button("Next");
    DOM.setElementAttribute(next.getElement(), "id", "nextButton");
    submit.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int i = 1;
        for (TextBox tb : answers) {
          service.addAnswer(exercise.getID(), i++, tb.getText(), new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
              userFeedback.showErrorMessage("Couldn't post answers for exercise.");
            }

            public void onSuccess(Void result) {
              userFeedback.showStatus("Answers entered.");
              next.setEnabled(true);
              submit.setEnabled(false);
            }
          }
          );
        }
      }
    });
    next.setEnabled(false);
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (controller.loadNextExercise(e)) { // we're on the last one

        }
      }
    });
    buttonRow.add(next);
 //   next.addStyleName("paddedHorizontalPanel");
  }
}
