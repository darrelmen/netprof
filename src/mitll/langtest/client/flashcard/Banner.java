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

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.recorder.FlashRecordPanelHeadless;
import mitll.langtest.shared.User;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Does fancy font sizing depending on available width...
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class Banner implements RequiresResize {
  private final Logger logger = Logger.getLogger("Banner");

  private static final double MAX_FONT_EM = 1.7d;
  private static final int SLOP = 55;
  private static final String NEW_PRO_F1_PNG = "NewProF1.png";
  private static final String RECORDING_DISABLED = "RECORDING DISABLED";
  private static final int MIN_SCREEN_WIDTH = 1100;
  private static final String LOG_OUT = "Log Out";
  private static final double MIN_RATIO = 0.7;
  private static final int min = 720;
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@dliflc.edu";
  //  private static final String LTEA_DLIFLC_EDU = "ltea@dliflc.edu";
  private final String HREF;
  private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Need Help? Questions? Contact us.";
  private static final String DOCUMENTATION = "User Manual";

  private final boolean isAnonymous;
  private Paragraph appName;
  private Image flashcardImage;
  private Image collab;
  private HTML userNameWidget;
  private final String nameForAnswer;
  private Paragraph subtitle;
  private HTML browserInfo;
  private Panel qc, recordAudio;
  private Dropdown cogMenu;
  private final PropertyHandler props;
  private NavLink userC, resultsC, monitoringC, eventsC, reloadLink, downloadC;

  /**
   * @see mitll.langtest.client.InitialUI#makeHeaderRow
   */
  public Banner(PropertyHandler props) {
    this.props = props;
    this.nameForAnswer = props.getNameForAnswer() + "s";
    isAnonymous = props.getLoginType().equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS);
    HREF = "mailto:" +
        NETPROF_HELP_LL_MIT_EDU + "?" +
        //   "cc=" + LTEA_DLIFLC_EDU + "&" +
        "Subject=Question%20about%20" + props.getLanguage() + "%20NetProF";
  }

  /**
   * @param splashText
   * @param userName
   * @param reload
   * @param downloadContext
   * @return
   * @see InitialUI#makeHeaderRow()
   */
  public Panel makeNPFHeaderRow(String splashText, boolean isBeta, String userName, HTML browserInfo,
                                ClickHandler logoutClickHandler,
                                ClickHandler users,
                                ClickHandler results,
                                ClickHandler monitoring,
                                ClickHandler events,
                                ClickHandler reload,
                                ClickHandler downloadContext) {
    HorizontalPanel headerRow = new HorizontalPanel();
    headerRow.setWidth("100%");
    headerRow.addStyleName("headerBackground");
    headerRow.addStyleName("headerLowerBorder");

    Panel iconLeftHeader = new HorizontalPanel();
    headerRow.add(iconLeftHeader);

    Panel flashcard = new FlowPanel();
    flashcard.addStyleName("inlineBlockStyle");
    flashcard.addStyleName("headerBackground");
    flashcard.addStyleName("leftAlign");
    String betaMark = isBeta ? ("<span><font color='yellow'>" + "&nbsp;BETA" + "</font></span>") : "";
    appName = new Paragraph("<span>" + props.getAppTitle() + "</span>" + betaMark);
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    flashcard.add(getSubtitle(splashText));

    flashcardImage = new Image(LangTest.LANGTEST_IMAGES + Banner.NEW_PRO_F1_PNG);
    flashcardImage.addStyleName("floatLeft");
    flashcardImage.addStyleName("rightFiveMargin");
    iconLeftHeader.add(flashcardImage);
    iconLeftHeader.add(flashcard);
    headerRow.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    collab = new Image(LangTest.LANGTEST_IMAGES + "collabIcon3.png");
    DivWidget widget = new DivWidget();
    widget.add(collab);

    Panel hp = new HorizontalPanel();
    hp.getElement().setId("UsernameContainer");
    hp.add(getAnchorManual());
    hp.add(getAnchor());

    userNameWidget = getUserNameWidget(userName);
    if (!isAnonymous) {
      hp.add(userNameWidget);
    }
    hp.add(qc = new SimplePanel());
    hp.add(recordAudio = new SimplePanel());

    // add log out/admin options cogMenu
    makeCogMenu(logoutClickHandler, users, results, monitoring, events, reload, downloadContext);

    if (!isAnonymous) {
      hp.add(cogMenu);
    }

    browserInfo.addStyleName("leftFiveMargin");
    browserInfo.addStyleName("darkerBlueColor");
    this.browserInfo = browserInfo;
    hp.add(browserInfo);
    widget.add(hp);
    hp.addStyleName("topMinusFiveMargin");

    headerRow.add(widget);
    headerRow.addAttachHandler(new AttachEvent.Handler() {
      @Override
      public void onAttachOrDetach(AttachEvent event) {
        onResize();
      }
    });

    return headerRow;
  }

  /**
   * @param splashText
   * @return
   * @see #makeNPFHeaderRow(String, boolean, String, HTML, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler)
   */
  private Paragraph getSubtitle(String splashText) {
    subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    subtitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return subtitle;
  }

  public void setSubtitle() {
    this.subtitle.setText(RECORDING_DISABLED);
    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
  }

  /**
   * @param logoutClickHandler
   * @param users
   * @param results
   * @param monitoring
   * @param events
   * @see #makeNPFHeaderRow
   */
  private void makeCogMenu(ClickHandler logoutClickHandler,
                           ClickHandler users,
                           ClickHandler results,
                           ClickHandler monitoring,
                           ClickHandler events,
                           ClickHandler reload,
                           ClickHandler downloadContext) {
    cogMenu = makeMenu(users, results, monitoring, events, reload, downloadContext);
    cogMenu.addStyleName("cogStyle");
    NavLink widget1 = new NavLink(LOG_OUT);
    widget1.addClickHandler(logoutClickHandler);
    cogMenu.add(widget1);
  }

  public void reflectPermissions(Collection<User.Permission> permissions) {
    boolean hasAudio = permissions.contains(User.Permission.RECORD_AUDIO);
    qc.clear();
    if (permissions.contains(User.Permission.QUALITY_CONTROL)) {
      Icon child = new Icon(IconType.EDIT);
      child.addStyleName("darkerBlueColor");
      if (!hasAudio) {
        child.getElement().getStyle().setMarginRight(3, Style.Unit.PX);
      }
      qc.add(child);
    }

    recordAudio.clear();
    if (hasAudio) {
      Icon child = new Icon(IconType.MICROPHONE);
      child.addStyleName("darkerBlueColor");
      child.getElement().getStyle().setMarginRight(3, Style.Unit.PX);
      recordAudio.add(child);
    }
  }

  /**
   * @param val
   * @see mitll.langtest.client.InitialUI#gotUser(mitll.langtest.shared.User)
   * @see mitll.langtest.client.InitialUI#handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see mitll.langtest.client.InitialUI#showLogin
   */
  public void setCogVisible(boolean val) {
    cogMenu.setVisible(val);
    userNameWidget.setVisible(val);
  }

  /**
   * @param v
   * @see mitll.langtest.client.InitialUI#showUserPermissions(long) (long)
   */
  public void setBrowserInfo(String v) {
    browserInfo.setHTML(v);
  }

  /**
   * @param userName
   * @return
   * @see #makeNPFHeaderRow(String, boolean, String, HTML, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler, ClickHandler)
   */
  private HTML getUserNameWidget(String userName) {
    userNameWidget = new HTML(userName);
    userNameWidget.getElement().setId("Username");
    userNameWidget.addStyleName("bold");

    userNameWidget.addStyleName("rightTenMargin");
    userNameWidget.addStyleName("blueColor");
    return userNameWidget;
  }

  private Anchor getAnchor() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, HREF);
  }

  private Anchor getAnchorManual() {
    Anchor anchor = getAnchor(DOCUMENTATION, "NetProF_Manual.pdf");
    anchor.getElement().getStyle().setColor("#5bb75b");
    return anchor;
  }

  private Anchor getAnchor(String title, String href) {
    Anchor emailAnchor = new Anchor(title, href);

    emailAnchor.getElement().setId("emailAnchor");
    emailAnchor.addStyleName("bold");
    emailAnchor.addStyleName("rightTwentyMargin");
    emailAnchor.getElement().setAttribute("download", "");
    emailAnchor.getElement().getStyle().setColor("#90B3CF");
    return emailAnchor;
  }


  /**
   * @param users
   * @param results
   * @param monitoring
   * @return
   * @see #makeCogMenu
   */
  private Dropdown makeMenu(ClickHandler users, ClickHandler results, ClickHandler monitoring, ClickHandler events, ClickHandler reload, ClickHandler downloadContext) {
    Dropdown w = new Dropdown();
    w.setRightDropdown(true);
    w.setIcon(IconType.COG);
    w.setIconSize(IconSize.LARGE);

    w.add(getAbout());

    userC = new NavLink("Users");
    userC.addClickHandler(users);
    w.add(userC);

    resultsC = new NavLink(nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1));
    resultsC.addClickHandler(results);
    w.add(resultsC);

    monitoringC = new NavLink("Monitoring");
    monitoringC.addClickHandler(monitoring);
    w.add(monitoringC);

    eventsC = new NavLink("Events");
    eventsC.addClickHandler(events);
    w.add(eventsC);

    reloadLink = new NavLink("Reload from Domino");
    if (reload != null) {
      reloadLink.addClickHandler(reload);
      w.add(reloadLink);
    }

    downloadC = new NavLink("Download Context");
    downloadC.addClickHandler(downloadContext);
    w.add(downloadC);
    return w;
  }

  private NavLink getAbout() {
    NavLink about = new NavLink("About NetProF");
    about.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        List<String> strings = java.util.Arrays.asList(
            "Language",
            "Version       ",
            "Release Date  ",
            "Model Version",
            "Recording type");

        List<String> values = null;
        try {
          String versionInfo = LangTest.VERSION_INFO;
          String releaseDate = props.getReleaseDate();
          String recordingInfo = FlashRecordPanelHeadless.usingWebRTC() ? " Browser recording" : "Flash recording";
          String model = props.getModelDir().replaceAll("models.", "");
          values = java.util.Arrays.asList(
              props.getLanguage(),
              versionInfo,
              releaseDate,
              model,
              recordingInfo
          );
        } catch (Exception e) {
          logger.warning("got " + e);
        }

        new ModalInfoDialog("About NetProF", strings, values, null, null, false, true) {
          @Override
          protected FlexTable addContent(Collection<String> messages, Collection<String> values, Modal modal, boolean bigger) {
            FlexTable flexTable = super.addContent(messages, values, modal, bigger);

            int rowCount = flexTable.getRowCount();
            flexTable.setHTML(rowCount + 1, 0, "Need Help?");
            flexTable.setHTML(rowCount + 1, 1, " <a href='" + HREF + "'>Help Email</a>");
            return flexTable;
          }
        };
      }
    });
    return about;
  }

  public void setVisibleAdmin(boolean visibleAdmin) {
    userC.setVisible(visibleAdmin);
    resultsC.setVisible(visibleAdmin);
    monitoringC.setVisible(visibleAdmin);
    eventsC.setVisible(visibleAdmin);
    reloadLink.setVisible(visibleAdmin);
  }

  /**
   * @param name
   * @see mitll.langtest.client.LangTest#gotUser
   */
  public void setUserName(String name) {
    this.userNameWidget.setText(name);
  }


  @Override
  public void onResize() {
    if (Window.getClientWidth() < MIN_SCREEN_WIDTH) {
      setFontWidth();
    } else {
      appName.getElement().getStyle().setFontSize(MAX_FONT_EM, Style.Unit.EM);
    }
  }

  private void setFontWidth() {
    int clientWidth = Window.getClientWidth();

    int offsetWidth = flashcardImage.getOffsetWidth();
    int offsetWidth1 = collab.getOffsetWidth();
    int residual = clientWidth - offsetWidth - offsetWidth1 - SLOP;

    //System.out.println("setFontWidth : left " + offsetWidth + " right " + offsetWidth1 + " window " + clientWidth + " residual " + residual);

    double ratio = 2.0d * (double) residual / (double) min;
    ratio *= 10;
    ratio = Math.floor(ratio);
    ratio /= 10;
    if (ratio < MIN_RATIO) ratio = MIN_RATIO;
    if (ratio > MAX_FONT_EM) ratio = MAX_FONT_EM;
    appName.getElement().getStyle().setFontSize(ratio, Style.Unit.EM);
  }
}