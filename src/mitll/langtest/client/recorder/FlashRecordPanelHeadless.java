/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.WavCallback;

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
  private static final String PX = "8px";
  private final String id = "flashcontent";
  public static MicPermission micPermission;
  private boolean didPopup = false;
  private static boolean permissionReceived;
  // private static boolean webAudioMicAvailable;
  // private static boolean usingWebAudio;
  private static boolean micConnected = true;
  //private static FlashRecordPanelHeadless selfRef;
  private static WebAudioRecorder webAudio;

    /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
	public FlashRecordPanelHeadless(){
    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id); // indicates the place for flash player to install in the page

    InlineHTML inner = new InlineHTML();
  //  inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed or support WebAudio.</p>");
    flashContent.add(inner);
    add(flashContent);
    hide();

    webAudio = new WebAudioRecorder();
    webAudio.advertise();

    //selfRef = this;
  }

  /**
   * @see #installFlash()
   * @return
   */
  public native boolean checkIfFlashInstalled() /*-{
      var hasFlash = false;
      try {
          var fo = new ActiveXObject('ShockwaveFlash.ShockwaveFlash');
          if(fo) hasFlash = true;
      } catch(e){
          if(navigator.mimeTypes ["application/x-shockwave-flash"] != undefined) hasFlash = true;
      }
      return hasFlash;
  }-*/;

  /**
   * @see #rememberInstallFlash()
   */
  void show() {
    setSize(WIDTH + "px", HEIGHT + "px");
  }

  /**
   * Show this widget (make it big enough to accommodate the permission dialog) and install the flash player.
   * @see mitll.langtest.client.LangTest#checkInitFlash()
   */
