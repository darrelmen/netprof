package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.User;

import java.util.Collection;

/**
 * Does fancy font sizing depending on available width...
 */
public class Flashcard implements RequiresResize {
  private static final String PRONUNCIATION_FEEDBACK = "NetProF – Networked Pronunciation Feedback";//"Classroom";//NetProF";//"PRONUNCIATION FEEDBACK";
  private static final double MAX_FONT_EM = 1.7d;
  private static final int SLOP = 55;
  private static final String NEW_PRO_F1_PNG = "NewProF1.png";
  private static final String RECORDING_DISABLED = "RECORDING DISABLED";
  private static final int MIN_SCREEN_WIDTH = 1100;
  private static final String LOG_OUT = "Log Out";
  private static final double MIN_RATIO = 0.7;
  private static final int min = 720;
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@ll.mit.edu";
  private final String HREF;
  public static final String NEED_HELP_QUESTIONS_CONTACT_US = "Need Help? Questions? Contact us.";

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

  /**
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Flashcard(PropertyHandler props) {
    this.nameForAnswer = props.getNameForAnswer() + "s";
    isAnonymous = props.getLoginType().equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS);
    HREF = "mailto:" +
        NETPROF_HELP_LL_MIT_EDU + "?Subject=Question%20about%20" + props.getLanguage() + "%20NetProF";
  }

  /**
   * @param splashText
   * @param userName
   * @return
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Panel makeNPFHeaderRow(String splashText, boolean isBeta, String userName, HTML browserInfo,
                                ClickHandler logoutClickHandler,
                                ClickHandler users,
                                ClickHandler results,
                                ClickHandler monitoring,
                                ClickHandler events) {
    return getHeaderRow(splashText, isBeta, userName, browserInfo, logoutClickHandler,
        users, results, monitoring, events);
  }

  /**
   * @param splashText
   * @param isBeta
   * @param userName
   * @param browserInfo
   * @param logoutClickHandler
   * @param users
   * @param results
   * @param monitoring
   * @param events
   * @return
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  private Panel getHeaderRow(String splashText,
                             boolean isBeta, String userName,
                             HTML browserInfo,
                             ClickHandler logoutClickHandler,

                             ClickHandler users,
                             ClickHandler results,
                             ClickHandler monitoring,
                             ClickHandler events) {
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
    appName = new Paragraph("<span>" + Flashcard.PRONUNCIATION_FEEDBACK + "</span>" + betaMark);
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    flashcard.add(getSubtitle(splashText));

    flashcardImage = new Image(LangTest.LANGTEST_IMAGES + Flashcard.NEW_PRO_F1_PNG);
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

    Anchor emailAnchor = getAnchor();
    hp.add(emailAnchor);

    userNameWidget = getUserNameWidget(userName);
    if (!isAnonymous) {
      hp.add(userNameWidget);
    }
    hp.add(qc = new SimplePanel());
    hp.add(recordAudio = new SimplePanel());

    // add log out/admin options cogMenu
    makeCogMenu(logoutClickHandler, users, results, monitoring, events);

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

  private Paragraph getSubtitle(String splashText) {
    subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    subtitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return subtitle;
  }

  private void makeCogMenu(ClickHandler logoutClickHandler, ClickHandler users, ClickHandler results, ClickHandler monitoring, ClickHandler events) {
    cogMenu = makeMenu(users, results, monitoring, events);
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
   * @see mitll.langtest.client.LangTest#gotUser(mitll.langtest.shared.User)
   * @see mitll.langtest.client.LangTest#handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see mitll.langtest.client.LangTest#showLogin(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel)
   */
  public void setCogVisible(boolean val) {
    cogMenu.setVisible(val);
    userNameWidget.setVisible(val);
  }

  /**
   * @param v
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   */
  public void setBrowserInfo(String v) {
    browserInfo.setHTML(v);
  }

  /**
   * @param userName
   * @return
   * @see #getHeaderRow
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
    Anchor emailAnchor = new Anchor(NEED_HELP_QUESTIONS_CONTACT_US, HREF);

    emailAnchor.getElement().setId("emailAnchor");
    emailAnchor.addStyleName("bold");
    emailAnchor.addStyleName("rightTwentyMargin");
    emailAnchor.getElement().getStyle().setColor("#90B3CF");
    return emailAnchor;
  }

  private NavLink userC, resultsC, monitoringC, eventsC;

  /**
   * @param users
   * @param results
   * @param monitoring
   * @return
   * @see #getHeaderRow
   */
  private Dropdown makeMenu(ClickHandler users, ClickHandler results, ClickHandler monitoring, ClickHandler events) {
    Dropdown w = new Dropdown();
    w.setRightDropdown(true);
    w.setIcon(IconType.COG);
    w.setIconSize(IconSize.LARGE);

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

    return w;
  }

  public void setVisibleAdmin(boolean visibleAdmin) {
    userC.setVisible(visibleAdmin);
    resultsC.setVisible(visibleAdmin);
    monitoringC.setVisible(visibleAdmin);
    eventsC.setVisible(visibleAdmin);
  }

  /**
   * @param name
   * @see mitll.langtest.client.LangTest#gotUser
   */
  public void setUserName(String name) {
    this.userNameWidget.setText(name);
  }

  public void setSplash() {
    this.subtitle.setText(RECORDING_DISABLED);

    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
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