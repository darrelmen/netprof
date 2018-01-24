package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.constants.NavbarPosition;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.VIEWS;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner {
  private final Logger logger = Logger.getLogger("NewBanner");


  private static final String NETPROF = "netprof";
  private static final String IS_YOUR_MICROPHONE_ACTIVE = "Is your microphone active?";

  private static final String NETPROF_MANUAL = "langtest/NetProF_Manual.pdf";
  private static final String MAILTO_SUBJECT = "Question%20about%20netprof";

  public static final String SHOW = "showStorage";
  /**
   * @see #getImage
   */
  private static final String NEW_PRO_F1_PNG = "NewProF1_48x48.png";
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@dliflc.edu";
  private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Contact us";
  private static final String DOCUMENTATION = "User Manual";
  /**
   * @see #setSubtitle
   */
  private static final String RECORDING_DISABLED = "RECORDING DISABLED";

  private final UILifecycle lifecycle;
  private ComplexWidget recnav, defectnav;

  private Nav lnav;
  private Dropdown cog;

  private INavigation navigation;
  private final Map<String, NavLink> nameToLink = new HashMap<>();
  private Dropdown userDrop;
  /**
   * @see #addChoicesForUser(ComplexWidget)
   */
  private final Map<VIEWS, NavLink> viewToLink = new HashMap<>();

  /**
   * @see #addChoicesForUser
   */
  private final List<Widget> choices = new ArrayList<>();

  private final ExerciseController controller;

  /**
   * setInverse = white on black background
   * @param userManager
   * @param lifecycle
   * @see InitialUI#InitialUI
   */
  public NewBanner(UserManager userManager,
                   UILifecycle lifecycle,
                   UserMenu userMenu,
                   Breadcrumbs breadcrumbs,
                   ExerciseController controller) {
    setPosition(NavbarPosition.TOP);
    // we do white on black now
//    setInverse(true);

    this.controller = controller;
    this.lifecycle = lifecycle;

//    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
//      public void execute() {
//        addWidgets(userManager, userMenu, breadcrumbs);
//      }
//    });
    addWidgets(userManager, userMenu, breadcrumbs);
  }

  private void addWidgets(UserManager userManager, UserMenu userMenu, Breadcrumbs breadcrumbs) {
    add(getImage());
    add(getBrand());
    add(breadcrumbs);

    styleBreadcrumbs(breadcrumbs);

    NavCollapse navCollapse = new NavCollapse();
    navCollapse.addStyleName("topFiveMargin");
    navCollapse.getElement().setId("navCollapse1");

    add(navCollapse);

    {
      navCollapse.add(this.lnav = getLNav());
      addChoicesForUser(lnav);
    }
    {
      Nav recnav = getRecNav();
      this.recnav = recnav;
      navCollapse.add(recnav);
    }
    recordMenuVisible();

    {
      Nav defectnav = getDefectNav();
      this.defectnav = defectnav;
      navCollapse.add(defectnav);
    }
    defectMenuVisible();

    navCollapse.add(getRightSideChoices(userManager, userMenu));

    setCogVisible(userManager.hasUser());
    // addHistoryListener();
  }

  @NotNull
  private Nav getRecNav() {
    Nav recnav = new Nav();
    recnav.getElement().setId("recnav");
    styleNav(recnav);
    viewToLink.put(VIEWS.RECORD, getChoice(recnav, VIEWS.RECORD));
    viewToLink.put(VIEWS.CONTEXT, getChoice(recnav, VIEWS.CONTEXT));
    return recnav;
  }

  private void styleNav(Nav recnav) {
    recnav.addStyleName("inlineFlex");
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);
  }

  @NotNull
  private Nav getDefectNav() {
    Nav nav = new Nav();
    nav.getElement().setId("defectnav");
    styleNav(nav);

    viewToLink.put(VIEWS.DEFECTS, getChoice(nav, VIEWS.DEFECTS));
    viewToLink.put(VIEWS.FIX, getChoice(nav, VIEWS.FIX));

    return nav;
  }

  @NotNull
  private Brand getBrand() {
    Brand netprof = new Brand(NETPROF);
    netprof.addStyleName("topFiveMargin");
    addHomeClick(netprof);
    netprof.addStyleName("handCursor");
    new TooltipHelper().addTooltip(netprof, "Choose a different language or project.");

    return netprof;
  }

  private void styleBreadcrumbs(Breadcrumbs breadcrumbs) {
    breadcrumbs.addStyleName("floatLeft");
    Style style = breadcrumbs.getElement().getStyle();
    style.setMarginTop(7, Style.Unit.PX);
    breadcrumbs.addStyleName("rightTwentyMargin");
  }

  @NotNull
  private Nav getLNav() {
    Nav lnav = new Nav();
    lnav.getElement().setId("lnav");
    styleNav(lnav);
    return lnav;
  }

  /**
   * @param userManager
   * @param userMenu
   * @return
   * @see #addWidgets
   */
  @NotNull
  private Nav getRightSideChoices(UserManager userManager, UserMenu userMenu) {
    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    addSubtitle(rnav);

    addUserMenu(userManager, userMenu, rnav);

    {
      cog = new Dropdown("");
      cog.setIcon(IconType.COG);

      userMenu.getCogMenuChoicesForAdmin().forEach(lt -> cog.add(lt.makeNewLink()));

      hasProjectChoices = userMenu.getProjectSpecificChoices();
      hasProjectChoices.forEach(lt -> cog.add(lt.makeNewLink()));

      rnav.add(cog);
    }

    getInfoMenu(userMenu, rnav);
    return rnav;
  }

  private List<LinkAndTitle> hasProjectChoices;

  private void addSubtitle(Nav rnav) {
    rnav.add(subtitle = new Label());

    subtitle.addStyleName("floatLeft");
    subtitle.setType(LabelType.WARNING);
    subtitle.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    new TooltipHelper().addTooltip(subtitle, IS_YOUR_MICROPHONE_ACTIVE);
    subtitle.setVisible(!controller.isRecordingEnabled());
  }

  /**
   * @param userManager
   * @param userMenu
   * @param rnav
   * @see #getRightSideChoices
   */
  private void addUserMenu(UserManager userManager, UserMenu userMenu, Nav rnav) {
    userDrop = new Dropdown(userManager.getUserID());
    userDrop.setIcon(IconType.USER);
    rnav.add(userDrop);
    userMenu.getStandardUserMenuChoices().forEach(lt -> userDrop.add(lt.makeNewLink()));
  }

  private Label subtitle;

  /**
   * Tell them we can't record.
   *
   * @see InitialUI#setSplash
   */
  @Override
  public void setSubtitle() {
    this.subtitle.setText(RECORDING_DISABLED);
    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
  }



  private void getInfoMenu(UserMenu userMenu, Nav rnav) {
    Dropdown info = new Dropdown();
    info.setIcon(IconType.INFO);
    rnav.add(info);
    info.add(userMenu.getAbout());
    info.add(getManual());
    info.add(getContactUs());
  }


  /**
   * @param nav
   */
  private void addChoicesForUser(ComplexWidget nav) {
    boolean first = true;
    for (VIEWS choice : Arrays.asList(VIEWS.LEARN, VIEWS.PRACTICE, VIEWS.PROGRESS, VIEWS.LISTS)) {
      NavLink choice1 = getChoice(nav, choice);
      if (first) {
        choice1.addStyleName("leftTwentyMargin");
      }
      first = false;
      choices.add(choice1);
      viewToLink.put(choice, choice1);
    }
  }

  @NotNull
  private NavLink getChoice(ComplexWidget nav, VIEWS views) {
    String instanceName = views.toString();
//    String historyToken = SelectionState.SECTION_SEPARATOR + SelectionState.INSTANCE + "=" + instanceName;
    NavLink learn = getLink(nav, instanceName);
    learn.addClickHandler(event -> {
      //  logger.info("getChoice got click on " + instanceName + " = " + historyToken);
      gotClickOnChoice(instanceName, learn);
      // setHistoryItem(historyToken);
    });
    return learn;
  }

  /**
   * @see NewContentChooser#getShowTab
   */
  public void showLearn() {
    gotClickOnChoice(VIEWS.LEARN.toString(), viewToLink.get(VIEWS.LEARN));
  }

  public void showDrill() {
    gotClickOnChoice(VIEWS.PRACTICE.toString(), viewToLink.get(VIEWS.PRACTICE));
  }

  private void gotClickOnChoice(String instanceName, NavLink learn) {
    showSection(instanceName);
    showActive(learn);
  }

  private void showSection(String instance1) {
    VIEWS choices = VIEWS.NONE;
    try {
      choices = VIEWS.valueOf(instance1.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.info("can't parse " + instance1);

    }
    navigation.showView(choices);
  }

  /**
   * @param learn
   * @see #checkProjectSelected
   * @see #gotClickOnChoice
   */
  private void showActive(NavLink learn) {
    for (NavLink link : nameToLink.values()) link.setActive(false);
    learn.setActive(true);
  }

  @NotNull
  private NavLink getLink(ComplexWidget nav, String learn1) {
    NavLink learn = new NavLink(learn1);
    nameToLink.put(learn1, learn);
    nav.add(learn);
    return learn;
  }

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

  @Override
  public Panel getBanner() {
    return this;
  }

  @Override
  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }

  @Override
  public void reflectPermissions(Collection<User.Permission> permissions) {
    recordMenuVisible();
    defectMenuVisible();
  }

  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);
    choices.forEach(choice -> choice.setVisible(val));
    cog.setVisible(isAdmin());

    boolean hasProject = controller.getProjectStartupInfo() != null;
    //   logger.info("setCogVisible " + val + " has project " + hasProject + " is admin " + admin);
    hasProjectChoices.forEach(linkAndTitle -> linkAndTitle.getMyLink().setVisible(hasProject));
  }

  @Override
  public void reset() {
    setCogVisible(false);
    setRecNavVisible(false);
    setDefectNavVisible(false);
  }

  @Override
  public void setUserName(String name) {
    userDrop.setTitle(name);
    userDrop.setText(name);
    recordMenuVisible();
  }

  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }

  private void defectMenuVisible() {
    boolean qc = isQC();
    boolean b = hasProjectChoice();
//    logger.info("is QC " + controller.getUser() + " : " + qc + " has choice " + b);
    boolean visible = qc && b;
    setDefectNavVisible(visible);
  }

  private void setDefectNavVisible(boolean visible) {
    defectnav.setVisible(visible);
  }

  private boolean isPermittedToRecord() {
    return controller.getUserState().hasPermission(User.Permission.RECORD_AUDIO) || isAdmin();
  }

  private boolean isAdmin() {
    return controller.getUserState().isAdmin();
  }

  private boolean isQC() {
   // logger.info("isQC " + controller.getUserState().getPermissions() + " is admin " + isAdmin());
    boolean admin = isAdmin();
   // logger.info("is admin " + admin);
    boolean canDoQC = controller.getUserState().hasPermission(User.Permission.QUALITY_CONTROL);
   // logger.info("is canDoQC " + canDoQC);
    return canDoQC || admin;
  }

  /**
   * @see InitialUI#addCrumbs
   * @see InitialUI#showInitialState
   */
  @Override
  public void checkProjectSelected() {
    setVisibleChoices(controller.getProjectStartupInfo() != null);
    VIEWS currentView = navigation.getCurrentView();
    NavLink linkToShow = viewToLink.get(currentView);

    if (linkToShow == null) {
      logger.warning("checkProjectSelected Current view is " + currentView);
      logger.warning("checkProjectSelected Current view link is " + linkToShow);
      logger.warning("checkProjectSelected huh? keys are " + viewToLink.keySet());
      linkToShow = viewToLink.get(VIEWS.LEARN);
    }
    showActive(linkToShow);
    recordMenuVisible();
  }

  public void setVisibleChoices(boolean show) {
    lnav.setVisible(show);
    reflectPermissions(controller.getPermissions());
  }

  private void recordMenuVisible() {
    if (recnav != null) {
      boolean visible = isPermittedToRecord() && hasProjectChoice();
      setRecNavVisible(visible);
    }
  }

  private void setRecNavVisible(boolean visible) {
    recnav.setVisible(visible);
  }

  private NavLink getContactUs() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, getMailTo());
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" + NETPROF_HELP_LL_MIT_EDU + "?" + "Subject=" + MAILTO_SUBJECT;
  }

  /**
   * Make sure this points to the manual.
   *
   * @return
   */
  private NavLink getManual() {
    NavLink anchor = getAnchor(DOCUMENTATION, NETPROF_MANUAL);
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
