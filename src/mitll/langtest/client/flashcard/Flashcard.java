package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
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
  Paragraph subtitle;
  HTML browserInfo;
  Panel qc,recordAudio;

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
    return getHeaderRow(splashText, isBeta,NEW_PRO_F1_PNG, PRONUNCIATION_FEEDBACK, userName, browserInfo, logoutClickHandler,
      users, results, monitoring,events);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   * @param splashText
   * @param isBeta
   * @param appIcon
   * @param appTitle
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
                            boolean isBeta, String appIcon, String appTitle, String userName,
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
    appName = new Paragraph("<span>" + appTitle + "</span>" +(isBeta?("<span><font color='yellow'>" + "&nbsp;BETA" + "</font></span>"):""));
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    subtitle.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);

      flashcard.add(subtitle);

    flashcardImage = new Image(LangTest.LANGTEST_IMAGES + appIcon);
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

    //if (permissions.contains(User.Permission.QUALITY_CONTROL)) {
      hp.add(qc = new SimplePanel());
    //}

    //if (permissions.contains(User.Permission.RECORD_AUDIO)) {
      hp.add(recordAudio = new SimplePanel());
    //}

    // add log out/admin options menu
    Dropdown menu = makeMenu(users, results, monitoring,events);
    menu.addStyleName("cogStyle");
    NavLink widget1 = new NavLink("Log Out");
    widget1.addClickHandler(logoutClickHandler);
    menu.add(widget1);

    if (!isAnonymous || adminView) {
      hp.add(menu);
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

  public void setBrowserInfo(String v) { browserInfo.setHTML(v);}

  private HTML getUserNameWidget(String userName) {
    userNameWidget = new HTML(userName);
    userNameWidget.getElement().setId("Username");
    userNameWidget.addStyleName("bold");

    userNameWidget.addStyleName("rightTenMargin");
    userNameWidget.addStyleName("blueColor");
    return userNameWidget;
  }

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

    if (users != null) {
      NavLink widget2 = new NavLink("Users");
      widget2.addClickHandler(users);
      w.add(widget2);
    }

    if (results != null) {
      NavLink widget2 = new NavLink(nameForAnswer.substring(0,1).toUpperCase()+nameForAnswer.substring(1));
      widget2.addClickHandler(results);
      w.add(widget2);
    }

    if (monitoring != null) {
      NavLink widget2 = new NavLink("Monitoring");
      widget2.addClickHandler(monitoring);
      w.add(widget2);
    }
    if (events != null) {
      NavLink widget2 = new NavLink("Events");
      widget2.addClickHandler(events);
      w.add(widget2);
    }
    return w;
  }

  /**
   * @see mitll.langtest.client.LangTest#gotUser(long)
   * @param name
   */
  public void setUserName(String name) {  this.userNameWidget.setText(name);  }
  public void setSplash(String name) {  this.subtitle.setText(name);

    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
  }

  @Override
  public void onResize() {
      int clientWidth = Window.getClientWidth();

      if (clientWidth < 1100) {
        setFontWidth();
      } else {
        DOM.setStyleAttribute(appName.getElement(), "fontSize", MAX_FONT_EM + "em");
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
    String fontsize = ratio + "em";
  //  System.out.println("setFontWidth : Setting font size to " + fontsize);
    DOM.setStyleAttribute(appName.getElement(), "fontSize", fontsize);
  }
}