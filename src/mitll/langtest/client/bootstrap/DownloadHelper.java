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

package mitll.langtest.client.bootstrap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.SelectionState;

import java.util.Collection;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/16/15.
 */
class DownloadHelper {
  /*private static final String DOWNLOAD_SPREADSHEET = "Download spreadsheet and audio for selected sections.";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";

  private final EventRegistration eventRegistration;
  private final String instance;
  private final FlexSectionExerciseList exerciseList;
  private Anchor downloadLink;
  private Anchor contextDownloadLink;
  private final boolean isTeacher;

  DownloadHelper(EventRegistration eventRegistration, String instance,
                        FlexSectionExerciseList exerciseList, boolean isTeacher) {
    this.eventRegistration = eventRegistration;
    this.instance = instance;
    this.exerciseList = exerciseList;
    this.isTeacher = isTeacher;
  }

  FlexTable getDownloadLinks() {
    FlexTable links = new FlexTable();
    links.setWidget(0, 0,downloadLink = getDownloadLink());
    if (isTeacher) {
      links.setWidget(0, 1,contextDownloadLink = getContextDownloadLink());
    }
    return links;
  }

  void updateDownloadLinks(SelectionState selectionState) {
    if (downloadLink != null)        downloadLink.setHTML(getURLForDownload(selectionState));
    if (contextDownloadLink != null) contextDownloadLink.setHTML(getURLForContextDownload(selectionState));
  }

  *//**
   * @return
   * @see #addButtonRow
   *//*
  private Anchor getDownloadLink() {
    final Anchor downloadLink = new Anchor(getDownloadURL());
    addTooltip(downloadLink);
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        eventRegistration.logEvent(downloadLink, "DownloadLink", "N/A", "downloading audio");
      }
    });
    downloadLink.getElement().setId("DownloadLink_" + instance);
    return downloadLink;
  }

  *//**
   * @param widget
   * @return
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @see #getDownloadLink
   *//*
  private void addTooltip(Widget widget) {
    new TooltipHelper().addTooltip(widget, DOWNLOAD_SPREADSHEET);
  }

  private Anchor getContextDownloadLink() {
    final Anchor downloadLink = new Anchor(getDownloadContextURL());
    new TooltipHelper().addTooltip(downloadLink, "Download spreadsheet and context audio for selected sections.");
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        eventRegistration.logEvent(downloadLink, "ContextDownloadLink", "N/A", "downloading context audio");
      }
    });
    downloadLink.getElement().setId("ContextDownloadLink_" + instance);
    return downloadLink;
  }

*//*
  public void updateDownloadLinks(SelectionState selectionState) {
    if (downloadLink != null)        downloadLink.setHTML(downloadHelper.getURLForDownload(selectionState));
    if (contextDownloadLink != null) contextDownloadLink.setHTML(downloadHelper.getURLForContextDownload(selectionState));
  }
*//*

  *//**
   * @see #getDownloadLink()
   * @return
   *//*
  private SafeHtml getDownloadURL() {
    return getURLForDownload(exerciseList.getSelectionState());
  }

  private SafeHtml getDownloadContextURL() {
    return getURLForContextDownload(exerciseList.getSelectionState());
  }

  *//**
   * @param selectionState
   * @return
   * @see #showSelectionState(mitll.langtest.client.list.SelectionState)
   *//*
  private SafeHtml getURLForDownload(SelectionState selectionState) {
    return getUrlDownloadLink(selectionState, DOWNLOAD_AUDIO, "download", "Download");
  }

  private SafeHtml getURLForContextDownload(SelectionState selectionState) {
    return getUrlDownloadLink(selectionState, DOWNLOAD_AUDIO, "context", "Context");
  }

  private SafeHtml getUrlDownloadLink(SelectionState selectionState, String command, String request, String title) {
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a class='" + "icon-download" +
        "' href='" +
        command +
        "?request=" + request + "&" + typeToSection +
        "'" +
        ">");
    sb.appendEscaped(" " + title);
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }*/
}
