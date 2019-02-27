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
  private static final String OGG = ".ogg";
  private static final String MP_3 = ".mp3";
  private static final String WAV = ".wav";
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
    audioPath = audioPath.endsWith(OGG) ?
        audioPath.replaceAll(OGG, MP_3) :
        audioPath.endsWith(WAV) ?
            audioPath.replaceAll(WAV, MP_3) :
            audioPath;

    setHRef(download, audioPath, host, exerciseID, controller.getUserState().getUser());
  }

  /**
   * @param download
   * @param audioPath
   * @param host
   * @param exerciseID
   * @param user
   * @see mitll.langtest.server.DownloadServlet#returnAudioFile
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
  static String getDownloadAudio(String host) {
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