/*  public void initRecorder() {
    initWebaudio();
  }*/

  /**
   * @see mitll.langtest.client.LangTest#checkInitFlash()
   * @see mitll.langtest.client.LangTest#showPopupOnDenial()
   */
  public void initFlash() {
    rememberInstallFlash();
  }

  /**
   * @see #initFlash()
   * @seex #webAudioMicNotAvailable
   */
  private void rememberInstallFlash() {
    System.out.println("rememberInstallFlash");

    if (!didPopup) {
      show();
      installFlash();
      System.out.println("rememberInstallFlash : did   installFlash");
      didPopup = true;
    }
    else {
      System.out.println("rememberInstallFlash didPopup " + didPopup);

    }
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   * @param micPermission
   */
  public static void setMicPermission(MicPermission micPermission) {
    FlashRecordPanelHeadless.micPermission = micPermission;
    //webAudio.setMicPermission(micPermission);
  }

  public native void initWebaudio() /*-{
      $wnd.initWebAudio();
  }-*/;

  public void recordOnClick() {
/*
    if (webAudio.isWebAudioMicAvailable()) {
      webAudio.startRecording();
    }
    else {
      flashRecordOnClick();
    }
*/

    if (permissionReceived) {
      if (!isMicAvailable()) {
        System.err.println("\n\n\nrecordOnClick huh? mic is not available");
      }
      else {
        System.out.println("\n\n\nrecordOnClick mic IS  available");

      }
      flashRecordOnClick();
    } else if (webAudio.isWebAudioMicAvailable()) {
      webAudio.startRecording();
    }
  }

  public native void flashRecordOnClick() /*-{
    $wnd.FlashRecorderLocal.record('audio', 'audio.wav');
  }-*/;

  private void stopRecording() {
/*
    if (webAudio.isWebAudioMicAvailable()) {
      webAudio.stopRecording();
    } else {
      flashStopRecording();
    }
*/

    if (permissionReceived) {
/*      if (!isMicAvailable()) {
        System.err.println("\n\n\nstopRecording huh? mic is not available");
      }*/
      flashStopRecording();
    } else if (webAudio.isWebAudioMicAvailable()) {
      webAudio.stopRecording();
    }
  }

  public native void flashStopRecording() /*-{
      $wnd.FlashRecorderLocal.stop();
  }-*/;

  public native boolean isMicAvailable() /*-{
      return $wnd.FlashRecorderLocal.isMicrophoneAvailable();
  }-*/;

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer()
   */
  public void hide() {
    System.out.println("hide...");
    setSize(PX, PX);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   */
  public void hide2() {
    if (permissionReceived) {
      System.out.println("hide2...");

      flashHide2();
    }
  }

  public native void flashHide2() /*-{
    $wnd.FlashRecorderLocal.hide2();
  }-*/;

  /**
   * @see mitll.langtest.client.WavCallback#getBase64EncodedWavFile
   * @see #stopRecording(mitll.langtest.client.WavCallback)
   * @return
   */
  private String getWav() {
    if (permissionReceived) {
      System.out.println("getWav...");

      return flashGetWav();
    } else {
      return "";
    }
  }

  /**
   * Base64 encoded byte array from action script.
   *
   * @return
   */
  public native String flashGetWav() /*-{
      return $wnd.FlashRecorderLocal.getWav();
  }-*/;

  /**
   * @see mitll.langtest.client.recorder.FlashRecordPanelHeadless#rememberInstallFlash()
   */
  private void installFlash() {
    if (gotPermission()) {
      System.out.println("installFlash :  got permission!");

      micPermission.gotPermission();
    } else {
      System.out.println("didn't get Flash Player permission!");
      if (checkIfFlashInstalled()) {
        installFlash(GWT.getModuleBaseURL(), id);
      }
      else {
        webAudio.tryWebAudio(); // kick web audio!
        //Window.alert("Flash player must be installed to record audio.");
      }
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#removeAndReloadFlash()
   */
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
    $wnd.noMicrophoneFound = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::noMicrophoneFound());

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
    System.err.println("---> mic *NOT* Connected! <--- ");
    permissionReceived = false;
    //if (!webAudio.isWebAudioMicAvailable()) {

    //}
    webAudio.tryWebAudio();
   // micPermission.gotDenial();
  }

  public static void noMicrophoneFound() {
    System.err.println("no mic available");
    permissionReceived = false;
    micConnected = false;
    micPermission.noMicAvailable();
  }

  public boolean gotPermission()  { return permissionReceived || webAudio.isWebAudioMicAvailable(); }
  public boolean isMicConnected() { return micConnected; }

/*  public static void webAudioMicAvailable() {
    //webAudioMicAvailable = true;

    System.out.println("webAudioMicAvailable -- connected!");
    permissionReceived = true;
    micPermission.gotPermission();
  }*/

/*
  public static void webAudioMicNotAvailable() {
    System.out.println("webAudioMicNotAvailable!");
    webAudioMicAvailable = false;
  //  selfRef.rememberInstallFlash();
  }
*/

/*  public static void getBase64(String encoded) {
    System.out.println("getBase64 " + encoded.length());
    if (FlashRecordPanelHeadless.wavCallback == null) {
      System.err.println("getBase64 no callback?");
    } else {
      FlashRecordPanelHeadless.wavCallback.getBase64EncodedWavFile(encoded);
      FlashRecordPanelHeadless.wavCallback = null;
    }
  }*/

  /**
   * @see mitll.langtest.client.LangTest#stopRecording(mitll.langtest.client.WavCallback)
   */
  //static WavCallback wavCallback = null;
  public void stopRecording(WavCallback wavCallback) {
    //FlashRecordPanelHeadless.wavCallback = wavCallback;
   // stopRecording();
    if (permissionReceived) {
      flashStopRecording();
      wavCallback.getBase64EncodedWavFile(getWav());
    }
    else if (webAudio.isWebAudioMicAvailable()) {
     // WebAudioRecorder.wavCallback = wavCallback;
      webAudio.stopRecording(wavCallback);
    }
  }
}
