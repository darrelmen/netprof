/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Roughly mimics the Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 *
 * Communicates with saveFeedback interface when a save(upload) completes {@link mitll.langtest.client.RecordExercisePanel.AnswerPanel}
 *
 * @author Gordon Vidaver
 *
 */
public class FlashRecordPanelHeadless extends SimplePanel {
  private String id = "flashcontent";
  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
	public FlashRecordPanelHeadless(){
/*    save_button = new InlineHTML();
    save_button.getElement().setId("save_button");//"flashcontent");
    add(save_button);*/

   // SimplePanel flashContent = new SimplePanel();
		/*flashContent.*/getElement().setId(id);//"flashcontent");
    /*flashContent.*/setSize(240+"px",160+"px");

    InlineHTML inner = new InlineHTML();
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    add(inner);
  }

  /**
   * Doesn't seem to work
   */
  public void reset() {
    //GWT.log("reset widget state --- ");
    //save_button.setVisible(false);
//    play_button.setVisible(false);
  }

  /**
   * Remember widget so we can show it later when the save completes.
   * @see mitll.langtest.client.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, mitll.langtest.client.recorder.SaveNotification)
   * @param w
   */
  public static void setSaveCompleteFeedbackWidget(SaveNotification w) {
    //saveFeedback = w;
  }

  /**
   * @see mitll.langtest.client.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, mitll.langtest.client.recorder.SaveNotification)
   * @paramx userID
   * @paramx e
   * @paramx qid
   */
  //public void setUpload(long userID, Exercise e, int qid) { upload.setSlots(userID, e,qid); }

  private static class ImageAnchor extends Anchor {
    public ImageAnchor() {}
    public void setResource(Image img) {
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }

  public native void recordOnClick() /*-{
    $wnd.Recorder.record('audio', 'audio.wav');
  }-*/;

  public native void stopRecording() /*-{
    $wnd.Recorder.stop();
  }-*/;

  public native void playbackOnClick() /*-{
    $wnd.Recorder.playBack('audio');
  }-*/;

  public native void showPermission() /*-{
    $wnd.Recorder.showPermission();
  }-*/;

  public native JsArrayInteger getWav() /*-{
    $wnd.Recorder.getWav();
  }-*/;

  public void initialize(String moduleBaseURL) {
    initializeJS(moduleBaseURL, id);
  }

	public native void initializeJS(String moduleBaseURL, String id) /*-{
		var appWidth = 24;
		var appHeight = 24;
		
		var flashvars = {'event_handler': 'microphone_recorder_events', 'record_image': (moduleBaseURL + 'images/record.png'),'upload_image': (moduleBaseURL + 'images/upload.png'), 'stop_image': (moduleBaseURL + 'images/stop.png')};
		var params = {};
		var attributes = {'id': "recorderApp", 'name': "recorderApp"};

    $wnd.gotSaveComplete = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::saveComplete());
    $wnd.swfCallback = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::swfCallback());

    function outputStatus(e) {
      //alert("e.success = " + e.success +"\ne.id = "+ e.id +"\ne.ref = "+ e.ref);
      //swfCallback();  // TODO somehow this sometimes shows up as undefined...
    }
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "recorder.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, outputStatus);
	//	$wnd.createGoodWave = $entry(@com.pretest.client.FlashRecordPanel::createGoodWave());
	//	$wnd.showWaitStatus = $entry(@com.pretest.client.FlashRecordPanel::showWaitStatus());
	//	$wnd.setPausePlayButtonEnabled = $entry(@com.pretest.client.FlashRecordPanel::setPausePlayButtonEnabled(Z));


  }-*/;

/*  public static native void setRecordingInfo(String name) *//*-{
		$wnd.Recorder.setRecordingInfo(name);
	}-*//*;
	
	public static native void record(String name) *//*-{
		$wnd.Recorder.record(name);
	}-*//*;*/

  public static void saveComplete() {
//    System.out.println("Save is complete!");
   // saveFeedback.gotSave();
  }

  public static void swfCallback() {
    System.out.println("embedSWF is complete!");
  }
}
