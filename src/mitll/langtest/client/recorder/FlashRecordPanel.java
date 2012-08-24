/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.Exercise;

/**
 * Roughly mimics the Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 *
 * Communicates with saveFeedback interface when a save(upload) completes {@link mitll.langtest.client.RecordExercisePanel.AnswerPanel}
 *
 * @author Gordon Vidaver
 * @deprecated use FlashRecordPanelHeadless instead
 */
public class FlashRecordPanel extends FlowPanel {
  private boolean showStatus = false;
  private boolean showUploadStatus = false;
  private final UploadForm upload;
  private static SaveNotification saveFeedback;  // remember for later
  //private InlineHTML save_button;
  private ImageAnchor play_button;
  private LangTestDatabaseAsync service;

  /**
   * @see mitll.langtest.client.LangTest#setupRecordPopup()
   */
	public FlashRecordPanel(String id, LangTestDatabaseAsync service){
    this.service = service;
/*    save_button = new InlineHTML();
    save_button.getElement().setId("save_button");//"flashcontent");
    add(save_button);*/

    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id);//"flashcontent");
    flashContent.setSize(240+"px",160+"px");

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
    play_button = new ImageAnchor();
    play_button.setResource(image2);

    play_button.getElement().setId("play_button");
    play_button.setTitle("Play");
    play_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        playbackOnClick();
      }
    });
    add(play_button);
    play_button.setVisible(false);

    SimplePanel uploadStatusPanel = new SimplePanel();
		uploadStatusPanel.getElement().setId("upload_status");
		add(uploadStatusPanel);
    uploadStatusPanel.setVisible(showUploadStatus);

    upload = new UploadForm();
    add(upload);
  }

  /**
   * Doesn't seem to work
   */
  public void reset() {
    //GWT.log("reset widget state --- ");
    //save_button.setVisible(false);
    play_button.setVisible(false);
  }

  /**
   * Remember widget so we can show it later when the save completes.
   * @see mitll.langtest.client.exercise.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, SaveNotification)
   * @param w
   */
  public static void setSaveCompleteFeedbackWidget(SaveNotification w) {
    saveFeedback = w;
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, SaveNotification)
   * @param userID
   * @param e
   * @param qid
   */
  public void setUpload(long userID, Exercise e, int qid) { upload.setSlots(userID, e,qid); }

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

  public native void showPermission() /*-{
    $wnd.Recorder.showPermission();
  }-*/;

  public native JsArrayInteger getWav() /*-{
    $wnd.Recorder.getWav();
  }-*/;

/*  private void sendArray(JsArrayInteger array) {
 //   JsArrayInteger array = getArray();
    List<Integer> byteArrayToSend = new ArrayList<Integer>(array.length());

    for (int i = 0; i < array.length(); i++) {
      int i1 = array.get(i);
      byteArrayToSend.add(i1);
    }
    service.postArray(byteArrayToSend,new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        GWT.log("sendArray : got failure " + caught);
      }

      public void onSuccess(Void result) {
        GWT.log("sendArray : got success " + result);
      }
    });
  }*/

  /**
   * @see mitll.langtest.client.LangTest#showPopupAt(int, int)
   * @param moduleBaseURL
   * @param id
   */
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
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "test.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, outputStatus);
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
    saveFeedback.gotSave();
  }

  public static void swfCallback() {
    System.out.println("embedSWF is complete!");
  }
}
