package mitll.langtest.client.sound;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.instrumentation.EventRegistration;

/**
 * Audio widgets like the little ones at <a href="http://www.schillmania.com/projects/soundmanager2/">Sound Manager 2</a>
 * Created by go22670 on 7/25/14.
 */
public class PlayAudioWidget {

  /**
   * Super simple audio widget
   * @param path
   * @return
   */
  public Anchor getAudioWidget(String path) {
    return getAudioWidget(path, "play");
  }

  /**
   * Add event tracking - every play is logged
   * @param path
   * @param title
   * @param eventRegistration
   * @return
   */
  public Anchor getAudioWidgetWithEventRecording(String path, String title, EventRegistration eventRegistration) {
    return getAudioWidgetWithEventRecording(path, title, "N/A", eventRegistration);
  }

  /**
   * Log with the exercise it's associated with.
   * @param path
   * @param title
   * @param exerciseID
   * @param eventRegistration - base class of ExerciseController
   * @return
   */
  public Anchor getAudioWidgetWithEventRecording(String path, String title, String exerciseID,
                                                 EventRegistration eventRegistration) {
    Anchor anchor = getAudioWidget(path, title);
    eventRegistration.registerWidget(anchor, anchor, exerciseID, "playing user audio " + path);
    return anchor;
  }

  /**
   * Simple play audio widget like in sm2 -
   * @param path to the audio file on the server - relative to webapp root
   * @param title to label the widget with
   * @return
   */
  public Anchor getAudioWidget(String path, String title) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a href=\"" +
        ensureForwardSlashes(path).replace(".wav", "." + AudioTag.COMPRESSED_TYPE) +
        "\"" +
        " title=\"" +
        title +
        "\" class=\"sm2_button\">" +
        title +
        "</a>");

    Anchor anchor = new Anchor(sb.toSafeHtml());
    anchor.getElement().setId("PlayAudioWidget_"+title);
    return anchor;
  }

  private String ensureForwardSlashes(String wavPath) {  return wavPath.replaceAll("\\\\", "/"); }

}
