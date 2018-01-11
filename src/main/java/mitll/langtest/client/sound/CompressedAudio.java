/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.sound;

import mitll.langtest.client.initial.BrowserCheck;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/23/16.
 */
public class CompressedAudio {
 // private final Logger logger = Logger.getLogger("CompressedAudio");
  private static final String OGG = ".ogg";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private static final boolean isIE = BrowserCheck.getIEVersion() > 0;
  public static String getPath(String path) {
    return ensureForwardSlashes(getPathNoSlashChange(path));
  }

  public static String getPathNoSlashChange(String path) {
    boolean b1 = supportsOgg();
    boolean useOGG = !isIE && b1;
 //   logger.info("using ogg " + useOGG + " is IE  " + isIE   + " supports OGG " + b1 + " checking " + path);
    return (path.endsWith(WAV)) ? path.replace(WAV, useOGG ? OGG : MP3) : (!useOGG && path.endsWith(OGG) ? path.replace(OGG,MP3) : path);
  }

  /**
   * @see #getPathNoSlashChange
   * @return
   */
  public static native boolean supportsOgg() /*-{
      if (typeof $wnd.Modernizr == "undefined") {
          console.log("no modernizr?");
          return false;
      }
      if ($wnd.Modernizr.audio.ogg) {
          //console.log("Can play ogg - ");
          return true;
      } else {
          //console.log("Can NOT play ogg - ");
          return false;
          // not-supported
      }
  }-*/;

  private static String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
