package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;

/**
 * Created by go22670 on 5/19/17.
 */
public class DownloadContainer {
  public static final String DOWNLOAD_AUDIO = "downloadAudio";

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
    //return downloadContainer;
  }
  /**
   * @return
   */
  private IconAnchor getDownloadIcon() {
    IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link");
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.TWO_TIMES);
    addTooltip(download, DOWNLOAD_YOUR_RECORDING);
 /*   download.addClickHandler(event -> controller.logEvent(download, "DownloadUserAudio_Icon", exid,
        "downloading audio file "));*/
    return download;
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  public void setDownloadHref(String audioPathToUse,
                       int id,
                       int user) {
    downloadContainer.setVisible(true);
    String href = DOWNLOAD_AUDIO +
        "?file=" +
        audioPathToUse +
        "&" +
        "exerciseID=" +
        id +
        "&" +
        "userID=" +
        user;
    download.setHref(href);
  }

  public Panel getDownloadContainer() {
    return downloadContainer;
  }
}
