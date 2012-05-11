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
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * @author gregbramble
 *
 */
public class FlashRecordPanel extends FlowPanel {
	//these are static so that the JSNI createGoodWave is accessible to the recorder.js file
//	private static RecordInstance currentRecordInstance;

  /**
   * @see mitll.langtest.client.RecordExercisePanel#getAnswerWidget(com.google.gwt.user.client.ui.Button, int)
   */
	public FlashRecordPanel(String id){
    InlineHTML save_button = new InlineHTML();
    //  flashContent.setSize("240px","160px");
    save_button.getElement().setId("save_button");//"flashcontent");
    add(save_button);

    InlineHTML flashContent = new InlineHTML();
  //  flashContent.setSize("240px","160px");
		flashContent.getElement().setId(id);//"flashcontent");
		flashContent.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");

		SimplePanel statusPanel = new SimplePanel();
		statusPanel.getElement().setId("status");
		add(statusPanel);

    // record
    Image image = new Image("images/record.png");
    image.setAltText("Record");
   // image.setWidth("24px");
   // image.setHeight("24px");
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

    // save -- TODO change icon
    // TODO listen for save event to be completed
    add(flashContent);

    //playback

    Image image2 = new Image("images/play.png");
    image2.setAltText("Play");
   // image2.setWidth("24px");
   // image2.setHeight("24px");
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

    SimplePanel activity = new SimplePanel();
    activity.getElement().setId("activity_level");
    add(activity);
		

    add(new UploadForm());

  //  setSize("400px","400px");
  }

  private static class ImageAnchor extends Anchor {
    public ImageAnchor() {}
    public void setResource(Image img) {
     // Image img = new Image(imageResource);
     // img.setStyleName("navbarimg");
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }

  public native void recordOnClick() /*-{
    $wnd.Recorder.record('audio', 'audio.wav');
  }-*/;

  public native void playbackOnClick() /*-{
    $wnd.Recorder.playBack('audio');
  }-*/;
	
/*	public static void init(RecordInstance currentRecordInstance, RepeatExercise currentExercise){
		FlashRecordPanel.currentRecordInstance = currentRecordInstance;
		
		setRecordingInfo(currentExercise.getName());			//this initializes the recorder
	}

	public static void createGoodWave(){
		currentRecordInstance.createGoodWave();
	}

	public static void showWaitStatus(){
		currentRecordInstance.showWaitStatus();
	}
	
	public static void setPausePlayButtonEnabled(boolean enable){
		currentRecordInstance.setPausePlayButtonEnabled(enable);
	}*/
	
	public native void initializeJS(String moduleBaseURL, String id) /*-{
		var appWidth = 24;
		var appHeight = 24;
		
		var flashvars = {'event_handler': 'microphone_recorder_events', 'record_image': (moduleBaseURL + 'images/record.png'), 'stop_image': (moduleBaseURL + 'images/stop.png')};
		var params = {};
		var attributes = {'id': "recorderApp", 'name': "recorderApp"};
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "recorder.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes);
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
}
