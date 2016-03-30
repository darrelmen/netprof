package mitll.langtest.client.sound;

import mitll.langtest.client.BrowserCheck;

import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/16.
 */
public class CompressedAudio {
 // private final Logger logger = Logger.getLogger("CompressedAudio");

  private static final String OGG = ".ogg";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private static boolean isIE = BrowserCheck.getIEVersion() > 0;
  public static native boolean supportsOgg() /*-{
      if (typeof $wnd.Modernizr == "undefined") {
          console.log("no modernizr?");
          return false;
      }
      if ($wnd.Modernizr.audio.ogg) {
          //console.log("Can play ogg - ");
          return true;
      } else {
          console.log("Can NOT play ogg - ");
          return false;
          // not-supported
      }
  }-*/;


  public static String getPath(String path) {
    return ensureForwardSlashes(getPathNoSlashChange(path));
  }

  public static String getPathNoSlashChange(String path) {
    boolean b1 = supportsOgg();
    boolean useOGG = !isIE && b1;
 //   logger.info("using ogg " + useOGG + " is IE  " + isIE   + " supports OGG " + b1 + " checking " + path);
    return (path.endsWith(WAV)) ? path.replace(WAV, useOGG ? OGG : MP3) : (!useOGG && path.endsWith(OGG) ? path.replace(OGG,MP3) : path);
  }

  private static String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
