package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

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
  private Exercise exercise = null;
  //private UserFeedback errorReporter;

  public ExercisePanel(Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback) {
    this.exercise = e;
    add(new HTML("<h3>Item #" + e.getID() + "</h3>"));
    add(new HTML(e.getContent()));
    int i = 1;
    for (Exercise.QAPair pair : e.getQuestions()) {
      add(new HTML("<h4>Question #" + (i++) + " : " + pair.getQuestion() + "</h3>"));
      TextBox answer = new TextBox();
      answer.setWidth(ANSWER_BOX_WIDTH);
      //answer.addStyleName("input");
      answers.add(answer);
      add(answer);
    }
    Button submit = new Button("Submit");
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("500px", "20px");
    add(spacer);
    add(submit);
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
            }
          }
          );
        }
      }
    });
  }
}
