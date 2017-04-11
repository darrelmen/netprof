package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.UILifecycle;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.DRILL;
import static mitll.langtest.client.banner.NewContentChooser.LEARN;
import static mitll.langtest.client.banner.NewContentChooser.PROGRESS;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner, ValueChangeHandler<String> {
  private final Logger logger = Logger.getLogger("NewBanner");
  private static final String NEW_PRO_F1_PNG = "NewProF1_48x48.png";
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@dliflc.edu";
  //  private static final String LTEA_DLIFLC_EDU = "ltea@dliflc.edu";
  // private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Need Help? Questions? Contact us.";
  private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Contact us";
  private static final String DOCUMENTATION = "User Manual";

  private UILifecycle lifecycle;
  private UserManager userManager;
  private INavigation navigation;
  private HandlerRegistration handlerRegistration;
  private Map<String,NavLink> nameToLink = new HashMap<>();

  /**
   * @param userManager
   * @param lifecycle
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
  public NewBanner(UserManager userManager, UILifecycle lifecycle, UserMenu userMenu) {
    Brand netprof = new Brand("netprof");
    addHomeClick(netprof);
    netprof.addStyleName("handCursor");

    add(getImage());
    add(netprof);
    NavCollapse navCollapse = new NavCollapse();
    add(navCollapse);

    Nav lnav = new Nav();
    navCollapse.add(lnav);
    addChoicesForUser(lnav);

    Nav rnav = getRightSideChoices(userManager, userMenu);
    navCollapse.add(rnav);

    setCogVisible(userManager.hasUser());
    this.userManager = userManager;
    this.lifecycle = lifecycle;

    addHistoryListener();
  }

  @NotNull
  private Nav getRightSideChoices(UserManager userManager, UserMenu userMenu) {
    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);

    userDrop = new Dropdown(userManager.getUserID());
    userDrop.setIcon(IconType.USER);
    rnav.add(userDrop);

    List<LinkAndTitle> userMenuChoices = userMenu.getUserMenuChoices();
    //logger.info("adding " + userMenuChoices.size());

    for (LinkAndTitle linkAndTitle : userMenuChoices) {
      userDrop.add(linkAndTitle.getLink());
    }

    getInfoMenu(userMenu, rnav);
    return rnav;
  }

  private void addHistoryListener() {
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    SelectionState selectionState = new SelectionState(token, false);
    String instance1 = selectionState.getInstance();

    logger.info("got " + token + " instance " + instance1);

    switch (instance1) {
      case LEARN:
        navigation.showLearn();
        showActive(nameToLink.get(LEARN));
        break;
      case DRILL:
        navigation.showDrill();
        showActive(nameToLink.get(DRILL));
        break;
      case PROGRESS:
        navigation.showProgress();
        showActive(nameToLink.get(PROGRESS));
        break;
      default:
        navigation.showLearn();
        break;
    }
  }

  private void getInfoMenu(UserMenu userMenu, Nav rnav) {
    Dropdown info = new Dropdown();
    info.setIcon(IconType.INFO);
    rnav.add(info);
    info.add(userMenu.getAbout());
    info.add(getManual());
    info.add(getContactUs());
  }

  private void addChoicesForUser(Nav nav) {
    for (String choice : Arrays.asList("Learn", "Drill", "Progress", "List")) {
      choices.add(getChoice(nav, choice));
    }
//    choices.add(getChoice(nav, "Drill"));
//    choices.add(getChoice(nav, "Progress", event -> navigation.showProgress()));
//    choices.add(getChoice(nav, "Lists", event -> navigation.showLists()));
  }

  private Dropdown userDrop;

  private List<Widget> choices = new ArrayList();

//  private NavLink addLearn(Nav nav) {
//    return getChoice(nav, "Learn");
//  }

  NavLink currentActive = null;

  @NotNull
  private NavLink getChoice(Nav nav, String learn1) {
    NavLink learn = getLink(nav, learn1);
    ClickHandler clickHandler = event -> {
      switch (learn1) {
        case "Learn":
          navigation.showLearn();
          break;
        case "Drill":
          navigation.showDrill();
          break;
        case "Progress":
          navigation.showProgress();
          break;
        case "Lists":
          navigation.showLists();
          break;
      }
      showActive(learn);
    };
    learn.addClickHandler(clickHandler);
    return learn;
  }

  @NotNull
  private NavLink getLink(Nav nav, String learn1) {
    NavLink learn = new NavLink(learn1);
    nameToLink.put(learn1,learn);
    nav.add(learn);
    return learn;
  }

  private void showActive(NavLink learn) {
    for (NavLink link:nameToLink.values()) link.setActive(false);

    learn.setActive(true);
//    if (currentActive != null) {
//      currentActive.setActive(false);
  //  }
    //currentActive = learn;

  }

//  private Widget addDrill(Nav nav) {
//    return getChoice(nav, "Drill", event -> navigation.showDrill());
//  }

  private Image getImage() {
    Image flashcardImage = new Image(LangTest.LANGTEST_IMAGES + NEW_PRO_F1_PNG);
    flashcardImage.addStyleName("floatLeftAndClear");
    flashcardImage.addStyleName("rightFiveMargin");
    flashcardImage.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    addHomeClick(flashcardImage);
    return flashcardImage;
  }

  private void addHomeClick(HasClickHandlers flashcardImage) {
    flashcardImage.addClickHandler(event -> lifecycle.chooseProjectAgain());
  }

  /**
   * @return
   * @seez #gotUser
   * @seez #makeHeaderRow()
   */
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : ("" + userManager.getUserID());
  }

  @Override
  public Panel getBanner() {
    return this;
  }

  @Override
  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }

  @Override
  public void setSubtitle() {

  }

  @Override
  public void reflectPermissions(Collection<User.Permission> permissions) {

  }

  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);
    for (Widget choice : choices) choice.setVisible(val);
  }

  @Override
  public void setBrowserInfo(String v) {

  }

  @Override
  public void setVisibleAdmin(boolean visibleAdmin) {

  }

  @Override
  public void setUserName(String name) {
    userDrop.setTitle(name);
    userDrop.setText(name);
  }

  @Override
  public void onResize() {

  }

  private NavLink getContactUs() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, getMailTo());
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" +
        NETPROF_HELP_LL_MIT_EDU + "?" +
        //   "cc=" + LTEA_DLIFLC_EDU + "&" +
        "Subject=Question%20about%20NetProF";
  }

  /**
   * Make sure this points to the manual.
   *
   * @return
   */
  private NavLink getManual() {
    NavLink anchor = getAnchor(DOCUMENTATION, "langtest/NetProF_Manual.pdf");
    anchor.getElement().getStyle().setColor("#5bb75b");
    return anchor;
  }

  private NavLink getAnchor(String title, String href) {
    NavLink emailAnchor = new NavLink(title, href);

    emailAnchor.getElement().setId("emailAnchor");
    emailAnchor.addStyleName("bold");
    emailAnchor.addStyleName("rightTwentyMargin");
    emailAnchor.getElement().setAttribute("download", "");
    emailAnchor.getElement().getStyle().setColor("#90B3CF");
    return emailAnchor;
  }
}
