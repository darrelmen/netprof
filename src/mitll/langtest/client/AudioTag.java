/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Use an mp3 audio reference and either WAV or WEBM.
 * <p/>
 * User: go22670
 * Date: 8/22/12
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioTag {
  private static final boolean INCLUDE_ALTERNATE_COMPRESSED = LangTestDatabase.WRITE_ALTERNATE_COMPRESSED_AUDIO;
  private static final String ALTERNATE_TYPE = "ogg";
  private static final boolean INCLUDE_ALTERNATE_AUDIO = true;
  private static final String PRELOAD_HINT = "auto";
  public static final String COMPRESSED_TYPE = "mp3";//"ogg";//"mp3";

  public SafeHtml getAudioTag(String result) {
    return getAudioTag(result, true);
  }

  /**
   * @param result
   * @return
   * @see mitll.langtest.client.result.ResultManager#addColumnsToTable
   */
  public SafeHtml getAudioTag(String result, boolean includeControls) {
    result = ensureForwardSlashes(result);
    String firstSource = INCLUDE_ALTERNATE_AUDIO ?
        "<source type=\"audio/" + (INCLUDE_ALTERNATE_COMPRESSED ? ALTERNATE_TYPE : "wav") + "\" " +
            "src=\"" + (INCLUDE_ALTERNATE_COMPRESSED ? result.replace(".wav", "." + ALTERNATE_TYPE) : result) + "\">" +
            "</source>\n" : "";
    String secondSource = "<source type=\"audio/" +
        COMPRESSED_TYPE +
        "\" src=\"" + result.replace(".wav", "." +
        COMPRESSED_TYPE) + "\"></source>\n";

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<audio preload=\"" + PRELOAD_HINT + "\" " +
        (includeControls ? "controls=\"controls\" " : "") +
        "tabindex=\"0\">\n" +
        firstSource +
        secondSource +
        "Your browser does not support the audio tag.\n" +
        "</audio>");
    return sb.toSafeHtml();
  }

  public String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
