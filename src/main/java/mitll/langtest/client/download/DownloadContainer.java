package mitll.langtest.client.download;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 5/19/17.
 */
public class DownloadContainer {
  /**
   * @see DownloadHelper#getURL
   */
  static final String DOWNLOAD_AUDIO = "downloadAudio";

  /**
   * @see #getDownloadIcon
   */
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";
  private IconAnchor download;
  private Panel downloadContainer;


  public DownloadContainer() {
    addDownloadAudioWidget();
  }


  private void addDownloadAudioWidget() {
    DivWidget downloadContainer = new DivWidget();

    downloadContainer.getElement().setId("downloadContainer");

    downloadContainer.add(download = getDownloadIcon());
    downloadContainer.setVisible(false);

    downloadContainer.addStyleName("leftFiveMargin");
    downloadContainer.addStyleName("rightFiveMargin");
    downloadContainer.addStyleName("topFiveMargin");

    this.downloadContainer = downloadContainer;
  }

  /**
   * @return
   * @seex mitll.langtest.client.gauge.ASRHistoryPanel#getDownload
   */
  private IconAnchor getDownloadIcon() {
    IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");

    download.getElement().setPropertyString("download", "");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    addTooltip(download, DOWNLOAD_YOUR_RECORDING);
 /*   download.addClickHandler(event -> controller.logEvent(download, "DownloadUserAudio_Icon", exid,
        "downloading audio file "));*/
    return download;
  }

  /**
   * @param audioPath
   * @param host
   * @return link for this audio
   * @see DownloadContainer#getDownloadIcon()
   * @see #getAudioAndScore
   */
  public IconAnchor getDownload(final String audioPath, int linkIndex, String dateFormat, String host,
                                int exerciseID, ExerciseController controller) {
    final IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link_" + linkIndex);

    download.getElement().setPropertyString("download", "");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.LARGE);
    download.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

    addTooltipForDate(download, dateFormat);
    setDownloadHref(download, audioPath, host, exerciseID, controller);

    download.addClickHandler(event -> controller.logEvent(download, "DownloadUserAudio_History",
        exerciseID, "downloading audio file " + audioPath));

    return download;
  }

  /**
   * Why the user id?
   * Why the exercise id?
   * Who wrote this? Bueller?
   *
   * @param audioPathToUse
   * @param exerciseID
   * @param user
   * @param host
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#setDownloadHref
   */
  public void setDownloadHref(String audioPathToUse,
                              int exerciseID,
                              int user,
                              String host) {
    downloadContainer.setVisible(true);
    setHRef(download, audioPathToUse, host, exerciseID, user);
  }

  /**
   * @param download
   * @param audioPath
   * @param host
   * @see #getDownload
   */
  private void setDownloadHref(IconAnchor download, String audioPath, String host, int exerciseID, ExerciseController controller) {
    audioPath = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;
    setHRef(download, audioPath, host, exerciseID, controller.getUserState().getUser());
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
   * @param download
   * @param audioPath
   * @param host
   * @param exerciseID
   * @param user
   */
  private void setHRef(IconAnchor download, String audioPath, String host, int exerciseID, int user) {
    String href = getDownloadAudio(host) +
        "?" +
        "file=" + audioPath + "&" +
        "exerciseID=" + exerciseID + "&" +
        "userID=" + user;

    download.setHref(href);
  }

  @NotNull
  public static String getDownloadAudio(String host) {
    return DownloadContainer.DOWNLOAD_AUDIO + (host.isEmpty() ? "" : "/" + host);
  }

  public Panel getDownloadContainer() {
    return downloadContainer;
  }


  private void addTooltip(Widget w, String tip) {
    new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * @param w
   * @see #getDownload
   */
  private void addTooltipForDate(Widget w, String dateFormat) {
    String tip = "Download recording" +
        (dateFormat.isEmpty()
            ? "" : " from " + dateFormat);
    new TooltipHelper().createAddTooltip(w, tip, Placement.LEFT);
  }
}
