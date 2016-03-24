package mitll.langtest.client.sound;

import mitll.langtest.client.AudioTag;

/**
 * Created by go22670 on 3/23/16.
 */
public class CompressedAudio {
  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;


  public static native boolean supportsOgg() /*-{
      if (typeof $wnd.Modernizr == "undefined") {
          console.log("no modernizr?");
          return false;
      }
      if ($wnd.Modernizr.audio.ogg) {
          return true;
      } else {
          return false;
          // not-supported
      }
  }-*/;


  public String getPath(String path) {
    return ensureForwardSlashes(getPathNoSlashChange(path));
  }

  public String getPathNoSlashChange(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, supportsOgg() ? ".ogg" : MP3) : path;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
