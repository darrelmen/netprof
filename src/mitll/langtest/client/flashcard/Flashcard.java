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
  private static final String PRONUNCIATION_FEEDBACK = "PRONUNCIATION FEEDBACK";
  private static final double MAX_FONT_EM = 1.7d;
  private static final int SLOP = 55;
  private static final String NEW_PRO_F1_PNG = "NewProF1.png";
  private final boolean isAnonymous;
  private Paragraph appName;
  private Image flashcardImage;
  private Image collab;
  private static final int min = 720;
  private HTML userNameWidget;
  private final String nameForAnswer;
  private final boolean adminView;
  private Paragraph subtitle;
  private HTML browserInfo;
  private Panel qc,recordAudio;
  private Dropdown cogMenu;

  /**
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Flashcard(PropertyHandler props) {
    this.nameForAnswer = props.getNameForAnswer() + "s";
    isAnonymous = props.getLoginType().equals(PropertyHandler.LOGIN_TYPE.ANONYMOUS);
    adminView = props.isAdminView();
  }

  /**
   * @param splashText
   * @param userName
   * @return
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Panel makeNPFHeaderRow(String splashText, boolean isBeta, String userName, HTML browserInfo, ClickHandler logoutClickHandler,
                                ClickHandler users,
                                ClickHandler results,
                                ClickHandler monitoring,
                                ClickHandler events) {
    return getHeaderRow(splashText, isBeta,NEW_PRO_F1_PNG, userName, browserInfo, logoutClickHandler,
      users, results, monitoring,events);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   * @param splashText
   * @param isBeta
   * @param appIcon
   * @param userName
   * @param browserInfo
   * @param logoutClickHandler
   * @param users
   * @param results
   * @param monitoring
   * @param events
   * @return
   */
  public Panel getHeaderRow(String splashText,
                            boolean isBeta, String appIcon, String userName,
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
    appName = new Paragraph("<span>" + Flashcard.PRONUNCIATION_FEEDBACK + "</span>" +(isBeta?("<span><font color='yellow'>" + "&nbsp;BETA" + "</font></span>"):""));
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    subtitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);

      flashcard.add(subtitle);

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
    userNameWidget = getUserNameWidget(userName);
    if (!isAnonymous || adminView) {
      hp.add(userNameWidget);
    }
    else {
      System.out.println("anon " +isAnonymous + " admin " + adminView);
    }

    //if (permissions.contains(User.Permission.QUALITY_CONTROL)) {
      hp.add(qc = new SimplePanel());
    //}

    //if (permissions.contains(User.Permission.RECORD_AUDIO)) {
      hp.add(recordAudio = new SimplePanel());
    //}

    // add log out/admin options cogMenu
    cogMenu = makeMenu(users, results, monitoring, events);
    cogMenu.addStyleName("cogStyle");
    NavLink widget1 = new NavLink("Log Out");
    widget1.addClickHandler(logoutClickHandler);
    cogMenu.add(widget1);

    if (!isAnonymous || adminView) {
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
   * @see mitll.langtest.client.LangTest#gotUser(mitll.langtest.shared.User)
   * @see mitll.langtest.client.LangTest#handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see mitll.langtest.client.LangTest#showLogin(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel)
   *
   * @param val
   */
  public void setCogVisible(boolean val) {
    cogMenu.setVisible(val);
    userNameWidget.setVisible(val);
  }

  /**
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   * @param v
   */
  public void setBrowserInfo(String v) { browserInfo.setHTML(v);}

  /**
   * @see #getHeaderRow(String, boolean, String, String, String, com.google.gwt.user.client.ui.HTML, com.google.gwt.event.dom.client.ClickHandler, com.google.gwt.event.dom.client.ClickHandler, com.google.gwt.event.dom.client.ClickHandler, com.google.gwt.event.dom.client.ClickHandler, com.google.gwt.event.dom.client.ClickHandler)
   * @param userName
   * @return
   */
  private HTML getUserNameWidget(String userName) {
    userNameWidget = new HTML(userName);
    userNameWidget.getElement().setId("Username");
    userNameWidget.addStyleName("bold");

    userNameWidget.addStyleName("rightTenMargin");
    userNameWidget.addStyleName("blueColor");
    return userNameWidget;
  }

  NavLink userC, resultsC, monitoringC, eventsC;
  /**
   * @see #getHeaderRow
   * @param users
   * @param results
   * @param monitoring
   * @return
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
   // if (visibleAdmin) {
      userC.setVisible(visibleAdmin);
      resultsC.setVisible(visibleAdmin);
      monitoringC.setVisible(visibleAdmin);
      eventsC.setVisible(visibleAdmin);
    //}
  }

  /**
   * @see mitll.langtest.client.LangTest#gotUser(long)
   * @param name
   */
  public void setUserName(String name) {  this.userNameWidget.setText(name);  }
  public void setSplash() {  this.subtitle.setText("RECORDING DISABLED");

    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
  }

  @Override
  public void onResize() {
      int clientWidth = Window.getClientWidth();

      if (clientWidth < 1100) {
        setFontWidth();
      } else {
        //DOM.setStyleAttribute(appName.getElement(), "fontSize", MAX_FONT_EM + "em");
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
    if (ratio < 0.7) ratio = 0.7;
    if (ratio > MAX_FONT_EM) ratio =  MAX_FONT_EM;
   // String fontsize = ratio + "em";
  //  System.out.println("setFontWidth : Setting font size to " + fontsize);
    //DOM.setStyleAttribute(appName.getElement(), "fontSize", fontsize);
    appName.getElement().getStyle().setFontSize(ratio, Style.Unit.EM);

  }
}