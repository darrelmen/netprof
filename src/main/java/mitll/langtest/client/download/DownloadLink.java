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

package mitll.langtest.client.download;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.client.exercise.ExerciseController;

import static mitll.langtest.client.download.DownloadContainer.getDownloadAudio;

public class DownloadLink {
  private static final String DOWNLOAD_SPREADSHEET_AND_AUDIO = "Download spreadsheet and audio for list.";

  private final ExerciseController controller;

  /**
   * @see mitll.langtest.client.custom.userlist.ListOperations#getDownloadLink
   * @param controller
   */
  public DownloadLink(ExerciseController controller) {
    this.controller = controller;
  }
  /**
   * @param listid
   * @param linkid
   * @param name
   * @return
   * @see ListManager#makeTabContent
   */
  public Anchor getDownloadLink(long listid, String linkid, final String name) {
    final Anchor downloadLink = new Anchor(getURLForDownload(listid, controller.getHost()));
    new TooltipHelper().addTooltip(downloadLink, DOWNLOAD_SPREADSHEET_AND_AUDIO);
    downloadLink.addClickHandler(event -> controller.logEvent(downloadLink, "DownloadLink", "N/A", "downloading audio for " + name));
    downloadLink.getElement().setId("DownloadLink_" + linkid);
    downloadLink.addStyleName("leftFiveMargin");
    return downloadLink;
  }

  /**
   * @return
   */
  private SafeHtml getURLForDownload(long listid, String host) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a class='" + "icon-download" + "' " +
        "href='" +
        getDownloadAudio(host) +
        "?list=" + listid +
        "'" +
        ">");
    sb.appendEscaped(" Download");
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }
}
