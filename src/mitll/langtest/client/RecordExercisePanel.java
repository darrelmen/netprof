package mitll.langtest.client;

import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.recorder.SaveNotification;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordExercisePanel extends ExercisePanel {
  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";
  private ExerciseController controller;
  private LangTestDatabaseAsync service;

  /**
   * @see LangTest#loadExercise(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public RecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                             final ExerciseController controller) {
    super(e,service,userFeedback,controller);
    this.controller = controller;
    this.service = service;
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * It's made visible by a call in {@link mitll.langtest.client.recorder.FlashRecordPanel#saveComplete}
   *
   * @see mitll.langtest.client.recorder.FlashRecordPanel#setSaveCompleteFeedbackWidget
   * @see mitll.langtest.client.recorder.FlashRecordPanel#saveComplete()
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
   * @param exercise
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, final int index) {
    return new AnswerPanel(index);
  }

  private class AnswerPanel extends HorizontalPanel implements SaveNotification {
    private Image check;
    private int index;
    public AnswerPanel( final int index) {
      this.index = index;
      Image image = new Image("images/record.png");
      image.setAltText("Record");

      this.check = new Image(IMAGES_CHECKMARK);
      check.getElement().setId("checkmark_" +index);
      check.setAltText("Audio Saved");

      final ImageAnchor record_button = new ImageAnchor(index, this);
      record_button.setResource(image);

      record_button.setTitle("Record");
      add(record_button);

      SimplePanel spacer = new SimplePanel();
      spacer.setHeight("24px");
      spacer.setWidth("100px") ;

      add(spacer);

      add(check);
      check.setVisible(false);
    }

    /**
     * @see mitll.langtest.client.recorder.FlashRecordPanel#saveComplete()
     */
    public void gotSave() {
      final AnswerPanel outer = this;
      //System.err.println(controller.getUser() + " " + exercise.getPlan() + ", " + exercise.getID() + ", " + index);
      service.isAnswerValid(controller.getUser(),exercise,index,new AsyncCallback<Boolean>() {
        public void onFailure(Throwable caught) {
          System.err.println("huh? could ask answer validity?");
        }

        public void onSuccess(Boolean result) {
         // System.err.println(controller.getUser() + " " + exercise.getPlan() + ", " + exercise.getID() + ", " + index + " valid " + result);
          check.setVisible(false);
          if (result) {
            check.setUrl(IMAGES_CHECKMARK);
            check.setAltText("Audio Saved");
            recordCompleted(outer);
          }
          else {
            check.setUrl(IMAGES_REDX_PNG);
            check.setAltText("Audio Invalid");
            recordIncomplete(outer);
          }
          check.setVisible(true);
        }
      });
    }
  }

  /**
   * Remembers answerPanel image widget so we can show it when save is complete.
   */
  private class ImageAnchor extends Anchor implements MouseOverHandler {
    private final int index;
    private SaveNotification answerPanel;
    public ImageAnchor(int index, SaveNotification answerPanel) {
      this.index = index;
      this.answerPanel = answerPanel;
      addDomHandler(this, MouseOverEvent.getType());
    }
    public void setResource(Image img) {
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }

    public void onMouseOver(MouseOverEvent event) {
      controller.showRecorder(exercise,index, this, answerPanel);
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
}
