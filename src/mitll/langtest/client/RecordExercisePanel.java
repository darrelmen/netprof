package mitll.langtest.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.recorder.FlashRecordPanel;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class RecordExercisePanel extends ExercisePanel {
 // public static PopupPanel recordPopup;

 // FlashRecordPanel flashRecordPanel;
  public RecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                             final ExerciseController controller) {
    super(e,service,userFeedback,controller);
//    flashRecordPanel = new FlashRecordPanel();

/*
    recordPopup = new PopupPanel();
    recordPopup.setStyleName("RecordPopup");
    recordPopup.setWidget(flashRecordPanel);
    recordPopup.setPopupPosition(-100, -100);
    recordPopup.show();					//show it temporarily so that it's on the page
*/

 //   flashRecordPanel.initializeJS(GWT.getModuleBaseURL());
/*
    try {
      FlashRecordPanel.setRecordingInfo("test");
    } catch (Exception e) {
      GWT.log("FlashRecordPanel.init : Got " + e.getMessage());
    }*/
  }

  /**
   * @see ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, LangTestDatabaseAsync, UserFeedback, ExerciseController)
   * @param next
   * @param index
   * @return
   */
  protected Widget getAnswerWidget2(Button next, int index) {
    FlashRecordPanel r = new FlashRecordPanel("flashcontent");//_" + index);
/*    InlineHTML inline = new InlineHTML();
    String id = "flashcontent_" + index;
    inline.getElement().setId(id);*/

    return r;
    //return super.getAnswerWidget(next);    //To change body of overridden methods use File | Settings | File Templates.
  }


  protected void initWidget(Widget w, int index) {
    String id = "flashcontent";//_" + index;
    System.out.println("init widget " + /*w + */" at " + index + " with " + id);
    //FlashRecordPanel r = (FlashRecordPanel) w;

/*
    recordPopup = new PopupPanel();
    recordPopup.setStyleName("RecordPopup");
    recordPopup.setWidget(r);
    recordPopup.setPopupPosition(-100, -100);
    recordPopup.show();					//show it temporarily so that it's on the page
*/

   // r.initializeJS(GWT.getModuleBaseURL(), id);
  }
}
