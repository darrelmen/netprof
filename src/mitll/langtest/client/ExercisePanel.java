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
  private boolean enableNextOnlyWhenAllCompleted = true;
  public ExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                       final ExerciseController controller) {
    this.exercise = e;
    add(new HTML("<h3>Item #" + e.getID() + "</h3>"));
    add(new HTML(e.getContent()));
    int i = 1;
    final Button next = new Button("Next");
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    for (Exercise.QAPair pair : e.getQuestions()) {
      add(new HTML("<h4>Question #" + (i++) + " : " + pair.getQuestion() + "</h3>"));
      final TextBox answer = new TextBox();
      answer.setWidth(ANSWER_BOX_WIDTH);
      answers.add(answer);
      if (enableNextOnlyWhenAllCompleted) {  // make sure user entered in answers for everything
        answer.addKeyUpHandler(new KeyUpHandler() {
          public void onKeyUp(KeyUpEvent event) {
            if (answer.getText().length() > 0) {
              completed.add(answer);
            } else {
              completed.remove(answer);
            }
            next.setEnabled((completed.size() == answers.size()));
          }
        });
      }
      add(answer);
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
  //  next.addStyleName("paddedHorizontalPanel");
    DOM.setElementAttribute(next.getElement(), "id", "nextButton");
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int i = 1;
        for (TextBox tb : answers) {
          service.addAnswer(exercise, i++, tb.getText(), new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
              userFeedback.showErrorMessage("Couldn't post answers for exercise.");
            }

            public void onSuccess(Void result) {
              controller.loadNextExercise(e);
            }
          }
          );
        }
      }
    });
  }
}
