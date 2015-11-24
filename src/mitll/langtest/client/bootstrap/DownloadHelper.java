/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
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
 * Created by go22670 on 11/16/15.
 */
public class DownloadHelper {
  private static final String DOWNLOAD_SPREADSHEET = "Download spreadsheet and audio for selected sections.";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";

  private final EventRegistration eventRegistration;
  private final String instance;
  private final FlexSectionExerciseList exerciseList;
  private Anchor downloadLink;
  private Anchor contextDownloadLink;
  private final boolean isTeacher;

  public DownloadHelper(EventRegistration eventRegistration, String instance,
                        FlexSectionExerciseList exerciseList, boolean isTeacher) {
    this.eventRegistration = eventRegistration;
    this.instance = instance;
    this.exerciseList = exerciseList;
    this.isTeacher = isTeacher;
  }

  public FlexTable getDownloadLinks() {
    FlexTable links = new FlexTable();
    links.setWidget(0, 0,downloadLink = getDownloadLink());
    if (isTeacher) {
      links.setWidget(0, 1,contextDownloadLink = getContextDownloadLink());
    }
    return links;
  }
  public void updateDownloadLinks(SelectionState selectionState) {
    if (downloadLink != null)        downloadLink.setHTML(getURLForDownload(selectionState));
    if (contextDownloadLink != null) contextDownloadLink.setHTML(getURLForContextDownload(selectionState));
  }


  /**
   * @return
   * @see #addButtonRow
   */
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

  /**
   * @param widget
   * @return
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @see #getDownloadLink
   */
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

/*
  public void updateDownloadLinks(SelectionState selectionState) {
    if (downloadLink != null)        downloadLink.setHTML(downloadHelper.getURLForDownload(selectionState));
    if (contextDownloadLink != null) contextDownloadLink.setHTML(downloadHelper.getURLForContextDownload(selectionState));
  }
*/


  private SafeHtml getDownloadURL() {
    SelectionState selectionState = exerciseList.getSelectionState();
    return getURLForDownload(selectionState);
  }

  private SafeHtml getDownloadContextURL() {
    SelectionState selectionState = exerciseList.getSelectionState();
    return getURLForContextDownload(selectionState);
  }

  /**
   * @param selectionState
   * @return
   * @see #showSelectionState(mitll.langtest.client.list.SelectionState)
   */
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
  }
}
