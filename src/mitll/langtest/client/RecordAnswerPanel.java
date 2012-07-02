package mitll.langtest.client;

import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.recorder.SaveNotification;
import mitll.langtest.shared.Exercise;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 7/2/12
* Time: 7:15 PM
* To change this template use File | Settings | File Templates.
*/
class RecordAnswerPanel extends HorizontalPanel implements SaveNotification, MouseOverHandler {
  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";
  private Image check;
  private int index;
  private ImageAnchor record_button;
  //private SimpleRecordExercisePanel widgets;
  LangTestDatabaseAsync service;
  int user;
  Exercise exercise;
  SimpleRecordExercisePanel exercisePanel;

  public RecordAnswerPanel(LangTestDatabaseAsync service, int user, Exercise exercise, SimpleRecordExercisePanel exercisePanel,final int index) {
    this.service = service;
    this.user = user;
    this.exercise = exercise;
    this.exercisePanel = exercisePanel;

    this.index = index;
    Image image = new Image("images/record.png");
    image.setAltText("Record");

    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");

    record_button = new ImageAnchor();
    record_button.setResource(image);

    record_button.setTitle("Record");
    add(record_button);

    SimplePanel spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("100px") ;

    add(spacer);

    add(check);
    check.setVisible(false);
    addDomHandler(this, MouseOverEvent.getType());
  }

  private static class ImageAnchor extends Anchor {
    Image img = null;
    public ImageAnchor() {}
    public void setResource(Image img) {
      if (this.img != null) {
        DOM.removeChild(getElement(), img.getElement());
      }
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
      this.img = img;
    }
  }

  /**
   * @see mitll.langtest.client.recorder.FlashRecordPanel#saveComplete()
   */
  public void gotSave() {
    final RecordAnswerPanel outer = this;
    //System.err.println(controller.getUser() + " " + exercise.getPlan() + ", " + exercise.getID() + ", " + index);
    service.isAnswerValid(user, exercise, index, new AsyncCallback<Boolean>() {
      public void onFailure(Throwable caught) {
        System.err.println("huh? could ask answer validity?");
      }

      public void onSuccess(Boolean result) {
       // System.err.println(controller.getUser() + " " + exercise.getPlan() + ", " + exercise.getID() + ", " + index + " valid " + result);
        check.setVisible(false);
        if (result) {
          check.setUrl(IMAGES_CHECKMARK);
          check.setAltText("Audio Saved");
          exercisePanel.recordCompleted(outer);
        }
        else {
          check.setUrl(IMAGES_REDX_PNG);
          check.setAltText("Audio Invalid");
          exercisePanel.recordIncomplete(outer);
        }
        check.setVisible(true);
      }
    });
  }
  public void onMouseOver(MouseOverEvent event) {
    //controller.showRecorder(exercise, index, record_button, this);
  }
}
