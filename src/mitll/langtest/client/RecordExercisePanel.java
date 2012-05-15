package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordExercisePanel extends ExercisePanel {
  ExerciseController controller;
  public RecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                             final ExerciseController controller) {
    super(e,service,userFeedback,controller);
    this.controller = controller;
  }

  /**
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
   * @param next
   * @param index
   * @return
   */
  protected Widget getAnswerWidget(Button next, final int index) {
    String id = "flashcontent_" + index;
    //System.out.println("getAnswerWidget widget " + /*w + */" at " + index + " with '" + id +"'");

   // FlashRecordPanel widget = new FlashRecordPanel(id);
    SimplePanel sp = new SimplePanel();

    Image image = new Image("images/record.png");
    image.setAltText("Record");
    final ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(image);

   // record_button.getElement().setId("record_button");
    record_button.setTitle("Record");
    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        System.out.println("showing " + "question_"+index);
        controller.showRecorder(exercise,index,record_button);
      }
    });
    sp.add(record_button);

    return sp;
  }

  private static class ImageAnchor extends Anchor {
    public ImageAnchor() {}
    public void setResource(Image img) {
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }

  /**
   * TODO : on the server, notice which audio posts have arrived, and take the latest ones...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }

  protected void initWidget(Widget w, int index) {
   // String id = "flashcontent_" + index;
   // System.out.println("init widget " + /*w + */" at " + index + " with '" + id +"'");
  //  FlashRecordPanel r = (FlashRecordPanel) w;
 //   r.initializeJS(GWT.getModuleBaseURL(), id);
  }
}
