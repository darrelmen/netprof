package mitll.langtest.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Use an mp3 audio reference and either WAV or WEBM.
 *
 * User: go22670
 * Date: 8/22/12
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioTag {
  private static final boolean INCLUDE_ALTERNATE_COMPRESSED = LangTestDatabase.WRITE_ALTERNATE_COMPRESSED_AUDIO;
  private static final String ALTERNATE_TYPE = "webm";
  private static final boolean INCLUDE_ALTERNATE_AUDIO = true;

  /**
   * @see mitll.langtest.client.result.ResultManager#getTable
   * @param result
   * @return
   */
  public SafeHtml getAudioTag(String result) {
    result = ensureForwardSlashes(result);
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    String firstSource = INCLUDE_ALTERNATE_AUDIO ?
        "<source type=\"audio/" + (INCLUDE_ALTERNATE_COMPRESSED ? ALTERNATE_TYPE : "wav") + "\" " +
           "src=\"" + (INCLUDE_ALTERNATE_COMPRESSED ? result.replace(".wav", "." +ALTERNATE_TYPE) : result) + "\">" +
        "</source>\n" : "";
    String secondSource = "<source type=\"audio/mp3\" src=\"" + result.replace(".wav", ".mp3") + "\"></source>\n";
    sb.appendHtmlConstant("<audio preload=\"auto\" controls=\"controls\" tabindex=\"0\">\n" +
        firstSource +
        secondSource +
        // "<source type=\"audio/ogg\" src=\"media/ac-LC1-009/ac-LC1-009-C.ogg\"></source>\n" +
        "Your browser does not support the audio tag.\n" +
        "</audio>");
    return sb.toSafeHtml();
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
