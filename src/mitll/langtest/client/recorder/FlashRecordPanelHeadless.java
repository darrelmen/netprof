/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
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
  private static final int WIDTH = 250;
  private static final int HEIGHT = 170;
  private String id = "flashcontent";
  private static MicPermission micPermission;
  private boolean didPopup = false;
  private static boolean permissionReceived;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
	public FlashRecordPanelHeadless(){
    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id); // indicates the place for flash player to install in the page

    InlineHTML inner = new InlineHTML();
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    flashContent.add(inner);
    add(flashContent);
    hide();
  }

  public void show() {
    setSize(WIDTH + "px", HEIGHT + "px");
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer()
   */
  public void hide() {
    setSize("8px", "8px");
  }

  /**
   * Show this widget (make it big enough to accommodate the permission dialog) and install the flash player.
   * @see mitll.langtest.client.LangTest#checkInitFlash()
   * @see mitll.langtest.client.LangTest#showPopupOnDenial()
   */
  public void initFlash() {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        if (!didPopup) {
          show();
          //System.out.println("initFlash : doing installFlash");
          installFlash();
          System.out.println("initFlash : did   installFlash");
          didPopup = true;
        }
      }
    });
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

  public native void hide2() /*-{
    $wnd.Recorder.hide2();
  }-*/;

  /**
   * Base64 encoded byte array from action script.
   * @return
   */
  public native String getWav() /*-{
    return $wnd.Recorder.getWav();
  }-*/;

  public void installFlash() {
    if (gotPermission()) {
      micPermission.gotPermission();
    }
    else {
      installFlash(GWT.getModuleBaseURL(), id);
    }
  }

  public void removeFlash() {
    removeFlash(id);
  }

  /**
   * Uses SWFObject to embed flash -- <a href='http://code.google.com/p/swfobject/'>SWFObject</a>
   * @see #initFlash
   * @param moduleBaseURL where to get the swf file the player will run
   * @param id marks the div that the flash player will live inside
   */
	private native void installFlash(String moduleBaseURL, String id) /*-{
		var appWidth = 240;
		var appHeight = 160;
		
		var flashvars = {'event_handler': 'microphone_recorder_events'};
		var params = {};
		var attributes = {'id': "recorderApp", 'name': "recorderApp"};

    $wnd.micConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micConnected());
    $wnd.micNotConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micNotConnected());

    function outputStatus(e) {
      //alert("e.success = " + e.success +"\ne.id = "+ e.id +"\ne.ref = "+ e.ref);
    }
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "test.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, outputStatus);
  }-*/;

  private native void removeFlash(String id) /*-{
    $wnd.swfobject.removeSWF("recorderApp");
  }-*/;

  /**
   * Event from flash when user clicks Accept
   */
  public static void micConnected() {
    System.out.println("micConnected!");
    permissionReceived = true;
    micPermission.gotPermission();
  }

  /**
   * Event from flash when user clicks Deny
   */
  public static void micNotConnected() {
    System.err.println("mic  NOT   Connected!");
    permissionReceived = false;
    micPermission.gotDenial();
  }

  public boolean gotPermission() { return permissionReceived; }
}
