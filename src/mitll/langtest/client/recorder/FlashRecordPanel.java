/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.Exercise;

/**
 * @author gregbramble
 *
 */
public class FlashRecordPanel extends FlowPanel {
  boolean showStatus = false;
  boolean showUploadStatus = false;
  private final UploadForm upload;
  private static Widget saveFeedback;  // remember for later

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
	public FlashRecordPanel(String id){
    InlineHTML save_button = new InlineHTML();
    save_button.getElement().setId("save_button");//"flashcontent");
    add(save_button);

    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id);//"flashcontent");

    InlineHTML inner = new InlineHTML();
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    flashContent.add(inner);

		SimplePanel statusPanel = new SimplePanel();
		statusPanel.getElement().setId("status");
		add(statusPanel);
    statusPanel.setVisible(showStatus);
    // record
    Image image = new Image("images/record.png");
    image.setAltText("Record");
    ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(image);

    record_button.getElement().setId("record_button");
    record_button.setTitle("Record");
    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        recordOnClick();
      }
    });
    add(record_button);

    add(flashContent);

    //playback

    Image image2 = new Image("images/play.png");
    image2.setAltText("Play");
    ImageAnchor play_button = new ImageAnchor();
    play_button.setResource(image2);

    play_button.getElement().setId("play_button");
    play_button.setTitle("Play");
    play_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        playbackOnClick();
      }
    });
    add(play_button);

    SimplePanel uploadStatusPanel = new SimplePanel();
		uploadStatusPanel.getElement().setId("upload_status");
		add(uploadStatusPanel);
    uploadStatusPanel.setVisible(showUploadStatus);

    upload = new UploadForm();
    add(upload);
  }

  /**
   * Remember widget so we can show it later when the save completes.
   * @param w
   */
  public static void setSaveCompleteFeedbackWidget(Widget w) {
    saveFeedback = w;
  }

  /**
   * @see mitll.langtest.client.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, com.google.gwt.user.client.ui.Widget)
   * @param e
   * @param qid
   * @param userID
   */
  public void setUpload(Exercise e, int qid, int userID) { upload.setSlots(e,qid, userID); }

  private static class ImageAnchor extends Anchor {
    public ImageAnchor() {}
    public void setResource(Image img) {
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }

  public native void recordOnClick() /*-{
    $wnd.Recorder.record('audio', 'audio.wav');
  }-*/;

  public native void playbackOnClick() /*-{
    $wnd.Recorder.playBack('audio');
  }-*/;

	public native void initializeJS(String moduleBaseURL, String id) /*-{
		var appWidth = 24;
		var appHeight = 24;
		
		var flashvars = {'event_handler': 'microphone_recorder_events', 'record_image': (moduleBaseURL + 'images/record.png'),'upload_image': (moduleBaseURL + 'images/upload.png'), 'stop_image': (moduleBaseURL + 'images/stop.png')};
		var params = {};
		var attributes = {'id': "recorderApp", 'name': "recorderApp"};

    $wnd.gotSaveComplete = $entry(@mitll.langtest.client.recorder.FlashRecordPanel::saveComplete());
    $wnd.swfCallback = $entry(@mitll.langtest.client.recorder.FlashRecordPanel::swfCallback());

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
    saveFeedback.setVisible(true);
  }

  public static void swfCallback() {
    System.out.println("embedSWF is complete!");
  }
}
