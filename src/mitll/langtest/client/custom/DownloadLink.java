/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.HasID;

/**
 * Created by go22670 on 10/20/15.
 * @see ListManager#makeTabContent(UserList, String, HasID)
 */
class DownloadLink {
  public static final String DOWNLOAD_AUDIO = "downloadAudio";
  public static final String DOWNLOAD_SPREADSHEET_AND_AUDIO = "Download spreadsheet and audio for list.";

  private final ExerciseController controller;
  DownloadLink(ExerciseController controller) {
    this.controller = controller;
  }
  /**
   * @param listid
   * @param linkid
   * @param name
   * @return
   * @see #makeTabContent
   */
  public Anchor getDownloadLink(long listid, String linkid, final String name) {
    final Anchor downloadLink = new Anchor(getURLForDownload(listid));
    new TooltipHelper().addTooltip(downloadLink, DOWNLOAD_SPREADSHEET_AND_AUDIO);
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadLink, "DownloadLink", "N/A", "downloading audio for " + name);
      }
    });
    downloadLink.getElement().setId("DownloadLink_" + linkid);
    downloadLink.addStyleName("leftFiveMargin");
    return downloadLink;
  }

  /**
   * @return
   * @seex #showSelectionState(mitll.langtest.client.list.SelectionState)
   * @paramx xselectionState
   */
  private SafeHtml getURLForDownload(long listid) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a class='" + "icon-download" +
        "' href='" +
        DOWNLOAD_AUDIO +
        "?list=" + listid +
        "'" +
        ">");
    sb.appendEscaped(" Download");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }
}
