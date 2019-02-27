/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.sound;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import mitll.langtest.client.instrumentation.EventRegistration;

import java.util.logging.Logger;

import static mitll.langtest.server.audio.AudioConversion.FILE_MISSING;

public class PlayAudioWidget {
  private final Logger logger = Logger.getLogger("PlayAudioWidget");

  /**
   * @see mitll.langtest.client.analysis.PhoneExampleContainer#addItem(Object)
   */
  public static native void addPlayer() /*-{
      $wnd.basicMP3Player.init();
  }-*/;

  /**
   * Log with the exercise it's associated with.
   * @param path
   * @param title
   * @param exerciseID - just
   * @param eventRegistration - base class of ExerciseController
   * @return
   */
  public Anchor getAudioWidgetWithEventRecording(String path, String title, int exerciseID,
                                                 EventRegistration eventRegistration) {
    Anchor anchor = getAudioWidget(path, title);
    eventRegistration.registerWidget(anchor, anchor, ""+exerciseID, "playing user audio " + path);
    if (path.contains(FILE_MISSING)) {
      logger.warning("getAudioWidgetWithEventRecording path is " + path + " title " + title + " ex " + exerciseID);
      anchor.setVisible(false);
    }
    return anchor;
  }

  /**
   * Simple play audio widget like in sm2 -
   * @param path to the audio file on the server - relative to webapp root
   * @param title to label the widget with
   * @return
   */
  private Anchor getAudioWidget(String path, String title) {
    SafeHtml html = getAudioTagHTML(path, title);
    Anchor anchor = new Anchor(html);
    anchor.getElement().setId("PlayAudioWidget_"+title);
    return anchor;
  }

  /**
   * OK to have this be mp3 for now, could be ogg?
   * @param path
   * @param title
   * @return
   */
  public static SafeHtml getAudioTagHTML(String path, String title) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href=\"" +
        ensureForwardSlashes(path).replace(".wav", ".mp3") +
        "\"" + " title=\"" +  title + "\"" +
        " class=\"sm2_button\">" +
        title +
        "</a>");

    return sb.toSafeHtml();
  }

  private static String ensureForwardSlashes(String wavPath) {  return wavPath.replaceAll("\\\\", "/"); }
}
