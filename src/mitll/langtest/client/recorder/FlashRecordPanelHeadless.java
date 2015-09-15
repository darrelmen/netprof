/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.WavCallback;

import java.util.Date;
import java.util.logging.Logger;

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
  private Logger logger = Logger.getLogger("FlashRecordPanelHeadless");

  private static final int WIDTH = 250;
  private static final int HEIGHT = 170;
  private static final String PX = "8px";
  private static final int FLASH_RECORDING_STOP_DELAY = 210; // was 160
  private final String id = "flashcontent";
  public static MicPermission micPermission;
  private boolean didPopup = false;
  private static boolean permissionReceived;

  private static WebAudioRecorder webAudio;
  private static FlashRecordPanelHeadless selfPointer;

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer()
   */
  public FlashRecordPanelHeadless() {
    SimplePanel flashContent = new SimplePanel();
    flashContent.getElement().setId(id); // indicates the place for flash player to install in the page
    add(flashContent);
    hide();

    webAudio = new WebAudioRecorder();
    webAudio.advertise();
    selfPointer = this;
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
    if (BrowserCheck.getIEVersion() != -1) {
      console("Found IE Version " + BrowserCheck.getIEVersion());
    }
  }

  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }

  private native static void consoleLog( String message) /*-{
      console.log( "FlashRecordPanelHeadless:" + message );
  }-*/;

  /**
   * Show this widget (make it big enough to accommodate the permission dialog) and install the flash player.
   * @see mitll.langtest.client.LangTest#checkInitFlash()
   * @see mitll.langtest.client.LangTest#showPopupOnDenial()
   */
  public boolean initFlash() {
    return rememberInstallFlash();
  }

  /**
   * @see #initFlash()
   * @seex #webAudioMicNotAvailable
   */
  private boolean rememberInstallFlash() {
    logger.info("rememberInstallFlash");

    if (!didPopup) {
      show();
      installFlash();
      logger.info("rememberInstallFlash : did   installFlash");
      didPopup = true;
      return false;
    }
    else {
      logger.info("rememberInstallFlash didPopup " + didPopup);

      return true;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   * @param micPermission
   */
  public static void setMicPermission(MicPermission micPermission) {
    FlashRecordPanelHeadless.micPermission = micPermission;
  }

  public void recordOnClick() {
    if (permissionReceived) {
      if (!isMicAvailable()) {
        System.err.println("recordOnClick mic is not available");
      }
      else {
        //logger.info("recordOnClick mic IS  available");
      }
      flashRecordOnClick();
    } else if (webAudio.isWebAudioMicAvailable()) {
      webAudio.startRecording();
    }
  }

  public native void flashRecordOnClick() /*-{
    $wnd.FlashRecorderLocal.record('audio', 'audio.wav');
  }-*/;

  public native void flashStopRecording() /*-{
      $wnd.FlashRecorderLocal.stop();
  }-*/;

  public native boolean isMicAvailable() /*-{
      return $wnd.FlashRecorderLocal.isMicrophoneAvailable();
  }-*/;

  /**
   * @see mitll.langtest.client.LangTest#hideFlash()
   * @see #FlashRecordPanelHeadless()
   */
  public void hide() {
    //logger.info("hide...");
    setSize(PX, PX);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   */
  public void hide2() {
    if (permissionReceived) {
      //logger.info("hide2...");
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
      //logger.info("getWav...");
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
      logger.info("installFlash :  got permission!");

      micPermission.gotPermission();
    } else {
      logger.info("installFlash : didn't get Flash Player permission!");
      if (checkIfFlashInstalled()) {
        logger.info("installFlash : looking for " + id);
        installFlash(GWT.getModuleBaseURL(), id);
      }
      else {
        console("installFlash : no flash, trying web audio");

        webAudio.tryWebAudio(); // kick web audio!
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
    $wnd.installFailure = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::installFailure());

      //This function is invoked by SWFObject once the <object> has been created
      var callback = function (e){

          //Only execute if SWFObject embed was successful
          if(!e.success || !e.ref){ $wnd.installFailure(); }
          else {  //deal with flash blocked
              if(typeof e.ref.PercentLoaded !== "undefined") {
                  if (e.ref.PercentLoaded() < 100) {
                      //$wnd.installFailure();
                  }
              }
              else {
 //                 $wnd.installFailure();

                  //var initialTimeout = setTimeout(function (){
                  //    //Ensure Flash Player's PercentLoaded method is available and returns a value
                  //    if(typeof e.ref.PercentLoaded !== "undefined" && e.ref.PercentLoaded()){
                  //        //Set up a timer to periodically check value of PercentLoaded
                  //        var loadCheckInterval = setInterval(function (){
                  //            //Once value == 100 (fully loaded) we can do whatever we want
                  //            if(e.ref.PercentLoaded() === 100){
                  //                //Execute function
                  //                fn();
                  //                //Clear timer
                  //                clearInterval(loadCheckInterval);
                  //            }
                  //        }, 1500);
                  //    }
                  //}, 200);
              }
          }
      };
		
		$wnd.swfobject.embedSWF(moduleBaseURL + "test.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, callback);
  }-*/;

  private native void removeFlash(String id) /*-{
      $wnd.swfobject.removeSWF("recorderApp");
  }-*/;


  public static native void installFailure() /*-{
      $wnd.micNotConnected();
  }-*/;

  /**
   * Event from flash when user clicks Accept
   */
  public static void micConnected() {
    consoleLog("micConnected!");
    permissionReceived = true;
    micPermission.gotPermission();
  }

  /**
   * Event from flash when user clicks Deny
   */
  public static void micNotConnected() {
    consoleLog("---> mic *NOT* Connected! <--- ");
    permissionReceived = false;

    selfPointer.hide();
    selfPointer.hide2(); // must be a separate call!

    webAudio.tryWebAudio();
  }

  public static void noMicrophoneFound() {
    consoleLog("no mic available");
    permissionReceived = false;

    micPermission.noMicAvailable();
  }

  public boolean gotPermission()  { return permissionReceived || usingWebRTC(); }

  public boolean usingFlash() { return permissionReceived; }
  public boolean usingWebRTC() { return webAudio.isWebAudioMicAvailable(); }

  /**
   * @see mitll.langtest.client.LangTest#stopRecording(mitll.langtest.client.WavCallback)
   */
  public void stopRecording(final WavCallback wavCallback) {
    if (permissionReceived) {
      final long then = System.currentTimeMillis();
      logger.info("stopRecording at " + then + " " + new Date(then));

      Timer t = new Timer() {
        @Override
        public void run() {

          long now = System.currentTimeMillis();
          logger.info("stopRecording timer at " + now + " diff " + (now - then) +
              " " + new Date(now));

          flashStopRecording();
          wavCallback.getBase64EncodedWavFile(getWav());
        }
      };
      t.schedule(FLASH_RECORDING_STOP_DELAY); // add flash delay
    }
    else if (webAudio.isWebAudioMicAvailable()) {
      webAudio.stopRecording(wavCallback);
    }
  }
}
