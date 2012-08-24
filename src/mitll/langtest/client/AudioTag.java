package mitll.langtest.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Only use an mp3 audio reference -- seems to be the common denominator among browsers.
 *
 * User: go22670
 * Date: 8/22/12
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioTag {
  private static final boolean USE_OGG = LangTestDatabase.USE_OGG;
  private static final boolean USE_WAV = false;

  /**
   * @see ResultManager#getTable(java.util.Collection
   * @param result
   * @return
   */
  public SafeHtml getAudioTag(String result) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    String firstSource = USE_WAV ? "<source type=\"audio/" +
        (USE_OGG ? "ogg" : "wav") +
        "\" src=\"" + (USE_OGG ? result.replace(".wav", ".ogg") : result) + "\"></source>\n" : "";
    sb.appendHtmlConstant("<audio preload=\"none\" controls=\"controls\" tabindex=\"0\">\n" +
        firstSource +
        "<source type=\"audio/mp3\" src=\"" + result.replace(".wav", ".mp3") + "\"></source>\n" +
        // "<source type=\"audio/ogg\" src=\"media/ac-LC1-009/ac-LC1-009-C.ogg\"></source>\n" +
        "Your browser does not support the audio tag.\n" +
        "</audio>");
    return sb.toSafeHtml();
  }
}
