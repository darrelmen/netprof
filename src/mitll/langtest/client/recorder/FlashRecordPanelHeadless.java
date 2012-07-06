/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Somewhat related to Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 *
 * Remember when recompiling the flash to do:
 *
 * mxmlc Recorder.as -static-link-runtime-shared-libraries=true -output test.swf;
 *
 * Download flash from <a href=''>Flash Download</a>
 * @author Gordon Vidaver *
 */
public class FlashRecordPanelHeadless extends AbsolutePanel {
  private String id = "flashcontent";
  private static MicPermission micPermission;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
	public FlashRecordPanelHeadless(){
    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id);

    InlineHTML inner = new InlineHTML();
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    flashContent.add(inner);
    add(flashContent);
    setSize(250 + "px", 170 + "px");
  }

  public static void setMicPermission(MicPermission micPermission) {
    FlashRecordPanelHeadless.micPermission = micPermission;
  }

  public native void recordOnClick() /*-{
    $wnd.Recorder.record('audio', 'audio.wav');
  }-*/;

  public native void stopRecording() /*-{
    $wnd.Recorder.stop();
  }-*/;

/*  public native void playbackOnClick() *//*-{
    $wnd.Recorder.playBack('audio');
  }-*//*;*/

  /**
   * Base64 encoded byte array from action script.
   * @return
   */
  public native String getWav() /*-{
    return $wnd.Recorder.getWav();
  }-*/;

	public native void initializeJS(String moduleBaseURL, String id) /*-{
		var appWidth = 240;
		var appHeight = 160;
		
		var flashvars = {'event_handler': 'microphone_recorder_events', 'record_image': (moduleBaseURL + 'images/record.png'),'upload_image': (moduleBaseURL + 'images/upload.png'), 'stop_image': (moduleBaseURL + 'images/stop.png')};
		var params = {};
		var attributes = {'id': "recorderApp", 'name': "recorderApp"};

    $wnd.micConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micConnected());
    $wnd.micNotConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micNotConnected());
   // $wnd.playbackStopped = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::playbackStopped());
    $wnd.swfCallback = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::swfCallback());

    function outputStatus(e) {
      //alert("e.success = " + e.success +"\ne.id = "+ e.id +"\ne.ref = "+ e.ref);
      //swfCallback();  // TODO somehow this sometimes shows up as undefined...
    }
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "test.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, outputStatus);
  }-*/;


  public static void micConnected() {
    System.out.println("micConnected!");
    micPermission.gotPermission();
  }
  public static void micNotConnected() {
    System.out.println("mic  NOT   Connected!");
    micPermission.gotDenial();
  }

/*  public static void playbackStopped() {
    System.out.println("mic  NOT   Connected!");
    micPermission.gotDenial();
  }*/

  public static void swfCallback() {
    System.out.println("embedSWF is complete!");
  }
}
